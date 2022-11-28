package gaiasky.input;

import com.badlogic.gdx.scenes.scene2d.Group;
import gaiasky.gui.GenericDialog;

public class WindowKbdListener extends GuiKbdListener {

    private final GenericDialog dialog;

    public WindowKbdListener(GenericDialog dialog) {
        super(dialog.getStage());
        this.dialog = dialog;
    }

    @Override
    public Group getContentContainer() {
        return dialog.getActualContentContainer();
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
