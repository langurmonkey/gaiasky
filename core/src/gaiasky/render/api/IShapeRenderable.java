/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaiasky.render.RenderingContext;
import gaiasky.scene.camera.ICamera;

public interface IShapeRenderable extends IRenderable {

    /**
     * Renders the shape(s).
     */
    void render(ShapeRenderer shapeRenderer, RenderingContext rc, float alpha, ICamera camera);
}
