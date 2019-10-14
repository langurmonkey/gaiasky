/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.CatalogInfo;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;

import java.time.ZoneId;

public class DatasetPreferencesWindow extends GenericDialog {

    private final CatalogInfo ci;
    private OwnTextField pointSize;
    private OwnCheckBox allVisible;

    public DatasetPreferencesWindow(CatalogInfo ci, Skin skin, Stage stage){
        super(I18n.txt("gui.preferences") + " - " + ci.name, skin, stage);
        this.ci = ci;

        setAcceptText(I18n.txt("gui.ok"));
        setCancelText(I18n.txt("gui.cancel"));
        setModal(false);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        float selectWidth = 100f * GlobalConf.UI_SCALE_FACTOR;

        // Name
        content.add(new OwnLabel(I18n.txt("gui.dataset.name"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad);
        content.add(new OwnLabel(ci.name, skin)).left().padRight(pad).padBottom(pad).row();
        // Type
        content.add(new OwnLabel(I18n.txt("gui.dataset.type"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad);
        content.add(new OwnLabel(ci.type.toString(), skin)).left().padRight(pad).padBottom(pad).row();
        // Added
        content.add(new OwnLabel(I18n.txt("gui.dataset.loaded"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad);
        content.add(new OwnLabel(ci.loadDateUTC.atZone(ZoneId.systemDefault()).toString(), skin)).left().padRight(pad).padBottom(pad).row();
        // Desc
        content.add(new OwnLabel(I18n.txt("gui.dataset.description"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad * 2f);
        content.add(new OwnLabel(TextUtils.capString(ci.description, 55), skin)).left().padRight(pad).padBottom(pad * 2f).row();

        // Highlight
        content.add(new OwnLabel(I18n.txt("gui.dataset.highlight"), skin, "hud-header")).left().colspan(2).padBottom(pad).row();

        // Point size
        IValidator pointSizeValidator = new FloatValidator(0.5f, 5.0f);
        pointSize = new OwnTextField(Float.toString(ci.hlSizeFactor), skin, pointSizeValidator);
        content.add(new OwnLabel(I18n.txt("gui.dataset.highlight.size"), skin)).right().padRight(pad).padBottom(pad);
        content.add(pointSize).left().padRight(pad).padBottom(pad).row();

        // All visible
        allVisible = new OwnCheckBox(I18n.txt("gui.dataset.highlight.allvisible"), skin, pad);
        allVisible.setChecked(ci.hlAllVisible);
        content.add(allVisible).left().colspan(2).padBottom(pad);


    }

    @Override
    protected void accept() {
        // Point size
        if(pointSize.isValid()) {
            float newVal = Parser.parseFloat(pointSize.getText());
            if(newVal != ci.hlSizeFactor) {
                ci.setHlSizeFactor(newVal);
            }
        }
        // All visible
        boolean vis = allVisible.isChecked();
        if(vis != ci.hlAllVisible){
            ci.setHlAllVisible(vis);
        }

    }

    @Override
    protected void cancel() {
        // Just close
    }
}
