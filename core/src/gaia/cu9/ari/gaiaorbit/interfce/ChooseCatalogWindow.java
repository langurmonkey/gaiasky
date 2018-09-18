package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;

/**
 * GUI window to choose the catalog to use
 * @author tsagrista
 *
 */
public class ChooseCatalogWindow extends GenericDialog {

    private DatasetsWidget dw;
    private String assetsLoc;

    public ChooseCatalogWindow(Stage stage, Skin skin) {
        super(txt("gui.dschooser.title"), skin, stage);
        assetsLoc = GlobalConf.ASSETS_LOC;

        setCancelText(txt("gui.exit"));
        setAcceptText(txt("gui.start"));

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        Cell<Actor> cell = content.add((Actor) null);

        dw = new DatasetsWidget(skin, assetsLoc);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();

        cell.clearActor();
        cell.space(3 * GlobalConf.SCALE_FACTOR);
        cell.padTop(10 * GlobalConf.SCALE_FACTOR);
        cell.setActor(dw.buildDatasetsWidget(catalogFiles));
    }

    @Override
    protected void accept() {
        boolean first = true;
        // Update setting
        if (dw != null && dw.cbs != null) {
            GlobalConf.data.CATALOG_JSON_FILES = "";
            for (Button b : dw.cbs) {
                if (b.isChecked()) {
                    // Add all selected to list
                    String candidate = dw.candidates.get(b);
                    if (!first) {
                        GlobalConf.data.CATALOG_JSON_FILES += "," + candidate;
                    } else {
                        GlobalConf.data.CATALOG_JSON_FILES += candidate;
                        first = false;
                    }
                }
            }
        }
        // No change to execute exit event, manually restore cursor to default
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        // Data load can start
        EventManager.instance.post(Events.LOAD_DATA_CMD);
    }

    @Override
    protected void cancel() {
        Gdx.app.exit();
    }

}
