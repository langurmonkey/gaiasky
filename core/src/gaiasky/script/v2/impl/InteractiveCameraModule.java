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
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.view.FocusView;
import gaiasky.script.v2.api.CamcorderAPI;
import gaiasky.script.v2.api.CameraAPI;
import gaiasky.script.v2.api.InteractiveCameraAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The interactive camera module contains calls and methods that modify the camera using the <i>interactive mode</i>. In this mode,
 * the camera is modified through events that mimic inputs from different devices such as the mouse or the keyboard. This mode
 * produces good results when running scripts interactively (i.e. using the REST server, or the in-app console), but those results
 * are not reproducible, as they typically are affected by external factors such as the current frame rate, the computer clock, or
 * the pre-existing camera momentum and location.
 * <p>
 * If you need reproducible results, we recommend the following:
 * <ul>
 *     <li>
 *         Use the transitions API&mdash;see {@link CameraAPI#transition(double[], double[], double[], double)}
 *         and others in the same family of calls, prefixed by "transition", with the form <code>transition[...]()</code>.
 *     </li>
 *     <li>
 *         Manipulate the internal camera state directly with {@link CameraAPI#set_position(double[])}, {@link CameraAPI#set_direction(double[])},
 *         and {@link CameraAPI#set_up(double[])}. In this case, you need to implement your camera logic in your script.
 *     </li>
 *     <li>
 *         Use camera paths (see {@link CamcorderAPI}).
 *     </li>
 *     <li>
 *         Use key-framed camera paths TODO.
 *     </li>
 * </ul>
 */
public class InteractiveCameraModule extends APIModule implements InteractiveCameraAPI {

    /** Reference to the scene. **/
    Scene scene;
    /** Focus view. **/
    final FocusView focusView;
    /** Currently active stop instances. **/
    private final Set<AtomicBoolean> stops;

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public InteractiveCameraModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
        this.focusView = new FocusView();
        this.stops = new HashSet<>();
    }

    @Override
    public void set_cinematic(boolean cinematic) {
        api.base.post_runnable(() -> em.post(Event.CAMERA_CINEMATIC_CMD, this, cinematic));
    }

    @Override
    public void go_to_object(String name) {
        go_to_object(name, -1);
    }

    @Override
    public void go_to_object(String name, double angle) {
        go_to_object(name, angle, -1);
    }

    @Override
    public void go_to_object(String name, double solidAngle, float waitTimeSeconds) {
        go_to_object(name, solidAngle, waitTimeSeconds, null);
    }

    /**
     * Same as {@link #go_to_object(String, double, float)}, but using an integer for <code>waitTimeSeconds</code>.
     */
    public void go_to_object(String name, double solidAngle, int waitTimeSeconds) {
        go_to_object(name, solidAngle, (float) waitTimeSeconds);
    }

    /**
     * Same as {@link #go_to_object(String, double, float)}, but using an integer for <code>waitTimeSeconds</code>, and
     * a long for <code>solidAngle</code>.
     */
    public void go_to_object(String name, long solidAngle, int waitTimeSeconds) {
        go_to_object(name, (double) solidAngle, (float) waitTimeSeconds);
    }

    /**
     * Same as {@link #go_to_object(String, double, float)}, but using a long for <code>solidAngle</code>.
     */
    public void go_to_object(String name, long solidAngle, float waitTimeSeconds) {
        go_to_object(name, (double) solidAngle, waitTimeSeconds);
    }

    /**
     * Version of {@link #go_to_object(String, double, float)} that gets an optional {@link AtomicBoolean} that
     * enables stopping the execution of the call when its value changes.
     */
    private void go_to_object(String name, double solidAngle, float waitTimeSeconds, AtomicBoolean stop) {
        if (api.validator.checkString(name, "name") && api.validator.checkObjectName(name)) {
            Entity focus = scene.findFocus(name);
            focusView.setEntity(focus);
            focusView.getFocus(name);
            go_to_object(focus, solidAngle, waitTimeSeconds, stop);
        }
    }

    /**
     * Version of {@link #go_to_object(String, double, int)} that gets an optional {@link AtomicBoolean} that
     * enables stopping the execution of the call when its value changes.
     */
    public void go_to_object(String name, double solidAngle, int waitTimeSeconds, AtomicBoolean stop) {
        go_to_object(name, solidAngle, (float) waitTimeSeconds, stop);
    }

    /**
     * Version of {@link #go_to_object(String, double, float, AtomicBoolean)} that gets the actual entity
     * object as an {@link Entity} instead of the entity name as a {@link String}.
     */
    void go_to_object(Entity object, double solidAngle, float waitTimeSeconds, AtomicBoolean stop) {
        if (api.validator.checkNotNull(object, "object") && api.validator.checkNum(solidAngle, -Double.MAX_VALUE, Double.MAX_VALUE, "solidAngle")) {
            stops.add(stop);
            NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

            focusView.setEntity(object);
            api.camera.changeFocus(focusView, cam, waitTimeSeconds);

            /* target angle */
            double target = FastMath.toRadians(solidAngle);
            if (target < 0) {
                // Particles have different sizes to the rest.
                if (focusView.isParticleSet()) {
                    var rx0 = 1.31; // pc
                    var rx1 = 2805.0; // pc
                    var y0 = 1.0;
                    var y1 = 0.001;
                    target = FastMath.toRadians(y0 + (y1 - y0) * (focusView.getAbsolutePosition(api.aux3b1)
                            .lenDouble() * Constants.U_TO_PC - rx0) / (rx1 - rx0));

                } else {
                    target = FastMath.toRadians(20.0);
                }
            }

            long prevTime = TimeUtils.millis();
            if (focusView.getSolidAngle() < target) {
                // Add forward movement while distance > target distance.
                while (focusView.getSolidAngle() < target && (stop == null || !stop.get())) {
                    // dt in ms.
                    long dt = TimeUtils.timeSinceMillis(prevTime);
                    prevTime = TimeUtils.millis();

                    em.post(Event.CAMERA_FWD, this, (double) dt);
                    try {
                        api.base.sleep(0.1f);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            } else {
                // Add backward movement while distance > target distance
                while (focusView.getSolidAngleApparent() > target && (stop == null || !stop.get())) {
                    // dt in ms
                    long dt = TimeUtils.timeSinceMillis(prevTime);
                    prevTime = TimeUtils.millis();

                    em.post(Event.CAMERA_FWD, this, (double) -dt);
                    try {
                        api.base.sleep(0.1f);
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            }

            // We can stop now
            em.post(Event.CAMERA_STOP, this);
        }
    }

    /**
     * Version of {@link #go_to_object(Entity, double, float, AtomicBoolean)} that gets the <code>waitTimeSeconds</code>
     * as an integer instead of a float.
     */
    public void go_to_object(Entity object, double solidAngle, int waitTimeSeconds, AtomicBoolean stop) {
        go_to_object(object, solidAngle, (float) waitTimeSeconds, stop);
    }

    @Override
    public void add_forward(final double value) {
        if (api.validator.checkNum(value, -100d, 100d, "cameraForward"))
            api.base.post_runnable(() -> em.post(Event.CAMERA_FWD, this, value));
    }

    public void add_forward(final long value) {
        add_forward((double) value);
    }

    @Override
    public void add_rotation(final double deltaX, final double deltaY) {
        if (api.validator.checkNum(deltaX, -100d, 100d, "deltaX")
                && api.validator.checkNum(deltaY, -100d, 100d, "deltaY"))
            api.base.post_runnable(() -> em.post(Event.CAMERA_ROTATE, this, deltaX, deltaY));
    }

    public void add_rotation(final double deltaX, final long deltaY) {
        add_rotation(deltaX, (double) deltaY);
    }

    public void add_rotation(final long deltaX, final double deltaY) {
        add_rotation((double) deltaX, deltaY);
    }

    @Override
    public void add_roll(final double value) {
        if (api.validator.checkNum(value, -100d, 100d, "roll")) api.base.post_runnable(() -> em.post(Event.CAMERA_ROLL, this, value));
    }

    public void add_roll(final long roll) {
        add_roll((double) roll);
    }

    @Override
    public void add_turn(final double deltaX, final double deltaY) {
        if (api.validator.checkNum(deltaX, -100d, 100d, "deltaX")
                && api.validator.checkNum(deltaY, -100d, 100d, "deltaY")) {
            api.base.post_runnable(() -> em.post(Event.CAMERA_TURN, this, deltaX, deltaY));
        }
    }

    public void add_turn(final double deltaX, final long deltaY) {
        add_turn(deltaX, (double) deltaY);
    }

    public void add_turn(final long deltaX, final double deltaY) {
        add_turn((double) deltaX, deltaY);
    }

    public void add_turn(final long deltaX, final long deltaY) {
        add_turn((double) deltaX, (double) deltaY);
    }

    @Override
    public void add_yaw(final double amount) {
        add_turn(amount, 0d);
    }

    public void cameraYaw(final long amount) {
        add_yaw((double) amount);
    }

    @Override
    public void add_pitch(final double amount) {
        add_turn(0d, amount);
    }

    public void add_pitch(final long amount) {
        add_pitch((double) amount);
    }

    @Override
    public double get_speed() {
        return GaiaSky.instance.cameraManager.getSpeed();
    }

    @Override
    public void speed_setting(final float speed) {
        if (api.validator.checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed")) {
            api.base.post_runnable(() -> em.post(Event.CAMERA_SPEED_CMD,
                                                 this,
                                                 MathUtilsDouble.lint(speed,
                                                                      Constants.MIN_SLIDER,
                                                                      Constants.MAX_SLIDER,
                                                                      Constants.MIN_CAM_SPEED,
                                                                      Constants.MAX_CAM_SPEED),
                                                 false));
        }
    }

    public void speed_setting(final int speed) {
        speed_setting((float) speed);
    }

    @Override
    public void rotation_speed_setting(float speed) {
        if (api.validator.checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed")) {
            api.base.post_runnable(() -> em.post(Event.ROTATION_SPEED_CMD,
                                                 this,
                                                 MathUtilsDouble.lint(speed,
                                                                      Constants.MIN_SLIDER,
                                                                      Constants.MAX_SLIDER,
                                                                      Constants.MIN_ROT_SPEED,
                                                                      Constants.MAX_ROT_SPEED)));
        }
    }

    public void rotation_speed_setting(final int speed) {
        this.rotation_speed_setting((float) speed);
    }

    @Override
    public void turning_speed_setting(float speed) {
        if (api.validator.checkNum(speed, Constants.MIN_SLIDER, Constants.MAX_SLIDER, "speed")) {
            api.base.post_runnable(() -> em.post(Event.TURNING_SPEED_CMD,
                                                 this,
                                                 MathUtilsDouble.lint(speed,
                                                                      Constants.MIN_SLIDER,
                                                                      Constants.MAX_SLIDER,
                                                                      Constants.MIN_TURN_SPEED,
                                                                      Constants.MAX_TURN_SPEED),
                                                 false));
        }
    }

    public void turning_speed_setting(final int speed) {
        this.turning_speed_setting((float) speed);
    }

    @Override
    public void land_on(String name) {
        if (api.validator.checkString(name, "name")) {
            Entity target = api.scene.get_entity(name);
            if (Mapper.focus.has(target)) {
                synchronized (focusView) {
                    focusView.setEntity(target);
                    focusView.getFocus(name);
                }
                land_on(target, null);
            }
        }
    }

    public void land_on(Entity object, AtomicBoolean stop) {
        if (api.validator.checkNotNull(object, "object")) {

            stops.add(stop);

            synchronized (focusView) {
                if (Mapper.atmosphere.has(object)) {
                    focusView.setEntity(object);
                    // Planets.
                    NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;
                    // FOCUS_MODE wait - 2 seconds
                    float waitTimeSeconds = -1;

                    /*
                     * SAVE
                     */

                    // Save speed, set it to 50
                    double speed = Settings.settings.scene.camera.speed;
                    em.post(Event.CAMERA_SPEED_CMD, this, 25f / 10f, false);

                    // Save turn speed, set it to 50
                    double turnSpeedBak = Settings.settings.scene.camera.turn;
                    em.post(Event.TURNING_SPEED_CMD,
                            this,
                            (float) MathUtilsDouble.flint(20d,
                                                          Constants.MIN_SLIDER,
                                                          Constants.MAX_SLIDER,
                                                          Constants.MIN_TURN_SPEED,
                                                          Constants.MAX_TURN_SPEED),
                            false);

                    // Save cinematic
                    boolean cinematic = Settings.settings.scene.camera.cinematic;
                    Settings.settings.scene.camera.cinematic = true;

                    /*
                     * FOCUS
                     */
                    api.camera.changeFocus(focusView, cam, waitTimeSeconds);

                    /* target distance */
                    double target = 100 * Constants.M_TO_U;

                    Vector3Q camObj = api.aux3b1;
                    focusView.getAbsolutePosition(camObj).add(cam.posInv).nor();
                    Vector3D dir = cam.direction;

                    // Add forward movement while distance > target distance
                    boolean distanceNotMet = (focusView.getDistToCamera() - focusView.getRadius()) > target;
                    boolean viewNotMet = FastMath.abs(dir.angle(camObj)) < 90;

                    long prevTime = TimeUtils.millis();
                    while ((distanceNotMet || viewNotMet) && (stop == null || !stop.get())) {
                        // dt in ms
                        long dt = TimeUtils.timeSinceMillis(prevTime);
                        prevTime = TimeUtils.millis();

                        if (distanceNotMet) em.post(Event.CAMERA_FWD, this, 0.1d * dt);
                        else cam.stopForwardMovement();

                        if (viewNotMet) {
                            if (focusView.getDistToCamera() - focusView.getRadius() < focusView.getRadius() * 5)
                                // Start turning where we are at n times the radius
                                em.post(Event.CAMERA_TURN, this, 0d, dt / 500d);
                        } else {
                            cam.stopRotateMovement();
                        }

                        try {
                            api.base.sleep_frames(1);
                        } catch (Exception e) {
                            logger.error(e);
                        }

                        // focus.transform.getTranslation(aux);
                        viewNotMet = FastMath.abs(dir.angle(camObj)) < 90;
                        distanceNotMet = (focusView.getDistToCamera() - focusView.getRadius()) > target;
                    }

                    // STOP
                    em.post(Event.CAMERA_STOP, this);

                    // Roll till done
                    Vector3D up = cam.up;
                    // aux1 <- camera-object
                    camObj = focusView.getAbsolutePosition(api.aux3b1).sub(cam.pos);
                    double ang1 = up.angle(camObj);
                    double ang2 = up.cpy().rotate(cam.direction, 1).angle(camObj);
                    double rollSign = ang1 < ang2 ? -1d : 1d;

                    if (ang1 < 170) {
                        rollAndWait(rollSign * 0.02d, 170d, 50L, cam, camObj, stop);

                        // STOP
                        cam.stopMovement();

                        rollAndWait(rollSign * 0.006d, 176d, 50L, cam, camObj, stop);
                        // STOP
                        cam.stopMovement();

                        rollAndWait(rollSign * 0.003d, 178d, 50L, cam, camObj, stop);
                    }

                    /*
                     * RESTORE
                     */

                    // We can stop now
                    em.post(Event.CAMERA_STOP, this);

                    // Restore cinematic
                    Settings.settings.scene.camera.cinematic = cinematic;

                    // Restore speed
                    em.post(Event.CAMERA_SPEED_CMD, this, (float) speed, false);

                    // Restore turning speed
                    em.post(Event.TURNING_SPEED_CMD, this, (float) turnSpeedBak, false);
                }
            }
        }
    }

    void rollAndWait(double roll, double target, long sleep, NaturalCamera cam, Vector3Q camObject, AtomicBoolean stop) {
        // Apply roll and wait
        double ang = cam.up.angle(camObject);

        while (ang < target && (stop == null || !stop.get())) {
            cam.addRoll(roll, false);

            try {
                api.base.sleep(sleep);
            } catch (Exception e) {
                logger.error(e);
            }

            ang = cam.up.angle(api.aux3d1);
        }
    }

    @Override
    public void land_at_location(String name, String locationName) {
        land_at_location(name, locationName, null);
    }

    public void land_at_location(String name, String locationName, AtomicBoolean stop) {
        if (api.validator.checkString(name, "name")) {
            stops.add(stop);
            Entity entity = api.scene.get_entity(name);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(name);
                }
                land_at_location(entity, locationName, stop);
            }
        }
    }

    public void land_at_location(Entity object, String locationName, AtomicBoolean stop) {
        if (api.validator.checkNotNull(object, "object") && api.validator.checkString(locationName, "locationName")) {

            stops.add(stop);
            if (Mapper.atmosphere.has(object)) {
                synchronized (focusView) {
                    focusView.setEntity(object);
                    Entity loc = focusView.getChildByNameAndArchetype(locationName, scene.archetypes().get("Loc"));
                    if (loc != null) {
                        var locMark = Mapper.loc.get(loc);
                        land_at_location(object, locMark.location.x, locMark.location.y, stop);
                        return;
                    }
                    logger.info("Location '" + locationName + "' not found on object '" + focusView.getCandidateName() + "'");
                }
            }
        }
    }

    @Override
    public void land_at_location(String name, double longitude, double latitude) {
        if (api.validator.checkString(name, "name")) {
            Entity entity = api.scene.get_entity(name);
            if (Mapper.focus.has(entity)) {
                synchronized (focusView) {
                    focusView.setEntity(entity);
                    focusView.getFocus(name);
                }
                land_at_location(entity, longitude, latitude, null);
            }
        }
    }

    public void land_at_location(Entity entity, double longitude, double latitude, AtomicBoolean stop) {
        if (api.validator.checkNotNull(entity, "object")
                && api.validator.checkNum(latitude, -90d, 90d, "latitude")
                && api.validator.checkNum(longitude, -360d, 360d, "longitude")) {
            synchronized (focusView) {
                focusView.setEntity(entity);
                stops.add(stop);
                String nameStub = focusView.getCandidateName() + " [loc]";

                if (!scene.index().containsEntity(nameStub)) {
                    var archetype = scene.archetypes().get("Invisible");
                    Entity invisible = archetype.createEntity();
                    var base = Mapper.base.get(invisible);
                    base.setName(nameStub);
                    base.setCt("Others");
                    var graph = Mapper.graph.get(invisible);
                    graph.translation = new Vector3Q();
                    graph.setParent(Scene.ROOT_NAME);
                    scene.initializeEntity(invisible);
                    scene.setUpEntity(invisible);
                    EventManager.publish(Event.SCENE_ADD_OBJECT_NO_POST_CMD, this, invisible, true);
                }
                Entity invisible = scene.getEntity(nameStub);

                if (Mapper.atmosphere.has(entity)) {
                    NaturalCamera cam = GaiaSky.instance.cameraManager.naturalCamera;

                    double targetAngle = 35 * MathUtilsDouble.degRad;
                    if (focusView.getSolidAngle() > targetAngle) {
                        // Zoom out
                        while (focusView.getSolidAngle() > targetAngle && (stop == null || !stop.get())) {
                            cam.addForwardForce(-5d);
                            api.base.sleep_frames(1);
                        }
                        // STOP
                        cam.stopMovement();
                    }

                    // Go to object
                    go_to_object(focusView.getEntity(), 15, -1, stop);

                    // Save speed, set it to 50
                    double speed = Settings.settings.scene.camera.speed;
                    em.post(Event.CAMERA_SPEED_CMD, this, 25f / 10f);

                    // Save turn speed, set it to 50
                    double turnSpeedBak = Settings.settings.scene.camera.turn;
                    em.post(Event.TURNING_SPEED_CMD,
                            this,
                            (float) MathUtilsDouble.flint(50d,
                                                          Constants.MIN_SLIDER,
                                                          Constants.MAX_SLIDER,
                                                          Constants.MIN_TURN_SPEED,
                                                          Constants.MAX_TURN_SPEED));

                    // Save rotation speed, set it to 20
                    double rotationSpeedBak = Settings.settings.scene.camera.rotate;
                    em.post(Event.ROTATION_SPEED_CMD,
                            this,
                            (float) MathUtilsDouble.flint(20d,
                                                          Constants.MIN_SLIDER,
                                                          Constants.MAX_SLIDER,
                                                          Constants.MIN_ROT_SPEED,
                                                          Constants.MAX_ROT_SPEED));

                    // Save cinematic
                    boolean cinematic = Settings.settings.scene.camera.cinematic;
                    Settings.settings.scene.camera.cinematic = true;

                    // Save crosshair
                    boolean crosshair = Settings.settings.scene.crosshair.focus;
                    Settings.settings.scene.crosshair.focus = false;

                    // Get target position
                    Vector3Q target = api.aux3b1;
                    focusView.getPositionAboveSurface(longitude, latitude, 50, target);

                    // Get object position
                    Vector3Q objectPosition = focusView.getAbsolutePosition(api.aux3b2);

                    // Check intersection with object
                    boolean intersects = IntersectorDouble.checkIntersectSegmentSphere(cam.pos.tov3d(api.aux3d3),
                                                                                       target.tov3d(api.aux3d1),
                                                                                       objectPosition.tov3d(api.aux3d2),
                                                                                       focusView.getRadius());

                    if (intersects) {
                        add_rotation(5d, 5d);
                    }

                    while (intersects && (stop == null || !stop.get())) {
                        api.base.sleep(0.1f);

                        objectPosition = focusView.getAbsolutePosition(api.aux3b2);
                        intersects = IntersectorDouble.checkIntersectSegmentSphere(cam.pos.tov3d(api.aux3d3),
                                                                                   target.tov3d(api.aux3d1),
                                                                                   objectPosition.tov3d(api.aux3d2),
                                                                                   focusView.getRadius());
                    }

                    api.camera.stop();

                    Mapper.base.get(invisible).ct = focusView.getCt();
                    Mapper.body.get(invisible).pos.set(target);

                    // Go to object
                    go_to_object(nameStub, 20, 0, stop);

                    // Restore cinematic
                    Settings.settings.scene.camera.cinematic = cinematic;

                    // Restore speed
                    em.post(Event.CAMERA_SPEED_CMD, this, (float) speed);

                    // Restore turning speed
                    em.post(Event.TURNING_SPEED_CMD, this, (float) turnSpeedBak);

                    // Restore rotation speed
                    em.post(Event.ROTATION_SPEED_CMD, this, (float) rotationSpeedBak);

                    // Restore crosshair
                    Settings.settings.scene.crosshair.focus = crosshair;

                    // Land
                    land_on(focusView.getEntity(), stop);
                }
                EventManager.publish(Event.SCENE_REMOVE_OBJECT_CMD, this, invisible, true);
            }
        }
    }

    @Override
    public void dispose() {
        // Stop all ongoing processes.
        for (var stop : stops) {
            if (stop != null) stop.set(true);
        }

        super.dispose();
    }
}
