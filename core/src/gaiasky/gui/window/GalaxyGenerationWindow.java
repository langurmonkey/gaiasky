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
import gaiasky.scene.record.BillboardDataset.Distribution;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;

/**
 * Interface to the procedural generation of galaxies using billboard datasets and compute shaders.
 */
public class GalaxyGenerationWindow extends GenericDialog implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GalaxyGenerationWindow.class);

    private static int sequence = 153;

    private final Scene scene;
    private Entity entityFull, entityHalf;
    private final FocusView viewFull, viewHalf;

    public GalaxyGenerationWindow(FocusView target, Scene scene, Stage stage, Skin skin) {
        super("", skin, stage);
        this.scene = scene;
        this.viewFull = new FocusView();
        this.viewHalf = new FocusView();

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
        this.viewFull.setEntity(this.entityFull);
        this.viewHalf.setEntity(this.entityHalf);
        this.getTitleLabel().setText(I18n.msg("gui.galaxy.title", viewFull.getLocalizedName()));
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
        float thirdWidthBox = fullWidthBox / 3f - 10f;

        // FIRST: global parameters:
        // Size, fades, transforms, etc.

        // Size in KPC
        OwnSliderPlus sizeKpc = new OwnSliderPlus(I18n.msg("gui.galaxy.size"), 0.2f, 40.0f, 0.1f, skin);
        sizeKpc.setWidth(fieldWidthTotal);
        sizeKpc.setValue((float) (viewFull.getSize() * Constants.U_TO_KPC));
        sizeKpc.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                var sizeInternal = sizeKpc.getValue() * Constants.KPC_TO_U;
                viewFull.getBody().setSize(sizeInternal);
                viewHalf.getBody().setSize(sizeInternal);
            }
        });
        content.add(sizeKpc).left().padBottom(pad18).row();

        // Rotations.


        // SECOND: billboard datasets (full).
        var datasetsTable = new Table(skin);
        datasetsTable.top();

        addDatasets(entityFull, datasetsTable, thirdWidthBox, halfWidthBox, fullWidthBox, tabContentWidth);
        addDatasets(entityHalf, datasetsTable, thirdWidthBox, halfWidthBox, fullWidthBox, tabContentWidth);

        var scroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(true, false);
        scroll.setForceScroll(false, true);
        scroll.setSmoothScrolling(false);
        scroll.setWidth(fullWidthBox + 100f);
        scroll.setHeight(scrollHeight);

        content.add(scroll).left().row();

        // THIRD: buttons.
        var buttons = new Table(skin);
        // Add channel
        OwnTextButton addChannel = new OwnTextButton(I18n.msg("gui.galaxy.add"), skin);
        addChannel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                GaiaSky.postRunnable(() -> {
                    var bb = Mapper.billboardSet.get(entityHalf);
                    var ds = bb.datasets;
                    var newDs = new BillboardDataset[ds.length + 1];
                    System.arraycopy(ds, 0, newDs, 0, ds.length);

                    var dataset = new BillboardDataset();
                    dataset.setType(BillboardDataset.ParticleType.GAS);
                    dataset.setLayers(new int[]{0, 1, 2});
                    dataset.setBaseColor(new double[]{0.8, 0.8, 0.8});
                    dataset.setMaxSize(20.0);
                    newDs[ds.length] = dataset;

                    bb.datasets = newDs;

                    ((GalaxyGenerationWindow) me).rebuild();
                });
            }
        });
        addChannel.pad(pad10, pad20, pad10, pad20);
        addChannel.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.add.info"), skin));
        buttons.add(addChannel).center().padRight(pad34);

        // Generate
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
        generate.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.generate.info"), skin));

        buttons.add(generate).center().padRight(pad34);
        content.add(buttons).center().padTop(pad34);

    }

    /**
     * Adds the billboard datasets of the given entity to the given table.
     *
     * @param entity          The entity.
     * @param datasetsTable   The table.
     * @param thirdWidthBox   The 1/3 width.
     * @param halfWidthBox    The half width.
     * @param fullWidthBox    The bull width.
     * @param tabContentWidth The tab content width.
     */
    private void addDatasets(Entity entity, Table datasetsTable, float thirdWidthBox, float halfWidthBox, float fullWidthBox, float tabContentWidth) {
        final var SLIDER_STEPS = 1_000;
        final var cpSize = 32f;
        var render = Mapper.render.get(entity);
        var billboard = Mapper.billboardSet.get(entity);
        var datasets = billboard.datasets;
        var half = render.halfResolutionBuffer;
        for (var ds : datasets) {
            var dsTable = new Table(skin);

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
                }
            });
            OwnLabel distributionLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.distribution"), skin);
            distributionLabel.setWidth(halfWidthBox);

            dsTable.add(distributionLabel).left().padRight(pad20).padBottom(pad18);
            dsTable.add(distribution).left().padBottom(pad18).row();

            // Num particles
            float nMin = ds.type.nParticles[0];
            float nMax = ds.type.nParticles[1];
            float nStep = (nMax - nMin) / SLIDER_STEPS;
            OwnSliderPlus nParticles = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.particles"), nMin, nMax, nStep, skin);
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

            // Dataset translation, XYZ
            float transMin = -1.0f;
            float transMax = 1.0f;
            float transStep = (transMax - transMin) / SLIDER_STEPS;
            OwnSliderPlus x = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.x"), transMin, transMax, transStep, skin);
            x.setWidth(thirdWidthBox);
            x.setValue(ds.translation.x);
            x.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.x = x.getValue();
                }
            });
            OwnSliderPlus y = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.y"), transMin, transMax, transStep, skin);
            y.setWidth(thirdWidthBox);
            y.setValue(ds.translation.y);
            y.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.y = y.getValue();
                }
            });
            OwnSliderPlus z = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.z"), transMin, transMax, transStep, skin);
            z.setWidth(thirdWidthBox);
            z.setValue(ds.translation.z);
            z.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.z = z.getValue();
                }
            });
            Table translationTable = new Table(skin);
            translationTable.add(x).left().padRight(pad20);
            translationTable.add(y).left().padRight(pad20);
            translationTable.add(z).left();
            dsTable.add(translationTable).colspan(2).left().padBottom(pad18).row();

            // Dataset rotation
            float rotMin = -90.0f;
            float rotMax = 90.0f;
            float rotStep = (rotMax - rotMin) / SLIDER_STEPS;
            OwnSliderPlus rx = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.rx"), rotMin, rotMax, rotStep, skin);
            rx.setWidth(thirdWidthBox);
            rx.setValue(ds.rotation.x);
            rx.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.x = rx.getValue();
                }
            });
            OwnSliderPlus ry = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.ry"), rotMin, rotMax, rotStep, skin);
            ry.setWidth(thirdWidthBox);
            ry.setValue(ds.rotation.y);
            ry.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.y = ry.getValue();
                }
            });
            OwnSliderPlus rz = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.rz"), rotMin, rotMax, rotStep, skin);
            rz.setWidth(thirdWidthBox);
            rz.setValue(ds.rotation.z);
            rz.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.z = rz.getValue();
                }
            });
            Table rotTable = new Table(skin);
            rotTable.add(rx).left().padRight(pad20);
            rotTable.add(ry).left().padRight(pad20);
            rotTable.add(rz).left();
            dsTable.add(rotTable).colspan(2).left().padBottom(pad18).row();

            // Dataset scale
            float sMin = -3.0f;
            float sMax = 3.0f;
            float sStep = (sMax - sMin) / SLIDER_STEPS;
            OwnSliderPlus sx = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.sx"), sMin, sMax, sStep, skin);
            sx.setWidth(thirdWidthBox);
            sx.setValue(ds.scale.x);
            sx.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.x = sx.getValue();
                }
            });
            OwnSliderPlus sy = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.sy"), sMin, sMax, sStep, skin);
            sy.setWidth(thirdWidthBox);
            sy.setValue(ds.scale.y);
            sy.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.y = sy.getValue();
                }
            });
            OwnSliderPlus sz = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.sz"), sMin, sMax, sStep, skin);
            sz.setWidth(thirdWidthBox);
            sz.setValue(ds.scale.z);
            sz.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.z = sz.getValue();
                }
            });
            Table sclTable = new Table(skin);
            sclTable.add(sx).left().padRight(pad20);
            sclTable.add(sy).left().padRight(pad20);
            sclTable.add(sz).left();
            dsTable.add(sclTable).colspan(2).left().padBottom(pad18).row();

            // Colors and color noise
            var cLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.color"), skin);
            var c1 = new ColorPicker("c1", ds.getColorRGBA(0), stage, skin);
            c1.setNewColorRunnable(() -> {
                ds.setColorRGBA(c1.color, 0);
            });
            var c2 = new ColorPicker("c2", ds.getColorRGBA(1), stage, skin);
            c2.setNewColorRunnable(() -> {
                ds.setColorRGBA(c2.color, 1);
            });
            var c3 = new ColorPicker("c3", ds.getColorRGBA(2), stage, skin);
            c3.setNewColorRunnable(() -> {
                ds.setColorRGBA(c3.color, 2);
            });
            var c4 = new ColorPicker("c4", ds.getColorRGBA(3), stage, skin);
            c4.setNewColorRunnable(() -> {
                ds.setColorRGBA(c4.color, 3);
            });
            Table colorsTable = new Table(skin);
            colorsTable.add(cLabel).left().padRight(pad34);
            colorsTable.add(c1).size(cpSize).left().padRight(pad10);
            colorsTable.add(c2).size(cpSize).left().padRight(pad10);
            colorsTable.add(c3).size(cpSize).left().padRight(pad10);
            colorsTable.add(c4).size(cpSize).left().padRight(pad10);
            OwnSliderPlus colorNoise = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.color.noise"), 0.0f, 1f, 0.01f, skin);
            colorNoise.setWidth(halfWidthBox);
            colorNoise.setValue(ds.sizeNoise);
            colorNoise.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setColorNoise((double) colorNoise.getValue());
                }
            });
            dsTable.add(colorsTable).left().padRight(pad20).padBottom(pad18);
            dsTable.add(colorNoise).left().padBottom(pad18).row();

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
            float minRadMin = ds.type.minRadius[0];
            float minRadMax = ds.type.minRadius[1];
            float minRadStep = (minRadMax - minRadMin) / SLIDER_STEPS;
            OwnSliderPlus minRadius = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.radius.min"), minRadMin, minRadMax, minRadStep, skin);
            minRadius.setWidth(halfWidthBox);
            minRadius.setValue(ds.minRadius);
            minRadius.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setMinRadius((double) minRadius.getValue());
                }
            });
            float baseRadMin = ds.type.baseRadius[0];
            float baseRadMax = ds.type.baseRadius[1];
            float baseRadStep = (baseRadMax - baseRadMin) / SLIDER_STEPS;
            OwnSliderPlus baseRadius = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.radius.base"), baseRadMin, baseRadMax, baseRadStep, skin);
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

            // Particle size and size noise
            float sizeMin = ds.type.size[0];
            float sizeMax = ds.type.size[1];
            float sizeStep = (sizeMax - sizeMin) / SLIDER_STEPS;
            OwnSliderPlus size = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.size"), sizeMin, sizeMax, sizeStep, skin);
            size.setWidth(halfWidthBox);
            size.setValue(ds.size);
            size.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSize((double) size.getValue());
                }
            });
            OwnSliderPlus sizeNoise = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.size.noise"), 0.0f, 1f, 0.01f, skin);
            sizeNoise.setWidth(halfWidthBox);
            sizeNoise.setValue(ds.sizeNoise);
            sizeNoise.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSizeNoise((double) sizeNoise.getValue());
                }
            });
            dsTable.add(size).left().padRight(pad20).padBottom(pad18);
            dsTable.add(sizeNoise).left().padBottom(pad18).row();

            // Intensity
            float iMin = ds.type.intensity[0];
            float iMax = ds.type.intensity[1];
            float iStep = (iMax - iMin) / SLIDER_STEPS;
            OwnSliderPlus intensity = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.intensity"), iMin, iMax, iStep, skin);
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
            OwnSliderPlus numArms = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.arms"), 1f, 8f, 1f, skin);
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
            float angleMin = ds.type.baseAngle[0];
            float angleMax = ds.type.baseAngle[1];
            float angleStep = (angleMax - angleMin) / SLIDER_STEPS;
            OwnSliderPlus spiralAngle = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.angle"), angleMin, angleMax, angleStep, skin);
            spiralAngle.setNumberFormatter(new DecimalFormat("##0.###"));
            spiralAngle.setWidth(fullWidthBox);
            spiralAngle.setValue(ds.baseAngle);
            spiralAngle.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setBaseAngle((double) spiralAngle.getValue());
                }
            });
            dsTable.add(spiralAngle).colspan(2).left().padBottom(pad18).row();

            // Eccentricity
            float eMin = ds.type.eccentricity[0];
            float eMax = ds.type.eccentricity[1];
            float eStep = (eMax - eMin) / SLIDER_STEPS;
            OwnSliderPlus eccentricity = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.eccentricity"), eMin, eMax, eStep, skin);
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
            float dMin = ds.type.spiralDeltaPos[0];
            float dMax = ds.type.spiralDeltaPos[1];
            float dStep = (dMax - dMin) / SLIDER_STEPS;
            OwnSliderPlus deltaX = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.delta.x"), dMin, dMax, dStep, skin);
            deltaX.setWidth(halfWidthBox);
            deltaX.setValue(ds.spiralDeltaPos[0]);
            deltaX.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.spiralDeltaPos[0] = deltaX.getValue();
                }
            });
            OwnSliderPlus deltaY = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.delta.y"), dMin, dMax, dStep, skin);
            deltaY.setWidth(halfWidthBox);
            deltaY.setValue(ds.spiralDeltaPos[1]);
            deltaY.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.spiralDeltaPos[1] = deltaY.getValue();
                }
            });
            dsTable.add(deltaX).left().padRight(pad20).padBottom(pad18);
            dsTable.add(deltaY).left().padBottom(pad18).row();

            // Aspect
            float aspectMin = ds.type.aspect[0];
            float aspectMax = ds.type.aspect[1];
            float aspectStep = (aspectMax - aspectMin) / SLIDER_STEPS;
            OwnSliderPlus aspect = new OwnSliderPlus(I18n.msg("gui.galaxy.ds.aspect"), aspectMin, aspectMax, aspectStep, true, skin);
            aspect.setDisplayValueMapped(true);
            aspect.setNumberFormatter(new DecimalFormat("#0.#"));
            aspect.setWidth(fullWidthBox);
            aspect.setMappedValue(ds.aspect);
            aspect.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setAspect((double) aspect.getMappedValue());
                }
            });
            dsTable.add(aspect).colspan(2).left().padBottom(pad18).row();

            // Header
            String title = ds.type.name() + (half ? " (half res)" : "");
            CollapsiblePane groupPane = new CollapsiblePane(stage, title,
                                                            dsTable, tabContentWidth, skin, "hud-header", "expand-collapse",
                                                            null, true, null);

            datasetsTable.add(groupPane).padBottom(pad18).row();
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
