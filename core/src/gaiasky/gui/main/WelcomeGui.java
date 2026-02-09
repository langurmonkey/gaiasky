/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetManagerWindow;
import gaiasky.gui.iface.PopupNotificationsInterface;
import gaiasky.gui.window.*;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.input.GuiKbdListener;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.gdx.loader.OwnTextureLoader;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.vr.openxr.XrLoadStatus;
import net.jafama.FastMath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Manages the interface that shows up immediately after starting Gaia Sky. Coordinates access to the dataset manager window, and
 * provides access to start the application.
 */
public class WelcomeGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(WelcomeGui.class);
    private static final AtomicReference<DataDescriptor> localDatasets = new AtomicReference<>();

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
    private Array<Button> buttonList;
    private int currentSelected = 0;
    private PopupNotificationsInterface popupInterface;
    private WelcomeGuiKbdListener kbdListener;
    private Table datasetsContainer;
    private boolean preventRecommended = false;
    private float dsScrollY;

    /**
     * Creates an initial GUI.
     *
     * @param skipWelcome Skips the welcome screen if possible.
     * @param vrStatus    The status of VR.
     */
    public WelcomeGui(final Skin skin,
                      final Graphics graphics,
                      final Float unitsPerPixel,
                      final boolean skipWelcome,
                      final XrLoadStatus vrStatus) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.skipWelcome = skipWelcome;
        this.vrStatus = vrStatus;
        this.vr = vrStatus.vrInitOk();
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
        popupInterface.top()
                .right();
        popupInterface.setFillParent(true);

        if (DataDescriptorUtils.dataLocationOldVersionDatasetsCheck()) {
            var fsCheck = new DataLocationCheckWindow(I18n.msg("gui.dscheck.title"), skin, stage);
            fsCheck.setAcceptListener(() -> {
                // Clean old datasets in a thread in the background.
                GaiaSky.instance.getExecutorService()
                        .execute(DataDescriptorUtils::cleanDataLocationOldDatasets);
                // Continue immediately.
                continueWelcomeGui01();
            });
            fsCheck.setCancelListener(this::continueWelcomeGui01);
            fsCheck.show(stage);
        } else {
            continueWelcomeGui01();
        }

    }

    private void continueWelcomeGui01() {
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
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this,
                                             I18n.msg("gui.welcome.gamepad.notification", Controllers.getControllers().size),
                                             10f);
                    }
                };
                Timer.schedule(notification, 2);
            }

            buildWaitingUI();

            // Test mirrors in order and select the first working one.
            testMirrorConnectionChain(0,
                                      this::continueWelcomeGui02,
                                      this::connectionError);

        }
    }

    private void checkDataMirror(){
        // If no data mirror was selected, choose first.
        if (Settings.settings.program.url.currentMirror == null) {
            Settings.settings.program.url.currentMirror = Settings.settings.program.url.dataMirrors[0];
        }
    }
    private void checkDescriptorMirror() {
        // If no descriptor mirror was selected, choose first.
        if (Settings.settings.program.url.currentDescriptor == null) {
            Settings.settings.program.url.currentDescriptor = Settings.settings.program.url.dataDescriptors[0];
        }
    }

    /**
     * Tests and selects the best data descriptor mirror, or displays an error if no mirror could be contacted.
     * If successful, proceeds with {@link #continueWelcomeGui03()}.
     */
    private void continueWelcomeGui02() {
        checkDataMirror();
        // Select working data descriptor mirror, in order.
        testDataDescMirrorConnectionChain(0,
                                          this::continueWelcomeGui03,
                                          this::connectionError);
    }

    /**
     * Once the mirrors have been selected, we proceed with downloading the data descriptor and building the UI.
     */
    private void continueWelcomeGui03() {
        checkDescriptorMirror();
        // Fetch descriptor file.
        dataDescriptor = Gdx.files.absolute(SysUtils.getDataTempDir(Settings.settings.data.location) + "/gaiasky-data.json.gz");
        DownloadHelper.downloadFile(Settings.settings.program.url.getCurrentDataDescriptor(),
                                    dataDescriptor,
                                    Settings.settings.program.offlineMode,
                                    null,
                                    null,
                                    (digest) -> GaiaSky.postRunnable(() -> {
                                        // Data descriptor ok. Skip welcome screen only if flag and base data present
                                        if (skipWelcome && baseDataPresent()) {
                                            startLoading();
                                        } else {
                                            buildWelcomeUI();
                                        }
                                    }),
                                    this::connectionError, null);

        /* CAPTURE SCROLL FOCUS */
        stage.addListener(event -> {
            if (event instanceof InputEvent ie) {

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

    private void connectionError() {
        // Fail?
        downloadError = true;
        if (Settings.settings.program.offlineMode) {
            logger.warn(I18n.msg("gui.welcome.error.offlinemode"));
        } else {
            logger.error(I18n.msg("gui.welcome.error.nointernet"));
        }
        if (baseDataPresent()) {
            // Just post a tooltip and go on.
            GaiaSky.postRunnable(() -> {
                var title = I18n.msg("gui.download.noconnection.continue");
                if (Settings.settings.program.offlineMode) {
                    title = I18n.msg("gui.system.offlinemode.tooltip");
                }
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, title, 10f);
                checkDataMirror();
                checkDescriptorMirror();
                buildWelcomeUI();
            });
        } else {
            // Error and exit.
            logger.error(I18n.msg("gui.welcome.error.nobasedata"));
            GaiaSky.postRunnable(() -> GuiUtils.addNoConnectionExit(skin, stage));
        }
    }

    private void testMirrorConnectionChain(final int index, Runnable success, Runnable fail) {
        var mirrors = Settings.settings.program.url.dataMirrors;
        final var n = mirrors.length;
        DownloadHelper.testConnection(mirrors[index] + "index.html",
                                      (url) -> {
                                          logger.info("Selected data mirror: " + url);
                                          Settings.settings.program.url.currentMirror = mirrors[index];
                                          success.run();
                                      },
                                      () -> {
                                          // Fail, try next.
                                          if (index + 1 < n) {
                                              testMirrorConnectionChain(index + 1, success, fail);
                                          } else {
                                              fail.run();
                                          }
                                      });
    }

    private void testDataDescMirrorConnectionChain(final int index, Runnable success, Runnable fail) {
        var mirrors = Settings.settings.program.url.dataDescriptors;
        final var n = mirrors.length;
        DownloadHelper.testConnection(mirrors[index],
                                      (url) -> {
                                          logger.info("Selected data descriptor mirror: " + url);
                                          Settings.settings.program.url.currentDescriptor = mirrors[index];
                                          success.run();
                                      },
                                      () -> {
                                          // Fail, try next.
                                          if (index + 1 < n) {
                                              testDataDescMirrorConnectionChain(index + 1, success, fail);
                                          } else {
                                              fail.run();
                                          }
                                      });
    }

    private void buildWaitingUI() {
        // Render message.
        if (!Settings.settings.program.offlineMode) {
            this.updateUnitsPerPixel(1.6f);
            // Central table
            Table centerContainer = new Table(skin);
            centerContainer.setFillParent(true);
            centerContainer.bottom()
                    .right();
            if (bgTex == null) {
                bgTex = new Texture(OwnTextureLoader.Factory.loadFromFile(Gdx.files.internal("img/splash/splash.jpg"), false));
            }
            Drawable bg = new SpriteDrawable(new Sprite(bgTex));
            centerContainer.setBackground(bg);

            var table = new Table(skin);
            var gaiaSky = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openXr), skin, "main-title");
            table.add(gaiaSky)
                    .row();
            var msg = new OwnLabel(I18n.msg("gui.welcome.datasets.updates"), skin);
            table.add(msg);

            centerContainer.add(table)
                    .bottom()
                    .right()
                    .padBottom(30f)
                    .padRight(30f);

            stage.addActor(centerContainer);
        }
    }

    private void buildWelcomeUI() {
        stage.clear();
        addOwnListeners();
        buttonList = new Array<>();
        serverDatasets = !downloadError ? DataDescriptorUtils.instance()
                .buildServerDatasets(dataDescriptor) : null;
        reloadLocalDatasets();

        // Central table
        Table centerContainer = new Table(skin);
        centerContainer.setFillParent(true);
        centerContainer.center();
        if (bgTex == null) {
            bgTex = new Texture(OwnTextureLoader.Factory.loadFromFile(Gdx.files.internal("img/splash/splash.jpg"), false));
        }
        Drawable bg = new SpriteDrawable(new Sprite(bgTex));
        centerContainer.setBackground(bg);

        float pad16 = 16f;
        float pad18 = 18f;
        float pad28 = 28f;
        float pad32 = 32f;

        float buttonWidth = 460f;
        float buttonHeight = 110f;

        EventManager.instance.subscribe(this, Event.UI_RELOAD_CMD, Event.UI_SCALE_RECOMPUTE_CMD);
        EventManager.publish(Event.UI_SCALE_RECOMPUTE_CMD, this);

        // CENTRAL TABLE
        // Contains the logo and the start/dataset/exit buttons.
        Table center = new Table(skin);
        center.center();
        center.setBackground("bg-pane");
        center.pad(pad32);
        center.padLeft(pad32 * 5f);
        center.padRight(pad32 * 5f);

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

        FileHandle gsIcon = Gdx.files.internal("icon/gs_icon_256.png");
        Texture iconTex = new Texture(gsIcon);
        iconTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image logo = new Image(iconTex);
        logo.setScale(0.9f);
        logo.setOrigin(Align.center);

        OwnLabel gaiaSky = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openXr), skin, "main-title");
        gaiaSky.setFontScale(1.5f);
        OwnLabel version = new OwnLabel(Settings.settings.version.version, skin, "main-title");
        version.setColor(skin.getColor("theme"));
        Table title = new Table(skin);
        title.add(gaiaSky)
                .bottom()
                .left()
                .padBottom(pad16)
                .row();
        title.add(version)
                .bottom()
                .left()
                .padRight(pad16);

        titleGroup.add(logo)
                .center()
                .padLeft(pad28)
                .padRight(pad28 * 4f);
        titleGroup.add(new Separator(skin, "default"))
                .fillY()
                .padRight(pad28 * 2f);
        titleGroup.add(title)
                .expandX();

        String textStyle = "main-title-s";


        // Add title to center.
        center.add(titleGroup)
                .expandX()
                .center()
                .padBottom(pad18 * 5f)
                .colspan(2)
                .row();


        final int numLocalDatasets = localDatasets.get().datasets.size() + (baseDataPresent ? 1 : 0);
        final boolean regularStart = numLocalDatasets > 0 || preventRecommended;

        if (regularStart) {
            // Regular Welcome screen options: Start Gaia Sky, Dataset Manager, Dataset List
            // Start Gaia Sky button
            OwnTextIconButton startButton = new OwnTextIconButton(I18n.msg("gui.welcome.start", Settings.APPLICATION_NAME), skin,
                                                                  "start");
            startButton.setSpace(pad18);
            startButton.setPad(pad16);
            startButton.setContentAlign(Align.center);
            startButton.align(Align.center);
            startButton.setHeight(buttonHeight);
            startButton.pack();
            startButton.setWidth(Math.max(startButton.getWidth(), buttonWidth));
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
            startGroup.add(startLabel)
                    .top()
                    .left()
                    .padBottom(pad16)
                    .row();
            if (!baseDataPresent) {
                // No basic data, can't start!
                startButton.setDisabled(true);
                var text = TextUtils.breakCharacters(I18n.msg("gui.welcome.start.nobasedata"), 50);
                OwnLabel noBaseData = new OwnLabel(text, skin, textStyle);
                noBaseData.setColor(ColorUtils.gRedC);
                startGroup.add(noBaseData)
                        .bottom()
                        .left();
            } else if (numCatalogsAvailable > 0 && numTotalCatalogsEnabled == 0) {
                var text = TextUtils.breakCharacters(I18n.msg("gui.welcome.start.nocatalogs"), 50);
                OwnLabel noCatsSelected = new OwnLabel(text, skin, textStyle);
                noCatsSelected.setColor(ColorUtils.gRedC);
                startGroup.add(noCatsSelected)
                        .bottom()
                        .left();
            } else if (numGaiaDRCatalogsEnabled > 1 || numStarCatalogsEnabled == 0) {
                var text = TextUtils.breakCharacters(I18n.msg("gui.welcome.start.check"), 50);
                OwnLabel tooManyDR = new OwnLabel(text, skin, textStyle);
                tooManyDR.setColor(ColorUtils.gRedC);
                startGroup.add(tooManyDR)
                        .bottom()
                        .left();
            } else {
                var text = TextUtils.breakCharacters(I18n.msg("gui.welcome.start.ready"), 50);
                OwnLabel ready = new OwnLabel(text, skin, textStyle);
                ready.setColor(ColorUtils.gGreenC);
                startGroup.add(ready)
                        .bottom()
                        .left();
            }

            // Dataset manager button
            OwnTextIconButton datasetManagerButton = new OwnTextIconButton(I18n.msg("gui.welcome.dsmanager"), skin,
                                                                           "cloud-download");
            datasetManagerButton.setSpace(pad18);
            datasetManagerButton.setPad(pad16);
            datasetManagerButton.setContentAlign(Align.center);
            datasetManagerButton.align(Align.center);
            datasetManagerButton.setHeight(buttonHeight);
            datasetManagerButton.pack();
            datasetManagerButton.setWidth(Math.max(datasetManagerButton.getWidth(), buttonWidth));
            datasetManagerButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    addDatasetManagerWindow(serverDatasets);
                }
                return true;
            });
            buttonList.add(datasetManagerButton);

            Table datasetManagerInfo = new Table(skin);
            OwnLabel downloadLabel = new OwnLabel(I18n.msg("gui.welcome.dsmanager.desc"), skin, textStyle);
            datasetManagerInfo.add(downloadLabel)
                    .top()
                    .left()
                    .padBottom(pad16);
            if (serverDatasets != null && serverDatasets.updatesAvailable) {
                datasetManagerInfo.row();
                OwnLabel updates = new OwnLabel(I18n.msg("gui.welcome.dsmanager.updates", serverDatasets.numUpdates), skin,
                                                textStyle);
                updates.setColor(ColorUtils.gYellowC);
                datasetManagerInfo.add(updates)
                        .bottom()
                        .left();
            } else if (!baseDataPresent) {
                datasetManagerInfo.row();
                OwnLabel getBasedata = new OwnLabel(I18n.msg("gui.welcome.dsmanager.info"), skin, textStyle);
                getBasedata.setColor(ColorUtils.gGreenC);
                datasetManagerInfo.add(getBasedata)
                        .bottom()
                        .left();
            } else {
                // Number selected
                OwnLabel numCatalogsEnabled = new OwnLabel(
                        I18n.msg("gui.welcome.enabled", numTotalCatalogsEnabled, numCatalogsAvailable), skin,
                        textStyle);
                numCatalogsEnabled.setColor(ColorUtils.gBlueC);
                datasetManagerInfo.row()
                        .padBottom(pad16);
                datasetManagerInfo.add(numCatalogsEnabled)
                        .left()
                        .padBottom(pad18);
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

            Table vrOptionsTable = null;
            if (vrStatus.vrInitOk()) {
                vrOptionsTable = new Table(skin);
                // VR demo mode.
                var vrDemo = new OwnCheckBox(I18n.msg("gui.vr.demo"), skin, 10f);
                vrDemo.setChecked(Settings.settings.runtime.vrDemoMode);
                vrDemo.listenTo(Event.VR_DEMO_MODE_CMD);
                vrDemo.addListener((event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.VR_DEMO_MODE_CMD, vrDemo, vrDemo.isChecked());
                        return true;
                    }
                    return false;
                }));
                OwnImageButton vrDemoTooltip = new OwnImageButton(skin, "tooltip");
                vrDemoTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.vr.demo.info"), skin));
                // VR desktop mirror.
                var vrMirror = new OwnCheckBox(I18n.msg("gui.vr.mirror"), skin, 10f);
                vrMirror.setChecked(Settings.settings.runtime.vrDesktopMirror);
                vrMirror.listenTo(Event.VR_DESKTOP_MIRROR_CMD);
                vrMirror.addListener((event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.VR_DESKTOP_MIRROR_CMD, vrMirror, vrMirror.isChecked());
                        return true;
                    }
                    return false;
                }));
                OwnImageButton vrMirrorTooltip = new OwnImageButton(skin, "tooltip");
                vrMirrorTooltip.addListener(new OwnTextTooltip(I18n.msg("gui.vr.mirror.info"), skin));
                // Add to table.
                vrOptionsTable.add(vrDemo)
                        .left()
                        .padRight(pad16);
                vrOptionsTable.add(vrDemoTooltip)
                        .left().row();
                vrOptionsTable.add(vrMirror)
                        .left()
                        .padRight(pad16);
                vrOptionsTable.add(vrMirrorTooltip)
                        .left();
            }


            // Start button
            center.add(startButton)
                    .right()
                    .top()
                    .padBottom(pad18 * 6f)
                    .padRight(pad28 * 2f);
            center.add(startGroup)
                    .top()
                    .left()
                    .padBottom(pad18 * 6f)
                    .row();

            // Dataset manager
            center.add(datasetManagerButton)
                    .right()
                    .top()
                    .padBottom(pad32)
                    .padRight(pad28 * 2f);
            center.add(datasetManagerInfo)
                    .left()
                    .top()
                    .padBottom(pad32)
                    .row();

            center.add(selectionInfo)
                    .colspan(2)
                    .center()
                    .top()
                    .padBottom(pad32 * (vrOptionsTable != null ? 1f : 2f))
                    .row();

            if (vrOptionsTable != null) {
                center.add(vrOptionsTable)
                        .colspan(2)
                        .center()
                        .top()
                        .padBottom(pad32 * 2f)
                        .row();
            }

            if (numLocalDatasets == 0 && preventRecommended) {
                // Add back button
                OwnTextIconButton backButton = new OwnTextIconButton(I18n.msg("gui.back"), skin, "back");
                backButton.addListener(new OwnTextTooltip(I18n.msg("gui.back.prev"), skin, 10));
                backButton.setHeight(buttonHeight * 0.6f);
                backButton.setWidth(buttonWidth * 0.5f);
                backButton.addListener(new ClickListener() {
                    public void clicked(InputEvent event,
                                        float x,
                                        float y) {
                        preventRecommended = false;
                        reloadView();
                    }
                });
                backButton.pack();

                center.add(backButton)
                        .colspan(2)
                        .left()
                        .bottom()
                        .padBottom(pad32);
            }

        } else {
            // Recommended datasets
            OwnTextIconButton recommendedDatasetsButton = new OwnTextIconButton(I18n.msg("gui.welcome.recommended"), skin,
                                                                                "start");
            recommendedDatasetsButton.setSpace(pad18);
            recommendedDatasetsButton.setPad(pad16);
            recommendedDatasetsButton.setContentAlign(Align.center);
            recommendedDatasetsButton.align(Align.center);
            recommendedDatasetsButton.setHeight(buttonHeight);
            recommendedDatasetsButton.pack();
            recommendedDatasetsButton.setWidth(Math.max(recommendedDatasetsButton.getWidth(), buttonWidth));
            recommendedDatasetsButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    // Download recommended datasets view.
                    Set<String> recommendedDatasets;
                    if (serverDatasets.recommended != null && serverDatasets.recommended.length > 0) {
                        recommendedDatasets = Set.of(serverDatasets.recommended);
                    } else {
                        recommendedDatasets = Settings.settings.program.recommendedDatasets;
                    }
                    var recommended = serverDatasets.datasets.stream()
                            .filter(ds -> recommendedDatasets.contains(ds.key))
                            .sorted(Comparator.comparing(datasetDesc -> datasetDesc.name))
                            .toList();
                    var bdwTitle = I18n.msg("gui.batch.recommended.title");
                    var bdwInfo = I18n.msg("gui.batch.recommended.info");
                    var bdw = new BatchDownloadWindow(bdwTitle, bdwInfo, skin, stage, recommended);
                    bdw.setSuccessRunnable(() -> {
                        bdw.closeCancel();
                        bdw.dispose();
                        startLoading();
                    });
                    bdw.setErrorRunnable(() -> {
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.batch.error.global"));
                        bdw.closeCancel();
                        bdw.dispose();
                        reloadView();
                    });
                    bdw.show(stage);
                    bdw.downloadDatasets();
                }
                return true;
            });
            buttonList.add(recommendedDatasetsButton);

            // Full control
            OwnTextIconButton customizeButton = new OwnTextIconButton(I18n.msg("gui.welcome.custom"), skin, "cloud-download");
            customizeButton.setSpace(pad18);
            customizeButton.setPad(pad16);
            customizeButton.setContentAlign(Align.center);
            customizeButton.align(Align.center);
            customizeButton.setHeight(buttonHeight);
            customizeButton.pack();
            customizeButton.setWidth(Math.max(recommendedDatasetsButton.getWidth(), buttonWidth));
            customizeButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    preventRecommended = true;
                    reloadView();
                }
                return true;
            });
            buttonList.add(customizeButton);

            // Recommended datasets button
            center.add(recommendedDatasetsButton)
                    .expandX()
                    .center()
                    .top()
                    .padBottom(pad18 * 3f)
                    .row();

            // Customize datasets button
            center.add(customizeButton)
                    .expandX()
                    .center()
                    .top()
                    .padBottom(pad32 * 5f)
                    .row();
        }

        // Add to center container
        centerContainer.add(center)
                .center().row();
        centerContainer.add(new OwnLabel("的一是在不了有和人这", skin));

        // Enabled DATASETS
        datasetsContainer = new Table(skin);
        datasetsContainer.setFillParent(true);

        Table datasetsTable = new Table(skin);
        datasetsTable.setBackground("bg-pane");
        datasetsTable.right()
                .pad(pad16);
        Table datasets = new Table(skin);
        datasets.left();

        var enabledDatasets = getEnabledDatasets();
        int count = enabledDatasets != null ? enabledDatasets.size() : 0;

        datasetsTable.add(new OwnLabel(I18n.msg("gui.welcome.datasets.enabled", count), skin, "header"))
                .left()
                .padBottom(pad16)
                .row();

        if (enabledDatasets != null && !enabledDatasets.isEmpty()) {
            enabledDatasets.stream()
                    .sorted()
                    .forEach(ds -> {
                        var typeIcon = new OwnImage(skin.getDrawable(DatasetManagerWindow.getIcon(ds.type)));
                        typeIcon.setSize(30f, 30f);
                        var disable = new OwnTextIconButton("", skin, "select-none");
                        disable.setSize(40f, 35f);
                        disable.addListener(new OwnTextTooltip(I18n.msg("gui.download.disable") + " " + ds.name, skin, 40));
                        disable.addListener(new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, Actor actor) {
                                Settings.settings.data.disableDataset(ds);
                                reloadView();
                            }
                        });
                        var dsName = new OwnLabel(TextUtils.capString(ds.name, 32), skin);
                        dsName.setWidth(410f);
                        var g = hg(typeIcon, dsName);
                        datasets.add(disable)
                                .center()
                                .padRight(pad16 / 2f)
                                .left()
                                .padBottom(pad16);
                        datasets.add(g)
                                .left()
                                .padBottom(pad16)
                                .row();
                    });
        } else {
            var noDatasets = new OwnLabel(I18n.msg("gui.welcome.datasets.enabled.no"), skin);
            noDatasets.setWidth(410f);
            datasets.add(noDatasets)
                    .padLeft(5f)
                    .left();
        }
        datasets.pack();

        final var datasetsScroll = getOwnScrollPane(datasets);
        datasetsTable.add(datasetsScroll)
                .top()
                .left();

        datasetsContainer.add(datasetsTable)
                .right()
                .expandX()
                .padRight(pad32);

        // TOP INFO - VERSION LINE
        Table topLeft = new VersionLineTable(skin);

        // Screen mode button
        Table screenMode = new Table(skin);
        screenMode.setFillParent(true);
        screenMode.top()
                .right();
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

        // About button
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

        // Preferences button
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

        // Exit button
        OwnTextIconButton exit = new OwnTextIconButton("", skin, "quit");
        exit.addListener(new OwnTextTooltip(I18n.msg("context.quit"), skin, 10));
        exit.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                GaiaSky.postRunnable(Gdx.app::exit);
                return true;
            }
            return false;
        });
        exit.pack();


        // Add to button list.
        buttonList.add(preferences);
        buttonList.add(about);
        buttonList.add(exit);
        buttonList.add(screenModeButton);

        HorizontalGroup bottomRight = new HorizontalGroup();
        bottomRight.space(pad18);
        bottomRight.addActor(preferences);
        bottomRight.addActor(about);
        bottomRight.addActor(exit);
        bottomRight.setFillParent(true);
        bottomRight.bottom()
                .right()
                .pad(pad28);

        stage.addActor(centerContainer);
        stage.addActor(datasetsContainer);
        stage.addActor(topLeft);
        stage.addActor(screenMode);
        stage.addActor(bottomRight);
        stage.addActor(popupInterface);

        if (baseDataPresent) {
            // Check if there is an update for the base data, and show a notice if so
            if (serverDatasets != null && serverDatasets.updatesAvailable) {
                DatasetDesc baseData = serverDatasets.findDatasetByKey(Constants.DEFAULT_DATASET_KEY);
                if (baseData != null && baseData.myVersion < baseData.serverVersion) {
                    // We have a base data update, show notice.
                    GenericDialog baseDataNotice = new GenericDialog(I18n.msg("gui.basedata.title"), skin, stage) {

                        @Override
                        protected void build() {
                            content.clear();
                            content.pad(pad34, pad28 * 2f, pad34, pad28 * 2f);
                            content.add(
                                            new OwnLabel(
                                                    I18n.msg("gui.basedata.default", baseData.name, I18n.msg("gui.welcome.dsmanager")),
                                                    skin, "huge"))
                                    .left()
                                    .colspan(
                                            3)
                                    .padBottom(pad34 * 2f)
                                    .row();
                            content.add(new OwnLabel(I18n.msg("gui.basedata.version", baseData.myVersion), skin, "header-large"))
                                    .center()
                                    .padRight(pad34);
                            content.add(new OwnLabel("->", skin, "main-title-s"))
                                    .center()
                                    .padRight(pad34);
                            content.add(
                                            new OwnLabel(I18n.msg("gui.basedata.version", baseData.serverVersion), skin, "header-large"))
                                    .center()
                                    .padRight(pad34);
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
    }

    private OwnScrollPane getOwnScrollPane(Table datasets) {
        var datasetsScroll = new OwnScrollPane(datasets, skin, "minimalist-nobg");
        datasetsScroll.setScrollbarsVisible(true);
        datasetsScroll.setForceScroll(false, true);
        datasetsScroll.setScrollingDisabled(true, false);
        datasetsScroll.setOverscroll(false, false);
        datasetsScroll.setSmoothScrolling(true);
        datasetsScroll.setHeight(FastMath.min(Gdx.graphics.getHeight() * getUnitsPerPixel() * 0.8f, datasets.getHeight()));
        datasetsScroll.setWidth(520f);
        datasetsScroll.pack();

        datasetsScroll.addListener(event -> {
            if (event instanceof InputEvent) {
                dsScrollY = datasetsScroll.getScrollY();
            }
            return false;
        });

        datasetsScroll.setScrollY(dsScrollY);
        return datasetsScroll;
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
                    Settings.settings.data.dataFiles.addFirst(base.checkStr);
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

    private static void reloadLocalDatasets() {
        localDatasets.set(DataDescriptorUtils.instance()
                                  .buildLocalDatasets(null));
    }

    private int numTotalDatasetsEnabled() {
        return localDatasets.get() != null ? (int) localDatasets.get().datasets.stream()
                .filter(ds -> Settings.settings.data.dataFiles.contains(ds.checkStr))
                .count() : 0;
    }

    private Collection<DatasetDesc> getEnabledDatasets() {
        return localDatasets.get() != null ? localDatasets.get().datasets.stream()
                .filter(ds -> Settings.settings.data.dataFiles.contains(ds.checkStr))
                .sorted(Comparator.comparing(a -> a.type))
                .collect(Collectors.toList()) : null;
    }

    private int numCatalogsAvailable() {
        return localDatasets.get() != null ? localDatasets.get().datasets.size() : 0;
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
        if (serverDatasets == null && localDatasets.get() == null) {
            return 0;
        }

        for (String f : Settings.settings.data.dataFiles) {
            // File name with no extension
            Path path = Settings.settings.data.dataPath(f);
            String filenameExt = path.getFileName()
                    .toString();
            try {
                DatasetDesc dataset = null;
                // Try with server description
                if (serverDatasets != null) {
                    dataset = serverDatasets.findDatasetByDescriptor(path);
                }
                // Try local description
                if (dataset == null && localDatasets.get() != null) {
                    dataset = localDatasets.get()
                            .findDatasetByDescriptor(path);
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
        Path location = Paths.get(Settings.settings.data.location)
                .normalize();
        newFiles.add(location.resolve(Constants.DEFAULT_DATASET_KEY));
        newFiles.add(location.resolve(Constants.DEFAULT_DATASET_KEY)
                             .resolve("dataset.json"));
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDatasetManagerWindow(DataDescriptor dd) {
        if (datasetManager == null) {
            datasetManager = new DatasetManagerWindow(stage, skin, dd);
            datasetManager.setAcceptListener(() -> {
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

    protected HorizontalGroup hg(Actor... actors) {
        var hg = new HorizontalGroup();
        hg.space(10f);
        for (Actor a : actors)
            hg.addActor(a);
        return hg;
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
                    GenericDialog.updatePads();
                    // UI theme reload broadcast
                    EventManager.publish(Event.UI_THEME_RELOAD_INFO, this, globalResources.getSkin());
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.ui.reload"));
                    // Reload window
                    this.skin = globalResources.getSkin();
                    reloadView();
                });
            }
            case UI_SCALE_RECOMPUTE_CMD -> {
                int height;
                if (data != null && data.length > 0) {
                    height = (Integer) data[0];
                } else {
                    height = Gdx.graphics.getHeight();
                }
                GaiaSky.instance.applyUIScale(height, this);
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
            ChangeEvent event = POOLS.obtain(ChangeEvent.class);
            event.setTarget(b);
            b.fire(event);
            POOLS.free(event);
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

    public static AtomicReference<DataDescriptor> getLocalDatasets() {
        return localDatasets;
    }

    @Override
    public void resize(final int width, final int height) {
        if (preferencesWindow != null) {
            preferencesWindow.resize(width, height);
        }
        // Hide/show datasets container depending on aspect ratio.
        // Must ensure enough space is left on the right side.
        if (datasetsContainer != null) {
            datasetsContainer.setVisible(!((float) width / (float) height < 1.7));
        }
        super.resize(width, height);
    }
}
