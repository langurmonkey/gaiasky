package gaiasky.util.svt;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Extends texture to be able to use draw operations on any mipmap level.
 */
public class TextureExt extends Texture {
    public TextureExt(TextureData data) {
        super(data);
    }

    public TextureExt(String internalPath) {
        super(internalPath);
    }

    /**
     * Draws the given {@link Pixmap} to the texture mipmap level at position x, y. No clipping is performed so you have to make sure that you
     * draw only inside the texture region.
     *
     * @param pixmap      The Pixmap.
     * @param x           The x coordinate in pixels.
     * @param y           The y coordinate in pixels.
     * @param mipmapLevel The mipmap level to draw.
     */
    public void draw(Pixmap pixmap, int x, int y, int mipmapLevel) {
        if (getTextureData().isManaged())
            throw new GdxRuntimeException("can't draw to a managed texture");

        bind();
        Gdx.gl.glTexSubImage2D(glTarget, mipmapLevel, x, y, pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
    }
}
