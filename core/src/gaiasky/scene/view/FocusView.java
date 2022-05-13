package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
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
public class FocusView extends AbstractView implements IFocus {

    /** The base component. **/
    private Base base;
    /** The body component. **/
    private Body body;
    /** The graph component. **/
    private GraphNode graph;
    /** The octant component. **/
    private Octant octant;
    /** The magnitude component. **/
    private Magnitude mag;

    /** The particle set component, if any. **/
    private ParticleSet particleSet;
    /** The star set component, if any. **/
    private StarSet starSet;

    /** Creates an empty focus view. **/
    public FocusView() {
        super();
    }

    /**
     * Creates an abstract view with the given entity.
     *
     * @param entity The entity.
     */
    public FocusView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        check(entity, Mapper.base, Base.class);
        check(entity, Mapper.body, Body.class);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        this.base = Mapper.base.get(entity);
        this.body = Mapper.body.get(entity);
        this.graph = Mapper.graph.get(entity);
        this.octant = Mapper.octant.get(entity);
        this.mag = Mapper.magnitude.get(entity);
        this.particleSet = Mapper.particleSet.get(entity);
        this.starSet = Mapper.starSet.get(entity);
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
        return false;
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
        return false;
    }

    @Override
    public float[] getColor() {
        return body.color;
    }
}
