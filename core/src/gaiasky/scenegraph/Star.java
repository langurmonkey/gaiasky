/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.ModelComponent;
import gaiasky.util.Constants;
import gaiasky.util.ModelCache;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.FloatExtAttribute;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;

import java.util.Map;
import java.util.TreeMap;

/**
 * Represents a single star. Currently, only the Sun uses this class.
 *
 * @deprecated Move Sun to star group
 */
@Deprecated
public class Star extends Particle {

    /** Model used to represent the star **/
    private ModelComponent mc;

    /** HIP number, negative if non-existent **/
    public int hip = -1;

    double modelDistance;

    public Star() {
        super();
    }

    public Star(Vector3b pos, float appMag, float absMag, float colorBV, String[] names, long starID) {
        super(pos, appMag, absMag, colorBV, names, starID);
    }

    /**
     * Creates a new Star object
     *
     * @param pos     The position of the star in equatorial cartesian coordinates
     * @param appMag  The apparent magnitude
     * @param absMag  The absolute magnitude
     * @param colorBV The B-V color index
     * @param names   The proper names of the star, if any
     * @param ra      in degrees
     * @param dec     in degrees
     * @param starID  The star id
     */
    public Star(Vector3b pos, float appMag, float absMag, float colorBV, String[] names, float ra, float dec, long starID) {
        super(pos, appMag, absMag, colorBV, names, ra, dec, starID);
    }

    /**
     * Creates a new Star object
     *
     * @param pos     The position of the star in equatorial cartesian coordinates
     * @param appMag  The apparent magnitude
     * @param absMag  The absolute magnitude
     * @param colorBV The B-V color index
     * @param names   The proper names of the star, if any
     * @param ra      in degrees
     * @param dec     in degrees
     * @param starID  The star id
     * @param hip     The HIP identifier
     * @param source  Catalog source. 1: Gaia, 2: HIP, 3: TYC, -1: Unknown
     */
    public Star(Vector3b pos, float appMag, float absMag, float colorBV, String[] names, float ra, float dec, long starID, int hip, byte source) {
        super(pos, appMag, absMag, colorBV, names, ra, dec, starID);
        this.hip = hip;
        this.catalogSource = source;
    }

    /**
     * Creates a new Star object
     *
     * @param pos     The position of the star in equatorial cartesian coordinates
     * @param pm      The proper motion of the star in equatorial cartesian
     *                coordinates
     * @param pmSph   The proper motion with muAlpha, muDelta, radVel.
     * @param appMag  The apparent magnitude
     * @param absMag  The absolute magnitude
     * @param colorBV The B-V color index
     * @param names   The proper names of the star, if any
     * @param ra      in degrees
     * @param dec     in degrees
     * @param starID  The star id
     */
    public Star(Vector3b pos, Vector3 pm, Vector3 pmSph, float appMag, float absMag, float colorBV, String[] names, float ra, float dec, long starID) {
        super(pos, pm, pmSph, appMag, absMag, colorBV, names, ra, dec, starID);
    }

    /**
     * Creates a new Star object
     *
     * @param pos     The position of the star in equatorial cartesian coordinates
     * @param pm      The proper motion of the star in equatorial cartesian
     *                coordinates
     * @param pmSph   The proper motion with muAlpha, muDelta, radVel.
     * @param appMag  The apparent magnitude
     * @param absMag  The absolute magnitude
     * @param colorBV The B-V color index
     * @param names   The proper names of the star, if any
     * @param ra      in degrees
     * @param dec     in degrees
     * @param starID  The star id
     * @param hip     HIP number, if any
     * @param source  Catalog source. 1: Gaia, 2: HIP, 3: TYC, -1: Unknown
     */
    public Star(Vector3b pos, Vector3 pm, Vector3 pmSph, float appMag, float absMag, float colorBV, String[] names, float ra, float dec, long starID, int hip, byte source) {
        super(pos, pm, pmSph, appMag, absMag, colorBV, names, ra, dec, starID);
        this.hip = hip;
        this.catalogSource = source;
    }

    @Override
    public void initialize() {
        setDerivedAttributes();
        radius = size * Constants.STAR_SIZE_FACTOR;
        modelDistance = 172.4643429 * radius;
    }

    @Override
    public void doneLoading(final AssetManager manager) {
        super.doneLoading(manager);
        initModel(manager);

        if (!Float.isFinite(this.absmag)) {
            double distPc;
            if(this.coordinates != null) {
                distPc = this.coordinates.getEquatorialCartesianCoordinates(GaiaSky.instance.time.getTime(), aux3b1.get()).lend() * Constants.U_TO_PC;
            } else {
                distPc = this.getAbsolutePosition(aux3b1.get()).lend() * Constants.U_TO_PC;
            }
            this.absmag = (float) AstroUtils.apparentToAbsoluteMagnitude(distPc, this.appmag);
        }
    }

    private void initModel(final AssetManager manager) {
        Texture tex = manager.get(Settings.settings.data.dataFile("tex/base/star.jpg"), Texture.class);
        Texture lut = manager.get(Settings.settings.data.dataFile("tex/base/lut.jpg"), Texture.class);
        tex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        Map<String, Object> params = new TreeMap<>();
        params.put("quality", 120L);
        params.put("diameter", 1d);
        params.put("flip", false);

        Pair<IntModel, Map<String, Material>> pair = ModelCache.cache.getModel("sphere", params, Usage.Position | Usage.Normal | Usage.TextureCoordinates, GL20.GL_TRIANGLES);
        IntModel model = pair.getFirst();
        Material mat = pair.getSecond().get("base");
        mat.clear();
        mat.set(new TextureAttribute(TextureAttribute.Diffuse, tex));
        mat.set(new TextureAttribute(TextureAttribute.Normal, lut));
        // Only to activate view vector (camera position)
        mat.set(new ColorAttribute(ColorAttribute.Specular));
        mat.set(new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
        Matrix4 modelTransform = new Matrix4();
        mc = new ModelComponent(false);
        mc.initialize(null);
        mc.env = new Environment();
        mc.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f));
        mc.env.set(new FloatExtAttribute(FloatExtAttribute.Time, 0f));
        mc.instance = new IntModelInstance(model, modelTransform);
        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            mc.rec.setUpRelativisticEffectsMaterial(mc.instance.materials);
        mc.setModelInitialized(true);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        camera.checkClosestParticle(this);
        if (this.shouldRender()) {
            addToRender(this, RenderGroup.POINT_STAR);

            if (camera.getCurrent() instanceof FovCamera) {
                // Render as point, do nothing
                addToRender(this, RenderGroup.BILLBOARD_STAR);
            } else {
                if (viewAngleApparent >= thpointTimesFovfactor) {
                    addToRender(this, RenderGroup.BILLBOARD_STAR);
                    if (distToCamera < modelDistance) {
                        camera.checkClosestBody(this);
                        addToRender(this, RenderGroup.MODEL_VERT_STAR);
                    }
                }
                if (this.hasPm && viewAngleApparent >= thpointTimesFovfactor / Settings.settings.scene.properMotion.number) {
                    addToRender(this, RenderGroup.LINE);
                }
            }

            if ((renderText() || camera.getCurrent() instanceof FovCamera)) {
                addToRender(this, RenderGroup.FONT_LABEL);
            }
        }
    }

    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext renderContext, RenderGroup group) {
        float opacity = (float) MathUtilsd.lint(distToCamera, modelDistance / 50f, modelDistance, 1f, 0f);
        ((ColorAttribute) mc.env.get(ColorAttribute.AmbientLight)).color.set(cc[0], cc[1], cc[2], 1f);
        ((FloatExtAttribute) mc.env.get(FloatExtAttribute.Time)).value = (float) t;
        mc.update(alpha * opacity);
        // Local transform
        translation.getMatrix(mc.instance.transform).scl((float) (getRadius() * 2d));
        modelBatch.render(mc.instance, mc.env);
    }

    public void addHit(int screenX, int screenY, int w, int h, int minPixDist, NaturalCamera camera, Array<IFocus> hits) {
        if (checkHitCondition()) {
            Vector3 pos = aux3f1.get();
            Vector3b aux = aux3b1.get();
            Vector3b posD = getAbsolutePosition(aux).add(camera.getInversePos());
            pos.set(posD.valuesf());

            if (camera.direction.dot(posD) > 0) {
                // The object is in front of us
                double angle = computeViewAngle(camera.getFovFactor()) * Settings.settings.scene.star.brightness * 1e3f;

                PerspectiveCamera perspectiveCamera;
                if (Settings.settings.program.modeStereo.active) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        perspectiveCamera = camera.getCameraStereoLeft();
                    } else {
                        perspectiveCamera = camera.getCameraStereoRight();
                    }
                    perspectiveCamera.update();
                } else {
                    perspectiveCamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / perspectiveCamera.fieldOfView);
                double pixelSize = Math.max(minPixDist, ((angle * perspectiveCamera.viewportHeight) / perspectiveCamera.fieldOfView) / 2);
                perspectiveCamera.project(pos);
                pos.y = perspectiveCamera.viewportHeight - pos.y;
                if (Settings.settings.program.modeStereo.active) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, perspectiveCamera, pixelSize)) {
                    //Hit
                    hits.add(this);
                }
            }
        }
    }

    public String toString() {
        return "Star{" + " name=" + namesConcat() + " id=" + id + " sph=" + posSph + " pos=" + pos + " appmag=" + appmag + '}';
    }

    @Override
    public double getPmX() {
        return pm.x;
    }

    @Override
    public double getPmY() {
        return pm.y;
    }

    @Override
    public double getPmZ() {
        return pm.z;
    }

    @Override
    protected double computeViewAngle(float fovFactor) {
        if (viewAngle > Constants.THRESHOLD_DOWN / fovFactor && viewAngle < Constants.THRESHOLD_UP / fovFactor) {
            return 20f * Constants.THRESHOLD_DOWN / fovFactor;
        }
        return viewAngle;
    }

    @Override
    public int getHip() {
        return hip;
    }

    @Override
    protected void addToIndex(Map<String, SceneGraphNode> map) {
        // Hip
        if (hip > 0) {
            String hipid = "hip " + hip;
            map.put(hipid, this);
        }
    }

    @Override
    protected void removeFromIndex(Map<String, SceneGraphNode> map) {
        // Hip
        if (hip > 0) {
            String hipid = "hip " + hip;
            map.remove(hipid);
        }
    }

}
