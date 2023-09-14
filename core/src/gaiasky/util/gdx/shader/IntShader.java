/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.IntRenderable;

public interface IntShader extends Disposable {
    /**
     * Initializes the IntShader, must be called before the IntShader can be used. This typically compiles a {@link ShaderProgram},
     * fetches uniform locations and performs other preparations for usage of the IntShader.
     */
    void init();

    /** Compare this shader against the other, used for sorting, light weight shaders are rendered first. */
    int compareTo(IntShader other); // TODO: probably better to add some weight value to sort on

    /**
     * Checks whether this shader is intended to render the {@link IntRenderable}. Use this to make sure a call to the
     * {@link #render(IntRenderable)} method will succeed. This is expected to be a fast, non-blocking method. Note that this method
     * will only return true if it is intended to be used. Even when it returns false the IntShader might still be capable of
     * rendering, but it's not preferred to do so.
     *
     * @param instance The renderable to check against this shader.
     *
     * @return true if this shader is intended to render the {@link IntRenderable}, false otherwise.
     */
    boolean canRender(IntRenderable instance);

    /**
     * Initializes the context for exclusive rendering by this shader. Use the {@link #render(IntRenderable)} method to render a
     * {@link IntRenderable}. When done rendering the {@link #end()} method must be called.
     *
     * @param camera  The camera to use when rendering
     * @param context The context to be used, which must be exclusive available for the shader until the call to the {@link #end()}
     *                method.
     */
    void begin(Camera camera, RenderContext context);

    /**
     * Renders the {@link IntRenderable}, must be called between {@link #begin(Camera, RenderContext)} and {@link #end()}. The IntShader
     * instance might not be able to render every type of {@link IntRenderable}s. Use the {@link #canRender(IntRenderable)} method to
     * check if the IntShader is capable of rendering a specific {@link IntRenderable}.
     *
     * @param renderable The renderable to render, all required fields (e.g. {@link IntRenderable#material} and others) must be set.
     *                   The {@link IntRenderable#shader} field will be ignored.
     */
    void render(final IntRenderable renderable);

    /**
     * Cleanup the context so other shaders can render. Must be called when done rendering using the {@link #render(IntRenderable)}
     * method, which must be preceded by a call to {@link #begin(Camera, RenderContext)}. After a call to this method an call to
     * the {@link #render(IntRenderable)} method will fail until the {@link #begin(Camera, RenderContext)} is called.
     */
    void end();
}
