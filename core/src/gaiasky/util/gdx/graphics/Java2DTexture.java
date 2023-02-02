package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;

public class Java2DTexture extends Texture {

    protected BufferedImage bufferImg;
    protected IntBuffer buffer;
    private final Color BACKGROUND = new Color(0, 0, 0, 0);

    public Java2DTexture(int width, int height, Format format) {
        super(width, height, format);
        bufferImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffer = BufferUtils.newIntBuffer(width * height);
    }

    public Java2DTexture(int width, int height) {
        this(width, height, Pixmap.Format.RGBA8888);
    }

    public Java2DTexture() {
        this(1024, 1024);
    }

    public BufferedImage getBufferedImage() {
        return bufferImg;
    }

    public Graphics2D begin() {
        //you could probably cache this instead of requesting it every time
        Graphics2D g2d = (Graphics2D) bufferImg.getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setBackground(BACKGROUND);
        g2d.clearRect(0, 0, bufferImg.getWidth(), bufferImg.getHeight());
        g2d.setColor(java.awt.Color.white);
        return g2d;
    }

    public void end() {
        // now we pass the BufferedImage pixel data to the LibGDX texture...
        int width = bufferImg.getWidth();
        int height = bufferImg.getHeight();
        //you could probably cache this rather than requesting it every upload
        int[] pixels = ((DataBufferInt)bufferImg.getRaster().getDataBuffer())
                .getData();
        this.bind();
        buffer.rewind();
        buffer.put(pixels);
        buffer.flip();
        Gdx.gl.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height,
                GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
    }

}
