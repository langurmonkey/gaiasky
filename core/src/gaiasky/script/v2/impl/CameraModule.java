/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.view.FocusView;
import gaiasky.script.v2.api.CameraAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.SlaveManager;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.*;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * The camera module contains methods and calls that modify and query the camera system in Gaia Sky.
 */
public class CameraModule extends APIModule implements IObserver, CameraAPI {

    /** Scene reference. **/
    private Scene scene;
    /** Focus view. **/
    private final FocusView focusView;
    /** Currently active stop instances. **/
    private final Set<AtomicBoolean> stops;
    /** Internal camera transition sequence number. **/
    private int cTransSeq = 0;

    /** Axuiliary vector. **/
    private final Vector3Q aux3b1 = new Vector3Q();

    /** The interactive camera module, to manipulate the camera in interactive mode. **/
    public InteractiveCameraModule interactive;

    /**
     * Create a new module with the given attributes.
     *
     * @param em   Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public CameraModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
        this.focusView = new FocusView();
        this.stops = new HashSet<>();
        this.interactive = new InteractiveCameraModule(em, api, "interactive");

        em.subscribe(this, Event.SCENE_LOADED);
    }

    @Override
    public void focus_mode(final String focusName) {
        focus_mode(focusName, 0.0f);
    }

    @Override
    public void focus_mode(final String focusName, final float waitTimeSeconds) {
        if (api.validator.checkString(focusName, "focusName") && api.validator.checkFocusName(focusName)) {
            var entity = api.scene.get_entity(focusName);
            focus_mode(entity, waitTimeSeconds);
        }
    }

    /**
     * Set the camera in focus mode, and sets the focus object to the given {@link Entity}.
     * Additionally, <code>waitTimeSeconds</code> contains the amount of time,
     * in seconds, to wait for the camera * transition that makes it point to the new focus object to finish.
     * If the transition has not finished after this amount of time, the call returns. If the transition finishes
     * before this amount of time, it returns immediately after finishing.
     *
     * @param entity          Reference to the new focus {@link Entity}.
     * @param waitTimeSeconds Maximum time, in seconds, to wait for the camera to face the
     *                        focus. If negative, the call waits until the camera transition is finished.
     */
    private void focus_mode(final Entity entity, final float waitTimeSeconds) {
        if (api.validator.checkNotNull(entity, "Entity is null")) {
            synchronized (focusView) {
                focusView.setEntity(entity);
                if (Mapper.focus.has(entity)) {
                    NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;
                    changeFocus(focusView, cam, waitTimeSeconds);
                } else {
                    logger.error("Object can't be set as focus: " + focusView.getName());
                }
            }
        }
    }

    /**
     * Alias to {@link #focus_mode(String, float)}, but with <code>waitTimeSeconds</code> given as an <code>int</code>.
     */
    public void focus_mode(final String focusName, final int waitTimeSeconds) {
        focus_mode(focusName, (float) waitTimeSeconds);
    }

    @Override
    public void focus_mode_instant(final String focusName) {
        if (api.validator.checkString(focusName, "focusName")) {
            Entity entity = api.scene.get_entity(focusName);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(focusName);
                    em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                    em.post(Event.FOCUS_CHANGE_CMD, this, focusView.getEntity());
                }

                api.base.post_runnable(() -> {
                    // Instantly set the camera direction to look towards the focus
                    Vector3Q camPos = GaiaSky.instance.cameraManager.getPos();
                    Vector3Q dir = new Vector3Q();
                    synchronized (focusView) {
                        focusView.setEntity(entity);
                        focusView.getAbsolutePosition(dir).sub(camPos);
                    }
                    em.post(Event.CAMERA_DIR_CMD, this, (Object) dir.nor().valuesD());
                });
                // Make sure the last action is flushed
                api.base.sleep_frames(2);
            } else {
                logger.error("FOCUS_MODE object does not exist: " + focusName);
            }
        }
    }

    @Override
    public void focus_mode_instant_go(final String focusName) {
        focus_mode_instant_go(focusName, true);
    }

    /**
     * Alias to {@link #focus_mode_instant(String)}, but with an extra boolean parameter (for internal use)
     * that makes sure that the camera changes are flushed before returning.
     */
    public void focus_mode_instant_go(final String focusName, final boolean sleep) {
        if (api.validator.checkString(focusName, "focusName")) {
            Entity entity = api.scene.get_entity(focusName);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(focusName);
                    em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                    em.post(Event.FOCUS_CHANGE_CMD, this, focusView.getEntity(), true);
                    em.post(Event.GO_TO_OBJECT_CMD, this);
                }
                // Make sure the last action is flushed
                if (sleep) api.base.sleep_frames(2);
            }
        }
    }

    /**
     * Internal method to change the current camera focus object.
     * <p>
     * This method checks if the object is the current focus of the given camera. If it is not,
     * it sets it as focus and waits for the transaction to commit, if necessary.
     *
     * @param object          The new focus object.
     * @param cam             The current camera.
     * @param waitTimeSeconds Max time to wait for the camera to face the focus, in
     *                        seconds. If negative, we wait until the end.
     */
    protected void changeFocus(FocusView object, NaturalCamera cam, double waitTimeSeconds) {
        // Post focus change and wait, if needed
        FocusView currentFocus = (FocusView) cam.getFocus();
        if (currentFocus == null || currentFocus.isSet() || currentFocus.getEntity() != object.getEntity()) {
            em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
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
                    api.base.sleep_frames(1);
                } catch (Exception e) {
                    logger.error(e);
                }
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000d;
            }
        }
    }

    @Override
    public boolean wait_focus(String name, long timeoutMs) {
        long iniTime = TimeUtils.millis();
        NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;
        while (cam.focus == null || !cam.focus.getName().equalsIgnoreCase(name)) {
            api.base.sleep_frames(1);
            long spent = TimeUtils.millis() - iniTime;
            if (timeoutMs > 0 && spent > timeoutMs) {
                // Timeout!
                return true;
            }
        }
        return false;
    }


    @Override
    public void set_focus_lock(final boolean lock) {
        api.base.post_runnable(() -> em.post(Event.FOCUS_LOCK_CMD, this, lock));
    }

    @Override
    public void center_focus(boolean centerFocus) {
        api.base.post_runnable(() -> em.post(Event.CAMERA_CENTER_FOCUS_CMD, this, centerFocus));
    }


    @Override
    public void stop() {
        api.base.post_runnable(() -> em.post(Event.CAMERA_STOP, this));

    }

    @Override
    public void center() {
        api.base.post_runnable(() -> em.post(Event.CAMERA_CENTER, this));
    }

    @Override
    public void free_mode() {
        api.base.post_runnable(() -> em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE));
    }

    @Override
    public void set_position(final double[] vec) {
        this.set_position(vec, "km");
    }

    @Override
    public void set_position(double x, double y, double z) {
        this.set_position(new double[]{x, y, z});
    }

    @Override
    public void set_position(double x, double y, double z, String units) {
        set_position(new double[]{x, y, z}, units);
    }

    @Override
    public void set_position(double x, double y, double z, boolean immediate) {
        set_position(new double[]{x, y, z}, immediate);
    }

    @Override
    public void set_position(double x, double y, double z, String units, boolean immediate) {
        set_position(new double[]{x, y, z}, units, immediate);
    }

    /**
     * Alias for {@link #set_position(double[], boolean)}.
     */
    public void set_position(final List<?> vec, boolean immediate) {
        set_position(api.dArray(vec), immediate);
    }

    @Override
    public void set_position(double[] position, boolean immediate) {
        set_position(position, "km", immediate);
    }

    @Override
    public void set_position(double[] position, String units, boolean immediate) {
        if (api.validator.checkLength(position, 3, "position")
                && api.validator.checkDistanceUnits(units)) {
            Settings.DistanceUnits u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            if (immediate) {
                sendPositionEvent(position, u);
            } else {
                api.base.post_runnable(() -> sendPositionEvent(position, u));
            }
        }

    }

    /**
     * Alias for {@link #set_position(double[], String, boolean)}.
     */
    public void set_position(List<Double> position, String units, boolean immediate) {
        set_position(api.dArray(position), units, immediate);
    }

    private void sendPositionEvent(double[] position, Settings.DistanceUnits units) {
        // Convert to km
        position[0] = units.toInternalUnits(position[0]);
        position[1] = units.toInternalUnits(position[1]);
        position[2] = units.toInternalUnits(position[2]);
        // Send event
        em.post(Event.CAMERA_POS_CMD, this, (Object) position);
    }

    @Override
    public double[] get_position() {
        return get_position("km");
    }

    @Override
    public double[] get_position(String units) {
        if (api.validator.checkDistanceUnits(units)) {
            var u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            Vector3D campos = GaiaSky.instance.cameraManager.getPos().tov3d(api.aux3d1);
            return new double[]{u.fromInternalUnits(campos.x), u.fromInternalUnits(campos.y), u.fromInternalUnits(campos.z)};
        }
        return null;
    }

    @Override
    public void set_position(double[] position, String units) {
        set_position(position, units, false);
    }

    /**
     * Alias for {@link #set_position(double[])}.
     */
    public void set_position(final List<?> vec) {
        set_position(vec, "km");
    }

    public void set_position(final List<?> vec, String units) {
        set_position(api.dArray(vec), units);
    }

    /**
     * Alias for {@link #set_direction(double[], boolean)}.
     */
    public void set_direction(final List<?> dir, final boolean immediate) {
        set_direction(api.dArray(dir), immediate);
    }

    @Override
    public void set_direction(double[] direction, boolean immediate) {
        if (api.validator.checkLength(direction, 3, "direction")) {
            if (immediate) {
                sendDirectionEvent(direction);
            } else {
                api.base.post_runnable(() -> sendDirectionEvent(direction));
            }
        }
    }

    /**
     * Private method to send the camera direction event.
     *
     * @param direction Direction vector.
     */
    private void sendDirectionEvent(final double[] direction) {
        em.post(Event.CAMERA_DIR_CMD, this, (Object) direction);
    }

    @Override
    public double[] get_direction() {
        Vector3D camDir = GaiaSky.instance.cameraManager.getDirection();
        return new double[]{camDir.x, camDir.y, camDir.z};
    }

    @Override
    public void set_direction(final double[] direction) {
        set_direction(direction, false);
    }

    @Override
    public void set_direction_equatorial(double alpha, double delta) {
        if (api.validator.checkNum(delta, -90.0, 90.0, "declination")) {
            // Camera free.
            free_mode();

            // Direction.
            api.aux3d1.set(FastMath.toRadians(alpha), FastMath.toRadians(delta), Constants.PC_TO_U);
            var targetPoint = Coordinates.sphericalToCartesian(api.aux3d1, api.aux3d2);
            var dir = targetPoint.sub(GaiaSky.instance.cameraManager.getPos()).nor();

            // Up.
            api.aux3d3.set(0, FastMath.PI / 2.0, Constants.PC_TO_U);
            var targetUp = Coordinates.sphericalToCartesian(api.aux3d3, api.aux3d4).nor();
            dir.put(api.aux3d5).crs(targetUp);
            var up = api.aux3d5.crs(dir).nor();

            transition_orientation(new double[]{dir.x, dir.y, dir.z}, new double[]{up.x, up.y, up.z}, 2.5, "logisticsigmoid", 0.2, false);
        }
    }

    @Override
    public void set_direction_galactic(double l, double b) {
        if (api.validator.checkNum(b, -90.0, 90.0, "galactic latitude")) {
            var eq = Coordinates.galacticToEquatorial(FastMath.toRadians(l), FastMath.toRadians(b), new Vector2D());
            set_direction_equatorial(FastMath.toDegrees(eq.x), FastMath.toDegrees(eq.y));
        }
    }

    public void set_direction(final List<?> dir) {
        set_direction(api.dArray(dir));
    }

    public void set_up(final List<?> up, final boolean immediate) {
        set_up(api.dArray(up), immediate);
    }

    @Override
    public void set_up(final double[] up, final boolean immediate) {
        if (api.validator.checkLength(up, 3, "up")) {
            if (immediate) {
                sendUpEvent(up);
            } else {
                api.base.post_runnable(() -> sendUpEvent(up));
            }
        }
    }

    private void sendUpEvent(final double[] up) {
        em.post(Event.CAMERA_UP_CMD, this, (Object) up);
    }

    @Override
    public double[] get_up() {
        Vector3D camUp = GaiaSky.instance.cameraManager.getUp();
        return new double[]{camUp.x, camUp.y, camUp.z};
    }

    @Override
    public void set_up(final double[] up) {
        set_up(up, false);
    }

    public void set_up(final List<?> up) {
        set_up(api.dArray(up));
    }

    @Override
    public void set_state(double[] pos, double[] dir, double[] up) {
        api.base.post_runnable(() -> {
            em.post(Event.CAMERA_POS_CMD, this, (Object) pos);
            em.post(Event.CAMERA_DIR_CMD, this, (Object) dir);
            em.post(Event.CAMERA_UP_CMD, this, (Object) up);
        });
    }

    public void set_state(List<?> pos, List<?> dir, List<?> up) {
        set_state(api.dArray(pos), api.dArray(dir), api.dArray(up));
    }

    @Override
    public void set_state_and_time(double[] pos, double[] dir, double[] up, long time) {
        api.base.post_runnable(() -> {
            em.post(Event.CAMERA_PROJECTION_CMD, this, pos, dir, up);
            em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(time));
        });
    }

    public void set_state_and_time(List<?> pos, List<?> dir, List<?> up, long time) {
        set_state_and_time(api.dArray(pos), api.dArray(dir), api.dArray(up), time);
    }


    @Override
    public void set_orientation_quaternion(double[] quaternion) {
        if (api.validator.checkLength(quaternion, 4, "quaternion")) {
            QuaternionDouble q = new QuaternionDouble(quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
            var dir = api.aux3d1;
            var up = api.aux3d2;
            q.getDirection(dir);
            q.getUp(up);

            em.post(Event.CAMERA_DIR_CMD, this, (Object) dir.values());
            em.post(Event.CAMERA_UP_CMD, this, (Object) up.values());
        }
    }

    public void set_orientation_quaternion(List<?> quaternion) {
        set_orientation_quaternion(api.dArray(quaternion));
    }

    @Override
    public double[] get_orientation_quaternion() {
        var cam = GaiaSky.instance.getICamera();
        QuaternionDouble q = new QuaternionDouble();
        q.setFromCamera(cam.getDirection(), cam.getUp());
        return q.values();
    }

    @Override
    public void set_position_and_focus(String focus, String other, double rotation, double solidAngle) {
        if (api.validator.checkNum(solidAngle, 1e-50d, Double.MAX_VALUE, "solidAngle")
                && api.validator.checkNotNull(focus, "focus")
                && api.validator.checkNotNull(other, "other")) {

            if (scene.index().containsEntity(focus) && scene.index().containsEntity(other)) {
                Entity focusObj, otherObj;
                synchronized (focusView) {
                    focusObj = scene.findFocus(focus);
                    focusView.setEntity(focusObj);
                    focusView.getFocus(focus);

                    otherObj = scene.findFocus(other);
                    focusView.setEntity(otherObj);
                    focusView.getFocus(other);
                }
                set_position_and_focus(focusObj, otherObj, rotation, solidAngle);
            }
        }
    }

    public void set_position_and_focus(String focus, String other, long rotation, long solidAngle) {
        set_position_and_focus(focus, other, (double) rotation, (double) solidAngle);
    }

    public void point_at_equatorial(double ra, double dec) {
        em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE);
        em.post(Event.FREE_MODE_COORD_CMD, this, ra, dec);
    }

    public void point_at_sky_coordinate(long ra, long dec) {
        point_at_equatorial((double) ra, (double) dec);
    }

    private void set_position_and_focus(Entity focus, Entity other, double rotation, double solidAngle) {
        if (api.validator.checkNum(solidAngle, 1e-50d, Double.MAX_VALUE, "solidAngle")
                && api.validator.checkNotNull(focus, "focus")
                && api.validator.checkNotNull(other, "other")) {

            em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
            em.post(Event.FOCUS_CHANGE_CMD, this, focus);

            synchronized (focusView) {
                focusView.setEntity(focus);
                double radius = focusView.getRadius();
                double dist = radius / FastMath.tan(Math.toRadians(solidAngle / 2)) + radius;

                // Up to ecliptic North Pole.
                Vector3D up = new Vector3D(0, 1, 0).mul(Coordinates.eclToEq());

                Vector3Q focusPos = api.aux3b1;
                focusView.getAbsolutePosition(focusPos);

                focusView.setEntity(other);
                Vector3Q otherPos = api.aux3b2;
                focusView.getAbsolutePosition(otherPos);
                focusView.clearEntity();

                Vector3Q otherToFocus = api.aux3b3;
                otherToFocus.set(focusPos).sub(otherPos).nor();
                Vector3D focusToOther = api.aux3d4.set(otherToFocus);
                focusToOther.scl(-dist).rotate(up, rotation);

                // New camera position
                Vector3D newCamPos = api.aux3d5.set(focusToOther).add(focusPos).scl(Constants.U_TO_KM);

                // New camera direction
                Vector3D newCamDir = api.aux3d6.set(focusToOther);
                newCamDir.scl(-1).nor();

                // Finally, set values
                this.set_position(newCamPos.values());
                set_direction(newCamDir.values());
                set_up(up.values());
            }
        }
    }


    @Override
    public void go_to_object_instant(String name) {
        this.focus_mode_instant_go(name);
    }

    @Override
    public void go_to_object(String name, double positionDurationSeconds, double orientationDurationSeconds) {
        go_to_object(name, positionDurationSeconds, orientationDurationSeconds, true);
    }

    @Override
    public void go_to_object(String name, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds) {
        go_to_object(name, solidAngle, positionDurationSeconds, orientationDurationSeconds, true);
    }

    @Override
    public void go_to_object(String name, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        go_to_object(name, -1.0, positionDurationSeconds, orientationDurationSeconds, true);
    }

    @Override
    public void go_to_object(String name, double solidAngle, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        if (api.validator.checkString(name, "name") && api.validator.checkObjectName(name)) {
            Entity focus = scene.findFocus(name);
            go_to_object(focus, solidAngle, positionDurationSeconds, orientationDurationSeconds, sync);
        } else {
            logger.error("Could not find position of " + name);
        }
    }

    public void go_to_object(Entity object, double positionDurationSeconds, double orientationDurationSeconds, boolean sync) {
        go_to_object(object, -1.0, positionDurationSeconds, orientationDurationSeconds, sync);
    }

    public void go_to_object(Entity object,
                             double solidAngle,
                             double positionDurationSeconds,
                             double orientationDurationSeconds,
                             boolean sync) {
        go_to_object(object, solidAngle, positionDurationSeconds, orientationDurationSeconds, sync, null);
    }

    public void go_to_object(Entity object,
                             double solidAngle,
                             double positionDurationSeconds,
                             double orientationDurationSeconds,
                             boolean sync,
                             AtomicBoolean stop) {
        focusView.setEntity(object);
        // Get focus radius.
        var radius = focusView.getRadius();
        // Get object position.
        var objectPos = focusView.getAbsolutePosition(focusView.getName(), api.aux3b1);
        // Get start position.
        var camPos = api.aux3b2.set(GaiaSky.instance.cameraManager.getPos());
        var camUp = api.aux3b3.set(GaiaSky.instance.cameraManager.getUp());

        if (objectPos != null && camPos != null) {
            var o = objectPos;
            var c = camPos;
            var u = camUp;


            // Camera to object vector.
            var camObj = api.aux3b4.set(o).sub(c);
            // Direction is object - camera.
            var dir = api.aux3b5.set(camObj).nor();
            // Up vector from current camera up.
            var up = api.aux3b1.set(camUp).crs(dir).crs(dir).scl(-1).nor();

            // Length between camera and object, computed from the solid angle.
            double targetAngle = FastMath.toRadians(solidAngle);
            if (targetAngle < 0.0) {
                // Particles have different sizes to the rest.
                if (focusView.isParticleSet()) {
                    var rx0 = 1.31; // pc
                    var rx1 = 2805.0; // pc
                    var y0 = 1.0;
                    var y1 = 0.001;
                    targetAngle = FastMath.toRadians(y0 + (y1 - y0) * (focusView.getAbsolutePosition(api.aux3b1)
                            .lenDouble() * Constants.U_TO_PC - rx0) / (rx1 - rx0));
                } else {
                    targetAngle = FastMath.toRadians(20.0);
                }
            }
            var targetDistance = radius / FastMath.tan(targetAngle * 0.5);
            var len = camObj.len().subtract(new Quadruple(targetDistance));
            // Final position.
            var pos = camObj.nor().scl(len).add(c);


            transition(pos.valuesD(),
                       "internal",
                       dir.valuesD(),
                       up.valuesD(),
                       positionDurationSeconds,
                       "logisticsigmoid",
                       60.0,
                       orientationDurationSeconds,
                       "logisticsigmoid",
                       17.0,
                       sync,
                       stop);
        }
    }

    @Override
    public double get_distance_to_object(String name) {
        if (api.validator.checkObjectName(name)) {
            Entity entity = api.scene.get_entity(name);
            if (Mapper.focus.has(entity)) {
                focusView.setEntity(entity);
                focusView.getFocus(name);
                if (focusView.getSet() != null) {
                    var pos = focusView.getAbsolutePosition(name, aux3b1);
                    return pos.sub(GaiaSky.instance.getICamera().getPos()).lenDouble() * Constants.U_TO_KM;
                } else {
                    return (focusView.getDistToCamera() - focusView.getRadius()) * Constants.U_TO_KM;
                }
            }
        }

        return -1;
    }

    @Override
    public void set_max_speed(int index) {
        if (api.validator.checkNum(index, 0, 21, "index")) api.base.post_runnable(() -> em.post(Event.SPEED_LIMIT_CMD, this, index));
    }

    @Override
    public void set_tracking_object(String objectName) {
        if (objectName == null) {
            remove_tracking_object();
        } else if (api.validator.checkFocusName(objectName)) {
            synchronized (focusView) {
                Entity trackingObject = api.scene.get_focus(objectName);
                em.post(Event.CAMERA_TRACKING_OBJECT_CMD, this, trackingObject, objectName);
            }
        } else {
            remove_tracking_object();
        }
    }

    @Override
    public void remove_tracking_object() {
        em.post(Event.CAMERA_TRACKING_OBJECT_CMD, this, null, null);
    }

    @Override
    public void set_orientation_lock(boolean lock) {
        api.base.post_runnable(() -> em.post(Event.ORIENTATION_LOCK_CMD, this, lock));
    }

    @Override
    public IFocus get_closest_object() {
        return GaiaSky.instance.cameraManager.getClosestBody();
    }

    @Override
    public void set_fov(final float newFov) {
        if (!SlaveManager.projectionActive()) {
            if (api.validator.checkNum(newFov, Constants.MIN_FOV, Constants.MAX_FOV, "newFov"))
                api.base.post_runnable(() -> em.post(Event.FOV_CHANGED_CMD, this, newFov));
        }
    }

    public void set_fov(final int newFov) {
        set_fov((float) newFov);
    }

    @Override
    public void transition_km(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        transition(camPos, "km", camDir, camUp, seconds, true);
    }

    public void transition_km(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        transition_km(api.dArray(camPos), api.dArray(camDir), api.dArray(camUp), seconds);
    }

    public void transition_km(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        transition_km(camPos, camDir, camUp, (double) seconds);
    }

    @Override
    public void transition(double[] camPos, double[] camDir, double[] camUp, double seconds) {
        transition(camPos, "internal", camDir, camUp, seconds);
    }

    @Override
    public void transition(double[] camPos, String units, double[] camDir, double[] camUp, double seconds) {
        transition(camPos, units, camDir, camUp, seconds, true);
    }

    public void transition(double[] camPos, double[] camDir, double[] camUp, long seconds) {
        transition(camPos, "internal", camDir, camUp, seconds);
    }

    public void transition(double[] camPos, String units, double[] camDir, double[] camUp, long seconds) {
        transition(camPos, units, camDir, camUp, (double) seconds);
    }

    public void transition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds) {
        transition(camPos, "internal", camDir, camUp, seconds);
    }

    public void transition(List<?> camPos, String units, List<?> camDir, List<?> camUp, double seconds) {
        transition(api.dArray(camPos), units, api.dArray(camDir), api.dArray(camUp), seconds);
    }

    public void transition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds) {
        transition(camPos, "internal", camDir, camUp, seconds);
    }

    public void transition(List<?> camPos, String units, List<?> camDir, List<?> camUp, long seconds) {
        transition(api.dArray(camPos), units, api.dArray(camDir), api.dArray(camUp), seconds);
    }

    @Override
    public void transition(double[] camPos, double[] camDir, double[] camUp, double seconds, boolean sync) {
        transition(camPos, "internal", camDir, camUp, seconds, sync);
    }

    @Override
    public void transition(double[] camPos, String units, double[] camDir, double[] camUp, double seconds, boolean sync) {
        transition(camPos, units, camDir, camUp, seconds, "none", 0, seconds, "none", 0, sync);
    }

    public void transition(List<?> camPos,
                           List<?> camDir,
                           List<?> camUp,
                           double seconds,
                           String positionSmoothType,
                           double positionSmoothFactor,
                           String orientationSmoothType,
                           double orientationSmoothFactor) {
        transition(api.dArray(camPos),
                   "internal",
                   api.dArray(camDir),
                   api.dArray(camUp),
                   seconds,
                   positionSmoothType,
                   positionSmoothFactor,
                   seconds,
                   orientationSmoothType,
                   orientationSmoothFactor,
                   true);
    }

    @Override
    public void transition(double[] camPos,
                           double[] camDir,
                           double[] camUp,
                           double positionDurationSeconds,
                           String positionSmoothType,
                           double positionSmoothFactor,
                           double orientationDurationSeconds,
                           String orientationSmoothType,
                           double orientationSmoothFactor) {
        transition(camPos,
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

    public void transition(List<?> camPos,
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
        transition(api.dArray(camPos),
                   units,
                   api.dArray(camDir),
                   api.dArray(camUp),
                   positionDurationSeconds,
                   positionSmoothType,
                   positionSmoothFactor,
                   orientationDurationSeconds,
                   orientationSmoothType,
                   orientationSmoothFactor,
                   sync);
    }

    @Override
    public void transition(double[] camPos,
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
        transition(camPos,
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

    public void transition(double[] camPos,
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
        if (api.validator.checkDistanceUnits(units)
                && api.validator.checkSmoothType(positionSmoothType, "positionSmoothType")
                && api.validator.checkSmoothType(orientationSmoothType, "orientationSmoothType")) {
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            // Put camera in free mode.
            em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE);

            // Set up final actions
            String name = "cameraTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> api.base.remove_runnable(name);

            var u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
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
            api.base.park_runnable(name, r);

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
                api.base.remove_runnable(name);
            }
        }
    }

    @Override
    public void transition_position(double[] camPos,
                                    String units,
                                    double durationSeconds,
                                    String smoothType,
                                    double smoothFactor,
                                    boolean sync) {
        transition_position(camPos, units, durationSeconds, smoothType, smoothFactor, sync, null);
    }

    public void transition_position(double[] camPos,
                                    String units,
                                    double durationSeconds,
                                    String smoothType,
                                    double smoothFactor,
                                    boolean sync,
                                    AtomicBoolean stop) {
        if (api.validator.checkDistanceUnits(units) && api.validator.checkSmoothType(smoothType, "smoothType")) {
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            // Put camera in free mode.
            em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE);

            // Set up final actions
            String name = "cameraTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> api.base.remove_runnable(name);

            var u = Settings.DistanceUnits.valueOf(units.toUpperCase(Locale.ROOT));
            double[] posUnits = new double[]{u.toInternalUnits(camPos[0]), u.toInternalUnits(camPos[1]), u.toInternalUnits(camPos[2])};

            // Create and park position transition runnable
            CameraTransitionRunnable r = new CameraTransitionRunnable(cam,
                                                                      posUnits,
                                                                      durationSeconds,
                                                                      smoothType,
                                                                      smoothFactor,
                                                                      end,
                                                                      stop);
            api.base.park_runnable(name, r);

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
                api.base.remove_runnable(name);
            }
        }

    }

    @Override
    public void transition_orientation(double[] camDir,
                                       double[] camUp,
                                       double durationSeconds,
                                       String smoothType,
                                       double smoothFactor,
                                       boolean sync) {
        transition_orientation(camDir, camUp, durationSeconds, smoothType, smoothFactor, sync, null);
    }

    public void transition_orientation(double[] camDir,
                                       double[] camUp,
                                       double durationSeconds,
                                       String smoothType,
                                       double smoothFactor,
                                       boolean sync,
                                       AtomicBoolean stop) {
        if (api.validator.checkSmoothType(smoothType, "smoothType")) {
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            // Put camera in free mode.
            em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE);

            // Set up final actions
            String name = "cameraTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> api.base.remove_runnable(name);

            // Create and park orientation transition runnable
            CameraTransitionRunnable r = new CameraTransitionRunnable(cam,
                                                                      camDir,
                                                                      camUp,
                                                                      durationSeconds,
                                                                      smoothType,
                                                                      smoothFactor,
                                                                      end,
                                                                      stop);
            api.base.park_runnable(name, r);

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
                api.base.remove_runnable(name);
            }
        }

    }

    public void transition(List<?> camPos, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        transition(camPos, "internal", camDir, camUp, seconds, sync);
    }

    public void transition(List<?> camPos, String units, List<?> camDir, List<?> camUp, double seconds, boolean sync) {
        transition(api.dArray(camPos), units, api.dArray(camDir), api.dArray(camUp), seconds, sync);
    }

    public void transition(List<?> camPos, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        transition(camPos, "internal", camDir, camUp, seconds, sync);
    }

    public void transition(List<?> camPos, String units, List<?> camDir, List<?> camUp, long seconds, boolean sync) {
        transition(camPos, units, camDir, camUp, (double) seconds, sync);
    }

    public void transition_orientation(List<?> camDir,
                                       List<?> camUp,
                                       double durationSeconds,
                                       String smoothType,
                                       double smoothFactor,
                                       boolean sync) {
        transition_orientation(api.dArray(camDir), api.dArray(camUp), durationSeconds, smoothType, smoothFactor, sync);
    }

    public void transition_position(List<?> camPos, String units, double durationSeconds, String smoothType, double smoothFactor, boolean sync) {
        transition_position(api.dArray(camPos), units, durationSeconds, smoothType, smoothFactor, sync);
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

    static class CameraTransitionRunnable implements Runnable {
        final Object lock;
        final Vector3D v3d1, v3d2, v3d3;
        final Vector3D aux3d3 = new Vector3D();
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

    @Override
    public void dispose() {
        // Stop all ongoing processes.
        for (var stop : stops) {
            if (stop != null) stop.set(true);
        }

        interactive.dispose();

        super.dispose();
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.SCENE_LOADED) {
            this.scene = (Scene) data[0];
            this.focusView.setScene(this.scene);
            // Set interactive
            this.interactive.scene = this.scene;
            this.interactive.focusView.setScene(this.scene);
        }
    }
}
