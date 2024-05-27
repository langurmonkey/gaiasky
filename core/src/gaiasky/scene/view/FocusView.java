/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IVisibilitySwitch;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.component.*;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.record.RotationComponent;
import gaiasky.scene.system.update.ModelUpdater;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.tree.OctreeNode;

public class FocusView extends BaseView implements IFocus, IVisibilitySwitch {

    private final Vector3d D31 = new Vector3d();
    private final Vector3d D32 = new Vector3d();
    private final Vector3b B31 = new Vector3b();
    private final Vector3b B33 = new Vector3b();
    private final Matrix4 matAux = new Matrix4();
    private final Matrix4d matDAux = new Matrix4d();
    /**
     * Particle component, maybe.
     **/
    protected ParticleExtra extra;
    /**
     * Focus component.
     **/
    private Focus focus;
    /**
     * The graph component.
     **/
    private GraphNode graph;
    /**
     * The octant component.
     **/
    private Octant octant;
    /**
     * The magnitude component.
     **/
    private Magnitude mag;
    /**
     * The particle set component, if any.
     **/
    private ParticleSet particleSet;
    /**
     * The star set component, if any.
     **/
    private StarSet starSet;
    /**
     * The highlight component, initialized lazily.
     **/
    private Highlight hl;
    /**
     * Implementation of pointer collision.
     **/
    private final FocusHit focusHit;
    /**
     * Reference to the scene.
     **/
    private Scene scene;
    /**
     * The focus active computer.
     **/
    private FocusActive focusActive;
    private FocusView auxView;
    private ModelUpdater updater;

    /**
     * Creates a focus view with the given scene.
     **/
    public FocusView(Scene scene) {
        super();
        this.scene = scene;
        this.focusHit = new FocusHit();
        this.focusActive = new FocusActive();
        this.updater = new ModelUpdater(null, 0);
    }

    /**
     * Creates an empty focus view.
     **/
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
        this.hl = null;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public boolean isParticle() {
        return isValid() && extra != null;
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
            String particleName = set.getLocalizedName();
            if (particleName == null) {
                return base.getLocalizedName();
            } else {
                return particleName;
            }
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
    public boolean hasName(String name,
                           boolean matchCase) {
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
        if (set != null && set.data() != null) {
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

    public void setVisibleGroup(boolean visibility) {
        setVisible(visibility);
    }

    public boolean isVisibleGroup(boolean attributeValue) {
        return isVisible(attributeValue);
    }

    @Override
    public void setVisible(boolean visible,
                           String name) {
        var set = getSet();
        if (set != null) {
            set.setVisible(visible, name, Mapper.render.get(entity));
        } else {
            setVisible(visible);
        }
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
        return getStarAncestor(entity);
    }

    private Entity getStarAncestor(Entity me) {
        if (me == null) {
            return null;
        }
        if (Mapper.hip.has(me) || Mapper.starSet.has(me) || Mapper.particleSet.has(me)) {
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
    public Vector3b getAbsolutePosition(String name,
                                        Vector3b out) {
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
     */
    public void getPositionAboveSurface(double longitude,
                                        double latitude,
                                        double distance,
                                        Vector3b out) {
        Vector3d aux1 = D31;
        Vector3d aux2 = D32;

        // Lon/Lat/Radius
        longitude *= MathUtilsDouble.degRad;
        latitude *= MathUtilsDouble.degRad;
        double rad = 1;
        Coordinates.sphericalToCartesian(longitude, latitude, rad, aux1);

        aux2.set(aux1.z, aux1.y, aux1.x).scl(1, -1, -1).scl(-(getRadius() + distance * Constants.KM_TO_U));
        Matrix4d ori = new Matrix4d(graph.orientation);
        var rotation = getRotationComponent();
        if (rotation != null) {
            ori.rotate(0, 1, 0, rotation.angle);
        }
        aux2.mul(ori);

        getAbsolutePosition(out).add(aux2);
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
     * @return True if position should be recomputed for this entity
     */
    protected boolean mustUpdatePosition(ITimeFrameProvider time) {
        return time.getHdiff() != 0;
    }

    public Vector3b getPredictedPosition(Vector3b out,
                                         String name,
                                         ITimeFrameProvider time,
                                         boolean force) {
        if (!isValid()) {
            return out;
        }
        if (getSet() != null) {
            if (name != null && !name.isBlank()) {
                return getSet().getAbsolutePosition(name, time.getTime(), out);
            } else {
                return getSet().getAbsolutePosition(time.getTime(), out);
            }
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

                // Return to pool.
                scene.returnCopyObject(copy);

                return out;
            }
        }
    }

    @Override
    public Vector3b getPredictedPosition(Vector3b out,
                                         ITimeFrameProvider time,
                                         ICamera unused,
                                         boolean force) {
        return getPredictedPosition(out, null, time, force);
    }

    @Override
    public Vector3b getPredictedPosition(Vector3b out, double deltaTime) {
        // Get a line copy of focus and update it.
        var copy = scene.getLineCopy(entity);

        // Get root of line copy.
        var copyGraph = Mapper.graph.get(copy);
        var root = copyGraph.getRoot(copy);
        // This updates the graph node for all entities in the line.
        scene.updateEntity(root, (float) deltaTime);

        // This updates the rest of components of our entity.
        scene.updateEntity(copy, (float) deltaTime);

        EntityUtils.getAbsolutePosition(copy, out);

        // Return to pool.
        scene.returnCopyObject(copy);

        return out;
    }

    @Override
    public double getDistToCamera() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getDistToCamera();
        } else {
            return body.distToCamera;
        }
    }

    @Override
    public double getClosestDistToCamera() {
        if (!isValid()) {
            return 0;
        }
        if (starSet != null) {
            return starSet.getClosestDistToCamera();
        } else {
            return getDistToCamera();
        }
    }

    @Override
    public double getSolidAngle() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getSolidAngle();
        } else {
            return body.solidAngle;
        }
    }

    @Override
    public double getSolidAngleApparent() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getSolidAngleApparent();
        } else {
            return body.solidAngleApparent;
        }
    }

    @Override
    public double getCandidateSolidAngleApparent() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getCandidateSolidAngleApparent();
        } else {
            return getSolidAngleApparent();
        }
    }

    @Override
    public double getAlpha() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getAlpha();
        } else {
            return body.posSph.x;
        }
    }

    @Override
    public double getDelta() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getDelta();
        } else {
            return body.posSph.y;
        }
    }

    @Override
    public double getSize() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getSize();
        } else {
            return body.size;
        }
    }

    @Override
    public double getRadius() {
        if (!isValid()) {
            return 0;
        }
        var set = getSet();
        if (set != null) {
            return set.getRadius();
        } else {
            return extra != null ? extra.radius : body.size / 2.0;
        }
    }

    @Override
    public double getTEff(){
        if (!isValid()) {
            return 0;
        }
        var set = getStarSet();
        if (set != null) {
            return set.getTEff();
        } else {
            return extra != null ? extra.tEff : -1;
        }
    }

    @Override
    public double getElevationAt(Vector3b camPos) {
        if (!isValid()) {
            return 0;
        }
        if (isModel()) {
            return getElevationAt(camPos, false);
        } else {
            return getRadius();
        }
    }

    @Override
    public double getElevationAt(Vector3b camPos,
                                 boolean useFuturePosition) {
        if (isModel()) {
            if (useFuturePosition) {
                Vector3b nextPos = getPredictedPosition(B33, GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
                return getElevationAt(camPos, nextPos);
            } else {
                return getElevationAt(camPos, null);
            }
        } else {
            return getRadius();
        }
    }

    @Override
    public double getElevationAt(Vector3b camPos,
                                 Vector3b nextPos) {
        if (isModel()) {
            var model = Mapper.model.get(entity);
            var mc = model.model;
            double multiplier = Settings.settings.scene.renderer.elevation.multiplier;
            double height = 0;
            if (mc != null && mc.mtc != null && mc.mtc.heightData != null) {
                double dCam;
                Vector3b cart = B31;
                if (nextPos != null) {
                    cart.set(nextPos);
                    dCam = D32.set(camPos).sub(cart).len();
                } else {
                    getAbsolutePosition(cart);
                    dCam = getDistToCamera();
                }
                // Only when we have height map, and we are below the highest point in the surface.
                if (dCam < getRadius() + mc.mtc.heightScale * multiplier) {
                    // Object-camera normalised vector
                    cart.scl(-1).add(camPos).nor();

                    updater.setToLocalTransform(entity, body, graph, 1, matAux, false);
                    matAux.inv();
                    matDAux.set(matAux.getValues());
                    cart.mul(matDAux);

                    Vector3d sph = D32;
                    Coordinates.cartesianToSpherical(cart, sph);

                    double u = (((sph.x * Nature.TO_DEG) + 270.0) % 360.0) / 360.0;
                    double v = (sph.y * Nature.TO_DEG + 90.0) / 180.0;
                    // Get the height at the given UV position, and scale it properly.
                    double heightNormalized = mc.mtc.heightData.getNormalizedHeight(u, v);
                    height = heightNormalized * mc.mtc.heightScale;

                    // Debug by painting on diffuse texture at same position.
                    // var mat = mc.mtc.getMaterial();
                    // if(mat.has(TextureAttribute.Diffuse)) {
                    //    Texture diffuse = ((TextureAttribute) Objects.requireNonNull(mat.get(TextureAttribute.Diffuse))).textureDescription.texture;
                    //    if(!diffuse.isManaged()){
                    //        p.setColor((float) heightNormalized, 1, 0, 1);
                    //        p.fill();
                    //        diffuse.draw(p, (int) (u * diffuse.getWidth()), (int) ((1 - v) * diffuse.getHeight()));
                    //    }
                    //}
                }
            }
            return getRadius() + height * multiplier;

        } else {
            return getRadius();
        }
    }

    @Override
    public double getHeightScale() {
        if (isModel()) {
            var model = Mapper.model.get(entity);
            var mc = model.model;
            if (mc != null && mc.mtc != null && mc.mtc.heightData != null) {
                return mc.mtc.heightScale;
            }
        }
        return 0;
    }

    @Override
    public float getAppmag() {
        if (starSet != null) {
            return starSet.focus.appMag();
        } else if (particleSet != null) {
            return 0;
        } else if (mag != null) {
            return mag.appMag;
        } else {
            return 0;
        }
    }

    @Override
    public float getAbsmag() {
        if (starSet != null && starSet.focus != null) {
            return starSet.focus.absMag();
        } else if (particleSet != null) {
            return 0;
        } else if (mag != null) {
            return mag.absMag;
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
        var orientation = Mapper.orientation.get(entity);
        return orientation != null ? orientation.rigidRotation : null;
    }

    @Override
    public QuaternionDouble getOrientationQuaternion() {
        if (Mapper.orientation.has(entity)) {
            var orientation = Mapper.orientation.get(entity);
            if (orientation.quaternionOrientation != null) {
                return orientation.quaternionOrientation.getCurrentQuaternion();
            }
        }
        return null;
    }

    @Override
    public void addHitCoordinate(int screenX,
                                 int screenY,
                                 int w,
                                 int h,
                                 int pixelDist,
                                 NaturalCamera camera,
                                 Array<IFocus> hits) {

    }

    @Override
    public void addEntityHitCoordinate(int screenX,
                                       int screenY,
                                       int w,
                                       int h,
                                       int pixelDist,
                                       NaturalCamera camera,
                                       Array<Entity> hits) {
        if (focus != null && focus.focusable && focus.hitCoordinatesConsumer != null) {
            focus.hitCoordinatesConsumer.apply(focusHit, this, screenX, screenY, w, h, pixelDist, camera, hits);
        }
    }

    @Override
    public void addHitRay(Vector3d p0,
                          Vector3d p1,
                          NaturalCamera camera,
                          Array<IFocus> hits) {

    }

    @Override
    public void addEntityHitRay(Vector3d p0,
                                Vector3d p1,
                                NaturalCamera camera,
                                Array<Entity> hits) {
        if (focus != null && focus.focusable && focus.hitRayConsumer != null) {
            focus.hitRayConsumer.apply(focusHit, this, p0, p1, camera, hits);
        }
    }

    @Override
    public void makeFocus() {
        if (isSet()) {
            getSet().makeFocus();
        }
    }

    @Override
    public IFocus getFocus(String name) {
        if (name != null) {
            if (isSet()) {
                getSet().setFocusIndex(name);
            }
        }
        return this;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return isValid() && Mapper.coordinates.has(entity) && Mapper.coordinates.get(entity).timeOverflow;
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

    public void setForceLabel(Boolean forceLabel,
                              String name) {
        if (isSet()) {
            getSet().setForceLabel(forceLabel, name);
        } else if (Mapper.label.has(entity)) {
            Mapper.label.get(entity).setForceLabel(forceLabel);
        }
    }

    public boolean isForceLabel(String name) {
        if (isValid() && isSet()) {
            return getSet().isForceLabel(name);
        } else {
            return isForceLabel();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FocusView) {
            return this.entity == ((FocusView) other).getEntity();
        }
        return false;

    }

    @Override
    public boolean isForceLabel() {
        if (isValid() && Mapper.label.has(entity)) {
            return Mapper.label.get(entity).forceLabel;
        }
        return false;
    }

    /**
     * Sets the label color, as an RGBA float array.
     *
     * @param color The label color.
     */
    public void setLabelColor(float[] color) {
        body.labelColor = color;
    }

    public void setLabelColor(float[] color,
                              String name) {
        if (isSet()) {
            getSet().setLabelColor(color, name);
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

    /**
     * Checks whether the entity is a particle or star set.
     *
     * @return True if the entity is a particle or star set.
     */
    public boolean isSet() {
        return isValid() && (particleSet != null || starSet != null);
    }

    public boolean isParticleSet() {
        return isValid() && particleSet != null;
    }

    public boolean isStarSet() {
        return isValid() && starSet != null;
    }

    public ParticleSet getSet() {
        return particleSet != null ? particleSet : starSet;
    }

    public boolean isModel() {
        return isValid() && Mapper.modelScaffolding.has(entity);
    }

    public boolean isCluster() {
        return isValid() && Mapper.cluster.has(entity);
    }

    public boolean isCelestial() {
        return isValid() && Mapper.celestial.has(entity);
    }

    public boolean hasProperMotion() {
        return isValid() && Mapper.pm.has(entity) || starSet != null;
    }

    public double getMuAlpha() {
        if (isSet()) {
            var set = getSet();
            if (set.focus != null && set.focus.hasProperMotion())
                return set.focus.mualpha();
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
        if (isSet()) {
            var set = getSet();
            if (set.focus != null && set.focus.hasProperMotion())
                return set.focus.mudelta();
            else
                return 0;
        } else if (isValid() && Mapper.pm.has(entity)) {
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
        if (isSet()) {
            var set = getSet();
            if (set.focus != null && set.focus.hasProperMotion())
                return set.focus.radvel();
            else
                return 0;
        } else if (isValid() && Mapper.pm.has(entity)) {
            var pm = Mapper.pm.get(entity);
            if (pm.pmSph != null) {
                return pm.pmSph.z;
            } else {
                return 0;
            }
        }
        return 0;
    }

    public Entity getChildByNameAndArchetype(String name,
                                             Archetype archetype) {
        int size = graph.children.size;
        for (int i = 0; i < size; i++) {
            Entity child = graph.children.get(i);
            if (child != null) {
                var base = Mapper.base.get(child);
                if (base.getName().equalsIgnoreCase(name.toLowerCase().trim()) && archetype.matches(child)) {
                    return child;
                }
            }
        }
        return null;
    }

    private void initAuxView() {
        if (auxView == null) {
            auxView = new FocusView();
        }
    }

    private void initHighlight() {
        if (isValid() && hl == null)
            hl = Mapper.highlight.get(entity);
    }

    /**
     * Marks the element for update in VRAM.
     */
    public void markForUpdate() {
        if (!isValid()) {
            return;
        }
        var set = getSet();
        if (set != null) {
            set.markForUpdate(Mapper.render.get(entity));
        }
        if (Mapper.orbitElementsSet.has(entity)) {
            Mapper.orbitElementsSet.get(entity).markForUpdate(Mapper.render.get(entity));
        }
        if (Mapper.verts.has(entity)) {
            Mapper.verts.get(entity).markForUpdate(Mapper.render.get(entity));
        }
    }

    /**
     * Highlight using a plain color.
     *
     * @param state      Whether to highlight.
     * @param color      The plain color.
     * @param allVisible All visible.
     */
    public void highlight(boolean state,
                          float[] color,
                          boolean allVisible) {
        initHighlight();
        markForUpdate();

        // Set highlight properties.
        this.hl.highlighted = state;
        if (state) {
            hl.hlplain = true;
            hl.hlallvisible = allVisible;
            System.arraycopy(color, 0, hl.hlc, 0, color.length);
        }

        // In octrees, highlight all objects.
        if (Mapper.octree.has(entity) && octant != null) {
            Array<Entity> l = getOctreeObjects(new Array<>());
            if (l != null && l.size > 0) {
                initAuxView();
                for (Entity e : l) {
                    auxView.setEntity(e);
                    auxView.highlight(state, color, allVisible);
                }
            }
        }
    }

    /**
     * Highlight using a colormap.
     *
     * @param state   Whether to highlight.
     * @param cmi     Color map index.
     * @param cmAlpha Color map alpha value.
     * @param cma     Color map attribute.
     * @param cmMin   Min mapping value.
     * @param cmMax   Max mapping value.
     */
    public void highlight(boolean state,
                          int cmi,
                          float cmAlpha,
                          IAttribute cma,
                          double cmMin,
                          double cmMax,
                          boolean allVisible) {
        initHighlight();
        markForUpdate();

        hl.highlighted = state;
        if (state) {
            hl.hlplain = false;
            hl.hlallvisible = allVisible;
            hl.hlcmi = cmi;
            hl.hlcmAlpha = cmAlpha;
            hl.hlcma = cma;
            hl.hlcmmin = cmMin;
            hl.hlcmmax = cmMax;
        }

        // In octrees, highlight all objects.
        if (Mapper.octree.has(entity) && octant != null) {
            Array<Entity> l = getOctreeObjects(new Array<>());
            if (l != null && l.size > 0) {
                initAuxView();
                for (Entity e : l) {
                    auxView.setEntity(e);
                    auxView.highlight(state, cmi, cmAlpha, cma, cmMin, cmMax, allVisible);
                }
            }
        }
    }

    public boolean isHighlighted() {
        initHighlight();
        return hl.highlighted;
    }

    public boolean isOctree() {
        return isValid() && Mapper.octree.has(entity);
    }

    public Array<Entity> getOctreeObjects(Array<Entity> list) {
        if (octant == null) {
            return list;
        }
        return getOctreeObjects(octant.octant, list);
    }

    public boolean isPlanet() {
        return isValid() && base.archetype != null && base.archetype.getName().equals("Planet");
    }

    /**
     * Returns true if this focus is a single star.
     *
     * @return True if this is a single star.
     */
    public boolean isSingleStar() {
        return isValid() && base.archetype != null && base.archetype.getName().equals("Star");
    }

    /**
     * Returns whether this focus is a star of any kind (set or single).
     *
     * @return True if the focus is a star.
     */
    public boolean isStar() {
        return isValid() && (isStarSet() || isSingleStar());
    }

    private Array<Entity> getOctreeObjects(OctreeNode node,
                                           Array<Entity> list) {
        if (node != null && node.objects != null) {
            for (IOctreeObject object : node.objects) {
                if (object instanceof OctreeObjectView oov) {
                    list.add(oov.getEntity());
                }
            }
            if (node.children != null) {
                for (OctreeNode child : node.children) {
                    if (child != null) {
                        getOctreeObjects(child, list);
                    }
                }
            }
        }
        return list;
    }

}
