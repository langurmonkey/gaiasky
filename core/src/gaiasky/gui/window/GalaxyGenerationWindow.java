/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectSet;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.BlendMode;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.BillboardDataset.ChannelType;
import gaiasky.scene.record.BillboardDataset.Distribution;
import gaiasky.scene.record.BillboardDataset.HeightProfile;
import gaiasky.scene.record.GalaxyGenerator;
import gaiasky.scene.record.GalaxyGenerator.GalaxyMorphology;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.*;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.LongValidator;

import java.text.DecimalFormat;

/**
 * Interface to the procedural generation of galaxies using billboard datasets and compute shaders.
 */
public class GalaxyGenerationWindow extends GenericDialog implements IObserver {
    private final static int SLIDER_STEPS = 1_000;
    private static final float pad5 = 5f;
    /** Saves the scroll position for each (full-res) entity. **/
    private static final ObjectFloatMap<Entity> scrollY = new ObjectFloatMap<>();
    /** Saves the datasets whose collapsible panes are expanded. **/
    private static final ObjectSet<BillboardDataset> expandedDatasets = new ObjectSet<>();

    private final GalaxyGenerator gen;
    private final Scene scene;
    private Entity entityFull, entityHalf;
    private final FocusView viewFull, viewHalf;
    private GalaxyMorphology morphology;
    private boolean morphologyChanged = false;
    private OwnSelectBox<GalaxyMorphology> morphologyBox;

    private Matrix4 m = new Matrix4();

    public GalaxyGenerationWindow(FocusView target, Scene scene, Stage stage, Skin skin) {
        super("", skin, stage);
        this.scene = scene;
        this.viewFull = new FocusView();
        this.viewHalf = new FocusView();
        this.gen = new GalaxyGenerator();

        update(target, null);

        setModal(false);
        setCancelText(I18n.msg("gui.close"));

        // Build UI
        buildSuper();

        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED);
    }

    public GalaxyGenerationWindow(String newName, GalaxyMorphology morphology, Scene scene, Stage stage, Skin skin) {
        super("", skin, stage);
        this.scene = scene;
        this.morphology = morphology;
        this.viewFull = new FocusView();
        this.viewHalf = new FocusView();
        this.gen = new GalaxyGenerator();

        update(null, newName);

        setModal(false);
        setCancelText(I18n.msg("gui.close"));

        // Build UI
        buildSuper();

        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED);
    }

    private void update(FocusView target, String name) {

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
        this.getTitleLabel().setText(I18n.msg("gui.galaxy.title", viewFull.getLocalizedName()));
    }

    public void reinitialize(FocusView target) {
        update(target, null);

        // Build UI
        rebuild();
    }

    /**
     * Rebuilds the UI.
     */
    protected void rebuild() {
        this.content.clear();
        build();
    }

    /**
     * Relaunches the compute shader to generate the current entity.
     */
    protected void regenerate() {
        // Discard current datasets.
        if (entityFull != null && Mapper.render.has(entityFull))
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityFull));
        if (entityHalf != null && Mapper.render.has(entityHalf))
            EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(entityHalf));
    }


    @Override
    protected void build() {
        float fieldWidthTotal = 950f;
        float tabContentWidth = 900f;
        float scrollHeight = 700f;
        float fullWidthBox = 850f;
        float halfWidthBox = fullWidthBox / 2f - 12f;
        float thirdWidthBox = fullWidthBox / 3f - 12f;
        float buttonWidth = 380f;

        // Title
        var mainTitle = new OwnLabel(I18n.msg("gui.galaxy.galaxy", viewFull.getLocalizedName()), skin, "header");
        content.add(mainTitle).left().padBottom(pad20).row();

        // FIRST: global parameters:
        // Size, fades, transforms, etc.

        var bb = Mapper.billboardSet.get(entityFull);
        var seedLabel = new OwnLabel(I18n.msg("gui.galaxy.randomize.seed"), skin);
        var seedValidator = new LongValidator(-999999L, 999999L);
        var seed = new OwnTextField(Long.toString(bb.seed), skin, seedValidator);
        seed.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (seed.isValid()) {
                    bb.seed = seed.getLongValue(123L);
                }
            }
        });
        var morphologyLabel = new OwnLabel(I18n.msg("gui.galaxy.morphology"), skin);
        morphologyBox = new OwnSelectBox<>(skin);
        morphologyBox.setItems(GalaxyMorphology.values());
        morphologyBox.setSelected(morphology);
        morphologyBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                morphology = morphologyBox.getSelected();
                bb.morphology = morphology;
                morphologyChanged = true;
            }
        });
        var seedMorphTable = new Table(skin);
        seedMorphTable.add(seedLabel).padRight(pad10);
        seedMorphTable.add(seed).padRight(pad34);
        seedMorphTable.add(morphologyLabel).padRight(pad10);
        seedMorphTable.add(morphologyBox);
        content.add(seedMorphTable).left().padBottom(pad20).row();

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
        content.add(sizeKpc).left().padBottom(pad20).row();

        // Object position
        var position = new OwnTextButton(I18n.msg("gui.galaxy.pos"), skin);
        position.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GaiaSky.postRunnable(() -> {
                    var bodyFull = Mapper.body.get(entityFull);
                    var bodyHalf = Mapper.body.get(entityHalf);

                    var camera = GaiaSky.instance.getICamera();
                    var cpos = camera.getPos();
                    var cdir = new Vector3D(camera.getDirection());
                    var pos = new Vector3Q(cpos);
                    pos.add(cdir.nor().scl(bodyFull.size));

                    bodyFull.pos.set(pos);
                    bodyHalf.pos.set(pos);
                });
            }
        });
        content.add(position).left().padBottom(pad20).row();

        // Object rotation
        var trf = Mapper.transform.get(entityFull);
        m = trf.matrix == null ? m.idt() : trf.matrix.putIn(m);
        var euler = Matrix4Utils.recoverEulerAngles(m);
        var yaw = euler[0];
        var pitch = euler[1];
        var roll = euler[2];

        float rotMin = -180.0f;
        float rotMax = 180.0f;
        float rotStep = (rotMax - rotMin) / SLIDER_STEPS;
        var rx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rx"), rotMin, rotMax, rotStep, 0f, skin);
        var ry = new OwnSliderReset(I18n.msg("gui.galaxy.ds.ry"), rotMin, rotMax, rotStep, 0f, skin);
        var rz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rz"), rotMin, rotMax, rotStep, 0f, skin);

        // Set up Rx
        rx.setWidth(thirdWidthBox);
        rx.setValue(yaw);
        rx.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                commitRotation(rx.getMappedValue(), ry.getMappedValue(), rz.getMappedValue());
            }
        });
        // Set up Ry
        ry.setWidth(thirdWidthBox);
        ry.setValue(pitch);
        ry.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                commitRotation(rx.getMappedValue(), ry.getMappedValue(), rz.getMappedValue());
            }
        });
        // Set up Rz
        rz.setWidth(thirdWidthBox);
        rz.setValue(roll);
        rz.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                commitRotation(rx.getMappedValue(), ry.getMappedValue(), rz.getMappedValue());
            }
        });
        Table trfTable = new Table(skin);
        trfTable.add(rx).left().padRight(pad5);
        trfTable.add(ry).left().padRight(pad5);
        trfTable.add(rz).left();
        content.add(trfTable).left().padBottom(pad20).row();


        // SECOND: channels -> billboard datasets.
        var datasetsTable = new Table(skin);
        datasetsTable.top();

        addDatasets(entityFull, datasetsTable, thirdWidthBox, halfWidthBox, fullWidthBox, tabContentWidth);
        datasetsTable.add(new Separator(skin, "gray")).fillX().expandX().padBottom(pad20).row();
        addDatasets(entityHalf, datasetsTable, thirdWidthBox, halfWidthBox, fullWidthBox, tabContentWidth);

        var scroll = new OwnScrollPane(datasetsTable, skin, "minimalist");
        scroll.setScrollingDisabled(true, false);
        scroll.setForceScroll(false, true);
        scroll.setSmoothScrolling(false);
        scroll.setWidth(fullWidthBox + 100f);
        scroll.setHeight(scrollHeight);
        scroll.addListener(new InputListener() {
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                // Save last scroll Y position.
                scrollY.put(entityFull, scroll.getScrollY());
                return super.scrolled(event, x, y, amountX, amountY);
            }
        });

        var channelLabel = new OwnLabel(I18n.msg("gui.galaxy.channels"), skin, "header");

        content.add(channelLabel).left().padBottom(pad20).row();
        content.add(scroll).left().row();
        content.pack();
        scroll.setScrollY(scrollY.get(entityFull, 0f));

        // THIRD: buttons.
        var buttonsTop = new Table(skin);
        var buttonsBottom = new Table(skin);

        // Add channel (full)
        var addChannelFull = newAddChannelButton(entityFull, "gui.galaxy.add.full", buttonWidth);
        buttonsTop.add(addChannelFull).center().padRight(pad34).padBottom(pad10);

        // Add channel (half)
        var addChannelHalf = newAddChannelButton(entityHalf, "gui.galaxy.add.half", buttonWidth);
        buttonsTop.add(addChannelHalf).center().padBottom(pad10).row();

        // Generate
        var generate = new OwnTextIconButton(I18n.msg("gui.galaxy.generate"), skin, "generate");
        generate.setColor(ColorUtils.gGreenC);
        generate.setWidth(buttonWidth);
        generate.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                var bbFull = Mapper.billboardSet.get(entityFull);
                if (morphologyBox != null &&
                        morphologyChanged) {
                    // Regenerate full dataset!
                    generateRandom(morphology, bb.seed);
                    morphologyChanged = false;
                } else {
                    regenerate();
                }
            }
        });
        generate.pad(pad10, pad20, pad10, pad20);
        generate.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.generate.info"), skin));
        var export = new OwnTextIconButton("", skin, "export");
        export.pad(pad10, pad20, pad10, pad20);
        export.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.export.info"), skin));
        export.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                var json = gen.convertToJson(entityFull, entityHalf);
                var string = json.toJson(JsonWriter.OutputType.json);
                Gdx.app.getClipboard().setContents(string);
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.galaxy.export.done"));
            }
        });
        var genTable = new Table(skin);
        genTable.add(generate).left().padRight(pad20);
        genTable.add(export);

        buttonsTop.add(genTable).colspan(2).center();

        // Random galaxy
        var randomize = new OwnTextIconButton(I18n.msg("gui.galaxy.randomize", morphologyBox.getSelected().name()), skin, "random");
        randomize.setColor(ColorUtils.gYellowC);
        randomize.setWidth(buttonWidth - 80f);
        randomize.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.randomize.info"), skin));
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // New seed, same morphology.
                var seedValue = StdRandom.uniform(999999);
                bb.seed = seedValue;
                generateRandom(bb.morphology, seedValue);
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);
        buttonsBottom.add(randomize).center().padBottom(pad10).row();


        content.add(buttonsTop).center().padTop(pad34).padBottom(pad20).row();
        content.add(new Separator(skin, "gray")).fillX().expandX().padBottom(pad20).row();
        content.add(buttonsBottom).center();

    }

    private void generateRandom(final GalaxyMorphology gm, final long seed) {
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

    private OwnTextIconButton newAddChannelButton(Entity entity, String key, float buttonWidth) {
        var button = new OwnTextIconButton(I18n.msg(key), skin, "new");
        button.setWidth(buttonWidth);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                GaiaSky.postRunnable(() -> {
                    var bb = Mapper.billboardSet.get(entity);
                    var ds = bb.datasets;
                    var newDs = new BillboardDataset[ds.length + 1];
                    System.arraycopy(ds, 0, newDs, 0, ds.length);

                    var dataset = new BillboardDataset();
                    dataset.setType(ChannelType.POINT);
                    dataset.setLayers(new int[]{0, 1, 2});
                    dataset.setBaseColor(new double[]{0.8, 0.8, 0.8});
                    dataset.setMaxSize(20.0);
                    newDs[ds.length] = dataset;

                    bb.datasets = newDs;

                    rebuild();
                });
            }
        });
        button.pad(pad10, pad20, pad10, pad20);
        button.addListener(new OwnTextTooltip(I18n.msg(key + ".info"), skin));
        return button;
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
        final var cpSize = 32f;
        var render = Mapper.render.get(entity);
        var billboard = Mapper.billboardSet.get(entity);
        var datasets = billboard.datasets;
        var half = render.halfResolutionBuffer;
        int channel = 0;
        for (var ds : datasets) {
            var dsTable = new Table(skin);
            // Type
            OwnSelectBox<ComboBoxBean<ChannelType>> type = new OwnSelectBox<>(skin);
            type.setItems(ComboBoxBean.getValues(ChannelType.class));
            type.setWidth(halfWidthBox);
            type.setSelectedIndex(ds.type.ordinal());
            type.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.type = type.getSelected().value;
                    if (ds.type == ChannelType.DUST) {
                        GaiaSky.postRunnable(() -> {
                            // Subtractive blending.
                            ds.setBlending(BlendMode.SUBTRACTIVE);
                            ds.setDepthMask(false);
                        });
                    }
                    rebuild();
                }
            });
            OwnLabel typeLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.type"), skin);
            typeLabel.setWidth(halfWidthBox);

            dsTable.add(typeLabel).left().padRight(pad20).padBottom(pad18);
            dsTable.add(type).left().padBottom(pad18).row();

            // Distribution
            OwnSelectBox<ComboBoxBean<Distribution>> distribution = new OwnSelectBox<>(skin);
            distribution.setItems(ComboBoxBean.getValues(Distribution.class));
            distribution.setWidth(halfWidthBox);
            distribution.setSelectedIndex(ds.distribution.ordinal());
            distribution.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.distribution = distribution.getSelected().value;
                    rebuild();
                    regenerate();
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
            var nParticles = new OwnSliderReset(I18n.msg("gui.galaxy.ds.particles"), nMin, nMax, nStep, nMin + (nMax - nMin) / 2, skin);
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
            var x = new OwnSliderReset(I18n.msg("gui.galaxy.ds.x"), transMin, transMax, transStep, 0f, skin);
            x.setWidth(thirdWidthBox);
            x.setValue(ds.translation.x);
            x.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.x = x.getValue();
                }
            });
            var y = new OwnSliderReset(I18n.msg("gui.galaxy.ds.y"), transMin, transMax, transStep, 0f, skin);
            y.setWidth(thirdWidthBox);
            y.setValue(ds.translation.y);
            y.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.translation.y = y.getValue();
                }
            });
            var z = new OwnSliderReset(I18n.msg("gui.galaxy.ds.z"), transMin, transMax, transStep, 0f, skin);
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
            translationTable.add(x).left().padRight(pad5);
            translationTable.add(y).left().padRight(pad5);
            translationTable.add(z).left();
            dsTable.add(translationTable).colspan(2).left().padBottom(pad18).row();

            // Dataset rotation
            float rotMin = -180.0f;
            float rotMax = 180.0f;
            float rotStep = (rotMax - rotMin) / SLIDER_STEPS;
            var rx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rx"), rotMin, rotMax, rotStep, 0f, skin);
            rx.setWidth(thirdWidthBox);
            rx.setValue(ds.rotation.x);
            rx.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.x = rx.getValue();
                }
            });
            var ry = new OwnSliderReset(I18n.msg("gui.galaxy.ds.ry"), rotMin, rotMax, rotStep, 0f, skin);
            ry.setWidth(thirdWidthBox);
            ry.setValue(ds.rotation.y);
            ry.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.rotation.y = ry.getValue();
                }
            });
            var rz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.rz"), rotMin, rotMax, rotStep, 0f, skin);
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
            rotTable.add(rx).left().padRight(pad5);
            rotTable.add(ry).left().padRight(pad5);
            rotTable.add(rz).left();
            dsTable.add(rotTable).colspan(2).left().padBottom(pad18).row();

            // Dataset scale
            float sMin = -3.0f;
            float sMax = 3.0f;
            float sStep = (sMax - sMin) / SLIDER_STEPS;
            var sx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.sx"), sMin, sMax, sStep, 1f, skin);
            sx.setWidth(thirdWidthBox);
            sx.setValue(ds.scale.x);
            sx.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.x = sx.getValue();
                }
            });
            var sy = new OwnSliderReset(I18n.msg("gui.galaxy.ds.sy"), sMin, sMax, sStep, 1f, skin);
            sy.setWidth(thirdWidthBox);
            sy.setValue(ds.scale.y);
            sy.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.y = sy.getValue();
                }
            });
            var sz = new OwnSliderReset(I18n.msg("gui.galaxy.ds.sz"), sMin, sMax, sStep, 1f, skin);
            sz.setWidth(thirdWidthBox);
            sz.setValue(ds.scale.z);
            sz.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.scale.z = sz.getValue();
                }
            });
            var sclTable = new Table(skin);
            sclTable.add(sx).left().padRight(pad5);
            sclTable.add(sy).left().padRight(pad5);
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
            var colorsTable = new Table(skin);
            colorsTable.add(cLabel).left().padRight(pad34);
            colorsTable.add(c1).size(cpSize).left().padRight(pad10);
            colorsTable.add(c2).size(cpSize).left().padRight(pad10);
            colorsTable.add(c3).size(cpSize).left().padRight(pad10);
            colorsTable.add(c4).size(cpSize).left().padRight(pad10);
            var colorNoise = new OwnSliderReset(I18n.msg("gui.galaxy.ds.color.noise"), 0.0f, 1f, 0.01f, 0f, skin);
            colorNoise.setWidth(halfWidthBox);
            colorNoise.setValue(ds.colorNoise);
            colorNoise.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setColorNoise((double) colorNoise.getValue());
                }
            });
            dsTable.add(colorsTable).left().padRight(pad5).padBottom(pad18);
            dsTable.add(colorNoise).left().padBottom(pad18).row();

            if (ds.distribution.isFlat()) {
                // Warp
                var warp = new OwnSliderReset(I18n.msg("gui.galaxy.ds.warp"), ds.type.warpStrength[0], ds.type.warpStrength[1], 0.001f, 0f, skin);
                warp.setWidth(fullWidthBox);
                warp.setValue(ds.warpStrength);
                warp.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setWarpStrength((double) warp.getValue());
                    }
                });
                dsTable.add(warp).colspan(2).left().padBottom(pad18).row();

                // Height scale
                var heightScale = new OwnSliderReset(I18n.msg("gui.galaxy.ds.height.scale"), 0.0f, 0.2f, 0.001f, 0.01f, skin);
                heightScale.setWidth(halfWidthBox);
                heightScale.setValue(ds.heightScale);
                heightScale.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setHeightScale((double) heightScale.getValue());
                    }
                });

                // Height profile
                OwnSelectBox<HeightProfile> heightProfile = new OwnSelectBox<>(skin);
                heightProfile.setItems(HeightProfile.values());
                heightProfile.setWidth(halfWidthBox);
                heightProfile.setSelected(ds.heightProfile);
                heightProfile.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.heightProfile = heightProfile.getSelected();
                    }
                });
                OwnLabel heightProfileLabel = new OwnLabel(I18n.msg("gui.galaxy.ds.height.profile"), skin);
                heightProfileLabel.setWidth(halfWidthBox);

                dsTable.add(heightProfile).left().padBottom(pad18).padRight(pad5);
                dsTable.add(heightScale).left().padBottom(pad18).row();

            }

            // Bar only has base_radius. All the others have also min_radius.
            // Min and base radii
            float minRadMin = ds.type.minRadius[0];
            float minRadMax = ds.type.minRadius[1];
            float minRadStep = (minRadMax - minRadMin) / SLIDER_STEPS;
            var minRadius = new OwnSliderReset(I18n.msg("gui.galaxy.ds.radius.min"),
                                               minRadMin,
                                               minRadMax,
                                               minRadStep,
                                               minRadMin + (minRadMax - minRadMin) / 2f,
                                               skin);
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
            var baseRadius = new OwnSliderReset(I18n.msg("gui.galaxy.ds.radius.base"),
                                                baseRadMin,
                                                baseRadMax,
                                                baseRadStep,
                                                baseRadMin + (baseRadMax - baseRadMin) / 2f,
                                                skin);
            baseRadius.setWidth(ds.distribution.isBar() ? fullWidthBox : halfWidthBox);
            baseRadius.setValue(ds.baseRadius);
            baseRadius.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setBaseRadius((double) baseRadius.getValue());
                }
            });
            if (!ds.distribution.isBar()) {
                dsTable.add(minRadius).left().padRight(pad5).padBottom(pad18);
                dsTable.add(baseRadius).left().padBottom(pad18).row();
            } else {
                dsTable.add(baseRadius).colspan(2).left().padBottom(pad18).row();
            }

            // Particle size and size noise
            float sizeMin = ds.type.size[0];
            float sizeMax = ds.type.size[1];
            var size = new OwnSliderReset(I18n.msg("gui.galaxy.ds.size"), sizeMin, sizeMax, true, sizeMin + (sizeMax - sizeMin) / 2f, skin);
            size.setNumberFormatter(new DecimalFormat("######0.########"));
            size.setDisplayValueMapped(true);
            size.setLogarithmicExponent(4.0);
            size.setWidth(thirdWidthBox + 115f);
            size.setMappedValue(ds.size);
            size.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.setSize((double) size.getMappedValue());
                }
            });
            var sizeMask = new OwnCheckBox(I18n.msg("gui.galaxy.ds.size.perlin"), skin, 4f);
            sizeMask.setChecked(ds.sizeMask);
            sizeMask.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    ds.setSizeMask(sizeMask.isChecked());
                    if (ds.sizeMask) {
                        ds.sizeNoise = 20f;
                    } else {
                        ds.sizeNoise = 0.2f;
                    }
                    GaiaSky.postRunnable(() -> {
                        rebuild();
                    });
                }
            });
            OwnSliderReset sizeNoise;
            if (ds.sizeMask) {
                sizeNoise = new OwnSliderReset(I18n.msg("gui.galaxy.ds.size.scale"), 1.0f, 50f, 0.01f, 0f, skin);
            } else {
                sizeNoise = new OwnSliderReset(I18n.msg("gui.galaxy.ds.size.noise"), 0.0f, 1f, 0.01f, 10f, skin);
            }
            sizeNoise.setWidth(thirdWidthBox);
            sizeNoise.setValue(ds.sizeNoise);
            sizeNoise.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    ds.sizeNoise = sizeNoise.getValue();
                }
            });
            var sizeTable = new Table(skin);
            sizeTable.add(size).left().padRight(pad34);
            sizeTable.add(sizeMask).right().padRight(pad5);
            sizeTable.add(sizeNoise).left().padRight(pad5);

            dsTable.add(sizeTable).colspan(2).left().padBottom(pad18).row();

            // Intensity
            float iMin = ds.type.intensity[0];
            float iMax = ds.type.intensity[1];
            float iStep = (iMax - iMin) / SLIDER_STEPS;
            var intensity = new OwnSliderReset(I18n.msg("gui.galaxy.ds.intensity"), iMin, iMax, iStep, iMin + (iMax - iMin) / 2f, skin);
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

            if (ds.distribution.isLogarithmicSpiral()) {
                // Number of arms
                var numArms = new OwnSliderReset(I18n.msg("gui.galaxy.ds.arm.number"), 1f, 8f, 1f, 4f, skin);
                numArms.setNumberFormatter(new DecimalFormat("#0"));
                numArms.setWidth(halfWidthBox);
                numArms.setValue(ds.numArms);
                numArms.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setSpiralArms((long) numArms.getValue());
                    }
                });
                // Arm sigma
                float minSigma = ds.type.armSigma[0];
                float maxSigma = ds.type.armSigma[1];
                float stepSigma = (maxSigma - minSigma) / SLIDER_STEPS;
                var armSigma = new OwnSliderReset(I18n.msg("gui.galaxy.ds.arm.sigma"), minSigma, maxSigma, stepSigma, 0.2f, skin);
                armSigma.setNumberFormatter(new DecimalFormat("#0.####"));
                armSigma.setWidth(halfWidthBox);
                armSigma.setValue(ds.armSigma);
                armSigma.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setArmSigma((double) armSigma.getValue());
                    }
                });

                dsTable.add(numArms).left().padRight(pad5).padBottom(pad18);
                dsTable.add(armSigma).left().padBottom(pad18).row();
            }
            if (ds.distribution.isDensityWaveSpiral()) {
                // Number of arms
                var numArms = new OwnSliderReset(I18n.msg("gui.galaxy.ds.arm.number"), 2f, 4f, 2f, 4f, skin);
                numArms.setNumberFormatter(new DecimalFormat("#0"));
                numArms.setWidth(fullWidthBox);
                numArms.setValue(ds.numArms);
                numArms.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setSpiralArms((long) numArms.getValue());
                    }
                });
                dsTable.add(numArms).colspan(2).left().padBottom(pad18).row();
            }

            if (ds.distribution.isAnySpiral()) {
                // Spiral angle
                float angleMin = ds.type.baseAngle[0];
                float angleMax = ds.type.baseAngle[1];
                var baseAngle = new OwnSliderReset(I18n.msg("gui.galaxy.ds.angle"),
                                                   angleMin,
                                                   angleMax,
                                                   true,
                                                   angleMin + (angleMax - angleMin) / 2f,
                                                   skin);
                baseAngle.setLogarithmicExponent(1.2);
                baseAngle.setNumberFormatter(new DecimalFormat("##0.###"));
                baseAngle.setWidth(fullWidthBox);
                baseAngle.setValue(ds.baseAngle);
                baseAngle.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setBaseAngle((double) baseAngle.getValue());
                    }
                });
                dsTable.add(baseAngle).colspan(2).left().padBottom(pad18).row();
            }

            if (ds.distribution.isDensityWaveSpiral()) {
                // Single eccentricity.
                float eMin = ds.type.eccentricity[0];
                float eMax = ds.type.eccentricity[1];
                float eStep = (eMax - eMin) / SLIDER_STEPS;
                var ec = new OwnSliderReset(I18n.msg("gui.galaxy.ds.eccentricity"), eMin, eMax, eStep, 0.3f, skin);
                ec.setNumberFormatter(new DecimalFormat("0.####"));
                ec.setWidth(fullWidthBox);
                ec.setValue(ds.eccentricity[0]);
                ec.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setEccentricity((double) ec.getValue());
                    }
                });
                dsTable.add(ec).colspan(2).left().padBottom(pad18).row();
            } else if (ds.distribution.isEllipsoid()) {
                // Eccentricity in X and Y.
                float eMin = ds.type.eccentricity[0];
                float eMax = ds.type.eccentricity[1];
                float eStep = (eMax - eMin) / SLIDER_STEPS;
                var ecx = new OwnSliderReset(I18n.msg("gui.galaxy.ds.eccentricity.x"), eMin, eMax, eStep, 0.0f, skin);
                ecx.setNumberFormatter(new DecimalFormat("0.####"));
                ecx.setWidth(halfWidthBox);
                ecx.setValue(ds.eccentricity[0]);
                ecx.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setEccentricityX((double) ecx.getValue());
                    }
                });
                var ecy = new OwnSliderReset(I18n.msg("gui.galaxy.ds.eccentricity.y"), eMin, eMax, eStep, 0.0f, skin);
                ecy.setNumberFormatter(new DecimalFormat("0.####"));
                ecy.setWidth(halfWidthBox);
                ecy.setValue(ds.eccentricity[1]);
                ecy.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setEccentricityY((double) ecy.getValue());
                    }
                });
                dsTable.add(ecy).left().padRight(pad5).padBottom(pad18);
                dsTable.add(ecx).left().padBottom(pad18).row();
            }

            if (ds.distribution.isDensityWaveSpiral()) {
                // Displacement in x and y
                float dMin = ds.type.spiralDeltaPos[0];
                float dMax = ds.type.spiralDeltaPos[1];
                float dStep = (dMax - dMin) / SLIDER_STEPS;
                var deltaX = new OwnSliderReset(I18n.msg("gui.galaxy.ds.delta.x"), dMin, dMax, dStep, 0f, skin);
                deltaX.setWidth(halfWidthBox);
                deltaX.setValue(ds.spiralDeltaPos[0]);
                deltaX.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.spiralDeltaPos[0] = deltaX.getValue();
                    }
                });
                var deltaY = new OwnSliderReset(I18n.msg("gui.galaxy.ds.delta.y"), dMin, dMax, dStep, 0f, skin);
                deltaY.setWidth(halfWidthBox);
                deltaY.setValue(ds.spiralDeltaPos[1]);
                deltaY.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.spiralDeltaPos[1] = deltaY.getValue();
                    }
                });
                dsTable.add(deltaX).left().padRight(pad5).padBottom(pad18);
                dsTable.add(deltaY).left().padBottom(pad18).row();
            }

            if (ds.distribution.isBar()) {
                // Aspect
                float aspectMin = ds.type.aspect[0];
                float aspectMax = ds.type.aspect[1];
                float aspectStep = (aspectMax - aspectMin) / SLIDER_STEPS;
                var aspect = new OwnSliderReset(I18n.msg("gui.galaxy.ds.aspect"), aspectMin, aspectMax, aspectStep, 1f, skin);
                aspect.setNumberFormatter(new DecimalFormat("#0.###"));
                aspect.setWidth(fullWidthBox);
                aspect.setValue(ds.aspect);
                aspect.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        ds.setAspect((double) aspect.getMappedValue());
                    }
                });
                dsTable.add(aspect).colspan(2).left().padBottom(pad18).row();
            }

            // Delete dataset (top icon)
            var delete = new OwnTextIconButton("", skin, "rubbish");
            delete.addListener(new OwnTextTooltip(I18n.msg("gui.galaxy.delete"), skin));
            delete.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    var e = half ? entityHalf : entityFull;
                    var bb = Mapper.billboardSet.get(e);
                    bb.removeDataset(ds);
                    EventManager.publish(Event.GPU_DISPOSE_BILLBOARD_DATASET, this, Mapper.render.get(e));
                    rebuild();
                }
            });

            // Header
            String key = "gui.galaxy.channel." + (half ? "half" : "full");
            String name = ds.type.name();
            String title = I18n.msg(key, channel, name);
            CollapsiblePane groupPane = new CollapsiblePane(stage, title,
                                                            dsTable, tabContentWidth, skin, "header-s", "expand-collapse",
                                                            null, expandedDatasets.contains(ds), null, delete);
            // Update set of expanded datasets whenever this group pane is expanded or collapsed.
            groupPane.setExpandCollapseRunnable(() -> {
                var expanded = groupPane.isExpanded();
                if (expanded) {
                    expandedDatasets.add(ds);
                } else {
                    expandedDatasets.remove(ds);
                }
            });

            datasetsTable.add(groupPane).padBottom(pad18).row();
            channel++;
        }
    }

    private void commitRotation(float yaw, float pitch, float roll) {
        m.setFromEulerAngles(pitch, yaw, roll);
        var full = Mapper.transform.get(entityFull);
        var half = Mapper.transform.get(entityHalf);
        if (full.matrix == null) {
            full.matrix = new Matrix4D();
        }
        full.matrix.set(m);
        if (half.matrix == null) {
            half.matrix = new Matrix4D();
        }
        half.matrix.set(m);
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

    private final FocusView eventView = new FocusView();

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.FOCUS_CHANGED) {
            if (data[0] instanceof String) {
                eventView.setEntity(scene.getEntity((String) data[0]));
            } else if (data[0] instanceof FocusView fv) {
                eventView.setEntity(fv.getEntity());
            }
            if (eventView.isBillboardDataset()) {
                reinitialize(eventView);
            }
        }
    }

}
