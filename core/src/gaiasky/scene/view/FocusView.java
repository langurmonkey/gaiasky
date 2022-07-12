package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.IVisibilitySwitch;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Settings;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

/**
 * An entity view that implements the {@link IFocus} methods.
 */
public class FocusView extends BaseView implements IFocus, IVisibilitySwitch {

    /** Focus component. **/
    private Focus focus;
    /** The graph component. **/
    private GraphNode graph;
    /** The octant component. **/
    private Octant octant;
    /** The magnitude component. **/
    private Magnitude mag;
    /** Particle component, maybe. **/
    protected ParticleExtra extra;

    /** The particle set component, if any. **/
    private ParticleSet particleSet;
    /** The star set component, if any. **/
    private StarSet starSet;

    /** Implementation of pointer collision. **/
    private FocusHit focusHit;

    /** Reference to the scene. **/
    private Scene scene;

    /** The focus active computer. **/
    private FocusActive focusActive;

    /** Creates a focus view with the given scene. **/
    public FocusView(Scene scene) {
        super();
        this.scene = scene;
        this.focusHit = new FocusHit();
        this.focusActive = new FocusActive();
    }

    /** Creates an empty focus view. **/
    public FocusView() {
        this((Scene) null);
    }

    /**
     * Creates an abstract view with the given entity.
     *
     * @param entity The entity.
     */
    public FocusView(Entity entity) {
        super(entity);
        this.focusHit = new FocusHit();
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.focus = Mapper.focus.get(entity);
        this.graph = Mapper.graph.get(entity);
        this.octant = Mapper.octant.get(entity);
        this.mag = Mapper.magnitude.get(entity);
        this.extra = Mapper.extra.get(entity);
        this.particleSet = Mapper.particleSet.get(entity);
        this.starSet = Mapper.starSet.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.focus = null;
        this.graph = null;
        this.octant = null;
        this.mag = null;
        this.extra = null;
        this.particleSet = null;
        this.starSet = null;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public boolean isParticle() {
        return extra != null;
    }

    @Override
    public long getId() {
        return base.id;
    }

    @Override
    public long getCandidateId() {
        return base.id;
    }

    @Override
    public String getLocalizedName() {
        return base.getLocalizedName();
    }

    @Override
    public String getName() {
        return base.getName();
    }

    public void setName(String name) {
        base.setName(name);
    }

    @Override
    public String getDescription() {
        if (Mapper.datasetDescription.has(entity)) {
            return Mapper.datasetDescription.get(entity).description;
        }
        return null;
    }

    @Override
    public void setDescription(String description) {
        if (Mapper.datasetDescription.has(entity)) {
            Mapper.datasetDescription.get(entity).description = description;
        }
    }

    @Override
    public boolean isVisible(String name) {
        var set = getSet();
        if (set != null && set.index.containsKey(name)) {
            return set.isVisible(set.index.get(name));
        } else {
            return isVisible();
        }
    }

    @Override
    public void setVisible(boolean visible, String name) {
        var set = getSet();
        if (set != null) {
            set.setVisible(set.index.get(name), visible, Mapper.render.get(entity));
        } else {
            setVisible(visible);
        }
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
        if (getSet() != null) {
            return getSet().getClosestName();
        } else {
            return getName();
        }
    }

    @Override
    public String getCandidateName() {
        if (getSet() != null) {
            return getSet().getCandidateName();
        } else {
            return getName();
        }
    }

    @Override
    public ComponentTypes getCt() {
        return base.ct;
    }

    @Override
    public boolean isFocusActive() {
        return focus.activeFunction.apply(focusActive, entity, base);
    }

    @Override
    public Vector3b getPos() {
        return body.pos;
    }

    @Override
    public IFocus getFirstStarAncestor() {
        var out = getFirstStarAncestorEntity();
        if (out == null) {
            return null;
        } else if (out == entity) {
            return this;
        } else {
            return new FocusView(out);
        }
    }

    public IFocus getFirstStarAncestor(FocusView view) {
        var out = getFirstStarAncestorEntity();
        if (out == null) {
            return null;
        } else {
            view.setEntity(out);
            return view;
        }
    }

    public Entity getFirstStarAncestorEntity() {
        if (Mapper.hip.has(entity) || starSet != null || particleSet != null) {
            return entity;
        } else if (graph.parent != null) {
            return getStarAncestor(graph.parent);
        } else {
            return null;
        }
    }

    private Entity getStarAncestor(Entity me) {
        if (me == null) {
            return null;
        }
        if (Mapper.hip.has(me)) {
            return me;
        } else if (Mapper.graph.has(me)) {
            var graph = Mapper.graph.get(me);
            return getStarAncestor(graph.parent);
        } else {
            return null;
        }
    }

    @Override
    public Vector3b getAbsolutePosition(Vector3b out) {
        if (getSet() != null) {
            return getSet().getAbsolutePosition(out);
        } else {
            return EntityUtils.getAbsolutePosition(entity, out);
        }
    }

    @Override
    public Vector3b getAbsolutePosition(String name, Vector3b out) {
        if (getSet() != null) {
            return getSet().getAbsolutePosition(name, out);
        } else {
            return EntityUtils.getAbsolutePosition(entity, out);
        }
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        if (starSet != null) {
            return out.set(starSet.proximity.updating[0].absolutePos);
        } else {
            return getAbsolutePosition(out);
        }
    }

    @Override
    public Vector2d getPosSph() {
        return body.posSph;
    }

    /**
     * Whether position must be recomputed for this entity. By default, only
     * when time is on
     *
     * @param time The current time
     * @return True if position should be recomputed for this entity
     */
    protected boolean mustUpdatePosition(ITimeFrameProvider time) {
        return time.getHdiff() != 0;
    }

    @Override
    public Vector3b getPredictedPosition(Vector3b out, ITimeFrameProvider time, ICamera camera, boolean force) {
        if (getSet() != null) {
            return getSet().getAbsolutePosition(out);
        } else {
            if (!mustUpdatePosition(time) && !force) {
                return getAbsolutePosition(out);
            } else {
                // Get a line copy of focus and update it.
                var copy = scene.getLineCopy(entity);

                // Get root of line copy.
                var copyGraph = Mapper.graph.get(copy);
                var root = copyGraph.getRoot(copy);
                // This updates the graph node for all entities in the line.
                scene.updateEntity(root, (float) time.getHdiff());

                // This updates the rest of components of our entity.
                scene.updateEntity(copy, (float) time.getHdiff());

                EntityUtils.getAbsolutePosition(copy, out);

                // Return all to pool.
                var currentEntity = copy;
                do {
                    var graph = Mapper.graph.get(currentEntity);
                    var parent = graph.parent;
                    ((Poolable) currentEntity).reset();
                    currentEntity = parent;
                } while (currentEntity != null);

                return out;
            }
        }
    }

    @Override
    public double getDistToCamera() {
        if (getSet() != null) {
            return getSet().focusDistToCamera;
        } else {
            return body.distToCamera;
        }
    }

    @Override
    public double getClosestDistToCamera() {
        if (starSet != null) {
            return starSet.getClosestDistToCamera();
        } else {
            return getDistToCamera();
        }
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
        if (getSet() != null) {
            return getSet().getCandidateViewAngleApparent();
        } else {
            return getViewAngleApparent();
        }
    }

    @Override
    public double getAlpha() {
        return body.posSph.x;
    }

    @Override
    public double getDelta() {
        return body.posSph.y;
    }

    @Override
    public double getSize() {
        return body.size;
    }

    @Override
    public double getRadius() {
        return extra != null ? extra.radius : body.size / 2.0;
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
        return mag.appmag;
    }

    @Override
    public float getAbsmag() {
        return mag.absmag;
    }

    @Override
    public Matrix4d getOrientation() {
        return graph.orientation;
    }

    @Override
    public RotationComponent getRotationComponent() {
        return Mapper.rotation.has(entity) ? Mapper.rotation.get(entity).rc : null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        if (Mapper.attitude.has(entity)) {
            var attitude = Mapper.attitude.get(entity);
            if (attitude.attitude != null) {
                return attitude.attitude.getQuaternion();
            }
        }
        return null;
    }

    @Override
    public void addHitCoordinate(int screenX, int screenY, int w, int h, int pixelDist, NaturalCamera camera, Array<IFocus> hits) {

    }

    @Override
    public void addEntityHitCoordinate(int screenX, int screenY, int w, int h, int pixelDist, NaturalCamera camera, Array<Entity> hits) {
        if (focus != null && focus.hitCoordinatesConsumer != null) {
            focus.hitCoordinatesConsumer.apply(focusHit, this, screenX, screenY, w, h, pixelDist, camera, hits);
        }
    }

    @Override
    public void addHitRay(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {

    }

    @Override
    public void addEntityHitRay(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<Entity> hits) {
        if (focus != null && focus.hitRayConsumer != null) {
            focus.hitRayConsumer.apply(focusHit, this, p0, p1, camera, hits);
        }
    }

    @Override
    public void makeFocus() {
        if (particleSet != null) {
            particleSet.makeFocus();
        } else if (starSet != null) {
            starSet.makeFocus();
        }
    }

    @Override
    public IFocus getFocus(String name) {
        if (particleSet != null) {
            particleSet.setFocusIndex(name);
        } else if (starSet != null) {
            starSet.setFocusIndex(name);
        }
        return this;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return Mapper.coordinates.has(entity) && Mapper.coordinates.get(entity).timeOverflow;
    }

    @Override
    public int getSceneGraphDepth() {
        return graph.getSceneGraphDepth();
    }

    @Override
    public OctreeNode getOctant() {
        return octant != null ? octant.octant : null;
    }

    public void setForceLabel(Boolean forceLabel, String name) {
        if (starSet != null) {
            starSet.setForceLabel(forceLabel, name);
        } else {
            setForcelabel(forceLabel);
        }
    }

    public boolean isForceLabel(String name) {
        if (starSet != null) {
            return starSet.isForceLabel(name);
        } else {
            return base.forceLabel;
        }
    }

    @Override
    public boolean isCopy() {
        return base.copy;
    }

    @Override
    public float[] getColor() {
        return body.color;
    }

    public GraphNode getGraph() {
        return graph;
    }

    public Magnitude getMag() {
        return mag;
    }

    public ParticleExtra getExtra() {
        return extra;
    }

    public ParticleSet getParticleSet() {
        return particleSet;
    }

    public StarSet getStarSet() {
        return starSet;
    }

    public ParticleSet getSet() {
        return particleSet != null ? particleSet : starSet;
    }
}
