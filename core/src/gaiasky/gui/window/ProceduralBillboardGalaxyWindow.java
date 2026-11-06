/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
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
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.BlendMode;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes.ChannelType;
import gaiasky.scene.record.GalaxyAttributes.Distribution;
import gaiasky.scene.record.GalaxyAttributes.HeightProfile;
import gaiasky.scene.record.GalaxyGenerator;
import gaiasky.scene.record.GalaxyGenerator.GalaxyMorphology;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Interface to the procedural generation of galaxies using billboard datasets and compute shaders.
 * <p>
 * A procedural galaxy is drawn as two billboard entities: a full-resolution one with the compact
 * channels (stars, HII regions) and a half-resolution sibling with the diffuse ones (gas, bulge,
 * dust) that get composited over it. The window edits both, which is why it is the one that has to
 * name two sized entities and two transformed ones.
 * <p>
 * The layout and the window lifecycle come from {@link AbstractGalaxyGenWindow}; this class only
 * says which entities to act on and what the channel list and the three actions do.
 */
public class ProceduralBillboardGalaxyWindow extends AbstractGalaxyGenWindow {
    private Entity entityFull, entityHalf;
    private final FocusView viewFull, viewHalf;

    public ProceduralBillboardGalaxyWindow(FocusView target, Scene scene, Stage stage, Skin skin) {
        super(scene, stage, skin);
        this.viewFull = new FocusView();
        this.viewHalf = new FocusView();

        // Entities before UI: the scaffold reads them while building.
        init(target, null);
        buildSuper();
    }

    public ProceduralBillboardGalaxyWindow(String newName, GalaxyMorphology morphology, Scene scene, Stage stage, Skin skin) {
        super(scene, stage, skin);
        this.viewFull = new FocusView();
        this.viewHalf = new FocusView();
        this.morphology = morphology;

        // Entities before UI: the scaffold reads them while building.
        init(null, newName);
        buildSuper();
    }

    @Override
    protected void init(FocusView target, String name) {
        if (target == null) {
            // Create new object with the given name, a given radius r, and 2r in front of the camera.
            var radius = 10 * Constants.KPC_TO_U;
            var camera = GaiaSky.instance.getICamera();
            var cpos = camera.getPos();
            var cdir = new Vector3D(camera.getDirection());
            var pos = new Vector3Q(cpos);
            pos.add(cdir.nor().scl(radius * 2.0));

            // Create entity.
            var pair = GaiaSky.instance.scripting().apiv2().scene.createNewProceduralGalaxy(name,
                                                                                            radius,
                                                                                            pos,
                                                                                            morphology);
            var entityFull = pair.getFirst();
            var entityHalf = pair.getSecond();
            // Add to scene.
            GaiaSky.postRunnable(() -> {
                scene.initializeEntity(entityFull);
                scene.initializeEntity(entityHalf);
                scene.setUpEntity(entityFull);
                scene.setUpEntity(entityHalf);
                EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, entityFull, true);
                EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, entityHalf, true);
            });

            this.entityFull = entityFull;
            this.entityHalf = entityHalf;
        } else {
            this.entityFull = target.getEntity();
            var graph = Mapper.graph.get(this.entityFull);
            var archetype = scene.archetypes().get("BillboardGroup");
            this.entityHalf = graph.getFirstChildOfType(archetype);
        }
        var fullBB = Mapper.billboardSet.get(this.entityFull);
        this.morphology = fullBB.morphology != null ? fullBB.morphology : GalaxyMorphology.Sc;
        this.viewFull.setEntity(this.entityFull);
        this.viewHalf.setEntity(this.entityHalf);
    }

    // ---------------------------------------------------------------------------------------------
    // Hooks.
    // ---------------------------------------------------------------------------------------------

    @Override
    protected boolean isEditableFocus(FocusView view) {
        // A hybrid galaxy is also a billboard dataset, but it is the volume window's: retargeting
        // this one at it would show a half-resolution pane for an entity that has no billboard
        // child.
        return view.isBillboardDataset() && !hasVolumeChild(view.getEntity());
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
        // Arrays.asList, not List.of: a billboard entity that is not a procedural galaxy has no
        // half-resolution child, and List.of would throw on the null.
        return Arrays.asList(entityFull, entityHalf);
    }

    @Override
    protected Entity getTransformSource() {
        return entityFull;
    }

    @Override
    protected long getSeed() {
        return Mapper.billboardSet.get(entityFull).seed;
    }

    @Override
    protected void setSeed(long seed) {
        Mapper.billboardSet.get(entityFull).seed = seed;
    }

    @Override
    protected void onMorphologyChanged(GalaxyMorphology morphology) {
        Mapper.billboardSet.get(entityFull).morphology = morphology;
    }

    @Override
    protected void buildChannelSections(Table table) {
        addBillboardDatasets(entityFull, table, THIRD_WIDTH_BOX, HALF_WIDTH_BOX, FULL_WIDTH_BOX, TAB_CONTENT_WIDTH);
        table.add(new Separator(skin, "gray")).fillX().expandX().padBottom(pad20).row();
        addBillboardDatasets(entityHalf, table, THIRD_WIDTH_BOX, HALF_WIDTH_BOX, FULL_WIDTH_BOX, TAB_CONTENT_WIDTH);
    }

    @Override
    protected void buildChannelButtons(Table table) {
        // One per half: the two entities have independent channel lists.
        var addChannelFull = newAddChannelButton(entityFull, "gui.galaxy.add.full", BUTTON_WIDTH);
        table.add(addChannelFull).center().padRight(pad34).padBottom(pad10);
        var addChannelHalf = newAddChannelButton(entityHalf, "gui.galaxy.add.half", BUTTON_WIDTH);
        table.add(addChannelHalf).center().padBottom(pad10).row();
    }

    @Override
    protected void onGenerate() {
        if (morphologyChanged) {
            // The morphology changed, so the datasets have to be drawn again from scratch.
            generateRandom(morphology, getSeed());
            morphologyChanged = false;
        } else {
            regenerate();
        }
    }

    @Override
    protected void onExport() {
        var json = gen.convertToJson(entityFull, entityHalf);
        var string = GalaxyGenerator.toJsonString(json);
        Gdx.app.getClipboard().setContents(string);
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.galaxy.export.done"));
    }

    @Override
    protected void onRandomize() {
        // The base has already replaced the seed.
        generateRandom(morphology, getSeed());
    }

    @Override
    protected Object getPersistenceKey() {
        // The full-resolution entity identifies the galaxy; the half one is its child.
        return entityFull;
    }

    /**
     * Relaunches the compute shader to generate the current entity.
     */
    @Override
    protected void regenerate() {
        // Discard current datasets.
        if (entityFull != null && Mapper.render.has(entityFull))
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityFull));
        if (entityHalf != null && Mapper.render.has(entityHalf))
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityHalf));
    }

    private void generateRandom(GalaxyMorphology gm, long seed) {
        var pair = gen.generateGalaxy(gm, seed);
        var full = pair.getFirst();
        var half = pair.getSecond();
        // Regenerate forces disposal of current datasets.
        regenerate();
        GaiaSky.postRunnable(() -> {
            if (full != null && full.length > 0) {
                var f = Mapper.billboardSet.get(entityFull);
                f.datasets = full;
            }
            if (half != null && half.length > 0) {
                var h = Mapper.billboardSet.get(entityHalf);
                h.datasets = half;
            }
            rebuild();
        });
    }

}
