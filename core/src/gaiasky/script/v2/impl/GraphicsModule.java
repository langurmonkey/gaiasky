/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect;
import gaiasky.script.v2.api.GraphicsAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;

import java.util.Locale;

/**
 * The graphics module contains methods and calls that modify and query the graphics and rendering system.
 * <p>
 * The methods in this API are organized like this:
 * <ul>
 *     <li><code>mode_[...]()</code> &mdash; activate or deactivate camera modes</li>
 *     <li><code>effect_[...]()</code> &mdash; activate or deactivate post-processing effects</li>
 *     <li><code>set_[...]()</code> &mdash; set properties or settings
 *     <ul>
 *     <li><code>set_image_[...]()</code> &mdash; set image levels</li>
 *     </ul>
 *     </li>
 * </ul>
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
    public void set_image_brightness(double level) {
        if (api.validator.checkNum(level, -1d, 1d, "brightness")) api.base.post_runnable(() -> em.post(Event.BRIGHTNESS_CMD, this, (float) level));
    }

    public void set_image_brightness(long level) {
        set_image_brightness((double) level);
    }

    @Override
    public void set_image_contrast(double level) {
        if (api.validator.checkNum(level, 0d, 2d, "contrast")) api.base.post_runnable(() -> em.post(Event.CONTRAST_CMD, this, (float) level));
    }

    public void set_image_contrast(long level) {
        set_image_contrast((double) level);
    }

    @Override
    public void set_image_hue(double level) {
        if (api.validator.checkNum(level, 0d, 2d, "hue")) api.base.post_runnable(() -> em.post(Event.HUE_CMD, this, (float) level));
    }

    public void set_image_hue(long level) {
        set_image_hue((double) level);
    }

    @Override
    public void set_image_saturation(double level) {
        if (api.validator.checkNum(level, 0d, 2d, "saturation")) api.base.post_runnable(() -> em.post(Event.SATURATION_CMD, this, (float) level));
    }

    public void set_image_saturation(long level) {
        set_image_saturation((double) level);
    }

    @Override
    public void set_gamma_correction(double level) {
        if (api.validator.checkNum(level, 0d, 3d, "gamma correction")) api.base.post_runnable(() -> em.post(Event.GAMMA_CMD, this, (float) level));
    }

    public void set_gamma_correction(long level) {
        set_gamma_correction((double) level);
    }

    @Override
    public void set_hdr_tone_mapping(String type) {
        if (api.validator.checkString(type, new String[]{"auto", "AUTO", "exposure", "EXPOSURE", "none", "NONE"}, "tone mapping type"))
            api.base.post_runnable(() -> em.post(Event.TONEMAPPING_TYPE_CMD, this, Settings.ToneMapping.valueOf(type.toUpperCase(Locale.ROOT))));
    }

    @Override
    public void set_exposure_tone_mapping(double level) {
        if (api.validator.checkNum(level, 0d, 20d, "exposure")) api.base.post_runnable(() -> em.post(Event.EXPOSURE_CMD, this, (float) level));
    }

    public void set_exposure_tone_mapping(long level) {
        set_exposure_tone_mapping((double) level);
    }

    @Override
    public void mode_cubemap(boolean state, String projection) {
        if (api.validator.checkStringEnum(projection, CubmeapProjectionEffect.CubemapProjection.class, "projection")) {
            CubmeapProjectionEffect.CubemapProjection newProj = CubmeapProjectionEffect.CubemapProjection.valueOf(projection.toUpperCase(Locale.ROOT));
            api.base.post_runnable(() -> em.post(Event.CUBEMAP_CMD, this, state, newProj));
        }
    }

    @Override
    public void mode_panorama(boolean state) {
        api.base.post_runnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubmeapProjectionEffect.CubemapProjection.EQUIRECTANGULAR));
    }

    @Override
    public void mode_reprojection(String mode) {
        if (api.validator.checkStringEnum(mode, Settings.ReprojectionMode.class, "re-projection mode")) {
            Settings.ReprojectionMode newMode = Settings.ReprojectionMode.valueOf(mode.toUpperCase(Locale.ROOT));
            api.base.post_runnable(() -> em.post(Event.REPROJECTION_CMD, this, newMode != Settings.ReprojectionMode.DISABLED, newMode));
        }
    }

    @Override
    public void set_back_buffer_scale(float scale) {
        if (api.validator.checkNum(scale, 0.5f, 4f, "back buffer scale")) {
            api.base.post_runnable(() -> GaiaSky.instance.resetDynamicResolution());
            api.base.post_runnable(() -> em.post(Event.BACKBUFFER_SCALE_CMD, this, scale));
        }
    }

    @Override
    public void set_index_of_refraction(float ior) {
        em.post(Event.INDEXOFREFRACTION_CMD, this, ior);
    }

    @Override
    public void mode_planetarium(boolean state) {
        api.base.post_runnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubmeapProjectionEffect.CubemapProjection.AZIMUTHAL_EQUIDISTANT));
    }

    @Override
    public void set_cubemap_resolution(int resolution) {
        if (api.validator.checkNum(resolution, 20, 15000, "resolution")) {
            api.base.post_runnable(() -> em.post(Event.CUBEMAP_RESOLUTION_CMD, this, resolution));
        }
    }

    @Override
    public void set_cubemap_projection(String projection) {
        if (api.validator.checkStringEnum(projection, CubmeapProjectionEffect.CubemapProjection.class, "projection")) {
            CubmeapProjectionEffect.CubemapProjection newProj = CubmeapProjectionEffect.CubemapProjection.valueOf(projection.toUpperCase(Locale.ROOT));
            em.post(Event.CUBEMAP_PROJECTION_CMD, this, newProj);
        }
    }

    @Override
    public void mode_orthosphere(boolean state) {
        api.base.post_runnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubmeapProjectionEffect.CubemapProjection.ORTHOSPHERE));
    }

    @Override
    public void mode_stereoscopic(boolean state) {
        api.base.post_runnable(() -> em.post(Event.STEREOSCOPIC_CMD, this, state));
    }

    @Override
    public void set_stereo_profile(int index) {
        var profiles = Settings.StereoProfile.values();
        api.validator.checkIndex(index, profiles, "profile index");
        api.base.post_runnable(() -> em.post(Event.STEREO_PROFILE_CMD, this, profiles[index]));
    }

    @Override
    public long get_current_frame_number() {
        return GaiaSky.instance.frames;
    }

    @Override
    public void effect_lens_flare(boolean state) {
        api.base.post_runnable(() -> em.post(Event.LENS_FLARE_CMD, this, state ? 1f : 0f));
    }

    @Override
    public void effect_lens_flare(double value) {
        if (api.validator.checkNum(value, Constants.MIN_LENS_FLARE_STRENGTH, Constants.MAX_LENS_FLARE_STRENGTH, "strength")) {
            api.base.post_runnable(() -> em.post(Event.LENS_FLARE_CMD, this, (float) value));
        }
    }

    @Override
    public void effect_motion_blur(boolean active) {
        var strength = active ? 0.8f : 0f;
        api.base.post_runnable(() -> em.post(Event.MOTION_BLUR_CMD, this, strength));
    }

    @Override
    public void effect_motion_blur(double value) {
        api.base.post_runnable(() -> em.post(Event.MOTION_BLUR_CMD, this, (float) value));
    }

    @Override
    public void effect_star_glow(boolean state) {
        api.base.post_runnable(() -> em.post(Event.LIGHT_GLOW_CMD, this, state));
    }

    @Override
    public void effect_bloom(float value) {
        if (api.validator.checkNum(value, 0f, 1f, "bloom strength")) {
            api.base.post_runnable(() -> em.post(Event.BLOOM_CMD, this, value));
        }
    }

    @Override
    public void effect_chromatic_aberration(float value) {
        if (api.validator.checkNum(value,
                                   Constants.MIN_CHROMATIC_ABERRATION_AMOUNT,
                                   Constants.MAX_CHROMATIC_ABERRATION_AMOUNT,
                                   "chromatic aberration amount")) {
            api.base.post_runnable(() -> em.post(Event.CHROMATIC_ABERRATION_CMD, this, value));
        }
    }

    public void effect_bloom(int level) {
        effect_bloom((float) level);
    }

    @Override
    public void set_smooth_lod_transitions(boolean value) {
        api.base.post_runnable(() -> em.post(Event.OCTREE_PARTICLE_FADE_CMD, this, value));
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
    public void set_star_brightness_power(float value) {
        if (api.validator.checkFinite(value, "brightness-pow")) {
            // Default to 1 in case of overflow to maintain compatibility.
            if (value < Constants.MIN_STAR_BRIGHTNESS_POW || value > Constants.MAX_STAR_BRIGHTNESS_POW) {
                value = 1.0f;
            }
            em.post(Event.STAR_BRIGHTNESS_POW_CMD, this, value);
        }
    }

    @Override
    public void set_star_glow_factor(float value) {
        if (api.validator.checkNum(value, 0.001, 5, "glow-factor")) {
            em.post(Event.STAR_GLOW_FACTOR_CMD, this, value);
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
    public void set_star_brightness(final float value) {
        if (api.validator.checkNum(value, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "brightness")) em.post(Event.STAR_BRIGHTNESS_CMD,
                                                                                                             this,
                                                                                                             MathUtilsDouble.lint(value,
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

    public void set_star_base_opacity(long opacity) {
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
    public void set_orbit_solid_angle_threshold(float deg) {
        if (api.validator.checkNum(deg, 0.0f, 180f, "solid-angle")) {
            api.base.post_runnable(() -> EventManager.publish(Event.ORBIT_SOLID_ANGLE_TH_CMD, this, (double) deg));
        }
    }

    @Override
    public void set_limit_fps(double fps) {
        if (api.validator.checkNum(fps, -Double.MAX_VALUE, Constants.MAX_FPS, "limitFps")) {
            em.post(Event.LIMIT_FPS_CMD, this, fps);
        }
    }

    @Override
    public void set_limit_fps(int fps) {
        set_limit_fps((double) fps);
    }

}
