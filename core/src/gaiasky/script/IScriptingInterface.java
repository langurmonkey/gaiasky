/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.group.DatasetOptions;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IVisibilitySwitch;
import gaiasky.scene.component.AttitudeComponent;
import gaiasky.scene.component.RigidRotation;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Constants;
import gaiasky.util.coord.IPythonCoordinatesProvider;

import java.util.List;

/**
 * <p>Definition of the public API exposed to scripts via Py4j or the REST server.</p>
 * <p>For more information on Gaia Sky scripting, see
 * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Scripting.html">this page</a>.</p>
 */
@SuppressWarnings("unused")
public sealed interface IScriptingInterface permits EventScriptingInterface {

    /**
     * Gets the location of the assets folder.
     */
    String getAssetsLocation();

    /**
     * Pre-loads the given images as textures for later use. They will be cached
     * for the subsequent uses.
     *
     * @param paths The texture paths.
     */
    void preloadTextures(String[] paths);

    /**
     * Pre-loads the given image as a texture for later use. The texture will
     * be cached for later use.
     *
     * @param path The path of the image file to preload.
     */
    void preloadTexture(String path);

    /**
     * Sets the current time frame to <b>real time</b>. All the commands
     * executed after this command becomes active will be in the <b>real
     * time</b> frame (clock ticks).
     */
    void activateRealTimeFrame();

    /**
     * Sets the current time frame to <b>simulation time</b>. All the commands
     * executed after this command becomes active will be in the <b>simulation
     * time</b> frame (simulation clock in the app).
     */
    void activateSimulationTimeFrame();

    /**
     * Displays a popup notification on the screen for the default duration.
     *
     * @param message The message text.
     */
    void displayPopupNotification(String message);

    /**
     * Displays a popup notification on the screen for the given duration.
     *
     * @param message  The message text.
     * @param duration The duration in seconds until the notification is removed.
     */
    void displayPopupNotification(String message,
                                  float duration);

    /**
     * Sets a headline message that will appear in a big font in the screen.
     *
     * @param headline The headline text.
     */
    void setHeadlineMessage(String headline);

    /**
     * Sets a subhead message that will appear in a small font below the
     * headline.
     *
     * @param subhead The subhead text.
     */
    void setSubheadMessage(String subhead);

    /**
     * Clears the headline message.
     */
    void clearHeadlineMessage();

    /**
     * Clears the subhead message.
     */
    void clearSubheadMessage();

    /**
     * Clears both the subhead and the headline messages.
     */
    void clearAllMessages();

    /**
     * Adds a new one-line message in the screen with the given id and the given
     * coordinates. If an object already exists with the given id, it is
     * removed. However, if a message object already exists with the same id,
     * its properties are updated. <strong>The messages placed with this method
     * will not appear in the screenshots/frames in advanced mode. This is
     * intended for running interactively only.</strong>
     *
     * @param id       A unique identifier, used to identify this message when you
     *                 want to remove it.
     * @param message  The string message, to be displayed in one line. But explicit
     *                 newline breaks the line.
     * @param x        The x coordinate of the bottom-left corner, in [0,1] from
     *                 left to right. This is not resolution-dependant.
     * @param y        The y coordinate of the bottom-left corner, in [0,1] from
     *                 bottom to top. This is not resolution-dependant.
     * @param r        The red component of the color in [0,1].
     * @param g        The green component of the color in [0,1].
     * @param b        The blue component of the color in [0,1].
     * @param a        The alpha component of the color in [0,1].
     * @param fontSize The size of the font. The system will use the existing font
     *                 closest to the chosen size and scale it up or down to match
     *                 the desired size. Scaling can cause artifacts, so to ensure
     *                 the best font quality, stick to the existing sizes.
     */
    void displayMessageObject(int id,
                              String message,
                              float x,
                              float y,
                              float r,
                              float g,
                              float b,
                              float a,
                              float fontSize);

    /**
     * Same as {@link #displayImageObject(int, String, float, float, float, float, float, float)} but
     * using an array for the color
     * instead of giving each component separately.
     *
     * @param id       A unique identifier, used to identify this message when you
     *                 want to remove it.
     * @param message  The string message, to be displayed in one line. But explicit
     *                 newline breaks the line.
     * @param x        The x coordinate of the bottom-left corner, in [0,1] from
     *                 left to right. This is not resolution-dependant.
     * @param y        The y coordinate of the bottom-left corner, in [0,1] from
     *                 bottom to top. This is not resolution-dependant.
     * @param color    The color as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fontSize The size of the font. The system will use the existing font
     *                 closest to the chosen size and scale it up or down to match
     *                 the desired size. Scaling can cause artifacts, so to ensure
     *                 the best font quality, stick to the existing sizes.
     */
    void displayMessageObject(int id,
                              String message,
                              double x,
                              double y,
                              double[] color,
                              double fontSize);

    /**
     * Adds a new multi-line text in the screen with the given id, coordinates
     * and size. If an object already exists with the given id, it is removed.
     * However, if a text object already exists with the same id, its properties
     * are updated. <strong>The texts placed with this method will not appear in
     * the screenshots/frames in advanced mode. This is intended for running
     * interactively only.</strong>
     *
     * @param id        A unique identifier, used to identify this message when you
     *                  want to remove it.
     * @param text      The string message, to be displayed line-wrapped in the box
     *                  defined by maxWidth and maxHeight. Explicit newline still
     *                  breaks the line.
     * @param x         The x coordinate of the bottom-left corner, in [0,1] from
     *                  left to right. This is not resolution-dependant.
     * @param y         The y coordinate of the bottom-left corner, in [0,1] from
     *                  bottom to top. This is not resolution-dependant.
     * @param maxWidth  The maximum width in screen percentage [0,1]. Set to 0 to let
     *                  the system decide.
     * @param maxHeight The maximum height in screen percentage [0,1]. Set to 0 to
     *                  let the system decide.
     * @param r         The red component of the color in [0,1].
     * @param g         The green component of the color in [0,1].
     * @param b         The blue component of the color in [0,1].
     * @param a         The alpha component of the color in [0,1].
     * @param fontSize  The size of the font. The system will use the existing font
     *                  closest to the chosen size.
     */
    void displayTextObject(int id,
                           String text,
                           float x,
                           float y,
                           float maxWidth,
                           float maxHeight,
                           float r,
                           float g,
                           float b,
                           float a,
                           float fontSize);

    /**
     * Adds a new image object at the given coordinates. If an object already
     * exists with the given id, it is removed. However, if an image object
     * already exists with the same id, its properties are updated.<br>
     * <strong>The messages placed with this method will not appear in the
     * screenshots/frames in advanced mode. This is intended for running
     * interactively only.</strong>
     *
     * @param id   A unique identifier, used to identify this message when you
     *             want to remove it.
     * @param path The path to the image. It can either be an absolute path (not
     *             recommended) or a path relative to the Gaia Sky work directory.
     * @param x    The x coordinate of the bottom-left corner, in [0,1] from
     *             left to right. This is not resolution-dependant.
     * @param y    The y coordinate of the bottom-left corner, in [0,1] from
     *             bottom to top. This is not resolution-dependant.
     */
    void displayImageObject(int id,
                            String path,
                            float x,
                            float y);

    /**
     * Adds a new image object at the given coordinates. If an object already
     * exists with the given id, it is removed. However, if an image object
     * already exists with the same id, its properties are updated.<br>
     * <strong>Warning: This method will only work in the asynchronous mode. Run
     * the script with the "asynchronous" check box activated!</strong>
     *
     * @param id   A unique identifier, used to identify this message when you
     *             want to remove it.
     * @param path The path to the image. It can either be an absolute path (not
     *             recommended) or a path relative to the Gaia Sky work directory.
     * @param x    The x coordinate of the bottom-left corner, in [0,1] from
     *             left to right. This is not resolution-dependant.
     * @param y    The y coordinate of the bottom-left corner, in [0,1] from
     *             bottom to top. This is not resolution-dependant.
     * @param r    The red component of the color in [0,1].
     * @param g    The green component of the color in [0,1].
     * @param b    The blue component of the color in [0,1].
     * @param a    The alpha component of the color in [0,1].
     */
    void displayImageObject(int id,
                            final String path,
                            float x,
                            float y,
                            float r,
                            float g,
                            float b,
                            float a);

    /**
     * Same as {@link #displayImageObject(int, String, float, float, float, float, float, float)} but
     * using a
     * double array for the color instead of each component separately.
     *
     * @param id    A unique identifier, used to identify this message when you
     *              want to remove it.
     * @param path  The path to the image. It can either be an absolute path (not
     *              recommended) or a path relative to the Gaia Sky work directory.
     * @param x     The x coordinate of the bottom-left corner, in [0,1] from
     *              left to right. This is not resolution-dependant.
     * @param y     The y coordinate of the bottom-left corner, in [0,1] from
     *              bottom to top. This is not resolution-dependant.
     * @param color The color as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void displayImageObject(int id,
                            final String path,
                            double x,
                            double y,
                            double[] color);

    /**
     * Removes all objects.
     */
    void removeAllObjects();

    /**
     * Removes the item with the given id.
     *
     * @param id Integer with the integer id of the object to remove.
     */
    void removeObject(int id);

    /**
     * Removes the items with the given ids. They can either messages, images or
     * whatever else.
     *
     * @param ids Vector with the integer ids of the objects to remove
     */
    void removeObjects(int[] ids);

    /**
     * Disables all input events from mouse, keyboard, touchscreen, etc.
     */
    void disableInput();

    /**
     * Enables all input events.
     */
    void enableInput();

    /**
     * Enables or disables the cinematic camera mode.
     *
     * @param cinematic Whether to enable or disable the cinematic mode.
     */
    void setCinematicCamera(boolean cinematic);

    /**
     * Sets the camera in focus mode with the focus object that bears the given
     * <code>focusName</code>. It returns immediately, so it does not wait for
     * the camera direction to point to the focus.
     *
     * @param focusName The name of the new focus object.
     */
    void setCameraFocus(String focusName);

    /**
     * Sets the camera in focus mode with the focus object that bears the given
     * <code>focusName</code>. The amount of time to block and wait for the
     * camera to face the focus can also be specified in
     * <code>waitTimeSeconds</code>.
     *
     * @param focusName       The name of the new focus object.
     * @param waitTimeSeconds Maximum time in seconds to wait for the camera to face the
     *                        focus. If negative, we wait indefinitely.
     */
    void setCameraFocus(String focusName,
                        float waitTimeSeconds);

    /**
     * Sets the camera in focus mode with the given focus object. It also
     * instantly sets the camera direction vector to point towards the focus.
     *
     * @param focusName The name of the new focus object.
     */
    void setCameraFocusInstant(final String focusName);

    /**
     * Sets the camera in focus mode with the given focus object and instantly moves
     * the camera next to the focus object.
     *
     * @param focusName The name of the new focus object.
     */
    void setCameraFocusInstantAndGo(final String focusName);

    /**
     * Activates or deactivates the camera lock to the focus reference system
     * when in focus mode.
     *
     * @param lock Activate or deactivate the lock.
     */
    void setCameraLock(boolean lock);

    /**
     * Whether to look for the focus constantly when in focus mode and center it
     * in the view or whether the view must be free. Use True to center the focus
     * (default behaviour) and False to set it to a free view.
     *
     * @param centerFocus Whether to center the focus or not.
     */
    void setCameraCenterFocus(boolean centerFocus);

    /**
     * Locks or unlocks the orientation of the camera to the focus object's
     * rotation.
     *
     * @param lock Whether to lock or unlock the camera orientation to the focus.
     */
    void setCameraOrientationLock(boolean lock);

    /**
     * Sets the camera in free mode.
     */
    void setCameraFree();

    /**
     * Sets the camera position to the given coordinates, in Km, equatorial
     * system.
     *
     * @param vec Vector of three components in internal coordinates and Km.
     *
     * @deprecated Use {@link #setCameraPosition(double[])} instead.
     */
    @Deprecated
    void setCameraPostion(double[] vec);

    /**
     * Sets the camera position to the given coordinates, in the internal reference system and kilometres.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param position  Vector of three components in internal coordinates and Km.
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void setCameraPosition(double[] position,
                           boolean immediate);

    /**
     * Sets the camera position to the given coordinates, in the internal reference system and in the requested units.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param position  Vector of three components in internal coordinates and the requested units.
     * @param units     The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void setCameraPosition(double[] position,
                           String units,
                           boolean immediate);

    /**
     * Component-wise version of {@link #setCameraPosition(double[])}.
     */
    void setCameraPosition(double x,
                           double y,
                           double z);

    /**
     * Component-wise version of {@link #setCameraPosition(double[], String)}.
     */
    void setCameraPosition(double x,
                           double y,
                           double z,
                           String units);

    /**
     * Component-wise version of {@link #setCameraPosition(double[], boolean)}.
     */
    void setCameraPosition(double x,
                           double y,
                           double z,
                           boolean immediate);

    /**
     * Component-wise version of {@link #setCameraPosition(double[], String, boolean)}.
     */
    void setCameraPosition(double x,
                           double y,
                           double z,
                           String units,
                           boolean immediate);

    /**
     * Gets the current camera position, in km.
     *
     * @return The camera position coordinates in the internal reference system,
     * in km.
     */
    double[] getCameraPosition();

    /**
     * Gets the current camera position, in the requested units.
     *
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     *
     * @return The camera position coordinates in the internal reference system and in the requested units.
     */
    double[] getCameraPosition(String units);

    /**
     * Sets the camera position to the given coordinates, in the internal reference system and kilometres.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #setCameraPosition(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param position Vector of three components in internal coordinates and Km.
     */
    void setCameraPosition(double[] position);

    /**
     * Sets the camera position to the given coordinates, in the internal reference system and the given units.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #setCameraPosition(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param position Vector of three components in internal coordinates and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void setCameraPosition(double[] position,
                           String units);

    /**
     * Sets the camera direction vector to the given vector, in the internal reference system.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param direction The direction vector in the internal reference system.
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void setCameraDirection(double[] direction,
                            boolean immediate);

    /**
     * Gets the current camera direction vector.
     *
     * @return The camera direction vector in the internal reference system.
     */
    double[] getCameraDirection();

    /**
     * Sets the camera direction vector to the given vector, in the internal reference system.
     * You can convert from spherical coordinates using
     * {@link #equatorialCartesianToInternalCartesian(double[], double)},
     * {@link #galacticToInternalCartesian(double, double, double)} and
     * {@link #eclipticToInternalCartesian(double, double, double)}.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #setCameraDirection(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param dir The direction vector in equatorial cartesian coordinates.
     */
    void setCameraDirection(double[] dir);

    /**
     * Sets the camera up vector to the given vector, in the internal reference system.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param up        The up vector in equatorial coordinates.
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void setCameraUp(double[] up,
                     boolean immediate);

    /**
     * Gets the current camera up vector.
     *
     * @return The camera up vector in the internal reference system.
     */
    double[] getCameraUp();

    /**
     * Sets the camera up vector to the given vector, in the internal reference system.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #setCameraUp(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param up The up vector in equatorial coordinates.
     */
    void setCameraUp(double[] up);

    /**
     * Sets the camera orientation to the given quaternion, given as an array of [x, y, z, w].
     *
     * @param quaternion The 4-component quaternion.
     */
    void setCameraOrientationQuaternion(double[] quaternion);

    /**
     * Gets the current camera orientation quaternion.
     *
     * @return The current camera orientation quaternion, as an array of [x, y, z, w].
     */
    double[] getCameraOrientationQuaternion();

    /**
     * Sets the focus and instantly moves the camera to a point in the line
     * defined by <code>focus</code>-<code>other</code> and rotated
     * <code>rotation</code> degrees around <code>focus</code> using the camera
     * up vector as a rotation axis.
     *
     * @param focus      The name of the focus object.
     * @param other      The name of the other object, to the fine a line from this to
     *                   focus. Usually a light source.
     * @param rotation   The rotation angle, in degrees.
     * @param solidAngle The target solid angle which determines the distance, in degrees.
     */
    void setCameraPositionAndFocus(String focus,
                                   String other,
                                   double rotation,
                                   double solidAngle);

    /**
     * Sets the camera in free mode and points it to the given coordinates in equatorial system.
     *
     * @param ra  Right ascension in degrees.
     * @param dec Declination in degrees.
     */
    void pointAtSkyCoordinate(double ra,
                              double dec);

    /**
     * Gets the current physical speed of the camera in km/h.
     *
     * @return The current speed of the camera in km/h.
     */
    double getCameraSpeed();

    /**
     * Changes the speed multiplier of the camera and its acceleration.
     *
     * @param speed The new speed, from 0 to 100.
     */
    void setCameraSpeed(float speed);

    /**
     * Changes the speed of the camera when it rotates around a focus.
     *
     * @param speed The new rotation speed in [0,100]
     */
    void setCameraRotationSpeed(float speed);

    /**
     * Changes the speed of the camera when it rotates around a focus.
     *
     * @param speed The new rotation speed in [0,100]
     *
     * @deprecated Use {@link #setCameraRotationSpeed(float)}
     */
    @Deprecated
    void setRotationCameraSpeed(float speed);

    /**
     * Changes the turning speed of the camera.
     *
     * @param speed The new turning speed, from 1 to 100.
     */
    void setCameraTurningSpeed(float speed);

    /**
     * Changes the turning speed of the camera.
     *
     * @param speed The new turning speed, from 1 to 100.
     *
     * @deprecated Use {@link #setCameraTurningSpeed(float)}
     */
    @Deprecated
    void setTurningCameraSpeed(float speed);

    /**
     * Sets the speed limit of the camera given an index. The index corresponds
     * to the following:
     * <ul>
     * <li>0 - 1 Km/h</li>
     * <li>1 - 10 Km/h</li>
     * <li>2 - 100 Km/h</li>
     * <li>3 - 1000 Km/h</li>
     * <li>4 - 1 Km/s</li>
     * <li>5 - 10 Km/s</li>
     * <li>6 - 100 Km/s</li>
     * <li>7 - 1000 Km/s</li>
     * <li>8 - 0.01 c</li>
     * <li>9 - 0.1 c</li>
     * <li>10 - 0.5 c</li>
     * <li>11 - 0.8 c</li>
     * <li>12 - 0.9 c</li>
     * <li>13 - 0.99 c</li>
     * <li>14 - 0.99999 c</li>
     * <li>15 - 1 c</li>
     * <li>16 - 2 c</li>
     * <li>17 - 10 c</li>
     * <li>18 - 1e3 c</li>
     * <li>19 - 1 AU/s</li>
     * <li>20 - 10 AU/s</li>
     * <li>21 - 1000 AU/s</li>
     * <li>22 - 10000 AU/s</li>
     * <li>23 - 1 pc/s</li>
     * <li>24 - 2 pc/s</li>
     * <li>25 - 10 pc/s</li>
     * <li>26 - 1000 pc/s</li>
     * <li>27 - unlimited</li>
     * </ul>
     *
     * @param index The index of the top speed.
     */
    void setCameraSpeedLimit(int index);

    /**
     * Sets the camera to track the object with the given name. In this mode,
     * the position of the camera is still dependent on the focus object (if any), but
     * its direction points to the tracking object.
     *
     * @param objectName The name of the new tracking object.
     */
    void setCameraTrackingObject(String objectName);

    /**
     * Removes the tracking object from the camera, if any.
     */
    void removeCameraTrackingObject();

    /**
     * Adds a forward movement to the camera with the given value. If value is
     * negative the movement is backwards.
     *
     * @param value The magnitude of the movement, between -1 and 1.
     */
    void cameraForward(double value);

    /**
     * Adds a rotation movement to the camera around the current focus, or a pitch/yaw if in free mode.
     * <p>
     * If the camera is not using the cinematic behaviour ({@link #setCinematicCamera(boolean)},
     * the rotation movement will not be permanent. Use the cinematic behaviour to have the camera
     * continue to rotate around the focus.
     *
     * @param deltaX The x component, between 0 and 1. Positive is right and
     *               negative is left.
     * @param deltaY The y component, between 0 and 1. Positive is up and negative
     *               is down.
     */
    void cameraRotate(double deltaX,
                      double deltaY);

    /**
     * Adds a roll force to the camera.
     *
     * @param roll The intensity of the roll.
     */
    void cameraRoll(double roll);

    /**
     * Adds a turn force to the camera (yaw and/or pitch). If the camera is in focus mode, it
     * permanently deviates the line of sight from the focus until centered
     * again.
     * If the camera is not using the cinematic behaviour ({@link #setCinematicCamera(boolean)},
     * the turn will not be permanent. Use the cinematic behaviour to have the turn
     * persist in time.
     *
     * @param deltaX The x component, between 0 and 1. Positive is right and
     *               negative is left.
     * @param deltaY The y component, between 0 and 1. Positive is up and negative
     *               is down.
     */
    void cameraTurn(double deltaX,
                    double deltaY);

    /**
     * Adds a yaw to the camera. Same as {@link #cameraTurn(double, double)} with
     * deltaY set to zero.
     *
     * @param amount The amount.
     */
    void cameraYaw(double amount);

    /**
     * Adds a pitch to the camera. Same as {@link #cameraTurn(double, double)} with
     * deltaX set to zero.
     *
     * @param amount The amount.
     */
    void cameraPitch(double amount);

    /**
     * Stops all camera motion.
     */
    void cameraStop();

    /**
     * Centers the camera to the focus, removing any deviation of the line of
     * sight. Useful to center the focus object again after turning.
     */
    void cameraCenter();

    /**
     * Returns the closest object to the camera in this instant as a
     * {@link IFocus}.
     *
     * @return The closest object to the camera.
     */
    IFocus getClosestObjectToCamera();

    /**
     * Changes the field of view of the camera.
     *
     * @param newFov The new field of view value in degrees, between {@link gaiasky.util.Constants#MIN_FOV} and
     *               {@link gaiasky.util.Constants#MAX_FOV}.
     */
    void setFov(float newFov);

    /**
     * Sets the camera state (position, direction and up vector).
     *
     * @param pos The position of the camera in internal units, not Km.
     * @param dir The direction of the camera.
     * @param up  The up vector of the camera.
     */
    void setCameraState(double[] pos,
                        double[] dir,
                        double[] up);

    /**
     * Sets the camera state (position, direction and up vector) plus the current time.
     *
     * @param pos  The position of the camera in internal units, not Km.
     * @param dir  The direction of the camera.
     * @param up   The up vector of the camera.
     * @param time The new time of the camera as the
     *             number of milliseconds since the epoch (Jan 1, 1970).
     */
    void setCameraStateAndTime(double[] pos,
                               double[] dir,
                               double[] up,
                               long time);

    /**
     * See {@link #setComponentTypeVisibility(String, boolean)}.
     *
     * @deprecated
     */
    @Deprecated
    void setVisibility(String key,
                       boolean visible);

    /**
     * Sets the component described by the given name visible or invisible.
     *
     * @param key     The key of the component: "element.stars", "element.planets",
     *                "element.moons", etc. See
     *                {@link gaiasky.render.ComponentTypes.ComponentType}.
     * @param visible The visible value.
     */
    void setComponentTypeVisibility(String key,
                                    boolean visible);

    /**
     * Gets the visibility of the component type described by the key. Examples of keys are
     * "element.stars", "element.planets" or "element.moons". See {@link gaiasky.render.ComponentTypes.ComponentType}.
     *
     * @param key The key of the component type to query.
     *
     * @return The visibility of the component type.
     */
    boolean getComponentTypeVisibility(String key);

    /**
     * <p>
     * Projects the world space position of the given object to screen coordinates. It's the same as GLU gluProject with
     * one small deviation: The viewport is assumed to span the
     * whole screen. The screen coordinate system has its origin in the bottom left, with the y-axis pointing upwards
     * and the x-axis pointing to the right. This
     * makes it easily usable in conjunction with Batch and similar classes.
     * </p>
     * <p>This call only works if Gaia Sky is using the simple perspective projection mode. It does not work with any of
     * the following modes: panorama (with any of
     * the projections), planetarium, orthosphere, stereoscopic, or any of the re-projection modes.</p>
     *
     * @param name The name of the object to get the screen coordinates for.
     *
     * @return An array with the x and y screen coordinates, in pixels, with the origin at the bottom-left. If the
     * object with the given name does not exist, or it falls
     * off-screen, it returns null.
     */
    double[] getObjectScreenCoordinates(String name);

    /**
     * Sets the visibility of a particular object. Use this method to hide individual objects.
     * Changes to the individual object visibility are not persisted on restart.
     *
     * @param name    The name of the object. Must be an instance of {@link IVisibilitySwitch}.
     * @param visible The visible status to set. Set to false in order to hide the object. True to make it visible.
     *
     * @return True if the visibility was set successfully, false if there were errors.
     */
    boolean setObjectVisibility(String name,
                                boolean visible);

    /**
     * Gets the visibility of a particular object.
     *
     * @param name The name of the object. Must be an instance of {@link IVisibilitySwitch}.
     *
     * @return The visibility status of the object, if it exists.
     */
    boolean getObjectVisibility(String name);

    /**
     * <p>
     * Sets the given quaternions file (CSV with times and quaternions) as the orientation provider
     * for this object. This call removes the previous orientation model from the object (either
     * {@link RigidRotation} or {@link AttitudeComponent}.
     * </p>
     * <p>
     * The interpolation between quaternions is done using slerp (spherical linear interpolation).
     * </p>
     *
     * @param name The name of the object. The object must already have an <code>Orientation</code> component.
     * @param file The file path. The file is a CSV where each line has the time (ISO-8601) and the
     *             x, y, z and w components of the quaternion for that time. For instance, a valid line would
     *             be "<code>2018-04-01T15:20:15Z,0.9237,0.3728,0.0358,0.0795</code>".
     *
     * @return True if the object was found and could be updated.
     */
    boolean setObjectQuaternionSlerpOrientation(String name,
                                                String file);


    /**
     * <p>
     * Sets the given quaternions file (CSV with times and quaternions) as the orientation provider
     * for this object. This call removes the previous orientation model from the object (either
     * {@link RigidRotation} or {@link AttitudeComponent}.
     * </p>
     * <p>
     * The interpolation between quaternions is done using nlerp (normalized linear interpolation).
     * </p>
     *
     * @param name The name of the object. The object must already have an <code>Orientation</code> component.
     * @param file The file path. The file is a CSV where each line has the time (ISO-8601) and the
     *             x, y, z and w components of the quaternion for that time. For instance, a valid line would
     *             be "<code>2018-04-01T15:20:15Z,0.9237,0.3728,0.0358,0.0795</code>".
     *
     * @return True if the object was found and could be updated.
     */
    boolean setObjectQuaternionNlerpOrientation(String name,
                                                String file);

    /**
     * Sets the label size factor. The label size will be multiplied by this.
     *
     * @param factor Factor in {@link Constants#MIN_LABEL_SIZE} and {@link Constants#MAX_LABEL_SIZE}.
     */
    void setLabelSizeFactor(float factor);

    /**
     * Forces the label to display for the given object, bypassing the solid angle
     * requirements that are usually in place and determine label visibility.
     * This setting does not override the visibility of the object itself, or of
     * the label visibility component element.
     * Changes to the force display label flag are not persisted on restart.
     * /*
     *
     * @param name       The object name.
     * @param forceLabel Whether to force the label to render for this object or not.
     */
    void setForceDisplayLabel(String name,
                              boolean forceLabel);

    /**
     * Sets the label color of the object identified by the given name.
     * The label color must be an array of RGBA values in [0,1].
     *
     * @param name  The object name.
     * @param color The label color as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void setLabelColor(String name,
                       double[] color);

    /**
     * Gets the value of the force display label flag for the object identified with the
     * given name.
     *
     * @param name The name of the object.
     *
     * @return The value of the force display label flag of the object, if it exists.
     */
    boolean getForceDisplayLabel(String name);

    /**
     * Sets the line width factor. The line width will be multiplied by this.
     *
     * @param factor Factor in {@link Constants#MIN_LINE_WIDTH} and {@link Constants#MAX_LINE_WIDTH}.
     */
    void setLineWidthFactor(float factor);

    /**
     * Sets the number factor of proper motion vectors that are visible. In [1,100].
     *
     * @param factor Factor in [1,100].
     */
    void setProperMotionsNumberFactor(float factor);

    /**
     * Sets the length of the proper motion vectors, in [500,30000].
     *
     * @param factor Factor in [500,30000].
     */
    void setProperMotionsLengthFactor(float factor);

    /**
     * Sets the color mode of proper motion vectors.
     *
     * @param mode The color mode:
     *             <ul>
     *             <li>0 - direction: the normalised cartesian velocity components XYZ are mapped to the color channels RGB.</li>
     *             <li>1 - magnitude (speed): the magnitude of the velocity vector is mapped using a rainbow scheme (blue-green-yellow-red) with the color map limit at 100 Km/s.</li>
     *             <li>2 - has radial velocity: blue for stars with radial velocity, red for stars without.</li>
     *             <li>3 - redshift from Sun: blue stars have negative radial velocity (from the Sun), red stars have positive radial velocity (from the Sun). Blue is mapped to -100 Km/s, red is mapped to 100 Km/s.</li>
     *             <li>4 - redshift from camera: blue stars have negative radial velocity (from the camera), red stars have positive radial velocity (from the camera). Blue is mapped to -100 Km/s, red is mapped to 100 Km/s.</li>
     *             <li>5 - single color: same color for all velocity vectors.</li>
     *             </ul>
     */
    void setProperMotionsColorMode(int mode);

    /**
     * Sets whether to show arrowheads or not for the velocity vectors.
     *
     * @param arrowheadsEnabled Whether to show the velocity vectors with arrowheads.
     */
    void setProperMotionsArrowheads(boolean arrowheadsEnabled);

    /**
     * Returns the current maximum number of velocity vectors per star group.
     *
     * @return Max number of velocity vectors per star group.
     */
    long getProperMotionsMaxNumber();

    /**
     * Sets the maximum number of proper motion vectors to add per star group.
     *
     * @param maxNumber The maximum number of proper motion vectors per star group.
     */
    void setProperMotionsMaxNumber(long maxNumber);

    /**
     * Sets the visibility of all cross-hairs.
     *
     * @param visible The visibility state, which applies to all cross-hairs.
     */
    void setCrosshairVisibility(boolean visible);

    /**
     * Sets the visibility of the focus object crosshair.
     *
     * @param visible The visibility state.
     */
    void setFocusCrosshairVisibility(boolean visible);

    /**
     * Sets the visibility of the closest object crosshair.
     *
     * @param visible The visibility state.
     */
    void setClosestCrosshairVisibility(boolean visible);

    /**
     * Sets the visibility of the home object crosshair.
     *
     * @param visible The visibility state.
     */
    void setHomeCrosshairVisibility(boolean visible);

    /**
     * Shows or hides the minimap.
     *
     * @param visible The visibility state.
     */
    void setMinimapVisibility(boolean visible);

    /**
     * Sets the ambient light to a certain value.
     *
     * @param value The value of the ambient light in [0,1].
     */
    void setAmbientLight(float value);

    /**
     * Sets the time of the application to the given time, in UTC.
     *
     * @param year     The year to represent.
     * @param month    The month-of-year to represent, from 1 (January) to 12
     *                 (December).
     * @param day      The day-of-month to represent, from 1 to 31.
     * @param hour     The hour-of-day to represent, from 0 to 23.
     * @param min      The minute-of-hour to represent, from 0 to 59.
     * @param sec      The second-of-minute to represent, from 0 to 59.
     * @param millisec The millisecond-of-second, from 0 to 999.
     */
    void setSimulationTime(int year,
                           int month,
                           int day,
                           int hour,
                           int min,
                           int sec,
                           int millisec);

    /**
     * Returns the current simulation time as the number of milliseconds since
     * 1970-01-01T00:00:00Z (UTC).
     *
     * @return Number of milliseconds since the epoch (Jan 1, 1970 00:00:00 UTC).
     */
    long getSimulationTime();

    /**
     * Sets the time of the application. The long value represents specified
     * number of milliseconds since the standard base time known as "the epoch",
     * namely January 1, 1970, 00:00:00 GMT.
     *
     * @param time Number of milliseconds since the epoch (Jan 1, 1970).
     */
    void setSimulationTime(long time);

    /**
     * Returns the current UTC simulation time in an array.
     *
     * @return The current simulation time in an array with the given indices.
     * <ul>
     * <li>0 - The year.</li>
     * <li>1 - The month, from 1 (January) to 12 (December).</li>
     * <li>2 - The day-of-month, from 1 to 31.</li>
     * <li>3 - The hour-of-day, from 0 to 23.</li>
     * <li>4 - The minute-of-hour, from 0 to 59.</li>
     * <li>5 - The second-of-minute, from 0 to 59.</li>
     * <li>6 - The millisecond-of-second, from 0 to 999.</li>
     * </ul>
     */
    int[] getSimulationTimeArr();

    /**
     * Starts the simulation.
     */
    void startSimulationTime();

    /**
     * Stops the simulation time.
     */
    void stopSimulationTime();

    /**
     * Queries whether the time is on or not.
     *
     * @return True if the time is on, false otherwise.
     */
    boolean isSimulationTimeOn();

    /**
     * Deprecated, use {@link #setTimeWarp(double)} instead.
     */
    @Deprecated
    void setSimulationPace(double pace);

    /**
     * Sets the simulation time warp factor. Positive values make time advance forward, while negative values make time
     * run backwards. A warp factor of 1 sets a real time pace to the simulation time.
     *
     * @param warpFactor The warp as a factor. A value of 2.0 sets the
     *                   Gaia Sky time to be twice as fast as real world time.
     */
    void setTimeWarp(double warpFactor);

    /**
     * Sets a time bookmark in the global clock that, when reached, the clock
     * automatically stops.
     *
     * @param ms The time as the number of milliseconds since the epoch (Jan 1,
     *           1970).
     */
    void setTargetTime(long ms);

    /**
     * Sets a time bookmark in the global clock that, when reached, the clock
     * automatically stops.
     *
     * @param year     The year to represent.
     * @param month    The month-of-year to represent, from 1 (January) to 12
     *                 (December).
     * @param day      The day-of-month to represent, from 1 to 31.
     * @param hour     The hour-of-day to represent, from 0 to 23.
     * @param min      The minute-of-hour to represent, from 0 to 59.
     * @param sec      The second-of-minute to represent, from 0 to 59.
     * @param milliSec The millisecond-of-second, from 0 to 999.
     */
    void setTargetTime(int year,
                       int month,
                       int day,
                       int hour,
                       int min,
                       int sec,
                       int milliSec);

    /**
     * Unsets the target time bookmark from the global clock, if any.
     */
    void unsetTargetTime();

    /**
     * Gets the star brightness value.
     *
     * @return The brightness value, between 0 and 100.
     */
    float getStarBrightness();

    /**
     * Sets the star brightness value.
     *
     * @param brightness The brightness value, between 0 and 100.
     */
    void setStarBrightness(float brightness);

    /**
     * Sets the star brightness power profile value in [1.1, 0.9]. Default value is 1.
     * The power is applied to the star solid angle (from camera),
     * before clamping, as sa = pow(sa, r).
     *
     * @param power The power value in [0, 100].
     */
    void setStarBrightnessPower(float power);

    /**
     * Sets the star glow factor level value. This controls the amount of glow light
     * when the camera is close to stars. Must be between {@link Constants#MIN_STAR_GLOW_FACTOR} and
     * {@link Constants#MAX_STAR_GLOW_FACTOR}.
     * Default is 0.06.
     *
     * @param glowFactor The new glow factor value.
     */
    void setStarGlowFactor(float glowFactor);

    /**
     * Gets the current point size value in pixels.
     *
     * @return The size value, in pixels.
     */
    float getPointSize();

    /**
     * Gets the current star size value in pixels.
     *
     * @return The size value, in pixels.
     *
     * @deprecated Use {@link  #getPointSize()} instead.
     */
    @Deprecated
    float getStarSize();

    /**
     * Sets the base point size.
     *
     * @param size The size value, between {@link Constants#MIN_STAR_POINT_SIZE} and
     *             {@link Constants#MAX_STAR_POINT_SIZE}.
     */
    void setPointSize(float size);

    /**
     * Sets the size of the rasterized stars, in pixels.
     *
     * @param size The size value in pixels, between {@link Constants#MIN_STAR_POINT_SIZE} and
     *             {@link Constants#MAX_STAR_POINT_SIZE}.
     *
     * @deprecated Use {@link  #setPointSize(float)} instead.
     */
    @Deprecated
    void setStarSize(float size);

    /**
     * Gets the base star opacity.
     *
     * @return The base opacity value.
     */
    float getStarBaseOpacity();

    /**
     * Gets the minimum star opacity.
     *
     * @return The minimum opacity value.
     *
     * @deprecated Use {@link #getStarBaseOpacity()} instead.
     */
    @Deprecated
    float getStarMinOpacity();

    /**
     * Sets the base star opacity.
     *
     * @param opacity The base opacity value, between {@link Constants#MIN_STAR_MIN_OPACITY} and
     *                {@link Constants#MAX_STAR_MIN_OPACITY}.
     */
    void setStarBaseOpacity(float opacity);

    /**
     * Sets the minimum star opacity.
     *
     * @param opacity The minimum opacity value, between {@link Constants#MIN_STAR_MIN_OPACITY} and
     *                {@link Constants#MAX_STAR_MIN_OPACITY}.
     *
     * @deprecated Use {@link #setStarBaseOpacity(float)} instead.
     */
    @Deprecated
    void setStarMinOpacity(float opacity);

    /**
     * Sets the star texture index, in [1, 4]
     * <p>
     * 1 - horizontal spike
     * 2 - god rays
     * 3 - horizontal and vertical spikes
     * 4 - simple radial profile
     *
     * @param index The new star texture index
     */
    void setStarTextureIndex(int index);

    /**
     * Sets the number of nearest stars to be processed for each
     * star group. This will limit the number of stars that are
     * rendered with billboards, labels and velocity vectors.
     *
     * @param n The new number of nearest stars
     */
    void setStarGroupNearestNumber(int n);

    /**
     * Enable or disable the rendering of close stars as billboards.
     *
     * @param flag The state flag
     */
    void setStarGroupBillboard(boolean flag);

    /**
     * Sets the solid angle below which orbits fade and disappear.
     *
     * @param angleDeg The threshold angle in degrees
     */
    void setOrbitSolidAngleThreshold(float angleDeg);

    /**
     * Sets the projection yaw angle (if this is a slave instance), in degrees.
     * The yaw angle turns the camera to the right.
     * This function is intended for multi-projector setups, to configure
     * slaves without restarting Gaia Sky.
     *
     * @param yaw The yaw angle in degrees.
     */
    void setProjectionYaw(float yaw);

    /**
     * Sets the projection pitch angle (if this is a slave instance), in degrees.
     * The pitch angle turns the camera up.
     * This function is intended for multi-projector setups, to configure
     * slaves without restarting Gaia Sky.
     *
     * @param pitch The pitch angle in degrees.
     */
    void setProjectionPitch(float pitch);

    /**
     * Sets the projection roll angle (if this is a slave instance), in degrees.
     * The roll angle rolls the camera clockwise.
     * This function is intended for multi-projector setups, to configure
     * slaves without restarting Gaia Sky.
     *
     * @param roll The roll angle in degrees.
     */
    void setProjectionRoll(float roll);

    /**
     * Same as {@link #setFov(float)}, but bypassing the restriction of an active
     * projection in the slave (slaves that have an active projection do not accept
     * fov modification events).
     *
     * @param fov The field of view angle.
     */
    void setProjectionFov(float fov);

    /**
     * Limits the frame rate of Gaia Sky.
     *
     * @param limitFps The new maximum frame rate as a double-precision floating point number. Set zero or negative to
     *                 unlimited.
     */
    void setLimitFps(double limitFps);

    /**
     * Limits the frame rate of Gaia Sky.
     *
     * @param limitFps The new maximum frame rate as an integer number. Set zero or negative to unlimited.
     */
    void setLimitFps(int limitFps);

    /**
     * Configures the screenshot system, setting the resolution of the images,
     * the output directory and the image name prefix.
     *
     * @param width      Width of images.
     * @param height     Height of images.
     * @param directory  The output directory path.
     * @param namePrefix The file name prefix.
     */
    void configureScreenshots(int width,
                              int height,
                              String directory,
                              String namePrefix);

    /**
     * Sets the screenshot mode. Possible values are 'simple' and 'advanced'.
     * The <b>simple</b> mode is faster and just outputs the last frame rendered to the Gaia Sky window, with the same
     * resolution and containing the UI elements.
     * The <b>advanced</b> mode redraws the last frame using the resolution configured using
     * {@link #configureScreenshots(int, int, String, String)} and
     * it does not draw the UI.
     *
     * @param screenshotMode The screenshot mode. 'simple' or 'advanced'.
     */
    void setScreenshotsMode(String screenshotMode);

    /**
     * Takes a screenshot of the current frame and saves it to the configured
     * location (see {@link #configureScreenshots(int, int, String, String)}).
     */
    void saveScreenshot();

    /**
     * Alias to {@link #saveScreenshot()}.
     */
    void takeScreenshot();

    /**
     * Configures the frame output system, setting the resolution of the images,
     * the target frames per second, the output directory and the image name
     * prefix.
     *
     * @param width      Width of images.
     * @param height     Height of images.
     * @param fps        Target frames per second (number of images per second).
     * @param directory  The output directory path.
     * @param namePrefix The file name prefix.
     *
     * @deprecated Use {@link #configureFrameOutput(int, int, int, String, String)} instead.
     */
    @Deprecated
    void configureRenderOutput(int width,
                               int height,
                               int fps,
                               String directory,
                               String namePrefix);

    /**
     * Configures the frame output system, setting the resolution of the images,
     * the target frames per second, the output directory and the image name
     * prefix. This function sets the frame output mode to 'advanced'.
     *
     * @param width      Width of images.
     * @param height     Height of images.
     * @param fps        Target frames per second (number of images per second).
     * @param directory  The output directory path.
     * @param namePrefix The file name prefix.
     */
    void configureFrameOutput(int width,
                              int height,
                              int fps,
                              String directory,
                              String namePrefix);

    /**
     * Configures the frame output system, setting the resolution of the images,
     * the target frames per second, the output directory and the image name
     * prefix. This function sets the frame output mode to 'advanced'.
     *
     * @param width      Width of images.
     * @param height     Height of images.
     * @param fps        Target frames per second (number of images per second).
     * @param directory  The output directory path.
     * @param namePrefix The file name prefix.
     */
    void configureFrameOutput(int width,
                              int height,
                              double fps,
                              String directory,
                              String namePrefix);

    /**
     * Sets the frame output mode. Possible values are 'simple' and 'advanced'.
     * The <b>simple</b> mode is faster and just outputs the last frame rendered to the Gaia Sky window, with the same
     * resolution and containing the UI elements.
     * The <b>advanced</b> mode redraws the last frame using the resolution configured using
     * {@link #configureFrameOutput(int, int, int, String, String)} and
     * it does not draw the UI.
     *
     * @param screenshotMode The screenshot mode. 'simple' or 'advanced'.
     */
    void setFrameOutputMode(String screenshotMode);

    /**
     * Is the frame output system on?
     *
     * @return True if the frame output is active.
     *
     * @deprecated Use {@link #isFrameOutputActive()} instead.
     */
    @Deprecated
    boolean isRenderOutputActive();

    /**
     * Is the frame output system on?
     *
     * @return True if the render output is active.
     */
    boolean isFrameOutputActive();

    /**
     * Gets the current FPS setting in the frame output system.
     *
     * @return The FPS setting.
     *
     * @deprecated Use {@link #getFrameOutputFps()} instead.
     */
    @Deprecated
    double getRenderOutputFps();

    /**
     * Gets the current FPS setting in the frame output system.
     *
     * @return The FPS setting.
     */
    double getFrameOutputFps();

    /**
     * Activates or deactivates the image output system. If called with true,
     * the system starts outputting images right away.
     *
     * @param active Whether to activate or deactivate the frame output system.
     */
    void setFrameOutput(boolean active);

    /**
     * Gets an object from the scene graph by <code>name</code> or id (HIP, TYC, Gaia SourceId).
     *
     * @param name The name or id (HIP, TYC, Gaia SourceId) of the object.
     *
     * @return The object as a {@link gaiasky.scene.view.FocusView}, or null
     * if it does not exist.
     */
    FocusView getObject(String name);

    /**
     * Gets an object by <code>name</code> or id (HIP, TYC, Gaia SourceID), optionally waiting
     * until the object is available, with a timeout.
     *
     * @param name           The name or id (HIP, TYC, Gaia SourceId) of the object.
     * @param timeoutSeconds The timeout in seconds to wait until returning.
     *                       If negative, it waits indefinitely.
     *
     * @return The object if it exists, or null if it does not and block is false, or if block is true and
     * the timeout has passed.
     */
    FocusView getObject(String name,
                        double timeoutSeconds);

    /**
     * Gets a {@link gaiasky.scene.component.Verts} object from the scene by <code>name</code>.
     *
     * @param name The name of the line object.
     *
     * @return The line object as a {@link gaiasky.scene.view.VertsView}, or null
     * if it does not exist.
     */
    VertsView getLineObject(String name);

    /**
     * Gets a {@link gaiasky.scene.component.Verts} object from the scene by <code>name</code>.
     *
     * @param name           The name of the line object.
     * @param timeoutSeconds The timeout in seconds to wait until returning.
     *                       If negative, it waits indefinitely.
     *
     * @return The line object as a {@link gaiasky.scene.view.VertsView}, or null
     * if it does not exist.
     */
    VertsView getLineObject(String name,
                            double timeoutSeconds);

    /**
     * Sets the given size scaling factor to the object identified by
     * <code>name</code>. This method will only work with model objects such as
     * planets, asteroids, satellites, etc. It will not work with orbits, stars
     * or any other types.
     * <p>
     * Also, <strong>use this with caution</strong>, as scaling the size of
     * objects can have unintended side effects, and remember to set the scaling
     * back to 1.0 at the end of your script.
     * </p>
     *
     * @param name          The name or id of the object.
     * @param scalingFactor The scaling factor to scale the size of that object.
     */
    void setObjectSizeScaling(String name,
                              double scalingFactor);

    /**
     * Sets the given orbit coordinates scaling factor to the AbstractOrbitCoordinates identified by
     * <code>name</code>. See {@link gaiasky.util.coord.AbstractOrbitCoordinates} and its subclasses.
     * <p>
     * Also, <strong>use this with caution</strong>, as scaling coordinates
     * may have unintended side effects, and remember to set the scaling
     * back to 1.0 at the end of your script.
     * Additionally, use either {@link #refreshAllOrbits()} or
     * {@link #refreshObjectOrbit(String)} right after
     * this call in order to immediately refresh the scaled orbits.
     * </p>
     *
     * @param name          The name of the coordinates object (OrbitLintCoordinates, EclipticCoordinates, SaturnVSOP87,
     *                      UranusVSOP87, EarthVSOP87, MercuryVSOP87, ..., PlutoCoordinates,
     *                      HeliotropicOribtCoordinates, MoonAACoordinates).
     *                      Optionally, you can append ':objectName' to select a single object. For instance, both Gaia
     *                      and JWST have
     *                      heliotropic orbit coordinates. To only select the Gaia orbit provider, use
     *                      "HeliotropicOrbitCoordinates:Gaia".
     * @param scalingFactor The scaling factor.
     */
    void setOrbitCoordinatesScaling(String name,
                                    double scalingFactor);

    /**
     * Forces the refresh of the orbit of the object identified by
     * <code>name</code>. This should generally be called after a call to
     * {@link #setOrbitCoordinatesScaling(String, double)}.
     */
    void refreshObjectOrbit(String name);

    /**
     * Forces all orbits to refresh immediately.
     */
    void refreshAllOrbits();

    /**
     * Forcefully triggers an update of the scene and the positions of all
     * the objects. Useful to call after operations that modify the position of objects.
     */
    void forceUpdateScene();

    /**
     * Gets the size of the object identified by <code>name</code>, in Km, by
     * name or id (HIP, TYC, sourceId).
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return The radius of the object in Km. If the object identified by name
     * or id (HIP, TYC, sourceId). does not exist, it returns a negative
     * value.
     */
    double getObjectRadius(String name);

    /**
     * <p>
     * Runs a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle is <code>20 degrees</code>.
     * </p>
     * <p>Warning: This method is not deterministic. It is implemented as a loop that sends 'camera forward' events
     * to the main thread, running in a separate thread. If you need total synchronization and reproducibility,
     * look into the {@link #parkRunnable(String, Runnable)} family of calls.</p>
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     */
    void goToObject(String name);

    /**
     * Runs a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle <code>viewAngle</code> is met. If angle is
     * negative, the default angle is <code>20 degrees</code>.
     *
     * <p>Warning: This method is not deterministic. It is implemented as a loop that sends 'camera forward' events
     * to the main thread, running in a separate thread. If you need total synchronization and reproducibility,
     * look into the {@link #parkRunnable(String, Runnable)} family of calls.</p>
     *
     * @param name       The name or id (HIP, TYC, sourceId) of the object.
     * @param solidAngle The target solid angle of the object, in degrees. The angle
     *                   gets larger and larger as we approach the object.
     */
    void goToObject(String name,
                    double solidAngle);

    /**
     * Runs a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle <code>viewAngle</code> is met. If angle is
     * negative, the default angle is <code>20 degrees</code>. If
     * <code>waitTimeSeconds</code> is positive, it indicates the number of
     * seconds to wait (block the function) for the camera to face the focus
     * before starting the forward movement. This very much depends on the
     * <code>turn velocity</code> of the camera. See
     * {@link #setTurningCameraSpeed(float)}.
     *
     * <p>Warning: This method is not deterministic. It is implemented as a loop that sends 'camera forward' events
     * to the main thread, running in a separate thread. If you need total synchronization and reproducibility,
     * look into the {@link #parkRunnable(String, Runnable)} family of calls.</p>
     *
     * @param name            The name or id (HIP, TYC, sourceId) of the object.
     * @param solidAngle      The target solid angle of the object, in degrees. The angle
     *                        gets larger and larger as we approach the object.
     * @param waitTimeSeconds The seconds to wait for the camera direction vector and the
     *                        vector from the camera position to the target object to be
     *                        aligned.
     */
    void goToObject(String name,
                    double solidAngle,
                    float waitTimeSeconds);

    /**
     * Sets the camera in focus mode with the given focus object and instantly moves
     * the camera next to the focus object.
     *
     * @param name The name of the new focus object.
     */
    void goToObjectInstant(String name);

    /**
     * Move the camera to the object identified by the given name, internally using smooth camera transition calls,
     * like {@link #cameraTransition(double[], double[], double[], double)}.
     * The target position and orientation are computed first during the call execution and are not subsequently updated. This means that
     * the camera does not follow the object if it moves. If time is activated, and the object moves, this call does not go to the
     * object's current position at the end, but at the beginning.
     *
     * @param name                       The name of the object to go to.
     * @param positionDurationSeconds    The duration of the transition in position, in seconds.
     * @param orientationDurationSeconds The duration of the transition in orientation, in seconds.
     */
    void goToObjectSmooth(String name, double positionDurationSeconds, double orientationDurationSeconds);

    /**
     * Same as {@link #goToObjectSmooth(String, double, double)}, but with the target solid angle of the object.
     *
     * @param name                       The name of the object to go to.
     * @param solidAngle                 The target solid angle of the object, in degrees. This
     *                                   is used to compute the final distance to the object. The angle
     *                                   gets larger and larger as we approach the object.
     * @param positionDurationSeconds    The duration of the transition in position, in seconds.
     * @param orientationDurationSeconds The duration of the transition in orientation, in seconds.
     */
    void goToObjectSmooth(String name, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds);

    /**
     * Same as {@link #goToObjectSmooth(String, double, double)}, but with a boolean that indicates whether the call is synchronous.
     *
     * @param name                       The name of the object to go to.
     * @param positionDurationSeconds    The duration of the transition in position, in seconds.
     * @param orientationDurationSeconds The duration of the transition in orientation, in seconds.
     * @param sync                       If true, the call is synchronous and waits for the camera
     *                                   file to finish. Otherwise, it returns immediately.
     */
    void goToObjectSmooth(String name, double positionDurationSeconds, double orientationDurationSeconds, boolean sync);

    /**
     * Same as {@link #goToObjectSmooth(String, double, double, boolean)}, but with the target solid angle of the object.
     *
     * @param name                       The name of the object to go to.
     * @param solidAngle                 The target solid angle of the object, in degrees. This
     *                                   is used to compute the final distance to the object. The angle
     *                                   gets larger and larger as we approach the object.
     * @param positionDurationSeconds    The duration of the transition in position, in seconds.
     * @param orientationDurationSeconds The duration of the transition in orientation, in seconds.
     * @param sync                       If true, the call is synchronous and waits for the camera
     *                                   file to finish. Otherwise, it returns immediately.
     */
    void goToObjectSmooth(String name, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds, boolean sync);

    /**
     * Lands on the object with the given name, if it is a planet or moon. The land location is
     * determined by the line of sight from the current position of the camera
     * to the object.
     *
     * @param name The proper name of the object.
     */
    void landOnObject(String name);

    /**
     * Lands on the object with the given <code>name</code>, if it is
     * a planet or moon, at the
     * location with the given name, if it exists.
     *
     * @param name         The proper name of the object.
     * @param locationName The name of the location to land on
     */
    void landAtObjectLocation(String name,
                              String locationName);

    /**
     * Lands on the object with the given <code>name</code>, if it is a
     * planet or moon, at the
     * location specified in by [latitude, longitude], in degrees.
     *
     * @param name      The proper name of the object.
     * @param longitude The location longitude, in degrees.
     * @param latitude  The location latitude, in degrees.
     */
    void landAtObjectLocation(String name,
                              double longitude,
                              double latitude);

    /**
     * Returns the distance to the surface of the object identified with the
     * given <code>name</code>. If the object is an abstract node or does not
     * exist, it returns a negative distance.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return The distance to the object in km if it exists, a negative value
     * otherwise.
     */
    double getDistanceTo(String name);

    /**
     * Returns the star parameters given its identifier or name, if the star exists
     * and it is loaded.
     *
     * @param starId The star identifier or name.
     *
     * @return An array with (ra [deg], dec [deg], parallax [mas], pmra [mas/yr], pmdec [mas/yr], radvel [km/s], appmag
     * [mag], red [0,1], green [0,1], blue [0,1]) if the
     * star exists and is loaded, null otherwise.
     */
    double[] getStarParameters(String starId);

    /**
     * Gets the current position of the object identified by <code>name</code> in
     * the internal coordinate system and internal units. If the object does not exist,
     * it returns null.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return A 3-vector with the object's position in internal units and the internal reference system.
     */
    double[] getObjectPosition(String name);

    /**
     * Gets the current position of the object identified by <code>name</code> in
     * the internal coordinate system and the requested distance units. If the object does not exist,
     * it returns null.
     *
     * @param name  The name or id (HIP, TYC, sourceId) of the object.
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     *
     * @return A 3-vector with the object's position in the requested units and internal reference system.
     */
    double[] getObjectPosition(String name,
                               String units);

    /**
     * Gets the predicted position of the object identified by <code>name</code> in
     * the internal coordinate system and internal units. If the object does not exist,
     * it returns null.
     * The predicted position is the position of the object in the next update cycle, and
     * may be useful to compute the camera state.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return A 3-vector with the object's predicted position in the internal reference system.
     */
    double[] getObjectPredictedPosition(String name);

    /**
     * Gets the predicted position of the object identified by <code>name</code> in
     * the internal coordinate system and the requested distance units. If the object does not exist,
     * it returns null.
     * The predicted position is the position of the object in the next update cycle, and
     * may be useful to compute the camera state.
     *
     * @param name  The name or id (HIP, TYC, sourceId) of the object.
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     *
     * @return A 3-vector with the object's predicted position in the requested units and in the internal reference system.
     */
    double[] getObjectPredictedPosition(String name,
                                        String units);

    /**
     * Sets the internal position of the object identified by <code>name</code>. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param name     The name of the object.
     * @param position The position in the internal reference system and internal units.
     */
    void setObjectPosition(String name,
                           double[] position);

    /**
     * Sets the internal position of the object identified by <code>name</code>. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param name     The name of the object.
     * @param position The position in the internal reference system and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void setObjectPosition(String name,
                           double[] position,
                           String units);

    /**
     * Sets the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object in a focus view wrapper.
     * @param position The position in the internal reference system and internal units.
     */
    void setObjectPosition(FocusView object,
                           double[] position);

    /**
     * Sets the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object in a focus view wrapper.
     * @param position The position in the internal reference system and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void setObjectPosition(FocusView object,
                           double[] position,
                           String units);

    /**
     * Sets the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object entity.
     * @param position The position in the internal reference system and internal units.
     */
    void setObjectPosition(Entity object,
                           double[] position);

    /**
     * Sets the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object entity.
     * @param position The position in the internal reference system and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void setObjectPosition(Entity object,
                           double[] position,
                           String units);

    /**
     * Sets the coordinates provider for the object identified with the given name, if possible.
     * The provider object must implement {@link IPythonCoordinatesProvider}, and in particular
     * the method {@link IPythonCoordinatesProvider#getEquatorialCartesianCoordinates(Object, Object)}, which
     * takes in a julian date and outputs the object coordinates in the internal cartesian system.
     *
     * @param name     The name of the object.
     * @param provider The coordinate provider instance.
     */
    void setObjectCoordinatesProvider(String name,
                                      IPythonCoordinatesProvider provider);

    /**
     * Removes the current coordinates provider from the object with the given name. This method must
     * be called before shutting down the gateway if the coordinates provider has been previously set
     * for the given object from Python with {@link #setObjectCoordinatesProvider(String, IPythonCoordinatesProvider)}.
     * Otherwise, Gaia Sky will crash due to the missing connection to Python.
     *
     * @param name The name of the object for which to remove the coordinate provider.
     */
    void removeObjectCoordinatesProvider(String name);

    /**
     * Adds a new trajectory object with the given name, points and color. The trajectory
     * is rendered using the 'line renderer' setting in the preferences dialog.
     * This is a very similar call to {@link #addPolyline(String, double[], double[])},
     * but in this case the line can be rendered with higher quality
     * polyline quadstrips.
     *
     * @param name   The name to identify the trajectory, to possibly remove it later.
     * @param points The points of the trajectory. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn] in the internal reference system.
     * @param color  The color of the trajectory as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void addTrajectoryLine(String name,
                           double[] points,
                           double[] color);

    /**
     * Adds a new trajectory object with the given name, points and color. The trajectory
     * is rendered using the 'line renderer' setting in the preferences dialog.
     * This is a very similar call to {@link #addPolyline(String, double[], double[])},
     * but in this case the line can be rendered with higher quality
     * polyline quadstrips.
     *
     * @param name     The name to identify the trajectory, to possibly remove it later.
     * @param points   The points of the trajectory. It is an array containing all the
     *                 points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn] in the internal reference system.
     * @param color    The color of the trajectory as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param trailMap The bottom mapping position for the trail. The orbit trail assigns an opacity value to
     *                 each point of the orbit, where 1 is the location of the last point in the points list, and 0 is
     *                 the first one.
     *                 This mapping parameter defines the location in the orbit (in [0,1]) where we map the opacity
     *                 value of 0. Set to 0 to have a full trail. Set to 0.5 to have a trail that spans half the orbit.
     *                 Set to 1 to have no orbit at all. Set to negative to disable the trail.
     */
    void addTrajectoryLine(String name,
                           double[] points,
                           double[] color,
                           double trailMap);

    /**
     * Adds a new polyline with the given name, points and color. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. The default primitive of GL_LINE_STRIP
     * is used.
     *
     * @param name   The name to identify the polyline, to possibly remove it later.
     * @param points The points of the polyline. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn] in the internal reference system.
     * @param color  The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void addPolyline(String name,
                     double[] points,
                     double[] color);

    /**
     * Adds a new polyline with the given name, points, color and line width. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. The default primitive type of GL_LINE_STRIP
     * is used.
     *
     * @param name      The name to identify the polyline, to possibly remove it later.
     * @param points    The points of the polyline. It is an array containing all the
     *                  points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color     The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param lineWidth The line width. Usually a value between 1 (default) and 10.
     */
    void addPolyline(String name,
                     double[] points,
                     double[] color,
                     double lineWidth);

    /**
     * Adds a new polyline with the given name, points, color and line width. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. The default primitive type of GL_LINE_STRIP
     * is used. This version enables the addition of arrow caps. In the case arrow caps
     * are enabled, the line will be rendered in CPU mode (no VBO), making it slightly slower, especially for lines with
     * many points.
     * The arrow cap is added at the first point in the series.
     *
     * @param name      The name to identify the polyline, to possibly remove it later.
     * @param points    The points of the polyline. It is an array containing all the
     *                  points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color     The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param lineWidth The line width. Usually a value between 1 (default) and 10.
     * @param arrowCaps Whether to represent arrow caps. If enabled, the line is rendered in CPU mode, which is slower.
     */
    void addPolyline(String name,
                     double[] points,
                     double[] color,
                     double lineWidth,
                     boolean arrowCaps);

    /**
     * Adds a new polyline with the given name, points, color, line width and primitive. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it.
     *
     * @param name      The name to identify the polyline, to possibly remove it later.
     * @param points    The points of the polyline. It is an array containing all the
     *                  points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color     The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param lineWidth The line width. Usually a value between 1 (default) and 10.
     * @param primitive The GL primitive: GL_LINES=1, GL_LINE_LOOP=2, GL_LINE_STRIP=3
     */
    void addPolyline(String name,
                     double[] points,
                     double[] color,
                     double lineWidth,
                     int primitive);

    /**
     * Adds a new polyline with the given name, points, color, line width, primitive and arrow caps. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. This version enables the addition of arrow caps. In the case arrow
     * caps
     * are enabled, the line will be rendered in CPU mode (no VBO), making it slightly slower, especially for lines with
     * many points.
     * The arrow cap is added at the first point in the series.
     *
     * @param name      The name to identify the polyline, to possibly remove it later.
     * @param points    The points of the polyline. It is an array containing all the
     *                  points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color     The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param lineWidth The line width. Usually a value between 1 (default) and 10.
     * @param primitive The GL primitive: GL_LINES=1, GL_LINE_LOOP=2, GL_LINE_STRIP=3
     * @param arrowCaps Whether to represent arrow caps. If enabled, the line is rendered in CPU mode, which is slower.
     */
    void addPolyline(String name,
                     double[] points,
                     double[] color,
                     double lineWidth,
                     int primitive,
                     boolean arrowCaps);

    /**
     * <p>
     * Removes the model object identified by the given name from the internal
     * scene graph model of Gaia Sky, if it exists.
     * If the object has children, they are removed recursively.
     * Be careful with this function, as it can have unexpected side effects
     * depending on what objects are removed.
     * For example,
     * </p>
     *
     * <code>
     * gs.removeModelObject("Earth")
     * </code>
     *
     * <p>
     * removes the Earth, the Moon, Gaia and any dependent object from Gaia Sky.
     * </p>
     *
     * @param name The name of the object to remove.
     */
    void removeModelObject(String name);

    /**
     * Sets the vertical scroll position in the GUI.
     *
     * @param pixelY The pixel to set the scroll position to.
     */
    void setGuiScrollPosition(float pixelY);

    /**
     * Enables the GUI rendering. This makes the user interface
     * to be rendered and updated again if it was previously disabled. Otherwise, it has
     * no effect.
     */
    void enableGui();

    /**
     * Disables the GUI rendering. This causes the user interface
     * to no longer be rendered or updated.
     */
    void disableGui();

    /**
     * Gets the current scale factor applied to the UI.
     *
     * @return The scale factor.
     */
    float getGuiScaleFactor();

    /**
     * Maximizes the interface window.
     *
     * @deprecated The controls window is part of the old UI. Use {@link #expandUIPane(String)} instead.
     */
    @Deprecated
    void maximizeInterfaceWindow();

    /**
     * Minimizes the interface window.
     *
     * @deprecated The controls window is part of the old UI. Use {@link #collapseUIPane(String)} instead.
     */
    @Deprecated
    void minimizeInterfaceWindow();

    /**
     * Expands the UI pane with the given name. Possible names are:
     *
     * <ul>
     *     <li>Time</li>
     *     <li>Camera</li>
     *     <li>Visibility</li>
     *     <li>VisualSettings</li>
     *     <li>Datasets</li>
     *     <li>Bookmarks</li>
     *     <li>LocationLog</li>
     * </ul>
     * <p>
     * Please, mind the case!
     */
    void expandUIPane(String panelName);

    /**
     * @deprecated Use {@link #expandUIPane(String)}.
     */
    @Deprecated
    void expandGuiComponent(String name);

    /**
     * Collapses the UI pane with the given name. Possible names are:
     *
     * <ul>
     *     <li>Time</li>
     *     <li>Camera</li>
     *     <li>Visibility</li>
     *     <li>VisualSettings</li>
     *     <li>Datasets</li>
     *     <li>Bookmarks</li>
     *     <li>LocationLog</li>
     * </ul>
     * <p>
     * Please, mind the case!
     */
    void collapseUIPane(String panelName);

    /**
     * @deprecated Use {@link #collapseUIPane(String)}.
     */
    @Deprecated
    void collapseGuiComponent(String name);

    /**
     * Moves the interface window to a new position.
     *
     * @param x The new x coordinate of the new top-left corner of the window,
     *          in [0,1] from left to right.
     * @param y The new y coordinate of the new top-left corner of the window,
     *          in [0,1] from bottom to top.
     *
     * @deprecated The controls window is now deprecated in favour of the new UI.
     */
    @Deprecated
    void setGuiPosition(float x,
                        float y);

    /**
     * Blocks the execution until any kind of input (keyboard, mouse, etc.) is
     * received.
     */
    void waitForInput();

    /**
     * Blocks the execution until the Enter key is pressed.
     */
    void waitForEnter();

    /**
     * Blocks the execution until the given key or button is pressed.
     *
     * @param code The key or button code. Please see
     *             {@link com.badlogic.gdx.Input}.
     */
    void waitForInput(int code);

    /**
     * Returns the screen width in pixels.
     *
     * @return The screen width in pixels.
     */
    int getScreenWidth();

    /**
     * Returns the screen height in pixels.
     *
     * @return The screen height in pixels.
     */
    int getScreenHeight();

    /**
     * Returns the size and position of the GUI element that goes by the given
     * name or null if such element does not exist. <strong>Warning> This will
     * only work in asynchronous mode.</strong>
     *
     * @param name The name of the gui element.
     *
     * @return A vector of floats with the position (0, 1) of the bottom left
     * corner in pixels from the bottom-left of the screen and the size
     * (2, 3) in pixels of the element.
     */
    float[] getPositionAndSizeGui(String name);

    /**
     * Returns a string with the version number, build string, system, builder and build time.
     *
     * @return A string with the full version information.
     */
    String getVersion();

    /**
     * Returns the version number string.
     *
     * @return The version number string.
     */
    String getVersionNumber();

    /**
     * Returns the build string.
     *
     * @return The build string.
     */
    String getBuildString();

    /**
     * Blocks the script until the focus is the object indicated by the name.
     * There is an optional time out.
     *
     * @param name      The name of the focus to wait for.
     * @param timeoutMs Timeout in ms to wait. Set negative to disable timeout.
     *
     * @return True if the timeout ran out. False otherwise.
     */
    boolean waitFocus(String name,
                      long timeoutMs);

    /**
     * Sets the target frame rate of the camcorder. This artificially sets the frame rate (the frame time) of Gaia
     * Sky to this value while the camera is recording and playing. Make sure to use the right FPS setting during
     * playback!
     *
     * @param targetFps The target frame rate for the camcorder.
     */
    void setCamcorderFps(double targetFps);

    /**
     * @deprecated Use {@link #setCamcorderFps(double)} instead.
     */
    @Deprecated
    void setCameraRecorderFps(double targetFps);

    /**
     * Gets the current frame rate setting of the camcorder.
     *
     * @return The FPS setting of the camcorder.
     */
    double getCamcorderFps();

    /**
     * Starts recording the camera path to an auto-generated file in the default
     * camera directory. This command has no
     * effect if the camera is already being recorded.
     */
    void startRecordingCameraPath();

    /**
     * Starts recording a camera path with the given filename. The filename
     * is without extension or path. The final path with the camera file, after
     * invoking {@link #stopRecordingCameraPath()}, is:
     * <br /><br />
     * <code>{@link #getDefaultCameraDir()} + "/" + filename + ".gsc"</code>
     * <br /><br />
     * This command has no effect if the camera is already being recorded.
     */
    void startRecordingCameraPath(String fileName);

    /**
     * Stops the current camera recording. This command has no effect if the
     * camera was not being recorded.
     */
    void stopRecordingCameraPath();

    /**
     * Runs the camera recording file with the given path. Does not
     * wait for the camera file to finish playing.
     *
     * @param file The path of the camera file. Path is relative to the application's root directory or absolute.
     */
    void runCameraRecording(String file);

    /**
     * Runs a .gsc camera path file and returns immediately. This
     * function does not wait for the camera file to finish playing.
     *
     * @param file The path to the camera file. Path is relative to the application's root directory or absolute.
     */
    void runCameraPath(String file);

    /**
     * Alias for {@link #runCameraPath(String)}
     */
    void playCameraPath(String file);

    /**
     * Runs a .gsc camera path file and returns immediately. This
     * function accepts a boolean indicating whether to wait for the
     * camera path file to finish or not.
     *
     * @param file The path to the camera file. Path is relative to the application's root directory or absolute.
     * @param sync If true, the call is synchronous and waits for the camera
     *             file to finish. Otherwise, it returns immediately.
     */
    void runCameraPath(String file,
                       boolean sync);

    /**
     * Alias for {@link #runCameraPath(String, boolean)}
     */
    void playCameraPath(String file,
                        boolean sync);

    /**
     * <p>
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. This function waits for the transition to finish and then returns control
     * to the script.
     * </p>
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos  The target camera position in the internal reference system.
     * @param camDir  The target camera direction in the internal reference system.
     * @param camUp   The target camera up in the internal reference system.
     * @param seconds The duration of the transition in seconds.
     */
    void cameraTransition(double[] camPos,
                          double[] camDir,
                          double[] camUp,
                          double seconds);

    /**
     * <p>
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. This function waits for the transition to finish and then returns control
     * to the script.
     * <p>
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos  The target camera position in the internal reference system and the given distance units.
     * @param units   The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     * @param camDir  The target camera direction in the internal reference system.
     * @param camUp   The target camera up in the internal reference system.
     * @param seconds The duration of the transition in seconds.
     */
    void cameraTransition(double[] camPos,
                          String units,
                          double[] camDir,
                          double[] camUp,
                          double seconds);

    /**
     * Same as {@link #cameraTransition(double[], double[], double[], double)} but the
     * camera position is given in Km.
     *
     * @param camPos  The target camera position in Km.
     * @param camDir  The target camera direction vector.
     * @param camUp   The target camera up vector.
     * @param seconds The duration of the transition in seconds.
     */
    void cameraTransitionKm(double[] camPos,
                            double[] camDir,
                            double[] camUp,
                            double seconds);

    /**
     * <p>
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. Optionally, the transition may be run synchronously or asynchronously to the
     * current script.
     * </p>
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos  The target camera position in the internal reference system.
     * @param camDir  The target camera direction in the internal reference system.
     * @param camUp   The target camera up in the internal reference system.
     * @param seconds The duration of the transition in seconds.
     * @param sync    If true, the call waits for the transition to finish before returning, otherwise it returns
     *                immediately.
     */
    void cameraTransition(double[] camPos,
                          double[] camDir,
                          double[] camUp,
                          double seconds,
                          boolean sync);

    /**
     * <p>
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. Optionally, the transition may be run synchronously or asynchronously to the
     * current script.
     * </p>
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos  The target camera position in the internal reference system and the given distance units.
     * @param units   The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     * @param camDir  The target camera direction in the internal reference system.
     * @param camUp   The target camera up in the internal reference system.
     * @param seconds The duration of the transition in seconds.
     * @param sync    If true, the call waits for the transition to finish before returning, otherwise it returns
     *                immediately.
     */
    void cameraTransition(double[] camPos,
                          String units,
                          double[] camDir,
                          double[] camUp,
                          double seconds,
                          boolean sync);

    /**
     * <p>
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds.
     * </p>
     * <p>
     * This function accepts smoothing types and factors for the position and orientation.
     * </p>
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos                     The target camera position in the internal reference system and the given
     *                                   distance units.
     * @param camDir                     The target camera direction in the internal reference system.
     * @param camUp                      The target camera up in the internal reference system.
     * @param positionDurationSeconds    The duration of the transition in position, in seconds.
     * @param positionSmoothType         The function type to use for the smoothing of positions. Either "logit",
     *                                   "logisticsigmoid" or "none".
     *                                   <ul>
     *                                   <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                                   an effect, otherwise, linear interpolation is used.</li>
     *                                   <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                                   0.09 and 0.01.</li>
     *                                   <li>"none": no smoothing is applied.</li>
     *                                   </ul>
     * @param positionSmoothFactor       Smooth factor for the positions (depends on type).
     * @param orientationDurationSeconds The duration of the transition in orientation, in seconds.
     * @param orientationSmoothType      The function type to use for the smoothing of orientations. Either "logit",
     *                                   "logisticsigmoid" or "none".
     *                                   <ul>
     *                                   <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                                   an effect, otherwise, linear interpolation is used.</li>
     *                                   <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                                   0.09 and 0.01.</li>
     *                                   <li>"none": no smoothing is applied.</li>
     *                                   </ul>
     * @param orientationSmoothFactor    Smooth factor for the orientations (depends on type).
     */
    void cameraTransition(double[] camPos,
                          double[] camDir,
                          double[] camUp,
                          double positionDurationSeconds,
                          String positionSmoothType,
                          double positionSmoothFactor,
                          double orientationDurationSeconds,
                          String orientationSmoothType,
                          double orientationSmoothFactor);

    /**
     * <p>
     * Creates a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds.
     * </p>
     * <p>
     * This function accepts smoothing types and factors for the position and orientation.
     * </p>
     * <p>
     * Optionally, this call may return immediately (async) or it may wait for the transition to finish (sync).
     * </p>
     * <p>
     * This function puts the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos                     The target camera position in the internal reference system and the given
     *                                   distance units.
     * @param units                      The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     * @param camDir                     The target camera direction in the internal reference system.
     * @param camUp                      The target camera up in the internal reference system.
     * @param positionDurationSeconds    The duration of the transition in position, in seconds.
     * @param positionSmoothType         The function type to use for the smoothing of positions. Either "logit",
     *                                   "logisticsigmoid" or "none".
     *                                   <ul>
     *                                   <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                                   an effect, otherwise, linear interpolation is used.</li>
     *                                   <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                                   0.09 and 0.01.</li>
     *                                   <li>"none": no smoothing is applied.</li>
     *                                   </ul>
     * @param positionSmoothFactor       Smooth factor for the positions (depends on type).
     * @param orientationDurationSeconds The duration of the transition in orientation, in seconds.
     * @param orientationSmoothType      The function type to use for the smoothing of orientations. Either "logit",
     *                                   "logisticsigmoid" or "none".
     *                                   <ul>
     *                                   <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                                   an effect, otherwise, linear interpolation is used.</li>
     *                                   <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                                   0.09 and 0.01.</li>
     *                                   <li>"none": no smoothing is applied.</li>
     *                                   </ul>
     * @param orientationSmoothFactor    Smooth factor for the orientations (depends on type).
     * @param sync                       If true, the call waits for the transition to finish before returning,
     *                                   otherwise it returns immediately.
     */
    void cameraTransition(double[] camPos,
                          String units,
                          double[] camDir,
                          double[] camUp,
                          double positionDurationSeconds,
                          String positionSmoothType,
                          double positionSmoothFactor,
                          double orientationDurationSeconds,
                          String orientationSmoothType,
                          double orientationSmoothFactor,
                          boolean sync);

    /**
     * <p>
     * Creates a smooth transition from the current camera position to the given camera position in
     * the given number of seconds.
     * </p>
     * <p>
     * This function accepts smoothing type and factor.
     * </p>
     * <p>
     * Optionally, this call may return immediately (async) or it may wait for the transition to finish (sync).
     * </p>
     * <p>
     * This function puts the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camPos          The target camera position in the internal reference system and the given
     *                        distance units.
     * @param units           The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     * @param durationSeconds The duration of the transition in position, in seconds.
     * @param smoothType      The function type to use for the smoothing of positions. Either "logit",
     *                        "logisticsigmoid" or "none".
     *                        <ul>
     *                        <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                        an effect, otherwise, linear interpolation is used.</li>
     *                        <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                        0.09 and 0.01.</li>
     *                        <li>"none": no smoothing is applied.</li>
     *                        </ul>
     * @param smoothFactor    Smooth factor for the positions (depends on type).
     * @param sync            If true, the call waits for the transition to finish before returning,
     *                        otherwise it returns immediately.
     */
    void cameraPositionTransition(double[] camPos,
                                  String units,
                                  double durationSeconds,
                                  String smoothType,
                                  double smoothFactor,
                                  boolean sync);

    /**
     * <p>
     * Creates a smooth transition from the current camera orientation to the given camera orientation {camDir, camUp}
     * in
     * the given number of seconds.
     * </p>
     * <p>
     * This function accepts smoothing type and factor.
     * </p>
     * <p>
     * Optionally, this call may return immediately (async) or it may wait for the transition to finish (sync).
     * </p>
     * <p>
     * This function puts the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     * </p>
     *
     * @param camDir          The target camera direction in the internal reference system.
     * @param camUp           The target camera up in the internal reference system.
     * @param durationSeconds The duration of the transition in orientation, in seconds.
     * @param smoothType      The function type to use for the smoothing of orientations. Either "logit",
     *                        "logisticsigmoid" or "none".
     *                        <ul>
     *                        <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                        an effect, otherwise, linear interpolation is used.</li>
     *                        <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                        0.09 and 0.01.</li>
     *                        <li>"none": no smoothing is applied.</li>
     *                        </ul>
     * @param smoothFactor    Smooth factor for the orientations (depends on type).
     * @param sync            If true, the call waits for the transition to finish before returning,
     *                        otherwise it returns immediately.
     */
    void cameraOrientationTransition(double[] camDir,
                                     double[] camUp,
                                     double durationSeconds,
                                     String smoothType,
                                     double smoothFactor,
                                     boolean sync);

    /**
     * Creates a time transition from the current time to the given time (year, month, day, hour, minute, second,
     * millisecond). The time is given in UTC.
     *
     * @param year            The year to represent.
     * @param month           The month-of-year to represent, from 1 (January) to 12
     *                        (December).
     * @param day             The day-of-month to represent, from 1 to 31.
     * @param hour            The hour-of-day to represent, from 0 to 23.
     * @param min             The minute-of-hour to represent, from 0 to 59.
     * @param sec             The second-of-minute to represent, from 0 to 59.
     * @param milliseconds    The millisecond-of-second, from 0 to 999.
     * @param durationSeconds The duration of the transition, in seconds.
     * @param smoothType      The function type to use for smoothing. Either "logit", "logisticsigmoid" or "none".
     *                        <ul>
     *                        <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                        an effect, otherwise, linear interpolation is used.</li>
     *                        <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                        0.09 and 0.01.</li>
     *                        <li>"none": no smoothing is applied.</li>
     *                        </ul>
     * @param smoothFactor    Smoothing factor (depends on type, see #smoothType).
     * @param sync            If true, the call waits for the transition to finish before returning,
     *                        otherwise it returns immediately.
     */
    void timeTransition(int year,
                        int month,
                        int day,
                        int hour,
                        int min,
                        int sec,
                        int milliseconds,
                        double durationSeconds,
                        String smoothType,
                        double smoothFactor,
                        boolean sync);

    /**
     * Sleeps for the given number of seconds in the application time (FPS), so
     * if we are capturing frames and the frame rate is set to 30 FPS, the
     * command sleep(1) will put the script to sleep for 30 frames.
     *
     * @param seconds The number of seconds to wait.
     */
    void sleep(float seconds);

    /**
     * Sleeps for a number of frames. The frame monitor is notified at the beginning
     * of each frame, before the update-render cycle. When frames is 1, this method
     * returns just before the processing of the next frame starts.
     *
     * @param frames The number of frames to wait.
     */
    void sleepFrames(long frames);

    /**
     * Converts galactic coordinates to the internal cartesian coordinate
     * system.
     *
     * @param l The galactic longitude in degrees.
     * @param b The galactic latitude in degrees.
     * @param r The distance in Km.
     *
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     * internal reference system, in internal units.
     */
    double[] galacticToInternalCartesian(double l,
                                         double b,
                                         double r);

    /**
     * Converts ecliptic coordinates to the internal cartesian coordinate
     * system.
     *
     * @param l The ecliptic longitude in degrees.
     * @param b The ecliptic latitude in degrees.
     * @param r The distance in Km.
     *
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     * internal reference system, in internal units.
     */
    double[] eclipticToInternalCartesian(double l,
                                         double b,
                                         double r);

    /**
     * Converts equatorial coordinates to the internal cartesian coordinate
     * system.
     *
     * @param ra  The right ascension in degrees.
     * @param dec The declination in degrees.
     * @param r   The distance in Km.
     *
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     * internal reference system, in internal units.
     */
    double[] equatorialToInternalCartesian(double ra,
                                           double dec,
                                           double r);

    /**
     * Converts internal cartesian coordinates to equatorial
     * <code>[ra, dec, distance]</code> coordinates.
     *
     * @param x The x component, in any distance units.
     * @param y The y component, in any distance units.
     * @param z The z component, in any distance units.
     *
     * @return An array of doubles containing <code>[ra, dec, distance]</code>
     * with <code>ra</code> and <code>dec</code> in degrees and
     * <code>distance</code> in the same distance units as the input
     * position.
     */
    double[] internalCartesianToEquatorial(double x,
                                           double y,
                                           double z);

    /**
     * Converts regular cartesian coordinates, where XY is the equatorial plane, with X pointing to
     * the vernal equinox (ra=0) and Y points to ra=90, and Z pointing to the celestial north pole (dec=90)
     * to internal cartesian coordinates with internal units.
     *
     * @param eq       Equatorial cartesian coordinates (X->[ra=0,dec=0], Y->[ra=90,dec=0], Z->[ra=0,dec=90])
     * @param kmFactor Factor used to bring the input coordinate units to Kilometers, so that <code>eq * factor =
     *                 Km</code>
     *
     * @return Internal coordinates ready to be fed in other scripting functions
     */
    double[] equatorialCartesianToInternalCartesian(double[] eq,
                                                    double kmFactor);

    /**
     * Converts equatorial cartesian coordinates (in the internal reference system)
     * to galactic cartesian coordinates.
     *
     * @param eq Vector with [x, y, z] equatorial cartesian coordinates
     *
     * @return Vector with [x, y, z] galactic cartesian coordinates
     */
    double[] equatorialToGalactic(double[] eq);

    /**
     * Converts equatorial cartesian coordinates (in the internal reference system)
     * to ecliptic cartesian coordinates.
     *
     * @param eqInternal Vector with [x, y, z] equatorial cartesian coordinates
     *
     * @return Vector with [x, y, z] ecliptic cartesian coordinates
     */
    double[] equatorialToEcliptic(double[] eqInternal);

    /**
     * Converts galactic cartesian coordinates (in the internal reference system)
     * to equatorial cartesian coordinates.
     *
     * @param galInternal Vector with [x, y, z] galactic cartesian coordinates
     *
     * @return Vector with [x, y, z] equatorial cartesian coordinates
     */
    double[] galacticToEquatorial(double[] galInternal);

    /**
     * Converts ecliptic cartesian coordinates (in the internal reference system)
     * to equatorial cartesian coordinates.
     *
     * @param eclInternal Vector with [x, y, z] ecliptic cartesian coordinates
     *
     * @return Vector with [x, y, z] equatorial cartesian coordinates
     */
    double[] eclipticToEquatorial(double[] eclInternal);

    /**
     * Sets the brightness level of the render system.
     *
     * @param level The brightness level as a double precision floating point
     *              number in [-1,1]. The neutral value is 0.0.
     */
    void setBrightnessLevel(double level);

    /**
     * Sets the contrast level of the render system.
     *
     * @param level The contrast level as a double precision floating point number
     *              in [0,2]. The neutral value is 1.0.
     */
    void setContrastLevel(double level);

    /**
     * Sets the hue level of the render system.
     *
     * @param level The hue level as a double precision floating point number
     *              in [0,2]. The neutral value is 1.0.
     */
    void setHueLevel(double level);

    /**
     * Sets the saturation level of the render system.
     *
     * @param level The saturation level as a double precision floating point number
     *              in [0,2]. The neutral value is 1.0.
     */
    void setSaturationLevel(double level);

    /**
     * Sets the gamma correction level.
     *
     * @param level The gamma correction level in [0,3] as a floating point number.
     *              The neutral value is 1.2.
     */
    void setGammaCorrectionLevel(double level);

    /**
     * Sets the high dynamic range tone mapping algorithm type. The types can be:
     * <ul>
     *     <li>"auto" - performs an automatic HDR tone mapping based on the current luminosity of the scene</li>
     *     <li>"exposure" - performs an exposure-based HDR tone mapping. The exposure value must be set with {@link #setExposureToneMappingLevel(double)}</li>
     *     <li>"aces" - performs the ACES tone mapping</li>
     *     <li>"uncharted" - performs the tone mapping implemented in Uncharted</li>
     *     <li>"filmic" - performs a filmic tone mapping</li>
     *     <li>"none" - no HDR tone mapping</li>
     * </ul>
     *
     * @param type The HDR tone mapping type. One of ["auto"|"exposure"|"aces"|"uncharted"|"filmic"|"none"].
     */
    void setHDRToneMappingType(String type);

    /**
     * Sets the exposure level.
     *
     * @param level The exposure level in [0,n]. Set to 0 to disable exposure tone mapping.
     */
    void setExposureToneMappingLevel(double level);

    /**
     * Enables or disables the planetarium mode.
     *
     * @param state The boolean state. True to activate, false to deactivate.
     */
    void setPlanetariumMode(boolean state);

    /**
     * Enables and disables the cubemap mode.
     *
     * @param state      The boolean state. True to activate, false to deactivate.
     * @param projection The projection as a string.
     */
    void setCubemapMode(boolean state,
                        String projection);

    /**
     * Enables or disables the panorama mode.
     *
     * @param state The boolean state. True to activate, false to deactivate.
     */
    void setPanoramaMode(boolean state);

    /**
     * Sets the resolution (width and height are the same) of each side of the
     * frame buffers used to capture each of the 6 directions that go into the
     * cubemap to construct the equirectangular image for the 360 mode. This
     * should roughly be 1/3 of the output resolution at which the 360 mode are
     * to be captured (or screen resolution).
     *
     * @param resolution The resolution of each of the sides of the cubemap for the 360
     *                   mode.
     */
    void setCubemapResolution(int resolution);

    /**
     * Sets the cubemap projection to use.
     * Accepted values are:
     * <ul>
     *     <li>"equirectangular" - spherical projection.</li>
     *     <li>"cylindrical" - cylindrical projection.</li>
     *     <li>"hammer" - Hammer projection.</li>
     *     <li>"orthographic" - orthographic projection, with the two hemispheres side-by-side.</li>
     *     <li>"orthosphere" - orthographic projection, with the two hemispheres overlaid. That gives an outside view of the camera's celestial sphere. </li>
     *     <li>"orthosphere_crosseye" - same as orthosphere, but duplicated to produce a stereoscopic cross-eye image (side by side). </li>
     *     <li>"azimuthal_equidistant" - azimuthal equidistant projection, used in Planetarium mode.</li>
     * </ul>
     * See {@link CubmeapProjectionEffect} for possible
     * values.
     *
     * @param projection The projection, in
     *                   ["equirectangular"|"cylindrical"|"hammer"|"orthographic"|"orthosphere"|"orthosphere_crossye"|"azimuthal_equidistant"].
     */
    void setCubemapProjection(String projection);

    /**
     * Enables or disables the orthosphere view mode.
     *
     * @param state The state, true to activate and false to deactivate.
     */
    void setOrthosphereViewMode(boolean state);

    /**
     * Sets index of refraction of celestial sphere in orthosphere view mode.
     *
     * @param ior The index of refraction.
     */
    void setIndexOfRefraction(float ior);

    /**
     * Enables and disables the stereoscopic mode.
     *
     * @param state The boolean state. True to activate, false to deactivate.
     */
    void setStereoscopicMode(boolean state);

    /**
     * Changes the stereoscopic profile.
     *
     * @param index The index of the new profile:
     *              <ul>
     *              <li>0 - VR_HEADSET</li>
     *              <li>1 - HD_3DTV</li>
     *              <li>2 - CROSSEYE</li>
     *              <li>3 - PARALLEL_VIEW</li>
     *              <li>4 - ANAGLYPHIC (red-cyan)</li>
     *              </ul>
     */
    void setStereoscopicProfile(int index);

    /**
     * Sets the re-projection mode. Possible modes are:
     * <ul>
     *     <li>"disabled"</li>
     *     <li>"default"</li>
     *     <li>"accurate"</li>
     *     <li>"stereographic_screen"</li>
     *     <li>"stereographic_long"</li>
     *     <li>"stereographic_short"</li>
     *     <li>"stereographic_180"</li>
     *     <li>"lambert_screen"</li>
     *     <li>"lambert_long"</li>
     *     <li>"lambert_short"</li>
     *     <li>"lambert_180"</li>
     *     <li>"orthographic_screen"</li>
     *     <li>"orthographic_long"</li>
     *     <li>"orthographic_short"</li>
     *     <li>"orthographic_180"</li>
     * </ul>
     *
     * @param mode The re-projection mode, as a string.
     */
    void setReprojectionMode(String mode);

    /**
     * Sets the scaling factor for the back-buffer.
     *
     * @param scale The back-buffer scaling factor.
     */
    void setBackBufferScale(float scale);

    /**
     * Gets the current frame number. Useful for timing actions in scripts.
     *
     * @return The current frame number.
     */
    long getCurrentFrameNumber();

    /**
     * Enables or disables the lens flare effect.
     *
     * @param state Activate (true) or deactivate (false).
     */
    void setLensFlare(boolean state);

    /**
     * Sets the strength of the lens flare effect, in [0,1].
     * Set to 0 to disable the effect.
     *
     * @param strength The strength or intensity of the lens flare, in [0,1].
     */
    void setLensFlare(double strength);

    /**
     * Enables or disables the camera motion blur effect.
     *
     * @param state Activate (true) or deactivate (false).
     */
    void setMotionBlur(boolean state);

    /**
     * Enables or disables the camera motion blur effect.
     *
     * @param strength The strength of camera the motion blur effect, in [{@link Constants#MOTIONBLUR_MIN}, {@link Constants#MOTIONBLUR_MAX}].
     */
    void setMotionBlur(double strength);

    /**
     * Enables or disables the stars glowing over objects.
     *
     * @param state Activate (true) or deactivate (false).
     *
     * @deprecated Use {@link #setStarGlowOverObjects(boolean)} instead.
     */
    @Deprecated
    void setStarGlow(boolean state);

    /**
     * Enables or disables stars' light glowing and spilling over closer objects.
     *
     * @param state Enable (true) or disable (false).
     */
    void setStarGlowOverObjects(boolean state);

    /**
     * Sets the strength value for the bloom effect.
     *
     * @param value Bloom strength between 0 and 100. Set to 0 to deactivate the
     *              bloom.
     */
    void setBloom(float value);

    /**
     * Sets the amount of chromatic aberration. Set to 0 to disable the effect.
     *
     * @param value Chromatic aberration amount in [0,0.05].
     */
    void setChromaticAberration(float value);

    /**
     * Sets the value of smooth lod transitions, allowing or disallowing octant fade-ins of
     * as they come into view.
     *
     * @param value Activate (true) or deactivate (false).
     */
    void setSmoothLodTransitions(boolean value);

    /**
     * Resets to zero the image sequence number used to generate the file names of the
     * frame output images.
     */
    void resetImageSequenceNumber();

    /**
     * Gets the absolute path of the default directory where the still frames are saved.
     *
     * @return Absolute path of directory where still frames are saved.
     */
    String getDefaultFramesDir();

    /**
     * Gets the absolute path of the default directory where the screenshots are saved.
     *
     * @return Absolute path of directory where screenshots are saved.
     */
    String getDefaultScreenshotsDir();

    /**
     * Gets the absolute path of the default directory where the camera files are saved.
     *
     * @return Absolute path of directory where camera files are saved.
     */
    String getDefaultCameraDir();

    /**
     * Gets the absolute path to the location of the music files
     *
     * @return Absolute path to the location of the music files
     */
    String getDefaultMusicDir();

    /**
     * Gets the absolute path to the location of the inputListener mappings.
     *
     * @return Absolute path to the location of the inputListener mappings.
     */
    String getDefaultMappingsDir();

    /**
     * Gets the absolute path of the local data directory, configured in your <code>config.yaml</code> file.
     *
     * @return Absolute path to the location of the data files.
     */
    String getDataDir();

    /**
     * Gets the absolute path to the location of the configuration directory
     *
     * @return Absolute path of config directory.
     */
    String getConfigDir();

    /**
     * Returns the default data directory. That is ~/.gaiasky/ in Windows and macOS, and ~/.local/share/gaiasky
     * in Linux.
     *
     * @return Absolute path of data directory.
     */
    String getLocalDataDir();

    /**
     * Posts a {@link Runnable} to the main loop thread that runs once after the update-scene stage, and
     * before the render stage.
     *
     * @param runnable The runnable to run.
     */
    void postRunnable(Runnable runnable);

    /**
     * See {@link #parkSceneRunnable(String, Runnable)}.
     */
    void parkRunnable(String id,
                      Runnable runnable);

    /**
     * <p>
     * Parks an update {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes or it is unparked by {@link #unparkRunnable(String)}.
     * This object runs after the update-scene stage and before the render stage,
     * so it is intended for updating scene objects.
     * </p>
     * <p>
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #unparkRunnable(String)} if needed.
     * </p>
     *
     * @param id       The string id to identify the runnable.
     * @param runnable The scene update runnable to park.
     */
    void parkSceneRunnable(String id,
                           Runnable runnable);

    /**
     * <p>
     * Parks a camera update {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes or it is unparked by {@link #unparkRunnable(String)}.
     * This object runs after the update-camera stage and before the update-scene, so it is intended for updating the
     * camera only.
     * </p>
     * <p>
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #unparkRunnable(String)} if needed.
     * </p>
     *
     * @param id       The string id to identify the runnable.
     * @param runnable The camera update runnable to park.
     */
    void parkCameraRunnable(String id,
                            Runnable runnable);

    /**
     * Removes the runnable with the given id, if any. Use this method to remove previously parked scene and camera
     * runnables.
     *
     * @param id The id of the runnable to remove.
     */
    void removeRunnable(String id);

    /**
     * Removes the runnable with the given id, if any.
     *
     * @param id The id of the runnable to remove.
     *
     * @deprecated Use {@link #removeRunnable(String)}.
     */
    @Deprecated
    void unparkRunnable(String id);

    /**
     * Loads a VOTable, FITS, CSV or JSON dataset file with the given name.
     * In this version, the loading happens synchronously, so the catalog is available to Gaia Sky immediately after
     * this call returns.
     * The actual loading process is carried out
     * making educated guesses about semantics using UCDs and column names.
     * Please check <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     *
     * @param dsName The name of the dataset, used to identify the subsequent operations on the
     *               dataset.
     * @param path   Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code> file to load.
     *
     * @return False if the dataset could not be loaded, true otherwise.
     */
    boolean loadDataset(String dsName,
                        String path);

    /**
     * Loads a VOTable, FITS, CSV or JSON dataset file with the given name.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call acts exactly like
     * {@link #loadDataset(String, String)}.<br/>
     * If <code>sync</code> is false, the loading happens
     * in a new thread and the call returns immediately. In this case, you can use
     * {@link #hasDataset(String)}
     * to check whether the dataset is already loaded and available.
     * The actual loading process is carried out making educated guesses about semantics using UCDs and column names.
     * Please check <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     *
     * @param dsName The name of the dataset, used to identify the subsequent operations on the
     *               dataset.
     * @param path   Absolute path (or relative to the working path of Gaia Sky) to the file to load.
     * @param sync   Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadDataset(final String dsName,
                        final String path,
                        final boolean sync);

    /**
     * Loads a VOTable, FITS, CSV or JSON dataset file with the given name.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call acts exactly like
     * {@link #loadDataset(String, String, boolean)}.<br/>
     * If <code>sync</code> is false, the loading happens
     * in a new thread and the call returns immediately. In this case, you can use
     * {@link #hasDataset(String)}
     * to check whether the dataset is already loaded and available.
     * The actual loading process is carried out making educated guesses about semantics using UCDs and column names.
     * Please check <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     * This version includes the catalog info type.
     *
     * @param dsName  The name of the dataset, used to identify the subsequent operations on the
     *                dataset.
     * @param path    Absolute path (or relative to the working path of Gaia Sky) to the file to load.
     * @param type    The {@link CatalogInfoSource} object to use as the dataset type.
     * @param options The {@link DatasetOptions} object holding the options for this dataset.
     * @param sync    Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadDataset(final String dsName,
                        final String path,
                        final CatalogInfoSource type,
                        final DatasetOptions options,
                        final boolean sync);

    /**
     * Loads a star dataset from a VOTable, a CSV or a FITS file.
     * The dataset does not have a label.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName The name of the dataset.
     * @param path   Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *               <code>.csv</code> or <code>.fits</code> file to load.
     * @param sync   Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarDataset(String dsName,
                            String path,
                            boolean sync);

    /**
     * Loads a star dataset from a VOTable, a CSV or a FITS file.
     * The dataset does not have a label.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the star magnitudes, as in <code>appmag = appmag -
     *                       magnitudeScale</code>.
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarDataset(String dsName,
                            String path,
                            double magnitudeScale,
                            boolean sync);

    /**
     * Loads a star dataset from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the star magnitudes, as in <code>appmag = appmag -
     *                       magnitudeScale</code>.
     * @param labelColor     The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarDataset(String dsName,
                            String path,
                            double magnitudeScale,
                            double[] labelColor,
                            boolean sync);

    /**
     * Loads a star dataset from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the star magnitudes, as in <code>appmag = appmag -
     *                       magnitudeScale</code>.
     * @param labelColor     The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fadeIn         Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut        Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarDataset(String dsName,
                            String path,
                            double magnitudeScale,
                            double[] labelColor,
                            double[] fadeIn,
                            double[] fadeOut,
                            boolean sync);

    /**
     * Loads a particle dataset (point cloud) from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param profileDecay  The profile decay of the particles as in 1 - distCentre^decay.
     * @param particleColor The base color of the particles, as an array of RGBA (red, green, blue, alpha) values in
     *                      [0,1].
     * @param colorNoise    In [0,1], the noise to apply to the color so that each particle gets a slightly different
     *                      tone. Set to 0 so that all particles get the same color.
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param particleSize  The size of the particles in pixels.
     * @param ct            The name of the component type to use like "Stars", "Galaxies", etc. (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadParticleDataset(String dsName,
                                String path,
                                double profileDecay,
                                double[] particleColor,
                                double colorNoise,
                                double[] labelColor,
                                double particleSize,
                                String ct,
                                boolean sync);

    /**
     * Loads a particle dataset (point cloud) from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param profileDecay  The profile decay of the particles as in 1 - distCentre^decay.
     * @param particleColor The base color of the particles, as an array of RGBA (red, green, blue, alpha) values in
     *                      [0,1].
     * @param colorNoise    In [0,1], the noise to apply to the color so that each particle gets a slightly different
     *                      tone. Set to 0 so that all particles get the same color.
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param particleSize  The size of the particles in pixels.
     * @param ct            The name of the component type to use like "Stars", "Galaxies", etc. (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadParticleDataset(String dsName,
                                String path,
                                double profileDecay,
                                double[] particleColor,
                                double colorNoise,
                                double[] labelColor,
                                double particleSize,
                                String ct,
                                double[] fadeIn,
                                double[] fadeOut,
                                boolean sync);

    /**
     * Loads a particle dataset (point cloud) from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName             The name of the dataset.
     * @param path               Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                           <code>.csv</code> or <code>.fits</code> file to load.
     * @param profileDecay       The profile decay of the particles as in 1 - distCentre^decay.
     * @param particleColor      The base color of the particles, as an array of RGBA (red, green, blue, alpha) values
     *                           in [0,1].
     * @param colorNoise         In [0,1], the noise to apply to the color so that each particle gets a slightly
     *                           different tone. Set to 0 so that all particles get the same color.
     * @param labelColor         The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param particleSize       The size of the particles in pixels.
     * @param particleSizeLimits The minimum and maximum size of the particles in pixels.
     * @param ct                 The name of the component type to use like "Stars", "Galaxies", etc. (see
     *                           {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn             Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                           camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut            Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                           camera to the Sun) of this dataset. Set to null to disable.
     * @param sync               Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadParticleDataset(String dsName,
                                String path,
                                double profileDecay,
                                double[] particleColor,
                                double colorNoise,
                                double[] labelColor,
                                double particleSize,
                                double[] particleSizeLimits,
                                String ct,
                                double[] fadeIn,
                                double[] fadeOut,
                                boolean sync);

    /**
     * Loads a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel. Uses the same color for
     * clusters and labels.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles and labels, as an array of RGBA (red, green, blue, alpha)
     *                      values in [0,1].
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarClusterDataset(String dsName,
                                   String path,
                                   double[] particleColor,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync);

    /**
     * Loads a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles, as an array of RGBA (red, green, blue, alpha) values in
     *                      [0,1].
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarClusterDataset(String dsName,
                                   String path,
                                   double[] particleColor,
                                   double[] labelColor,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync);

    /**
     * Loads a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel. Uses the same color
     * for clusters and labels.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles and labels, as an array of RGBA (red, green, blue, alpha)
     *                      values in [0,1].
     * @param ct            The name of the component type to use (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarClusterDataset(String dsName,
                                   String path,
                                   double[] particleColor,
                                   String ct,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync);

    /**
     * Loads a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles and labels, as an array of RGBA (red, green, blue, alpha)
     *                      values in [0,1].
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param ct            The name of the component type to use (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadStarClusterDataset(String dsName,
                                   String path,
                                   double[] particleColor,
                                   double[] labelColor,
                                   String ct,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync);

    /**
     * Loads a variable star dataset from a VOTable, CSV or FITS file.
     * The variable star table must have the following columns representing the light curve:
     * <ul>
     *     <li><code>g_transit_time</code>: list of times as Julian days since J2010 for each of the magnitudes</li>
     *     <li><code>g_transit_mag</code>: list of magnitudes corresponding to the times in <code>g_transit_times</code></li>
     *     <li><code>pf</code>: the period in days</li>
     * </ul>
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the magnitudes in the light curve, as in <code>appmag =
     *                       appmag - magnitudeScale</code>.
     * @param labelColor     The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fadeIn         Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut        Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     * <code>sync</code> is false.
     */
    boolean loadVariableStarDataset(String dsName,
                                    String path,
                                    double magnitudeScale,
                                    double[] labelColor,
                                    double[] fadeIn,
                                    double[] fadeOut,
                                    boolean sync);

    /**
     * Loads a Gaia Sky JSON dataset file asynchronously. The call returns immediately, and the
     * dataset becomes available when it finished loading.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean loadJsonCatalog(String dsName,
                            String path);

    /**
     * Loads a JSON Gaia Sky dataset file asynchronously. The call returns immediately, and the
     * dataset becomes available when it finished loading.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean loadJsonDataset(String dsName,
                            String path);

    /**
     * Loads a Gaia Sky JSON dataset file in a synchronous or asynchronous manner.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     * @param sync   If true, the call does not return until the dataset is loaded and available in Gaia Sky.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean loadJsonDataset(String dsName,
                            String path,
                            boolean sync);

    /**
     * Loads a Gaia Sky JSON dataset file in a synchronous or asynchronous manner.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     * @param select If true, focus the first object in the dataset after loading.
     * @param sync   If true, the call does not return until the dataset is loaded and available in Gaia Sky.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean loadJsonDataset(String dsName,
                            String path,
                            boolean select,
                            boolean sync);

    /**
     * Removes the dataset identified by the given name, if it exists.
     *
     * @param dsName The name of the dataset to remove.
     *
     * @return False if the dataset could not be found.
     */
    boolean removeDataset(String dsName);

    /**
     * Hides the dataset identified by the given name, if it exists and is not hidden.
     *
     * @param dsName The name of the dataset to hide.
     *
     * @return False if the dataset could not be found.
     */
    boolean hideDataset(String dsName);

    /**
     * Returns the names of all datasets currently loaded.
     *
     * @return A list with all the names of the loaded datasets.
     */
    List<String> listDatasets();

    /**
     * Checks whether the dataset identified by the given name is loaded
     *
     * @param dsName The name of the dataset to query.
     *
     * @return True if the dataset is loaded, false otherwise.
     */
    boolean hasDataset(String dsName);

    /**
     * Shows (un-hides) the dataset identified by the given name, if it exists and is hidden
     *
     * @param dsName The name of the dataset to show.
     *
     * @return False if the dataset could not be found.
     */
    boolean showDataset(String dsName);

    /**
     * Sets the given 4x4 matrix (in column-major order) as the transformation matrix to apply
     * to all the data points in the dataset identified by the given name.
     *
     * @param dsName The name of the dataset.
     * @param matrix The 16 values of the 4x4 transformation matrix in column-major order.
     *
     * @return True if the dataset was found and the transformation matrix could be applied. False otherwise.
     */
    boolean setDatasetTransformationMatrix(String dsName,
                                           double[] matrix);

    /**
     * Clears the transformation matrix (if any) in the dataset identified by the given name.
     *
     * @param dsName The name of the dataset.
     *
     * @return True if the dataset was found and the transformations cleared.
     */
    boolean clearDatasetTransformationMatrix(String dsName);

    /**
     * Enables or disables the dataset highlight, using a plain color given by the color index:
     * <ul>
     *     <li>0 - blue</li>
     *     <li>1 - red</li>
     *     <li>2 - yellow</li>
     *     <li>3 - green</li>
     *     <li>4 - pink</li>
     *     <li>5 - orange</li>
     *     <li>6 - purple</li>
     *     <li>7 - brown</li>
     *     <li>8 - magenta</li>
     * </ul>
     *
     * @param dsName     The dataset name.
     * @param colorIndex Color index in [0,8].
     * @param highlight  Whether to highlight or not.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlightDataset(String dsName,
                             int colorIndex,
                             boolean highlight);

    /**
     * Enables or disables the dataset highlight using a plain color chosen by the system.
     *
     * @param dsName    The dataset name.
     * @param highlight State.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlightDataset(String dsName,
                             boolean highlight);

    /**
     * Enables or disables the dataset highlight, using a given plain color.
     *
     * @param dsName    The dataset name.
     * @param r         Red component.
     * @param highlight State.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlightDataset(String dsName,
                             float r,
                             float g,
                             float b,
                             float a,
                             boolean highlight);

    /**
     * Enables or disables the dataset highlight, using the given color map on the given attribute with the given
     * maximum and minimum mapping values.
     *
     * @param dsName        The dataset name.
     * @param attributeName The attribute name. You can use basic attributes (please mind the case!):
     *                      <ul><li>RA</li><li>DEC</li><li>Distance</li><li>GalLatitude</li><li>GalLongitude</li><li>EclLatitude</li><li>EclLongitude</li></ul>
     *                       Or star-only attributes (if your dataset contains stars, mind the case!):
     *                       <ul><li>Mualpha</li><li>Mudelta</li><li>Radvel</li><li>Absmag</li><li>Appmag</li></ul>
     *                       Or even extra attributes (if you loaded the dataset yourself), matching by column name.
     * @param colorMap      The color map to use, in
     *                      ["reds"|"greens"|"blues"|"rainbow18"|"rainbow"|"seismic"|"carnation"|"hotmeal"|"cool"].
     * @param minMap        The minimum mapping value.
     * @param maxMap        The maximum mapping value.
     * @param highlight     State.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlightDataset(String dsName,
                             String attributeName,
                             String colorMap,
                             double minMap,
                             double maxMap,
                             boolean highlight);

    /**
     * Sets the size increase factor of this dataset when highlighted.
     *
     * @param dsName     The dataset name.
     * @param sizeFactor The size factor to apply to the particles when highlighted, must be in
     *                   [{@link gaiasky.util.Constants#MIN_DATASET_SIZE_FACTOR},
     *                   {@link gaiasky.util.Constants#MAX_DATASET_SIZE_FACTOR}].
     *
     * @return False if the dataset could not be found.
     */
    boolean setDatasetHighlightSizeFactor(String dsName,
                                          float sizeFactor);

    /**
     * Sets the 'all visible' property of datasets when highlighted. If set to true, all stars in the dataset have an
     * increased minimum
     * opacity when highlighted, so that they are all visible. Otherwise, stars retain their minimum opacity and base
     * brightness.
     *
     * @param dsName     The dataset name.
     * @param allVisible Whether all stars in the dataset should be visible when highlighted or not.
     *
     * @return False if the dataset could not be found.
     */
    boolean setDatasetHighlightAllVisible(String dsName,
                                          boolean allVisible);

    /**
     * Sets the dataset point size multiplier.
     *
     * @param dsName     The dataset name.
     * @param multiplier The multiplier, as a positive floating point number.
     */
    void setDatasetPointSizeMultiplier(String dsName,
                                       double multiplier);

    /**
     * Creates a shape object of the given type with the given size around the object with the given name and primitive.
     *
     * @param shapeName   The name of the shape object.
     * @param shape       The shape type, one of
     *                    <ul><li>sphere</li><li>icosphere</li><li>octahedronsphere</li><li>ring</li><li>cylinder</li><li>cone</li></ul>
     * @param primitive   The primitive to use, one of <ul><li>lines</li><li>triangles</li></ul>. Use 'lines' to create
     *                    a wireframe shape, use 'triangles' for a solid shape.
     * @param size        The size of the object in kilometers.
     * @param objectName  The name of the object to use as the position.
     * @param r           The red component of the color in [0,1].
     * @param g           The green component of the color in [0,1].
     * @param b           The blue component of the color in [0,1].
     * @param a           The alpha component of the color in [0,1].
     * @param showLabel   Whether to show a label with the name of the shape.
     * @param trackObject Whether to track the object if/when it moves.
     */
    void addShapeAroundObject(String shapeName,
                              String shape,
                              String primitive,
                              double size,
                              String objectName,
                              float r,
                              float g,
                              float b,
                              float a,
                              boolean showLabel,
                              boolean trackObject);

    /**
     * Creates a shape object of the given type with the given size around the object with the given name, primitive and
     * orientation.
     *
     * @param shapeName   The name of the shape object.
     * @param shape       The shape type, one of
     *                    <ul><li>sphere</li><li>icosphere</li><li>octahedronsphere</li><li>ring</li><li>cylinder</li><li>cone</li></ul>
     * @param primitive   The primitive to use, one of <ul><li>lines</li><li>triangles</li></ul>. Use 'lines' to create
     *                    a wireframe shape, use 'triangles' for a solid shape.
     * @param orientation The orientation to use, one of
     *                    <ul><li>camera</li><li>equatorial</li><li>ecliptic</li><li>galactic</li></ul>.
     * @param size        The size of the object in kilometers.
     * @param objectName  The name of the object to use as the position.
     * @param r           The red component of the color in [0,1].
     * @param g           The green component of the color in [0,1].
     * @param b           The blue component of the color in [0,1].
     * @param a           The alpha component of the color in [0,1].
     * @param showLabel   Whether to show a label with the name of the shape.
     * @param trackObject Whether to track the object if/when it moves.
     */
    void addShapeAroundObject(String shapeName,
                              String shape,
                              String primitive,
                              String orientation,
                              double size,
                              String objectName,
                              float r,
                              float g,
                              float b,
                              float a,
                              boolean showLabel,
                              boolean trackObject);

    /**
     * <p>Creates a backup of the current settings state that can be restored later on.
     * The settings are backed up in a stack, so multiple calls to this method put different copies of the settings
     * on the stack in a LIFO fashion.</p>
     * <p>This method, together with {@link #restoreSettings()}, are useful to back up and restore
     * the
     * settings at the beginning and end of your scripts, respectively, and ensure that the user settings are left
     * unmodified after your script ends.</p>
     */
    void backupSettings();

    /**
     * <p>Takes the settings object at the top of the settings stack and makes it effective.</p>
     * <p>This method, together with {@link #backupSettings()}, are useful to back up and restore the
     * settings at the beginning and end of your scripts, respectively, and ensure that the user settings are left
     * unmodified after your script ends.</p>
     * <p>WARN: This function applies all settings immediately, and the user interface may be re-initialized.
     * Be aware that the UI may be set to its default state after this call.</p>
     *
     * @return True if the stack was not empty and the settings were restored successfully. False otherwise.
     */
    boolean restoreSettings();

    /**
     * Clears the stack of settings objects. This will invalidate all previous calls to
     * {@link #backupSettings()},
     * effectively making the settings stack empty. Calling {@link #restoreSettings()} after this
     * method will return false.
     */
    void clearSettingsStack();

    /**
     * Forces a re-initialization of the entire user interface of Gaia Sky on the fly.
     */
    void resetUserInterface();

    /**
     * Sets the maximum simulation time allowed, in years. This sets the maximum time in the future (years)
     * and in the past (-years). This setting is not saved to the configuration and resets to 5 Myr after
     * restart.
     *
     * @param years The maximum year number to allow.
     */
    void setMaximumSimulationTime(long years);

    /**
     * Returns the column-major matrix representing the given reference system transformation.
     *
     * @param name <p>The name of the reference system transformation:</p>
     *             <ul>
     *             <li>'equatorialtoecliptic', 'eqtoecl'</li>
     *             <li>'ecliptictoequatorial', 'ecltoeq'</li>
     *             <li>'galactictoequatorial', 'galtoeq'</li>
     *             <li>'equatorialtogalactic', 'eqtogal'</li>
     *             <li>'ecliptictogalactic', 'ecltogal</li>
     *             <li>'galactictoecliptic', 'galtoecl</li>
     *             </ul>
     *
     * @return The transformation matrix in column-major order.
     */
    double[] getRefSysTransform(String name);

    /**
     * Returns the meter to internal unit conversion factor. Use this factor to multiply
     * your coordinates in meters to get them in internal units.
     *
     * @return The factor M_TO_U.
     */
    double getMeterToInternalUnitConversion();

    /**
     * Returns the internal unit to meter conversion factor. Use this factor to multiply
     * your coordinates in internal units to get them in meters.
     *
     * @return The factor U_TO_M.
     */
    double getInternalUnitToMeterConversion();

    /**
     * Converts the value in internal units to metres.
     *
     * @param internalUnits The value in internal units.
     *
     * @return The value in metres.
     */
    double internalUnitsToMetres(double internalUnits);

    /**
     * Converts the value in internal units to Kilometers.
     *
     * @param internalUnits The value in internal units.
     *
     * @return The value in Kilometers.
     */
    double internalUnitsToKilometres(double internalUnits);

    /**
     * Converts the array in internal units to Kilometers.
     *
     * @param internalUnits The array in internal units.
     *
     * @return The array in Kilometers.
     */
    double[] internalUnitsToKilometres(double[] internalUnits);

    /**
     * Converts the value in internal units to parsecs.
     *
     * @param internalUnits The value in internal units.
     *
     * @return The value in parsecs.
     */
    double internalUnitsToParsecs(double internalUnits);

    /**
     * Converts the array in internal units to parsecs.
     *
     * @param internalUnits The array in internal units.
     *
     * @return The array in parsecs.
     */
    double[] internalUnitsToParsecs(double[] internalUnits);

    /**
     * Converts the metres to internal units.
     *
     * @param metres The value in metres.
     *
     * @return The value in internal units.
     */
    double metresToInternalUnits(double metres);

    /**
     * Converts the kilometres to internal units.
     *
     * @param kilometres The value in kilometers.
     *
     * @return The value in internal units.
     */
    double kilometresToInternalUnits(double kilometres);

    /**
     * Converts the parsecs to internal units.
     *
     * @param parsecs The value in parsecs.
     *
     * @return The value in internal units.
     */
    double parsecsToInternalUnits(double parsecs);

    /**
     * Gets the current frame number. The number begins at 0 for the first frame produced
     * when Gaia Sky is started and increases continuously.
     *
     * @return The current frame number.
     */
    long getFrameNumber();

    /**
     * Rotates a 3D vector around the given axis by the specified angle in degrees.
     * Vectors are arrays with 3 components. If more components are there, they are ignored.
     *
     * @param vector Vector to rotate, with at least 3 components.
     * @param axis   The axis, with at least 3 components.
     * @param angle  Angle in degrees.
     *
     * @return The new vector, rotated.
     */
    double[] rotate3(double[] vector,
                     double[] axis,
                     double angle);

    /**
     * Rotates a 2D vector by the specified angle in degrees, counter-clockwise assuming that
     * the y axis points up.
     *
     * @param vector Vector to rotate, with at least 2 components.
     *
     * @return The new vector, rotated.
     */
    double[] rotate2(double[] vector,
                     double angle);

    /**
     * Computes the cross product between the two 3D vectors.
     *
     * @param vec1 First 3D vector.
     * @param vec2 Second 3D vector.
     *
     * @return Cross product 3D vector.
     */
    double[] cross3(double[] vec1,
                    double[] vec2);

    /**
     * Computes the dot product between the two 3D vectors.
     *
     * @param vec1 First 3D vector.
     * @param vec2 Second 3D vector.
     *
     * @return The dot product scalar.
     */
    double dot3(double[] vec1,
                double[] vec2);

    /**
     * Print text using the internal logging system.
     *
     * @param message The message.
     */
    void print(String message);

    /**
     * Print text using the internal logging system.
     *
     * @param message The message.
     */
    void log(String message);

    /**
     * Log an error using the internal logging system.
     *
     * @param message The error message.
     */
    void error(String message);

    /**
     * Initiates the quit action to terminate the program.
     */
    void quit();

}
