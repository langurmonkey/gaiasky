/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import gaiasky.data.group.DatasetOptions;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.script.v2.impl.APIv2;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.CatalogManager;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.LruCache;
import gaiasky.util.Settings;
import gaiasky.util.coord.IPythonCoordinatesProvider;
import uk.ac.starlink.util.DataSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of the Gaia Sky scripting API located at {@link IScriptingInterface}. This implementation uses
 * mainly the event manager to communicate with the rest of Gaia Sky.
 */
@SuppressWarnings({"unused", "WeakerAccess", "SingleStatementInBlock", "SameParameterValue"})
public final class EventScriptingInterface implements IScriptingInterface  {
    private static final Log logger = Logger.getLogger(EventScriptingInterface.class);

    // Reference to the event manager
    private final EventManager em;
    private LruCache<String, Texture> textures;
    private TrajectoryUtils trajectoryUtils;

    /** APIv2 object reference. This is the gateway to access the methods and calls in APIv2. **/
    public final APIv2 apiv2;

    public EventScriptingInterface(final AssetManager manager, final CatalogManager catalogManager) {
        this.em = EventManager.instance;
        this.apiv2 = new APIv2(manager, catalogManager);
    }

    private void initializeTextures() {
        if (textures == null) {
            textures = new LruCache<>(100);
        }
    }

    private double[] dArray(List<?> l) {
        if (l == null) return null;
        double[] res = new double[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Double) o;
        }
        return res;
    }

    private int[] iArray(List<?> l) {
        if (l == null) return null;
        int[] res = new int[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Integer) o;
        }
        return res;
    }

    @Override
    public void activateRealTimeFrame() {
        apiv2.time.activate_real_time_frame();
    }

    @Override
    public void activateSimulationTimeFrame() {
        apiv2.time.activate_simulation_time_frame();
    }

    @Override
    public void displayPopupNotification(String message) {
        apiv2.ui.display_popup_notification(message);
    }

    @Override
    public void displayPopupNotification(String message, float duration) {
        apiv2.ui.display_popup_notification(message, duration);
    }

    public void displayPopupNotification(String message, Double duration) {
        apiv2.ui.display_popup_notification(message, duration);
    }

    @Override
    public void setHeadlineMessage(final String headline) {
        apiv2.ui.set_headline_message(headline);
    }

    @Override
    public void setSubheadMessage(final String subhead) {
        apiv2.ui.set_subhead_message(subhead);
    }

    @Override
    public void clearHeadlineMessage() {
        apiv2.ui.clear_headline_message();
    }

    @Override
    public void clearSubheadMessage() {
        apiv2.ui.clear_subhead_message();
    }

    @Override
    public void clearAllMessages() {
        apiv2.ui.clear_all_messages();
    }

    @Override
    public void disableInput() {
        apiv2.input.disable();
    }

    @Override
    public void enableInput() {
        apiv2.input.enable();
    }

    @Override
    public void setCinematicCamera(boolean cinematic) {
        apiv2.camera.interactive.set_cinematic(cinematic);
    }

    @Override
    public void setCameraFocus(final String focusName) {
        apiv2.camera.focus_mode(focusName);
    }

    @Override
    public void setCameraFocus(final String focusName, final float waitTimeSeconds) {
        apiv2.camera.focus_mode(focusName, waitTimeSeconds);
    }

    public void setCameraFocus(final String focusName, final int waitTimeSeconds) {
        apiv2.camera.focus_mode(focusName, waitTimeSeconds);
    }

    public void setCameraFocus(final Entity entity, final float waitTimeSeconds) {
        apiv2.camera.focus_mode(entity, waitTimeSeconds);
    }

    @Override
    public void setCameraFocusInstant(final String focusName) {
        apiv2.camera.focus_mode_instant(focusName);
    }

    @Override
    public void setCameraFocusInstantAndGo(final String focusName) {
        apiv2.camera.focus_mode_instant_go(focusName);
    }

    public void setCameraFocusInstantAndGo(final String focusName, final boolean sleep) {
        apiv2.camera.focus_mode_instant_go(focusName, sleep);
    }

    @Override
    public void setCameraLock(final boolean lock) {
        apiv2.camera.set_focus_lock(lock);
    }

    @Override
    public void setCameraCenterFocus(boolean centerFocus) {
        apiv2.camera.center_focus(centerFocus);
    }

    @Override
    public void setCameraFree() {
        apiv2.camera.free_mode();
    }

    @Override
    public void setCameraPostion(final double[] vec) {
        apiv2.camera.set_position(vec);
    }

    @Override
    public void setCameraPosition(double x, double y, double z) {
        apiv2.camera.set_position(x, y, z);
    }

    @Override
    public void setCameraPosition(double x, double y, double z, String units) {
        apiv2.camera.set_position(x, y, z, units);
    }

    @Override
    public void setCameraPosition(double x, double y, double z, boolean immediate) {
        apiv2.camera.set_position(x, y, z, immediate);
    }

    @Override
    public void setCameraPosition(double x, double y, double z, String units, boolean immediate) {
        apiv2.camera.set_position(x, y, z, units, immediate);
    }

    public void setCameraPosition(final List<?> vec, boolean immediate) {
        apiv2.camera.set_position(vec, immediate);
    }

    @Override
    public void setCameraPosition(double[] position, boolean immediate) {
        apiv2.camera.set_position(position, immediate);
    }

    @Override
    public void setCameraPosition(double[] position, String units, boolean immediate) {
        apiv2.camera.set_position(position, units, immediate);
    }

    public void setCameraPosition(List<Double> position, String units, boolean immediate) {
        apiv2.camera.set_position(position, units, immediate);
    }

    @Override
    public double[] getCameraPosition() {
        return apiv2.camera.get_position();
    }

    @Override
    public double[] getCameraPosition(String units) {
        return apiv2.camera.get_position(units);
    }

    @Override
    public void setCameraPosition(final double[] position) {
        apiv2.camera.set_position(position);
    }

    @Override
    public void setCameraPosition(double[] position, String units) {
        apiv2.camera.set_position(position, units);
    }

    public void setCameraPosition(final List<?> vec) {
        apiv2.camera.set_position(vec);
    }

    public void setCameraPosition(final List<?> vec, String units) {
        apiv2.camera.set_position(vec, units);
    }

    public void setCameraDirection(final List<?> dir, final boolean immediate) {
        apiv2.camera.set_direction(dir, immediate);
    }

    @Override
    public void setCameraDirection(double[] direction, boolean immediate) {
        apiv2.camera.set_direction(direction, immediate);
    }

    @Override
    public double[] getCameraDirection() {
        return apiv2.camera.get_direction();
    }

    @Override
    public void setCameraDirection(final double[] direction) {
        apiv2.camera.set_direction(direction);
    }

    @Override
    public void setCameraDirectionEquatorial(double alpha, double delta) {
        apiv2.camera.set_direction_equatorial(alpha, delta);
    }

    @Override
    public void setCameraDirectionGalactic(double l, double b) {
        apiv2.camera.set_direction_galactic(l, b);
    }

    public void setCameraDirection(final List<?> dir) {
        apiv2.camera.set_direction(dir);
    }

    public void setCameraUp(final List<?> up, final boolean immediate) {
        apiv2.camera.set_up(up, immediate);
    }

    @Override
    public void setCameraUp(final double[] up, final boolean immediate) {
        apiv2.camera.set_up(up, immediate);
    }

    @Override
    public double[] getCameraUp() {
        return apiv2.camera.get_up();
    }

    @Override
    public void setCameraUp(final double[] up) {
        apiv2.camera.set_up(up);
    }

    public void setCameraUp(final List<?> up) {
        apiv2.camera.set_up(up);
    }

    @Override
    public void setCameraOrientationQuaternion(double[] quaternion) {
        apiv2.camera.set_orientation_quaternion(quaternion);
    }

    public void setCameraOrientationQuaternion(List<?> quaternion) {
        apiv2.camera.set_orientation_quaternion(quaternion);
    }

    @Override
    public double[] getCameraOrientationQuaternion() {
        return apiv2.camera.get_orientation_quaternion();
    }

    public void pointAtSkyCoordinate(double ra, double dec) {
        apiv2.camera.point_at_equatorial(ra, dec);
    }

    public void pointAtSkyCoordinate(long ra, long dec) {
        apiv2.camera.point_at_equatorial(ra, dec);
    }

    @Override
    public void setCameraPositionAndFocus(String focus, String other, double rotation, double solidAngle) {
        apiv2.camera.set_position_and_focus(focus, other, rotation, solidAngle);
    }

    public void setCameraPositionAndFocus(String focus, String other, long rotation, long solidAngle) {
        apiv2.camera.set_position_and_focus(focus, other, rotation, solidAngle);
    }


    @Override
    public double getCameraSpeed() {
        return apiv2.camera.interactive.get_speed();
    }

    @Override
    public void setCameraSpeed(final float speed) {
        apiv2.camera.interactive.speed_setting(speed);
    }

    public void setCameraSpeed(final int speed) {
        apiv2.camera.interactive.speed_setting(speed);
    }

    @Override
    public void setCameraRotationSpeed(float speed) {
        apiv2.camera.interactive.rotation_speed_setting(speed);
    }

    public void setCameraRotationSpeed(final int speed) {
        apiv2.camera.interactive.rotation_speed_setting(speed);
    }

    @Override
    public void setRotationCameraSpeed(final float speed) {
        apiv2.camera.interactive.rotation_speed_setting(speed);
    }

    public void setRotationCameraSpeed(final int speed) {
        apiv2.camera.interactive.rotation_speed_setting(speed);
    }

    @Override
    public void setCameraTurningSpeed(float speed) {
        apiv2.camera.interactive.turning_speed_setting(speed);
    }

    public void setCameraTurningSpeed(final int speed) {
        apiv2.camera.interactive.turning_speed_setting(speed);
    }

    @Override
    public void setTurningCameraSpeed(final float speed) {
        apiv2.camera.interactive.turning_speed_setting(speed);
    }

    public void setTurningCameraSpeed(final int speed) {
        apiv2.camera.interactive.turning_speed_setting(speed);
    }

    @Override
    public void setCameraSpeedLimit(int index) {
        apiv2.camera.set_max_speed(index);
    }

    @Override
    public void setCameraTrackingObject(String objectName) {
        apiv2.camera.set_tracking_object(objectName);
    }

    @Override
    public void removeCameraTrackingObject() {
        apiv2.camera.remove_tracking_object();
    }

    @Override
    public void setCameraOrientationLock(boolean lock) {
        apiv2.camera.set_orientation_lock(lock);
    }

    @Override
    public void cameraForward(final double value) {
        apiv2.camera.interactive.add_forward(value);
    }

    public void cameraForward(final long value) {
        apiv2.camera.interactive.add_forward(value);
    }

    @Override
    public void cameraRotate(final double deltaX, final double deltaY) {
        apiv2.camera.interactive.add_rotation(deltaX, deltaY);
    }

    public void cameraRotate(final double deltaX, final long deltaY) {
        apiv2.camera.interactive.add_rotation(deltaX, deltaY);
    }

    public void cameraRotate(final long deltaX, final double deltaY) {
        apiv2.camera.interactive.add_rotation(deltaX, deltaY);
    }

    @Override
    public void cameraRoll(final double roll) {
        apiv2.camera.interactive.add_roll(roll);
    }

    public void cameraRoll(final long roll) {
        apiv2.camera.interactive.add_roll(roll);
    }

    @Override
    public void cameraTurn(final double deltaX, final double deltaY) {
        apiv2.camera.interactive.add_turn(deltaX, deltaY);
    }

    public void cameraTurn(final double deltaX, final long deltaY) {
        apiv2.camera.interactive.add_turn(deltaX, deltaY);
    }

    public void cameraTurn(final long deltaX, final double deltaY) {
        apiv2.camera.interactive.add_turn(deltaX, deltaY);
    }

    public void cameraTurn(final long deltaX, final long deltaY) {
        apiv2.camera.interactive.add_turn(deltaX, deltaY);
    }

    @Override
    public void cameraYaw(final double amount) {
        apiv2.camera.interactive.add_yaw(amount);
    }

    public void cameraYaw(final long amount) {
        apiv2.camera.interactive.add_yaw(amount);
    }

    @Override
    public void cameraPitch(final double amount) {
        apiv2.camera.interactive.add_pitch(amount);
    }

    public void cameraPitch(final long amount) {
        apiv2.camera.interactive.add_pitch(amount);
    }

    @Override
    public void cameraStop() {
        apiv2.camera.stop();

    }

    @Override
    public void cameraCenter() {
        apiv2.camera.center();
    }

    @Override
    public IFocus getClosestObjectToCamera() {
        return apiv2.camera.get_closest_object();
    }

    @Override
    public void setFov(final float newFov) {
        apiv2.camera.set_fov(newFov);
    }

    @Override
    public double getFov() {
        return apiv2.camera.get_fov();
    }

    public void setFov(final int newFov) {
        apiv2.camera.set_fov(newFov);
    }

    @Override
    public void setVisibility(final String key, final boolean visible) {
        apiv2.scene.set_component_type_visibility(key, visible);
    }

    @Override
    public void setComponentTypeVisibility(String key, boolean visible) {
        apiv2.scene.set_component_type_visibility(key, visible);
    }

    @Override
    public boolean getComponentTypeVisibility(String key) {
        return apiv2.scene.get_component_type_visibility(key);
    }

    @Override
    public double[] getObjectScreenCoordinates(String name) {
        return apiv2.scene.get_object_screen_coordinates(name);
    }

    @Override
    public boolean setObjectVisibility(String name, boolean visible) {
        return apiv2.scene.set_object_visibility(name, visible);
    }

    @Override
    public boolean getObjectVisibility(String name) {
        return apiv2.scene.get_object_visibility(name);
    }

    @Override
    public boolean setObjectQuaternionSlerpOrientation(String name, String file) {
        return apiv2.scene.set_object_quaternion_slerp_orientation(name, file);
    }

    @Override
    public boolean setObjectQuaternionNlerpOrientation(String name, String file) {
        return apiv2.scene.set_object_quaternion_nlerp_orientation(name, file);
    }

    @Override
    public void setLabelSizeFactor(float factor) {
        apiv2.scene.set_label_size_factor(factor);
    }

    public void setLabelSizeFactor(int factor) {
        apiv2.scene.set_label_size_factor(factor);
    }

    @Override
    public void setForceDisplayLabel(String name, boolean forceLabel) {
        apiv2.scene.set_force_display_label(name, forceLabel);
    }

    @Override
    public void setLabelIncludeRegexp(String regexp) {
        apiv2.scene.set_label_include_regexp(regexp);
    }

    @Override
    public void setLabelExcludeRegexp(String regexp) {
        apiv2.scene.set_label_exclude_regexp(regexp);
    }

    @Override
    public void setMuteLabel(String name, boolean muteLabel) {
        apiv2.scene.set_mute_label(name, muteLabel);
    }

    @Override
    public void setLabelColor(String name, double[] color) {
        apiv2.scene.set_label_color(name, color);
    }

    public void setLabelColor(String name, final List<?> color) {
        apiv2.scene.set_label_color(name, color);
    }

    @Override
    public boolean getForceDisplayLabel(String name) {
        return apiv2.scene.get_force_dispaly_label(name);
    }

    @Override
    public void setLineWidthFactor(final float factor) {
        apiv2.scene.set_line_width_factor(factor);
    }

    public void setLineWidthFactor(int factor) {
        apiv2.scene.set_line_width_factor(factor);
    }

    @Override
    public void setProperMotionsNumberFactor(float factor) {
        apiv2.scene.set_velocity_vectors_number_factor(factor);
    }

    public void setProperMotionsNumberFactor(int factor) {
        apiv2.scene.set_velocity_vectors_number_factor(factor);
    }

    public void setUnfilteredProperMotionsNumberFactor(float factor) {
        apiv2.scene.set_unfiltered_velocity_vectors_number_factor(factor);
    }

    @Override
    public void setProperMotionsColorMode(int mode) {
        apiv2.scene.set_velocity_vectors_color_mode(mode);
    }

    @Override
    public void setProperMotionsArrowheads(boolean arrowheadsEnabled) {
        apiv2.scene.set_velocity_vectors_arrowheads(arrowheadsEnabled);
    }

    @Override
    public void setProperMotionsLengthFactor(float factor) {
        apiv2.scene.set_velocity_vectors_length_factor(factor);
    }

    public void setProperMotionsLengthFactor(int factor) {
        apiv2.scene.set_velocity_vectors_length_factor(factor);
    }

    @Override
    public long getProperMotionsMaxNumber() {
        return apiv2.scene.get_velocity_vector_max_number();
    }

    @Override
    public void setProperMotionsMaxNumber(long maxNumber) {
        apiv2.scene.set_velocity_vector_max_number(maxNumber);
    }

    @Override
    public void setCrosshairVisibility(boolean visible) {
        apiv2.ui.set_crosshair_visibility(visible);
    }

    @Override
    public void setFocusCrosshairVisibility(boolean visible) {
        apiv2.ui.set_focus_crosshair_visibility(visible);
    }

    @Override
    public void setClosestCrosshairVisibility(boolean visible) {
        apiv2.ui.set_closest_crosshair_visibility(visible);
    }

    @Override
    public void setHomeCrosshairVisibility(boolean visible) {
        apiv2.ui.set_home_crosshair_visibility(visible);
    }

    @Override
    public void setMinimapVisibility(boolean visible) {
        apiv2.ui.set_minimap_visibility(visible);
    }

    @Override
    public void setAmbientLight(final float value) {
        apiv2.graphics.set_ambient_light(value);
    }

    public void setAmbientLight(final int value) {
        apiv2.graphics.set_ambient_light(value);
    }

    @Override
    public void setSimulationTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        apiv2.time.set_clock(year, month, day, hour, min, sec, millisec);
    }

    @Override
    public void setSimulationTime(final long time) {
        apiv2.time.set_clock(time);
    }

    @Override
    public long getSimulationTime() {
        return apiv2.time.get_clock();
    }

    @Override
    public int[] getSimulationTimeArr() {
        return apiv2.time.get_clock_array();
    }

    @Override
    public void startSimulationTime() {
        apiv2.time.start_clock();
    }

    @Override
    public void stopSimulationTime() {
        apiv2.time.stop_clock();
    }

    @Override
    public boolean isSimulationTimeOn() {
        return apiv2.time.is_clock_on();
    }

    @Override
    public void setSimulationPace(final double warp) {
        apiv2.time.set_time_warp(warp);
    }

    public void setSimulationPace(final long warp) {
        apiv2.time.set_time_warp(warp);
    }

    @Override
    public void setTimeWarp(final double warp) {
        apiv2.time.set_time_warp(warp);
    }

    public void setTimeWarp(final long warp) {
        apiv2.time.set_time_warp(warp);
    }

    @Override
    public void setTargetTime(long ms) {
        apiv2.time.set_target_time(ms);
    }

    @Override
    public void setTargetTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        apiv2.time.set_target_time(year, month, day, hour, min, sec, millisec);
    }

    @Override
    public void unsetTargetTime() {
        apiv2.time.remove_target_time();
    }

    @Override
    public void setStarBrightnessPower(float power) {
        apiv2.graphics.set_star_brightness_power(power);
    }

    @Override
    public void setStarGlowFactor(float glowFactor) {
        apiv2.graphics.set_star_glow_factor(glowFactor);
    }

    @Override
    public float getStarBrightness() {
        return apiv2.graphics.get_star_brightness();
    }

    @Override
    public void setStarBrightness(final float brightness) {
        apiv2.graphics.set_star_brightness(brightness);
    }

    public void setStarBrightness(final int brightness) {
        apiv2.graphics.set_star_brightness(brightness);
    }

    @Override
    public float getPointSize() {
        return apiv2.graphics.get_point_size();
    }

    @Override
    public float getStarSize() {
        return apiv2.graphics.get_point_size();
    }

    @Override
    public void setPointSize(final float size) {
        apiv2.graphics.set_point_size(size);
    }

    @Override
    public void setStarSize(final float size) {
        apiv2.graphics.set_point_size(size);
    }

    public void setStarSize(final int size) {
        apiv2.graphics.set_point_size(size);
    }

    @Override
    public float getStarBaseOpacity() {
        return apiv2.graphics.get_star_base_opacity();
    }

    @Override
    public float getStarMinOpacity() {
        return apiv2.graphics.get_star_base_opacity();
    }

    @Override
    public void setStarBaseOpacity(float opacity) {
        apiv2.graphics.set_star_base_opacity(opacity);
    }

    @Override
    public void setStarMinOpacity(float minOpacity) {
        apiv2.graphics.set_star_base_opacity(minOpacity);
    }

    public float getMinStarOpacity() {
        return apiv2.graphics.get_star_base_opacity();
    }

    public void setMinStarOpacity(float minOpacity) {
        apiv2.graphics.set_star_base_opacity(minOpacity);
    }

    public void setMinStarOpacity(int opacity) {
        apiv2.graphics.set_star_base_opacity(opacity);
    }

    @Override
    public void setStarTextureIndex(int index) {
        apiv2.graphics.set_star_texture_index(index);
    }

    @Override
    public void setStarGroupNearestNumber(int n) {
        apiv2.graphics.set_star_set_metadata_size(n);
    }

    @Override
    public void setStarGroupBillboard(boolean flag) {
        apiv2.graphics.set_star_set_billboard(flag);
    }

    @Override
    public void setOrbitSolidAngleThreshold(float angleDeg) {
        apiv2.graphics.set_orbit_solid_angle_threshold(angleDeg);
    }

    @Override
    public void setProjectionYaw(float yaw) {
        apiv2.instances.set_projection_yaw(yaw);
    }

    @Override
    public void setProjectionPitch(float pitch) {
        apiv2.instances.set_projection_pitch(pitch);
    }

    @Override
    public void setProjectionRoll(float roll) {
        apiv2.instances.set_projection_roll(roll);
    }

    @Override
    public void setProjectionFov(float fov) {
        apiv2.instances.set_projection_fov(fov);
    }

    @Override
    public void setLimitFps(double limitFps) {
        apiv2.graphics.set_limit_fps(limitFps);
    }

    @Override
    public void setLimitFps(int limitFps) {
        apiv2.graphics.set_limit_fps(limitFps);
    }

    @Override
    public void configureScreenshots(int width, int height, String directory, String namePrefix) {
        apiv2.output.configure_screenshots(width, height, directory, namePrefix);
    }

    @Override
    public void setScreenshotsMode(String mode) {
        apiv2.output.screenshot_mode(mode);
    }

    @Override
    public void saveScreenshot() {
        apiv2.output.screenshot();
    }

    @Override
    public void takeScreenshot() {
        apiv2.output.screenshot();
    }

    @Override
    public void configureFrameOutput(int width, int height, int fps, String directory, String namePrefix) {
        apiv2.output.configure_frame_output(width, height, fps, directory, namePrefix);
    }

    @Override
    public void configureFrameOutput(int width, int height, double fps, String directory, String namePrefix) {
        apiv2.output.configure_frame_output(width, height, fps, directory, namePrefix);
    }

    @Override
    public void configureRenderOutput(int width, int height, int fps, String directory, String namePrefix) {
        apiv2.output.configure_frame_output(width, height, fps, directory, namePrefix);
    }

    @Override
    public void setFrameOutputMode(String mode) {
        apiv2.output.frame_output_mode(mode);
    }

    @Override
    public boolean isFrameOutputActive() {
        return apiv2.output.is_frame_output_active();
    }

    @Override
    public boolean isRenderOutputActive() {
        return apiv2.output.is_frame_output_active();
    }

    @Override
    public double getFrameOutputFps() {
        return apiv2.output.get_frame_output_fps();
    }

    @Override
    public double getRenderOutputFps() {
        return apiv2.output.get_frame_output_fps();
    }

    @Override
    public void setFrameOutput(boolean active) {
        apiv2.output.frame_output(active);
    }

    @Override
    public FocusView getObject(String name) {
        return apiv2.scene.get_object(name);
    }

    @Override
    public FocusView getObject(String name, double timeOutSeconds) {
        return apiv2.scene.get_object(name, timeOutSeconds);
    }

    @Override
    public VertsView getLineObject(String name) {
        return apiv2.scene.get_line_object(name);
    }

    @Override
    public VertsView getLineObject(String name, double timeOutSeconds) {
        return apiv2.scene.get_line_object(name, timeOutSeconds);
    }

    public Entity getEntity(String name) {
        return apiv2.scene.get_entity(name);
    }

    public Entity getEntity(String name, double timeOutSeconds) {
        return apiv2.scene.get_entity(name, timeOutSeconds);
    }

    private Entity getFocus(String name) {
        return apiv2.scene.get_focus(name);
    }

    private Entity getFocusEntity(String name) {
        return apiv2.scene.get_focus_entity(name);
    }

    @Override
    public void setObjectSizeScaling(String name, double scalingFactor) {
        apiv2.scene.set_object_size_scaling(name, scalingFactor);
    }

    public void setObjectSizeScaling(Entity object, double scalingFactor) {
        apiv2.scene.set_object_size_scaling(object, scalingFactor);
    }

    @Override
    public void setOrbitCoordinatesScaling(String name, double scalingFactor) {
        apiv2.scene.set_orbit_coordinates_scaling(name, scalingFactor);
    }

    @Override
    public void refreshAllOrbits() {
        apiv2.scene.refresh_all_orbits();
    }

    @Override
    public void forceUpdateScene() {
        apiv2.scene.force_update_scene();
    }

    public void refreshObjectOrbit(String name) {
        apiv2.scene.refresh_object_orbit(name);
    }

    @Override
    public double getObjectRadius(String name) {
        return apiv2.scene.get_object_radius(name);
    }

    @Override
    public void goToObject(String name) {
        apiv2.camera.interactive.go_to_object(name);
    }

    @Override
    public void goToObject(String name, double solidAngle) {
        apiv2.camera.interactive.go_to_object(name, solidAngle);
    }

    @Override
    public void goToObject(String name, double solidAngle, float waitTimeSeconds) {
        apiv2.camera.interactive.go_to_object(name, solidAngle, waitTimeSeconds);
    }

    /**
     * Same as {@link IScriptingInterface#goToObject(String, double, float)}, but using an integer for <code>waitTimeSeconds</code>.
     */
    public void goToObject(String name, double solidAngle, int waitTimeSeconds) {
        apiv2.camera.interactive.go_to_object(name, solidAngle, waitTimeSeconds);
    }

    /**
     * Same as {@link IScriptingInterface#goToObject(String, double, float)}, but using an integer for <code>waitTimeSeconds</code>, and
     * a long for <code>solidAngle</code>.
     */
    public void goToObject(String name, long solidAngle, int waitTimeSeconds) {
        apiv2.camera.interactive.go_to_object(name, solidAngle, waitTimeSeconds);
    }

    /**
     * Same as {@link IScriptingInterface#goToObject(String, double, float)}, but using a long for <code>solidAngle</code>.
     */
    public void goToObject(String name, long solidAngle, float waitTimeSeconds) {
        apiv2.camera.interactive.go_to_object(name, solidAngle, waitTimeSeconds);
    }

    /**
     * Version of {@link EventScriptingInterface#goToObject(String, double, int)} that gets an optional {@link AtomicBoolean} that
     * enables stopping the execution of the call when its value changes.
     */
    public void goToObject(String name, double solidAngle, int waitTimeSeconds, AtomicBoolean stop) {
        apiv2.camera.interactive.go_to_object(name, solidAngle, waitTimeSeconds);
    }

    /**
     * Version of {@link EventScriptingInterface#goToObject(String, double, int, AtomicBoolean)} that gets an {@link Entity} reference
     * instead of its name.
     */
    public void goToObject(Entity object, double solidAngle, int waitTimeSeconds, AtomicBoolean stop) {
        apiv2.camera.interactive.go_to_object(object, solidAngle, waitTimeSeconds, stop);
    }

    @Override
    public void goToObjectInstant(String name) {
        apiv2.camera.go_to_object_instant(name);
    }

    @Override
    public void goToObjectSmooth(String name, double positionDurationSeconds, double orientationDurationSeconds) {
        apiv2.camera.go_to_object(name, positionDurationSeconds, orientationDurationSeconds);
    }

    @Override
    public void goToObjectSmooth(String name, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds) {
        apiv2.camera.go_to_object(name, solidAngle, positionDurationSeconds, orientationDurationSeconds);
    }

    @Override
    public void goToObjectSmooth(String name, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        apiv2.camera.go_to_object(name, positionDurationSeconds, orientationDurationSeconds, sync);
    }

    @Override
    public void goToObjectSmooth(String name, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        apiv2.camera.go_to_object(name, solidAngle, positionDurationSeconds, orientationDurationSeconds, sync);
    }

    public void goToObjectSmooth(Entity object, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        apiv2.camera.go_to_object(object, positionDurationSeconds, orientationDurationSeconds, sync);
    }

    public void goToObjectSmooth(Entity object, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        apiv2.camera.go_to_object(object, solidAngle, positionDurationSeconds, orientationDurationSeconds, sync);
    }

    public void goToObjectSmooth(Entity object,
                                 double solidAngle,
                                 double positionDurationSeconds,
                                 double orientationDurationSeconds,
                                 boolean sync,
                                 AtomicBoolean stop) {
        apiv2.camera.go_to_object(object, solidAngle, positionDurationSeconds, orientationDurationSeconds, sync, stop);
    }

    @Override
    public void landOnObject(String name) {
        apiv2.camera.interactive.land_on(name);
    }

    void landOnObject(Entity object, AtomicBoolean stop) {
        apiv2.camera.interactive.land_on(object, stop);
    }

    @Override
    public void landAtObjectLocation(String name, String locationName) {
        apiv2.camera.interactive.land_at_location(name, locationName);
    }

    public void landAtObjectLocation(String name, String locationName, AtomicBoolean stop) {
        apiv2.camera.interactive.land_at_location(name, locationName, stop);
    }

    public void landAtObjectLocation(Entity object, String locationName, AtomicBoolean stop) {
        apiv2.camera.interactive.land_at_location(object, locationName, stop);
    }

    @Override
    public void landAtObjectLocation(String name, double longitude, double latitude) {
        apiv2.camera.interactive.land_at_location(name, longitude, latitude);
    }

    public void landAtObjectLocation(Entity object, double longitude, double latitude, AtomicBoolean stop) {
        apiv2.camera.interactive.land_at_location(object, longitude, latitude, stop);
    }

    @Override
    public double getDistanceTo(String name) {
        return apiv2.camera.get_distance_to_object(name);
    }

    @Override
    public double[] getStarParameters(String id) {
        return apiv2.scene.get_star_parameters(id);
    }

    @Override
    public double[] getObjectPosition(String name) {
        return apiv2.scene.get_object_position(name);
    }

    @Override
    public double[] getObjectPosition(String name, String units) {
        return apiv2.scene.get_object_position(name, units);
    }

    @Override
    public double[] getObjectPredictedPosition(String name) {
        return apiv2.scene.get_object_predicted_position(name);
    }

    @Override
    public double[] getObjectPredictedPosition(String name, String units) {
        return apiv2.scene.get_object_predicted_position(name, units);
    }

    @Override
    public void setObjectPosition(String name, double[] position) {
        apiv2.scene.set_object_posiiton(name, position);
    }

    @Override
    public void setObjectPosition(String name, double[] position, String units) {
        apiv2.scene.set_object_posiiton(name, position, units);
    }

    public void setObjectPosition(String name, List<?> position) {
        apiv2.scene.set_object_posiiton(name, position);
    }

    public void setObjectPosition(String name, List<?> position, String units) {
        apiv2.scene.set_object_posiiton(name, position, units);
    }

    @Override
    public void setObjectPosition(FocusView object, double[] position) {
        apiv2.scene.set_object_posiiton(object, position);
    }

    @Override
    public void setObjectPosition(FocusView object, double[] position, String units) {
        apiv2.scene.set_object_posiiton(object, position, units);
    }

    public void setObjectPosition(FocusView object, List<?> position) {
        apiv2.scene.set_object_posiiton(object, position);
    }

    public void setObjectPosition(FocusView object, List<?> position, String units) {
        apiv2.scene.set_object_posiiton(object, position, units);
    }

    @Override
    public void setObjectPosition(Entity object, double[] position) {
        apiv2.scene.set_object_posiiton(object, position);
    }

    @Override
    public void setObjectPosition(Entity object, double[] position, String units) {
        apiv2.scene.set_object_posiiton(object, position, units);
    }

    @Override
    public void setObjectCoordinatesProvider(String name, IPythonCoordinatesProvider provider) {
        apiv2.scene.set_object_coordinates_provider(name, provider);
    }

    @Override
    public void removeObjectCoordinatesProvider(String name) {
        apiv2.scene.remove_object_coordinates_provider(name);
    }

    public void setObjectPosition(Entity object, List<?> position) {
        apiv2.scene.set_object_posiiton(object, position);
    }

    @Override
    public void setGuiScrollPosition(final float pixelY) {
        // This is removed from APIv2
        postRunnable(() -> em.post(Event.GUI_SCROLL_POSITION_CMD, this, pixelY));

    }

    public void setGuiScrollPosition(final int pixelY) {
        // This is removed from APIv2
        setGuiScrollPosition((float) pixelY);
    }

    @Override
    public void enableGui() {
        apiv2.ui.enable();
    }

    @Override
    public void disableGui() {
        apiv2.ui.disable();
    }

    @Override
    public float getGuiScaleFactor() {
        return Settings.settings.program.ui.scale;
    }

    @Override
    public void displayMessageObject(final int id,
                                     final String message,
                                     final float x,
                                     final float y,
                                     final float r,
                                     final float g,
                                     final float b,
                                     final float a,
                                     final float fontSize) {
        apiv2.ui.display_message(id, message, x, y, r, g, b, a, fontSize);
    }

    @Override
    public void displayMessageObject(final int id,
                                     final String message,
                                     final double x,
                                     final double y,
                                     final double[] color,
                                     final double fontSize) {
        apiv2.ui.display_message(id, message, x, y, color, fontSize);
    }

    public void displayMessageObject(final int id, final String message, final double x, final double y, final List<?> color, final double fontSize) {
        apiv2.ui.display_message(id, message, x, y, color, fontSize);
    }

    public void displayMessageObject(final int id,
                                     final String message,
                                     final float x,
                                     final float y,
                                     final float r,
                                     final float g,
                                     final float b,
                                     final float a,
                                     final int fontSize) {
        apiv2.ui.display_message(id, message, x, y, r, g, b, a, fontSize);
    }

    @Override
    public void displayTextObject(final int id,
                                  final String text,
                                  final float x,
                                  final float y,
                                  final float maxWidth,
                                  final float maxHeight,
                                  final float r,
                                  final float g,
                                  final float b,
                                  final float a,
                                  final float fontSize) {
        apiv2.ui.display_text(id, text, x, y, maxWidth, maxHeight, r, g, b, a, fontSize);
    }

    public void displayTextObject(final int id,
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
        apiv2.ui.display_text(id, text, x, y, maxWidth, maxHeight, r, g, b, a, fontSize);
    }

    @Override
    public void displayImageObject(final int id,
                                   final String path,
                                   final float x,
                                   final float y,
                                   final float r,
                                   final float g,
                                   final float b,
                                   final float a) {
        apiv2.ui.display_image(id, path, x, y, r, g, b, a);
    }

    @Override
    public void displayImageObject(final int id, final String path, final double x, final double y, final double[] color) {
        apiv2.ui.display_image(id, path, x, y, color);
    }

    public void displayImageObject(final int id, final String path, final double x, final double y, final List<?> color) {
        apiv2.ui.display_image(id, path, x, y, color);
    }

    @Override
    public void displayImageObject(final int id, final String path, final float x, final float y) {
        apiv2.ui.display_image(id, path, x, y);
    }

    @Override
    public void removeAllObjects() {
        apiv2.ui.remove_all_objects();
    }

    @Override
    public void removeObject(final int id) {
        apiv2.ui.remove_object(id);
    }

    @Override
    public void removeObjects(final int[] ids) {
        apiv2.ui.remove_objects(ids);
    }

    public void removeObjects(final List<?> ids) {
        apiv2.ui.remove_objects(ids);
    }

    @Override
    public void maximizeInterfaceWindow() {
        // Removed from APIv2
        postRunnable(() -> em.post(Event.GUI_FOLD_CMD, this, false));
    }

    @Override
    public void minimizeInterfaceWindow() {
        // Removed from APIv2
        postRunnable(() -> em.post(Event.GUI_FOLD_CMD, this, true));
    }

    @Override
    public void expandUIPane(String paneName) {
        apiv2.ui.expand_pane(paneName);
    }

    @Override
    public void collapseUIPane(String paneName) {
        apiv2.ui.collapse_pane(paneName);
    }

    @Override
    public void expandGuiComponent(String paneName) {
        apiv2.ui.expand_pane(paneName);
    }

    @Override
    public void collapseGuiComponent(String paneName) {
        apiv2.ui.collapse_pane(paneName);
    }

    @Override
    public void setGuiPosition(final float x, final float y) {
        // Removed in APIv2
        postRunnable(() -> em.post(Event.GUI_MOVE_CMD, this, x, y));
    }

    public void setGuiPosition(final int x, final int y) {
        // Removed in APIv2
        setGuiPosition((float) x, (float) y);
    }

    public void setGuiPosition(final float x, final int y) {
        // Removed in APIv2
        setGuiPosition(x, (float) y);
    }

    public void setGuiPosition(final int x, final float y) {
        // Removed in APIv2
        setGuiPosition((float) x, y);
    }

    @Override
    public void waitForInput() {
        apiv2.input.wait_input();
    }

    @Override
    public void waitForEnter() {
        apiv2.input.wait_enter();
    }

    @Override
    public void waitForInput(int keyCode) {
        apiv2.input.wait_input(keyCode);
    }

    @Override
    public int getScreenWidth() {
        return apiv2.ui.get_client_width();
    }

    @Override
    public int getScreenHeight() {
        return apiv2.ui.get_client_height();
    }

    @Override
    public float[] getPositionAndSizeGui(String name) {
        return apiv2.ui.get_position_and_size(name);
    }

    @Override
    public String getVersion() {
        return apiv2.base.get_version();
    }

    @Override
    public String getVersionNumber() {
        return Settings.settings.version.version;
    }

    @Override
    public String getBuildString() {
        return apiv2.base.get_build_string();
    }

    @Override
    public boolean waitFocus(String name, long timeoutMs) {
        return apiv2.camera.wait_focus(name, timeoutMs);
    }

    @Override
    public void setCamcorderFps(double targetFps) {
        apiv2.camcorder.set_fps(targetFps);
    }

    @Override
    public void setCameraRecorderFps(double targetFps) {
        apiv2.camcorder.set_fps(targetFps);
    }

    @Override
    public double getCamcorderFps() {
        return apiv2.camcorder.get_fps();
    }

    @Override
    public String getAssetsLocation() {
        return apiv2.base.get_assets_dir();
    }

    @Override
    public void preloadTexture(String path) {
        apiv2.ui.preload_texture(path);
    }

    @Override
    public void preloadTextures(String[] paths) {
        apiv2.ui.preload_textures(paths);
    }

    @Override
    public void startRecordingCameraPath() {
        apiv2.camcorder.start();
    }

    @Override
    public void startRecordingCameraPath(String fileName) {
        apiv2.camcorder.start();
    }

    @Override
    public void stopRecordingCameraPath() {
        apiv2.camcorder.stop();
    }

    @Override
    public void playCameraPath(String file, boolean sync) {
        apiv2.camcorder.play(file, sync);
    }

    @Override
    public void runCameraPath(String file, boolean sync) {
        apiv2.camcorder.play(file, sync);
    }

    @Override
    public void playCameraPath(String file) {
        apiv2.camcorder.play(file);
    }

    @Override
    public void runCameraPath(String file) {
        apiv2.camcorder.play(file);
    }

    @Override
    public void runCameraRecording(String file) {
        apiv2.camcorder.play(file);
    }

    @Override
    public void cameraTransitionKm(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        apiv2.camera.transition_km(camPos, camDir, camUp, seconds);
    }

    public void cameraTransitionKm(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        apiv2.camera.transition_km(camPos, camDir, camUp, seconds);
    }

    public void cameraTransitionKm(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        apiv2.camera.transition_km(camPos, camDir, camUp, seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, String units, double[] camDir, double[] camUp, double seconds) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds);
    }

    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, long seconds) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds);
    }

    public void cameraTransition(double[] camPos, String units, double[] camDir, double[] camUp, long seconds) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, double seconds) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, long seconds) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds, boolean sync) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds, sync);
    }

    @Override
    public void cameraTransition(double[] camPos, String units, double[] camDir, double[] camUp, double seconds, boolean sync) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds, sync);
    }

    public void cameraTransition(List<?> camPos,
                                 List<?> camDir,
                                 List<?> camUp,
                                 double seconds,
                                 String positionSmoothType,
                                 double positionSmoothFactor,
                                 String orientationSmoothType,
                                 double orientationSmoothFactor) {
        apiv2.camera.transition(camPos,
                                camDir,
                                camUp,
                                seconds,
                                positionSmoothType,
                                positionSmoothFactor,
                                orientationSmoothType,
                                orientationSmoothFactor);
    }

    @Override
    public void cameraTransition(double[] camPos,
                                 double[] camDir,
                                 double[] camUp,
                                 double positionDurationSeconds,
                                 String positionSmoothType,
                                 double positionSmoothFactor,
                                 double orientationDurationSeconds,
                                 String orientationSmoothType,
                                 double orientationSmoothFactor) {
        apiv2.camera.transition(camPos,
                                camDir,
                                camUp,
                                positionDurationSeconds,
                                positionSmoothType,
                                positionSmoothFactor,
                                orientationDurationSeconds,
                                orientationSmoothType,
                                orientationSmoothFactor);
    }

    public void cameraTransition(List<?> camPos,
                                 String units,
                                 List<?> camDir,
                                 List<?> camUp,
                                 double positionDurationSeconds,
                                 String positionSmoothType,
                                 double positionSmoothFactor,
                                 double orientationDurationSeconds,
                                 String orientationSmoothType,
                                 double orientationSmoothFactor,
                                 boolean sync) {
        apiv2.camera.transition(camPos,
                                units,
                                camDir,
                                camUp,
                                positionDurationSeconds,
                                positionSmoothType,
                                positionSmoothFactor,
                                orientationDurationSeconds,
                                orientationSmoothType,
                                orientationSmoothFactor,
                                sync);
    }

    @Override
    public void cameraTransition(double[] camPos,
                                 String units,
                                 double[] camDir,
                                 double[] camUp,
                                 double positionDurationSeconds,
                                 String positionSmoothType,
                                 double positionSmoothFactor,
                                 double orientationDurationSeconds,
                                 String orientationSmoothType,
                                 double orientationSmoothFactor,
                                 boolean sync) {
        apiv2.camera.transition(camPos,
                                units,
                                camDir,
                                camUp,
                                positionDurationSeconds,
                                positionSmoothType,
                                positionSmoothFactor,
                                orientationDurationSeconds,
                                orientationSmoothType,
                                orientationSmoothFactor,
                                sync);
    }

    public void cameraTransition(double[] camPos,
                                 String units,
                                 double[] camDir,
                                 double[] camUp,
                                 double positionDurationSeconds,
                                 String positionSmoothType,
                                 double positionSmoothFactor,
                                 double orientationDurationSeconds,
                                 String orientationSmoothType,
                                 double orientationSmoothFactor,
                                 boolean sync,
                                 AtomicBoolean stop) {
        apiv2.camera.transition(camPos,
                                units,
                                camDir,
                                camUp,
                                positionDurationSeconds,
                                positionSmoothType,
                                positionSmoothFactor,
                                orientationDurationSeconds,
                                orientationSmoothType,
                                orientationSmoothFactor,
                                sync,
                                stop);
    }

    @Override
    public void cameraPositionTransition(double[] camPos,
                                         String units,
                                         double durationSeconds,
                                         String smoothType,
                                         double smoothFactor,
                                         boolean sync) {
        apiv2.camera.transition_position(camPos, units, durationSeconds, smoothType, smoothFactor, sync);
    }

    public void cameraPositionTransition(double[] camPos,
                                         String units,
                                         double durationSeconds,
                                         String smoothType,
                                         double smoothFactor,
                                         boolean sync,
                                         AtomicBoolean stop) {
        apiv2.camera.transition_position(camPos, units, durationSeconds, smoothType, smoothFactor, sync, stop);
    }

    @Override
    public void cameraOrientationTransition(double[] camDir,
                                            double[] camUp,
                                            double durationSeconds,
                                            String smoothType,
                                            double smoothFactor,
                                            boolean sync) {
        apiv2.camera.transition_orientation(camDir, camUp, durationSeconds, smoothType, smoothFactor, sync);
    }

    public void cameraOrientationTransition(double[] camDir,
                                            double[] camUp,
                                            double durationSeconds,
                                            String smoothType,
                                            double smoothFactor,
                                            boolean sync,
                                            AtomicBoolean stop) {
        apiv2.camera.transition_orientation(camDir, camUp, durationSeconds, smoothType, smoothFactor, sync, stop);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds, sync);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds, sync);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        apiv2.camera.transition(camPos, camDir, camUp, seconds, sync);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        apiv2.camera.transition(camPos, units, camDir, camUp, seconds, sync);
    }

    public void cameraOrientationTransition(List<?> camDir,
                                            List<?> camUp,
                                            double durationSeconds,
                                            String smoothType,
                                            double smoothFactor,
                                            boolean sync) {
        apiv2.camera.transition_orientation(camDir, camUp, durationSeconds, smoothType, smoothFactor, sync);
    }

    public void cameraPositionTransition(List<?> camPos, String units, double durationSeconds, String smoothType, double smoothFactor, boolean sync) {
        apiv2.camera.transition_position(camPos, units, durationSeconds, smoothType, smoothFactor, sync);
    }

    @Override
    public void timeTransition(int year,
                               int month,
                               int day,
                               int hour,
                               int min,
                               int sec,
                               int milliseconds,
                               double durationSeconds,
                               String smoothType,
                               double smoothFactor,
                               boolean sync) {
        apiv2.time.transition(year, month, day, hour, min, sec, milliseconds, durationSeconds, smoothType, smoothFactor, sync);
    }

    public void timeTransition(int year,
                               int month,
                               int day,
                               int hour,
                               int min,
                               int sec,
                               int milliseconds,
                               double durationSeconds,
                               String smoothType,
                               double smoothFactor,
                               boolean sync,
                               AtomicBoolean stop) {
        apiv2.time.transition(year, month, day, hour, min, sec, milliseconds, durationSeconds, smoothType, smoothFactor, sync, stop);
    }

    @Override
    public void sleep(float seconds) {
        apiv2.base.sleep(seconds);
    }

    public void sleep(int seconds) {
        apiv2.base.sleep(seconds);
    }

    @Override
    public void sleepFrames(long frames) {
        apiv2.base.sleep_frames(frames);
    }

    @Override
    public double[] galacticToInternalCartesian(double l, double b, double r) {
        return apiv2.refsys.galactic_to_cartesian(l, b, r);
    }

    public double[] galacticToInternalCartesian(int l, int b, int r) {
        return apiv2.refsys.galactic_to_cartesian(l, b, r);
    }

    @Override
    public double[] eclipticToInternalCartesian(double l, double b, double r) {
        return apiv2.refsys.ecliptic_to_cartesian(l, b, r);
    }

    public double[] eclipticToInternalCartesian(int l, int b, int r) {
        return apiv2.refsys.ecliptic_to_cartesian(l, b, r);
    }

    @Override
    public double[] equatorialToInternalCartesian(double ra, double dec, double r) {
        return apiv2.refsys.equatorial_to_cartesian(ra, dec, r);
    }

    public double[] equatorialToInternalCartesian(int ra, int dec, int r) {
        return apiv2.refsys.equatorial_to_cartesian(ra, dec, r);
    }

    public double[] internalCartesianToEquatorial(double x, double y, double z) {
        return apiv2.refsys.cartesian_to_equatorial(x, y, z);
    }

    public double[] internalCartesianToEquatorial(int x, int y, int z) {
        return apiv2.refsys.cartesian_to_equatorial(x, y, z);
    }

    @Override
    public double[] equatorialCartesianToInternalCartesian(double[] eq, double kmFactor) {
        return apiv2.refsys.equatorial_cartesian_to_internal(eq, kmFactor);
    }

    public double[] equatorialCartesianToInternalCartesian(final List<?> eq, double kmFactor) {
        return apiv2.refsys.equatorial_cartesian_to_internal(eq, kmFactor);
    }

    @Override
    public double[] equatorialToGalactic(double[] eq) {
        return apiv2.refsys.equatorial_to_galactic(eq);
    }

    public double[] equatorialToGalactic(List<?> eq) {
        return apiv2.refsys.equatorial_to_galactic(eq);
    }

    @Override
    public double[] equatorialToEcliptic(double[] eq) {
        return apiv2.refsys.equatorial_to_ecliptic(eq);
    }

    public double[] equatorialToEcliptic(List<?> eq) {
        return apiv2.refsys.equatorial_to_ecliptic(eq);
    }

    @Override
    public double[] galacticToEquatorial(double[] gal) {
        return apiv2.refsys.galactic_to_equatorial(gal);
    }

    public double[] galacticToEquatorial(List<?> gal) {
        return apiv2.refsys.galactic_to_equatorial(gal);
    }

    @Override
    public double[] eclipticToEquatorial(double[] ecl) {
        return apiv2.refsys.ecliptic_to_equatorial(ecl);
    }

    public double[] eclipticToEquatorial(List<?> ecl) {
        return apiv2.refsys.ecliptic_to_equatorial(ecl);
    }

    @Override
    public void setBrightnessLevel(double level) {
        apiv2.graphics.set_image_brightness(level);
    }

    public void setBrightnessLevel(long level) {
        apiv2.graphics.set_image_brightness(level);
    }

    @Override
    public void setContrastLevel(double level) {
        apiv2.graphics.set_image_contrast(level);
    }

    public void setContrastLevel(long level) {
        apiv2.graphics.set_image_contrast(level);
    }

    @Override
    public void setHueLevel(double level) {
        apiv2.graphics.set_image_hue(level);
    }

    public void setHueLevel(long level) {
        apiv2.graphics.set_image_hue(level);
    }

    @Override
    public void setSaturationLevel(double level) {
        apiv2.graphics.set_image_saturation(level);
    }

    public void setSaturationLevel(long level) {
        apiv2.graphics.set_image_saturation(level);
    }

    @Override
    public void setGammaCorrectionLevel(double level) {
        apiv2.graphics.set_gamma_correction(level);
    }

    public void setGammaCorrectionLevel(long level) {
        apiv2.graphics.set_gamma_correction(level);
    }

    @Override
    public void setHDRToneMappingType(String type) {
        apiv2.graphics.set_hdr_tone_mapping(type);
    }

    @Override
    public void setExposureToneMappingLevel(double level) {
        apiv2.graphics.set_exposure_tone_mapping(level);
    }

    public void setExposureToneMappingLevel(long level) {
        apiv2.graphics.set_exposure_tone_mapping(level);
    }

    @Override
    public void setCubemapMode(boolean state, String projection) {
        apiv2.graphics.mode_cubemap(state, projection);
    }

    @Override
    public void setPanoramaMode(boolean state) {
        apiv2.graphics.mode_panorama(state);
    }

    @Override
    public void setReprojectionMode(String mode) {
        apiv2.graphics.mode_reprojection(mode);
    }

    @Override
    public void setBackBufferScale(float scale) {
        apiv2.graphics.set_back_buffer_scale(scale);
    }

    @Override
    public void setIndexOfRefraction(float ior) {
        apiv2.graphics.set_index_of_refraction(ior);
    }

    @Override
    public void setPlanetariumMode(boolean state) {
        apiv2.graphics.mode_planetarium(state);
    }

    @Override
    public void setCubemapResolution(int resolution) {
        apiv2.graphics.set_cubemap_resolution(resolution);
    }

    @Override
    public void setCubemapProjection(String projection) {
        apiv2.graphics.set_cubemap_projection(projection);
    }

    @Override
    public void setOrthosphereViewMode(boolean state) {
        apiv2.graphics.mode_orthosphere(state);
    }

    @Override
    public void setStereoscopicMode(boolean state) {
        apiv2.graphics.mode_stereoscopic(state);
    }

    @Override
    public void setStereoscopicProfile(int index) {
        apiv2.graphics.set_stereo_profile(index);
    }

    @Override
    public long getCurrentFrameNumber() {
        return apiv2.graphics.get_current_frame_number();
    }

    @Override
    public void setLensFlare(boolean state) {
        apiv2.graphics.effect_lens_flare(state);
    }

    @Override
    public void setLensFlare(double strength) {
        apiv2.graphics.effect_lens_flare(strength);
    }

    @Override
    public void setMotionBlur(boolean active) {
        apiv2.graphics.effect_motion_blur(active);
    }

    @Override
    public void setMotionBlur(double value) {
        apiv2.graphics.effect_motion_blur(value);
    }

    @Override
    public void setStarGlow(boolean state) {
        apiv2.graphics.effect_star_glow(state);
    }

    @Override
    public void setStarGlowOverObjects(boolean state) {
        apiv2.graphics.effect_star_glow(state);
    }

    @Override
    public void setBloom(float value) {
        apiv2.graphics.effect_bloom(value);
    }

    @Override
    public void setChromaticAberration(float value) {
        apiv2.graphics.effect_chromatic_aberration(value);
    }

    public void setBloom(int level) {
        apiv2.graphics.effect_bloom(level);
    }

    @Override
    public void setSmoothLodTransitions(boolean value) {
        apiv2.graphics.set_smooth_lod_transitions(value);
    }

    @Override
    public double[] rotate3(double[] vector, double[] axis, double angle) {
        return apiv2.geom.rotate3(vector, axis, angle);
    }

    public double[] rotate3(double[] vector, double[] axis, long angle) {
        return apiv2.geom.rotate3(vector, axis, angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, double angle) {
        return apiv2.geom.rotate3(vector, axis, angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, long angle) {
        return apiv2.geom.rotate3(vector, axis, angle);
    }

    @Override
    public double[] rotate2(double[] vector, double angle) {
        return apiv2.geom.rotate2(vector, angle);
    }

    public double[] rotate2(double[] vector, long angle) {
        return apiv2.geom.rotate2(vector, angle);
    }

    public double[] rotate2(List<?> vector, double angle) {
        return apiv2.geom.rotate2(vector, angle);
    }

    public double[] rotate2(List<?> vector, long angle) {
        return apiv2.geom.rotate2(vector, angle);
    }

    @Override
    public double[] cross3(double[] vec1, double[] vec2) {
        return apiv2.geom.cross3(vec1, vec2);
    }

    public double[] cross3(List<?> vec1, List<?> vec2) {
        return apiv2.geom.cross3(vec1, vec2);
    }

    @Override
    public double dot3(double[] vec1, double[] vec2) {
        return apiv2.geom.dot3(vec1, vec2);
    }

    public double dot3(List<?> vec1, List<?> vec2) {
        return apiv2.geom.dot3(vec1, vec2);
    }

    @Override
    public void addTrajectoryLine(String name, double[] points, double[] color) {
        apiv2.scene.add_trajectory_line(name, points, color);
    }

    public void addTrajectoryLine(String name, List<?> points, List<?> color) {
        apiv2.scene.add_trajectory_line(name, points, color);
    }

    @Override
    public void addTrajectoryLine(String name, double[] points, double[] color, double trailMap) {
        apiv2.scene.add_trajectory_line(name, points, color, trailMap);
    }

    public void addTrajectoryLine(String name, List<?> points, List<?> color, double trailMap) {
        apiv2.scene.add_trajectory_line(name, points, color, trailMap);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color) {
        apiv2.scene.add_polyline(name, points, color);
    }

    public void addPolyline(String name, List<?> points, List<?> color) {
        apiv2.scene.add_polyline(name, points, color);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth) {
        apiv2.scene.add_polyline(name, points, color, lineWidth);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, boolean arrowCaps) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, arrowCaps);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive, boolean arrowCaps) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive, arrowCaps);
    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth) {
        apiv2.scene.add_polyline(name, points, color, lineWidth);
    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth, int primitive) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth) {
        apiv2.scene.add_polyline(name, points, color, lineWidth);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, boolean arrowCaps) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, int primitive) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, int primitive, boolean arrowCaps) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth) {
        apiv2.scene.add_polyline(name, points, color, lineWidth);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, boolean arrowCaps) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, int primitive) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, int primitive, boolean arrowCaps) {
        apiv2.scene.add_polyline(name, points, color, lineWidth, primitive, arrowCaps);
    }

    @Override
    public void removeModelObject(String name) {
        apiv2.scene.remove_object(name);
    }

    @Override
    public void postRunnable(Runnable runnable) {
        apiv2.base.post_runnable(runnable);
    }

    @Override
    public void parkRunnable(String id, Runnable runnable) {
        apiv2.base.park_runnable(id, runnable);
    }

    @Override
    public void parkSceneRunnable(String id, Runnable runnable) {
        apiv2.base.park_scene_runnable(id, runnable);
    }

    @Override
    public void parkCameraRunnable(String id, Runnable runnable) {
        apiv2.base.park_camera_runnable(id, runnable);
    }

    @Override
    public void removeRunnable(String id) {
        apiv2.base.remove_runnable(id);
    }

    @Override
    public void unparkRunnable(String id) {
        apiv2.base.remove_runnable(id);
    }

    @Override
    public void setCameraState(double[] pos, double[] dir, double[] up) {
        apiv2.camera.set_state(pos, dir, up);
    }

    public void setCameraState(List<?> pos, List<?> dir, List<?> up) {
        apiv2.camera.set_state(pos, dir, up);
    }

    @Override
    public void setCameraStateAndTime(double[] pos, double[] dir, double[] up, long time) {
        apiv2.camera.set_state_and_time(pos, dir, up, time);
    }

    public void setCameraStateAndTime(List<?> pos, List<?> dir, List<?> up, long time) {
        apiv2.camera.set_state_and_time(pos, dir, up, time);
    }

    @Override
    public void resetImageSequenceNumber() {
        apiv2.output.reset_frame_output_sequence_number();
    }

    @Override
    public boolean loadDataset(String dsName, String absolutePath) {
        return apiv2.data.load_dataset(dsName, absolutePath);
    }

    @Override
    public boolean loadDataset(String dsName, String path, boolean sync) {
        return apiv2.data.load_dataset(dsName, path, sync);
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoSource type, boolean sync) {
        return apiv2.data.load_dataset(dsName, path, type, sync);
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        return apiv2.data.load_dataset(dsName, path, type, datasetOptions, sync);
    }

    public boolean loadDataset(String dsName, DataSource ds, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        return apiv2.data.load_dataset(dsName, ds, type, datasetOptions, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, boolean sync) {
        return apiv2.data.load_star_dataset(dsName, path, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, boolean sync) {
        return apiv2.data.load_star_dataset(dsName, path, magnitudeScale, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, double[] labelColor, boolean sync) {
        return apiv2.data.load_star_dataset(dsName, path, magnitudeScale, labelColor, sync);
    }

    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, final List<?> labelColor, boolean sync) {
        return apiv2.data.load_star_dataset(dsName, path, magnitudeScale, labelColor, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName,
                                   String path,
                                   double magnitudeScale,
                                   double[] labelColor,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync) {
        return apiv2.data.load_star_dataset(dsName,
                                            path,
                                            magnitudeScale,
                                            labelColor,
                                            fadeIn,
                                            fadeOut,
                                            sync);
    }

    public boolean loadStarDataset(String dsName,
                                   String path,
                                   double magnitudeScale,
                                   final List<?> labelColor,
                                   final List<?> fadeIn,
                                   final List<?> fadeOut,
                                   boolean sync) {
        return apiv2.data.load_star_dataset(dsName,
                                            path,
                                            magnitudeScale,
                                            labelColor,
                                            fadeIn,
                                            fadeOut,
                                            sync);
    }

    public boolean loadStarDataset(String dsName,
                                   String path,
                                   CatalogInfoSource type,
                                   double magnitudeScale,
                                   double[] labelColor,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync) {
        return apiv2.data.load_star_dataset(dsName,
                                            path,
                                            type,
                                            magnitudeScale,
                                            labelColor,
                                            fadeIn,
                                            fadeOut,
                                            sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       double[] particleColor,
                                       double colorNoise,
                                       double[] labelColor,
                                       double particleSize,
                                       String ct,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                ct,
                                                sync);
    }

    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       List<?> particleColor,
                                       double colorNoise,
                                       List<?> labelColor,
                                       double particleSize,
                                       String ct,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                ct,
                                                sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       double[] particleColor,
                                       double colorNoise,
                                       double[] labelColor,
                                       double particleSize,
                                       String ct,
                                       double[] fadeIn,
                                       double[] fadeOut,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                ct,
                                                fadeIn,
                                                fadeOut,
                                                sync);
    }

    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       final List<?> particleColor,
                                       double colorNoise,
                                       final List<?> labelColor,
                                       double particleSize,
                                       String ct,
                                       final List<?> fadeIn,
                                       final List<?> fadeOut,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                ct,
                                                fadeIn,
                                                fadeOut,
                                                sync);
    }

    @Override
    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       double[] particleColor,
                                       double colorNoise,
                                       double[] labelColor,
                                       double particleSize,
                                       double[] sizeLimits,
                                       String ct,
                                       double[] fadeIn,
                                       double[] fadeOut,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                sizeLimits,
                                                ct,
                                                fadeIn,
                                                fadeOut,
                                                sync);
    }

    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       final List<?> particleColor,
                                       double colorNoise,
                                       final List<?> labelColor,
                                       double particleSize,
                                       List<?> sizeLimits,
                                       String ct,
                                       final List<?> fadeIn,
                                       final List<?> fadeOut,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                sizeLimits,
                                                ct,
                                                fadeIn,
                                                fadeOut,
                                                sync);
    }

    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       double profileDecay,
                                       double[] particleColor,
                                       double colorNoise,
                                       double[] labelColor,
                                       double particleSize,
                                       double[] sizeLimits,
                                       ComponentType ct,
                                       double[] fadeIn,
                                       double[] fadeOut,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                sizeLimits,
                                                ct,
                                                fadeIn,
                                                fadeOut,
                                                sync);
    }

    public boolean loadParticleDataset(String dsName,
                                       String path,
                                       CatalogInfoSource type,
                                       double profileDecay,
                                       double[] particleColor,
                                       double colorNoise,
                                       double[] labelColor,
                                       double particleSize,
                                       double[] sizeLimits,
                                       ComponentType ct,
                                       double[] fadeIn,
                                       double[] fadeOut,
                                       boolean sync) {
        return apiv2.data.load_particle_dataset(dsName,
                                                path,
                                                type,
                                                profileDecay,
                                                particleColor,
                                                colorNoise,
                                                labelColor,
                                                particleSize,
                                                sizeLimits,
                                                ct,
                                                fadeIn,
                                                fadeOut,
                                                sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List<?> particleColor, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, fadeIn, fadeOut, sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          double[] particleColor,
                                          double[] labelColor,
                                          double[] fadeIn,
                                          double[] fadeOut,
                                          boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          List<?> particleColor,
                                          List<?> labelColor,
                                          List<?> fadeIn,
                                          List<?> fadeOut,
                                          boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, fadeIn, fadeOut, sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          double[] particleColor,
                                          String ct,
                                          double[] fadeIn,
                                          double[] fadeOut,
                                          boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, ct, fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          List<?> particleColor,
                                          String ct,
                                          List<?> fadeIn,
                                          List<?> fadeOut,
                                          boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, ct, fadeIn, fadeOut, sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          double[] particleColor,
                                          double[] labelColor,
                                          String ct,
                                          double[] fadeIn,
                                          double[] fadeOut,
                                          boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, labelColor, ct, fadeIn, fadeOut, sync);
    }

    @Override
    public boolean loadVariableStarDataset(String dsName,
                                           String path,
                                           double magnitudeScale,
                                           double[] labelColor,
                                           double[] fadeIn,
                                           double[] fadeOut,
                                           boolean sync) {
        return apiv2.data.load_variable_star_dataset(dsName, path, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadVariableStarDataset(String dsName,
                                           String path,
                                           CatalogInfoSource type,
                                           double magnitudeScale,
                                           double[] labelColor,
                                           double[] fadeIn,
                                           double[] fadeOut,
                                           boolean sync) {
        return apiv2.data.load_variable_star_dataset(dsName, path, type, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          List<?> particleColor,
                                          List<?> labelColor,
                                          String ct,
                                          List<?> fadeIn,
                                          List<?> fadeOut,
                                          boolean sync) {
        return apiv2.data.load_star_cluster_dataset(dsName, path, particleColor, labelColor, ct, fadeIn, fadeOut, sync);
    }

    public boolean loadJsonCatalog(String dsName, String path) {
        return apiv2.data.load_json_dataset(dsName, path);
    }

    @Override
    public boolean loadJsonDataset(String dsName, String path) {
        return apiv2.data.load_json_dataset(dsName, path);
    }

    public boolean loadJsonDataset(String dsName, String pathString, boolean sync) {
        return apiv2.data.load_json_dataset(dsName, pathString, sync);
    }

    public boolean loadJsonDataset(String dsName, String pathString, boolean select, boolean sync) {
        return apiv2.data.load_json_dataset(dsName, pathString, select, sync);
    }

    @Override
    public boolean hasDataset(String dsName) {
        return apiv2.data.dataset_exists(dsName);
    }

    @Override
    public boolean setDatasetTransformationMatrix(String dsName, double[] matrix) {
        return apiv2.data.set_dataset_transform_matrix(dsName, matrix);
    }

    public boolean clearDatasetTransformationMatrix(String dsName) {
        return apiv2.data.clear_dataset_transform_matrix(dsName);
    }

    @Override
    public boolean removeDataset(String dsName) {
        return apiv2.data.remove_dataset(dsName);
    }

    @Override
    public boolean hideDataset(String dsName) {
        return apiv2.data.hide_dataset(dsName);
    }

    @Override
    public boolean showDataset(String dsName) {
        return apiv2.data.show_dataset(dsName);
    }

    @Override
    public boolean highlightDataset(String dsName, boolean highlight) {
        return apiv2.data.highlight_dataset(dsName, highlight);
    }

    @Override
    public boolean highlightDataset(String dsName, int colorIndex, boolean highlight) {
        return apiv2.data.highlight_dataset(dsName, colorIndex, highlight);
    }

    @Override
    public boolean highlightDataset(String dsName, float r, float g, float b, float a, boolean highlight) {
        float[] color = new float[]{r, g, b, a};
        return apiv2.data.highlight_dataset(dsName, color, highlight);
    }

    @Override
    public boolean highlightDataset(String dsName, String attributeName, String colorMap, double minMap, double maxMap, boolean highlight) {
        return apiv2.data.highlight_dataset(dsName, attributeName, colorMap, minMap, maxMap, highlight);
    }

    @Override
    public boolean setDatasetHighlightSizeFactor(String dsName, float sizeFactor) {
        return apiv2.data.set_dataset_highlight_size_factor(dsName, sizeFactor);
    }

    @Override
    public boolean setDatasetHighlightAllVisible(String dsName, boolean allVisible) {
        return apiv2.data.set_dataset_highlight_all_visible(dsName, allVisible);
    }

    @Override
    public void setDatasetPointSizeMultiplier(String dsName, double multiplier) {
        apiv2.data.set_dataset_point_size_factor(dsName, multiplier);
    }

    @Override
    public void addShapeAroundObject(String shapeName,
                                     String shapeType,
                                     String primitive,
                                     double size,
                                     String objectName,
                                     float r,
                                     float g,
                                     float b,
                                     float a,
                                     boolean showLabel,
                                     boolean trackObject) {
        apiv2.scene.add_shape_around_object(shapeName, shapeType, primitive, size, objectName, r, g, b, a, showLabel, trackObject);
    }

    @Override
    public void addShapeAroundObject(String shapeName,
                                     String shapeType,
                                     String primitive,
                                     String orientation,
                                     double size,
                                     String objectName,
                                     float r,
                                     float g,
                                     float b,
                                     float a,
                                     boolean showLabel,
                                     boolean trackObject) {
        apiv2.scene.add_shape_around_object(shapeName, shapeType, primitive, size, objectName, r, g, b, a, showLabel, trackObject);
    }

    @Override
    public void backupSettings() {
        apiv2.base.settings_backup();
    }

    @Override
    public boolean restoreSettings() {
        return apiv2.base.settings_restore();
    }

    @Override
    public void clearSettingsStack() {
        apiv2.base.settings_clear_stack();
    }

    @Override
    public void resetUserInterface() {
        apiv2.ui.reload();
    }

    @Override
    public double[] getRefSysTransform(String name) {
        return apiv2.refsys.get_transform_matrix(name);
    }

    @Override
    public void setMaximumSimulationTime(long years) {
        apiv2.time.setMaximumSimulationTime(years);
    }

    public void setMaximumSimulationTime(double years) {
        apiv2.time.setMaximumSimulationTime(years);
    }

    public void setMaximumSimulationTime(Long years) {
        apiv2.time.setMaximumSimulationTime(years);
    }

    public void setMaximumSimulationTime(Double years) {
        apiv2.time.setMaximumSimulationTime(years);
    }

    public void setMaximumSimulationTime(Integer years) {
        apiv2.time.setMaximumSimulationTime(years);
    }

    @Override
    public double getMeterToInternalUnitConversion() {
        return apiv2.base.m_to_internal();
    }

    @Override
    public double getInternalUnitToMeterConversion() {
        return apiv2.base.internal_to_m();
    }

    @Override
    public double internalUnitsToMetres(double internalUnits) {
        return apiv2.base.internal_to_m(internalUnits);
    }

    @Override
    public double internalUnitsToKilometres(double internalUnits) {
        return apiv2.base.internal_to_km(internalUnits);
    }

    @Override
    public double[] internalUnitsToKilometres(double[] internalUnits) {
        return apiv2.base.internal_to_km(internalUnits);
    }

    @Override
    public double internalUnitsToParsecs(double internalUnits) {
        return apiv2.base.internal_to_pc(internalUnits);
    }

    @Override
    public double[] internalUnitsToParsecs(double[] internalUnits) {
        return apiv2.base.internal_to_pc(internalUnits);
    }

    public double[] internalUnitsToKilometres(List<?> internalUnits) {
        return apiv2.base.internal_to_km(internalUnits);
    }

    @Override
    public double metresToInternalUnits(double metres) {
        return apiv2.base.m_to_internal(metres);
    }

    @Override
    public double kilometresToInternalUnits(double kilometres) {
        return apiv2.base.km_to_internal(kilometres);
    }

    @Override
    public double parsecsToInternalUnits(double parsecs) {
        return apiv2.base.pc_to_internal(parsecs);
    }

    public double kilometersToInternalUnits(double kilometres) {
        return apiv2.base.km_to_internal(kilometres);
    }

    @Override
    public List<String> listDatasets() {
        return apiv2.data.list_datasets();
    }

    @Override
    public long getFrameNumber() {
        return apiv2.graphics.get_current_frame_number();
    }

    @Override
    public String getDefaultFramesDir() {
        return apiv2.base.get_default_frame_output_dir();
    }

    @Override
    public String getDefaultScreenshotsDir() {
        return apiv2.base.get_default_screenshots_dir();
    }

    @Override
    public String getDefaultCameraDir() {
        return apiv2.base.get_camcorder_dir();
    }

    @Override
    public String getDefaultMappingsDir() {
        return apiv2.base.get_mappings_dir();
    }

    @Override
    public String getDataDir() {
        return apiv2.data.get_datasets_directory();
    }

    @Override
    public String getConfigDir() {
        return apiv2.base.get_config_dir();
    }

    @Override
    public String getLocalDataDir() {
        return apiv2.base.get_data_dir();
    }

    @Override
    public void print(String message) {
        apiv2.base.print(message);
    }

    @Override
    public void log(String message) {
        apiv2.base.log(message);
    }

    @Override
    public void error(String message) {
        apiv2.base.error(message);
    }

    @Override
    public void quit() {
        apiv2.base.quit();
    }

}
