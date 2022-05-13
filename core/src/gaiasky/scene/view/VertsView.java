package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IGPUVertsRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scenegraph.SceneGraphNode;

/**
 * A view which exposes common vertex buffer renderable operations.
 * Can be reused for multiple entities by using {@link #setEntity(Entity)}.
 */
public class VertsView extends AbstractView implements IGPUVertsRenderable {

    /** The verts component . **/
    private Verts verts;
    /** The base component. **/
    private Base base;
    /** The body component. **/
    private Body body;
    /** The graph component. **/
    private GraphNode graph;
    /** The trajectory component (if any). **/
    private Trajectory trajectory;

    public VertsView() {
        super();
    }

    public VertsView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        check(entity, Mapper.base, Base.class);
        check(entity, Mapper.body, Body.class);
        check(entity, Mapper.verts, Verts.class);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        this.verts = Mapper.verts.get(entity);
        this.base = Mapper.base.get(entity);
        this.body = Mapper.body.get(entity);
        this.graph = Mapper.graph.get(entity);
        this.trajectory = Mapper.trajectory.get(entity);
    }

    @Override
    public void markForUpdate() {
        EventManager.publish(Event.GPU_DISPOSE_VERTS_OBJECT, Mapper.render.get(entity), verts.renderGroup);
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
    public SceneGraphNode getParent() {
        return null;
    }

    @Override
    public Entity getParentEntity() {
        return graph.parent;
    }

    @Override
    public boolean isClosedLoop() {
        return verts.closedLoop;
    }

    @Override
    public void setClosedLoop(boolean closedLoop) {
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

    @Override
    public void setPrimitiveSize(float size) {
        verts.primitiveSize = size;
    }

    @Override
    public float getPrimitiveSize() {
        return verts.primitiveSize;
    }

    @Override
    public ComponentTypes getComponentType() {
        return base.ct;
    }

    @Override
    public double getDistToCamera() {
        return body.distToCamera;
    }

    @Override
    public float getOpacity() {
        return base.opacity;
    }
}
