/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.data.StreamingOctreeLoader;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.Planet;
import gaiasky.util.*;
import gaiasky.util.camera.CameraUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class CameraManager implements ICamera, IObserver {
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
        SPACECRAFT_MODE,
        /**
         * FOV1
         **/
        GAIA_FOV1_MODE,
        /**
         * FOV2
         **/
        GAIA_FOV2_MODE,
        /**
         * Both fields of view
         **/
        GAIA_FOVS_MODE;

        static TwoWayHashmap<String, CameraMode> equivalences;

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

        public boolean isGaiaFov() {
            return this.equals(CameraMode.GAIA_FOV1_MODE) || this.equals(CameraMode.GAIA_FOV2_MODE) || this.equals(CameraMode.GAIA_FOVS_MODE);
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

        /**
         * Returns the current FOV mode:
         * <ul>
         * <li>1 - FOV1</li>
         * <li>2 - FOV2</li>
         * <li>3 - FOV1&2</li>
         * <li>0 - No FOV mode</li>
         * </ul>
         *
         * @return The current FOV mode of the camera as an integer
         */
        public int getGaiaFovMode() {
            return switch (this) {
                case GAIA_FOV1_MODE -> 1;
                case GAIA_FOV2_MODE -> 2;
                case GAIA_FOVS_MODE -> 3;
                default -> 0;
            };
        }
    }

    public CameraMode mode;

    public ICamera current;

    public NaturalCamera naturalCamera;
    public FovCamera fovCamera;
    public SpacecraftCamera spacecraftCamera;
    public RelativisticCamera relativisticCamera;

    public IFocus previousClosest;

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
    private final Vector3 isec;
    private final Matrix4 localTransformInv;

    /**
     * Current velocity in km/h
     **/
    protected double speed;
    /**
     * Velocity vector
     **/
    protected Vector3d velocity, velocityNormalized;

    public CameraManager(AssetManager manager, CameraMode mode, boolean vr, GlobalResources globalResources) {
        // Initialize Cameras
        this.naturalCamera = new NaturalCamera(manager, this, vr, globalResources.getSpriteShader(), globalResources.getShapeShader());
        this.fovCamera = new FovCamera(manager, this, globalResources.getSpriteBatch());
        this.spacecraftCamera = new SpacecraftCamera(this);
        this.relativisticCamera = new RelativisticCamera(manager, this);

        this.cameras = new ICamera[]{naturalCamera, fovCamera, spacecraftCamera};

        this.mode = mode;
        this.lastPos = new Vector3d();
        this.in = new Vector3d();
        this.inb = new Vector3b();
        this.out = new Vector3d();
        this.vec = new Vector3();
        this.v0 = new Vector3();
        this.v1 = new Vector3();
        this.isec = new Vector3();
        this.velocity = new Vector3d();
        this.velocityNormalized = new Vector3d();
        this.localTransformInv = new Matrix4();

        updateCurrentCamera();

        EventManager.instance.subscribe(this, Event.CAMERA_MODE_CMD, Event.FOV_CHANGE_NOTIFICATION);
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
                EventManager.publish(Event.CAMERA_CINEMATIC_CMD, this, false );
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
            case GAIA_FOV1_MODE:
            case GAIA_FOV2_MODE:
            case GAIA_FOVS_MODE:
                current = fovCamera;
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

    public void swapBuffers(){
        current.swapBuffers();
    }

    @Override
    public void setGamepadInput(boolean state) {
        current.setGamepadInput(state);
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
        if (speed > (Settings.settings.runtime.openVr ? 5e6 : 5e5)) {
            StreamingOctreeLoader.clearQueue();
        }

        // Post event with camera motion parameters
        EventManager.publish(Event.CAMERA_MOTION_UPDATE, this, current.getPos(), speed, velocityNormalized, current.getCamera());

        // Update last pos and dir
        lastPos.set(current.getPos());

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();
        int width = Gdx.graphics.getWidth();;
        int height = Gdx.graphics.getHeight();

        // This check is for windows, which crashes when the window is minimized
        // as graphics.get[Width|Height]() returns 0.
        if(width > 0 && height > 0) {
            // Update Pointer and view Alpha/Delta
            updateRADEC(screenX, screenY, width / 2, height / 2);
        }
        // Update Pointer LAT/LON
        updateFocusLatLon(screenX, screenY);

        // Work out and broadcast the closest objects
        IFocus closestBody = getClosestBody();
        if (closestBody != null && closestBody.getOctant() != null && !closestBody.getOctant().observed)
            closestBody = null;

        IFocus closestParticle = getClosestParticle();
        if (closestParticle != null && closestParticle.getOctant() != null && !closestParticle.getOctant().observed)
            closestParticle = null;

        if (closestBody != null || closestParticle != null) {
            if (closestBody == null)
                setClosest(closestParticle);
            else if (closestParticle == null)
                setClosest(closestBody);
            else {
                setClosest(closestBody.getDistToCamera() < closestParticle.getClosestDistToCamera() ? closestBody : closestParticle);
            }
        }
        IFocus newClosest = getClosest();
        EventManager.publish(Event.CAMERA_CLOSEST_INFO, this, newClosest, getClosestBody(), getClosestParticle());

        if(newClosest != previousClosest){
            EventManager.publish(Event.CAMERA_NEW_CLOSEST, this, newClosest);
            previousClosest = newClosest;
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
        }catch(NumberFormatException e) {
            // Something fishy with the pointer coordinates
        }

    }

    private void updateFocusLatLon(int screenX, int screenY) {
        if (isNatural()) {
            // Hover over planets gets us lat/lon
            if (current.getFocus() != null && current.getFocus() instanceof Planet) {
                Planet p = (Planet) current.getFocus();
                double[] lonlat = new double[2];
                boolean ok = CameraUtils.getLonLat(p, getCurrent(), screenX, screenY, v0, v1, vec, isec, in, out, localTransformInv, lonlat);

                if (ok)
                    EventManager.publish(Event.LON_LAT_UPDATED, this, lonlat[0], lonlat[1], screenX, screenY);

            }

        }
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
    public boolean isFocus(IFocus cb) {
        return current.isFocus(cb);
    }

    @Override
    public void checkClosestBody(IFocus focus) {
        current.checkClosestBody(focus);
    }

    @Override
    public IFocus getFocus() {
        return current.getFocus();
    }

    @Override
    public boolean isVisible(CelestialBody cb) {
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
    public void setCamera(PerspectiveCamera perspectiveCamera) {
        current.setCamera(perspectiveCamera);
    }

    @Override
    public void setCameraStereoLeft(PerspectiveCamera cam) {
        current.setCameraStereoLeft(cam);
    }

    @Override
    public void setCameraStereoRight(PerspectiveCamera cam) {
        current.setCameraStereoRight(cam);
    }

    @Override
    public PerspectiveCamera getCameraStereoLeft() {
        return current.getCameraStereoLeft();
    }

    @Override
    public PerspectiveCamera getCameraStereoRight() {
        return current.getCameraStereoRight();
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
    public IFocus getCloseLightSource(int i){
        return current.getCloseLightSource(i);
    }

    @Override
    public void resize(int width, int height) {
        for (ICamera cam : cameras)
            cam.resize(width, height);
    }

    @Override
    public void setShift(Vector3d shift) {
        current.setShift(shift);
    }

    @Override
    public Vector3d getShift() {
        return current.getShift();
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
     * Stores the normalized rays representing the camera frustum in eye space in a 4x4 matrix.  Each row is a vector.
     *
     * @param cam The perspective camera
     * @param frustumCorners The matrix to fill
     * @return The same matrix
     */
    public static Matrix4 getFrustumCornersEye(PerspectiveCamera cam, Matrix4 frustumCorners) {
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
        return frustumCorners;
    }
}
