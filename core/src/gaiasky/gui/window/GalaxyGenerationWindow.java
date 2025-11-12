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
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.BillboardDataset.Distribution;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface to the procedural generation of galaxies using billboard datasets and compute shaders.
 */
public class GalaxyGenerationWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GalaxyGenerationWindow.class);

    private static int sequence = 153;

    private final Scene scene;
    private Entity entityFull, entityHalf;
    private final FocusView view;

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
            var pair = GaiaSky.instance.scripting().apiv2().scene.createNewProceduralGalaxy(name,
                                                                                            radius,
                                                                                            pos);

            var entityFull = pair.getFirst();
            var entityHalf = pair.getSecond();
            // Add to scene.
            GaiaSky.postRunnable(() -> {
                scene.initializeEntity(entityFull);
                scene.initializeEntity(entityHalf);
                scene.setUpEntity(entityFull);
                scene.setUpEntity(entityHalf);
                EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, entityFull, true);
            });

            this.entityFull = entityFull;
            this.entityHalf = entityHalf;
        } else {
            this.entityFull = target.getEntity();
            var graph = Mapper.graph.get(this.entityFull);
            var archetype = scene.archetypes().get("BillboardGroup");
            this.entityHalf = graph.getFirstChildOfType(archetype);
        }
        this.view.setEntity(this.entityFull);
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
        float fieldWidthTotal = 950f;
        float tabContentWidth = 900f;
        float scrollHeight = 800f;
        float fullWidthBox = 850f;
        float halfWidthBox = fullWidthBox / 2f - 10f;

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


        // SECOND: billboard datasets (full).
        var datasetsTable = new Table(skin);
        datasetsTable.top();

        addDatasets(entityFull, datasetsTable, halfWidthBox, fullWidthBox, tabContentWidth);
        addDatasets(entityHalf, datasetsTable, halfWidthBox, fullWidthBox, tabContentWidth);

        var scroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(true, false);
        scroll.setForceScroll(false, true);
        scroll.setSmoothScrolling(false);
        scroll.setWidth(fullWidthBox + 100f);
        scroll.setHeight(scrollHeight);

        content.add(scroll).

                left().

                row();


        // THIRD: generate button.
        OwnTextButton generate = new OwnTextButton(I18n.msg("gui.galaxy.generate"), skin);
        generate.addListener(new

                                     ChangeListener() {
                                         @Override
                                         public void changed(ChangeEvent event,
                                                             Actor actor) {
                                             // Discard current datasets.
                                             if (entityFull != null && Mapper.render.has(entityFull))
                                                 EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityFull));
                                             if (entityHalf != null && Mapper.render.has(entityHalf))
                                                 EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityHalf));
                                         }
                                     });
        generate.pad(pad10, pad20, pad10, pad20);
        generate.addListener(new

                                     OwnTextTooltip(I18n.msg("gui.galaxy.generate.info"), skin));

        content.add(generate).

                center().

                padTop(pad34);

    }

    /**
     * Adds the billboard datasets of the given entity to the given table.
     *
     * @param entity          The entity.
     * @param datasetsTable   The table.
     * @param halfWidthBox    The half width.
     * @param fullWidthBox    The bull width.
     * @param tabContentWidth The tab content width.
     */
    private void addDatasets(Entity entity, Table datasetsTable, float halfWidthBox, float fullWidthBox, float tabContentWidth) {
        var render = Mapper.render.get(entity);
        var billboard = Mapper.billboardSet.get(entity);
        var datasets = billboard.datasets;
        var half = render.halfResolutionBuffer;
        for (var ds : datasets) {
            var dsTable = new Table(skin);

            // List of all widgets used only in density wave distribution.
            final Set<Disableable> densityWidgets = new HashSet<>();
            // List of all widgets used only in spiral distribution.
            final Set<Disableable> spiralWidgets = new HashSet<>();
            // List of all widgets
            final Set<Disableable> allWidgets = new HashSet<>();

            // Distribution
            OwnSelectBox<Distribution> distribution = new OwnSelectBox<>(skin);
            distribution.setItems(Distribution.values());
            distribution.setWidth(halfWidthBox);
            distribution.setSelected(ds.distribution);
            distribution.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.distribution = distribution.getSelected();
                    updateState(allWidgets, densityWidgets, spiralWidgets, ds.distribution);
                }
            });
            OwnLabel distributionLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.distribution"), skin);
            distributionLabel.setWidth(halfWidthBox);

            dsTable.add(distributionLabel).left().padRight(pad20).padBottom(pad18);
            dsTable.add(distribution).left().padBottom(pad18).row();

            // Num particles
            OwnSliderPlus nParticles = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.particles"), 1, 100_000, 1, skin);
            nParticles.setNumberFormatter(new DecimalFormat("#####0"));
            nParticles.setWidth(fullWidthBox);
            nParticles.setValue(ds.particleCount);
            nParticles.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setParticleCount((long) nParticles.getValue());
                }
            });
            dsTable.add(nParticles).colspan(2).left().padBottom(pad18).row();

            // Height scale
            OwnSliderPlus heightScale = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.height"), 0.0f, 1.0f, 0.001f, skin);
            heightScale.setWidth(fullWidthBox);
            heightScale.setValue(ds.heightScale);
            heightScale.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setHeightScale((double) heightScale.getValue());
                }
            });
            dsTable.add(heightScale).colspan(2).left().padBottom(pad18).row();

            // Min and base radii
            OwnSliderPlus minRadius = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.radius.min"), 0f, 1f, 0.001f, skin);
            minRadius.setWidth(halfWidthBox);
            minRadius.setValue(ds.minRadius);
            minRadius.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setMinRadius((double) minRadius.getValue());
                }
            });
            OwnSliderPlus baseRadius = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.radius.base"), 0f, 1f, 0.001f, skin);
            baseRadius.setWidth(halfWidthBox);
            baseRadius.setValue(ds.baseRadius);
            baseRadius.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setBaseRadius((double) baseRadius.getValue());
                }
            });
            dsTable.add(minRadius).left().padRight(pad20).padBottom(pad18);
            dsTable.add(baseRadius).left().padBottom(pad18).row();

            // Particle size
            OwnSliderPlus size = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.size"), 0.1f, 300.0f, 0.1f, skin);
            size.setWidth(fullWidthBox);
            size.setValue(ds.size);
            size.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSize((double) size.getValue());
                }
            });
            dsTable.add(size).colspan(2).left().padBottom(pad18).row();

            // Intensity
            OwnSliderPlus intensity = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.intensity"), 0.0f, 1.0f, 0.001f, true, skin);
            intensity.setDisplayValueMapped(true);
            intensity.setNumberFormatter(new DecimalFormat("#0.######"));
            intensity.setWidth(fullWidthBox);
            intensity.setMappedValue(ds.intensity);
            intensity.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setIntensity((double) intensity.getMappedValue());
                }
            });
            dsTable.add(intensity).colspan(2).left().padBottom(pad18).row();

            // Number of arms
            OwnSliderPlus numArms = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.arms"), 1f, 10f, 1f, skin);
            numArms.setNumberFormatter(new DecimalFormat("#0"));
            numArms.setWidth(fullWidthBox);
            numArms.setValue(ds.spiralArms);
            numArms.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSpiralArms((long) numArms.getValue());
                }
            });
            dsTable.add(numArms).colspan(2).left().padBottom(pad18).row();

            // Spiral angle
            OwnSliderPlus spiralAngle = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.angle"), 0f, 800f, 0.1f, skin);
            spiralAngle.setNumberFormatter(new DecimalFormat("##0.###"));
            spiralAngle.setWidth(fullWidthBox);
            spiralAngle.setValue(ds.spiralAngle);
            spiralAngle.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSpiralAngle((double) spiralAngle.getValue());
                }
            });
            dsTable.add(spiralAngle).colspan(2).left().padBottom(pad18).row();

            // Eccentricity
            OwnSliderPlus eccentricity = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.eccentricity"), 0f, 0.25f, 0.01f, skin);
            eccentricity.setNumberFormatter(new DecimalFormat("0.####"));
            eccentricity.setWidth(fullWidthBox);
            eccentricity.setValue(ds.eccentricity);
            eccentricity.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setEccentricity((double) eccentricity.getValue());
                }
            });
            dsTable.add(eccentricity).colspan(2).left().padBottom(pad18).row();

            // Displacement in x and y
            OwnSliderPlus deltaX = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.delta.x"), -0.5f, 0.5f, 0.01f, skin);
            deltaX.setWidth(halfWidthBox);
            deltaX.setValue(ds.displacement[0]);
            deltaX.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.displacement[0] = deltaX.getValue();
                }
            });
            OwnSliderPlus deltaY = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.delta.y"), -0.5f, 0.5f, 0.01f, skin);
            deltaY.setWidth(halfWidthBox);
            deltaY.setValue(ds.displacement[1]);
            deltaY.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.displacement[1] = deltaY.getValue();
                }
            });
            dsTable.add(deltaX).left().padRight(pad20).padBottom(pad18);
            dsTable.add(deltaY).left().padBottom(pad18).row();

            // Add density wave and spiral distribution widgets.
            allWidgets.addAll(Set.of(nParticles,
                                     heightScale,
                                     baseRadius,
                                     minRadius,
                                     size,
                                     intensity,
                                     numArms,
                                     spiralAngle,
                                     eccentricity,
                                     deltaY,
                                     deltaX));
            densityWidgets.addAll(Set.of(spiralAngle, eccentricity, deltaY, deltaX));
            spiralWidgets.addAll(Set.of(spiralAngle, numArms));
            updateState(allWidgets, densityWidgets, spiralWidgets, ds.distribution);

            // Header
            String title = ds.type.name() + (half ? " (half res)" : "");
            CollapsiblePane groupPane = new CollapsiblePane(stage, title,
                                                            dsTable, tabContentWidth, skin, "hud-header", "expand-collapse",
                                                            null, true, null);

            datasetsTable.add(groupPane).padBottom(pad18).row();
        }
    }

    private void updateState(Set<Disableable> all, Set<Disableable> density, Set<Disableable> spiral, Distribution d) {
        for (var w : all) {
            var enable = (d == Distribution.DENSITY && density.contains(w))
                    || (d == Distribution.SPIRAL && spiral.contains(w))
                    || (!spiral.contains(w) && !density.contains(w));

            w.setDisabled(!enable);
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
