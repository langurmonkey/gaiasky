/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.gdx.graphics.GalaxyVolumeGenerator;
import gaiasky.render.gdx.graphics.VolumeCalibration;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.render.gdx.graphics.VoxelGrid;
import gaiasky.render.gdx.shader.attribute.IntAttribute;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Volume;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.MaterialComponent;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;

import java.util.List;
import java.util.Locale;

/**
 * Initializes volume entities, such as those used for volumetric rendering.
 */
public class VolumeInitializer extends AbstractInitSystem {
    /**
     * Shadows the instance logger of {@link AbstractInitSystem}: generation has to be reachable
     * without an instance - see {@link #generateNow(Entity)}, which the GUI calls - and everything
     * else in this class is static for the same reason.
     */
    private static final Logger.Log logger = Logger.getLogger(VolumeInitializer.class);

    /** The shaders clamp the step count to at least 40, so anything lower does nothing useful. */
    private static final int MIN_ITERATIONS = 40;

    /** Set once compute-based volume generation has been reported as unavailable, to log it once. */
    private static boolean generationUnavailableLogged;

    public VolumeInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var model = Mapper.model.get(entity);
        var volume = Mapper.volume.get(entity);
        if (volume.fragmentShader != null && volume.vertexShader != null) {
            // Key is hash code of concatenation.
            volume.key = (volume.fragmentShader + volume.vertexShader).hashCode();
        }

        // Renderer.
        model.renderConsumer = ModelEntityRenderSystem::renderVolume;
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var model = Mapper.model.get(entity);
        var volume = Mapper.volume.get(entity);
        var label = Mapper.label.get(entity);
        var sa = Mapper.sa.get(entity);

        // Ray-march iteration count for this graphics quality, bridged into the shader as an attribute.
        int iterations = volume.iterations[GaiaSky.settings().graphics.quality.ordinal()];
        if (iterations < MIN_ITERATIONS) {
            logger.warn("Volume iterations " + iterations + " below " + MIN_ITERATIONS + ", clamping to " + MIN_ITERATIONS);
            iterations = MIN_ITERATIONS;
        }
        if (model.model.env != null) {
            model.model.env.set(new IntAttribute(IntAttribute.VolumeIterations, iterations));
        } else {
            // ModelInitializer runs before this one and builds the environment, so this only
            // happens if that ordering changes - and it would silently leave the shader at its own
            // 90 iterations.
            logger.warn("Volume model has no environment, cannot set the iteration count: " + entity);
        }
        // Set units.
        model.model.setUnits(Constants.KM_TO_U);
        // Set size.
        model.model.updateSize(body.size);

        // Procedural volume: voxelize its own fields into the material's two slots.
        if (volume.isProcedural()) {
            scheduleGeneration(entity, volume, model.model, body.size * Constants.U_TO_PC);
        }

        // Set up label
        sa.thresholdLabel = (Math.toRadians(1e-6) / GaiaSky.settings().scene.label.number) * 5000.0;
        label.textScale = 0.2f;
        label.labelMax = 0.0006f;
        if (label.labelFactor == 0)
            label.labelFactor = 0.5e-3f;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextBase;

    }

    /**
     * Regenerates the volume textures of the given entity from its current fields, now.
     * <p>
     * This is the entry point for editing a procedural volume at runtime: the GUI mutates
     * {@link Volume#fields} and calls this, and the volumes are re-voxelised with the fields as they
     * are at that moment. Both slots refill their existing texture whenever the grid still fits, so
     * an edit costs two dispatches and no material re-initialisation - see
     * {@link GalaxyVolumeGenerator#canReuse}.
     * <p>
     * The work is posted to the render thread: it touches GL state (image bindings, SSBO, texture
     * uploads) and the material's attributes, none of which may be done from anywhere else.
     *
     * @param entity The volume entity, which must already be set up.
     */
    public static void generateNow(Entity entity) {
        if (entity == null) {
            return;
        }
        var volume = Mapper.volume.get(entity);
        if (volume == null || !volume.isProcedural()) {
            logger.warn("Cannot regenerate the volume of " + name(entity) + ": the entity has no volume fields.");
            return;
        }
        var model = Mapper.model.get(entity);
        var body = Mapper.body.get(entity);
        if (model == null || model.model == null || body == null) {
            logger.warn("Cannot regenerate the volume of " + name(entity) + ": the entity is not set up.");
            return;
        }
        var modelComponent = model.model;
        String entityName = name(entity);
        double sizePc = body.size * Constants.U_TO_PC;
        GaiaSky.postRunnable(() -> {
            // The entity may be removed between the request and the next frame.
            if (volume.isDisposed()) {
                return;
            }
            scheduleGeneration(entityName, volume, modelComponent, sizePc);
        });
    }

    /**
     * Prepares the generation of a procedural volume's slots: it binds the results to the material
     * now if the material already exists, and otherwise hooks onto the material's initialization.
     * <p>
     * The second case is the common one: with lazy texture initialization on (the default), the
     * material of a volume is not built at set-up time but on the first frame the entity is drawn,
     * inside {@link ModelComponent#touch(com.badlogic.gdx.math.Matrix4)}. Generating at set-up would
     * then have nowhere to bind the textures, which is what used to make this a no-op.
     *
     * @param entity  The entity, for the log and to resolve its name.
     * @param volume  The volume component holding the fields.
     * @param model   The model component, whose material receives the textures.
     * @param sizePc  Lateral span of the galaxy, in parsecs.
     */
    private static void scheduleGeneration(Entity entity,
                                           Volume volume,
                                           ModelComponent model,
                                           double sizePc) {
        scheduleGeneration(name(entity), volume, model, sizePc);
    }

    /**
     * Same as {@link #scheduleGeneration(Entity, Volume, ModelComponent, double)}, with the entity's
     * name resolved by the caller: the deferred paths outlive the entity and must not hold it.
     */
    private static void scheduleGeneration(String name,
                                           Volume volume,
                                           ModelComponent model,
                                           double sizePc) {
        final MaterialComponent mtc;
        if (model.mtc == null) {
            // A procedural volume cannot render without a material: the generated textures have
            // nowhere to be bound, the shader never gets volumeFlag, and the entity draws nothing at
            // all - which looks like the galaxy vanished, with no error, since the scene loaded
            // fine. A scene whose material block only ever held volumeDepthThreshold has no reason
            // to keep it now that the threshold is derived from the fields, so create the material
            // here rather than drop the volume.
            mtc = new MaterialComponent();
            model.setMaterial(mtc);
            logger.info("The volume entity " + name + " declares no material; using a default one so that "
                                + "its procedural volume can be generated and bound.");
        } else {
            mtc = model.mtc;
        }
        if (!GalaxyVolumeGenerator.isSupported()) {
            if (!generationUnavailableLogged) {
                generationUnavailableLogged = true;
                logger.warn("Cannot generate procedural volumes: compute shaders are not available. "
                                    + "The volumes fall back to their shaders' own defaults.");
            }
            return;
        }
        if (sizePc <= 0.0) {
            logger.warn("Cannot generate the volumes of " + name + ": its size is " + sizePc + " pc");
            return;
        }

        Runnable generation = () -> {
            // The entity may be removed before the material arrives; binding a texture then would
            // leak it, since disposal already ran.
            if (volume.isDisposed()) {
                return;
            }
            generateVolumes(name, volume, mtc, sizePc);
        };

        if (mtc.getMaterial() != null) {
            generation.run();
        } else {
            mtc.whenMaterialInitialized(generation);
        }
    }

    private static String name(Entity entity) {
        var base = Mapper.base.get(entity);
        return base != null && base.getName() != null ? base.getName() : String.valueOf(entity);
    }

    /**
     * Voxelizes the fields of a procedural volume into its two slots and binds the result to the
     * material. The fields are the galaxy's own definition: no billboard set is read, written or
     * emptied to produce a volume.
     * <p>
     * Runs on the GL thread. It is triggered at set-up and from
     * {@link #generateNow(Entity)}; the textures are released with the component when the entity is
     * removed. Each slot refills the texture it already holds when the grid has not changed, so a
     * regeneration keeps the material's attributes and the shader variant, and only a new grid - or
     * a slot that went from empty to populated - costs a new texture.
     *
     * @param name   Name of the entity, for logging.
     * @param volume The volume component holding the fields.
     * @param mtc    The material component, which receives the textures.
     * @param sizePc Lateral span of the galaxy, in parsecs.
     */
    private static void generateVolumes(String name,
                                        Volume volume,
                                        MaterialComponent mtc,
                                        double sizePc) {
        var quality = GaiaSky.settings().graphics.quality;
        var generator = GalaxyVolumeGenerator.instance();
        long start = System.nanoTime();

        var emissiveGrid = gridFor(volume.emissiveFields(), sizePc, quality);
        updateSlot(generator, volume, mtc, Volume.SLOT_EMISSIVE, volume.emissiveFields(), emissiveGrid);

        // The slot's threshold comes from the fields' own occlusion fractions unless the scene pins
        // one. It has to be set before the texture is bound: the attribute that carries it is built
        // from volumeDepthThreshold(slot) inside setVolumeTexture.
        mtc.setVolumeDepthThresholdDerived(Volume.SLOT_OCCLUSIVE,
                                          VolumeCalibration.occlusiveThreshold(volume.occlusiveFields()));
        float occlusiveThreshold = mtc.volumeDepthThreshold(Volume.SLOT_OCCLUSIVE);
        var occlusiveGrid = gridFor(volume.occlusiveFields(), sizePc, quality);
        updateSlot(generator, volume, mtc, Volume.SLOT_OCCLUSIVE, volume.occlusiveFields(), occlusiveGrid);
        if (volume.getGeneratedVolume(Volume.SLOT_OCCLUSIVE) != null) {
            logOcclusivePeaks(name, volume.occlusiveFields(), generator.readStats(occlusiveGrid), occlusiveThreshold);
            if (occlusiveThreshold <= 0f) {
                // A generated volume has no descriptor of its own to inherit this from, and the
                // depth pre-pass drops every slot whose threshold is zero - so the lanes render but
                // hide nothing. It is a silent failure otherwise: the scene looks right.
                logger.warn("The occlusive volume of " + name + " is bound with no depth threshold: no dust field "
                                    + "asks to occlude (occlusionThreshold is 0 on all of them) and "
                                    + "material.volumeDepthThreshold[" + Volume.SLOT_OCCLUSIVE + "] says nothing, so its "
                                    + "dust will not occlude anything. Give the field that should hide the background "
                                    + "an occlusionThreshold in (0, 1), e.g. 0.1.");
            }
        }

        // The grid is the thing to look at when the result looks coarse: a lateral voxel of N pc is
        // N / (spanPc / spanOnScreenPx) pixels wide, so this line plus a screenshot answers whether
        // the blockiness is the grid or something else. The thresholds say whether the dust can
        // occlude at all - the depth pre-pass skips a slot whose threshold is zero.
        logger.info(String.format(Locale.ROOT,
                                  "Generated the volumes of %s in %.1f ms (%.0f pc span, %s): emissive %s (depth threshold %.4f), occlusive %s (depth threshold %.4f)",
                                  name,
                                  (System.nanoTime() - start) / 1.0e6,
                                  sizePc,
                                  quality,
                                  emissiveGrid == null ? "none" : emissiveGrid.toString(),
                                  mtc.volumeDepthThreshold(Volume.SLOT_EMISSIVE),
                                  occlusiveGrid == null ? "none" : occlusiveGrid.toString(),
                                  occlusiveThreshold));
    }

    /**
     * Brings one slot up to date with the fields routed to it: refills the texture it already holds
     * when the grid still fits, allocates one when it does not, and unbinds and releases it when the
     * slot has no fields any more.
     *
     * @param generator The volume generator.
     * @param volume    The volume component, which owns the generated textures.
     * @param mtc       The material that samples them.
     * @param slot      {@link Volume#SLOT_EMISSIVE} or {@link Volume#SLOT_OCCLUSIVE}.
     * @param fields    The fields routed to the slot.
     * @param grid      The grid for those fields, or null when the slot is empty.
     */
    private static void updateSlot(GalaxyVolumeGenerator generator,
                                   Volume volume,
                                   MaterialComponent mtc,
                                   int slot,
                                   List<GalaxyAttributes> fields,
                                   VoxelGrid grid) {
        VolumeTexture previous = volume.getGeneratedVolume(slot);
        if (fields.isEmpty() || grid == null) {
            if (previous != null) {
                // Unbind first: the texture may not be released while the material still samples it.
                mtc.clearVolumeTexture(slot);
                volume.setGeneratedVolume(slot, null);
            }
            return;
        }
        VolumeTexture texture = generator.generate(fields, grid, previous);
        if (texture == null) {
            // Already reported by the generator. Whatever the slot held stays bound: showing the
            // previous volume beats showing nothing.
            return;
        }
        if (!mtc.setVolumeTexture(slot, texture)) {
            // Nothing samples it, so it would leak. A reused texture is the component's own and is
            // left for the component to release.
            if (previous == null || texture.texture() != previous.texture()) {
                texture.texture().dispose();
            }
            return;
        }
        volume.setGeneratedVolume(slot, texture);
    }

    private static VoxelGrid gridFor(List<GalaxyAttributes> fields,
                                     double sizePc,
                                     gaiasky.util.Settings.GraphicsQuality quality) {
        return fields.isEmpty() ? null : GalaxyAttributes.volumeGrid(fields, sizePc, quality);
    }

    /**
     * Logs, per occlusive field, the peak of its unit density as the generator measured it, the
     * brightness that peak ends up stored at once scaled by the field's intensity, and the fraction
     * of it the field is allowed to occlude from. The slot's threshold is compared against the
     * stored density, so "stored" against "threshold" says whether a field can hide anything behind
     * it at all: above the threshold it occludes, below it only dims.
     */
    private static void logOcclusivePeaks(String name, List<GalaxyAttributes> fields, float[] stats, float threshold) {
        if (stats == null || fields == null || fields.isEmpty()) {
            return;
        }
        var sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            var field = fields.get(i);
            float stored = (float) Math.min(VolumeCalibration.OCCLUSIVE_TARGET_PEAK * field.intensity, 1.0);
            sb.append(String.format(Locale.ROOT,
                                    "%sfield %d (%s, %s): peak %.4f, stored %.4f, occludes above %.0f%% of it -> %s",
                                    i == 0 ? "" : "; ",
                                    i,
                                    field.type,
                                    field.distribution,
                                    stats[6 + i],
                                    stored,
                                    field.occlusionThreshold * 100f,
                                    stored > threshold ? "occludes" : "dims only"));
        }
        sb.append(String.format(Locale.ROOT, "; slot threshold %.4f", threshold));
        logger.info("Occlusive fields of " + name + ": " + sb);
    }

}
