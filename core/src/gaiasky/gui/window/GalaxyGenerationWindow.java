/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.CollapsiblePane;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.DoubleValidator;

import java.util.Random;

/**
 * Interface to the procedural generation of galaxies using billboard datasets and compute shaders.
 */
public class GalaxyGenerationWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GalaxyGenerationWindow.class);

    private static int sequence = 153;
    private static final String DEFAULT_NAME = "new";

    private final Random rand = new Random();
    private Entity entity;
    private final FocusView view;
    private float fieldWidth, fieldWidthAll, fieldWidthTotal, textWidth;

    public GalaxyGenerationWindow(FocusView target, Scene scene, Skin skin, Stage stage) {
        super(I18n.msg("gui.galaxy.title", target == null ? DEFAULT_NAME : target.getLocalizedName()), skin, stage);

        if (target == null) {
            // Create new object with a given radius r, and 2r in front of the camera.
            String name = generateNewName();
            var radius = 5 * Constants.KPC_TO_U;
            var camera = GaiaSky.instance.getICamera();
            var cpos = camera.getPos();
            var cdir = new Vector3D(camera.getDirection());
            var pos = new Vector3Q(cpos);
            pos.add(cdir.nor().scl(radius * 2.0));

            // Create entity.
            var entity = GaiaSky.instance.scripting().apiv2().scene.createNewProceduralGalaxy(name,
                                                                                              radius,
                                                                                              pos);
            // Add to scene.
            GaiaSky.postRunnable(() -> {
                scene.initializeEntity(entity);
                scene.setUpEntity(entity);
                EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, entity, true);
            });

            this.entity = entity;
        } else {
            this.entity = target.getEntity();
        }
        this.view = new FocusView(this.entity);

        this.setModal(false);
        setAcceptText(I18n.msg("gui.close"));

        // Build UI
        buildSuper();
    }

    private static String generateNewName() {
        return "galaxy_" + sequence++;
    }

    private void reinitialize(Entity entity) {
        this.entity = entity;
        this.view.setEntity(entity);
        this.setModal(false);

        this.getTitleLabel().setText(I18n.msg("gui.galaxy.title", view.getLocalizedName()));

        // Build UI
        rebuild();
    }

    protected void rebuild() {
        this.content.clear();
        build();
    }

    @Override
    protected void build() {
        this.textWidth = 220f;
        this.fieldWidth = 500f;
        this.fieldWidthAll = 750f;
        this.fieldWidthTotal = 950f;
        float tabContentWidth = 400f;

        // First, global parameters:
        // Size, fades, transforms, etc.

        // Size in KPC
        var sizeVal = new DoubleValidator(0, 100);
        OwnLabel sizeLabel = new OwnLabel(I18n.msg("gui.galaxy.size"), skin);
        OwnTextField size = new OwnTextField(Double.toString(view.getSize() * Constants.U_TO_KPC), skin, sizeVal);
        size.setWidth(fieldWidth);

        content.add(sizeLabel).left().padRight(pad34).padBottom(pad18);
        content.add(size).left().padBottom(pad18).row();

        // Rotations.


        // Second, billboard datasets.
        var render = Mapper.render.get(entity);
        var billboard = Mapper.billboardSet.get(entity);
        var datasets = billboard.datasets;
        var half = render.halfResolutionBuffer;
        for (var ds : datasets) {
            var table = new Table(skin);

            // Header
            String title = ds.type.name() + (half ? " (half res)" : "");
            CollapsiblePane groupPane = new CollapsiblePane(stage, title,
                                                            table, tabContentWidth, skin, "hud-header", "expand-collapse",
                                                            null, true, null);

            content.add(groupPane).colspan(2).padBottom(pad18).row();
        }
    }

    @Override
    protected boolean accept() {
        return false;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(Event event, Object source, Object... data) {

    }

}
