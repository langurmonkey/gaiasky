/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.api.IPostProcessor.RenderType;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.render.util.ItemsManager;
import gaiasky.util.Settings;
import gaiasky.util.Settings.UpscaleFilter;
import org.lwjgl.opengl.GL30;

import java.util.function.IntSupplier;

public final class PostProcessor implements Disposable {
    private static final Array<PingPongBuffer> buffers = new Array<>(2);
    /** Enable pipeline state queries: beware the pipeline can stall! */
    public static boolean EnableQueryStates = false;
    public static PostProcessor currentPostProcessor;
    private static PipelineState pipelineState = null;
    private static Format pixmapFormat;
    private final PingPongBuffer composite;
    private final ItemsManager<PostProcessorEffect> effectsManager = new ItemsManager<>();
    private final Color clearColor = Color.CLEAR;
    private final Rectangle viewport = new Rectangle();
    private final boolean useDepth;
    // maintains a per-frame updated list of enabled effects
    private final Array<PostProcessorEffect> enabledEffects = new Array<>(5);
    private TextureWrap compositeWrapU;
    private TextureWrap compositeWrapV;
    private TextureFilter compositeMinFilter, compositeMagFilter;
    private int clearBits = GL20.GL_COLOR_BUFFER_BIT;
    private float clearDepth = 1f;
    private boolean enabled;
    private boolean capturing;
    private boolean hasCaptured;
    private PostProcessorListener listener = null;

    /** Construct a new PostProcessor with FBO dimensions set to the size of the screen */
    public PostProcessor(RenderType rt, boolean useDepth, boolean useAlphaChannel, boolean use32Bits) {
        this(rt, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), useDepth, useAlphaChannel, use32Bits, false, false, true);
    }

    /**
     * Construct a new PostProcessor with the given parameters, defaulting to <em>TextureWrap.ClampToEdge</em> as texture wrap
     * mode
     */
    public PostProcessor(RenderType rt, int fboWidth, int fboHeight, boolean hasDepth, boolean useAlphaChannel, boolean use32Bits, boolean hasNormal, boolean hasReflectionMask, boolean preventFloatBuffer) {
        this(rt, fboWidth, fboHeight, hasDepth, useAlphaChannel, use32Bits, hasNormal, hasReflectionMask, preventFloatBuffer, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    /** Construct a new PostProcessor with the given parameters and the specified texture wrap mode */
    public PostProcessor(RenderType rt, int fboWidth, int fboHeight, boolean useDepth, boolean useAlphaChannel, boolean use32Bits, boolean hasNormal, boolean hasReflectionMask, boolean preventFloatBuffer, TextureWrap u, TextureWrap v) {
        if (use32Bits) {
            if (useAlphaChannel) {
                pixmapFormat = Format.RGBA8888;
            } else {
                pixmapFormat = Format.RGB888;
            }
        } else {
            if (useAlphaChannel) {
                pixmapFormat = Format.RGBA4444;
            } else {
                pixmapFormat = Format.RGB565;
            }
        }

        composite = newPingPongBuffer(fboWidth, fboHeight, pixmapFormat, useDepth, hasNormal, hasReflectionMask, preventFloatBuffer);
        setBufferTextureWrap(u, v);
        if (rt == RenderType.screen) {
            UpscaleFilter upscaleFilter = Settings.settings.postprocess.upscaleFilter;
            setBufferTextureFilter(upscaleFilter.minification, upscaleFilter.magnification);
        }

        pipelineState = new PipelineState();

        capturing = false;
        hasCaptured = false;
        enabled = true;
        this.useDepth = useDepth;
        if (useDepth) {
            clearBits |= GL20.GL_DEPTH_BUFFER_BIT;
        }

        setViewport(null);
    }

    /**
     * Creates and returns a managed PingPongBuffer buffer, just create and forget. If rebind() is called on context loss, managed
     * PingPongBuffers will be rebound for you.
     * <p>
     * This is a drop-in replacement for the same-signature PingPongBuffer's constructor.
     */
    public static PingPongBuffer newPingPongBuffer(int width, int height, Format frameBufferFormat, boolean hasDepth) {
        return newPingPongBuffer(width, height, frameBufferFormat, hasDepth, false, false, true);
    }

    /**
     * Creates and returns a managed PingPongBuffer buffer, just create and forget. If rebind() is called on context loss, managed
     * PingPongBuffers will be rebound for you.
     * <p>
     * This is a drop-in replacement for the same-signature PingPongBuffer's constructor.
     */
    public static PingPongBuffer newPingPongBuffer(int width, int height,
                                                   Format pixmapFormat,
                                                   boolean hasDepth,
                                                   boolean hasNormal,
                                                   boolean hasReflectionMask,
                                                   boolean preventFloatBuffer) {
        PingPongBuffer buffer = new PingPongBuffer(width, height, pixmapFormat, hasDepth, hasNormal, hasReflectionMask, preventFloatBuffer);
        buffers.add(buffer);
        return buffer;
    }

    /** Provides a way to query the pipeline for the most used states */
    public static boolean isStateEnabled(int pname) {
        if (EnableQueryStates) {
            return pipelineState.isEnabled(pname);
        }

        return false;
    }

    /**
     * Returns the internal framebuffer format, computed from the parameters specified during construction. NOTE: the returned
     * Format will be valid after construction and NOT early!
     */
    public static Format getFramebufferFormat() {
        return pixmapFormat;
    }

    public Rectangle getViewport() {
        return viewport;
    }

    /**
     * Sets the viewport to be restored, if null is specified then the viewport will NOT be restored at all.
     * <p>
     * The predefined effects will restore the viewport settings at the final blitting stage (render to screen) by invoking the
     * restoreViewport static method.
     */
    public void setViewport(Rectangle viewport) {
        if (viewport != null) {
            this.viewport.set(viewport);
        }
    }

    /** Frees owned resources. */
    @Override
    public void dispose() {
        dispose(true);
    }

    public void dispose(boolean cleanAllBuffers) {
        effectsManager.dispose();

        if (cleanAllBuffers) {
            // cleanup managed buffers, if any
            for (int i = 0; i < buffers.size; i++) {
                buffers.get(i).dispose();
            }
            buffers.clear();
        } else {
            composite.dispose();
            buffers.removeValue(composite, true);
        }

        enabledEffects.clear();

        if (cleanAllBuffers) {
            pipelineState.dispose();
        }
    }

    /** Whether the post-processor is enabled */
    public boolean isEnabled() {
        return enabled;
    }

    /** Sets whether the post-processor should be enabled */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** If called before capturing it will indicate if the next capture call will succeed or not. */
    public boolean isReady() {
        boolean hasEffects = false;

        for (PostProcessorEffect e : effectsManager) {
            if (e.isEnabled()) {
                hasEffects = true;
                break;
            }
        }

        return (enabled && !capturing && hasEffects);
    }

    /** Returns the number of the currently enabled effects */
    public int getEnabledEffectsCount() {
        return enabledEffects.size;
    }

    /** Sets the listener that will receive events triggered by the PostProcessor rendering pipeline. */
    public void setListener(PostProcessorListener listener) {
        this.listener = listener;
    }

    /**
     * Adds the specified effect to the effect chain and transfer ownership to the PostProcessor, it will manage cleaning it up
     * for you. The order of the inserted effects IS important, since effects will be applied in a FIFO fashion, the first added is
     * the first being applied.
     */
    public void addEffect(PostProcessorEffect effect) {
        effectsManager.add(effect);
    }

    /** Removes the specified effect from the effect chain. */
    public void removeEffect(PostProcessorEffect effect) {
        effectsManager.remove(effect);
    }

    /** Adds the given effect after an effect with the given type. If no effect with the given type exists, it adds the effect at the end. **/
    public void addEffect(PostProcessorEffect effect, Class<? extends PostProcessorEffect> after) {
       var it = effectsManager.iterator();
       int idx = -1;
       var found = false;
       while(it.hasNext()) {
          var o = it.next();
          idx ++;
          if (after.isInstance(o)) {
              found = true;
             break;
          }
       }
       if (found) {
           effectsManager.add(effect, idx + 1);
       } else {
           addEffect(effect);
       }


    }

    /** Sets the color that will be used to clear the buffer. */
    public void setClearColor(Color color) {
        clearColor.set(color);
    }

    /** Sets the color that will be used to clear the buffer. */
    public void setClearColor(float r, float g, float b, float a) {
        clearColor.set(r, g, b, a);
    }

    /** Sets the clear bit for when glClear is invoked. */
    public void setClearBits(int bits) {
        clearBits = bits;
    }

    /** Sets the depth value with which to clear the depth buffer when needed. */
    public void setClearDepth(float depth) {
        clearDepth = depth;
    }

    public void setBufferTextureFilter(TextureFilter minFilter, TextureFilter magFilter) {
        compositeMinFilter = minFilter;
        compositeMagFilter = magFilter;
        composite.texture1.setFilter(minFilter, magFilter);
        composite.texture2.setFilter(minFilter, magFilter);
    }

    public void setBufferTextureWrap(TextureWrap u, TextureWrap v) {
        compositeWrapU = u;
        compositeWrapV = v;

        composite.texture1.setWrap(compositeWrapU, compositeWrapV);
        composite.texture2.setWrap(compositeWrapU, compositeWrapV);
    }

    public boolean capture() {
        return capture(this::buildEnabledEffectsList);
    }

    public boolean captureCubemap() {
        return capture(this::buildEnabledEffectsListCubemap);
    }

    public boolean captureVR() {
        return capture(this::buildEnabledEffectsListVR);
    }

    /**
     * Starts capturing the scene, clears the buffer with the clear color specified by {@link #setClearColor(Color)} or
     * {@link #setClearColor(float r, float g, float b, float a)}.
     *
     * @return true or false, whether capturing has been initiated. Capturing will fail in case there are no enabled effects
     * in the chain or this instance is not enabled or capturing is already started.
     */
    public boolean capture(IntSupplier supplier) {
        hasCaptured = false;

        if (enabled && !capturing) {
            if (supplier.getAsInt() == 0) {
                // no enabled effects
                return false;
            }

            capturing = true;
            composite.getMainBuffer().begin();
            //composite.begin();
            //composite.capture();

            if (useDepth) {
                Gdx.gl.glClearDepthf(clearDepth);
            }

            Gdx.gl.glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
            Gdx.gl.glClear(clearBits);
            return true;
        }

        return false;
    }

    public boolean captureNoClear() {
        return captureNoClear(this::buildEnabledEffectsList);
    }

    public boolean captureNoClearCubemap() {
        return captureNoClear(this::buildEnabledEffectsListCubemap);
    }

    public boolean captureNoClearReprojection() {
        return captureNoClear(this::buildEnabledEffectsListVR);
    }

    /**
     * Starts capturing the scene as {@link #capture()}, but <strong>without</strong> clearing the screen.
     *
     * @return true or false, depending on whether capturing has been initiated.
     */
    public boolean captureNoClear(IntSupplier supplier) {
        hasCaptured = false;

        if (enabled && !capturing) {
            if (supplier.getAsInt() == 0) {
                return false;
            }

            capturing = true;
            composite.begin();
            composite.capture();
            return true;
        }

        return false;
    }

    /** Stops capturing the scene and returns the result, or null if nothing was captured. */
    public GaiaSkyFrameBuffer captureEnd() {
        if (enabled && capturing) {
            capturing = false;
            hasCaptured = true;
            composite.getMainBuffer().end();
            return composite.getMainBuffer();
        }

        return null;
    }

    public PingPongBuffer getCombinedBuffer() {
        return composite;
    }

    /** Regenerates and/or rebinds owned resources when needed, eg. when the OpenGL context is lost. */
    public void rebind() {
        // Wrap.
        composite.texture1.setWrap(compositeWrapU, compositeWrapV);
        composite.texture2.setWrap(compositeWrapU, compositeWrapV);
        // Filter.
        composite.texture1.setFilter(compositeMinFilter, compositeMagFilter);
        composite.texture2.setFilter(compositeMinFilter, compositeMagFilter);

        for (int i = 0; i < buffers.size; i++) {
            buffers.get(i).rebind();
        }

        for (PostProcessorEffect e : effectsManager) {
            e.rebind();
        }
    }

    // Assumes the two textures are the same dimensions
    void copyFrameBufferTexture(int width, int height, FrameBuffer fbIn, FrameBuffer fbOut)
    {
        // Bind input FBO + texture to a color attachment
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbIn.getFramebufferHandle());
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, fbIn.getColorBufferTexture().glTarget, 0);
        GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);

        // Bind destination FBO + texture to another color attachment
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fbOut.getFramebufferHandle());
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D, fbOut.getColorBufferTexture().glTarget, 0);
        GL30.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT1);

        // specify source, destination drawing (sub)rectangles.
        GL30.glBlitFramebuffer(0, 0, width, height,
                0, 0, width, height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);

        // unbind the color attachments
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL30.GL_TEXTURE_2D, 0, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, 0, 0);
    }

    /**
     * Stops capturing the scene and apply the effect chain, if there is one. If the specified output FrameBuffer is NULL, then
     * the rendering will be performed to screen.
     */
    public void render(FrameBuffer destination) {
        captureEnd();

        if (!hasCaptured) {
            return;
        }

        Array<PostProcessorEffect> items = enabledEffects;

        int count = items.size;
        if (count > 0) {
            currentPostProcessor = this;

            Gdx.gl.glDisable(GL20.GL_CULL_FACE);
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

            // render effects chain, [0,n-2]
            if (count > 1) {
                for (int i = 0; i < count - 1; i++) {
                    PostProcessorEffect e = items.get(i);

                    composite.capture();
                    {
                        // We use the main buffer as the first source.
                        var source = i == 0 ? composite.getMainBuffer() : composite.getSourceBuffer();
                        e.render(source, composite.getResultBuffer(), composite.getMainBuffer());
                    }
                }

                // complete
                composite.end();
            }

            currentPostProcessor = null;

            if (listener != null && destination == null) {
                listener.beforeRenderToScreen();
            }

            // render with null dest (to screen)
            items.get(count - 1).render(composite.getResultBuffer(), destination, composite.getMainBuffer());

            // ensure default texture unit #0 is active
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        } else {
            Gdx.app.log("PostProcessor", "No post-processor effects enabled, aborting render");
        }
    }

    /** Convenience method to render to screen. */
    public void render() {
        render(null);
    }

    public int buildEnabledEffectsList(boolean cubemap, boolean reprojection) {
        if (cubemap) {
            return buildEnabledEffectsListCubemap();
        } else if (reprojection) {
            return buildEnabledEffectsListVR();
        } else {
            return buildEnabledEffectsList();
        }

    }

    public int buildEnabledEffectsList() {
        enabledEffects.clear();
        for (PostProcessorEffect e : effectsManager) {
            if (e.isEnabled()) {
                enabledEffects.add(e);
            }
        }

        return enabledEffects.size;
    }

    public int buildEnabledEffectsListCubemap() {
        enabledEffects.clear();
        for (PostProcessorEffect e : effectsManager) {
            if (e.isEnabled() && e.isEnabledInCubemap()) {
                enabledEffects.add(e);
            }
        }

        return enabledEffects.size;
    }

    public int buildEnabledEffectsListVR() {
        enabledEffects.clear();
        for (PostProcessorEffect e : effectsManager) {
            if (e.isEnabled() && e.isEnabledInVR()) {
                enabledEffects.add(e);
            }
        }

        return enabledEffects.size;
    }

    /** Restores the previously set viewport if one was specified earlier and the destination buffer is the screen */
    void restoreViewport(FrameBuffer dest) {
        if (dest == null) {
            HdpiUtils.glViewport((int) viewport.x, (int) viewport.y, (int) viewport.width, (int) viewport.height);
        }
    }

}
