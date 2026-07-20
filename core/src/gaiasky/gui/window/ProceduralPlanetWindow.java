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
import gaiasky.render.util.NoiseType;
import gaiasky.scene.Mapper;
import gaiasky.scene.record.*;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.SysUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;
import net.jafama.FastMath;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.function.Function;

/**
 * Interface to the procedural generation system for planetary surfaces, atmospheres and cloud layers.
 */
public class ProceduralPlanetWindow extends GenericDialog implements IObserver {
    private static final Log logger = Logger.getLogger(ProceduralPlanetWindow.class);
    // Selected tab persists across windows
    private static int lastTabSelected;

    private Entity target;
    private final FocusView view;
    private final Random rand;
    private MaterialComponent initMtc, mtc;
    private CloudComponent initClc, clc;
    private AtmosphereComponent initAc, ac;
    private float fieldWidth, fieldWidthAll, fieldWidthTotal, textWidth, scrollPaneHeight;
    private boolean updateTabSelected = true;
    private OwnSliderReset lutHueShift, lutSaturation;
    private Texture currentLutTexture;
    private Cell<?> lutImageCell;
    private OwnTextButton genCloudsButton, genSurfaceButton;
    /**
     * The surface generation is enabled only when the model is <b>NOT</b> using cubemap textures.
     **/
    private boolean surfaceEnabled;

    private int genCloudNum, genSurfaceNum;

    public ProceduralPlanetWindow(FocusView target,
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
        this.fieldWidthTotal = 1000f;
        this.scrollPaneHeight = 600f;
        float tabContentWidth = 400f;
        float tabWidth = 240f;

        // Create the tab buttons
        HorizontalGroup group = new HorizontalGroup();
        group.align(Align.left);

        // SURFACE TAB
        Button tabSurface = this.surfaceEnabled ? new OwnTextButton(I18n.msg("gui.procedural.surface"), skin, "toggle-big") : null;
        if (this.surfaceEnabled) {
            tabSurface.pad(pad10);
            tabSurface.setWidth(tabWidth);
            group.addActor(tabSurface);
        }

        // CLOUDS TAB
        Button tabClouds = new OwnTextButton(I18n.msg("gui.procedural.cloud"), skin, "toggle-big");
        tabClouds.pad(pad10);
        tabClouds.setWidth(tabWidth);
        group.addActor(tabClouds);

        // ATMOSPHERE TAB
        Button tabAtmosphere = new OwnTextButton(I18n.msg("gui.procedural.atmosphere"), skin, "toggle-big");
        tabAtmosphere.pad(pad10);
        tabAtmosphere.setWidth(tabWidth);
        group.addActor(tabAtmosphere);

        content.add(group).left().row();

        // Create the tab content. Just using images here for simplicity.
        Stack tabContent = new Stack();

        // SURFACE
        Table contentSurface = this.surfaceEnabled ? new Table(skin) : null;
        if (this.surfaceEnabled) {
            contentSurface.setWidth(tabContentWidth);
            contentSurface.align(Align.top | Align.left);
            buildContentSurface(contentSurface);
            tabContent.addActor(contentSurface);
        }

        // CLOUDS
        Table contentClouds = new Table(skin);
        contentClouds.setWidth(tabContentWidth);
        contentClouds.align(Align.top | Align.left);
        buildContentClouds(contentClouds);
        tabContent.addActor(contentClouds);

        // ATMOSPHERE
        Table contentAtmosphere = new Table(skin);
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

        var bottomTable = new Table(skin);
        // Randomize button
        var randomize = new OwnTextIconButton(I18n.msg("gui.procedural.randomize", I18n.msg("gui.procedural.all")), skin, "random", "big");
        randomize.setColor(ColorUtils.gYellowC);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                randomizeAll();
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);


        // Resolution
        var pgResolution = new OwnSliderPlus(I18n.msg("gui.ui.procedural.resolution"),
                                             Constants.PG_RESOLUTION_MIN,
                                             Constants.PG_RESOLUTION_MAX,
                                             1,
                                             skin);
        pgResolution.setValueLabelTransform((value) -> value.intValue() * 2 + "x" + value.intValue());
        pgResolution.setWidth(fieldWidthTotal / 2f + 270f);
        pgResolution.setValue(GaiaSky.settings().graphics.proceduralGenerationResolution[1]);
        pgResolution.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                int pgHeight = (int) pgResolution.getValue();
                int pgWidth = pgHeight * 2;
                EventManager.publish(Event.PROCEDURAL_GENERATION_RESOLUTION_CMD, this, pgWidth, pgHeight);
            }
        });

        // Add bottom controls.
        bottomTable.add(randomize).left().padRight(pad34);
        bottomTable.add(pgResolution).left();
        content.add(bottomTable).colspan(2).left().padBottom(pad34).row();

        // Save textures checkbox.
        OwnCheckBox saveTextures = new OwnCheckBox(I18n.msg("gui.procedural.savetextures"), skin, pad10);
        saveTextures.setChecked(GaiaSky.settings().program.saveProceduralTextures);
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

    private final float w = 100f;
    private final float h = 80f;

    private void addSurfacePresets(Table content,
                                   Function<Boolean, Boolean> randomizeFunc,
                                   Function<Boolean, Boolean> gasGiantFunc,
                                   Function<Boolean, Boolean> earthLikeFunc,
                                   Function<Boolean, Boolean> desertFunc,
                                   Function<Boolean, Boolean> tropicalFunc,
                                   Function<Boolean, Boolean> iceWorldFunc,
                                   Function<Boolean, Boolean> rockyPlanetFunc,
                                   Function<Boolean, Boolean> moltenLavaFunc,
                                   Function<Boolean, Boolean> alienFunc
    ) {
        // Randomize
        var randomize = new OwnTextIconButton("", skin, "dice");
        randomize.setSize(w, h);
        randomize.setIconColor(ColorUtils.gYellowC);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                randomizeFunc.apply(true);
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);
        randomize.setTooltip(I18n.msg("gui.procedural.info.button.randomize", I18n.msg("gui.procedural.surface")));

        // Earth like
        var earthLike = new OwnTextIconButton("", skin, "planet-earth");
        earthLike.setSize(w, h);
        earthLike.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                earthLikeFunc.apply(true);
            }
        });
        earthLike.pad(pad10, pad20, pad10, pad20);
        earthLike.setTooltip(I18n.msg("gui.procedural.button.earthlike"));

        // Desert
        var desert = new OwnTextIconButton("", skin, "planet-desert");
        desert.setSize(w, h);
        desert.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                desertFunc.apply(true);
            }
        });
        desert.pad(pad10, pad20, pad10, pad20);
        desert.setTooltip(I18n.msg("gui.procedural.button.desert"));

        // Tropical islands
        var tropicalIslands = new OwnTextIconButton("", skin, "planet-tropical");
        tropicalIslands.setSize(w, h);
        tropicalIslands.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                tropicalFunc.apply(true);
            }
        });
        tropicalIslands.pad(pad10, pad20, pad10, pad20);
        tropicalIslands.setTooltip(I18n.msg("gui.procedural.button.tropical"));

        // Gas giant
        var gasGiant = new OwnTextIconButton("", skin, "planet-gasgiant");
        gasGiant.setSize(w, h);
        gasGiant.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                gasGiantFunc.apply(true);
            }
        });
        gasGiant.pad(pad10, pad20, pad10, pad20);
        gasGiant.setTooltip(I18n.msg("gui.procedural.button.gasgiant"));

        // Frozen world
        var frozenWorld = new OwnTextIconButton("", skin, "planet-ice");
        frozenWorld.setSize(w, h);
        frozenWorld.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                iceWorldFunc.apply(true);
            }
        });
        frozenWorld.pad(pad10, pad20, pad10, pad20);
        frozenWorld.setTooltip(I18n.msg("gui.procedural.button.ice"));

        // Rocky planet
        var rockyPlanet = new OwnTextIconButton("", skin, "planet-rocky");
        rockyPlanet.setSize(w, h);
        rockyPlanet.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                rockyPlanetFunc.apply(true);
            }
        });
        rockyPlanet.pad(pad10, pad20, pad10, pad20);
        rockyPlanet.setTooltip(I18n.msg("gui.procedural.button.rocky"));

        // Molten lava world
        var moltenLava = new OwnTextIconButton("", skin, "planet-lava");
        moltenLava.setSize(w, h);
        moltenLava.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                moltenLavaFunc.apply(true);
            }
        });
        moltenLava.pad(pad10, pad20, pad10, pad20);
        moltenLava.setTooltip(I18n.msg("gui.procedural.button.lava"));

        // Molten lava world
        var alienPlanet = new OwnTextIconButton("", skin, "planet-alien");
        alienPlanet.setSize(w, h);
        alienPlanet.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                alienFunc.apply(true);
            }
        });
        alienPlanet.pad(pad10, pad20, pad10, pad20);
        alienPlanet.setTooltip(I18n.msg("gui.procedural.button.alien"));

        Table bt = new Table(skin);
        bt.add(randomize).pad(5f).padRight(15f);
        bt.add(earthLike).pad(5f);
        bt.add(desert).pad(5f);
        bt.add(tropicalIslands).pad(5f);
        bt.add(frozenWorld).pad(5f);
        bt.add(rockyPlanet).pad(5f);
        bt.add(moltenLava).pad(5f);
        bt.add(gasGiant).pad(5f);
        bt.add(alienPlanet).pad(5f);

        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad20);
        buttonGroup.addActor(bt);

        content.add(buttonGroup).center().colspan(2).padBottom(pad34).row();
    }

    private void addCloudPresets(Table content,
                                 Function<Boolean, Boolean> randomizeFunc
    ) {
        // Randomize
        var randomize = new OwnTextIconButton("", skin, "dice");
        randomize.setSize(w, h);
        randomize.setIconColor(ColorUtils.gYellowC);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                randomizeFunc.apply(true);
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);
        randomize.setTooltip(I18n.msg("gui.procedural.info.button.randomize", I18n.msg("gui.procedural.cloud")));

        Table bt = new Table(skin);
        bt.add(randomize).pad(5f);

        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad20);
        buttonGroup.addActor(bt);

        content.add(buttonGroup).center().colspan(2).padBottom(pad34).row();
    }

    private void addAtmospherePresets(Table content,
                                      Function<Boolean, Boolean> randomizeFunc,
                                      Function<Boolean, Boolean> blueFunc,
                                      Function<Boolean, Boolean> redFunc,
                                      Function<Boolean, Boolean> greenFunc,
                                      Function<Boolean, Boolean> whiteFunc,
                                      Function<Boolean, Boolean> magentaFunc,
                                      Function<Boolean, Boolean> yellowFunc,
                                      Function<Boolean, Boolean> cyanFunc
    ) {
        // Randomize
        var randomize = new OwnTextIconButton("", skin, "dice");
        randomize.setSize(w, h);
        randomize.setIconColor(ColorUtils.gYellowC);
        randomize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                randomizeFunc.apply(true);
            }
        });
        randomize.pad(pad10, pad20, pad10, pad20);
        randomize.setTooltip(I18n.msg("gui.procedural.info.button.randomize", I18n.msg("gui.procedural.atmosphere")));

        // White
        var planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var white = new OwnTextIconButton("", planet, skin);
        white.setSize(w, h);
        white.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                whiteFunc.apply(true);
            }
        });
        white.pad(pad10, pad20, pad10, pad20);

        // Blue
        planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var blue = new OwnTextIconButton("", planet, skin);
        blue.setSize(w, h);
        blue.setIconColor(ColorUtils.gBlueC);
        blue.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                blueFunc.apply(true);
            }
        });
        blue.pad(pad10, pad20, pad10, pad20);

        // Red
        planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var red = new OwnTextIconButton("", planet, skin);
        red.setSize(w, h);
        red.setIconColor(ColorUtils.gRedC);
        red.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                redFunc.apply(true);
            }
        });
        red.pad(pad10, pad20, pad10, pad20);

        // Green
        planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var green = new OwnTextIconButton("", planet, skin);
        green.setSize(w, h);
        green.setIconColor(ColorUtils.gGreenC);
        green.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                greenFunc.apply(true);
            }
        });
        green.pad(pad10, pad20, pad10, pad20);

        // Magenta
        planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var magenta = new OwnTextIconButton("", planet, skin);
        magenta.setSize(w, h);
        magenta.setIconColor(ColorUtils.ddMagentaC);
        magenta.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                magentaFunc.apply(true);
            }
        });
        magenta.pad(pad10, pad20, pad10, pad20);

        // Yellow
        planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var yellow = new OwnTextIconButton("", planet, skin);
        yellow.setSize(w, h);
        yellow.setIconColor(ColorUtils.gYellowC);
        yellow.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                yellowFunc.apply(true);
            }
        });
        yellow.pad(pad10, pad20, pad10, pad20);

        // Cyan
        planet = new OwnImage(skin.getDrawable("icon-planet"), false);
        var cyan = new OwnTextIconButton("", planet, skin);
        cyan.setSize(w, h);
        cyan.setIconColor(ColorUtils.oCyanC);
        cyan.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                cyanFunc.apply(true);
            }
        });
        cyan.pad(pad10, pad20, pad10, pad20);


        Table bt = new Table(skin);
        bt.add(randomize).pad(5f).padRight(15f);
        bt.add(white).pad(5f);
        bt.add(blue).pad(5f);
        bt.add(red).pad(5f);
        bt.add(green).pad(5f);
        bt.add(yellow).pad(5f);
        bt.add(cyan).pad(5f);
        bt.add(magenta).pad(5f);

        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad20);
        buttonGroup.addActor(bt);

        content.add(buttonGroup).center().colspan(2).padBottom(pad34).row();
    }

    private OwnTextButton addGenButton(Table content,
                                       String key,
                                       Function<Boolean, Boolean> generateFunc,
                                       int colspan) {
        String name = I18n.msg(key);

        // Generate button
        var generate = new OwnTextIconButton(I18n.msg("gui.procedural.generate", name), skin, "wrench");
        generate.setSize(w * 4f, h);
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

        content.add(buttonGroup).center().colspan(colspan).padBottom(pad34).row();

        return generate;
    }

    private void addNoiseGroup(Table content,
                               NoiseComponent nc,
                               boolean clouds) {

        var fieldWidthNoise = fieldWidthAll + 250f;

        // Noise group table.
        Table noiseTable = new Table(skin);

        String suffix = clouds ? "" : ".terrain";

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

        // Base level.
        var waterLevel = new OwnSliderReset(I18n.msg("gui.procedural.base" + suffix), 0.0f, 1.0f, 0.01f, 0.2f, skin);
        waterLevel.setWidth(fieldWidthNoise);
        waterLevel.setValue(nc.baseLevel);
        waterLevel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.baseLevel = waterLevel.getMappedValue();
            }
        });
        OwnImageButton waterLevelTooltip = new OwnImageButton(skin, "tooltip");
        waterLevelTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.base"), skin));
        noiseTable.add(waterLevel).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(waterLevelTooltip).left().padBottom(pad18).row();

        // Remap.
        var remap = new OwnCheckBox(I18n.msg("gui.procedural.remap"), skin, pad10);
        remap.setChecked(nc.remap);
        remap.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        nc.remap = remap.isChecked();
                    }
                }
        );
        OwnImageButton remapTooltip = new OwnImageButton(skin, "tooltip");
        remapTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.remap"), skin));

        noiseTable.add(remap).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(remapTooltip).left().padBottom(pad18).row();

        // Octaves (recursive detail).
        var octaves = new OwnSliderReset(I18n.msg("gui.procedural.octaves"), 1, 8, 1, clouds ? 6 : 5, skin);
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

        // Frequency (continent fragmentation).
        var frequency = new OwnSliderReset(I18n.msg("gui.procedural.frequency" + suffix), 0.01f, 1.0f, 0.01f, 1.0f, skin);
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

        // Lacunarity (subdivision rate).
        var lacunarity = new OwnSliderReset(I18n.msg("gui.procedural.lacunarity"), 0.1f, 5.0f, 0.1f, 2.0f, skin);
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

        // Persistence (elevation coarseness).
        var persistence = new OwnSliderReset(I18n.msg("gui.procedural.persistence" + suffix), 0.01f, 0.9f, 0.01f, 0.5f, skin);
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

        if (!clouds) {
            // Plains.
            var plainsHeight = new OwnSliderReset(I18n.msg("gui.procedural.plains.height"), 0.01f, 0.8f, 0.01f, 0.1f, skin);
            plainsHeight.setWidth(fieldWidthNoise / 2f - pad10 * 1.1f);
            plainsHeight.setValue(nc.plainsHeight);
            plainsHeight.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    nc.plainsHeight = plainsHeight.getMappedValue();
                }
            });
            var plainsSlope = new OwnSliderReset(I18n.msg("gui.procedural.plains.slope"), 0.05f, 0.5f, 0.01f, 0.2f, skin);
            plainsSlope.setWidth(fieldWidthNoise / 2f - pad10 * 1.1f);
            plainsSlope.setValue(nc.plainsSlope);
            plainsSlope.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    nc.plainsSlope = plainsSlope.getMappedValue();
                }
            });

            HorizontalGroup plainsGroup = new HorizontalGroup();
            plainsGroup.space(pad10 * 2f);
            plainsGroup.addActor(plainsHeight);
            plainsGroup.addActor(plainsSlope);
            OwnImageButton plainsTooltip = new OwnImageButton(skin, "tooltip");
            plainsTooltip.addListener(new OwnTextTooltip(
                    I18n.msg("gui.procedural.info.plains"),
                    skin));
            noiseTable.add(plainsGroup).colspan(2).left().padBottom(pad18).padRight(pad10);
            noiseTable.add(plainsTooltip).left().padBottom(pad18).row();
        }

        // Warping.
        var warpStrength = new OwnSliderReset(I18n.msg("gui.procedural.warp.strength"), 0.0f, 2.0f, 0.01f, 0.0f, skin);
        warpStrength.setWidth(fieldWidthNoise / 2f - pad10 * 1.1f);
        warpStrength.setValue(nc.warpStrength);
        warpStrength.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.warpStrength = warpStrength.getMappedValue();
            }
        });
        var warpFrequency = new OwnSliderReset(I18n.msg("gui.procedural.warp.frequency"), 0.1f, 5.0f, 0.01f, 1.0f, skin);
        warpFrequency.setWidth(fieldWidthNoise / 2f - pad10 * 1.1f);
        warpFrequency.setValue(nc.warpFrequency);
        warpFrequency.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                nc.warpFrequency = warpFrequency.getMappedValue();
            }
        });

        HorizontalGroup warpGroup = new HorizontalGroup();
        warpGroup.space(pad10 * 2f);
        warpGroup.addActor(warpStrength);
        warpGroup.addActor(warpFrequency);
        OwnImageButton warpTooltip = new OwnImageButton(skin, "tooltip");
        warpTooltip.addListener(new OwnTextTooltip(
                I18n.msg("gui.procedural.info.warp"),
                skin));
        noiseTable.add(warpGroup).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(warpTooltip).left().padBottom(pad18).row();

        // Smoothing, turbulence, and ridge.
        var smoothing = new OwnCheckBox(I18n.msg("gui.procedural.smoothing"), skin, pad10);
        var turbulence = new OwnCheckBox(I18n.msg("gui.procedural.turbulence"), skin, pad10);
        var ridge = new OwnCheckBox(I18n.msg("gui.procedural.ridge"), skin, pad10);

        // Smoothing.
        smoothing.setChecked(nc.smoothing);
        smoothing.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event,
                                        Actor actor) {
                        nc.smoothing = smoothing.isChecked();
                    }
                }
        );
        var smoothingTooltip = new OwnImageButton(skin, "tooltip");
        smoothingTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.smoothing"), skin));

        noiseTable.add(smoothing).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(smoothingTooltip).left().padBottom(pad18).row();


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
        var turbulenceTooltip = new OwnImageButton(skin, "tooltip");
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
        var ridgeTooltip = new OwnImageButton(skin, "tooltip");
        ridgeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.ridge"), skin));

        noiseTable.add(ridge).colspan(2).left().padBottom(pad18).padRight(pad10);
        noiseTable.add(ridgeTooltip).left().padBottom(pad18).row();

        content.add(noiseTable).colspan(3).center().padBottom(pad34).row();
    }

    private void updateLutImage() {
        if (lutImageCell != null) {
            lutImageCell.clearActor();
            var manager = MaterialComponent.getLUTManager();
            var path = manager.getLUTPath(mtc.lut);
            Pixmap p = new Pixmap(path);
            int w = p.getWidth();
            int h = p.getHeight();
            if (lutHueShift != null) {
                float hue = lutHueShift.getMappedValue();
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        Color col = new Color(p.getPixel(x, y));
                        float[] rgb = new float[]{col.r, col.g, col.b, 1f};
                        if (hue != 0) {
                            // Shift hue of lookup table by an amount in degrees.
                            float[] hsb = ColorUtils.rgbToHsb(rgb);
                            hsb[0] = ((hsb[0] * 360f + hue) % 360f) / 360f;
                            // Apply saturation.
                            hsb[1] = MathUtilsDouble.clamp(hsb[1] * lutSaturation.getMappedValue(), 0f, 1f);
                            rgb = ColorUtils.hsbToRgb(hsb);
                        }
                        col.set(rgb[0], rgb[1], rgb[2], 1f);
                        p.drawPixel(x, y, Color.rgba8888(col));
                    }
                }
            }
            var newLutTexture = new Texture(p);
            newLutTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            var img = new OwnImage(newLutTexture, false);
            img.setScaling(Scaling.fill);
            lutImageCell.setActor(img);
            lutImageCell.size(310f, 310f);
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
            // Presets
            content.add(new OwnLabel(I18n.msg("gui.procedural.presets"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

            // Add button group with presets.
            addSurfacePresets(content,
                              this::randomizeSurface,
                              this::randomizeSurfaceGasGiant,
                              this::randomizeSurfaceEarthLike,
                              this::randomizeSurfaceDesert,
                              this::randomizeSurfaceTropical,
                              this::randomizeSurfaceFrozen,
                              this::randomizeSurfaceRockyPlanet,
                              this::randomizeSurfaceLava,
                              this::randomizeSurfaceAlien);

            content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).row();

            // Parameters
            content.add(new OwnLabel(I18n.msg("gui.procedural.param.surface"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

            // Add generate and randomize buttons
            genSurfaceButton = addGenButton(content, "gui.procedural.surface", this::generateSurface, 2);

            // Two-column layout: left (controls) + right (LUT image + checkbox)
            Table leftCol = new Table(skin);
            Table rightCol = new Table(skin);

            Array<String> lookUpTables = MaterialComponent.getLUTManager().getPresetNames();

            // --- LEFT COLUMN ---

            // LUT select box
            OwnSelectBox<String> lookUpTablesBox = new OwnSelectBox<>(skin);
            lookUpTablesBox.setItems(lookUpTables);
            lookUpTablesBox.setWidth(fieldWidthAll / 3f + 60f);
            lookUpTablesBox.setSelected(mtc.lut);
            lookUpTablesBox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.lut = lookUpTablesBox.getSelected();
                    updateLutImage();
                }
            });

            var lookUpTablesLabel = new OwnLabel(I18n.msg("gui.procedural.lut"), skin);
            lookUpTablesLabel.setWidth(fieldWidthAll / 3f + 50f);

            var lutTooltip = new OwnImageButton(skin, "tooltip");
            lutTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.lut"), skin));
            leftCol.add(lookUpTablesLabel).left().padBottom(pad18).padRight(pad18);
            leftCol.add(lookUpTablesBox).left().padBottom(pad18).padRight(pad10);
            leftCol.add(lutTooltip).left().padBottom(pad18).row();

            // LUT hue shift
            lutHueShift = new OwnSliderReset(I18n.msg("gui.procedural.hueshift"), 0.0f, 360.0f, 0.1f, 0.0f, skin);
            lutHueShift.setWidth(fieldWidthAll * 2f / 3f + 154f);
            lutHueShift.setValueSuffix("°");
            lutHueShift.setValue(mtc.lutHueShift);
            lutHueShift.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.lutHueShift = lutHueShift.getMappedValue();
                    updateLutImage();
                }
            });
            var hueShiftTooltip = new OwnImageButton(skin, "tooltip");
            hueShiftTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.hueshift"), skin));
            leftCol.add(lutHueShift).colspan(2).left().padBottom(pad18).padRight(pad10);
            leftCol.add(hueShiftTooltip).left().padBottom(pad18).row();

            // LUT saturation
            lutSaturation = new OwnSliderReset(I18n.msg("gui.procedural.saturation"), 0.0f, 2.0f, 0.01f, 1.0f, skin);
            lutSaturation.setWidth(fieldWidthAll * 2f / 3f + 154f);
            lutSaturation.setValue(mtc.lutSaturation);
            lutSaturation.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.lutSaturation = lutSaturation.getMappedValue();
                    updateLutImage();
                }
            });
            var lutSaturationTooltip = new OwnImageButton(skin, "tooltip");
            lutSaturationTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.saturation"), skin));
            leftCol.add(lutSaturation).colspan(2).left().padBottom(pad34 * 1.5f).padRight(pad10);
            leftCol.add(lutSaturationTooltip).left().padBottom(pad34 * 1.5f).row();

            // Height scale
            var heightScale = new OwnSliderReset(I18n.msg("gui.procedural.heightscale"), 1.0f, 100.0f, 0.1f, 10f, skin);
            heightScale.setWidth(fieldWidthAll * 2f / 3f + 154f);
            heightScale.setValueSuffix(" km");
            heightScale.setValue((float) (mtc.heightScale * Constants.U_TO_KM));
            heightScale.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.heightScale = (float) (heightScale.getMappedValue() * Constants.KM_TO_U);
                }
            });
            var heightScaleTooltip = new OwnImageButton(skin, "tooltip");
            heightScaleTooltip.setTooltip(I18n.msg("gui.procedural.info.heightscale"));
            leftCol.add(heightScale).colspan(2).left().padBottom(pad18).padRight(pad10);
            leftCol.add(heightScaleTooltip).left().padBottom(pad18).row();

            // Latitude influence
            var latitudeInfluence = new OwnSliderReset(I18n.msg("gui.procedural.latitude_influence"), 0.0f, 1.0f, 0.01f, 0.8f, skin);
            latitudeInfluence.setWidth(fieldWidthAll * 2f / 3f + 154f);
            latitudeInfluence.setValue(mtc.nc.latitudeInfluence);
            latitudeInfluence.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.nc.latitudeInfluence = latitudeInfluence.getValue();
                }
            });
            var latitudeTooltip = new OwnImageButton(skin, "tooltip");
            latitudeTooltip.setTooltip(I18n.msg("gui.procedural.info.latitude_influence"));
            leftCol.add(latitudeInfluence).colspan(2).left().padBottom(pad18).padRight(pad10);
            leftCol.add(latitudeTooltip).left().padBottom(pad18).row();

            // Emission checkbox
            var emission = new OwnCheckBox(I18n.msg("gui.procedural.emission"), skin, pad10);
            emission.setChecked(mtc.nc.genEmissiveMap);
            emission.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event,
                                    Actor actor) {
                    mtc.nc.genEmissiveMap = emission.isChecked();
                }
            });
            OwnImageButton emissionTooltip = new OwnImageButton(skin, "tooltip");
            emissionTooltip.setTooltip(I18n.msg("gui.procedural.info.emission"));
            leftCol.add(emission).colspan(2).left().padBottom(pad18).padRight(pad10);
            leftCol.add(emissionTooltip).left().padBottom(pad18).row();

            // --- RIGHT COLUMN ---

            // LUT image
            lutImageCell = rightCol.add();
            lutImageCell.size(fieldWidthAll / 3f, fieldWidthAll / 3f).padBottom(pad18).row();

            // Initial update
            updateLutImage();


            // Stitch left and right columns together
            Table scrollContent = new Table(skin);
            scrollContent.add(leftCol).top().left().padRight(pad34).padBottom(pad34);
            scrollContent.add(rightCol).center().left().padBottom(pad34).row();


            // Noise
            addNoiseGroup(scrollContent, mtc.nc, false);

            var scrollPane = scrollPane(scrollContent);

            content.add(scrollPane).colspan(2).center().top().row();
            content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).row();

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
            clc.randomizeAll(rand.nextLong(), view.getRadius());
        } else {
            // Copy existing
            clc.copyFrom(initClc);
            clc.setDiffuse("generate");
        }
        // Presets
        content.add(new OwnLabel(I18n.msg("gui.procedural.presets"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

        // Add button group with presets.
        addCloudPresets(content, this::randomizeClouds);

        content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).row();

        // Parameters
        content.add(new OwnLabel(I18n.msg("gui.procedural.param.cloud"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

        // Add button group
        genCloudsButton = addGenButton(content, "gui.procedural.cloud", this::generateClouds, 2);

        Table scrollContent = new Table(skin);

        // Cloud color
        var cloudColor = new ColorPicker(new float[]{clc.color[0], clc.color[1], clc.color[2], clc.color[3]}, stage, skin);
        cloudColor.setSize(128f, 128f);
        cloudColor.setNewColorRunnable(() -> {
            float[] col = cloudColor.getPickedColorArray();
            clc.color[0] = col[0];
            clc.color[1] = col[1];
            clc.color[2] = col[2];
            clc.color[3] = col[3];
        });

        var cloudColorLabel = new OwnLabel(I18n.msg("gui.procedural.cloudcolor"), skin);
        cloudColorLabel.setWidth(textWidth + 112f);
        var cloudColorTooltip = new OwnImageButton(skin, "tooltip");
        cloudColorTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.cloudcolor"), skin));
        var cloudGroup = new Table(skin);
        cloudGroup.add(cloudColorLabel).padRight(pad18).padBottom(pad18);
        cloudGroup.add(cloudColor).padRight(535f).padBottom(pad18);
        cloudGroup.add(cloudColorTooltip).right().padBottom(pad18).row();
        scrollContent.add(cloudGroup).left().padBottom(pad18).row();

        // Noise
        addNoiseGroup(scrollContent, clc.nc, true);

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
        // Presets
        content.add(new OwnLabel(I18n.msg("gui.procedural.presets"), skin, "hud-header")).colspan(2).left().padBottom(pad34).row();

        addAtmospherePresets(content,
                             this::randomizeAtmosphere,
                             this::randomizeBlueAtmosphere,
                             this::randomizeRedAtmosphere,
                             this::randomizeGreenAtmosphere,
                             this::randomizeWhiteAtmosphere,
                             this::randomizeMagentaAtmosphere,
                             this::randomizeYellowAtmosphere,
                             this::randomizeCyanAtmosphere);

        content.add(new Separator(skin, "gray")).center().colspan(2).growX().padBottom(pad34).row();

        // Parameters
        content.add(new OwnLabel(I18n.msg("gui.procedural.param.atm"), skin, "hud-header")).colspan(3).left().padBottom(pad34).row();

        // Add button group
        addGenButton(content, "gui.procedural.atmosphere", this::generateAtmosphere, 3);

        // Wavelengths
        var wavelength0 = new OwnSliderPlus(I18n.msg("gui.procedural.wavelength", "0"), 0.4f, 1.0f, 0.01f, skin);
        wavelength0.setWidth(fieldWidthTotal / 3f - pad10 * 1.3f);
        wavelength0.setValue((float) ac.wavelengths[0]);
        wavelength0.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.wavelengths[0] = wavelength0.getMappedValue();
            }
        });
        var wavelength1 = new OwnSliderPlus(I18n.msg("gui.procedural.wavelength", "1"), 0.4f, 1.0f, 0.01f, skin);
        wavelength1.setWidth(fieldWidthTotal / 3f - pad10 * 1.3f);
        wavelength1.setValue((float) ac.wavelengths[1]);
        wavelength1.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.wavelengths[1] = wavelength1.getMappedValue();
            }
        });
        var wavelength2 = new OwnSliderPlus(I18n.msg("gui.procedural.wavelength", "2"), 0.4f, 1.0f, 0.01f, skin);
        wavelength2.setWidth(fieldWidthTotal / 3f - pad10 * 1.3f);
        wavelength2.setValue((float) ac.wavelengths[2]);
        wavelength2.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.wavelengths[2] = wavelength2.getMappedValue();
            }
        });

        var wavelengthGroup = new HorizontalGroup();
        wavelengthGroup.space(pad10 * 2f);
        wavelengthGroup.addActor(wavelength0);
        wavelengthGroup.addActor(wavelength1);
        wavelengthGroup.addActor(wavelength2);
        var wavelengthTooltip = new OwnImageButton(skin, "tooltip");
        wavelengthTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.wavelength"), skin));
        content.add(wavelengthGroup).left().padBottom(pad18).padRight(pad10);
        content.add(wavelengthTooltip).left().padBottom(pad18).row();

        // eSun
        var eSun = new OwnSliderReset(I18n.msg("gui.procedural.esun"), -1.0f, 30.0f, 0.1f, 12f, skin);
        eSun.setWidth(fieldWidthTotal);
        eSun.setValue(ac.m_eSun);
        eSun.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.m_eSun = eSun.getMappedValue();
            }
        });
        var esunTooltip = new OwnImageButton(skin, "tooltip");
        esunTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.esun"), skin));
        content.add(eSun).left().padBottom(pad18).padRight(pad10);
        content.add(esunTooltip).left().padBottom(pad18).row();

        DecimalFormat nf = new DecimalFormat("#0.0000##");
        // Kr
        var kr = new OwnSliderReset(I18n.msg("gui.procedural.kr"), 0.0f, 0.03f, 0.0001f, 0.0041f, skin);
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
        var krTooltip = new OwnImageButton(skin, "tooltip");
        krTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.kr"), skin));
        content.add(kr).left().padBottom(pad18).padRight(pad10);
        content.add(krTooltip).left().padBottom(pad18).row();

        // Kr
        var km = new OwnSliderReset(I18n.msg("gui.procedural.km"), 0.0f, 0.01f, 0.0001f, 0.001f, skin);
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
        var kmTooltip = new OwnImageButton(skin, "tooltip");
        kmTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.km"), skin));
        content.add(km).left().padBottom(pad18).padRight(pad10);
        content.add(kmTooltip).left().padBottom(pad18).row();

        // O3 optical depth
        var o3 = new OwnSliderReset(I18n.msg("gui.procedural.o3depth"), 0.0f, 0.3f, 0.001f, 0.025f, skin);
        o3.setWidth(fieldWidthTotal);
        o3.setNumberFormatter(nf);
        o3.setValue(ac.o3OpticalDepth);
        o3.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.o3OpticalDepth = o3.getMappedValue();
            }
        });
        var o3Tooltip = new OwnImageButton(skin, "tooltip");
        o3Tooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.o3depth"), skin));
        content.add(o3).left().padBottom(pad18).padRight(pad10);
        content.add(o3Tooltip).left().padBottom(pad18).row();

        // Fog density
        var fogDensity = new OwnSliderReset(I18n.msg("gui.procedural.fogdensity"),
                                            Constants.MIN_ATM_FOG_DENSITY,
                                            Constants.MAX_ATM_FOG_DENSITY,
                                            Constants.SLIDER_STEP_TINY,
                                            0.3f,
                                            skin);
        fogDensity.setWidth(fieldWidthTotal);
        fogDensity.setValue(ac.fogDensity);
        fogDensity.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.fogDensity = fogDensity.getMappedValue();
            }
        });
        var fogDensityTooltip = new OwnImageButton(skin, "tooltip");
        fogDensityTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.fogdensity"), skin));
        content.add(fogDensity).left().padBottom(pad18).padRight(pad10);
        content.add(fogDensityTooltip).left().padBottom(pad18).row();

        // Num samples
        var samples = new OwnSliderReset(I18n.msg("gui.procedural.samples"), 3, 15, 1, 10, skin);
        samples.setWidth(fieldWidthTotal);
        samples.setValue(ac.samples);
        samples.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                ac.samples = (int) samples.getValue();
            }
        });
        var samplesTooltip = new OwnImageButton(skin, "tooltip");
        samplesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.samples"), skin));
        content.add(samples).left().padBottom(pad18).padRight(pad10);
        content.add(samplesTooltip).left().padBottom(pad18).row();

        // Fog color
        var fogColor = new ColorPicker(new float[]{ac.fogColor.x, ac.fogColor.y, ac.fogColor.z, 1f}, stage, skin);
        fogColor.setNewColorRunnable(() -> {
            float[] col = fogColor.getPickedColorArray();
            ac.fogColor.x = col[0];
            ac.fogColor.y = col[1];
            ac.fogColor.z = col[2];
        });
        var fogColorLabel = new OwnLabel(I18n.msg("gui.procedural.fogcolor"), skin);
        fogColorLabel.setWidth(textWidth);
        var fogColorTooltip = new OwnImageButton(skin, "tooltip");
        fogColorTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.fogcolor"), skin));
        var fogColorGroup = new HorizontalGroup();
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
            case 7, 8 -> initMtc.randomizeFrozenPlanet(rand.nextLong());
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

    protected Boolean randomizeSurfaceDesert(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeDesert(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceTropical(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeTropical(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceFrozen(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeFrozenPlanet(rand.nextLong());

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

    protected Boolean randomizeSurfaceLava(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeLava(rand.nextLong());

        if (rebuild) {
            // Others are the same
            this.initClc = this.clc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateSurface(false);
    }

    protected Boolean randomizeSurfaceAlien(Boolean rebuild) {
        this.initMtc = new MaterialComponent();
        this.initMtc.randomizeAlien(rand.nextLong());

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
        this.initClc.randomizeAll(rand.nextLong(), view.getRadius());

        if (rebuild) {
            // Others are the same
            this.initMtc = this.mtc;
            this.initAc = this.ac;

            rebuild();
        }

        return generateClouds(false);
    }

    protected Boolean randomizeBlueAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, true, 1.3, 0.9, 0.7);
    }

    protected Boolean randomizeGreenAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, true, 1.3, 0.5, 1.2);
    }

    protected Boolean randomizeRedAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, true, 0.7, 1.2, 1.3);
    }

    protected Boolean randomizeYellowAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, true, 0.7, 0.63, 1.4);
    }

    protected Boolean randomizeMagentaAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, true, 0.5, 1.4, 0.6);
    }

    protected Boolean randomizeCyanAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, true, 1.4, 0.6, 0.7);
    }

    protected Boolean randomizeWhiteAtmosphere(Boolean rebuild) {
        return randomizeAtmosphereWhite(rebuild);
    }

    protected Boolean randomizeAtmosphere(Boolean rebuild) {
        return randomizeAtmosphere(rebuild, false, 1, 1, 1);
    }

    protected Boolean randomizeAtmosphere(Boolean rebuild,
                                          boolean regularScattering,
                                          double redMask,
                                          double greenMask,
                                          double blueMask) {
        this.initAc = new AtmosphereComponent();
        this.initAc.randomizeAll(rand.nextLong(), view.getRadius(), regularScattering, redMask, greenMask, blueMask);

        if (rebuild) {
            // Others are the same
            this.initMtc = this.mtc;
            this.initClc = this.clc;

            rebuild();
        }

        return generateAtmosphere(false);
    }

    protected Boolean randomizeAtmosphereWhite(Boolean rebuild) {
        this.initAc = new AtmosphereComponent();
        this.initAc.randomizeWhite(rand.nextLong(), view.getRadius());

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
