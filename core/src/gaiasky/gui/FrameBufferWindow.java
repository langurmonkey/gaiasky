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

    public FrameBufferWindow(String title, Skin skin, Stage stage, FrameBuffer frameBuffer, float scale) {
        super(title, skin, stage);
        frameBufferWidget = new FrameBufferWidget(frameBuffer);
        frameBufferWidget.setScale(scale);

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

    public FrameBufferWindow(String title, Skin skin, Stage stage, FrameBuffer frameBuffer) {
        this(title, skin, stage, frameBuffer, 1);
    }

    /**
     * Flip the frame buffer.
     *
     * @param x Flip horizontally.
     * @param y Flip vertically.
     */
    public void setFlip(boolean x, boolean y) {
        if (frameBufferWidget != null) {
            frameBufferWidget.setFlip(x, y);
        }
    }

    /**
     * Set the scale factor of the frame buffer.
     *
     * @param scale The scale factor in both x and y dimensions.
     */
    public void setScale(float scale) {
        if (frameBufferWidget != null) {
            frameBufferWidget.setScale(scale);
        }
    }

    /**
     * Set the scale factor of the frame buffer.
     *
     * @param scaleX The scale factor in x.
     * @param scaleY The scale factor in y.
     */
    public void setScale(float scaleX, float scaleY) {
        if (frameBufferWidget != null) {
            frameBufferWidget.setScale(scaleX, scaleY);
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
