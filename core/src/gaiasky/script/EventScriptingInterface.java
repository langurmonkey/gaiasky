/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.data.SceneJsonLoader;
import gaiasky.data.StarClusterLoader;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.DatasetOptions.DatasetLoadType;
import gaiasky.data.group.STILDataProvider;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.OrientationComboBoxBean.ShapeOrientation;
import gaiasky.gui.beans.PrimitiveComboBoxBean.Primitive;
import gaiasky.gui.beans.ShapeComboBoxBean.Shape;
import gaiasky.gui.window.ColormapPicker;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.SetUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.VertsView;
import gaiasky.script.v2.impl.APIv2;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.DistanceUnits;
import gaiasky.util.Settings.ReprojectionMode;
import gaiasky.util.camera.rec.Camcorder;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.IPythonCoordinatesProvider;
import gaiasky.util.filter.attrib.AttributeUCD;
import gaiasky.util.filter.attrib.IAttribute;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.*;
import gaiasky.util.screenshot.ImageRenderer;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Implementation of the Gaia Sky scripting API located at {@link IScriptingInterface}. This implementation uses
 * mainly the event manager to communicate with the rest of Gaia Sky.
 */
@SuppressWarnings({"unused", "WeakerAccess", "SwitchStatementWithTooFewBranches", "SingleStatementInBlock", "SameParameterValue"})
public final class EventScriptingInterface implements IScriptingInterface, IObserver {
    private static final Log logger = Logger.getLogger(EventScriptingInterface.class);

    // Reference to the event manager
    private final EventManager em;
    // Reference to asset manager
    private final AssetManager manager;
    // Reference to the catalog manager
    private final CatalogManager catalogManager;
    // Auxiliary vectors
    private final Vector3D aux3d1, aux3d2, aux3d3, aux3d4, aux3d5, aux3d6;
    private final Vector3Q aux3b1, aux3b2, aux3b3, aux3b4, aux3b5;
    private final Vector2D aux2d1;
    private final Set<AtomicBoolean> stops;
    private final FocusView focusView;
    private final VertsView vertsView;
    private LruCache<String, Texture> textures;
    private TrajectoryUtils trajectoryUtils;
    private Scene scene;
    private int inputCode = -1;
    private int cTransSeq = 0;

    private final Deque<Settings> settingsStack;


    /** APIv2 object reference. This is the gateway to access the methods and calls in APIv2. **/
    public final APIv2 apiv2;

    public EventScriptingInterface(final AssetManager manager, final CatalogManager catalogManager) {
        this.em = EventManager.instance;
        this.manager = manager;
        this.catalogManager = catalogManager;
        this.focusView = new FocusView();
        this.vertsView = new VertsView();
        this.settingsStack = new ConcurrentLinkedDeque<>();
        this.apiv2 = new APIv2(manager, catalogManager);

        stops = new HashSet<>();

        // Auxiliary vectors
        aux3d1 = new Vector3D();
        aux3d2 = new Vector3D();
        aux3d3 = new Vector3D();
        aux3d4 = new Vector3D();
        aux3d5 = new Vector3D();
        aux3d6 = new Vector3D();
        aux3b1 = new Vector3Q();
        aux3b2 = new Vector3Q();
        aux3b3 = new Vector3Q();
        aux3b4 = new Vector3Q();
        aux3b5 = new Vector3Q();
        aux2d1 = new Vector2D();

        em.subscribe(this, Event.INPUT_EVENT, Event.DISPOSE, Event.SCENE_LOADED);
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
        apiv2.ui.display_message(id, message, x, y, r ,g, b, a, fontSize);
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
        if (checkNum(targetFps, Constants.MIN_FPS, Constants.MAX_FPS, "targetFps")) {
            em.post(Event.CAMRECORDER_FPS_CMD, this, targetFps);
        }
    }

    @Override
    public void setCameraRecorderFps(double targetFps) {
        setCamcorderFps(targetFps);
    }

    @Override
    public double getCamcorderFps() {
        return apiv2.camcorder.get_camcorder_fps();
    }

    @Override
    public void preloadTexture(String path) {
        preloadTextures(new String[]{path});
    }

    @Override
    public String getAssetsLocation() {
        return Settings.ASSETS_LOC;
    }

    @Override
    public void preloadTextures(String[] paths) {
        initializeTextures();
        for (final String path : paths) {
            // This only works in async mode!
            postRunnable(() -> manager.load(path, Texture.class));
            while (!manager.isLoaded(path)) {
                sleepFrames(1);
            }
            Texture tex = manager.get(path, Texture.class);
            textures.put(path, tex);
        }
    }

    @Override
    public void startRecordingCameraPath() {
        em.post(Event.RECORD_CAMERA_CMD, this, true, null);
    }

    @Override
    public void startRecordingCameraPath(String fileName) {
        em.post(Event.RECORD_CAMERA_CMD, this, true, Path.of(fileName).getFileName().toString());
    }

    @Override
    public void stopRecordingCameraPath() {
        em.post(Event.RECORD_CAMERA_CMD, this, false, null, false);
    }

    @Override
    public void playCameraPath(String file, boolean sync) {
        runCameraPath(file, sync);
    }

    @Override
    public void runCameraPath(String file, boolean sync) {
        em.post(Event.PLAY_CAMERA_CMD, this, true, file);

        // Wait if needed
        if (sync) {
            Object monitor = new Object();
            IObserver watcher = (event, source, data) -> {
                switch (event) {
                    case CAMERA_PLAY_INFO -> {
                        Boolean status = (Boolean) data[0];
                        if (!status) {
                            synchronized (monitor) {
                                monitor.notify();
                            }
                        }
                    }
                    default -> {
                    }
                }
            };
            em.subscribe(watcher, Event.CAMERA_PLAY_INFO);
            // Wait for camera to finish
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    logger.error(e, "Error waiting for camera file to finish");
                }
            }
        }
    }

    @Override
    public void playCameraPath(String file) {
        runCameraPath(file);
    }

    @Override
    public void runCameraPath(String file) {
        runCameraPath(file, false);
    }

    @Override
    public void runCameraRecording(String file) {
        runCameraPath(file, false);
    }

    @Override
    public void cameraTransitionKm(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        cameraTransition(camPos, "km", camDir, camUp, seconds, true);
    }

    public void cameraTransitionKm(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        cameraTransitionKm(dArray(camPos), dArray(camDir), dArray(camUp), seconds);
    }

    public void cameraTransitionKm(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        cameraTransitionKm(camPos, camDir, camUp, (double) seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, String units, double[] camDir, double[] camUp, double seconds) {
        cameraTransition(camPos, units, camDir, camUp, seconds, true);
    }

    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, long seconds) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds);
    }

    public void cameraTransition(double[] camPos, String units, double[] camDir, double[] camUp, long seconds) {
        cameraTransition(camPos, units, camDir, camUp, (double) seconds);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, double seconds) {
        cameraTransition(dArray(camPos), units, dArray(camDir), dArray(camUp), seconds);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, long seconds) {
        cameraTransition(dArray(camPos), units, dArray(camDir), dArray(camUp), seconds);
    }

    @Override
    public void cameraTransition(double[] camPos, double[] camDir, double[] camUp, double seconds, boolean sync) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds, sync);
    }

    @Override
    public void cameraTransition(double[] camPos, String units, double[] camDir, double[] camUp, double seconds, boolean sync) {
        cameraTransition(camPos, units, camDir, camUp, seconds, "none", 0, seconds, "none", 0, sync);
    }

    public void cameraTransition(List<?> camPos,
                                 List<?> camDir,
                                 List<?> camUp,
                                 double seconds,
                                 String positionSmoothType,
                                 double positionSmoothFactor,
                                 String orientationSmoothType,
                                 double orientationSmoothFactor) {
        cameraTransition(dArray(camPos),
                         "internal",
                         dArray(camDir),
                         dArray(camUp),
                         seconds,
                         positionSmoothType,
                         positionSmoothFactor,
                         seconds,
                         orientationSmoothType,
                         orientationSmoothFactor,
                         true);
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
        cameraTransition(camPos,
                         "internal",
                         camDir,
                         camUp,
                         positionDurationSeconds,
                         positionSmoothType,
                         positionSmoothFactor,
                         orientationDurationSeconds,
                         orientationSmoothType,
                         orientationSmoothFactor,
                         true);
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
        cameraTransition(dArray(camPos),
                         units,
                         dArray(camDir),
                         dArray(camUp),
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
        cameraTransition(camPos,
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
                         null);
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
        if (checkDistanceUnits(units, "units") && checkSmoothType(positionSmoothType, "positionSmoothType") && checkSmoothType(orientationSmoothType,
                                                                                                                               "orientationSmoothType")) {
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            // Put camera in free mode.
            em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);

            // Set up final actions
            String name = "cameraTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> unparkRunnable(name);

            var u = DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            double[] finalPosition = new double[]{u.toInternalUnits(camPos[0]), u.toInternalUnits(camPos[1]), u.toInternalUnits(camPos[2])};

            // Create and park runnable
            CameraTransitionRunnable r = new CameraTransitionRunnable(cam,
                                                                      finalPosition,
                                                                      camDir,
                                                                      camUp,
                                                                      positionDurationSeconds,
                                                                      positionSmoothType,
                                                                      positionSmoothFactor,
                                                                      orientationDurationSeconds,
                                                                      orientationSmoothType,
                                                                      orientationSmoothFactor,
                                                                      end,
                                                                      stop);
            parkRunnable(name, r);

            if (sync) {
                // Wait on lock
                synchronized (r.lock) {
                    try {
                        r.lock.wait();
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }

                // Remove and return
                unparkRunnable(name);
            }
        }
    }

    @Override
    public void cameraPositionTransition(double[] camPos,
                                         String units,
                                         double durationSeconds,
                                         String smoothType,
                                         double smoothFactor,
                                         boolean sync) {
        cameraPositionTransition(camPos, units, durationSeconds, smoothType, smoothFactor, sync, null);
    }

    public void cameraPositionTransition(double[] camPos,
                                         String units,
                                         double durationSeconds,
                                         String smoothType,
                                         double smoothFactor,
                                         boolean sync,
                                         AtomicBoolean stop) {
        if (checkDistanceUnits(units, "units") && checkSmoothType(smoothType, "smoothType")) {
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            // Put camera in free mode.
            em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);

            // Set up final actions
            String name = "cameraTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> unparkRunnable(name);

            var u = DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            double[] posUnits = new double[]{u.toInternalUnits(camPos[0]), u.toInternalUnits(camPos[1]), u.toInternalUnits(camPos[2])};

            // Create and park position transition runnable
            CameraTransitionRunnable r = new CameraTransitionRunnable(cam, posUnits, durationSeconds, smoothType, smoothFactor, end, stop);
            parkRunnable(name, r);

            if (sync) {
                // Wait on lock
                synchronized (r.lock) {
                    try {
                        r.lock.wait();
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }

                // Remove and return
                unparkRunnable(name);
            }
        }

    }

    @Override
    public void cameraOrientationTransition(double[] camDir,
                                            double[] camUp,
                                            double durationSeconds,
                                            String smoothType,
                                            double smoothFactor,
                                            boolean sync) {
        cameraOrientationTransition(camDir, camUp, durationSeconds, smoothType, smoothFactor, sync, null);
    }

    public void cameraOrientationTransition(double[] camDir,
                                            double[] camUp,
                                            double durationSeconds,
                                            String smoothType,
                                            double smoothFactor,
                                            boolean sync,
                                            AtomicBoolean stop) {
        if (checkSmoothType(smoothType, "smoothType")) {
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            // Put camera in free mode.
            em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);

            // Set up final actions
            String name = "cameraTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> unparkRunnable(name);

            // Create and park orientation transition runnable
            CameraTransitionRunnable r = new CameraTransitionRunnable(cam, camDir, camUp, durationSeconds, smoothType, smoothFactor, end, stop);
            parkRunnable(name, r);

            if (sync) {
                // Wait on lock.
                synchronized (r.lock) {
                    try {
                        r.lock.wait();
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }

                // Remove and return
                unparkRunnable(name);
            }
        }

    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds, sync);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        cameraTransition(dArray(camPos), units, dArray(camDir), dArray(camUp), seconds, sync);
    }

    public void cameraTransition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        cameraTransition(camPos, "internal", camDir, camUp, seconds, sync);
    }

    public void cameraTransition(List<?> camPos, String units, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        cameraTransition(camPos, units, camDir, camUp, (double) seconds, sync);
    }

    public void cameraOrientationTransition(List<?> camDir,
                                            List<?> camUp,
                                            double durationSeconds,
                                            String smoothType,
                                            double smoothFactor,
                                            boolean sync) {
        cameraOrientationTransition(dArray(camDir), dArray(camUp), durationSeconds, smoothType, smoothFactor, sync);
    }

    public void cameraPositionTransition(List<?> camPos, String units, double durationSeconds, String smoothType, double smoothFactor, boolean sync) {
        cameraPositionTransition(dArray(camPos), units, durationSeconds, smoothType, smoothFactor, sync);
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
        timeTransition(year, month, day, hour, min, sec, milliseconds, durationSeconds, smoothType, smoothFactor, sync, null);
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
        if (checkDateTime(year, month, day, hour, min, sec, milliseconds)) {
            // Set up final actions
            String name = "timeTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> unparkRunnable(name);

            // Create and park orientation transition runnable
            TimeTransitionRunnable r = new TimeTransitionRunnable(year,
                                                                  month,
                                                                  day,
                                                                  hour,
                                                                  min,
                                                                  sec,
                                                                  milliseconds,
                                                                  durationSeconds,
                                                                  smoothType,
                                                                  smoothFactor,
                                                                  end,
                                                                  stop);
            parkRunnable(name, r);

            if (sync) {
                // Wait on lock.
                synchronized (r.lock) {
                    try {
                        r.lock.wait();
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }

                // Remove and return
                removeRunnable(name);
            }

        }
    }

    @Override
    public void sleep(float seconds) {
        if (checkNum(seconds, 0f, Float.MAX_VALUE, "seconds")) {
            if (seconds == 0f) return;

            if (isFrameOutputActive()) {
                sleepFrames(Math.max(1, FastMath.round(getFrameOutputFps() * seconds)));
            } else if (Camcorder.instance.isRecording()) {
                sleepFrames(Math.max(1, FastMath.round(getCamcorderFps() * seconds)));
            } else {
                try {
                    Thread.sleep(Math.round(seconds * 1000f));
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
        }
    }

    public void sleep(int seconds) {
        sleep((float) seconds);
    }

    @Override
    public void sleepFrames(long frames) {
        long frameCount = 0;
        while (frameCount < frames) {
            try {
                synchronized (GaiaSky.instance.frameMonitor) {
                    GaiaSky.instance.frameMonitor.wait();
                }
                frameCount++;
            } catch (InterruptedException e) {
                logger.error("Error while waiting on frameMonitor", e);
            }
        }
    }

    /**
     * Checks if the object is the current focus of the given camera. If it is not,
     * it sets it as focus and waits if necessary.
     *
     * @param object          The new focus object.
     * @param cam             The current camera.
     * @param waitTimeSeconds Max time to wait for the camera to face the focus, in
     *                        seconds. If negative, we wait until the end.
     */
    private void changeFocus(FocusView object, NaturalCamera cam, double waitTimeSeconds) {
        // Post focus change and wait, if needed
        FocusView currentFocus = (FocusView) cam.getFocus();
        if (currentFocus == null || currentFocus.isSet() || currentFocus.getEntity() != object.getEntity()) {
            em.post(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
            em.post(Event.FOCUS_CHANGE_CMD, this, object.getEntity());

            // Wait til camera is facing focus or
            if (waitTimeSeconds < 0) {
                waitTimeSeconds = Double.MAX_VALUE;
            }
            long start = System.currentTimeMillis();
            double elapsedSeconds = 0;
            while (!cam.facingFocus && elapsedSeconds < waitTimeSeconds) {
                // Wait
                try {
                    sleepFrames(1);
                } catch (Exception e) {
                    logger.error(e);
                }
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000d;
            }
        }
    }

    @Override
    public double[] galacticToInternalCartesian(double l, double b, double r) {
        Vector3D pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3D());
        pos.mul(Coordinates.galacticToEquatorial());
        return new double[]{pos.x, pos.y, pos.z};
    }

    public double[] galacticToInternalCartesian(int l, int b, int r) {
        return galacticToInternalCartesian((double) l, (double) b, (double) r);
    }

    @Override
    public double[] eclipticToInternalCartesian(double l, double b, double r) {
        Vector3D pos = Coordinates.sphericalToCartesian(l * Nature.TO_RAD, b * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3D());
        pos.mul(Coordinates.eclipticToEquatorial());
        return new double[]{pos.x, pos.y, pos.z};
    }

    public double[] eclipticToInternalCartesian(int l, int b, int r) {
        return eclipticToInternalCartesian((double) l, (double) b, (double) r);
    }

    @Override
    public double[] equatorialToInternalCartesian(double ra, double dec, double r) {
        Vector3D pos = Coordinates.sphericalToCartesian(ra * Nature.TO_RAD, dec * Nature.TO_RAD, r * Constants.KM_TO_U, new Vector3D());
        return new double[]{pos.x, pos.y, pos.z};
    }

    public double[] equatorialToInternalCartesian(int ra, int dec, int r) {
        return equatorialToInternalCartesian((double) ra, (double) dec, (double) r);
    }

    public double[] internalCartesianToEquatorial(double x, double y, double z) {
        Vector3Q in = aux3b1.set(x, y, z);
        Vector3D out = aux3d6;
        Coordinates.cartesianToSpherical(in, out);
        return new double[]{out.x * Nature.TO_DEG, out.y * Nature.TO_DEG, in.lenDouble()};
    }

    public double[] internalCartesianToEquatorial(int x, int y, int z) {
        return internalCartesianToEquatorial((double) x, (double) y, (double) z);
    }

    @Override
    public double[] equatorialCartesianToInternalCartesian(double[] eq, double kmFactor) {
        aux3d1.set(eq).scl(kmFactor).scl(Constants.KM_TO_U);
        return new double[]{aux3d1.y, aux3d1.z, aux3d1.x};
    }

    public double[] equatorialCartesianToInternalCartesian(final List<?> eq, double kmFactor) {
        return equatorialCartesianToInternalCartesian(dArray(eq), kmFactor);
    }

    @Override
    public double[] equatorialToGalactic(double[] eq) {
        aux3d1.set(eq).mul(Coordinates.eqToGal());
        return aux3d1.values();
    }

    public double[] equatorialToGalactic(List<?> eq) {
        return equatorialToGalactic(dArray(eq));
    }

    @Override
    public double[] equatorialToEcliptic(double[] eq) {
        aux3d1.set(eq).mul(Coordinates.eqToEcl());
        return aux3d1.values();
    }

    public double[] equatorialToEcliptic(List<?> eq) {
        return equatorialToEcliptic(dArray(eq));
    }

    @Override
    public double[] galacticToEquatorial(double[] gal) {
        aux3d1.set(gal).mul(Coordinates.galToEq());
        return aux3d1.values();
    }

    public double[] galacticToEquatorial(List<?> gal) {
        return galacticToEquatorial(dArray(gal));
    }

    @Override
    public double[] eclipticToEquatorial(double[] ecl) {
        aux3d1.set(ecl).mul(Coordinates.eclToEq());
        return aux3d1.values();
    }

    public double[] eclipticToEquatorial(List<?> ecl) {
        return eclipticToEquatorial(dArray(ecl));
    }

    @Override
    public void setBrightnessLevel(double level) {
        if (checkNum(level, -1d, 1d, "brightness")) postRunnable(() -> em.post(Event.BRIGHTNESS_CMD, this, (float) level));
    }

    public void setBrightnessLevel(long level) {
        setBrightnessLevel((double) level);
    }

    @Override
    public void setContrastLevel(double level) {
        if (checkNum(level, 0d, 2d, "contrast")) postRunnable(() -> em.post(Event.CONTRAST_CMD, this, (float) level));
    }

    public void setContrastLevel(long level) {
        setContrastLevel((double) level);
    }

    @Override
    public void setHueLevel(double level) {
        if (checkNum(level, 0d, 2d, "hue")) postRunnable(() -> em.post(Event.HUE_CMD, this, (float) level));
    }

    public void setHueLevel(long level) {
        setHueLevel((double) level);
    }

    @Override
    public void setSaturationLevel(double level) {
        if (checkNum(level, 0d, 2d, "saturation")) postRunnable(() -> em.post(Event.SATURATION_CMD, this, (float) level));
    }

    public void setSaturationLevel(long level) {
        setSaturationLevel((double) level);
    }

    @Override
    public void setGammaCorrectionLevel(double level) {
        if (checkNum(level, 0d, 3d, "gamma correction")) postRunnable(() -> em.post(Event.GAMMA_CMD, this, (float) level));
    }

    public void setGammaCorrectionLevel(long level) {
        setGammaCorrectionLevel((double) level);
    }

    @Override
    public void setHDRToneMappingType(String type) {
        if (checkString(type, new String[]{"auto", "AUTO", "exposure", "EXPOSURE", "none", "NONE"}, "tone mapping type"))
            postRunnable(() -> em.post(Event.TONEMAPPING_TYPE_CMD, this, Settings.ToneMapping.valueOf(type.toUpperCase(Locale.ROOT))));
    }

    @Override
    public void setExposureToneMappingLevel(double level) {
        if (checkNum(level, 0d, 20d, "exposure")) postRunnable(() -> em.post(Event.EXPOSURE_CMD, this, (float) level));
    }

    public void setExposureToneMappingLevel(long level) {
        setExposureToneMappingLevel((double) level);
    }

    @Override
    public void setCubemapMode(boolean state, String projection) {
        if (checkStringEnum(projection, CubemapProjection.class, "projection")) {
            CubmeapProjectionEffect.CubemapProjection newProj = CubemapProjection.valueOf(projection.toUpperCase(Locale.ROOT));
            postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, newProj));
        }
    }

    @Override
    public void setPanoramaMode(boolean state) {
        postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubemapProjection.EQUIRECTANGULAR));
    }

    @Override
    public void setReprojectionMode(String mode) {
        if (checkStringEnum(mode, ReprojectionMode.class, "re-projection mode")) {
            ReprojectionMode newMode = ReprojectionMode.valueOf(mode.toUpperCase(Locale.ROOT));
            postRunnable(() -> em.post(Event.REPROJECTION_CMD, this, newMode != ReprojectionMode.DISABLED, newMode));
        }
    }

    @Override
    public void setBackBufferScale(float scale) {
        if (checkNum(scale, 0.5f, 4f, "back buffer scale")) {
            postRunnable(() -> GaiaSky.instance.resetDynamicResolution());
            postRunnable(() -> em.post(Event.BACKBUFFER_SCALE_CMD, this, scale));
        }
    }

    @Override
    public void setIndexOfRefraction(float ior) {
        em.post(Event.INDEXOFREFRACTION_CMD, this, ior);
    }

    @Override
    public void setPlanetariumMode(boolean state) {
        postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubemapProjection.AZIMUTHAL_EQUIDISTANT));
    }

    @Override
    public void setCubemapResolution(int resolution) {
        if (checkNum(resolution, 20, 15000, "resolution")) {
            postRunnable(() -> em.post(Event.CUBEMAP_RESOLUTION_CMD, this, resolution));
        }
    }

    @Override
    public void setCubemapProjection(String projection) {
        if (checkStringEnum(projection, CubemapProjection.class, "projection")) {
            CubemapProjection newProj = CubemapProjection.valueOf(projection.toUpperCase(Locale.ROOT));
            em.post(Event.CUBEMAP_PROJECTION_CMD, this, newProj);
        }
    }

    @Override
    public void setOrthosphereViewMode(boolean state) {
        postRunnable(() -> em.post(Event.CUBEMAP_CMD, this, state, CubemapProjection.ORTHOSPHERE));
    }

    @Override
    public void setStereoscopicMode(boolean state) {
        postRunnable(() -> em.post(Event.STEREOSCOPIC_CMD, this, state));
    }

    @Override
    public void setStereoscopicProfile(int index) {
        postRunnable(() -> em.post(Event.STEREO_PROFILE_CMD, this, index));
    }

    @Override
    public long getCurrentFrameNumber() {
        return GaiaSky.instance.frames;
    }

    @Override
    public void setLensFlare(boolean state) {
        postRunnable(() -> em.post(Event.LENS_FLARE_CMD, this, state ? 1f : 0f));
    }

    @Override
    public void setLensFlare(double strength) {
        if (checkNum(strength, Constants.MIN_LENS_FLARE_STRENGTH, Constants.MAX_LENS_FLARE_STRENGTH, "strength")) {
            postRunnable(() -> em.post(Event.LENS_FLARE_CMD, this, (float) strength));
        }
    }

    @Override
    public void setMotionBlur(boolean active) {
        var strength = active ? 0.8f : 0f;
        postRunnable(() -> em.post(Event.MOTION_BLUR_CMD, this, strength));
    }

    @Override
    public void setMotionBlur(double strength) {
        postRunnable(() -> em.post(Event.MOTION_BLUR_CMD, this, (float) strength));
    }

    @Override
    public void setStarGlow(boolean state) {
        setStarGlowOverObjects(state);
    }

    @Override
    public void setStarGlowOverObjects(boolean state) {
        postRunnable(() -> em.post(Event.LIGHT_GLOW_CMD, this, state));
    }

    @Override
    public void setBloom(float value) {
        if (checkNum(value, 0f, 1f, "bloom strength")) {
            postRunnable(() -> em.post(Event.BLOOM_CMD, this, value));
        }
    }

    @Override
    public void setChromaticAberration(float value) {
        if (checkNum(value, Constants.MIN_CHROMATIC_ABERRATION_AMOUNT, Constants.MAX_CHROMATIC_ABERRATION_AMOUNT, "chromatic aberration amount")) {
            postRunnable(() -> em.post(Event.CHROMATIC_ABERRATION_CMD, this, value));
        }
    }

    public void setBloom(int level) {
        setBloom((float) level);
    }

    @Override
    public void setSmoothLodTransitions(boolean value) {
        postRunnable(() -> em.post(Event.OCTREE_PARTICLE_FADE_CMD, this, value));
    }

    @Override
    public double[] rotate3(double[] vector, double[] axis, double angle) {
        Vector3D v = aux3d1.set(vector);
        Vector3D a = aux3d2.set(axis);
        return v.rotate(a, angle).values();
    }

    public double[] rotate3(double[] vector, double[] axis, long angle) {
        return rotate3(vector, axis, (double) angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, double angle) {
        return rotate3(dArray(vector), dArray(axis), angle);
    }

    public double[] rotate3(List<?> vector, List<?> axis, long angle) {
        return rotate3(vector, axis, (double) angle);
    }

    @Override
    public double[] rotate2(double[] vector, double angle) {
        Vector2D v = aux2d1.set(vector);
        return v.rotate(angle).values();
    }

    public double[] rotate2(double[] vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    public double[] rotate2(List<?> vector, double angle) {
        return rotate2(dArray(vector), angle);
    }

    public double[] rotate2(List<?> vector, long angle) {
        return rotate2(vector, (double) angle);
    }

    @Override
    public double[] cross3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).crs(aux3d2.set(vec2)).values();
    }

    public double[] cross3(List<?> vec1, List<?> vec2) {
        return cross3(dArray(vec1), dArray(vec2));
    }

    @Override
    public double dot3(double[] vec1, double[] vec2) {
        return aux3d1.set(vec1).dot(aux3d2.set(vec2));
    }

    public double dot3(List<?> vec1, List<?> vec2) {
        return dot3(dArray(vec1), dArray(vec2));
    }

    @Override
    public void addTrajectoryLine(String name, double[] points, double[] color) {
        var ignored = addLineObject(name, points, color, 1.5f, GL20.GL_LINE_STRIP, false, -1, "Orbit");
    }

    public void addTrajectoryLine(String name, List<?> points, List<?> color) {
        var ignored = addLineObject(name, points, color, 1.5f, GL20.GL_LINE_STRIP, false, -1, "Orbit");
    }

    @Override
    public void addTrajectoryLine(String name, double[] points, double[] color, double trailMap) {
        var entity = addLineObject(name, points, color, 1.5f, GL20.GL_LINE_STRIP, false, trailMap, "Orbit");
    }

    public void addTrajectoryLine(String name, List<?> points, List<?> color, double trailMap) {
        addTrajectoryLine(name, dArray(points), dArray(color), trailMap);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color) {
        addPolyline(name, points, color, 1f);
    }

    public void addPolyline(String name, List<?> points, List<?> color) {
        addPolyline(name, points, color, 1f);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth) {
        addPolyline(name, points, color, lineWidth, false);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, boolean arrowCaps) {
        addPolyline(name, points, color, lineWidth, GL20.GL_LINE_STRIP, arrowCaps);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive) {
        addPolyline(name, points, color, lineWidth, primitive, false);
    }

    @Override
    public void addPolyline(String name, double[] points, double[] color, double lineWidth, int primitive, boolean arrowCaps) {
        addLineObject(name, points, color, lineWidth, primitive, arrowCaps, -1f, "Polyline");
    }

    public Entity addLineObject(String name,
                                List<?> points,
                                List<?> color,
                                double lineWidth,
                                int primitive,
                                boolean arrowCaps,
                                double trailMap,
                                String archetypeName) {
        return addLineObject(name, dArray(points), dArray(color), lineWidth, primitive, arrowCaps, trailMap, archetypeName);
    }

    public Entity addLineObject(String name,
                                double[] points,
                                double[] color,
                                double lineWidth,
                                int primitive,
                                boolean arrowCaps,
                                double trailMap,
                                String archetypeName) {
        if (checkString(name, "name") && checkNum(lineWidth, 0.1f, 50f, "lineWidth") && checkNum(primitive, 1, 3, "primitive")) {
            var archetype = scene.archetypes().get(archetypeName);
            var entity = archetype.createEntity();

            var base = Mapper.base.get(entity);
            base.setName(name);
            base.setComponentType(ComponentType.Orbits);

            var body = Mapper.body.get(entity);
            body.setColor(color);
            body.setLabelColor(color);
            body.setSizePc(100d);

            var line = Mapper.line.get(entity);
            line.lineWidth = (float) lineWidth;

            var arrow = Mapper.arrow.get(entity);
            arrow.arrowCap = arrowCaps;

            var verts = Mapper.verts.get(entity);
            synchronized (vertsView) {
                vertsView.setEntity(entity);
                vertsView.setPrimitiveSize((float) lineWidth);
                vertsView.setPoints(points);
                vertsView.setRenderGroup(arrowCaps ? RenderGroup.LINE : RenderGroup.LINE_GPU);
                vertsView.setClosedLoop(false);
                vertsView.setGlPrimitive(primitive);
            }

            var trajectory = Mapper.trajectory.get(entity);
            if (trajectory != null) {
                if (trailMap < 0) {
                    // Trail disabled.
                    trajectory.orbitTrail = false;
                    trajectory.setTrailMap(trailMap);
                } else {
                    trailMap = MathUtilsDouble.clamp(trailMap, 0.0, 1.0);
                    trajectory.orbitTrail = true;
                    trajectory.setTrailMap(trailMap);
                }
            }

            var graph = Mapper.graph.get(entity);
            graph.setParent(Scene.ROOT_NAME);

            scene.initializeEntity(entity);
            scene.setUpEntity(entity);

            em.post(Event.SCENE_ADD_OBJECT_CMD, this, entity, true);

            return entity;
        }
        return null;
    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth) {
        addPolyline(name, points, color, (float) lineWidth);
    }

    public void addPolyline(String name, double[] points, double[] color, int lineWidth, int primitive) {
        addPolyline(name, points, color, (float) lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth) {
        addPolyline(name, dArray(points), dArray(color), lineWidth);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, boolean arrowCaps) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, int primitive) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, float lineWidth, int primitive, boolean arrowCaps) {
        addPolyline(name, dArray(points), dArray(color), lineWidth, primitive, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth) {
        addPolyline(name, points, color, (float) lineWidth);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, boolean arrowCaps) {
        addPolyline(name, points, color, (float) lineWidth, arrowCaps);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, int primitive) {
        addPolyline(name, points, color, (float) lineWidth, primitive);
    }

    public void addPolyline(String name, List<?> points, List<?> color, int lineWidth, int primitive, boolean arrowCaps) {
        addPolyline(name, points, color, (float) lineWidth, primitive, arrowCaps);
    }

    @Override
    public void removeModelObject(String name) {
        if (checkString(name, "name")) {
            em.post(Event.SCENE_REMOVE_OBJECT_CMD, this, name, true);
        }
    }

    @Override
    public void postRunnable(Runnable runnable) {
        GaiaSky.postRunnable(runnable);
    }

    @Override
    public void parkRunnable(String id, Runnable runnable) {
        parkSceneRunnable(id, runnable);
    }

    @Override
    public void parkSceneRunnable(String id, Runnable runnable) {
        if (checkString(id, "id")) {
            em.post(Event.PARK_RUNNABLE, this, id, runnable);
        }
    }

    @Override
    public void parkCameraRunnable(String id, Runnable runnable) {
        if (checkString(id, "id")) {
            em.post(Event.PARK_CAMERA_RUNNABLE, this, id, runnable);
        }
    }

    @Override
    public void removeRunnable(String id) {
        if (checkString(id, "id")) em.post(Event.UNPARK_RUNNABLE, this, id);
    }

    @Override
    public void unparkRunnable(String id) {
        removeRunnable(id);
    }

    @Override
    public void setCameraState(double[] pos, double[] dir, double[] up) {
        postRunnable(() -> {
            em.post(Event.CAMERA_POS_CMD, this, (Object) pos);
            em.post(Event.CAMERA_DIR_CMD, this, (Object) dir);
            em.post(Event.CAMERA_UP_CMD, this, (Object) up);
        });
    }

    public void setCameraState(List<?> pos, List<?> dir, List<?> up) {
        setCameraState(dArray(pos), dArray(dir), dArray(up));
    }

    @Override
    public void setCameraStateAndTime(double[] pos, double[] dir, double[] up, long time) {
        postRunnable(() -> {
            em.post(Event.CAMERA_PROJECTION_CMD, this, pos, dir, up);
            em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(time));
        });
    }

    public void setCameraStateAndTime(List<?> pos, List<?> dir, List<?> up, long time) {
        setCameraStateAndTime(dArray(pos), dArray(dir), dArray(up), time);
    }

    @Override
    public void resetImageSequenceNumber() {
        ImageRenderer.resetSequenceNumber();
    }

    @Override
    public boolean loadDataset(String dsName, String absolutePath) {
        return loadDataset(dsName, absolutePath, CatalogInfoSource.SCRIPT, true);
    }

    @Override
    public boolean loadDataset(String dsName, String path, boolean sync) {
        return loadDataset(dsName, path, CatalogInfoSource.SCRIPT, sync);
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoSource type, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, false));
            t.start();
            return true;
        }
    }

    public boolean loadDataset(String dsName, String path, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, path, type, datasetOptions, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, path, type, datasetOptions, false));
            t.start();
            return true;
        }
    }

    public boolean loadDataset(String dsName, DataSource ds, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        if (sync) {
            return loadDatasetImmediate(dsName, ds, type, datasetOptions, true);
        } else {
            Thread t = new Thread(() -> loadDatasetImmediate(dsName, ds, type, datasetOptions, false));
            t.start();
            return true;
        }
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, 1, new double[]{0, 0, 0, 0}, null, null, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, new double[]{0, 0, 0, 0}, null, null, sync);
    }

    @Override
    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, double[] labelColor, boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, null, null, sync);
    }

    public boolean loadStarDataset(String dsName, String path, double magnitudeScale, final List<?> labelColor, boolean sync) {
        return loadStarDataset(dsName, path, magnitudeScale, dArray(labelColor), sync);
    }

    @Override
    public boolean loadStarDataset(String dsName,
                                   String path,
                                   double magnitudeScale,
                                   double[] labelColor,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync) {
        return loadStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadStarDataset(String dsName,
                                   String path,
                                   double magnitudeScale,
                                   final List<?> labelColor,
                                   final List<?> fadeIn,
                                   final List<?> fadeOut,
                                   boolean sync) {
        return loadStarDataset(dsName, path, magnitudeScale, dArray(labelColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    public boolean loadStarDataset(String dsName,
                                   String path,
                                   CatalogInfoSource type,
                                   double magnitudeScale,
                                   double[] labelColor,
                                   double[] fadeIn,
                                   double[] fadeOut,
                                   boolean sync) {
        DatasetOptions dops = DatasetOptions.getStarDatasetOptions(dsName, magnitudeScale, labelColor, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
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
        return loadParticleDataset(dsName,
                                   path,
                                   profileDecay,
                                   particleColor,
                                   colorNoise,
                                   labelColor,
                                   particleSize,
                                   new double[]{1.5d, 100d},
                                   ct,
                                   null,
                                   null,
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
        return loadParticleDataset(dsName,
                                   path,
                                   profileDecay,
                                   dArray(particleColor),
                                   colorNoise,
                                   dArray(labelColor),
                                   particleSize,
                                   ct,
                                   null,
                                   null,
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
        return loadParticleDataset(dsName,
                                   path,
                                   profileDecay,
                                   particleColor,
                                   colorNoise,
                                   labelColor,
                                   particleSize,
                                   new double[]{Math.tan(Math.toRadians(0.1)), FastMath.tan(Math.toRadians(6.0))},
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
        return loadParticleDataset(dsName,
                                   path,
                                   profileDecay,
                                   dArray(particleColor),
                                   colorNoise,
                                   dArray(labelColor),
                                   particleSize,
                                   ct,
                                   dArray(fadeIn),
                                   dArray(fadeOut),
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
        ComponentType compType = ComponentType.valueOf(ct);
        return loadParticleDataset(dsName,
                                   path,
                                   profileDecay,
                                   particleColor,
                                   colorNoise,
                                   labelColor,
                                   particleSize,
                                   sizeLimits,
                                   compType,
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
        return loadParticleDataset(dsName,
                                   path,
                                   profileDecay,
                                   dArray(particleColor),
                                   colorNoise,
                                   dArray(labelColor),
                                   particleSize,
                                   dArray(sizeLimits),
                                   ct,
                                   dArray(fadeIn),
                                   dArray(fadeOut),
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
        return loadParticleDataset(dsName,
                                   path,
                                   CatalogInfoSource.SCRIPT,
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
        DatasetOptions dops = DatasetOptions.getParticleDatasetOptions(dsName,
                                                                       profileDecay,
                                                                       particleColor,
                                                                       colorNoise,
                                                                       labelColor,
                                                                       particleSize,
                                                                       sizeLimits,
                                                                       ct,
                                                                       fadeIn,
                                                                       fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName, String path, double[] particleColor, double[] fadeIn, double[] fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, particleColor, ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName, String path, List<?> particleColor, List<?> fadeIn, List<?> fadeOut, boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          double[] particleColor,
                                          double[] labelColor,
                                          double[] fadeIn,
                                          double[] fadeOut,
                                          boolean sync) {
        return loadStarClusterDataset(dsName, path, particleColor, labelColor, ComponentType.Clusters.toString(), fadeIn, fadeOut, sync);
    }

    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          List<?> particleColor,
                                          List<?> labelColor,
                                          List<?> fadeIn,
                                          List<?> fadeOut,
                                          boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(labelColor), dArray(fadeIn), dArray(fadeOut), sync);
    }

    @Override
    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          double[] particleColor,
                                          String ct,
                                          double[] fadeIn,
                                          double[] fadeOut,
                                          boolean sync) {
        ComponentType compType = ComponentType.valueOf(ct);
        DatasetOptions dops = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, particleColor.clone(), compType, fadeIn, fadeOut);
        return loadDataset(dsName, path, CatalogInfoSource.SCRIPT, dops, sync);
    }

    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          List<?> particleColor,
                                          String ct,
                                          List<?> fadeIn,
                                          List<?> fadeOut,
                                          boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), ct, dArray(fadeIn), dArray(fadeOut), sync);
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
        ComponentType compType = ComponentType.valueOf(ct);
        DatasetOptions datasetOptions = DatasetOptions.getStarClusterDatasetOptions(dsName, particleColor, labelColor, compType, fadeIn, fadeOut);
        return loadDataset(dsName, path, CatalogInfoSource.SCRIPT, datasetOptions, sync);
    }

    @Override
    public boolean loadVariableStarDataset(String dsName,
                                           String path,
                                           double magnitudeScale,
                                           double[] labelColor,
                                           double[] fadeIn,
                                           double[] fadeOut,
                                           boolean sync) {
        return loadVariableStarDataset(dsName, path, CatalogInfoSource.SCRIPT, magnitudeScale, labelColor, fadeIn, fadeOut, sync);
    }

    public boolean loadVariableStarDataset(String dsName,
                                           String path,
                                           CatalogInfoSource type,
                                           double magnitudeScale,
                                           double[] labelColor,
                                           double[] fadeIn,
                                           double[] fadeOut,
                                           boolean sync) {
        DatasetOptions dops = DatasetOptions.getVariableStarDatasetOptions(dsName, magnitudeScale, labelColor, ComponentType.Stars, fadeIn, fadeOut);
        return loadDataset(dsName, path, type, dops, sync);
    }

    public boolean loadStarClusterDataset(String dsName,
                                          String path,
                                          List<?> particleColor,
                                          List<?> labelColor,
                                          String ct,
                                          List<?> fadeIn,
                                          List<?> fadeOut,
                                          boolean sync) {
        return loadStarClusterDataset(dsName, path, dArray(particleColor), dArray(labelColor), ct, dArray(fadeIn), dArray(fadeOut), sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfoSource type, boolean sync) {
        return loadDatasetImmediate(dsName, path, type, null, sync);
    }

    private boolean loadDatasetImmediate(String dsName, String path, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        Path p = Paths.get(path);
        if (Files.exists(p) && Files.isReadable(p)) {
            try {
                return loadDatasetImmediate(dsName, new FileDataSource(p.toFile()), type, datasetOptions, sync);
            } catch (Exception e) {
                logger.error("Error loading file: " + p, e);
            }
        } else {
            logger.error("Can't read file: " + path);
        }
        return false;
    }

    private List<IParticleRecord> loadParticleBeans(DataSource ds, DatasetOptions datasetOptions, STILDataProvider provider) {
        provider.setDatasetOptions(datasetOptions);
        String catalogName = datasetOptions != null && datasetOptions.catalogName != null ? datasetOptions.catalogName : ds.getName();
        return provider.loadData(ds, 1.0f, () -> {
            // Create
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, 0.01f);
        }, (current, count) -> {
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, (float) current / (float) count);
            if (current % 250000 == 0) {
                logger.info(current + " objects loaded...");
            }
        }, () -> {
            // Force remove
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, catalogName, 2f);
        });
    }

    public boolean loadJsonCatalog(String dsName, String path) {
        return loadJsonDataset(dsName, path, true);
    }

    @Override
    public boolean loadJsonDataset(String dsName, String path) {
        return loadJsonDataset(dsName, path, true);
    }

    public boolean loadJsonDataset(String dsName, String pathString, boolean sync) {
        return loadJsonDataset(dsName, pathString, false, sync);
    }

    public boolean loadJsonDataset(String dsName, String pathString, boolean select, boolean sync) {
        // Load internal JSON dataset file.
        try {
            logger.info(I18n.msg("notif.catalog.loading", pathString));
            final Array<Entity> objects = SceneJsonLoader.loadJsonFile(Gdx.files.absolute(pathString), scene);
            int i = 0;
            for (Entity e : objects) {
                if (e == null) {
                    logger.error("Entity is null: " + i);
                }
                i++;
            }
            logger.info(I18n.msg("notif.catalog.loaded", objects.size, I18n.msg("gui.objects")));
            if (objects.size > 0) {
                GaiaSky.postRunnable(() -> {
                    objects.forEach(scene.engine::addEntity);
                    objects.forEach(scene::initializeEntity);
                    objects.forEach(scene::addToIndex);

                    // Wait for entity in new task.
                    GaiaSky.instance.getExecutorService().execute(() -> {
                        while (!GaiaSky.instance.assetManager.isFinished()) {
                            // Active wait
                            sleepFrames(1);
                        }

                        // Finish initialization and touch scene graph.
                        GaiaSky.postRunnable(() -> {
                            objects.forEach((entity) -> EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, entity, false));
                            objects.forEach(scene::setUpEntity);
                            GaiaSky.instance.touchSceneGraph();

                            if (select) {
                                focusView.setEntity(objects.get(0));
                                setCameraFocus(focusView.getName());
                            }
                        });
                    });
                });
            }

        } catch (Exception e) {
            notifyErrorPopup(I18n.msg("error.loading.format", pathString), e);
            return false;
        }
        return true;
    }

    private void notifyErrorPopup(String message) {
        notifyErrorPopup(message, null);
    }

    private void notifyErrorPopup(String message, Exception e) {
        if (e != null) {
            logger.error(message, e);
        } else {
            logger.error(message);
        }
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, message);
    }

    private boolean loadDatasetImmediate(String dsName, DataSource ds, CatalogInfoSource type, DatasetOptions datasetOptions, boolean sync) {
        try {
            logger.info(I18n.msg("notif.catalog.loading", dsName));

            // Create star/particle group or star clusters
            if (checkString(dsName, "datasetName") && !checkDatasetName(dsName)) {
                // Only local files checked.
                Path path = null;
                if (ds instanceof FileDataSource) {
                    var file = ((FileDataSource) ds).getFile();
                    path = file.toPath();
                    String pathString = file.getAbsolutePath();
                    if (!Files.exists(file.toPath())) {
                        notifyErrorPopup(I18n.msg("error.loading.notexistent", pathString));
                        return false;
                    }
                    if (!Files.isReadable(path)) {
                        notifyErrorPopup(I18n.msg("error.file.read", pathString));
                        return false;
                    }
                    if (Files.isDirectory(path)) {
                        notifyErrorPopup(I18n.msg("error.file.isdir", pathString));
                        return false;
                    }
                }

                if (path != null && path.getFileName().toString().endsWith(".json")) {
                    // Only local files allowed for JSON.
                    loadJsonDataset(dsName, path.toString(), sync);
                } else if (datasetOptions == null || datasetOptions.type == DatasetLoadType.STARS || datasetOptions.type == DatasetLoadType.VARIABLES) {
                    var provider = new STILDataProvider();
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions, provider);
                    if (data != null && !data.isEmpty()) {
                        // STAR GROUP
                        AtomicReference<Entity> starGroup = new AtomicReference<>();
                        postRunnable(() -> {
                            if (datasetOptions != null) datasetOptions.initializeCatalogInfo = false;
                            starGroup.set(SetUtils.createStarSet(scene,
                                                                 dsName,
                                                                 ds.getName(),
                                                                 data,
                                                                 provider.getColumnInfoList(),
                                                                 datasetOptions,
                                                                 false));

                            // Catalog info.
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), null, type, 1.5f, starGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, starGroup.get(), true);
                            // Add to catalog manager -> setUp.
                            scene.setUpEntity(starGroup.get());

                            String typeStr = datasetOptions == null || datasetOptions.type == DatasetLoadType.STARS ? I18n.msg("gui.dsload.stars.name") : I18n.msg(
                                    "gui.dsload.variablestars.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (starGroup.get() == null || Mapper.graph.get(starGroup.get()).parent != null)) {
                            sleepFrames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetLoadType.PARTICLES) {
                    // PARTICLE GROUP
                    var provider = new STILDataProvider();
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions, provider);
                    if (data != null && !data.isEmpty()) {
                        AtomicReference<Entity> particleGroup = new AtomicReference<>();
                        postRunnable(() -> {
                            datasetOptions.initializeCatalogInfo = false;
                            particleGroup.set(SetUtils.createParticleSet(scene,
                                                                         dsName,
                                                                         ds.getName(),
                                                                         data,
                                                                         provider.getColumnInfoList(),
                                                                         datasetOptions,
                                                                         false));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), ds.getURL().toString(), type, 1.5f, particleGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, ci.entity, true);
                            // Add to catalog manager -> setUp
                            scene.setUpEntity(particleGroup.get());

                            String typeStr = I18n.msg("gui.dsload.objects.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (particleGroup.get() == null || Mapper.graph.get(particleGroup.get()).parent != null)) {
                            sleepFrames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetLoadType.PARTICLES_EXT) {
                    // PARTICLE GROUP EXTENDED
                    var provider = new STILDataProvider();
                    List<IParticleRecord> data = loadParticleBeans(ds, datasetOptions, provider);
                    if (data != null && !data.isEmpty()) {
                        AtomicReference<Entity> particleGroup = new AtomicReference<>();
                        postRunnable(() -> {
                            datasetOptions.initializeCatalogInfo = false;
                            particleGroup.set(SetUtils.createParticleSet(scene,
                                                                         dsName,
                                                                         ds.getName(),
                                                                         data,
                                                                         provider.getColumnInfoList(),
                                                                         datasetOptions,
                                                                         false));

                            // Catalog info
                            CatalogInfo ci = new CatalogInfo(dsName, ds.getName(), ds.getURL().toString(), type, 1.5f, particleGroup.get());
                            // Add to scene.
                            EventManager.publish(Event.SCENE_ADD_OBJECT_CMD, this, ci.entity, true);
                            // Add to catalog manager -> setUp
                            scene.setUpEntity(particleGroup.get());

                            String typeStr = I18n.msg("gui.dsload.objects.name");
                            logger.info(I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", data.size(), typeStr));
                        });
                        // Sync waiting until the node is in the scene graph
                        while (sync && (particleGroup.get() == null || Mapper.graph.get(particleGroup.get()).parent != null)) {
                            sleepFrames(1);
                        }
                    }
                } else if (datasetOptions.type == DatasetLoadType.CLUSTERS) {
                    // STAR CLUSTERS
                    var archetype = scene.archetypes().get("GenericCatalog");
                    var entity = archetype.createEntity();

                    var base = Mapper.base.get(entity);
                    base.setName(dsName);
                    base.setCt(datasetOptions.ct.toString());

                    var body = Mapper.body.get(entity);
                    body.setColor(datasetOptions.particleColor);
                    body.setLabelColor(datasetOptions.labelColor);
                    body.setPosition(new double[]{0, 0, 0});

                    var fade = Mapper.fade.get(entity);
                    fade.setFadeIn(datasetOptions.fadeIn);
                    fade.setFadeOut(datasetOptions.fadeOut);

                    var graph = Mapper.graph.get(entity);
                    graph.setParent(Scene.ROOT_NAME);
                    AtomicInteger numLoaded = new AtomicInteger(-5);

                    postRunnable(() -> {
                        // Load data
                        StarClusterLoader scl = new StarClusterLoader();
                        scl.initialize(ds, scene);
                        scl.setParentName(dsName);
                        scl.loadData();
                        Array<Entity> clusters = scl.getClusters();
                        numLoaded.set(clusters.size);

                        if (!clusters.isEmpty()) {
                            // Initialize.
                            scene.initializeEntity(entity);
                            for (Entity cluster : clusters) {
                                scene.initializeEntity(cluster);
                                var cBody = Mapper.body.get(cluster);
                                cBody.setColor(datasetOptions.particleColor);
                                cBody.setLabelColor(datasetOptions.labelColor);
                            }

                            // Insert
                            scene.insert(entity, true);
                            for (Entity cluster : clusters) {
                                scene.insert(cluster, true);
                            }

                            // Finalize
                            scene.setUpEntity(entity);
                            for (Entity cluster : clusters) {
                                scene.setUpEntity(cluster);
                            }

                            String typeStr = I18n.msg("gui.dsload.clusters.name");
                            logger.info(I18n.msg("notif.catalog.loaded", graph.children.size, typeStr));
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                                 this,
                                                 dsName + ": " + I18n.msg("notif.catalog.loaded", graph.children.size, typeStr));
                        }
                    });
                    // Sync waiting until the node is in the scene graph
                    while (sync && graph.parent == null) {
                        int loaded = numLoaded.get();
                        if (loaded == 0) {
                            // Stop waiting, no objects loaded.
                            break;
                        }
                        sleepFrames(1);
                    }
                }
                // One extra flush frame
                sleepFrames(1);
                return true;
            } else {
                logger.error(dsName + ": invalid or already existing dataset name");
                return false;
            }
        } catch (Exception e) {
            logger.error(e);
            return false;
        }

    }

    @Override
    public boolean hasDataset(String dsName) {
        return checkString(dsName, "datasetName") && checkDatasetName(dsName);
    }

    @Override
    public boolean setDatasetTransformationMatrix(String dsName, double[] matrix) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName) && checkNotNull(matrix, "matrix")) {
            var ci = catalogManager.get(dsName);
            if (ci != null && ci.entity != null) {
                var affine = Mapper.affine.get(ci.entity);
                if (affine != null) {
                    affine.clear();
                    affine.setMatrix(matrix);
                }
                return true;
            }
        }
        return false;
    }

    public boolean clearDatasetTransformationMatrix(String dsName) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            var ci = catalogManager.get(dsName);
            if (ci != null && ci.entity != null) {
                var affine = Mapper.affine.get(ci.entity);
                if (affine != null) {
                    affine.clear();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeDataset(String dsName) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            postRunnable(() -> EventManager.publish(Event.CATALOG_REMOVE, this, dsName));
            return true;
        }
        return false;
    }

    @Override
    public boolean hideDataset(String dsName) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            postRunnable(() -> EventManager.publish(Event.CATALOG_VISIBLE, this, dsName, false));
            return true;
        }
        return false;
    }

    @Override
    public boolean showDataset(String dsName) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            postRunnable(() -> EventManager.publish(Event.CATALOG_VISIBLE, this, dsName, true));
            return true;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, boolean highlight) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            CatalogInfo ci = this.catalogManager.get(dsName);
            postRunnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            return true;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, int colorIndex, boolean highlight) {
        float[] color = ColorUtils.getColorFromIndex(colorIndex);
        return highlightDataset(dsName, color[0], color[1], color[2], color[3], highlight);
    }

    @Override
    public boolean highlightDataset(String dsName, float r, float g, float b, float a, boolean highlight) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            CatalogInfo ci = this.catalogManager.get(dsName);
            ci.plainColor = true;
            ci.hlColor[0] = r;
            ci.hlColor[1] = g;
            ci.hlColor[2] = b;
            ci.hlColor[3] = a;
            postRunnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            return true;
        }
        return false;
    }

    @Override
    public boolean highlightDataset(String dsName, String attributeName, String colorMap, double minMap, double maxMap, boolean highlight) {
        if (checkString(dsName, "datasetName") && checkDatasetName(dsName)) {
            CatalogInfo ci = this.catalogManager.get(dsName);
            IAttribute attribute = getAttributeByName(attributeName, ci);
            int cmapIndex = getCmapIndexByName(colorMap);
            if (attribute != null && cmapIndex >= 0) {
                ci.plainColor = false;
                ci.hlCmapIndex = cmapIndex;
                ci.hlCmapMin = minMap;
                ci.hlCmapMax = maxMap;
                ci.hlCmapAttribute = attribute;
                postRunnable(() -> EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight));
            } else {
                if (attribute == null) logger.error("Could not find attribute with name '" + attributeName + "'");
                if (cmapIndex < 0) logger.error("Could not find color map with name '" + colorMap + "'");
            }
            return true;
        }
        return false;
    }

    private int getCmapIndexByName(String name) {
        for (Pair<String, Integer> cmap : ColormapPicker.cmapList) {
            if (name.equalsIgnoreCase(cmap.getFirst())) return cmap.getSecond();
        }
        return -1;
    }

    private IAttribute getAttributeByName(String name, CatalogInfo ci) {
        try {
            // One of the default attributes
            Class<?> clazz = Class.forName("gaiasky.util.filter.attrib.Attribute" + name);
            Constructor<?> constructor = clazz.getConstructor();
            return (IAttribute) constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Try extra attributes

            // New
            {
                if (ci.entity != null) {
                    var entity = ci.entity;
                    synchronized (focusView) {
                        focusView.setEntity(entity);
                        if (focusView.isSet()) {
                            ObjectMap.Keys<UCD> ucds = focusView.getSet().data().getFirst().extraKeys();
                            for (UCD ucd : ucds)
                                if (ucd.colName.equalsIgnoreCase(name)) return new AttributeUCD(ucd);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean setDatasetHighlightSizeFactor(String dsName, float sizeFactor) {
        if (checkString(dsName, "datasetName") && checkNum(sizeFactor,
                                                           Constants.MIN_DATASET_SIZE_FACTOR,
                                                           Constants.MAX_DATASET_SIZE_FACTOR,
                                                           "sizeFactor")) {

            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                ci.setHlSizeFactor(sizeFactor);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public boolean setDatasetHighlightAllVisible(String dsName, boolean allVisible) {
        if (checkString(dsName, "datasetName")) {

            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                CatalogInfo ci = this.catalogManager.get(dsName);
                ci.setHlAllVisible(allVisible);
            } else {
                logger.warn("Dataset with name " + dsName + " does not exist");
            }
            return exists;
        }
        return false;
    }

    @Override
    public void setDatasetPointSizeMultiplier(String dsName, double multiplier) {
        if (checkString(dsName, "datasetName")) {
            boolean exists = this.catalogManager.contains(dsName);
            if (exists) {
                em.post(Event.CATALOG_POINT_SIZE_SCALING_CMD, this, dsName, multiplier);
            } else {
                logger.warn("Catalog does not exist: " + dsName);
            }
        }
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
        addShapeAroundObject(shapeName,
                             shapeType,
                             primitive,
                             ShapeOrientation.EQUATORIAL.toString(),
                             size,
                             objectName,
                             r,
                             g,
                             b,
                             a,
                             showLabel,
                             trackObject);
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
        if (checkString(shapeName, "shapeName") && checkStringEnum(shapeType, Shape.class, "shape") && checkStringEnum(primitive,
                                                                                                                       Primitive.class,
                                                                                                                       "primitive") && checkStringEnum(
                orientation,
                ShapeOrientation.class,
                "orientation") && checkNum(size, 0, Double.MAX_VALUE, "size") && checkObjectName(objectName)) {
            final var shapeLc = shapeType.toLowerCase(Locale.ROOT);
            postRunnable(() -> {
                Entity trackingObject = getFocusEntity(objectName);
                float[] color = new float[]{r, g, b, a};
                int primitiveInt = Primitive.valueOf(primitive.toUpperCase(Locale.ROOT)).equals(Primitive.LINES) ? GL20.GL_LINES : GL20.GL_TRIANGLES;
                // Create shape
                Archetype at = scene.archetypes().get("ShapeObject");
                Entity newShape = at.createEntity();

                var base = Mapper.base.get(newShape);
                base.setName(shapeName.trim());
                base.ct = new ComponentTypes(ComponentType.Others.ordinal());

                var body = Mapper.body.get(newShape);
                body.setColor(color);
                body.setLabelColor(new float[]{r, g, b, a});
                body.size = (float) (size * Constants.KM_TO_U);

                var graph = Mapper.graph.get(newShape);
                graph.setParent(Scene.ROOT_NAME);

                var focus = Mapper.focus.get(newShape);
                focus.focusable = false;

                var shape = Mapper.shape.get(newShape);
                if (trackObject) {
                    shape.track = new FocusView(trackingObject);
                } else {
                    body.setPos(EntityUtils.getAbsolutePosition(trackingObject, objectName, new Vector3Q()));
                }
                shape.trackName = objectName;

                var trf = Mapper.transform.get(newShape);
                var m = new Matrix4();
                var orient = ShapeOrientation.valueOf(orientation.toUpperCase(Locale.ROOT));
                switch (orient) {
                    case CAMERA -> {
                        var camera = GaiaSky.instance.cameraManager.getCamera();
                        m.rotateTowardDirection(camera.direction, camera.up);
                    }
                    case EQUATORIAL -> m.idt();
                    case ECLIPTIC -> m.set(Coordinates.eclipticToEquatorialF());
                    case GALACTIC -> m.set(Coordinates.galacticToEquatorialF());
                }
                trf.setTransformMatrix(m);

                Map<String, Object> params = new HashMap<>();
                params.put("quality", 25L);
                params.put("divisions", shapeLc.equalsIgnoreCase(Shape.OCTAHEDRONSPHERE.toString()) ? 3L : 35L);
                params.put("recursion", 3L);
                params.put("diameter", 1.0);
                params.put("width", 1.0);
                params.put("height", 1.0);
                params.put("depth", 1.0);
                params.put("innerradius", 0.6);
                params.put("outerradius", 1.0);
                params.put("sphere-in-ring", false);
                params.put("flip", false);

                var model = Mapper.model.get(newShape);
                model.model = new ModelComponent();
                model.model.type = shapeLc;
                model.model.setPrimitiveType(primitiveInt);
                model.model.setParams(params);
                model.model.setStaticLight(true);
                model.model.setUseColor(true);
                model.model.setBlendMode(BlendMode.ADDITIVE);
                model.model.setCulling(false);

                var rt = Mapper.renderType.get(newShape);
                rt.renderGroup = RenderGroup.MODEL_VERT_ADDITIVE;

                // Initialize shape.
                scene.initializeEntity(newShape);
                scene.setUpEntity(newShape);

                // Add to scene.
                EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, newShape, false);
            });
        }
    }

    @Override
    public void backupSettings() {
        settingsStack.push(Settings.settings.clone());
    }

    @Override
    public boolean restoreSettings() {
        if (settingsStack.isEmpty()) {
            return false;
        }

        if (SettingsManager.setSettingsInstance(settingsStack.pop())) {
            // Apply settings.
            Settings.settings.apply();
            // Reload UI.
            // postRunnable(()->{
            // em.post(Event.UI_RELOAD_CMD, this, GaiaSky.instance.getGlobalResources());
            // });
            return true;
        }
        return false;
    }

    @Override
    public void clearSettingsStack() {
        settingsStack.clear();
    }

    @Override
    public void resetUserInterface() {
        EventManager.publish(Event.UI_RELOAD_CMD, this, GaiaSky.instance.getGlobalResources());
    }

    @Override
    public void setMaximumSimulationTime(long years) {
        if (checkFinite(years, "years")) {
            Settings.settings.runtime.setMaxTime(Math.abs(years));
        }
    }

    @Override
    public double[] getRefSysTransform(String name) {
        var mat = Coordinates.getTransformD(name);
        if (mat != null) {
            return mat.val;
        } else {
            logger.error(name + ": no transformation found with the given name");
            return null;
        }
    }

    public void setMaximumSimulationTime(double years) {
        setMaximumSimulationTime((long) years);
    }

    public void setMaximumSimulationTime(Long years) {
        setMaximumSimulationTime(years.longValue());
    }

    public void setMaximumSimulationTime(Double years) {
        setMaximumSimulationTime(years.longValue());
    }

    public void setMaximumSimulationTime(Integer years) {
        setMaximumSimulationTime(years.longValue());
    }

    @Override
    public double getMeterToInternalUnitConversion() {
        return Constants.M_TO_U;
    }

    @Override
    public double getInternalUnitToMeterConversion() {
        return Constants.U_TO_M;
    }

    @Override
    public double internalUnitsToMetres(double internalUnits) {
        return internalUnits * Constants.U_TO_M;
    }

    @Override
    public double internalUnitsToKilometres(double internalUnits) {
        return internalUnits * Constants.U_TO_KM;
    }

    @Override
    public double[] internalUnitsToKilometres(double[] internalUnits) {
        double[] result = new double[internalUnits.length];
        for (int i = 0; i < internalUnits.length; i++) {
            result[i] = internalUnitsToKilometres(internalUnits[i]);
        }
        return result;
    }

    @Override
    public double internalUnitsToParsecs(double internalUnits) {
        return internalUnits * Constants.U_TO_PC;
    }

    @Override
    public double[] internalUnitsToParsecs(double[] internalUnits) {
        double[] result = new double[internalUnits.length];
        for (int i = 0; i < internalUnits.length; i++) {
            result[i] = internalUnitsToParsecs(internalUnits[i]);
        }
        return result;
    }

    public double[] internalUnitsToKilometres(List<?> internalUnits) {
        double[] result = new double[internalUnits.size()];
        for (int i = 0; i < internalUnits.size(); i++) {
            result[i] = internalUnitsToKilometres((double) internalUnits.get(i));
        }
        return result;
    }

    @Override
    public double metresToInternalUnits(double metres) {
        return metres * Constants.M_TO_U;
    }

    @Override
    public double kilometresToInternalUnits(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    @Override
    public double parsecsToInternalUnits(double parsecs) {
        return parsecs * Constants.PC_TO_U;
    }

    public double kilometersToInternalUnits(double kilometres) {
        return kilometres * Constants.KM_TO_U;
    }

    @Override
    public List<String> listDatasets() {
        Set<String> names = this.catalogManager.getDatasetNames();
        if (names != null) return new ArrayList<>(names);
        else return new ArrayList<>();
    }

    @Override
    public long getFrameNumber() {
        return GaiaSky.instance.frames;
    }

    @Override
    public String getDefaultFramesDir() {
        return SysUtils.getDefaultFramesDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultScreenshotsDir() {
        return SysUtils.getDefaultScreenshotsDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultCameraDir() {
        return SysUtils.getDefaultCameraDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultMusicDir() {
        return SysUtils.getDefaultMusicDir().toAbsolutePath().toString();
    }

    @Override
    public String getDefaultMappingsDir() {
        return SysUtils.getDefaultMappingsDir().toAbsolutePath().toString();
    }

    @Override
    public String getDataDir() {
        return SysUtils.getDataDir().toAbsolutePath().toString();
    }

    @Override
    public String getConfigDir() {
        return SysUtils.getConfigDir().toAbsolutePath().toString();
    }

    @Override
    public String getLocalDataDir() {
        return SysUtils.getLocalDataDir().toAbsolutePath().toString();
    }

    @Override
    public void print(String message) {
        logger.info(message);
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void quit() {
        Gdx.app.exit();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case INPUT_EVENT -> inputCode = (Integer) data[0];
            case DISPOSE -> {
                // Stop all
                for (AtomicBoolean stop : stops) {
                    if (stop != null) stop.set(true);
                }
            }
            case SCENE_LOADED -> {
                this.scene = (Scene) data[0];
                this.focusView.setScene(this.scene);
            }
            default -> {
            }
        }

    }

    private boolean checkNum(int value, int min, int max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkNum(long value, long min, long max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkNum(float value, float min, float max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkNum(double value, double min, double max, String name) {
        if (value < min || value > max) {
            logger.error(name + " must be between " + min + " and " + max + ": " + value);
            return false;
        }
        return true;
    }

    private boolean checkFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            logger.error(name + " must be finite: " + value);
            return false;
        }
        return true;
    }

    private boolean checkFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            logger.error(name + " must be finite: " + value);
            return false;
        }
        return true;
    }

    private boolean checkLengths(double[] array, int length1, int length2, String name) {
        if (array.length != length1 && array.length != length2) {
            logger.error(name + " must have a length of " + length1 + " or " + length2 + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    private boolean checkLength(double[] array, int length, String name) {
        if (array.length != length) {
            logger.error(name + " must have a length of " + length + ". Current length is " + array.length);
            return false;
        }
        return true;
    }

    private boolean checkString(String value, String name) {
        if (value == null || value.isEmpty()) {
            logger.error(name + " can't be null nor empty");
            return false;
        }
        return true;
    }

    private boolean checkString(String value, String[] possibleValues, String name) {
        if (checkString(value, name)) {
            for (String v : possibleValues) {
                if (value.equals(v)) return true;
            }
            logPossibleValues(value, possibleValues, name);
            return false;
        }
        logPossibleValues(value, possibleValues, name);
        return false;
    }

    private boolean checkDirectoryExists(String location, String name) {
        if (location == null) {
            logger.error(name + ": location can't be null");
            return false;
        }
        Path p = Path.of(location);
        if (Files.notExists(p)) {
            logger.error(name + ": path does not exist");
            return false;
        }
        return true;
    }

    private boolean checkObjectName(String name) {
        if (getEntity(name) == null) {
            logger.error(name + ": object with this name does not exist");
            return false;
        }
        return true;
    }

    private boolean checkObjectName(String name, double timeOutSeconds) {
        if (getEntity(name, timeOutSeconds) == null) {
            logger.error(name + ": object with this name does not exist");
            return false;
        }
        return true;
    }

    private boolean checkDatasetName(String name) {
        if (!this.catalogManager.contains(name)) {
            logger.error(name + ": no dataset found with the given name");
            return false;
        }
        return true;
    }

    private boolean checkFocusName(String name) {
        Entity entity = getFocus(name);
        if (entity == null) {
            logger.error(name + ": focus with this name does not exist");
        }
        return entity != null;
    }

    private boolean checkDateTime(int year, int month, int day, int hour, int min, int sec, int millisec) {
        boolean ok;
        ok = checkNum(month, 1, 12, "month");
        ok = ok && checkNum(day, 1, 31, "month");
        ok = ok && checkNum(hour, 0, 23, "month");
        ok = ok && checkNum(min, 0, 59, "month");
        ok = ok && checkNum(sec, 0, 59, "month");
        ok = ok && checkNum(millisec, 0, 990, "month");

        if (ok) {
            // Try to create a date.
            try {
                var date = LocalDateTime.of(year, month, day, hour, min, sec, millisec);
            } catch (DateTimeException e) {
                logger.error("Date/time error: " + e.getLocalizedMessage());
                ok = false;
            }
        }
        return ok;
    }

    private void logPossibleValues(String value, String[] possibleValues, String name) {
        logger.error(name + " value not valid: " + value + ". Possible values are:");
        for (String v : possibleValues)
            logger.error(v);
    }

    private <T extends Enum<T>> boolean checkStringEnum(String value, Class<T> clazz, String name) {
        if (checkString(value, name)) {
            for (Enum<T> en : EnumSet.allOf(clazz)) {
                if (value.equalsIgnoreCase(en.toString()) || value.equalsIgnoreCase(en.name())) {
                    return true;
                }
            }
            logger.error(name + " value not valid: " + value + ". Must be a value in the enum " + clazz.getSimpleName() + ":");
            for (Enum<T> en : EnumSet.allOf(clazz)) {
                logger.error(en.toString());
            }
        }
        return false;
    }

    private boolean checkNotNull(Object o, String name) {
        if (o == null) {
            logger.error(name + " can't be null");
            return false;
        }
        return true;
    }

    private boolean checkDistanceUnits(String units, String name) {
        try {
            DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkSmoothType(String type, String name) {
        return type.equalsIgnoreCase("logit") || type.equalsIgnoreCase("logisticsigmoid") || type.equalsIgnoreCase("none");
    }

    enum TransitionType {
        POSITION, ORIENTATION, ALL;

        public boolean isPosition() {
            return this == ALL || this == POSITION;
        }

        public boolean isOrientation() {
            return this == ALL || this == ORIENTATION;
        }

        public boolean isAll() {
            return this == ALL;
        }
    }

    class TimeTransitionRunnable implements Runnable {
        final Object lock;
        final Long currentTimeMs;
        final AtomicBoolean stop;
        Long targetTimeMs;
        Long dt;
        final double duration;
        double elapsed, start;
        Runnable end;
        /** Maps input x to output x for positions. **/
        Function<Double, Double> mapper;

        /**
         * Creates a time transition to the given time in UTC.
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
         *                        <li>"logit": starts slow and ends slow. The smooth factor must be over 12 to produce
         *                        an effect, otherwise, linear interpolation is used.</li>
         *                        <li>"logisticsigmoid": starts fast and ends fast. The smooth factor must be between
         *                        0.09 and 0.01.</li>
         *                        <li>"none": no smoothing is applied.</li>
         *                        </ul>
         * @param smoothFactor    Smoothing factor (depends on type, see #smoothType).
         * @param end             An optional runnable that is executed when the transition has completed.
         * @param stop            A reference to a boolean value as an {@link AtomicBoolean} that stops the execution of the runnable
         *                        when it changes to true.
         */
        public TimeTransitionRunnable(int year,
                                      int month,
                                      int day,
                                      int hour,
                                      int min,
                                      int sec,
                                      int milliseconds,
                                      double durationSeconds,
                                      String smoothType,
                                      double smoothFactor,
                                      Runnable end,
                                      AtomicBoolean stop) {

            lock = new Object();
            duration = durationSeconds;
            this.start = GaiaSky.instance.getT();
            this.currentTimeMs = GaiaSky.instance.time.getTime().toEpochMilli();
            this.mapper = getMapper(smoothType, smoothFactor);
            this.stop = stop;

            try {
                LocalDateTime date = LocalDateTime.of(year, month, day, hour, min, sec, milliseconds);
                var instant = date.toInstant(ZoneOffset.UTC);
                targetTimeMs = instant.toEpochMilli();
                dt = targetTimeMs - currentTimeMs;
            } catch (DateTimeException e) {
                logger.error("Could not create time transition: bad date.", e);
            }
        }

        private Function<Double, Double> getMapper(String smoothingType, double smoothingFactor) {
            Function<Double, Double> mapper;
            if (Objects.equals(smoothingType, "logisticsigmoid")) {
                final double fac = MathUtilsDouble.clamp(smoothingFactor, 12.0, 500.0);
                mapper = (x) -> MathUtilsDouble.clamp(MathUtilsDouble.logisticSigmoid(x, fac), 0.0, 1.0);
            } else if (Objects.equals(smoothingType, "logit")) {
                final double fac = MathUtilsDouble.clamp(smoothingFactor, 0.01, 0.09);
                mapper = (x) -> MathUtilsDouble.clamp(MathUtilsDouble.logit(x) * fac + 0.5, 0.0, 1.0);
            } else {
                mapper = (x) -> x;
            }
            return mapper;
        }

        @Override
        public void run() {
            // Update elapsed time.
            elapsed = GaiaSky.instance.getT() - start;

            double alpha = MathUtilsDouble.clamp(elapsed / duration, 0.0, 0.999999999999999999);
            // Linear interpolation in simulation time.
            alpha = mapper.apply(alpha);
            var t = currentTimeMs + (long) (alpha * dt);
            em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(t));

            // Finish if needed.
            if ((stop != null && stop.get()) || elapsed >= duration) {
                // On end, run runnable if present, otherwise notify lock
                if (end != null) {
                    end.run();
                } else {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }
        }
    }

    class CameraTransitionRunnable implements Runnable {
        final Object lock;
        final Vector3D v3d1, v3d2, v3d3;
        final AtomicBoolean stop;
        NaturalCamera cam;
        /** Duration of the position interpolation, in seconds. **/
        double posDuration;
        /** Duration of the orientation interpolation, in seconds. **/
        double orientationDuration;
        double elapsed, start;
        Vector3D targetDir, targetUp;
        PathDouble<Vector3D> posInterpolator;
        QuaternionDouble startOrientation, endOrientation, qd;
        Runnable end;
        /** Maps input x to output x for positions. **/
        Function<Double, Double> positionMapper;
        /** Maps input x to output x for orientations. **/
        Function<Double, Double> orientationMapper;

        /** Type of transition **/
        final TransitionType type;

        /**
         * A runnable that interpolates the camera state (position, direction, up) to the new given state
         * in the specified number of seconds. This method uses a pure linear interpolation.
         *
         * @param cam     The camera to use.
         * @param pos     The final position.
         * @param dir     The final direction.
         * @param up      The final up vector.
         * @param seconds The number of seconds to complete the transition.
         * @param end     An optional runnable that is executed when the transition has completed.
         * @param stop    A reference to a boolean value as an {@link AtomicBoolean} that stops the execution of the runnable
         *                when it changes to true.
         */
        public CameraTransitionRunnable(NaturalCamera cam,
                                        double[] pos,
                                        double[] dir,
                                        double[] up,
                                        double seconds,
                                        Runnable end,
                                        AtomicBoolean stop) {
            this(cam, pos, dir, up, seconds, "", 0, seconds, "", 0, end, stop);
        }

        /**
         * A runnable that interpolates the camera orientation to the new given orientation
         * in the specified number of seconds. This method accepts a smoothing factor and type.
         *
         * @param cam             The camera to use.
         * @param dir             The final position.
         * @param up              The final position.
         * @param durationSeconds The duration of the position interpolation, in seconds.
         * @param smoothType      Position smooth type.
         * @param smoothFactor    Position smooth factor (depends on type).
         * @param end             An optional runnable that is executed when the transition has completed.
         * @param stop            A reference to a boolean value as an {@link AtomicBoolean} that stops the execution of the runnable
         *                        when it changes to true.
         */
        public CameraTransitionRunnable(NaturalCamera cam,
                                        double[] dir,
                                        double[] up,
                                        double durationSeconds,
                                        String smoothType,
                                        double smoothFactor,
                                        Runnable end,
                                        AtomicBoolean stop) {
            this(cam, null, dir, up, -1, null, -1, durationSeconds, smoothType, smoothFactor, end, stop);
        }

        /**
         * A runnable that interpolates the camera position to the new given position
         * in the specified number of seconds. This method accepts a smoothing factor and type.
         *
         * @param cam             The camera to use.
         * @param pos             The final position.
         * @param durationSeconds The duration of the position interpolation, in seconds.
         * @param smoothType      Position smooth type.
         * @param smoothFactor    Position smooth factor (depends on type).
         * @param end             An optional runnable that is executed when the transition has completed.
         * @param stop            A reference to a boolean value as an {@link AtomicBoolean} that stops the execution of the runnable
         *                        when it changes to true.
         */
        public CameraTransitionRunnable(NaturalCamera cam,
                                        double[] pos,
                                        double durationSeconds,
                                        String smoothType,
                                        double smoothFactor,
                                        Runnable end,
                                        AtomicBoolean stop) {
            this(cam, pos, null, null, durationSeconds, smoothType, smoothFactor, -1, null, -1, end, stop);
        }

        /**
         * A runnable that interpolates the camera state (position, direction, up) to the new given state
         * in the specified number of seconds. This method accepts smoothing factors and types for the
         * position and orientation.
         *
         * @param cam                        The camera to use.
         * @param pos                        The final position.
         * @param dir                        The final direction.
         * @param up                         The final up vector.
         * @param positionDurationSeconds    The duration of the position interpolation, in seconds.
         * @param positionSmoothType         Position smooth type.
         * @param positionSmoothFactor       Position smooth factor (depends on type).
         * @param orientationDurationSeconds The duration of the orientation interpolation, in seconds.
         * @param orientationSmoothType      Orientation smooth type.
         * @param orientationSmoothFactor    Orientation smooth factor (depends on type).
         * @param end                        An optional runnable that is executed when the transition has completed.
         * @param stop                       A reference to a boolean value as an {@link AtomicBoolean} that stops the execution of the runnable
         *                                   when it changes to true.
         */
        public CameraTransitionRunnable(NaturalCamera cam,
                                        double[] pos,
                                        double[] dir,
                                        double[] up,
                                        double positionDurationSeconds,
                                        String positionSmoothType,
                                        double positionSmoothFactor,
                                        double orientationDurationSeconds,
                                        String orientationSmoothType,
                                        double orientationSmoothFactor,
                                        Runnable end,
                                        AtomicBoolean stop) {
            if (pos == null) {
                type = TransitionType.ORIENTATION;
            } else if (dir == null || up == null) {
                type = TransitionType.POSITION;
            } else {
                type = TransitionType.ALL;
            }
            this.cam = cam;
            if (type.isPosition() && pos != null) {
                this.posDuration = positionDurationSeconds;
            }
            if (type.isOrientation() && dir != null && up != null) {
                this.targetDir = new Vector3D(dir).nor();
                this.targetUp = new Vector3D(up).nor();
                this.orientationDuration = orientationDurationSeconds;
            }
            this.start = GaiaSky.instance.getT();
            this.elapsed = 0;
            this.end = end;
            this.lock = new Object();
            this.stop = stop;

            if (type.isPosition()) {
                // Mappers.
                String posType = positionSmoothType.toLowerCase(Locale.ROOT).strip();
                positionMapper = getMapper(posType, positionSmoothFactor);
                // Set up interpolation.
                posInterpolator = getPath(cam.getPos().tov3d(aux3d3), pos);
            }

            if (type.isOrientation()) {
                String orientationType = orientationSmoothType.toLowerCase(Locale.ROOT).strip();
                orientationMapper = getMapper(orientationType, orientationSmoothFactor);

                // Start and end orientations.
                startOrientation = new QuaternionDouble();
                startOrientation.setFromCamera(cam.direction, cam.up);
                // End orientation.
                endOrientation = new QuaternionDouble();
                endOrientation.setFromCamera(targetDir, targetUp);
            }

            // Aux
            v3d1 = new Vector3D();
            v3d2 = new Vector3D();
            v3d3 = new Vector3D();
            qd = new QuaternionDouble();
        }

        private Function<Double, Double> getMapper(String smoothingType, double smoothingFactor) {
            Function<Double, Double> mapper;
            if (Objects.equals(smoothingType, "logisticsigmoid")) {
                final double fac = MathUtilsDouble.clamp(smoothingFactor, 12.0, 500.0);
                mapper = (x) -> MathUtilsDouble.clamp(MathUtilsDouble.logisticSigmoid(x, fac), 0.0, 1.0);
            } else if (Objects.equals(smoothingType, "logit")) {
                final double fac = MathUtilsDouble.clamp(smoothingFactor, 0.01, 0.09);
                mapper = (x) -> MathUtilsDouble.clamp(MathUtilsDouble.logit(x) * fac + 0.5, 0.0, 1.0);
            } else {
                mapper = (x) -> x;
            }
            return mapper;
        }

        /**
         * Gets a path from p0 to p1.
         *
         * @param p0 The initial position.
         * @param p1 The final position.
         *
         * @return The linear interpolation path.
         */
        private PathDouble<Vector3D> getPath(Vector3D p0, double[] p1) {
            Vector3D[] points = new Vector3D[]{new Vector3D(p0), new Vector3D(p1)};
            return new LinearDouble<>(points);
        }

        @Override
        public void run() {
            // Update elapsed time
            elapsed = GaiaSky.instance.getT() - start;

            if (type.isPosition()) {
                // Interpolation variable.
                double alphaPos = MathUtilsDouble.clamp(elapsed / posDuration, 0.0, 0.999999999999999999);
                // Interpolate camera position.
                cam.setPos(posInterpolator.valueAt(v3d1, positionMapper.apply(alphaPos)));
            }

            if (type.isOrientation()) {
                // Interpolation variable.
                double alphaOrientation = MathUtilsDouble.clamp(elapsed / orientationDuration, 0.0, 0.999999999999999999);

                // Interpolate camera orientation using quaternions.
                qd.set(startOrientation).slerp(endOrientation, orientationMapper.apply(alphaOrientation));
                var up = qd.getUp(v3d3);
                cam.setUp(up);
                var direction = qd.getDirection(v3d3);
                cam.setDirection(v3d3);
            }

            // Finish if needed.
            if ((stop != null && stop.get()) || (elapsed >= posDuration && elapsed >= orientationDuration)) {
                // On end, run runnable if present, otherwise notify lock
                if (end != null) {
                    end.run();
                } else {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }
        }
    }
}
