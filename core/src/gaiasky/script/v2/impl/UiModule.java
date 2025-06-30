/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.api.IGui;
import gaiasky.script.v2.api.UiAPI;
import gaiasky.util.LruCache;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.util.List;

/**
 * The UI module contains calls and methods to access, modify, and query the user interface.
 */
public class UiModule extends APIModule implements UiAPI {

    /** List of user-created custom textures. **/
    private LruCache<String, Texture> textures;

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public UiModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    /**
     * Initialize the textures LRU cache.
     */
    private void initializeTextures() {
        if (textures == null) {
            textures = new LruCache<>(100);
        }
    }

    @Override
    public void display_message(final int id,
                                final String msg,
                                final float x,
                                final float y,
                                final float r,
                                final float g,
                                final float b,
                                final float a,
                                final float size) {
        api.base.post_runnable(() -> em.post(Event.ADD_CUSTOM_MESSAGE, this, id, msg, x, y, r, g, b, a, size));
    }

    @Override
    public void display_message(final int id,
                                final String msg,
                                final double x,
                                final double y,
                                final double[] color,
                                final double size) {
        if (api.validator.checkNotNull(color, "color")
                && api.validator.checkLengths(color, 3, 4, "color")) {
            float a = color.length > 3 ? (float) color[3] : 1f;
            display_message(id, msg, (float) x, (float) y, (float) color[0], (float) color[1], (float) color[2], a, (float) size);
        }
    }

    public void display_message(final int id, final String message, final double x, final double y, final List<?> color, final double fontSize) {
        display_message(id, message, x, y, api.dArray(color), fontSize);
    }

    public void display_message(final int id,
                                final String message,
                                final float x,
                                final float y,
                                final float r,
                                final float g,
                                final float b,
                                final float a,
                                final int fontSize) {
        display_message(id, message, x, y, r, g, b, a, (float) fontSize);
    }

    @Override
    public void display_text(final int id,
                             final String msg,
                             final float x,
                             final float y,
                             final float max_w,
                             final float max_h,
                             final float r,
                             final float g,
                             final float b,
                             final float a,
                             final float size) {
        api.base.post_runnable(() -> em.post(Event.ADD_CUSTOM_TEXT, this, id, msg, x, y, max_w, max_h, r, g, b, a, size));
    }

    public void display_text(final int id,
                             final String text,
                             final float x,
                             final float y,
                             final float maxWidth,
                             final float maxHeight,
                             final float r,
                             final float g,
                             final float b,
                             final float a,
                             final int fontSize) {
        display_text(id, text, x, y, maxWidth, maxHeight, r, g, b, a, (float) fontSize);
    }

    @Override
    public void display_image(final int id,
                              final String path,
                              final float x,
                              final float y,
                              final float r,
                              final float g,
                              final float b,
                              final float a) {
        api.base.post_runnable(() -> {
            Texture tex = getTexture(path);
            em.post(Event.ADD_CUSTOM_IMAGE, this, id, tex, x, y, r, g, b, a);
        });
    }

    @Override
    public void display_image(final int id, final String path, final double x, final double y, final double[] color) {
        if (api.validator.checkNotNull(color, "color") && api.validator.checkLengths(color, 3, 4, "color")) {
            float a = color.length > 3 ? (float) color[3] : 1f;
            display_image(id, path, (float) x, (float) y, (float) color[0], (float) color[1], (float) color[2], a);
        }
    }

    public void display_image(final int id, final String path, final double x, final double y, final List<?> color) {
        display_image(id, path, x, y, api.dArray(color));
    }

    @Override
    public void display_image(final int id, final String path, final float x, final float y) {
        api.base.post_runnable(() -> {
            Texture tex = getTexture(path);
            em.post(Event.ADD_CUSTOM_IMAGE, this, id, tex, x, y);
        });
    }

    @Override
    public void remove_all_objects() {
        api.base.post_runnable(() -> em.post(Event.REMOVE_ALL_OBJECTS, this));
    }

    @Override
    public void remove_object(final int id) {
        api.base.post_runnable(() -> em.post(Event.REMOVE_OBJECTS, this, (Object) new int[]{id}));
    }

    @Override
    public void remove_objects(final int[] ids) {
        api.base.post_runnable(() -> em.post(Event.REMOVE_OBJECTS, this, (Object) ids));
    }

    public void remove_objects(final List<?> ids) {
        remove_objects(api.iArray(ids));
    }

    @Override
    public void enable() {
        api.base.post_runnable(() -> em.post(Event.DISPLAY_GUI_CMD, this, true, I18n.msg("notif.cleanmode")));
    }

    @Override
    public void disable() {
        api.base.post_runnable(() -> em.post(Event.DISPLAY_GUI_CMD, this, false, I18n.msg("notif.cleanmode")));
    }

    @Override
    public int get_client_width() {
        return Gdx.graphics.getWidth();
    }

    @Override
    public int get_client_height() {
        return Gdx.graphics.getHeight();
    }

    @Override
    public float[] get_position_and_size(String name) {
        IGui gui = GaiaSky.instance.mainGui;
        Actor actor = gui.getGuiStage().getRoot().findActor(name);
        if (actor != null) {
            float x = actor.getX();
            float y = actor.getY();
            // x and y relative to parent, so we need to add coordinates of
            // parents up to top
            Group parent = actor.getParent();
            while (parent != null) {
                x += parent.getX();
                y += parent.getY();
                parent = parent.getParent();
            }
            return new float[]{x, y, actor.getWidth(), actor.getHeight()};
        } else {
            return null;
        }

    }

    @Override
    public float get_ui_scale_factor() {
        return Settings.settings.program.ui.scale;
    }

    @Override
    public void expand_pane(String name) {
        if (api.validator.checkString(name,
                                      new String[]{"Time", "Camera", "Visibility", "VisualSettings", "Datasets", "LocationLog", "Bookmarks"},
                                      "panelName")) {
            api.base.post_runnable(() -> em.post(Event.EXPAND_COLLAPSE_PANE_CMD, this, name + "Component", true));
        }
    }

    @Override
    public void collapse_pane(String name) {
        if (api.validator.checkString(name,
                                      new String[]{"Time", "Camera", "Visibility", "VisualSettings", "Datasets", "LocationLog", "Bookmarks"},
                                      "panelName")) {
            api.base.post_runnable(() -> em.post(Event.EXPAND_COLLAPSE_PANE_CMD, this, name + "Component", false));
        }
    }

    @Override
    public void display_popup_notification(String msg) {
        if (api.validator.checkString(msg, "message")) {
            em.post(Event.POST_POPUP_NOTIFICATION, this, msg);
        }
    }

    @Override
    public void display_popup_notification(String msg, float duration) {
        if (api.validator.checkString(msg, "message")) {
            em.post(Event.POST_POPUP_NOTIFICATION, this, msg, duration);
        }
    }

    /**
     * Alias to {@link #display_popup_notification(String, float)}.
     */
    public void display_popup_notification(String message, Double duration) {
        display_popup_notification(message, duration.floatValue());
    }

    @Override
    public void set_headline_message(final String msg) {
        api.base.post_runnable(() -> em.post(Event.POST_HEADLINE_MESSAGE, this, msg));
    }

    @Override
    public void set_subhead_message(final String msg) {
        api.base.post_runnable(() -> em.post(Event.POST_SUBHEAD_MESSAGE, this, msg));
    }

    @Override
    public void clear_headline_message() {
        api.base.post_runnable(() -> em.post(Event.CLEAR_HEADLINE_MESSAGE, this));
    }

    @Override
    public void clear_subhead_message() {
        api.base.post_runnable(() -> em.post(Event.CLEAR_SUBHEAD_MESSAGE, this));
    }

    @Override
    public void clear_all_messages() {
        api.base.post_runnable(() -> em.post(Event.CLEAR_MESSAGES, this));
    }

    @Override
    public void set_crosshair_visibility(boolean visible) {
        set_focus_crosshair_visibility(visible);
        set_closest_crosshair_visibility(visible);
        set_home_crosshair_visibility(visible);
    }

    @Override
    public void set_focus_crosshair_visibility(boolean visible) {
        api.base.post_runnable(() -> em.post(Event.CROSSHAIR_FOCUS_CMD, this, visible));
    }

    @Override
    public void set_closest_crosshair_visibility(boolean visible) {
        api.base.post_runnable(() -> em.post(Event.CROSSHAIR_CLOSEST_CMD, this, visible));
    }

    @Override
    public void set_home_crosshair_visibility(boolean visible) {
        api.base.post_runnable(() -> em.post(Event.CROSSHAIR_HOME_CMD, this, visible));
    }

    @Override
    public void set_minimap_visibility(boolean visible) {
        api.base.post_runnable(() -> em.post(Event.MINIMAP_DISPLAY_CMD, this, visible));
    }

    @Override
    public void preload_texture(String path) {
        preload_textures(new String[]{path});
    }

    @Override
    public void preload_textures(String[] paths) {
        initializeTextures();
        for (final String path : paths) {
            // This only works in async mode!
            api.base.post_runnable(() -> api.assetManager.load(path, Texture.class));
            while (!api.assetManager.isLoaded(path)) {
                api.base.sleep_frames(1);
            }
            Texture tex = api.assetManager.get(path, Texture.class);
            textures.put(path, tex);
        }
    }

    private Texture getTexture(String path) {
        if (textures == null || !textures.containsKey(path)) {
            preload_texture(path);
        }
        return textures.get(path);
    }

    @Override
    public void reload() {
        EventManager.publish(Event.UI_RELOAD_CMD, this, GaiaSky.instance.getGlobalResources());
    }

}
