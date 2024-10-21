/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.*;
import gaiasky.gui.datasets.DatasetManagerWindow;
import gaiasky.gui.main.GSKeys;
import gaiasky.gui.main.GamepadMappings;
import gaiasky.gui.main.KeyBindings;
import gaiasky.gui.main.KeyBindings.ProgramAction;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.*;
import gaiasky.util.Settings.PostprocessSettings.AntialiasType;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.gdx.loader.WarpMeshReader;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.screenshot.ImageRenderer;
import gaiasky.util.validator.*;
import net.jafama.FastMath;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class PreferencesWindow extends GenericDialog implements IObserver {
    private static final Log logger = Logger.getLogger(PreferencesWindow.class);

    private final Array<OwnLabel> labels;

    private final DecimalFormat nf3;
    private final GlobalResources globalResources;
    // This flag is active when the dialog is called from the welcome screen.
    private final boolean welcomeScreen;
    private OwnCheckBox fullScreen;
    private OwnCheckBox windowed;
    private OwnCheckBox maxFps;
    private OwnCheckBox multithreadCb;
    private OwnCheckBox lodFadeCb;
    private OwnCheckBox cbAutoCamRec;
    private OwnCheckBox real;
    private OwnCheckBox invertX;
    private OwnCheckBox invertY;
    private OwnCheckBox highAccuracyPositions;
    private OwnCheckBox shadowsCb;
    private OwnCheckBox displayTimeNoUi;
    private OwnCheckBox pointerCoords;
    private OwnCheckBox modeChangeInfo;
    private OwnCheckBox debugInfo;
    private OwnCheckBox crosshairFocus;
    private OwnCheckBox crosshairClosest;
    private OwnCheckBox crosshairHome;
    private OwnCheckBox pointerGuides;
    private OwnCheckBox newUI;
    private OwnCheckBox exitConfirmation;
    private OwnCheckBox recGridProjectionLines;
    private OwnCheckBox frameCoordinates;
    private OwnCheckBox dynamicResolution;
    private OwnCheckBox ssr;
    private OwnCheckBox eclipses;
    private OwnCheckBox eclipseOutlines;
    private OwnCheckBox starSpheres;
    private OwnCheckBox shaderCache;
    private OwnCheckBox saveTextures;
    private OwnSelectBox<DisplayMode> fullScreenResolutions;
    private OwnSelectBox<ComboBoxBean> graphicsQuality, antiAlias, pointCloudRenderer, lineRenderer, numThreads, screenshotMode,
            screenshotFormat, frameOutputMode, frameOutputFormat, nShadows, distUnitsSelect, toneMappingSelect;
    private OwnSelectBox<LangComboBoxBean> lang;
    private OwnSelectBox<ElevationComboBoxBean> elevationSb;
    private OwnSelectBox<String> recGridOrigin, recGridStyle;
    private OwnSelectBox<StringComobBoxBean> theme;
    private OwnSelectBox<FileComboBoxBean> gamepadMappings;
    private OwnSelectBox<ReprojectionMode> reprojectionMode;
    private OwnSelectBox<UpscaleFilter> upscaleFilter;
    private OwnTextField fadeTimeField, widthField, heightField, ssWidthField, ssHeightField, frameOutputPrefix,
            frameOutputFps, foWidthField, foHeightField, camRecFps, cmResolution, plResolution, plAperture, plAngle,
            smResolution, maxFpsInput;
    private OwnSliderPlus lodTransitions, tessQuality, minimapSize, pointerGuidesWidth, uiScale, backBufferScale,
            celestialSphereIndexOfRefraction, bloomEffect, screenshotQuality, frameQuality, unsharpMask, svtCacheSize,
            chromaticAberration, filmGrain, lensFlare, velocityVectors, motionBlur, pgResolution;
    private OwnTextButton screenshotsLocation, frameOutputLocation, meshWarpFileLocation;
    private Path screenshotsPath, frameOutputPath, meshWarpFilePath;
    private OwnLabel frameSequenceNumber;
    private ColorPicker pointerGuidesColor;
    private OwnLabel tessQualityLabel;
    private Cell<?> noticeHiResCell;
    private Table controllersTable;
    // Backup values
    private ToneMapping toneMappingBak;
    private float brightnessBak, contrastBak, hueBak, saturationBak, gammaBak, exposureBak, bloomBak, unsharpMaskBak,
            aberrationBak, lensFlareBak, filmGrainBak;
    private boolean lightGlowBak, debugInfoBak, frameCoordinatesBak;
    private int FXAAQuality, FXAAQualityBak;
    private ReprojectionMode reprojectionBak;
    private UpscaleFilter upscaleFilterBak;
    private AtomicBoolean vsyncValue;

    public PreferencesWindow(final Stage stage,
                             final Skin skin,
                             final GlobalResources globalResources) {
        this(stage, skin, globalResources, false);
    }

    public PreferencesWindow(final Stage stage,
                             final Skin skin,
                             final GlobalResources globalResources,
                             final boolean welcomeScreen) {
        super(I18n.msg("gui.settings") + " - " + Settings.settings.version.version + " - " + I18n.msg("gui.build", Settings.settings.version.build), skin, stage);

        this.tabContents = new Array<>();
        this.labels = new Array<>();
        this.globalResources = globalResources;
        this.welcomeScreen = welcomeScreen;

        this.nf3 = new DecimalFormat("0.000");

        setAcceptText(I18n.msg("gui.saveprefs"));
        setCancelText(I18n.msg("gui.cancel"));

        // Build UI.
        buildSuper();

        EventManager.instance.subscribe(this, Event.CONTROLLER_CONNECTED_INFO, Event.CONTROLLER_DISCONNECTED_INFO);
        EventManager.instance.subscribe(this, Event.INVERT_Y_CMD, Event.INVERT_X_CMD, Event.WINDOW_RESOLUTION_INFO);
    }

    private OwnTextIconButton createTab(String title,
                                        Image img,
                                        Skin skin) {
        OwnTextIconButton tab = new OwnTextIconButton(TextUtils.capString(title, 26), img, skin, "toggle-big");
        tab.addListener(new OwnTextTooltip(title, skin));
        tab.pad(pad10);
        tab.setWidth(480f);
        return tab;
    }

    /**
     * Adds a new group to the given container with the given label and content.
     *
     * @param container The container table.
     * @param title     The title label.
     * @param content   The content group.
     * @param padTop    Padding to the above element.
     */
    private void addContentGroup(Table container,
                                 Label title,
                                 WidgetGroup content,
                                 float padTop) {
        container.add(title).left().padTop(padTop).row();
        container.add(new Separator(skin, "small")).bottom().left().expandX().fillX().padBottom(pad20).row();
        container.add(content).left().row();
    }

    /**
     * Adds a new group to the given container with the given label and content.
     *
     * @param container The container table.
     * @param title     The title label.
     * @param content   The content group.
     */
    private void addContentGroup(Table container,
                                 Label title,
                                 WidgetGroup content) {
        addContentGroup(container, title, content, pad34 * 2f);
    }

    @Override
    protected void build() {
        final float contentWidth = 1100f;
        final float contentHeight = 1120f;
        final float taWidth = 960f;
        final float inputSmallWidth = 233f;
        final float inputWidth = 500f;
        final float selectWidth = 500f;
        final float scrollHeight = 640f;
        final float controlsScrollWidth = 1300f;
        final float controlsScrollHeight = 600f;
        final float sliderWidth = 500f;
        final float buttonHeight = 40f;

        final var settings = Settings.settings;
        boolean safeMode = settings.program.safeMode;
        boolean vr = settings.runtime.openXr;

        // Create the tab buttons
        Table tabsTable = new Table(skin);
        tabsTable.align(Align.left | Align.top);

        final OwnTextIconButton tabGraphics = createTab(I18n.msg("gui.graphicssettings"), new Image(skin.getDrawable("iconic-bolt")), skin);
        final OwnTextIconButton tabScene = createTab(I18n.msg("gui.ui.scene.settings"), new Image(skin.getDrawable("iconic-compass")), skin);
        final OwnTextIconButton tabUI = createTab(I18n.msg("gui.ui.interfacesettings"), new Image(skin.getDrawable("iconic-browser")), skin);
        final OwnTextIconButton tabPerformance = createTab(I18n.msg("gui.performance"), new Image(skin.getDrawable("iconic-dial")), skin);
        final OwnTextIconButton tabControls = createTab(I18n.msg("gui.controls"), new Image(skin.getDrawable("iconic-laptop")), skin);
        final OwnTextIconButton tabScreenshots = createTab(I18n.msg("gui.screenshots"), new Image(skin.getDrawable("iconic-image")), skin);
        final OwnTextIconButton tabFrames = createTab(I18n.msg("gui.frameoutput.title"), new Image(skin.getDrawable("iconic-layers")), skin);
        final OwnTextIconButton tabCamera = createTab(I18n.msg("gui.camerarec.title"), new Image(skin.getDrawable("iconic-camera-slr")), skin);
        final OwnTextIconButton tab360 = createTab(I18n.msg("gui.360.title"), new Image(skin.getDrawable("iconic-cubemap")), skin);
        final OwnTextIconButton tabPlanetarium = createTab(I18n.msg("gui.planetarium.title"), new Image(skin.getDrawable("iconic-dome")), skin);
        final OwnTextIconButton tabData = createTab(I18n.msg("gui.data"), new Image(skin.getDrawable("iconic-clipboard")), skin);
        final OwnTextIconButton tabSystem = createTab(I18n.msg("gui.system"), new Image(skin.getDrawable("iconic-terminal")), skin);

        tabsTable.add(tabGraphics).row();
        tabsTable.add(tabScene).row();
        tabsTable.add(tabUI).row();
        tabsTable.add(tabPerformance).row();
        tabsTable.add(tabControls).row();
        tabsTable.add(tabScreenshots).row();
        tabsTable.add(tabFrames).row();
        tabsTable.add(tabCamera).row();
        tabsTable.add(tab360).row();
        tabsTable.add(tabPlanetarium).row();
        tabsTable.add(tabData).row();
        tabsTable.add(tabSystem).row();
        content.add(tabsTable).align(Align.left | Align.top).padLeft(pad10);

        tabButtons = new Array<>();
        tabButtons.add(tabGraphics);
        tabButtons.add(tabScene);
        tabButtons.add(tabUI);
        tabButtons.add(tabPerformance);
        tabButtons.add(tabControls);
        tabButtons.add(tabScreenshots);
        tabButtons.add(tabFrames);
        tabButtons.add(tabCamera);
        tabButtons.add(tab360);
        tabButtons.add(tabPlanetarium);
        tabButtons.add(tabData);
        tabButtons.add(tabSystem);

        // Create the tab content. Just using images here for simplicity.
        tabStack = new Stack();
        tabStack.setSize(contentWidth, contentHeight);

        /*
         * ==== GRAPHICS ====
         */
        final Table contentGraphicsTable = new Table(skin);
        final OwnScrollPane contentGraphics = new OwnScrollPane(contentGraphicsTable, skin, "minimalist-nobg");
        contentGraphics.setWidth(contentWidth);
        contentGraphics.setHeight(scrollHeight);
        contentGraphics.setScrollingDisabled(true, false);
        contentGraphics.setFadeScrollBars(false);
        contentGraphicsTable.align(Align.top | Align.left);

        // PRESETS
        Label titlePresets = new OwnLabel(I18n.msg("gui.presets"), skin, "header");
        Table presets = new Table();

        float buttonWidth = 200f;
        // Low
        OwnTextButton low = new OwnTextButton(I18n.msg("gui.presets.low"), skin);
        low.setWidth(buttonWidth);
        low.setColor(ColorUtils.gBlueC);
        low.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                /* PRESET LOW */

                // Low graphics quality.
                graphicsQuality.setSelectedIndex(GraphicsQuality.LOW.ordinal());
                // No anti-aliasing.
                antiAlias.setSelectedIndex(idxAa(AntialiasType.NONE));
                FXAAQuality = 0;
                // Legacy point style.
                pointCloudRenderer.setSelectedIndex(PointCloudMode.POINTS.ordinal());
                // Legacy line style.
                lineRenderer.setSelectedIndex(LineMode.GL_LINES.ordinal());
                // Lens flare.
                lensFlare.setValue(0f);
                Settings.settings.postprocess.lensFlare.type = LensFlareType.SIMPLE;
                // No bloom.
                bloomEffect.setValue(0f);
                // No unsharp mask.
                unsharpMask.setValue(0f);
                // No chromatic aberration.
                chromaticAberration.setValue(0f);
                // No film grain.
                filmGrain.setValue(0f);
                // No elevation representation.
                elevationSb.setSelectedIndex(ElevationType.NONE.ordinal());
                // No shadows.
                shadowsCb.setChecked(false);
                // No motion blur.
                motionBlur.setValue(0f);
                // No HDR tone mapping.
                toneMappingSelect.setSelectedIndex(ToneMapping.NONE.ordinal());
            }
        });
        low.pad(pad10, pad20, pad10, pad20);
        low.addListener(new OwnTextTooltip(I18n.msg("gui.presets.low.info"), skin));
        // Medium
        OwnTextButton medium = new OwnTextButton(I18n.msg("gui.presets.med"), skin);
        medium.setWidth(buttonWidth);
        medium.setColor(ColorUtils.gBlueC);
        medium.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                /* PRESET MEDIUM */

                // Normal graphics quality.
                graphicsQuality.setSelectedIndex(GraphicsQuality.NORMAL.ordinal());
                // FXAA anti-aliasing.
                antiAlias.setSelectedIndex(idxAa(AntialiasType.FXAA));
                FXAAQuality = 1;
                // Triangles as point style.
                pointCloudRenderer.setSelectedIndex(PointCloudMode.TRIANGLES.ordinal());
                // Polyline quadstrip line style.
                lineRenderer.setSelectedIndex(LineMode.POLYLINE_QUADSTRIP.ordinal());
                // Simple lens flare.
                lensFlare.setValue(1f);
                Settings.settings.postprocess.lensFlare.type = LensFlareType.SIMPLE;
                // Vertex displacement elevation representation.
                elevationSb.setSelectedIndex(ElevationType.REGULAR.ordinal());
                // 5 shadows, 1024.
                shadowsCb.setChecked(true);
                nShadows.setSelectedIndex(4);
                if (smResolution.getDoubleValue(0) < 1024) {
                    smResolution.setText("1024");
                }
            }
        });
        medium.pad(pad10, pad20, pad10, pad20);
        medium.addListener(new OwnTextTooltip(I18n.msg("gui.presets.med.info"), skin));
        // High
        OwnTextButton high = new OwnTextButton(I18n.msg("gui.presets.high"), skin);
        high.setWidth(buttonWidth);
        high.setColor(ColorUtils.gBlueC);
        high.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                /* PRESET HIGH */

                // Normal graphics quality.
                graphicsQuality.setSelectedIndex(GraphicsQuality.HIGH.ordinal());
                // FXAA anti-aliasing.
                antiAlias.setSelectedIndex(idxAa(AntialiasType.FXAA));
                FXAAQuality = 2;
                // Triangles as point style.
                pointCloudRenderer.setSelectedIndex(PointCloudMode.TRIANGLES.ordinal());
                // Polyline quadstrip line style.
                lineRenderer.setSelectedIndex(LineMode.POLYLINE_QUADSTRIP.ordinal());
                // Complex lens flare.
                lensFlare.setValue(1f);
                Settings.settings.postprocess.lensFlare.type = LensFlareType.COMPLEX;
                // Tessellation elevation representation.
                elevationSb.setSelectedIndex(ElevationType.TESSELLATION.ordinal());
                // 6 shadows, 2048.
                shadowsCb.setChecked(true);
                nShadows.setSelectedIndex(5);
                if (smResolution.getDoubleValue(0) < 2048) {
                    smResolution.setText("2048");
                }
            }
        });
        high.pad(pad10, pad20, pad10, pad20);
        high.addListener(new OwnTextTooltip(I18n.msg("gui.presets.high.info"), skin));

        presets.add(low).left().padRight(pad34).padBottom(pad10);
        presets.add(new OwnLabel(TextUtils.breakCharacters(I18n.msg("gui.presets.low.info"), 80), skin)).left().padBottom(pad10).padRight(pad34);
        presets.add(getRequiresRestartLabel()).left().padBottom(pad10).row();
        presets.add(medium).center().padRight(pad34).padBottom(pad10);
        presets.add(new OwnLabel(TextUtils.breakCharacters(I18n.msg("gui.presets.med.info"), 80), skin)).left().padBottom(pad10).padRight(pad34);
        presets.add(getRequiresRestartLabel()).left().padBottom(pad10).row();
        presets.add(high).center().padRight(pad34).padBottom(pad10);
        presets.add(new OwnLabel(TextUtils.breakCharacters(I18n.msg("gui.presets.high.info"), 80), skin)).left().padBottom(pad10).padRight(pad34);
        presets.add(getRequiresRestartLabel()).left().padBottom(pad10).row();

        // Add to content
        addContentGroup(contentGraphicsTable, titlePresets, presets, 0f);

        // RESOLUTION/MODE
        Label titleResolution = new OwnLabel(I18n.msg("gui.resolutionmode"), skin, "header");
        Table mode = new Table();

        // Full screen mode resolutions
        Array<DisplayMode> modes = new Array<>(Gdx.graphics.getDisplayModes());
        modes.sort((o1, o2) -> Integer.compare(o2.height * o2.width, o1.height * o1.width));
        fullScreenResolutions = new OwnSelectBox<>(skin);
        fullScreenResolutions.setWidth(selectWidth);
        fullScreenResolutions.setItems(modes);

        DisplayMode selectedMode = null;
        for (DisplayMode dm : modes) {
            if (dm.width == settings.graphics.fullScreen.resolution[0] && dm.height == settings.graphics.fullScreen.resolution[1]
                    && dm.bitsPerPixel == settings.graphics.fullScreen.bitDepth && dm.refreshRate == settings.graphics.fullScreen.refreshRate) {
                selectedMode = dm;
                break;
            }
        }
        if (selectedMode != null) {
            fullScreenResolutions.setSelected(selectedMode);
        }

        // Get current resolution
        Table windowedResolutions = new Table(skin);
        IValidator widthValidator = new IntValidator(100, 10000);
        widthField = new OwnTextField("", skin, widthValidator);
        widthField.setWidth(inputSmallWidth);
        IValidator heightValidator = new IntValidator(100, 10000);
        heightField = new OwnTextField("", skin, heightValidator);
        heightField.setWidth(inputSmallWidth);
        final OwnLabel xLabel = new OwnLabel("x", skin);
        populateWidthHeight(false);

        windowedResolutions.add(widthField).left().padRight(pad10);
        windowedResolutions.add(xLabel).left().padRight(pad10);
        windowedResolutions.add(heightField).left().row();

        // Radio buttons
        fullScreen = new OwnCheckBox(I18n.msg("gui.fullscreen"), skin, "radio", pad10);
        fullScreen.addListener(event -> {
            if (event instanceof ChangeEvent) {
                selectFullscreen(fullScreen.isChecked(), widthField, heightField, fullScreenResolutions, xLabel);
                return true;
            }
            return false;
        });
        fullScreen.setChecked(settings.graphics.fullScreen.active);

        windowed = new OwnCheckBox(I18n.msg("gui.windowed"), skin, "radio", pad10);
        windowed.addListener(event -> {
            if (event instanceof ChangeEvent) {
                selectFullscreen(!windowed.isChecked(), widthField, heightField, fullScreenResolutions, xLabel);
                return true;
            }
            return false;
        });
        windowed.setChecked(!settings.graphics.fullScreen.active);
        selectFullscreen(settings.graphics.fullScreen.active, widthField, heightField, fullScreenResolutions, xLabel);

        new ButtonGroup<>(fullScreen, windowed);

        // VSYNC
        OwnLabel vsyncLabel = new OwnLabel(I18n.msg("gui.vsync"), skin);
        OwnCheckBox vSync = new OwnCheckBox("", skin);
        vSync.setName("V-sync");
        vSync.setChecked(settings.graphics.vsync);
        vsyncValue = new AtomicBoolean(settings.graphics.vsync);
        vSync.addListener(e -> {
            if (e instanceof ChangeEvent ce) {
                vsyncValue.set(((OwnCheckBox) ce.getTarget()).isChecked());
                return true;
            }
            return false;
        });

        // LIMIT FPS
        IValidator limitFpsValidator = new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS);
        double limitFps = settings.graphics.fpsLimit <= 0 ? 60 : settings.graphics.fpsLimit;
        this.maxFpsInput = new OwnTextField(nf3.format(MathUtilsDouble.clamp(limitFps, Constants.MIN_FPS, Constants.MAX_FPS)), skin, limitFpsValidator);
        this.maxFpsInput.setDisabled(settings.graphics.fpsLimit <= 0);

        OwnLabel maxFpsLabel = new OwnLabel(I18n.msg("gui.limitfps"), skin);
        maxFps = new OwnCheckBox("", skin);
        maxFps.setChecked(settings.graphics.fpsLimit > 0);
        maxFps.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                enableComponents(maxFps.isChecked(), this.maxFpsInput);
                return true;
            }
            return false;
        });
        HorizontalGroup maxFpsGroup = new HorizontalGroup();
        maxFpsGroup.space(pad18);
        maxFpsGroup.addActor(maxFps);
        maxFpsGroup.addActor(this.maxFpsInput);

        labels.addAll(vsyncLabel, maxFpsLabel);

        mode.add(fullScreen).left().padRight(pad18);
        mode.add(fullScreenResolutions).left().row();
        mode.add(windowed).left().padRight(pad18).padTop(pad18).padBottom(pad18);
        mode.add(windowedResolutions).left().padTop(pad18).padBottom(pad18).row();
        mode.add(vsyncLabel).left().padRight(pad34).padBottom(pad10);
        mode.add(vSync).left().padBottom(pad10).row();
        mode.add(maxFpsLabel).left().padRight(pad34).padBottom(pad10);
        mode.add(maxFpsGroup).left().padBottom(pad10).row();

        // Add to content
        addContentGroup(contentGraphicsTable, titleResolution, mode);

        // GRAPHICS SETTINGS
        Label titleGraphics = new OwnLabel(I18n.msg("gui.graphicssettings"), skin, "header");
        Table graphics = new Table();

        OwnLabel graphicsQualityLabel = new OwnLabel(I18n.msg("gui.gquality"), skin);
        graphicsQualityLabel.addListener(new OwnTextTooltip(I18n.msg("gui.gquality.info"), skin));

        ComboBoxBean[] gqs = new ComboBoxBean[GraphicsQuality.values().length];
        int i = 0;
        for (GraphicsQuality q : GraphicsQuality.values()) {
            gqs[i] = new ComboBoxBean(I18n.msg(q.key), q.ordinal());
            i++;
        }
        graphicsQuality = new OwnSelectBox<>(skin);
        graphicsQuality.setItems(gqs);
        graphicsQuality.setWidth(selectWidth);
        graphicsQuality.setSelected(gqs[settings.graphics.quality.ordinal()]);
        graphicsQuality.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                ComboBoxBean s = graphicsQuality.getSelected();
                GraphicsQuality gq = GraphicsQuality.values()[s.value];
                if ((DataDescriptor.localDataDescriptor == null || !DataDescriptor.localDataDescriptor.datasetPresent(Constants.HI_RES_TEXTURES_DATASET_KEY)) && (gq.isHigh()
                        || gq.isUltra())) {
                    // Show notice
                    // Hi resolution textures notice
                    if (noticeHiResCell != null && noticeHiResCell.getActor() == null) {
                        String infoString = I18n.msg("gui.gquality.hires.info") + "\n";
                        int lines1 = GlobalResources.countOccurrences(infoString, '\n');
                        OwnTextArea noticeHiRes = new OwnTextArea(infoString, skin, "info");
                        noticeHiRes.setDisabled(true);
                        noticeHiRes.setPrefRows(lines1 + 1);
                        noticeHiRes.setWidth(1200f);
                        noticeHiRes.clearListeners();
                        noticeHiResCell.setActor(noticeHiRes);
                    }
                } else {
                    // Hide notice
                    if (noticeHiResCell != null) {
                        noticeHiResCell.setActor(null);
                    }

                }
            }
            return false;
        });

        OwnImageButton gQualityTooltip = new OwnImageButton(skin, "tooltip");
        gQualityTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.gquality.info"), skin));

        // AA
        OwnLabel aaLabel = new OwnLabel(I18n.msg("gui.aa"), skin);
        aaLabel.addListener(new OwnTextTooltip(I18n.msg("gui.aa.info"), skin));

        ComboBoxBean[] aas = new ComboBoxBean[]{new ComboBoxBean(I18n.msg("gui.aa.no"), 0), new ComboBoxBean(I18n.msg("gui.aa.fxaa"), -1),
                new ComboBoxBean(I18n.msg("gui.aa.nfaa"), -2)};
        antiAlias = new OwnSelectBox<>(skin);
        antiAlias.setItems(aas);
        antiAlias.setWidth(selectWidth);
        antiAlias.setSelected(aas[idxAa(settings.postprocess.antialiasing.type)]);
        FXAAQuality = settings.postprocess.antialiasing.quality;

        OwnImageButton aaTooltip = new OwnImageButton(skin, "tooltip");
        aaTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.aa.info"), skin));

        // Only if not VR, the triangles break in VR
        // POINT CLOUD
        ComboBoxBean[] pointCloudItems = new ComboBoxBean[]{
                new ComboBoxBean(I18n.msg("gui.pointcloud.tris"), PointCloudMode.TRIANGLES.ordinal()),
                new ComboBoxBean(I18n.msg("gui.pointcloud.points"), PointCloudMode.POINTS.ordinal())};
        pointCloudRenderer = new OwnSelectBox<>(skin);
        pointCloudRenderer.setItems(pointCloudItems);
        pointCloudRenderer.setWidth(selectWidth);
        pointCloudRenderer.setSelected(pointCloudItems[settings.scene.renderer.pointCloud.ordinal()]);

        // LINE RENDERER
        OwnLabel lrLabel = new OwnLabel(I18n.msg("gui.linerenderer"), skin);
        ComboBoxBean[] lineRenderers = new ComboBoxBean[]{
                new ComboBoxBean(I18n.msg("gui.linerenderer.quads"), LineMode.POLYLINE_QUADSTRIP.ordinal()),
                new ComboBoxBean(I18n.msg("gui.linerenderer.lines"), LineMode.GL_LINES.ordinal())};
        lineRenderer = new OwnSelectBox<>(skin);
        lineRenderer.setItems(lineRenderers);
        lineRenderer.setWidth(selectWidth);
        lineRenderer.setSelected(lineRenderers[settings.scene.renderer.line.mode.ordinal()]);
        // Disable in safe mode
        lrLabel.setDisabled(safeMode);
        lineRenderer.setDisabled(safeMode);

        // BLOOM
        OwnLabel bloomLabel = new OwnLabel(I18n.msg("gui.bloom"), skin, "default");
        bloomEffect = new OwnSliderPlus("", Constants.MIN_BLOOM, Constants.MAX_BLOOM, Constants.SLIDER_STEP_TINY, skin);
        bloomEffect.setName("bloom effect");
        bloomEffect.setWidth(sliderWidth);
        bloomEffect.setValue(settings.postprocess.bloom.intensity);
        bloomEffect.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.BLOOM_CMD, bloomEffect, bloomEffect.getValue());
                return true;
            }
            return false;
        });

        // UNSHARP MASK
        OwnLabel unsharpMaskLabel = new OwnLabel(I18n.msg("gui.unsharpmask"), skin, "default");
        unsharpMask = new OwnSliderPlus("", Constants.MIN_UNSHARP_MASK_FACTOR, Constants.MAX_UNSHARP_MASK_FACTOR, Constants.SLIDER_STEP_TINY, skin);
        unsharpMask.setName("unsharp mask factor");
        unsharpMask.setWidth(sliderWidth);
        unsharpMask.setValue(settings.postprocess.unsharpMask.factor);
        unsharpMask.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.UNSHARP_MASK_CMD, unsharpMask, unsharpMask.getValue());
                return true;
            }
            return false;
        });

        // CHROMATIC ABERRATION
        OwnLabel chromaticAberrationLabel = new OwnLabel(I18n.msg("gui.chromaticaberration"), skin, "default");
        chromaticAberration = new OwnSliderPlus("", Constants.MIN_CHROMATIC_ABERRATION_AMOUNT, Constants.MAX_CHROMATIC_ABERRATION_AMOUNT,
                Constants.SLIDER_STEP_TINY * 0.1f, skin);
        chromaticAberration.setName("chromatic aberration amount");
        chromaticAberration.setWidth(sliderWidth);
        chromaticAberration.setValue(settings.postprocess.chromaticAberration.amount);
        chromaticAberration.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CHROMATIC_ABERRATION_CMD, chromaticAberration, chromaticAberration.getValue());
                return true;
            }
            return false;
        });

        // FILM GRAIN
        OwnLabel filmGrainLabel = new OwnLabel(I18n.msg("gui.filmgrain"), skin, "default");
        filmGrain = new OwnSliderPlus("", Constants.MIN_FILM_GRAIN_INTENSITY, Constants.MAX_FILM_GRAIN_INTENSITY,
                Constants.SLIDER_STEP_TINY * 0.1f, skin);
        filmGrain.setName("film grain intensity");
        filmGrain.setWidth(sliderWidth);
        filmGrain.setValue(settings.postprocess.filmGrain.intensity);
        filmGrain.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.FILM_GRAIN_CMD, filmGrain, filmGrain.getValue());
                return true;
            }
            return false;
        });

        // LABELS
        labels.addAll(graphicsQualityLabel, aaLabel, lrLabel, bloomLabel, chromaticAberrationLabel, filmGrainLabel);

        // LENS FLARE
        OwnLabel lensFlareLabel = new OwnLabel(I18n.msg("gui.lensflare"), skin);
        lensFlare = new OwnSliderPlus("", Constants.MIN_LENS_FLARE_STRENGTH, Constants.MAX_LENS_FLARE_STRENGTH, Constants.SLIDER_STEP_TINY, skin);
        lensFlare.setName("lens flare strength");
        lensFlare.setWidth(sliderWidth);
        lensFlare.setValue(settings.postprocess.lensFlare.strength);
        lensFlare.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.LENS_FLARE_CMD, lensFlare, lensFlare.getValue());
                return true;
            }
            return false;
        });

        // FADE TIME
        OwnLabel fadeTimeLabel = new OwnLabel(I18n.msg("gui.fadetime"), skin, "default");
        IValidator fadeTimeValidator = new LongValidator(Constants.MIN_FADE_TIME_MS, Constants.MAX_FADE_TIME_MS);
        fadeTimeField = new OwnTextField(Long.toString(settings.scene.fadeMs), skin, fadeTimeValidator);
        fadeTimeField.setWidth(inputWidth);
        OwnImageButton fadeTimeTooltip = new OwnImageButton(skin, "tooltip");
        fadeTimeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.fadetime.info"), skin));

        graphics.add(graphicsQualityLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(graphicsQuality).left().padRight(pad18).padBottom(pad10);
        graphics.add(gQualityTooltip).left().padBottom(pad10).padRight(pad10);
        graphics.add(getRequiresRestartLabel()).width(40).left().padBottom(pad10).row();
        noticeHiResCell = graphics.add();
        noticeHiResCell.colspan(3).left().row();
        graphics.add(aaLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(antiAlias).left().padRight(pad18).padBottom(pad10);
        graphics.add(aaTooltip).left().padBottom(pad10).row();
        OwnLabel pointCloudLabel = new OwnLabel(I18n.msg("gui.pointcloud"), skin);
        OwnImageButton pointCloudTooltip = new OwnImageButton(skin, "tooltip");
        pointCloudTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.pointcloud.info"), skin));
        graphics.add(pointCloudLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(pointCloudRenderer).left().padBottom(pad10);
        graphics.add(pointCloudTooltip).left().padRight(pad10).padBottom(pad10);
        graphics.add(getRequiresRestartLabel()).width(40).left().padBottom(pad10).row();
        OwnImageButton lineTooltip = new OwnImageButton(skin, "tooltip");
        lineTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.linerenderer.info"), skin));
        graphics.add(lrLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(lineRenderer).left().padBottom(pad10);
        graphics.add(lineTooltip).left().padRight(pad10).padBottom(pad10).row();
        graphics.add(lensFlareLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(lensFlare).left().padBottom(pad10).row();
        graphics.add(bloomLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(bloomEffect).left().padBottom(pad10).row();
        graphics.add(unsharpMaskLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(unsharpMask).left().padBottom(pad10).row();
        graphics.add(chromaticAberrationLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(chromaticAberration).left().padBottom(pad10).row();
        graphics.add(filmGrainLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(filmGrain).left().padBottom(pad10).row();
        graphics.add(fadeTimeLabel).left().padRight(pad34).padBottom(pad10);
        graphics.add(fadeTimeField).left().padRight(pad18).padBottom(pad10);
        graphics.add(fadeTimeTooltip).left().padRight(pad34).padBottom(pad10).row();

        // Add to content
        addContentGroup(contentGraphicsTable, titleGraphics, graphics);

        // ELEVATION
        Label titleElevation = new OwnLabel(I18n.msg("gui.elevation.title"), skin, "header");
        Table elevation = new Table();

        // ELEVATION TYPE
        OwnLabel elevationTypeLabel = new OwnLabel(I18n.msg("gui.elevation.type"), skin);
        ElevationComboBoxBean[] ecbb = new ElevationComboBoxBean[ElevationType.values().length];
        i = 0;
        for (ElevationType et : ElevationType.values()) {
            ecbb[i] = new ElevationComboBoxBean(I18n.msg("gui.elevation.type." + et.toString().toLowerCase()), et);
            i++;
        }
        elevationSb = new OwnSelectBox<>(skin);
        elevationSb.setItems(ecbb);
        elevationSb.setWidth(selectWidth);
        elevationSb.setSelectedIndex(Settings.settings.scene.renderer.elevation.type.ordinal());
        elevationSb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                enableComponents(elevationSb.getSelected().type.isTessellation(), tessQuality, tessQualityLabel);
            }
            return false;
        });
        // Disable in safe mode.
        elevationTypeLabel.setDisabled(safeMode);
        elevationSb.setDisabled(safeMode);

        // TESSELLATION QUALITY
        tessQualityLabel = new OwnLabel(I18n.msg("gui.elevation.tessellation.quality"), skin);
        tessQualityLabel.setDisabled(!settings.scene.renderer.elevation.type.isTessellation());

        tessQuality = new OwnSliderPlus("", Constants.MIN_TESS_QUALITY, Constants.MAX_TESS_QUALITY, 0.1f, skin);
        tessQuality.setDisabled(!settings.scene.renderer.elevation.type.isTessellation());
        tessQuality.setWidth(sliderWidth);
        tessQuality.setValue((float) settings.scene.renderer.elevation.quality);

        // LABELS
        labels.add(elevationTypeLabel, tessQualityLabel);

        elevation.add(elevationTypeLabel).left().padRight(pad34).padBottom(pad10);
        elevation.add(elevationSb).left().padRight(pad18).padBottom(pad10);
        elevation.add(getRequiresRestartLabel()).left().padBottom(pad10).row();
        elevation.add(tessQualityLabel).left().padRight(pad34).padBottom(pad10);
        elevation.add(tessQuality).left().padRight(pad18).padBottom(pad10);

        // Add to content
        addContentGroup(contentGraphicsTable, titleElevation, elevation);

        // SHADOWS
        Label titleShadows = new OwnLabel(I18n.msg("gui.graphics.shadows"), skin, "header");
        Table shadows = new Table();

        // SHADOW MAP RESOLUTION
        OwnLabel smResolutionLabel = new OwnLabel(I18n.msg("gui.graphics.shadows.resolution"), skin);
        smResolutionLabel.setDisabled(!settings.scene.renderer.shadow.active);
        IntValidator smResValidator = new IntValidator(128, GaiaSky.instance.maxTextureSize);
        smResolution = new OwnTextField(Integer.toString(MathUtils.clamp(settings.scene.renderer.shadow.resolution, 128, GaiaSky.instance.maxTextureSize)), skin, smResValidator);
        smResolution.setWidth(inputWidth);
        smResolution.setDisabled(!settings.scene.renderer.shadow.active);

        // N SHADOWS
        OwnLabel nShadowsLabel = new OwnLabel("#" + I18n.msg("gui.graphics.shadows"), skin);
        nShadowsLabel.setDisabled(!settings.scene.renderer.shadow.active);

        int nSh = 10;
        ComboBoxBean[] nsh = new ComboBoxBean[nSh];
        IntStream.rangeClosed(1, nSh).forEach(s -> nsh[s - 1] = new ComboBoxBean(String.valueOf(s), s));

        nShadows = new OwnSelectBox<>(skin);
        nShadows.setItems(nsh);
        nShadows.setWidth(selectWidth);
        nShadows.setSelected(nsh[settings.scene.renderer.shadow.number - 1]);
        nShadows.setDisabled(!settings.scene.renderer.shadow.active);

        // ENABLE SHADOWS
        OwnLabel shadowsLabel = new OwnLabel(I18n.msg("gui.graphics.shadows.enable"), skin);
        shadowsCb = new OwnCheckBox("", skin);
        shadowsCb.setChecked(settings.scene.renderer.shadow.active);
        shadowsCb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Enable or disable resolution
                enableComponents(shadowsCb.isChecked(), smResolution, smResolutionLabel, nShadows, nShadowsLabel);
                return true;
            }
            return false;
        });

        // LABELS
        labels.add(smResolutionLabel);

        shadows.add(shadowsLabel).left().padRight(pad34).padBottom(pad10);
        shadows.add(shadowsCb).left().padRight(pad18).padBottom(pad10).row();
        shadows.add(smResolutionLabel).left().padRight(pad34).padBottom(pad10);
        shadows.add(smResolution).left().padRight(pad18).padBottom(pad10).row();
        shadows.add(nShadowsLabel).left().padRight(pad34).padBottom(pad10);
        shadows.add(nShadows).left().padRight(pad18).padBottom(pad10);

        // Add to content
        addContentGroup(contentGraphicsTable, titleShadows, shadows);

        // IMAGE LEVELS
        Label titleDisplay = new OwnLabel(I18n.msg("gui.graphics.imglevels"), skin, "header");
        Table imageLevels = new Table();


        /* Brightness */
        OwnLabel brightnessLabel = new OwnLabel(I18n.msg("gui.brightness"), skin, "default");
        Slider brightness = new OwnSliderPlus("", Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        brightness.setName("brightness");
        brightness.setWidth(sliderWidth);
        brightness.setValue(MathUtilsDouble.lint(settings.postprocess.levels.brightness, Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS, Constants.MIN_SLIDER,
                Constants.MAX_SLIDER));
        brightness.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.BRIGHTNESS_CMD, brightness,
                        MathUtilsDouble.lint(brightness.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_BRIGHTNESS,
                                Constants.MAX_BRIGHTNESS), true);
                return true;
            }
            return false;
        });

        imageLevels.add(brightnessLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(brightness).left().padRight(pad18).padBottom(pad10).row();

        /* Contrast */
        OwnLabel contrastLabel = new OwnLabel(I18n.msg("gui.contrast"), skin, "default");
        Slider contrast = new OwnSliderPlus("", Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        contrast.setName("contrast");
        contrast.setWidth(sliderWidth);
        contrast.setValue(
                MathUtilsDouble.lint(settings.postprocess.levels.contrast, Constants.MIN_CONTRAST, Constants.MAX_CONTRAST, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        contrast.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CONTRAST_CMD, contrast,
                        MathUtilsDouble.lint(contrast.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_CONTRAST,
                                Constants.MAX_CONTRAST), true);
                return true;
            }
            return false;
        });

        imageLevels.add(contrastLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(contrast).left().padRight(pad18).padBottom(pad10).row();

        /* Hue */
        OwnLabel hueLabel = new OwnLabel(I18n.msg("gui.hue"), skin, "default");
        Slider hue = new OwnSliderPlus("", Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        hue.setName("hue");
        hue.setWidth(sliderWidth);
        hue.setValue(MathUtilsDouble.lint(settings.postprocess.levels.hue, Constants.MIN_HUE, Constants.MAX_HUE, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        hue.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.HUE_CMD, hue,
                        MathUtilsDouble.lint(hue.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_HUE, Constants.MAX_HUE), true);
                return true;
            }
            return false;
        });

        imageLevels.add(hueLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(hue).left().padRight(pad18).padBottom(pad10).row();

        /* Saturation */
        OwnLabel saturationLabel = new OwnLabel(I18n.msg("gui.saturation"), skin, "default");
        Slider saturation = new OwnSliderPlus("", Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        saturation.setName("saturation");
        saturation.setWidth(sliderWidth);
        saturation.setValue(MathUtilsDouble.lint(settings.postprocess.levels.saturation, Constants.MIN_SATURATION, Constants.MAX_SATURATION, Constants.MIN_SLIDER,
                Constants.MAX_SLIDER));
        saturation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SATURATION_CMD, saturation,
                        MathUtilsDouble.lint(saturation.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_SATURATION,
                                Constants.MAX_SATURATION), true);
                return true;
            }
            return false;
        });

        imageLevels.add(saturationLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(saturation).left().padRight(pad18).padBottom(pad10).row();

        /* Gamma */
        OwnLabel gammaLabel = new OwnLabel(I18n.msg("gui.gamma"), skin, "default");
        Slider gamma = new OwnSliderPlus("", Constants.MIN_GAMMA, Constants.MAX_GAMMA, Constants.SLIDER_STEP_TINY, false, skin);
        gamma.setName("gamma");
        gamma.setWidth(sliderWidth);
        gamma.setValue(settings.postprocess.levels.gamma);
        gamma.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.GAMMA_CMD, gamma, gamma.getValue(), true);
                return true;
            }
            return false;
        });

        imageLevels.add(gammaLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(gamma).left().padRight(pad18).padBottom(pad10).row();

        /* Tone Mapping */
        OwnLabel toneMappingLabel = new OwnLabel(I18n.msg("gui.tonemapping.type"), skin, "default");
        int nToneMapping = ToneMapping.values().length;
        ComboBoxBean[] toneMappingTypes = new ComboBoxBean[nToneMapping];
        for (int itm = 0; itm < nToneMapping; itm++) {
            ToneMapping tm = ToneMapping.values()[itm];
            toneMappingTypes[itm] = new ComboBoxBean(I18n.msg("gui.tonemapping." + tm.name().toLowerCase(Locale.ROOT)), tm.ordinal());
        }

        toneMappingSelect = new OwnSelectBox<>(skin);
        toneMappingSelect.setItems(toneMappingTypes);
        toneMappingSelect.setWidth(selectWidth);
        toneMappingSelect.setSelectedIndex(settings.postprocess.toneMapping.type.ordinal());
        imageLevels.add(toneMappingLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(toneMappingSelect).left().padBottom(pad10).row();

        /* Exposure */
        OwnLabel exposureLabel = new OwnLabel(I18n.msg("gui.exposure"), skin, "default");
        exposureLabel.setDisabled(settings.postprocess.toneMapping.type != ToneMapping.EXPOSURE);
        Slider exposure = new OwnSliderPlus("", Constants.MIN_EXPOSURE, Constants.MAX_EXPOSURE, 0.1f, false, skin);
        exposure.setName("exposure");
        exposure.setWidth(sliderWidth);
        exposure.setValue(settings.postprocess.toneMapping.exposure);
        exposure.setDisabled(settings.postprocess.toneMapping.type != ToneMapping.EXPOSURE);
        exposure.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.EXPOSURE_CMD, exposure, exposure.getValue());
                return true;
            }
            return false;
        });
        toneMappingSelect.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                ToneMapping newTM = ToneMapping.values()[toneMappingSelect.getSelectedIndex()];
                EventManager.publish(Event.TONEMAPPING_TYPE_CMD, toneMappingSelect, newTM);
                boolean disabled = newTM != ToneMapping.EXPOSURE;
                exposureLabel.setDisabled(disabled);
                exposure.setDisabled(disabled);
                return true;
            }
            return false;
        });

        imageLevels.add(exposureLabel).left().padRight(pad34).padBottom(pad10);
        imageLevels.add(exposure).left().padRight(pad18).padBottom(pad10).row();

        // LABELS
        labels.addAll(brightnessLabel, contrastLabel, hueLabel, saturationLabel, gammaLabel);

        // Add to content
        addContentGroup(contentGraphicsTable, titleDisplay, imageLevels);

        if (!vr) {
            // VIRTUAL TEXTURES
            Label titleSVT = new OwnLabel(I18n.msg("gui.svt"), skin, "header");
            Table svtTable = new Table();

            /* Cache size */
            OwnLabel svtCacheSizeLabel = new OwnLabel(I18n.msg("gui.svt.cachesize"), skin, "default");
            svtCacheSize = new OwnSliderPlus("", Constants.MIN_TILE_CACHE, Constants.MAX_TILE_CACHE, 1, skin);
            svtCacheSize.setValueLabelTransform((val) -> Integer.toString((int) (val * val)));
            svtCacheSize.setName("cacheSize");
            svtCacheSize.setWidth(sliderWidth);
            svtCacheSize.setValue(settings.scene.renderer.virtualTextures.cacheSize);

            svtTable.add(svtCacheSizeLabel).left().padRight(pad34).padBottom(pad10);
            svtTable.add(svtCacheSize).left().padRight(pad18).padBottom(pad10);
            svtTable.add(getRequiresRestartLabel()).left().padBottom(pad10).row();

            labels.addAll(svtCacheSizeLabel);

            // Add to content
            addContentGroup(contentGraphicsTable, titleSVT, svtTable);

            // EXPERIMENTAL
            Label titleExperimental = new OwnLabel(I18n.msg("gui.experimental"), skin, "header");
            Table experimental = new Table();

            // Re-projection
            OwnLabel reprojectionLabel = new OwnLabel(I18n.msg("gui.reproj"), skin);
            ReprojectionMode[] reprojectionModes = ReprojectionMode.values();
            reprojectionMode = new OwnSelectBox<>(skin);
            reprojectionMode.setItems(reprojectionModes);
            reprojectionMode.setWidth(selectWidth);
            if (!settings.postprocess.reprojection.active) {
                reprojectionMode.setSelected(reprojectionModes[ReprojectionMode.DISABLED.ordinal()]);
            } else {
                reprojectionMode.setSelected(reprojectionModes[settings.postprocess.reprojection.mode.ordinal()]);
            }
            reprojectionMode.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    var newMode = reprojectionMode.getSelected();
                    EventManager.publish(Event.REPROJECTION_CMD, this, newMode != ReprojectionMode.DISABLED, newMode);
                    return true;
                }
                return false;
            });

            experimental.add(reprojectionLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(reprojectionMode).left().padRight(pad18).padBottom(pad10).row();

            // Dynamic resolution
            OwnLabel dynamicResolutionLabel = new OwnLabel(I18n.msg("gui.dynamicresolution"), skin);
            dynamicResolution = new OwnCheckBox("", skin);
            dynamicResolution.setChecked(settings.graphics.dynamicResolution);
            OwnImageButton dynamicResolutionTooltip = new OwnImageButton(skin, "tooltip");
            dynamicResolutionTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.dynamicresolution.info"), skin));

            experimental.add(dynamicResolutionLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(dynamicResolution).left().padRight(pad18).padBottom(pad10);
            experimental.add(dynamicResolutionTooltip).left().padBottom(pad10).row();

            // Back-buffer scale
            OwnLabel backBufferScaleLabel = new OwnLabel(I18n.msg("gui.backbuffer.scale"), skin);
            backBufferScaleLabel.setDisabled(settings.graphics.dynamicResolution);
            backBufferScale = new OwnSliderPlus("", Constants.BACKBUFFER_SCALE_MIN, Constants.BACKBUFFER_SCALE_MAX, Constants.BACKBUFFER_SCALE_STEP, skin);
            backBufferScale.setWidth(sliderWidth);
            backBufferScale.setMappedValue(settings.graphics.backBufferScale);
            backBufferScale.setDisabled(settings.graphics.dynamicResolution);
            OwnImageButton backBufferTooltip = new OwnImageButton(skin, "tooltip");
            backBufferTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.backbuffer.scale.info"), skin));
            dynamicResolution.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    backBufferScale.setDisabled(dynamicResolution.isChecked());
                    backBufferScaleLabel.setDisabled(dynamicResolution.isChecked());
                    return true;
                }
                return false;
            });

            experimental.add(backBufferScaleLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(backBufferScale).left().padRight(pad18).padBottom(pad10);
            experimental.add(backBufferTooltip).left().padBottom(pad10).row();

            // Upscale filter
            OwnLabel upscaleFilterLabel = new OwnLabel(I18n.msg("gui.upscale.filter"), skin);
            UpscaleFilter[] upscaleFilterValues = UpscaleFilter.values();
            upscaleFilter = new OwnSelectBox<>(skin);
            upscaleFilter.setItems(upscaleFilterValues);
            upscaleFilter.setWidth(selectWidth);
            upscaleFilter.setSelected(upscaleFilterValues[settings.postprocess.upscaleFilter.ordinal()]);
            upscaleFilter.addListener(new OwnTextTooltip(I18n.msg("gui.upscale.filter.info"), skin));
            upscaleFilter.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    var newMode = upscaleFilter.getSelected();
                    EventManager.publish(Event.UPSCALE_FILTER_CMD, this, upscaleFilter.getSelected());
                    return true;
                }
                return false;
            });

            experimental.add(upscaleFilterLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(upscaleFilter).left().padRight(pad18).padBottom(pad10).row();

            // Index of refraction of celestial sphere
            OwnLabel celestialSphereIndexOfRefractionLabel = new OwnLabel(I18n.msg("gui.indexofrefraction"), skin);
            celestialSphereIndexOfRefraction = new OwnSliderPlus("", 1.f, 2.5f, 0.05f, skin);
            celestialSphereIndexOfRefraction.setWidth(sliderWidth);
            celestialSphereIndexOfRefraction.setMappedValue(settings.program.modeCubemap.celestialSphereIndexOfRefraction);
            OwnImageButton celestialSphereIndexOfRefractionTooltip = new OwnImageButton(skin, "tooltip");
            celestialSphereIndexOfRefractionTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.indexofrefraction.info"), skin));

            experimental.add(celestialSphereIndexOfRefractionLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(celestialSphereIndexOfRefraction).left().padRight(pad18).padBottom(pad10);
            experimental.add(celestialSphereIndexOfRefractionTooltip).left().padBottom(pad10).row();

            // SSR
            OwnLabel ssrLabel = new OwnLabel(I18n.msg("gui.ssr"), skin);
            ssr = new OwnCheckBox("", skin);
            ssr.setChecked(!safeMode && settings.postprocess.ssr.active);
            ssr.setDisabled(safeMode);
            OwnImageButton ssrTooltip = new OwnImageButton(skin, "tooltip");
            ssrTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.ssr.info"), skin));

            experimental.add(ssrLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(ssr).left().padRight(pad18).padBottom(pad10);
            experimental.add(ssrTooltip).left().padBottom(pad10).row();

            // MOTION BLUR
            OwnLabel motionBlurLabel = new OwnLabel(I18n.msg("gui.motionblur"), skin);
            motionBlur = new OwnSliderPlus("", Constants.MOTIONBLUR_MIN, Constants.MOTIONBLUR_MAX, Constants.SLIDER_STEP_TINY, skin);
            motionBlur.setWidth(sliderWidth);
            motionBlur.setMappedValue(settings.postprocess.motionBlur.strength);
            motionBlur.addListener(event -> {
                if (event instanceof ChangeEvent ce) {
                    EventManager.publish(Event.MOTION_BLUR_CMD, this, motionBlur.getMappedValue());
                }
                return false;
            });

            experimental.add(motionBlurLabel).left().padRight(pad34).padBottom(pad10);
            experimental.add(motionBlur).left().padRight(pad18).padBottom(pad10);

            // LABELS
            labels.addAll(dynamicResolutionLabel);
            labels.addAll(ssrLabel);

            // Add to content
            addContentGroup(contentGraphicsTable, titleExperimental, experimental);
        }

        /*
         * ==== SCENE ====
         */
        final Table contentSceneTable = new Table(skin);
        final OwnScrollPane contentScene = new OwnScrollPane(contentSceneTable, skin, "minimalist-nobg");
        contentScene.setWidth(contentWidth);
        contentScene.setHeight(scrollHeight);
        contentScene.setScrollingDisabled(true, false);
        contentScene.setFadeScrollBars(false);
        contentSceneTable.align(Align.top | Align.left);

        // RECURSIVE GRID
        OwnLabel titleRecgrid = new OwnLabel(I18n.msg("gui.ui.recursivegrid"), skin, "header");
        Table rg = new Table();

        // ORIGIN
        OwnLabel originLabel = new OwnLabel(I18n.msg("gui.ui.recursivegrid.origin"), skin);
        String[] origins = new String[]{I18n.msg("gui.ui.recursivegrid.origin.refsys"), I18n.msg("gui.ui.recursivegrid.origin.focus")};
        recGridOrigin = new OwnSelectBox<>(skin);
        recGridOrigin.setWidth(selectWidth);
        recGridOrigin.setItems(origins);
        recGridOrigin.setSelectedIndex(settings.program.recursiveGrid.origin.ordinal());

        // STYLE
        OwnLabel styleLabel = new OwnLabel(I18n.msg("gui.ui.recursivegrid.style"), skin);
        String[] styles = new String[]{I18n.msg("gui.ui.recursivegrid.style.circular"), I18n.msg("gui.ui.recursivegrid.style.square")};
        recGridStyle = new OwnSelectBox<>(skin);
        recGridStyle.setWidth(selectWidth);
        recGridStyle.setItems(styles);
        recGridStyle.setSelectedIndex(settings.program.recursiveGrid.style.ordinal());

        // PROJECTION LINES
        OwnLabel recGridProjectionLinesLabel = new OwnLabel(I18n.msg("gui.ui.recursivegrid.projlines"), skin);
        recGridProjectionLines = new OwnCheckBox("", skin);
        recGridProjectionLines.setName("origin projection lines cb");
        recGridProjectionLines.setChecked(settings.program.recursiveGrid.projectionLines);

        labels.add(originLabel, styleLabel, recGridProjectionLinesLabel);

        // Add to table.
        rg.add(originLabel).left().padBottom(pad10).padRight(pad34);
        rg.add(recGridOrigin).left().padBottom(pad10).row();
        rg.add(styleLabel).left().padBottom(pad10).padRight(pad34);
        rg.add(recGridStyle).left().padBottom(pad10).row();
        rg.add(recGridProjectionLinesLabel).left().padBottom(pad10).padRight(pad34);
        rg.add(recGridProjectionLines).left().padBottom(pad10).row();

        // Add to content.
        addContentGroup(contentSceneTable, titleRecgrid, rg, 0f);

        // ECLIPSES
        Label titleEclipses = new OwnLabel(I18n.msg("gui.graphics.eclipses"), skin, "header");
        Table eclipsesTable = new Table();
        // Enable eclipses.
        OwnLabel eclipsesLabel = new OwnLabel(I18n.msg("gui.graphics.eclipses.enable"), skin);
        eclipses = new OwnCheckBox("", skin);
        eclipses.setChecked(settings.scene.renderer.eclipses.active);
        eclipses.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Enable or disable resolution
                enableComponents(eclipses.isChecked(), eclipseOutlines);
                return true;
            }
            return false;
        });
        // Eclipse outlines
        OwnLabel eclipsesOutlinesLabel = new OwnLabel(I18n.msg("gui.graphics.eclipses.outlines"), skin);
        eclipseOutlines = new OwnCheckBox("", skin);
        eclipseOutlines.setChecked(settings.scene.renderer.eclipses.outlines);

        labels.add(eclipsesLabel, eclipsesOutlinesLabel);

        eclipsesTable.add(eclipsesLabel).left().padRight(pad34).padBottom(pad10);
        eclipsesTable.add(eclipses).left().padRight(pad18).padBottom(pad10).row();
        eclipsesTable.add(eclipsesOutlinesLabel).left().padRight(pad34).padBottom(pad10);
        eclipsesTable.add(eclipseOutlines).left().padRight(pad18).padBottom(pad10);

        // Add to content
        addContentGroup(contentSceneTable, titleEclipses, eclipsesTable);

        // STARS
        OwnLabel titleStars = new OwnLabel(I18n.msg("gui.ui.scene.stars"), skin, "header");
        Table starsTable = new Table();

        // Render stars as spheres
        OwnLabel starSpheresLabel = new OwnLabel(I18n.msg("gui.ui.scene.starspheres"), skin);
        starSpheres = new OwnCheckBox("", skin);
        starSpheres.setChecked(settings.scene.star.renderStarSpheres);


        // LIGHT GLOW
        OwnLabel lightGlowLabel = new OwnLabel(I18n.msg("gui.lightscattering"), skin);
        CheckBox lightGlow = new OwnCheckBox("", skin);
        lightGlow.setName("light scattering");
        lightGlow.setChecked(settings.postprocess.lightGlow.active);
        lightGlow.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.LIGHT_GLOW_CMD, lightGlow, lightGlow.isChecked());
                return true;
            }
            return false;
        });

        labels.add(starSpheresLabel, lightGlowLabel);

        starsTable.add(lightGlowLabel).left().padRight(pad34).padBottom(pad10);
        starsTable.add(lightGlow).left().padRight(pad18).padBottom(pad10).row();
        starsTable.add(starSpheresLabel).left().padRight(pad34).padBottom(pad10);
        starsTable.add(starSpheres).left().padRight(pad18).padBottom(pad10).row();

        // Add to content
        addContentGroup(contentSceneTable, titleStars, starsTable);

        // PROCEDURAL GENERATION
        OwnLabel titleProcedural = new OwnLabel(I18n.msg("gui.ui.procedural"), skin, "header");
        Table pgen = new Table();

        // RESOLUTION
        OwnLabel pgResolutionLabel = new OwnLabel(I18n.msg("gui.ui.procedural.resolution"), skin);
        pgResolution = new OwnSliderPlus("", Constants.PG_RESOLUTION_MIN, Constants.PG_RESOLUTION_MAX, 1, skin);
        pgResolution.setValueLabelTransform((value) -> value.intValue() * 2 + "x" + value.intValue());
        pgResolution.setWidth(sliderWidth);
        pgResolution.setValue(settings.graphics.proceduralGenerationResolution[1]);

        labels.add(pgResolutionLabel);

        // Add to table.
        pgen.add(pgResolutionLabel).left().padBottom(pad10).padRight(pad34);
        pgen.add(pgResolution).left().padBottom(pad10).row();

        // SAVE TO DISK

        // Save textures
        OwnLabel saveTexturesLabel = new OwnLabel(I18n.msg("gui.procedural.savetextures"), skin);
        vSync = new OwnCheckBox("", skin);
        saveTextures = new OwnCheckBox("", skin, pad10);
        saveTextures.setChecked(Settings.settings.program.saveProceduralTextures);
        OwnImageButton saveTexturesTooltip = new OwnImageButton(skin, "tooltip");
        saveTexturesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.procedural.info.savetextures", SysUtils.getProceduralPixmapDir().toString()), skin));
        HorizontalGroup saveTexturesGroup = new HorizontalGroup();
        saveTexturesGroup.space(pad10);
        saveTexturesGroup.addActor(saveTextures);
        saveTexturesGroup.addActor(saveTexturesTooltip);

        // Add to table.
        pgen.add(saveTexturesLabel).left().padBottom(pad10).padRight(pad34);
        pgen.add(saveTexturesGroup).left().padBottom(pad10).padRight(pad34);

        // Add to content.
        addContentGroup(contentSceneTable, titleProcedural, pgen);


        /*
         * ==== UI ====
         */

        final Table contentUITable = new Table(skin);
        contentUITable.setWidth(contentWidth);
        final OwnScrollPane contentUI = new OwnScrollPane(contentUITable, skin, "minimalist-nobg");
        contentUI.setWidth(contentWidth);
        contentUI.setHeight(scrollHeight);
        contentUI.setScrollingDisabled(true, false);
        contentUI.setFadeScrollBars(false);
        contentUITable.align(Align.top | Align.left);

        OwnLabel titleUI = new OwnLabel(I18n.msg("gui.ui.interfacesettings"), skin, "header");

        Table ui = new Table();

        // LANGUAGE
        OwnLabel langLabel = new OwnLabel(I18n.msg("gui.ui.language"), skin);
        File i18nDir = new File(Settings.ASSETS_LOC + File.separator + "i18n");
        String i18nName = "gsbundle";
        String[] files = i18nDir.list();
        assert files != null;
        Array<LangComboBoxBean> langs = new Array<>();
        i = 0;
        for (String file : files) {
            if (file.startsWith(i18nName) && file.endsWith(".properties")) {
                String locale = file.substring(i18nName.length(), file.length() - ".properties".length());
                // Default locale
                if (locale.isEmpty())
                    locale = "-en-GB";

                // Remove underscore _
                locale = locale.substring(1).replace("_", "-");
                Locale loc = Locale.forLanguageTag(locale);
                langs.add(new LangComboBoxBean(loc));
            }
            i++;
        }
        langs.sort();

        lang = new OwnSelectBox<>(skin);
        lang.setWidth(selectWidth);
        lang.setItems(langs);

        String locale = settings.program.getLocale();
        int localeIndex = idxLang(locale, langs);
        if (localeIndex < 0 || localeIndex >= langs.size) {
            // Default is en_GB
            localeIndex = 2;
        }
        lang.setSelected(langs.get(localeIndex));

        // THEME
        OwnLabel themeLabel = new OwnLabel(I18n.msg("gui.ui.theme"), skin);

        StringComobBoxBean[] themes = new StringComobBoxBean[]{
                new StringComobBoxBean(I18n.msg("gui.theme.darkgreen"), "dark-green"),
                new StringComobBoxBean(I18n.msg("gui.theme.darkblue"), "dark-blue"),
                new StringComobBoxBean(I18n.msg("gui.theme.darkorange"), "dark-orange"),
                new StringComobBoxBean(I18n.msg("gui.theme.nightred"), "night-red")};
        theme = new OwnSelectBox<>(skin);
        theme.setWidth(selectWidth);
        theme.setItems(themes);
        int themeIndex;
        if (settings.program.ui.theme.contains("dark-green")) {
            themeIndex = 0;
        } else if (settings.program.ui.theme.contains("dark-blue")) {
            themeIndex = 1;
        } else if (settings.program.ui.theme.contains("dark-orange")) {
            themeIndex = 2;
        } else {
            themeIndex = 3;
        }
        theme.setSelectedIndex(themeIndex);

        // SCALING
        OwnLabel uiScaleLabel = new OwnLabel(I18n.msg("gui.ui.theme.scale"), skin);
        uiScale = new OwnSliderPlus("", Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX, Constants.SLIDER_STEP_TINY, skin);
        uiScale.setWidth(sliderWidth);
        uiScale.setValue(settings.program.ui.scale);
        OwnTextButton applyUiScale = new OwnTextButton(I18n.msg("gui.apply"), skin);
        applyUiScale.pad(0, pad18, 0, pad18);
        applyUiScale.setHeight(buttonHeight);
        applyUiScale.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.UI_SCALE_FACTOR_CMD, uiScale, uiScale.getValue());
                EventManager.publish(Event.UI_SCALE_RECOMPUTE_CMD, uiScale);
                return true;
            }
            return false;
        });

        // MINIMAP SIZE
        OwnLabel minimapSizeLabel = new OwnLabel(I18n.msg("gui.ui.minimap.size"), skin, "default");
        minimapSize = new OwnSliderPlus("", Constants.MIN_MINIMAP_SIZE, Constants.MAX_MINIMAP_SIZE, 1f, skin);
        minimapSize.setName("minimapSize");
        minimapSize.setWidth(sliderWidth);
        minimapSize.setValue(settings.program.minimap.size);

        // PREFERRED DISTANCE UNITS
        OwnLabel distUnitsLabel = new OwnLabel(I18n.msg("gui.ui.distance.units"), skin, "default");
        DistanceUnits[] dus = DistanceUnits.values();
        ComboBoxBean[] distUnits = new ComboBoxBean[dus.length];
        for (int idu = 0; idu < dus.length; idu++) {
            DistanceUnits du = dus[idu];
            distUnits[idu] = new ComboBoxBean(I18n.msg("gui.ui.distance.units." + du.name().toLowerCase(Locale.ROOT)), du.ordinal());
        }
        distUnitsSelect = new OwnSelectBox<>(skin);
        distUnitsSelect.setItems(distUnits);
        distUnitsSelect.setWidth(selectWidth);
        distUnitsSelect.setSelectedIndex(settings.program.ui.distanceUnits.ordinal());

        // DISPLAY TIME in NO-UI MODE
        OwnLabel displayTimeNoUiLabel = new OwnLabel(I18n.msg("gui.ui.nogui.time"), skin);
        displayTimeNoUi = new OwnCheckBox("", skin);
        displayTimeNoUi.setChecked(settings.program.displayTimeNoUi);

        // MODE CHANGE POP-UP CHECKBOX
        OwnLabel modeChangeInfoLabel = new OwnLabel(I18n.msg("gui.ui.modechangeinfo"), skin);
        modeChangeInfo = new OwnCheckBox("", skin);
        modeChangeInfo.setChecked(settings.program.ui.modeChangeInfo);

        // NEW UI
        OwnLabel newUILabel = new OwnLabel(I18n.msg("gui.ui.newui"), skin);
        newUI = new OwnCheckBox("", skin);
        newUI.setName("new ui cb");
        newUI.setChecked(settings.program.ui.newUI);
        OwnImageButton newUITooltip = new OwnImageButton(skin, "tooltip");
        newUITooltip.addListener(new OwnTextTooltip(I18n.msg("gui.ui.newui.info"), skin));
        HorizontalGroup newUIGroup = new HorizontalGroup();
        newUIGroup.space(pad18);
        newUIGroup.addActor(newUI);
        newUIGroup.addActor(newUITooltip);

        // LABELS
        labels.addAll(langLabel, themeLabel, uiScaleLabel, minimapSizeLabel, distUnitsLabel, modeChangeInfoLabel, newUILabel);

        // Add to table
        ui.add(langLabel).left().padRight(pad34).padBottom(pad18);
        ui.add(lang).colspan(2).left().padBottom(pad18).row();
        ui.add(themeLabel).left().padRight(pad34).padBottom(pad18);
        ui.add(theme).colspan(2).left().padBottom(pad18).row();
        ui.add(uiScaleLabel).left().padRight(pad34).padBottom(pad18);
        ui.add(uiScale).left().padRight(pad10).padBottom(pad18);
        ui.add(applyUiScale).left().padBottom(pad18).row();
        ui.add(minimapSizeLabel).left().padRight(pad10).padBottom(pad18);
        ui.add(minimapSize).colspan(2).left().padRight(pad10).padBottom(pad18).row();
        ui.add(distUnitsLabel).left().padRight(pad10).padBottom(pad18);
        ui.add(distUnitsSelect).colspan(2).left().padRight(pad10).padBottom(pad18).row();
        ui.add(displayTimeNoUiLabel).left().padRight(pad10).padBottom(pad18);
        ui.add(displayTimeNoUi).colspan(2).left().padRight(pad10).padBottom(pad18).row();
        ui.add(modeChangeInfoLabel).left().padRight(pad10).padBottom(pad18);
        ui.add(modeChangeInfo).colspan(2).left().padRight(pad10).padBottom(pad18).row();
        ui.add(newUILabel).left().padRight(pad10).padBottom(pad18);
        ui.add(newUIGroup).left().colspan(2).padBottom(pad10).row();


        /* CROSSHAIR AND MARKERS */
        OwnLabel titleCrosshair = new OwnLabel(I18n.msg("gui.ui.crosshair"), skin, "header");
        Table ch = new Table();

        // CROSSHAIR FOCUS
        OwnLabel crosshairFocusLabel = new OwnLabel(I18n.msg("gui.ui.crosshair.focus"), skin);
        crosshairFocus = new OwnCheckBox("", skin);
        crosshairFocus.setName("ch focus");
        crosshairFocus.setChecked(settings.scene.crosshair.focus);

        // CROSSHAIR CLOSEST
        OwnLabel crosshairClosestLabel = new OwnLabel(I18n.msg("gui.ui.crosshair.closest"), skin);
        crosshairClosest = new OwnCheckBox("", skin);
        crosshairClosest.setName("ch closest");
        crosshairClosest.setChecked(settings.scene.crosshair.closest);

        // CROSSHAIR HOME
        OwnLabel crosshairHomeLabel = new OwnLabel(I18n.msg("gui.ui.crosshair.home"), skin);
        crosshairHome = new OwnCheckBox("", skin);
        crosshairHome.setName("ch home");
        crosshairHome.setChecked(settings.scene.crosshair.home);

        labels.add(crosshairClosestLabel, crosshairHomeLabel, crosshairFocusLabel);

        // Add to table
        ch.add(crosshairFocusLabel).left().padRight(pad34).padBottom(pad10);
        ch.add(crosshairFocus).left().padBottom(pad10).row();
        ch.add(crosshairClosestLabel).left().padRight(pad34).padBottom(pad10);
        ch.add(crosshairClosest).left().padBottom(pad10).row();
        ch.add(crosshairHomeLabel).left().padRight(pad34).padBottom(pad10);
        ch.add(crosshairHome).left().padBottom(pad10).row();

        /* POINTER GUIDES */
        OwnLabel titleGuides = new OwnLabel(I18n.msg("gui.ui.pointer.guides"), skin, "header");
        Table pg = new Table();

        // POINTER COORDINATES CHECKBOX
        OwnLabel pointerCoordsLabel = new OwnLabel(I18n.msg("gui.ui.pointercoordinates"), skin);
        pointerCoords = new OwnCheckBox("", skin);
        pointerCoords.setChecked(settings.program.pointer.coordinates);

        // GUIDES CHECKBOX
        OwnLabel pointerGuidesLabel = new OwnLabel(I18n.msg("gui.ui.pointer.guides.display"), skin);
        pointerGuides = new OwnCheckBox("", skin);
        pointerGuides.setName("pointer guides cb");
        pointerGuides.setChecked(settings.program.pointer.guides.active);
        OwnImageButton guidesTooltip = new OwnImageButton(skin, "tooltip");
        guidesTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.ui.pointer.guides.info"), skin));
        HorizontalGroup pointerGuidesCbGroup = new HorizontalGroup();
        pointerGuidesCbGroup.space(pad18);
        pointerGuidesCbGroup.addActor(pointerGuides);
        pointerGuidesCbGroup.addActor(guidesTooltip);

        // GUIDES COLOR
        OwnLabel pointerGuidesColorLabel = new OwnLabel(I18n.msg("gui.ui.pointer.guides.color"), skin);
        float colorPickerSize = 32f;
        pointerGuidesColor = new ColorPicker(stage, skin);
        pointerGuidesColor.setPickedColor(settings.program.pointer.guides.color);

        // GUIDES WIDTH
        OwnLabel pointerGuidesWidthLabel = new OwnLabel(I18n.msg("gui.ui.pointer.guides.width"), skin, "default");
        pointerGuidesWidth = new OwnSliderPlus("", Constants.MIN_POINTER_GUIDES_WIDTH, Constants.MAX_POINTER_GUIDES_WIDTH, Constants.SLIDER_STEP_TINY, skin);
        pointerGuidesWidth.setName("pointerguideswidth");
        pointerGuidesWidth.setWidth(sliderWidth);
        pointerGuidesWidth.setValue(settings.program.pointer.guides.width);

        labels.add(pointerGuidesLabel, pointerGuidesColorLabel, pointerGuidesWidthLabel);

        // Add to table
        pg.add(pointerGuidesLabel).left().padBottom(pad10).padRight(pad34);
        pg.add(pointerGuidesCbGroup).left().colspan(2).padBottom(pad10).row();
        pg.add(pointerCoordsLabel).left().padRight(pad34).padBottom(pad18);
        pg.add(pointerCoords).left().padRight(pad10).padBottom(pad18).row();
        pg.add(pointerGuidesColorLabel).left().padBottom(pad10).padRight(pad34);
        pg.add(pointerGuidesColor).left().size(colorPickerSize).padBottom(pad10).row();
        pg.add(pointerGuidesWidthLabel).left().padBottom(pad10).padRight(pad34);
        pg.add(pointerGuidesWidth).left().padBottom(pad10).padRight(pad34).row();

        // UV GRIDS
        OwnLabel titleUVGrids = new OwnLabel(I18n.msg("gui.ui.uvgrid"), skin, "header");
        Table uvg = new Table();

        // FRAME COORDINATES
        OwnLabel frameCoordinatesLabel = new OwnLabel(I18n.msg("gui.ui.uvgrid.framecoords"), skin);
        frameCoordinates = new OwnCheckBox("", skin);
        frameCoordinates.setChecked(settings.program.uvGrid.frameCoordinates);
        frameCoordinates.addListener((event) -> {
            if (event instanceof ChangeEvent ce) {
                EventManager.publish(Event.UV_GRID_FRAME_COORDINATES_CMD, this, frameCoordinates.isChecked());
            }
            return false;
        });

        labels.add(frameCoordinatesLabel);

        // Add to table
        uvg.add(frameCoordinatesLabel).left().padBottom(pad10).padRight(pad34);
        uvg.add(frameCoordinates).left().padBottom(pad10);

        // Add to content
        addContentGroup(contentUITable, titleUI, ui, 0f);
        addContentGroup(contentUITable, titleCrosshair, ch);
        addContentGroup(contentUITable, titleGuides, pg);
        addContentGroup(contentUITable, titleUVGrids, uvg);


        /*
         * ==== PERFORMANCE ====
         */
        final Table contentPerformance = new Table(skin);
        contentPerformance.setWidth(contentWidth);
        contentPerformance.align(Align.top | Align.left);

        // MULTITHREADING
        OwnLabel titleMultiThread = new OwnLabel(I18n.msg("gui.multithreading"), skin, "header");

        Table multiThread = new Table(skin);

        OwnLabel numThreadsLabel = new OwnLabel(I18n.msg("gui.thread.number"), skin);
        int maxThreads = Runtime.getRuntime().availableProcessors();
        ComboBoxBean[] cbs = new ComboBoxBean[maxThreads + 1];
        cbs[0] = new ComboBoxBean(I18n.msg("gui.letdecide"), 0);
        for (i = 1; i <= maxThreads; i++) {
            cbs[i] = new ComboBoxBean(I18n.msg("gui.thread", i), i);
        }
        numThreads = new OwnSelectBox<>(skin);
        numThreads.setWidth(selectWidth);
        numThreads.setItems(cbs);
        numThreads.setSelectedIndex(settings.performance.numberThreads);

        OwnLabel multithreadLabel = new OwnLabel(I18n.msg("gui.thread.enable"), skin);
        multithreadCb = new OwnCheckBox("", skin);
        multithreadCb.addListener(event -> {
            if (event instanceof ChangeEvent) {
                numThreads.setDisabled(!multithreadCb.isChecked());
                // Add notice
                return true;
            }
            return false;
        });
        multithreadCb.setChecked(settings.performance.multithreading);
        numThreads.setDisabled(!multithreadCb.isChecked());

        // Add to table
        multiThread.add(multithreadLabel).left().padRight(pad34).padBottom(pad10);
        multiThread.add(multithreadCb).left().padBottom(pad10).row();
        multiThread.add(numThreadsLabel).left().padRight(pad34).padBottom(pad10);
        multiThread.add(numThreads).left().padBottom(pad10).row();
        final Cell<Actor> noticeMultiThreadCell = multiThread.add((Actor) null);
        noticeMultiThreadCell.colspan(2).left();

        multithreadCb.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (noticeMultiThreadCell.getActor() == null) {
                    String nextInfoStr = I18n.msg("gui.ui.info") + '\n';
                    int lines = GlobalResources.countOccurrences(nextInfoStr, '\n');
                    TextArea nextTimeInfo = new OwnTextArea(nextInfoStr, skin, "info");
                    nextTimeInfo.setDisabled(true);
                    nextTimeInfo.setWidth(400f);
                    nextTimeInfo.setPrefRows(lines + 1);
                    nextTimeInfo.clearListeners();
                    noticeMultiThreadCell.setActor(nextTimeInfo);
                }
                return true;
            }
            return false;
        });

        // Add to content
        addContentGroup(contentPerformance, titleMultiThread, multiThread, 0f);

        // DRAW DISTANCE
        OwnLabel titleLod = new OwnLabel(I18n.msg("gui.lod"), skin, "header");

        Table lod = new Table(skin);

        // Smooth transitions
        OwnLabel lodFadeLabel = new OwnLabel(I18n.msg("gui.lod.fade"), skin);
        lodFadeCb = new OwnCheckBox("", skin);
        lodFadeCb.setChecked(settings.scene.octree.fade);

        // Draw distance
        OwnLabel ddLabel = new OwnLabel(I18n.msg("gui.lod.thresholds"), skin);
        lodTransitions = new OwnSliderPlus("", Constants.MIN_SLIDER, Constants.MAX_SLIDER, 0.1f, Constants.MIN_LOD_TRANS_ANGLE_DEG, Constants.MAX_LOD_TRANS_ANGLE_DEG,
                skin);
        lodTransitions.setDisplayValueMapped(true);
        lodTransitions.setWidth(sliderWidth);
        lodTransitions.setMappedValue(settings.scene.octree.threshold[0] * MathUtilsDouble.radDeg);

        OwnImageButton lodTooltip = new OwnImageButton(skin, "tooltip");
        lodTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.lod.thresholds.info"), skin));

        // Add to table
        lod.add(lodFadeLabel).left().padRight(pad34).padBottom(pad10);
        lod.add(lodFadeCb).colspan(2).left().padBottom(pad10).row();
        lod.add(ddLabel).left().padRight(pad34).padBottom(pad10);
        lod.add(lodTransitions).left().padRight(pad18).padBottom(pad10);
        lod.add(lodTooltip).left().padBottom(pad10);

        // Add to content
        addContentGroup(contentPerformance, titleLod, lod);

        // VELOCITY VECTORS
        OwnLabel titleVelVectors = new OwnLabel(I18n.msg("gui.velvec"), skin, "header");

        Table velVectors = new Table(skin);

        // Max num of velocity vectors per star set.
        OwnLabel velVectorsLabel = new OwnLabel(I18n.msg("gui.velvec.num"), skin);
        velocityVectors = new OwnSliderPlus("", Constants.MIN_VELOCITY_VECTORS_STAR_GROUP, Constants.MAX_VELOCITY_VECTORS_STAR_GROUP, Constants.SLIDER_STEP, skin);
        velocityVectors.setWidth(sliderWidth);
        velocityVectors.setValue(settings.scene.star.group.numVelocityVector);

        OwnImageButton velVectorsTooltip = new OwnImageButton(skin, "tooltip");
        velVectorsTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.velvec.num.info"), skin));

        // LABELS
        labels.addAll(numThreadsLabel, ddLabel, lodFadeLabel, velVectorsLabel);

        // Add to table
        velVectors.add(velVectorsLabel).left().padRight(pad34).padBottom(pad10);
        velVectors.add(velocityVectors).left().padRight(pad18).padBottom(pad10);
        velVectors.add(velVectorsTooltip).left().padBottom(pad10);

        // Add to content
        addContentGroup(contentPerformance, titleVelVectors, velVectors);

        /*
         * ==== CONTROLS ====
         */
        final Table contentControls = new Table(skin);
        contentControls.setWidth(contentWidth);
        contentControls.align(Align.top | Align.left);

        OwnLabel titleController = new OwnLabel(I18n.msg("gui.controller"), skin, "header");

        // DETECTED CONTROLLER NAMES
        controllersTable = new Table(skin);
        OwnLabel detectedLabel = new OwnLabel(I18n.msg("gui.controller.detected"), skin);
        generateGamepadsList(controllersTable);

        // CONTROLLER MAPPINGS
        OwnLabel mappingsLabel = new OwnLabel(I18n.msg("gui.controller.mappingsfile"), skin);
        gamepadMappings = new OwnSelectBox<>(skin);
        reloadGamepadMappings(null);

        // INVERT X
        OwnLabel invertXLabel = new OwnLabel(I18n.msg("gui.controller.axis.invert", "X"), skin);
        invertX = new OwnCheckBox("", skin);
        invertX.setChecked(settings.controls.gamepad.invertX);
        // INVERT Y
        OwnLabel invertYLabel = new OwnLabel(I18n.msg("gui.controller.axis.invert", "Y"), skin);
        invertY = new OwnCheckBox("", skin);
        invertY.setChecked(settings.controls.gamepad.invertY);

        labels.add(detectedLabel, mappingsLabel, invertXLabel, invertYLabel);

        // KEY BINDINGS
        OwnLabel titleKeybindings = new OwnLabel(I18n.msg("gui.keymappings"), skin, "header");

        Map<ProgramAction, Array<TreeSet<Integer>>> keyboardMappings = KeyBindings.instance.getSortedMappingsInv();
        String[][] data = new String[keyboardMappings.size()][];

        i = 0;
        for (ProgramAction action : keyboardMappings.keySet()) {
            Array<TreeSet<Integer>> keys = keyboardMappings.get(action);

            String[] act = new String[1 + keys.size];
            act[0] = action.actionName;
            for (int j = 0; j < keys.size; j++) {
                act[j + 1] = keysToString(keys.get(j));
            }

            data[i] = act;
            i++;
        }

        Table controls = new Table(skin);
        controls.align(Align.left | Align.top);
        // Header
        controls.add(new OwnLabel(I18n.msg("gui.keymappings.action"), skin, "header")).padBottom(pad18).left();
        controls.add(new OwnLabel(I18n.msg("gui.keymappings.keys"), skin, "header")).padBottom(pad18).left().row();

        controls.add(new OwnLabel(I18n.msg("action.forward"), skin, "huge")).left().padRight(pad18).padBottom(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.UP).toUpperCase(), skin, "mono-pink-big")).padBottom(pad10).left().row();
        controls.add(new OwnLabel(I18n.msg("action.backward"), skin, "huge")).left().padRight(pad18).padBottom(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.DOWN).toUpperCase(), skin, "mono-pink-big")).padBottom(pad10).left().row();
        controls.add(new OwnLabel(I18n.msg("action.left"), skin, "huge")).left().padRight(pad18).padBottom(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.LEFT).toUpperCase(), skin, "mono-pink-big")).padBottom(pad10).left().row();
        controls.add(new OwnLabel(I18n.msg("action.right"), skin, "huge")).left().padRight(pad18).padBottom(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.RIGHT).toUpperCase(), skin, "mono-pink-big")).padBottom(pad10).left().row();

        // Controls
        boolean plus = false;
        for (String[] action : data) {
            HorizontalGroup keysGroup = new HorizontalGroup();
            keysGroup.space(pad10);
            for (int j = 1; j < action.length; j++) {
                String[] keys = action[j].split("\\+");
                for (int k = 0; k < keys.length; k++) {
                    keysGroup.addActor(new OwnLabel(keys[k].trim().replace('_', '-').replace("PL", "+"), skin, "mono-pink-big"));
                    if (k < keys.length - 1)
                        keysGroup.addActor(new OwnLabel("+", skin, "huge"));
                }
                if (j < action.length - 1)
                    keysGroup.addActor(new OwnLabel("/", skin, "huge"));
            }
            controls.add(new OwnLabel(action[0], skin, "huge")).left().padRight(pad18).padBottom(pad10);
            controls.add(keysGroup).padBottom(pad10).left().row();
        }

        OwnScrollPane controlsScroll = new OwnScrollPane(controls, skin, "minimalist-nobg");
        controlsScroll.setWidth(controlsScrollWidth);
        controlsScroll.setHeight(controlsScrollHeight);
        controlsScroll.setScrollingDisabled(true, false);
        controlsScroll.setSmoothScrolling(true);
        controlsScroll.setFadeScrollBars(false);
        scrolls.add(controlsScroll);

        Table controller = new Table(skin);
        controller.add(detectedLabel).left().padBottom(pad18).padRight(pad18);
        controller.add(controllersTable).left().padBottom(pad18).row();
        controller.add(mappingsLabel).left().padBottom(pad18).padRight(pad18);
        controller.add(gamepadMappings).left().padBottom(pad18).row();
        controller.add(invertXLabel).left().padBottom(pad18).padRight(pad18);
        controller.add(invertX).left().padBottom(pad18).row();
        controller.add(invertYLabel).left().padBottom(pad34).padRight(pad18);
        controller.add(invertY).left().padBottom(pad34).row();

        // Add to content
        addContentGroup(contentControls, titleController, controller, 0f);
        addContentGroup(contentControls, titleKeybindings, controlsScroll);

        /*
         * ==== SCREENSHOTS ====
         */
        final Table contentScreenshots = new Table(skin);
        contentScreenshots.setWidth(contentWidth);
        contentScreenshots.align(Align.top | Align.left);

        // SCREEN CAPTURE
        OwnLabel titleScreenshots = new OwnLabel(I18n.msg("gui.screencapture"), skin, "header");

        Table screenshots = new Table(skin);

        // Info
        String ssInfoStr = I18n.msg("gui.screencapture.info") + '\n';
        int ssLines = GlobalResources.countOccurrences(ssInfoStr, '\n');
        TextArea screenshotsInfo = new OwnTextArea(ssInfoStr, skin, "info");
        screenshotsInfo.setDisabled(true);
        screenshotsInfo.setPrefRows(ssLines + 1);
        screenshotsInfo.setWidth(taWidth);
        screenshotsInfo.clearListeners();

        // Save location
        OwnLabel screenshotsLocationLabel = new OwnLabel(I18n.msg("gui.screenshots.save"), skin);
        screenshotsLocation = new OwnTextButton(TextUtils.capString(settings.screenshot.location, 45), skin);
        screenshotsLocation.addListener(new OwnTextTooltip(settings.screenshot.location, skin));
        screenshotsPath = Path.of(settings.screenshot.location);
        screenshotsLocation.pad(pad10);
        screenshotsLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.screenshots.directory.choose"), skin, stage, Paths.get(settings.screenshot.location),
                        FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setShowHidden(settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> settings.program.fileChooser.showHidden = showHidden);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        // do stuff with result
                        screenshotsLocation.setText(TextUtils.capString(result.toString(), 45));
                        screenshotsPath = result;
                        screenshotsLocation.addListener(new OwnTextTooltip(result.toString(), skin));
                    }
                    return true;
                });
                fc.show(stage);

                return true;
            }
            return false;
        });

        // Size
        final OwnLabel screenshotsSizeLabel = new OwnLabel(I18n.msg("gui.screenshots.size"), skin);
        screenshotsSizeLabel.setDisabled(settings.screenshot.isSimpleMode());
        final OwnLabel xScreenshotsLabel = new OwnLabel("x", skin);
        IValidator screenshotsSizeValidator = new IntValidator(ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE);
        ssWidthField = new OwnTextField(
                Integer.toString(MathUtils.clamp(settings.screenshot.resolution[0], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)),
                skin, screenshotsSizeValidator);
        ssWidthField.setWidth(inputSmallWidth);
        ssWidthField.setDisabled(settings.screenshot.isSimpleMode());
        ssHeightField = new OwnTextField(
                Integer.toString(MathUtils.clamp(settings.screenshot.resolution[1], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)),
                skin, screenshotsSizeValidator);
        ssHeightField.setWidth(inputSmallWidth);
        ssHeightField.setDisabled(settings.screenshot.isSimpleMode());
        HorizontalGroup ssSizeGroup = new HorizontalGroup();
        ssSizeGroup.space(pad10);
        ssSizeGroup.addActor(ssWidthField);
        ssSizeGroup.addActor(xScreenshotsLabel);
        ssSizeGroup.addActor(ssHeightField);

        // Mode
        OwnLabel ssModeLabel = new OwnLabel(I18n.msg("gui.screenshots.mode"), skin);
        ComboBoxBean[] screenshotModes = new ComboBoxBean[]{new ComboBoxBean(I18n.msg("gui.screenshots.mode.simple"), 0),
                new ComboBoxBean(I18n.msg("gui.screenshots.mode.redraw"), 1)};
        screenshotMode = new OwnSelectBox<>(skin);
        screenshotMode.setItems(screenshotModes);
        screenshotMode.setWidth(selectWidth);
        screenshotMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Simple
                // Redraw
                enableComponents(screenshotMode.getSelected().value != 0, ssWidthField, ssHeightField, screenshotsSizeLabel, xScreenshotsLabel);
                return true;
            }
            return false;
        });
        screenshotMode.setSelected(screenshotModes[settings.screenshot.mode.ordinal()]);
        screenshotMode.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        // Quality
        OwnLabel ssQualityLabel = new OwnLabel(I18n.msg("gui.screenshots.quality"), skin);
        screenshotQuality = new OwnSliderPlus("", Constants.MIN_SCREENSHOT_QUALITY, Constants.MAX_SCREENSHOT_QUALITY, Constants.SLIDER_STEP, skin);
        screenshotQuality.setName("screenshot quality");
        screenshotQuality.setWidth(sliderWidth);
        screenshotQuality.setValue(settings.screenshot.quality * 100f);

        // Format
        OwnLabel ssFormatLabel = new OwnLabel(I18n.msg("gui.screenshots.format"), skin);
        ComboBoxBean[] screenshotFormats = new ComboBoxBean[]{new ComboBoxBean(I18n.msg("gui.screenshots.format.png"), 0),
                new ComboBoxBean(I18n.msg("gui.screenshots.format.jpg"), 1)};
        screenshotFormat = new OwnSelectBox<>(skin);
        screenshotFormat.setItems(screenshotFormats);
        screenshotFormat.setWidth(selectWidth);
        screenshotFormat.addListener(event -> {
            if (event instanceof ChangeEvent) {
                enableComponents(screenshotFormat.getSelected().value == 1, screenshotQuality, ssQualityLabel);
                return true;
            }
            return false;
        });
        screenshotFormat.setSelected(screenshotFormats[settings.screenshot.format.ordinal()]);
        enableComponents(screenshotFormat.getSelected().value == 1, screenshotQuality, ssQualityLabel);

        OwnImageButton screenshotsModeTooltip = new OwnImageButton(skin, "tooltip");
        screenshotsModeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        HorizontalGroup ssModeGroup = new HorizontalGroup();
        ssModeGroup.space(pad10);
        ssModeGroup.addActor(screenshotMode);
        ssModeGroup.addActor(screenshotsModeTooltip);

        // LABELS
        labels.addAll(screenshotsLocationLabel, ssModeLabel, screenshotsSizeLabel, ssFormatLabel, ssQualityLabel);

        // Add to table
        screenshots.add(screenshotsInfo).colspan(2).left().padBottom(pad10).row();
        screenshots.add(screenshotsLocationLabel).left().padRight(pad34).padBottom(pad10);
        screenshots.add(screenshotsLocation).left().expandX().padBottom(pad10).row();
        screenshots.add(ssModeLabel).left().padRight(pad34).padBottom(pad10);
        screenshots.add(ssModeGroup).left().expandX().padBottom(pad10).row();
        screenshots.add(screenshotsSizeLabel).left().padRight(pad34).padBottom(pad10);
        screenshots.add(ssSizeGroup).left().expandX().padBottom(pad10).row();
        screenshots.add(ssFormatLabel).left().padRight(pad34).padBottom(pad10);
        screenshots.add(screenshotFormat).left().expandX().padBottom(pad10).row();
        screenshots.add(ssQualityLabel).left().padRight(pad34).padBottom(pad10);
        screenshots.add(screenshotQuality).left().expandX().padBottom(pad10).row();

        // Add to content
        addContentGroup(contentScreenshots, titleScreenshots, screenshots, 0f);

        /*
         * ==== FRAME OUTPUT ====
         */
        final Table contentFrames = new Table(skin);
        contentFrames.setWidth(contentWidth);
        contentFrames.align(Align.top | Align.left);

        // FRAME OUTPUT CONFIG
        OwnLabel titleFrameOutput = new OwnLabel(I18n.msg("gui.frameoutput"), skin, "header");

        Table frameOutput = new Table(skin);

        // Info
        String frameOutputInfoString = I18n.msg("gui.frameoutput.info") + '\n';
        ssLines = GlobalResources.countOccurrences(frameOutputInfoString, '\n');
        TextArea frameOutputInfo = new OwnTextArea(frameOutputInfoString, skin, "info");
        frameOutputInfo.setDisabled(true);
        frameOutputInfo.setPrefRows(ssLines + 1);
        frameOutputInfo.setWidth(taWidth);
        frameOutputInfo.clearListeners();

        // Save location
        OwnLabel frameOutputLocationLabel = new OwnLabel(I18n.msg("gui.frameoutput.location"), skin);
        frameOutputLocation = new OwnTextButton(TextUtils.capString(settings.frame.location, 45), skin);
        frameOutputLocation.addListener(new OwnTextTooltip(settings.frame.location, skin));
        frameOutputPath = Path.of(settings.frame.location);
        frameOutputLocation.pad(pad10);
        frameOutputLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.frameoutput.directory.choose"), skin, stage, Paths.get(settings.frame.location),
                        FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setShowHidden(settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> settings.program.fileChooser.showHidden = showHidden);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        // do stuff with result
                        frameOutputLocation.setText(TextUtils.capString(result.toString(), 45));
                        frameOutputPath = result;
                        frameOutputLocation.addListener(new OwnTextTooltip(result.toString(), skin));
                    }
                    return true;
                });
                fc.show(stage);

                return true;
            }
            return false;
        });

        // Prefix
        OwnLabel prefixLabel = new OwnLabel(I18n.msg("gui.frameoutput.prefix"), skin);
        frameOutputPrefix = new OwnTextField(settings.frame.prefix, skin, new RegexpValidator("^\\w+$"));
        frameOutputPrefix.setWidth(inputWidth);

        // FPS
        OwnLabel fpsLabel = new OwnLabel(I18n.msg("gui.target.fps"), skin);
        frameOutputFps = new OwnTextField(nf3.format(settings.frame.targetFps), skin, new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS));
        frameOutputFps.setWidth(inputWidth);

        // Size
        final OwnLabel frameoutputSizeLabel = new OwnLabel(I18n.msg("gui.frameoutput.size"), skin);
        frameoutputSizeLabel.setDisabled(settings.frame.isSimpleMode());
        final OwnLabel xFrameLabel = new OwnLabel("x", skin);
        IValidator frameoutputSizeValidator = new IntValidator(ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE);
        foWidthField = new OwnTextField(
                Integer.toString(MathUtils.clamp(settings.frame.resolution[0], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)), skin,
                frameoutputSizeValidator);
        foWidthField.setWidth(inputSmallWidth);
        foWidthField.setDisabled(settings.frame.isSimpleMode());
        foHeightField = new OwnTextField(
                Integer.toString(MathUtils.clamp(settings.frame.resolution[1], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)), skin,
                frameoutputSizeValidator);
        foHeightField.setWidth(inputSmallWidth);
        foHeightField.setDisabled(settings.frame.isSimpleMode());
        HorizontalGroup foSizeGroup = new HorizontalGroup();
        foSizeGroup.space(pad10);
        foSizeGroup.addActor(foWidthField);
        foSizeGroup.addActor(xFrameLabel);
        foSizeGroup.addActor(foHeightField);

        // Quality
        OwnLabel foQualityLabel = new OwnLabel(I18n.msg("gui.screenshots.quality"), skin);
        frameQuality = new OwnSliderPlus("", Constants.MIN_SCREENSHOT_QUALITY, Constants.MAX_SCREENSHOT_QUALITY, Constants.SLIDER_STEP, skin);
        frameQuality.setName("frame quality");
        frameQuality.setWidth(sliderWidth);
        frameQuality.setValue(settings.frame.quality * 100f);

        // Format
        OwnLabel foFormatLabel = new OwnLabel(I18n.msg("gui.screenshots.format"), skin);
        ComboBoxBean[] frameFormats = new ComboBoxBean[]{new ComboBoxBean(I18n.msg("gui.screenshots.format.png"), 0),
                new ComboBoxBean(I18n.msg("gui.screenshots.format.jpg"), 1)};
        frameOutputFormat = new OwnSelectBox<>(skin);
        frameOutputFormat.setItems(screenshotFormats);
        frameOutputFormat.setWidth(selectWidth);
        frameOutputFormat.addListener(event -> {
            if (event instanceof ChangeEvent) {
                enableComponents(frameOutputFormat.getSelected().value == 1, frameQuality, foQualityLabel);
                return true;
            }
            return false;
        });
        frameOutputFormat.setSelected(screenshotFormats[settings.frame.format.ordinal()]);
        enableComponents(frameOutputFormat.getSelected().value == 1, frameQuality, foQualityLabel);

        // Mode
        OwnLabel fomodeLabel = new OwnLabel(I18n.msg("gui.screenshots.mode"), skin);
        ComboBoxBean[] frameoutputModes = new ComboBoxBean[]{new ComboBoxBean(I18n.msg("gui.screenshots.mode.simple"), 0),
                new ComboBoxBean(I18n.msg("gui.screenshots.mode.redraw"), 1)};
        frameOutputMode = new OwnSelectBox<>(skin);
        frameOutputMode.setItems(frameoutputModes);
        frameOutputMode.setWidth(selectWidth);
        frameOutputMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Simple
                // Redraw
                enableComponents(frameOutputMode.getSelected().value != 0, foWidthField, foHeightField, frameoutputSizeLabel, xFrameLabel);
                return true;
            }
            return false;
        });
        frameOutputMode.setSelected(frameoutputModes[settings.frame.mode.ordinal()]);
        frameOutputMode.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        OwnImageButton frameoutputModeTooltip = new OwnImageButton(skin, "tooltip");
        frameoutputModeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        HorizontalGroup foModeGroup = new HorizontalGroup();
        foModeGroup.space(pad10);
        foModeGroup.addActor(frameOutputMode);
        foModeGroup.addActor(frameoutputModeTooltip);

        // Counter
        OwnLabel counterLabel = new OwnLabel(I18n.msg("gui.frameoutput.sequence"), skin);
        HorizontalGroup counterGroup = new HorizontalGroup();
        counterGroup.space(pad10);
        frameSequenceNumber = new OwnLabel(Integer.toString(ImageRenderer.getSequenceNumber()), skin);
        frameSequenceNumber.setWidth(inputSmallWidth * 3f);
        OwnTextButton resetCounter = new OwnTextButton(I18n.msg("gui.frameoutput.sequence.reset"), skin);
        resetCounter.pad(pad18);
        resetCounter.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                ImageRenderer.resetSequenceNumber();
                frameSequenceNumber.setText("0");
            }
            return false;
        });

        counterGroup.addActor(frameSequenceNumber);

        // LABELS
        labels.addAll(frameOutputLocationLabel, prefixLabel, fpsLabel, fomodeLabel, frameoutputSizeLabel);

        // Add to table
        frameOutput.add(frameOutputInfo).colspan(2).left().padBottom(pad10).row();
        frameOutput.add(frameOutputLocationLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(frameOutputLocation).left().expandX().padBottom(pad10).row();
        frameOutput.add(prefixLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(frameOutputPrefix).left().padBottom(pad10).row();
        frameOutput.add(fpsLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(frameOutputFps).left().padBottom(pad10).row();
        frameOutput.add(fomodeLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(foModeGroup).left().expandX().padBottom(pad10).row();
        frameOutput.add(frameoutputSizeLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(foSizeGroup).left().expandX().padBottom(pad10).row();
        frameOutput.add(foFormatLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(frameOutputFormat).left().expandX().padBottom(pad10).row();
        frameOutput.add(foQualityLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(frameQuality).left().expandX().padBottom(pad10).row();
        frameOutput.add(counterLabel).left().padRight(pad34).padBottom(pad10);
        frameOutput.add(counterGroup).left().expandX().padBottom(pad10).row();
        frameOutput.add().padRight(pad34);
        frameOutput.add(resetCounter).left();

        // Add to content
        addContentGroup(contentFrames, titleFrameOutput, frameOutput, 0f);

        /*
         * ==== CAMERA ====
         */
        final Table contentCamera = new Table(skin);
        contentCamera.setWidth(contentWidth);
        contentCamera.align(Align.top | Align.left);

        // CAMERA RECORDING
        Table camrec = new Table(skin);

        OwnLabel titleCamrec = new OwnLabel(I18n.msg("gui.camerarec.title"), skin, "header");

        // fps
        OwnLabel camfpsLabel = new OwnLabel(I18n.msg("gui.target.fps"), skin);
        camRecFps = new OwnTextField(nf3.format(settings.camrecorder.targetFps), skin, new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS));
        camRecFps.setWidth(inputWidth);
        OwnImageButton camrecFpsTooltip = new OwnImageButton(skin, "tooltip");
        camrecFpsTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera.targetfps"), skin));

        // Keyframe preferences
        Button keyframePrefs = new OwnTextIconButton(I18n.msg("gui.keyframes.preferences"), skin, "preferences");
        keyframePrefs.setName("keyframe preferences");
        keyframePrefs.pad(pad18);
        keyframePrefs.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.kf.editprefs"), skin));
        keyframePrefs.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                KeyframePreferencesWindow kpw = new KeyframePreferencesWindow(stage, skin);
                kpw.setAcceptListener(() -> {
                    if (kpw.camcorderFps != null && kpw.camcorderFps.isValid()) {
                        camRecFps.setText(kpw.camcorderFps.getText());
                    }
                });
                kpw.show(stage);
                return true;
            }
            return false;
        });

        // Activate automatically
        OwnLabel autoCamrecLabel = new OwnLabel(I18n.msg("gui.camerarec.frameoutput"), skin);
        cbAutoCamRec = new OwnCheckBox("", skin);
        cbAutoCamRec.setChecked(settings.camrecorder.auto);
        cbAutoCamRec.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera.frameoutput"), skin));
        OwnImageButton camrecAutoTooltip = new OwnImageButton(skin, "tooltip");
        camrecAutoTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera.frameoutput"), skin));

        // LABELS
        labels.add(autoCamrecLabel);

        // Add to table
        camrec.add(camfpsLabel).left().padRight(pad34).padBottom(pad10);
        camrec.add(camRecFps).left().expandX().padBottom(pad10);
        camrec.add(camrecFpsTooltip).left().padLeft(pad10).padBottom(pad10).row();
        camrec.add(autoCamrecLabel).left().padRight(pad34).padBottom(pad10);
        camrec.add(cbAutoCamRec).left().padBottom(pad10);
        camrec.add(camrecAutoTooltip).left().padLeft(pad10).padBottom(pad10).row();
        camrec.add(keyframePrefs).colspan(3).left().padTop(pad34 * 2f).row();

        // Add to content
        addContentGroup(contentCamera, titleCamrec, camrec, 0f);

        /*
         * ==== PANORAMA ====
         */
        final Table content360 = new Table(skin);
        content360.setWidth(contentWidth);
        content360.align(Align.top | Align.left);

        // CUBEMAP
        OwnLabel titleCubemap = new OwnLabel(I18n.msg("gui.360"), skin, "header");
        Table cubemap = new Table(skin);

        // Info
        String cminfostr = I18n.msg("gui.360.info") + '\n';
        ssLines = GlobalResources.countOccurrences(cminfostr, '\n');
        TextArea cmInfo = new OwnTextArea(cminfostr, skin, "info");
        cmInfo.setDisabled(true);
        cmInfo.setPrefRows(ssLines + 1);
        cmInfo.setWidth(taWidth);
        cmInfo.clearListeners();

        // Resolution
        OwnLabel cmResolutionLabel = new OwnLabel(I18n.msg("gui.360.resolution"), skin);
        cmResolution = new OwnTextField(Integer.toString(settings.program.modeCubemap.faceResolution), skin, new IntValidator(20, 15000));
        cmResolution.setWidth(inputWidth);
        cmResolution.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (cmResolution.isValid()) {
                    plResolution.setText(cmResolution.getText());
                }
                return true;
            }
            return false;
        });

        // LABELS
        labels.add(cmResolutionLabel);

        // Add to table
        cubemap.add(cmInfo).colspan(2).left().padBottom(pad10).row();
        cubemap.add(cmResolutionLabel).left().padRight(pad34).padBottom(pad10);
        cubemap.add(cmResolution).left().expandX().padBottom(pad10).row();

        // Add to content
        addContentGroup(content360, titleCubemap, cubemap, 0f);

        /*
         * ==== PLANETARIUM ====
         */
        final Table contentPlanetarium = new Table(skin);
        contentPlanetarium.setWidth(contentWidth);
        contentPlanetarium.align(Align.top | Align.left);

        // Planetarium title
        OwnLabel titlePlanetarium = new OwnLabel(I18n.msg("gui.planetarium"), skin, "header");
        Table planetarium = new Table(skin);

        // Aperture
        OwnLabel apertureLabel = new OwnLabel(I18n.msg("gui.planetarium.aperture"), skin);
        plAperture = new OwnTextField(Float.toString(settings.program.modeCubemap.planetarium.aperture), skin, new FloatValidator(30, 360));
        plAperture.setWidth(inputWidth);

        // Skew angle
        OwnLabel plAngleLabel = new OwnLabel(I18n.msg("gui.planetarium.angle"), skin);
        plAngle = new OwnTextField(Float.toString(settings.program.modeCubemap.planetarium.angle), skin, new FloatValidator(-180, 180));
        plAngle.setWidth(inputWidth);

        // Info
        String plInfoStr = I18n.msg("gui.planetarium.info") + '\n';
        ssLines = GlobalResources.countOccurrences(plInfoStr, '\n');
        TextArea plInfo = new OwnTextArea(plInfoStr, skin, "info");
        plInfo.setDisabled(true);
        plInfo.setPrefRows(ssLines + 1);
        plInfo.setWidth(taWidth);
        plInfo.clearListeners();

        // Resolution
        OwnLabel plResolutionLabel = new OwnLabel(I18n.msg("gui.360.resolution"), skin);
        plResolution = new OwnTextField(Integer.toString(settings.program.modeCubemap.faceResolution), skin, new IntValidator(20, 15000));
        plResolution.setWidth(inputWidth);
        plResolution.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (plResolution.isValid()) {
                    cmResolution.setText(plResolution.getText());
                }
                return true;
            }
            return false;
        });

        // LABELS
        labels.add(plResolutionLabel, plAngleLabel, plResolutionLabel);

        // Add to table
        planetarium.add(apertureLabel).left().padRight(pad34).padBottom(pad18 * 3f);
        planetarium.add(plAperture).left().expandX().padBottom(pad18 * 3f).row();
        planetarium.add(plAngleLabel).left().padRight(pad34).padBottom(pad18 * 3f);
        planetarium.add(plAngle).left().expandX().padBottom(pad18 * 3f).row();
        planetarium.add(plInfo).colspan(2).left().padBottom(pad10).row();
        planetarium.add(plResolutionLabel).left().padRight(pad34).padBottom(pad10);
        planetarium.add(plResolution).left().expandX().padBottom(pad10).row();

        // Spherical mirror
        OwnLabel titleSphericalMirror = new OwnLabel(I18n.msg("gui.planetarium.sphericalmirror"), skin, "header");
        Table sphericalMirror = new Table(skin);

        // Warp file
        OwnLabel warpFileLabel = new OwnLabel(I18n.msg("gui.planetarium.sphericalmirror.warpfile"), skin);
        var currentMeshWarp = settings.program.modeCubemap.planetarium.sphericalMirrorWarp;
        var meshWarpText = currentMeshWarp != null ? currentMeshWarp.getFileName().toString() : I18n.msg("gui.planetarium.sphericalmirror.warpfile.select");
        var meshWarpPath = currentMeshWarp != null ? currentMeshWarp.getParent() : SysUtils.getUserHome();
        meshWarpFileLocation = new OwnTextButton(TextUtils.capString(meshWarpText, 45), skin);
        meshWarpFileLocation.pad(pad10, pad34, pad10, pad34);
        meshWarpFileLocation.addListener(
                new OwnTextTooltip(currentMeshWarp != null ? currentMeshWarp.toString() : I18n.msg("gui.planetarium.sphericalmirror.warpfile.select"), skin));
        meshWarpFilePath = settings.program.modeCubemap.planetarium.sphericalMirrorWarp;
        meshWarpFileLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.planetarium.sphericalmirror.warpfile"), skin, stage, meshWarpPath, FileChooser.FileChooserTarget.FILES);
                fc.setShowHidden(settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> settings.program.fileChooser.showHidden = showHidden);
                fc.setFileFilter(pathname -> Files.exists(pathname) && Files.isRegularFile(pathname));
                fc.setAcceptedFiles("*.*");
                fc.setResultListener((success, result) -> {
                    if (success) {
                        if (WarpMeshReader.isValidWarpMeshAscii(result)) {
                            // do stuff with result
                            meshWarpFileLocation.setText(TextUtils.capString(result.getFileName().toString(), 45));
                            meshWarpFilePath = result;
                            meshWarpFileLocation.addListener(new OwnTextTooltip(result.toString(), skin));
                        } else {
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.planetarium.sphericalmirror.warpfile.invalid", result.toString()));
                        }
                    }
                    return true;
                });
                fc.show(stage);

                return true;
            }
            return false;
        });
        OwnImageButton meshWarpTooltip = new OwnImageButton(skin, "tooltip");
        meshWarpTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.planetarium.sphericalmirror.warpfile.tooltip"), skin));

        // LABELS
        labels.add(warpFileLabel);

        // Add to table
        sphericalMirror.add(warpFileLabel).left().padRight(pad34).padBottom(pad18 * 3f);
        sphericalMirror.add(meshWarpFileLocation).left().expandX().padBottom(pad18 * 3f).padRight(pad18);
        sphericalMirror.add(meshWarpTooltip).left().padBottom(pad18 * 3f);

        // Add to content
        addContentGroup(contentPlanetarium, titlePlanetarium, planetarium, 0f);
        addContentGroup(contentPlanetarium, titleSphericalMirror, sphericalMirror);


        /*
         * ==== DATA ====
         */
        final Table contentDataTable = new Table(skin);
        contentDataTable.setWidth(contentWidth);
        contentDataTable.align(Align.top | Align.left);
        final OwnScrollPane contentData = new OwnScrollPane(contentDataTable, skin, "minimalist-nobg");
        contentData.setWidth(contentWidth);
        contentData.setHeight(scrollHeight);
        contentData.setScrollingDisabled(true, false);
        contentData.setFadeScrollBars(false);

        // GENERAL OPTIONS
        OwnLabel titleGeneralData = new OwnLabel(I18n.msg("gui.data.options"), skin, "header");
        OwnLabel highAccuracyPositionsLabel = new OwnLabel(I18n.msg("gui.data.highaccuracy"), skin);
        highAccuracyPositions = new OwnCheckBox("", skin);
        highAccuracyPositions.setChecked(settings.data.highAccuracy);
        highAccuracyPositions.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.data.highaccuracy"), skin));
        OwnImageButton highAccTooltip = new OwnImageButton(skin, "tooltip");
        highAccTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.data.highaccuracy"), skin));

        labels.add(highAccuracyPositionsLabel);

        // DATA SOURCE
        final OwnLabel titleData = new OwnLabel(I18n.msg("gui.data.source"), skin, "header");

        // Info
        String dsInfoStr = I18n.msg("gui.data.source.info") + '\n';
        int dsLines = GlobalResources.countOccurrences(dsInfoStr, '\n');
        final TextArea dataSourceInfo = new OwnTextArea(dsInfoStr, skin, "info");
        dataSourceInfo.setDisabled(true);
        dataSourceInfo.setPrefRows(dsLines + 1);
        dataSourceInfo.setWidth(taWidth);
        dataSourceInfo.clearListeners();

        final OwnTextButton dataDownload = new OwnTextButton(I18n.msg("gui.download.title"), skin);
        dataDownload.pad(pad34, pad34 * 2f, pad34, pad34 * 2f);
        dataDownload.setHeight(buttonHeight);
        dataDownload.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (DataDescriptor.serverDataDescriptor != null || DataDescriptor.localDataDescriptor != null) {
                    DataDescriptor dd = DataDescriptor.serverDataDescriptor != null ? DataDescriptor.serverDataDescriptor : DataDescriptor.localDataDescriptor;
                    DatasetManagerWindow ddw = new DatasetManagerWindow(stage, skin, dd, false, I18n.msg("gui.save"));
                    ddw.setModal(true);
                    ddw.show(stage);
                } else {
                    // Try again
                    FileHandle dataDescriptor = Gdx.files.absolute(SysUtils.getTempDir(settings.data.location) + "/gaiasky-data.json");
                    DownloadHelper.downloadFile(settings.program.url.dataDescriptor, dataDescriptor, Settings.settings.program.offlineMode, null, null,
                            (digest) -> {
                                DataDescriptor dd = DataDescriptorUtils.instance().buildServerDatasets(dataDescriptor);
                                DatasetManagerWindow ddw = new DatasetManagerWindow(stage, skin, dd, false, null);
                                ddw.setModal(true);
                                ddw.show(stage);
                            },
                            () -> {
                                // Fail?
                                logger.error("No internet connection or server is down!");
                                GuiUtils.addNoConnectionWindow(skin, stage);
                            }, null);
                }
                return true;
            }
            return false;
        });

        // ATTITUDE
        OwnLabel titleAttitude = new OwnLabel(I18n.msg("gui.gaia.attitude"), skin, "header");
        Table attitude = new Table(skin);

        real = new OwnCheckBox(I18n.msg("gui.gaia.real"), skin, "radio", pad10);
        real.setChecked(settings.data.realGaiaAttitude);
        OwnCheckBox nsl = new OwnCheckBox(I18n.msg("gui.gaia.nsl"), skin, "radio", pad10);
        nsl.setChecked(!settings.data.realGaiaAttitude);

        new ButtonGroup<>(real, nsl);

        // Add to table
        attitude.add(nsl).left().padBottom(pad10).padRight(pad20);
        attitude.add(getRequiresRestartLabel()).left().padBottom(pad10).row();
        attitude.add(real).left().padBottom(pad10).padRight(pad20);
        attitude.add(getRequiresRestartLabel()).left().padBottom(pad10).row();
        final Cell<Actor> noticeAttCell = attitude.add((Actor) null);
        noticeAttCell.colspan(2).left();

        EventListener attNoticeListener = event -> {
            if (event instanceof ChangeEvent) {
                if (noticeAttCell.getActor() == null) {
                    String nextInfoStr = I18n.msg("gui.ui.info") + '\n';
                    int lines1 = GlobalResources.countOccurrences(nextInfoStr, '\n');
                    TextArea nextTimeInfo = new OwnTextArea(nextInfoStr, skin, "info");
                    nextTimeInfo.setDisabled(true);
                    nextTimeInfo.setPrefRows(lines1 + 1);
                    nextTimeInfo.setWidth(taWidth);
                    nextTimeInfo.clearListeners();
                    noticeAttCell.setActor(nextTimeInfo);
                }
                return true;
            }
            return false;
        };
        real.addListener(attNoticeListener);
        nsl.addListener(attNoticeListener);

        Table generalData = new Table(skin);
        generalData.add(highAccuracyPositionsLabel).left().padBottom(pad34);
        generalData.add(highAccuracyPositions).left().padRight(pad34).padBottom(pad34);
        generalData.add(highAccTooltip).left().padBottom(pad34).row();

        // Add to content
        addContentGroup(contentDataTable, titleGeneralData, generalData, 0f);

        if (!welcomeScreen) {
            Table dataSource = new Table(skin);
            dataSource.add(dataSourceInfo).left().padBottom(pad10).row();
            dataSource.add(dataDownload).left().padBottom(pad34).row();

            addContentGroup(contentDataTable, titleData, dataSource);
        }
        addContentGroup(contentDataTable, titleAttitude, attitude);


        /*
         * ==== SYSTEM ====
         */
        final Table contentSystem = new Table(skin);
        contentSystem.setWidth(contentWidth);
        contentSystem.align(Align.top | Align.left);

        // SYSTEM PREFERENCES
        OwnLabel titleSystemPrefs = new OwnLabel(I18n.msg("gui.system.reporting"), skin, "header");
        Table stats = new Table(skin);

        // DEBUG INFO
        OwnLabel debugInfoLabel = new OwnLabel(I18n.msg("gui.system.debuginfo"), skin);
        debugInfo = new OwnCheckBox("", skin);
        debugInfo.setChecked(settings.program.debugInfo);
        debugInfo.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_DEBUG_CMD, debugInfo, !settings.program.debugInfo);
                return true;
            }
            return false;
        });

        // SHADER CACHE
        OwnLabel cacheLabel = new OwnLabel(I18n.msg("gui.system.shader.cache"), skin);
        shaderCache = new OwnCheckBox("", skin);
        shaderCache.setChecked(settings.program.shaderCache);
        OwnImageButton shaderCacheTooltip = new OwnImageButton(skin, "tooltip");
        shaderCacheTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.system.shader.cache.info"), skin));

        // CLEAR SHADER CACHE BUTTON
        OwnTextButton clearCache = new OwnTextButton(I18n.msg("gui.system.shader.cache.clear"), skin);
        clearCache.addListener(event -> {
            if (event instanceof ChangeEvent ce) {
                var path = SysUtils.getShaderCacheDir();
                try {
                    FileUtils.cleanDirectory(path.toFile());
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, clearCache, I18n.msg("gui.system.shader.cache.clean", path.toString()), 4f);
                } catch (IOException e) {
                    logger.error(e);
                }
                return true;
            } else {
                return false;
            }
        });
        clearCache.pad(0, pad34, 0, pad34);
        clearCache.setHeight(buttonHeight);

        // EXIT CONFIRMATION
        OwnLabel exitConfirmationLabel = new OwnLabel(I18n.msg("gui.quit.confirmation"), skin);
        exitConfirmation = new OwnCheckBox("", skin);
        exitConfirmation.setChecked(settings.program.exitConfirmation);

        labels.addAll(debugInfoLabel, cacheLabel, exitConfirmationLabel);

        // RELOAD DEFAULTS
        OwnTextButton reloadDefaults = new OwnTextButton(I18n.msg("gui.system.reloaddefaults"), skin);
        reloadDefaults.addListener(event -> {
            if (event instanceof ChangeEvent) {
                reloadDefaultPreferences();
                me.hide();
                // Prevent saving current state
                GaiaSky.instance.saveState = false;
                Gdx.app.exit();
                return true;
            }

            return false;
        });
        reloadDefaults.pad(0, pad34, 0, pad34);
        reloadDefaults.setHeight(buttonHeight);

        OwnLabel warningLabel = new OwnLabel(I18n.msg("gui.system.reloaddefaults.warn"), skin, "default-red");

        // Add to table
        stats.add(debugInfoLabel).left().padBottom(pad10);
        stats.add(debugInfo).left().padBottom(pad10).row();
        stats.add(exitConfirmationLabel).left().padBottom(pad10);
        stats.add(exitConfirmation).left().padBottom(pad10).row();
        stats.add(cacheLabel).left().padBottom(pad10);
        stats.add(shaderCache).left().padBottom(pad10).padRight(pad18);
        stats.add(shaderCacheTooltip).left().padBottom(pad10).row();
        stats.add(clearCache).left().colspan(2).padBottom(pad34 * 3f).row();
        stats.add(warningLabel).left().colspan(2).padBottom(pad34).row();
        stats.add(reloadDefaults).left().colspan(2);

        // Add to content
        addContentGroup(contentSystem, titleSystemPrefs, stats, 0f);

        /* COMPUTE LABEL WIDTH */
        float maxLabelWidth = inputSmallWidth * 2.5f;
        for (OwnLabel l : labels) {
            l.pack();
            if (l.getWidth() > maxLabelWidth)
                maxLabelWidth = l.getWidth();
        }
        for (OwnLabel l : labels) {
            l.setWidth(maxLabelWidth);
        }

        /* ADD ALL CONTENT */
        addTabContent(contentGraphics);
        addTabContent(contentScene);
        addTabContent(contentUI);
        addTabContent(contentPerformance);
        addTabContent(contentControls);
        addTabContent(contentScreenshots);
        addTabContent(contentFrames);
        addTabContent(contentCamera);
        addTabContent(content360);
        addTabContent(contentPlanetarium);
        addTabContent(contentData);
        addTabContent(contentSystem);

        /* ADD TO MAIN TABLE */
        content.add(tabStack).left().padLeft(pad20).expand().fill();

        // Set tab listeners.
        setUpTabListeners();
    }

    private void populateWidthHeight(boolean force) {
        if (force || widthField != null && widthField.getText().isBlank()) {
            widthField.setText(Integer.toString(MathUtils.clamp(Gdx.graphics.getWidth(), 100, 10000)));
        }
        if (force || heightField != null && heightField.getText().isBlank()) {
            heightField.setText(Integer.toString(MathUtils.clamp(Gdx.graphics.getHeight(), 100, 10000)));
        }
    }

    private Label getRequiresRestartLabel() {
        OwnLabel restart = new OwnLabel("*", skin, "mono-pink-big");
        restart.addListener(new OwnTextTooltip(I18n.msg("gui.restart"), skin));
        return restart;
    }

    @Override
    public GenericDialog show(Stage stage,
                              Action action) {
        GenericDialog result = super.show(stage, action);
        updateBackupValues();
        populateWidthHeight(true);
        return result;
    }

    @Override
    public void touch() {
        final var settings = Settings.settings;
        // Effects
        setSlider(bloomEffect, settings.postprocess.bloom.intensity);
        setSlider(unsharpMask, settings.postprocess.unsharpMask.factor);
        setSlider(chromaticAberration, settings.postprocess.chromaticAberration.amount);
        setSlider(tessQuality, (float) settings.scene.renderer.elevation.quality);

        // Screen mode
        boolean fullscreen = settings.graphics.fullScreen.active;
        setCheckBox(fullScreen, fullscreen);
        setCheckBox(windowed, !fullscreen);
        enableComponents(!fullscreen, widthField, heightField);
        enableComponents(fullscreen, fullScreenResolutions);
    }

    private void setSlider(OwnSliderPlus slider,
                           float value) {
        slider.setProgrammaticChangeEvents(false);
        slider.setValue(value);
        slider.setProgrammaticChangeEvents(true);
    }

    private void setCheckBox(CheckBox cb,
                             boolean checked) {
        cb.setProgrammaticChangeEvents(false);
        cb.setChecked(checked);
        cb.setProgrammaticChangeEvents(true);
    }

    private void updateBackupValues() {
        final var settings = Settings.settings;
        bloomBak = settings.postprocess.bloom.intensity;
        unsharpMaskBak = settings.postprocess.unsharpMask.factor;
        aberrationBak = settings.postprocess.chromaticAberration.amount;
        filmGrainBak = settings.postprocess.filmGrain.intensity;
        lensFlareBak = settings.postprocess.lensFlare.strength;
        lightGlowBak = settings.postprocess.lightGlow.active;
        brightnessBak = settings.postprocess.levels.brightness;
        contrastBak = settings.postprocess.levels.contrast;
        hueBak = settings.postprocess.levels.hue;
        saturationBak = settings.postprocess.levels.saturation;
        gammaBak = settings.postprocess.levels.gamma;
        toneMappingBak = settings.postprocess.toneMapping.type;
        exposureBak = settings.postprocess.toneMapping.exposure;
        debugInfoBak = settings.program.debugInfo;
        reprojectionBak = settings.postprocess.reprojection.mode;
        upscaleFilterBak = settings.postprocess.upscaleFilter;
        frameCoordinatesBak = settings.program.uvGrid.frameCoordinates;
        FXAAQualityBak = settings.postprocess.antialiasing.quality;
    }

    protected void reloadGamepadMappings(Path selectedFile) {
        Array<FileComboBoxBean> gamepadMappingsFile = new Array<>();
        Path mappingsAssets = Path.of(Settings.ASSETS_LOC, SysUtils.getMappingsDirName());
        Path mappingsData = SysUtils.getDefaultMappingsDir();
        Array<Path> mappingFiles = new Array<>();
        GlobalResources.listRecursive(mappingsAssets, mappingFiles, ".inputListener", ".controller");
        GlobalResources.listRecursive(mappingsData, mappingFiles, ".inputListener", ".controller");
        FileComboBoxBean selected = null;
        for (Path path : mappingFiles) {
            FileComboBoxBean fileBean = new MappingFileComboBoxBean(path);
            gamepadMappingsFile.add(fileBean);
            if (selectedFile == null && Settings.settings.controls.gamepad.mappingsFile.endsWith(path.getFileName().toString())) {
                selected = fileBean;
            } else if (selectedFile != null && selectedFile.toAbsolutePath().toString().endsWith(path.getFileName().toString())) {
                selected = fileBean;
            }
        }

        gamepadMappings.setItems(gamepadMappingsFile);
        gamepadMappings.setSelected(selected);
        gamepadMappings.pack();
    }

    protected void generateGamepadsList(Table table) {
        Array<Controller> controllers = Controllers.getControllers();

        Array<OwnLabel> controllerNames = new Array<>();
        for (Controller c : controllers) {
            OwnLabel cl = new OwnLabel(c.getName(), skin, "default-blue");
            cl.setName(c.getName());
            if (Settings.settings.controls.gamepad.isControllerBlacklisted(c.getName())) {
                cl.setText(cl.getText() + " [*]");
                cl.setColor(1, 0, 0, 1);
                cl.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.controller.blacklist"), skin));
            }
            controllerNames.add(cl);
        }
        if (controllerNames.isEmpty()) {
            controllerNames.add(new OwnLabel(I18n.msg("gui.controller.nocontrollers"), skin));
        }

        if (table == null)
            table = new Table(skin);
        table.clear();
        int i = 0;
        for (OwnLabel cn : controllerNames) {
            String controllerName = cn.getName();
            table.add(cn).left().padBottom(i == controllerNames.size - 1 ? 0f : pad18).padRight(pad34);
            if (controllerName != null && !Settings.settings.controls.gamepad.isControllerBlacklisted(controllerName)) {
                OwnTextButton config = new OwnTextButton(I18n.msg("gui.controller.configure"), skin);
                config.pad(pad10, pad18, pad10, pad18);
                config.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        // Get currently selected mappings
                        GamepadMappings cm = new GamepadMappings(controllerName, Path.of(gamepadMappings.getSelected().file));
                        GamepadConfigWindow ccw = new GamepadConfigWindow(controllerName, cm, stage, skin);
                        ccw.setAcceptListener(() -> {
                            if (ccw.savedFile != null) {
                                // File was saved, reload, select
                                reloadGamepadMappings(ccw.savedFile);
                            }
                        });
                        ccw.show(stage);
                        return true;
                    }
                    return false;
                });
                table.add(config).left().padBottom(i == controllerNames.size - 1 ? 0f : pad18).row();
            } else {
                table.add().left().row();
            }
            i++;
        }
        table.pack();

    }

    @Override
    protected boolean accept() {
        saveCurrentPreferences();
        unsubscribe();
        return true;
    }

    @Override
    public void cancel() {
        revertLivePreferences();
        unsubscribe();
    }

    @Override
    public void dispose() {
        unsubscribe();
    }

    private void reloadDefaultPreferences() {
        // User config file
        Path userFolder = SysUtils.getConfigDir();
        Path userFolderConfFile = userFolder.resolve("config.yaml");

        // Internal config
        Path confFolder = Paths.get(Settings.ASSETS_LOC, "conf" + File.separator);
        Path internalFolderConfFile = confFolder.resolve("config.yaml");

        // Delete current conf
        if (Files.exists(userFolderConfFile)) {
            try {
                Files.delete(userFolderConfFile);
            } catch (IOException e) {
                logger.error(e);
            }
        }

        // Copy file
        try {
            if (Files.exists(confFolder) && Files.isDirectory(confFolder)) {
                // Running released package
                GlobalResources.copyFile(internalFolderConfFile, userFolderConfFile, true);
                logger.info("Default configuration file applied successfully! Gaia Sky will shut down now");
            } else {
                throw new IOException("File " + confFolder + " does not exist!");
            }

        } catch (Exception e) {
            logger.error(e, "Error copying default preferences file to user folder: " + userFolderConfFile);
        }

    }

    private void saveCurrentPreferences() {
        // Add all properties to settings.instance
        final var settings = Settings.settings;

        final boolean reloadFullScreenMode = fullScreen.isChecked() != settings.graphics.fullScreen.active;
        final var selected = fullScreenResolutions.getSelected();
        final boolean reloadScreenMode = reloadFullScreenMode || (settings.graphics.fullScreen.active && (settings.graphics.fullScreen.resolution[0] != selected.width
                || settings.graphics.fullScreen.resolution[1] != selected.height || settings.graphics.fullScreen.refreshRate != selected.refreshRate
                || settings.graphics.fullScreen.bitDepth != selected.bitsPerPixel)) || (
                !settings.graphics.fullScreen.active && (settings.graphics.resolution[0] != Integer.parseInt(widthField.getText()))
                        || settings.graphics.resolution[1] != Integer.parseInt(heightField.getText()));
        boolean resetRenderFlags = false;

        settings.graphics.fullScreen.active = fullScreen.isChecked();

        // Full screen options
        settings.graphics.fullScreen.resolution[0] = selected.width;
        settings.graphics.fullScreen.resolution[1] = selected.height;
        settings.graphics.fullScreen.bitDepth = selected.bitsPerPixel;
        settings.graphics.fullScreen.refreshRate = selected.refreshRate;

        // Windowed options
        settings.graphics.resolution[0] = Integer.parseInt(widthField.getText());
        settings.graphics.resolution[1] = Integer.parseInt(heightField.getText());

        // Graphics
        ComboBoxBean bean = graphicsQuality.getSelected();
        boolean restartDialog = settings.graphics.quality.ordinal() != bean.value;
        if (settings.graphics.quality.ordinal() != bean.value) {
            settings.graphics.quality = GraphicsQuality.values()[bean.value];
        }

        // Antialiasing
        bean = antiAlias.getSelected();
        AntialiasType newAntiAlias = settings.postprocess.getAntialias(bean.value);
        if (settings.postprocess.antialiasing.type != newAntiAlias) {
            EventManager.publish(Event.ANTIALIASING_CMD, this, newAntiAlias);
        }
        EventManager.publish(Event.FXAA_QUALITY_CMD, this, FXAAQuality);

        settings.graphics.vsync = vsyncValue.get();
        try {
            // Windows backend crashes for some reason
            Gdx.graphics.setVSync(settings.graphics.vsync);
        } catch (Exception e) {
            logger.error(e);
        }

        // FPS limiter
        if (maxFps.isChecked()) {
            EventManager.publish(Event.LIMIT_FPS_CMD, this, Parser.parseDouble(maxFpsInput.getText()));
        } else {
            EventManager.publish(Event.LIMIT_FPS_CMD, this, 0.0);
        }

        // Point cloud renderer
        PointCloudMode newPointCloudMode = PointCloudMode.values()[pointCloudRenderer.getSelected().value];
        restartDialog = restartDialog || newPointCloudMode != settings.scene.renderer.pointCloud;
        settings.scene.renderer.pointCloud = newPointCloudMode;

        restartDialog = restartDialog || Settings.settings.data.realGaiaAttitude != real.isChecked();

        // Line renderer
        boolean reloadLineRenderer = settings.scene.renderer.line.mode != LineMode.values()[lineRenderer.getSelected().value];
        bean = lineRenderer.getSelected();
        settings.scene.renderer.line.mode = LineMode.values()[bean.value];

        // Elevation representation
        ElevationType newType = elevationSb.getSelected().type;
        if (SysUtils.isMac() && newType.isTessellation()) {
            newType = ElevationType.REGULAR;
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.elevation.macos"));
            logger.info(I18n.msg("gui.elevation.macos"));
        }
        boolean reloadElevation = newType != settings.scene.renderer.elevation.type;
        settings.scene.renderer.elevation.type = newType;
        restartDialog = restartDialog || reloadElevation;

        // Tess quality
        EventManager.publish(Event.TESSELLATION_QUALITY_CMD, this, tessQuality.getValue());

        // Shadow mapping
        settings.scene.renderer.shadow.active = shadowsCb.isChecked();
        int newShadowResolution = Integer.parseInt(smResolution.getText());
        int newShadowNumber = nShadows.getSelected().value;
        final boolean reloadShadows =
                shadowsCb.isChecked() && (settings.scene.renderer.shadow.resolution != newShadowResolution || settings.scene.renderer.shadow.number != newShadowNumber);

        // Procedural generation texture resolution
        int pgHeight = (int) pgResolution.getValue();
        int pgWidth = pgHeight * 2;
        EventManager.publish(Event.PROCEDURAL_GENERATION_RESOLUTION_CMD, this, pgWidth, pgHeight);

        // Procedural generation save textures.
        EventManager.publish(Event.PROCEDURAL_GENERATION_SAVE_TEXTURES_CMD, this, saveTextures.isChecked());

        // Eclipses
        boolean eclipsesActiveBefore = settings.scene.renderer.eclipses.active;
        settings.scene.renderer.eclipses.active = eclipses.isChecked();
        settings.scene.renderer.eclipses.outlines = eclipseOutlines.isChecked();
        if (eclipsesActiveBefore && !eclipses.isChecked()) {
            // We just deactivated eclipses!
            EventManager.publish(Event.ECLIPSES_CMD, this, eclipses.isChecked());
        }

        // Star spheres
        settings.scene.star.renderStarSpheres = starSpheres.isChecked();

        // Fade time
        settings.scene.fadeMs = MathUtils.clamp(fadeTimeField.getLongValue(settings.scene.fadeMs), Constants.MIN_FADE_TIME_MS, Constants.MAX_FADE_TIME_MS);

        // Dynamic resolution
        settings.graphics.dynamicResolution = !settings.runtime.openXr && dynamicResolution.isChecked();
        if (!settings.graphics.dynamicResolution) {
            GaiaSky.postRunnable(() -> GaiaSky.instance.resetDynamicResolution());
        }

        // SVT cache size
        if (svtCacheSize != null) {
            var reloadSVT = (int) svtCacheSize.getValue() != settings.scene.renderer.virtualTextures.cacheSize;
            restartDialog = restartDialog || reloadSVT;
            if (reloadSVT) {
                GaiaSky.postRunnable(() -> EventManager.publish(Event.SVT_CACHE_SIZE_CMD, this, (int) svtCacheSize.getValue()));
            }
        }

        // SSR
        if (ssr != null) {
            var reloadSSR = settings.postprocess.ssr.active != ssr.isChecked();
            resetRenderFlags = reloadSSR;
            if (reloadSSR) {
                GaiaSky.postRunnable(() -> EventManager.publish(Event.SSR_CMD, ssr, ssr.isChecked()));
            }
        }

        // Back-buffer scale
        if (backBufferScale != null && !backBufferScale.isDisabled()) {
            if (settings.graphics.backBufferScale != backBufferScale.getValue()) {
                GaiaSky.postRunnable(() -> EventManager.publish(Event.BACKBUFFER_SCALE_CMD, backBufferScale, backBufferScale.getValue()));
            }
        }

        // Interface
        LangComboBoxBean languageBean = lang.getSelected();
        StringComobBoxBean newTheme = theme.getSelected();
        // UI scale
        float factor = uiScale.getMappedValue();
        EventManager.publish(Event.UI_SCALE_FACTOR_CMD, this, factor);
        EventManager.publish(Event.UI_SCALE_RECOMPUTE_CMD, this);

        boolean reloadLang = !languageBean.locale.toLanguageTag().equals(settings.program.getLocale());
        boolean reloadUI = reloadLang ||
                !settings.program.ui.theme.equals(newTheme.value) ||
                settings.program.minimap.size != minimapSize.getValue() ||
                settings.program.ui.newUI != newUI.isChecked();

        settings.program.locale = languageBean.locale.toLanguageTag();
        I18n.forceInit(new FileHandle(Settings.ASSETS_LOC + File.separator + "i18n/gsbundle"), new FileHandle(Settings.ASSETS_LOC + File.separator + "i18n/objects"));
        settings.program.ui.theme = newTheme.value;
        boolean previousPointerCoords = settings.program.pointer.coordinates;
        settings.program.pointer.coordinates = pointerCoords.isChecked();
        if (previousPointerCoords != settings.program.pointer.coordinates) {
            EventManager.publish(Event.DISPLAY_POINTER_COORDS_CMD, this, settings.program.pointer.coordinates);
        }

        // Cross-hairs
        EventManager.publish(Event.CROSSHAIR_FOCUS_CMD, this, crosshairFocus.isChecked());
        EventManager.publish(Event.CROSSHAIR_CLOSEST_CMD, this, crosshairClosest.isChecked());
        EventManager.publish(Event.CROSSHAIR_HOME_CMD, this, crosshairHome.isChecked());

        // Pointer guides
        EventManager.publish(Event.POINTER_GUIDES_CMD, this, pointerGuides.isChecked(), pointerGuidesColor.getPickedColor(), pointerGuidesWidth.getMappedValue());

        // Recursive grid
        settings.program.recursiveGrid.origin = OriginType.values()[recGridOrigin.getSelectedIndex()];
        settings.program.recursiveGrid.style = GridStyle.values()[recGridStyle.getSelectedIndex()];
        settings.program.recursiveGrid.projectionLines = recGridProjectionLines.isChecked();

        // Minimap size
        settings.program.minimap.size = minimapSize.getValue();

        // New UI
        settings.program.ui.newUI = newUI.isChecked();

        // Distance units
        settings.program.ui.distanceUnits = DistanceUnits.values()[distUnitsSelect.getSelectedIndex()];

        // Display time in no-UI mode
        settings.program.displayTimeNoUi = displayTimeNoUi.isChecked();

        // Mode change info
        settings.program.ui.modeChangeInfo = modeChangeInfo.isChecked();

        // Performance
        bean = numThreads.getSelected();
        settings.performance.numberThreads = bean.value;
        settings.performance.multithreading = multithreadCb.isChecked();

        settings.scene.octree.fade = lodFadeCb.isChecked();
        settings.scene.octree.threshold[0] =
                MathUtilsDouble.lint(lodTransitions.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_LOD_TRANS_ANGLE_DEG,
                        Constants.MAX_LOD_TRANS_ANGLE_DEG) * (float) MathUtilsDouble.degRad;
        // Here we use a 0.4 rad between the thresholds
        settings.scene.octree.threshold[1] = settings.scene.octree.fade ? settings.scene.octree.threshold[0] + 0.4f : settings.scene.octree.threshold[0];
        // Number of velocity vectors per star group.
        settings.scene.star.group.numVelocityVector = (int) velocityVectors.getValue();

        // Data
        boolean highAccuracy = settings.data.highAccuracy;
        settings.data.highAccuracy = highAccuracyPositions.isChecked();

        if (highAccuracy != settings.data.highAccuracy) {
            // Event
            EventManager.publish(Event.HIGH_ACCURACY_CMD, this, settings.data.highAccuracy);
        }

        // Screenshots
        File ssFile = screenshotsPath.toFile();
        if (ssFile.exists() && ssFile.isDirectory())
            settings.screenshot.location = ssFile.getAbsolutePath();
        ScreenshotMode prev = settings.screenshot.mode;
        settings.screenshot.mode = ScreenshotMode.values()[screenshotMode.getSelectedIndex()];
        int ssw = Integer.parseInt(ssWidthField.getText());
        int ssh = Integer.parseInt(ssHeightField.getText());
        boolean ssUpdate = ssw != settings.screenshot.resolution[0] || ssh != settings.screenshot.resolution[1] || !prev.equals(settings.screenshot.mode);
        settings.screenshot.resolution[0] = ssw;
        settings.screenshot.resolution[1] = ssh;
        if (ssUpdate)
            EventManager.publish(Event.SCREENSHOT_SIZE_UPDATE, this, settings.screenshot.resolution[0], settings.screenshot.resolution[1]);
        // Format
        settings.screenshot.format = ImageFormat.values()[screenshotFormat.getSelected().value];
        // Quality
        settings.screenshot.quality = screenshotQuality.getValue() / 100f;

        // Frame output
        File foFile = frameOutputPath.toFile();
        if (foFile.exists() && foFile.isDirectory())
            settings.frame.location = foFile.getAbsolutePath();
        String text = frameOutputPrefix.getText();
        if (text.matches("^\\w+$")) {
            settings.frame.prefix = text;
        }
        prev = settings.frame.mode;
        settings.frame.mode = ScreenshotMode.values()[frameOutputMode.getSelectedIndex()];
        int fow = Integer.parseInt(foWidthField.getText());
        int foh = Integer.parseInt(foHeightField.getText());
        boolean frameOutputUpdate = fow != settings.frame.resolution[0] || foh != settings.frame.resolution[1] || !prev.equals(settings.frame.mode);
        settings.frame.resolution[0] = fow;
        settings.frame.resolution[1] = foh;
        settings.frame.targetFps = Parser.parseDouble(frameOutputFps.getText());
        if (frameOutputUpdate)
            EventManager.publish(Event.FRAME_SIZE_UPDATE, this, settings.frame.resolution[0], settings.frame.resolution[1]);
        // Format
        settings.frame.format = ImageFormat.values()[frameOutputFormat.getSelected().value];
        // Quality
        settings.frame.quality = frameQuality.getValue() / 100f;

        // Camera recording
        EventManager.publish(Event.CAMRECORDER_FPS_CMD, this, Parser.parseDouble(camRecFps.getText()));
        settings.camrecorder.auto = cbAutoCamRec.isChecked();

        // Cubemap resolution (same as plResolution)
        int newResolution = Integer.parseInt(cmResolution.getText());
        if (newResolution != settings.program.modeCubemap.faceResolution)
            EventManager.publish(Event.CUBEMAP_RESOLUTION_CMD, this, newResolution);

        // Planetarium aperture
        float ap = Float.parseFloat(plAperture.getText());
        if (ap != settings.program.modeCubemap.planetarium.aperture) {
            EventManager.publish(Event.PLANETARIUM_APERTURE_CMD, this, ap);
        }

        // Planetarium angle
        float pa = Float.parseFloat(plAngle.getText());
        if (pa != settings.program.modeCubemap.planetarium.angle) {
            EventManager.publish(Event.PLANETARIUM_ANGLE_CMD, this, pa);
        }

        // Spherical mirror projection warp mesh file
        if (settings.program.modeCubemap.planetarium.sphericalMirrorWarp != meshWarpFilePath) {
            EventManager.publish(Event.PLANETARIUM_GEOMETRYWARP_FILE_CMD, this, meshWarpFilePath);
        }

        // Index of refraction
        if (celestialSphereIndexOfRefraction != null) {
            EventManager.publish(Event.INDEXOFREFRACTION_CMD, this, celestialSphereIndexOfRefraction.getValue());
        }

        // Controllers
        if (gamepadMappings.getSelected() != null) {
            String mappingsFile = gamepadMappings.getSelected().file;
            if (!mappingsFile.equals(settings.controls.gamepad.mappingsFile) && !SysUtils.isAppImagePath(mappingsFile)) {
                settings.controls.gamepad.mappingsFile = mappingsFile;
                EventManager.publish(Event.RELOAD_CONTROLLER_MAPPINGS, this, mappingsFile);
            }
        }
        EventManager.publish(Event.INVERT_X_CMD, this, invertX.isChecked());
        EventManager.publish(Event.INVERT_Y_CMD, this, invertY.isChecked());

        // Gaia attitude
        settings.data.realGaiaAttitude = real.isChecked();

        // System
        if (settings.program.debugInfo != debugInfoBak) {
            EventManager.publish(Event.SHOW_DEBUG_CMD, this, !debugInfoBak);
        }

        // Shader cache
        settings.program.shaderCache = shaderCache.isChecked();

        // Exit confirmation
        settings.program.exitConfirmation = exitConfirmation.isChecked();

        // Save configuration
        SettingsManager.persistSettings(new File(System.getProperty("properties.file")));
        EventManager.publish(Event.PROPERTIES_WRITTEN, this);

        if (reloadScreenMode) {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.SCREEN_MODE_CMD, this));
        }

        if (reloadLineRenderer) {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.LINE_RENDERER_UPDATE, this));
        }

        if (reloadShadows) {
            GaiaSky.postRunnable(() -> {
                settings.scene.renderer.shadow.resolution = newShadowResolution;
                settings.scene.renderer.shadow.number = newShadowNumber;

                EventManager.publish(Event.REBUILD_SHADOW_MAP_DATA_CMD, this);
            });
        }

        if (resetRenderFlags) {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.RESET_RENDERER, this));
        }

        if (reloadLang) {
            reloadLanguage();
        }

        if (reloadUI) {
            reloadUI(globalResources);
        }

        if (restartDialog) {
            showRestartDialog(I18n.msg("gui.restart.setting"));
        }

    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    /**
     * Reverts preferences which have been modified live. It needs backup values.
     */
    private void revertLivePreferences() {
        EventManager.publish(Event.BRIGHTNESS_CMD, this, brightnessBak);
        EventManager.publish(Event.CONTRAST_CMD, this, contrastBak);
        EventManager.publish(Event.HUE_CMD, this, hueBak);
        EventManager.publish(Event.SATURATION_CMD, this, saturationBak);
        EventManager.publish(Event.GAMMA_CMD, this, gammaBak);
        EventManager.publish(Event.LENS_FLARE_CMD, this, lensFlareBak);
        EventManager.publish(Event.LIGHT_GLOW_CMD, this, lightGlowBak);
        EventManager.publish(Event.BLOOM_CMD, this, bloomBak);
        EventManager.publish(Event.UNSHARP_MASK_CMD, this, unsharpMaskBak);
        EventManager.publish(Event.CHROMATIC_ABERRATION_CMD, this, aberrationBak);
        EventManager.publish(Event.FILM_GRAIN_CMD, this, filmGrainBak);
        EventManager.publish(Event.EXPOSURE_CMD, this, exposureBak);
        EventManager.publish(Event.TONEMAPPING_TYPE_CMD, this, toneMappingBak);
        EventManager.publish(Event.SHOW_DEBUG_CMD, this, debugInfoBak);
        EventManager.publish(Event.REPROJECTION_CMD, this, reprojectionBak != ReprojectionMode.DISABLED, reprojectionBak);
        EventManager.publish(Event.UPSCALE_FILTER_CMD, this, upscaleFilterBak);
        EventManager.publish(Event.UV_GRID_FRAME_COORDINATES_CMD, this, frameCoordinatesBak);
        EventManager.publish(Event.FXAA_QUALITY_CMD, this, FXAAQualityBak);
    }

    private void reloadLanguage() {
        EventManager.publish(Event.SCENE_RELOAD_NAMES_CMD, this);
    }

    private void reloadUI(final GlobalResources globalResources) {
        EventManager.publish(Event.UI_RELOAD_CMD, this, globalResources);
    }

    @Override
    protected void showDialogHook(Stage stage) {
        if (frameSequenceNumber != null) {
            frameSequenceNumber.setText(Integer.toString(ImageRenderer.getSequenceNumber()));
        }
    }

    private void showRestartDialog(String text) {
        EventManager.publish(Event.SHOW_RESTART_ACTION, this, text);
    }

    private void selectFullscreen(boolean fullscreen,
                                  OwnTextField widthField,
                                  OwnTextField heightField,
                                  SelectBox<DisplayMode> fullScreenResolutions,
                                  OwnLabel xLabel) {
        final var settings = Settings.settings;
        if (fullscreen) {
            settings.graphics.resolution[0] = fullScreenResolutions.getSelected().width;
            settings.graphics.resolution[1] = fullScreenResolutions.getSelected().height;
        } else if (!widthField.getText().isBlank() && !heightField.getText().isBlank()) {
            settings.graphics.resolution[0] = Integer.parseInt(widthField.getText());
            settings.graphics.resolution[1] = Integer.parseInt(heightField.getText());
        } else {
            populateWidthHeight(true);
        }

        enableComponents(!fullscreen, widthField, heightField, xLabel);
        enableComponents(fullscreen, fullScreenResolutions);
    }

    private int idxAa(AntialiasType x) {
        if (x.getAACode() == -1)
            return 1;
        if (x.getAACode() == -2)
            return 2;
        if (x.getAACode() == 0)
            return 0;
        return (int) (Math.log(x.getAACode()) / FastMath.log(2) + 1e-10) + 2;
    }

    private int idxLang(String code,
                        Array<LangComboBoxBean> langs) {
        if (code == null || code.isEmpty()) {
            code = I18n.messages.getLocale().toLanguageTag();
        }
        for (int i = 0; i < langs.size; i++) {
            if (langs.get(i).locale.toLanguageTag().equals(code)) {
                return i;
            }
        }
        return -1;
    }

    private String keysToString(TreeSet<Integer> keys) {
        StringBuilder s = new StringBuilder();

        int i = 0;
        int n = keys.size();
        for (Integer key : keys) {
            s.append(keyToString(key).toUpperCase().replace(' ', '_'));
            if (i < n - 1) {
                s.append("+");
            }

            i++;
        }

        return s.toString();
    }

    private String keyToString(int key) {
        var str = Keys.toString(key);
        if (str.equals("+")) {
            str = "PL";
        }
        return str;
    }

    public void resize(int width, int height) {
        populateWidthHeight(true);
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        switch (event) {
            case CONTROLLER_CONNECTED_INFO, CONTROLLER_DISCONNECTED_INFO -> generateGamepadsList(controllersTable);
            case INVERT_X_CMD -> {
                if (source != this && invertX != null) {
                    invertX.setProgrammaticChangeEvents(false);
                    invertX.setChecked((Boolean) data[0]);
                    invertX.setProgrammaticChangeEvents(true);
                }
            }
            case INVERT_Y_CMD -> {
                if (source != this && invertY != null) {
                    invertY.setProgrammaticChangeEvents(false);
                    invertY.setChecked((Boolean) data[0]);
                    invertY.setProgrammaticChangeEvents(true);
                }
            }
            case WINDOW_RESOLUTION_INFO -> {
                if (source != this) {
                    var width = (Integer) data[0];
                    var height = (Integer) data[1];
                    widthField.setText(width.toString());
                    heightField.setText(height.toString());
                }
            }
            default -> {
            }
        }
    }

}
