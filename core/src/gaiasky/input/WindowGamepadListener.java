package gaiasky.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import gaiasky.gui.GenericDialog;
import gaiasky.gui.IGamepadMappings;

/**
 * Provides the default gamepad listener for UI windows.
 */
public class WindowGamepadListener extends GuiGamepadListener {

    private final GenericDialog dialog;

    public WindowGamepadListener(String mappingsFile, GenericDialog dialog) {
        super(mappingsFile, dialog.getStage());
        this.dialog = dialog;
    }

    public WindowGamepadListener(IGamepadMappings mappings, GenericDialog dialog) {
        super(mappings, dialog.getStage());
        this.dialog = dialog;
    }


    @Override
    public void moveLeft() {
    }

    @Override
    public void moveRight() {
    }

    @Override
    public Group getContentContainer() {
        return dialog.getActualContentContainer();
    }

    @Override
    public void select() {
        Actor target = stage.getKeyboardFocus();
        if (target != dialog.acceptButton && dialog.acceptButton != null) {
            stage.setKeyboardFocus(dialog.acceptButton);
        } else if (target != dialog.cancelButton && dialog.cancelButton != null) {
            stage.setKeyboardFocus(dialog.cancelButton);
        }
    }

    @Override
    public void tabLeft() {
        dialog.tabLeft();
    }

    @Override
    public void tabRight() {
        dialog.tabRight();
    }

    @Override
    public void back() {
        select();
    }

    @Override
    public void start() {
        dialog.closeAccept();
    }

}
