/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.IGPUVertsRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * Represents a group of vertices which are sent to the GPU in a VBO
 */
public class VertsObject extends SceneGraphNode implements IGPUVertsRenderable {

    protected boolean blend = true, depth = true, additive = true;

    protected int glPrimitive;

    /** The render group **/
    protected RenderGroup renderGroup;

    /** Whether to close the polyline (connect end point to start point) or not **/
    protected boolean closedLoop = true;

    // Line width
    protected float primitiveSize = 1f;

    protected PointCloudData pointCloudData;

    public VertsObject(RenderGroup rg, int glPrimitive) {
        super();
        this.renderGroup = rg;
        this.localTransform = new Matrix4();
        this.glPrimitive = glPrimitive;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && pointCloudData != null && pointCloudData.getNumPoints() > 0)
            addToRender(this, renderGroup);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        translation.getMatrix(localTransform);
    }

    /**
     * Sets the 3D points of the line in the internal reference system.
     *
     * @param points Vector with the points. If length is not multiple of 3, some points are discarded.
     */
    public void setPoints(double[] points) {
        int n = points.length;
        if (n % 3 != 0) {
            n = n - n % 3;
        }
        if (pointCloudData == null)
            pointCloudData = new PointCloudData(n / 3);
        else
            pointCloudData.clear();

        pointCloudData.addPoints(points);
        markForUpdate();
    }

    /**
     * Adds the given points to this data
     *
     * @param points The points to add
     */
    public void addPoints(double[] points) {
        if (pointCloudData == null) {
            setPoints(points);
        } else {
            pointCloudData.addPoints(points);
            markForUpdate();
        }
    }

    /**
     * Adds the given point ot this data
     *
     * @param point The point to add
     */
    public void addPoint(Vector3d point) {
        if (pointCloudData == null) {
            setPoints(point.values());
        } else {
            pointCloudData.addPoint(point);
            markForUpdate();
        }

    }

    public boolean isEmpty() {
        return pointCloudData.isEmpty();
    }

    /**
     * Clears the data from this object, both in RAM and VRAM
     */
    public void clear() {
        setPoints(new double[] {});
    }

    public void setPointCloudData(PointCloudData pcd){
        this.pointCloudData = pcd;
    }

    @Override
    public PointCloudData getPointCloud() {
        return pointCloudData;
    }

    @Override
    public float[] getColor() {
        return cc;
    }

    @Override
    public double getAlpha() {
        return cc[3];
    }

    @Override
    public Matrix4 getLocalTransform() {
        return localTransform;
    }

    @Override
    public SceneGraphNode getParent() {
        return parent;
    }

    @Override
    public void setPrimitiveSize(float lineWidth) {
        this.primitiveSize = lineWidth;
    }

    @Override
    public float getPrimitiveSize() {
        return primitiveSize;
    }

    @Override
    public boolean isClosedLoop() {
        return closedLoop;
    }

    @Override
    public void setClosedLoop(boolean closedLoop) {
        this.closedLoop = closedLoop;
    }

    public void setBlend(boolean blend) {
        this.blend = blend;
    }

    public void setAdditive(boolean additive) {
        this.additive = additive;
    }

    public void setDepth(boolean depth) {
        this.depth = depth;
    }

    @Override
    public void blend() {
        if (blend) {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            if(additive){
                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            }else {
                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        } else {
            Gdx.gl20.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void depth() {
        Gdx.gl20.glDepthMask(depth);
        if (depth) {
            Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        } else {
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);
        }
    }

    public void setGlPrimitive(int glPrimitive){
        this.glPrimitive = glPrimitive;
    }

    @Override
    public int getGlPrimitive() {
        return glPrimitive;
    }

    public void setPrimitiveLineStip() {
        glPrimitive = GL20.GL_LINE_STRIP;
    }

    public void setPrimitiveLines() {
        glPrimitive = GL20.GL_LINES;
    }

    public void setPrimitiveLineLoop() {
        glPrimitive = GL20.GL_LINE_LOOP;
    }

    public void setPrimitivePoints() {
        glPrimitive = GL20.GL_POINTS;
    }

    @Override
    public void markForUpdate() {
        EventManager.publish(Event.MARK_FOR_UPDATE, this, renderGroup);
    }

    public boolean isLine(){
        return renderGroup == RenderGroup.LINE_GPU;
    }

    public boolean isPoint(){
        return renderGroup == RenderGroup.POINT_GPU;
    }
}

