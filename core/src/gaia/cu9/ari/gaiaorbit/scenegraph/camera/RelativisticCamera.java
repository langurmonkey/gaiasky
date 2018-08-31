package gaia.cu9.ari.gaiaorbit.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;

import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public class RelativisticCamera extends AbstractCamera {

    public Vector3d direction, up;

    public RelativisticCamera(CameraManager parent) {
        super(parent);
        // TODO Auto-generated constructor stub
    }

    public RelativisticCamera(AssetManager assetManager, CameraManager parent) {
        super(parent);
        initialize(assetManager);

    }

    private void initialize(AssetManager manager) {
        camera = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;

        fovFactor = camera.fieldOfView / 40f;

        up = new Vector3d(1, 0, 0);
        direction = new Vector3d(0, 1, 0);
    }

    @Override
    public PerspectiveCamera getCamera() {
        return camera;
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        // TODO Auto-generated method stub

    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDirection(Vector3d dir) {
        // TODO Auto-generated method stub

    }

    @Override
    public Vector3d getDirection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vector3d getUp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vector3d[] getDirections() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getNCameras() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getTranslateUnits() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void update(double dt, ITimeFrameProvider time) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateMode(CameraMode mode, boolean postEvent) {
        // TODO Auto-generated method stub

    }

    @Override
    public CameraMode getMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IFocus getFocus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isFocus(IFocus cb) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void resize(int width, int height) {
        // TODO Auto-generated method stub

    }

}
