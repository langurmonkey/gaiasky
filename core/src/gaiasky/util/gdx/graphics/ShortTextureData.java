package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Blending;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.ShortBuffer;

/**
 * Provides a texture data object backed by a 16-bit short buffer.
 * Ideal to use with GL_R[GBA]16[F|I|UI].
 */
public class ShortTextureData implements TextureData {

    int width = 0;
    int height = 0;

    int internalFormat;
    int format;
    int type;

    boolean isGpuOnly;
    boolean useMipMaps;

    boolean isPrepared = false;
    ShortBuffer buffer;

    public ShortTextureData(int w, int h, int internalFormat, int format, int type, boolean useMipMaps, boolean isGpuOnly) {
        this.width = w;
        this.height = h;
        this.internalFormat = internalFormat;
        this.format = format;
        this.type = type;
        this.useMipMaps = useMipMaps;
        this.isGpuOnly = isGpuOnly;
    }

    @Override
    public TextureDataType getType() {
        return TextureDataType.Custom;
    }

    @Override
    public boolean isPrepared() {
        return isPrepared;
    }

    @Override
    public void prepare() {
        if (isPrepared)
            throw new GdxRuntimeException("Already prepared");
        if (!isGpuOnly) {
            int amountOfShorts = 0;
            if (Gdx.graphics.getGLVersion().getType().equals(GLVersion.Type.OpenGL)) {
                if (internalFormat == GL30.GL_RGBA16F || internalFormat == GL30.GL_RGBA16I || internalFormat == GL30.GL_RGBA16UI)
                    amountOfShorts = 4;
                if (internalFormat == GL30.GL_RGB16F || internalFormat == GL30.GL_RGB16I || internalFormat == GL30.GL_RGB16UI)
                    amountOfShorts = 3;
                if (internalFormat == GL30.GL_RG16F || internalFormat == GL30.GL_RG16I || internalFormat == GL30.GL_RG16UI)
                    amountOfShorts = 2;
                if (internalFormat == GL30.GL_R16F || internalFormat == GL30.GL_R16I || internalFormat == GL30.GL_R16UI)
                    amountOfShorts = 1;
            }
            this.buffer = BufferUtils.newShortBuffer(width * height * amountOfShorts);
        }
        isPrepared = true;
    }

    @Override
    public void consumeCustomData(int target) {
        if (!Gdx.graphics.isGL30Available()) {
            if (!Gdx.graphics.supportsExtension("GL_ARB_texture_float"))
                throw new GdxRuntimeException("Extension GL_ARB_texture_float not supported!");
        }

        Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
        if (useMipMaps()) {
            // Use GPU if possible.
            if (Gdx.graphics.supportsExtension("GL_ARB_framebuffer_object")
                    || Gdx.graphics.supportsExtension("GL_EXT_framebuffer_object")
                    || Gdx.gl20.getClass().getName().equals("com.badlogic.gdx.backends.lwjgl3.Lwjgl3GLES20") // LWJGL3ANGLE
                    || Gdx.gl30 != null) {
                Gdx.gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, buffer);
                Gdx.gl20.glGenerateMipmap(target);
            } else {
                // Use CPU.
                generateMipMapCPU(target, width, height);
            }
        } else {
            Gdx.gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, buffer);
        }
    }

    /**
     * Generates mipmaps, but it only uses a part of the current buffer, does not downsample images.
     *
     * @param target        The GL target.
     * @param textureWidth  The width.
     * @param textureHeight The height.
     */
    private void generateMipMapCPU(int target, int textureWidth, int textureHeight) {
        Gdx.gl.glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, buffer);
        if ((Gdx.gl20 == null) && textureWidth != textureHeight)
            throw new GdxRuntimeException("texture width and height must be square when using mipmapping.");
        int currentWidth = width / 2;
        int currentHeight = height / 2;
        int level = 1;
        while (currentWidth > 0 && currentHeight > 0) {
            Gdx.gl.glTexImage2D(target, level, internalFormat, width, height, 0, format, type, buffer);

            currentWidth = currentWidth / 2;
            currentHeight = currentHeight / 2;
            level++;
        }
    }

    @Override
    public Pixmap consumePixmap() {
        throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
    }

    @Override
    public boolean disposePixmap() {
        throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
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
        return Format.RGBA8888; // it's not true, but ShortTextureData.getFormat() isn't used anywhere
    }

    @Override
    public boolean useMipMaps() {
        return true;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    public ShortBuffer getBuffer() {
        return buffer;
    }
}
