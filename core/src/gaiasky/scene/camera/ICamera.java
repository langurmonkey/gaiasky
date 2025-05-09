/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public interface ICamera {

    /**
     * Loading is finished, gather resources.
     *
     * @param manager The asset manager.
     */
    void doneLoading(AssetManager manager);

    /**
     * Returns the perspective camera.
     *
     * @return The perspective camera.
     */
    PerspectiveCamera getCamera();

    /**
     * Sets the active camera
     *
     * @param perspectiveCamera The perspective camera.
     */
    void setCamera(PerspectiveCamera perspectiveCamera);

    PerspectiveCamera getCameraStereoLeft();

    void setCameraStereoLeft(PerspectiveCamera cam);

    PerspectiveCamera getCameraStereoRight();

    void setCameraStereoRight(PerspectiveCamera cam);

    PerspectiveCamera[] getFrontCameras();

    ICamera getCurrent();

    float getFovFactor();

    Vector3Q getPos();

    void setPos(Vector3d pos);

    void setPos(Vector3Q pos);

    Vector3Q getPreviousPos();

    void setPreviousPos(Vector3d pos);

    void setPreviousPos(Vector3Q pos);

    Vector3Q getDPos();

    void setDPos(Vector3d dPos);

    void setDPos(Vector3Q dPos);

    Vector3Q getInversePos();

    Vector3d getDirection();

    void setDirection(Vector3d dir);

    Vector3d getVelocity();

    Vector3d getUp();

    Vector3d[] getDirections();

    int getNCameras();

    double speedScaling();

    Vector3d getShift();

    void setShift(Vector3d shift);

    Matrix4 getProjView();

    Matrix4 getPreviousProjView();

    void setPreviousProjView(Matrix4 mat);

    /**
     * Updates the camera.
     *
     * @param dt   The time since the las frame in seconds.
     * @param time The frame time provider (simulation time).
     */
    void update(double dt, ITimeFrameProvider time);

    void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode newMode, boolean centerFocus);

    CameraMode getMode();

    void updateAngleEdge(int width, int height);

    /**
     * Gets the angle of the edge of the screen, diagonally. It assumes the
     * vertical angle is the field of view and corrects the horizontal using the
     * aspect ratio. It depends on the viewport size and the field of view
     * itself.
     *
     * @return The angle in radians.
     */
    float getAngleEdge();

    CameraManager getManager();

    void render(int rw, int rh);

    /**
     * Gets the current velocity of the camera in km/h.
     *
     * @return The velocity in km/h.
     */
    double getSpeed();

    /**
     * Gets the distance from the camera to the centre of our reference frame
     * (Sun)
     *
     * @return The distance
     */
    double getDistance();

    /**
     * Returns the focus if any.
     *
     * @return The focus object if it is in focus mode. Null otherwise.
     */
    IFocus getFocus();

    /**
     * Checks whether the current camera has a focus set.
     *
     * @return True if the camera has a focus.
     */
    boolean hasFocus();

    /**
     * Checks if the given entity is the current focus.
     *
     * @param entity The entity.
     *
     * @return Whether the entity is focus.
     */
    boolean isFocus(Entity entity);

    /**
     * Called after updating the body's distance to the cam, it updates the
     * closest body in the camera to figure out the camera near
     *
     * @param focus The body to check
     */
    void checkClosestBody(IFocus focus);

    /**
     * Called after updating the body's distance to the cam, it updates the
     * closest body in the camera to figure out the camera near
     *
     * @param entity The body to check.
     */
    void checkClosestBody(Entity entity);

    IFocus getClosestBody();

    IFocus getSecondClosestBody();

    boolean isVisible(Entity cb);

    boolean isVisible(double viewAngle, Vector3d pos, double distToCamera);

    void resize(int width, int height);

    /**
     * Gets the current closest particle to this camera
     *
     * @return The closest particle
     */
    IFocus getClosestParticle();

    /**
     * Gets the current i-close light source to this camera
     *
     * @return The i close light source (star?)
     */
    IFocus getCloseLightSource(int i);

    /**
     * Sets the current closest particle to this camera. This will be only set if
     * the given particle is closer than the current.
     *
     * @param particle The candidate particle
     */
    void checkClosestParticle(IFocus particle);

    /**
     * Returns the current closest object
     */
    IFocus getClosest();

    /**
     * Sets the closest of all
     *
     * @param focus The new closest object
     */
    void setClosest(IFocus focus);

    void updateFrustumPlanes();

    double getNear();

    double getFar();

    void swapBuffers();

    /**
     * Main input mode is a gamepad.
     **/
    void setGamepadInput(boolean state);

    /**
     * Sets the current pointer coordinates, as projected on the current focus object. This only applies when the
     * camera is in focus mode, and the focus is a planet.
     *
     * @param point The Cartesian coordinates of the pointer on the sphere representing the focus object.
     */
    void setPointerProjectionOnFocus(Vector3 point);

    double getSpeedScaling();

    double getSpeedScalingCapped();

}