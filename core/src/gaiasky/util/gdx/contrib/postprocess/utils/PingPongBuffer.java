/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Encapsulates a framebuffer with the ability to ping-pong between two buffers.
 * <p>
 * Upon {@link #begin()} the buffer is reset to a known initial state, this is
 * usually done just before the first usage of the buffer.
 * <p>
 * Subsequent {@link #capture()} calls will initiate writing to the next
 * available buffer, returning the previously used one, effectively ping-ponging
 * between the two. Until {@link #end()} is called, chained rendering will be
 * possible by retrieving the necessary buffers via {@link PingPongBuffer#getSouceTexture()},
 * {@link #getSourceBuffer()}, {@link #getResultTexture()} or
 * {@link #getResultBuffer}.
 * <p>
 * When finished, {@link #end()} should be called to stop capturing. When the
 * OpenGL context is lost, {@link #rebind()} should be called.
 *
 * @author bmanuel
 */
public final class PingPongBuffer {
    private GaiaSkyFrameBuffer buffer1, buffer2;
    public Texture texture1, texture2, textureDepth, textureVel, textureNormal, textureReflectionMap;
    public int width, height;
    public final boolean ownResources;

    // internal state
    private Texture texResult, texSrc;
    private GaiaSkyFrameBuffer bufResult, bufSrc;
    private boolean writeState, pending1, pending2;

    // save/restore state
    private final GaiaSkyFrameBuffer ownedMain, ownedExtra;
    private GaiaSkyFrameBuffer ownedResult, ownedSource;
    private int ownedW, ownedH;

    public PingPongBuffer(int width, int height, Format frameBufferFormat, boolean hasDepth) {
        this(width, height, frameBufferFormat, hasDepth, true, false, false, false);
    }

    /** Creates a new ping-pong buffer and owns the resources. */
    public PingPongBuffer(int width, int height, Format frameBufferFormat, boolean hasDepth, boolean hasVelocity, boolean hasNormal, boolean hasReflectionMask, boolean preventFloatBuffer) {
        ownResources = true;

        // BUFFER USED FOR THE ACTUAL RENDERING:
        // n RENDER TARGETS:
        //      0: FLOAT TEXTURE ATTACHMENT (allow values outside of [0,1])
        //      1: FLOAT TEXTURE ATTACHMENT (DEPTH BUFFER)
        //      2: FLOAT TEXTURE ATTACHMENT (NORMAL BUFFER)
        //      3: FLOAT TEXTURE ATTACHMENT (REFLECTION MASK)
        //      4: FLOAT TEXTURE ATTACHMENT (POSITION BUFFER)
        // 1 DEPTH TEXTURE ATTACHMENT
        ownedMain = createMainFrameBuffer(width, height, hasDepth, hasVelocity, hasNormal, hasReflectionMask, frameBufferFormat, preventFloatBuffer);

        // EXTRA BUFFER:
        // SINGLE RENDER TARGET WITH A COLOR TEXTURE ATTACHMENT
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
        addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);
        ownedExtra = new GaiaSkyFrameBuffer(frameBufferBuilder);

        // Buffer the scene is rendered to is actually the second
        set(ownedExtra, ownedMain);
    }

    /** Creates a new ping-pong buffer with the given buffers. */
    public PingPongBuffer(GaiaSkyFrameBuffer buffer1, GaiaSkyFrameBuffer buffer2) {
        ownResources = false;
        ownedMain = null;
        ownedExtra = null;
        set(buffer1, buffer2);
    }

    public static GaiaSkyFrameBuffer createMainFrameBuffer(int width, int height, boolean hasDepth, boolean hasVelocity, boolean hasNormal, boolean hasReflectionMask, Format frameBufferFormat, boolean preventFloatBuffer) {
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);

        // 0
        // Main color render target
        addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);

        // 1
        // Depth buffer
        if (hasDepth) {
            addDepthRenderTarget(frameBufferBuilder, preventFloatBuffer);
        }

        // 2
        // Velocity buffer
        if (hasVelocity) {
            addFloatRenderTarget(frameBufferBuilder, frameBufferFormat);
        }

        // 3
        // Normal buffer
        if (hasNormal) {
            addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);
        }

        // 4
        // Reflection mask buffer
        if (hasReflectionMask) {
            addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);
        }

        return new GaiaSkyFrameBuffer(frameBufferBuilder);

    }

    private static void addColorRenderTarget(FrameBufferBuilder fbb, Format fbf, boolean preventFloatBuffer) {
        if (Gdx.graphics.isGL30Available() && !preventFloatBuffer) {
            addFloatRenderTarget(fbb, fbf);
        } else {
            addColorRenderTarget(fbb, fbf);
        }
    }

    private static void addFloatRenderTarget(FrameBufferBuilder fbb, Format fbf) {
        fbb.addFloatAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT, true);
    }

    private static void addColorRenderTarget(FrameBufferBuilder fbb, Format fbf) {
        fbb.addBasicColorTextureAttachment(fbf);
    }

    private static void addDepthRenderTarget(FrameBufferBuilder fbb, boolean preventFloatBuffer) {
        if (Gdx.graphics.isGL30Available() && !preventFloatBuffer) {
            // 32 bit depth buffer texture
            fbb.addDepthTextureAttachment(GL20.GL_DEPTH_COMPONENT32, GL20.GL_FLOAT);
        } else {
            fbb.addBasicDepthRenderBuffer();
        }
    }

    /**
     * An instance of this object can also be used to manipulate some other
     * externally-allocated buffers, applying just the same ping-ponging
     * behavior.
     * <p>
     * If this instance of the object was owning the resources, they will be
     * preserved and will be restored by a {@link #reset()} call.
     *
     * @param buffer1 the first buffer
     * @param buffer2 the second buffer
     */
    public void set(GaiaSkyFrameBuffer buffer1, GaiaSkyFrameBuffer buffer2) {
        if (ownResources) {
            ownedResult = bufResult;
            ownedSource = bufSrc;
            ownedW = width;
            ownedH = height;
        }

        this.buffer1 = buffer1;
        this.buffer2 = buffer2;
        width = this.buffer1.getWidth();
        height = this.buffer1.getHeight();
        rebind();
    }

    /** Restore the previous buffers if the instance was owning resources. */
    public void reset() {
        if (ownResources) {
            buffer1 = ownedMain;
            buffer2 = ownedExtra;
            width = ownedW;
            height = ownedH;
            bufResult = ownedResult;
            bufSrc = ownedSource;
        }
    }

    /** Free the resources, if any. */
    public void dispose() {
        if (ownResources) {
            // make sure we delete what we own
            // if the caller didn't call {@link #reset()}
            ownedMain.dispose();
            ownedExtra.dispose();
        }
    }

    /**
     * When needed graphics memory could be invalidated so buffers should be
     * rebuilt.
     */
    public void rebind() {
        texture1 = buffer1.getColorBufferTexture();
        texture2 = buffer2.getColorBufferTexture();
        textureDepth = buffer1.getDepthBufferTexture();
        textureVel = buffer1.getVelocityBufferTexture();
        textureNormal = buffer1.getNormalBufferTexture();
        textureReflectionMap = buffer1.getReflectionBufferTexture();
    }

    /**
     * Ensures the initial buffer state is always the same before starting
     * ping-ponging.
     */
    public void begin() {
        pending1 = false;
        pending2 = false;
        writeState = true;

        texSrc = texture1;
        bufSrc = buffer1;
        texResult = texture2;
        bufResult = buffer2;
    }

    /**
     * Starts and/or continue ping-ponging, begin capturing on the next
     * available buffer, returns the result of the previous {@link #capture()}
     * call.
     *
     * @return the Texture containing the result.
     */
    public Texture capture() {
        endPending();

        if (writeState) {
            // set src
            texSrc = texture1;
            bufSrc = buffer1;

            // set result
            texResult = texture2;
            bufResult = buffer2;

            // write to other
            pending2 = true;
            buffer2.begin();
        } else {
            texSrc = texture2;
            bufSrc = buffer2;

            texResult = texture1;
            bufResult = buffer1;

            pending1 = true;
            buffer1.begin();
        }

        writeState = !writeState;
        return texSrc;
    }

    /**
     * Finishes ping-ponging, must always be called after a call to
     * {@link #capture()}
     */
    public void end() {
        endPending();
    }

    /** @return the source texture of the current ping-pong chain. */
    public Texture getSouceTexture() {
        return texSrc;
    }

    /** @return the source buffer of the current ping-pong chain. */
    public GaiaSkyFrameBuffer getSourceBuffer() {
        return bufSrc;
    }

    /** @return the result's texture of the latest {@link #capture()}. */
    public Texture getResultTexture() {
        return texResult;
    }

    /** @return Returns the result's buffer of the latest {@link #capture()}. */
    public GaiaSkyFrameBuffer getResultBuffer() {
        return bufResult;
    }

    public GaiaSkyFrameBuffer getMainBuffer() {
        return ownedMain;
    }

    /** @return the depth texture attachment containing the depth buffer */
    public Texture getDepthTexture() {
        return textureDepth;
    }

    // internal use
    // finish writing to the buffers, mark as not pending anymore.
    private void endPending() {
        if (pending1) {
            buffer1.end();
            pending1 = false;
        }

        if (pending2) {
            buffer2.end();
            pending2 = false;
        }
    }
}
