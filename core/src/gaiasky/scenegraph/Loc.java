/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.I3DTextRenderable;
import gaiasky.render.ILineRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.Quaterniond;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

public class Loc extends SceneGraphNode implements IFocus, I3DTextRenderable, ILineRenderable {
    private static final float LOWER_LIMIT = 3e-4f;
    private static final float UPPER_LIMIT = 3e-3f;

    /**
     * The display name
     **/
    String displayName;

    /**
     * Longitude and latitude
     **/
    Vector2 location;
    Vector3 location3d;
    /**
     * This controls the distance from the center in case of non-spherical
     * objects
     **/
    float distFactor = 1f;

    // Size in Km
    float sizeKm;

    public Loc() {
        cc = new float[]{1f, 1f, 1f, 1f};
        localTransform = new Matrix4();
        location3d = new Vector3();
    }

    public void initialize() {
        this.sizeKm = (float) (this.size * Constants.U_TO_KM);
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (this.shouldRender() && this.renderText()) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {

        if (parent.viewAngle > ((ModelBody) parent).thresholdQuad * 30f || camera.isFocus(this)) {
            updateLocalValues(time, camera);

            this.translation.add(pos);

            this.opacity = this.getVisibilityOpacityFactor();

            Vector3d aux = D31.get();
            this.distToCamera = (float) translation.put(aux).len();
            this.viewAngle = (float) FastMath.atan(size / distToCamera);
            this.viewAngleApparent = this.viewAngle / camera.getFovFactor();
            if (!copy) {
                addToRenderLists(camera);
            }
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        ModelBody papa = (ModelBody) parent;
        papa.setToLocalTransform(distFactor, localTransform, false);

        location3d.set(0, 0, -.5f);
        // Latitude [-90..90]
        location3d.rotate(location.y, 1, 0, 0);
        // Longitude [0..360]
        location3d.rotate(location.x + 90, 0, 1, 0);

        location3d.mul(localTransform);

    }

    public Vector2 getLocation() {
        return location;
    }

    public void setLocation(double[] pos) {
        this.location = new Vector2((float) pos[0], (float) pos[1]);
    }

    @Override
    public boolean renderText() {
        if(GaiaSky.instance.isOn(ct) && (viewAngle >= LOWER_LIMIT && viewAngle <= UPPER_LIMIT * Constants.DISTANCE_SCALE_FACTOR || forceLabel)) {
            Vector3d aux = D31.get();
            translation.put(aux).scl(-1);

            double cosAlpha = aux.add(location3d.x, location3d.y, location3d.z).nor().dot(GaiaSky.instance.cameraManager.getDirection().nor());
            return cosAlpha < -0.3f;
        } else {
            return false;
        }
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {
        Vector3d pos = D31.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", forceLabel ? 2f : (float) (viewAngleApparent * ((ModelBody) parent).locVaMultiplier * Constants.U_TO_KM));
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thLabel", forceLabel ? 1f : ((ModelBody) parent).locThresholdLabel / (float) Constants.DISTANCE_SCALE_FACTOR);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, distToCamera, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor(), this.forceLabel);
    }

    @Override
    public float[] textColour() {
        return labelcolor;
    }

    @Override
    public float textSize() {
        return size / 1.5f;
    }

    @Override
    public float textScale() {
        return sizeKm * 1e-7f / textSize() * (float) Constants.DISTANCE_SCALE_FACTOR;
    }

    @Override
    public void textPosition(ICamera cam, Vector3d out) {
        out.set(location3d);
        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        return displayName;
    }

    @Override
    public void textDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    @Override
    public boolean isLabel() {
        return false;
    }

    /**
     * Sets the absolute size of this entity
     *
     * @param size
     */
    public void setSize(Double size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setSize(Long size) {
        this.size = (float) (size * Constants.KM_TO_U);
    }

    public void setDistFactor(Double distFactor) {
        this.distFactor = distFactor.floatValue();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        this.displayName = "˟ " + getLocalizedName();
    }

    @Override
    public float getTextOpacity() {
        return getOpacity();
    }

    @Override
    public boolean mustAddToIndex() {
        return false;
    }

    @Override
    public float getLineWidth() {
        return 1.0f;
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        Vector3d pos = D31.get();
        textPosition(camera, pos);

        Vector3 v = F31.get();
        pos.put(v);
        camera.getCamera().project(v);
        v.set(v.x + 5, renderer.rc.h() - v.y + 5, v.z);
        v.z = (float) pos.z;
        camera.getCamera().unproject(v);

        renderer.addLine(this, pos.x, pos.y, pos.z, v.x, v.y, v.z, 0.5f, 0.5f, 1f, 1f);
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }

    @Override
    public long getCandidateId() {
        return getId();
    }

    @Override
    public String getClosestName() {
        return getName();
    }

    @Override
    public String getCandidateName() {
        return getName();
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public Vector3b getClosestAbsolutePos(Vector3b out) {
        getAbsolutePosition(out);
        return out;
    }

    @Override
    public double getClosestDistToCamera() {
        return distToCamera;
    }

    @Override
    public double getCandidateViewAngleApparent() {
        return viewAngleApparent;
    }

    @Override
    public float getAppmag() {
        return 0;
    }

    @Override
    public float getAbsmag() {
        return 0;
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
        return this;
    }

    @Override
    public boolean isCoordinatesTimeOverflow() {
        return false;
    }
}
