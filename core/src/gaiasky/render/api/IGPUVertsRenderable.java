/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.data.util.PointCloudData;
import gaiasky.scenegraph.SceneGraphNode;

public interface IGPUVertsRenderable extends IRenderable {

    void markForUpdate();

    PointCloudData getPointCloud();

    float[] getColor();

    double getAlpha();

    Matrix4 getLocalTransform();

    SceneGraphNode getParent();

    boolean isClosedLoop();

    void setClosedLoop(boolean closedLoop);

    void blend();
    void depth();

    int getGlPrimitive();

    /**
     * Line width for lines, point size for points
     *
     * @param size The size
     */
    void setPrimitiveSize(float size);

    float getPrimitiveSize();
}
