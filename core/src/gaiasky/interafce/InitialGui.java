/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
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

        } else if(GlobalConf.program.isSlave()){
            // If slave, data load can start
            EventManager.instance.post(Events.LOAD_DATA_CMD);
        } else {
            // Otherwise, check for updates, etc.
            DatasetsWidget dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
            Array<FileHandle> catalogFiles = dw.buildCatalogFiles();

            clearGui();

            FileHandle dataDescriptor = Gdx.files.absolute(SysUtils.getDefaultTmpDir() + "/gaiasky-data.json");
            DownloadHelper.downloadFile(GlobalConf.program.DATA_DESCRIPTOR_URL, dataDescriptor, null, (digest) -> {
                GaiaSky.postRunnable(() -> {
                    /**
                     * Display download manager if:
                     * - force display (args), or
                     * - base data not found, or
                     * - no catalogs found in data folder, or
                     * - new versions of current datasets found
                     */
                    DataDescriptor dd = DataDescriptorUtils.instance().buildDatasetsDescriptor(dataDescriptor);
                    if (datasetsDownload || !basicDataPresent() || catalogFiles.size == 0 || dd.updatesAvailable) {
                        // No catalog files, display downloader
                        addDownloaderWindow(dd);
                    } else {
                        displayChooser();
                    }
                });
            }, () -> {
                // Fail?
                logger.error("No internet connection or server is down! We will attempt to continue");
                if (basicDataPresent()) {
                    // Go on all in
                    GaiaSky.postRunnable(() -> {
                        GuiUtils.addNoConnectionWindow(skin, ui, () -> displayChooser());
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

    private boolean isCatalogSelected() {
        return GlobalConf.data.CATALOG_JSON_FILES != null && GlobalConf.data.CATALOG_JSON_FILES.size > 0;
    }

    private void displayChooser() {
        DatasetsWidget dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();
        clearGui();
        /**
         * Display chooser if:
         * - force display (args), or
         * - show criterion is 'always' (conf)
         * - catalogs available and no catalogs are selected
         */
        if (catalogChooser || GlobalConf.program.CATALOG_CHOOSER.always() || (catalogFiles.size > 0 && (!isCatalogSelected() && !GlobalConf.program.CATALOG_CHOOSER.never()))) {
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

    private void addDownloaderWindow(DataDescriptor dd) {
        if (ddw == null) {
            ddw = new DownloadDataWindow(ui, skin, dd);
            ddw.setAcceptRunnable(() -> {
                Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                displayChooser();
            });
            ddw.setCancelRunnable(() -> Gdx.app.exit());
        }
        ddw.show(ui);
    }

    private void addDatasetChooser(String noticeKey) {
        if (cdw == null)
            cdw = new CatalogChooserWindow(ui, skin, noticeKey);
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
