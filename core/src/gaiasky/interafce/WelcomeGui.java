/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.vr.openvr.VRStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Welcome screen that allows access to the main application and the dataset manager. It provides some information
 * on possible problems with the selection, available updates and more.
 */
public class WelcomeGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(WelcomeGui.class);

    private final VRStatus vrStatus;
    private final boolean skipWelcome;

    protected DatasetManagerWindow ddw;

    private FileHandle dataDescriptor;

    private boolean downloadError = false;
    private Texture bgTex;
    private DataDescriptor serverDatasets;
    private DataDescriptor localDatasets;

    /**
     * Creates an initial GUI
     *
     * @param skipWelcome Skips the welcome screen if possible
     * @param vrStatus    The status of VR
     */
    public WelcomeGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final boolean skipWelcome, final VRStatus vrStatus) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.lock = new Object();
        this.skipWelcome = skipWelcome;
        this.vrStatus = vrStatus;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);

        if (vrStatus.vrInitFailed()) {
            if (vrStatus.equals(VRStatus.ERROR_NO_CONTEXT))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRConnectionExit(skin, ui));
            else if (vrStatus.equals(VRStatus.ERROR_RENDERMODEL))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRDataExit(skin, ui));
        } else if (Settings.settings.program.net.slave.active || GaiaSky.instance.isHeadless()) {
            // If slave or headless, data load can start
            gaiaSky();
        } else {
            reloadLocalDatasets();

            // Otherwise, check for updates, etc.
            clearGui();

            dataDescriptor = Gdx.files.absolute(SysUtils.getTempDir(Settings.settings.data.location) + "/gaiasky-data.json");
            DownloadHelper.downloadFile(Settings.settings.program.url.dataDescriptor, dataDescriptor, null, null, (digest) -> GaiaSky.postRunnable(() -> {
                // Data descriptor ok. Skip welcome screen only if flag and base data present
                if (skipWelcome && baseDataPresent()) {
                    gaiaSky();
                } else {
                    buildWelcomeUI();
                }
            }), () -> {
                // Fail?
                downloadError = true;
                logger.error(I18n.txt("gui.welcome.error.nointernet"));
                if (baseDataPresent()) {
                    // Go on all in
                    GaiaSky.postRunnable(() -> GuiUtils.addNoConnectionWindow(skin, ui, () -> buildWelcomeUI()));
                } else {
                    // Error and exit
                    logger.error(I18n.txt("gui.welcome.error.nobasedata"));
                    GaiaSky.postRunnable(() -> GuiUtils.addNoConnectionExit(skin, ui));
                }
            }, null);

            /* CAPTURE SCROLL FOCUS */
            ui.addListener(event -> {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;

                    if (ie.getType() == Type.keyUp) {
                        if (ie.getKeyCode() == Input.Keys.ESCAPE) {
                            Gdx.app.exit();
                        } else if (ie.getKeyCode() == Input.Keys.ENTER) {
                            if (baseDataPresent()) {
                                gaiaSky();
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
        serverDatasets = !downloadError ? DataDescriptorUtils.instance().buildServerDatasets(dataDescriptor) : null;
        // Center table
        Table center = new Table(skin);
        center.setFillParent(true);
        center.center();
        if (bgTex == null)
            bgTex = new Texture(Gdx.files.internal("img/splash/splash.jpg"));
        Drawable bg = new SpriteDrawable(new Sprite(bgTex));
        center.setBackground(bg);

        float pad16 = 16f;
        float pad18 = 18f;
        float pad32 = 32f;
        float pad28 = 28f;

        float bw = 540f;
        float bh = 110f;

        Set<String> removed = removeNonExistent();
        if (removed.size() > 0) {
            logger.warn(I18n.txt("gui.welcome.warn.nonexistent", removed.size()));
            logger.warn(TextUtils.setToStr(removed));
        }
        int numCatalogsAvailable = numCatalogsAvailable();
        int numGaiaDRCatalogsSelected = numGaiaDRCatalogsSelected();
        int numStarCatalogsSelected = numStarCatalogsSelected();
        int numTotalCatalogsSelected = numTotalCatalogsSelected();
        boolean baseDataPresent = baseDataPresent();

        // Title
        HorizontalGroup titleGroup = new HorizontalGroup();
        titleGroup.space(pad32);
        OwnLabel title = new OwnLabel(I18n.txt("gui.welcome.title", Settings.APPLICATION_NAME, Settings.settings.version.version), skin, "main-title");
        OwnLabel gs = new OwnLabel(Settings.APPLICATION_NAME + " " + Settings.settings.version.version, skin, "main-title");
        gs.setColor(skin.getColor("theme"));
        titleGroup.addActor(title);
        titleGroup.addActor(gs);

        String textStyle = "main-title-s";

        // Start Gaia Sky button
        OwnTextIconButton startButton = new OwnTextIconButton(I18n.txt("gui.welcome.start", Settings.APPLICATION_NAME), skin, "start");
        startButton.setSpace(pad18);
        startButton.setContentAlign(Align.center);
        startButton.align(Align.center);
        startButton.setSize(bw, bh);
        startButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                gaiaSky();
            }
            return true;
        });
        Table startGroup = new Table(skin);
        OwnLabel startLabel = new OwnLabel(I18n.txt("gui.welcome.start.desc", Settings.APPLICATION_NAME), skin, textStyle);
        startGroup.add(startLabel).top().left().padBottom(pad16).row();
        if (!baseDataPresent) {
            // No basic data, can't start!
            startButton.setDisabled(true);

            OwnLabel noBaseData = new OwnLabel(I18n.txt("gui.welcome.start.nobasedata"), skin, textStyle);
            noBaseData.setColor(ColorUtils.gRedC);
            startGroup.add(noBaseData).bottom().left();
        } else if (numCatalogsAvailable > 0 && numTotalCatalogsSelected == 0) {
            OwnLabel noCatsSelected = new OwnLabel(I18n.txt("gui.welcome.start.nocatalogs"), skin, textStyle);
            noCatsSelected.setColor(ColorUtils.gRedC);
            startGroup.add(noCatsSelected).bottom().left();
        } else if (numGaiaDRCatalogsSelected > 1 || numStarCatalogsSelected == 0) {
            OwnLabel tooManyDR = new OwnLabel(I18n.txt("gui.welcome.start.check"), skin, textStyle);
            tooManyDR.setColor(ColorUtils.gRedC);
            startGroup.add(tooManyDR).bottom().left();
        } else {
            OwnLabel ready = new OwnLabel(I18n.txt("gui.welcome.start.ready"), skin, textStyle);
            ready.setColor(ColorUtils.gGreenC);
            startGroup.add(ready).bottom().left();
        }

        // Dataset manager button
        OwnTextIconButton datasetManagerButton = new OwnTextIconButton(I18n.txt("gui.welcome.dsmanager"), skin, "cloud-download");
        datasetManagerButton.setSpace(pad18);
        datasetManagerButton.setContentAlign(Align.center);
        datasetManagerButton.align(Align.center);
        datasetManagerButton.setSize(bw * 0.8f, bh * 0.8f);
        datasetManagerButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                addDatasetManagerWindow(serverDatasets);
            }
            return true;
        });
        Table datasetManagerInfo = new Table(skin);
        OwnLabel downloadLabel = new OwnLabel(I18n.txt("gui.welcome.dsmanager.desc"), skin, textStyle);
        datasetManagerInfo.add(downloadLabel).top().left().padBottom(pad16);
        if (serverDatasets != null && serverDatasets.updatesAvailable) {
            datasetManagerInfo.row();
            OwnLabel updates = new OwnLabel(I18n.txt("gui.welcome.dsmanager.updates", serverDatasets.numUpdates), skin, textStyle);
            updates.setColor(ColorUtils.gYellowC);
            datasetManagerInfo.add(updates).bottom().left();
        } else if (!baseDataPresent) {
            datasetManagerInfo.row();
            OwnLabel getBasedata = new OwnLabel(I18n.txt("gui.welcome.dsmanager.info"), skin, textStyle);
            getBasedata.setColor(ColorUtils.gGreenC);
            datasetManagerInfo.add(getBasedata).bottom().left();
        }

        // Selection problems/issues
        Table selectionInfo = new Table(skin);
        if (numGaiaDRCatalogsSelected > 1) {
            OwnLabel tooManyDR = new OwnLabel(I18n.txt("gui.welcome.catalogsel.manydrcatalogs"), skin, textStyle);
            tooManyDR.setColor(ColorUtils.gRedC);
            selectionInfo.add(tooManyDR);
        } else if (numStarCatalogsSelected > 1) {
            OwnLabel warn2Star = new OwnLabel(I18n.txt("gui.welcome.catalogsel.manystarcatalogs"), skin, textStyle);
            warn2Star.setColor(ColorUtils.aOrangeC);
            selectionInfo.add(warn2Star);
        } else if (numStarCatalogsSelected == 0) {
            OwnLabel noStarCatalogs = new OwnLabel(I18n.txt("gui.welcome.catalogsel.nostarcatalogs"), skin, textStyle);
            noStarCatalogs.setColor(ColorUtils.aOrangeC);
            selectionInfo.add(noStarCatalogs);
        }

        // Exit button
        OwnTextIconButton quitButton = new OwnTextIconButton(I18n.txt("gui.exit"), skin, "quit");
        quitButton.setSpace(pad16);
        quitButton.align(Align.center);
        quitButton.setSize(bw * 0.5f, bh * 0.6f);
        quitButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Title
        center.add(titleGroup).center().padBottom(pad18 * 6f).colspan(2).row();

        // Start button
        center.add(startButton).right().top().padBottom(pad18 * 10f).padRight(pad28 * 2f);
        center.add(startGroup).top().left().padBottom(pad18 * 10f).row();

        // Dataset manager
        center.add(datasetManagerButton).right().top().padBottom(pad32).padRight(pad28 * 2f);
        center.add(datasetManagerInfo).left().top().padBottom(pad32).row();

        center.add(selectionInfo).colspan(2).center().top().padBottom(pad32 * 4f).row();

        // Quit
        center.add(quitButton).center().top().colspan(2);

        // Version line table
        Table topLeft = new VersionLineTable(skin);

        ui.addActor(center);
        ui.addActor(topLeft);

        if (!baseDataPresent) {
            // Open dataset manager if base data is not there
            addDatasetManagerWindow(serverDatasets);
        } else {
            // Check if there is an update for the base data, and show a notice if so
            if (serverDatasets != null && serverDatasets.updatesAvailable) {
                DatasetDesc baseData = serverDatasets.findDataset("default-data");
                if (baseData != null && baseData.myVersion < baseData.serverVersion) {
                    // We have a base data update, show notice
                    GenericDialog baseDataNotice = new GenericDialog(I18n.txt("gui.basedata.title"), skin, ui) {

                        @Override
                        protected void build() {
                            content.clear();
                            content.pad(pad20, pad28 * 2f, pad20, pad28 * 2f);
                            content.add(new OwnLabel(I18n.txt("gui.basedata.default", baseData.name, I18n.txt("gui.welcome.dsmanager")), skin, "msg-24")).left().colspan(3).padBottom(pad20 * 2f).row();
                            content.add(new OwnLabel(I18n.txt("gui.basedata.version", baseData.myVersion), skin, "header-large")).center().padRight(pad20);
                            content.add(new OwnLabel("->", skin, "main-title-s")).center().padRight(pad20);
                            content.add(new OwnLabel(I18n.txt("gui.basedata.version", baseData.serverVersion), skin, "header-large")).center().padRight(pad20);
                        }

                        @Override
                        protected void accept() {
                            // Nothing
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
                    baseDataNotice.setAcceptText(I18n.txt("gui.ok"));
                    baseDataNotice.buildSuper();
                    baseDataNotice.show(ui);
                }
            }
        }
    }

    private void gaiaSky() {
        if (bgTex != null)
            bgTex.dispose();
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        EventManager.instance.post(Events.LOAD_DATA_CMD);
    }

    /**
     * Reloads the view completely
     */
    private void reloadView() {
        reloadLocalDatasets();
        clearGui();
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        buildWelcomeUI();
    }

    private void reloadLocalDatasets() {
        this.localDatasets = DataDescriptorUtils.instance().buildLocalDatasets(null);
    }

    private int numTotalCatalogsSelected() {
        return Settings.settings.data.catalogFiles.size();
    }

    private int numCatalogsAvailable() {
        return this.localDatasets != null ? this.localDatasets.datasets.size() : 0;
    }

    private int numGaiaDRCatalogsSelected() {
        int matches = 0;
        for (String f : Settings.settings.data.catalogFiles) {
            String filename = Path.of(f).getFileName().toString();
            if (isGaiaDRCatalogFile(filename)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean isGaiaDRCatalogFile(String name) {
        return name.matches("^\\S*catalog-[e]?dr\\d+(int\\d+)?-\\S+(\\.json)$");
    }

    private int numStarCatalogsSelected() {
        int matches = 0;
        if (serverDatasets == null && localDatasets == null) {
            return 0;
        }

        for (String f : Settings.settings.data.catalogFiles) {
            // File name with no extension
            Path path = Path.of(f);
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
        for (String f : Settings.settings.data.catalogFiles) {
            // File name with no extension
            Path path = Path.of(f);
            if (!Files.exists(path)) {
                // File does not exist, remove from selected list!
                toRemove.add(f);
            }
        }

        // Remove non-existent files
        for (String out : toRemove) {
            Settings.settings.data.catalogFiles.remove(out);
        }

        return toRemove;
    }

    /**
     * Checks if the basic Gaia Sky data folders are present
     * in the default data folder
     *
     * @return True if basic data is found
     */
    private boolean baseDataPresent() {
        Array<Path> required = new Array<>();
        fillBasicDataFiles(required);

        for (Path p : required) {
            if (!Files.exists(p) || !Files.isReadable(p)) {
                logger.info("Data files not found: " + p);
                return false;
            }
        }

        return true;
    }

    private void fillBasicDataFiles(Array<Path> required) {
        Path dataPath = Paths.get(Settings.settings.data.location).normalize();
        required.add(dataPath.resolve("data-main.json"));
        required.add(dataPath.resolve("asteroids.json"));
        required.add(dataPath.resolve("planets.json"));
        required.add(dataPath.resolve("satellites.json"));
        required.add(dataPath.resolve("tex/base"));
        required.add(dataPath.resolve("attitudexml"));
        required.add(dataPath.resolve("orbit"));
        required.add(dataPath.resolve("oort"));
        required.add(dataPath.resolve("galaxy"));
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDatasetManagerWindow(DataDescriptor dd) {
        if (ddw == null) {
            ddw = new DatasetManagerWindow(ui, skin, dd);
            ddw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                reloadView();
            });
        } else {
            ddw.refresh();
        }
        ddw.show(ui);
    }

    public void clearGui() {
        if (ui != null) {
            ui.clear();
        }
        if (ddw != null) {
            ddw.remove();
        }
    }

    @Override
    protected void rebuildGui() {

    }

}
