/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.UiModule;

/**
 * API definition for the UI module, {@link UiModule}.
 * <p>
 * The UI module contains calls and methods to access, modify, and query the user interface.
 */
public interface UiAPI {
    /**
     * Add a new one-line message in the screen with the given id and the given
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
    void display_message(int id,
                         String message,
                         float x,
                         float y,
                         float r,
                         float g,
                         float b,
                         float a,
                         float fontSize);

    /**
     * Same as {@link #display_image(int, String, float, float, float, float, float, float)} but
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
    void display_message(int id,
                         String message,
                         double x,
                         double y,
                         double[] color,
                         double fontSize);

    /**
     * Add a new multi-line text in the screen with the given id, coordinates
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
    void display_text(int id,
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
     * Add a new image object at the given coordinates. If an object already
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
    void display_image(int id,
                       String path,
                       float x,
                       float y);

    /**
     * Add a new image object at the given coordinates. If an object already
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
    void display_image(int id,
                       final String path,
                       float x,
                       float y,
                       float r,
                       float g,
                       float b,
                       float a);

    /**
     * Same as {@link #display_image(int, String, float, float, float, float, float, float)} but
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
    void display_image(int id,
                       final String path,
                       double x,
                       double y,
                       double[] color);

    /**
     * Remove all objects (messages, texts and images).
     */
    void remove_all_objects();

    /**
     * Remove the object with the given id.
     * <p>
     * This object is assumed to have been added with
     * {@link #display_message(int, String, float, float, float, float, float, float, float)},
     * {@link #display_text(int, String, float, float, float, float, float, float, float, float, float)}, or
     * {@link #display_image(int, String, float, float)}.
     *
     * @param id Integer with the integer id of the object to remove.
     */
    void remove_object(int id);

    /**
     * Remove the items with the given ids. They can either be messages, images or
     * whatever else.
     * <p>
     * The objects to remove are assumed to have been added with
     * {@link #display_message(int, String, float, float, float, float, float, float, float)},
     * {@link #display_text(int, String, float, float, float, float, float, float, float, float, float)}, or
     * {@link #display_image(int, String, float, float)}.
     *
     * @param ids Vector with the integer ids of the objects to remove.
     */
    void remove_objects(int[] ids);

    /**
     * Enable the GUI rendering. This makes the user interface
     * to be rendered and updated again if it was previously disabled. Otherwise, it has
     * no effect.
     */
    void enable();

    /**
     * Disable the GUI rendering. This causes the user interface
     * to no longer be rendered or updated.
     */
    void disable();

    /**
     * Get the current scale factor applied to the UI.
     *
     * @return The scale factor.
     */
    float get_ui_scale_factor();

    /**
     * Return the width of the client area in logical pixels.
     *
     * @return The width in logical pixels.
     */
    int get_client_width();

    /**
     * Return the height of the client area in logical pixels.
     *
     * @return The height in logical pixels.
     */
    int get_client_height();

    /**
     * Return the size and position of the GUI actor that goes by the given
     * name, or null if such element does not exist.
     * <p>
     * The actor names are given at creation, and are internal to the Gaia Sky source code. You need to dive into the
     * source code if you want to manipulate GUI actors.
     * <p>
     * <strong>Warning: This will
     * only work in asynchronous mode.</strong>
     *
     * @param name The name of the gui element.
     *
     * @return A vector of floats with the position (0, 1) of the bottom left
     *         corner in pixels from the bottom-left of the screen and the size
     *         (2, 3) in pixels of the element.
     */
    float[] get_position_and_size(String name);

    /**
     * Expand the UI pane with the given name. Possible names are:
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
    void expand_pane(String panelName);

    /**
     * Collapse the UI pane with the given name. Possible names are:
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
    void collapse_pane(String panelName);

    /**
     * Display a popup notification on the screen with the given contents for the default duration of 8 seconds.
     * The notification appears at the top-right of the screen and stays there until the duration time elapses, then it disappears.
     *
     * @param message The notification text.
     */
    void display_popup_notification(String message);

    /**
     * Display a popup notification on the screen for the given duration.
     * The notification appears at the top-right of the screen and stays there until the duration time elapses, then it disappears.
     *
     * @param message  The notification text.
     * @param duration The duration, in seconds, until the notification automatically disappears. Set this to a negative number so that the
     *                 notification never expires. If this is the case, the notification must be manually closed by the user.
     */
    void display_popup_notification(String message, float duration);

    /**
     * Set the contents of the headline message. The headline message appears in the middle of the screen with a big font.
     *
     * @param headline The headline text.
     */
    void set_headline_message(final String headline);

    /**
     * Set the contents of the sub-header message. The sub-header message appears just below the headline message, in the middle of the screen,
     * with a somewhat smaller font.
     *
     * @param subhead The sub-header text.
     */
    void set_subhead_message(final String subhead);

    /**
     * Clear the content of the headline message. After this method is called, the headline message disappears from screen.
     */
    void clear_headline_message();

    /**
     * Clear the content of the sub-header message. After this method is called, the sub-header message disappears from screen.
     */
    void clear_subhead_message();

    /**
     * Clear both the headline and the sub-header messages. After this method is called, both the headline and the sub-header messages
     * disappear from screen.
     */
    void clear_all_messages();

    /**
     * Set the visibility of all crosshairs. Affects the visibility of the focus, closest, and home objects' crosshairs.
     *
     * @param visible The visibility state, which applies to all cross-hairs.
     */
    void set_crosshair_visibility(boolean visible);

    /**
     * Set the visibility of the focus object crosshair.
     *
     * @param visible The visibility state.
     */
    void set_focus_crosshair_visibility(boolean visible);

    /**
     * Set the visibility of the closest object crosshair.
     *
     * @param visible The visibility state.
     */
    void set_closest_crosshair_visibility(boolean visible);

    /**
     * Set the visibility of the home object crosshair.
     *
     * @param visible The visibility state.
     */
    void set_home_crosshair_visibility(boolean visible);

    /**
     * Set the visibility of the minimap.
     *
     * @param visible The visibility state.
     */
    void set_minimap_visibility(boolean visible);

    /**
     * Preload the given image file paths as textures for later use. They will be cached
     * for the subsequent uses.
     *
     * @param paths The texture paths as an array of strings.
     */
    void preload_textures(String[] paths);

    /**
     * Preload the given image as a texture for later use. The texture will
     * be cached for later use.
     *
     * @param path The path of the image file to preload as a string.
     */
    void preload_texture(String path);
}
