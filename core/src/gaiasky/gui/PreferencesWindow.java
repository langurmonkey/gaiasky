/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
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
import gaiasky.gui.KeyBindings.ProgramAction;
import gaiasky.gui.beans.*;
import gaiasky.input.WindowGamepadListener;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.*;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.screenshot.ImageRenderer;
import gaiasky.util.validator.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static gaiasky.util.Settings.OriginType.values;

/**
 * The default preferences window.
 */
public class PreferencesWindow extends GenericDialog implements IObserver {
    private static final Log logger = Logger.getLogger(PreferencesWindow.class);

    private final Array<OwnLabel> labels;

    private final DecimalFormat nf3;

    private CheckBox fullScreen, windowed, vsync, maxFps, multithreadCb, lodFadeCb, cbAutoCamrec, real, nsl, invertX, invertY, highAccuracyPositions, shadowsCb, pointerCoords, modeChangeInfo, debugInfo, crosshairFocus, crosshairClosest, crosshairHome, pointerGuides, exitConfirmation, recGridProjectionLines, dynamicResolution, motionBlur, ssr;
    private OwnSelectBox<DisplayMode> fullScreenResolutions;
    private OwnSelectBox<ComboBoxBean> graphicsQuality, aa, pointCloudRenderer, lineRenderer, numThreads, screenshotMode, frameOutputMode, nShadows, distUnitsSelect;
    private OwnSelectBox<LangComboBoxBean> lang;
    private OwnSelectBox<ElevationComboBoxBean> elevationSb;
    private OwnSelectBox<String> recGridOrigin;
    private OwnSelectBox<StrComboBoxBean> theme;
    private OwnSelectBox<FileComboBoxBean> gamepadMappings;
    private OwnSelectBox<ReprojectionMode> reprojectionMode;
    private OwnTextField fadeTimeField, widthField, heightField, ssWidthField, ssHeightField, frameOutputPrefix, frameOutputFps, foWidthField, foHeightField, camRecFps, cmResolution, plResolution, plAperture, plAngle, smResolution, maxFpsInput;
    private OwnSlider lodTransitions, tessQuality, minimapSize, pointerGuidesWidth, uiScale, backBufferScale, celestialSphereIndexOfRefraction;
    private OwnTextButton screenshotsLocation, frameOutputLocation;
    private OwnLabel frameSequenceNumber;
    private ColorPicker pointerGuidesColor;
    private OwnLabel tessQualityLabel;
    private Cell<?> noticeHiResCell;
    private Table controllersTable;

    // Backup values
    private ToneMapping toneMappingBak;
    private float brightnessBak, contrastBak, hueBak, saturationBak, gammaBak, exposureBak, bloomBak, unsharpMaskBak;
    private boolean lensflareBak, lightGlowBak, debugInfoBak;
    private ReprojectionMode reprojectionBak;

    private final GlobalResources globalResources;
    private final Settings settings;

    // This flag is active when the dialog is called from the welcome screen
    private final boolean welcomeScreen;

    public PreferencesWindow(final Stage stage, final Skin skin, final GlobalResources globalResources) {
        this(stage, skin, globalResources, false);
    }

    public PreferencesWindow(final Stage stage, final Skin skin, final GlobalResources globalResources, final boolean welcomeScreen) {
        super(I18n.msg("gui.settings") + " - " + Settings.settings.version.version + " - " + I18n.msg("gui.build", Settings.settings.version.build), skin, stage);

        this.settings = Settings.settings;
        this.tabContents = new Array<>();
        this.labels = new Array<>();
        this.globalResources = globalResources;
        this.welcomeScreen = welcomeScreen;

        this.nf3 = new DecimalFormat("0.000");

        setAcceptText(I18n.msg("gui.saveprefs"));
        setCancelText(I18n.msg("gui.cancel"));

        // Build UI.
        buildSuper();

        gamepadListener = new WindowGamepadListener(Settings.settings.controls.gamepad.mappingsFile, stage, this);

        EventManager.instance.subscribe(this, Event.CONTROLLER_CONNECTED_INFO, Event.CONTROLLER_DISCONNECTED_INFO);
    }

    private OwnTextIconButton createTab(String title, Image img, Skin skin) {
        OwnTextIconButton tab = new OwnTextIconButton(TextUtils.capString(title, 26), img, skin, "toggle-big");
        tab.addListener(new OwnTextTooltip(title, skin));
        tab.pad(pad5);
        tab.setWidth(480f);
        return tab;
    }

    @Override
    protected void build() {
        final float contentWidth = 1100f;
        final float contentHeight = 1120f;
        final float taWidth = 960f;
        final float textWidth = 104f;
        final float scrollHeight = 640f;
        final float controlsScrollWidth = 1000f;
        final float controlsScrollHeight = 560f;
        final float sliderWidth = textWidth * 3f;
        final float buttonHeight = 40f;

        boolean safeMode = settings.program.safeMode;
        boolean vr = settings.runtime.openVr;

        // Create the tab buttons
        Table tabsTable = new Table(skin);
        tabsTable.align(Align.left | Align.top);

        final OwnTextIconButton tabGraphics = createTab(I18n.msg("gui.graphicssettings"), new Image(skin.getDrawable("iconic-bolt")), skin);
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
        content.add(tabsTable).align(Align.left | Align.top).padLeft(pad5);

        tabButtons = new Array<>();
        tabButtons.add(tabGraphics);
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

        // RESOLUTION/MODE
        Label titleResolution = new OwnLabel(I18n.msg("gui.resolutionmode"), skin, "header");
        Table mode = new Table();

        // Full screen mode resolutions
        Array<DisplayMode> modes = new Array<>(Gdx.graphics.getDisplayModes());
        modes.sort((o1, o2) -> Integer.compare(o2.height * o2.width, o1.height * o1.width));
        fullScreenResolutions = new OwnSelectBox<>(skin);
        fullScreenResolutions.setWidth(textWidth * 3.45f);
        fullScreenResolutions.setItems(modes);

        DisplayMode selectedMode = null;
        for (DisplayMode dm : modes) {
            if (dm.width == settings.graphics.fullScreen.resolution[0] && dm.height == settings.graphics.fullScreen.resolution[1] && dm.bitsPerPixel == settings.graphics.fullScreen.bitDepth && dm.refreshRate == settings.graphics.fullScreen.refreshRate) {
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
        widthField = new OwnTextField(Integer.toString(MathUtils.clamp(Gdx.graphics.getWidth(), 100, 10000)), skin, widthValidator);
        widthField.setWidth(textWidth);
        IValidator heightValidator = new IntValidator(100, 10000);
        heightField = new OwnTextField(Integer.toString(MathUtils.clamp(Gdx.graphics.getHeight(), 100, 10000)), skin, heightValidator);
        heightField.setWidth(textWidth);
        final OwnLabel widthLabel = new OwnLabel(I18n.msg("gui.width") + ":", skin);
        final OwnLabel heightLabel = new OwnLabel(I18n.msg("gui.height") + ":", skin);

        windowedResolutions.add(widthLabel).left().padRight(pad5);
        windowedResolutions.add(widthField).left().padRight(pad5);
        windowedResolutions.add(heightLabel).left().padRight(pad5);
        windowedResolutions.add(heightField).left().row();

        // Radio buttons
        fullScreen = new OwnCheckBox(I18n.msg("gui.fullscreen"), skin, "radio", pad5);
        fullScreen.addListener(event -> {
            if (event instanceof ChangeEvent) {
                selectFullscreen(fullScreen.isChecked(), widthField, heightField, fullScreenResolutions, widthLabel, heightLabel);
                return true;
            }
            return false;
        });
        fullScreen.setChecked(settings.graphics.fullScreen.active);

        windowed = new OwnCheckBox(I18n.msg("gui.windowed"), skin, "radio", pad5);
        windowed.addListener(event -> {
            if (event instanceof ChangeEvent) {
                selectFullscreen(!windowed.isChecked(), widthField, heightField, fullScreenResolutions, widthLabel, heightLabel);
                return true;
            }
            return false;
        });
        windowed.setChecked(!settings.graphics.fullScreen.active);
        selectFullscreen(settings.graphics.fullScreen.active, widthField, heightField, fullScreenResolutions, widthLabel, heightLabel);

        new ButtonGroup<>(fullScreen, windowed);

        // VSYNC
        OwnLabel vsyncLabel = new OwnLabel(I18n.msg("gui.vsync"), skin);
        vsync = new OwnCheckBox("", skin);
        vsync.setChecked(settings.graphics.vsync);
        vsync.setColor(Color.YELLOW);

        // LIMIT FPS
        IValidator limitFpsValidator = new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS);
        double limitFps = settings.graphics.fpsLimit == 0 ? 60 : settings.graphics.fpsLimit;
        this.maxFpsInput = new OwnTextField(nf3.format(MathUtilsd.clamp(limitFps, Constants.MIN_FPS, Constants.MAX_FPS)), skin, limitFpsValidator);
        this.maxFpsInput.setDisabled(settings.graphics.fpsLimit == 0);

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
        maxFpsGroup.space(pad10);
        maxFpsGroup.addActor(maxFps);
        maxFpsGroup.addActor(this.maxFpsInput);

        labels.addAll(vsyncLabel, maxFpsLabel);

        mode.add(fullScreen).left().padRight(pad10);
        mode.add(fullScreenResolutions).left().row();
        mode.add(windowed).left().padRight(pad10).padTop(pad10).padBottom(pad5);
        mode.add(windowedResolutions).left().padTop(pad10).padBottom(pad5).row();
        mode.add(vsyncLabel).left().padRight(pad20).padBottom(pad5);
        mode.add(vsync).left().padBottom(pad5).row();
        mode.add(maxFpsLabel).left().padRight(pad20).padBottom(pad5);
        mode.add(maxFpsGroup).left().padBottom(pad5).row();

        // Add to content
        contentGraphicsTable.add(titleResolution).left().padBottom(pad10).row();
        contentGraphicsTable.add(mode).left().padBottom(pad20).row();

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
        graphicsQuality.setWidth(textWidth * 3f);
        graphicsQuality.setSelected(gqs[settings.graphics.quality.ordinal()]);
        graphicsQuality.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                ComboBoxBean s = graphicsQuality.getSelected();
                GraphicsQuality gq = GraphicsQuality.values()[s.value];
                if ((DataDescriptor.localDataDescriptor == null || !DataDescriptor.localDataDescriptor.datasetPresent("hi-res-textures")) && (gq.isHigh() || gq.isUltra())) {
                    // Show notice
                    // Hi resolution textures notice
                    if (noticeHiResCell != null && noticeHiResCell.getActor() == null) {
                        String infoString = I18n.msg("gui.gquality.hires.info") + "\n";
                        int lines1 = GlobalResources.countOccurrences(infoString, '\n');
                        OwnTextArea noticeHiRes = new OwnTextArea(infoString, skin, "info");
                        noticeHiRes.setDisabled(true);
                        noticeHiRes.setPrefRows(lines1 + 1);
                        noticeHiRes.setWidth(600f);
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

        OwnImageButton gqualityTooltip = new OwnImageButton(skin, "tooltip");
        gqualityTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.gquality.info"), skin));

        // AA
        OwnLabel aaLabel = new OwnLabel(I18n.msg("gui.aa"), skin);
        aaLabel.addListener(new OwnTextTooltip(I18n.msg("gui.aa.info"), skin));

        ComboBoxBean[] aas = new ComboBoxBean[] { new ComboBoxBean(I18n.msg("gui.aa.no"), 0), new ComboBoxBean(I18n.msg("gui.aa.fxaa"), -1), new ComboBoxBean(I18n.msg("gui.aa.nfaa"), -2) };
        aa = new OwnSelectBox<>(skin);
        aa.setItems(aas);
        aa.setWidth(textWidth * 3f);
        aa.setSelected(aas[idxAa(2, settings.postprocess.antialias)]);

        OwnImageButton aaTooltip = new OwnImageButton(skin, "tooltip");
        aaTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.aa.info"), skin));

        // Only if not VR, the triangles break in VR
        if (!settings.runtime.openVr) {
            // POINT CLOUD
            ComboBoxBean[] pointCloudItems = new ComboBoxBean[] { new ComboBoxBean(I18n.msg("gui.pointcloud.tris"), PointCloudMode.TRIANGLES.ordinal()), new ComboBoxBean(I18n.msg("gui.pointcloud.instancedtris"), PointCloudMode.TRIANGLES_INSTANCED.ordinal()), new ComboBoxBean(I18n.msg("gui.pointcloud.points"), PointCloudMode.POINTS.ordinal()) };
            pointCloudRenderer = new OwnSelectBox<>(skin);
            pointCloudRenderer.setItems(pointCloudItems);
            pointCloudRenderer.setWidth(textWidth * 3f);
            pointCloudRenderer.setSelected(pointCloudItems[settings.scene.renderer.pointCloud.ordinal()]);
        }

        // LINE RENDERER
        OwnLabel lrLabel = new OwnLabel(I18n.msg("gui.linerenderer"), skin);
        ComboBoxBean[] lineRenderers = new ComboBoxBean[] { new ComboBoxBean(I18n.msg("gui.linerenderer.normal"), LineMode.GL_LINES.ordinal()), new ComboBoxBean(I18n.msg("gui.linerenderer.quad"), LineMode.POLYLINE_QUADSTRIP.ordinal()) };
        lineRenderer = new OwnSelectBox<>(skin);
        lineRenderer.setItems(lineRenderers);
        lineRenderer.setWidth(textWidth * 3f);
        lineRenderer.setSelected(lineRenderers[settings.scene.renderer.line.ordinal()]);

        // BLOOM
        OwnLabel bloomLabel = new OwnLabel(I18n.msg("gui.bloom"), skin, "default");
        Slider bloomEffect = new OwnSlider(Constants.MIN_BLOOM, Constants.MAX_BLOOM, Constants.SLIDER_STEP_TINY, skin);
        bloomEffect.setName("bloom effect");
        bloomEffect.setWidth(sliderWidth);
        bloomEffect.setValue(settings.postprocess.bloom.intensity * 10f);
        bloomEffect.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.BLOOM_CMD, bloomEffect, bloomEffect.getValue());
                return true;
            }
            return false;
        });

        // UNSHARP MASK
        OwnLabel unsharpMaskLabel = new OwnLabel(I18n.msg("gui.unsharpmask"), skin, "default");
        Slider unsharpMaskFactor = new OwnSlider(Constants.MIN_UNSHARP_MASK_FACTOR, Constants.MAX_UNSHARP_MASK_FACTOR, Constants.SLIDER_STEP_TINY, skin);
        unsharpMaskFactor.setName("unsharp mask factor");
        unsharpMaskFactor.setWidth(sliderWidth);
        unsharpMaskFactor.setValue(settings.postprocess.unsharpMask.factor);
        unsharpMaskFactor.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.UNSHARP_MASK_CMD, unsharpMaskFactor, unsharpMaskFactor.getValue());
                return true;
            }
            return false;
        });

        // LABELS
        labels.addAll(graphicsQualityLabel, aaLabel, lrLabel, bloomLabel);

        // LENS FLARE
        OwnLabel lensFlareLabel = new OwnLabel(I18n.msg("gui.lensflare"), skin);
        CheckBox lensFlare = new OwnCheckBox("", skin);
        lensFlare.setName("lens flare");
        lensFlare.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.LENS_FLARE_CMD, lensFlare, lensFlare.isChecked());
                return true;
            }
            return false;
        });
        lensFlare.setChecked(settings.postprocess.lensFlare.active);

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

        // FADE TIME
        OwnLabel fadeTimeLabel = new OwnLabel(I18n.msg("gui.fadetime"), skin, "default");
        IValidator fadeTimeValidator = new LongValidator(Constants.MIN_FADE_TIME_MS, Constants.MAX_FADE_TIME_MS);
        fadeTimeField = new OwnTextField(Long.toString(settings.scene.fadeMs), skin, fadeTimeValidator);
        fadeTimeField.setWidth(sliderWidth);
        OwnImageButton fadeTimeTooltip = new OwnImageButton(skin, "tooltip");
        fadeTimeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.fadetime.info"), skin));

        graphics.add(graphicsQualityLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(graphicsQuality).left().padRight(pad10).padBottom(pad5);
        graphics.add(gqualityTooltip).left().padBottom(pad5).row();
        noticeHiResCell = graphics.add();
        noticeHiResCell.colspan(3).left().row();
        graphics.add(aaLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(aa).left().padRight(pad10).padBottom(pad5);
        graphics.add(aaTooltip).left().padBottom(pad5).row();
        if (!settings.runtime.openVr) {
            OwnLabel pointCloudLabel = new OwnLabel(I18n.msg("gui.pointcloud"), skin);
            OwnImageButton pointCloudTooltip = new OwnImageButton(skin, "tooltip");
            pointCloudTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.pointcloud.info"), skin));
            OwnLabel restart = new OwnLabel(I18n.msg("gui.restart"), skin, "default-pink");
            graphics.add(pointCloudLabel).left().padRight(pad20).padBottom(pad5);
            graphics.add(pointCloudRenderer).left().padBottom(pad5);
            graphics.add(pointCloudTooltip).left().padRight(pad20).padBottom(pad5);
            graphics.add(restart).left().padRight(pad20).padBottom(pad5).row();
        }
        graphics.add(lrLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(lineRenderer).left().padBottom(pad5).row();
        graphics.add(bloomLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(bloomEffect).left().padBottom(pad5).row();
        graphics.add(unsharpMaskLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(unsharpMaskFactor).left().padBottom(pad5).row();
        graphics.add(lensFlareLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(lensFlare).left().padBottom(pad5).row();
        graphics.add(lightGlowLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(lightGlow).left().padBottom(pad5).row();
        graphics.add(fadeTimeLabel).left().padRight(pad20).padBottom(pad5);
        graphics.add(fadeTimeField).left().padRight(pad10).padBottom(pad5);
        graphics.add(fadeTimeTooltip).left().padRight(pad20).padBottom(pad5).row();

        // Add to content
        contentGraphicsTable.add(titleGraphics).left().padBottom(pad10).row();
        contentGraphicsTable.add(graphics).left().padBottom(pad20).row();

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
        elevationSb.setWidth(textWidth * 3f);
        elevationSb.setSelectedIndex(Settings.settings.scene.renderer.elevation.type.ordinal());
        elevationSb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                enableComponents(elevationSb.getSelected().type.isTessellation(), tessQuality, tessQualityLabel);
            }
            return false;
        });

        // TESSELLATION QUALITY
        tessQualityLabel = new OwnLabel(I18n.msg("gui.elevation.tessellation.quality"), skin);
        tessQualityLabel.setDisabled(!settings.scene.renderer.elevation.type.isTessellation());

        tessQuality = new OwnSlider(Constants.MIN_TESS_QUALITY, Constants.MAX_TESS_QUALITY, 0.1f, skin);
        tessQuality.setDisabled(!settings.scene.renderer.elevation.type.isTessellation());
        tessQuality.setWidth(sliderWidth);
        tessQuality.setValue((float) settings.scene.renderer.elevation.quality);

        // LABELS
        labels.add(elevationTypeLabel, tessQualityLabel);

        elevation.add(elevationTypeLabel).left().padRight(pad20).padBottom(pad5);
        elevation.add(elevationSb).left().padRight(pad10).padBottom(pad5).row();
        elevation.add(tessQualityLabel).left().padRight(pad20).padBottom(pad5);
        elevation.add(tessQuality).left().padRight(pad10).padBottom(pad5);

        // Add to content
        contentGraphicsTable.add(titleElevation).left().padBottom(pad10).row();
        contentGraphicsTable.add(elevation).left().padBottom(pad20).row();

        // SHADOWS
        Label titleShadows = new OwnLabel(I18n.msg("gui.graphics.shadows"), skin, "header");
        Table shadows = new Table();

        // SHADOW MAP RESOLUTION
        OwnLabel smResolutionLabel = new OwnLabel(I18n.msg("gui.graphics.shadows.resolution"), skin);
        smResolutionLabel.setDisabled(!settings.scene.renderer.shadow.active);
        IntValidator smResValidator = new IntValidator(128, 4096);
        smResolution = new OwnTextField(Integer.toString(MathUtils.clamp(settings.scene.renderer.shadow.resolution, 128, 4096)), skin, smResValidator);
        smResolution.setWidth(textWidth * 3f);
        smResolution.setDisabled(!settings.scene.renderer.shadow.active);

        // N SHADOWS
        OwnLabel nShadowsLabel = new OwnLabel("#" + I18n.msg("gui.graphics.shadows"), skin);
        nShadowsLabel.setDisabled(!settings.scene.renderer.shadow.active);

        int nSh = 10;
        ComboBoxBean[] nsh = new ComboBoxBean[nSh];
        IntStream.rangeClosed(1, nSh).forEach(s -> nsh[s - 1] = new ComboBoxBean(String.valueOf(s), s));

        nShadows = new OwnSelectBox<>(skin);
        nShadows.setItems(nsh);
        nShadows.setWidth(textWidth * 3f);
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

        shadows.add(shadowsLabel).left().padRight(pad20).padBottom(pad5);
        shadows.add(shadowsCb).left().padRight(pad10).padBottom(pad5).row();
        shadows.add(smResolutionLabel).left().padRight(pad20).padBottom(pad5);
        shadows.add(smResolution).left().padRight(pad10).padBottom(pad5).row();
        shadows.add(nShadowsLabel).left().padRight(pad20).padBottom(pad5);
        shadows.add(nShadows).left().padRight(pad10).padBottom(pad5);

        // Add to content
        contentGraphicsTable.add(titleShadows).left().padBottom(pad10).row();
        contentGraphicsTable.add(shadows).left().padBottom(pad20).row();

        // IMAGE LEVELS
        Label titleDisplay = new OwnLabel(I18n.msg("gui.graphics.imglevels"), skin, "header");
        Table imageLevels = new Table();


        /* Brightness */
        OwnLabel brightnessLabel = new OwnLabel(I18n.msg("gui.brightness"), skin, "default");
        Slider brightness = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        brightness.setName("brightness");
        brightness.setWidth(sliderWidth);
        brightness.setValue(MathUtilsd.lint(settings.postprocess.levels.brightness, Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        brightness.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.BRIGHTNESS_CMD, brightness, MathUtilsd.lint(brightness.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_BRIGHTNESS, Constants.MAX_BRIGHTNESS), true);
                return true;
            }
            return false;
        });

        imageLevels.add(brightnessLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(brightness).left().padRight(pad10).padBottom(pad5).row();

        /* Contrast */
        OwnLabel contrastLabel = new OwnLabel(I18n.msg("gui.contrast"), skin, "default");
        Slider contrast = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        contrast.setName("contrast");
        contrast.setWidth(sliderWidth);
        contrast.setValue(MathUtilsd.lint(settings.postprocess.levels.contrast, Constants.MIN_CONTRAST, Constants.MAX_CONTRAST, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        contrast.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CONTRAST_CMD, contrast, MathUtilsd.lint(contrast.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_CONTRAST, Constants.MAX_CONTRAST), true);
                return true;
            }
            return false;
        });

        imageLevels.add(contrastLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(contrast).left().padRight(pad10).padBottom(pad5).row();

        /* Hue */
        OwnLabel hueLabel = new OwnLabel(I18n.msg("gui.hue"), skin, "default");
        Slider hue = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        hue.setName("hue");
        hue.setWidth(sliderWidth);
        hue.setValue(MathUtilsd.lint(settings.postprocess.levels.hue, Constants.MIN_HUE, Constants.MAX_HUE, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        hue.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.HUE_CMD, hue, MathUtilsd.lint(hue.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_HUE, Constants.MAX_HUE), true);
                return true;
            }
            return false;
        });

        imageLevels.add(hueLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(hue).left().padRight(pad10).padBottom(pad5).row();

        /* Saturation */
        OwnLabel saturationLabel = new OwnLabel(I18n.msg("gui.saturation"), skin, "default");
        Slider saturation = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, skin);
        saturation.setName("saturation");
        saturation.setWidth(sliderWidth);
        saturation.setValue(MathUtilsd.lint(settings.postprocess.levels.saturation, Constants.MIN_SATURATION, Constants.MAX_SATURATION, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        saturation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SATURATION_CMD, saturation, MathUtilsd.lint(saturation.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_SATURATION, Constants.MAX_SATURATION), true);
                return true;
            }
            return false;
        });

        imageLevels.add(saturationLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(saturation).left().padRight(pad10).padBottom(pad5).row();

        /* Gamma */
        OwnLabel gammaLabel = new OwnLabel(I18n.msg("gui.gamma"), skin, "default");
        Slider gamma = new OwnSlider(Constants.MIN_GAMMA, Constants.MAX_GAMMA, 0.1f, false, skin);
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

        imageLevels.add(gammaLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(gamma).left().padRight(pad10).padBottom(pad5).row();

        /* Tone Mapping */
        OwnLabel toneMappingLabel = new OwnLabel(I18n.msg("gui.tonemapping.type"), skin, "default");
        int nToneMapping = ToneMapping.values().length;
        ComboBoxBean[] toneMappingTypes = new ComboBoxBean[nToneMapping];
        for (int itm = 0; itm < nToneMapping; itm++) {
            ToneMapping tm = ToneMapping.values()[itm];
            toneMappingTypes[itm] = new ComboBoxBean(I18n.msg("gui.tonemapping." + tm.name().toLowerCase(Locale.ROOT)), tm.ordinal());
        }

        OwnSelectBox<ComboBoxBean> toneMappingSelect = new OwnSelectBox<>(skin);
        toneMappingSelect.setItems(toneMappingTypes);
        toneMappingSelect.setWidth(textWidth * 3f);
        toneMappingSelect.setSelectedIndex(settings.postprocess.toneMapping.type.ordinal());
        imageLevels.add(toneMappingLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(toneMappingSelect).left().padBottom(pad5).row();

        /* Exposure */
        OwnLabel exposureLabel = new OwnLabel(I18n.msg("gui.exposure"), skin, "default");
        exposureLabel.setDisabled(settings.postprocess.toneMapping.type != ToneMapping.EXPOSURE);
        Slider exposure = new OwnSlider(Constants.MIN_EXPOSURE, Constants.MAX_EXPOSURE, 0.1f, false, skin);
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

        imageLevels.add(exposureLabel).left().padRight(pad20).padBottom(pad5);
        imageLevels.add(exposure).left().padRight(pad10).padBottom(pad5).row();

        // LABELS
        labels.addAll(brightnessLabel, contrastLabel, hueLabel, saturationLabel, gammaLabel);

        // Add to content
        contentGraphicsTable.add(titleDisplay).left().padBottom(pad10).row();
        contentGraphicsTable.add(imageLevels).left().padBottom(pad20).row();

        if (!settings.runtime.openVr) {
            // EXPERIMENTAL
            Label titleExperimental = new OwnLabel(I18n.msg("gui.experimental"), skin, "header");
            Table experimental = new Table();

            // Re-projection
            OwnLabel reprojectionLabel = new OwnLabel(I18n.msg("gui.reproj"), skin);
            ReprojectionMode[] reprojectionModes = ReprojectionMode.values();
            reprojectionMode = new OwnSelectBox<>(skin);
            reprojectionMode.setItems(reprojectionModes);
            reprojectionMode.setWidth(textWidth * 3f);
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

            experimental.add(reprojectionLabel).left().padRight(pad20).padBottom(pad5);
            experimental.add(reprojectionMode).left().padRight(pad10).padBottom(pad5).row();

            // Dynamic resolution
            OwnLabel dynamicResolutionLabel = new OwnLabel(I18n.msg("gui.dynamicresolution"), skin);
            dynamicResolution = new OwnCheckBox("", skin);
            dynamicResolution.setChecked(settings.graphics.dynamicResolution);
            OwnImageButton dynamicResolutionTooltip = new OwnImageButton(skin, "tooltip");
            dynamicResolutionTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.dynamicresolution.info"), skin));

            experimental.add(dynamicResolutionLabel).left().padRight(pad20).padBottom(pad5);
            experimental.add(dynamicResolution).left().padRight(pad10).padBottom(pad5);
            experimental.add(dynamicResolutionTooltip).left().padBottom(pad5).row();

            // Back-buffer scale
            OwnLabel backBufferScaleLabel = new OwnLabel(I18n.msg("gui.backbuffer.scale"), skin);
            backBufferScaleLabel.setDisabled(settings.graphics.dynamicResolution);
            backBufferScale = new OwnSlider(Constants.BACKBUFFER_SCALE_MIN, Constants.BACKBUFFER_SCALE_MAX, Constants.BACKBUFFER_SCALE_STEP, skin);
            backBufferScale.setWidth(textWidth * 3f);
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

            experimental.add(backBufferScaleLabel).left().padRight(pad20).padBottom(pad5);
            experimental.add(backBufferScale).left().padRight(pad10).padBottom(pad5);
            experimental.add(backBufferTooltip).left().padBottom(pad5).row();

            // Index of refraction of celestial sphere
            OwnLabel celestialSphereIndexOfRefractionLabel = new OwnLabel(I18n.msg("gui.indexofrefraction"), skin);
            celestialSphereIndexOfRefraction = new OwnSlider(1.f, 2.5f, 0.05f, skin);
            celestialSphereIndexOfRefraction.setWidth(textWidth * 3f);
            celestialSphereIndexOfRefraction.setMappedValue(settings.program.modeCubemap.celestialSphereIndexOfRefraction);
            OwnImageButton celestialSphereIndexOfRefractionTooltip = new OwnImageButton(skin, "tooltip");
            celestialSphereIndexOfRefractionTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.indexofrefraction.info"), skin));

            experimental.add(celestialSphereIndexOfRefractionLabel).left().padRight(pad20).padBottom(pad5);
            experimental.add(celestialSphereIndexOfRefraction).left().padRight(pad10).padBottom(pad5);
            experimental.add(celestialSphereIndexOfRefractionTooltip).left().padBottom(pad5).row();

            // SSR
            OwnLabel ssrLabel = new OwnLabel(I18n.msg("gui.ssr"), skin);
            ssr = new OwnCheckBox("", skin);
            ssr.setChecked(!safeMode && !vr && settings.postprocess.ssr.active);
            ssr.setDisabled(safeMode || vr);
            OwnImageButton ssrTooltip = new OwnImageButton(skin, "tooltip");
            ssrTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.ssr.info"), skin));

            experimental.add(ssrLabel).left().padRight(pad20).padBottom(pad5);
            experimental.add(ssr).left().padRight(pad10).padBottom(pad5);
            experimental.add(ssrTooltip).left().padBottom(pad5).row();

            // MOTION BLUR
            OwnLabel motionBlurLabel = new OwnLabel(I18n.msg("gui.motionblur"), skin);
            motionBlur = new OwnCheckBox("", skin);
            motionBlur.setName("motion blur");
            motionBlur.setChecked(!safeMode && !vr && settings.postprocess.motionBlur.active);
            motionBlur.setDisabled(safeMode || vr);

            experimental.add(motionBlurLabel).left().padRight(pad20).padBottom(pad5);
            experimental.add(motionBlur).left().padRight(pad10).padBottom(pad5);

            // LABELS
            labels.addAll(dynamicResolutionLabel);
            labels.addAll(ssrLabel);

            // Add to content
            contentGraphicsTable.add(titleExperimental).left().padBottom(pad10).row();
            contentGraphicsTable.add(experimental).left();
        }

        /*
         * ==== UI ====
         */
        float labelWidth = 400f;

        final Table contentUI = new Table(skin);
        contentUI.setWidth(contentWidth);
        contentUI.align(Align.top | Align.left);

        OwnLabel titleUI = new OwnLabel(I18n.msg("gui.ui.interfacesettings"), skin, "header");

        Table ui = new Table();

        // LANGUAGE
        OwnLabel langLabel = new OwnLabel(I18n.msg("gui.ui.language"), skin);
        langLabel.setWidth(labelWidth);
        File i18nDir = new File(settings.ASSETS_LOC + File.separator + "i18n");
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
        lang.setWidth(textWidth * 3f);
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
        themeLabel.setWidth(labelWidth);

        StrComboBoxBean[] themes = new StrComboBoxBean[] { new StrComboBoxBean(I18n.msg("gui.theme.darkgreen"), "dark-green"), new StrComboBoxBean(I18n.msg("gui.theme.darkblue"), "dark-blue"), new StrComboBoxBean(I18n.msg("gui.theme.darkorange"), "dark-orange"), new StrComboBoxBean(I18n.msg("gui.theme.nightred"), "night-red") };
        theme = new OwnSelectBox<>(skin);
        theme.setWidth(textWidth * 3f);
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
        OwnLabel uiScalelabel = new OwnLabel(I18n.msg("gui.ui.theme.scale"), skin);
        uiScalelabel.setWidth(labelWidth);
        uiScale = new OwnSlider(Constants.UI_SCALE_MIN, Constants.UI_SCALE_MAX, Constants.SLIDER_STEP_SMALL, Constants.UI_SCALE_INTERNAL_MIN, Constants.UI_SCALE_INTERNAL_MAX, skin);
        uiScale.setWidth(textWidth * 3f);
        uiScale.setMappedValue(settings.program.ui.scale);
        OwnTextButton applyUiScale = new OwnTextButton(I18n.msg("gui.apply"), skin);
        applyUiScale.pad(0, pad10, 0, pad10);
        applyUiScale.setHeight(buttonHeight);
        applyUiScale.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.UI_SCALE_CMD, uiScale, uiScale.getMappedValue());
                return true;
            }
            return false;
        });

        // MINIMAP SIZE
        OwnLabel minimapSizeLabel = new OwnLabel(I18n.msg("gui.ui.minimap.size"), skin, "default");
        minimapSizeLabel.setWidth(labelWidth);
        minimapSize = new OwnSlider(Constants.MIN_MINIMAP_SIZE, Constants.MAX_MINIMAP_SIZE, 1f, skin);
        minimapSize.setName("minimapSize");
        minimapSize.setWidth(sliderWidth);
        minimapSize.setValue(settings.program.minimap.size);

        // PREFERRED DISTANCE UNITS
        OwnLabel distUnitsLabel = new OwnLabel(I18n.msg("gui.ui.distance.units"), skin, "default");
        distUnitsLabel.setWidth(labelWidth);
        DistanceUnits[] dus = DistanceUnits.values();
        ComboBoxBean[] distUnits = new ComboBoxBean[dus.length];
        for (int idu = 0; idu < dus.length; idu++) {
            DistanceUnits du = dus[idu];
            distUnits[idu] = new ComboBoxBean(I18n.msg("gui.ui.distance.units." + du.name().toLowerCase(Locale.ROOT)), du.ordinal());
        }
        distUnitsSelect = new OwnSelectBox<>(skin);
        distUnitsSelect.setItems(distUnits);
        distUnitsSelect.setWidth(textWidth * 3f);
        distUnitsSelect.setSelectedIndex(settings.program.ui.distanceUnits.ordinal());

        // MODE CHANGE POP-UP CHECKBOX
        OwnLabel modeChangeInfoLabel = new OwnLabel(I18n.msg("gui.ui.modechangeinfo"), skin);
        modeChangeInfo = new OwnCheckBox("", skin);
        modeChangeInfo.setChecked(settings.program.ui.modeChangeInfo);

        // LABELS
        labels.addAll(langLabel, themeLabel);

        // Add to table
        ui.add(langLabel).left().padRight(pad20).padBottom(pad10);
        ui.add(lang).colspan(2).left().padBottom(pad10).row();
        ui.add(themeLabel).left().padRight(pad20).padBottom(pad10);
        ui.add(theme).colspan(2).left().padBottom(pad10).row();
        ui.add(uiScalelabel).left().padRight(pad20).padBottom(pad10);
        ui.add(uiScale).left().padRight(pad5).padBottom(pad10);
        ui.add(applyUiScale).left().padBottom(pad10).row();
        ui.add(minimapSizeLabel).left().padRight(pad5).padBottom(pad10);
        ui.add(minimapSize).colspan(2).left().padRight(pad5).padBottom(pad10).row();
        ui.add(distUnitsLabel).left().padRight(pad5).padBottom(pad10);
        ui.add(distUnitsSelect).colspan(2).left().padRight(pad5).padBottom(pad10).row();
        ui.add(modeChangeInfoLabel).left().padRight(pad5).padBottom(pad10);
        ui.add(modeChangeInfo).colspan(2).left().padRight(pad5).padBottom(pad10).row();


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
        ch.add(crosshairFocusLabel).left().padRight(pad20).padBottom(pad5);
        ch.add(crosshairFocus).left().padBottom(pad5).row();
        ch.add(crosshairClosestLabel).left().padRight(pad20).padBottom(pad5);
        ch.add(crosshairClosest).left().padBottom(pad5).row();
        ch.add(crosshairHomeLabel).left().padRight(pad20).padBottom(pad5);
        ch.add(crosshairHome).left().padBottom(pad5).row();

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
        pointerGuidesCbGroup.space(pad10);
        pointerGuidesCbGroup.addActor(pointerGuides);
        pointerGuidesCbGroup.addActor(guidesTooltip);

        // GUIDES COLOR
        OwnLabel pointerGuidesColorLabel = new OwnLabel(I18n.msg("gui.ui.pointer.guides.color"), skin);
        float colorPickerSize = 32f;
        pointerGuidesColor = new ColorPicker(stage, skin);
        pointerGuidesColor.setPickedColor(settings.program.pointer.guides.color);

        // GUIDES WIDTH
        OwnLabel pointerGuidesWidthLabel = new OwnLabel(I18n.msg("gui.ui.pointer.guides.width"), skin, "default");
        pointerGuidesWidthLabel.setWidth(labelWidth);
        pointerGuidesWidth = new OwnSlider(Constants.MIN_POINTER_GUIDES_WIDTH, Constants.MAX_POINTER_GUIDES_WIDTH, Constants.SLIDER_STEP_TINY, skin);
        pointerGuidesWidth.setName("pointerguideswidth");
        pointerGuidesWidth.setWidth(sliderWidth);
        pointerGuidesWidth.setValue(settings.program.pointer.guides.width);

        labels.add(pointerGuidesLabel, pointerGuidesColorLabel, pointerGuidesWidthLabel);

        // Add to table
        pg.add(pointerGuidesLabel).left().padBottom(pad5).padRight(pad20);
        pg.add(pointerGuides).left().colspan(2).padBottom(pad5).row();
        pg.add(pointerCoordsLabel).left().padRight(pad20).padBottom(pad10);
        pg.add(pointerCoords).colspan(2).left().padRight(pad5).padBottom(pad10).row();
        pg.add(guidesTooltip).left().colspan(2).padBottom(pad5).row();
        pg.add(pointerGuidesColorLabel).left().padBottom(pad5).padRight(pad20);
        pg.add(pointerGuidesColor).left().size(colorPickerSize).padBottom(pad5).row();
        pg.add(pointerGuidesWidthLabel).left().padBottom(pad5).padRight(pad20);
        pg.add(pointerGuidesWidth).left().padBottom(pad5).padRight(pad20);

        /* RECURSIVE GRID */
        OwnLabel titleRecgrid = new OwnLabel(I18n.msg("gui.ui.recursivegrid"), skin, "header");
        Table rg = new Table();

        // ORIGIN
        OwnLabel originLabel = new OwnLabel(I18n.msg("gui.ui.recursivegrid.origin"), skin);
        originLabel.setWidth(labelWidth);
        String[] origins = new String[] { I18n.msg("gui.ui.recursivegrid.origin.refsys"), I18n.msg("gui.ui.recursivegrid.origin.focus") };
        recGridOrigin = new OwnSelectBox<>(skin);
        recGridOrigin.setWidth(textWidth * 3f);
        recGridOrigin.setItems(origins);
        recGridOrigin.setSelectedIndex(settings.program.recursiveGrid.origin.ordinal());

        // PROJECTION LINES
        OwnLabel recGridProjectionLinesLabel = new OwnLabel(I18n.msg("gui.ui.recursivegrid.projlines"), skin);
        recGridProjectionLines = new OwnCheckBox("", skin);
        recGridProjectionLines.setName("origin projection lines cb");
        recGridProjectionLines.setChecked(settings.program.recursiveGrid.projectionLines);

        // Add to table
        rg.add(originLabel).left().padBottom(pad5).padRight(pad20);
        rg.add(recGridOrigin).left().padBottom(pad5).row();
        rg.add(recGridProjectionLinesLabel).left().padBottom(pad5).padRight(pad20);
        rg.add(recGridProjectionLines).left().padBottom(pad5);

        // Add to content
        contentUI.add(titleUI).left().padBottom(pad10).row();
        contentUI.add(ui).left().padBottom(pad20).row();
        contentUI.add(titleCrosshair).left().padBottom(pad10).row();
        contentUI.add(ch).left().padBottom(pad20).row();
        contentUI.add(titleGuides).left().padBottom(pad10).row();
        contentUI.add(pg).left().padBottom(pad20).row();
        contentUI.add(titleRecgrid).left().padBottom(pad10).row();
        contentUI.add(rg).left();


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
        numThreads.setWidth(textWidth * 3f);
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
        multiThread.add(multithreadLabel).left().padRight(pad20).padBottom(pad5);
        multiThread.add(multithreadCb).left().padBottom(pad5).row();
        multiThread.add(numThreadsLabel).left().padRight(pad20).padBottom(pad5);
        multiThread.add(numThreads).left().padBottom(pad5).row();
        final Cell<Actor> noticeMultiThreadCell = multiThread.add((Actor) null);
        noticeMultiThreadCell.colspan(2).left();

        multithreadCb.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (noticeMultiThreadCell.getActor() == null) {
                    String nextInfoStr = I18n.msg("gui.ui.info") + '\n';
                    int lines = GlobalResources.countOccurrences(nextInfoStr, '\n');
                    TextArea nextTimeInfo = new OwnTextArea(nextInfoStr, skin, "info");
                    nextTimeInfo.setDisabled(true);
                    nextTimeInfo.setPrefRows(lines + 1);
                    nextTimeInfo.setWidth(taWidth);
                    nextTimeInfo.clearListeners();
                    noticeMultiThreadCell.setActor(nextTimeInfo);
                }
                return true;
            }
            return false;
        });

        // Add to content
        contentPerformance.add(titleMultiThread).left().padBottom(pad10).row();
        contentPerformance.add(multiThread).left().padBottom(pad20).row();

        // DRAW DISTANCE
        OwnLabel titleLod = new OwnLabel(I18n.msg("gui.lod"), skin, "header");

        Table lod = new Table(skin);

        // Smooth transitions
        OwnLabel lodFadeLabel = new OwnLabel(I18n.msg("gui.lod.fade"), skin);
        lodFadeCb = new OwnCheckBox("", skin);
        lodFadeCb.setChecked(settings.scene.octree.fade);

        // Draw distance
        OwnLabel ddLabel = new OwnLabel(I18n.msg("gui.lod.thresholds"), skin);
        lodTransitions = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 0.1f, Constants.MIN_LOD_TRANS_ANGLE_DEG, Constants.MAX_LOD_TRANS_ANGLE_DEG, false, skin);
        lodTransitions.setDisplayValueMapped(true);
        lodTransitions.setWidth(sliderWidth);
        lodTransitions.setMappedValue(settings.scene.octree.threshold[0] * MathUtilsd.radDeg);

        OwnImageButton lodTooltip = new OwnImageButton(skin, "tooltip");
        lodTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.lod.thresholds.info"), skin));

        // LABELS
        labels.addAll(numThreadsLabel, ddLabel, lodFadeLabel);

        // Add to table
        lod.add(lodFadeLabel).left().padRight(pad20).padBottom(pad5);
        lod.add(lodFadeCb).colspan(2).left().padBottom(pad5).row();
        lod.add(ddLabel).left().padRight(pad20).padBottom(pad5);
        lod.add(lodTransitions).left().padRight(pad10).padBottom(pad5);
        lod.add(lodTooltip).left().padBottom(pad5);

        // Add to content
        contentPerformance.add(titleLod).left().padBottom(pad10).row();
        contentPerformance.add(lod).left();

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
        controls.add(new OwnLabel(I18n.msg("gui.keymappings.action"), skin, "header")).left();
        controls.add(new OwnLabel(I18n.msg("gui.keymappings.keys"), skin, "header")).left().row();

        controls.add(new OwnLabel(I18n.msg("action.forward"), skin)).left().padRight(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.UP).toUpperCase(), skin, "mono-pink")).left().row();
        controls.add(new OwnLabel(I18n.msg("action.backward"), skin)).left().padRight(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.DOWN).toUpperCase(), skin, "mono-pink")).left().row();
        controls.add(new OwnLabel(I18n.msg("action.left"), skin)).left().padRight(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.LEFT).toUpperCase(), skin, "mono-pink")).left().row();
        controls.add(new OwnLabel(I18n.msg("action.right"), skin)).left().padRight(pad10);
        controls.add(new OwnLabel(GSKeys.toString(Keys.RIGHT).toUpperCase(), skin, "mono-pink")).left().row();

        // Controls
        for (String[] action : data) {
            HorizontalGroup keysGroup = new HorizontalGroup();
            keysGroup.space(pad5);
            for (int j = 1; j < action.length; j++) {
                String[] keys = action[j].split("\\+");
                for (int k = 0; k < keys.length; k++) {
                    keysGroup.addActor(new OwnLabel(keys[k].trim().replace('_', '-'), skin, "mono-pink"));
                    if (k < keys.length - 1)
                        keysGroup.addActor(new OwnLabel("+", skin));
                }
                if (j < action.length - 1)
                    keysGroup.addActor(new OwnLabel("/", skin));
            }
            controls.add(new OwnLabel(action[0], skin)).left().padRight(pad10);
            controls.add(keysGroup).left().row();
        }

        OwnScrollPane controlsScroll = new OwnScrollPane(controls, skin, "minimalist-nobg");
        controlsScroll.setWidth(controlsScrollWidth);
        controlsScroll.setHeight(controlsScrollHeight);
        controlsScroll.setScrollingDisabled(true, false);
        controlsScroll.setSmoothScrolling(true);
        controlsScroll.setFadeScrollBars(false);
        scrolls.add(controlsScroll);

        // Add to content
        contentControls.add(titleController).colspan(2).left().padBottom(pad10).row();
        contentControls.add(detectedLabel).left().padBottom(pad10).padRight(pad10);
        contentControls.add(controllersTable).left().padBottom(pad10).row();
        contentControls.add(mappingsLabel).left().padBottom(pad10).padRight(pad10);
        contentControls.add(gamepadMappings).left().padBottom(pad10).row();
        contentControls.add(invertXLabel).left().padBottom(pad10).padRight(pad10);
        contentControls.add(invertX).left().padBottom(pad10).row();
        contentControls.add(invertYLabel).left().padBottom(pad10).padRight(pad10);
        contentControls.add(invertY).left().padBottom(pad10).row();
        contentControls.add(titleKeybindings).colspan(2).left().padBottom(pad10).row();
        contentControls.add(controlsScroll).colspan(2).left();

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
        screenshotsLocationLabel.pack();
        screenshotsLocation = new OwnTextButton(settings.screenshot.location, skin);
        screenshotsLocation.pad(pad5);
        screenshotsLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.screenshots.directory.choose"), skin, stage, Paths.get(settings.screenshot.location), FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setShowHidden(settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> settings.program.fileChooser.showHidden = showHidden);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        // do stuff with result
                        screenshotsLocation.setText(result.toString());
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
        final OwnLabel xLabel = new OwnLabel("x", skin);
        IValidator screenshotsSizeValidator = new IntValidator(ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE);
        ssWidthField = new OwnTextField(Integer.toString(MathUtils.clamp(settings.screenshot.resolution[0], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)), skin, screenshotsSizeValidator);
        ssWidthField.setWidth(textWidth);
        ssWidthField.setDisabled(settings.screenshot.isSimpleMode());
        ssHeightField = new OwnTextField(Integer.toString(MathUtils.clamp(settings.screenshot.resolution[1], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)), skin, screenshotsSizeValidator);
        ssHeightField.setWidth(textWidth);
        ssHeightField.setDisabled(settings.screenshot.isSimpleMode());
        HorizontalGroup ssSizeGroup = new HorizontalGroup();
        ssSizeGroup.space(pad10);
        ssSizeGroup.addActor(ssWidthField);
        ssSizeGroup.addActor(xLabel);
        ssSizeGroup.addActor(ssHeightField);

        // Mode
        OwnLabel ssModeLabel = new OwnLabel(I18n.msg("gui.screenshots.mode"), skin);
        ComboBoxBean[] screenshotModes = new ComboBoxBean[] { new ComboBoxBean(I18n.msg("gui.screenshots.mode.simple"), 0), new ComboBoxBean(I18n.msg("gui.screenshots.mode.redraw"), 1) };
        screenshotMode = new OwnSelectBox<>(skin);
        screenshotMode.setItems(screenshotModes);
        screenshotMode.setWidth(textWidth * 3f);
        screenshotMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Simple
                // Redraw
                enableComponents(screenshotMode.getSelected().value != 0, ssWidthField, ssHeightField, screenshotsSizeLabel, xLabel);
                return true;
            }
            return false;
        });
        screenshotMode.setSelected(screenshotModes[settings.screenshot.mode.ordinal()]);
        screenshotMode.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        OwnImageButton screenshotsModeTooltip = new OwnImageButton(skin, "tooltip");
        screenshotsModeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        HorizontalGroup ssModeGroup = new HorizontalGroup();
        ssModeGroup.space(pad5);
        ssModeGroup.addActor(screenshotMode);
        ssModeGroup.addActor(screenshotsModeTooltip);

        // LABELS
        labels.addAll(screenshotsLocationLabel, ssModeLabel, screenshotsSizeLabel);

        // Add to table
        screenshots.add(screenshotsInfo).colspan(2).left().padBottom(pad5).row();
        screenshots.add(screenshotsLocationLabel).left().padRight(pad20).padBottom(pad5);
        screenshots.add(screenshotsLocation).left().expandX().padBottom(pad5).row();
        screenshots.add(ssModeLabel).left().padRight(pad20).padBottom(pad5);
        screenshots.add(ssModeGroup).left().expandX().padBottom(pad5).row();
        screenshots.add(screenshotsSizeLabel).left().padRight(pad20).padBottom(pad5);
        screenshots.add(ssSizeGroup).left().expandX().padBottom(pad5).row();

        // Add to content
        contentScreenshots.add(titleScreenshots).left().padBottom(pad10).row();
        contentScreenshots.add(screenshots).left();

        /*
         * ==== FRAME OUTPUT ====
         */
        final Table contentFrames = new Table(skin);
        contentFrames.setWidth(contentWidth);
        contentFrames.align(Align.top | Align.left);

        // FRAME OUTPUT CONFIG
        OwnLabel titleFrameoutput = new OwnLabel(I18n.msg("gui.frameoutput"), skin, "header");

        Table frameoutput = new Table(skin);

        // Info
        String foinfostr = I18n.msg("gui.frameoutput.info") + '\n';
        ssLines = GlobalResources.countOccurrences(foinfostr, '\n');
        TextArea frameoutputInfo = new OwnTextArea(foinfostr, skin, "info");
        frameoutputInfo.setDisabled(true);
        frameoutputInfo.setPrefRows(ssLines + 1);
        frameoutputInfo.setWidth(taWidth);
        frameoutputInfo.clearListeners();

        // Save location
        OwnLabel frameoutputLocationLabel = new OwnLabel(I18n.msg("gui.frameoutput.location"), skin);
        frameOutputLocation = new OwnTextButton(settings.frame.location, skin);
        frameOutputLocation.pad(pad5);
        frameOutputLocation.addListener(event -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = new FileChooser(I18n.msg("gui.frameoutput.directory.choose"), skin, stage, Paths.get(settings.frame.location), FileChooser.FileChooserTarget.DIRECTORIES);
                fc.setShowHidden(settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> settings.program.fileChooser.showHidden = showHidden);
                fc.setResultListener((success, result) -> {
                    if (success) {
                        // do stuff with result
                        frameOutputLocation.setText(result.toString());
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
        frameOutputPrefix.setWidth(textWidth * 3f);

        // FPS
        OwnLabel fpsLabel = new OwnLabel(I18n.msg("gui.target.fps"), skin);
        frameOutputFps = new OwnTextField(nf3.format(settings.frame.targetFps), skin, new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS));
        frameOutputFps.setWidth(textWidth * 3f);

        // Size
        final OwnLabel frameoutputSizeLabel = new OwnLabel(I18n.msg("gui.frameoutput.size"), skin);
        frameoutputSizeLabel.setDisabled(settings.frame.isSimpleMode());
        final OwnLabel xLabelfo = new OwnLabel("x", skin);
        IValidator frameoutputSizeValidator = new IntValidator(ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE);
        foWidthField = new OwnTextField(Integer.toString(MathUtils.clamp(settings.frame.resolution[0], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)), skin, frameoutputSizeValidator);
        foWidthField.setWidth(textWidth);
        foWidthField.setDisabled(settings.frame.isSimpleMode());
        foHeightField = new OwnTextField(Integer.toString(MathUtils.clamp(settings.frame.resolution[1], ScreenshotSettings.MIN_SCREENSHOT_SIZE, ScreenshotSettings.MAX_SCREENSHOT_SIZE)), skin, frameoutputSizeValidator);
        foHeightField.setWidth(textWidth);
        foHeightField.setDisabled(settings.frame.isSimpleMode());
        HorizontalGroup foSizeGroup = new HorizontalGroup();
        foSizeGroup.space(pad10);
        foSizeGroup.addActor(foWidthField);
        foSizeGroup.addActor(xLabelfo);
        foSizeGroup.addActor(foHeightField);

        // Mode
        OwnLabel fomodeLabel = new OwnLabel(I18n.msg("gui.screenshots.mode"), skin);
        ComboBoxBean[] frameoutputModes = new ComboBoxBean[] { new ComboBoxBean(I18n.msg("gui.screenshots.mode.simple"), 0), new ComboBoxBean(I18n.msg("gui.screenshots.mode.redraw"), 1) };
        frameOutputMode = new OwnSelectBox<>(skin);
        frameOutputMode.setItems(frameoutputModes);
        frameOutputMode.setWidth(textWidth * 3f);
        frameOutputMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Simple
                // Redraw
                enableComponents(frameOutputMode.getSelected().value != 0, foWidthField, foHeightField, frameoutputSizeLabel, xLabelfo);
                return true;
            }
            return false;
        });
        frameOutputMode.setSelected(frameoutputModes[settings.frame.mode.ordinal()]);
        frameOutputMode.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        OwnImageButton frameoutputModeTooltip = new OwnImageButton(skin, "tooltip");
        frameoutputModeTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.screenshotmode"), skin));

        HorizontalGroup foModeGroup = new HorizontalGroup();
        foModeGroup.space(pad5);
        foModeGroup.addActor(frameOutputMode);
        foModeGroup.addActor(frameoutputModeTooltip);

        // Counter
        OwnLabel counterLabel = new OwnLabel(I18n.msg("gui.frameoutput.sequence"), skin);
        HorizontalGroup counterGroup = new HorizontalGroup();
        counterGroup.space(pad5);
        frameSequenceNumber = new OwnLabel(Integer.toString(ImageRenderer.getSequenceNumber()), skin);
        frameSequenceNumber.setWidth(textWidth * 3f);
        OwnTextButton resetCounter = new OwnTextButton(I18n.msg("gui.frameoutput.sequence.reset"), skin);
        resetCounter.pad(pad10);
        resetCounter.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                ImageRenderer.resetSequenceNumber();
                frameSequenceNumber.setText("0");
            }
            return false;
        });

        counterGroup.addActor(frameSequenceNumber);

        // LABELS
        labels.addAll(frameoutputLocationLabel, prefixLabel, fpsLabel, fomodeLabel, frameoutputSizeLabel);

        // Add to table
        frameoutput.add(frameoutputInfo).colspan(2).left().padBottom(pad5).row();
        frameoutput.add(frameoutputLocationLabel).left().padRight(pad20).padBottom(pad5);
        frameoutput.add(frameOutputLocation).left().expandX().padBottom(pad5).row();
        frameoutput.add(prefixLabel).left().padRight(pad20).padBottom(pad5);
        frameoutput.add(frameOutputPrefix).left().padBottom(pad5).row();
        frameoutput.add(fpsLabel).left().padRight(pad20).padBottom(pad5);
        frameoutput.add(frameOutputFps).left().padBottom(pad5).row();
        frameoutput.add(fomodeLabel).left().padRight(pad20).padBottom(pad5);
        frameoutput.add(foModeGroup).left().expandX().padBottom(pad5).row();
        frameoutput.add(frameoutputSizeLabel).left().padRight(pad20).padBottom(pad5);
        frameoutput.add(foSizeGroup).left().expandX().padBottom(pad5).row();
        frameoutput.add(counterLabel).left().padRight(pad20).padBottom(pad5);
        frameoutput.add(counterGroup).left().expandX().padBottom(pad5).row();
        frameoutput.add().padRight(pad20);
        frameoutput.add(resetCounter).left();

        // Add to content
        contentFrames.add(titleFrameoutput).left().padBottom(pad10).row();
        contentFrames.add(frameoutput).left();

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
        camRecFps.setWidth(textWidth * 3f);
        OwnImageButton camrecFpsTooltip = new OwnImageButton(skin, "tooltip");
        camrecFpsTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera.targetfps"), skin));

        // Keyframe preferences
        Button keyframePrefs = new OwnTextIconButton(I18n.msg("gui.keyframes.preferences"), skin, "preferences");
        keyframePrefs.setName("keyframe preferences");
        keyframePrefs.pad(pad10);
        keyframePrefs.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.kf.editprefs"), skin));
        keyframePrefs.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                KeyframePreferencesWindow kpw = new KeyframePreferencesWindow(stage, skin);
                kpw.setAcceptRunnable(() -> {
                    if (kpw.camrecFps != null && kpw.camrecFps.isValid()) {
                        camRecFps.setText(kpw.camrecFps.getText());
                    }
                });
                kpw.show(stage);
                return true;
            }
            return false;
        });

        // Activate automatically
        OwnLabel autoCamrecLabel = new OwnLabel(I18n.msg("gui.camerarec.frameoutput"), skin);
        cbAutoCamrec = new OwnCheckBox("", skin);
        cbAutoCamrec.setChecked(settings.camrecorder.auto);
        cbAutoCamrec.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera.frameoutput"), skin));
        OwnImageButton camrecAutoTooltip = new OwnImageButton(skin, "tooltip");
        camrecAutoTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera.frameoutput"), skin));

        // LABELS
        labels.add(autoCamrecLabel);

        // Add to table
        camrec.add(camfpsLabel).left().padRight(pad20).padBottom(pad5);
        camrec.add(camRecFps).left().expandX().padBottom(pad5);
        camrec.add(camrecFpsTooltip).left().padLeft(pad5).padBottom(pad5).row();
        camrec.add(autoCamrecLabel).left().padRight(pad20).padBottom(pad5);
        camrec.add(cbAutoCamrec).left().padBottom(pad5);
        camrec.add(camrecAutoTooltip).left().padLeft(pad5).padBottom(pad5).row();
        camrec.add(keyframePrefs).colspan(3).left().padTop(pad20 * 2f).row();

        // Add to content
        contentCamera.add(titleCamrec).left().padBottom(pad10).row();
        contentCamera.add(camrec).left();

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
        cmResolution.setWidth(textWidth * 3f);
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
        cubemap.add(cmInfo).colspan(2).left().padBottom(pad5).row();
        cubemap.add(cmResolutionLabel).left().padRight(pad20).padBottom(pad5);
        cubemap.add(cmResolution).left().expandX().padBottom(pad5).row();

        // Add to content
        content360.add(titleCubemap).left().padBottom(pad10).row();
        content360.add(cubemap).left();

        /*
         * ==== PLANETARIUM ====
         */
        final Table contentPlanetarium = new Table(skin);
        contentPlanetarium.setWidth(contentWidth);
        contentPlanetarium.align(Align.top | Align.left);

        // CUBEMAP
        OwnLabel titlePlanetarium = new OwnLabel(I18n.msg("gui.planetarium"), skin, "header");
        Table planetarium = new Table(skin);

        // Aperture
        Label apertureLabel = new OwnLabel(I18n.msg("gui.planetarium.aperture"), skin);
        plAperture = new OwnTextField(Float.toString(settings.program.modeCubemap.planetarium.aperture), skin, new FloatValidator(30, 360));
        plAperture.setWidth(textWidth * 3f);

        // Skew angle
        Label plAngleLabel = new OwnLabel(I18n.msg("gui.planetarium.angle"), skin);
        plAngle = new OwnTextField(Float.toString(settings.program.modeCubemap.planetarium.angle), skin, new FloatValidator(-180, 180));
        plAngle.setWidth(textWidth * 3f);

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
        plResolution.setWidth(textWidth * 3f);
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
        labels.add(plResolutionLabel);

        // Add to table
        planetarium.add(apertureLabel).left().padRight(pad20).padBottom(pad10 * 3f);
        planetarium.add(plAperture).left().expandX().padBottom(pad10 * 3f).row();
        planetarium.add(plAngleLabel).left().padRight(pad20).padBottom(pad10 * 3f);
        planetarium.add(plAngle).left().expandX().padBottom(pad10 * 3f).row();
        planetarium.add(plInfo).colspan(2).left().padBottom(pad5).row();
        planetarium.add(plResolutionLabel).left().padRight(pad20).padBottom(pad5);
        planetarium.add(plResolution).left().expandX().padBottom(pad5).row();

        // Add to content
        contentPlanetarium.add(titlePlanetarium).left().padBottom(pad10).row();
        contentPlanetarium.add(planetarium).left();


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
        dataDownload.pad(pad20, pad20 * 2f, pad20, pad20 * 2f);
        dataDownload.setHeight(buttonHeight);
        dataDownload.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (DataDescriptor.serverDataDescriptor != null || DataDescriptor.localDataDescriptor != null) {
                    DataDescriptor dd = DataDescriptor.serverDataDescriptor != null ? DataDescriptor.serverDataDescriptor : DataDescriptor.localDataDescriptor;
                    DatasetManagerWindow ddw = new DatasetManagerWindow(stage, skin, dd, false, null);
                    ddw.setModal(true);
                    ddw.show(stage);
                } else {
                    // Try again
                    FileHandle dataDescriptor = Gdx.files.absolute(SysUtils.getTempDir(settings.data.location) + "/gaiasky-data.json");
                    DownloadHelper.downloadFile(settings.program.url.dataDescriptor, dataDescriptor, Settings.settings.program.offlineMode, null, null, (digest) -> {
                        DataDescriptor dd = DataDescriptorUtils.instance().buildServerDatasets(dataDescriptor);
                        DatasetManagerWindow ddw = new DatasetManagerWindow(stage, skin, dd, false, null);
                        ddw.setModal(true);
                        ddw.show(stage);
                    }, () -> {
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

        real = new OwnCheckBox(I18n.msg("gui.gaia.real"), skin, "radio", pad5);
        real.setChecked(settings.data.realGaiaAttitude);
        nsl = new OwnCheckBox(I18n.msg("gui.gaia.nsl"), skin, "radio", pad5);
        nsl.setChecked(!settings.data.realGaiaAttitude);

        OwnLabel restart = new OwnLabel(I18n.msg("gui.restart"), skin, "default-pink");

        new ButtonGroup<>(real, nsl);

        // Add to table
        attitude.add(restart).left().padBottom(pad10).row();
        attitude.add(nsl).left().padBottom(pad5).row();
        attitude.add(real).left().padBottom(pad5).row();
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

        // Add to content
        contentDataTable.add(titleGeneralData).left().padBottom(pad10).row();
        contentDataTable.add(highAccuracyPositionsLabel).left().padBottom(pad20);
        contentDataTable.add(highAccuracyPositions).left().padRight(pad20).padBottom(pad20);
        contentDataTable.add(highAccTooltip).left().padBottom(pad20).row();
        if (!welcomeScreen) {
            contentDataTable.add(titleData).left().colspan(3).padBottom(pad10).row();
            contentDataTable.add(dataSourceInfo).left().colspan(3).padBottom(pad5).row();
            contentDataTable.add(dataDownload).left().colspan(3).padBottom(pad20).row();
        }
        contentDataTable.add(titleAttitude).left().padBottom(pad10).row();
        contentDataTable.add(attitude).left();


        /*
         * ==== SYSTEM ====
         */
        final Table contentSystem = new Table(skin);
        contentSystem.setWidth(contentWidth);
        contentSystem.align(Align.top | Align.left);

        // STATS
        OwnLabel titleStats = new OwnLabel(I18n.msg("gui.system.reporting"), skin, "header");
        Table stats = new Table(skin);

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

        // EXIT CONFIRMATION
        OwnLabel exitConfirmationLabel = new OwnLabel(I18n.msg("gui.quit.confirmation"), skin);
        exitConfirmation = new OwnCheckBox("", skin);
        exitConfirmation.setChecked(settings.program.exitConfirmation);

        labels.addAll(debugInfoLabel, exitConfirmationLabel);

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
        reloadDefaults.pad(0, pad10, 0, pad10);
        reloadDefaults.setHeight(buttonHeight);

        OwnLabel warningLabel = new OwnLabel(I18n.msg("gui.system.reloaddefaults.warn"), skin, "default-red");

        // Add to table
        stats.add(debugInfoLabel).left().padBottom(pad5);
        stats.add(debugInfo).left().padBottom(pad5).row();
        stats.add(exitConfirmationLabel).left().padBottom(pad10);
        stats.add(exitConfirmation).left().padBottom(pad10).row();
        stats.add(warningLabel).left().colspan(2).padBottom(pad20).row();
        stats.add(reloadDefaults).left().colspan(2);

        // Add to content
        contentSystem.add(titleStats).left().padBottom(pad10).row();
        contentSystem.add(stats).left();

        /* COMPUTE LABEL WIDTH */
        float maxLabelWidth = 0;
        for (OwnLabel l : labels) {
            l.pack();
            if (l.getWidth() > maxLabelWidth)
                maxLabelWidth = l.getWidth();
        }
        maxLabelWidth = Math.max(textWidth * 2, maxLabelWidth);
        for (OwnLabel l : labels)
            l.setWidth(maxLabelWidth);

        /* ADD ALL CONTENT */
        addTabContent(contentGraphics);
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
        content.add(tabStack).left().padLeft(pad15).expand().fill();

        // Set tab listeners.
        setUpTabListeners();
    }

    @Override
    public GenericDialog show(Stage stage, Action action) {
        GenericDialog result = super.show(stage, action);
        updateBackupValues();
        return result;
    }

    private void updateBackupValues() {
        bloomBak = settings.postprocess.bloom.intensity;
        unsharpMaskBak = settings.postprocess.unsharpMask.factor;
        lensflareBak = settings.postprocess.lensFlare.active;
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
            if (selectedFile == null && settings.controls.gamepad.mappingsFile.endsWith(path.getFileName().toString())) {
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
            if (settings.controls.gamepad.isControllerBlacklisted(c.getName())) {
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
            table.add(cn).left().padBottom(i == controllerNames.size - 1 ? 0f : pad10).padRight(pad20);
            if (controllerName != null && !settings.controls.gamepad.isControllerBlacklisted(controllerName)) {
                OwnTextButton config = new OwnTextButton(I18n.msg("gui.controller.configure"), skin);
                config.pad(pad5, pad10, pad5, pad10);
                config.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        // Get currently selected mappings
                        GamepadMappings cm = new GamepadMappings(controllerName, Path.of(gamepadMappings.getSelected().file));
                        GamepadConfigWindow ccw = new GamepadConfigWindow(controllerName, cm, stage, skin);
                        ccw.setAcceptRunnable(() -> {
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
                table.add(config).left().padBottom(i == controllerNames.size - 1 ? 0f : pad10).row();
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
    protected void cancel() {
        revertLivePreferences();
        unsubscribe();
    }

    @Override
    public void dispose() {
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
            Logger.getLogger(this.getClass()).error(e, "Error copying default preferences file to user folder: " + userFolderConfFile);
        }

    }

    private void saveCurrentPreferences() {
        // Add all properties to settings.instance

        final boolean reloadFullScreenMode = fullScreen.isChecked() != settings.graphics.fullScreen.active;
        final var selected = fullScreenResolutions.getSelected();
        final boolean reloadScreenMode = reloadFullScreenMode || (settings.graphics.fullScreen.active && (settings.graphics.fullScreen.resolution[0] != selected.width || settings.graphics.fullScreen.resolution[1] != selected.height || settings.graphics.fullScreen.refreshRate != selected.refreshRate || settings.graphics.fullScreen.bitDepth != selected.bitsPerPixel)) || (!settings.graphics.fullScreen.active && (settings.graphics.resolution[0] != Integer.parseInt(widthField.getText()))
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
        if (settings.graphics.quality.ordinal() != bean.value) {
            settings.graphics.quality = GraphicsQuality.values()[bean.value];
            EventManager.publish(Event.GRAPHICS_QUALITY_UPDATED, this, settings.graphics.quality);
        }

        bean = aa.getSelected();
        Antialias newAntiAlias = settings.postprocess.getAntialias(bean.value);
        if (settings.postprocess.antialias != newAntiAlias) {
            settings.postprocess.antialias = settings.postprocess.getAntialias(bean.value);
            EventManager.publish(Event.ANTIALIASING_CMD, this, settings.postprocess.antialias);
        }

        settings.graphics.vsync = vsync.isChecked();
        try {
            // Windows backend crashes for some reason
            Gdx.graphics.setVSync(settings.graphics.vsync);
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }

        // FPS limiter
        if (maxFps.isChecked()) {
            EventManager.publish(Event.LIMIT_FPS_CMD, this, Parser.parseDouble(maxFpsInput.getText()));
        } else {
            EventManager.publish(Event.LIMIT_FPS_CMD, this, 0.0);
        }

        boolean restartDialog = false;
        if (!settings.runtime.openVr) {
            // Point cloud renderer
            PointCloudMode newPointCloudMode = PointCloudMode.values()[pointCloudRenderer.getSelected().value];
            restartDialog = newPointCloudMode != settings.scene.renderer.pointCloud;
            settings.scene.renderer.pointCloud = newPointCloudMode;
        }
        restartDialog = restartDialog || Settings.settings.data.realGaiaAttitude != real.isChecked();

        // Line renderer
        boolean reloadLineRenderer = settings.scene.renderer.line != LineMode.values()[lineRenderer.getSelected().value];
        bean = lineRenderer.getSelected();
        settings.scene.renderer.line = LineMode.values()[bean.value];

        // Elevation representation
        ElevationType newType = elevationSb.getSelected().type;
        if (SysUtils.isMac() && newType.isTessellation()) {
            newType = ElevationType.NONE;
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.elevation.macos"));
            logger.info(I18n.msg("gui.elevation.macos"));
        }
        boolean reloadElevation = newType != settings.scene.renderer.elevation.type;
        if (reloadElevation) {
            EventManager.publish(Event.ELEVATION_TYPE_CMD, this, newType);
        }

        // Tess quality
        EventManager.publish(Event.TESSELLATION_QUALITY_CMD, this, tessQuality.getValue());

        // Shadow mapping
        settings.scene.renderer.shadow.active = shadowsCb.isChecked();
        int newShadowResolution = Integer.parseInt(smResolution.getText());
        int newShadowNumber = nShadows.getSelected().value;
        final boolean reloadShadows = shadowsCb.isChecked() && (settings.scene.renderer.shadow.resolution != newShadowResolution || settings.scene.renderer.shadow.number != newShadowNumber);

        // Fade time
        settings.scene.fadeMs = MathUtils.clamp(fadeTimeField.getLongValue(settings.scene.fadeMs), Constants.MIN_FADE_TIME_MS, Constants.MAX_FADE_TIME_MS);

        // Dynamic resolution
        settings.graphics.dynamicResolution = !settings.runtime.openVr && dynamicResolution.isChecked();
        if (!settings.graphics.dynamicResolution) {
            GaiaSky.postRunnable(() -> GaiaSky.instance.resetDynamicResolution());
        }

        // Motion blur
        if (motionBlur != null) {
            resetRenderFlags = settings.postprocess.motionBlur.active != motionBlur.isChecked();
            GaiaSky.postRunnable(() -> EventManager.publish(Event.MOTION_BLUR_CMD, this, motionBlur.isChecked()));
        }

        // SSR
        if (ssr != null) {
            resetRenderFlags = resetRenderFlags || settings.postprocess.ssr.active != ssr.isChecked();
            GaiaSky.postRunnable(() -> EventManager.publish(Event.SSR_CMD, ssr, ssr.isChecked()));
        }

        // Back-buffer scale
        if (backBufferScale != null && !backBufferScale.isDisabled()) {
            GaiaSky.postRunnable(() -> EventManager.publish(Event.BACKBUFFER_SCALE_CMD, backBufferScale, backBufferScale.getValue()));
        }

        // Interface
        LangComboBoxBean languageBean = lang.getSelected();
        StrComboBoxBean newTheme = theme.getSelected();
        // UI scale
        float factor = uiScale.getMappedValue();
        EventManager.publish(Event.UI_SCALE_CMD, this, factor);

        boolean reloadLang = !languageBean.locale.toLanguageTag().equals(settings.program.getLocale());
        boolean reloadUI = reloadLang || !settings.program.ui.theme.equals(newTheme.value) || settings.program.minimap.size != minimapSize.getValue();
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
        settings.program.recursiveGrid.origin = values()[recGridOrigin.getSelectedIndex()];
        settings.program.recursiveGrid.projectionLines = recGridProjectionLines.isChecked();

        // Minimap size
        settings.program.minimap.size = minimapSize.getValue();

        // Distance units
        settings.program.ui.distanceUnits = DistanceUnits.values()[distUnitsSelect.getSelectedIndex()];

        // Mode change info
        settings.program.ui.modeChangeInfo = modeChangeInfo.isChecked();

        // Performance
        bean = numThreads.getSelected();
        settings.performance.numberThreads = bean.value;
        settings.performance.multithreading = multithreadCb.isChecked();

        settings.scene.octree.fade = lodFadeCb.isChecked();
        settings.scene.octree.threshold[0] = MathUtilsd.lint(lodTransitions.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_LOD_TRANS_ANGLE_DEG, Constants.MAX_LOD_TRANS_ANGLE_DEG) * (float) MathUtilsd.degRad;
        // Here we use a 0.4 rad between the thresholds
        settings.scene.octree.threshold[1] = settings.scene.octree.fade ? settings.scene.octree.threshold[0] + 0.4f : settings.scene.octree.threshold[0];

        // Data
        boolean highAccuracy = settings.data.highAccuracy;
        settings.data.highAccuracy = highAccuracyPositions.isChecked();

        if (highAccuracy != settings.data.highAccuracy) {
            // Event
            EventManager.publish(Event.HIGH_ACCURACY_CMD, this, settings.data.highAccuracy);
        }

        // Screenshots
        File ssFile = new File(screenshotsLocation.getText().toString());
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

        // Frame output
        File foFile = new File(frameOutputLocation.getText().toString());
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

        // Camera recording
        EventManager.publish(Event.CAMRECORDER_FPS_CMD, this, Parser.parseDouble(camRecFps.getText()));
        settings.camrecorder.auto = cbAutoCamrec.isChecked();

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

        // Index of refraction
        EventManager.publish(Event.INDEXOFREFRACTION_CMD, this, celestialSphereIndexOfRefraction.getValue());

        // Controllers
        if (gamepadMappings.getSelected() != null) {
            String mappingsFile = gamepadMappings.getSelected().file;
            if (!mappingsFile.equals(settings.controls.gamepad.mappingsFile)) {
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
        EventManager.publish(Event.LENS_FLARE_CMD, this, lensflareBak);
        EventManager.publish(Event.LIGHT_GLOW_CMD, this, lightGlowBak);
        EventManager.publish(Event.BLOOM_CMD, this, bloomBak);
        EventManager.publish(Event.UNSHARP_MASK_CMD, this, unsharpMaskBak);
        EventManager.publish(Event.EXPOSURE_CMD, this, exposureBak);
        EventManager.publish(Event.TONEMAPPING_TYPE_CMD, this, toneMappingBak);
        EventManager.publish(Event.SHOW_DEBUG_CMD, this, debugInfoBak);
        EventManager.publish(Event.REPROJECTION_CMD, this, reprojectionBak != ReprojectionMode.DISABLED, reprojectionBak);
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

    private void selectFullscreen(boolean fullscreen, OwnTextField widthField, OwnTextField heightField, SelectBox<DisplayMode> fullScreenResolutions, OwnLabel widthLabel, OwnLabel heightLabel) {
        if (fullscreen) {
            settings.graphics.resolution[0] = fullScreenResolutions.getSelected().width;
            settings.graphics.resolution[1] = fullScreenResolutions.getSelected().height;
        } else {
            settings.graphics.resolution[0] = Integer.parseInt(widthField.getText());
            settings.graphics.resolution[1] = Integer.parseInt(heightField.getText());
        }

        enableComponents(!fullscreen, widthField, heightField, widthLabel, heightLabel);
        enableComponents(fullscreen, fullScreenResolutions);
    }

    private int idxAa(int base, Antialias x) {
        if (x.getAACode() == -1)
            return 1;
        if (x.getAACode() == -2)
            return 2;
        if (x.getAACode() == 0)
            return 0;
        return (int) (Math.log(x.getAACode()) / Math.log(base) + 1e-10) + 2;
    }

    private int idxLang(String code, Array<LangComboBoxBean> langs) {
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
        if (key == Keys.PLUS) {
            return "+";
        }
        return GSKeys.toString(key);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case CONTROLLER_CONNECTED_INFO, CONTROLLER_DISCONNECTED_INFO -> generateGamepadsList(controllersTable);
        default -> {
        }
        }
    }

}
