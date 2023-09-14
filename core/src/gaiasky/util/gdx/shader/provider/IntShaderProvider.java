/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.provider;

import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.IntShader;

public interface IntShaderProvider {
    /**
     * Returns a {@link IntShader} for the given {@link IntRenderable}. The RenderInstance may already contain a IntShader, in which case
     * the provider may decide to return that.
     *
     * @param renderable the Renderable
     *
     * @return the IntShader to be used for the RenderInstance
     */
    IntShader getShader(IntRenderable renderable);

    /** Disposes all resources created by the provider */
    void dispose();
}
