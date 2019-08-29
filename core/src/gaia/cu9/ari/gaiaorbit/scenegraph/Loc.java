/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.render.I3DTextRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.gdx.g2d.ExtSpriteBatch;
import gaia.cu9.ari.gaiaorbit.util.gdx.shader.ExtShaderProgram;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import net.jafama.FastMath;

public class Loc extends AbstractPositionEntity implements I3DTextRenderable {
    private static final float LOWER_LIMIT = 3e-4f;
    private static final float UPPER_LIMIT = 3e-3f;

    /** The display name **/
    String displayName;

    /** Longitude and latitude **/
    Vector2 location;
    Vector3 location3d;
    /**
     * This controls the distance from the center in case of non-spherical
     * objects
     **/
    float distFactor = 1f;

    public Loc() {
        cc = new float[] { 1f, 1f, 1f, 1f };
        localTransform = new Matrix4();
        location3d = new Vector3();
    }

    public void initialize() {

    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (renderText() && isVisibilityOn()) {
            addToRender(this, RenderGroup.FONT_LABEL);
        }
    }

    @Override
    public void updateLocal(ITimeFrameProvider time, ICamera camera) {

        if (((ModelBody) parent).viewAngle > ((ModelBody) parent).THRESHOLD_QUAD() * 30f) {
            updateLocalValues(time, camera);

            this.translation.add(pos);

            Vector3d aux = aux3d1.get();
            this.distToCamera = (float) translation.put(aux).len();
            this.viewAngle = (float) FastMath.atan(size / distToCamera) / camera.getFovFactor();
            this.viewAngleApparent = this.viewAngle * camera.getFovFactor();
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
        if (viewAngle < LOWER_LIMIT || viewAngle > UPPER_LIMIT * Constants.DISTANCE_SCALE_FACTOR || !GaiaSky.instance.isOn(ct.getFirstOrdinal())) {
            return false;
        }
        Vector3d aux = aux3d1.get();
        translation.put(aux).scl(-1);

        double cosalpha = aux.add(location3d.x, location3d.y, location3d.z).nor().dot(GaiaSky.instance.cam.getDirection().nor());
        return cosalpha < -0.3f;
    }

    /**
     * Label rendering.
     */
    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera) {

        Vector3d pos = aux3d1.get();
        textPosition(camera, pos);
        shader.setUniformf("u_viewAngle", (float) (viewAngleApparent * ((ModelBody) parent).locVaMultiplier * Constants.U_TO_KM));
        shader.setUniformf("u_viewAnglePow", 1f);
        shader.setUniformf("u_thOverFactor", ((ModelBody) parent).locThOverFactor / (float) Constants.DISTANCE_SCALE_FACTOR);
        shader.setUniformf("u_thOverFactorScl", 1f);
        render3DLabel(batch, shader, sys.fontDistanceField, camera, rc, text(), pos, textScale() * camera.getFovFactor(), textSize() * camera.getFovFactor());
    }

    @Override
    public float[] textColour() {
        return cc;
    }

    @Override
    public float textSize() {
        return size / 3.5f;
    }

    @Override
    public float textScale() {
        return 1e-4f / textSize() * (float) Constants.DISTANCE_SCALE_FACTOR;
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
        Gdx.gl.glDepthMask(true);
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
        this.name = name;
        this.displayName = '\u02D9' + " " + name;
    }

    @Override
    public float getTextOpacity(){
        return getOpacity();
    }
}
