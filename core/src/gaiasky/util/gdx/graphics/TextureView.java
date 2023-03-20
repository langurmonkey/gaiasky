package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;

/**
 * A view of a pre-existing GL texture. Needs the width and height to be usable.
 */
public class TextureView extends GLTexture {
    private int width, height;

    public TextureView() {
        super(GL20.GL_TEXTURE_2D, 0);
    }

    public TextureView(int glHandle, int width, int height) {
        super(GL20.GL_TEXTURE_2D, glHandle);
        this.width = width;
        this.height = height;
    }

    public void setTexture(int glHandle, int width, int height) {
        this.glHandle = glHandle;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    protected void reload() {
    }
}
