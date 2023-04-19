/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Array;

public interface IntRenderableSorter {
    /**
     * Sorts the array of {@link IntRenderable} instances based on some criteria, e.g. material, distance to camera etc.
     *
     * @param renderables the array of renderables to be sorted
     */
    void sort(Camera camera, Array<IntRenderable> renderables);
}
