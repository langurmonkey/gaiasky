/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IGPUVertsRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.component.Verts;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.util.math.Vector3D;

public class VertsView extends BaseView implements IGPUVertsRenderable {

    private final TrajectoryUtils utils;
    /**
     * The verts component .
     **/
    private Verts verts;
    /**
     * The graph component.
     **/
    private GraphNode graph;
    /**
     * The trajectory component (if any).
     **/
    private Trajectory trajectory;

    public VertsView() {
        super();
        this.utils = new TrajectoryUtils();
    }

    public VertsView(Entity entity) {
        super(entity);
        this.utils = new TrajectoryUtils();
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.verts, Verts.class);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.verts = Mapper.verts.get(entity);
        this.graph = Mapper.graph.get(entity);
        this.trajectory = Mapper.trajectory.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.verts = null;
        this.graph = null;
        this.trajectory = null;
    }

    @Override
    public void markForUpdate() {
        EventManager.publish(Event.GPU_DISPOSE_VERTS_OBJECT, Mapper.render.get(entity), verts.renderGroup);
        if (trajectory != null) {
            utils.updateSize(body, trajectory, verts);
        }
    }

    @Override
    public PointCloudData getPointCloud() {
        return verts.pointCloudData;
    }

    @Override
    public float[] getColor() {
        return body.color;
    }

    @Override
    public double getAlpha() {
        return trajectory != null ? trajectory.alpha : body.color[3];
    }

    @Override
    public Matrix4 getLocalTransform() {
        return graph.localTransform;
    }

    @Override
    public Entity getParentEntity() {
        return graph.parent;
    }

    @Override
    public boolean isClosedLoop() {
        return trajectory != null ? trajectory.closedLoop : verts.closedLoop;
    }

    @Override
    public void setClosedLoop(boolean closedLoop) {
        if (trajectory != null) {
            trajectory.closedLoop = closedLoop;
        }
        verts.closedLoop = closedLoop;
    }

    @Override
    public void blend() {
        EntityUtils.blend(verts);
    }

    @Override
    public void depth() {
        EntityUtils.depth(verts);
    }

    @Override
    public int getGlPrimitive() {
        return verts.glPrimitive;
    }

    public void setGlPrimitive(int glPrimitive) {
        verts.glPrimitive = glPrimitive;
    }

    @Override
    public float getPrimitiveSize() {
        return verts.primitiveSize;
    }

    @Override
    public void setPrimitiveSize(float size) {
        verts.primitiveSize = size;
    }

    @Override
    public double getDistToCamera() {
        return body.distToCamera;
    }

    @Override
    public float getOpacity() {
        return base.opacity;
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
        if (verts.pointCloudData == null)
            verts.pointCloudData = new PointCloudData(n / 3);
        else
            verts.pointCloudData.clear();

        verts.pointCloudData.addPoints(points);
        markForUpdate();
    }

    /**
     * Adds the given points to this data
     *
     * @param points The points to add
     */
    public void addPoints(double[] points) {
        if (verts.pointCloudData == null) {
            setPoints(points);
        } else {
            verts.pointCloudData.addPoints(points);
            markForUpdate();
        }
    }

    /**
     * Adds the given point ot this data
     *
     * @param point The point to add
     */
    public void addPoint(Vector3D point) {
        if (verts.pointCloudData == null) {
            setPoints(point.values());
        } else {
            verts.pointCloudData.addPoint(point);
            markForUpdate();
        }
    }

    public void setRenderGroup(RenderGroup renderGroup) {
        verts.renderGroup = renderGroup;
    }

    public boolean isEmpty() {
        return verts.pointCloudData.isEmpty();
    }

    /**
     * Clears the data from this object, both in RAM and VRAM
     */
    public void clear() {
        setPoints(new double[]{});
    }
}
