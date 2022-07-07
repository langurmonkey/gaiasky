package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

/**
 * An entity view that implements the {@link IFocus} methods.
 */
public class FocusView extends BaseView implements IFocus {

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

    /** Creates an empty focus view. **/
    public FocusView() {
        super();
        focusHit = new FocusHit();
    }

    /**
     * Creates an abstract view with the given entity.
     *
     * @param entity The entity.
     */
    public FocusView(Entity entity) {
        super(entity);
        focusHit = new FocusHit();
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
    public boolean isFocusActive() {
        return false;
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
        return EntityUtils.getAbsolutePosition(entity, out);
    }

    @Override
    public Vector3b getAbsolutePosition(String name, Vector3b out) {
        return base.hasName(name) ? getAbsolutePosition(out) : null;
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
        return null;
    }

    @Override
    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    @Override
    public void addHitCoordinate(int screenX, int screenY, int w, int h, int pixelDist, NaturalCamera camera, Array<IFocus> hits) {
        if(focus != null && focus.hitCoordinatesConsumer != null) {
            focus.hitCoordinatesConsumer.apply(focusHit, this, screenX, screenY, w, h, pixelDist, camera, hits);
        }
    }

    @Override
    public void addHitRay(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        if(focus != null && focus.hitRayConsumer != null) {
            focus.hitRayConsumer.apply(focusHit, this, p0, p1, camera, hits);
        }
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
        return Mapper.coordinates.has(entity) && Mapper.coordinates.get(entity).timeOverflow;
    }

    @Override
    public int getSceneGraphDepth() {
        return graph.getSceneGraphDepth();
    }

    @Override
    public OctreeNode getOctant() {
        return octant.octant;
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
