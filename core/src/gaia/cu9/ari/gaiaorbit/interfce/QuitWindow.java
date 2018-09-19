package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

/**
 * Generic dialog that displays the confirmation quit message and offers options
 * to cancel it or go through.
 * @author tsagrista
 *
 */
public class QuitWindow extends GenericDialog {

    public QuitWindow(Stage ui, Skin skin) {
        super(txt("gui.quit.title"), skin, ui);

        setAcceptText(txt("gui.yes"));
        setCancelText(txt("gui.no"));
        
        buildSuper();
    }
    
    @Override
    protected void build() {
        content.clear();
       
        content.add(new OwnLabel(txt("gui.quit.sure"), skin)).left().pad(pad).row();
    }

    @Override
    protected void accept() {
        Gdx.app.postRunnable(()->{
            Gdx.app.exit();
        });
    }

    @Override
    protected void cancel() {
        // Do nothing
    }

}
