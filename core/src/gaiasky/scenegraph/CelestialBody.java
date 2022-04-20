/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.*;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Intersectord;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

/**
 * Represents any celestial body.
 */
public abstract class CelestialBody extends SceneGraphNode implements I3DTextRenderable, IQuadRenderable, IModelRenderable, IFocus {

    /**
     * radius/distance limit for rendering at all. If angle is smaller than this
     * quantity, no rendering happens.
     */
    public abstract double THRESHOLD_NONE();

    /**
     * radius/distance limit for rendering as shader. If angle is any bigger, we
     * render as a model.
     */
    public abstract double THRESHOLD_QUAD();

    /**
     * radius/distance limit for rendering as point. If angle is any bigger, we
     * render with shader.
     */
    public abstract double THRESHOLD_POINT();

    public float TH_OVER_FACTOR;

    /** NAME FOR WIKIPEDIA **/
    public String wikiname;
    /** Absolute magnitude, m = -2.5 log10(flux), with the flux at 10 pc **/
    public float absmag = Float.NaN;
    /** Apparent magnitude, m = -2.5 log10(flux) **/
    public float appmag;
    /** Red, green and blue colors and their revamped cousins **/
    public float[] ccPale;

    /**
     * The B-V color index, calculated as the magnitude in B minus the magnitude
     * in V
     **/
    public float colorbv;
    /** Holds information about the rotation of the body **/
    public RotationComponent rc;

    /** Component alpha mirror **/
    public float compalpha;

    /**
     * Whether we are out of the time baseline range in the algorithm that works
     * out the coordinates of this body
     **/
    protected boolean coordinatesTimeOverflow = false;

    /**
     * Simple constructor
     */
    public CelestialBody() {
        super();
        TH_OVER_FACTOR = (float) (THRESHOLD_POINT() / Settings.settings.scene.label.number);
    }

    /**
     * Billboard quad render, for planets and stars.
     */
    @Override
    public void render(ExtShaderProgram shader, float alpha, IntMesh mesh, ICamera camera) {
        compalpha = alpha;

        float size = getFuzzyRenderSize(camera);
        shader.setUniformf("u_pos", translation.put(F31.get()));
        shader.setUniformf("u_size", size);

        shader.setUniformf("u_color", ccPale[0], ccPale[1], ccPale[2], alpha * opacity);
        shader.setUniformf("u_inner_rad", getInnerRad());
        shader.setUniformf("u_distance", (float) distToCamera);
        shader.setUniformf("u_apparent_angle", (float) viewAngleApparent);
        shader.setUniformf("u_thpoint", (float) THRESHOLD_POINT() * camera.getFovFactor());

        // Whether light scattering is enabled or not
        shader.setUniformi("u_lightScattering", (this instanceof Star && GaiaSky.instance.getPostProcessor().isLightScatterEnabled()) ? 1 : 0);

        shader.setUniformf("u_radius", (float) getRadius());

        // Render the mesh
        mesh.render(shader, GL20.GL_TRIANGLES, 0, 6);
    }

    public float getFuzzyRenderSize(ICamera camera) {
        float thAngleQuad = (float) THRESHOLD_QUAD() * camera.getFovFactor();
        double size = 0f;
        if (viewAngle >= THRESHOLD_POINT() * camera.getFovFactor()) {
            if (viewAngle < thAngleQuad) {
                size = FastMath.tan(thAngleQuad) * distToCamera;
            } else {
                size = this.size;
            }
        }
        return (float) (size / camera.getFovFactor());
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        if (camera.getCurrent() instanceof FovCamera) {
            render2DLabel(batch, shader, rc, sys.font2d, camera, text(), pos.put(D31.get()));
        } else {
            // 3D distance font
            Vector3d pos = D31.get();
            textPosition(camera, pos);
            shader.setUniformf("u_viewAngle", forceLabel ? 2f : (float) viewAngleApparent);
            shader.setUniformf("u_viewAnglePow", forceLabel ? 1f : getViewAnglePow());
            shader.setUniformf("u_thOverFactor", forceLabel ? 1f : getThOverFactor(camera));
            shader.setUniformf("u_thOverFactorScl", forceLabel ? 1f : getThOverFactorScl());

            render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor(), this.forceLabel);
        }
    }

    protected float getViewAnglePow() {
        return 1f;
    }

    protected float getThOverFactorScl() {
        return 1f;
    }

    protected float getThOverFactor(ICamera camera) {
        return TH_OVER_FACTOR / camera.getFovFactor();
    }

    protected void setColor2Data() {
        final float plus = .1f;
        ccPale = new float[] { Math.min(1, cc[0] + plus), Math.min(1, cc[1] + plus), Math.min(1, cc[2] + plus) };
    }

    public abstract float getInnerRad();

    public void setMag(Double mag) {
        this.absmag = mag.floatValue();
        this.appmag = mag.floatValue();
    }

    public void setAbsmag(Double absmag) {
        this.absmag = absmag.floatValue();
    }

    public void setAppmag(Double appmag) {
        this.appmag = appmag.floatValue();
    }

    public float getAppmag() {
        return appmag;
    }

    public float getAbsmag() {
        return absmag;
    }

    public boolean isActive() {
        return true;
    }

    /**
     * Adds all the children that are focusable objects to the list.
     *
     * @param list The list to add to.
     */
    public void addFocusableObjects(Array<IFocus> list) {
        list.add(this);
        super.addFocusableObjects(list);
    }

    public double getViewAngle() {
        return viewAngle;
    }

    /**
     * Sets the size of this entity in kilometers
     *
     * @param size The size in km
     */
    public void setSize(Double size) {
        // Size gives us the radius, and we want the diameter
        this.size = (float) (size * 2 * Constants.KM_TO_U);
    }

    public void setColorbv(Double colorbv) {
        this.colorbv = colorbv.floatValue();
    }

    public boolean isStar() {
        return false;
    }

    /**
     * Sets the rotation period in hours
     */
    public void setRotation(RotationComponent rc) {
        this.rc = rc;
    }

    @Override
    public <T extends SceneGraphNode> T getSimpleCopy() {
        CelestialBody copy = super.getSimpleCopy();
        copy.absmag = this.absmag;
        copy.appmag = this.appmag;
        copy.colorbv = this.colorbv;
        copy.rc = this.rc;
        return (T) copy;
    }

    @Override
    public boolean renderText() {
        return names != null && GaiaSky.instance.isOn(ComponentType.Labels) && (forceLabel || FastMath.pow(viewAngleApparent, getViewAnglePow()) >= (TH_OVER_FACTOR * getThOverFactorScl()));
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textScale() {
        return (float) FastMath.atan(labelMax()) * labelFactor() * 4e2f;
    }

    @Override
    public float textSize() {
        return (float) (labelMax() * distToCamera * labelFactor());
    }

    protected float labelSizeConcrete() {
        return this.size;
    }

    protected abstract float labelFactor();

    protected abstract float labelMax();

    public void textPosition(ICamera cam, Vector3d out) {
        translation.put(out);
        double len = out.len();
        out.clamp(0, len - getRadius()).scl(0.9f);

        Vector3d aux = D32.get();
        aux.set(cam.getUp());

        aux.crs(out).nor();

        float dist = -0.02f * cam.getFovFactor() * (float) out.len();

        aux.add(cam.getUp()).nor().scl(dist);

        out.add(aux);

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return getLocalizedName();
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public boolean isLabel() {
        return true;
    }

    public double getPmX() {
        return 0;
    }

    public double getPmY() {
        return 0;
    }

    public double getPmZ() {
        return 0;
    }

    public RotationComponent getRotationComponent() {
        return rc;
    }

    public Quaterniond getOrientationQuaternion() {
        return null;
    }

    public void addHit(int screenX, int screenY, int w, int h, int minPixDist, NaturalCamera camera, Array<IFocus> hits) {
        if (checkHitCondition()) {
            Vector3 pos = F31.get();
            Vector3b aux = B31.get();
            Vector3b posd = getAbsolutePosition(aux).add(camera.getInversePos());
            pos.set(posd.valuesf());

            if (camera.direction.dot(posd) > 0) {
                // The object is in front of us
                double angle = computeViewAngle(camera.getFovFactor());

                PerspectiveCamera pCamera;
                if (Settings.settings.program.modeStereo.active) {
                    if (screenX < Gdx.graphics.getWidth() / 2f) {
                        pCamera = camera.getCameraStereoLeft();
                    } else {
                        pCamera = camera.getCameraStereoRight();
                    }
                    pCamera.update();
                } else {
                    pCamera = camera.camera;
                }

                angle = (float) Math.toDegrees(angle * camera.getFovFactor()) * (40f / pCamera.fieldOfView);
                double pixelSize = Math.max(minPixDist, ((angle * pCamera.viewportHeight) / pCamera.fieldOfView) / 2);
                pCamera.project(pos);
                pos.y = pCamera.viewportHeight - pos.y;
                if (Settings.settings.program.modeStereo.active) {
                    pos.x /= 2;
                }
                // Check click distance
                if (checkClickDistance(screenX, screenY, pos, camera, pCamera, pixelSize)) {
                    //Hit
                    hits.add(this);
                }
            }
        }
    }

    public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
        if (checkHitCondition()) {
            Vector3b aux = B31.get();
            Vector3b posb = getAbsolutePosition(aux).add(camera.getInversePos());

            if (camera.direction.dot(posb) > 0) {
                // The star is in front of us
                // Diminish the size of the star
                // when we are close by
                double dist = posb.lend();
                double distToLine = Intersectord.distanceLinePoint(p0, p1, posb.put(D31.get()));
                double value = distToLine / dist;

                if (value < 0.01) {
                    hits.add(this);
                }
            }
        }
    }

    protected boolean checkClickDistance(int screenX, int screenY, Vector3 pos, NaturalCamera camera, PerspectiveCamera pcamera, double pixelSize) {
        return pos.dst(screenX % pcamera.viewportWidth, screenY, pos.z) <= pixelSize;
    }

    protected double computeViewAngle(float fovFactor) {
        return viewAngle;
    }

    protected boolean checkHitCondition() {
        return !coordinatesTimeOverflow && GaiaSky.instance.isOn(ct);
    }

    public void makeFocus() {
    }

    @Override
    public long getCandidateId() {
        return getId();
    }

    @Override
    public String getCandidateName() {
        return getName();
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return getViewAngleApparent();
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return coordinatesTimeOverflow;
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    public void setAltname(String altname) {
        this.addName(altname);
    }

    @Override
    public IFocus getFocus(String name) {
        if (this.hasName(name))
            return this;

        return null;
    }

    @Override
    public boolean isValidPosition() {
        return !coordinatesTimeOverflow;
    }

    @Override
    public String getClosestName() {
        return getName();
    }

    @Override
    public double getClosestDistToCamera() {
        return getDistToCamera();
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        return getAbsolutePosition(out);
    }

    public String getWikiname() {
        return wikiname;
    }

    public void setWikiname(String wikiname) {
        this.wikiname = wikiname;
    }
}
