package gaiasky.gui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.TextureWidget;

/**
 * A UI window that shows the contents of a frame buffer or a texture
 * using a {@link TextureWidget}.
 */
public class TextureWindow extends GenericDialog {

    private final TextureWidget textureWidget;

    public TextureWindow(String title, Skin skin, Stage stage, Texture texture, float scale) {
        super(title, skin, stage);
        textureWidget = new TextureWidget(texture);
        textureWidget.setDebug(true);
        textureWidget.setScale(scale);

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

    public TextureWindow(String title, Skin skin, Stage stage, Texture texture) {
        this(title, skin, stage, texture, 1);
    }

    public TextureWindow(String title, Skin skin, Stage stage, FrameBuffer frameBuffer, float scale) {
        this(title, skin, stage, frameBuffer.getColorBufferTexture(), scale);
    }

    public TextureWindow(String title, Skin skin, Stage stage, FrameBuffer frameBuffer) {
        this(title, skin, stage, frameBuffer, 1);
    }

    /**
     * Flip the texture.
     *
     * @param x Flip horizontally.
     * @param y Flip vertically.
     */
    public void setFlip(boolean x, boolean y) {
        if (textureWidget != null) {
            textureWidget.setFlip(x, y);
        }
    }

    /**
     * Set the scale factor of the texture.
     *
     * @param scale The scale factor in both x and y dimensions.
     */
    public void setScale(float scale) {
        if (textureWidget != null) {
            textureWidget.setScale(scale);
        }
    }

    /**
     * Set the scale factor of the texture.
     *
     * @param scaleX The scale factor in x.
     * @param scaleY The scale factor in y.
     */
    public void setScale(float scaleX, float scaleY) {
        if (textureWidget != null) {
            textureWidget.setScale(scaleX, scaleY);
        }
    }

    @Override
    protected void build() {

        Container<TextureWidget> textureContainer = new Container<>();
        textureContainer.setActor(textureWidget);

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
