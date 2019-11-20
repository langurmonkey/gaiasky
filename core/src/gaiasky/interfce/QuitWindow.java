/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnLabel;

/**
 * Generic dialog that displays the confirmation quit message and offers options
 * to cancel it or go through.
 *
 * @author tsagrista
 */
public class QuitWindow extends GenericDialog {

    public QuitWindow(Stage ui, Skin skin) {
        super(I18n.txt("gui.quit.title"), skin, ui);

        setAcceptText(I18n.txt("gui.yes"));
        setCancelText(I18n.txt("gui.no"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        content.add(new OwnLabel(I18n.txt("gui.quit.sure"), skin)).left().pad(pad5).row();
    }

    @Override
    protected void accept() {
        // Only run if it does not have an accept runnable already
        // Otherwise, it comes from the exit hook
        GaiaSky.postRunnable(() -> Gdx.app.exit());
    }

    @Override
    protected void cancel() {
        // Do nothing
    }

}
