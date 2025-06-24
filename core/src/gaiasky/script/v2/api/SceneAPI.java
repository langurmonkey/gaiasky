/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.api.IVisibilitySwitch;
import gaiasky.scene.component.AttitudeComponent;
import gaiasky.scene.component.RigidRotation;
import gaiasky.scene.component.Verts;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.script.v2.impl.SceneModule;
import gaiasky.util.Constants;
import gaiasky.util.coord.AbstractOrbitCoordinates;
import gaiasky.util.coord.IPythonCoordinatesProvider;

/**
 * API definition for the scene module, {@link SceneModule}.
 * <p>
 * The scene module contains calls and methods to access, modify, and query the internal scene.
 */
public interface SceneAPI {

    /**
     * Get an object from the scene graph by <code>name</code> or id (HIP, TYC, Gaia SourceId).
     *
     * @param name The name or id (HIP, TYC, Gaia SourceId) of the object.
     *
     * @return The object as a {@link FocusView}, or null
     *         if it does not exist.
     */
    FocusView get_object(String name);

    /**
     * Get an object by <code>name</code> or id (HIP, TYC, Gaia SourceID), optionally waiting
     * until the object is available, with a timeout.
     *
     * @param name           The name or id (HIP, TYC, Gaia SourceId) of the object.
     * @param timeoutSeconds The timeout in seconds to wait until returning.
     *                       If negative, it waits indefinitely.
     *
     * @return The object if it exists, or null if it does not and block is false, or if block is true and
     *         the timeout has passed.
     */
    FocusView get_object(String name,
                         double timeoutSeconds);

    /**
     * Get the line object identified by the given <code>name</code>. The returned line object is of type {@link Verts}.
     * {@link Verts} represents objects that are rendered as primitives. Typically, they are either lines or points.
     *
     * @param name The name of the line object.
     *
     * @return The line object as a {@link VertsView} instance, or null
     *         if it does not exist.
     */
    VertsView get_line_object(String name);

    /**
     * Get the line object identified by the given <code>name</code>. The returned line object is of type {@link Verts}.
     * {@link Verts} represents objects that are rendered as primitives. Typically, they are either lines or points.
     * <p>
     * Sometimes, adding objects to the internal scene graph incurs in a delay of a few frames. If one calls a method to add
     * a line object, and then immediately calls this method, it may be that the object is still not available in the internal
     * scene graph. That is why this version includes a timeout time, which is the maximum time that this method will wait for
     * the object to become available. If the object becomes available before the timeout has passed, ti is returned. Otherwise,
     * this call returns null.
     *
     * @param name           The name of the line object.
     * @param timeoutSeconds The timeout, in seconds, to wait for the object to be ready.
     *                       If negative, it waits indefinitely.
     *
     * @return The line object as a {@link VertsView}, or null
     *         if it does not exist.
     */
    VertsView get_line_object(String name,
                              double timeoutSeconds);

    /**
     * Get the reference to an entity given its name.
     *
     * @param name Entity name.
     **/
    Entity get_entity(String name);

    /**
     * Get the reference to an entity given its name and a timeout in seconds.
     * If the entity is not in the scene after the timeout has passed, ths method
     * returns null.
     *
     * @param name           Entity name.
     * @param timeOutSeconds Timeout time, in seconds.
     **/
    Entity get_entity(String name, double timeOutSeconds);

    /**
     * Get a focus object from the scene given its name.
     *
     * @param name The name of the focus object.
     *
     * @return The reference to the object if it exists, null otherwise.
     */
    Entity get_focus(String name);

    /**
     * Return the star parameters given its identifier or name, if the star exists
     * and it is loaded.
     *
     * @param starId The star identifier or name.
     *
     * @return An array with (ra [deg], dec [deg], parallax [mas], pmra [mas/yr], pmdec [mas/yr], radvel [km/s], appmag
     *         [mag], red [0,1], green [0,1], blue [0,1]) if the
     *         star exists and is loaded, null otherwise.
     */
    double[] get_star_parameters(String starId);

    /**
     * Get the current position of the object identified by <code>name</code> in
     * the internal coordinate system and internal units. If the object does not exist,
     * it returns null.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return A 3-vector with the object's position in internal units and the internal reference system.
     */
    double[] get_object_position(String name);

    /**
     * Get the current position of the object identified by <code>name</code> in
     * the internal coordinate system and the requested distance units. If the object does not exist,
     * it returns null.
     *
     * @param name  The name or id (HIP, TYC, sourceId) of the object.
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     *
     * @return A 3-vector with the object's position in the requested units and internal reference system.
     */
    double[] get_object_position(String name,
                                 String units);

    /**
     * Get the predicted position of the object identified by <code>name</code> in
     * the internal coordinate system and internal units. If the object does not exist,
     * it returns null.
     * <p>
     * The predicted position is the position of the object in the next update cycle, and
     * may be useful to compute the camera state.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return A 3-vector with the object's predicted position in the internal reference system.
     */
    double[] get_object_predicted_position(String name);

    /**
     * Get the predicted position of the object identified by <code>name</code> in
     * the internal coordinate system and the requested distance units. If the object does not exist,
     * it returns null.
     * <p>
     * The predicted position is the position of the object in the next update cycle, and
     * may be useful to compute the camera state.
     *
     * @param name  The name or id (HIP, TYC, sourceId) of the object.
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     *
     * @return A 3-vector with the object's predicted position in the requested units and in the internal reference system.
     */
    double[] get_object_predicted_position(String name,
                                           String units);

    /**
     * Set the internal position of the object identified by <code>name</code>. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param name     The name of the object.
     * @param position The position in the internal reference system and internal units.
     */
    void set_object_posiiton(String name,
                             double[] position);

    /**
     * Set the internal position of the object identified by <code>name</code>. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param name     The name of the object.
     * @param position The position in the internal reference system and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_object_posiiton(String name,
                             double[] position,
                             String units);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object in a focus view wrapper.
     * @param position The position in the internal reference system and internal units.
     */
    void set_object_posiiton(FocusView object,
                             double[] position);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object in a focus view wrapper.
     * @param position The position in the internal reference system and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_object_posiiton(FocusView object,
                             double[] position,
                             String units);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object entity.
     * @param position The position in the internal reference system and internal units.
     */
    void set_object_posiiton(Entity object,
                             double[] position);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object   The object entity.
     * @param position The position in the internal reference system and the given units.
     * @param units    The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_object_posiiton(Entity object,
                             double[] position,
                             String units);

    /**
     * Set the coordinates provider for the object identified with the given name, if possible.
     * The provider object must implement {@link IPythonCoordinatesProvider}, and in particular
     * the method {@link IPythonCoordinatesProvider#getEquatorialCartesianCoordinates(Object, Object)}, which
     * takes in a julian date and outputs the object coordinates in the internal cartesian system.
     *
     * @param name     The name of the object.
     * @param provider The coordinate provider instance.
     */
    void set_object_coordinates_provider(String name,
                                         IPythonCoordinatesProvider provider);

    /**
     * Remove the current coordinates provider from the object with the given name. This method must
     * be called before shutting down the gateway if the coordinates provider has been previously set
     * for the given object from Python with {@link #set_object_coordinates_provider(String, IPythonCoordinatesProvider)}.
     * Otherwise, Gaia Sky will crash due to the missing connection to Python.
     *
     * @param name The name of the object for which to remove the coordinate provider.
     */
    void remove_object_coordinates_provider(String name);

    /**
     * Sets the component described by the given name visible or invisible.
     * <p>
     * The possible keys are listed below. They are passed in as a string:
     * <ul>
     *     <li>
     *         <code>element.stars</code>
     *     </li>
     *     <li>
     *         <code>element.planets</code>
     *     </li>
     *     <li>
     *         <code>element.moons</code>
     *     </li>
     *     <li>
     *         <code>element.satellites</code>
     *     </li>
     *     <li>
     *         <code>element.asteroids</code>
     *     </li>
     *     <li>
     *         <code>element.clusters</code>
     *     </li>
     *     <li>
     *         <code>element.milkyway</code>
     *     </li>
     *     <li>
     *         <code>element.galaxies</code>
     *     </li>
     *     <li>
     *         <code>element.nebulae</code>
     *     </li>
     *     <li>
     *         <code>element.meshes</code>
     *     </li>
     *     <li>
     *         <code>element.systems</code>
     *     </li>
     *     <li>
     *         <code>element.labels</code>
     *     </li>
     *     <li>
     *         <code>element.orbits</code>
     *     </li>
     *     <li>
     *         <code>element.locations</code>
     *     </li>
     *     <li>
     *         <code>element.countries</code>
     *     </li>
     *     <li>
     *         <code>element.ruler</code>
     *     </li>
     *     <li>
     *         <code>element.equatorial</code>
     *     </li>
     *     <li>
     *         <code>element.ecliptic</code>
     *     </li>
     *     <li>
     *         <code>element.galactic</code>
     *     </li>
     *     <li>
     *         <code>element.recursivegrid</code>
     *     </li>
     *     <li>
     *         <code>element.constellatios</code>
     *     </li>
     *     <li>
     *         <code>element.boundaries</code>
     *     </li>
     *     <li>
     *         <code>element.atmospheres</code>
     *     </li>
     *     <li>
     *         <code>element.clouds</code>
     *     </li>
     *     <li>
     *         <code>element.effects</code>
     *     </li>
     *     <li>
     *         <code>element.axes</code>
     *     </li>
     *     <li>
     *         <code>element.velocityvectors</code>
     *     </li>
     *     <li>
     *         <code>element.keyframes</code>
     *     </li>
     *     <li>
     *         <code>element.others</code>
     *     </li>
     * </ul>
     *
     * @param key     The key of the component, as a string.
     *                {@link ComponentTypes.ComponentType}.
     * @param visible The visible value.
     */
    void set_component_type_visibility(String key,
                                       boolean visible);

    /**
     * Get the visibility of the component type described by the key.
     * The possible keys are listed below. They are passed in as a string:
     * <ul>
     *     <li>
     *         <code>element.stars</code>
     *     </li>
     *     <li>
     *         <code>element.planets</code>
     *     </li>
     *     <li>
     *         <code>element.moons</code>
     *     </li>
     *     <li>
     *         <code>element.satellites</code>
     *     </li>
     *     <li>
     *         <code>element.asteroids</code>
     *     </li>
     *     <li>
     *         <code>element.clusters</code>
     *     </li>
     *     <li>
     *         <code>element.milkyway</code>
     *     </li>
     *     <li>
     *         <code>element.galaxies</code>
     *     </li>
     *     <li>
     *         <code>element.nebulae</code>
     *     </li>
     *     <li>
     *         <code>element.meshes</code>
     *     </li>
     *     <li>
     *         <code>element.systems</code>
     *     </li>
     *     <li>
     *         <code>element.labels</code>
     *     </li>
     *     <li>
     *         <code>element.orbits</code>
     *     </li>
     *     <li>
     *         <code>element.locations</code>
     *     </li>
     *     <li>
     *         <code>element.countries</code>
     *     </li>
     *     <li>
     *         <code>element.ruler</code>
     *     </li>
     *     <li>
     *         <code>element.equatorial</code>
     *     </li>
     *     <li>
     *         <code>element.ecliptic</code>
     *     </li>
     *     <li>
     *         <code>element.galactic</code>
     *     </li>
     *     <li>
     *         <code>element.recursivegrid</code>
     *     </li>
     *     <li>
     *         <code>element.constellatios</code>
     *     </li>
     *     <li>
     *         <code>element.boundaries</code>
     *     </li>
     *     <li>
     *         <code>element.atmospheres</code>
     *     </li>
     *     <li>
     *         <code>element.clouds</code>
     *     </li>
     *     <li>
     *         <code>element.effects</code>
     *     </li>
     *     <li>
     *         <code>element.axes</code>
     *     </li>
     *     <li>
     *         <code>element.velocityvectors</code>
     *     </li>
     *     <li>
     *         <code>element.keyframes</code>
     *     </li>
     *     <li>
     *         <code>element.others</code>
     *     </li>
     * </ul>
     * <p>
     * See {@link ComponentTypes.ComponentType} for more information.
     *
     * @param key The key of the component type to query.
     *
     * @return The visibility of the component type.
     */
    boolean get_component_type_visibility(String key);

    /**
     * Project the world space position of the object identified by the given <code>name</code> to screen coordinates.
     * It's the same as GLU's <code>gluProject()</code> with
     * one small deviation: The viewport is assumed to span the
     * whole screen. The screen coordinate system has its origin in the bottom left, with the y-axis pointing upwards
     * and the x-axis pointing to the right. This
     * makes it easily usable in conjunction with Batch and similar classes.
     * <p>
     * This call only works if Gaia Sky is using the simple perspective projection mode. It does not work with any of
     * the following modes:
     * <ul>
     *     <li>
     *         Panorama mode (all projections)
     *     </li>
     *     <li>
     *         Planetarium mode
     *     </li>
     *     <li>
     *         Orthosphere mode
     *     </li>
     *     <li>
     *         Stereoscopic (3D) mode
     *     </li>
     *     <li>
     *         Re-projection mode (all re-projection shaders)
     *     </li>
     * </ul>
     *
     * @param name The name of the object to get the screen coordinates for.
     *
     * @return An array with the x and y screen coordinates, in pixels, with the origin at the bottom-left. If the
     *         object with the given name does not exist, or it falls
     *         off-screen, it returns null.
     */
    double[] get_object_screen_coordinates(String name);

    /**
     * Set the visibility of a particular object. Use this method to hide individual objects.
     * Changes to the individual object visibility are not persisted on restart.
     *
     * @param name    The name of the object. Must be an instance of {@link IVisibilitySwitch}.
     * @param visible The visible status to set. Set to false in order to hide the object. True to make it visible.
     *
     * @return True if the visibility was set successfully, false if there were errors.
     */
    boolean set_object_visibility(String name,
                                  boolean visible);

    /**
     * Get the visibility of a particular object.
     *
     * @param name The name of the object. Must be an instance of {@link IVisibilitySwitch}.
     *
     * @return The visibility status of the object, if it exists.
     */
    boolean get_object_visibility(String name);

    /**
     * Set the given size scaling factor to the object identified by
     * <code>name</code>. This method will only work with model objects such as
     * planets, asteroids, satellites, etc. It will not work with orbits, stars
     * or any other object types.
     * <p>
     * Also, <strong>use this with caution</strong>, as scaling the size of
     * objects can have unintended side effects, and remember to set the scaling
     * back to 1.0 at the end of your script.
     * </p>
     *
     * @param name          The name or id of the object.
     * @param scalingFactor The scaling factor to scale the size of that object.
     */
    void set_object_size_scaling(String name,
                                 double scalingFactor);

    /**
     * Set the given orbit coordinates scaling factor to the orbit object identified by
     * <code>name</code>. The object must be attached to an orbit that extends {@link AbstractOrbitCoordinates}.
     * <p>
     * For more information, check out {@link AbstractOrbitCoordinates} and its subclasses.
     * <p>
     * Also, <strong>use this with caution</strong>, as scaling coordinates
     * may have unintended side effects, and remember to set the scaling
     * back to 1.0 at the end of your script.
     * Additionally, use either {@link #refresh_all_orbits()} or
     * {@link #refresh_object_orbit(String)} right after
     * this call in order to immediately refresh the scaled orbits.
     * </p>
     *
     * @param name          The name of the coordinates object (OrbitLintCoordinates, EclipticCoordinates, SaturnVSOP87,
     *                      UranusVSOP87, EarthVSOP87, MercuryVSOP87, ..., PlutoCoordinates,
     *                      HeliotropicOrbitCoordinates, MoonAACoordinates).
     *                      Optionally, you can append ':objectName' to select a single object. For instance, both Gaia
     *                      and JWST have
     *                      heliotropic orbit coordinates. To only select the Gaia orbit provider, use
     *                      "HeliotropicOrbitCoordinates:Gaia".
     * @param scalingFactor The scaling factor.
     */
    void set_orbit_coordinates_scaling(String name,
                                       double scalingFactor);

    /**
     * Force the refresh of the orbit of the object identified by
     * <code>name</code>. This should generally be called after a call to
     * {@link #set_orbit_coordinates_scaling(String, double)}.
     */
    void refresh_object_orbit(String name);

    /**
     * Force all orbits to refresh immediately. Some orbits need refreshing whenever the time changes. For instance, orbits
     * of objects whose positions are based on VSOP87 need to be recomputed roughly once every loop. This method
     * triggers an immediate refresh of all these orbits regardless of the position of the object in the orbit.
     */
    void refresh_all_orbits();

    /**
     * Forcefully trigger an update of the internal scene graph, and the positions of all
     * the objects. This method updates the internal state of all the entities in the scene graph.
     * <p>
     * Useful to call after operations that modify the position of objects.
     */
    void force_update_scene();

    /**
     * Get the internal radius of the object identified by the given <code>name</code>, in km.
     * <p>
     * In some objects, such as stars, the radius is not the physical length from the center to the surface, but
     * a representation of its absolute magnitude.
     *
     * @param name The name or ID (HIP, TYC, sourceId) of the object.
     *
     * @return The radius of the object in km. If the object identified by name
     *         or ID (HIP, TYC, sourceId) does not exist, it returns a negative
     *         value.
     */
    double get_object_radius(String name);

    /**
     * Set the given quaternions file (CSV with times and quaternions) as the orientation provider
     * for this object. This call removes the previous orientation model from the object (either
     * {@link RigidRotation} or {@link AttitudeComponent}.
     * <p>
     * The interpolation between quaternions is done using <b>slerp</b> (spherical linear interpolation).
     *
     * @param name The name of the object. The object must already have an <code>Orientation</code> component.
     * @param file The file path. The file is a CSV where each line has the time (ISO-8601) and the
     *             x, y, z and w components of the quaternion for that time. For instance, a valid line would
     *             be "<code>2018-04-01T15:20:15Z,0.9237,0.3728,0.0358,0.0795</code>".
     *
     * @return True if the object was found and could be updated.
     */
    boolean set_object_quaternion_slerp_orientation(String name,
                                                    String file);


    /**
     * Set the given quaternions file (CSV with times and quaternions) as the orientation provider
     * for this object. This call removes the previous orientation model from the object (either
     * {@link RigidRotation} or {@link AttitudeComponent}.
     * <p>
     * The interpolation between quaternions is done using <b>nlerp</b> (normalized linear interpolation).
     *
     * @param name The name of the object. The object must already have an <code>Orientation</code> component.
     * @param file The file path. The file is a CSV where each line has the time (ISO-8601) and the
     *             x, y, z and w components of the quaternion for that time. For instance, a valid line would
     *             be "<code>2018-04-01T15:20:15Z,0.9237,0.3728,0.0358,0.0795</code>".
     *
     * @return True if the object was found and could be updated.
     */
    boolean set_object_quaternion_nlerp_orientation(String name,
                                                    String file);

    /**
     * Set the global label size factor. This is a multiplier that applies to all object labels.
     *
     * @param factor Factor in {@link Constants#MIN_LABEL_SIZE} and {@link Constants#MAX_LABEL_SIZE}.
     */
    void set_label_size_factor(float factor);

    /**
     * Force the label of the given object to be displayed, ignoring the usual
     * solid angle-based visibility rules. The object is identified by the given <code>name</code>.
     * <p>
     * Note: This does not override the visibility of the object itself, nor the
     * visibility settings of the label component.
     * <p>
     * This setting is temporary and will not persist after a restart.
     *
     * @param name       The object name.
     * @param forceLabel Whether to force the label to render for this object or not.
     */
    void set_force_display_label(String name,
                                 boolean forceLabel);

    /**
     * Set the label color of the object identified by the given <code>name</code>.
     * The label color must be an array of RGBA values in [0,1].
     *
     * @param name  The object name.
     * @param color The label color as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void set_label_color(String name,
                         double[] color);

    /**
     * Gets the value of the force display label flag for the object identified by the
     * given <code>name</code>.
     *
     * @param name The name of the object.
     *
     * @return The value of the force display label flag of the object, if it exists.
     */
    boolean get_force_dispaly_label(String name);

    /**
     * Set the global line width factor. This is a multiplier that applies to all lines.
     *
     * @param factor Factor in {@link Constants#MIN_LINE_WIDTH} and {@link Constants#MAX_LINE_WIDTH}.
     */
    void set_line_width_factor(float factor);

    /**
     * Set the number factor of the velocity vectors (proper motions) that are visible. In [1,100].
     * <p>
     * This is just a factor, not an absolute number,
     * as actual number of proper motion vectors depends on the stars currently in view.
     *
     * @param factor Factor in [1,100].
     */
    void set_velocity_vectors_number_factor(float factor);

    /**
     * Set the length factor of the velocity vectors (proper motions), in [500,30000]. This is just a factor, not
     * an absolute length measure.
     *
     * @param factor Factor in [500,30000].
     */
    void set_velocity_vectors_length_factor(float factor);

    /**
     * Set the color mode of the velocity vectors (proper motions).
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
    void set_velocity_vectors_color_mode(int mode);

    /**
     * Specify whether arrowheads should be displayed on velocity vectors.
     *
     * @param arrowheadsEnabled Whether to show the velocity vectors with arrowheads.
     */
    void set_velocity_vectors_arrowheads(boolean arrowheadsEnabled);

    /**
     * Return the current maximum number of velocity vectors per star group. This is a global setting stored in the configuration file.
     *
     * @return Max number of velocity vectors per star group.
     */
    long get_velocity_vector_max_number();

    /**
     * Set the maximum number of proper motion vectors to add per star group. This modifies the global setting stored in the
     * configuration file, and will be persisted when the application exits.
     *
     * @param maxNumber The maximum number of proper motion vectors per star group.
     */
    void set_velocity_vector_max_number(long maxNumber);

}
