/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.camera;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

public interface ICamera {

    /**
     * Returns the perspective camera.
     * 
     * @return The perspective camera.
     */
    public PerspectiveCamera getCamera();

    /**
     * Sets the active camera
     * 
     * @param cam
     */
    public void setCamera(PerspectiveCamera cam);

    public PerspectiveCamera getCameraStereoLeft();

    public PerspectiveCamera getCameraStereoRight();

    public void setCameraStereoLeft(PerspectiveCamera cam);

    public void setCameraStereoRight(PerspectiveCamera cam);

    public PerspectiveCamera[] getFrontCameras();

    public ICamera getCurrent();

    public float getFovFactor();

    public Vector3d getPos();

    public void setPos(Vector3d pos);

    public void setDirection(Vector3d dir);

    public Vector3d getInversePos();

    public Vector3d getDirection();

    public Vector3d getVelocity();

    public Vector3d getUp();

    public Vector3d[] getDirections();

    public int getNCameras();

    public double getTranslateUnits();

    /**
     * Updates the camera.
     * 
     * @param dt
     *            The time since the las frame in seconds.
     * @param time
     *            The frame time provider (simulation time).
     */
    public void update(double dt, ITimeFrameProvider time);

    public void updateMode(CameraMode mode, boolean centerFocus, boolean postEvent);

    public CameraMode getMode();

    public void updateAngleEdge(int width, int height);

    /**
     * Gets the angle of the edge of the screen, diagonally. It assumes the
     * vertical angle is the field of view and corrects the horizontal using the
     * aspect ratio. It depends on the viewport size and the field of view
     * itself.
     * 
     * @return The angle in radians.
     */
    public float getAngleEdge();

    public CameraManager getManager();

    public void render(int rw, int rh);

    /**
     * Gets the current velocity of the camera in km/h.
     * 
     * @return The velocity in km/h.
     */
    public double getSpeed();

    /**
     * Gets the distance from the camera to the centre of our reference frame
     * (Sun)
     * 
     * @return The distance
     */
    public double getDistance();

    /**
     * Returns the foucs if any
     * 
     * @return The foucs object if it is in focus mode. Null otherwise
     */
    public IFocus getFocus();

    /**
     * Checks if this body is the current focus
     * 
     * @param cb
     *            The body
     * @return Whether the body is focus
     */
    public boolean isFocus(IFocus cb);

    /**
     * Called after updating the body's distance to the cam, it updates the
     * closest body in the camera to figure out the camera near
     * 
     * @param  focus
     *            The body to check
     */
    public void checkClosest(IFocus focus);

    public IFocus getClosest();

    public IFocus getClosest2();

    public boolean isVisible(ITimeFrameProvider time, CelestialBody cb);

    public boolean isVisible(ITimeFrameProvider time, double viewAngle, Vector3d pos, double distToCamera);

    public void computeGaiaScan(ITimeFrameProvider time, CelestialBody cb);

    public void resize(int width, int height);

    /**
     * Gets the current closest star to this camera
     * 
     * @return The closest star
     */
    public IStarFocus getClosestStar();

    /**
     * Sets the current closest star to this camera. This will be only set if
     * the given star is closer than the current.
     * 
     * @param star
     *            The candidate star
     */
    public void setClosestStar(IStarFocus star);

}