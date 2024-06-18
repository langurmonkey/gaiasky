/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.Settings;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class PingPongBuffer implements Disposable {
    public final boolean ownResources;
    // save/restore state
    private final GaiaSkyFrameBuffer ownedMain, ownedExtra;
    public Texture texture1, texture2, textureDepth, textureNormal, textureReflectionMap;
    public int width, height;
    private GaiaSkyFrameBuffer buffer1, buffer2;
    // internal state
    private Texture texResult, texSrc;
    private GaiaSkyFrameBuffer bufResult, bufSrc;
    private boolean writeState, pending1, pending2;
    private GaiaSkyFrameBuffer ownedResult, ownedSource;
    private int ownedW, ownedH;

    /** Creates a new ping-pong buffer and owns the resources. */
    public PingPongBuffer(int width, int height, Format pixmapFormat, boolean hasDepth, boolean hasNormal, boolean hasReflectionMask, boolean preventFloatBuffer) {
        ownResources = true;

        // BUFFER USED FOR THE ACTUAL RENDERING:
        // n RENDER TARGETS:
        //      0: COLOR 0 - FLOAT TEXTURE ATTACHMENT (allow values outside of [0,1])
        //      1: DEPTH   - FLOAT TEXTURE ATTACHMENT (DEPTH BUFFER)
        //      2: COLOR 1 - FLOAT TEXTURE ATTACHMENT (NORMAL BUFFER)
        //      3: COLOR 2 - FLOAT TEXTURE ATTACHMENT (REFLECTION MASK)
        // 1 DEPTH TEXTURE ATTACHMENT
        ownedMain = createMainFrameBuffer(width, height, hasDepth, hasNormal, hasReflectionMask, pixmapFormat, preventFloatBuffer);

        // EXTRA BUFFER:
        // SINGLE RENDER TARGET WITH A COLOR TEXTURE ATTACHMENT
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
        addColorRenderTarget(frameBufferBuilder, pixmapFormat, preventFloatBuffer);
        ownedExtra = new GaiaSkyFrameBuffer(frameBufferBuilder, 0);

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

    public static GaiaSkyFrameBuffer createMainFrameBuffer(int width, int height, boolean hasDepth, boolean hasNormal, boolean hasReflectionMask, Format frameBufferFormat, boolean preventFloatBuffer) {
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);

        int colorIndex, depthIndex = -1, normalIndex = -1, reflectionMaskIndex = -1;
        int idx = 0;

        // 0
        // Main color render target.
        addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);
        colorIndex = idx++;

        // 1
        // Depth buffer.
        if (hasDepth) {
            addDepthRenderTarget(frameBufferBuilder, preventFloatBuffer);
            if (!preventFloatBuffer)
                depthIndex = idx++;
        }

        // 2
        // Normal buffer.
        if (hasNormal) {
            addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);
            normalIndex = idx++;
        }

        // 3
        // Reflection mask buffer.
        if (hasReflectionMask) {
            addColorRenderTarget(frameBufferBuilder, frameBufferFormat, preventFloatBuffer);
            reflectionMaskIndex = idx;
        }

        return new GaiaSkyFrameBuffer(frameBufferBuilder, colorIndex, depthIndex, normalIndex, reflectionMaskIndex);

    }

    private static void addColorRenderTarget(FrameBufferBuilder builder, Format pixmapFormat, boolean preventFloatBuffer) {
        if (Gdx.graphics.isGL30Available() && !preventFloatBuffer) {
            addFloatRenderTarget(builder, Settings.settings.graphics.useSRGB ? GL30.GL_SRGB8_ALPHA8 : GL30.GL_RGBA16F);
        } else {
            addColorRenderTarget(builder, pixmapFormat);
        }
    }

    private static void addFloatRenderTarget(FrameBufferBuilder builder, int internalFormat) {
        builder.addFloatAttachment(internalFormat, GL30.GL_RGBA, GL30.GL_FLOAT, true);
    }

    private static void addColorRenderTarget(FrameBufferBuilder builder, Format pixmapFormat) {
        builder.addBasicColorTextureAttachment(pixmapFormat);
    }

    private static void addDepthRenderTarget(FrameBufferBuilder builder, boolean preventFloatBuffer) {
        if (Gdx.graphics.isGL30Available() && !preventFloatBuffer) {
            // 24 bit depth buffer texture
            builder.addDepthTextureAttachment(GL20.GL_DEPTH_COMPONENT24, GL20.GL_FLOAT);
        } else {
            builder.addBasicDepthRenderBuffer();
        }
    }

    /**
     * An instance of this object can also be used to manipulate some other
     * externally-allocated buffers, applying just the same ping-ponging
     * behavior.
     * <p>
     * If this instance of the object was owning the resources, they will be
     * preserved and will be restored by a {@link #reset()} call.
     * </p>
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
        textureNormal = buffer1.getNormalBufferTexture();
        textureReflectionMap = buffer1.getReflectionMaskBufferTexture();
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
     * available buffer, returns the result of the previous call.
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
