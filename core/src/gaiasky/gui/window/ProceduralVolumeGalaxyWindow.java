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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
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
import gaiasky.scene.record.GalaxyGenerator;
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
 * A galaxy keeps the structure it was created with: this window only ever produces and edits the
 * hybrid one (a billboard entity plus a child volume). A billboard-only galaxy - a full-resolution
 * and a half-resolution billboard entity - belongs to {@link ProceduralBillboardGalaxyWindow}, and
 * {@code MainGui} routes by that structure. Converting one into the other would mean replacing the
 * object under the user's cursor, so it is not offered.
 */
public class ProceduralVolumeGalaxyWindow extends AbstractGalaxyGenWindow {
    /** Seed of the galaxies this window creates on its own, same as the billboard window's. **/
    private static final long DEFAULT_SEED = 123L;

    /** The full-resolution billboard entity: stars, HII regions and points. **/
    private Entity entityFull;
    /** The diffuse half: the child {@link Volume} entity that holds the density fields. **/
    private Entity entityDiffuse;
    private final FocusView viewFull = new FocusView();
    /** Set when a volume field was edited and has not been voxelised again yet. **/
    private boolean fieldsDirty;
    /**
     * Set when the seed field changed since the last draw. A new seed is a new galaxy, so Generate
     * draws from it; without this, every Generate would be a new draw and would discard the edits
     * made to the datasets and the fields.
     */
    private boolean seedChanged;
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

            var pair = createPair(name, radius, pos, morphology, DEFAULT_SEED);
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
            entityDiffuse = findVolumeChild(entityFull);
            var bb = Mapper.billboardSet.get(entityFull);
            if (bb != null && bb.morphology != null) {
                morphology = bb.morphology;
            }
        }
        viewFull.setEntity(entityFull);
        fieldsDirty = false;
        seedChanged = false;
    }

    /**
     * @param full The full-resolution billboard entity.
     *
     * @return Its volume child, or null if it has none.
     */
    private Entity findVolumeChild(Entity full) {
        if (full == null || !Mapper.graph.has(full)) {
            return null;
        }
        return Mapper.graph.get(full).getFirstChildOfType(scene.archetypes().get("Volume"));
    }

    /** @return Whether the diffuse half is a volume, as opposed to a billboard entity. **/
    private boolean isHybrid() {
        return entityDiffuse != null && Mapper.volume.has(entityDiffuse);
    }

    private Pair<Entity, Entity> createPair(String name,
                                            double radius,
                                            Vector3Q pos,
                                            GalaxyMorphology morphology,
                                            long seed) {
        var api = GaiaSky.instance.scripting().apiv2().scene;
        return api.createNewProceduralVolumeGalaxy(name, radius, pos, morphology, seed);
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
        // Either a volume entity handed over by a script or the billboard entity of a hybrid
        // galaxy. A billboard-only galaxy belongs to the billboard window, and accepting it here
        // would make both windows follow the same focus.
        var entity = view.getEntity();
        return entity != null && (Mapper.volume.has(entity) || (view.isBillboardDataset() && hasVolumeChild(entity)));
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
        // Arrays.asList, not List.of: a galaxy whose volume child is missing has a null second half.
        return Arrays.asList(entityFull, entityDiffuse);
    }

    @Override
    protected Entity getTransformSource() {
        return entityFull;
    }

    /**
     * The size the window shows and edits is the galaxy's radius, which is the billboard half's body
     * size. The volume's is the diameter of its box, twice that, so taking the largest of the two -
     * what the base does - would report the volume's diameter as the radius and compound on every
     * edit of the slider.
     */
    @Override
    protected double getSize() {
        var body = bodyOf(entityFull);
        return body != null ? body.size : super.getSize();
    }

    /** Scales the volume's diameter with the radius the billboard half follows. **/
    @Override
    protected void applySize(double internalSize) {
        for (var entity : getSizedEntities()) {
            var body = bodyOf(entity);
            if (body != null) {
                body.setSize(entity == entityDiffuse ? internalSize * 2.0 : internalSize);
                // The billboard half follows its body size on its own, every frame. The volume does
                // not: its shader scales the box by the material's body size, so the material has to
                // be told, or the volume keeps the diameter it was voxelised at.
                var model = Mapper.model.get(entity);
                if (model != null && model.model != null) {
                    model.model.updateSize(body.size);
                }
            }
        }
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
            // Only a Generate consumes it: the seed is a description of the galaxy to draw, not
            // something to apply to the one on screen.
            seedChanged = true;
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
        var billboardsHeader = new OwnLabel(I18n.msg("gui.galaxy.section.billboards"), skin, "header");
        tip(billboardsHeader, "gui.galaxy.section.billboards");
        table.add(billboardsHeader).left().padLeft(pad10).padBottom(pad10).row();
        addBillboardDatasets(entityFull, table, THIRD_WIDTH_BOX, HALF_WIDTH_BOX, FULL_WIDTH_BOX, TAB_CONTENT_WIDTH);

        if (isHybrid()) {
            table.add(new Separator(skin, "gray")).fillX().expandX().padTop(pad20).padBottom(pad20).row();
            buildVolumeSections(table);
        }
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
        table.add(volumeHeader).left().padLeft(pad10).padBottom(pad10).row();
        if (volume == null) {
            return;
        }
        var slotCounter = new OwnLabel(I18n.msg("gui.galaxy.volume.slots",
                                                volume.emissiveFields().size(),
                                                GalaxyAttributes.MAX_FIELDS,
                                                volume.occlusiveFields().size(),
                                                GalaxyAttributes.MAX_FIELDS), skin);
        tip(slotCounter, "gui.galaxy.volume.slots");
        table.add(slotCounter).left().padLeft(pad10).padBottom(pad10).row();

        // The threshold the next generation will write into the material: it is derived from the
        // fields, so it follows the occlusion sliders below live, without a dispatch.
        var thresholdLabel = new OwnLabel(I18n.msg("gui.galaxy.volume.threshold",
                                                   DECIMAL.format(VolumeCalibration.occlusiveThreshold(volume.occlusiveFields()))),
                                          skin, "grey-normal");
        tip(thresholdLabel, "gui.galaxy.volume.threshold");
        // Only the threshold line separates the counters from the fields, so it gets a small gap of
        // its own: the panes below already carry theirs.
        table.add(thresholdLabel).left().padLeft(pad10).padBottom(pad10).row();

        var fields = volume.fields;
        if (fields == null || fields.length == 0) {
            table.add(new OwnLabel(I18n.msg("gui.galaxy.volume.empty"), skin, "grey-normal"))
                    .left().padLeft(pad10).padBottom(pad18).row();
            return;
        }

        // The hint sits above the fields and only takes room while there is something to say: an
        // empty label has no height, but its padding would still push the first field down, which is
        // the dead space between the threshold line and the panes. markFieldsDirty() flips the text
        // in place, so the line appears without rebuilding the pane being dragged.
        editedHint = new OwnLabel(fieldsDirty ? I18n.msg("gui.galaxy.volume.edited") : "", skin, "grey-normal");
        tip(editedHint, "gui.galaxy.volume.edited");
        table.add(editedHint).left().padLeft(pad10).padBottom(fieldsDirty ? pad10 : 0f).row();

        for (var field : fields) {
            // The volume pipeline has one spiral (Distribution#toVolumeDistribution): a field loaded
            // from a file that names the second one becomes the first, so the header, the selector
            // and the next export all agree on the only spiral this pane can produce.
            field.distribution = field.distribution.toVolumeDistribution();
            // On the header rather than at the end of the editors, as in the billboard pane: the
            // control that removes the field sits next to the one that expands it.
            var delete = new OwnTextIconButton("", skin, "rubbish");
            tip(delete, "gui.galaxy.delete");
            delete.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event,
                                    float x,
                                    float y) {
                    super.clicked(event, x, y);
                    removeField(volume, field);
                }
            });
            var pane = new CollapsiblePane(stage,
                                           field.type.localizedName() + " - " + field.distribution.localizedName(),
                                           buildFieldDetail(field),
                                           TAB_CONTENT_WIDTH,
                                           skin,
                                           "header-s",
                                           "expand-collapse",
                                           null,
                                           isExpanded(field),
                                           null,
                                           delete);
            pane.setExpandCollapseRunnable(() -> setExpanded(field, pane.isExpanded()));
            table.add(pane).padBottom(pad18).row();
        }
    }

    /**
     * Builds the editors of one field.
     *
     * @param field The field to edit.
     *
     * @return A table the collapsible pane can wrap.
     */
    private Table buildFieldDetail(GalaxyAttributes field) {
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

        // Distribution. One spiral only: the field model draws both of them with the same curve, so
        // a second entry would be the same thing under another name (the billboard pane keeps both).
        var distributions = new Array<Distribution>();
        for (var d : Distribution.values()) {
            if (!d.isLogarithmicSpiral()) {
                distributions.add(d);
            }
        }
        var distribution = new OwnSelectBox<ComboBoxBean<Distribution>>(skin);
        distribution.setItems(ComboBoxBean.getValues(distributions));
        distribution.setWidth(HALF_WIDTH_BOX);
        // By value, not by ordinal: the list is filtered, so the two no longer coincide.
        for (var item : distribution.getItems()) {
            if (item.value == field.distribution) {
                distribution.setSelected(item);
                break;
            }
        }
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

        // Only the controls the chosen distribution reads are built: the field model is a switch, and
        // a knob that writes a parameter no branch reads looks like a broken knob. The billboard pane
        // gates its own controls on the same predicates.
        var dist = field.distribution;

        // Height profile. Only the flat distributions have a vertical profile at all: a sphere, an
        // ellipsoid, a cone or an irregular field is shaped by its radial profile alone.
        if (dist.isFlat()) {
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
        }

        // Radial profile: min and base radius side by side, as in the billboard pane, and with the
        // same ranges: baseRadius is a fraction of the object radius in both channel types, and a
        // volume cannot use a value above 1 at all - its box ends there.
        var radialTable = new Table(skin);
        var minRadius = fieldSlider("gui.galaxy.ds.radius.min", field.type.minRadius[0], field.type.minRadius[1],
                                    0.01f, field.minRadius, v -> field.minRadius = v);
        minRadius.setWidth(HALF_WIDTH_BOX);
        var baseRadius = fieldSlider("gui.galaxy.ds.radius.base", field.type.baseRadius[0], field.type.baseRadius[1],
                                     0.01f, field.baseRadius, v -> field.baseRadius = v);
        baseRadius.setWidth(HALF_WIDTH_BOX);
        radialTable.add(minRadius).left().padRight(PAD5);
        radialTable.add(baseRadius).left();
        detail.add(radialTable).colspan(2).left().padBottom(pad18).row();

        // Arms. The two spirals are the only distributions whose density has an azimuthal
        // modulation. The cone is the exception: its base angle is a half-angle, not a pitch, so it
        // gets a slider with that meaning instead.
        if (dist.isAnySpiral()) {
            var arms = fieldSlider("gui.galaxy.ds.arm.number", 0f, 8f, 1f, field.numArms,
                                   v -> field.numArms = Math.round(v));
            arms.setNumberFormatter(new DecimalFormat("##0"));
            addSlider(detail, arms);
            addSlider(detail, fieldSlider("gui.galaxy.ds.arm.sigma", 0.01f, 1.5f, 0.01f, field.armSigma,
                                          v -> field.armSigma = v));
            detail.add(angleSlider(field)).colspan(2).left().padBottom(pad18).row();
        } else if (dist == Distribution.CONE) {
            detail.add(coneAngleSlider(field)).colspan(2).left().padBottom(pad18).row();
        }

        // Flattening and aspect. The ellipsoid is the only distribution that scales its radius per
        // axis, and so the only one that reads the two flattening values; the bar is the only one
        // with an aspect ratio. Neither reads the other's parameter.
        if (dist.isEllipsoid()) {
            var eccTable = new Table(skin);
            var eccX = fieldSlider("gui.galaxy.ds.eccentricity.x", -0.95f, 0.95f, 0.01f, field.eccentricity[0],
                                   v -> field.eccentricity[0] = v);
            eccX.setWidth(HALF_WIDTH_BOX);
            var eccY = fieldSlider("gui.galaxy.ds.eccentricity.y", -0.95f, 0.95f, 0.01f, field.eccentricity[1],
                                   v -> field.eccentricity[1] = v);
            eccY.setWidth(HALF_WIDTH_BOX);
            eccTable.add(eccX).left().padRight(PAD5);
            eccTable.add(eccY).left();
            detail.add(eccTable).colspan(2).left().padBottom(pad18).row();
        } else if (dist.isBar()) {
            addSlider(detail, fieldSlider("gui.galaxy.ds.aspect", 0.1f, 3.0f, 0.01f, field.aspect,
                                          v -> field.aspect = v));
        }

        // Vertical structure: the warp and the sech^2 slab only exist in the flat branch.
        if (dist.isFlat()) {
            // The range is the channel type's: a warp is a magnitude, and past 0.15 it folds the disk
            // instead of bending it.
            addSlider(detail, fieldSlider("gui.galaxy.ds.warp", field.type.warpStrength[0], field.type.warpStrength[1], 0.001f, field.warpStrength,
                                          v -> field.warpStrength = v));
            addSlider(detail, fieldSlider("gui.galaxy.ds.height.scale", 0f, 0.5f, 0.005f, field.heightScale,
                                          v -> field.heightScale = v));
        }

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

        // Volume-only knobs: the sprite path has no equivalent for any of these. The arm warp
        // displaces the arm centreline, so it only means something where there is an arm.
        if (dist.isAnySpiral()) {
            // The arm warp is a phase displacement of the centreline in fractions of the arm spacing:
            // past 0.15 the arms stop reading as arms and start reading as a braid. The sign only
            // mirrors the bend, so the range is not signed.
            addSlider(detail, fieldSlider("gui.galaxy.field.lane.warp", 0f, 1f, 0.005f, field.laneWarp,
                                          v -> field.laneWarp = v));
            addSlider(detail, fieldSlider("gui.galaxy.field.lane.noise.scale", 0.1f, 20f, 0.1f, field.laneNoiseScale,
                                          v -> field.laneNoiseScale = v));
            // Only the dust of a spiral has lanes to break up (GalaxyDensityField#sample).
            if (field.isOcclusive()) {
                // Width along the arm: positive thins the centre and thickens the outskirts, and
                // stretches the dust envelope so those outer lanes are above the alpha floor;
                // negative mirrors it.
                addSlider(detail, fieldSlider("gui.galaxy.field.lane.width.taper", -1f, 1f, 0.01f, field.laneWidthTaper,
                                              v -> field.laneWidthTaper = v));
                // Structure inside the lane: zero-mean, so it rearranges the dust instead of dimming
                // it, and it is the only knob here that works fine-grained.
                addSlider(detail, fieldSlider("gui.galaxy.field.lane.structure", 0f, 1f, 0.01f, field.armStructure,
                                              v -> field.armStructure = v));
            }
        }
        addSlider(detail, fieldSlider("gui.galaxy.field.noise.mask", 0f, 1f, 0.01f, field.noiseMask,
                                      v -> field.noiseMask = v));
        addSlider(detail, fieldSlider("gui.galaxy.field.noise.mask.freq", 0f, 40f, 0.5f, field.noiseMaskFreq,
                                      v -> field.noiseMaskFreq = v));
        var octaves = fieldSlider("gui.galaxy.field.noise.mask.octaves", 0f, 4f, 1f, field.noiseMaskOctaves,
                                  v -> field.noiseMaskOctaves = Math.round(v));
        octaves.setNumberFormatter(new DecimalFormat("##0"));
        addSlider(detail, octaves);

        // Occlusion: only dust writes depth, so only dust gets the knob.
        if (field.isOcclusive()) {
            addSlider(detail, fieldSlider("gui.galaxy.field.occlusion", 0f, 1f, 0.01f, field.occlusionThreshold,
                                          v -> field.occlusionThreshold = v));
        }

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
    private OwnSliderReset fieldSlider(String key,
                                       float min,
                                       float max,
                                       float step,
                                       float value,
                                       FieldConsumer setter) {
        // The reset value is the one the field was drawn or edited to when the pane was built, so
        // the button undoes the edits made to this pane, not the whole generation.
        var slider = new OwnSliderReset(I18n.msg(key), min, max, step, value, skin);
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

    /**
     * Base angle of a field. It is carried in tenths of a degree, the same unit the descriptor and
     * the shaders use, and the slider covers the range the generator draws from
     * ({@link ChannelType#baseAngle}, 0..1000, i.e. 0..100 deg) with the same curve as the billboard
     * pane's slider of the same name. A narrower range here would clamp a dataset's 1000 to its
     * maximum and move the volume's arms dozens of degrees away from the stars'.
     *
     * @param field The field being edited.
     *
     * @return The slider, ready to be added.
     */
    private OwnSliderReset angleSlider(GalaxyAttributes field) {
        var baseAngle = new OwnSliderReset(I18n.msg("gui.galaxy.ds.angle"),
                                           field.type.baseAngle[0],
                                           field.type.baseAngle[1],
                                           true,
                                           field.baseAngle,
                                           skin);
        tip(baseAngle, "gui.galaxy.ds.angle");
        baseAngle.setLogarithmicExponent(1.2);
        baseAngle.setValueLabelTransform(TENTHS_TO_DEGREES);
        baseAngle.setWidth(FULL_WIDTH_BOX);
        baseAngle.setMappedValue(field.baseAngle);
        baseAngle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                field.baseAngle = baseAngle.getMappedValue();
                markFieldsDirty();
            }
        });
        return baseAngle;
    }

    /**
     * The base angle of a cone field. The field model clamps it to [1, 60] degrees and uses it as
     * the half-angle of the cone, so the arm slider - logarithmic, labelled as a pitch, and ranging
     * over the whole pitch range - would both mislabel it and allow values that do nothing.
     *
     * @param field The field to edit.
     *
     * @return The slider, configured and listening.
     */
    private OwnSliderReset coneAngleSlider(GalaxyAttributes field) {
        // Stored in tenths of a degree, like the arm pitch; shown in degrees.
        var angle = new OwnSliderReset(I18n.msg("gui.galaxy.ds.cone.angle"), 1f, 60f, 0.5f, field.baseAngle * 0.1f, skin);
        tip(angle, "gui.galaxy.ds.cone.angle");
        angle.setWidth(FULL_WIDTH_BOX);
        angle.setValue(field.baseAngle * 0.1f);
        angle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                field.baseAngle = angle.getValue() * 10f;
                markFieldsDirty();
            }
        });
        return angle;
    }

    /** Adds a slider on its own row, spanning both columns of the pane. **/
    private void addSlider(Table table,
                           OwnSliderReset slider) {
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
        table.row();
    }

    // ---------------------------------------------------------------------------------------------
    // Actions.
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onGenerate() {
        if (morphologyChanged || seedChanged) {
            // A new morphology or a new seed is a new galaxy: it replaces every dataset and every
            // field, edits included. That is what Randomize does, and what the seed field means.
            redraw();
        } else {
            // Nothing about the draw changed, so generate what the panes hold: the edits are the
            // object of this window, and pressing Generate must not throw them away.
            regenerateCurrent();
        }
    }

    /**
     * Relaunches the generation with the datasets and the fields as the panes left them. Nothing is
     * re-drawn from the seed: the billboard shader runs again over the current datasets, and the
     * volume is voxelised again into its existing textures.
     */
    private void regenerateCurrent() {
        fieldsDirty = false;
        morphologyChanged = false;
        regenerate();
        if (isHybrid()) {
            VolumeInitializer.generateNow(entityDiffuse);
        }
        rebuild();
    }

    /**
     * Draws a new galaxy from the current seed and morphology, replacing the datasets and the fields
     * of the current entities. Nothing about the entities themselves changes, so this is the cheap
     * path: no entity is created or removed.
     */
    private void redraw() {
        if (entityFull == null) {
            return;
        }
        long seed = getSeed();
        var gm = morphology;
        var spec = gen.generateGalaxySpec(gm, seed);
        // A fresh draw replaces the fields wholesale, edits included.
        fieldsDirty = false;
        morphologyChanged = false;
        seedChanged = false;

        regenerate();
        GaiaSky.postRunnable(() -> {
            var bb = Mapper.billboardSet.get(entityFull);
            if (bb != null) {
                bb.morphology = gm;
                bb.seed = seed;
                bb.datasets = spec.full;
            }
            var volume = entityDiffuse != null ? Mapper.volume.get(entityDiffuse) : null;
            if (volume != null) {
                volume.fields = spec.volumeFields;
                VolumeInitializer.generateNow(entityDiffuse);
            }
            rebuild();
        });
    }

    @Override
    protected void regenerate() {
        if (entityFull != null && Mapper.render.has(entityFull)) {
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityFull));
        }
    }

    @Override
    protected void onExport() {
        // Both halves go in one descriptor: the billboard entity, and the volume child that carries
        // the density fields the raymarcher voxelises at load time.
        var json = gen.convertToHybridJson(entityFull, entityDiffuse);
        var string = GalaxyGenerator.toJsonString(json);
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
