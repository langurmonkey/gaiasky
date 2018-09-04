package gaia.cu9.ari.gaiaorbit.interfce;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.DownloadHelper;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

/**
 * Displays dataset downloader and dataset chooser screen if needed.
 * 
 * @author Toni Sagrista
 *
 */
public class InitialGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(InitialGui.class);

    private boolean dsdownload, catchooser;
    
    protected DownloadDataWindow ddw;
    protected ChooseCatalogWindow cdw;

    /** Lock object for synchronisation **/

    /**
     * Creates an initial GUI
     * @param dsdownload Forces dataset download window
     * @param catchooser Forces catalog chooser window
     */
    public InitialGui(boolean dsdownload, boolean catchooser) {
        lock = new Object();
        this.catchooser = catchooser;
        this.dsdownload = dsdownload;
    }

    @Override
    public void initialize(AssetManager assetManager) {

        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        DatasetsWidget dw = new DatasetsWidget(skin, GlobalConf.ASSETS_LOC);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();

        clearGui();

        DownloadHelper.downloadFile(GlobalConf.program.DATA_DESCRIPTOR_URL, Gdx.files.absolute(SysUtils.getDefaultTmpDir() + "/gaiasky-data.json"), 
                null, 
                (md5sum) -> {
                    Gdx.app.postRunnable(() -> {
                        if (dsdownload || !basicDataPresent() || catalogFiles.size == 0) {
                            // No catalog files, display downloader
                            addDownloaderWindow();
                        } else {
                            displayChooser();
                        }
                    });
                }, 
                null, 
                null);

    }

    private void displayChooser() {
        clearGui();
        if (catchooser || GlobalConf.program.DISPLAY_DATASET_DIALOG) {
            addDatasetChooser();
        } else {
            // Event
            EventManager.instance.post(Events.LOAD_DATA_CMD);
        }

    }

    /**
     * Checks if the basic Gaia Sky data folders are present
     * in the default data folder
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
        required.add(dataPath.resolve("sdss"));

        for (Path p : required) {
            if (!Files.exists(p) || !Files.isReadable(p)) {
                logger.info("Data files not found: " + dataPath.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDownloaderWindow() {
        if (ddw == null) {
            ddw = new DownloadDataWindow(ui, skin);
            ddw.setAcceptRunnable(() -> {
                displayChooser();
            });
        }
        ddw.show(ui);
    }

    private void addDatasetChooser() {
        if (cdw == null)
            cdw = new ChooseCatalogWindow(ui, skin);
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
