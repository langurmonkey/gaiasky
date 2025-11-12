/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.BillboardDataset.ParticleType;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.DoubleValidator;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Interface to the procedural generation of galaxies using billboard datasets and compute shaders.
 */
public class GalaxyGenerationWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GalaxyGenerationWindow.class);

    private static int sequence = 153;
    private static final String DEFAULT_NAME = "new";

    private final Random rand = new Random();
    private final Scene scene;
    private Entity entity;
    private final FocusView view;
    private float fieldWidthTotal, fieldWidthBox, tabContentWidth;

    public GalaxyGenerationWindow(FocusView target, Scene scene, Stage stage, Skin skin) {
        super("", skin, stage);
        this.scene = scene;
        this.view = new FocusView();

        update(target);

        setModal(false);
        setCancelText(I18n.msg("gui.close"));

        // Build UI
        buildSuper();
    }

    private static String generateNewName() {
        return "galaxy_" + sequence++;
    }

    private void update(FocusView target) {

        if (target == null) {
            // Create new object with a given radius r, and 2r in front of the camera.
            String name = generateNewName();
            var radius = 10 * Constants.KPC_TO_U;
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
        this.view.setEntity(this.entity);
        this.getTitleLabel().setText(I18n.msg("gui.galaxy.title", view.getLocalizedName()));

    }

    public void reinitialize(FocusView target) {
        update(target);

        // Build UI
        rebuild();
    }

    protected void rebuild() {
        this.content.clear();
        build();
    }

    @Override
    protected void build() {
        this.fieldWidthTotal = 950f;
        this.fieldWidthBox = 650f;
        this.tabContentWidth = 700f;

        // FIRST: global parameters:
        // Size, fades, transforms, etc.

        // Size in KPC
        OwnSliderPlus sizeKpc = new OwnSliderPlus(I18n.msg("gui.galaxy.size"), 0.2f, 40.0f, 0.1f, skin);
        sizeKpc.setWidth(fieldWidthTotal);
        sizeKpc.setValue((float) (view.getSize() * Constants.U_TO_KPC));
        sizeKpc.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                var sizeInternal = sizeKpc.getValue() * Constants.KPC_TO_U;
                view.getBody().setSize(sizeInternal);
            }
        });
        content.add(sizeKpc).left().padBottom(pad18).row();

        // Rotations.


        // SECOND: billboard datasets.
        var render = Mapper.render.get(entity);
        var billboard = Mapper.billboardSet.get(entity);
        var datasets = billboard.datasets;
        var half = render.halfResolutionBuffer;
        for (var ds : datasets) {
            var table = new Table(skin);

            // Size
            OwnSliderPlus size = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.size"), 0.1f, 300.0f, 0.1f, skin);
            size.setWidth(fieldWidthBox);
            size.setValue(ds.size);
            size.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSize((double) size.getValue());
                }
            });
            table.add(size).left().padBottom(pad18).row();

            // Num particles
            OwnSliderPlus nParticles = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.particles"), 1, 100_000, 1, skin);
            nParticles.setNumberFormatter(new DecimalFormat("#####0"));
            nParticles.setWidth(fieldWidthBox);
            nParticles.setValue(ds.particleCount);
            nParticles.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setParticleCount((long) nParticles.getValue());
                }
            });
            table.add(nParticles).left().padBottom(pad18).row();

            // Intensity
            OwnSliderPlus intensity = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.intensity"), 0.0f, 1.0f, 0.001f, true, skin);
            intensity.setDisplayValueMapped(true);
            intensity.setNumberFormatter(new DecimalFormat("#0.######"));
            intensity.setWidth(fieldWidthBox);
            intensity.setMappedValue(ds.intensity);
            intensity.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setIntensity((double) intensity.getMappedValue());
                }
            });
            table.add(intensity).left().padBottom(pad18).row();


            // Header
            String title = ds.type.name() + (half ? " (half res)" : "");
            CollapsiblePane groupPane = new CollapsiblePane(stage, title,
                                                            table, tabContentWidth, skin, "hud-header", "expand-collapse",
                                                            null, true, null);

            content.add(groupPane).padBottom(pad18).row();
        }

        // THIRD: generate button.
        OwnTextButton generate = new OwnTextButton(I18n.msg("gui.galaxy.generate"), skin);
        generate.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, render);
            }
        });
        generate.pad(pad10, pad20, pad10, pad20);
        generate.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.generate.info"), skin));

        content.add(generate).center().padTop(pad34);

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
