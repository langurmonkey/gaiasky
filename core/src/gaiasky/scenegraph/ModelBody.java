/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.ShadowMapImpl;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.scenegraph.component.MaterialComponent;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Abstract class with the basic functionality of bodies represented by a 3D
 * model
 */
public abstract class ModelBody extends CelestialBody {
    protected static final double TH_ANGLE_POINT = Math.toRadians(0.30);

    /**
     * Solid angle limit for rendering as point. If angle is any bigger, we render
     * with shader.
     */
    public double THRESHOLD_POINT() {
        return TH_ANGLE_POINT;
    }

    /** MODEL **/
    public ModelComponent mc;

    /** TRANSFORMATIONS - are applied each cycle **/
    public ITransform[] transformations;

    /** Multiplier for Loc view angle **/
    public float locVaMultiplier = 1f;
    /** ThOverFactor for Locs **/
    public float locThOverFactor = 1f;

    /** Size factor, which can be set to scale model objects up or down **/
    public float sizeScaleFactor = 1f;

    /** Fade opacity, special to model bodies **/
    protected float fadeOpacity;

    /** Shadow map properties **/
    private ShadowMapImpl shadowMap;

    /** State flag; whether to render the shadow (number of times left) **/
    public int shadow;

    /** Name of the reference plane for this object. Defaults to equator **/
    public String refPlane;
    /** Name of the transformation to the reference plane **/
    public String refPlaneTransform;
    public String inverseRefPlaneTransform;

    /**
     * Array with shadow camera distance, cam near and cam far as a function of
     * the radius of the object
     */
    public double[] shadowMapValues;

    /** The seed for random components **/
    protected List<Long> seed = Arrays.asList(1L);

    /** The components to randomize---possible values are ["model", "cloud", "atmosphere"] **/
    protected List<String> randomize;

    public ModelBody() {
        super();
        localTransform = new Matrix4();
        orientation = new Matrix4d();
    }

    public void initialize() {
        if (isRandomizeModel()) {
            // Ignore current model component (if any) and create a random one
            mc = new ModelComponent(true);
            mc.randomizeAll(getSeed("model"), size);
            if (Settings.settings.program.debugInfo) {
                Logger.getLogger(Planet.class).debug("::" + getName() + "::");
                Logger.getLogger(Planet.class).debug("============MODEL===========");
                mc.print(Logger.getLogger(Planet.class));
            }
        }
        if (mc != null) {
            mc.initialize(this.getName());
        }
        setColor2Data();
    }

    @Override
    public void doneLoading(AssetManager manager) {
        super.doneLoading(manager);

        if (mc != null) {
            mc.doneLoading(manager, localTransform, cc);
        }
    }

    // At what distance the light has the maximum intensity
    private static final double LIGHT_X0 = 0.1 * Constants.AU_TO_U;
    // At what distance the light is 0
    private static final double LIGHT_X1 = 5e4 * Constants.AU_TO_U;

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {
        super.updateLocal(time, camera);
        // Update light with global position
        if (mc != null && distToCamera <= LIGHT_X1) {
            for (int i = 0; i < Constants.N_DIR_LIGHTS; i++) {
                IFocus lightSource = camera.getCloseLightSource(i);
                if (lightSource != null) {
                    if (lightSource instanceof Proximity.NearbyRecord) {
                        translation.put(mc.directional(i).direction);
                        Proximity.NearbyRecord nr = (Proximity.NearbyRecord) lightSource;
                        if (nr.isStar() || nr.isStarGroup()) {
                            float[] col = nr.getColor();
                            double closestDist = nr.getClosestDistToCamera();
                            float colFactor = (float) Math.pow(MathUtilsd.lint(closestDist, LIGHT_X0, LIGHT_X1, 1.0, 0.0), 2.0);
                            mc.directional(i).direction.sub(nr.pos.put(aux3f1.get()));
                            mc.directional(i).color.set(col[0] * colFactor, col[1] * colFactor, col[2] * colFactor, colFactor);
                        } else {
                            Vector3b campos = camera.getPos();
                            mc.directional(i).direction.add(campos.x.floatValue(), campos.y.floatValue(), campos.z.floatValue());
                            mc.directional(i).color.set(1f, 1f, 1f, 1f);
                        }

                    }
                } else {
                    // Disable light
                   mc.directional(i).color.set(0f, 0f, 0f, 0f);
                }
            }
        }
        updateLocalTransform();
    }

    /**
     * Update the local transform with the transform and the rotations/scales
     * necessary. Override if your model contains more than just the position
     * and size.
     */
    protected void updateLocalTransform() {
        setToLocalTransform(sizeScaleFactor, localTransform, true);
    }

    public void setToLocalTransform(float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        setToLocalTransform(size, sizeFactor, localTransform, forceUpdate);
    }

    public void setToLocalTransform(float size, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        if (sizeFactor != 1 || forceUpdate) {
            if (rc != null) {
                translation.getMatrix(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(refPlaneTransform)).rotate(0, 1, 0, (float) rc.ascendingNode).rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt)).rotate(0, 1, 0, (float) rc.angle);
                orientation.idt().mul(Coordinates.getTransformD(refPlaneTransform)).rotate(0, 1, 0, (float) rc.ascendingNode).rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt));
            } else {
                translation.getMatrix(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(refPlaneTransform));
                orientation.idt().mul(Coordinates.getTransformD(refPlaneTransform));
            }
        } else {
            localTransform.set(this.localTransform);
        }

        // Apply transformations
        if (transformations != null)
            for (ITransform tc : transformations)
                tc.apply(localTransform);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && this.isValidPosition() && parent.isValidPosition()) {
            camera.checkClosestBody(this);
            double thPoint = (THRESHOLD_POINT() * camera.getFovFactor()) / sizeScaleFactor;
            if (viewAngleApparent >= thPoint) {
                double thQuad2 = THRESHOLD_QUAD() * camera.getFovFactor() * 2 / sizeScaleFactor;
                double thQuad1 = thQuad2 / 8.0 / sizeScaleFactor;
                if (viewAngleApparent < thPoint * 4) {
                    fadeOpacity = (float) MathUtilsd.lint(viewAngleApparent, thPoint, thPoint * 4, 1, 0);
                } else {
                    fadeOpacity = (float) MathUtilsd.lint(viewAngleApparent, thQuad1, thQuad2, 0, 1);
                }

                if (viewAngleApparent < thQuad1) {
                    addToRender(this, RenderGroup.BILLBOARD_SSO);
                } else if (viewAngleApparent > thQuad2) {
                    addToRenderModel();
                } else {
                    // Both
                    addToRender(this, RenderGroup.BILLBOARD_SSO);
                    addToRenderModel();
                }
                if (renderText()) {
                    addToRender(this, RenderGroup.FONT_LABEL);
                }
            }
            if(!isInRender(this, RenderGroup.FONT_LABEL) && forceLabel) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
        }
    }

    private void addToRenderModel() {
        RenderGroup rg = renderTessellated() ? RenderGroup.MODEL_PIX_TESS : RenderGroup.MODEL_PIX;
        addToRender(this, rg);
    }

    public boolean renderTessellated() {
        return Settings.settings.scene.renderer.elevation.type.isTessellation() && mc.hasHeight();
    }

    @Override
    public float getInnerRad() {
        return 0.2f;
    }

    public void dispose() {
        super.dispose();
        if (mc != null) {
            mc.dispose();
        }
    }

    /**
     * Billboard quad rendering
     */
    @Override
    public void render(ExtShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;

        float size = getFuzzyRenderSize(camera);

        Vector3 bbPos = translation.put(aux3f1.get());
        // Get it a tad closer to the camera to prevent occlusion with orbit
        float l = bbPos.len();
        bbPos.nor().scl(l * 0.99f);
        shader.setUniformf("u_pos", bbPos);
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", cc[0], cc[1], cc[2], alpha * (1 - fadeOpacity));
        shader.setUniformf("u_inner_rad", getInnerRad());
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_thpoint", (float) THRESHOLD_POINT() * camera.getFovFactor());

        // Whether light scattering is enabled or not
        shader.setUniformi("u_lightScattering", 0);

        shader.setUniformf("u_radius", (float) getRadius());

        // Sprite.render
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    /** Model rendering **/
    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {
        render(modelBatch, group, alpha, t, true);
    }

    /** Model opaque rendering. Disable shadow mapping **/
    public void render(IntModelBatch modelBatch, RenderGroup group, float alpha, double t, boolean shadowEnv) {
        if (mc.isModelInitialised()) {
            if (shadowEnv)
                prepareShadowEnvironment();

            mc.update(alpha * fadeOpacity);
            modelBatch.render(mc.instance, mc.env);
        }
    }

    @Override
    protected float labelMax() {
        return (float) (.5e-4 / Constants.DISTANCE_SCALE_FACTOR);
    }

    public void setModel(ModelComponent mc) {
        this.mc = mc;
    }

    public float getFuzzyRenderSize(ICamera camera) {
        float thAngleQuad = (float) THRESHOLD_QUAD() * camera.getFovFactor();
        double size = 0f;
        if (viewAngle >= THRESHOLD_POINT() * camera.getFovFactor()) {
            size = Math.tan(thAngleQuad) * distToCamera * 2f;
        }
        return (float) size;
    }

    protected float getViewAnglePow() {
        return 1.0f;
    }

    protected float getThOverFactorScl() {
        return ct.get(ComponentType.Moons.ordinal()) ? 2500f : 25f;
    }

    protected float getThOverFactor(ICamera camera) {
        return TH_OVER_FACTOR;
    }

    @Override
    public float textScale() {
        return Math.max(1f, labelSizeConcrete()) * 2e-1f;
    }

    protected float labelSizeConcrete() {
        return (float) Math.pow(this.size * .6e1f, .001f);
    }

    public void setLocvamultiplier(Double val) {
        this.locVaMultiplier = val.floatValue();
    }

    public void setLocthoverfactor(Double val) {
        this.locThOverFactor = val.floatValue();
    }

    public void setTransformations(Object[] transformations) {
        this.transformations = new ITransform[transformations.length];
        for (int i = 0; i < transformations.length; i++)
            this.transformations[i] = (ITransform) transformations[i];
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
        Vector3d aux1 = aux3d1.get();
        Vector3d aux2 = aux3d2.get();

        // Lon/Lat/Radius
        longitude *= MathUtilsd.degRad;
        latitude *= MathUtilsd.degRad;
        double rad = 1;
        Coordinates.sphericalToCartesian(longitude, latitude, rad, aux1);

        aux2.set(aux1.z, aux1.y, aux1.x).scl(1, -1, -1).scl(-(getRadius() + distance * Constants.KM_TO_U));
        //aux2.rotate(rc.angle, 0, 1, 0);
        Matrix4d ori = new Matrix4d(orientation);
        ori.rotate(0, 1, 0, rc.angle);
        aux2.mul(ori);

        getAbsolutePosition(out).add(aux2);
        return out;
    }

    Matrix4 mataux = new Matrix4();
    Matrix4d matauxd = new Matrix4d();

    @Override
    public double getHeight(Vector3b camPos) {
        return getHeight(camPos, false);
    }

    @Override
    public double getHeight(Vector3b camPos, boolean useFuturePosition) {
        if (useFuturePosition) {
            Vector3b nextPos = getPredictedPosition(aux3b3.get(), GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
            return getHeight(camPos, nextPos);
        } else {
            return getHeight(camPos, null);
        }

    }

    @Override
    public double getHeight(Vector3b camPos, Vector3b nextPos) {
        double height = 0;
        if (mc != null && mc.mtc != null && mc.mtc.heightMap != null) {
            double dCam;
            Vector3b cart = aux3b1.get();
            if (nextPos != null) {
                cart.set(nextPos);
                getPredictedPosition(cart, GaiaSky.instance.time, GaiaSky.instance.getICamera(), false);
                dCam = aux3d2.get().set(camPos).sub(cart).len();
            } else {
                getAbsolutePosition(cart);
                dCam = distToCamera;
            }
            // Only when we have height map and we are below the highest point in the surface
            if (dCam < getRadius() + mc.mtc.heightScale * Settings.settings.scene.renderer.elevation.multiplier * 4) {
                float[][] m = mc.mtc.heightMap;
                int W = mc.mtc.heightMap.length;
                int H = mc.mtc.heightMap[0].length;

                // Object-camera normalised vector
                cart.scl(-1).add(camPos).nor();

                setToLocalTransform(1, mataux, false);
                mataux.inv();
                matauxd.set(mataux.getValues());
                cart.mul(matauxd);

                Vector3d sph = aux3d2.get();
                Coordinates.cartesianToSpherical(cart, sph);

                double u = (((sph.x * Nature.TO_DEG) + 270.0) % 360.0) / 360.0;
                double v = 1d - (sph.y * Nature.TO_DEG + 90.0) / 180.0;

                // Bilinear interpolation
                int i1 = (int) (W * u);
                int i2 = (i1 + 1) % W;
                int j1 = (int) (H * v);
                int j2 = (j1 + 1) % H;

                double dx = 1.0 / W;
                double dy = 1.0 / H;
                double x1 = (double) i1 / (double) W;
                double x2 = (x1 + dx) % 1.0;
                double y1 = (double) j1 / (double) H;
                double y2 = (y1 + dy) % 1.0;
                double x = u;
                double y = v;

                double f11 = m[i1][j1];
                double f21 = m[i2][j1];
                double f12 = m[i1][j2];
                double f22 = m[i2][j2];

                double denom = (x2 - x1) * (y2 - y1);
                height = (((x2 - x) * (y2 - y)) / denom) * f11 + ((x - x1) * (y2 - y) / denom) * f21 + ((x2 - x) * (y - y1) / denom) * f12 + ((x - x1) * (y - y1) / denom) * f22;
            }
        }
        return getRadius() + height * Settings.settings.scene.renderer.elevation.multiplier;
    }

    public double getHeightScale() {
        if (mc != null && mc.mtc != null && mc.mtc.heightMap != null) {
            return mc.mtc.heightScale;
        }
        return 0;
    }

    /**
     * Sets the shadow environment
     */
    protected void prepareShadowEnvironment() {
        if (Settings.settings.scene.renderer.shadow.active) {
            Environment env = mc.env;
            SceneGraphRenderer sgr = GaiaSky.instance.sgr;
            if (shadow > 0 && sgr.smTexMap.containsKey(this)) {
                Matrix4 combined = sgr.smCombinedMap.get(this);
                Texture tex = sgr.smTexMap.get(this);
                if (env.shadowMap == null) {
                    if (shadowMap == null)
                        shadowMap = new ShadowMapImpl(combined, tex);
                    env.shadowMap = shadowMap;
                }
                shadowMap.setProjViewTrans(combined);
                shadowMap.setDepthMap(tex);

                shadow--;
            } else {
                env.shadowMap = null;
            }
        } else {
            mc.env.shadowMap = null;
        }
    }

    /**
     * If we render the model, we set up a sphere at the object's position with
     * its radius and check for intersections with the ray
     */
    public void addHit(int screenX, int screenY, int w, int h, int minPixDist, NaturalCamera camera, Array<IFocus> hits) {
        if (checkHitCondition()) {
            if (viewAngleApparent < THRESHOLD_QUAD() * camera.getFovFactor()) {
                super.addHit(screenX, screenY, w, h, minPixDist, camera, hits);
            } else {
                Vector3 auxf = aux3f1.get();
                Vector3d aux1d = aux3d1.get();
                Vector3d aux2d = aux3d2.get();
                Vector3d aux3d = aux3d3.get();

                // aux1d contains the position of the body in the camera ref sys
                aux1d.set(translation);
                auxf.set(aux1d.valuesf());

                if (camera.direction.dot(aux1d) > 0) {
                    // The object is in front of us
                    auxf.set(screenX, screenY, 2f);
                    camera.camera.unproject(auxf).nor();

                    // aux2d contains the position of the click in the camera ref sys
                    aux2d.set(auxf.x, auxf.y, auxf.z);

                    // aux3d contains the camera position, [0,0,0]
                    aux3d.set(0, 0, 0);

                    boolean intersect = Intersectord.checkIntersectRaySpehre(aux3d, aux2d, aux1d, getRadius());
                    if (intersect) {
                        //Hit
                        hits.add(this);
                    }
                }
            }
        }

    }

    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        if (checkHitCondition()) {
            if (viewAngleApparent < THRESHOLD_QUAD() * camera.getFovFactor()) {
                super.addHit(p0, p1, camera, hits);
            } else {
                Vector3d aux1d = aux3d1.get();

                // aux1d contains the position of the body in the camera ref sys
                aux1d.set(translation);

                boolean intersect = Intersectord.checkIntersectRaySpehre(p0, p1, aux1d, getRadius());
                if (intersect) {
                    //Hit
                    hits.add(this);
                }
            }
        }
    }

    @Override
    public double getSize() {
        return super.getSize() * sizeScaleFactor;
    }

    public double getRadius() {
        return super.getRadius() * sizeScaleFactor;
    }

    /**
     * Whether shadows should be rendered for this object
     *
     * @return Whether shadows should be rendered for this object
     */
    public boolean isShadow() {
        return shadowMapValues != null;
    }

    /**
     * Sets the shadow mapping values for this object
     *
     * @param shadowMapValues The values
     */
    public void setShadowvalues(double[] shadowMapValues) {
        this.shadowMapValues = shadowMapValues;
    }

    public void setSizescalefactor(Double sizescalefactor) {
        this.sizeScaleFactor = sizescalefactor.floatValue();
    }

    public void setRefplane(String refplane) {
        this.refPlane = refplane;
        this.refPlaneTransform = refplane + "toequatorial";
        this.inverseRefPlaneTransform = "equatorialto" + refplane;
    }

    public void setSeed(Long seed) {
        this.seed = Arrays.asList(seed);
    }

    public void setSeed(int[] seed) {
        this.seed = new ArrayList<>(seed.length);
        for (int s : seed) {
            this.seed.add((long) s);
        }
    }

    public void setRandomize(String[] randomize) {
        this.randomize = Arrays.asList(randomize);
    }

    public void setRandomize(String randomize) {
        this.randomize = Arrays.asList(randomize);
    }

    protected boolean isRandomizeModel() {
        return this.randomize != null && this.randomize.contains("model");
    }

    /**
     * Gets the seed corresponding to the given component by matching it using
     * the position in the randomize vector.
     *
     * @param component The component name.
     *
     * @return The seed.
     */
    protected long getSeed(String component) {
        if (randomize != null && randomize.contains(component)) {
            int idx;
            if ((idx = randomize.indexOf(component)) >= 0 && seed.size() > idx) {
                return seed.get(idx);
            }
        }
        // Get first otherwise
        return this.seed.get(0);
    }

    protected boolean isRandomizeAtmosphere() {
        return this.randomize != null && this.randomize.contains("atmosphere");
    }

    protected boolean isRandomizeCloud() {
        return this.randomize != null && this.randomize.contains("cloud");
    }

    public ModelComponent getModelComponent() {
        return mc;
    }

    public MaterialComponent getMaterialComponent() {
        return mc != null ? mc.mtc : null;
    }

}
