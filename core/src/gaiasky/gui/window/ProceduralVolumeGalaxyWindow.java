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
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Volume;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyAttributes.ChannelType;
import gaiasky.scene.record.GalaxyGenerator.GalaxyMorphology;
import gaiasky.scene.system.initialize.VolumeInitializer;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;

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
     * A field is listed with the two things that identify it - its channel type and its density
     * model - plus the slot it lands in. The editable parameters come next; they live here because
     * this is the pane the volume fields belong to, and the scene loads them from the same object.
     *
     * @param table The table to fill.
     */
    private void buildVolumeSections(Table table) {
        var volume = Mapper.volume.get(entityDiffuse);
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
        table.add(slotCounter).left().padBottom(pad18).row();

        var fields = volume.fields;
        if (fields == null || fields.length == 0) {
            table.add(new OwnLabel(I18n.msg("gui.galaxy.volume.empty"), skin, "grey-normal"))
                    .left().padBottom(pad18).row();
            return;
        }
        for (var field : fields) {
            var slot = field.isOcclusive() ? I18n.msg("gui.galaxy.volume.slot.occlusive")
                                           : I18n.msg("gui.galaxy.volume.slot.emissive");
            // Summary only for now; the editors replace this table when the pane is filled in.
            var detail = new Table(skin);
            var summary = new OwnLabel(I18n.msg("gui.galaxy.volume.field.summary",
                                                slot,
                                                field.baseRadius,
                                                field.intensity),
                                       skin, "grey-normal");
            tip(summary, "gui.galaxy.volume.field.summary");
            detail.add(summary).left().row();
            var pane = new CollapsiblePane(stage,
                                           field.type.localizedName() + " - " + field.distribution.localizedName(),
                                           detail,
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
            recreate();
        } else {
            generateInPlace();
        }
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
        // The base has already written the new seed through setSeed().
        onGenerate();
    }
}
