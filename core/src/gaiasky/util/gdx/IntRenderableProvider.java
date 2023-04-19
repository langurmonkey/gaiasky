/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public interface IntRenderableProvider {
    /**
     * Returns {@link IntRenderable} instances. Renderables are obtained from the provided {@link Pool} and added to the provided
     * array. The IntRenderables obtained using {@link Pool#obtain()} will later be put back into the pool, do not store them
     * internally. The resulting array can be rendered via a {@link IntModelBatch}.
     *
     * @param renderables the output array
     * @param pool        the pool to obtain IntRenderables from
     */
    void getRenderables(Array<IntRenderable> renderables, Pool<IntRenderable> pool);
}
