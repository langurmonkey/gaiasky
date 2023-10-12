/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.utils.DefaultTextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter;
import com.badlogic.gdx.utils.*;
import gaiasky.util.gdx.shader.DefaultIntShader;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.provider.DefaultIntShaderProvider;
import gaiasky.util.gdx.shader.provider.IntShaderProvider;

public class IntModelBatch implements Disposable {
    protected final RenderablePool renderablesPool = new RenderablePool();
    /** list of Renderables to be rendered in the current batch **/
    protected final Array<IntRenderable> renderables = new Array<>();
    /** the {@link RenderContext} **/
    protected final RenderContext context;
    /** the {@link IntShaderProvider}, provides {@link IntShader} instances for IntRenderables **/
    protected final IntShaderProvider shaderProvider;
    /** the {@link IntRenderableSorter} **/
    protected final IntRenderableSorter sorter;
    private final boolean ownContext;
    protected Camera camera;

    /**
     * Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
     *
     * @param context        The {@link RenderContext} to use.
     * @param shaderProvider The {@link IntShaderProvider} to use, will be disposed when this ModelBatch is disposed.
     * @param sorter         The {@link RenderableSorter} to use.
     */
    public IntModelBatch(final RenderContext context, final IntShaderProvider shaderProvider, final IntRenderableSorter sorter) {
        this.sorter = (sorter == null) ? new DefaultIntRenderableSorter() : sorter;
        this.ownContext = (context == null);
        this.context = (context == null) ? new RenderContext(new DefaultTextureBinder(DefaultTextureBinder.LRU, 1)) : context;
        this.shaderProvider = (shaderProvider == null) ? new DefaultIntShaderProvider() : shaderProvider;
    }

    /**
     * Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
     *
     * @param context        The {@link RenderContext} to use.
     * @param shaderProvider The {@link IntShaderProvider} to use, will be disposed when this ModelBatch is disposed.
     */
    public IntModelBatch(final RenderContext context, final IntShaderProvider shaderProvider) {
        this(context, shaderProvider, null);
    }

    /**
     * Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
     *
     * @param context The {@link RenderContext} to use.
     * @param sorter  The {@link IntRenderableSorter} to use.
     */
    public IntModelBatch(final RenderContext context, final IntRenderableSorter sorter) {
        this(context, null, sorter);
    }

    /**
     * Construct a ModelBatch, using this constructor makes you responsible for calling context.begin() and context.end() yourself.
     *
     * @param context The {@link RenderContext} to use.
     */
    public IntModelBatch(final RenderContext context) {
        this(context, null, null);
    }

    /**
     * Construct a ModelBatch
     *
     * @param shaderProvider The {@link IntShaderProvider} to use, will be disposed when this ModelBatch is disposed.
     **/
    public IntModelBatch(final IntShaderProvider shaderProvider) {
        this(null, shaderProvider, null);
    }

    /**
     * Construct a ModelBatch
     *
     * @param shaderProvider The {@link IntShaderProvider} to use, will be disposed when this ModelBatch is disposed.
     * @param sorter         The {@link IntRenderableSorter} to use.
     */
    public IntModelBatch(final IntShaderProvider shaderProvider, final IntRenderableSorter sorter) {
        this(null, shaderProvider, sorter);
    }

    /**
     * Construct a ModelBatch with the default implementation and the specified ubershader. See {@link DefaultIntShader} for more
     * information about using a custom ubershader. Requires OpenGL ES 2.0.
     *
     * @param vertexShaderCode   The vertex shader to use.
     * @param fragmentShaderCode The fragment shader to use.
     */
    public IntModelBatch(final String vertexShaderCode, final String fragmentShaderCode) {
        this(null, new DefaultIntShaderProvider(vertexShaderCode, fragmentShaderCode), null);
    }

    /** Construct a ModelBatch with the default implementation */
    public IntModelBatch() {
        this(null, null, null);
    }

    /**
     * Start rendering one or more {@link IntRenderable}s. Use one of the render() methods to provide the renderables. Must be
     * followed by a call to {@link #end()}. The OpenGL context must not be altered between {@link #begin(Camera)} and
     * {@link #end()}.
     *
     * @param cam The {@link Camera} to be used when rendering and sorting.
     */
    public void begin(final Camera cam) {
        if (camera != null)
            throw new GdxRuntimeException("Call end() first.");
        camera = cam;
        if (ownContext)
            context.begin();
    }

    /**
     * Provides access to the current camera in between {@link #begin(Camera)} and {@link #end()}. Do not change the camera's
     * values. Use {@link #setCamera(Camera)}, if you need to change the camera.
     *
     * @return The current camera being used or null if called outside {@link #begin(Camera)} and {@link #end()}.
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * Change the camera in between {@link #begin(Camera)} and {@link #end()}. This causes the batch to be flushed. Can only be
     * called after the call to {@link #begin(Camera)} and before the call to {@link #end()}.
     *
     * @param cam The new camera to use.
     */
    public void setCamera(final Camera cam) {
        if (camera == null)
            throw new GdxRuntimeException("Call begin() first.");
        if (renderables.size > 0)
            flush();
        camera = cam;
    }

    /**
     * Checks whether the {@link RenderContext} returned by {@link #getRenderContext()} is owned and managed by this ModelBatch.
     * When the RenderContext isn't owned by the ModelBatch, you are responsible for calling the {@link RenderContext#begin()} and
     * {@link RenderContext#end()} methods yourself, as well as disposing the RenderContext.
     *
     * @return True if this ModelBatch owns the RenderContext, false otherwise.
     */
    public boolean ownsRenderContext() {
        return ownContext;
    }

    /** @return the {@link RenderContext} used by this ModelBatch. */
    public RenderContext getRenderContext() {
        return context;
    }

    /** @return the {@link IntShaderProvider} used by this ModelBatch. */
    public IntShaderProvider getShaderProvider() {
        return shaderProvider;
    }

    /** @return the {@link IntRenderableSorter} used by this ModelBatch. */
    public IntRenderableSorter getRenderableSorter() {
        return sorter;
    }

    /**
     * Flushes the batch, causing all {@link IntRenderable}s in the batch to be rendered. Can only be called after the call to
     * {@link #begin(Camera)} and before the call to {@link #end()}.
     */
    public void flush() {
        sorter.sort(camera, renderables);
        IntShader currentShader = null;
        for (int i = 0; i < renderables.size; i++) {
            final IntRenderable renderable = renderables.get(i);
            if (currentShader != renderable.shader) {
                if (currentShader != null)
                    currentShader.end();
                currentShader = renderable.shader;
                currentShader.begin(camera, context);
            }
            currentShader.render(renderable);
        }
        if (currentShader != null)
            currentShader.end();
        renderablesPool.flush();
        renderables.clear();
    }

    /**
     * End rendering one or more {@link IntRenderable}s. Must be called after a call to {@link #begin(Camera)}. This will flush the
     * batch, causing any renderables provided using one of the render() methods to be rendered. After a call to this method the
     * OpenGL context can be altered again.
     */
    public void end() {
        flush();
        if (ownContext)
            context.end();
        camera = null;
    }

    /**
     * Cancels the current render operation without rendering anything. To use in case of a crash.
     */
    public void cancel() {
        renderablesPool.flush();
        renderables.clear();
        if (ownContext)
            context.end();
        camera = null;
    }

    /**
     * Add a single {@link IntRenderable} to the batch. The {@link IntShaderProvider} will be used to fetch a suitable {@link IntShader}.
     * Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderable The {@link IntRenderable} to be added.
     */
    public void render(final IntRenderable renderable) {
        renderable.shader = shaderProvider.getShader(renderable);
        renderable.meshPart.mesh.setAutoBind(false);
        renderables.add(renderable);
    }

    /**
     * Calls {@link RenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProvider the renderable provider
     */
    public void render(final IntRenderableProvider renderableProvider) {
        final int offset = renderables.size;
        renderableProvider.getRenderables(renderables, renderablesPool);
        for (int i = offset; i < renderables.size; i++) {
            IntRenderable renderable = renderables.get(i);
            renderable.shader = shaderProvider.getShader(renderable);
        }
    }

    /**
     * Calls {@link RenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProviders one or more renderable providers
     */
    public <T extends IntRenderableProvider> void render(final Iterable<T> renderableProviders) {
        for (final IntRenderableProvider renderableProvider : renderableProviders)
            render(renderableProvider);
    }

    /**
     * Calls {@link RenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Any environment set on the returned renderables will be replaced with the given environment.
     * Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProvider the renderable provider
     * @param environment        the {@link Environment} to use for the renderables
     */
    public void render(final IntRenderableProvider renderableProvider, final Environment environment) {
        final int offset = renderables.size;
        renderableProvider.getRenderables(renderables, renderablesPool);
        for (int i = offset; i < renderables.size; i++) {
            IntRenderable renderable = renderables.get(i);
            renderable.environment = environment;
            renderable.shader = shaderProvider.getShader(renderable);
        }
    }

    /**
     * Calls {@link IntRenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Any environment set on the returned renderables will be replaced with the given environment.
     * Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProviders one or more renderable providers
     * @param environment         the {@link Environment} to use for the renderables
     */
    public <T extends IntRenderableProvider> void render(final Iterable<T> renderableProviders, final Environment environment) {
        for (final IntRenderableProvider renderableProvider : renderableProviders)
            render(renderableProvider, environment);
    }

    /**
     * Calls {@link RenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Any shaders set on the returned renderables will be replaced with the given {@link IntShader}.
     * Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProvider the renderable provider
     * @param shader             the shader to use for the renderables
     */
    public void render(final IntRenderableProvider renderableProvider, final IntShader shader) {
        final int offset = renderables.size;
        renderableProvider.getRenderables(renderables, renderablesPool);
        for (int i = offset; i < renderables.size; i++) {
            IntRenderable renderable = renderables.get(i);
            renderable.shader = shader;
            renderable.shader = shaderProvider.getShader(renderable);
        }
    }

    /**
     * Calls {@link IntRenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Any shaders set on the returned renderables will be replaced with the given {@link IntShader}.
     * Can only be called after a call to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProviders one or more renderable providers
     * @param shader              the shader to use for the renderables
     */
    public <T extends IntRenderableProvider> void render(final Iterable<T> renderableProviders, final IntShader shader) {
        for (final IntRenderableProvider renderableProvider : renderableProviders)
            render(renderableProvider, shader);
    }

    /**
     * Calls {@link IntRenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Any environment set on the returned renderables will be replaced with the given environment.
     * Any shaders set on the returned renderables will be replaced with the given {@link IntShader}. Can only be called after a call
     * to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProvider the renderable provider
     * @param environment        the {@link Environment} to use for the renderables
     * @param shader             the shader to use for the renderables
     */
    public void render(final IntRenderableProvider renderableProvider, final Environment environment, final IntShader shader) {
        final int offset = renderables.size;
        renderableProvider.getRenderables(renderables, renderablesPool);
        for (int i = offset; i < renderables.size; i++) {
            IntRenderable renderable = renderables.get(i);
            renderable.environment = environment;
            renderable.shader = shader;
            renderable.shader = shaderProvider.getShader(renderable);
        }
    }

    /**
     * Calls {@link IntRenderableProvider#getRenderables(Array, Pool)} and adds all returned {@link IntRenderable} instances to the
     * current batch to be rendered. Any environment set on the returned renderables will be replaced with the given environment.
     * Any shaders set on the returned renderables will be replaced with the given {@link IntShader}. Can only be called after a call
     * to {@link #begin(Camera)} and before a call to {@link #end()}.
     *
     * @param renderableProviders one or more renderable providers
     * @param environment         the {@link Environment} to use for the renderables
     * @param shader              the shader to use for the renderables
     */
    public <T extends IntRenderableProvider> void render(final Iterable<T> renderableProviders, final Environment environment, final IntShader shader) {
        for (final IntRenderableProvider renderableProvider : renderableProviders)
            render(renderableProvider, environment, shader);
    }

    @Override
    public void dispose() {
        shaderProvider.dispose();
    }

    protected static class RenderablePool extends FlushablePool<IntRenderable> {
        @Override
        protected IntRenderable newObject() {
            return new IntRenderable();
        }

        @Override
        public IntRenderable obtain() {
            IntRenderable renderable = super.obtain();
            renderable.environment = null;
            renderable.material = null;
            renderable.meshPart.set("", null, 0, 0, 0);
            renderable.shader = null;
            renderable.userData = null;
            return renderable;
        }
    }
}
