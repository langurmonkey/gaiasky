/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.camera.Proximity;
import gaiasky.util.camera.Proximity.NearbyRecord;
import gaiasky.util.math.FrustumDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

public abstract class AbstractCamera implements ICamera {
    protected static final Log logger = Logger.getLogger(AbstractCamera.class);

    private static final Matrix4d invProjectionView = new Matrix4d();
    private static final double VIEW_ANGLE = Math.toRadians(0.05);
    /**
     * Camera far value.
     **/
    public double CAM_FAR;
    /**
     * Camera near value.
     **/
    public double CAM_NEAR;
    public Vector3b pos, posInv, prevPos;
    public Vector3d tmp, shift;
    /**
     * The main camera.
     **/
    public PerspectiveCamera camera;
    public float fovFactor;
    /**
     * Angle from the center to the corner of the screen in scene coordinates,
     * in radians.
     **/
    protected float angleEdgeRad;
    /**
     * Aspect ratio.
     **/
    protected float ar;
    /**
     * Distance of camera to center.
     **/
    protected double distance;
    /**
     * The parent.
     **/
    protected CameraManager parent;
    /**
     * Stereoscopic mode cameras.
     **/
    protected PerspectiveCamera camLeft, camRight;
    /**
     * Vector with all perspective cameras.
     **/
    protected PerspectiveCamera[] cameras;
    protected Matrix4d projection, view, combined;
    protected FrustumDouble frustum;
    /**
     * Closest non-star body to the camera.
     **/
    protected FocusView closestBody;
    /**
     * The closest particle to the camera.
     */
    protected IFocus closestStar;
    protected FocusView closestStarView;
    protected Proximity proximity;
    protected Matrix4 prevCombined;
    /**
     * The closest between {@link AbstractCamera#closestBody} and
     * {@link AbstractCamera#closestStar}.
     */
    protected IFocus closest;
    /** The last input event was by a gamepad. **/
    protected boolean gamepadInput;

    protected AbstractCamera(CameraManager parent) {
        initNearFar();

        this.parent = parent;
        pos = new Vector3b();
        prevPos = new Vector3b();
        posInv = new Vector3b();
        shift = new Vector3d();
        tmp = new Vector3d();
        prevCombined = new Matrix4();

        camLeft = new PerspectiveCamera(Settings.settings.scene.camera.fov, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight());
        camLeft.near = (float) CAM_NEAR;
        camLeft.far = (float) CAM_FAR;

        camRight = new PerspectiveCamera(Settings.settings.scene.camera.fov, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight());
        camRight.near = (float) CAM_NEAR;
        camRight.far = (float) CAM_FAR;

        projection = new Matrix4d();
        view = new Matrix4d();
        combined = new Matrix4d();
        frustum = new FrustumDouble();

        closestBody = new FocusView();
        closestStarView = new FocusView();

        proximity = new Proximity(Constants.N_DIR_LIGHTS);
    }

    private void initNearFar() {
        CAM_NEAR = 0.5d * Constants.M_TO_U;
        CAM_FAR = Constants.MPC_TO_U;
    }

    @Override
    public void updateAngleEdge(int width, int height) {
        ar = (float) width / (float) height;
        angleEdgeRad = getAngleEdge(width, height, camera.fieldOfView);
    }

    public float getAngleEdge(int width, int height, float angle) {
        float ar = (float) width / (float) height;
        float w = angle * ar;
        return (float) (Math.toRadians(Math.sqrt(angle * angle + w * w))) / 2f;
    }

    @Override
    public float getFovFactor() {
        return fovFactor;
    }

    @Override
    public Vector3b getPos() {
        return pos;
    }

    @Override
    public void setPos(Vector3d pos) {
        this.pos.set(pos);
    }

    @Override
    public void setPos(Vector3b pos) {
        this.pos.set(pos);
    }

    @Override
    public Vector3b getPreviousPos() {
        return prevPos;
    }

    @Override
    public void setPreviousPos(Vector3b pos) {
        this.prevPos.set(pos);
    }

    @Override
    public void setPreviousPos(Vector3d prevpos) {
        this.prevPos.set(prevpos);
    }

    @Override
    public Vector3b getInversePos() {
        return posInv;
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

    @Override
    public boolean isVisible(Entity cb) {
        var body = Mapper.body.get(cb);
        var graph = Mapper.graph.get(cb);
        return isVisible(body.solidAngle, graph.translation, body.distToCamera);
    }

    @Override
    public boolean isVisible(double viewAngle, Vector3d pos, double distToCamera) {
        return (viewAngle > VIEW_ANGLE) || GlobalResources.isInView(pos, distToCamera, angleEdgeRad, tmp.set(getCamera().direction));
    }

    public boolean isVisible(double viewAngle, Vector3b pos, double distToCamera) {
        return (viewAngle > VIEW_ANGLE) || GlobalResources.isInView(pos, distToCamera, angleEdgeRad, tmp.set(getCamera().direction));
    }

    public double getDistance() {
        return distance;
    }

    public void copyParamsFrom(AbstractCamera other) {
        this.pos.set(other.pos);
        this.posInv.set(other.posInv);
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
    public void setCameraStereoLeft(PerspectiveCamera cam) {
        copyCamera(cam, camLeft);
    }

    @Override
    public PerspectiveCamera getCameraStereoRight() {
        return camRight;
    }

    @Override
    public void setCameraStereoRight(PerspectiveCamera cam) {
        copyCamera(cam, camRight);
    }

    @Override
    public Vector3d getShift() {
        return this.shift;
    }

    @Override
    public void setShift(Vector3d shift) {
        this.shift.set(shift);
    }

    public void update(PerspectiveCamera cam, Vector3d position, Vector3d direction, Vector3d up) {
        double aspect = cam.viewportWidth / cam.viewportHeight;
        projection.setToProjection(cam.near, cam.far, cam.fieldOfView, aspect);
        view.setToLookAt(position, tmp.set(position).add(direction), up);
        combined.set(projection);
        Matrix4d.mul(combined.val, view.val);

        invProjectionView.set(combined);
        Matrix4d.inv(invProjectionView.val);
        frustum.update(invProjectionView);
    }

    @Override
    public synchronized void checkClosestBody(IFocus cb) {
        if (cb instanceof FocusView candidate) {
            // A copy can never be the closest
            if (!cb.isCopy() && !Mapper.tagNoClosest.has(((FocusView) cb).getEntity())) {
                if (closestBody.getEntity() == null) {
                    closestBody.setEntity(candidate.getEntity());
                } else {
                    if (closestBody.getDistToCamera() - closestBody.getRadius() > cb.getDistToCamera() - cb.getRadius()) {
                        closestBody.setEntity(candidate.getEntity());
                    }
                }
            }
        }
    }

    public synchronized void checkClosestBody(Entity cb) {
        // A copy can never be the closest
        var base = Mapper.base.get(cb);
        if (!base.copy && !Mapper.tagNoClosest.has(cb)) {
            if (!closestBody.isValid()) {
                closestBody.setEntity(cb);
            } else {
                double distMinusRadius = closestBody.getDistToCamera() - closestBody.getRadius();
                Entity prev = closestBody.getEntity();

                // Change and test.
                closestBody.setEntity(cb);
                if (distMinusRadius < closestBody.getDistToCamera() - closestBody.getRadius()) {
                    closestBody.setEntity(prev);
                }
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

    public IFocus getCloseLightSource(int i) {
        assert proximity != null : "Proximity is null";
        assert i < proximity.effective.length : "Index out of bounds: i=" + i + ", length=" + proximity.effective.length;
        return proximity.effective[i];
    }

    public synchronized void checkClosestParticle(IFocus star) {
        if (star instanceof NearbyRecord) {
            proximity.update((NearbyRecord) star);
        } else {
            proximity.update(star, this);
        }

        if (closestStar == null || !closestStar.isEmpty() && closestStar.getClosestDistToCamera() > star.getClosestDistToCamera()) {
            if (star instanceof FocusView) {
                closestStarView.setEntity(((FocusView) star).getEntity());
                closestStar = closestStarView;
            } else {
                closestStar = star;
            }
        }
    }

    public void swapBuffers() {
        proximity.swapBuffers();
    }

    @Override
    public IFocus getClosest() {
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
    public Matrix4 getProjView() {
        return camera.combined;
    }

    @Override
    public Matrix4 getPreviousProjView() {
        return prevCombined;
    }

    @Override
    public void setPreviousProjView(Matrix4 mat) {
        prevCombined.set(mat);
    }

    @Override
    public void setGamepadInput(boolean state) {
        this.gamepadInput = state;
    }

    @Override
    public void setPointerProjectionOnFocus(Vector3 point) {
        // Empty.
    }
}
