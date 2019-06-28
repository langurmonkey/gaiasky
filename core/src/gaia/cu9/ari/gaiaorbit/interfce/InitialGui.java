/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.scene2d.Link;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

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

    protected DownloadDataWindow ddw;
    protected ChooseCatalogWindow cdw;

    /** Lock object for synchronisation **/

    /**
     * Creates an initial GUI
     *
     * @param datasetsDownload Forces dataset download window
     * @param catalogChooser Forces catalog chooser window
     */
    public InitialGui(boolean datasetsDownload, boolean catalogChooser) {
        lock = new Object();
        this.catalogChooser = catalogChooser;
        this.datasetsDownload = datasetsDownload;
    }

    @Override
    public void initialize(AssetManager assetManager) {

        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        DatasetsWidget dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();

        clearGui();

        FileHandle dataDescriptor = Gdx.files.absolute(SysUtils.getDefaultTmpDir() + "/gaiasky-data.json");
        DownloadHelper.downloadFile(GlobalConf.program.DATA_DESCRIPTOR_URL, dataDescriptor, null, (digest) -> {
            Gdx.app.postRunnable(() -> {
                /**
                 * Display download manager if:
                 * - force display (args), or
                 * - base data not found, or
                 * - no catalogs found in data folder
                 */
                if (datasetsDownload || !basicDataPresent() || catalogFiles.size == 0) {
                    // No catalog files, display downloader
                    addDownloaderWindow();
                } else {
                    displayChooser();
                }
            });
        }, () -> {
            // Fail?
            logger.error("No internet connection or server is down! We will attempt to continue");
            if (basicDataPresent()) {
                // Go on all in
                Gdx.app.postRunnable(() -> {
                    displayChooser();
                });
            } else {
                // Error and exit
                logger.error("No base data present - need an internet connection to continue, exiting");
                Gdx.app.postRunnable(() -> {
                    addExitWindow();
                });
            }
        }, null);

    }

    private boolean isCatalogSelected() {
        return GlobalConf.data.CATALOG_JSON_FILES != null && !GlobalConf.data.CATALOG_JSON_FILES.isEmpty();
    }

    private void displayChooser() {
        DatasetsWidget dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();
        clearGui();
        /**
         * Display chooser if:
         * - force display (args), or
         * - force display (conf), or
         * - catalogs available and yet no catalog is selected
         */
        if (catalogChooser || GlobalConf.program.DISPLAY_DATASET_DIALOG || (catalogFiles.size > 0 && !isCatalogSelected())) {
            String noticeKey = "gui.dschooser.nocatselected";
            addDatasetChooser(noticeKey);
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
        Array<Path> required = new Array<Path>();
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

    private void addExitWindow() {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", "No internet connection"), skin, ui) {

            @Override
            protected void build() {
                OwnLabel info = new OwnLabel("No internet connection and no base data found.\n" + "Gaia Sky will exit now", skin);
                Link manualDownload = new Link("Manual download", skin, "link", "http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
                content.add(info).pad(10).row();
                content.add(manualDownload).pad(10);
            }

            @Override
            protected void accept() {
                Gdx.app.exit();
            }

            @Override
            protected void cancel() {
                Gdx.app.exit();
            }

        };
        exitw.setAcceptText(I18n.txt("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(ui);
    }

    private void addDownloaderWindow() {
        if (ddw == null) {
            ddw = new DownloadDataWindow(ui, skin);
            ddw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                displayChooser();
            });
            ddw.setCancelRunnable(() -> {
                Gdx.app.exit();
            });
        }
        ddw.show(ui);
    }

    private void addDatasetChooser(String noticeKey) {
        if (cdw == null)
            cdw = new ChooseCatalogWindow(ui, skin, noticeKey);
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
