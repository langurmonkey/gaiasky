package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

/**
 * GUI for mobile devices which displays information and has some controls.
 * 
 * @author Toni Sagrista
 *
 */
public class MobileGui extends AbstractGui {

    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected DebugInterface debugInterface;
    protected RunStateInterface inputInterface;

    public MobileGui() {
        super();
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        skin = GlobalResources.skin;
        interfaces = new Array<IGuiInterface>();
        initialize();
    }

    private void initialize() {
        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        focusInterface.setFillParent(true);
        focusInterface.right().bottom();
        focusInterface.pad(0, 0, 5, 5);
        interfaces.add(focusInterface);

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.setFillParent(true);
        debugInterface.right().top();
        interfaces.add(debugInterface);

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true);
        notificationsInterface.setFillParent(true);
        notificationsInterface.left().bottom();
        notificationsInterface.pad(0, 5, 5, 0);
        interfaces.add(notificationsInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300, 150, 0);
        interfaces.add(messagesInterface);

        // INPUT STATE
        //	inputInterface = new ScriptStateInterface(skin);
        //	inputInterface.setFillParent(true);
        //	inputInterface.right().top();
        //	inputInterface.pad(50, 0, 0, 5);

        // Add to GUI
        rebuildGui();
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();

            if (debugInterface != null) {
                ui.addActor(debugInterface);
            }
            if (notificationsInterface != null) {
                ui.addActor(notificationsInterface);
            }
            if (messagesInterface != null) {
                ui.addActor(messagesInterface);
            }
            if (focusInterface != null) {
                ui.addActor(focusInterface);
            }
            if (inputInterface != null) {
                ui.addActor(inputInterface);
            }
        }
    }

    @Override
    public void update(double dt) {
        if (ui != null)
            ui.act((float) dt);
        if (notificationsInterface != null)
            notificationsInterface.update();
    }

}
