/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

/**
 * GUI window to choose the catalogs to load at startup.
 * This is shown at startup if no catalogs are selected and {@link GlobalConf#program#SKIP_CATALOG_CHOOSER} is false.
 * @author tsagrista
 *
 */
public class CatalogSelectionWindow extends GenericDialog {

    private DatasetsWidget dw;
    private String notice;

    public CatalogSelectionWindow(Stage stage, Skin skin){
        this(stage, skin, null);
    }

    public CatalogSelectionWindow(Stage stage, Skin skin, String noticeKey) {
        super(I18n.txt("gui.dschooser.title"), skin, stage);
        this.notice = I18n.txt(noticeKey);

        setAcceptText(I18n.txt("gui.ok"));

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        if(notice != null && !notice.isEmpty()){
            OwnImageButton tooltip = new OwnImageButton(skin, "tooltip");
            tooltip.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.catselection"), skin));
            HorizontalGroup hg = new HorizontalGroup();
            hg.space(pad10);
            hg.addActor(tooltip);
            hg.addActor(new OwnLabel(notice, skin));
            content.add(hg).left().pad(pad15).row();
        }

        Cell<Actor> cell = content.add((Actor) null);

        dw = new DatasetsWidget(skin);
        Array<FileHandle> catalogFiles = dw.buildCatalogFiles();

        cell.clearActor();
        cell.space(3f * GlobalConf.UI_SCALE_FACTOR);
        cell.padTop(pad5);
        cell.padLeft(pad20).padRight(pad20);
        cell.setActor(dw.buildDatasetsWidget(catalogFiles));

    }

    public void refresh(){
        content.clear();
        bottom.clear();
        build();
    }

    @Override
    protected void accept() {
        // Update setting
        if (dw != null && dw.cbs != null) {
            GlobalConf.data.CATALOG_JSON_FILES.clear();
            for (Button b : dw.cbs) {
                if (b.isChecked()) {
                    // Add all selected to list
                    String candidate = dw.candidates.get(b);
                    GlobalConf.data.CATALOG_JSON_FILES.add(candidate);
                }
            }
        }
        // No change to execute exit event, manually restore cursor to default
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
    }

    @Override
    protected void cancel() {
    }

}
