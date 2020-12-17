/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.IFocus;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Frustumd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public abstract class AbstractCamera implements ICamera {
    protected static final Log logger = Logger.getLogger(AbstractCamera.class);

    private static final Matrix4d invProjectionView = new Matrix4d();

    /** Camera far value **/
    public double CAM_FAR;
    /** Camera near value **/
    public double CAM_NEAR;

    public Vector3d pos, posinv, prevpos, shift, tmp;
    /**
     * Angle from the center to the corner of the screen in scene coordinates,
     * in radians
     **/
    protected float angleEdgeRad;
    /** Aspect ratio **/
    protected float ar;

    /** Distance of camera to center **/
    protected double distance;

    /** The parent **/
    protected CameraManager parent;


    /** The main camera **/
    public PerspectiveCamera camera;

    /** Stereoscopic mode cameras **/
    protected PerspectiveCamera camLeft, camRight;

    /** Vector with all perspective cameras **/
    protected PerspectiveCamera[] cameras;

    protected Matrix4d projection, view, combined;
    protected Frustumd frustumd;

    public float fovFactor;

    /** Closest non-star body to the camera **/
    protected IFocus closestBody;

    /**
     * The closest particle to the camera
     */
    protected IFocus closestStar;

    protected Matrix4 prevCombined;

    /**
     * The closest between {@link AbstractCamera#closestBody} and
     * {@link AbstractCamera#closestStar}
     */
    protected IFocus closest;

    private void initNearFar() {
        CAM_NEAR = 0.5d * Constants.M_TO_U;
        CAM_FAR = 1d * Constants.MPC_TO_U;
    }

    public AbstractCamera(CameraManager parent) {
        initNearFar();

        this.parent = parent;
        pos = new Vector3d();
        prevpos = new Vector3d();
        posinv = new Vector3d();
        shift = new Vector3d();
        tmp = new Vector3d();
        prevCombined = new Matrix4();

        camLeft = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight());
        camLeft.near = (float) CAM_NEAR;
        camLeft.far = (float) CAM_FAR;

        camRight = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight());
        camRight.near = (float) CAM_NEAR;
        camRight.far = (float) CAM_FAR;

        projection = new Matrix4d();
        view = new Matrix4d();
        combined = new Matrix4d();
        frustumd = new Frustumd();
    }

    @Override
    public void updateAngleEdge(int width, int height) {
        ar = (float) width / (float) height;
        angleEdgeRad = getAngleEdge(width, height, camera.fieldOfView);
    }

    public float getAngleEdge(int width, int height, float angle) {
        float ar = (float) width / (float) height;
        float h = angle;
        float w = h * ar;
        return (float) (Math.toRadians(Math.sqrt(h * h + w * w))) / 2f;
    }

    @Override
    public float getFovFactor() {
        return fovFactor;
    }

    @Override
    public Vector3d getPos() {
        return pos;
    }

    @Override
    public void setPos(Vector3d pos) {
        this.pos.set(pos);
    }

    @Override
    public Vector3d getPreviousPos(){
        return prevpos;
    }

    @Override
    public void setPreviousPos(Vector3d prevpos){
        this.prevpos.set(prevpos);
    }


    @Override
    public Vector3d getInversePos() {
        return posinv;
    }

    @Override
    public float getAngleEdge() {
        return angleEdgeRad;
    }

    @Override
    public CameraManager getManager() {
        return parent;
    }

    @Override
    public void render(int rw, int rh) {

    }

    @Override
    public ICamera getCurrent() {
        return this;
    }

    private static final double VIEW_ANGLE = Math.toRadians(0.05);

    @Override
    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb) {
        return isVisible(time, cb.viewAngle, cb.translation, cb.distToCamera);
    }
    @Override
    public boolean isVisible(ITimeFrameProvider time, Vector3d pos) {
        return isVisible(time, 1e-8, pos, pos.len());
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, double viewAngle, Vector3d pos, double distToCamera) {
        return (!(this instanceof FovCamera) && viewAngle > VIEW_ANGLE) || GlobalResources.isInView(pos, distToCamera, angleEdgeRad, tmp.set(getCamera().direction));
    }

    /**
     * Returns true if a body with the given position is observed in any of the
     * given directions using the given cone angle
     *
     * @param cb      The body.
     * @param fcamera The FovCamera.
     * @return True if the body is observed. False otherwise.
     */
    protected boolean computeVisibleFovs(CelestialBody cb, FovCamera fcamera) {
        boolean visible = false;
        Vector3d[] dirs;
        dirs = fcamera.directions;
        visible = visible || GlobalResources.isInView(cb.translation, cb.distToCamera, fcamera.angleEdgeRad, dirs[0]) || GlobalResources.isInView(cb.translation, cb.distToCamera, fcamera.angleEdgeRad, dirs[1]);
        return visible;
    }

    public double getDistance() {
        return distance;
    }

    public void copyParamsFrom(AbstractCamera other) {
        this.pos.set(other.pos);
        this.posinv.set(other.posinv);
        this.getDirection().set(other.getDirection());
        this.getUp().set(other.getUp());
        this.closestBody = other.closestBody;

    }

    private void copyCamera(PerspectiveCamera source, PerspectiveCamera target) {
        target.far = source.far;
        target.near = source.near;
        target.direction.set(source.direction);
        target.up.set(source.up);
        target.position.set(source.position);
        target.fieldOfView = source.fieldOfView;
        target.viewportHeight = source.viewportHeight;
        target.viewportWidth = source.viewportWidth;
    }

    @Override
    public PerspectiveCamera getCameraStereoLeft() {
        return camLeft;
    }

    @Override
    public PerspectiveCamera getCameraStereoRight() {
        return camRight;
    }

    @Override
    public void setCameraStereoLeft(PerspectiveCamera cam) {
        copyCamera(cam, camLeft);
    }

    @Override
    public void setCameraStereoRight(PerspectiveCamera cam) {
        copyCamera(cam, camRight);
    }

    @Override
    public void setShift(Vector3d shift) {
        this.shift.set(shift);
    }

    @Override
    public Vector3d getShift() {
        return this.shift;
    }

    public void update(PerspectiveCamera cam, Vector3d position, Vector3d direction, Vector3d up) {
        double aspect = cam.viewportWidth / cam.viewportHeight;
        projection.setToProjection(cam.near, cam.far, cam.fieldOfView, aspect);
        view.setToLookAt(position, tmp.set(position).add(direction), up);
        combined.set(projection);
        Matrix4d.mul(combined.val, view.val);

        invProjectionView.set(combined);
        Matrix4d.inv(invProjectionView.val);
        frustumd.update(invProjectionView);
    }

    @Override
    public void checkClosestBody(IFocus cb) {
        // A copy can never bee the closest
        if (!cb.isCopy())
            if (closestBody == null) {
                closestBody = cb;
            } else {
                if (closestBody.getDistToCamera() - closestBody.getRadius() > cb.getDistToCamera() - cb.getRadius()) {
                    closestBody = cb;
                }
            }
    }

    @Override
    public IFocus getClosestBody() {
        return closestBody;
    }

    @Override
    public IFocus getSecondClosestBody() {
        return closestBody;
    }

    public IFocus getClosestParticle() {
        return closestStar;
    }

    public void checkClosestParticle(IFocus star) {
        if (closestStar == null || closestStar.getClosestDistToCamera() > star.getClosestDistToCamera()) {
            closestStar = star;
        }
    }

    @Override
    public IFocus getClosest(){
        return closest;
    }

    @Override
    public void setClosest(IFocus focus) {
        closest = focus;
    }

    @Override
    public Vector3d getVelocity() {
        return null;
    }

    @Override
    public void updateFrustumPlanes() {
        initNearFar();
        setFrustumPlanes(camera);
        setFrustumPlanes(camLeft);
        setFrustumPlanes(camRight);
    }

    protected void setFrustumPlanes(PerspectiveCamera cam) {
        if (cam != null) {
            cam.near = (float) CAM_NEAR;
            cam.far = (float) CAM_FAR;
            cam.update();
        }
    }

    public double getNear() {
        return CAM_NEAR;
    }

    public double getFar() {
        return CAM_FAR;
    }

    @Override
    public Matrix4 getProjView(){
        return camera.combined;
    }

    @Override
    public Matrix4 getPreviousProjView(){
        return prevCombined;
    }

    @Override
    public void setPreviousProjView(Matrix4 mat){
        prevCombined.set(mat);
    }
}
