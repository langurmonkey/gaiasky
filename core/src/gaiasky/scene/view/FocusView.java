package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.system.update.ModelUpdater;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.IVisibilitySwitch;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.filter.attrib.IAttribute;
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
    /** The highlight component, initialized lazily. **/
    private Highlight hl;

    /** Implementation of pointer collision. **/
    private FocusHit focusHit;

    /** Reference to the scene. **/
    private Scene scene;

    /** The focus active computer. **/
    private FocusActive focusActive;

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3b B31 = new Vector3b();
    private final Vector3b B33 = new Vector3b();
    private final Matrix4 mataux = new Matrix4();
    private final Matrix4d matauxd = new Matrix4d();

    private ModelUpdater updater;

    /** Creates a focus view with the given scene. **/
    public FocusView(Scene scene) {
        super();
        this.scene = scene;
        this.focusHit = new FocusHit();
        this.focusActive = new FocusActive();
        this.updater = new ModelUpdater(null, 0);
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
        var set = getSet();
        if (set != null) {
            return set.getId();
        } else {
            return base.id;
        }
    }

    @Override
    public long getCandidateId() {
        var set = getSet();
        if (set != null) {
            return set.getCandidateId();
        } else {
            return base.id;
        }
    }

    @Override
    public String getLocalizedName() {
        var set = getSet();
        if (set != null) {
            return set.getLocalizedName();
        } else {
            return base.getLocalizedName();
        }
    }

    @Override
    public String getName() {
        var set = getSet();
        if (set != null) {
            var name = set.getName();
            if (name != null)
                return name;
        }
        return base.getName();
    }

    public void setName(String name) {
        base.setName(name);
    }

    @Override
    public String[] getNames() {
        var set = getSet();
        if (set != null) {
            var names = set.getNames();
            if (names != null)
                return names;
        }
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

    public int getNumParticles() {
        var set = getSet();
        if (set != null) {
            // Particles in set.
            return set.data().size();
        } else if (Mapper.octree.has(entity)) {
            // Number of objects in root node.
            return Mapper.octant.get(entity).octant.numObjectsRec;
        } else if (Mapper.datasetDescription.has(entity)) {
            if (graph.children != null && !graph.children.isEmpty()) {
                // Get number of children.
                return graph.numChildren;
            } else {
                // Only us.
                return 1;
            }
        } else {
            // Only us.
            return 1;
        }
    }

    public String getDataFile() {
        var set = getSet();
        if (set != null) {
            return set.datafile;
        } else {
            return null;
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

    public boolean isVisibleGroup() {
        return isVisibleGroup(false);
    }

    public boolean isVisibleGroup(boolean attributeValue) {
        return isVisible(attributeValue);
    }

    @Override
    public void setVisible(boolean visible, String name) {
        var set = getSet();
        if (set != null) {
            set.setVisible(visible, name, Mapper.render.get(entity));
        } else {
            setVisible(visible);
        }
    }

    public void setVisibleGroup(boolean visibility) {
        setVisible(visibility);
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

    @Override
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

    /**
     * Returns the cartesian position in the internal reference system above the
     * surface at the given longitude and latitude and distance.
     *
     * @param longitude The longitude in deg
     * @param latitude  The latitude in deg
     * @param distance  The distance in km
     * @param out       The vector to store the result
     *
     * @return The cartesian position above the surface of this body
     */
    public Vector3b getPositionAboveSurface(double longitude, double latitude, double distance, Vector3b out) {
        Vector3d aux1 = D31;
        Vector3d aux2 = D32;

        // Lon/Lat/Radius
        longitude *= MathUtilsd.degRad;
        latitude *= MathUtilsd.degRad;
        double rad = 1;
        Coordinates.sphericalToCartesian(longitude, latitude, rad, aux1);

        aux2.set(aux1.z, aux1.y, aux1.x).scl(1, -1, -1).scl(-(getRadius() + distance * Constants.KM_TO_U));
        //aux2.rotate(rc.angle, 0, 1, 0);
        var scaffolding = Mapper.modelScaffolding.get(entity);
        Matrix4d ori = new Matrix4d(graph.orientation);
        var rotation = getRotationComponent();
        if (rotation != null) {
            ori.rotate(0, 1, 0, rotation.angle);
        }
        aux2.mul(ori);

        getAbsolutePosition(out).add(aux2);
        return out;
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
        var set = getSet();
        if (set != null) {
            return set.getPosSph();
        } else {
            return body.posSph;
        }
    }

    /**
     * Whether position must be recomputed for this entity. By default, only
     * when time is on
     *
     * @param time The current time
     *
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
        var set = getSet();
        if (set != null) {
            return set.getDistToCamera();
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
    public double getSolidAngle() {
        var set = getSet();
        if (set != null) {
            return set.getSolidAngle();
        } else {
            return body.solidAngle;
        }
    }

    @Override
    public double getSolidAngleApparent() {
        var set = getSet();
        if (set != null) {
            return set.getSolidAngleApparent();
        } else {
            return body.solidAngleApparent;
        }
    }

    @Override
    public double getCandidateSolidAngleApparent() {
        var set = getSet();
        if (set != null) {
            return set.getCandidateSolidAngleApparent();
        } else {
            return getSolidAngleApparent();
        }
    }

    @Override
    public double getAlpha() {
        var set = getSet();
        if (set != null) {
            return set.getAlpha();
        } else {
            return body.posSph.x;
        }
    }

    @Override
    public double getDelta() {
        var set = getSet();
        if (set != null) {
            return set.getDelta();
        } else {
            return body.posSph.y;
        }
    }

    @Override
    public double getSize() {
        var set = getSet();
        if (set != null) {
            return set.getSize();
        } else {
            return body.size;
        }
    }

    @Override
    public double getRadius() {
        var set = getSet();
        if (set != null) {
            return set.getRadius();
        } else {
            return extra != null ? extra.radius : body.size / 2.0;
        }
    }

    @Override
    public double getHeight(Vector3b camPos) {
        if (isModel()) {
            return getHeight(camPos, false);
        } else {
            return getRadius();
        }
    }

    @Override
    public double getHeight(Vector3b camPos, boolean useFuturePosition) {
        if (isModel()) {
            if (useFuturePosition) {
                Vector3b nextPos = getPredictedPosition(B33, GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
                return getHeight(camPos, nextPos);
            } else {
                return getHeight(camPos, null);
            }
        } else {
            return getRadius();
        }
    }

    @Override
    public double getHeight(Vector3b camPos, Vector3b nextPos) {
        if (isModel()) {
            var model = Mapper.model.get(entity);
            var mc = model.model;
            double height = 0;
            if (mc != null && mc.mtc != null && mc.mtc.heightMap != null) {
                double dCam;
                Vector3b cart = B31;
                if (nextPos != null) {
                    cart.set(nextPos);
                    getPredictedPosition(cart, GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
                    dCam = D32.set(camPos).sub(cart).len();
                } else {
                    getAbsolutePosition(cart);
                    dCam = getDistToCamera();
                }
                // Only when we have height map and we are below the highest point in the surface
                if (dCam < getRadius() + mc.mtc.heightScale * Settings.settings.scene.renderer.elevation.multiplier * 4) {
                    float[][] m = mc.mtc.heightMap;
                    int W = mc.mtc.heightMap.length;
                    int H = mc.mtc.heightMap[0].length;

                    // Object-camera normalised vector
                    cart.scl(-1).add(camPos).nor();

                    updater.setToLocalTransform(entity, body, graph, 1, mataux, false);
                    mataux.inv();
                    matauxd.set(mataux.getValues());
                    cart.mul(matauxd);

                    Vector3d sph = D32;
                    Coordinates.cartesianToSpherical(cart, sph);

                    double x = (((sph.x * Nature.TO_DEG) + 270.0) % 360.0) / 360.0;
                    double y = 1d - (sph.y * Nature.TO_DEG + 90.0) / 180.0;

                    // Bilinear interpolation
                    int i1 = (int) (W * x);
                    int i2 = (i1 + 1) % W;
                    int j1 = (int) (H * y);
                    int j2 = (j1 + 1) % H;

                    double dx = 1.0 / W;
                    double dy = 1.0 / H;
                    double x1 = (double) i1 / (double) W;
                    double x2 = (x1 + dx) % 1.0;
                    double y1 = (double) j1 / (double) H;
                    double y2 = (y1 + dy) % 1.0;

                    double f11 = m[i1][j1];
                    double f21 = m[i2][j1];
                    double f12 = m[i1][j2];
                    double f22 = m[i2][j2];

                    double denominator = (x2 - x1) * (y2 - y1);
                    height = (((x2 - x) * (y2 - y)) / denominator) * f11 + ((x - x1) * (y2 - y) / denominator) * f21 + ((x2 - x) * (y - y1) / denominator) * f12 + ((x - x1) * (y - y1) / denominator) * f22;
                }
            }
            return getRadius() + height * Settings.settings.scene.renderer.elevation.multiplier;

        } else {
            return getRadius();
        }
    }

    @Override
    public double getHeightScale() {
        if (isModel()) {
            var model = Mapper.model.get(entity);
            var mc = model.model;
            if (mc != null && mc.mtc != null && mc.mtc.heightMap != null) {
                return mc.mtc.heightScale;
            }
        }
        return 0;
    }

    @Override
    public float getAppmag() {
        if (starSet != null) {
            return starSet.focus.appmag();
        } else if (particleSet != null) {
            return 0;
        } else if (mag != null) {
            return mag.appmag;
        } else {
            return 0;
        }
    }

    @Override
    public float getAbsmag() {
        if (starSet != null) {
            return starSet.focus.absmag();
        } else if (particleSet != null) {
            return 0;
        } else if (mag != null) {
            return mag.absmag;
        } else {
            return 0;
        }
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

    public int getHip() {
        if (starSet != null) {
            if (starSet.focus != null && starSet.focus.hip() > 0)
                return starSet.focus.hip();
        } else if (Mapper.hip.has(entity)) {
            return Mapper.hip.get(entity).hip;
        }
        return -1;
    }

    public void setForceLabel(Boolean forceLabel, String name) {
        if (starSet != null) {
            starSet.setForceLabel(forceLabel, name);
        } else {
            setForceLabel(forceLabel);
        }
    }

    public boolean isForceLabel(String name) {
        if (starSet != null) {
            return starSet.isForceLabel(name);
        } else {
            return base.forceLabel;
        }
    }

    /**
     * Sets the label color, as an RGBA float array.
     *
     * @param color The label color.
     */
    public void setLabelColor(float[] color) {
        body.labelColor = color;
    }

    public void setLabelColor(float[] color, String name) {
        if (starSet != null) {
            starSet.setLabelColor(color, name);
        } else {
            this.setLabelColor(color);
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

    public boolean isModel() {
        return Mapper.modelScaffolding.has(entity);
    }

    public boolean isCluster() {
        return Mapper.cluster.has(entity);
    }

    public boolean isCelestial() {
        return Mapper.celestial.has(entity);
    }

    public boolean hasProperMotion() {
        return Mapper.pm.has(entity) || starSet != null;
    }

    public double getMuAlpha() {
        if (starSet != null) {
            if (starSet.focus != null)
                return starSet.focus.mualpha();
            else
                return 0;
        } else if (Mapper.pm.has(entity)) {
            var pm = Mapper.pm.get(entity);
            if (pm.pmSph != null) {
                return pm.pmSph.x;
            } else {
                return 0;
            }
        }
        return 0;
    }

    public double getMuDelta() {
        if (starSet != null) {
            if (starSet.focus != null)
                return starSet.focus.mudelta();
            else
                return 0;
        } else if (Mapper.pm.has(entity)) {
            var pm = Mapper.pm.get(entity);
            if (pm.pmSph != null) {
                return pm.pmSph.y;
            } else {
                return 0;
            }
        }
        return 0;
    }

    public double getRadialVelocity() {
        if (starSet != null) {
            if (starSet.focus != null)
                return starSet.focus.radvel();
            else
                return 0;
        } else if (Mapper.pm.has(entity)) {
            var pm = Mapper.pm.get(entity);
            if (pm.pmSph != null) {
                return pm.pmSph.z;
            } else {
                return 0;
            }
        }
        return 0;
    }

    public Entity getChildByNameAndArchetype(String name, Archetype archetype) {
        int size = graph.children.size;
        for (int i = 0; i < size; i++) {
            Entity child = graph.children.get(i);
            if (child != null) {
                var base = Mapper.base.get(child);
                if (base.getName().equalsIgnoreCase(name) && archetype.matches(child)) {
                    return child;
                }
            }
        }
        return null;
    }

    private void initHighlight() {
        if (hl == null)
            hl = Mapper.highlight.get(entity);
    }

    /**
     * Highlight using a plain color.
     *
     * @param state      Whether to highlight.
     * @param color      The plain color.
     * @param allVisible All visible.
     */
    public void highlight(boolean state, float[] color, boolean allVisible) {
        initHighlight();
        var set = getSet();
        if (set != null) {
            set.markForUpdate(Mapper.render.get(entity));
        }

        this.hl.highlighted = state;
        if (state) {
            hl.hlplain = true;
            hl.hlallvisible = allVisible;
            System.arraycopy(color, 0, hl.hlc, 0, color.length);
        }
    }

    /**
     * Highlight using a colormap.
     *
     * @param state Whether to highlight.
     * @param cmi   Color map index.
     * @param cma   Color map attribute.
     * @param cmmin Min mapping value.
     * @param cmmax Max mapping value.
     */
    public void highlight(boolean state, int cmi, IAttribute cma, double cmmin, double cmmax, boolean allVisible) {
        initHighlight();
        var set = getSet();
        if (set != null) {
            set.markForUpdate(Mapper.render.get(entity));
        }

        hl.highlighted = state;
        if (state) {
            hl.hlplain = false;
            hl.hlallvisible = allVisible;
            hl.hlcmi = cmi;
            hl.hlcma = cma;
            hl.hlcmmin = cmmin;
            hl.hlcmmax = cmmax;
        }
    }

    public boolean isHighlighted() {
        initHighlight();
        return hl.highlighted;
    }
}
