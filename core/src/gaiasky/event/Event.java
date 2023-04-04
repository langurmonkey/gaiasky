/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.event;

import gaiasky.render.RenderGroup;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IStarFocus;
import gaiasky.scene.api.IVisibilitySwitch;
import gaiasky.util.gdx.contrib.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;

import static gaiasky.util.Settings.*;

/**
 * Represents an event.
 */
public enum Event {

    /**
     * Adds or modifies a custom image. Contains:
     * <ol start='0'>
     * <li><strong>id</strong> - integer</li>
     * <li><strong>tex</strong> - Texture</li>
     * <li><strong>x</strong> - X position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>y</strong> - Y position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>r</strong> - optional, float in [0..1]</li>
     * <li><strong>g</strong> - optional, float in [0..1]</li>
     * <li><strong>b</strong> - optional, float in [0..1]</li>
     * <li><strong>a</strong> - optional, float in [0..1]</li>
     * </ol>
     */
    ADD_CUSTOM_IMAGE,
    /**
     * Adds or modifies a custom message. Contains:
     * <ol start='0'>
     * <li><strong>id</strong> - integer</li>
     * <li><strong>message</strong> - string</li>
     * <li><strong>x</strong> - X position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>y</strong> - Y position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>r</strong> - float in [0..1]</li>
     * <li><strong>g</strong> - float in [0..1]</li>
     * <li><strong>b</strong> - float in [0..1]</li>
     * <li><strong>a</strong> - float in [0..1]</li>
     * <li><strong>size</strong> - float</li>
     * </ol>
     */
    ADD_CUSTOM_MESSAGE,
    /**
     * Adds or modifies a custom message. Contains:
     * <ol start='0'>
     * <li><strong>id</strong> - integer</li>
     * <li><strong>message</strong> - string</li>
     * <li><strong>x</strong> - X position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>y</strong> - Y position of bottom-left corner, float in
     * [0..1]</li>
     * <li><strong>x</strong> - maxWidth maximum width in screen percentage,
     * float in [0..1]</li>
     * <li><strong>y</strong> - maxHeight maximum height in screen percentage,
     * float in [0..1]</li>
     * <li><strong>r</strong> - float in [0..1]</li>
     * <li><strong>g</strong> - float in [0..1]</li>
     * <li><strong>b</strong> - float in [0..1]</li>
     * <li><strong>a</strong> - float in [0..1]</li>
     * <li><strong>size</strong> - float</li>
     * </ol>
     */
    ADD_CUSTOM_TEXT,
    /**
     * Adds the GUI component identified by the given name.
     **/
    ADD_GUI_COMPONENT,
    /**
     * Contains a float with the intensity of the light in [0,1].
     **/
    AMBIENT_LIGHT_CMD,

    /**
     * Anti aliasing changed, contains the new anti-aliasing value.
     **/
    ANTIALIASING_CMD,

    /** Add bookmark. Contains the path and a boolean indicating if it is a folder. **/
    BOOKMARKS_ADD,
    /** Remove bookmark. Contains the path to remove. **/
    BOOKMARKS_REMOVE,
    /** Remove all bookmarks with the given name, irrespective of the path. **/
    BOOKMARKS_REMOVE_ALL,
    /** Moves the bookmark 1 to a child of bookmark 2. **/
    BOOKMARKS_MOVE,
    /** Moves the bookmark up in the list of its parent. **/
    BOOKMARKS_MOVE_UP,
    /** Moves the bookmark down in the list of its parent. **/
    BOOKMARKS_MOVE_DOWN,

    /**
     * Empty event which informs that background loading is active.
     **/
    BACKGROUND_LOADING_INFO,

    /**
     * Contains the intensity value in [0,1].
     **/
    BLOOM_CMD,
    /**
     * Contains the sharpen factor in [0,2].
     */
    UNSHARP_MASK_CMD,
    /**
     * Contains the brightness level (float) in [-1,1].
     **/
    BRIGHTNESS_CMD,
    /**
     * Removes the turn of the camera in focus mode.
     **/
    CAMERA_CENTER,
    /**
     * Sets the 'diverted' attribute of the camera. Gets a boolean with the state.
     */
    CAMERA_CENTER_FOCUS_CMD,
    /**
     * Contains a boolean with the cinematic mode state (on/off).
     **/
    CAMERA_CINEMATIC_CMD,
    /** Broadcasts the overall closest (in [0]), the closest non-star body (in [1]) and the closest particle (in [2]) to this camera. Happens every frame. **/
    CAMERA_CLOSEST_INFO,
    /**
     * This event is broadcast whenever the closest object to the camera
     * changes. Contains the closest object as an {@link IFocus}.
     */
    CAMERA_NEW_CLOSEST,

    /**
     * Contains a double[] with the new direction.
     **/
    CAMERA_DIR_CMD,

    /**
     * Contains the forward force in [0,1].
     **/
    CAMERA_FWD,
    /**
     * Contains the new CameraMode object.
     **/
    CAMERA_MODE_CMD,
    /**
     * Informs of a new camera state. Contains:
     * <ol start='0'>
     * <li>Vector3d with the current position of the camera</li>
     * <li>Double with the speed of the camera in km/s</li>
     * <li>Vector3d with the velocity vector of the camera</li>
     * <li>The PerspectiveCamera</li>
     * </ul>
     **/
    CAMERA_MOTION_UPDATE,
    /**
     * Sent whenever the camera orientation changes. Note that this happens more
     * often than {@link #CAMERA_MOTION_UPDATE}, as multi-render modes (cubemap, vr, etc.)
     * need to send one of these every time the camera changes.
     */
    CAMERA_ORIENTATION_UPDATE,
    CAMERA_PAN,
    /**
     * Informs that the camera has started or stopped playing. Contains a
     * boolean (true - start, false - stop).
     **/
    CAMERA_PLAY_INFO,
    /**
     * Contains a double[] with the new position.
     **/
    CAMERA_POS_CMD,

    /**
     * Update camera position, direction and up vectors all at once.
     * Only meant for master-slave setups.
     */
    CAMERA_PROJECTION_CMD,

    /**
     * Contains the roll value in [-1,1].
     **/
    CAMERA_ROLL,

    /**
     * Contains the deltaX and deltaY, both in [-1,1].
     **/
    CAMERA_ROTATE,

    /**
     * Contains the new camera speed.
     **/
    CAMERA_SPEED_CMD,
    /**
     * Stops the camera motion.
     **/
    CAMERA_STOP,
    /**
     * Contains the deltaX and deltaY in [-1,1].
     **/
    CAMERA_TURN,
    /**
     * Contains a double[] with the new up vector.
     **/
    CAMERA_UP_CMD,
    /**
     * Sets or unsets the tracking object of the camera.
     * <ol start='0'>
     * <li>the new tracking object, or null to disable tracking mode</li>
     * <li>the name of the tracking object, or null to disable tracking mode</li>
     * </ol>
     */
    CAMERA_TRACKING_OBJECT_CMD,
    /**
     * Broadcasts the new camera tracking object, contains the object and the name, or null
     * to indicate the camera is not tracking.
     */
    CAMERA_TRACKING_OBJECT_UPDATE,

    /**
     * Limits the frame rate, contains a double with the new limit frame rate.
     */
    LIMIT_FPS_CMD,

    /**
     * Add the new catalog object to the catalog manager. Contains:
     * <ul>
     *     <li>[0] - CatalogInfo, the new catalog info object.</li>
     *     <li>[1] - boolean, add object to scene graph.</li>
     * </ul>
     **/
    CATALOG_ADD,
    /**
     * Highlight the catalog. Contains the CatalogInfo object, the highlight status (bool).
     */
    CATALOG_HIGHLIGHT,
    /**
     * Removes the catalog identified by the given string name.
     */
    CATALOG_REMOVE,
    /**
     * Sets the visibility of a catalog given its name.
     */
    CATALOG_VISIBLE,
    /**
     * Sets the point size multiplier as a positive double for a catalog given its name.
     * Contains the name of the catalog and the scaling value.
     * This only has effect if the catalog has points.
     */
    CATALOG_POINT_SIZE_SCALING_CMD,
    /**
     * Clears the headline message.
     **/
    CLEAR_HEADLINE_MESSAGE,
    /**
     * Clears all messages in the message interface.
     **/
    CLEAR_MESSAGES,

    /**
     * Clears the subhead message.
     **/
    CLEAR_SUBHEAD_MESSAGE,

    /**
     * Collapses a GUI pane. Contains its name.
     */
    COLLAPSE_PANE_CMD,
    /**
     * Contains the contrast level (float) in [0,2].
     **/
    CONTRAST_CMD,
    /**
     * Activates/deactivates the closest crosshair. Contains a boolean with the state.
     **/
    CROSSHAIR_CLOSEST_CMD,
    /**
     * Activates/deactivates the focus crosshair. Contains a boolean with the state.
     **/
    CROSSHAIR_FOCUS_CMD,
    /**
     * Activates/deactivates the home crosshair. Contains a boolean with the state.
     **/
    CROSSHAIR_HOME_CMD,
    /**
     * Sets cubemap mode. Contains a boolean with the new state, the new projection object.
     **/
    CUBEMAP_CMD,
    /**
     * Sets a new cubemap projection. Contains the CubemapProjection object.
     */
    CUBEMAP_PROJECTION_CMD,
    /**
     * Sets the resolution of the cubemap, contains an integer in [20..15000]
     * with the resolution.
     **/
    CUBEMAP_RESOLUTION_CMD,
    DEBUG_OBJECTS,
    DEBUG_QUEUE,
    DEBUG_RAM,
    /**
     * Debug info.
     **/
    /** The time. **/
    DEBUG_TIME,
    /** Contains the used graphics memory and total graphics memory in bytes. **/
    DEBUG_VRAM,
    /** Contains the number of running background threads, and the total number of threads in the pool. **/
    DEBUG_THREADS,
    /** Contains the current dynamic resolution level and the corresponding back buffer scale **/
    DEBUG_DYN_RES,
    /**
     * Toggles whole GUI display. Contains the a boolean
     * with the state (display/no display) and the localised name.
     **/
    DISPLAY_GUI_CMD,
    DISPLAY_MEM_INFO_WINDOW,
    /** All open windows must be closed **/
    CLOSE_ALL_GUI_WINDOWS_CMD,

    /**
     * Change UI scale factor. Contains the new internal scale factor.
     **/
    UI_SCALE_CMD,

    /**
     * Contains a boolean with the display status.
     */
    DISPLAY_POINTER_COORDS_CMD,

    /**
     * Contains the state (boolean), the color (float[4]) and the line width (float).
     */
    POINTER_GUIDES_CMD,

    /** Displays VR Controller hints. **/
    DISPLAY_VR_CONTROLLER_HINT_CMD,
    /** A controller has been connected, contains the name. **/
    CONTROLLER_CONNECTED_INFO,
    /** A controller has been disconnected, contains the name. **/
    CONTROLLER_DISCONNECTED_INFO,
    /**
     * Toggles VR GUI display. Contains a name and a boolean with the state.
     */
    DISPLAY_VR_GUI_CMD,

    /**
     * Dispose all resources, app is shutting down.
     **/
    DISPOSE,

    /**
     * Dispose the source GPU mesh.
     **/
    GPU_DISPOSE_PARTICLE_GROUP,
    /**
     * Dispose the source GPU mesh.
     **/
    GPU_DISPOSE_STAR_GROUP,
    /**
     * Dispose the source GPU mesh.
     **/
    GPU_DISPOSE_VARIABLE_GROUP,
    /**
     * Dispose the source GPU mesh.
     */
    GPU_DISPOSE_BILLBOARD_DATASET,
    /**
     * Dispose the source GPU mesh.
     * Mark a {@link gaiasky.scene.component.Verts} entity (in source) for update with the given render group in [0].
     */
    GPU_DISPOSE_VERTS_OBJECT,
    /**
     * Dispose the source GPU mesh.
     * Mark an {@link gaiasky.scene.component.OrbitElementsSet} entity (in source) for update.
     */
    GPU_DISPOSE_ORBITAL_ELEMENTS,

    /**
     * Sets the elevation multiplier. Contains the new multiplier in [{@link gaiasky.util.Constants#MIN_ELEVATION_MULT}, {@link gaiasky.util.Constants#MAX_ELEVATION_MULT}].
     */
    ELEVATION_MULTIPLIER_CMD,

    /**
     * Sets the elevation type.
     */
    ELEVATION_TYPE_CMD,
    /**
     * Contains the new time frame object.
     **/
    EVENT_TIME_FRAME_CMD,
    /**
     * Expands a GUI pane. Contains its name.
     */
    EXPAND_PANE_CMD,
    /**
     * Contains the exposure tone mapping level (float) in [0,n] (0 for disabled).
     */
    EXPOSURE_CMD,
    /**
     * Fisheye effect toggle. Contains a boolean with the state, and an integer with the mode.
     **/
    REPROJECTION_CMD,
    /**
     * Sets the back-buffer scale. Contains the new scale as a float.
     */
    BACKBUFFER_SCALE_CMD,
    /**
     * Issues the command to flush the frame system.
     **/
    FLUSH_FRAMES,

    /**
     * Informs that the focus has somehow changed and the GUI must be updated.
     * <ol start='0'>
     * <li>the new focus object.</li>
     * <li>boolean, Center focus. If true, the focus is centered on the view.</li>
     * </ol>
     **/
    FOCUS_CHANGED,

    /**
     * FOCUS_MODE change command.
     * <ol start='0'>
     * <li>the new focus object.</li>
     * </ol>
     **/
    FOCUS_CHANGE_CMD,

    /**
     * Updates focus information.
     * <ol start='0'>
     *     <li>distance to camera.</li>
     *     <li>solid angle.</li>
     *     <li>right ascension [deg].</li>
     *     <li>declination [deg].</li>
     *     <li>distance to the Sun.</li>
     *     <li>absolute magnitude from camera.</li>
     *     <li>absolute magnitude from Earth.</li>
     * </ol>
     **/
    FOCUS_INFO_UPDATED,

    /**
     * Contains the name, the boolean value.
     **/
    FOCUS_LOCK_CMD,

    /**
     * Informs that the given focus is not available any more (not visible or unloaded).
     */
    FOCUS_NOT_AVAILABLE,

    /**
     * Contains the a float with the new fov value and an optional boolean to indicate whether to cap the value to 95 degrees or not.
     **/
    FOV_CHANGED_CMD,

    /**
     * Notifies a fov update in the camera. Contains the new fov value (float)
     * and the new fovFactor (float).
     **/
    FOV_CHANGE_NOTIFICATION,

    /**
     * Frames per second info.
     **/
    FPS_INFO,

    /**
     * Posts the coordinates of the free mode focus.
     * <ol start='0'>
     * <li>ra  [deg]</li>
     * <li>dec [deg]</li>
     * </ol>
     */
    FREE_MODE_COORD_CMD,
    GAIA_POSITION,
    /**
     * Contains the gamma level (float) in [0,3].
     */
    GAMMA_CMD,
    /**
     * Executes the command to position the camera near the object in focus.
     **/
    GO_TO_OBJECT_CMD,
    /**
     * Moves the camera instantly to the home object.
     */
    GO_HOME_INSTANT_CMD,
    /**
     * Graphics quality updated, contains the new {@link GraphicsQuality} object.
     **/
    GRAPHICS_QUALITY_UPDATED,
    /**
     * Contains the x and the y in pixels of the position of the mass.
     **/
    GRAVITATIONAL_LENSING_PARAMS,

    /**
     * Start gravitational wave. Contains on-screen position (x,y) of source as two integer parameters.
     **/
    GRAV_WAVE_START,
    /**
     * Stops gravitational wave.
     **/
    GRAV_WAVE_STOP,
    /**
     * Maximizes or minimizes the GUI window. Contains a boolean with the fold
     * state (true - minimize, false - maximize).
     **/
    GUI_FOLD_CMD,
    /**
     * Moves the GUI window.
     * <ol>
     * <li><strong>x</strong> - X coordinate of the top-left corner, float in
     * [0..1] from left to right.</li>
     * <li><strong>y</strong> - Y coordinate of top-left corner, float in [0..1]
     * from bottom to top.</li>
     * </ol>
     */
    GUI_MOVE_CMD,
    /**
     * Sets the vertical scroll position. Contains the scroll position in pixels.
     **/
    GUI_SCROLL_POSITION_CMD,
    /**
     * Hides all uncertainties.
     */
    HIDE_UNCERTAINTIES,
    /**
     * Issues the command to change the high accuracy setting. Contains a
     * boolean with the setting.
     */
    HIGH_ACCURACY_CMD,
    /**
     * Runs the 'Go home' action.
     */
    HOME_CMD,
    /**
     * Contains the hue level (float) in [0,2].
     **/
    HUE_CMD,
    /**
     * Sets the index of refraction of the celestial sphere when orthospheric view is on.
     */
    INDEXOFREFRACTION_CMD,
    /**
     * Informs Gaia Sky is fully initialized and normal operation is about to start.
     */
    INITIALIZED_INFO,
    /**
     * Enables/disables input from mouse/keyboard/etc. Contains a boolean with
     * the new state.
     **/
    INPUT_ENABLED_CMD,
    /**
     * Issued when an input event is received. It contains the key or button
     * integer code (see {@link com.badlogic.gdx.Input}).
     **/
    INPUT_EVENT,
    /**
     * Notifies from a java exception, it sends the Throwable and an optional
     * tag.
     **/
    JAVA_EXCEPTION,
    /**
     * Exports the given array of keyframes to a camera path file.
     */
    KEYFRAMES_EXPORT,
    /**
     * Saves the given array of keyframes to a keyframes file.
     */
    KEYFRAMES_FILE_SAVE,
    /**
     * Refreshes the keyframes from the model.
     **/
    KEYFRAMES_REFRESH,
    /**
     * Add new keyframe at the end with the current camera settings.
     **/
    KEYFRAME_ADD,
    /**
     * The given keyframe has been selected.
     **/
    KEYFRAME_SELECT,
    /**
     * The given keyframe is no longer selected.
     **/
    KEYFRAME_UNSELECT,

    KEY_DOWN,
    KEY_UP,
    /**
     * Set label size. Contains the new label size.
     */
    LABEL_SIZE_CMD,
    /**
     * Line width factor. Contains the new factor.
     */
    LINE_WIDTH_CMD,
    /**
     * Lands at a certain location on a planet object.
     **/
    LAND_AT_LOCATION_OF_OBJECT,

    /**
     * Lands on a planet object.
     **/
    LAND_ON_OBJECT,

    /**
     * Activate/deactivate lens flare. Contains a boolean with the new state.
     **/
    LENS_FLARE_CMD,

    /**
     * Contains an int with the number of lights and a float[] with [x, y] of
     * the 10 closest stars in screen coordinates in [0,1].
     **/
    LIGHT_POS_2D_UPDATE,
    /**
     * Activate/deactivate the light scattering. Contains boolean with state.
     **/
    LIGHT_GLOW_CMD,

    /**
     * Issues the command to update the line render system. Contains no
     * parameters.
     **/
    LINE_RENDERER_UPDATE,
    /**
     * Dataset has been chosen, loading can start.
     **/
    LOAD_DATA_CMD, // CAMERA
    /**
     * Contains two Double values, the longitude and latitude in degrees.
     **/
    LON_LAT_UPDATED,
    /**
     * Opens a new popup window with information on the new mode. Contains a ModePopupInfo object (null to remove), a name
     * and the number of seconds until the popup disappears.
     */
    MODE_POPUP_CMD,
    /**
     * Contains the opacity of motion blur in [0,1].
     **/
    MOTION_BLUR_CMD,
    /**
     * Enables/disables screen-space reflections. Contains the boolean state.
     */
    SSR_CMD,
    /**
     * True to capture the mouse, false to un-capture.
     */
    MOUSE_CAPTURE_CMD,
    /**
     * Toggle mouse capture.
     */
    MOUSE_CAPTURE_TOGGLE,
    /**
     * Plays next music.
     **/
    MUSIC_NEXT_CMD,
    /**
     * Toggles the play.
     **/
    MUSIC_PLAYPAUSE_CMD,
    /**
     * Plays previous music.
     **/
    MUSIC_PREVIOUS_CMD,
    /**
     * Reload music files.
     **/
    MUSIC_RELOAD_CMD,
    /**
     * Info about current track.
     **/
    MUSIC_TRACK_INFO,
    /**
     * Volume of music, contains the volume (float in [0..1]).
     **/
    MUSIC_VOLUME_CMD,
    /**
     * Navigates smoothly to the given object.
     **/
    NAVIGATE_TO_OBJECT,
    /**
     * Informs the octree has been disposed.
     **/
    OCTREE_DISPOSED,
    /**
     * Toggles the fading of particles in the octree. Contains a boolean with
     * the state of the flag.
     **/
    OCTREE_PARTICLE_FADE_CMD,
    /**
     * Passes the OrbitData and the file name.
     **/
    ORBIT_DATA_LOADED,
    /** Sets the solid angle threshold for orbits and trajectories. **/
    ORBIT_SOLID_ANGLE_TH_CMD,
    /**
     * Contains the name, the lock orientation boolean value.
     */
    ORIENTATION_LOCK_CMD,
    /**
     * Posts scene update runnable that runs after the update-scene stage and before the render stage.
     * Contains an identifier (String) and the runnable object.
     **/
    PARK_RUNNABLE,
    /**
     * Posts camera update runnable that runs after the update-camera stage and before the
     * update-scene stage. Contains an identifier (String) and the runnable object.
     **/
    PARK_CAMERA_RUNNABLE,
    /**
     * Pauses background data loading thread, if any.
     **/
    PAUSE_BACKGROUND_LOADING,

    /**
     * Sets the aperture angle [deg] of the planetarium in cubemap mode.
     */
    PLANETARIUM_APERTURE_CMD,
    /**
     * Sets the planetarium angle [deg], an angle from the zenith to put the focus on in planetarium mode.
     */
    PLANETARIUM_ANGLE_CMD,
    /**
     * Sets the planetarium projection. Contains the {@link CubemapProjection} object, which needs to evaluate isPlanetarium() to true.
     */
    PLANETARIUM_PROJECTION_CMD,
    /**
     * A new geometry warp file has been selected. Contains the path to the file.
     */
    PLANETARIUM_GEOMETRYWARP_FILE_CMD,
    /**
     * Issues the play command. Contains the path to the file to play.
     **/
    PLAY_CAMERA_CMD,
    /**
     * Show or hide arrow caps. Contains boolean with state.
     */
    PM_ARROWHEADS_CMD,
    /**
     * Contains the mode.
     * Modes:
     * <ol start='0'>
     * <li>direction</li>
     * <li>length</li>
     * <li>has radial velocity: blue=stars with RV, red=stars without RV</li>
     * <li>redshift (sun): blue=-50 Km/s, red=50 Kms/s</li>
     * <li>redshift (camera): blue=-50 Km/s, red=50 Kms/s</li>
     * <li>unique color</li>
     * </ol>
     */
    PM_COLOR_MODE_CMD,
    /**
     * Contains the length factor for pm vectors.
     **/
    PM_LEN_FACTOR_CMD,
    /**
     * Contains the number factor for pm vectors.
     **/
    PM_NUM_FACTOR_CMD,
    /**
     * Will show a popup menu for a focus candidate. Contains the candidate and
     * the screenX and screenY coordinates of the click.
     **/
    POPUP_MENU_FOCUS,
    /**
     * Contains a string with the headline message, will be displayed in a big
     * font in the center of the screen.
     **/
    POST_HEADLINE_MESSAGE,
    /**
     * Post a new notification that is to be displayed in the notifications area
     * at the bottom left. Contains the notification level (same as log level:
     * ERROR, WARN, INFO, DEBUG) and an array of strings with the messages plus
     * an optional boolean indicating whether the message is permanent so should
     * stay until the next message is received.
     * The notifications sent via this event are logged in the system log and
     * made permanent when the program closes.
     **/
    POST_NOTIFICATION,
    /**
     * Post a notification that is to be displayed with a screen pop-up. Contains
     * the string message and an optional float duration, in seconds.
     * The notifications sent via this event are not logged.
     */
    POST_POPUP_NOTIFICATION,
    /**
     * Contains a string with the subhead message, will be displayed in a small
     * font below the headline message.
     **/
    POST_SUBHEAD_MESSAGE,
    /**
     * Sent when the properties in GlobalConf have been modified, usually after
     * a configuration dialog. Contains no data.
     **/
    PROPERTIES_WRITTEN,
    /**
     * Updates the position of the pointer and the view in equatorial coordinates.
     * <ol start='0'>
     * <li>pointer ra  [deg]</li>
     * <li>pointer dec [deg]</li>
     * <li>view ra     [deg]</li>
     * <li>view dec    [deg]</li>
     * <li>pointer x   [pixels]</li>
     * <li>pointer y   [pixels]</li>
     * </ol>
     **/
    RA_DEC_UPDATED,
    /**
     * Event to update the shadow map metadata.
     */
    REBUILD_SHADOW_MAP_DATA_CMD,

    /**
     * Forces recalculation of main controls window size.
     **/
    RECALCULATE_CONTROLS_WINDOW_SIZE,
    /**
     * Issues the command to enable camera recording. Contains the boolean
     * indicating the state (on/off) and a file name (null for auto-generated).
     **/
    RECORD_CAMERA_CMD,
    /**
     * Sets the target frame rate for the camera recorder. Contains a double with the frame rate.
     */
    CAMRECORDER_FPS_CMD,
    /**
     * Reloads the inputListener mappings. Contains the path to the new mappings
     * file.
     **/
    RELOAD_CONTROLLER_MAPPINGS,
    /**
     * Removes all the custom objects.
     **/
    REMOVE_ALL_OBJECTS,
    /**
     * Removes the GUI component identified by the given name.
     **/
    REMOVE_GUI_COMPONENT,
    /**
     * Removes the keyboard focus in the GUI.
     **/
    REMOVE_KEYBOARD_FOCUS,
    /** Issues the command to clean pressed keys in KebyoardInputController. **/
    CLEAN_PRESSED_KEYS,

    /**
     * Removes a previously added message or image. Contains the id.
     **/
    REMOVE_OBJECTS,
    /**
     * Issues the command to render a frame.
     **/
    RENDER_FRAME,
    /**
     * Issues the command to render the current frame buffer with a given
     * folder, file (without filename), width and height.
     **/
    RENDER_FRAME_BUFFER,

    /**
     * Resumes background data loading thread, if it exists and it is paused.
     **/
    RESUME_BACKGROUND_LOADING,

    /** Clears the octant loading queues. **/
    CLEAR_OCTANT_QUEUE,

    /**
     * Contains the new camera rotation speed.
     **/
    ROTATION_SPEED_CMD,
    /**
     * Attach object to first end of ruler. Contains object name.
     **/
    RULER_ATTACH_0,
    /**
     * Attach object to second end of ruler. Contains object name.
     **/
    RULER_ATTACH_1,
    /**
     * Clear all objects from ruler.
     **/
    RULER_CLEAR,
    /**
     * Notifies new distances for the ruler. Contains a double with the distance in internal units and a formatted string.
     **/
    RULER_DIST,

    /**
     * Submits a register/unregister command for a ray marching shader.
     * Contains the name, the status (true/false), the position and optionally the path to the shader (for creating) and the additional values.
     * <ol start='0'>
     * <li>name [string]</li>
     * <li>status [boolean]</li>
     * <li>position [vector3]</li>
     * <li>shader [string] - optional, only at creation</li>
     * <li>additional [float4] - optional, only at creation</li>
     * </ol>
     */
    RAYMARCHING_CMD,
    /**
     * Push new additional data to ray marching shader. Contains the name and the additional vector.
     * <ol start='0'>
     * <li>name [string]</li>
     * <li>additional[float4]</li>
     * </ol>
     */
    RAYMARCHING_ADDITIONAL_CMD,

    /**
     * SAMP information
     **/
    SAMP_INFO,

    /**
     * Contains the saturation level (float) in [0,2].
     **/
    SATURATION_CMD,

    /**
     * Forces a scene update.
     */
    SCENE_FORCE_UPDATE,

    /**
     * Sends an object to be added to the scene. Contains the object and an optional Boolean indicating
     * whether to add the object to the scene index. Defaults to true.
     **/
    SCENE_ADD_OBJECT_CMD,

    /**
     * Sends an object to be added to a scene, without using a post runnable. Contains the object and an optional
     * Boolean indicating whether to add the object to the scene index. Defaults to true.
     */
    SCENE_ADD_OBJECT_NO_POST_CMD,

    /**
     * Informs the scene has been loaded. Program can start.
     **/
    SCENE_LOADED,
    /**
     * Removes an object from the scene. Contains the name of the object or the object itself plus and optional
     * Boolean indicating whether to remove it from the index. Defaults to true.
     */
    SCENE_REMOVE_OBJECT_CMD,
    /**
     * Removes an object from the scene without using a post runnable. Contains the name of the object or the object itself plus and optional
     * Boolean indicating whether to remove it from the index. Defaults to true.
     */
    SCENE_REMOVE_OBJECT_NO_POST_CMD,
    /**
     * Recomputes the names of the scene entities with the current locale.
     */
    SCENE_RELOAD_NAMES_CMD,

    /**
     * Takes a screenshot. contains the width, height (integers) and the folder name and filename
     * (strings).
     */
    SCREENSHOT_CMD,
    /**
     * Configures the screenshots. Contains the width, height (integers) and the folder name and filename
     * (strings).
     */
    CONFIG_SCREENSHOT_CMD,
    /**
     * Sets the screenshot mode, either SIMPLE or ADVANCED. Gets a string or a {@link ScreenshotSettings} object.
     */
    SCREENSHOT_MODE_CMD,

    /**
     * Contains the path where the screenshot has been saved.
     */
    SCREENSHOT_INFO,
    /**
     * Informs of the new size of the screenshot system.
     */
    SCREENSHOT_SIZE_UPDATE,

    /**
     * Issues the frame output command. Contains a boolean with the state.
     **/
    FRAME_OUTPUT_CMD,
    /**
     * Sets the frame output mode, either SIMPLE or ADVANCED. Gets a string or a {@link ScreenshotSettings} object.
     */
    FRAME_OUTPUT_MODE_CMD,
    /**
     * Configures the render system. Contains width, height, FPS, folder and
     * file.
     **/
    CONFIG_FRAME_OUTPUT_CMD,
    /**
     * Informs of the new size of the frame output system.
     **/
    FRAME_SIZE_UPDATE,

    /**
     * Updates the screen mode according to what is in the
     * {@link gaiasky.util.Settings#graphics} bean.
     */
    SCREEN_MODE_CMD,

    /**
     * Adds a screen notification which lasts for a little while.
     * It contains a title string, an array of string messages and a
     * float with the time in seconds.
     */
    SCREEN_NOTIFICATION_CMD,

    SCROLLED,

    SHOW_ABOUT_ACTION,

    /** Brings up the VR user interface. **/
    SHOW_VR_UI,
    /** Broadcasts a new VR controller which has just been initialized. **/
    VR_CONTROLLER_INFO,
    /** Broadcasts the newly created OpenXR driver. **/
    VR_DRIVER_LOADED,

    /** Shows the slave configuration window **/
    SHOW_SLAVE_CONFIG_ACTION,
    /** Slave connection event. Contains slave index, url and a boolean with the state (true-connected, false-disconnected). **/
    SLAVE_CONNECTION_EVENT,

    /**
     * Contains an optional boolean indicating whether debug info should be
     * shown or not. Otherwise, it toggles its state.
     **/
    SHOW_DEBUG_CMD,
    SHOW_KEYFRAMES_WINDOW_ACTION,
    /**
     * Creates and shows a new texture window with the contents of a texture or frame buffer.
     * <ol start='0'>
     * <li>window title [string]</li>
     * <li>frame buffer or texture object [FrameBuffer|Texture]</li>
     * <li>(optional) scale [float]</li>
     * <li>(optional) flipX [boolean]</li>
     * <li>(optional) flipY [boolean]</li>
     * </ol>
     */
    SHOW_TEXTURE_WINDOW_ACTION,
    SHOW_LAND_AT_LOCATION_ACTION,
    SHOW_LOAD_CATALOG_ACTION,
    SHOW_LOG_ACTION,
    /**
     * Procedural surface and atmosphere generation.
     */
    SHOW_PROCEDURAL_GEN_ACTION,
    /**
     * Shows the minimap window/interface. Contains a boolean with the state.
     */
    SHOW_MINIMAP_ACTION,

    /**
     * Shows the camera path file selector, contains the stage and the skin.
     **/
    SHOW_PLAYCAMERA_ACTION,

    SHOW_PREFERENCES_ACTION,

    SHOW_PER_OBJECT_VISIBILITY_ACTION,
    /**
     * Contains the object (instance of {@link IVisibilitySwitch}), the name of the object
     * and a boolean with the new visibility state.
     */
    PER_OBJECT_VISIBILITY_CMD,

    /**
     * Sets the force label flag on the given object which causes the label to always be rendered regardless of the solid angle.
     * Contains the entity, the name of the object, a boolean with the new force label state, and the
     * source object.
     */
    FORCE_OBJECT_LABEL_CMD,

    /**
     * Sets the label color for a given object.
     * Contains the entity, the name of the object, the new color
     * as a float array (RGBA) in [0,1], and the source object.
     */
    LABEL_COLOR_CMD,

    /**
     * Quit action, can contain optional Runnable to run on accept().
     **/
    SHOW_QUIT_ACTION,
    SHOW_SEARCH_ACTION,
    /**
     * Shows a window with a summary of the search object in the data (string)
     * as taken from the wikipedia API.
     */
    SHOW_WIKI_INFO_ACTION,
    /** Updates the wiki info window if it is open. **/
    UPDATE_WIKI_INFO_ACTION,
    /**
     * Shows a window with the Gaia or Hipparcos archive info for the object in the data,
     * which must be a {@link IStarFocus}.
     */
    SHOW_ARCHIVE_VIEW_ACTION,
    /** Updates the archive view if it is open. **/
    UPDATE_ARCHIVE_VIEW_ACTION,
    /**
     * Show uncertainties for Tycho star, if available. Contains the star.
     */
    SHOW_UNCERTAINTIES,
    /**
     * Contains following info:
     * <ol start='0'>
     * <li>current speed [u/s]</li>
     * <li>current yaw angle [deg]</li>
     * <li>current pitch angle [deg]</li>
     * <li>current roll angle [deg]</li>
     * <li>thrust factor</li>
     * <li>engine power [-1..1]</li>
     * <li>yaw power [-1..1]</li>
     * <li>pitch power [-1..1]</li>
     * <li>roll power [-1..1]</li>
     * </ol>
     **/
    SPACECRAFT_INFO,
    /**
     * Contains the spacecraft object after it has been loaded.
     **/
    SPACECRAFT_LOADED,

    /**
     * Contains following info about the nearest object:
     * <ol start='0'>
     * <li>nearest object name</li>
     * <li>distance to nearest object [u]</li>
     * </ol>
     */
    SPACECRAFT_NEAREST_INFO,
    /**
     * Level spacecraft command, contains boolean with state.
     **/
    SPACECRAFT_STABILISE_CMD,
    /**
     * Stop spacecraft, contains boolean with state.
     **/
    SPACECRAFT_STOP_CMD,
    /**
     * Decreases thrust.
     **/
    SPACECRAFT_THRUST_DECREASE_CMD,

    /**
     * Increases thrust.
     **/
    SPACECRAFT_THRUST_INCREASE_CMD,
    /**
     * Broadcasts the new thrust index.
     **/
    SPACECRAFT_THRUST_INFO,
    /**
     * Contains the integer index of the new thrust.
     **/
    SPACECRAFT_THRUST_SET_CMD,

    /**
     * Use new machine.
     * Contains the integer index of the new machine.
     */
    SPACECRAFT_MACHINE_SELECTION_CMD,

    /**
     * Informs a new machine is in place. Contains the machine.
     */
    SPACECRAFT_MACHINE_SELECTION_INFO,

    /**
     * Contains the speed limit index as in:
     * <ol start='0'>
     * <li>100 km/h       </li>
     * <li>0.5 * c        </li>
     * <li>0.8 * c        </li>
     * <li>0.9 * c        </li>
     * <li>0.99 * c       </li>
     * <li>0.99999 * c    </li>
     * <li>c (3e8 m/s)    </li>
     * <li>2*c            </li>
     * <li>10*c           </li>
     * <li>1000*c         </li>
     * <li>1 AU/s         </li>
     * <li>10 AU/s        </li>
     * <li>1000 AU/s      </li>
     * <li>10000 AU/s     </li>
     * <li>1 pc/s         </li>
     * <li>2 pc/s         </li>
     * <li>10 pc/s        </li>
     * <li>1000 pc/s      </li>
     * <li>No limit       </li>
     * </ol>
     **/
    SPEED_LIMIT_CMD,
    /**
     * Contains the star brightness multiplier.
     **/
    STAR_BRIGHTNESS_CMD,
    /**
     * Contains the star brightness power.
     **/
    STAR_BRIGHTNESS_POW_CMD,
    /**
     * Contains the star glow factor.
     **/
    STAR_GLOW_FACTOR_CMD,
    /**
     * Minimum star opacity. Contains the opacity in [0,1].
     **/
    STAR_BASE_LEVEL_CMD,
    /**
     * Set a new value for the star point size.
     **/
    STAR_POINT_SIZE_CMD,

    /**
     * Decrease star point size by
     * {@link gaiasky.util.Constants#SLIDER_STEP_TINY}
     **/
    STAR_POINT_SIZE_DECREASE_CMD,

    /**
     * Increase star point size by
     * {@link gaiasky.util.Constants#SLIDER_STEP_TINY}
     **/
    STAR_POINT_SIZE_INCREASE_CMD,

    /**
     * Reset star point size to original value
     **/
    STAR_POINT_SIZE_RESET_CMD,

    /**
     * Update the number of nearest stars in star groups. This updates
     * the maximum number of billboards, labels and
     * velocity vectors per star group.
     */
    STAR_GROUP_NEAREST_CMD,

    /**
     * Set the flag to render stars as billboards or not.
     */
    STAR_GROUP_BILLBOARD_CMD,

    /**
     * Set the global texture index for billboards rendered in {@link RenderGroup#BILLBOARD_STAR}.
     */
    BILLBOARD_TEXTURE_IDX_CMD,

    /**
     * Stereoscopic vision, side by side rendering. Contains the state boolean.
     **/
    STEREOSCOPIC_CMD,

    /**
     * Switches stereoscopic profile, contains the index of the new profile.
     **/
    STEREO_PROFILE_CMD,

    /**
     * Stops the current camera playing operation, if any
     **/
    STOP_CAMERA_PLAY,

    /**
     * Sets and unsets the target time. Contains a time (set), or nothing
     * (unset)
     **/
    TARGET_TIME_CMD,

    /**
     * Sets the tessellation quality. Contains the new quality in [{@link gaiasky.util.Constants#MIN_TESS_QUALITY}, {@link gaiasky.util.Constants#MAX_TESS_QUALITY}]
     */
    TESSELLATION_QUALITY_CMD,

    /**
     * Issues a change time command, contains the Instant object with the new time
     **/
    TIME_CHANGE_CMD,

    /**
     * Notifies of a change in the time, contains the Instant object
     **/
    TIME_CHANGE_INFO,

    /**
     * Issues the command to toggle the time. Contains the boolean indicating
     * the state (may be null).
     **/
    TIME_STATE_CMD,

    /**
     * Divide the pace by 2.
     **/
    TIME_WARP_DECREASE_CMD,

    /**
     * Double the pace.
     **/
    TIME_WARP_INCREASE_CMD,

    /**
     * Contains a double with the new warp value.
     **/
    TIME_WARP_CMD,

    /**
     * Contains the new time warp factor
     **/
    TIME_WARP_CHANGED_INFO,

    /**
     * Toggles the collapsed state of a GUI pane. Contains its name
     */
    TOGGLE_EXPANDCOLLAPSE_PANE_CMD,

    /**
     * Toggles minimap visibility
     **/
    TOGGLE_MINIMAP,

    /**
     * Toggles the pause of the update process. Contains the localised name.
     **/
    TOGGLE_UPDATEPAUSE,

    /**
     * Toggle the visibility of a component type.
     * Contains the name of the type and a boolean with the state (on/off).
     **/
    TOGGLE_VISIBILITY_CMD,

    /**
     * Updates the progress bar with the given name, contains a name and a float value in (0,1)
     * The progress bar is removed when the value is >= 1.
     */
    UPDATE_LOAD_PROGRESS,

    /**
     * Contains the tone mapping type as an {@link ToneMapping} or a
     * string in [AUTO|EXPOSURE|NONE].
     */
    TONEMAPPING_TYPE_CMD,

    // INPUT LISTENER EVENTS
    TOUCH_DOWN,
    TOUCH_DRAGGED,
    TOUCH_UP,

    /**
     * Contains the new turning speed.
     **/
    TURNING_SPEED_CMD,
    /**
     * Issues the command to reload the UI, contains the {@link gaiasky.util.GlobalResources} instance.
     */
    UI_RELOAD_CMD,
    /**
     * Displays a dialog to restart. Contains the text, or nothing.
     */
    SHOW_RESTART_ACTION,
    /**
     * Informs the UI theme has been reloaded. Contains the new skin.
     */
    UI_THEME_RELOAD_INFO,
    /**
     * Un-parks a runnable. Contains the identifier (String)
     **/
    UNPARK_RUNNABLE,
    /**
     * Contains the new value
     **/
    UPDATEPAUSE_CHANGED,
    /**
     * Updates the camera recorder. Contains dt (float), position (vector3d),
     * direction (vector3d) and up (vector3d)
     **/
    UPDATE_CAM_RECORDER,

    /**
     * Update external GUIs signal. Contains the dt in seconds.
     **/
    UPDATE_GUI,

    /**
     * Clears all cached shaders.
     */
    CLEAR_SHADERS,

    /**
     * Resets the main renderer.
     */
    RESET_RENDERER,

    /**
     * Contains an array of booleans with the visibility of each ComponentType,
     * in the same order returned by ComponentType.values()
     **/
    VISIBILITY_OF_COMPONENTS,

    /** New VR device connected. Contains the VRDevice object. **/
    VR_DEVICE_CONNECTED,

    /** VR device disconnected. Contains the VRDevice object. **/
    VR_DEVICE_DISCONNECTED,

    /**
     * Informs of the current selecting state. Contains the state (true|false) and a double in [0,1] with the completion
     * rate
     */
    VR_SELECTING_STATE,

    /** Show/hide controller GUI **/
    SHOW_CONTROLLER_GUI_ACTION,

    INVERT_X_CMD,
    INVERT_Y_CMD,

    /**
     * This event informs a new DISTANCE_SCALE_FACTOR is in place
     */
    NEW_DISTANCE_SCALE_FACTOR,

    /**
     * Broadcast a new location record, added to the location log
     */
    NEW_LOCATION_RECORD,

    /**
     * Command to update constellations. Contains the scene object.
     */
    CONSTELLATION_UPDATE_CMD,

    /**
     * Informs of the material generation, contains a boolean (start, finish)
     */
    PROCEDURAL_GENERATION_SURFACE_INFO,
    /**
     * Informs of the cloud generation, contains a boolean (start, finish)
     */
    PROCEDURAL_GENERATION_CLOUD_INFO,

    /**
     * Signals the start of a dataset download. Contains the dataset key (String) and the
     * HttpRequest object.
     */
    DATASET_DOWNLOAD_START_INFO,
    /**
     * Informs of the progress of a current download.
     * Contains the dataset key (String), the progress value (float),
     * the progress status with the percentage (String) and the speed (String)
     */
    DATASET_DOWNLOAD_PROGRESS_INFO,

    /**
     * Informs that the download has finished.
     * Contains the dataset key (String) and the status:
     * <ol start='0'>
     *     <li>ok.</li>
     *     <li>error.</li>
     *     <li>cancelled.</li>
     *     <li>not found.</li>
     * </ol>
     */
    DATASET_DOWNLOAD_FINISH_INFO,

    /**
     * Sparse virtual texture operation.
     * Triggers the processing of the SVT view determination buffer and the loading/updating
     * of the SVT cache and indirection buffers. Contains the {@link java.nio.FloatBuffer}
     * with the contents of the view determination buffer.
     */
    SVT_TILE_DETECTION_READY,

    /**
     * Broadcasts material component [1] which has at least one sparse virtual
     * texture, and the sparse virtual texture ID for that material [0].
     */
    SVT_MATERIAL_INFO,

    /** Set cache size **/
    SVT_CACHE_SIZE_CMD,

    /** Set the new upscale filter. Contains the new {@link UpscaleFilter} object. **/
    UPSCALE_FILTER_CMD;

}
