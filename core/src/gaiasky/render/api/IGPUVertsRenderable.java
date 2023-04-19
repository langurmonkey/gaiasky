/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.data.util.PointCloudData;

public interface IGPUVertsRenderable extends IRenderable {

    void markForUpdate();

    PointCloudData getPointCloud();

    float[] getColor();

    double getAlpha();

    Matrix4 getLocalTransform();

    Entity getParentEntity();

    boolean isClosedLoop();

    void setClosedLoop(boolean closedLoop);

    void blend();

    void depth();

    int getGlPrimitive();

    float getPrimitiveSize();

    /**
     * Line width for lines, point size for points
     *
     * @param size The size
     */
    void setPrimitiveSize(float size);
}
