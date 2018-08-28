package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

/**
 * Displays dataset downloader and dataset chooser screen if needed.
 * 
 * @author Toni Sagrista
 *
 */
public class InitialGui extends AbstractGui {

    protected DownloadCatalogWindow ddw;
    protected ChooseCatalogWindow cdw;

    /** Lock object for synchronisation **/

    public InitialGui() {
        lock = new Object();
    }

    @Override
    public void initialize(AssetManager assetManager) {

        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        String assetsLoc = System.getProperty("assets.location") != null ? System.getProperty("assets.location") : "";
        DatasetsWidget dw = new DatasetsWidget(skin, assetsLoc);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();

        clearGui();

        if (catalogFiles.size != 0) {
            // No catalog files, display downloader
            addDatasetDownloader();
        } else {
            displayChooser();
        }

    }

    private void displayChooser() {
        clearGui();
        if (GlobalConf.program.DISPLAY_DATASET_DIALOG) {
            addDatasetChooser();
        } else {
            // Event
            EventManager.instance.post(Events.LOAD_DATA_CMD);
        }

    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    private void addDatasetDownloader() {
        if (ddw == null) {
            ddw = new DownloadCatalogWindow(ui, skin);
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
