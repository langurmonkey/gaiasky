/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.math.Matrix4;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

public interface IGPUVertsRenderable extends IRenderable {
    boolean inGpu();

    void markForUpdate();

    int getOffset();

    int getCount();

    PointCloudData getPointCloud();

    float[] getColor();

    double getAlpha();

    Matrix4 getLocalTransform();

    SceneGraphNode getParent();

    void setInGpu(boolean inGpu);

    void setOffset(int offset);

    void setCount(int count);

    boolean isClosedLoop();

    void setClosedLoop(boolean closedLoop);

    void blend();
    void depth();

    /**
     * Line width for lines, point size for points
     *
     * @param size The size
     */
    void setPrimitiveSize(float size);

    float getPrimitiveSize();
}
