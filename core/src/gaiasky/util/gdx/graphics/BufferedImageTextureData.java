package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.utils.GdxRuntimeException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Texture data that uses Java2D {@link BufferedImage} to provide
 * image data to textures.
 */
public class BufferedImageTextureData implements TextureData {
    private final FileHandle file;
    private final boolean useMipMaps;
    private BufferedImage image = null;
    int width = 0;
    int height = 0;

    public BufferedImageTextureData(FileHandle file, boolean useMipMaps) {
        this.file = file;
        this.useMipMaps = useMipMaps;
    }

    @Override
    public TextureDataType getType() {
        return TextureDataType.Custom;
    }

    @Override
    public boolean isPrepared() {
        return image != null;
    }

    @Override
    public void prepare() {
        if (image != null)
            throw new GdxRuntimeException("Already prepared");

        // Prepare
        try {
            image = ImageIO.read(file.file());
            width = image.getWidth();
            height = image.getHeight();
        } catch (IOException ignored) {
        }

    }

    @Override
    public Pixmap consumePixmap() {
        return null;
    }

    @Override
    public boolean disposePixmap() {
        return false;
    }

    @Override
    public void consumeCustomData(int target) {
        if (image == null)
            throw new GdxRuntimeException("Call prepare() before calling consumeCompressedData()");

        var pixmap = new Pixmap(width, height, Format.RGBA8888);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int argb = image.getRGB(x, y);
                int blue = argb & 255;
                int green = (argb >> 8) & 255;
                int red = (argb >> 16) & 255;
                int alpha = (argb >> 24) & 255;
                pixmap.setColor(red / 255f, green / 255f, blue / 255f, alpha / 255f);
                pixmap.drawPixel(x, y);
            }
        }
        Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), width, height, 0,
                pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());

        if (useMipMaps()) {
            MipMapGenerator.generateMipMap(target, pixmap, width, height);
        }

        // Dispose prepared data.
        pixmap.dispose();
        image = null;
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
    public Format getFormat() {
        return Format.RGBA8888;
    }

    @Override
    public boolean useMipMaps() {
        return useMipMaps;
    }

    @Override
    public boolean isManaged() {
        return true;
    }
}
