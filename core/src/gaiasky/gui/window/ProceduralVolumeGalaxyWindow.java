/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.JsonWriter;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.gdx.graphics.VolumeCalibration;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Volume;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyAttributes.ChannelType;
import gaiasky.scene.record.GalaxyAttributes.Distribution;
import gaiasky.scene.record.GalaxyAttributes.HeightProfile;
import gaiasky.scene.record.GalaxyGenerator.GalaxyMorphology;
import gaiasky.scene.system.initialize.VolumeInitializer;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Interface to the procedural generation of hybrid galaxies: the same morphologies as
 * {@link ProceduralBillboardGalaxyWindow}, but with the diffuse channels (gas, bulge, dust) drawn
 * as an analytic density field through the volume raymarcher instead of as sprites, and only the
 * compact ones (stars, HII regions, points) left as billboards.
 * <p>
 * Both halves come from one seeded draw of {@link gaiasky.scene.record.GalaxyGenerator}, so the
 * volume is always the one the billboards describe. The window edits the billboard datasets and the
 * volume fields of that single draw: the billboard section below is the same pane the billboard
 * window builds, and the volume section lists the fields each slot will sum.
 * <p>
 * The <em>generate billboards only</em> toggle switches the window back to the legacy pipeline, for
 * the times when the billboard-only result is what is wanted. It changes the structure of the
 * object - a billboard galaxy has a full-resolution and a half-resolution billboard entity, a hybrid
 * one has a billboard entity and a child volume - so the change only takes effect on Generate.
 */
public class ProceduralVolumeGalaxyWindow extends AbstractGalaxyGenWindow {
    /** Seed of the galaxies this window creates on its own, same as the billboard window's. **/
    private static final long DEFAULT_SEED = 123L;

    /** The full-resolution billboard entity: stars, HII regions and points. **/
    private Entity entityFull;
    /**
     * The diffuse half: a child {@link Volume} entity in hybrid mode, the half-resolution billboard
     * entity when generating billboards only.
     */
    private Entity entityDiffuse;
    private final FocusView viewFull = new FocusView();
    /** Whether the window generates a billboard-only galaxy, as the legacy window does. **/
    private boolean billboardsOnly;
    /** Set when the toggle no longer matches the structure the current entities have. **/
    private boolean structureDirty;
    /** Set when a volume field was edited and has not been voxelised again yet. **/
    private boolean fieldsDirty;
    /** The "press Generate" line of the volume section, kept to flip it without a rebuild. **/
    private OwnLabel editedHint;

    /** Formats the readouts and the slider labels: enough digits to be useful, few enough to read. **/
    private static final DecimalFormat DECIMAL = new DecimalFormat("#0.###");

    public ProceduralVolumeGalaxyWindow(FocusView target, Scene scene, Stage stage, Skin skin) {
        super(scene, stage, skin);
        // Entities before UI: the scaffold reads them while building.
        init(target, null);
        buildSuper();
    }

    public ProceduralVolumeGalaxyWindow(String newName, GalaxyMorphology morphology, Scene scene, Stage stage, Skin skin) {
        super(scene, stage, skin);
        this.morphology = morphology;
        init(null, newName);
        buildSuper();
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle.
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void init(FocusView target, String name) {
        if (target == null) {
            var radius = 10 * Constants.KPC_TO_U;
            var camera = GaiaSky.instance.getICamera();
            var pos = new Vector3Q(camera.getPos());
            var dir = new Vector3D(camera.getDirection());
            pos.add(dir.nor().scl(radius * 2.0));

            var pair = createPair(name, radius, pos, morphology, DEFAULT_SEED, billboardsOnly);
            entityFull = pair.getFirst();
            entityDiffuse = pair.getSecond();
            GaiaSky.postRunnable(() -> register(pair));
        } else {
            var entity = target.getEntity();
            // The diffuse child is not focusable, but a script may still hand it over.
            if (Mapper.volume.has(entity) && Mapper.graph.has(entity) && Mapper.graph.get(entity).parent != null) {
                entity = Mapper.graph.get(entity).parent;
            }
            entityFull = entity;
            entityDiffuse = findDiffuseChild(entityFull);
            billboardsOnly = !isHybrid();
            var bb = Mapper.billboardSet.get(entityFull);
            if (bb != null && bb.morphology != null) {
                morphology = bb.morphology;
            }
        }
        viewFull.setEntity(entityFull);
        structureDirty = false;
        fieldsDirty = false;
    }

    /**
     * @param full The full-resolution billboard entity.
     *
     * @return Its diffuse child, whichever of the two it is, or null if it has none.
     */
    private Entity findDiffuseChild(Entity full) {
        if (full == null || !Mapper.graph.has(full)) {
            return null;
        }
        var graph = Mapper.graph.get(full);
        var volume = graph.getFirstChildOfType(scene.archetypes().get("Volume"));
        return volume != null ? volume : graph.getFirstChildOfType(scene.archetypes().get("BillboardGroup"));
    }

    /** @return Whether the diffuse half is a volume, as opposed to a billboard entity. **/
    private boolean isHybrid() {
        return entityDiffuse != null && Mapper.volume.has(entityDiffuse);
    }

    private Pair<Entity, Entity> createPair(String name,
                                            double radius,
                                            Vector3Q pos,
                                            GalaxyMorphology morphology,
                                            long seed,
                                            boolean billboardsOnly) {
        var api = GaiaSky.instance.scripting().apiv2().scene;
        return billboardsOnly
                ? api.createNewProceduralGalaxy(name, radius, pos, morphology, seed)
                : api.createNewProceduralVolumeGalaxy(name, radius, pos, morphology, seed);
    }

    /** Adds a freshly created pair to the scene. Must run on the render thread. **/
    private void register(Pair<Entity, Entity> pair) {
        var full = pair.getFirst();
        var diffuse = pair.getSecond();
        scene.initializeEntity(full);
        scene.initializeEntity(diffuse);
        scene.setUpEntity(full);
        scene.setUpEntity(diffuse);
        EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, full, true);
        EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, diffuse, true);
    }

    // ---------------------------------------------------------------------------------------------
    // Hooks.
    // ---------------------------------------------------------------------------------------------

    @Override
    protected boolean isEditableFocus(FocusView view) {
        return view.getEntity() != null && (view.isBillboardDataset() || Mapper.volume.has(view.getEntity()));
    }

    @Override
    protected String getHeaderText() {
        return I18n.msg("gui.galaxy.galaxy", viewFull.getLocalizedName());
    }

    @Override
    protected String getWindowTitleText() {
        return I18n.msg("gui.galaxy.title", viewFull.getLocalizedName());
    }

    @Override
    protected List<Entity> getSizedEntities() {
        // Arrays.asList, not List.of: either half may be missing while the structure is being changed.
        return Arrays.asList(entityFull, entityDiffuse);
    }

    @Override
    protected Entity getTransformSource() {
        return entityFull;
    }

    @Override
    protected long getSeed() {
        var bb = Mapper.billboardSet.get(entityFull);
        return bb == null ? DEFAULT_SEED : bb.seed;
    }

    @Override
    protected void setSeed(long seed) {
        var bb = Mapper.billboardSet.get(entityFull);
        if (bb != null) {
            bb.seed = seed;
        }
    }

    @Override
    protected void onMorphologyChanged(GalaxyMorphology morphology) {
        var bb = Mapper.billboardSet.get(entityFull);
        if (bb != null) {
            bb.morphology = morphology;
        }
    }

    /**
     * The diffuse channels are never drawn as sprites in this window, so a billboard dataset may not
     * take one of their types: setting it would leave the dataset without a density model.
     */
    @Override
    protected ChannelType[] getBillboardChannelTypes() {
        return new ChannelType[]{ChannelType.STAR, ChannelType.HII, ChannelType.POINT};
    }

    @Override
    protected Object getPersistenceKey() {
        return entityFull;
    }

    // ---------------------------------------------------------------------------------------------
    // Channel list.
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void buildChannelSections(Table table) {
        buildGenerationMode(table);

        var billboardsHeader = new OwnLabel(I18n.msg("gui.galaxy.section.billboards"), skin, "header");
        tip(billboardsHeader, "gui.galaxy.section.billboards");
        table.add(billboardsHeader).left().padBottom(pad10).row();
        addBillboardDatasets(entityFull, table, THIRD_WIDTH_BOX, HALF_WIDTH_BOX, FULL_WIDTH_BOX, TAB_CONTENT_WIDTH);

        table.add(new Separator(skin, "gray")).fillX().expandX().padTop(pad20).padBottom(pad20).row();

        if (isHybrid()) {
            buildVolumeSections(table);
        } else if (entityDiffuse != null) {
            var halfHeader = new OwnLabel(I18n.msg("gui.galaxy.section.half"), skin, "header");
            tip(halfHeader, "gui.galaxy.section.half");
            table.add(halfHeader).left().padBottom(pad10).row();
            addBillboardDatasets(entityDiffuse, table, THIRD_WIDTH_BOX, HALF_WIDTH_BOX, FULL_WIDTH_BOX, TAB_CONTENT_WIDTH);
        }
    }

    private void buildGenerationMode(Table table) {
        var toggle = new OwnCheckBox(I18n.msg("gui.galaxy.billboards.only"), skin, 4f);
        toggle.setChecked(billboardsOnly);
        tip(toggle, "gui.galaxy.billboards.only");
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                if (billboardsOnly != toggle.isChecked()) {
                    billboardsOnly = toggle.isChecked();
                    // A different structure cannot be applied in place: Generate rebuilds the object.
                    structureDirty = true;
                    rebuild();
                }
            }
        });
        table.add(toggle).left().padBottom(pad10).row();
        if (structureDirty) {
            table.add(new OwnLabel(I18n.msg("gui.galaxy.billboards.only.pending"), skin, "grey-normal"))
                    .left().padBottom(pad10).row();
        }
        table.add().padBottom(pad20).row();
    }

    /**
     * The volume half of the window.
     * <p>
     * One collapsible pane per field, holding the parameters {@code galaxyDensity()} reads from it.
     * The panes are the same editors the scene JSON drives, so what is on screen is exactly what a
     * saved descriptor would carry: there is no hidden state between the window and the volume.
     * <p>
     * An edit only touches the field object; it does not voxelise. Re-voxelisation is a compute
     * dispatch per slot, so it is deferred to Generate, and the section says so until it runs.
     *
     * @param table The table to fill.
     */
    private void buildVolumeSections(Table table) {
        var volume = Mapper.volume.get(entityDiffuse);
        editedHint = null;
        var volumeHeader = new OwnLabel(I18n.msg("gui.galaxy.section.volume"), skin, "header");
        tip(volumeHeader, "gui.galaxy.section.volume");
        table.add(volumeHeader).left().padBottom(pad10).row();
        if (volume == null) {
            return;
        }
        var slotCounter = new OwnLabel(I18n.msg("gui.galaxy.volume.slots",
                                                volume.emissiveFields().size(),
                                                GalaxyAttributes.MAX_FIELDS,
                                                volume.occlusiveFields().size(),
                                                GalaxyAttributes.MAX_FIELDS), skin);
        tip(slotCounter, "gui.galaxy.volume.slots");
        table.add(slotCounter).left().padBottom(pad10).row();

        // The threshold the next generation will write into the material: it is derived from the
        // fields, so it follows the occlusion sliders below live, without a dispatch.
        var thresholdLabel = new OwnLabel(I18n.msg("gui.galaxy.volume.threshold",
                                                   DECIMAL.format(VolumeCalibration.occlusiveThreshold(volume.occlusiveFields()))),
                                          skin, "grey-normal");
        tip(thresholdLabel, "gui.galaxy.volume.threshold");
        table.add(thresholdLabel).left().padBottom(pad18).row();

        var fields = volume.fields;
        if (fields == null || fields.length == 0) {
            table.add(new OwnLabel(I18n.msg("gui.galaxy.volume.empty"), skin, "grey-normal"))
                    .left().padBottom(pad18).row();
            return;
        }

        // Empty when clean rather than hidden: a hidden label still takes its cell.
        editedHint = new OwnLabel(fieldsDirty ? I18n.msg("gui.galaxy.volume.edited") : "", skin, "grey-normal");
        tip(editedHint, "gui.galaxy.volume.edited");
        table.add(editedHint).left().padBottom(pad18).row();

        for (var field : fields) {
            var pane = new CollapsiblePane(stage,
                                           field.type.localizedName() + " - " + field.distribution.localizedName(),
                                           buildFieldDetail(field, volume),
                                           TAB_CONTENT_WIDTH,
                                           skin,
                                           "header-s",
                                           "expand-collapse",
                                           null,
                                           isExpanded(field),
                                           null,
                                           null);
            pane.setExpandCollapseRunnable(() -> setExpanded(field, pane.isExpanded()));
            table.add(pane).padBottom(pad18).row();
        }
    }

    /**
     * Builds the editors of one field.
     *
     * @param field  The field to edit.
     * @param volume The volume the field belongs to, used by the delete button.
     *
     * @return A table the collapsible pane can wrap.
     */
    private Table buildFieldDetail(GalaxyAttributes field,
                                   Volume volume) {
        var detail = new Table(skin);

        // Readout. The stored peak and the occluding fraction are the two numbers the raymarcher
        // compares, so they are shown next to the editors that move them.
        var slot = field.isOcclusive() ? I18n.msg("gui.galaxy.volume.slot.occlusive")
                                       : I18n.msg("gui.galaxy.volume.slot.emissive");
        var summary = new OwnLabel(I18n.msg("gui.galaxy.volume.field.summary",
                                            slot,
                                            DECIMAL.format(field.baseRadius),
                                            DECIMAL.format(field.intensity)), skin, "grey-normal");
        tip(summary, "gui.galaxy.volume.field.summary");
        detail.add(summary).colspan(2).left().padBottom(pad10).row();

        if (field.isOcclusive()) {
            float storedPeak = (float) Math.min(VolumeCalibration.OCCLUSIVE_TARGET_PEAK * Math.max(0.0, field.intensity), 1.0);
            var occlusion = new OwnLabel(I18n.msg("gui.galaxy.volume.field.occlusion",
                                                  DECIMAL.format(storedPeak),
                                                  Math.round(Math.max(0f, field.occlusionThreshold) * 100f)),
                                         skin, "grey-normal");
            tip(occlusion, "gui.galaxy.volume.field.occlusion");
            detail.add(occlusion).colspan(2).left().padBottom(pad18).row();
        }

        // Distribution
        var distribution = new OwnSelectBox<ComboBoxBean<Distribution>>(skin);
        distribution.setItems(ComboBoxBean.getValues(Distribution.class));
        distribution.setWidth(HALF_WIDTH_BOX);
        distribution.setSelectedIndex(field.distribution.ordinal());
        distribution.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                field.distribution = distribution.getSelected().value;
                markFieldsDirty();
                // The pane title names the distribution.
                rebuild();
            }
        });
        tip(distribution, "gui.galaxy.ds.distribution");
        var distributionLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.distribution"), skin);
        distributionLabel.setWidth(HALF_WIDTH_BOX);
        detail.add(distributionLabel).left().padRight(pad20).padBottom(pad18);
        detail.add(distribution).left().padBottom(pad18).row();

        // Height profile
        var heightProfile = new OwnSelectBox<HeightProfile>(skin);
        heightProfile.setItems(HeightProfile.values());
        heightProfile.setWidth(HALF_WIDTH_BOX);
        heightProfile.setSelected(field.heightProfile);
        heightProfile.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                field.heightProfile = heightProfile.getSelected();
                markFieldsDirty();
            }
        });
        tip(heightProfile, "gui.galaxy.ds.height.profile");
        var heightProfileLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.height.profile"), skin);
        heightProfileLabel.setWidth(HALF_WIDTH_BOX);
        detail.add(heightProfileLabel).left().padRight(pad20).padBottom(pad18);
        detail.add(heightProfile).left().padBottom(pad18).row();

        // Radial profile
        addSlider(detail, fieldSlider("gui.galaxy.ds.radius.base", 0.1f, 2.0f, 0.01f, field.baseRadius,
                                      v -> field.baseRadius = v));
        addSlider(detail, fieldSlider("gui.galaxy.ds.radius.min", 0f, 0.95f, 0.01f, field.minRadius,
                                      v -> field.minRadius = v));

        // Arms
        var arms = fieldSlider("gui.galaxy.ds.arm.number", 0f, 8f, 1f, field.numArms,
                               v -> field.numArms = Math.round(v));
        arms.setNumberFormatter(new DecimalFormat("##0"));
        addSlider(detail, arms);
        addSlider(detail, fieldSlider("gui.galaxy.ds.arm.sigma", 0.05f, 1.5f, 0.01f, field.armSigma,
                                      v -> field.armSigma = v));
        addSlider(detail, fieldSlider("gui.galaxy.ds.angle", -180f, 180f, 0.5f, field.baseAngle,
                                      v -> field.baseAngle = v));

        // Ellipsoid: eccentricity per axis, then the bar aspect.
        var eccTable = new Table(skin);
        var eccX = fieldSlider("gui.galaxy.ds.eccentricity.x", -0.95f, 0.95f, 0.01f, field.eccentricity[0],
                               v -> field.eccentricity[0] = v);
        eccX.setWidth(THIRD_WIDTH_BOX);
        var eccY = fieldSlider("gui.galaxy.ds.eccentricity.y", -0.95f, 0.95f, 0.01f, field.eccentricity[1],
                               v -> field.eccentricity[1] = v);
        eccY.setWidth(THIRD_WIDTH_BOX);
        var aspect = fieldSlider("gui.galaxy.ds.aspect", 0.1f, 3.0f, 0.01f, field.aspect,
                                 v -> field.aspect = v);
        aspect.setWidth(THIRD_WIDTH_BOX);
        eccTable.add(eccX).left().padRight(PAD5);
        eccTable.add(eccY).left().padRight(PAD5);
        eccTable.add(aspect).left();
        detail.add(eccTable).colspan(2).left().padBottom(pad18).row();

        // Vertical structure
        addSlider(detail, fieldSlider("gui.galaxy.ds.warp", 0f, 1.0f, 0.01f, field.warpStrength,
                                      v -> field.warpStrength = v));
        addSlider(detail, fieldSlider("gui.galaxy.ds.height.scale", 0f, 0.5f, 0.005f, field.heightScale,
                                      v -> field.heightScale = v));

        // Colors and color noise
        var colorsTable = new Table(skin);
        var colorLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.color"), skin);
        tip(colorLabel, "gui.galaxy.ds.color");
        colorsTable.add(colorLabel).left().padRight(pad34);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            var picker = new ColorPicker("c" + idx, field.getColorRGBA(idx), stage, skin);
            tip(picker, "gui.galaxy.ds.color");
            picker.setNewColorRunnable(() -> {
                field.setColorRGBA(picker.color(), idx);
                markFieldsDirty();
            });
            colorsTable.add(picker).size(32f).left().padRight(pad10);
        }
        detail.add(colorsTable).colspan(2).left().padBottom(pad18).row();
        addSlider(detail, fieldSlider("gui.galaxy.ds.color.noise", 0f, 1f, 0.01f, field.colorNoise,
                                      v -> field.colorNoise = v));

        // Intensity
        addSlider(detail, fieldSlider("gui.galaxy.ds.intensity", 0f, 5f, 0.01f, field.intensity,
                                      v -> field.intensity = v));

        // Volume-only knobs: the sprite path has no equivalent for any of these.
        addSlider(detail, fieldSlider("gui.galaxy.field.lane.warp", -0.5f, 0.5f, 0.01f, field.laneWarp,
                                      v -> field.laneWarp = v));
        addSlider(detail, fieldSlider("gui.galaxy.field.lane.rough", 0f, 3f, 0.01f, field.laneRough,
                                      v -> field.laneRough = v));
        addSlider(detail, fieldSlider("gui.galaxy.field.lane.noise.scale", 0.1f, 10f, 0.1f, field.laneNoiseScale,
                                      v -> field.laneNoiseScale = v));
        addSlider(detail, fieldSlider("gui.galaxy.field.noise.mask", 0f, 1f, 0.01f, field.noiseMask,
                                      v -> field.noiseMask = v));
        addSlider(detail, fieldSlider("gui.galaxy.field.noise.mask.freq", 0f, 40f, 0.5f, field.noiseMaskFreq,
                                      v -> field.noiseMaskFreq = v));
        var octaves = fieldSlider("gui.galaxy.field.noise.mask.octaves", 0f, 8f, 1f, field.noiseMaskOctaves,
                                  v -> field.noiseMaskOctaves = Math.round(v));
        octaves.setNumberFormatter(new DecimalFormat("##0"));
        addSlider(detail, octaves);

        // Occlusion: only dust writes depth, so only dust gets the knob.
        if (field.isOcclusive()) {
            addSlider(detail, fieldSlider("gui.galaxy.field.occlusion", 0f, 1f, 0.01f, field.occlusionThreshold,
                                          v -> field.occlusionThreshold = v));
        }

        // Delete
        var delete = new OwnTextIconButton("", skin, "delete");
        tip(delete, "gui.galaxy.delete");
        delete.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                removeField(volume, field);
            }
        });
        detail.add(delete).colspan(2).left().padBottom(pad10).row();
        return detail;
    }

    /** Writes a float into a field member, so that the slider builder stays generic. **/
    @FunctionalInterface
    private interface FieldConsumer {
        void accept(float value);
    }

    /**
     * @param key    The i18n key of the slider label and of its tooltip.
     * @param min    Minimum.
     * @param max    Maximum.
     * @param step   Step.
     * @param value  Current value.
     * @param setter The field member to write to.
     *
     * @return The slider, configured and listening. It is not added to any table: some of them share
     *         a row.
     */
    private OwnSliderPlus fieldSlider(String key,
                                      float min,
                                      float max,
                                      float step,
                                      float value,
                                      FieldConsumer setter) {
        var slider = new OwnSliderPlus(I18n.msg(key), min, max, step, skin);
        tip(slider, key);
        slider.setWidth(FULL_WIDTH_BOX);
        // Before the listener: setting the value fires it, and the field is already at that value.
        slider.setValue(value);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                setter.accept(slider.getValue());
                markFieldsDirty();
            }
        });
        return slider;
    }

    /** Adds a slider on its own row, spanning both columns of the pane. **/
    private void addSlider(Table table,
                           OwnSliderPlus slider) {
        table.add(slider).colspan(2).left().padBottom(pad18).row();
    }

    /**
     * Drops a field out of the volume. The next generation re-voxelises without it, and a slot left
     * with no fields is unbound and released by {@link VolumeInitializer}.
     *
     * @param volume The volume the field belongs to.
     * @param field  The field to remove.
     */
    private void removeField(Volume volume,
                             GalaxyAttributes field) {
        var remaining = new ArrayList<>(Arrays.asList(volume.fields));
        if (!remaining.remove(field)) {
            return;
        }
        // An empty array, not null: null means "loaded from a file", and a procedural volume that
        // lost all its fields still has to be able to unbind its slots.
        volume.fields = remaining.toArray(new GalaxyAttributes[0]);
        setExpanded(field, false);
        markFieldsDirty();
        // The panes are built from the array, so the list has to be rebuilt.
        rebuild();
    }

    /** Flags the volume fields as edited and says so, without rebuilding the pane being dragged. **/
    private void markFieldsDirty() {
        if (!fieldsDirty) {
            fieldsDirty = true;
            if (editedHint != null) {
                editedHint.setText(I18n.msg("gui.galaxy.volume.edited"));
            }
        }
    }

    @Override
    protected void buildChannelButtons(Table table) {
        table.add(newAddChannelButton(entityFull, "gui.galaxy.add.full", BUTTON_WIDTH))
                .center().padRight(pad34).padBottom(pad10);
        if (!isHybrid()) {
            table.add(newAddChannelButton(entityDiffuse, "gui.galaxy.add.half", BUTTON_WIDTH))
                    .center().padBottom(pad10);
        }
        table.row();
    }

    // ---------------------------------------------------------------------------------------------
    // Actions.
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onGenerate() {
        if (structureDirty) {
            // The toggle asks for a different structure: the entities have to be replaced.
            recreate();
        } else if (fieldsDirty && !morphologyChanged) {
            // The fields are the object of this window, so an edited field wins over a new draw:
            // re-voxelise what the panes hold instead of overwriting it with the seed's version.
            reapplyFields();
        } else {
            generateInPlace();
        }
    }

    /**
     * Voxelises the fields as the panes left them, without touching the billboard datasets or the
     * seed. This is the cheap path an edited field takes: two compute dispatches, and the slot
     * textures are refilled rather than reallocated.
     */
    private void reapplyFields() {
        if (!isHybrid()) {
            // No volume fields in a billboards-only galaxy, so there is nothing to re-apply.
            generateInPlace();
            return;
        }
        fieldsDirty = false;
        VolumeInitializer.generateNow(entityDiffuse);
        rebuild();
    }

    /**
     * Replaces the datasets and the fields of the current entities with a fresh draw. Nothing about
     * the entities themselves changes, so this is the cheap path: no entity is created or removed.
     */
    private void generateInPlace() {
        if (entityFull == null) {
            return;
        }
        long seed = getSeed();
        var gm = morphology;
        var spec = gen.generateGalaxySpec(gm, seed);
        boolean hybrid = isHybrid();
        // A fresh draw replaces the fields wholesale, edits included.
        fieldsDirty = false;
        morphologyChanged = false;

        regenerate();
        GaiaSky.postRunnable(() -> {
            var bb = Mapper.billboardSet.get(entityFull);
            if (bb != null) {
                bb.morphology = gm;
                bb.seed = seed;
                bb.datasets = spec.full;
            }
            if (hybrid) {
                var volume = Mapper.volume.get(entityDiffuse);
                volume.fields = spec.volumeFields;
                VolumeInitializer.generateNow(entityDiffuse);
            } else {
                var half = Mapper.billboardSet.get(entityDiffuse);
                if (half != null) {
                    half.datasets = spec.half;
                }
            }
            rebuild();
        });
    }

    /**
     * Rebuilds the galaxy with the structure the toggle asks for: a hybrid one (billboard entity +
     * child volume) or a billboard-only one (full-resolution + half-resolution billboard entities).
     * The name, the size and the position carry over; the draw is taken from the current seed.
     */
    private void recreate() {
        if (entityFull == null) {
            return;
        }
        var old = entityFull;
        var body = bodyOf(old);
        var base = Mapper.base.get(old);
        var name = base != null ? base.getName() : "galaxy";
        double radius = body != null ? body.size : 10 * Constants.KPC_TO_U;
        var pos = new Vector3Q(body != null ? body.pos : new Vector3Q());
        long seed = getSeed();
        var gm = morphology;
        boolean billboardsOnly = this.billboardsOnly;

        var pair = createPair(name, radius, pos, gm, seed, billboardsOnly);
        GaiaSky.postRunnable(() -> {
            // Directly, not through the event: the old object has to be gone before the new one is
            // inserted under the same name.
            scene.remove(old, true);
            register(pair);
        });

        entityFull = pair.getFirst();
        entityDiffuse = pair.getSecond();
        viewFull.setEntity(entityFull);
        structureDirty = false;
        fieldsDirty = false;
        rebuild();
    }

    @Override
    protected void regenerate() {
        if (entityFull != null && Mapper.render.has(entityFull)) {
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityFull));
        }
        if (entityDiffuse != null && !isHybrid() && Mapper.render.has(entityDiffuse)) {
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityDiffuse));
        }
    }

    @Override
    protected void onExport() {
        if (isHybrid()) {
            // The JSON writer still has no fields block to emit, so an export here would write a
            // galaxy that loads back as billboards only.
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.galaxy.export.volume.unsupported"));
            return;
        }
        var json = gen.convertToJson(entityFull, entityDiffuse);
        var string = json.toJson(JsonWriter.OutputType.json);
        Gdx.app.getClipboard().setContents(string);
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.galaxy.export.done"));
    }

    @Override
    protected void onRandomize() {
        // The base has already written the new seed through setSeed(). A new draw replaces every
        // field, so whatever was edited in the panes is superseded - the button says "another
        // galaxy", not "the same one with these edits".
        fieldsDirty = false;
        onGenerate();
    }
}
