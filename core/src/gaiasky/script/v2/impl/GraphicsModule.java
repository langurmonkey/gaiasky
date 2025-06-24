/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.GraphicsAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;

/**
 * The graphics module contains methods and calls that modify and query the graphics and rendering system.
 */
public class GraphicsModule extends APIModule implements GraphicsAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public GraphicsModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void set_ambient_light(final float value) {
        if (api.validator.checkNum(value, Constants.MIN_AMBIENT_LIGHT, Constants.MAX_AMBIENT_LIGHT, "ambientLight"))
            api.base.post_runnable(() -> em.post(Event.AMBIENT_LIGHT_CMD, this, value));
    }

    public void set_ambient_light(final int value) {
        set_ambient_light((float) value);
    }

    @Override
    public void set_star_brightness_power(float power) {
        if (api.validator.checkFinite(power, "brightness-pow")) {
            // Default to 1 in case of overflow to maintain compatibility.
            if (power < Constants.MIN_STAR_BRIGHTNESS_POW || power > Constants.MAX_STAR_BRIGHTNESS_POW) {
                power = 1.0f;
            }
            em.post(Event.STAR_BRIGHTNESS_POW_CMD, this, power);
        }
    }

    @Override
    public void set_star_glow_factor(float glowFactor) {
        if (api.validator.checkNum(glowFactor, 0.001, 5, "glow-factor")) {
            em.post(Event.STAR_GLOW_FACTOR_CMD, this, glowFactor);
        }
    }

    @Override
    public float get_star_brightness() {
        return MathUtilsDouble.lint(Settings.settings.scene.star.brightness,
                                    Constants.MIN_STAR_BRIGHTNESS,
                                    Constants.MAX_STAR_BRIGHTNESS,
                                    Constants.MIN_SLIDER,
                                    Constants.MAX_SLIDER);
    }

    @Override
    public void set_star_brightness(final float brightness) {
        if (api.validator.checkNum(brightness, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "brightness")) em.post(Event.STAR_BRIGHTNESS_CMD,
                                                                                                                  this,
                                                                                                                  MathUtilsDouble.lint(brightness,
                                                                                                                                       Constants.MIN_SLIDER,
                                                                                                                                       Constants.MAX_SLIDER,
                                                                                                                                       Constants.MIN_STAR_BRIGHTNESS,
                                                                                                                                       Constants.MAX_STAR_BRIGHTNESS));
    }

    public void set_star_brightness(final int brightness) {
        set_star_brightness((float) brightness);
    }

    @Override
    public float get_point_size() {
        return MathUtilsDouble.lint(Settings.settings.scene.star.pointSize,
                                    Constants.MIN_STAR_POINT_SIZE,
                                    Constants.MAX_STAR_POINT_SIZE,
                                    Constants.MIN_SLIDER,
                                    Constants.MAX_SLIDER);
    }

    @Override
    public void set_point_size(final float size) {
        if (api.validator.checkNum(size, Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, "size"))
            em.post(Event.STAR_POINT_SIZE_CMD, this, size);
    }

    public void set_point_size(final long size) {
        set_point_size((float) size);
    }

    @Override
    public float get_star_base_opacity() {
        return MathUtilsDouble.lint(Settings.settings.scene.star.opacity[0],
                                    Constants.MIN_STAR_MIN_OPACITY,
                                    Constants.MAX_STAR_MIN_OPACITY,
                                    Constants.MIN_SLIDER,
                                    Constants.MAX_SLIDER);
    }

    @Override
    public void set_star_base_opacity(float opacity) {
        if (api.validator.checkNum(opacity, Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, "min-opacity"))
            EventManager.publish(Event.STAR_BASE_LEVEL_CMD, this, opacity);
    }

    public void setStarBaseOpacity(long opacity) {
        set_star_base_opacity((float) opacity);
    }

    @Override
    public void set_star_texture_index(int index) {
        if (api.validator.checkNum(index, 1, 4, "index")) {
            EventManager.publish(Event.BILLBOARD_TEXTURE_IDX_CMD, this, index);
        }
    }

    @Override
    public void set_star_set_metadata_size(int n) {
        if (api.validator.checkNum(n, 1, 1000000, "nNearest")) {
            EventManager.publish(Event.STAR_GROUP_NEAREST_CMD, this, n);
        }
    }

    @Override
    public void set_star_set_billboard(boolean flag) {
        EventManager.publish(Event.STAR_GROUP_BILLBOARD_CMD, this, flag);
    }

    @Override
    public void set_orbit_solid_angle_threshold(float angleDeg) {
        if (api.validator.checkNum(angleDeg, 0.0f, 180f, "solid-angle")) {
            api.base.post_runnable(() -> EventManager.publish(Event.ORBIT_SOLID_ANGLE_TH_CMD, this, (double) angleDeg));
        }
    }

    @Override
    public void set_limit_fps(double limitFps) {
        if (api.validator.checkNum(limitFps, -Double.MAX_VALUE, Constants.MAX_FPS, "limitFps")) {
            em.post(Event.LIMIT_FPS_CMD, this, limitFps);
        }
    }

    @Override
    public void set_limit_fps(int limitFps) {
        set_limit_fps((double) limitFps);
    }

}
