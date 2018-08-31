package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf.ProgramConf.StereoProfile;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

/**
 * Full OpenGL GUI with all the controls and whistles.
 * 
 * @author Toni Sagrista
 *
 */
public class StereoGui extends AbstractGui {
    private Skin skin;

    protected NotificationsInterface notificationsOne, notificationsTwo;

    protected INumberFormat nf;

    public StereoGui() {
        super();
    }

    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
    }

    /**
     * Constructs the interface
     */
    public void doneLoading(AssetManager assetManager) {
        Logger.getLogger(this.getClass()).info(txt("notif.gui.init"));

        interfaces = new Array<IGuiInterface>();

        skin = GlobalResources.skin;

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, Events.STEREO_PROFILE_CMD);
    }

    private void buildGui() {
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }

        nf = NumberFormatFactory.getFormatter("##0.###");

        // NOTIFICATIONS ONE - BOTTOM LEFT
        notificationsOne = new NotificationsInterface(skin, lock, true, true, false, false);
        notificationsOne.setFillParent(true);
        notificationsOne.left().bottom();
        notificationsOne.pad(0, 5, 5, 0);
        interfaces.add(notificationsOne);

        // NOTIFICATIONS TWO - BOTTOM CENTRE
        notificationsTwo = new NotificationsInterface(skin, lock, true, true, false, false);
        notificationsTwo.setFillParent(true);
        notificationsTwo.bottom();
        notificationsTwo.setX(Gdx.graphics.getWidth() / 2);
        notificationsTwo.pad(0, 5, 5, 0);
        interfaces.add(notificationsTwo);

        /** ADD TO UI **/
        rebuildGui();

    }

    protected void rebuildGui() {

        if (ui != null) {
            ui.clear();
            if (notificationsOne != null)
                ui.addActor(notificationsOne);
            if (notificationsTwo != null)
                ui.addActor(notificationsTwo);

        }
    }

    /**
     * Removes the focus from this Gui and returns true if the focus was in the
     * GUI, false otherwise.
     * 
     * @return true if the focus was in the GUI, false otherwise.
     */
    public boolean cancelTouchFocus() {
        if (ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public void update(double dt) {
        notificationsTwo.setX(notificationsTwo.getMessagesWidth() / 2);
        ui.act((float) dt);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case STEREO_PROFILE_CMD:
            StereoProfile profile = StereoProfile.values()[(Integer) data[0]];
            if (profile == StereoProfile.ANAGLYPHIC) {
                notificationsTwo.setVisible(false);
            } else {
                notificationsTwo.setVisible(true);
            }
            break;
        default:
            break;
        }
    }

}
