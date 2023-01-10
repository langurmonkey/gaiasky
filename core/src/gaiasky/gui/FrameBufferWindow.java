package gaiasky.gui;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.FrameBufferWidget;

/**
 * A UI window that shows the contents of a frame buffer.
 */
public class FrameBufferWindow extends GenericDialog {

    private final FrameBufferWidget frameBufferWidget;

    public FrameBufferWindow(String title, Skin skin, Stage stage, FrameBuffer frameBuffer) {
        super(title, skin, stage);
        frameBufferWidget = new FrameBufferWidget(frameBuffer);

        setCancelText(I18n.msg("gui.close"));

        // Not modal.
        setModal(false);
        defaultMouseKbdListener = false;
        defaultGamepadListener = false;

        // Build.
        buildSuper();
        // Pack.
        pack();
    }

    /**
     * Flip the frame buffer.
     * @param x Flip horizontally.
     * @param y Flip vertically.
     */
    public void setFlip(boolean x, boolean y) {
       if(frameBufferWidget != null) {
           frameBufferWidget.setFlip(x, y);
       }
    }

    @Override
    protected void build() {

        Container<FrameBufferWidget> textureContainer = new Container<>();
        textureContainer.setActor(frameBufferWidget);

        content.add(textureContainer);
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
