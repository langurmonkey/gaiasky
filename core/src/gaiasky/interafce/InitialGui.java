/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
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
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.vr.openvr.VRStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Displays dataset downloader and dataset chooser screen if needed.
 *
 * @author Toni Sagrista
 */
public class InitialGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(InitialGui.class);

    private boolean datasetsDownload, catalogChooser;
    private VRStatus vrStatus;

    protected DownloadDataWindow ddw;
    protected CatalogChooserWindow cdw;

    private FileHandle dataDescriptor;
    private Array<FileHandle> catalogFiles;

    private boolean downloadError = false;
    private Texture bgTex;
    private DatasetsWidget dw;

    /** Lock object for synchronisation **/

    /**
     * Creates an initial GUI
     *
     * @param datasetsDownload Forces dataset download window
     * @param catalogChooser   Forces catalog chooser window
     * @param vrStatus         The status of VR
     */
    public InitialGui(boolean datasetsDownload, boolean catalogChooser, VRStatus vrStatus) {
        lock = new Object();
        this.catalogChooser = catalogChooser;
        this.datasetsDownload = datasetsDownload;
        this.vrStatus = vrStatus;
    }

    @Override
    public void initialize(AssetManager assetManager) {

        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        if (vrStatus.vrInitFailed()) {
            if (vrStatus.equals(VRStatus.ERROR_NO_CONTEXT))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRConnectionExit(skin, ui));
            else if (vrStatus.equals(VRStatus.ERROR_RENDERMODEL))
                GaiaSky.postRunnable(() -> GuiUtils.addNoVRDataExit(skin, ui));

        } else if (GlobalConf.program.isSlave()) {
            // If slave, data load can start
            EventManager.instance.post(Events.LOAD_DATA_CMD);
        } else {
            dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
            catalogFiles = dw.buildCatalogFiles();

            // Otherwise, check for updates, etc.
            clearGui();

            dataDescriptor = Gdx.files.absolute(SysUtils.getDefaultTmpDir() + "/gaiasky-data.json");
            DownloadHelper.downloadFile(GlobalConf.program.DATA_DESCRIPTOR_URL, dataDescriptor, null, (digest) -> {
                GaiaSky.postRunnable(() -> {
                    // Data descriptor ok
                    buildWelcomeUI();
                });
            }, () -> {
                // Fail?
                downloadError = true;
                logger.error("No internet connection or server is down! We will attempt to continue");
                if (basicDataPresent()) {
                    // Go on all in
                    GaiaSky.postRunnable(() -> {
                        GuiUtils.addNoConnectionWindow(skin, ui, () -> buildWelcomeUI());
                    });
                } else {
                    // Error and exit
                    logger.error("No base data present - need an internet connection to continue, exiting");
                    GaiaSky.postRunnable(() -> {
                        GuiUtils.addNoConnectionExit(skin, ui);
                    });
                }
            }, null);

        }
    }

    private void buildWelcomeUI() {
        final DataDescriptor dd = !downloadError ? DataDescriptorUtils.instance().buildDatasetsDescriptor(dataDescriptor) : null;
        // Center table
        Table center = new Table(skin);
        center.setFillParent(true);
        center.center();
        if (bgTex == null)
            bgTex = new Texture(Gdx.files.internal("img/splash/splash.jpg"));
        Drawable bg = new SpriteDrawable(new Sprite(bgTex));
        center.setBackground(bg);

        float pad5 = 5f * GlobalConf.UI_SCALE_FACTOR;
        float pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
        float pad15 = 15f * GlobalConf.UI_SCALE_FACTOR;
        float pad20 = 20f * GlobalConf.UI_SCALE_FACTOR;
        float pad25 = 25f * GlobalConf.UI_SCALE_FACTOR;

        float bw = 350f * GlobalConf.UI_SCALE_FACTOR;
        float bh = 85f * GlobalConf.UI_SCALE_FACTOR;

        // Title
        OwnLabel title = new OwnLabel("Welcome to Gaia Sky " + GlobalConf.version.version, skin, "main-title");
        Color theme = skin.getColor("theme");
        title.setColor(theme);

        String textStyle = "main-title-s";

        // Data downloader
        OwnTextIconButton downloadButton = new OwnTextIconButton("Dataset manager", skin, "cloud-download");
        downloadButton.setSpace(pad15);
        downloadButton.setContentAlign(Align.center);
        downloadButton.align(Align.center);
        downloadButton.setSize(bw, bh);
        downloadButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                addDatasetManagerWindow(dd);
            }
            return true;
        });
        Table downloadGroup = new Table(skin);
        OwnLabel downloadLabel = new OwnLabel("Download, update and manage your datasets.\nChoose this if it is the first time launching Gaia Sky.", skin, textStyle);
        downloadGroup.add(downloadLabel).top().left().padBottom(pad10);
        if (dd != null && dd.updatesAvailable) {
            downloadGroup.row();
            OwnLabel updates = new OwnLabel(dd.numUpdates + " update(s) available!", skin, textStyle);
            updates.setColor(ColorUtils.gYellowC);
            downloadGroup.add(updates).bottom().left();
        } else if (!basicDataPresent()) {
            downloadGroup.row();
            OwnLabel getBasedata = new OwnLabel("Download the base-data package and some catalogs here!", skin, textStyle);
            getBasedata.setColor(ColorUtils.gGreenC);
            downloadGroup.add(getBasedata).bottom().left();
        }

        // Catalog chooser
        OwnTextIconButton catalogButton = new OwnTextIconButton("Catalog selection", skin, "check");
        catalogButton.setSpace(pad15);
        catalogButton.setContentAlign(Align.center);
        catalogButton.align(Align.center);
        catalogButton.setSize(bw, bh);
        catalogButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                String noticeKey;
                if (catalogFiles.size > 0 && numCatalogDRFiles() > 1) {
                    noticeKey = "gui.dschooser.morethanonedr";
                } else {
                    noticeKey = "gui.dschooser.nocatselected";
                }
                addCatalogSelectionWindow(noticeKey);
            }
            return true;
        });
        Table catalogGroup = new Table(skin);
        OwnLabel catalogLabel = new OwnLabel("Choose which catalog(s) to load at startup.\nWatch out! Only one Gaia LOD dataset should be selected!", skin, textStyle);
        catalogGroup.add(catalogLabel).top().left().padBottom(pad10).row();
        if (catalogFiles.size == 0) {
            // No catalog files, disable and add notice
            catalogButton.setDisabled(true);

            OwnLabel noCatalogs = new OwnLabel("You have no catalog files yet, use the 'Dataset manager' to get some!", skin, textStyle);
            noCatalogs.setColor(ColorUtils.aOrangeC);
            catalogGroup.add(noCatalogs).bottom().left();
        } else {
            OwnLabel ok = new OwnLabel(numCatalogsSelected() + " catalog(s) selected", skin, textStyle);
            ok.setColor(skin.getColor("theme"));
            catalogGroup.add(ok).bottom().left();
        }

        // Start
        OwnTextIconButton startButton = new OwnTextIconButton("Start Gaia Sky", skin, "start");
        startButton.setSpace(pad15);
        startButton.setContentAlign(Align.center);
        startButton.align(Align.center);
        startButton.setSize(bw, bh);
        startButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                bgTex.dispose();
                EventManager.instance.post(Events.LOAD_DATA_CMD);
            }
            return true;
        });
        Table startGroup = new Table(skin);
        OwnLabel startLabel = new OwnLabel("Launch Gaia Sky and start the action.", skin, textStyle);
        startGroup.add(startLabel).top().left().padBottom(pad10).row();
        if (!basicDataPresent()) {
            // No basic data, can't start!
            startButton.setDisabled(true);

            OwnLabel noBaseData = new OwnLabel("Base data package not found, get it with the 'Dataset manager'!", skin, textStyle);
            noBaseData.setColor(ColorUtils.aOrangeC);
            startGroup.add(noBaseData).bottom().left();
        } else if(catalogFiles.size > 0 && numCatalogsSelected() == 0){
            OwnLabel noCatsSelected = new OwnLabel("You have not selected any of the downloaded catalogs, use 'Catalog selection' to do so!", skin, textStyle);
            noCatsSelected.setColor(ColorUtils.aOrangeC);
            startGroup.add(noCatsSelected).bottom().left();
        } else {
            OwnLabel ready = new OwnLabel("Ready to go!", skin, textStyle);
            ready.setColor(ColorUtils.gGreenC);
            startGroup.add(ready).bottom().left();

        }

        // Quit
        OwnTextIconButton quitButton = new OwnTextIconButton("Quit", skin, "quit");
        quitButton.setSpace(pad10);
        quitButton.align(Align.center);
        quitButton.setSize(bw * 0.5f, bh * 0.6f);
        quitButton.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        center.add(title).center().padBottom(pad15 * 5f).colspan(2).row();

        center.add(startButton).center().top().padBottom(pad15 * 6f).padRight(pad25);
        center.add(startGroup).top().left().padBottom(pad15 * 6f).row();

        center.add(downloadButton).center().top().padBottom(pad20).padRight(pad25);
        center.add(downloadGroup).left().top().padBottom(pad20).row();

        center.add(catalogButton).center().top().padBottom(pad15 * 14f).padRight(pad25);
        center.add(catalogGroup).left().top().padBottom(pad15 * 14f).row();

        center.add(quitButton).center().top().colspan(2);

        // Bottom left table
        Table bottomRight = new Table();
        bottomRight.setFillParent(true);
        bottomRight.right().bottom();
        bottomRight.pad(pad10);
        bottomRight.add(new OwnLabel(GlobalConf.version.version + " - build " + GlobalConf.version.build, skin, "hud-med"));

        ui.addActor(center);
        ui.addActor(bottomRight);

    }

    /**
     * Reloads the view completely
     */
    private void reloadView(){
        if(dw == null){
            dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
        }
        catalogFiles = dw.buildCatalogFiles();
        clearGui();
        buildWelcomeUI();
    }

    private void regularGaiaSky(Array<FileHandle> catalogFiles) {

        /**
         * Display download manager if:
         * - force display (args), or
         * - base data not found, or
         * - no catalogs found in data folder, or
         * - new versions of current datasets found
         */
        try {
            DataDescriptor dd = DataDescriptorUtils.instance().buildDatasetsDescriptor(dataDescriptor);
            if (datasetsDownload || !basicDataPresent() || catalogFiles.size == 0 || dd.updatesAvailable) {
                // No catalog files, display downloader
                addDatasetManagerWindow(dd);
            } else {
                addCatalogSelectionWindow();
            }
        } catch (Exception e) {
            logger.error(e);
            logger.error("Error building data descriptor from URL: " + GlobalConf.program.DATA_DESCRIPTOR_URL);
            if (GlobalConf.program.DATA_DESCRIPTOR_URL.contains("http://")) {
                logger.info("You are using HTTP but the server may be HTTPS - please check your URL in the properties file");
            }
            addCatalogSelectionWindow();
        }

    }

    private boolean isCatalogSelected() {
        return GlobalConf.data.CATALOG_JSON_FILES != null && GlobalConf.data.CATALOG_JSON_FILES.size > 0;
    }

    private int numCatalogsSelected() {
        return GlobalConf.data.CATALOG_JSON_FILES.size;
    }

    private int numCatalogDRFiles() {
        int matches = 0;
        for (String f : GlobalConf.data.CATALOG_JSON_FILES) {
            if (f.matches("^\\S*catalog-[e]?dr\\d+-\\S+$")) {
                matches++;
            }
        }
        return matches;
    }

    private void addCatalogSelectionWindow() {
        DatasetsWidget dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();
        /**
         * Display chooser if:
         * - force display (args), or
         * - show criterion is 'always' (conf)
         * - catalogs available and no catalogs are selected
         * - catalogs available and more than one xDRx (DR1, DR2, eDR3, ...) catalog selected
         */
        if (catalogChooser || GlobalConf.program.CATALOG_CHOOSER.always() || (catalogFiles.size > 0 && (!isCatalogSelected() && !GlobalConf.program.CATALOG_CHOOSER.never())) || (catalogFiles.size > 0 && (numCatalogDRFiles() > 1 && !GlobalConf.program.CATALOG_CHOOSER.never()))) {
            String noticeKey;
            if (catalogFiles.size > 0 && numCatalogDRFiles() > 1) {
                noticeKey = "gui.dschooser.morethanonedr";
            } else {
                noticeKey = "gui.dschooser.nocatselected";
            }
            addCatalogSelectionWindow(noticeKey);
        } else {
            // Event
            EventManager.instance.post(Events.LOAD_DATA_CMD);
        }

    }

    /**
     * Checks if the basic Gaia Sky data folders are present
     * in the default data folder
     *
     * @return
     */
    private boolean basicDataPresent() {
        Path dataPath = Paths.get(GlobalConf.data.DATA_LOCATION).normalize();
        // Add all paths to check in this list
        Array<Path> required = new Array<>();
        required.add(dataPath.resolve("data-main.json"));
        required.add(dataPath.resolve("asteroids.json"));
        required.add(dataPath.resolve("planets.json"));
        required.add(dataPath.resolve("satellites.json"));
        required.add(dataPath.resolve("tex"));
        required.add(dataPath.resolve("attitudexml"));
        required.add(dataPath.resolve("meshes"));

        for (Path p : required) {
            if (!Files.exists(p) || !Files.isReadable(p)) {
                logger.info("Data files not found: " + p.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDatasetManagerWindow(DataDescriptor dd) {
        if (ddw == null) {
            ddw = new DownloadDataWindow(ui, skin, dd);
            ddw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                reloadView();
            });
        }
        ddw.show(ui);
    }

    private void addCatalogSelectionWindow(String noticeKey) {
        if (cdw == null) {
            cdw = new CatalogChooserWindow(ui, skin, noticeKey);
            cdw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                reloadView();
            });
        }
        cdw.show(ui);
    }

    public void clearGui() {
        if (ui != null) {
            ui.clear();
        }
        if (ddw != null) {
            ddw.remove();
        }
        if (cdw != null) {
            cdw.remove();
        }
    }

    @Override
    protected void rebuildGui() {

    }

}
