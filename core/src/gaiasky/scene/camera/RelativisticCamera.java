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
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.util.Settings;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class RelativisticCamera extends AbstractCamera {

    public Vector3d direction, up;

    public RelativisticCamera(CameraManager parent) {
        super(parent);
    }

    public RelativisticCamera(AssetManager assetManager, CameraManager parent) {
        super(parent);
        initialize(assetManager);

    }

    private void initialize(AssetManager manager) {
        camera = new PerspectiveCamera(Settings.settings.scene.camera.fov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;

        fovFactor = camera.fieldOfView / 40f;

        up = new Vector3d(1, 0, 0);
        direction = new Vector3d(0, 1, 0);
    }

    @Override
    public void doneLoading(AssetManager manager) {
    }

    @Override
    public PerspectiveCamera getCamera() {
        return camera;
    }

    @Override
    public void setCamera(PerspectiveCamera perspectiveCamera) {
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return null;
    }

    @Override
    public Vector3d getDirection() {
        return null;
    }

    @Override
    public void setDirection(Vector3d dir) {
    }

    @Override
    public Vector3d getUp() {
        return null;
    }

    @Override
    public Vector3d[] getDirections() {
        return null;
    }

    @Override
    public int getNCameras() {
        return 0;
    }

    @Override
    public double speedScaling() {
        return 0;
    }

    @Override
    public void update(double dt, ITimeFrameProvider time) {
    }

    @Override
    public void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode newMode, boolean centerFocus) {
    }

    @Override
    public CameraMode getMode() {
        return null;
    }

    @Override
    public double getSpeed() {
        return 0;
    }

    @Override
    public IFocus getFocus() {
        return null;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public boolean isFocus(Entity entity) {
        return false;
    }

    @Override
    public void resize(int width, int height) {

    }

}
