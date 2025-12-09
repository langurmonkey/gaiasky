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

import java.util.regex.Pattern;

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
     * @param name    The name or id (HIP, TYC, Gaia SourceId) of the object.
     * @param timeout The timeout in seconds to wait until returning.
     *                If negative, it waits indefinitely.
     *
     * @return The object if it exists, or null if it does not and block is false, or if block is true and
     *         the timeout has passed.
     */
    FocusView get_object(String name,
                         double timeout);

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
     * @param name    The name of the line object.
     * @param timeout The timeout, in seconds, to wait for the object to be ready.
     *                If negative, it waits indefinitely.
     *
     * @return The line object as a {@link VertsView}, or null
     *         if it does not exist.
     */
    VertsView get_line_object(String name,
                              double timeout);

    /**
     * Get the reference to an entity given its name using the internal index. This method
     * returns proper scene entities only. If you need any other kind of entity (datasets,
     * hooks, etc.), use {@link #get_non_index_entity(String, double)}.
     *
     * @param name Entity name.
     *
     * @return Reference to the entity.
     **/
    Entity get_entity(String name);

    /**
     * Get the reference to an entity given its name and a timeout in seconds.
     * This method returns proper scene entities only. If you need any other kind of entity
     * (datasets, hooks, etc.), use {@link #get_non_index_entity(String)}
     * If the entity is not in the scene after the timeout has passed, ths method
     * returns null.
     *
     * @param name    Entity name.
     * @param timeout Timeout time, in seconds.
     *
     * @return Reference to the entity.
     **/
    Entity get_entity(String name, double timeout);

    /**
     * Gets the reference to an entity given its name without using the index. This method
     * is slower than its indexed counterpart ({@link #get_entity(String)}, but it can
     * get any type of entity.
     *
     * @param name Entity name.
     *
     * @return Reference to the entity.
     */
    Entity get_non_index_entity(String name);

    /**
     * Get the reference to an entity given its name and a timeout in seconds, without using the index.
     * This method is slower than its indexed counterpart ({@link #get_entity(String, double)}, but it
     * can get non-indexed entities.
     * <p>
     * If the entity is not in the scene after the timeout has passed, ths method
     * returns null.
     *
     * @param name    Entity name.
     * @param timeout Timeout time, in seconds.
     *
     * @return Reference to the entity.
     **/
    Entity get_non_index_entity(String name, double timeout);

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
     * @param name The star identifier or name.
     *
     * @return An array with (ra [deg], dec [deg], parallax [mas], pmra [mas/yr], pmdec [mas/yr], radvel [km/s], appmag
     *         [mag], red [0,1], green [0,1], blue [0,1]) if the
     *         star exists and is loaded, null otherwise.
     */
    double[] get_star_parameters(String name);

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
     * @param name The name of the object.
     * @param pos  The position in the internal reference system and internal units.
     */
    void set_object_posiiton(String name,
                             double[] pos);

    /**
     * Set the internal position of the object identified by <code>name</code>. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param name  The name of the object.
     * @param pos   The position in the internal reference system and the given units.
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_object_posiiton(String name,
                             double[] pos,
                             String units);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object The object in a focus view wrapper.
     * @param pos    The position in the internal reference system and internal units.
     */
    void set_object_posiiton(FocusView object,
                             double[] pos);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object The object in a focus view wrapper.
     * @param pos    The position in the internal reference system and the given units.
     * @param units  The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_object_posiiton(FocusView object,
                             double[] pos,
                             String units);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object The object entity.
     * @param pos    The position in the internal reference system and internal units.
     */
    void set_object_posiiton(Entity object,
                             double[] pos);

    /**
     * Set the internal position of the given entity object. Note that
     * depending on the object type, the position may be already calculated and set elsewhere
     * in the update stage, so use with care.
     *
     * @param object The object entity.
     * @param pos    The position in the internal reference system and the given units.
     * @param units  The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_object_posiiton(Entity object,
                             double[] pos,
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
     * @param name   The name or id of the object.
     * @param factor The scaling factor to scale the size of that object.
     */
    void set_object_size_scaling(String name,
                                 double factor);

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
     * @param name   The name of the coordinates object (OrbitLintCoordinates, EclipticCoordinates, SaturnVSOP87,
     *               UranusVSOP87, EarthVSOP87, MercuryVSOP87, ..., PlutoCoordinates,
     *               HeliotropicOrbitCoordinates, MoonAACoordinates).
     *               Optionally, you can append ':objectName' to select a single object. For instance, both Gaia
     *               and JWST have
     *               heliotropic orbit coordinates. To only select the Gaia orbit provider, use
     *               "HeliotropicOrbitCoordinates:Gaia".
     * @param factor The scaling factor.
     */
    void set_orbit_coordinates_scaling(String name,
                                       double factor);

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
     * @param path The file path. The file is a CSV where each line has the time (ISO-8601) and the
     *             x, y, z and w components of the quaternion for that time. For instance, a valid line would
     *             be "<code>2018-04-01T15:20:15Z,0.9237,0.3728,0.0358,0.0795</code>".
     *
     * @return True if the object was found and could be updated.
     */
    boolean set_object_quaternion_slerp_orientation(String name,
                                                    String path);


    /**
     * Set the given quaternions file (CSV with times and quaternions) as the orientation provider
     * for this object. This call removes the previous orientation model from the object (either
     * {@link RigidRotation} or {@link AttitudeComponent}.
     * <p>
     * The interpolation between quaternions is done using <b>nlerp</b> (normalized linear interpolation).
     *
     * @param name The name of the object. The object must already have an <code>Orientation</code> component.
     * @param path The file path. The file is a CSV where each line has the time (ISO-8601) and the
     *             x, y, z and w components of the quaternion for that time. For instance, a valid line would
     *             be "<code>2018-04-01T15:20:15Z,0.9237,0.3728,0.0358,0.0795</code>".
     *
     * @return True if the object was found and could be updated.
     */
    boolean set_object_quaternion_nlerp_orientation(String name,
                                                    String path);

    /**
     * Set the global label size factor. This is a multiplier that applies to all object labels.
     *
     * @param factor Factor in {@link Constants#MIN_LABEL_SIZE} and {@link Constants#MAX_LABEL_SIZE}.
     */
    void set_label_size_factor(float factor);

    /**
     * Force the label of the object identified by <code>name</code> to be displayed, ignoring the usual
     * solid angle-based visibility rules. If called with <code>force = true</code>, the label for the
     * given object is always rendered. Calling this method with <code>false</code> does not cause the label
     * to be muted. If you want to mute the label, use {@link #set_mute_label(String, boolean)} instead.
     * <p>
     * Note: This does not override the visibility of the object itself, nor the
     * visibility settings of the <code>labels</code> component type.
     * <p>
     * This setting is temporary and will not persist after a restart.
     *
     * @param name  The object name.
     * @param force Whether to force the label to render for this object or not.
     */
    void set_force_display_label(String name,
                                 boolean force);

    /**
     * Set the global include regular expression for filtering labels. Only labels that match this regular expression are
     * rendered after this call. This call disables the global exclude regular expression (set with {@link #set_label_exclude_regexp(String)}), if
     * it is set.
     * <p>
     * Java (like many other c-style languages) interprets backslashes (<code>'\'</code>) as escape characters. If you are calling the API
     * programmatically (i.e.
     * from Python), make sure to escape the backslashes in your source code; use <code>"\\d+"</code> instead of <code>"\d+"</code>.
     * <p>
     * You clear all label-filtering regular expressions, effectively reverting the effects of this call, with {@link #clear_label_filter_regexps()}.
     * <p>
     * The include regular expression is not persisted to the settings file, and never lives longer than the current Gaia Sky instance.
     * It is cleared after a restart.
     *
     * @param regexp The regular expression string, in Java format. See {@link Pattern} for more information.
     */
    void set_label_include_regexp(String regexp);

    /**
     * Set the global exclude regular expression for filtering labels. Labels that match this regular expression are
     * not rendered after this call. This call disables the global include regular expression (set with {@link #set_label_include_regexp(String)}),
     * if
     * it is set.
     * <p>
     * Java (like many other c-style languages) interprets backslashes (<code>'\'</code>) as escape characters. If you are calling the API
     * programmatically (i.e.
     * from Python), make sure to escape the backslashes in your source code; use <code>"\\d+"</code> instead of <code>"\d+"</code>.
     * <p>
     * You clear all label-filtering regular expressions, effectively reverting the effects of this call, with {@link #clear_label_filter_regexps()}.
     * <p>
     * The exclude regular expression is not persisted to the settings file, and never lives longer than the current Gaia Sky instance.
     * It is cleared after a restart.
     *
     * @param regexp The regular expression string, in Java format. See {@link Pattern} for more information.
     */
    void set_label_exclude_regexp(String regexp);

    /**
     * Clears all label-filtering regular expressions currently in use, set with {@link #set_label_exclude_regexp(String)} or
     * {@link #set_label_include_regexp(String)}, if any.
     * <p>
     * After this call, the label include and exclude regular expressions, used to filter labels in and out, are cleared.
     */
    void clear_label_filter_regexps();

    /**
     * Mute the label of the object identified by <code>name</code>, ignoring the usual solid angle-based
     * visibility rules. If called with <code>mute = true</code>, the label for the given object is
     * never rendered. Calling this method with <code>false</code> does not cause the label to always
     * display. If you want to force-display the label, use {@link #set_force_display_label(String, boolean)} instead.
     * <p>
     * Note: This does not override the visibility of the object itself, nor the
     * visibility settings of the <code>labels</code> component type.
     * <p>
     * This setting is temporary and will not persist after a restart.
     *
     * @param name The object name.
     * @param mute Whether to mute the label for the object or not.
     */
    void set_mute_label(String name, boolean mute);

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
    boolean get_force_display_label(String name);

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
     * @param enabled Whether to show the velocity vectors with arrowheads.
     */
    void set_velocity_vectors_arrowheads(boolean enabled);

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
     * @param num The maximum number of proper motion vectors per star group.
     */
    void set_velocity_vector_max_number(long num);

    /**
     * Add a new trajectory object with the given name, points and color. The trajectory
     * is rendered using the 'line renderer' setting in the preferences dialog.
     * This is a very similar call to {@link #add_polyline(String, double[], double[])},
     * but in this case the line can be rendered with higher quality
     * polyline quadstrips.
     *
     * @param name   The name to identify the trajectory, to possibly remove it later.
     * @param points The points of the trajectory. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn] in the internal reference system.
     * @param color  The color of the trajectory as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void add_trajectory_line(String name,
                             double[] points,
                             double[] color);

    /**
     * Add a new trajectory object with the given name, points and color. The trajectory
     * is rendered using the 'line renderer' setting in the preferences' dialog.
     * This is a very similar call to {@link #add_polyline(String, double[], double[])},
     * but in this case the line can be rendered with higher quality
     * polyline quadstrips.
     *
     * @param name   The name to identify the trajectory, to possibly remove it later.
     * @param points The points of the trajectory. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn] in the internal reference system.
     * @param color  The color of the trajectory as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param trail  The bottom mapping position for the trail. The orbit trail assigns an opacity value to
     *               each point of the orbit, where 1 is the location of the last point in the points list, and 0 is
     *               the first one.
     *               This mapping parameter defines the location in the orbit (in [0,1]) where we map the opacity
     *               value of 0. Set to 0 to have a full trail. Set to 0.5 to have a trail that spans half the orbit.
     *               Set to 1 to have no orbit at all. Set to negative to disable the trail.
     */
    void add_trajectory_line(String name,
                             double[] points,
                             double[] color,
                             double trail);

    /**
     * Add a new polyline with the given name, points and color. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. The default primitive of GL_LINE_STRIP
     * is used.
     *
     * @param name   The name to identify the polyline, to possibly remove it later.
     * @param points The points of the polyline. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn] in the internal reference system.
     * @param color  The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     */
    void add_polyline(String name,
                      double[] points,
                      double[] color);

    /**
     * Add a new polyline with the given name, points, color and line width. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. The default primitive type of GL_LINE_STRIP
     * is used.
     *
     * @param name   The name to identify the polyline, to possibly remove it later.
     * @param points The points of the polyline. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color  The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param width  The line width. Usually a value between 1 (default) and 10.
     */
    void add_polyline(String name,
                      double[] points,
                      double[] color,
                      double width);

    /**
     * Add a new polyline with the given name, points, color and line width. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it. The default primitive type of GL_LINE_STRIP
     * is used. This version enables the addition of arrow caps. In the case arrow caps
     * are enabled, the line will be rendered in CPU mode (no VBO), making it slightly slower, especially for lines with
     * many points.
     * The arrow cap is added at the first point in the series.
     *
     * @param name   The name to identify the polyline, to possibly remove it later.
     * @param points The points of the polyline. It is an array containing all the
     *               points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color  The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param width  The line width. Usually a value between 1 (default) and 10.
     * @param caps   Whether to represent arrow caps. If enabled, the line is rendered in CPU mode, which is slower.
     */
    void add_polyline(String name,
                      double[] points,
                      double[] color,
                      double width,
                      boolean caps);

    /**
     * Add a new polyline with the given name, points, color, line width and primitive. The polyline will
     * be created with the 'Others' component type, so you need to enable the
     * visibility of 'Others' in order to see it.
     *
     * @param name      The name to identify the polyline, to possibly remove it later.
     * @param points    The points of the polyline. It is an array containing all the
     *                  points as in [x0, y0, z0, x1, y1, z1, ..., xn, yn, zn].
     * @param color     The color of the polyline as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param width     The line width. Usually a value between 1 (default) and 10.
     * @param primitive The GL primitive: <code>GL_LINES</code>=1, <code>GL_LINE_LOOP</code>=2, <code>GL_LINE_STRIP</code>=3
     */
    void add_polyline(String name,
                      double[] points,
                      double[] color,
                      double width,
                      int primitive);

    /**
     * Add a new polyline with the given name, points, color, line width, primitive and arrow caps. The polyline will
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
     * @param width     The line width. Usually a value between 1 (default) and 10.
     * @param primitive The GL primitive: <code>GL_LINES</code>=1, <code>GL_LINE_LOOP</code>=2, <code>GL_LINE_STRIP</code>=3
     * @param caps      Whether to represent arrow caps. If enabled, the line is rendered in CPU mode, which is slower.
     */
    void add_polyline(String name,
                      double[] points,
                      double[] color,
                      double width,
                      int primitive,
                      boolean caps);

    /**
     * Remove the model object identified by the given name from the internal
     * scene graph model of Gaia Sky, if it exists.
     * If the object has children, they are removed recursively.
     * Be careful with this function, as it can have unexpected side effects
     * depending on what objects are removed.
     * For example,
     * <p>
     * <code>
     * gs.removeModelObject("Earth")
     * </code>
     * <p>
     * removes the Earth, the Moon, Gaia and any dependent object from Gaia Sky.
     *
     * @param name The name of the object to remove.
     */
    void remove_object(String name);

    /**
     * Add a shape object of the given type with the given size around the object with the given name and primitive.
     *
     * @param name      The name of the shape object.
     * @param shape     The shape type, one of
     *                  <ul><li>sphere</li><li>icosphere</li><li>octahedronsphere</li><li>ring</li><li>cylinder</li><li>cone</li></ul>
     * @param primitive The primitive to use, one of <ul><li>lines</li><li>triangles</li></ul>. Use 'lines' to create
     *                  a wireframe shape, use 'triangles' for a solid shape.
     * @param size      The size of the object in kilometers.
     * @param obj_name  The name of the object to use as the position.
     * @param r         The red component of the color in [0,1].
     * @param g         The green component of the color in [0,1].
     * @param b         The blue component of the color in [0,1].
     * @param a         The alpha component of the color in [0,1].
     * @param label     Whether to show a label with the name of the shape.
     * @param track     Whether to track the object if/when it moves.
     */
    void add_shape_around_object(String name,
                                 String shape,
                                 String primitive,
                                 double size,
                                 String obj_name,
                                 float r,
                                 float g,
                                 float b,
                                 float a,
                                 boolean label,
                                 boolean track);

    /**
     * Add a shape object of the given type with the given size around the object with the given name, primitive and
     * orientation.
     *
     * @param name      The name of the shape object.
     * @param shape     The shape type, one of
     *                  <ul><li>sphere</li><li>icosphere</li><li>octahedronsphere</li><li>ring</li><li>cylinder</li><li>cone</li></ul>
     * @param primitive The primitive to use, one of <ul><li>lines</li><li>triangles</li></ul>. Use 'lines' to create
     *                  a wireframe shape, use 'triangles' for a solid shape.
     * @param ori       The orientation to use, one of
     *                  <ul><li>camera</li><li>equatorial</li><li>ecliptic</li><li>galactic</li></ul>.
     * @param size      The size of the object in kilometers.
     * @param obj_name  The name of the object to use as the position.
     * @param r         The red component of the color in [0,1].
     * @param g         The green component of the color in [0,1].
     * @param b         The blue component of the color in [0,1].
     * @param a         The alpha component of the color in [0,1].
     * @param label     Whether to show a label with the name of the shape.
     * @param track     Whether to track the object if/when it moves.
     */
    void add_shape_around_object(String name,
                                 String shape,
                                 String primitive,
                                 String ori,
                                 double size,
                                 String obj_name,
                                 float r,
                                 float g,
                                 float b,
                                 float a,
                                 boolean label,
                                 boolean track);

}
