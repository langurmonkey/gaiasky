/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FolderValidator;
import gaiasky.util.validator.IValidator;

public class NewBookmarkFolderDialog extends GenericDialog {
    private static final Log logger = Logger.getLogger(NewBookmarkFolderDialog.class);

    public OwnTextField input;

    public NewBookmarkFolderDialog(String parent, Skin skin, Stage ui) {
        super(I18n.txt("gui.bookmark.context.newfolder", parent), skin, ui);

        setAcceptText(I18n.txt("gui.ok"));
        setCancelText(I18n.txt("gui.cancel"));

        // Build
        buildSuper();

        // Pack
        pack();
    }
    public void build() {
        // Info message
        IValidator val = new FolderValidator();
        input = new OwnTextField("", skin, val);
        input.setWidth(480f);
        input.setMessageText("New folder");

        content.add(input).top().left().expand().row();
    }

    @Override
    public void accept(){
        stage.unfocusAll();
    }
    @Override
    public void cancel(){
        stage.unfocusAll();
    }

    @Override
    public GenericDialog show(Stage stage, Action action) {
        GenericDialog gd = super.show(stage, action);
        // FOCUS_MODE to input
        stage.setKeyboardFocus(input);
        return gd;
    }

    @Override
    public void dispose() {

    }

}
