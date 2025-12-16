/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.postprocess.filters.NoiseFilter.NoiseType;
import gaiasky.scene.Mapper;
import gaiasky.scene.record.*;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;
import net.jafama.FastMath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.function.Function;

/**
 * Interface to the procedural generation system for planetary surfaces, atmospheres and cloud layers.
 */
public class ProceduralGenerationWindow extends GenericDialog implements IObserver {
    private static final Log logger = Logger.getLogger(ProceduralGenerationWindow.class);
    // Selected tab persists across windows
    private static int lastTabSelected = 0;

    private Entity target;
    private final FocusView view;
    private final Random rand;
    private MaterialComponent initMtc, mtc;
    private CloudComponent initClc, clc;
    private AtmosphereComponent initAc, ac;
    private float fieldWidth, fieldWidthAll, fieldWidthTotal, textWidth, scrollPaneHeight;
    private boolean updateTabSelected = true;
    private OwnSliderPlus hueShift;
    private Texture currentLutTexture;
    private Cell<?> lutImageCell;
    private OwnTextButton genCloudsButton, genSurfaceButton;
    /**
     * The surface generation is enabled only when the model is <b>NOT</b> using cubemap textures.
     **/
    private boolean surfaceEnabled;

    private int genCloudNum = 0, genSurfaceNum = 0;

    public ProceduralGenerationWindow(FocusView target,
                                      Stage stage,
                                      Skin skin) {
        super(I18n.msg("gui.procedural.title", target.getLocalizedName()), skin, stage);
        this.target = target.getEntity();
        this.view = new FocusView(target.getEntity());
        this.initMtc = Mapper.model.get(this.target).model.mtc;
        this.initClc = Mapper.cloud.get(this.target).cloud;
        this.initAc = Mapper.atmosphere.get(this.target).atmosphere;
        this.rand = new Random(1884L);
        this.setModal(false);
        this.surfaceEnabled = initMtc.diffuseCubemap == null &&
                initMtc.specularCubemap == null &&
                initMtc.heightCubemap == null &&
                initMtc.emissiveCubemap == null;

        EventManager.instance.subscribe(this, Event.PROCEDURAL_GENERATION_CLOUD_INFO,
                                        Event.PROCEDURAL_GENERATION_SURFACE_INFO,
                                        Event.FOCUS_CHANGED);

        setAcceptText(I18n.msg("gui.close"));

        // Build UI
        buildSuper();

    }

    private void reinitialize(Entity target) {
        this.target = target;
        this.view.setEntity(target);
        this.initMtc = Mapper.model.get(this.target).model.mtc;
        this.initClc = Mapper.cloud.get(this.target).cloud;
        this.initAc = Mapper.atmosphere.get(this.target).atmosphere;
        this.setModal(false);
        this.surfaceEnabled = initMtc.diffuseCubemap == null &&
                initMtc.specularCubemap == null &&
                initMtc.heightCubemap == null &&
                initMtc.emissiveCubemap == null;

        this.getTitleLabel().setText(I18n.msg("gui.procedural.title", view.getLocalizedName()));

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
        this.scrollPaneHeight = 600f;
        float tabContentWidth = 400f;
        float tabWidth = 240f;

        // Create the tab buttons
        HorizontalGroup group = new HorizontalGroup();
        group.align(Align.left);

        // SURFACE TAB
        final Button tabSurface = this.surfaceEnabled ? new OwnTextButton(I18n.msg("gui.procedural.surface"), skin, "toggle-big") : null;
        if (this.surfaceEnabled) {
            tabSurface.pad(pad10);
            tabSurface.setWidth(tabWidth);
            group.addActor(tabSurface);
        }

        // CLOUDS TAB
        final Button tabClouds = new OwnTextButton(I18n.msg("gui.procedural.cloud"), skin, "toggle-big");
        tabClouds.pad(pad10);
        tabClouds.setWidth(tabWidth);
        group.addActor(tabClouds);

        // ATMOSPHERE TAB
        final Button tabAtmosphere = new OwnTextButton(I18n.msg("gui.procedural.atmosphere"), skin, "toggle-big");
        tabAtmosphere.pad(pad10);
        tabAtmosphere.setWidth(tabWidth);
        group.addActor(tabAtmosphere);

        content.add(group).left().row();

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();

        // SURFACE
        final Table contentSurface = this.surfaceEnabled ? new Table(skin) : null;
        if (this.surfaceEnabled) {
            contentSurface.setWidth(tabContentWidth);
            contentSurface.align(Align.top | Align.left);
            buildContentSurface(contentSurface);
            tabContent.addActor(contentSurface);
        }

        // CLOUDS
        final Table contentClouds = new Table(skin);
        contentClouds.setWidth(tabContentWidth);
        contentClouds.align(Align.top | Align.left);
        buildContentClouds(contentClouds);
        tabContent.addActor(contentClouds);

        // ATMOSPHERE
        final Table contentAtmosphere = new Table(skin);
        contentAtmosphere.setWidth(tabContentWidth);
        contentAtmosphere.align(Align.top | Align.left);
        buildContentAtmosphere(contentAtmosphere);
        tabContent.addActor(contentAtmosphere);

        content.add(tabContent).padTop(pad34).padBottom(pad34).expand().fill().row();

        // Listen to changes in the tab button checked states
        // Set visibility of the tab content to match the checked state
        ChangeListener tabListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                if (contentSurface != null && tabSurface != null) {
                    contentSurface.setVisible(tabSurface.isChecked());
                }
                contentClouds.setVisible(tabClouds.isChecked());
                contentAtmosphere.setVisible(tabAtmosphere.isChecked());
                if (updateTabSelected) {
                    if (tabSurface != null && tabSurface.isChecked())
                        lastTabSelected = 0;
                    else if (tabClouds.isChecked())
                        lastTabSelected = surfaceEnabled ? 1 : 0;
                    else if (tabAtmosphere.isChecked())
                        lastTabSelected = surfaceEnabled ? 2 : 1;
                }
            }
        };
        if (tabSurface != null)
            tabSurface.addListener(tabListener);
        tabClouds.addListener(tabListener);
        tabAtmosphere.addListener(tabListener);

        // Let only one tab button be checked at a time
        ButtonGroup<Button> tabs = new ButtonGroup<>();
        tabs.setMinCheckCount(1);
        tabs.setMaxCheckCount(1);
        updateTabSelected = false;

        if (tabSurface != null)
            tabs.add(tabSurface);
        tabs.add(tabClouds);
        tabs.add(tabAtmosphere);
        updateTabSelected = true;

        var selectedTab = lastTabSelected;
        if (selectedTab >= tabs.getButtons().size) {
            selectedTab = 0;
        }
        tabs.setChecked(((TextButton) tabs.getButtons().get(selectedTab)).getText().toString());

        // Randomize button
        OwnTextButton randomize = new OwnTextIconButton(I18n.msg("gui.procedural.randomize", I18n.msg("gui.procedural.all")), skin, "random", "big");
        randomize.setColor(ColorUtils.gYellowC);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                randomizeAll();
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);

        content.add(randomize).center().padBottom(pad34).row();

        // Resolution
        OwnSliderPlus pgResolution = new OwnSliderPlus(I18n.msg("gui.ui.procedural.resolution"),
                                                       Constants.PG_RESOLUTION_MIN,
                                                       Constants.PG_RESOLUTION_MAX,
                                                       1,
                                                       skin);
        pgResolution.setValueLabelTransform((value) -> value.intValue() * 2 + "x" + value.intValue());
        pgResolution.setWidth(fieldWidthTotal + 50f);
        pgResolution.setValue(Settings.settings.graphics.proceduralGenerationResolution[1]);
        pgResolution.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                int pgHeight = (int) pgResolution.getValue();
                int pgWidth = pgHeight * 2;
                EventManager.publish(Event.PROCEDURAL_GENERATION_RESOLUTION_CMD, this, pgWidth, pgHeight);
            }
        });
        content.add(pgResolution).right().padBottom(pad18).row();

        // Save textures
        OwnCheckBox saveTextures = new OwnCheckBox(I18n.msg("gui.procedural.savetextures"), skin, pad10);
        saveTextures.setChecked(Settings.settings.program.saveProceduralTextures);
        saveTextures.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                EventManager.publish(Event.PROCEDURAL_GENERATION_SAVE_TEXTURES_CMD, this, saveTextures.isChecked());
            }
        });
        OwnImageButton saveTexturesTooltip = new OwnImageButton(skin, "tooltip");
        saveTexturesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.savetextures", SysUtils.getProceduralPixmapDir().toString()),
                                                           skin));
        HorizontalGroup saveTexturesGroup = new HorizontalGroup();
        saveTexturesGroup.space(pad10);
        saveTexturesGroup.addActor(saveTextures);
        saveTexturesGroup.addActor(saveTexturesTooltip);
        content.add(saveTexturesGroup).left();

    }

    private void addLocalButtons(Table content,
                                 Function<Boolean, Boolean> gasGiantFunc,
                                 Function<Boolean, Boolean> earthLikeFunc,
                                 Function<Boolean, Boolean> coldPlanetFunc,
                                 Function<Boolean, Boolean> rockyPlanetFunc
    ) {
        float w = 220f;

        // Gas giant
        OwnTextButton gasGiant = new OwnTextButton(I18n.msg("gui.procedural.button.gasgiant"), skin);
        gasGiant.setWidth(w);
        gasGiant.setColor(ColorUtils.gBlueC);
        gasGiant.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                gasGiantFunc.apply(true);
            }
        });
        gasGiant.pad(pad10, pad20, pad10, pad20);
        gasGiant.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.gasgiant"), skin));

        // Earth like
        OwnTextButton earthLike = new OwnTextButton(I18n.msg("gui.procedural.button.earthlike"), skin);
        earthLike.setWidth(w);
        earthLike.setColor(ColorUtils.gBlueC);
        earthLike.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                earthLikeFunc.apply(true);
            }
        });
        earthLike.pad(pad10, pad20, pad10, pad20);
        earthLike.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.earthlike"), skin));

        // Snow world
        OwnTextButton snowWorld = new OwnTextButton(I18n.msg("gui.procedural.button.snow"), skin);
        snowWorld.setWidth(w);
        snowWorld.setColor(ColorUtils.gBlueC);
        snowWorld.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                coldPlanetFunc.apply(true);
            }
        });
        snowWorld.pad(pad10, pad20, pad10, pad20);
        snowWorld.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.snow"), skin));

        // Rocky planet
        OwnTextButton rockyPlanet = new OwnTextButton(I18n.msg("gui.procedural.button.rocky"), skin);
        rockyPlanet.setWidth(w);
        rockyPlanet.setColor(ColorUtils.gBlueC);
        rockyPlanet.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                rockyPlanetFunc.apply(true);
            }
        });
        rockyPlanet.pad(pad10, pad20, pad10, pad20);
        rockyPlanet.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.rocky"), skin));

        Table bt = new Table(skin);
        bt.add(earthLike).pad(5f);
        bt.add(snowWorld).pad(5f);
        bt.add(rockyPlanet).pad(5f);
        bt.add(gasGiant).pad(5f);

        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad20);
        buttonGroup.addActor(bt);

        content.add(buttonGroup).center().colspan(2).padBottom(pad34).row();
    }

    private OwnTextButton addLocalButtons(Table content,
                                          String key,
                                          Function<Boolean, Boolean> generateFunc,
                                          Function<Boolean, Boolean> randomizeFunc,
                                          int colspan) {
        String name = I18n.msg(key);

        // Randomize button
        var randomize = new OwnTextIconButton(I18n.msg("gui.procedural.randomize", name), skin, "random");
        randomize.setColor(ColorUtils.gYellowC);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                randomizeFunc.apply(true);
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);
        randomize.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.button.randomize", name), skin));

        // Generate button
        var generate = new OwnTextIconButton(I18n.msg("gui.procedural.generate", name), skin, "generate");
        generate.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                generateFunc.apply(true);
            }
        });
        generate.pad(pad10, pad20, pad10, pad20);
        generate.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.button.generate", name), skin));

        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad20);
        buttonGroup.addActor(generate);
        buttonGroup.addActor(randomize);

        content.add(buttonGroup).center().colspan(colspan).padBottom(pad18).row();

        return generate;
    }

    private void addNoiseGroup(Table content,
                               NoiseComponent nc) {

        var fieldWidthNoise = fieldWidthAll + 250f;

        // Noise group table.
        Table noiseTable = new Table(skin);

        // Seed.
        FloatValidator lv = new FloatValidator(-10000f, 10000f);
        OwnTextField seedField = new OwnTextField(Float.toString(nc.seed), skin);
        seedField.setWidth(fieldWidth + 110f);
        seedField.setValidator(lv);
        seedField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.seed = seedField.getLongValue(1L);
            }
        });

        OwnLabel seedLabel = new OwnLabel(I18n.msg("gui.procedural.seed"), skin);
        seedLabel.setWidth(textWidth);
        OwnImageButton seedTooltip = new OwnImageButton(skin, "tooltip");
        seedTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.seed"), skin));
        noiseTable.add(seedLabel).left().padBottom(pad18).padRight(pad18);
        noiseTable.add(seedField).padBottom(pad18).padRight(pad10);
        noiseTable.add(seedTooltip).padBottom(pad18).row();

        // Noise type.
        OwnSelectBox<NoiseType> type = new OwnSelectBox<>(skin);
        type.setItems(NoiseType.values());
        type.setWidth(fieldWidth + 110f);
        type.setSelected(nc.type);
        type.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.type = type.getSelected();
            }
        });
        OwnLabel typeLabel = new OwnLabel(I18n.msg("gui.procedural.type"), skin);
        typeLabel.setWidth(textWidth);
        OwnImageButton typeTooltip = new OwnImageButton(skin, "tooltip");
        typeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.type"), skin));
        noiseTable.add(typeLabel).left().padBottom(pad18).padRight(pad18);
        noiseTable.add(type).padBottom(pad18).padRight(pad10);
        noiseTable.add(typeTooltip).padBottom(pad18).row();

        // Scale.
        OwnSliderPlus scaleX = new OwnSliderPlus(I18n.msg("gui.procedural.scale", "[x]"), 0.01f, 14.0f, 0.01f, skin);
        scaleX.setWidth(fieldWidthNoise / 3f - pad10 * 1.3f);
        scaleX.setValue((float) nc.scale[0]);
        scaleX.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.scale[0] = scaleX.getMappedValue();
            }
        });
        OwnSliderPlus scaleY = new OwnSliderPlus(I18n.msg("gui.procedural.scale", "[y]"), 0.01f, 14.0f, 0.01f, skin);
        scaleY.setWidth(fieldWidthNoise / 3f - pad10 * 1.3f);
        scaleY.setValue((float) nc.scale[1]);
        scaleY.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.scale[1] = scaleY.getMappedValue();
            }
        });
        OwnSliderPlus scaleZ = new OwnSliderPlus(I18n.msg("gui.procedural.scale", "[z]"), 0.01f, 14.0f, 0.01f, skin);
        scaleZ.setWidth(fieldWidthNoise / 3f - pad10 * 1.3f);
        scaleZ.setValue((float) nc.scale[2]);
        scaleZ.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.scale[2] = scaleZ.getMappedValue();
            }
        });

        HorizontalGroup scaleGroup = new HorizontalGroup();
        scaleGroup.space(pad10 * 2f);
        scaleGroup.addActor(scaleX);
        scaleGroup.addActor(scaleY);
        scaleGroup.addActor(scaleZ);
        OwnImageButton scaleTooltip = new OwnImageButton(skin, "tooltip");
        scaleTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.scale"), skin));
        noiseTable.add(scaleGroup).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(scaleTooltip).left().padBottom(pad18).row();

        // Amplitude.
        OwnSliderPlus amplitude = new OwnSliderPlus(I18n.msg("gui.procedural.amplitude"), 0.1f, 3.0f, 0.01f, skin);
        amplitude.setWidth(fieldWidthNoise);
        amplitude.setValue((float) nc.amplitude);
        amplitude.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.amplitude = amplitude.getMappedValue();
            }
        });
        OwnImageButton amplitudeTooltip = new OwnImageButton(skin, "tooltip");
        amplitudeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.amplitude"), skin));
        noiseTable.add(amplitude).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(amplitudeTooltip).left().padBottom(pad18).row();

        // Persistence.
        OwnSliderPlus persistence = new OwnSliderPlus(I18n.msg("gui.procedural.persistence"), 0.01f, 0.9f, 0.01f, skin);
        persistence.setWidth(fieldWidthNoise);
        persistence.setValue((float) nc.persistence);
        persistence.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.persistence = persistence.getMappedValue();
            }
        });
        OwnImageButton persistenceTooltip = new OwnImageButton(skin, "tooltip");
        persistenceTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.persistence"), skin));
        noiseTable.add(persistence).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(persistenceTooltip).left().padBottom(pad18).row();

        // Frequency.
        OwnSliderPlus frequency = new OwnSliderPlus(I18n.msg("gui.procedural.frequency"), 0.01f, 3.0f, 0.01f, skin);
        frequency.setWidth(fieldWidthNoise);
        frequency.setValue((float) nc.frequency);
        frequency.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.frequency = frequency.getMappedValue();
            }
        });
        OwnImageButton frequencyTooltip = new OwnImageButton(skin, "tooltip");
        frequencyTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.frequency"), skin));
        noiseTable.add(frequency).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(frequencyTooltip).left().padBottom(pad18).row();

        // Lacunarity.
        OwnSliderPlus lacunarity = new OwnSliderPlus(I18n.msg("gui.procedural.lacunarity"), 0.1f, 5.0f, 0.1f, skin);
        lacunarity.setWidth(fieldWidthNoise);
        lacunarity.setValue((float) nc.lacunarity);
        lacunarity.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.lacunarity = lacunarity.getMappedValue();
            }
        });
        OwnImageButton lacunarityTooltip = new OwnImageButton(skin, "tooltip");
        lacunarityTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.lacunarity"), skin));
        noiseTable.add(lacunarity).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(lacunarityTooltip).left().padBottom(pad18).row();

        // Octaves.
        OwnSliderPlus octaves = new OwnSliderPlus(I18n.msg("gui.procedural.octaves"), 1, 8, 1, skin);
        octaves.setWidth(fieldWidthNoise);
        octaves.setValue(nc.octaves);
        octaves.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.octaves = (int) octaves.getMappedValue();
            }
        });
        OwnImageButton octavesTooltip = new OwnImageButton(skin, "tooltip");
        octavesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.octaves"), skin));
        noiseTable.add(octaves).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(octavesTooltip).left().padBottom(pad18).row();

        // Terraces.
        OwnSliderPlus terraces = new OwnSliderPlus(I18n.msg("gui.procedural.terraces"), 0, 8, 1, skin);
        terraces.setWidth(fieldWidthNoise);
        terraces.setValue(nc.numTerraces);
        terraces.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.numTerraces = (int) terraces.getMappedValue();
            }
        });
        OwnImageButton terracesTooltip = new OwnImageButton(skin, "tooltip");
        terracesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.terraces"), skin));
        noiseTable.add(terraces).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(terracesTooltip).left().padBottom(pad18).row();

        // Terraces exponent.
        OwnSliderPlus terracesExp = new OwnSliderPlus(I18n.msg("gui.procedural.terraces.exp"), 1, 13, 1, skin);
        terracesExp.setWidth(fieldWidthNoise);
        terracesExp.setValue(nc.terracesExp);
        terracesExp.setValueLabelTransform((value) -> String.valueOf(value * 2.0 - 1.0));
        terracesExp.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.terracesExp = terracesExp.getMappedValue() * 2f - 1f;
            }
        });
        OwnImageButton terracesExpTooltip = new OwnImageButton(skin, "tooltip");
        terracesExpTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.terraces.exp"), skin));
        noiseTable.add(terracesExp).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(terracesExpTooltip).left().padBottom(pad18).row();

        // Range.
        OwnSliderPlus rangeMin = new OwnSliderPlus(I18n.msg("gui.procedural.range", "[min]"), -2f, 0.0f, 0.1f, skin);
        rangeMin.setWidth(fieldWidthNoise / 2f - pad10);
        rangeMin.setValue((float) nc.range[0]);
        rangeMin.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.range[0] = rangeMin.getMappedValue();
            }
        });
        OwnSliderPlus rangeMax = new OwnSliderPlus(I18n.msg("gui.procedural.range", "[max]"), 0.5f, 2.0f, 0.1f, skin);
        rangeMax.setWidth(fieldWidthNoise / 2f - pad10);
        rangeMax.setValue((float) nc.range[1]);
        rangeMax.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.range[1] = rangeMax.getMappedValue();
            }
        });

        HorizontalGroup rangeGroup = new HorizontalGroup();
        rangeGroup.space(pad10 * 2f);
        rangeGroup.addActor(rangeMin);
        rangeGroup.addActor(rangeMax);
        OwnImageButton rangeTooltip = new OwnImageButton(skin, "tooltip");
        rangeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.range"), skin));
        noiseTable.add(rangeGroup).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(rangeTooltip).left().padBottom(pad18).row();

        // Power.
        OwnSliderPlus power = new OwnSliderPlus(I18n.msg("gui.procedural.power"), 0.1f, 8f, 0.1f, skin);
        power.setWidth(fieldWidthNoise);
        power.setValue((float) nc.power);
        power.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.power = power.getMappedValue();
            }
        });
        OwnImageButton powerTooltip = new OwnImageButton(skin, "tooltip");
        powerTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.power"), skin));
        noiseTable.add(power).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(powerTooltip).left().padBottom(pad18).row();

        // Ridge and turbulence.
        OwnCheckBox turbulence = new OwnCheckBox(I18n.msg("gui.procedural.turbulence"), skin, pad10);
        OwnCheckBox ridge = new OwnCheckBox(I18n.msg("gui.procedural.ridge"), skin, pad10);

        // Turbulence.
        turbulence.setChecked(nc.turbulence);
        turbulence.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        nc.turbulence = turbulence.isChecked();
                        if (!nc.turbulence) {
                            nc.ridge = false;
                            ridge.setProgrammaticChangeEvents(false);
                            ridge.setChecked(false);
                            ridge.setProgrammaticChangeEvents(true);
                        }
                    }
                }
        );
        OwnImageButton turbulenceTooltip = new OwnImageButton(skin, "tooltip");
        turbulenceTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.turbulence"), skin));

        noiseTable.add(turbulence).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(turbulenceTooltip).left().padBottom(pad18).row();

        // Ridge.
        ridge.setChecked(nc.ridge);
        ridge.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        nc.ridge = ridge.isChecked();
                        if (nc.ridge) {
                            nc.turbulence = true;
                            turbulence.setProgrammaticChangeEvents(false);
                            turbulence.setChecked(true);
                            turbulence.setProgrammaticChangeEvents(true);
                        }
                    }
                }
        );
        OwnImageButton ridgeTooltip = new OwnImageButton(skin, "tooltip");
        ridgeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.ridge"), skin));

        noiseTable.add(ridge).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(ridgeTooltip).left().padBottom(pad18).row();


        content.add(new OwnLabel(I18n.msg("gui.procedural.noise.params"), skin, "hud-header")).left().padBottom(pad34).row();
        content.add(noiseTable).colspan(3).center().padBottom(pad34).row();
    }

    private void updateLutImage(Array<String> luts) {
        if (lutImageCell != null) {
            lutImageCell.clearActor();
            Pixmap p = new Pixmap(Settings.settings.data.dataFileHandle(luts.get(luts.indexOf(mtc.biomeLUT, false))));
            int w = p.getWidth();
            int h = p.getHeight();
            if (hueShift != null) {
                float hue = hueShift.getMappedValue();
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        Color col = new Color(p.getPixel(x, y));
                        float[] rgb = new float[]{col.r, col.g, col.b, 1f};
                        if (hue != 0) {
                            // Shift hue of lookup table by an amount in degrees
                            float[] hsb = ColorUtils.rgbToHsb(rgb);
                            hsb[0] = ((hsb[0] * 360f + hue) % 360f) / 360f;
                            rgb = ColorUtils.hsbToRgb(hsb);
                        }
                        col.set(rgb[0], rgb[1], rgb[2], 1f);
                        p.drawPixel(x, y, Color.rgba8888(col));
                    }
                }
            }
            Texture newLutTexture = new Texture(p);
            newLutTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Image img = new Image(newLutTexture);
            img.setScaling(Scaling.fill);
            lutImageCell.setActor(img);
            lutImageCell.size(260, 260);
            if (currentLutTexture != null) {
                currentLutTexture.dispose();
            }
            currentLutTexture = newLutTexture;
            p.dispose();
        }
    }

    private void buildContentSurface(Table content) {
        ModelComponent mc = Mapper.model.get(target).model;
        if (mc != null) {
            mtc = new MaterialComponent();
            if (initMtc == null) {
                // Generate random material
                mtc.randomizeAll(rand.nextLong());
            } else {
                // Copy existing
                mtc.copyFrom(initMtc);
            }
            // Title
            content.add(new OwnLabel(I18n.msg("gui.procedural.param.surface"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

            // Add button group with presets.
            addLocalButtons(content,
                            this::randomizeSurfaceGasGiant,
                            this::randomizeSurfaceEarthLike,
                            this::randomizeSurfaceColdPlanet,
                            this::randomizeSurfaceRockyPlanet);

            // Add generate and randomize buttons
            genSurfaceButton = addLocalButtons(content, "gui.procedural.surface", this::generateSurface, this::randomizeSurface, 2);

            content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad10).padTop(pad18).row();

            Table scrollContent = new Table(skin);

            // LUT
            Path dataPath = Settings.settings.data.dataPath("default-data/tex/lut");
            Array<String> lookUpTables = new Array<>();
            try (var stream = Files.list(dataPath)) {
                java.util.List<Path> l = stream.filter(f -> f.toString().endsWith("-lut.png") || f.toString().endsWith("-lut.jpg")).toList();
                var subPath = Path.of("default-data", "tex", "lut").toString();
                for (Path p : l) {
                    String name = p.toString();
                    lookUpTables.add(Constants.DATA_LOCATION_TOKEN + name.substring(name.indexOf(subPath)).replace("\\", "/"));
                }
            } catch (Exception ignored) {
            }
            if (lookUpTables.isEmpty()) {
                lookUpTables.add(Constants.DATA_LOCATION_TOKEN + "default-data/tex/lut/biome-lut.jpg");
                lookUpTables.add(Constants.DATA_LOCATION_TOKEN + "default-data/tex/lut/biome-smooth-lut.png");
            }
            OwnSelectBox<String> lookUpTablesBox = new OwnSelectBox<>(skin);
            lookUpTablesBox.setItems(lookUpTables);
            lookUpTablesBox.setWidth(fieldWidth + 80f);
            lookUpTablesBox.setSelected(mtc.biomeLUT);
            lookUpTablesBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.biomeLUT = lookUpTablesBox.getSelected();
                    updateLutImage(lookUpTables);
                }
            });

            OwnLabel lookUpTablesLabel = new OwnLabel(I18n.msg("gui.procedural.lut"), skin);
            lookUpTablesLabel.setWidth(textWidth);
            OwnImageButton lutTooltip = new OwnImageButton(skin, "tooltip");
            lutTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.lut"), skin));
            scrollContent.add(lookUpTablesLabel).left().padBottom(pad18).padRight(pad18);
            scrollContent.add(lookUpTablesBox).left().padBottom(pad18).padRight(pad10);
            scrollContent.add(lutTooltip).left().padBottom(pad18).row();
            lutImageCell = scrollContent.add();
            lutImageCell.colspan(3).padBottom(pad18).row();

            // Hue shift
            hueShift = new OwnSliderPlus(I18n.msg("gui.procedural.hueshift"), 0.0f, 360.0f, 0.1f, skin);
            hueShift.setWidth(fieldWidthTotal - 100f);
            hueShift.setValueSuffix("°");
            hueShift.setValue(mtc.biomeHueShift);
            hueShift.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.biomeHueShift = hueShift.getMappedValue();
                    updateLutImage(lookUpTables);
                }
            });
            OwnImageButton hueShiftTooltip = new OwnImageButton(skin, "tooltip");
            hueShiftTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.hueshift"), skin));
            scrollContent.add(hueShift).colspan(2).left().padBottom(pad34).padRight(pad10);
            scrollContent.add(hueShiftTooltip).left().padBottom(pad34).row();

            // Initial update
            updateLutImage(lookUpTables);

            // Height scale
            OwnSliderPlus heightScale = new OwnSliderPlus(I18n.msg("gui.procedural.heightscale"), 1.0f, 80.0f, 0.1f, skin);
            heightScale.setWidth(fieldWidthTotal - 100f);
            heightScale.setValueSuffix(" km");
            heightScale.setValue((float) (mtc.heightScale * Constants.U_TO_KM));
            heightScale.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.heightScale = (float) (heightScale.getMappedValue() * Constants.KM_TO_U);
                }
            });
            OwnImageButton heightScaleTooltip = new OwnImageButton(skin, "tooltip");
            heightScaleTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.heightscale"), skin));
            scrollContent.add(heightScale).colspan(2).left().padBottom(pad34).padRight(pad10);
            scrollContent.add(heightScaleTooltip).left().padBottom(pad34).row();

            // Generate emission.
            OwnCheckBox emission = new OwnCheckBox(I18n.msg("gui.procedural.emission"), skin, pad10);
            emission.setChecked(mtc.nc.genEmissiveMap);
            emission.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.nc.genEmissiveMap = emission.isChecked();
                }
            });
            OwnImageButton emissionTooltip = new OwnImageButton(skin, "tooltip");
            emissionTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.emission"), skin));
            scrollContent.add(emission).colspan(2).left().padBottom(pad34).padRight(pad10);
            scrollContent.add(emissionTooltip).left().padBottom(pad34).row();

            // Noise
            addNoiseGroup(scrollContent, mtc.nc);

            var scrollPane = scrollPane(scrollContent);

            content.add(scrollPane).colspan(2).center().top().row();
            content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).padTop(pad10).row();

        } else {
            // Error!
            OwnLabel l = new OwnLabel(I18n.msg("gui.procedural.nomodel", view.getName()), skin);
            content.add(l).pad(pad34).center();
        }
    }

    private void buildContentClouds(Table content) {
        clc = new CloudComponent();
        if (initClc == null) {
            // Generate random
            clc.randomizeAll(rand.nextLong(), view.getSize());
        } else {
            // Copy existing
            clc.copyFrom(initClc);
            clc.setDiffuse("generate");
        }
        // Title
        content.add(new OwnLabel(I18n.msg("gui.procedural.param.cloud"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

        // Add button group
        genCloudsButton = addLocalButtons(content, "gui.procedural.cloud", this::generateClouds, this::randomizeClouds, 2);

        content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).padTop(pad18).row();

        Table scrollContent = new Table(skin);

        // Cloud color
        ColorPicker cloudColor = new ColorPicker(new float[]{clc.color[0], clc.color[1], clc.color[2], clc.color[3]}, stage, skin);
        cloudColor.setSize(128f, 128f);
        cloudColor.setNewColorRunnable(() -> {
            float[] col = cloudColor.getPickedColor();
            clc.color[0] = col[0];
            clc.color[1] = col[1];
            clc.color[2] = col[2];
            clc.color[3] = col[3];
        });

        OwnLabel cloudColorLabel = new OwnLabel(I18n.msg("gui.procedural.cloudcolor"), skin);
        cloudColorLabel.setWidth(textWidth);
        OwnImageButton cloudColorTooltip = new OwnImageButton(skin, "tooltip");
        cloudColorTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.cloudcolor"), skin));
        HorizontalGroup cloudGroup = new HorizontalGroup();
        cloudGroup.space(pad20);
        cloudGroup.addActor(cloudColor);
        cloudGroup.addActor(cloudColorTooltip);
        scrollContent.add(cloudColorLabel).left().padRight(pad18).padBottom(pad18);
        scrollContent.add(cloudGroup).left().expandX().padBottom(pad18).padRight(pad10).row();

        // Noise
        addNoiseGroup(scrollContent, clc.nc);

        var scrollPane = scrollPane(scrollContent);

        content.add(scrollPane).colspan(2).center().top().row();
        content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).row();
    }

    private void buildContentAtmosphere(Table content) {
        ac = new AtmosphereComponent();
        if (initAc == null) {
            // Generate random
            ac.randomizeAll(rand.nextLong(), view.getRadius());
        } else {
            // Copy existing
            ac.copyFrom(initAc);
        }
        // Title
        content.add(new OwnLabel(I18n.msg("gui.procedural.param.atm"), skin, "hud-header")).colspan(3).left().padBottom(pad34).row();

        // Add button group
        addLocalButtons(content, "gui.procedural.atmosphere", this::generateAtmosphere, this::randomizeAtmosphere, 3);

        content.add(new Separator(skin, "gray")).center().colspan(3).growX().padBottom(pad34).padTop(pad18).row();

        // Wavelengths
        OwnSliderPlus wavelength0 = new OwnSliderPlus(I18n.msg("gui.procedural.wavelength", "0"), 0.4f, 1.0f, 0.01f, skin);
        wavelength0.setWidth(fieldWidthTotal / 3f - pad10 * 1.3f);
        wavelength0.setValue((float) ac.wavelengths[0]);
        wavelength0.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.wavelengths[0] = wavelength0.getMappedValue();
            }
        });
        OwnSliderPlus wavelength1 = new OwnSliderPlus(I18n.msg("gui.procedural.wavelength", "1"), 0.4f, 1.0f, 0.01f, skin);
        wavelength1.setWidth(fieldWidthTotal / 3f - pad10 * 1.3f);
        wavelength1.setValue((float) ac.wavelengths[1]);
        wavelength1.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.wavelengths[1] = wavelength1.getMappedValue();
            }
        });
        OwnSliderPlus wavelength2 = new OwnSliderPlus(I18n.msg("gui.procedural.wavelength", "2"), 0.4f, 1.0f, 0.01f, skin);
        wavelength2.setWidth(fieldWidthTotal / 3f - pad10 * 1.3f);
        wavelength2.setValue((float) ac.wavelengths[2]);
        wavelength2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.wavelengths[2] = wavelength2.getMappedValue();
            }
        });

        HorizontalGroup wavelengthGroup = new HorizontalGroup();
        wavelengthGroup.space(pad10 * 2f);
        wavelengthGroup.addActor(wavelength0);
        wavelengthGroup.addActor(wavelength1);
        wavelengthGroup.addActor(wavelength2);
        OwnImageButton wavelengthTooltip = new OwnImageButton(skin, "tooltip");
        wavelengthTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.wavelength"), skin));
        content.add(wavelengthGroup).left().padBottom(pad18).padRight(pad10);
        content.add(wavelengthTooltip).left().padBottom(pad18).row();

        // eSun
        OwnSliderPlus eSun = new OwnSliderPlus(I18n.msg("gui.procedural.esun"), -1.0f, 30.0f, 0.1f, skin);
        eSun.setWidth(fieldWidthTotal);
        eSun.setValue(ac.m_eSun);
        eSun.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.m_eSun = eSun.getMappedValue();
            }
        });
        OwnImageButton esunTooltip = new OwnImageButton(skin, "tooltip");
        esunTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.esun"), skin));
        content.add(eSun).left().padBottom(pad18).padRight(pad10);
        content.add(esunTooltip).left().padBottom(pad18).row();

        DecimalFormat nf = new DecimalFormat("#0.0000##");
        // Kr
        OwnSliderPlus kr = new OwnSliderPlus(I18n.msg("gui.procedural.kr"), 0.0f, 0.03f, 0.0001f, skin);
        kr.setWidth(fieldWidthTotal);
        kr.setNumberFormatter(nf);
        kr.setValue(ac.m_Kr);
        kr.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.m_Kr = kr.getMappedValue();
            }
        });
        OwnImageButton krTooltip = new OwnImageButton(skin, "tooltip");
        krTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.kr"), skin));
        content.add(kr).left().padBottom(pad18).padRight(pad10);
        content.add(krTooltip).left().padBottom(pad18).row();

        // Kr
        OwnSliderPlus km = new OwnSliderPlus(I18n.msg("gui.procedural.km"), 0.0f, 0.01f, 0.0001f, skin);
        km.setWidth(fieldWidthTotal);
        km.setNumberFormatter(nf);
        km.setValue(ac.m_Km);
        km.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.m_Km = km.getMappedValue();
            }
        });
        OwnImageButton kmTooltip = new OwnImageButton(skin, "tooltip");
        kmTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.km"), skin));
        content.add(km).left().padBottom(pad18).padRight(pad10);
        content.add(kmTooltip).left().padBottom(pad18).row();

        // Fog density
        OwnSliderPlus fogDensity = new OwnSliderPlus(I18n.msg("gui.procedural.fogdensity"),
                                                     Constants.MIN_ATM_FOG_DENSITY,
                                                     Constants.MAX_ATM_FOG_DENSITY,
                                                     Constants.SLIDER_STEP_TINY, skin);
        fogDensity.setWidth(fieldWidthTotal);
        fogDensity.setValue(ac.fogDensity);
        fogDensity.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.fogDensity = fogDensity.getMappedValue();
            }
        });
        OwnImageButton fogDensityTooltip = new OwnImageButton(skin, "tooltip");
        fogDensityTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.fogdensity"), skin));
        content.add(fogDensity).left().padBottom(pad18).padRight(pad10);
        content.add(fogDensityTooltip).left().padBottom(pad18).row();

        // Num samples
        OwnSliderPlus samples = new OwnSliderPlus(I18n.msg("gui.procedural.samples"), 2, 50, 1, skin);
        samples.setWidth(fieldWidthTotal);
        samples.setValue(ac.samples);
        samples.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.samples = (int) samples.getValue();
            }
        });
        OwnImageButton samplesTooltip = new OwnImageButton(skin, "tooltip");
        samplesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.samples"), skin));
        content.add(samples).left().padBottom(pad18).padRight(pad10);
        content.add(samplesTooltip).left().padBottom(pad18).row();

        // Fog color
        ColorPicker fogColor = new ColorPicker(new float[]{ac.fogColor.x, ac.fogColor.y, ac.fogColor.z, 1f}, stage, skin);
        fogColor.setNewColorRunnable(() -> {
            float[] col = fogColor.getPickedColor();
            ac.fogColor.x = col[0];
            ac.fogColor.y = col[1];
            ac.fogColor.z = col[2];
        });
        OwnLabel fogColorLabel = new OwnLabel(I18n.msg("gui.procedural.fogcolor"), skin);
        fogColorLabel.setWidth(textWidth);
        OwnImageButton fogColorTooltip = new OwnImageButton(skin, "tooltip");
        fogColorTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.fogcolor"), skin));
        HorizontalGroup fogColorGroup = new HorizontalGroup();
        fogColorGroup.space(pad18);
        fogColorGroup.addActor(fogColorLabel);
        fogColorGroup.addActor(fogColor);
        fogColorGroup.addActor(fogColorTooltip);
        content.add(fogColorGroup).left().padBottom(pad18).row();

        content.add(new Separator(skin, "gray")).center().colspan(3).growX().padTop(pad34).row();
    }

    protected Boolean randomizeSurface(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        switch (rand.nextInt(10)) {
            case 0, 1, 2, 3 -> initMtc.randomizeEarthLike(rand.nextLong());
            case 4 -> initMtc.randomizeRockyPlanet(rand.nextLong());
            case 5, 6 -> initMtc.randomizeGasGiant(rand.nextLong());
            case 7, 8 -> initMtc.randomizeColdPlanet(rand.nextLong());
            case 9 -> initMtc.randomizeAll(rand.nextLong());
        }

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceGasGiant(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeGasGiant(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceEarthLike(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeEarthLike(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceColdPlanet(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeColdPlanet(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceRockyPlanet(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeRockyPlanet(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeClouds(Boolean rebuild) {
        this.initClc = new CloudComponent();
        this.initClc.randomizeAll(rand.nextLong(), view.getSize());

        if (rebuild) {
            // Others are the same
            this.initMtc = this.mtc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateClouds(false);
    }

    protected Boolean randomizeAtmosphere(Boolean rebuild) {
        this.initAc = new AtmosphereComponent();
        this.initAc.randomizeAll(rand.nextLong(), view.getRadius());

        if (rebuild) {
            // Others are the same
            this.initMtc = this.mtc;
            this.initClc = this.clc;

            rebuild();
        }

        return generateAtmosphere(false);
    }

    protected void randomizeAll() {
        randomizeSurface(false);
        randomizeClouds(false);
        randomizeAtmosphere(false);
        rebuild();
    }

    protected Boolean generateSurface(Boolean ignored) {
        GaiaSky.postRunnable(this::generateSurfaceDirect);
        return ignored;
    }

    protected void generateSurfaceDirect() {
        if (genSurfaceNum == 0) {
            var model = Mapper.model.get(target);
            var materialComponent = model.model.mtc;
            if (materialComponent != null) {
                materialComponent.disposeTextures(GaiaSky.instance.assetManager);
                materialComponent.disposeNoiseBuffers();
            }
            if (mtc != null) {
                mtc.initialize(view.getName());
                model.model.setMaterial(mtc);
            }
        } else {
            logger.info(I18n.msg("gui.procedural.error.gen", I18n.msg("gui.procedural.surface")));
        }
    }

    protected Boolean generateClouds(Boolean ignored) {
        GaiaSky.postRunnable(this::generateCloudsDirect);
        return ignored;
    }

    protected void generateCloudsDirect() {
        if (genCloudNum == 0) {
            var cloud = Mapper.cloud.get(target);
            CloudComponent cloudComponent = cloud.cloud;
            if (cloudComponent != null) {
                cloudComponent.disposeTextures(GaiaSky.instance.assetManager);
                cloudComponent.disposeNoiseBuffers();
            }
            clc.initialize(view.getName(), false);
            cloud.cloud = clc;
            cloud.cloud.doneLoading(GaiaSky.instance.assetManager);
        } else {
            logger.info(I18n.msg("gui.procedural.error.gen", I18n.msg("gui.procedural.cloud")));
        }
    }

    protected Boolean generateAtmosphere(Boolean ignored) {
        GaiaSky.postRunnable(this::generateAtmosphereDirect);
        return ignored;
    }

    protected void generateAtmosphereDirect() {
        var atm = Mapper.atmosphere.get(target);
        var model = Mapper.model.get(target);
        atm.atmosphere = ac;
        atm.atmosphere.doneLoading(model.model.instance.materials.first(), (float) view.getSize());
    }

    protected void generateAll() {
        GaiaSky.postRunnable(() -> {
            generateSurfaceDirect();
            generateCloudsDirect();
            generateAtmosphereDirect();
        });
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {
    }

    /**
     * Sets the enabled property on the given components
     */
    protected void enableComponents(boolean enabled,
                                    Disableable... components) {
        for (Disableable c : components) {
            if (c != null)
                c.setDisabled(!enabled);
        }
    }

    @Override
    public void dispose() {

    }

    public void setKeyboardFocus() {
        stage.setKeyboardFocus(null);
    }

    private void updateButtonStatus() {
        genCloudsButton.setDisabled(genCloudNum > 0);
        genSurfaceButton.setDisabled(genSurfaceNum > 0);
    }

    private ScrollPane scrollPane(Actor actor) {
        ScrollPane scroll = new OwnScrollPane(actor, skin, "minimalist-nobg");
        scroll.setWidth(fieldWidthTotal + 100f);
        scroll.setHeight(scrollPaneHeight);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsVisible(true);
        return scroll;
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        switch (event) {
            case PROCEDURAL_GENERATION_CLOUD_INFO -> {
                boolean status = (Boolean) data[0];
                if (status) {
                    genCloudNum++;
                } else {
                    genCloudNum = FastMath.max(genCloudNum - 1, 0);
                }
                updateButtonStatus();
            }
            case PROCEDURAL_GENERATION_SURFACE_INFO -> {
                boolean status = (Boolean) data[0];
                if (status) {
                    genSurfaceNum++;
                } else {
                    genSurfaceNum = FastMath.max(genSurfaceNum - 1, 0);
                }
                updateButtonStatus();
            }
            case FOCUS_CHANGED -> {
                Entity entity;
                if (data[0] instanceof String) {
                    entity = GaiaSky.instance.scene.getEntity((String) data[0]);
                } else {
                    FocusView v = (FocusView) data[0];
                    entity = v.getEntity();
                }
                if (Mapper.atmosphere.has(entity)) {
                    reinitialize(entity);
                }
            }
        }
    }
}
