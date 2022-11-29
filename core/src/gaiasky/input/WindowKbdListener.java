package gaiasky.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.GenericDialog;

public class WindowKbdListener extends GuiKbdListener {

    private final GenericDialog dialog;

    public WindowKbdListener(GenericDialog dialog) {
        super(dialog.getStage());
        this.dialog = dialog;
    }

    @Override
    public Array<Group> getContentContainers() {
        var a = new Array<Group>(3);
        a.add(dialog.getCurrentContentContainer());
        a.add(dialog.getBottmGroup());
        a.add(dialog.getButtonsGroup());
        return a;
    }

    @Override
    public void close() {
        dialog.closeCancel();
    }

    @Override
    public void accept() {
    }

    @Override
    public void select() {
        var target = stage.getKeyboardFocus();
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
}
