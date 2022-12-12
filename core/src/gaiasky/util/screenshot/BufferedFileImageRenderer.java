/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.screenshot;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import gaiasky.render.BufferedFrame;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.ImageFormat;
import gaiasky.util.i18n.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Buffers the writing of images to disk
 */
public class BufferedFileImageRenderer implements IFileImageRenderer {
    private static final Log logger = Logger.getLogger(BufferedFileImageRenderer.class);

    /** Daemon timer **/
    private static final Timer timer = new Timer(true);

    /**
     * Output frame buffer and BufferedFrame pool
     */
    private final List<BufferedFrame> outputFrameBuffer;
    private final Pool<BufferedFrame> bfPool;
    private final int bufferSize;

    public BufferedFileImageRenderer(int bufferSize) {
        this.bufferSize = bufferSize;
        outputFrameBuffer = new ArrayList<>(bufferSize);
        bfPool = Pools.get(BufferedFrame.class, bufferSize);
    }

    @Override
    public String saveScreenshot(String folder, String filePrefix, int w, int h, boolean immediate, ImageFormat type, float quality) {
        String res;
        if (!immediate) {
            if (outputFrameBuffer.size() >= bufferSize) {
                flush();
            }

            synchronized (outputFrameBuffer) {
                BufferedFrame bf = bfPool.obtain();
                bf.pixmap = ImageRenderer.renderToPixmap(w, h);
                bf.folder = folder;
                bf.filename = filePrefix;

                outputFrameBuffer.add(bf);
            }
            res = "buffer";
        } else {
            // Screenshot while the frame buffer is on
            res = ImageRenderer.renderToImageGl20(folder, filePrefix, w, h, type, quality);
        }
        return res;
    }

    @Override
    public void flush() {
        synchronized (outputFrameBuffer) {
            final List<BufferedFrame> outputFrameBufferCopy = new ArrayList<BufferedFrame>(outputFrameBuffer);
            outputFrameBuffer.clear();

            final int size = outputFrameBufferCopy.size();
            if (size > 0) {
                // Notify
                logger.info(I18n.msg("notif.flushframebuffer"));

                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        String folder = null;
                        for (BufferedFrame bf : outputFrameBufferCopy) {
                            ImageRenderer.writePixmapToImage(bf.folder, bf.filename, bf.pixmap, Settings.settings.frame.format, Settings.settings.frame.quality);
                            folder = bf.folder;
                            bfPool.free(bf);
                        }
                        logger.info(I18n.msg("notif.flushframebuffer.finished", size, folder));
                    }
                };
                timer.schedule(tt, 0);
            }

        }

    }

}
