/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.camera.CameraUtils;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class CameraManager implements ICamera, IObserver {
    private final ICamera[] cameras;
    /**
     * Last position, for working out velocity
     **/
    private final Vector3d lastPos;
    private final Vector3d out;
    private final Vector3d in;
    private final Vector3b inb;
    private final Vector3 vec;
    private final Vector3 v0;
    private final Vector3 v1;
    private final Vector3 intersection;
    private final Matrix4 localTransformInv;
    public CameraMode mode;
    public ICamera current;
    public NaturalCamera naturalCamera;
    public SpacecraftCamera spacecraftCamera;
    public RelativisticCamera relativisticCamera;
    private final FocusView focusView;
    public IFocus previousClosest;
    /**
     * Current velocity in km/h
     **/
    protected double speed;
    /**
     * Velocity vector
     **/
    protected Vector3d velocity, velocityNormalized;
    private BackupProjectionCamera backupCamera;

    public CameraManager(AssetManager manager, CameraMode mode, boolean vr, GlobalResources globalResources) {
        // Initialize Cameras
        this.naturalCamera = new NaturalCamera(manager, this, vr, globalResources.getSpriteShader(), globalResources.getShapeShader());
        this.spacecraftCamera = new SpacecraftCamera(this);
        // TODO - develop relativistic camera.
        this.relativisticCamera = new RelativisticCamera(manager, this);

        this.cameras = new ICamera[]{naturalCamera, spacecraftCamera};
        this.focusView = new FocusView();

        this.mode = mode;
        this.lastPos = new Vector3d();
        this.in = new Vector3d();
        this.inb = new Vector3b();
        this.out = new Vector3d();
        this.vec = new Vector3();
        this.v0 = new Vector3();
        this.v1 = new Vector3();
        this.intersection = new Vector3();
        this.velocity = new Vector3d();
        this.velocityNormalized = new Vector3d();
        this.localTransformInv = new Matrix4();

        updateCurrentCamera();

        EventManager.instance.subscribe(this, Event.CAMERA_MODE_CMD, Event.FOV_CHANGE_NOTIFICATION);
    }

    /**
     * Stores the normalized rays representing the camera frustum in eye space in a 4x4 matrix.  Each row is a vector.
     *
     * @param cam            The perspective camera
     * @param frustumCorners The matrix to fill
     */
    public static void getFrustumCornersEye(PerspectiveCamera cam, Matrix4 frustumCorners) {
        float camFov = cam.fieldOfView;
        float camAspect = cam.viewportWidth / cam.viewportHeight;

        float fovWHalf = camFov * 0.5f;

        float tan_fov = (float) Math.tan(Math.toRadians(fovWHalf));

        Vector3 right = Vector3.X;
        Vector3 up = Vector3.Y;
        Vector3 forward = Vector3.Z;

        Vector3 toRight = (new Vector3(right)).scl(tan_fov * camAspect);
        Vector3 toTop = (new Vector3(up)).scl(tan_fov);

        Vector3 topLeft = (new Vector3(forward)).scl(-1).sub(toRight).add(toTop).nor();
        Vector3 topRight = (new Vector3(forward)).scl(-1).add(toRight).add(toTop).nor();
        Vector3 bottomRight = (new Vector3(forward)).scl(-1).add(toRight).sub(toTop).nor();
        Vector3 bottomLeft = (new Vector3(forward)).scl(-1).sub(toRight).sub(toTop).nor();

        // Top left
        frustumCorners.val[Matrix4.M00] = topLeft.x;
        frustumCorners.val[Matrix4.M10] = topLeft.y;
        frustumCorners.val[Matrix4.M20] = topLeft.z;

        // Top right
        frustumCorners.val[Matrix4.M01] = topRight.x;
        frustumCorners.val[Matrix4.M11] = topRight.y;
        frustumCorners.val[Matrix4.M21] = topRight.z;

        // Bottom right
        frustumCorners.val[Matrix4.M02] = bottomRight.x;
        frustumCorners.val[Matrix4.M12] = bottomRight.y;
        frustumCorners.val[Matrix4.M22] = bottomRight.z;

        // Bottom left
        frustumCorners.val[Matrix4.M03] = bottomLeft.x;
        frustumCorners.val[Matrix4.M13] = bottomLeft.y;
        frustumCorners.val[Matrix4.M23] = bottomLeft.z;
    }

    private AbstractCamera backupCam(ICamera current) {
        if (current instanceof AbstractCamera)
            return (AbstractCamera) current;
        else
            return null;
    }

    private void restoreCam(AbstractCamera cam, AbstractCamera copy) {
        if (copy != null)
            cam.copyParamsFrom(copy);
    }

    public void updateCurrentCamera() {
        AbstractCamera aux;
        // Update
        switch (mode) {
            case GAME_MODE:
                EventManager.publish(Event.CAMERA_CINEMATIC_CMD, this, false);
            case FREE_MODE:
            case FOCUS_MODE:
                aux = backupCam(current);
                current = naturalCamera;
                restoreCam(naturalCamera, aux);
                break;
            case SPACECRAFT_MODE:
                aux = backupCam(current);
                current = spacecraftCamera;
                restoreCam(spacecraftCamera, aux);
                break;
            default:
                break;
        }

    }

    public boolean isNatural() {
        return current == naturalCamera;
    }

    @Override
    public PerspectiveCamera getCamera() {
        return current.getCamera();
    }

    @Override
    public void setCamera(PerspectiveCamera perspectiveCamera) {
        current.setCamera(perspectiveCamera);
    }

    @Override
    public float getFovFactor() {
        return current.getFovFactor();
    }

    @Override
    public Vector3b getPos() {
        return current.getPos();
    }

    @Override
    public void setPos(Vector3d pos) {
        current.setPos(pos);
    }

    @Override
    public void setPos(Vector3b pos) {
        current.setPos(pos);
    }

    @Override
    public Vector3b getPreviousPos() {
        return current.getPreviousPos();
    }

    @Override
    public void setPreviousPos(Vector3d prevpos) {
        current.setPreviousPos(prevpos);
    }

    @Override
    public void setPreviousPos(Vector3b prevpos) {
        current.setPreviousPos(prevpos);
    }

    @Override
    public Vector3b getInversePos() {
        return current.getInversePos();
    }

    @Override
    public Vector3d getVelocity() {
        return current.getVelocity();
    }

    @Override
    public Vector3d getDirection() {
        return current.getDirection();
    }

    @Override
    public void setDirection(Vector3d dir) {
        current.setDirection(dir);
    }

    @Override
    public Vector3d getUp() {
        return current.getUp();
    }

    public void swapBuffers() {
        current.swapBuffers();
    }

    @Override
    public void setGamepadInput(boolean state) {
        current.setGamepadInput(state);
    }

    @Override
    public void setPointerProjectionOnFocus(Vector3 point) {
        current.setPointerProjectionOnFocus(point);
    }

    public void backupCamera() {
        backupCamera = new BackupProjectionCamera(current.getCamera());
    }

    public void restoreCamera() {
        if (backupCamera != null) {
            backupCamera.restore(current.getCamera());
            backupCamera = null;
        }
    }

    /**
     * Update method.
     *
     * @param dt   Delta time in seconds.
     * @param time The time frame provider.
     */
    public void update(double dt, ITimeFrameProvider time) {
        // Update the previous position for this frame
        current.setPreviousPos(current.getPos());
        // Update the previous projView matrix
        current.setPreviousProjView(current.getProjView());

        // Update the camera
        current.update(dt, time);

        // Speed = dx/dt
        velocity.set(lastPos).sub(current.getPos());
        velocityNormalized.set(velocity).nor();
        speed = (velocity.len() * Constants.U_TO_KM) / (dt * Nature.S_TO_H);

        // High speed?
        if (speed > (Settings.settings.runtime.openXr ? 5e6 : 5e5)) {
            EventManager.publish(Event.CLEAR_OCTANT_QUEUE, this);
        }

        // Post event with camera motion parameters
        EventManager.publish(Event.CAMERA_MOTION_UPDATE, this, current.getPos(), speed, velocityNormalized, current.getCamera());

        // Update last pos and dir
        lastPos.set(current.getPos());

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();
        int width = Gdx.graphics.getWidth();

        int height = Gdx.graphics.getHeight();

        // This check is for Windows, which crashes when the window is minimized
        // as graphics.get[Width|Height]() returns 0.
        if (width > 0 && height > 0) {
            // Update Pointer and view Alpha/Delta
            updateRADEC(screenX, screenY, width / 2, height / 2);
        }
        // Update Pointer LAT/LON.
        boolean ok = updateFocusLatLon(screenX, screenY);

        // Surface mode.
        if (ok && current.hasFocus() && ((FocusView) current.getFocus()).isPlanet()) {
            current.setPointerProjectionOnFocus(intersection);
        }

        // Work out and broadcast the closest objects
        IFocus closestBody = getClosestBody();
        if (!closestBody.isEmpty() && closestBody.getOctant() != null && !closestBody.getOctant().observed) {
            ((FocusView) closestBody).clearEntity();
        }

        IFocus closestParticle = getClosestParticle();
        if (closestParticle != null && closestParticle.getOctant() != null && !closestParticle.getOctant().observed) {
            closestParticle = null;
        }

        if (!closestBody.isEmpty() || closestParticle != null) {
            if (closestBody.isEmpty()) {
                setClosest(closestParticle);
            } else if (closestParticle == null) {
                setClosest(closestBody);
            } else {
                setClosest(closestBody.getDistToCamera() < closestParticle.getClosestDistToCamera() ? closestBody : closestParticle);
            }
        }

        IFocus newClosest = getClosest();
        EventManager.publish(Event.CAMERA_CLOSEST_INFO, this, newClosest, getClosestBody(), getClosestParticle());

        if (newClosest != null && !newClosest.equals(previousClosest)) {
            EventManager.publish(Event.CAMERA_NEW_CLOSEST, this, newClosest);
            if (newClosest instanceof FocusView) {
                focusView.setEntity(((FocusView) newClosest).getEntity());
                previousClosest = focusView;
            } else if (newClosest instanceof Proximity.NearbyRecord) {
                previousClosest = newClosest;
            }
        }
    }

    private void updateRADEC(int pointerX, int pointerY, int viewX, int viewY) {
        ICamera camera = current;

        // Pointer
        vec.set(pointerX, pointerY, 0.5f);
        camera.getCamera().unproject(vec);
        try {
            inb.set(vec);
            Coordinates.cartesianToSpherical(inb, out);

            double pointerRA = out.x * Nature.TO_DEG;
            double pointerDEC = out.y * Nature.TO_DEG;

            // View
            vec.set(viewX, viewY, 0.5f);
            camera.getCamera().unproject(vec);
            inb.set(vec);
            Coordinates.cartesianToSpherical(inb, out);

            double viewRA = out.x * Nature.TO_DEG;
            double viewDEC = out.y * Nature.TO_DEG;

            EventManager.publish(Event.RA_DEC_UPDATED, this, pointerRA, pointerDEC, viewRA, viewDEC, pointerX, pointerY);
        } catch (NumberFormatException e) {
            // Something fishy with the pointer coordinates
        }

    }

    private boolean updateFocusLatLon(int screenX, int screenY) {
        if (isNatural()) {
            // Hover over planets gets us lat/lon.
            if (current.hasFocus() && ((FocusView) current.getFocus()).isPlanet()) {
                FocusView e = (FocusView) current.getFocus();
                double[] lonlat = new double[2];
                boolean ok = CameraUtils.getLonLat(e, e.getEntity(), getCurrent(), screenX, screenY, v0, v1, vec, intersection, in, out, localTransformInv, lonlat);

                if (ok)
                    EventManager.publish(Event.LON_LAT_UPDATED, this, lonlat[0], lonlat[1], screenX, screenY);

                return ok;
            }
        }
        return false;
    }

    /**
     * Runs on each camera after a mode change.
     */
    public void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode newMode, boolean centerFocus, boolean postEvent) {
        previousMode = this.mode;
        previousCam = this.current;
        this.mode = newMode;
        updateCurrentCamera();
        for (ICamera cam : cameras) {
            cam.updateMode(previousCam, previousMode, newMode, centerFocus, postEvent);
        }

        if (postEvent) {
            EventManager.publish(Event.FOV_CHANGE_NOTIFICATION, this, this.getCamera().fieldOfView, getFovFactor());
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case CAMERA_MODE_CMD -> {
                CameraMode newCameraMode = (CameraMode) data[0];
                boolean centerFocus = true;
                if (data.length > 1)
                    centerFocus = (Boolean) data[1];
                updateMode(current, this.mode, newCameraMode, centerFocus, true);
            }
            case FOV_CHANGE_NOTIFICATION -> updateAngleEdge(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            default -> {
            }
        }

    }

    @Override
    public Vector3d[] getDirections() {
        return current.getDirections();
    }

    @Override
    public int getNCameras() {
        return current.getNCameras();
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return current.getFrontCameras();
    }

    @Override
    public CameraMode getMode() {
        return mode;
    }

    @Override
    public void updateAngleEdge(int width, int height) {
        for (ICamera cam : cameras)
            cam.updateAngleEdge(width, height);
    }

    @Override
    public float getAngleEdge() {
        return current.getAngleEdge();
    }

    @Override
    public CameraManager getManager() {
        return this;
    }

    @Override
    public void render(int rw, int rh) {
        current.render(rw, rh);
    }

    @Override
    public ICamera getCurrent() {
        return current;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    @Override
    public boolean isFocus(Entity cb) {
        return current.isFocus(cb);
    }

    @Override
    public void checkClosestBody(IFocus focus) {
        current.checkClosestBody(focus);
    }

    @Override
    public void checkClosestBody(Entity entity) {
        current.checkClosestBody(entity);
    }

    @Override
    public IFocus getFocus() {
        return current.getFocus();
    }

    @Override
    public boolean hasFocus() {
        return current.hasFocus();
    }

    @Override
    public boolean isVisible(Entity cb) {
        return current.isVisible(cb);
    }

    @Override
    public boolean isVisible(double viewAngle, Vector3d pos, double distToCamera) {
        return current.isVisible(viewAngle, pos, distToCamera);
    }

    @Override
    public double getDistance() {
        return current.getDistance();
    }

    @Override
    public PerspectiveCamera getCameraStereoLeft() {
        return current.getCameraStereoLeft();
    }

    @Override
    public void setCameraStereoLeft(PerspectiveCamera cam) {
        current.setCameraStereoLeft(cam);
    }

    @Override
    public PerspectiveCamera getCameraStereoRight() {
        return current.getCameraStereoRight();
    }

    @Override
    public void setCameraStereoRight(PerspectiveCamera cam) {
        current.setCameraStereoRight(cam);
    }

    @Override
    public IFocus getClosestBody() {
        return current.getClosestBody();
    }

    @Override
    public IFocus getSecondClosestBody() {
        return current.getSecondClosestBody();
    }

    @Override
    public IFocus getCloseLightSource(int i) {
        return current.getCloseLightSource(i);
    }

    @Override
    public void resize(int width, int height) {
        for (ICamera cam : cameras)
            cam.resize(width, height);
    }

    @Override
    public Vector3d getShift() {
        return current.getShift();
    }

    @Override
    public void setShift(Vector3d shift) {
        current.setShift(shift);
    }

    @Override
    public Matrix4 getProjView() {
        return current.getProjView();
    }

    @Override
    public Matrix4 getPreviousProjView() {
        return current.getPreviousProjView();
    }

    @Override
    public void setPreviousProjView(Matrix4 mat) {
        current.setPreviousProjView(mat);
    }

    @Override
    public IFocus getClosestParticle() {
        return current.getClosestParticle();
    }

    @Override
    public void checkClosestParticle(IFocus particle) {
        current.checkClosestParticle(particle);
    }

    @Override
    public IFocus getClosest() {
        return current.getClosest();
    }

    @Override
    public void setClosest(IFocus focus) {
        current.setClosest(focus);
    }

    @Override
    public double speedScaling() {
        return current.speedScaling();
    }

    @Override
    public void updateFrustumPlanes() {
        for (ICamera cam : cameras)
            cam.updateFrustumPlanes();
    }

    @Override
    public double getNear() {
        return current.getNear();
    }

    @Override
    public double getFar() {
        return current.getFar();
    }

    /**
     * Stores the normalized rays representing the camera frustum in world space in a 4x4 matrix.  Each row is a vector.
     *
     * @param frustumCorners The matrix to fill
     * @return The same matrix
     */
    public Matrix4 getFrustumCornersWorld(Matrix4 frustumCorners) {
        PerspectiveCamera cam = this.getCamera();
        float camFov = cam.fieldOfView;
        float camAspect = cam.viewportHeight / cam.viewportWidth;

        float fovW = camFov * 0.5f;
        float fovH = camFov * camAspect * 0.5f;

        Vector3 dir = new Vector3(cam.direction);

        Vector3 aux = new Vector3();

        Vector3 topLeft = (new Vector3(dir)).rotate(cam.up, fovW);
        topLeft.rotate(aux.set(cam.up).crs(topLeft), fovH).nor();

        Vector3 topRight = (new Vector3(dir)).rotate(cam.up, -fovW);
        topRight.rotate(aux.set(cam.up).crs(topLeft), fovH).nor();

        Vector3 bottomRight = (new Vector3(dir)).rotate(cam.up, -fovW);
        bottomRight.rotate(aux.set(cam.up).crs(topLeft), -fovH).nor();

        Vector3 bottomLeft = (new Vector3(dir)).rotate(cam.up, fovW);
        bottomLeft.rotate(aux.set(cam.up).crs(topLeft), -fovH).nor();

        // Store in column-major order, the glsl way
        // Top left
        frustumCorners.val[Matrix4.M00] = topLeft.x;
        frustumCorners.val[Matrix4.M10] = topLeft.y;
        frustumCorners.val[Matrix4.M20] = topLeft.z;

        // Top right
        frustumCorners.val[Matrix4.M01] = topRight.x;
        frustumCorners.val[Matrix4.M11] = topRight.y;
        frustumCorners.val[Matrix4.M21] = topRight.z;

        // Bottom right
        frustumCorners.val[Matrix4.M02] = bottomRight.x;
        frustumCorners.val[Matrix4.M12] = bottomRight.y;
        frustumCorners.val[Matrix4.M22] = bottomRight.z;

        // Bottom left
        frustumCorners.val[Matrix4.M03] = bottomLeft.x;
        frustumCorners.val[Matrix4.M13] = bottomLeft.y;
        frustumCorners.val[Matrix4.M23] = bottomLeft.z;
        return frustumCorners;
    }

    /**
     * Convenience enum to describe the camera mode
     */
    public enum CameraMode {
        /**
         * Free navigation
         **/
        FREE_MODE,
        /**
         * FOCUS_MODE
         **/
        FOCUS_MODE,
        /**
         * GAME_MODE mode
         **/
        GAME_MODE,
        /**
         * SPACECRAFT_MODE
         **/
        SPACECRAFT_MODE;

        static TwoWayMap<String, CameraMode> equivalences;

        public static CameraMode getMode(int idx) {
            if (idx >= 0 && idx < CameraMode.values().length) {
                return CameraMode.values()[idx];
            } else {
                return null;
            }
        }

        public String getKey() {
            return "camera." + this;
        }

        public String toStringI18n() {
            return I18n.msg(getKey());
        }

        public boolean isSpacecraft() {
            return this.equals(CameraMode.SPACECRAFT_MODE);
        }

        public boolean isFocus() {
            return this.equals(CameraMode.FOCUS_MODE);
        }

        public boolean isFree() {
            return this.equals(CameraMode.FREE_MODE);
        }

        public boolean isGame() {
            return this.equals(CameraMode.GAME_MODE);
        }

        public boolean useFocus() {
            return isFocus();
        }

        public boolean useClosest() {
            return isFree() || isGame();
        }
    }

    public static class BackupProjectionCamera {
        float near, far, fov;
        Vector3 position, direction, up;
        float viewportWidth, viewportHeight;

        public BackupProjectionCamera(PerspectiveCamera cam) {
            this.near = cam.near;
            this.far = cam.far;
            this.fov = cam.fieldOfView;
            this.position = new Vector3(cam.position);
            this.direction = new Vector3(cam.direction);
            this.up = new Vector3(cam.up);
            this.viewportHeight = cam.viewportHeight;
            this.viewportWidth = cam.viewportWidth;
        }

        public void restore(PerspectiveCamera cam) {
            if (position != null && direction != null && up != null) {
                cam.near = near;
                cam.far = far;
                cam.fieldOfView = fov;
                cam.position.set(position);
                cam.direction.set(direction);
                cam.up.set(up);
                cam.viewportWidth = viewportWidth;
                cam.viewportHeight = viewportHeight;
                cam.update();
            }
        }
    }
}
