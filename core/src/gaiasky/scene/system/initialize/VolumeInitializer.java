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
import gaiasky.render.gdx.graphics.VolumeDepthMask;
import gaiasky.render.gdx.graphics.VolumeTexture;
import gaiasky.render.gdx.graphics.VoxelGrid;
import gaiasky.render.gdx.shader.attribute.IntAttribute;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Volume;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.MaterialComponent;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;

import java.util.List;
import java.util.Locale;

/**
 * Initializes volume entities, such as those used for volumetric rendering.
 */
public class VolumeInitializer extends AbstractInitSystem {
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

        // Bind a 1x1 placeholder so the mask #define is present when the volume shader compiles.
        if (model.model.mtc != null) {
            var placeholder = VolumeDepthMask.placeholder();
            model.model.mtc.setVolumeDepthMask(placeholder, placeholder, 0f, 0f);
        }

        // Procedural volume: voxelise its own fields into the material's two slots.
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
     * Prepares the generation of a procedural volume's slots: it binds the results to the material
     * now if the material already exists, and otherwise hooks onto the material's initialisation.
     * <p>
     * The second case is the common one: with lazy texture initialisation on (the default), the
     * material of a volume is not built at set-up time but on the first frame the entity is drawn,
     * inside {@link ModelComponent#touch(com.badlogic.gdx.math.Matrix4)}. Generating at set-up would
     * then have nowhere to bind the textures, which is what used to make this a no-op.
     *
     * @param entity  The entity, for the log and to resolve its name.
     * @param volume  The volume component holding the fields.
     * @param model   The model component, whose material receives the textures.
     * @param sizePc  Lateral span of the galaxy, in parsecs.
     */
    private void scheduleGeneration(Entity entity,
                                    Volume volume,
                                    ModelComponent model,
                                    double sizePc) {
        var mtc = model.mtc;
        if (mtc == null) {
            logger.warn("Cannot generate the volumes of " + name(entity) + ": it has no material component.");
            return;
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
            logger.warn("Cannot generate the volumes of " + name(entity) + ": its size is " + sizePc + " pc");
            return;
        }

        // Captured as a string so the callback does not keep the entity alive.
        String name = name(entity);
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
     * Voxelises the fields of a procedural volume into its two slots and binds the result to the
     * material. The fields are the galaxy's own definition: no billboard set is read, written or
     * emptied to produce a volume.
     * <p>
     * Runs on the GL thread. Quality changes reload the scene in Gaia Sky, so this is the only place
     * generation is triggered, and the textures are released with the component when the entity is
     * removed.
     *
     * @param name   Name of the entity, for logging.
     * @param volume The volume component holding the fields.
     * @param mtc    The material component, which receives the textures.
     * @param sizePc Lateral span of the galaxy, in parsecs.
     */
    private void generateVolumes(String name,
                                 Volume volume,
                                 MaterialComponent mtc,
                                 double sizePc) {
        var quality = GaiaSky.settings().graphics.quality;
        var generator = GalaxyVolumeGenerator.instance();
        long start = System.nanoTime();

        var emissiveGrid = gridFor(volume.emissiveFields(), sizePc, quality);
        VolumeTexture emissive = generateSlot(generator, volume.emissiveFields(), emissiveGrid);
        if (emissive != null && mtc.setVolumeTexture(Volume.SLOT_EMISSIVE, emissive)) {
            volume.setGeneratedVolume(Volume.SLOT_EMISSIVE, emissive);
        } else if (emissive != null) {
            // Bound nowhere: release it now rather than leak it.
            emissive.texture().dispose();
        }

        var occlusiveGrid = gridFor(volume.occlusiveFields(), sizePc, quality);
        VolumeTexture occlusive = generateSlot(generator, volume.occlusiveFields(), occlusiveGrid);
        float occlusiveThreshold = mtc.volumeDepthThreshold(Volume.SLOT_OCCLUSIVE);
        if (occlusive != null && mtc.setVolumeTexture(Volume.SLOT_OCCLUSIVE, occlusive)) {
            volume.setGeneratedVolume(Volume.SLOT_OCCLUSIVE, occlusive);
            if (occlusiveThreshold <= 0f) {
                // A generated volume has no descriptor of its own to inherit this from, and the
                // depth pre-pass drops every slot whose threshold is zero - so the lanes render but
                // hide nothing. It is a silent failure otherwise: the scene looks right.
                logger.warn("The occlusive volume of " + name + " is bound with no depth threshold "
                                    + "(material.volumeDepthThreshold[" + Volume.SLOT_OCCLUSIVE + "] is 0), so its dust will "
                                    + "not occlude anything. Set it to a density in (0, 1], e.g. 0.005.");
            }
        } else if (occlusive != null) {
            occlusive.texture().dispose();
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

    private VoxelGrid gridFor(List<GalaxyAttributes> fields,
                              double sizePc,
                              gaiasky.util.Settings.GraphicsQuality quality) {
        return fields.isEmpty() ? null : GalaxyAttributes.volumeGrid(fields, sizePc, quality);
    }

    /**
     * Generates the texture of one slot, or returns null if the slot has no fields.
     *
     * @param generator The volume generator.
     * @param fields    The fields routed to this slot.
     * @param grid      The grid to fill, or null if the slot is empty.
     *
     * @return The generated texture, or null.
     */
    private VolumeTexture generateSlot(GalaxyVolumeGenerator generator,
                                       List<GalaxyAttributes> fields,
                                       VoxelGrid grid) {
        if (fields.isEmpty() || grid == null) {
            return null;
        }
        return generator.generate(fields, grid);
    }
}
