package gaia.cu9.ari.gaiaorbit.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import gaia.cu9.ari.gaiaorbit.data.StreamingOctreeLoader;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.Planet;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.TwoWayHashmap;
import gaia.cu9.ari.gaiaorbit.util.camera.CameraUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class CameraManager implements ICamera, IObserver {
    /**
     * Convenience enum to describe the camera mode
     * 
     * @author Toni Sagrista
     *
     */
    public enum CameraMode {
        /** Free navigation **/
        Free_Camera,
        /** Focus **/
        Focus,
        /** Relativistic camera **/
        //Relativistic,
        /** Gaia Scene **/
        Gaia_Scene,
        /** Spacecraft **/
        Spacecraft,
        /** FOV1 **/
        Gaia_FOV1,
        /** FOV2 **/
        Gaia_FOV2,
        /** Both fields of view **/
        Gaia_FOV1and2;

        static TwoWayHashmap<String, CameraMode> equivalences;

        static {
            String fc = "Free camera";
            String foc = "Focus object";
            //String rel = "Relativistic camera";
            String gs = "Gaia scene";
            String sc = "Spacecraft";
            String f1 = "Gaia FoV 1";
            String f2 = "Gaia FoV 2";
            String f12 = "Gaia FoV1 and FoV2";

            equivalences = new TwoWayHashmap<String, CameraMode>();
            equivalences.add(fc, Free_Camera);
            equivalences.add(foc, Focus);
            //equivalences.add(rel, Relativistic);
            equivalences.add(gs, Gaia_Scene);
            equivalences.add(sc, Spacecraft);
            equivalences.add(f1, Gaia_FOV1);
            equivalences.add(f2, Gaia_FOV2);
            equivalences.add(f12, Gaia_FOV1and2);

        }

        public static CameraMode getMode(int idx) {
            if (idx >= 0 && idx < CameraMode.values().length) {
                return CameraMode.values()[idx];
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return equivalences.getBackward(this);
        }

        public static CameraMode fromString(String str) {
            return equivalences.getForward(str);
        }

        public boolean isGaiaFov() {
            return this.equals(CameraMode.Gaia_FOV1) || this.equals(CameraMode.Gaia_FOV2) || this.equals(CameraMode.Gaia_FOV1and2);
        }

        public boolean isSpacecraft() {
            return this.equals(CameraMode.Spacecraft);
        }

        public boolean isFocus() {
            return this.equals(CameraMode.Focus);
        }

        public boolean isFree() {
            return this.equals(CameraMode.Free_Camera);
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
            switch (this) {
            case Gaia_FOV1:
                return 1;
            case Gaia_FOV2:
                return 2;
            case Gaia_FOV1and2:
                return 3;
            default:
                return 0;
            }
        }
    }

    public CameraMode mode;

    public ICamera current;

    public NaturalCamera naturalCamera;
    public FovCamera fovCamera;
    public SpacecraftCamera spacecraftCamera;
    public RelativisticCamera relativisticCamera;

    private ICamera[] cameras;

    /** Last position, for working out velocity **/
    private Vector3d lastPos, out, in;
    private Vector3 vec, v0, v1, isec;
    private Matrix4 localTransformInv;

    /** Current velocity in km/h **/
    protected double speed;
    /** Velocity vector **/
    protected Vector3d velocity, velocitynor;

    public CameraManager(AssetManager manager, CameraMode mode) {
        // Initialize
        // Initialize Cameras
        CameraMode previousMode = this.mode;
        naturalCamera = new NaturalCamera(manager, this);
        fovCamera = new FovCamera(manager, this);
        spacecraftCamera = new SpacecraftCamera(this);
        relativisticCamera = new RelativisticCamera(manager, this);

        cameras = new ICamera[] { naturalCamera, fovCamera, spacecraftCamera };

        this.mode = mode;
        lastPos = new Vector3d();
        in = new Vector3d();
        out = new Vector3d();
        vec = new Vector3();
        v0 = new Vector3();
        v1 = new Vector3();
        isec = new Vector3();
        velocity = new Vector3d();
        velocitynor = new Vector3d();
        localTransformInv = new Matrix4();

        updateCurrentCamera(previousMode);

        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD, Events.FOV_CHANGE_NOTIFICATION);
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

    public void updateCurrentCamera(CameraMode previousMode) {
        AbstractCamera aux = null;
        // Update
        switch (mode) {
        case Free_Camera:
        case Focus:
        case Gaia_Scene:
            aux = backupCam(current);
            current = naturalCamera;
            restoreCam(naturalCamera, aux);
            break;
        // case Relativistic:
        //     aux = backupCam(current);
        //     current = relativisticCamera;
        //     restoreCam(relativisticCamera, aux);
        case Spacecraft:
            aux = backupCam(current);
            current = spacecraftCamera;
            restoreCam(spacecraftCamera, aux);
            break;
        case Gaia_FOV1:
        case Gaia_FOV2:
        case Gaia_FOV1and2:
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
    public Vector3d getPos() {
        return current.getPos();
    }

    @Override
    public void setPos(Vector3d pos) {
        current.setPos(pos);
    }

    @Override
    public Vector3d getInversePos() {
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

    /**
     * Update method.
     * 
     * @param dt
     *            Delta time in seconds.
     * @param time
     *            The time frame provider.
     */
    public void update(double dt, ITimeFrameProvider time) {
        current.update(dt, time);
        if (current != fovCamera && GlobalConf.scene.COMPUTE_GAIA_SCAN) {
            fovCamera.updateDirections(time);
        }

        // Speed = dx/dt
        velocity.set(lastPos).sub(current.getPos());
        velocitynor.set(velocity).nor();
        speed = (velocity.len() * Constants.U_TO_KM) / (dt * Nature.S_TO_H);

        // High speed?
        if (speed > 5e3) {
            //System.out.println(panSpeed + " deg/s, " + speed + " km/h");
            StreamingOctreeLoader.clearQueue();
        }

        // Post event with camera motion parameters
        EventManager.instance.post(Events.CAMERA_MOTION_UPDATED, current.getPos(), speed, velocitynor, current.getCamera());

        // Update last pos and dir
        lastPos.set(current.getPos());

        int screenX = Gdx.input.getX();
        int screenY = Gdx.input.getY();

        // Update Pointer and view Alpha/Delta
        updateRADEC(screenX, screenY, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        // Update Pointer LAT/LON
        updateFocusLatLon(screenX, screenY);
    }

    private void updateRADEC(int pointerX, int pointerY, int viewX, int viewY) {
        ICamera camera = current;

        // Pointer
        vec.set(pointerX, pointerY, 0.5f);
        camera.getCamera().unproject(vec);
        in.set(vec);
        Coordinates.cartesianToSpherical(in, out);

        double pointerRA = out.x * Nature.TO_DEG;
        double pointerDEC = out.y * Nature.TO_DEG;

        // View
        vec.set(viewX, viewY, 0.5f);
        camera.getCamera().unproject(vec);
        in.set(vec);
        Coordinates.cartesianToSpherical(in, out);

        double viewRA = out.x * Nature.TO_DEG;
        double viewDEC = out.y * Nature.TO_DEG;

        EventManager.instance.post(Events.RA_DEC_UPDATED, pointerRA, pointerDEC, viewRA, viewDEC, pointerX, pointerY);

    }

    private void updateFocusLatLon(int screenX, int screenY) {
        if (isNatural()) {
            // Hover over planets gets us lat/lon
            if (current.getFocus() != null && current.getFocus() instanceof Planet) {
                Planet p = (Planet) current.getFocus();
                double[] lonlat = new double[2];
                boolean ok = CameraUtils.getLonLat(p, getCurrent(), screenX, screenY, v0, v1, vec, isec, in, out, localTransformInv, lonlat);

                if (ok)
                    EventManager.instance.post(Events.LON_LAT_UPDATED, lonlat[0], lonlat[1], screenX, screenY);

            }

        }
    }

    int pxRendererBackup = -1;

    /**
     * Sets the new camera mode and updates the frustum
     * 
     * @param mode
     */
    public void updateMode(CameraMode mode, boolean centerFocus, boolean postEvent) {
        CameraMode previousMode = this.mode;
        this.mode = mode;
        updateCurrentCamera(previousMode);
        for (ICamera cam : cameras) {
            cam.updateMode(mode, centerFocus, postEvent);
        }

        if (postEvent) {
            EventManager.instance.post(Events.FOV_CHANGE_NOTIFICATION, this.getCamera().fieldOfView, getFovFactor());
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CAMERA_MODE_CMD:
            CameraMode cm = (CameraMode) data[0];
            boolean centerFocus = true;
            if(data.length > 1)
                centerFocus = (Boolean)data[1];
            updateMode(cm, centerFocus, true);
            break;
        case FOV_CHANGE_NOTIFICATION:
            updateAngleEdge(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            break;
        default:
            break;
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
    public void checkClosest(IFocus focus) {
        current.checkClosest(focus);
    }

    @Override
    public IFocus getFocus() {
        return current.getFocus();
    }

    public void computeGaiaScan(ITimeFrameProvider time, CelestialBody cb) {
        current.computeGaiaScan(time, cb);
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb) {
        return current.isVisible(time, cb);
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, double viewAngle, Vector3d pos, double distToCamera) {
        return current.isVisible(time, viewAngle, pos, distToCamera);
    }

    @Override
    public double getDistance() {
        return current.getDistance();
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        current.setCamera(cam);
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
    public IFocus getClosest() {
        return current.getClosest();
    }

    @Override
    public IFocus getClosest2() {
        return current.getClosest2();
    }

    @Override
    public void resize(int width, int height) {
        for (ICamera cam : cameras)
            cam.resize(width, height);
    }

    @Override
    public IStarFocus getClosestStar() {
        return current.getClosestStar();
    }

    @Override
    public void setClosestStar(IStarFocus star) {
        current.setClosestStar(star);
    }

    @Override
    public double getTranslateUnits() {
        return current.getTranslateUnits();
    }

}
