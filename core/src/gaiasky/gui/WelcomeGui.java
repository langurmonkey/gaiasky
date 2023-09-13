/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.input.GuiKbdListener;
import gaiasky.util.Logger;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.gdx.loader.OwnTextureLoader;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;
import gaiasky.util.scene2d.Separator;
import gaiasky.vr.openxr.XrLoadStatus;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class WelcomeGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(WelcomeGui.class);

    private final XrLoadStatus vrStatus;
    private final boolean skipWelcome;
    private final WelcomeGuiGamepadListener gamepadListener;
    protected DatasetManagerWindow datasetManager;
    private AboutWindow aboutWindow;
    private PreferencesWindow preferencesWindow;
    private FileHandle dataDescriptor;
    private boolean downloadError = false;
    private Texture bgTex;
    private DataDescriptor serverDatasets;
    private DataDescriptor localDatasets;
    private Array<Button> buttonList;
    private int currentSelected = 0;
    private PopupNotificationsInterface popupInterface;
    private WelcomeGuiKbdListener kbdListener;

    /**
     * Creates an initial GUI
     *
     * @param skipWelcome Skips the welcome screen if possible
     * @param vrStatus    The status of VR
     */
    public WelcomeGui(final Skin skin,
                      final Graphics graphics,
                      final Float unitsPerPixel,
                      final boolean skipWelcome,
                      final XrLoadStatus vrStatus) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.lock = new Object();
        this.skipWelcome = skipWelcome;
        this.vrStatus = vrStatus;
        this.gamepadListener = new WelcomeGuiGamepadListener(Settings.settings.controls.gamepad.mappingsFile);
    }

    @Override
    public void initialize(AssetManager assetManager,
                           SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        this.stage = new Stage(vp, sb);
        var inputMultiplexer = GaiaSky.instance.inputMultiplexer;
        if (inputMultiplexer != null) {
            inputMultiplexer.addProcessor(this.stage);
        }
        this.kbdListener = new WelcomeGuiKbdListener(stage);

        popupInterface = new PopupNotificationsInterface(skin);
        popupInterface.top().right();
        popupInterface.setFillParent(true);

        if (DataDescriptorUtils.dataLocationOldVersionDatasetsCheck()) {
            var fsCheck = new DataLocationCheckWindow(I18n.msg("gui.dscheck.title"), skin, stage);
            fsCheck.setAcceptRunnable(() -> {
                // Clean old datasets in a thread in the background.
                GaiaSky.instance.getExecutorService().execute(DataDescriptorUtils::cleanDataLocationOldDatasets);
                // Continue immediately.
                continueWelcomeGui();
            });
            fsCheck.setCancelRunnable(this::continueWelcomeGui);
            fsCheck.show(stage);
        } else {
            continueWelcomeGui();
        }

    }

    private void continueWelcomeGui() {
        if (vrStatus.vrInitFailed()) {
            if (vrStatus.equals(XrLoadStatus.ERROR_NO_CONTEXT))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRConnectionExit(skin, stage));
            else if (vrStatus.equals(XrLoadStatus.ERROR_RENDERMODEL))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRDataExit(skin, stage));
        } else if (Settings.settings.program.net.slave.active || GaiaSky.instance.isHeadless()) {
            // If we are a slave or running headless, data load can start
            startLoading();
        } else {
            // Otherwise, check for updates, etc.
            clearGui();

            if (Controllers.getControllers().size > 0) {
                // Detected controllers -> schedule notification.
                Task notification = new Task() {
                    @Override
                    public void run() {
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.welcome.gamepad.notification", Controllers.getControllers().size), 10f);
                    }
                };
                Timer.schedule(notification, 2);
            }

            dataDescriptor = Gdx.files.absolute(SysUtils.getTempDir(Settings.settings.data.location) + "/gaiasky-data.json.gz");
            DownloadHelper.downloadFile(Settings.settings.program.url.dataDescriptor, dataDescriptor, Settings.settings.program.offlineMode, null, null,
                                        (digest) -> GaiaSky.postRunnable(() -> {
                                            // Data descriptor ok. Skip welcome screen only if flag and base data present
                                            if (skipWelcome && baseDataPresent()) {
                                                startLoading();
                                            } else {
                                                buildWelcomeUI();
                                            }
                                        }), () -> {
                        // Fail?
                        downloadError = true;
                        if (Settings.settings.program.offlineMode) {
                            logger.error(I18n.msg("gui.welcome.error.offlinemode"));
                        } else {
                            logger.error(I18n.msg("gui.welcome.error.nointernet"));
                        }
                        if (baseDataPresent()) {
                            // Go on all in
                            GaiaSky.postRunnable(() -> GuiUtils.addNoConnectionWindow(skin, stage, this::buildWelcomeUI));
                        } else {
                            // Error and exit
                            logger.error(I18n.msg("gui.welcome.error.nobasedata"));
                            GaiaSky.postRunnable(() -> GuiUtils.addNoConnectionExit(skin, stage));
                        }
                    }, null);

            /* CAPTURE SCROLL FOCUS */
            stage.addListener(event -> {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;

                    if (ie.getType() == Type.keyUp) {
                        if (ie.getKeyCode() == Input.Keys.ESCAPE) {
                            Gdx.app.exit();
                        } else if (ie.getKeyCode() == Input.Keys.ENTER) {
                            if (baseDataPresent()) {
                                startLoading();
                            } else {
                                addDatasetManagerWindow(serverDatasets);
                            }
                        }
                    }
                }
                return false;
            });

        }

    }

    private void buildWelcomeUI() {
        addOwnListeners();
        buttonList = new Array<>();
        serverDatasets = !downloadError ? DataDescriptorUtils.instance().buildServerDatasets(dataDescriptor) : null;
        reloadLocalDatasets();
        // Center table
        Table center = new Table(skin);
        center.setFillParent(true);
        center.center();
        if (bgTex == null) {
            bgTex = new Texture(OwnTextureLoader.Factory.loadFromFile(Gdx.files.internal("img/splash/splash.jpg"), false));
        }
        Drawable bg = new SpriteDrawable(new Sprite(bgTex));
        center.setBackground(bg);

        float pad16 = 16f;
        float pad18 = 18f;
        float pad28 = 28f;
        float pad32 = 32f;

        float buttonWidth = 440f;
        float buttonHeight = 110f;

        Table centerContent = new Table(skin);
        centerContent.center();
        centerContent.setBackground("table-bg");
        centerContent.pad(pad32);
        centerContent.padLeft(pad32 * 5f);
        centerContent.padRight(pad32 * 5f);

        Set<String> removed = removeNonExistent();
        if (!removed.isEmpty()) {
            logger.warn(I18n.msg("gui.welcome.warn.nonexistent", removed.size()));
            logger.warn(TextUtils.setToStr(removed));
        }
        int numCatalogsAvailable = numCatalogsAvailable();
        int numGaiaDRCatalogsEnabled = numGaiaDRCatalogsEnabled();
        int numStarCatalogsEnabled = numStarCatalogsEnabled();
        int numTotalCatalogsEnabled = numTotalDatasetsEnabled();
        boolean baseDataPresent = baseDataPresent();

        // Logo and title.
        Table titleGroup = new Table(skin);

        FileHandle gsIcon = Gdx.files.internal("icon/gs_icon.png");
        Texture iconTex = new Texture(gsIcon);
        iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image logo = new Image(iconTex);
        logo.setScale(1.4f);
        logo.setOrigin(Align.center);

        OwnLabel gaiaSky = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openXr), skin, "main-title");
        gaiaSky.setFontScale(1.5f);
        OwnLabel version = new OwnLabel(Settings.settings.version.version, skin, "main-title");
        version.setColor(skin.getColor("blue"));
        Table title = new Table(skin);
        title.add(gaiaSky).bottom().left().padBottom(pad16).row();
        title.add(version).bottom().left().padRight(pad16);

        titleGroup.add(logo).center().padRight(pad32 * 3f);
        titleGroup.add(new Separator(skin, "regular")).fillY().padRight(pad32);
        titleGroup.add(title);

        String textStyle = "main-title-s";

        // Start Gaia Sky button
        OwnTextIconButton startButton = new OwnTextIconButton(I18n.msg("gui.welcome.start", Settings.APPLICATION_NAME), skin, "start");
        startButton.setSpace(pad18);
        startButton.setContentAlign(Align.center);
        startButton.align(Align.center);
        startButton.setSize(buttonWidth, buttonHeight);
        startButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Check base data is enabled
                startLoading();
            }
            return true;
        });
        buttonList.add(startButton);

        Table startGroup = new Table(skin);
        OwnLabel startLabel = new OwnLabel(I18n.msg("gui.welcome.start.desc", Settings.APPLICATION_NAME), skin, textStyle);
        startGroup.add(startLabel).top().left().padBottom(pad16).row();
        if (!baseDataPresent) {
            // No basic data, can't start!
            startButton.setDisabled(true);

            OwnLabel noBaseData = new OwnLabel(I18n.msg("gui.welcome.start.nobasedata"), skin, textStyle);
            noBaseData.setColor(ColorUtils.gRedC);
            startGroup.add(noBaseData).bottom().left();
        } else if (numCatalogsAvailable > 0 && numTotalCatalogsEnabled == 0) {
            OwnLabel noCatsSelected = new OwnLabel(I18n.msg("gui.welcome.start.nocatalogs"), skin, textStyle);
            noCatsSelected.setColor(ColorUtils.gRedC);
            startGroup.add(noCatsSelected).bottom().left();
        } else if (numGaiaDRCatalogsEnabled > 1 || numStarCatalogsEnabled == 0) {
            OwnLabel tooManyDR = new OwnLabel(I18n.msg("gui.welcome.start.check"), skin, textStyle);
            tooManyDR.setColor(ColorUtils.gRedC);
            startGroup.add(tooManyDR).bottom().left();
        } else {
            OwnLabel ready = new OwnLabel(I18n.msg("gui.welcome.start.ready"), skin, textStyle);
            ready.setColor(ColorUtils.gGreenC);
            startGroup.add(ready).bottom().left();
        }

        // Dataset manager button
        OwnTextIconButton datasetManagerButton = new OwnTextIconButton(I18n.msg("gui.welcome.dsmanager"), skin, "cloud-download");
        datasetManagerButton.setSpace(pad18);
        datasetManagerButton.setContentAlign(Align.center);
        datasetManagerButton.align(Align.center);
        datasetManagerButton.setSize(buttonWidth * 0.8f, buttonHeight * 0.8f);
        datasetManagerButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                addDatasetManagerWindow(serverDatasets);
            }
            return true;
        });
        buttonList.add(datasetManagerButton);

        Table datasetManagerInfo = new Table(skin);
        OwnLabel downloadLabel = new OwnLabel(I18n.msg("gui.welcome.dsmanager.desc"), skin, textStyle);
        datasetManagerInfo.add(downloadLabel).top().left().padBottom(pad16);
        if (serverDatasets != null && serverDatasets.updatesAvailable) {
            datasetManagerInfo.row();
            OwnLabel updates = new OwnLabel(I18n.msg("gui.welcome.dsmanager.updates", serverDatasets.numUpdates), skin, textStyle);
            updates.setColor(ColorUtils.gYellowC);
            datasetManagerInfo.add(updates).bottom().left();
        } else if (!baseDataPresent) {
            datasetManagerInfo.row();
            OwnLabel getBasedata = new OwnLabel(I18n.msg("gui.welcome.dsmanager.info"), skin, textStyle);
            getBasedata.setColor(ColorUtils.gGreenC);
            datasetManagerInfo.add(getBasedata).bottom().left();
        } else {
            // Number selected
            OwnLabel numCatalogsEnabled = new OwnLabel(I18n.msg("gui.welcome.enabled", numTotalCatalogsEnabled, numCatalogsAvailable), skin, textStyle);
            numCatalogsEnabled.setColor(ColorUtils.gBlueC);
            datasetManagerInfo.row().padBottom(pad16);
            datasetManagerInfo.add(numCatalogsEnabled).left().padBottom(pad18);
        }

        // Selection problems/issues
        Table selectionInfo = new Table(skin);
        if (numCatalogsAvailable == 0) {
            // No catalog files, disable and add notice
            OwnLabel noCatalogs = new OwnLabel(I18n.msg("gui.welcome.catalogsel.nocatalogs"), skin, textStyle);
            noCatalogs.setColor(ColorUtils.aOrangeC);
            selectionInfo.add(noCatalogs);
        } else if (numGaiaDRCatalogsEnabled > 1) {
            OwnLabel tooManyDR = new OwnLabel(I18n.msg("gui.welcome.catalogsel.manydrcatalogs"), skin, textStyle);
            tooManyDR.setColor(ColorUtils.gRedC);
            selectionInfo.add(tooManyDR);
        } else if (numStarCatalogsEnabled > 1) {
            OwnLabel warn2Star = new OwnLabel(I18n.msg("gui.welcome.catalogsel.manystarcatalogs"), skin, textStyle);
            warn2Star.setColor(ColorUtils.aOrangeC);
            selectionInfo.add(warn2Star);
        } else if (numStarCatalogsEnabled == 0) {
            OwnLabel noStarCatalogs = new OwnLabel(I18n.msg("gui.welcome.catalogsel.nostarcatalogs"), skin, textStyle);
            noStarCatalogs.setColor(ColorUtils.aOrangeC);
            selectionInfo.add(noStarCatalogs);
        }

        // Exit button
        OwnTextIconButton exitButton = new OwnTextIconButton(I18n.msg("gui.exit"), skin, "quit");
        exitButton.setSpace(pad16);
        exitButton.align(Align.center);
        exitButton.setSize(buttonWidth * 0.5f, buttonHeight * 0.6f);
        exitButton.addListener(new OwnTextTooltip(I18n.msg("context.quit"), skin, 10));
        exitButton.addListener(new ClickListener() {
            public void clicked(InputEvent event,
                                float x,
                                float y) {
                GaiaSky.postRunnable(Gdx.app::exit);
            }
        });
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event,
                                Actor actor) {
                GaiaSky.postRunnable(Gdx.app::exit);
            }
        });
        buttonList.add(exitButton);

        // Title
        centerContent.add(titleGroup).center().padLeft(pad32 * 2f).padBottom(pad18 * 6f).colspan(2).row();

        // Start button
        centerContent.add(startButton).right().top().padBottom(pad18 * 10f).padRight(pad28 * 2f);
        centerContent.add(startGroup).top().left().padBottom(pad18 * 10f).row();

        // Dataset manager
        centerContent.add(datasetManagerButton).right().top().padBottom(pad32).padRight(pad28 * 2f);
        centerContent.add(datasetManagerInfo).left().top().padBottom(pad32).row();

        centerContent.add(selectionInfo).colspan(2).center().top().padBottom(pad32 * 4f).row();

        // Quit
        centerContent.add(exitButton).center().top().colspan(2);

        // Add to center
        center.add(centerContent).center();

        // Version line table
        Table topLeft = new VersionLineTable(skin);

        // Screen mode button
        Table screenMode = new Table(skin);
        screenMode.setFillParent(true);
        screenMode.top().right();
        screenMode.pad(pad16);
        OwnTextIconButton screenModeButton = new OwnTextIconButton("", skin, "screen-mode");
        screenModeButton.addListener(new OwnTextTooltip(I18n.msg("gui.fullscreen"), skin, 10));
        screenModeButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                Settings.settings.graphics.fullScreen.active = !Settings.settings.graphics.fullScreen.active;
                EventManager.publish(Event.SCREEN_MODE_CMD, screenModeButton);
                return true;
            }
            return false;
        });
        screenMode.add(screenModeButton);

        // Bottom icons
        OwnTextIconButton about = new OwnTextIconButton("", skin, "help");
        about.addListener(new OwnTextTooltip(I18n.msg("gui.help.about"), skin, 10));
        about.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (aboutWindow == null) {
                    aboutWindow = new AboutWindow(stage, skin);
                }
                if (!aboutWindow.isVisible() || !aboutWindow.hasParent()) {
                    aboutWindow.show(stage);
                }
                return true;
            }
            return false;
        });
        about.pack();

        OwnTextIconButton preferences = new OwnTextIconButton("", skin, "preferences");
        preferences.addListener(new OwnTextTooltip(I18n.msg("gui.preferences"), skin, 10));
        preferences.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (preferencesWindow == null) {
                    preferencesWindow = new PreferencesWindow(stage, skin, GaiaSky.instance.getGlobalResources(), true);
                }
                if (!preferencesWindow.isVisible() || !preferencesWindow.hasParent()) {
                    preferencesWindow.show(stage);
                }
                return true;
            }
            return false;
        });
        preferences.pack();
        preferences.setSize(about.getWidth(), about.getWidth());

        // Add to button list.
        buttonList.add(preferences);
        buttonList.add(about);
        buttonList.add(screenModeButton);

        HorizontalGroup bottomRight = new HorizontalGroup();
        bottomRight.space(pad18);
        bottomRight.addActor(preferences);
        bottomRight.addActor(about);
        bottomRight.setFillParent(true);
        bottomRight.bottom().right().pad(pad28);

        stage.addActor(center);
        stage.addActor(topLeft);
        stage.addActor(screenMode);
        stage.addActor(bottomRight);
        stage.addActor(popupInterface);

        if (baseDataPresent) {
            // Check if there is an update for the base data, and show a notice if so
            if (serverDatasets != null && serverDatasets.updatesAvailable) {
                DatasetDesc baseData = serverDatasets.findDataset(Constants.DEFAULT_DATASET_KEY);
                if (baseData != null && baseData.myVersion < baseData.serverVersion) {
                    // We have a base data update, show notice
                    GenericDialog baseDataNotice = new GenericDialog(I18n.msg("gui.basedata.title"), skin, stage) {

                        @Override
                        protected void build() {
                            content.clear();
                            content.pad(pad34, pad28 * 2f, pad34, pad28 * 2f);
                            content.add(new OwnLabel(I18n.msg("gui.basedata.default", baseData.name, I18n.msg("gui.welcome.dsmanager")), skin, "huge")).left().colspan(
                                    3).padBottom(pad34 * 2f).row();
                            content.add(new OwnLabel(I18n.msg("gui.basedata.version", baseData.myVersion), skin, "header-large")).center().padRight(pad34);
                            content.add(new OwnLabel("->", skin, "main-title-s")).center().padRight(pad34);
                            content.add(new OwnLabel(I18n.msg("gui.basedata.version", baseData.serverVersion), skin, "header-large")).center().padRight(pad34);
                        }

                        @Override
                        protected boolean accept() {
                            // Nothing
                            return true;
                        }

                        @Override
                        protected void cancel() {
                            // Nothing
                        }

                        @Override
                        public void dispose() {
                            // Nothing
                        }
                    };
                    baseDataNotice.setAcceptText(I18n.msg("gui.ok"));
                    baseDataNotice.buildSuper();
                    baseDataNotice.show(stage);
                }
            }
        }
        updateFocused();
        EventManager.instance.subscribe(this, Event.UI_RELOAD_CMD, Event.UI_SCALE_CMD);
    }

    private void ensureBaseDataEnabled(DataDescriptor dd) {
        if (dd != null) {
            DatasetDesc base = null;
            for (DatasetDesc dataset : dd.datasets) {
                if (dataset.baseData) {
                    base = dataset;
                    break;
                }
            }
            if (base != null) {
                if (!Settings.settings.data.dataFiles.contains(base.checkStr)) {
                    Settings.settings.data.dataFiles.add(0, base.checkStr);
                }
            }
        }
    }

    /**
     * Starts gaia sky.
     */
    public void startLoading() {
        EventManager.instance.removeAllSubscriptions(this);
        removeOwnListeners();
        ensureBaseDataEnabled(serverDatasets);

        if (popupInterface != null) {
            popupInterface.remove();
            popupInterface.dispose();
        }

        if (bgTex != null)
            bgTex.dispose();

        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        EventManager.publish(Event.LOAD_DATA_CMD, this);
    }

    /**
     * Reloads the view completely
     */
    private void reloadView() {
        clearGui();
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        buildWelcomeUI();
    }

    private void reloadLocalDatasets() {
        this.localDatasets = DataDescriptorUtils.instance().buildLocalDatasets(null);
    }

    private void savePreferences() {
        // Save configuration
        SettingsManager.persistSettings(new File(System.getProperty("properties.file")));
        EventManager.publish(Event.PROPERTIES_WRITTEN, this);
    }

    private int numTotalDatasetsEnabled() {
        return this.localDatasets != null ? (int) this.localDatasets.datasets.stream().filter(ds -> Settings.settings.data.dataFiles.contains(ds.checkStr)).count() : 0;
    }

    private int numCatalogsAvailable() {
        return this.localDatasets != null ? this.localDatasets.datasets.size() : 0;
    }

    private int numGaiaDRCatalogsEnabled() {
        int matches = 0;
        for (String f : Settings.settings.data.dataFiles) {
            String path = Settings.settings.data.dataFile(f);
            if (isGaiaDRCatalogFile(path)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean isGaiaDRCatalogFile(String name) {
        return name.matches("^\\S*catalog-[e]?dr\\d+(int\\d+)?-\\S+(\\.json)$");
    }

    private int numStarCatalogsEnabled() {
        int matches = 0;
        if (serverDatasets == null && localDatasets == null) {
            return 0;
        }

        for (String f : Settings.settings.data.dataFiles) {
            // File name with no extension
            Path path = Settings.settings.data.dataPath(f);
            String filenameExt = path.getFileName().toString();
            try {
                DatasetDesc dataset = null;
                // Try with server description
                if (serverDatasets != null) {
                    dataset = serverDatasets.findDatasetByDescriptor(path);
                }
                // Try local description
                if (dataset == null && localDatasets != null) {
                    dataset = localDatasets.findDatasetByDescriptor(path);
                }
                if ((dataset != null && dataset.isStarDataset()) || isGaiaDRCatalogFile(filenameExt)) {
                    matches++;
                }
            } catch (Exception e) {
                logger.error(e);
            }
        }

        return matches;
    }

    private Set<String> removeNonExistent() {
        Set<String> toRemove = new HashSet<>();
        final FileHandleResolver dataResolver = fileName -> Settings.settings.data.dataFileHandle(fileName);
        for (String f : Settings.settings.data.dataFiles) {
            // File name with no extension
            FileHandle fh = dataResolver.resolve(f);
            if (!fh.exists()) {
                // File does not exist, remove from selected list!
                toRemove.add(f);
            }
        }

        // Remove non-existent files
        for (String out : toRemove) {
            Settings.settings.data.dataFiles.remove(out);
        }

        return toRemove;
    }

    /**
     * Checks if the basic Gaia Sky data folders are present
     * in the default data folder
     *
     * @return True if basic data is found
     */
    public boolean baseDataPresent() {
        Array<Path> defaultDatasetFiles = new Array<>();
        fillDefaultDatasetFiles(defaultDatasetFiles);

        for (Path p : defaultDatasetFiles) {
            if (!Files.exists(p) || !Files.isReadable(p)) {
                logger.info("Data files not found: " + p);
                return false;
            }
        }

        return true;
    }

    private void fillDefaultDatasetFiles(Array<Path> newFiles) {
        // Fill in new data format.
        Path location = Paths.get(Settings.settings.data.location).normalize();
        newFiles.add(location.resolve(Constants.DEFAULT_DATASET_KEY));
        newFiles.add(location.resolve(Constants.DEFAULT_DATASET_KEY).resolve("dataset.json"));
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDatasetManagerWindow(DataDescriptor dd) {
        if (datasetManager == null) {
            datasetManager = new DatasetManagerWindow(stage, skin, dd);
            datasetManager.setAcceptRunnable(() -> {
                if (datasetManager != null) {
                    // Run with slight delay to wait for hide animation.
                    Timer.schedule(new Task() {
                        @Override
                        public void run() {
                            reloadView();
                        }
                    }, 0.5f);

                }
            });
        } else {
            datasetManager.refresh();
        }
        datasetManager.show(stage);
    }

    public void clearGui() {
        if (stage != null) {
            stage.clear();
        }
        if (datasetManager != null) {
            datasetManager.remove();
            datasetManager = null;
        }
        if (preferencesWindow != null) {
            preferencesWindow.remove();
            preferencesWindow = null;
        }
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        if (kbdListener != null && kbdListener.isActive()) {
            kbdListener.update();
        }
        if (gamepadListener != null && gamepadListener.isActive()) {
            gamepadListener.update();
        }
    }

    @Override
    protected void rebuildGui() {

    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        switch (event) {
        case UI_RELOAD_CMD -> {
            GaiaSky.postRunnable(() -> {
                GlobalResources globalResources = GaiaSky.instance.getGlobalResources();
                // Reinitialise GUI system
                globalResources.updateSkin();
                GenericDialog.updatePads();
                // UI theme reload broadcast
                EventManager.publish(Event.UI_THEME_RELOAD_INFO, this, globalResources.getSkin());
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.ui.reload"));
                // Reload window
                this.skin = globalResources.getSkin();
                reloadView();
            });
        }
        case UI_SCALE_CMD -> {
            float uiScale = (Float) data[0];
            this.updateUnitsPerPixel(1f / uiScale);
        }
        }
    }

    private void addOwnListeners() {
        if (kbdListener != null) {
            var multiplexer = GaiaSky.instance.inputMultiplexer;
            if (multiplexer != null)
                multiplexer.addProcessor(0, kbdListener);
            kbdListener.activate();
        }

        if (gamepadListener != null) {
            Settings.settings.controls.gamepad.addControllerListener(gamepadListener);
            gamepadListener.activate();
        }
    }

    private void removeOwnListeners() {
        if (kbdListener != null) {
            var multiplexer = GaiaSky.instance.inputMultiplexer;
            if (multiplexer != null)
                multiplexer.removeProcessor(kbdListener);
            kbdListener.deactivate();
        }

        if (gamepadListener != null) {
            Settings.settings.controls.gamepad.removeControllerListener(gamepadListener);
            gamepadListener.deactivate();
        }
    }

    public boolean updateFocused() {
        if (buttonList != null && buttonList.size != 0) {
            Button actor = buttonList.get(currentSelected);
            stage.setKeyboardFocus(actor);
            return true;
        }
        return false;
    }

    private boolean up() {
        if (currentSelected == 0) {
            currentSelected = buttonList.size - 1;
        } else {
            currentSelected = (currentSelected - 1) % buttonList.size;
        }
        return updateFocused();
    }

    private boolean down() {
        currentSelected = (currentSelected + 1) % buttonList.size;
        return updateFocused();
    }

    public void fireChange() {
        if (buttonList != null) {
            Button b = buttonList.get(currentSelected);
            ChangeEvent event = Pools.obtain(ChangeEvent.class);
            event.setTarget(b);
            b.fire(event);
            Pools.free(event);
        }
    }

    private class WelcomeGuiKbdListener extends GuiKbdListener {

        protected WelcomeGuiKbdListener(Stage stage) {
            super(stage);
        }

        @Override
        public boolean close() {
            GaiaSky.postRunnable(Gdx.app::exit);
            return true;
        }

        @Override
        public boolean accept() {
            startLoading();
            return true;
        }

        @Override
        public boolean select() {
            return false;
        }

        @Override
        public boolean tabLeft() {
            return false;
        }

        @Override
        public boolean tabRight() {
            return false;
        }

        @Override
        public boolean moveUp() {
            return up();
        }

        @Override
        public boolean moveDown() {
            return down();
        }

    }

    /**
     * Enable controller button selection.
     */
    private class WelcomeGuiGamepadListener extends AbstractGamepadListener {

        public WelcomeGuiGamepadListener(String mappingsFile) {
            super(mappingsFile);
        }

        @Override
        public void connected(Controller controller) {

        }

        @Override
        public void disconnected(Controller controller) {

        }

        @Override
        public boolean pollAxes() {
            if (lastControllerUsed != null) {
                float value = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisLstickV()));
                if (value > 0) {
                    down();
                    return true;
                } else if (value < 0) {
                    up();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean pollButtons() {
            if (isKeyPressed(mappings.getButtonDpadUp())) {
                up();
                return true;
            } else if (isKeyPressed(mappings.getButtonDpadDown())) {
                down();
                return true;
            }
            return false;
        }

        @Override
        public boolean buttonDown(Controller controller,
                                  int buttonCode) {
            long now = TimeUtils.millis();
            if (buttonCode == mappings.getButtonStart()) {
                startLoading();
                lastButtonPollTime = now;
            } else if (buttonCode == mappings.getButtonA()) {
                fireChange();
                lastButtonPollTime = now;
            } else if (buttonCode == mappings.getButtonDpadUp()) {
                up();
                lastButtonPollTime = now;
            } else if (buttonCode == mappings.getButtonDpadDown()) {
                down();
                lastButtonPollTime = now;
            }
            lastControllerUsed = controller;
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller,
                                int buttonCode) {
            lastControllerUsed = controller;
            return true;
        }

        @Override
        public boolean axisMoved(Controller controller,
                                 int axisCode,
                                 float value) {
            long now = TimeUtils.millis();
            if (now - lastAxisEvtTime > axisEventDelay) {
                // Event-based
                if (axisCode == mappings.getAxisLstickV()) {
                    value = (float) applyZeroPoint(value);
                    // LEFT STICK vertical - move vertically
                    if (value > 0) {
                        down();
                        lastAxisEvtTime = now;
                    } else if (value < 0) {
                        up();
                        lastAxisEvtTime = now;
                    }
                }
            }
            lastControllerUsed = controller;
            return true;
        }

    }

}
