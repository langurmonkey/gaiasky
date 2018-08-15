package gaia.cu9.ari.gaiaorbit.scenegraph.camera;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.Gaia;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.gaia.GaiaAttitudeServer;
import gaia.cu9.ari.gaiaorbit.util.gaia.Satellite;
import gaia.cu9.ari.gaiaorbit.util.math.Frustumd;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Quaterniond;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.GlobalClock;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * The field of view cameras.
 * 
 * @author Toni Sagrista
 *
 */
public class FovCamera extends AbstractCamera implements IObserver {
    private static final float FOV_CORR = 0.2f;
    private static final float FOV = (float) Satellite.FOV_AC + FOV_CORR;
    private static final float BAM_2 = (float) Satellite.BASICANGLE_DEGREE / 2f;
    private static final double GAIA_ASPECT_RATIO = (Satellite.FOV_AL + FOV_CORR) / FOV;

    /**
     * time that has to pass with the current scan rate so that we scan to the
     * edge of the current field of view.
     **/
    public long MAX_OVERLAP_TIME = 0l;
    public float MAX_OVERLAP_ANGLE = 0;

    private PerspectiveCamera camera2;
    private Frustumd frustum2;

    public Gaia gaia;

    Vector3d dirMiddle, up;
    public Vector3d[] directions;
    public List<Vector3d[]> interpolatedDirections;
    private Matrix4d trf;

    public long currentTime, lastTime;

    // Direction index for the render stage
    public int dirindex;

    private Vector3d dir1, dir2;
    private Matrix4d matrix;

    Viewport viewport, viewport2;

    Stage[] fpstages;
    Drawable fp, fp_fov1, fp_fov2;

    public FovCamera(AssetManager assetManager, CameraManager parent) {
        super(parent);
        initialize(assetManager);
        directions = new Vector3d[] { new Vector3d(), new Vector3d() };
        interpolatedDirections = new ArrayList<Vector3d[]>();
        dirMiddle = new Vector3d();
        up = new Vector3d();

        currentTime = 0l;
        lastTime = 0l;
        dir1 = new Vector3d();
        dir2 = new Vector3d();
        matrix = new Matrix4d();
    }

    public void initialize(AssetManager assetManager) {
        camera = new PerspectiveCamera(FOV, (float) (Gdx.graphics.getHeight() * GAIA_ASPECT_RATIO), Gdx.graphics.getHeight());
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;

        camera2 = new PerspectiveCamera(FOV, (float) (Gdx.graphics.getHeight() * GAIA_ASPECT_RATIO), Gdx.graphics.getHeight());
        camera2.near = (float) CAM_NEAR;
        camera2.far = (float) CAM_FAR;

        frustum2 = new Frustumd();

        fovFactor = FOV / 5f;

        /**
         * Fit viewport ensures a fixed aspect ratio. We set the camera field of
         * view equal to the satelltie's AC FOV and calculate the satellite
         * aspect ratio as FOV_AL/FOV_AC. With it we set the width of the
         * viewport to ensure we have the same vision as Gaia.
         */
        viewport = new FitViewport((float) (Gdx.graphics.getHeight() * GAIA_ASPECT_RATIO), Gdx.graphics.getHeight(), camera);
        viewport2 = new FitViewport((float) (Gdx.graphics.getHeight() * GAIA_ASPECT_RATIO), Gdx.graphics.getHeight(), camera2);

        /** Prepare stage with FP image **/
        fp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("img/gaia-focalplane.png"))));
        fp_fov1 = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("img/gaia-focalplane-fov1.png"))));
        fp_fov2 = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("img/gaia-focalplane-fov2.png"))));

        fpstages = new Stage[3];

        Stage fov12 = new Stage(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), GlobalResources.spriteBatch);
        Image i = new Image(fp);
        i.setFillParent(true);
        i.setAlign(Align.center);
        i.setColor(0.3f, 0.8f, 0.3f, .9f);
        fov12.addActor(i);

        Stage fov1 = new Stage(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), GlobalResources.spriteBatch);
        i = new Image(fp_fov1);
        i.setFillParent(true);
        i.setAlign(Align.center);
        i.setColor(0.3f, 0.8f, 0.3f, .9f);
        fov1.addActor(i);

        Stage fov2 = new Stage(new ScalingViewport(Scaling.stretch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), new OrthographicCamera()), GlobalResources.spriteBatch);
        i = new Image(fp_fov2);
        i.setFillParent(true);
        i.setAlign(Align.center);
        i.setColor(0.3f, 0.8f, 0.3f, .9f);
        fov2.addActor(i);

        fpstages[0] = fov1;
        fpstages[1] = fov2;
        fpstages[2] = fov12;

        EventManager.instance.subscribe(this, Events.GAIA_LOADED, Events.COMPUTE_GAIA_SCAN_CMD);
    }

    public void update(double dt, ITimeFrameProvider time) {
        distance = pos.len();

        up.set(0, 1, 0);

        /** POSITION **/
        AbstractPositionEntity fccopy = gaia.getLineCopy();
        fccopy.getRoot().translation.set(0f, 0f, 0f);
        fccopy.getRoot().update(time, null, this);

        this.pos.set(fccopy.translation);
        this.posinv.set(this.pos).scl(-1);

        /** ORIENTATION - directions and up **/
        updateDirections(time);
        trf = matrix;
        up.mul(trf).nor();

        // Update cameras
        updateCamera(directions[0], up, camera);

        updateCamera(directions[1], up, camera2);

        // Dir middle
        dirMiddle.set(0, 0, 1).mul(trf);

        // Return to pool
        SceneGraphNode ape = fccopy;
        do {
            ape.returnToPool();
            ape = ape.parent;
        } while (ape != null);

    }

    /**
     * Updates both FOVs' directions applying the right transformation.
     * 
     * @param time
     */
    public void updateDirections(ITimeFrameProvider time) {
        lastTime = currentTime;
        currentTime = time.getTime().toEpochMilli();
        trf = matrix;
        trf.idt();
        Quaterniond quat = GaiaAttitudeServer.instance.getAttitude(new Date(time.getTime().toEpochMilli())).getQuaternion();
        trf.rotate(quat).rotate(0, 0, 1, 180);
        directions[0].set(0, 0, 1).rotate(BAM_2, 0, 1, 0).mul(trf).nor();
        directions[1].set(0, 0, 1).rotate(-BAM_2, 0, 1, 0).mul(trf).nor();

        /** WORK OUT INTERPOLATED DIRECTIONS IN THE CASE OF FAST SCANNING **/
        interpolatedDirections.clear();
        if (GlobalConf.scene.COMPUTE_GAIA_SCAN) {
            if (lastTime != 0 && currentTime - lastTime > MAX_OVERLAP_TIME) {
                if (((GlobalClock) time).fps < 0) {
                    ((GlobalClock) time).fps = 10;
                    Logger.info(this.getClass().getSimpleName(), I18n.bundle.get("notif.timeprovider.fixed"));
                }
                for (long t = lastTime + MAX_OVERLAP_TIME; t < currentTime; t += MAX_OVERLAP_TIME) {
                    interpolatedDirections.add(getDirections(new Date(t)));
                }
            } else {
                if (((GlobalClock) time).fps > 0) {
                    ((GlobalClock) time).fps = -1;
                    Logger.info(this.getClass().getSimpleName(), I18n.bundle.get("notif.timeprovider.real"));
                }
            }
        }
    }

    public Vector3d[] getDirections(Date d) {
        trf = matrix;
        trf.idt();
        Quaterniond quat = GaiaAttitudeServer.instance.getAttitude(d).getQuaternion();
        trf.rotate(quat).rotate(0, 0, 1, 180);
        dir1.set(0, 0, 1).rotate(BAM_2, 0, 1, 0).mul(trf).nor();
        dir2.set(0, 0, 1).rotate(-BAM_2, 0, 1, 0).mul(trf).nor();
        return new Vector3d[] { dir1.cpy(), dir2.cpy() };
    }

    /**
     * Updates the given camera using the given direction and up vectors. Sets
     * the position to zero.
     * 
     * @param dir
     * @param up
     * @param cam
     */
    private void updateCamera(Vector3d dir, Vector3d up, PerspectiveCamera cam) {
        cam.position.set(0f, 0f, 0f);
        cam.direction.set(dir.valuesf());
        cam.up.set(up.valuesf());
        cam.update();
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        switch (parent.mode) {
        case Gaia_FOV1:
        default:
            return new PerspectiveCamera[] { camera };
        case Gaia_FOV2:

            return new PerspectiveCamera[] { camera2 };
        case Gaia_FOV1and2:
            return new PerspectiveCamera[] { camera, camera2 };
        }
    }

    @Override
    public PerspectiveCamera getCamera() {
        switch (parent.mode) {
        case Gaia_FOV1:
            return camera;
        case Gaia_FOV2:
            return camera2;
        default:
            return camera;
        }
    }

    @Override
    public float getFovFactor() {
        return this.fovFactor;
    }

    @Override
    public Vector3d getDirection() {
        int idx = parent.mode.ordinal() - CameraMode.Gaia_FOV1.ordinal();
        idx = Math.min(idx, 1);
        return directions[idx];
    }

    @Override
    public void setDirection(Vector3d dir) {
        int idx = parent.mode.ordinal() - CameraMode.Gaia_FOV1.ordinal();
        idx = Math.min(idx, 1);
        directions[idx].set(dir);
    }

    @Override
    public Vector3d getUp() {
        return up;
    }

    @Override
    public final Vector3d[] getDirections() {
        return directions;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case GAIA_LOADED:
            this.gaia = (Gaia) data[0];
            break;
        case COMPUTE_GAIA_SCAN_CMD:
            lastTime = 0;
            currentTime = 0;
            break;
        default:
            break;
        }

    }

    @Override
    public void updateMode(CameraMode mode, boolean postEvent) {
    }

    @Override
    public int getNCameras() {
        switch (parent.mode) {
        case Gaia_FOV1:
        case Gaia_FOV2:
            return 1;
        case Gaia_FOV1and2:
            return 2;
        default:
            return 0;
        }
    }

    @Override
    public CameraMode getMode() {
        return parent.mode;
    }

    /**
     * We have fixed field of view angles and thus fixed aspect ratio.
     */
    public void updateAngleEdge(int width, int height) {
        angleEdgeRad = (float) (Satellite.FOV_AL * Math.PI / 180);
        // Update max overlap time
        MAX_OVERLAP_TIME = (long) (angleEdgeRad / (Satellite.SCANRATE * (Math.PI / (3600 * 180)))) * 1000;
        MAX_OVERLAP_ANGLE = angleEdgeRad;
    }

    @Override
    public void render(int rw, int rh) {
        // Renders the focal plane CCDs
        fpstages[parent.mode.ordinal() - CameraMode.Gaia_FOV1.ordinal()].draw();
    }

    @Override
    public double getSpeed() {
        return parent.getSpeed();
    }

    @Override
    public boolean isFocus(IFocus cb) {
        return false;
    }

    @Override
    public IFocus getFocus() {
        return null;
    }

    public void computeGaiaScan(ITimeFrameProvider time, CelestialBody cb) {
        boolean visible = computeVisibleFovs(cb, this);
        cb.updateTransitNumber(visible && time.getDt() != 0, time, this);
    }

    @Override
    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb) {
        switch (parent.mode) {
        case Gaia_FOV1:
        case Gaia_FOV2:
            return super.isVisible(time, cb);
        case Gaia_FOV1and2:
            return computeVisibleFovs(cb, this);
        default:
            return false;
        }
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        // Nothing to do
    }

    @Override
    public void resize(int width, int height) {
        for (Stage stage : fpstages)
            stage.getViewport().update(width, height, true);

    }

    public Frustumd getFrustum2() {
        return frustum2;
    }

    @Override
    public double getTranslateUnits() {
        return 0;
    }

}
