package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

/**
 * Special component that marks an entity as focusable.
 * Maintains a few references to the entity and some components.
 */
public class Focus implements Component, IFocus {

    public Entity entity;
    public Base base;
    public Body body;

    @Override
    public long getId() {
        return Mapper.id.get(entity).id;
    }

    @Override
    public long getCandidateId() {
        return 0;
    }

    @Override
    public String getLocalizedName() {
        return base.getLocalizedName();
    }

    @Override
    public String getName() {
        return base.getName();
    }

    @Override
    public String[] getNames() {
        return base.names;
    }

    @Override
    public boolean hasName(String name) {
        return base.hasName(name);
    }

    @Override
    public boolean hasName(String name, boolean matchCase) {
        return base.hasName(name, matchCase);
    }

    @Override
    public String getClosestName() {
        return null;
    }

    @Override
    public String getCandidateName() {
        return null;
    }

    @Override
    public ComponentTypes getCt() {
        return base.ct;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Vector3b getPos() {
        return body.pos;
    }

    @Override
    public SceneGraphNode getFirstStarAncestor() {
        return null;
    }

    @Override
    public Vector3b getAbsolutePosition(Vector3b out) {
        out.set(body.pos);
        Entity entity = this.entity;
        GraphNode graph = Mapper.graph.get(entity);
        while (graph.parent != null) {
            entity = graph.parent;
            graph = Mapper.graph.get(entity);
            out.add(Mapper.body.get(entity).pos);
        }
        return out;
    }

    @Override
    public Vector3b getAbsolutePosition(String name, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        return null;
    }

    @Override
    public Vector2d getPosSph() {
        return body.posSph;
    }

    @Override
    public IFocus getNext(ITimeFrameProvider time, ICamera camera, boolean force) {
        return null;
    }

    @Override
    public Vector3b getPredictedPosition(Vector3b aux, ITimeFrameProvider time, ICamera camera, boolean force) {
        return null;
    }

    @Override
    public double getDistToCamera() {
        return body.distToCamera;
    }

    @Override
    public double getClosestDistToCamera() {
        return 0;
    }

    @Override
    public double getViewAngle() {
        return body.viewAngle;
    }

    @Override
    public double getViewAngleApparent() {
        return body.viewAngleApparent;
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return 0;
    }

    @Override
    public double getAlpha() {
        return body.posSph.x;
    }

    @Override
    public double getDelta() {
        return body.posSph.x;
    }

    @Override
    public double getSize() {
        return body.size;
    }

    @Override
    public double getRadius() {
        return body.size / 2.0;
    }

    @Override
    public double getHeight(Vector3b camPos) {
        return 0;
    }

    @Override
    public double getHeight(Vector3b camPos, boolean useFuturePosition) {
        return 0;
    }

    @Override
    public double getHeight(Vector3b camPos, Vector3b nextPos) {
        return 0;
    }

    @Override
    public double getHeightScale() {
        return 0;
    }

    @Override
    public float getAppmag() {
        return Mapper.magnitude.has(entity) ? Mapper.magnitude.get(entity).appmag : 0;
    }

    @Override
    public float getAbsmag() {
        return Mapper.magnitude.has(entity) ? Mapper.magnitude.get(entity).absmag : 0;
    }

    @Override
    public Matrix4d getOrientation() {
        return Mapper.graph.get(entity).orientation;
    }

    @Override
    public RotationComponent getRotationComponent() {
        return Mapper.rotation.has(entity) ? Mapper.rotation.get(entity).rc : null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    @Override
    public void addHit(int screenX, int screenY, int w, int h, int pxdist, NaturalCamera camera, Array<IFocus> hits) {

    }

    @Override
    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {

    }

    @Override
    public void makeFocus() {

    }

    @Override
    public IFocus getFocus(String name) {
        return null;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return Mapper.coordinates.has(entity) ? Mapper.coordinates.get(entity).timeOverflow : false;
    }

    @Override
    public int getSceneGraphDepth() {
        var graph = Mapper.graph.get(entity);
        if (graph.parent == null) {
            return 0;
        } else {
            return Mapper.focus.get(entity).getSceneGraphDepth() + 1;
        }
    }

    @Override
    public OctreeNode getOctant() {
        return Mapper.octant.get(entity).octant;
    }

    @Override
    public boolean isCopy() {
        return false;
    }

    @Override
    public float[] getColor() {
        return body.color;
    }
}
