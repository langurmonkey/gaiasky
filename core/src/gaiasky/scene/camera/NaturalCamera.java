/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import gaiasky.GaiaSky;
import gaiasky.data.util.GlobalResources;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.util.ControllerConnectionListener;
import gaiasky.gui.vr.OpenXRListener;
import gaiasky.input.AbstractMouseKbdListener;
import gaiasky.input.GameMouseKbdListener;
import gaiasky.input.MainGamepadListener;
import gaiasky.input.MainMouseKbdListener;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.gdx.g2d.Sprite;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.component.Body;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Constants;
import gaiasky.util.DecalUtils;
import gaiasky.util.MasterManager;
import gaiasky.util.SlaveManager;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL30;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The basic camera of Gaia Sky. Implements focus, free and game modes. It also incorporates the
 * cinematic and the non-cinematic behaviours: in the regular (non cinematic) mode the camera is
 * damped to a full stop when the input is released, while in cinematic mode it keeps rotating and
 * moving inwards and outwards until it is explicitly stopped.
 */
public class NaturalCamera extends AbstractCamera implements IObserver {

    /**
     * One metre in internal units.
     * <p>
     * A method, not a constant: the value depends on the distance scale factor, which is
     * initialised after the camera is built.
     */
    private static double minDist() {
        return Constants.M_TO_U;
    }

    /** Camera speed at a body surface, in internal units per second. See {@link #minDist()}. */
    private static double surfaceSpeed() {
        return 10d * Constants.M_TO_U;
    }

    /** Multiplier of {@link #surfaceSpeed()} for the top-speed cap. */
    private static final double SURFACE_SPEED_CAP_FACTOR = 10d;

    /** Minimum altitude, in body radii, considered by the near-surface damping. */
    private static final double MIN_ALTITUDE_RATIO = 1.0e-1;

    /** Maximum boost of the near-surface damping. */
    private static final double MAX_DAMPING_BOOST = 1.0 / MIN_ALTITUDE_RATIO;

    /**
     * Altitude at which the surface speed curve gives {@link #surfaceSpeed()}: one metre.
     * See {@link #minDist()}.
     */
    private static double speedScaleRefAltitude() {
        return Constants.M_TO_U;
    }

    /** Exponent of the power law of the surface speed curve. */
    private static final double SPEED_SCALE_EXPONENT = 0.67d;

    /** Ceiling of the speed curve. */
    private static final double MAX_SPEED_SCALING = 2.0e16;

    /** Distance beyond which the speed curve stops being linearU. */
    private static final double SPEED_SCALE_FAR_KNEE_M = 50.0 * Constants.AU_TO_U * Constants.U_TO_M;

    /** Exponent of the speed curve above {@link #SPEED_SCALE_FAR_KNEE_M}. */
    private static final double SPEED_SCALE_FAR_EXPONENT = 0.94d;

    /** Distance above which the curve is blended back to linear. */
    private static final double SPEED_SCALE_FAR_LINEAR_KNEE_M = 1.0e10 * Constants.PC_TO_U * Constants.U_TO_M;

    /** Decades of distance over which the power law is blended into the linear one. */
    private static final double SPEED_SCALE_FAR_LINEAR_RAMP_DECADES = 1.0d;

    /** Floor of the factor that slows the rotation as the focus fills the screen. */
    private static final double ROTATION_MIN_FACTOR = 0.15;

    /** Number of focus radii below which the rotation slowdown starts. */
    private static final double ROTATION_SLOWDOWN_RADII = 2.0;

    /** Time constant (s) of the angular rate ramp-up. */
    private static final double ROTATION_ACCEL_TAU = 0.3;

    /** Time constant (s) of the angular rate decay when the input stops. Unused in cinematic mode. */
    private static final double ROTATION_DECAY_TAU = 0.15;

    /** Effectively infinite time constant (s), used to hold the angular rate in cinematic mode. */
    private static final double ROTATION_HOLD_TAU = 1.0e9;

    /** Angular rates below this are snapped to zero. */
    private static final double ROTATION_MIN_RATE = 1e-9;

    /** Seconds without forward input before the camera starts damping to a stop. Only when {@link #stopsOnRelease()}. */
    private static final double FORWARD_RESPONSE_TIME = 0.25;

    /** Time constant (s) of the damping applied once the forward input is released. Only when {@link #stopsOnRelease()}. */
    private static final double STOP_DAMPING_TAU = 0.3;

    /** Seconds without forward input after which the camera stops outright. Only when {@link #stopsOnRelease()}. */
    private static final double FORWARD_STOP_TIME = 1.0;

    /** Seconds after the last rotation input during which the requested rate is still active. */
    private static final double ROTATION_INPUT_GRACE = 0.06;

    /**
     * The force acting on the entity and the friction.
     **/
    private final Vector3Q force;
    /**
     * We use this lock to update any attributes of this camera.
     **/
    private final Object updateLock = new Object();
    /**
     * VR offset.
     **/
    public final Vector3D vrOffset;
    /**
     * Acceleration and velocity.
     **/
    public final Vector3D accel, vel, posBak;
    public final Vector3D direction, up;
    /**
     * Indicates whether the camera is facing the focus or not.
     **/
    public boolean facingFocus;
    /**
     * The focus entity.
     */
    public FocusView focus;
    /**
     * Reference to focus, backup.
     **/
    public FocusView focusBak;
    /**
     * Previous and aux quaternions, for focus lock.
     */
    QuaternionDouble qPrev;
    /**
     * Fov value backup.
     **/
    float fovBackup;
    /**
     * Gravity in game mode.
     **/
    boolean gravity = true;
    /**
     * Has the direction diverted from the focus?
     **/
    boolean diverted = false;
    /**
     * Whether VR is active or not.
     **/
    boolean vr;
    /**
     * Flag that marks whether the projection has already been modified.
     * Only in master-slave configurations.
     */
    boolean projectionFlag = false;
    private Vector3Q friction;
    private Vector3Q focusDirection;
    /**
     * Auxiliary double vectors.
     **/
    private final Vector3D aux1, aux2, aux5;
    private final Vector3Q dx, aux1b, aux2b, aux3b, aux4b, aux5b, nextFocusPosition, nextClosestPosition;
    private final Vector2 aux2f2;
    /**
     * Auxiliary float vector.
     **/
    private final Vector3 auxf1;
    /**
     * Acceleration, velocity and position for pitch, yaw and roll.
     **/
    private Vector3D pitch, yaw, roll;
    /**
     * Acceleration, velocity and position for the horizontal and vertical
     * rotation around the focus.
     **/
    private Vector3D horizontal, vertical;
    /**
     * Time since last forward control issued, in seconds.
     **/
    private double lastFwdTime = 0d;
    /**
     * The last forward amount, positive forward, negative backward.
     **/
    private double lastFwdAmount = 0;
    /**
     * Thrust which keeps the camera going. Mainly for game pads.
     **/
    private double thrust = 0;
    private int thrustDirection = 0;
    /**
     * Whether the camera stops after a few seconds or keeps going.
     **/
    private boolean fullStop = true;
    private CameraMode lastMode;
    /**
     * Auxiliary focus view object, used to reduce the amount of allocations.
     */
    private final FocusView focusView;
    /**
     * The tracking object, if any.
     */
    private final FocusView trackingObject;
    /**
     * The name of the tracking object.
     */
    private String trackingName;
    /**
     * The direction point to seek.
     */
    private Vector3D lastVel;
    /**
     * FOCUS_MODE position.
     **/
    private Vector3Q focusPos;
    /**
     * Free mode target.
     **/
    private Vector3Q freeTargetPos;
    private boolean freeTargetOn;
    private Vector3Q desired;

    /**
     * Distance from the camera to the focus, as computed at the beginning of the current frame in
     * {@link #camUpdate(double, ITimeFrameProvider)}. Negative when it has not been computed yet,
     * in which case the distance is taken from the focus object instead. Used by {@link
     * #addRotateMovement(double, double, boolean, boolean)} so that the rotation slowdown and the
     * surface mode use the same, current distance.
     */
    private double distFromFocus = -1;

    /**
     * Unit vector from the camera to the centre of the focus, computed at the beginning of the
     * current frame together with {@link #distFromFocus}. Unlike {@link #focusDirection}, it is not
     * affected by surface mode. Used by {@link #updateVelocity(double, double, double)} to prevent
     * the camera from stepping past the focus object.
     */
    private final Vector3D camToFocusDir = new Vector3D();

    /**
     * Set by {@link #updateVelocity(double, double, double)} when the current step lands the camera
     * exactly on the focus clearance sphere. {@link #updatePosition(double, double, double)} zeroes
     * the velocity after taking the step, so no motion is left over to bounce the camera back.
     */
    private boolean stopAfterStep = false;

    /**
     * Time (in seconds) since the last rotation input arrived. Reset by {@link
     * #addAmount(Vector3D, double, boolean)} and the {@code setXxx} methods.
     */
    private double rotationIdleTime = Double.MAX_VALUE;

    /**
     * Surface mode Cartesian coordinates of the mouse pointer on the focus object at every frame.
     */
    private final Vector3D pointerCartesian;
    /**
     * This flag is up when new pointer coordinates are set. It is down when the coordinates have been consumed.
     **/
    private final AtomicBoolean pointerCoordinatesFlag = new AtomicBoolean(false);
    /**
     * Surface mode is active.
     */
    private final AtomicBoolean surfaceModeFlag = new AtomicBoolean(false);

    /**
     * VR mode stuff.
     **/
    private boolean firstAux = true;
    private float firstAngle = 0;
    /**
     * Velocity module, in case it comes from a game pad.
     * Sets velocity in the direction of the direction vector.
     **/
    private double velocityGamepad = 0;
    /**
     * Factor applied to all velocities.
     **/
    private double movementMultiplier = 1;
    /**
     * Factor applied to speed only.
     **/
    private double speedMultiplier = 1;

    private double speedScaling, speedScalingCapped;
    /**
     * VR velocity vectors.
     **/
    private Vector3D velocityVR0, velocityVR1;
    /**
     * Magnitude of velocityVR vector. Sets the velocity in the direction
     * of the VR controller.
     **/
    private double velocityVRX = 0;
    private double velocityVRY = 0;
    /**
     * Home object as defined in the properties file.
     **/
    private IFocus home;
    /**
     * The current listener.
     */
    private AbstractMouseKbdListener currentMouseKbdListener;
    /**
     * Implements the regular mouse+kbd camera input.
     **/
    private MainMouseKbdListener naturalMouseKbdListener;
    /**
     * Implements WASD movement + mouse look camera input.
     */
    private GameMouseKbdListener gameMouseKbdListener;

    /**
     * Implements gamepad camera input.
     **/
    private MainGamepadListener gamepadListener;

    /**
     * VR listener.
     **/
    private OpenXRListener openXRListener;

    private double DIST_SMOOTH_UP;
    private double MAX_ALLOWED_DISTANCE;

    /**
     * Time constant (in seconds) of the exponential low-pass filters applied to the raw distances
     * used for speed scaling. Using a time constant instead of a per-call factor makes the
     * filtering independent of the framerate.
     */
    private static final double DISTANCE_SMOOTHING_TAU = 0.1d;

    /**
     * Maximum relative growth of the speed scaling, in e-folds per second, when the reference
     * object changes. The scaling is linear in the smoothed distance, so this caps how fast the top
     * speed can rise after a handoff: about 4.5x per second, i.e. roughly 5 seconds to go from the
     * solar system to the interstellar regime.
     */
    static final double DISTANCE_MAX_GROWTH_RATE = 1.5d;

    /**
     * Maximum relative growth of the speed scaling, in e-folds per second, when the reference is the
     * same and the distance only grows because the camera is flying away from it. That is real
     * motion, not a discontinuity, so it may be much faster: leaving a star bubble takes about two
     * seconds from the low limit to the top speed, against twenty at
     * {@link #DISTANCE_MAX_GROWTH_RATE}.
     */
    static final double DISTANCE_GROWTH_RATE = 12.0d;

    /**
     * Factor by which the raw distance may grow from one frame to the next before the reference is
     * assumed to have changed. A continuous fly-away grows the distance by a few percent per frame;
     * a handoff or a culled body multiplies it by orders of magnitude.
     */
    private static final double REFERENCE_JUMP_FACTOR = 4.0d;

    /**
     * Hard cap on the growth of the smoothed distance in a single frame. Frame-rate safety net on
     * top of the rate caps: a stall or a breakpoint would otherwise let a whole second worth of
     * growth through at once.
     */
    private static final double DISTANCE_MAX_FRAME_RATIO = 4.0d;

    /**
     * Raw (unsmoothed) distance to the closest body, in internal units.
     */
    private double rawBodyDistance = Double.MAX_VALUE;
    /**
     * Raw (unsmoothed) smoothed-radius distance to the closest star, blended into
     * {@link #regularDistance} with the low and high limits.
     */
    private double rawStarDistance = Double.MAX_VALUE;
    /**
     * Raw (unsmoothed) distance the camera uses when no star is nearby: the minimum of
     * {@link #rawBodyDistance} and the focus distance.
     */
    private double regularDistance = Double.MAX_VALUE;
    /**
     * Minimum of {@link #rawStarDistance} and {@link #regularDistance}.
     */
    private double rawDistance = Double.MAX_VALUE;

    /**
     * Whether the raw distances have already been computed for the current frame.
     */
    private boolean rawDistancesValid = false;

    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;

    private Sprite spriteFocus, spriteClosest, spriteHome;
    private Texture crosshairArrow, crosshairGravWaves;

    /**
     * A reference to the main scene.
     **/
    private Scene scene;

    public NaturalCamera(AssetManager assetManager,
                         CameraManager parent,
                         boolean vr,
                         ShaderProgram spriteShader,
                         ShaderProgram shapeShader) {
        super(parent);
        this.vrOffset = new Vector3D();
        this.vel = new Vector3D();
        this.accel = new Vector3D();
        this.force = new Vector3Q();
        this.posBak = new Vector3D();
        this.qPrev = new QuaternionDouble();
        this.focus = new FocusView();
        this.focusView = new FocusView();
        this.trackingObject = new FocusView();
        this.vr = vr;
        this.pointerCartesian = new Vector3D();

        this.up = new Vector3D(1, 0, 0);
        this.direction = new Vector3D(0, 1, 0);

        this.aux1 = new Vector3D();
        this.aux2 = new Vector3D();
        this.aux5 = new Vector3D();
        this.auxf1 = new Vector3();
        this.aux2f2 = new Vector2();

        this.aux1b = new Vector3Q();
        this.aux2b = new Vector3Q();
        this.aux3b = new Vector3Q();
        this.aux4b = new Vector3Q();
        this.aux5b = new Vector3Q();

        this.dx = new Vector3Q();
        this.nextFocusPosition = new Vector3Q();
        this.nextClosestPosition = new Vector3Q();

        initialize(spriteShader, shapeShader);

    }

    public void initialize(ShaderProgram spriteShader,
                           ShaderProgram shapeShader) {
        if (vr) {
            camera = new PerspectiveCamera(GaiaSky.settings().scene.camera.fov, GaiaSky.settings().graphics.backBufferResolution[0],
                                           GaiaSky.settings().graphics.backBufferResolution[1]);
        } else {
            camera = new PerspectiveCamera(GaiaSky.settings().scene.camera.fov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;
        fovBackup = GaiaSky.settings().scene.camera.fov;

        // init cameras vector.
        cameras = new PerspectiveCamera[]{camera, camLeft, camRight};

        updateFovFactor();

        focusDirection = new Vector3Q();
        desired = new Vector3Q();
        pitch = new Vector3D(0.0f, 0.0f, -3.0291599E-6f);
        yaw = new Vector3D(0.0f, 0.0f, -7.9807205E-6f);
        roll = new Vector3D(0.0f, 0.0f, -1.4423944E-4f);
        horizontal = new Vector3D();
        vertical = new Vector3D();

        friction = new Vector3Q();
        lastVel = new Vector3D();
        focusPos = new Vector3Q();
        freeTargetPos = new Vector3Q();
        freeTargetOn = false;

        DIST_SMOOTH_UP = 5000.0 * Constants.MPC_TO_U;
        MAX_ALLOWED_DISTANCE = 50_000.0 * Constants.MPC_TO_U;

        // Mouse and keyboard listeners.
        naturalMouseKbdListener = new MainMouseKbdListener(this);
        gameMouseKbdListener = new GameMouseKbdListener(this);
        currentMouseKbdListener = null;
        // Controller listeners.
        gamepadListener = new MainGamepadListener(this, GaiaSky.settings().controls.gamepad.mappingsFile);
        ControllerConnectionListener controllerConnectionListener = new ControllerConnectionListener();
        Controllers.addListener(controllerConnectionListener);
        if (vr) {
            openXRListener = new OpenXRListener(this);
        }

        // Shape renderer (pointer guide lines).
        shapeRenderer = new ShapeRenderer(10, shapeShader);
        shapeRenderer.getProjectionMatrix()
                .setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight);

        // Init sprite batch for crosshair.
        spriteBatch = new SpriteBatch(50, spriteShader);

        // FOCUS_MODE is changed from GUI.
        EventManager.instance.subscribe(this, Event.SCENE_LOADED, Event.FOCUS_CHANGE_CMD, Event.FOV_CMD,
                                        Event.CAMERA_POS_CMD, Event.CAMERA_DIR_CMD, Event.CAMERA_UP_CMD,
                                        Event.CAMERA_PROJECTION_CMD,
                                        Event.CAMERA_FWD, Event.CAMERA_ROTATE, Event.CAMERA_PAN, Event.CAMERA_ROLL,
                                        Event.CAMERA_TURN,
                                        Event.CAMERA_STOP, Event.CAMERA_CENTER, Event.GO_TO_OBJECT_CMD, Event.CUBEMAP_CMD,
                                        Event.FREE_MODE_COORD_CMD, Event.FOCUS_NOT_AVAILABLE, Event.TOGGLE_VISIBILITY_CMD,
                                        Event.CAMERA_CENTER_FOCUS_CMD, Event.CONTROLLER_CONNECTED_INFO,
                                        Event.CONTROLLER_DISCONNECTED_INFO,
                                        Event.NEW_DISTANCE_SCALE_FACTOR, Event.CAMERA_TRACKING_OBJECT_CMD);
    }

    @Override
    public void doneLoading(AssetManager manager) {
        var globalResources = GaiaSky.instance.getGlobalResources();
        spriteFocus = new Sprite(globalResources.getTexture("crosshair-focus"));
        spriteClosest = new Sprite(globalResources.getTexture("crosshair-closest"));
        spriteHome = new Sprite(globalResources.getTexture("crosshair-home"));
        crosshairArrow = globalResources.getTexture("crosshair-arrow");
        crosshairGravWaves = globalResources.getTexture("crosshair-arrow");
    }

    /**
     * Computes the future positions at the given time of the closest and focus objects.
     *
     * @param time The time.
     */
    private void computeNextPositions(ITimeFrameProvider time) {
        // Next focus position.
        if (getMode().isFocus() && focus != null) {
            focus.getPredictedPosition(nextFocusPosition, time, this, false);
        }
        // Next closest position.
        if (closestBody != null && !closestBody.isEmpty()) {
            if (getMode().isFocus() && focus.getEntity() == closestBody.getEntity()) {
                nextClosestPosition.set(nextFocusPosition);
            } else {
                closestBody.getPredictedPosition(nextClosestPosition, time, this, false);
            }
        }
    }

    public void update(double dt,
                       ITimeFrameProvider time) {
        // SLAVE - orient.
        if (SlaveManager.projectionActive()) {
            camOrientProjection(SlaveManager.instance.yaw, SlaveManager.instance.pitch, SlaveManager.instance.roll);
        }

        // Update camera.
        camUpdate(dt, time);

        // MASTER - broadcast.
        if (MasterManager.instance != null) {
            // Send camera state.
            MasterManager.instance.boardcastCameraAndTime(this.pos, this.direction, this.up, time);
        }
    }

    double previousRotationAngle = 0;

    private void camUpdate(double dt,
                           ITimeFrameProvider time) {
        currentMouseKbdListener.update();
        gamepadListener.update();
        if (vr) {
            openXRListener.update();
        }

        // Next focus and closest positions.
        computeNextPositions(time);

        // The whole update thread must lock the value of direction and up.
        distance = pos.lenDouble();

        CameraMode m = (parent.current == this ? parent.mode : lastMode);
        // Reset the focus distance: it is recomputed in focus mode below.
        distFromFocus = -1;
        // Advance the rotation input idle timer (reset whenever an input arrives).
        rotationIdleTime += dt;
        // Advance the distance smoothing filters exactly once per frame.
        rawDistancesValid = false;
        updateSmoothedDistances(dt);
        speedScaling = m.isGame() ? speedScaling(1e-5) : speedScaling();
        // Lower bound of the velocity clamp. This must stay well above surfaceSpeed(), otherwise
        // the cap (and not the available speed) limits the camera right above a surface.
        speedScalingCapped = FastMath.max(SURFACE_SPEED_CAP_FACTOR * surfaceSpeed(), speedScaling);
        switch (m) {
            case FOCUS_MODE:
                if (!focus.isEmpty() && !focus.isCoordinatesTimeOverflow() && focus.isFocusable()) {
                    double appMagCamera, appMagEarth;
                    synchronized (updateLock) {
                        focusBak = focus;
                        this.focus.getAbsolutePosition(aux4b);

                        // Hack, fix this by understanding underlying problem.
                        if (!aux4b.hasNaN()) {
                            focusPos.set(aux4b);
                        }
                        dx.set(0, 0, 0);

                        if (GaiaSky.settings().scene.camera.focusLock.position) {
                            // Get focus dx.
                            dx.set(nextFocusPosition)
                                    .sub(focusPos);

                            // Lock orientation.
                            if (GaiaSky.settings().scene.camera.focusLock.orientation && time.getHdiff() != 0 && focus.getOrientation() != null) {
                                if (focus.getRotationComponent() != null) {
                                    var rc = focus.getRotationComponent();
                                    // Rotation component present - planets, etc.
                                    double deltaAngle = rc.angle - previousRotationAngle;
                                    previousRotationAngle = rc.angle;
                                    // aux5 <- focus position.
                                    aux5b.set(focusPos);
                                    // aux3 <- focus to camera vector.
                                    aux3b.set(pos)
                                            .sub(aux5b);
                                    // aux2 <- spin axis.
                                    aux2.set(0, 1, 0)
                                            .mul(focus.getOrientation());
                                    // rotate aux3 around focus spin axis.
                                    aux3b.rotate(aux2, deltaAngle);
                                    // aux3 <- camera pos after rotating.
                                    aux3b.add(aux5b);
                                    // pos <- aux3.
                                    pos.set(aux3b);
                                    direction.rotate(aux2, deltaAngle);
                                    up.rotate(aux2, deltaAngle);
                                } else if (focus.getOrientationQuaternion() != null) {
                                    // Compute partial rotation from qPrev to q.
                                    var q = focus.getOrientationQuaternion();
                                    qPrev.mulInverse(q);
                                    qPrev.inverse();
                                    // aux5 <- focus (future) position.
                                    aux5b.set(aux4b);
                                    // aux3 <- focus to camera vector.
                                    aux3b.set(pos)
                                            .sub(aux5b);
                                    // aux3 <- orientation difference from last frame = aux * O * O'^-1.
                                    aux3b.mul(qPrev);
                                    // aux3 <- camera pos after rotating.
                                    aux3b.add(aux5b);
                                    // pos <- aux3.
                                    pos.set(aux3b);
                                    direction.mul(qPrev);
                                    up.mul(qPrev);
                                    // Set previous.
                                    qPrev.set(q);
                                }
                            }
                            // Add dx to camera position.
                            pos.add(dx);
                        }

                        // aux4b <- focus.abspos + dx.
                        aux4b.add(dx);

                        // Surface mode.
                        Vector3Q camObj = aux1b.set(aux4b)
                                .sub(pos);
                        // Store it in the field, so the rotation slowdown uses the same value.
                        this.distFromFocus = camObj.lenDouble();
                        camToFocusDir.set(camObj)
                                .nor();

                        // Surface mode activates when we're at 1.8 radii from the focus object, and it is a planet. Camera can't be tracking an object.
                        surfaceModeFlag.set(
                                !gamepadInput && !vr && !isTracking() && focus.isPlanet() && distFromFocus < focus.getRadius() * 2.5 / fovFactor);

                        if (!vr) {
                            if (!diverted && !surfaceModeFlag.get()) {
                                directionToTarget(dt,
                                                  aux4b,
                                                  GaiaSky.settings().scene.camera.turn / (GaiaSky.settings().scene.camera.cinematic ? 1e3f : 1e2f));
                            } else {
                                updateRotationFree(dt, GaiaSky.settings().scene.camera.turn);
                            }
                            updateRoll(dt, GaiaSky.settings().scene.camera.turn);
                        }

                        updatePosition(dt, speedScalingCapped, speedScaling);
                        updateRotation(dt, aux4b);

                        // Update focus direction.
                        if (surfaceModeFlag.get() && pointerCoordinatesFlag.get()) {
                            // In surface mode, we use the cartesian coordinates of the pointer on the surface of the planet.
                            // We only do this when approaching the planet, not so when getting away.
                            focusDirection.set(pointerCartesian)
                                    .nor();
                            pointerCoordinatesFlag.set(false);
                        } else {
                            // In normal mode, we just use the vector from camera to focus.
                            focusDirection.set(aux4b)
                                    .sub(pos)
                                    .nor();
                        }
                        focus = focusBak;

                        // Stop camera at radius distance.
                        var dist = aux4b.dstD(pos);
                        var radius = focus.getRadius();
                        if (focus.isSolidObject() && dist < radius) {
                            // aux2 <- focus-cam with a length of radius
                            aux2b.set(pos)
                                    .sub(aux4b)
                                    .nor()
                                    .scl(radius);
                            // Correct camera position
                            pos.set(aux4b)
                                    .add(aux2b);
                        }

                        // Track
                        if (!vr && isTracking()) {
                            // Track the tracking object
                            this.trackingObject.getPredictedPosition(aux5b, trackingName, time, false);
                            directionToTrackingObject(aux5b);
                        }

                        // Apparent magnitude from camera
                        appMagCamera = computeFocusApparentMagnitudeCamera();
                        // Apparent magnitude from Earth (planets, etc)
                        appMagEarth = computeFocusApparentMagnitudeEarth();
                    }

                    EventManager.publish(Event.FOCUS_INFO_UPDATED,
                                         this,
                                         focus.getDistToCamera() - focus.getRadius(),
                                         focus.getSolidAngle(),
                                         focus.getAlpha(),
                                         focus.getDelta(),
                                         focus.getAbsolutePosition(aux2b)
                                                 .lenDouble() - focus.getRadius(),
                                         appMagCamera,
                                         appMagEarth);
                } else {
                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                }
                break;
            case GAME_MODE:
                synchronized (updateLock) {
                    if (gravity && (closestBody.getEntity() != null) && closestBody.isPlanet() && !currentMouseKbdListener.isKeyPressed(
                            Input.Keys.SPACE)) {
                        // Add gravity to force, pulling to the closest body
                        Vector3Q camObj = closestBody.getAbsolutePosition(aux1b)
                                .sub(pos);
                        double dist = camObj.lenDouble();
                        // Gravity acts only at twice the radius, in planets
                        if (dist < closestBody.getRadius() * 2d) {
                            force.add(camObj.nor()
                                              .scl(0.002d));
                            fullStop = false;
                        } else {
                            fullStop = true;
                        }
                    } else {
                        fullStop = true;
                    }
                }
            case FREE_MODE:
                synchronized (updateLock) {
                    updatePosition(dt, speedScalingCapped, GaiaSky.settings().scene.camera.targetMode ? speedScaling : 1);
                    if (!vr) {
                        // If target is present, update direction
                        if (freeTargetOn) {
                            directionToTarget(dt,
                                              freeTargetPos,
                                              GaiaSky.settings().scene.camera.turn / (GaiaSky.settings().scene.camera.cinematic ? 1e3d : 1e2d));
                            if (facingFocus) {
                                freeTargetOn = false;
                            }
                        }

                        // Update direction with pitch, yaw, roll
                        updateRotationFree(dt, GaiaSky.settings().scene.camera.turn);
                        updateRoll(dt, GaiaSky.settings().scene.camera.turn);
                    }
                    updateLateral(dt, speedScalingCapped);
                }
                break;
            default:
                break;
        }

        // Update camera recorder
        EventManager.publish(Event.UPDATE_CAM_RECORDER, this, time, pos, direction, up);

        // Update actual camera
        lastFwdTime += dt;
        lastMode = m;

        posDistanceCheck();

        if (pos.hasNaN()) {
            pos.set(posBak);
        } else {
            posBak.set(pos);
        }

        // Real perspective camera.
        updatePerspectiveCamera();
    }

    /**
     * Does a pre-transformation to the camera to orient it using the given yaw, pitch and roll
     * angles.
     *
     * @param yaw   The yaw angle (to the right)
     * @param pitch The pitch angle (up)
     * @param roll  The roll angle (clockwise)
     */
    public void camOrientProjection(float yaw,
                                    float pitch,
                                    float roll) {
        if (projectionFlag) {
            // yaw - rotate to the right
            direction.rotate(up, -yaw);

            // pitch - rotate up
            aux1.set(direction)
                    .crs(up);
            direction.rotate(aux1, pitch);
            up.rotate(aux1, pitch);

            // roll - clockwise
            up.rotate(direction, roll);
            projectionFlag = false;
        }
    }

    /**
     * Sets the perspective camera float values from the computed double vectors, and
     * recalculates the camera matrices.
     */
    public void updatePerspectiveCamera() {
        camera.position.set(0f, 0f, 0f);
        camera.direction.set(direction.valuesF());
        camera.up.set(up.valuesF());
        camera.update();

        posInv.set(pos)
                .scl(-1);
    }

    /**
     * Adds a forward movement by the given amount.
     *
     * @param amount Positive for forward force, negative for backward force.
     */
    public void addForwardForce(double amount) {
        double tu = speedScaling();
        // Avoid getting stuck in surface mode: the forward speed must never fall below the
        // surface speed floor, in either direction.
        tu = FastMath.max(surfaceSpeed(), tu);
        if (parent.mode == CameraMode.FOCUS_MODE) {
            desired.set(focusDirection);
        } else {
            desired.set(direction);
        }

        desired.nor()
                .scl(amount * tu * (vr ? 10 : 100));
        force.add(desired);
        // We reset the time counter
        lastFwdTime = 0;
        lastFwdAmount = amount;
    }

    /**
     * Sets the velocity of the VR controller as a vector. The magnitude of this
     * vector should not be larger than 1
     *
     * @param p0      Start point of the beam
     * @param p1      End point of the beam
     * @param amountX Amount in the perpendicular direction of p0-p1
     * @param amountY Amount in the direction of p0-p1
     */
    public void setVelocityVR(Vector3D p0,
                              Vector3D p1,
                              double amountX,
                              double amountY) {
        if (getMode() == CameraMode.FOCUS_MODE) {
            setVelocity(amountY);
        } else {
            velocityVR0 = p0;
            velocityVR1 = p1;
            velocityVRX = amountX;
            velocityVRY = amountY;
        }
    }

    /**
     * Clears the velocityVR vector
     */
    public void clearVelocityVR() {
        setVelocity(0);
        velocityVR0 = null;
        velocityVR1 = null;
        velocityVRX = 0;
        velocityVRY = 0;
    }

    public void forward(double amount) {
        forward(amount, 0);
    }

    public void forward(double amount,
                        double minTu) {
        double speedScaling = speedScaling(minTu);
        desired.set(direction)
                .nor()
                .scl(amount * speedScaling);
        // Capped at the speed scale of this frame, not at a fixed magnitude in internal units: a fixed
        // cap is a different physical speed at every distance scale factor, and at 1e6 it would hold
        // the camera below a walking pace across the extragalactic sky.
        vel.add(desired)
                .clamp(0, FastMath.max(speedScaling, surfaceSpeed()));
        lastFwdTime = 0;
    }

    public void strafe(double amount,
                       double minTu) {
        double speedScaling = speedScaling(minTu);
        desired.set(direction)
                .crs(up)
                .nor()
                .scl(amount * speedScaling);
        // Capped at the speed scale of this frame, not at a fixed magnitude in internal units: a fixed
        // cap is a different physical speed at every distance scale factor, and at 1e6 it would hold
        // the camera below a walking pace across the extragalactic sky.
        vel.add(desired)
                .clamp(0, FastMath.max(speedScaling, surfaceSpeed()));
        lastFwdTime = 0;
    }

    public void vertical(double amount) {
        vertical(amount, 0);
    }

    public void vertical(double amount,
                         double minTu) {
        double speedScaling = speedScaling(minTu);
        desired.set(up)
                .nor()
                .scl(amount * speedScaling);
        // Capped at the speed scale of this frame, not at a fixed magnitude in internal units: a fixed
        // cap is a different physical speed at every distance scale factor, and at 1e6 it would hold
        // the camera below a walking pace across the extragalactic sky.
        vel.add(desired)
                .clamp(0, FastMath.max(speedScaling, surfaceSpeed()));
        lastFwdTime = 0;
    }

    /**
     * Adds a rotation force to the camera. DeltaX corresponds to yaw (right/left)
     * and deltaY corresponds to pitch (up/down).
     *
     * @param deltaX              The yaw amount.
     * @param deltaY              The pitch amount.
     * @param focusLookKeyPressed The key to look around when on focus mode is
     *                            pressed.
     */
    public void addRotateMovement(double deltaX,
                                  double deltaY,
                                  boolean focusLookKeyPressed,
                                  boolean acceleration) {
        // Just update yaw with X and pitch with Y
        if (parent.mode.equals(CameraMode.FREE_MODE)) {
            deltaX *= fovFactor;
            deltaY *= fovFactor;
            addYaw(deltaX, acceleration);
            addPitch(deltaY, acceleration);
        } else if (parent.mode.equals(CameraMode.FOCUS_MODE)) {

            if (focusLookKeyPressed) {
                diverted = true;
                addYaw(deltaX * fovFactor, acceleration);
                addPitch(deltaY * fovFactor, acceleration);
            } else {
                double radius = focus.isSolidObject() ? focus.getRadius() : 0;
                // Use the same distance source as the surface mode flag, so that both use the
                // freshly computed distance in the current frame.
                double distToFocus = this.distFromFocus >= 0 ? this.distFromFocus : focus.getDistToCamera();
                double distanceInRadii = radius > 0 ? getFovFactor() * (distToFocus - radius) / radius : Double.MAX_VALUE;
                // Slow down as the focus fills the screen, but never stop being able to turn.
                double factor = (distanceInRadii < ROTATION_SLOWDOWN_RADII)
                        ? FastMath.max(distanceInRadii / ROTATION_SLOWDOWN_RADII, ROTATION_MIN_FACTOR)
                        : 1.0;
                // This factor slows the rotation as the focus gets closer and closer
                addHorizontal(deltaX * factor, acceleration);
                addVertical(deltaY * factor, acceleration);
            }
        }
    }

    /**
     * Adds the given amount of angular rate to the given angle vector. The {@code acceleration}
     * flag is ignored: the amount is always a requested rate, which {@link
     * #updatePosition(Vector3D, double)} ramps towards and decays back to rest when the input
     * stops.
     *
     * @param vec          The angle vector ({@code x} = requested rate, {@code y} = current rate,
     *                     {@code z} = rotation this frame).
     * @param amount       The requested angular rate.
     * @param acceleration Unused, kept for backwards compatibility.
     */
    public void addAmount(Vector3D vec,
                          double amount,
                          boolean acceleration) {
        vec.x = amount;
        rotationIdleTime = 0;
    }

    /**
     * Adds the given amount to the camera yaw acceleration
     **/
    public void addYaw(double amount,
                       boolean acceleration) {
        addAmount(yaw, amount, acceleration);
    }

    public void setYaw(double amount) {
        // A zero amount means the input has been released, so the current rate is left alone and
        // updatePosition() damps it to a halt. Anything else is applied directly.
        yaw.x = amount;
        if (amount != 0) {
            yaw.y = amount;
        }
        rotationIdleTime = 0;
    }

    /**
     * Adds the given amount to the camera pitch acceleration
     **/
    public void addPitch(double amount,
                         boolean acceleration) {
        addAmount(pitch, amount, acceleration);
    }

    public void setPitch(double amount) {
        // A zero amount means the input has been released, so the current rate is left alone and
        // updatePosition() damps it to a halt. Anything else is applied directly.
        pitch.x = amount;
        if (amount != 0) {
            pitch.y = amount;
        }
        rotationIdleTime = 0;
    }

    /**
     * Adds the given amount to the camera roll acceleration
     **/
    public void addRoll(double amount,
                        boolean acceleration) {
        addAmount(roll, amount, acceleration);
    }

    public void setRoll(double amount) {
        // A zero amount means the input has been released, so the current rate is left alone and
        // updatePosition() damps it to a halt. Anything else is applied directly.
        roll.x = amount;
        if (amount != 0) {
            roll.y = amount;
        }
        rotationIdleTime = 0;
    }

    /**
     * Adds the given amount to camera horizontal rotation around the focus
     * acceleration, or pan in free mode
     **/
    public void addHorizontal(double amount,
                              boolean acceleration) {
        addAmount(horizontal, amount, acceleration);
    }

    public void setHorizontal(double amount) {
        horizontal.x = amount * fovFactor;
        horizontal.y = amount * fovFactor;
        rotationIdleTime = 0;
    }

    /**
     * Adds the given amount to camera vertical rotation around the focus
     * acceleration, or pan in free mode
     **/
    public void addVertical(double amount,
                            boolean acceleration) {
        addAmount(vertical, amount, acceleration);
    }

    public void setVertical(double amount) {
        vertical.x = amount * fovFactor;
        vertical.y = amount * fovFactor;
        rotationIdleTime = 0;
    }

    /**
     * Stops the camera movement.
     *
     * @return True if the camera had any movement at all and it has been stopped.
     * False if camera was already still.
     */
    public boolean stopMovement() {
        boolean stopped = (vel.len2() != 0 || yaw.x != 0 || yaw.y != 0 || pitch.x != 0 || pitch.y != 0 || roll.x != 0 || roll.y != 0
                || vertical.x != 0 || vertical.y != 0 || horizontal.x != 0 || horizontal.y != 0);
        force.setZero();
        vel.setZero();
        yaw.setZero();
        pitch.setZero();
        roll.setZero();
        horizontal.setZero();
        vertical.setZero();
        rotationIdleTime = Double.MAX_VALUE;
        return stopped;
    }

    /**
     * Releases the current user input: the translation is stopped right away, but the rotation is
     * only left to decay smoothly to a stop by {@link #updatePosition(Vector3D, double)}.
     * <p>
     * This is what the mouse listener calls when the button is released, so that the camera keeps
     * turning for a moment and slows down, instead of stopping abruptly. In cinematic mode nothing
     * is damped and the camera simply keeps going.
     *
     * @return True if the camera had any movement at all and it has been released.
     * False if the camera was already still.
     */
    public boolean releaseInput() {
        boolean stopped = (vel.len2() != 0 || yaw.x != 0 || yaw.y != 0 || pitch.x != 0 || pitch.y != 0 || roll.x != 0 || roll.y != 0
                || vertical.x != 0 || vertical.y != 0 || horizontal.x != 0 || horizontal.y != 0);

        force.setZero();
        vel.setZero();

        if (!stopsOnRelease()) {
            // Cinematic mode: the camera keeps rotating, so nothing is reset at all.
            return stopped;
        }

        // Forget the requested rates only. The current rates (y) are left in place so that the
        // rotation is damped to a halt over ROTATION_DECAY_TAU instead of stopping abruptly.
        yaw.x = 0;
        pitch.x = 0;
        roll.x = 0;
        horizontal.x = 0;
        vertical.x = 0;
        rotationIdleTime = Double.MAX_VALUE;

        return stopped;
    }

    /**
     * Stops the camera movement.
     */
    public void stopTotalMovement() {
        force.setZero();
        vel.setZero();
        yaw.setZero();
        pitch.setZero();
        roll.setZero();
        horizontal.setZero();
        vertical.setZero();
        rotationIdleTime = Double.MAX_VALUE;
    }

    /**
     * Releases the current rotation input. The requested rates are forgotten, but the camera is
     * left to decay smoothly to a halt by {@link #updatePosition(Vector3D, double)} rather than
     * stopping abruptly. Does nothing in cinematic mode.
     */
    public void releaseRotateInput() {
        if (!stopsOnRelease()) {
            // Cinematic mode: the camera keeps rotating, so nothing is reset at all.
            return;
        }

        yaw.x = 0;
        pitch.x = 0;
        roll.x = 0;
        horizontal.x = 0;
        vertical.x = 0;
        rotationIdleTime = Double.MAX_VALUE;
    }

    public void stopRotateMovement() {
        yaw.setZero();
        pitch.setZero();
        horizontal.setZero();
        vertical.setZero();
        rotationIdleTime = Double.MAX_VALUE;
    }

    /**
     * Stops the camera movement.
     */
    public void stopForwardMovement() {
        force.setZero();
        vel.setZero();
    }

    protected void updateVelocity(double dt,
                                  double multiplier,
                                  double speedScaling) {

        boolean cinematic = GaiaSky.settings().scene.camera.cinematic;
        // Calculate velocity if coming from gamepad
        if (velocityGamepad != 0) {
            vel.set(direction)
                    .nor()
                    .scl(velocityGamepad * multiplier);
        } else if (velocityVRX != 0 || velocityVRY != 0) {
            aux1.set(velocityVR1)
                    .sub(velocityVR0)
                    .nor();

            // p0-p1 direction (Y)
            vel.set(aux1)
                    .scl(velocityVRY * multiplier);

            // cross(p0,p1) direction (X)
            aux1.crs(up)
                    .nor()
                    .scl(velocityVRX * multiplier);
            vel.add(aux1);
        }

        double forceLen = force.lenDouble();
        double velocity = vel.len();

        boolean noGamepadVrInput = velocityGamepad == 0 && velocityVRX == 0 && velocityVRY == 0;
        // No forward input has been received for a while, so the camera must come to a stop.
        boolean inputReleased = stopsOnRelease() && noGamepadVrInput && lastFwdTime > FORWARD_RESPONSE_TIME && fullStop;

        if (inputReleased && velocity > 0) {
            // Damp the velocity exponentially towards rest. Without this the camera keeps coasting
            // forever once the input is released.
            vel.scl(1d - smoothingAlpha(dampingTau(STOP_DAMPING_TAU), dt));
        }

        // Half a second after we have stopped zooming, real friction kicks in
        if (fullStop && focus.isValid()) {
            double elevation = focus.getElevationAt(pos);
            double counterAmount = lastFwdAmount < 0 && cinematic ? FastMath.min(speedScaling, 200) : 2;
            if (getMode().isFocus() && lastFwdAmount > 0) {
                // Extra braking when moving towards the body. This must stay bounded, otherwise the
                // factor diverges as the altitude goes to zero and the camera gets stuck on the surface.
                double altitude = FastMath.max(focus.getDistToCamera() - elevation, elevation * MIN_ALTITUDE_RATIO);
                double ratio = elevation / altitude;
                counterAmount *= FastMath.min(ratio, MAX_DAMPING_BOOST);
            }
            // The last term applies a greater scale when the direction and velocity vector face in the same general direction.
            double scl = -velocity * counterAmount * dt;
            if (Double.isFinite(scl)) {
                friction.set(vel)
                        .nor()
                        .scl(scl);
            } else {
                // Never keep a stale friction vector around.
                friction.setZero();
            }
        } else {
            friction.set(force)
                    .nor()
                    .scl(-forceLen * dt);
        }

        force.add(friction);

        // Stop outright once the damping has had time to bring the camera to a complete rest.
        if (stopsOnRelease() && noGamepadVrInput && lastFwdTime > FORWARD_STOP_TIME && fullStop || lastFwdAmount > 0 && speedScaling == 0) {
            stopForwardMovement();
        }

        if (thrust != 0)
            force.add(thrust)
                    .scl(thrustDirection);
        applyForce(force);

        if (!(force.isZero() && velocity == 0 && accel.isZero())) {
            vel.add(accel.scl(dt))
                    .scl(speedMultiplier);

            // Clamp to top speed
            if (GaiaSky.settings().scene.camera.speedLimit > 0 && vel.len() > GaiaSky.settings().scene.camera.speedLimit) {
                vel.clamp(0, GaiaSky.settings().scene.camera.speedLimit);
            }

            // Velocity changed direction
            if (lastVel.dot(vel) < 0) {
                vel.setZero();
            }

            velocity = vel.len();

            if (parent.mode.equals(CameraMode.FOCUS_MODE)) {
                // Use direction vector as velocity so that if we turn the
                // velocity also turns
                double sign = FastMath.signum(vel.dot(focusDirection));
                vel.set(focusDirection)
                        .nor()
                        .scl(sign * velocity);
            }

            vel.clamp(0, multiplier * speedMultiplier);

            // While the speed-up multiplier is active a single step must not carry the camera past the
            // focus object, or it crosses it and the radius clamp parks it on the far side. Solid
            // objects keep one radius of clearance (two radii from the centre) so the focus stays in
            // view. Without the multiplier none of this applies: the regular radius clamp already
            // prevents going below the surface, and landing has to work.
            if (speedMultiplier > 1 && parent.mode.equals(CameraMode.FOCUS_MODE) && distFromFocus > 0 && dt > 0) {
                double stopDist = focus.isSolidObject() ? focus.getRadius() * 2.0 : focus.getRadius();
                double remaining = distFromFocus - stopDist;
                double approach = vel.dot(camToFocusDir);
                if (approach > 0) {
                    if (remaining > 0 && approach * dt > remaining) {
                        // The step would cross the stop sphere. Prevent the overshoot by scaling the
                        // whole velocity so the camera lands exactly on it: killing the velocity
                        // instead would leave the camera a full step away (which, at speed-up speeds,
                        // is a large fraction of the way), where it would then re-accelerate and
                        // stall in a jitter. Scaling never reverses the velocity, so nothing bounces.
                        vel.scl(remaining / (approach * dt));
                        // The velocity is huge here (one step), and the focus direction it is projected
                        // onto on the next frame can change (in surface mode it is the pointer
                        // direction), which would send the camera back at full speed. Wipe it out once
                        // the step is taken.
                        stopAfterStep = true;
                    } else if (remaining <= 0) {
                        // Already inside the clearance band (landed, surface mode, or an object
                        // without a solid clamp). Forbid getting closer, but let it move away and
                        // sideways.
                        vel.add(aux2.set(camToFocusDir)
                                        .scl(-approach));
                    }
                }
            }
        }
    }

    /**
     * Updates the position of this entity using the current force
     */
    protected void updatePosition(double dt,
                                  double multiplier,
                                  double speedScaling) {

        stopAfterStep = false;
        updateVelocity(dt, multiplier, speedScaling);
        // Aux1 is the step to take
        aux1b.set(vel)
                .scl(dt);
        // Aux2 contains the new position
        pos.add(aux1b);

        if (stopAfterStep) {
            // The camera has just landed on the focus clearance sphere: kill all motion so it stays
            // there. Without this the velocity of that last step survives, and the next frame it is
            // re-projected onto the current focus direction (the pointer direction in surface mode),
            // which shoots the camera away from the focus.
            vel.setZero();
        }

        accel.setZero();

        lastVel.set(vel);
        force.setZero();
        posInv.set(pos)
                .scl(-1);
    }

    /**
     * Runs some checks on the computed position. It checks that the current position does not
     * collide with the terrain of the closest body, and it checks that the position is not further
     * than the maximum allowed distance.
     */
    private void posDistanceCheck() {
        // Check terrain collision.
        if (!closestBody.isEmpty()) {
            // Future position of closest object.
            aux5b.set(nextClosestPosition);

            double elevation =
                    closestBody.getElevationAt(pos, aux5b) + closestBody.getHeightScale() / FastMath.max(4.0,
                                                                                                         20.0 - GaiaSky.settings().scene.renderer.elevation.multiplier);
            double newDist = aux5b.scl(-1)
                    .add(pos)
                    .lenDouble();
            if (newDist < elevation) {
                aux5b.nor()
                        .scl(elevation - newDist);
                pos.add(aux5b);
                posInv.set(pos)
                        .scl(-1);
            }
        }

        // Check maximum allowed distance: 50 Gpc from the Sun.
        if (pos.lenDouble() >= MAX_ALLOWED_DISTANCE) {
            pos.clamp(0, MAX_ALLOWED_DISTANCE);
            posInv.set(pos)
                    .scl(-1);
        }
    }

    /**
     * Updates the rotation for the free camera.
     */
    private void updateRotationFree(double dt,
                                    double rotateSpeed) {
        // Add position to compensate for coordinates centered on camera
        if (updatePosition(pitch, dt)) {
            // Pitch
            aux1.set(direction)
                    .crs(up)
                    .nor();
            rotate(aux1, pitch.z * rotateSpeed * movementMultiplier);
        }
        if (updatePosition(yaw, dt)) {
            // Yaw
            rotate(up, -yaw.z * rotateSpeed * movementMultiplier);
        }

    }

    private void updateRoll(double dt,
                            double rotateSpeed) {
        if (updatePosition(roll, dt)) {
            // Roll
            rotate(direction, -roll.z * rotateSpeed * movementMultiplier);
        }
    }

    /**
     * Updates the direction vector using the pitch, yaw and roll forces.
     */
    private void updateRotation(double dt,
                                Vector3Q rotationCenter) {
        // Add position to compensate for coordinates centered on camera
        // rotationCenter.add(pos);
        if (updatePosition(vertical, dt)) {
            // Pitch
            aux1.set(direction)
                    .crs(up)
                    .nor();
            rotateAround(rotationCenter, aux1, vertical.z * GaiaSky.settings().scene.camera.rotate * movementMultiplier);
        }
        if (updatePosition(horizontal, dt)) {
            // Yaw
            rotateAround(rotationCenter, up, -horizontal.z * GaiaSky.settings().scene.camera.rotate * movementMultiplier);
        }


    }

    private void updateLateral(double dt,
                               double translateUnits) {
        // Pan with hor
        aux1.set(direction)
                .crs(up)
                .nor();
        aux1.scl(horizontal.y * translateUnits * movementMultiplier);
        aux2.set(up)
                .nor()
                .scl(vertical.y * translateUnits * movementMultiplier);
        aux1.add(aux2);
        if (GaiaSky.settings().scene.camera.speedLimit > 0 && aux1.len() > GaiaSky.settings().scene.camera.speedLimit) {
            aux1.clamp(0, GaiaSky.settings().scene.camera.speedLimit);
        }

        if (dt > 0) {
            translate(aux1.scl(dt));
        }

    }

    /**
     * Whether the camera comes to a full stop on its own once the input is released. This is the
     * case in the regular (non cinematic) mode. In cinematic mode the camera keeps rotating and
     * keeps moving inwards and outwards indefinitely, and only stops when explicitly requested.
     *
     * @return True if the camera is damped to a stop when the input is released.
     */
    private boolean stopsOnRelease() {
        return !GaiaSky.settings().scene.camera.cinematic;
    }

    /**
     * Updates the given angular rate vector of an angle using dt.
     * <p>
     * The vector holds three components: {@code x} is the angular rate requested by the input
     * listeners, {@code y} is the rate the camera is actually turning at, and {@code z} is the
     * rotation to apply this frame, i.e. {@code y} times {@code dt}.
     * <p>
     * The rate ramps up towards the requested value using a time constant, so the resulting
     * angular speed is framerate independent. This used to be an acceleration integrated as
     * {@code y += x * dt}, which made the rotation grow without bound and depend on the framerate.
     * <p>
     * When the input stops, the rate decays back to rest exponentially if {@link
     * #stopsOnRelease()}, or is held at its last value in cinematic mode, where the camera keeps
     * rotating indefinitely.
     */
    private boolean updatePosition(Vector3D angle,
                                   double dt) {
        if (angle.x != 0 || angle.y != 0) {
            boolean stopOnRelease = stopsOnRelease();

            // Forget the requested rate once the input has stopped. The input listeners keep their
            // last value around, so without this the camera would rotate indefinitely after the
            // mouse is released. Gamepad axes only report a change, so a held stick would be seen
            // as "idle" here; the gamepad input flag disables this until the stick reports again.
            // Not done in cinematic mode, where the rotation is deliberately kept.
            if (stopOnRelease && !gamepadInput && rotationIdleTime > ROTATION_INPUT_GRACE) {
                angle.x = 0;
            }

            // Requested angular rate. Once the input stops, the rate either decays to rest or is
            // held at its last value, depending on the mode.
            double target = angle.x;
            double tau;
            if (target != 0) {
                tau = ROTATION_ACCEL_TAU;
            } else if (stopOnRelease) {
                tau = dampingTau(ROTATION_DECAY_TAU);
            } else {
                // Cinematic mode: the camera keeps rotating at the last requested rate.
                tau = ROTATION_HOLD_TAU;
            }
            angle.y += (target - angle.y) * smoothingAlpha(tau, dt);

            // Snap to a full stop, otherwise the exponential decay leaves an endless crawl.
            if (stopOnRelease && target == 0 && Math.abs(angle.y) < ROTATION_MIN_RATE) {
                angle.y = 0;
            }

            // Rotation to apply this frame.
            angle.z = angle.y * dt;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the camera direction and up vectors with a gentle turn towards the
     * given target.
     *
     * @param dt           The current time step
     * @param target       The position of the target
     * @param turnVelocity The velocity at which to turn
     */
    private void directionToTarget(double dt,
                                   Vector3Q target,
                                   double turnVelocity) {
        desired.set(target)
                .sub(pos)
                .nor();
        double desiredDirectionAngle = desired.angle(direction);
        if (desiredDirectionAngle > FastMath.min(0.3, 0.3 * fovFactor)) {
            // Add desired to direction with given turn velocity (v*dt)
            desired.scl(turnVelocity * dt * movementMultiplier);
            direction.add(desired)
                    .nor();

            // Update up so that it is always perpendicular
            aux1.set(direction)
                    .crs(up);
            up.set(aux1)
                    .crs(direction)
                    .nor();
            facingFocus = false;
        } else {
            facingFocus = true;
        }
    }

    /**
     * Updates the camera direction and up vectors so that they track the given target instantly.
     *
     * @param target The position of the target
     */
    private void directionToTrackingObject(Vector3Q target) {
        desired.set(target)
                .sub(pos)
                .nor();
        direction.set(desired);
        aux1.set(direction)
                .crs(up);
        up.set(aux1)
                .crs(direction)
                .nor();
        facingFocus = false;
    }

    private void setMouseKbdListener(AbstractMouseKbdListener newListener) {
        InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
        // Remove from input processors
        if (currentMouseKbdListener != null) {
            im.removeProcessor(currentMouseKbdListener);

            // Deactivate
            currentMouseKbdListener.deactivate();
        }

        // Update reference
        currentMouseKbdListener = newListener;

        // Add to input processors
        im.addProcessor(currentMouseKbdListener);

        // Activate
        currentMouseKbdListener.activate();
    }

    /**
     * Updates the camera mode.
     */
    @Override
    public void updateMode(ICamera previousCam,
                           CameraMode previousMode,
                           CameraMode newMode,
                           boolean centerFocus) {
        InputProcessor ip = Gdx.input.getInputProcessor();
        if (ip instanceof InputMultiplexer im) {
            switch (newMode) {
                case FOCUS_MODE:
                    diverted = !centerFocus;
                    checkFocus();
                case FREE_MODE:
                case GAME_MODE:
                    AbstractMouseKbdListener newListener = newMode == CameraMode.GAME_MODE ? gameMouseKbdListener : naturalMouseKbdListener;
                    setMouseKbdListener(newListener);
                    addGamepadListener();
                    if (vr) {
                        GaiaSky.instance.xrDriver.addListener(openXRListener);
                    }
                    break;
                default:
                    // Unregister input controllers.
                    im.removeProcessor(currentMouseKbdListener);
                    removeGamepadListener();
                    // Remove vr listener.
                    if (vr) {
                        GaiaSky.instance.xrDriver.removeListener(openXRListener);
                    }
                    break;
            }
        }
    }

    public MainGamepadListener getGamepadListener() {
        return gamepadListener;
    }

    public void addGamepadListener() {
        GaiaSky.settings().controls.gamepad.addControllerListener(gamepadListener);
    }

    public void removeGamepadListener() {
        GaiaSky.settings().controls.gamepad.removeControllerListener(gamepadListener);
    }

    public void setFocus(String focusName,
                         Entity newFocus) {
        if (newFocus != null && GaiaSky.instance.isOn(Mapper.base.get(newFocus).ct)) {
            focus.setEntity(newFocus);
            focus.getFocus(focusName != null ? focusName : focus.getCandidateName());
            focus.makeFocus();
            // Reset facing focus.
            facingFocus = false;
            // Create event to notify focus change.
            EventManager.publish(Event.FOCUS_CHANGED, this, focus);
        }
    }

    /**
     * Blends the closest-star distance into the distance the camera would use without a star
     * nearby. A blend of 0 returns the star distance, a blend of 1 the regular distance, so each
     * star only affects the speed inside its own bubble.
     * <p>
     * The mix is geometric, not linear: the two distances routinely differ by many orders of
     * magnitude (a star radius against a distance of light years), and a linear mix would be
     * dominated by the larger one and jump as soon as the blend leaves 0.
     *
     * @param starDistance    Distance to the closest star, in internal units.
     * @param regularDistance Distance used when no star is nearby, in internal units.
     * @param blend           Blend factor, in [0, 1].
     *
     * @return The blended distance, in internal units.
     */
    static double blendStarDistance(double starDistance,
                                    double regularDistance,
                                    double blend) {
        if (!Double.isFinite(starDistance) || starDistance <= 0) {
            return regularDistance;
        }
        if (!Double.isFinite(regularDistance) || regularDistance <= 0) {
            return starDistance;
        }
        if (blend <= 0) {
            return starDistance;
        }
        if (blend >= 1) {
            return regularDistance;
        }
        return FastMath.exp(Math.log(starDistance) * (1 - blend) + Math.log(regularDistance) * blend);
    }

    /**
     * Interpolation factor between the low and high distance limits: 0 below the low limit, 1 above
     * the high one, linear in between.
     *
     * @param distance The distance to interpolate.
     * @param low      The low limit.
     * @param high     The high limit.
     *
     * @return The interpolation factor, in [0, 1].
     */
    static double starDistanceBlend(double distance,
                                    double low,
                                    double high) {
        if (!Double.isFinite(distance) || !Double.isFinite(low) || !Double.isFinite(high) || high <= low) {
            return distance >= high ? 1d : 0d;
        }
        if (distance <= low) {
            return 0d;
        }
        if (distance >= high) {
            return 1d;
        }
        return (distance - low) / (high - low);
    }

    /**
     * Sentinel for "no object to measure against this frame", so that it can never be mistaken for a
     * real measurement: see {@link #updateSmoothedDistances(double)}, which holds the previous
     * distance instead of feeding this through the filter.
     * <p>
     * Infinite, and not a large finite distance. A sentinel is compared against distances in
     * internal units, so its value in physical terms moves with the distance scale factor: a fixed
     * 1e20 units sat at 3.2 Gpc with the default factor, but only at 3.2 Mpc with 1e6, well inside
     * the range of real measurements and below the saturation distance of the curve. Everything past
     * the local group then read as "no reference", the smoothed distance stopped moving and the
     * camera crawled across the extragalactic sky at whatever speed it had near the last star.
     **/
    static final double NO_CLOSEST_BODY_DISTANCE = Double.POSITIVE_INFINITY;

    /**
     * Converts a body's distance to the camera and elevation into the distance the speed scaling uses.
     * Non-finite inputs yield {@link #NO_CLOSEST_BODY_DISTANCE}: a NaN here reaches the velocity clamp,
     * where every comparison is false and the camera stops translating altogether.
     *
     * @param distToCamera Distance from the camera to the body.
     * @param elevation    Elevation of the camera above the body's surface.
     * @param billboard    Whether the body is a billboard, which needs extra clearance.
     *
     * @return The distance used for the speed scaling.
     */
    static double closestBodyDistance(double distToCamera,
                                      double elevation,
                                      boolean billboard) {
        if (!Double.isFinite(distToCamera) || !Double.isFinite(elevation)) {
            return NO_CLOSEST_BODY_DISTANCE;
        }
        double distance = distToCamera - (elevation + minDist());
        if (!Double.isFinite(distance)) {
            return NO_CLOSEST_BODY_DISTANCE;
        }
        return distance * (billboard ? 15.0 : 1.0);
    }

    /**
     * For bodies, we just get the distance minus the elevation plus the minimum distance.
     *
     * @return The raw (unsmoothed) distance to the closest body.
     */
    private double getClosestBodyDistance() {
        if (closestBody == null || !closestBody.isValid()) {
            return NO_CLOSEST_BODY_DISTANCE;
        }
        return closestBodyDistance(closestBody.getDistToCamera(),
                                   closestBody.getElevationAt(pos, false),
                                   closestBody.isBillboard());
    }

    /**
     * Computes the raw (unsmoothed) distances used for the speed scaling. The result is cached,
     * so this method may be safely called multiple times per frame.
     *
     * @return The raw (unsmoothed) minimum distance.
     */
    private double computeRawDistances() {
        if (rawDistancesValid) {
            return rawDistance;
        }

        rawBodyDistance = getClosestBodyDistance();

        var focusDistance = getMode().isFocus() && focus != null ? focus.getDistToCamera() : Double.MAX_VALUE;

        // Distance the camera uses with no star nearby: what it uses with the star distance
        // scaling off entirely. Anything past DIST_SMOOTH_UP maps to the same speed, so the
        // saturation distance is used as the reference there instead of infinity: otherwise the
        // star would be blended against an almost infinite distance and could never slow the
        // camera down in empty space, which is precisely where it has to.
        regularDistance = FastMath.min(DIST_SMOOTH_UP, FastMath.min(rawBodyDistance, focusDistance));

        rawStarDistance = getClosestStarDistance(regularDistance);

        rawDistance = FastMath.min(rawStarDistance, regularDistance);
        rawDistancesValid = true;

        return rawDistance;
    }

    /**
     * Computes the frame-rate independent exponential smoothing factor for a given time constant.
     *
     * @param tau The time constant, in seconds.
     * @param dt  The frame delta time, in seconds.
     *
     * @return The smoothing factor in [0, 1].
     */
    private static double smoothingAlpha(double tau,
                                         double dt) {
        if (tau <= 0 || dt <= 0) {
            return 1d;
        }
        double alpha = 1d - FastMath.exp(-dt / tau);
        // Guard against numerical drift.
        return Math.clamp(alpha, 0d, 1d);
    }

    /**
     * Scales a damping time constant with the camera damping factor setting, so that the user can
     * control how long the camera keeps moving after the input has been released. It has no effect
     * in cinematic mode, where nothing is damped anyway.
     *
     * @param tau The base time constant, in seconds.
     *
     * @return The scaled time constant, in seconds.
     */
    private double dampingTau(double tau) {
        double factor = GaiaSky.settings().scene.camera.dampingFactor;
        if (!Double.isFinite(factor) || factor < 0d) {
            return tau;
        }
        return tau * factor;
    }

    /**
     * Advances the exponential low-pass filter of the raw distance. This must be called exactly
     * once per frame, so that the filtering does not depend on the number of times
     * {@link #speedScaling(double)} is invoked.
     *
     * @param dt The frame delta time, in seconds.
     */
    private void updateSmoothedDistances(double dt) {
        double raw = computeRawDistances();

        // A frame with no object to measure against is "unknown", not "infinitely far". Feeding a
        // huge distance through the filter multiplied the speed - which is linear in this distance -
        // by the ratio between it and the last real reference in a single frame, and the exponential
        // smoothing cannot reject it, it can only decay it, which takes seconds at the top speed.
        // Bodies are only registered as the closest one when they are extracted, i.e. when they are
        // actually drawn (ModelExtractor, ParticleExtractor), so this happens routinely when leaving
        // a system and the bodies fade out or are culled. Holding the last value keeps the speed
        // where it was.
        if (!Double.isFinite(raw)) {
            if (smoothedDistance < 0 || !Double.isFinite(smoothedDistance)) {
                smoothedDistance = 0;
            }
            return;
        }

        // Everything past the saturation point maps to the same speed, so keeping magnitudes that
        // large in the filter serves no purpose.
        raw = FastMath.min(raw, DIST_SMOOTH_UP);

        if (smoothedDistance < 0 || !Double.isFinite(smoothedDistance)) {
            // First frame: initialize.
            smoothedDistance = raw;
            return;
        }

        double alpha = smoothingAlpha(DISTANCE_SMOOTHING_TAU, dt);
        double next = smoothedDistance + (raw - smoothedDistance) * alpha;

        // Only a discontinuity in the raw distance means that the reference changed. A gradual one
        // means that the very same reference is getting farther away, so the camera is simply flying
        // out of its neighbourhood and the speed has to follow, not crawl up to it.
        double rate = raw > smoothedDistance * REFERENCE_JUMP_FACTOR
                ? DISTANCE_MAX_GROWTH_RATE
                : DISTANCE_GROWTH_RATE;

        smoothedDistance = limitGrowth(smoothedDistance, next, dt, rate);
    }

    /**
     * Bounds the relative growth of a smoothed distance. The speed scaling is linear in this
     * distance, so bounding its growth bounds the growth of the speed. The decay is deliberately
     * left unbounded - slowing down when something gets closer has to be immediate.
     *
     * @param current The current smoothed distance.
     * @param next    The distance the filter wants to move to.
     * @param dt      The frame delta time, in seconds.
     * @param rate    Maximum growth, in e-folds per second.
     *
     * @return The next distance, with its growth limited.
     */
    static double limitGrowth(double current,
                              double next,
                              double dt,
                              double rate) {
        if (dt <= 0 || current <= 0 || !Double.isFinite(current) || !Double.isFinite(next)) {
            return next;
        }
        double byRate = current * FastMath.exp(rate * dt);
        double byFrame = current * DISTANCE_MAX_FRAME_RATIO;
        return FastMath.min(next, FastMath.min(byRate, byFrame));
    }

    /**
     * Radius multiplier at which the closest-star distance starts blending in. Below
     * {@code starRadius * this} the star distance is used as is.
     */
    private double starDistanceScaleLow = 5.0E4;
    /**
     * Radius multiplier at which the closest-star distance has fully faded out. Above
     * {@code starRadius * this} the closest star no longer influences the distance.
     */
    private double starDistanceScaleHigh = 4.0E7;

    public double getStarDistanceScaleLow() {
        return starDistanceScaleLow;
    }

    public void setStarDistanceScaleLow(double starDistanceScaleLow) {
        if (Double.isFinite(starDistanceScaleLow) && starDistanceScaleLow > 0) {
            this.starDistanceScaleLow = starDistanceScaleLow;
        }
    }

    public double getStarDistanceScaleHigh() {
        return starDistanceScaleHigh;
    }

    public void setStarDistanceScaleHigh(double starDistanceScaleHigh) {
        if (Double.isFinite(starDistanceScaleHigh) && starDistanceScaleHigh > 0) {
            this.starDistanceScaleHigh = starDistanceScaleHigh;
        }
    }

    /**
     * Distance to the closest star, blended into the regular distance with the low and high radius
     * multipliers. Below the low limit the star distance is used as is, above the high limit it is
     * ignored, and in between the two are mixed: each star slows the camera down only inside its
     * own bubble.
     *
     * @param regularDistance The distance the camera uses when no star is nearby, in internal units.
     *
     * @return The blended distance, or {@link Double#MAX_VALUE} if the star distance is not to be
     * used at all.
     */
    private double getClosestStarDistance(double regularDistance) {
        if (getMode().isFocus() || !GaiaSky.settings().scene.camera.starDistanceScaling) {
            // Do not use star distance to compute velocity scaling.
            // Occasionally, traversing the star field with speed scaling depending on the closest star is actually not good.
            // Speed jumps after metadata updates are too noticeable.
            return Double.MAX_VALUE;
        }
        var star = proximity != null ? proximity.effective[0] : null;
        if (star == null) {
            return Double.MAX_VALUE;
        }

        var radius = star.getRadius();
        var starDistance = star.getClosestDistToCamera() + minDist();
        var blend = starDistanceBlend(starDistance, radius * starDistanceScaleLow, radius * starDistanceScaleHigh);

        return blendStarDistance(starDistance, regularDistance, blend);
    }

    /**
     * Snapshot of the closest-star distance scaling, for the star distance window. Read-only: it
     * does not modify any camera state.
     *
     * @param starName         Name of the closest star, or null if there is none.
     * @param radius           Radius of the closest star, in internal units.
     * @param starDistance     Distance to the closest star, in internal units.
     * @param limitLow         Low limit, {@code radius * starDistanceScaleLow}, in internal units.
     * @param limitHigh        High limit, {@code radius * starDistanceScaleHigh}, in internal units.
     * @param blend            0 when the star distance is used as is, 1 when it is ignored.
     * @param starContribution Star distance blended into the regular distance, in internal units.
     * @param regularDistance  Distance used when no star is nearby, in internal units.
     * @param rawDistance      Raw (unsmoothed) distance of the current frame, in internal units.
     * @param smoothedDistance Smoothed distance, in internal units.
     * @param speedScale       Current speed scaling, in internal units per second.
     * @param enabled          Whether the star distance scaling setting is on.
     * @param mode             Current camera mode.
     */
    public record StarDistanceDebug(String starName,
                                    double radius,
                                    double starDistance,
                                    double limitLow,
                                    double limitHigh,
                                    double blend,
                                    double starContribution,
                                    double regularDistance,
                                    double rawDistance,
                                    double smoothedDistance,
                                    double speedScale,
                                    boolean enabled,
                                    CameraMode mode) {
    }

    /**
     * Builds a snapshot of the closest-star distance scaling inputs and outputs for the star
     * distance window.
     *
     * @return The snapshot. Never null; the star fields are NaN when there is no closest star.
     */
    public StarDistanceDebug starDistanceDebug() {
        var star = proximity != null ? proximity.effective[0] : null;
        double radius = Double.NaN;
        double starDistance = Double.NaN;
        double low = Double.NaN;
        double high = Double.NaN;
        double blend = Double.NaN;
        double contribution = Double.NaN;

        if (star != null) {
            radius = star.getRadius();
            starDistance = star.getClosestDistToCamera() + minDist();
            low = radius * starDistanceScaleLow;
            high = radius * starDistanceScaleHigh;
            blend = starDistanceBlend(starDistance, low, high);
            contribution = blendStarDistance(starDistance, regularDistance, blend);
        }

        return new StarDistanceDebug(star != null ? star.getName() : null,
                                     radius,
                                     starDistance,
                                     low,
                                     high,
                                     blend,
                                     contribution,
                                     regularDistance,
                                     rawDistance,
                                     smoothedDistance,
                                     speedScaling,
                                     GaiaSky.settings().scene.camera.starDistanceScaling,
                                     getMode());
    }

    private double smoothedDistance = -1;

    /**
     * Maps an altitude (distance above a body surface) to a speed scaling value, using a power
     * curve instead of a linear mapping. Linear scaling collapses to zero at the surface, which
     * makes the camera unusably slow in surface mode.
     *
     * @param altitude Distance above the surface, in internal units.
     *
     * @return The speed scaling.
     */
    static double altitudeToSpeedScale(double altitude) {
        if (altitude <= 0 || !Double.isFinite(altitude)) {
            return surfaceSpeed();
        }

        double normalized = altitude / speedScaleRefAltitude();
        double scale = surfaceSpeed() * Math.pow(normalized, SPEED_SCALE_EXPONENT);

        return FastMath.min(scale, MAX_SPEED_SCALING);
    }

    /**
     * Maps the distance to the nearest reference object to a speed scaling. Kept separate from
     * {@link #speedScaling(double)} so that the curve can be tested without a camera.
     * <p>
     * Linear up to {@link #SPEED_SCALE_FAR_KNEE_M}, then a power law with
     * {@link #SPEED_SCALE_FAR_EXPONENT} up to {@link #SPEED_SCALE_FAR_LINEAR_KNEE_M}, then linear
     * again out to the extragalactic regime. The branches meet at both knees, and all of them are
     * scale invariant: the knees are converted to units with the current {@link Constants#M_TO_U},
     * so the curve means the same physical distance at every distance scale factor.
     *
     * @param distance     Distance to the nearest reference, in internal units.
     * @param distSmoothUp Distance at which the linear part of the curve saturates, in internal
     *                     units.
     *
     * @return The speed scaling, in internal units per second.
     */
    static double distanceToSpeedScale(double distance,
                                       double distSmoothUp) {
        if (!Double.isFinite(distance) || distance <= 0 || !Double.isFinite(distSmoothUp) || distSmoothUp <= 0) {
            return 0;
        }

        double linear = MAX_SPEED_SCALING * distance / distSmoothUp;
        double knee = SPEED_SCALE_FAR_KNEE_M * Constants.M_TO_U;
        if (distance <= knee || knee <= 0 || knee >= distSmoothUp) {
            return FastMath.min(linear, MAX_SPEED_SCALING);
        }

        // Value of the linear branch at the knee, so that the curve is continuous.
        double atKnee = MAX_SPEED_SCALING * knee / distSmoothUp;
        double far = atKnee * Math.pow(distance / knee, SPEED_SCALE_FAR_EXPONENT);

        // Out at the extragalactic scales the power law grows too slowly to cross a survey volume, so
        // the curve goes back to the linear one, which keeps the crossing time constant at every
        // scale and reaches the ceiling at distSmoothUp. The exponent branch is blended into it over
        // a decade of distance, so that the camera accelerates instead of jumping in speed where the
        // two branches meet.
        double farLinearKnee = SPEED_SCALE_FAR_LINEAR_KNEE_M * Constants.M_TO_U;
        if (farLinearKnee > knee && distance > farLinearKnee) {
            double t = Math.log10(distance / farLinearKnee) / SPEED_SCALE_FAR_LINEAR_RAMP_DECADES;
            t = t >= 1.0 ? 1.0 : (t <= 0.0 ? 0.0 : t * t * (3.0 - 2.0 * t));
            far = far * (1.0 - t) + linear * t;
        }

        return FastMath.min(far, MAX_SPEED_SCALING);
    }

    /**
     * The speed scaling function.
     *
     * @param min The minimum speed.
     *
     * @return The speed scaling.
     */
    public double speedScaling(double min) {
        double dist = computeRawDistances();
        double speed = GaiaSky.settings().scene.camera.speed;

        if (dist < speedScaleRefAltitude()) {
            // Very close to a surface: use the logarithmic curve. It is already in internal units per
            // second, because the surface speed it is built on is a physical speed converted with the
            // current distance scale factor. Multiplying it by the factor again would make the camera
            // fly at a walking pace times the factor.
            return FastMath.max(altitudeToSpeedScale(dist), min) * speed;
        }

        // Far from any surface: linear near, sub-linear once out of the solar system. This curve is
        // built out of ratios of internal distances, so it is scale invariant by itself and the
        // factor has to be applied here, once.
        double distanceMap = smoothedDistance >= 0 ? distanceToSpeedScale(smoothedDistance, DIST_SMOOTH_UP) : 0;

        return distanceMap > 0 ? Math.max(distanceMap, min) * speed * Constants.DISTANCE_SCALE_FACTOR : 0;
    }

    /**
     * The speed scaling function.
     *
     * @return The speed scaling.
     */
    public double speedScaling() {
        return speedScaling(0.5e-8);
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        switch (event) {
            case SCENE_LOADED -> {
                this.scene = (Scene) data[0];
                this.focus.setScene(this.scene);
                this.focusView.setScene(this.scene);
                this.trackingObject.setScene(this.scene);
                this.closestBody.setScene(this.scene);
                this.closestStarView.setScene(this.scene);
            }
            case FOCUS_CHANGE_CMD -> {
                setTrackingObject(null, null);
                // Check the type of the parameter: IFocus or String
                Entity newFocus = null;

                // Center focus or not
                boolean centerFocus = !vr;
                if (data.length > 1)
                    centerFocus = (Boolean) data[1];
                String focusName = null;
                if (data[0] instanceof String) {
                    focusName = (String) data[0];
                    newFocus = scene.getEntity(focusName);
                    diverted = !centerFocus;
                } else if (data[0] instanceof FocusView) {
                    newFocus = ((FocusView) data[0]).getEntity();
                    diverted = !centerFocus;
                } else if (data[0] instanceof Entity) {
                    newFocus = (Entity) data[0];
                    diverted = !centerFocus;
                }
                setFocus(focusName, newFocus);
                checkFocus();
            }
            case FOV_CMD -> {
                boolean checkMax = source instanceof Actor;
                float fov = MathUtilsDouble.clamp((float) data[0], Constants.MIN_FOV, checkMax ? Constants.MAX_FOV : 179f);
                for (PerspectiveCamera cam : cameras) {
                    cam.fieldOfView = fov;
                }
                updateFovFactor();
            }
            case CUBEMAP_CMD -> {
                boolean state = (boolean) data[0];
                CubemapProjection p = (CubemapProjection) data[1];
                if (p.isPlanetarium() && state && !vr) {
                    fovBackup = GaiaSky.instance.cameraManager.getCamera().fieldOfView;
                }
            }
            case CAMERA_POS_CMD -> {
                synchronized (updateLock) {
                    pos.set((double[]) data[0]);
                    posInv.set(pos)
                            .scl(-1d);
                }
            }
            case CAMERA_DIR_CMD -> {
                synchronized (updateLock) {
                    direction.set((double[]) data[0])
                            .nor();
                }
            }
            case CAMERA_UP_CMD -> {
                synchronized (updateLock) {
                    up.set((double[]) data[0])
                            .nor();
                }
            }
            case CAMERA_PROJECTION_CMD -> {
                synchronized (updateLock) {
                    // Position
                    pos.set((double[]) data[0]);
                    posInv.set(pos)
                            .scl(-1d);
                    // Direction
                    direction.set((double[]) data[1])
                            .nor();
                    // Up
                    up.set((double[]) data[2])
                            .nor();
                    // Change projection flag
                    projectionFlag = true;
                }
            }
            case CAMERA_FWD -> {
                synchronized (updateLock) {
                    addForwardForce((double) data[0]);
                }
            }
            case CAMERA_ROTATE -> {
                synchronized (updateLock) {
                    addRotateMovement((double) data[0], (double) data[1], false, true);
                }
            }
            case CAMERA_TURN -> {
                synchronized (updateLock) {
                    addRotateMovement((double) data[0], (double) data[1], true, true);
                }
            }
            case CAMERA_ROLL -> {
                synchronized (updateLock) {
                    addRoll((double) data[0], GaiaSky.settings().scene.camera.cinematic);
                }
            }
            case CAMERA_STOP -> {
                synchronized (updateLock) {
                    stopTotalMovement();
                }
            }
            case CAMERA_CENTER -> {
                synchronized (updateLock) {
                    diverted = false;
                }
            }
            case GO_TO_OBJECT_CMD -> {
                if (this.focus != null && this.focus.isValid()) {
                    IFocus f = this.focus;
                    GaiaSky.postRunnable(() -> {
                        setTrackingObject(null, null);
                        // Position camera near focus
                        stopTotalMovement();

                        f.getAbsolutePosition(aux1b);
                        pos.set(aux1b);

                        double dx = 0d;
                        double dy = f.getSize() / 4d;
                        double dz = -f.getSize() * 4d;
                        if (vr) {
                            dz = -dz;
                        }

                        pos.add(dx, dy, dz);
                        posInv.set(pos)
                                .scl(-1d);
                        direction.set(-dx, -dy, -dz)
                                .nor();
                        up.set(direction.x, direction.z, -direction.y)
                                .nor();
                        rotate(up, 0.01);
                        updatePerspectiveCamera();
                    });
                }
            }
            case FREE_MODE_COORD_CMD -> {
                synchronized (updateLock) {
                    double ra = (Double) data[0];
                    double dec = (Double) data[1];
                    double dist = 1e12d * Constants.PC_TO_U;
                    aux1.set(MathUtilsDouble.degRad * ra, MathUtilsDouble.degRad * dec, dist);
                    Coordinates.sphericalToCartesian(aux1, aux2);
                    freeTargetPos.set(aux2);
                    facingFocus = false;
                    freeTargetOn = true;
                }
            }
            case FOCUS_NOT_AVAILABLE -> {
                if (getMode().isFocus()) {
                    boolean found = false;
                    if (data[0] instanceof Entity entity) {
                        if (Mapper.octree.has(entity)) {
                            // Octree wrapper.
                            var root = Mapper.octant.get(entity);
                            OctreeNode octant = this.focus.getOctant();
                            if (octant != null && octant.getRoot() == root.octant) {
                                found = true;
                            }
                        } else if (Mapper.datasetDescription.has(entity)) {
                            // Generic catalog.
                            var graph = Mapper.graph.get(entity);
                            found = graph.children != null && graph.children.contains(focus.getEntity(), true);
                        } else if (Mapper.focus.has(entity)) {
                            // Focus.
                            focus.setEntity(entity);
                            found = isFocus(entity);
                        }
                    }
                    if (found) {
                        // Set camera  free
                        EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                    }
                }
            }
            case TOGGLE_VISIBILITY_CMD -> {
                if (getMode().isFocus()) {
                    ComponentType ct = ComponentType.getFromKey((String) data[0]);
                    if (this.focus != null && this.focus.isValid() && ct != null && this.focus.getCt()
                            .isEnabled(ct)) {
                        // Set camera  free
                        EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                    }
                }
            }
            case CAMERA_CENTER_FOCUS_CMD -> {
                synchronized (updateLock) {
                    setCenterFocus((Boolean) data[0]);
                }
            }
            case CONTROLLER_CONNECTED_INFO -> GaiaSky.settings().controls.gamepad.addControllerListener(gamepadListener, (String) data[0]);
            case CONTROLLER_DISCONNECTED_INFO -> {
                // Empty.
            }
            case NEW_DISTANCE_SCALE_FACTOR -> {
                synchronized (updateLock) {
                    DIST_SMOOTH_UP = 5000.0 * Constants.MPC_TO_U;
                    MAX_ALLOWED_DISTANCE = 50_000.0 * Constants.MPC_TO_U;
                }
            }
            case CAMERA_TRACKING_OBJECT_CMD -> {
                Entity newTrackingObject = (Entity) data[0];
                String newTrackingName = (String) data[1];
                synchronized (updateLock) {
                    this.setTrackingObject(newTrackingObject,
                                           newTrackingName != null ? newTrackingName.toLowerCase(Locale.ROOT) : null);
                }
            }
            default -> {
            }
        }

    }

    /**
     * Rotates the direction and up vector of this camera by the given angle around
     * the given axis, with the axis attached to given point. The direction and up
     * vector will not be orthogonal.
     *
     * @param rotationCenter the point to attach the axis to
     * @param rotationAxis   the axis to rotate around
     * @param angle          the angle, in degrees
     */
    public void rotateAround(Vector3Q rotationCenter,
                             Vector3D rotationAxis,
                             double angle) {
        rotate(rotationAxis, angle);

        // aux3 <- pos-point vector
        aux3b.set(pos)
                .sub(rotationCenter);
        aux3b.rotate(rotationAxis, angle);
        pos.set(aux3b)
                .add(rotationCenter);
        posDistanceCheck();
    }

    public void rotate(Vector3D axis,
                       double angle) {
        if (!axis.hasNaN() && Double.isFinite(angle)) {
            direction.rotate(axis, angle);
            up.rotate(axis, angle);
        }
    }

    /**
     * Moves the camera by the given amount on each axis.
     *
     * @param x the displacement on the x-axis
     * @param y the displacement on the y-axis
     * @param z the displacement on the z-axis
     */
    public void translate(double x,
                          double y,
                          double z) {
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            pos.add(x, y, z);
            posDistanceCheck();
        }
    }

    /**
     * Moves the camera by the given vector.
     *
     * @param vec the displacement vector
     */
    public void translate(Vector3D vec) {
        if (vec != null && !vec.hasNaN()) {
            pos.add(vec);
            posDistanceCheck();
        }
    }

    /**
     * Applies the given force to this entity's acceleration.
     *
     * @param force The force.
     */
    protected void applyForce(Vector3Q force) {
        if (force != null && !force.hasNaN()) {
            accel.add(force);
        }
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return new PerspectiveCamera[]{camera};
    }

    @Override
    public PerspectiveCamera getCamera() {
        return camera;
    }

    @Override
    public void setCamera(PerspectiveCamera perspectiveCamera) {
        this.camera = perspectiveCamera;
    }

    @Override
    public Vector3D getDirection() {
        return direction;
    }

    @Override
    public void setDirection(Vector3D dir) {
        this.direction.set(dir);
    }

    @Override
    public Vector3D getUp() {
        return up;
    }

    public void setUp(Vector3D up) {
        this.up.set(up);
    }

    @Override
    public Vector3D[] getDirections() {
        return new Vector3D[]{direction};
    }

    @Override
    public int getNCameras() {
        return 1;
    }

    @Override
    public CameraMode getMode() {
        return parent.mode;
    }

    @Override
    public double getSpeed() {
        return parent.getSpeed();
    }

    @Override
    public boolean isFocus(Entity focus) {
        return this.focus != null && this.focus.getEntity() == focus;
    }

    @Override
    public IFocus getFocus() {
        return getMode().equals(CameraMode.FOCUS_MODE) ? this.focus : null;
    }

    /**
     * Returns the focus position delta computed in the last update (next focus
     * position minus current focus position). It is zero if the position lock
     * is disabled. Only meaningful in focus mode.
     *
     * @return The focus position delta.
     */
    public Vector3Q getFocusDx() {
        return dx;
    }

    @Override
    public boolean hasFocus() {
        return getMode().equals(CameraMode.FOCUS_MODE) && focus != null && !focus.isEmpty();
    }

    /**
     * Checks the position of the camera does not collide with the focus object. Only objects whose
     * bounding sphere collides with the camera are checked, see {@link Body#cameraCollision}. Large
     * objects that always contain the camera, like the Milky Way, must opt out of this, or focusing
     * them teleports the camera outside of them.
     */
    private void checkFocus() {
        if (focus.isValid() && focus.isCameraCollision() && !Mapper.hip.has(focus.getEntity()) && focus.getSet() == null && focus.getElementsSet() == null) {
            // Move camera if too close to focus.
            this.focus.getAbsolutePosition(aux1b);
            if (pos.dstD(aux1b, aux2b) < this.focus.getRadius()) {
                // Position camera near focus.
                stopTotalMovement();

                this.focus.getAbsolutePosition(aux1b);
                pos.set(aux1b);

                pos.add(0d, 0d, -this.focus.getSize() * 6d);
                posInv.set(pos)
                        .scl(-1d);
                direction.set(0d, 0d, 1d);
            }
        } else if (!focus.isValid() && closest != null && closest.isValid()) {
            // Use closest object as focus.
            focus.setEntity(((FocusView) closest).getEntity());
            checkFocus();
        }
    }

    public void setCenterFocus(boolean centerFocus) {
        this.diverted = !centerFocus;
    }

    /**
     * Computes the apparent magnitude of the current focus object as seen from the camera
     * position.
     *
     * @return The magnitude.
     */
    private double computeFocusApparentMagnitudeCamera() {
        if (focus.isParticle() || focus.getMag() == null) {
            // Stars.

            // m - M = 5 * log10(d) - 5
            // m: apparent magnitude
            // M: absolute magnitude
            // d: distance [pc]
            return 5d * FastMath.log10(focus.getDistToCamera() * Constants.U_TO_PC) - 5d + focus.getAbsmag();
        } else if (focus.getMag() != null) {
            // Models, and other bodies.

            // m - H = 5 * log10(r * D) + g
            // m: apparent magnitude
            // H: absolute magnitude
            // r: dist to star [au]
            // D: dist to Earth [au]
            // g: term for phase effects (~0)
            double distCamAu = pos.put(aux4b)
                    .sub(focus.getAbsolutePosition(aux5b))
                    .lenDouble() * Constants.U_TO_AU;
            // Rest (models, etc.), use distance to star.
            IFocus starAncestor = focus.getFirstStarAncestor();
            double distStarAu = (starAncestor != null ?
                    starAncestor.getAbsolutePosition(aux4b)
                            .sub(focus.getAbsolutePosition(aux5b))
                            .lenDouble() :
                    focus.getAbsolutePosition(aux5b)
                            .lenDouble()) * Constants.U_TO_AU;
            return 5d * FastMath.log10(distStarAu * distCamAu) + focus.getAbsmag();
        }
        return Double.NaN;
    }

    /**
     * Computes the apparent magnitude of the current focus object as seen from Earth.
     *
     * @return The magnitude.
     */
    private double computeFocusApparentMagnitudeEarth() {
        // Apparent magnitude from Earth (planets, etc)
        Entity earth = scene.getEntity("Earth");
        if (focus.getMag() != null && earth != null) {
            // Distance between earth and the body
            // Apparent magnitude in Solar System bodies
            // m - H = 5 * log10(r * D) + g
            // m: apparent magnitude
            // H: absolute magnitude
            // r: dist from object to star [AU]
            // D: dist from object to Earth [AU]
            // g: term for phase effects (~0)
            double distEarthAu = EntityUtils.getAbsolutePosition(earth, aux4b)
                    .sub(focus.getAbsolutePosition(aux5b))
                    .lenDouble() * Constants.U_TO_AU;
            IFocus starAncestor = focus.getFirstStarAncestor(focusView);
            double distStarAu = (starAncestor != null ?
                    starAncestor.getAbsolutePosition(aux4b)
                            .sub(focus.getAbsolutePosition(aux5b))
                            .lenDouble() :
                    focus.getAbsolutePosition(aux5b)
                            .lenDouble()) * Constants.U_TO_AU;
            return 5d * FastMath.log10(distStarAu * distEarthAu) + focus.getAbsmag();
        } else {
            return Double.NaN;
        }
    }

    public void setThrust(double thrust,
                          int direction) {
        this.thrust = thrust;
        this.thrustDirection = direction;
    }

    @Override
    public void render(int rw,
                       int rh) {

        boolean modeStereo = GaiaSky.settings().program.modeStereo.active;
        boolean modeStereoVR = modeStereo && GaiaSky.settings().program.modeStereo.isStereoVR();
        boolean modeCubemap = GaiaSky.settings().program.modeCubemap.active;
        boolean modeReprojection = GaiaSky.settings().postprocess.reprojection.active;

        if (modeStereoVR || modeReprojection) {
            // No pointer guides or cross-hairs
            return;
        }

        // Pointer guides
        if (GaiaSky.settings().program.pointer.guides.active && !modeStereo && !modeCubemap && !vr) {
            int mouseX = Gdx.input.getX();
            int mouseY = rh - Gdx.input.getY();
            shapeRenderer.begin(ShapeType.Line);
            Gdx.gl.glEnable(GL30.GL_BLEND);
            Gdx.gl.glLineWidth(GaiaSky.settings().program.pointer.guides.width);
            float[] pc = GaiaSky.settings().program.pointer.guides.color;
            shapeRenderer.setColor(pc[0], pc[1], pc[2], pc[3]);
            shapeRenderer.line(0, mouseY, rw, mouseY);
            shapeRenderer.line(mouseX, 0, mouseX, rh);
            shapeRenderer.end();
        }

        spriteBatch.begin();

        boolean decal = modeCubemap || modeStereo || vr;
        float chScale = 2f / GaiaSky.instance.getUnitsPerPixel();
        if (modeCubemap) {
            chScale = 4f;
        }
        // Mark home in ORANGE
        if (GaiaSky.settings().scene.crosshair.home) {
            if (home == null && scene != null) {
                var homeEntity = scene.findFocus(GaiaSky.settings().scene.homeObject);
                home = new FocusView(homeEntity);
            }
            if (home != null) {
                drawCrossHair(spriteBatch, home, decal, false, spriteHome, crosshairArrow, chScale, rw, rh, 1f, 0.7f, 0.1f, 1f);
            }
        }

        // Mark closest object in BLUE
        if (GaiaSky.settings().scene.crosshair.closest && closest != null) {
            drawCrossHair(spriteBatch, closest, decal, false, spriteClosest, crosshairArrow, chScale, rw, rh, 0.3f, 0.5f, 1f,
                          0.6f);
        }

        // Mark the focus in GREEN
        if (GaiaSky.settings().scene.crosshair.focus && getMode().isFocus()) {
            // Green, focus mode
            drawCrossHair(spriteBatch, focus, decal, true, spriteFocus, crosshairArrow, chScale, rw, rh, 0.2f, 1f, 0.4f, 1f);
        }

        // Gravitational waves crosshair
        if (GaiaSky.settings().runtime.gravitationalWaves) {
            RelativisticEffectsManager gw = RelativisticEffectsManager.getInstance();

            float chw = crosshairGravWaves.getWidth();
            float chh = crosshairGravWaves.getHeight();
            float chw2 = chw / 2;
            float chh2 = chh / 2;

            aux1.set(gw.gw)
                    .nor()
                    .scl(1e12)
                    .add(posInv);

            GlobalResources.applyRelativisticAberration(aux1, this);
            // GravitationalWavesManager.instance().gravitationalWavePos(aux1);

            boolean inside = projectToScreen(aux1, auxf1, rw, rh, chw2, chh2);

            if (inside) {
                // Cyan
                spriteBatch.setColor(0, 1, 1, 1);
                spriteBatch.draw(crosshairGravWaves, auxf1.x - chw2, auxf1.y - chh2, chw, chh);
            }
        }

        spriteBatch.end();
    }

    /**
     * Draws a cross-hair as a decal, a 3D camera-oriented texture.
     *
     * @param batch     The batch.
     * @param chFocus   The focus element.
     * @param focusMode The mode.
     * @param sprite    The sprite.
     * @param r         Red.
     * @param g         Green.
     * @param b         Blue.
     * @param a         Alpha.
     */
    private void drawCrossHairDecal(SpriteBatch batch,
                                    IFocus chFocus,
                                    boolean focusMode,
                                    Sprite sprite,
                                    float r,
                                    float g,
                                    float b,
                                    float a) {
        sprite.setColor(r, g, b, a);
        Vector3Q p;
        if (!focusMode) {
            p = chFocus.getClosestAbsolutePos(aux1b)
                    .add(posInv);
        } else {
            p = chFocus.getAbsolutePosition(aux1b)
                    .add(posInv);
        }
        Vector3D pos = aux5;
        p.put(pos);
        DecalUtils.drawSprite(sprite, batch, (float) pos.x, (float) pos.y, (float) pos.z, 0.0008d,
                              1f, this, true, 0.04f, 0.04f);
    }

    private void drawCrossHair(SpriteBatch batch,
                               IFocus chFocus,
                               boolean decal,
                               boolean focusMode,
                               Sprite crosshairSprite,
                               Texture arrowTex,
                               float crosshairScale,
                               int rw,
                               int rh,
                               float r,
                               float g,
                               float b,
                               float a) {
        if (chFocus != null) {
            if (decal) {
                crosshairSprite.setScale(crosshairScale);
                drawCrossHairDecal(batch, chFocus, focusMode, crosshairSprite, r, g, b, a);
            } else {
                if (!focusMode) {
                    drawCrossHair(chFocus.getClosestAbsolutePos(aux1b)
                                          .add(posInv), chFocus.getClosestDistToCamera(),
                                  chFocus.getRadius(), crosshairSprite.getTexture(), crosshairScale,
                                  arrowTex, rw, rh, r, g, b, a);
                } else {
                    drawCrossHair(chFocus.getAbsolutePosition(aux1b)
                                          .add(posInv), chFocus.getDistToCamera(),
                                  chFocus.getRadius(), crosshairSprite.getTexture(), crosshairScale, arrowTex,
                                  rw, rh, r, g, b, a);
                }
            }
        }
    }

    /**
     * Draws a cross-hair given a camera-relative position.
     *
     * @param p              The position in floating camera coordinates.
     * @param distToCam      The distance to the camera.
     * @param radius         Radius of object.
     * @param crossHairTex   Cross-hair texture.
     * @param crossHairScale The scale of the cross-hair.
     * @param arrowTex       Arrow texture.
     * @param rw             Width.
     * @param rh             Height.
     * @param r              Red.
     * @param g              Green.
     * @param b              Blue.
     * @param a              Alpha.
     */
    private void drawCrossHair(Vector3Q p,
                               double distToCam,
                               double radius,
                               Texture crossHairTex,
                               float crossHairScale,
                               Texture arrowTex,
                               int rw,
                               int rh,
                               float r,
                               float g,
                               float b,
                               float a) {
        if (distToCam > radius * 2) {
            float chw = crossHairTex.getWidth() * crossHairScale;
            float chh = crossHairTex.getHeight() * crossHairScale;
            float chw2 = chw / 2;
            float chh2 = chh / (vr ? 1 : 2);

            Vector3D pos = aux5;
            p.put(pos);
            GlobalResources.applyRelativisticAberration(pos, this);
            RelativisticEffectsManager.getInstance()
                    .gravitationalWavePos(pos);

            if (vr) {
                pos.nor()
                        .scl(distToCam - radius);
            }
            boolean inside = projectToScreen(pos, auxf1, rw, rh, chw2, chh2);

            spriteBatch.setColor(r, g, b, a);

            if (inside) {
                spriteBatch.draw(crossHairTex, auxf1.x - chw2, auxf1.y - chh2, chw, chh);
            } else {
                if (vr) {
                    float ang = firstAux ? -90f + aux2f2.angleDeg() : firstAngle;
                    if (firstAux) {
                        firstAngle = ang;
                    }
                    firstAux = !firstAux;
                    aux2f2.set(auxf1.x - (rw / 2f), auxf1.y - (rh / 2f));
                    aux2.set(up)
                            .rotate(direction, 90)
                            .add(up)
                            .scl(0.04);
                    aux1.set(vrOffset)
                            .add(aux2)
                            .scl(1 / Constants.M_TO_U)
                            .add(direction);
                    projectToScreen(aux1, auxf1, rw, rh, chw2, chh2);
                    spriteBatch.draw(arrowTex, auxf1.x, auxf1.y, chw2, chh2, chw, chh, 1f, 1f, ang, 0, 0, (int) chw, (int) chw,
                                     false, false);
                } else {
                    aux2f2.set(auxf1.x - (rw / 2f), auxf1.y - (rh / 2f));
                    spriteBatch.draw(arrowTex,
                                     auxf1.x - chw2,
                                     auxf1.y - chh2,
                                     chw2,
                                     chh2,
                                     chw,
                                     chh,
                                     1f,
                                     1f,
                                     -90f + aux2f2.angleDeg(),
                                     0,
                                     0,
                                     (int) chw,
                                     (int) chh,
                                     false,
                                     false);
                }
            }
        }

    }

    /**
     * Projects to screen.
     *
     * @return False if projected point falls outside the screen bounds, true
     * otherwise.
     */
    private boolean projectToScreen(Vector3D vec,
                                    Vector3 out,
                                    int rw,
                                    int rh,
                                    float chw2,
                                    float chh2) {
        vec.put(out);
        camera.project(out, 0, 0, rw, rh);

        double ang = direction.angle(vec);
        if (ang > 90) {
            out.x = rw - out.x;
            out.y = rh - out.y;

            float w2 = rw / 2f;
            float h2 = rh / 2f;

            // Q1 | Q2
            // -------
            // Q3 | Q4

            if (out.x <= w2 && out.y >= h2) {
                // Q1
                out.x = chw2;
                out.y = rh - chh2;

            } else if (out.x > w2 && out.y > h2) {
                // Q2
                out.x = rw - chw2;
                out.y = rh - chh2;
            } else if (out.x <= w2) {
                // Q3
                out.x = chw2;
                out.y = chh2;
            } else if (out.y < h2) {
                // Q4
                out.x = rw - chw2;
                out.y = chh2;
            }
        }

        out.x = MathUtils.clamp(out.x, chw2, rw - chw2);
        out.y = MathUtils.clamp(out.y, chh2, rh - chh2);

        return ang * 2 < camera.fieldOfView;
    }

    @Override
    public void resize(int width,
                       int height) {
        if (!vr) {
            camera.viewportHeight = height;
            camera.viewportWidth = width;
            camera.update(true);
        }
        spriteBatch.getProjectionMatrix()
                .setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight);
        shapeRenderer.getProjectionMatrix()
                .setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight);
    }

    @Override
    public void setPointerProjectionOnFocus(Vector3 point) {
        this.pointerCartesian.set(point);
        this.pointerCoordinatesFlag.set(true);
    }

    @Override
    public double getSpeedScaling() {
        return speedScaling;
    }

    @Override
    public double getSpeedScalingCapped() {
        return speedScalingCapped;
    }

    @Override
    public Vector3D getVelocity() {
        return vel;
    }

    /**
     * Sets the gamepad velocity as it comes from the joystick sensor.
     *
     * @param amount The amount in [-1, 1].
     */
    public void setVelocity(double amount) {
        velocityGamepad = amount;
    }

    public void setDiverted(boolean diverted) {
        this.diverted = diverted;
    }

    public void setCameraMultipliers(double movementMultiplier,
                                     double speedMultiplier) {
        this.movementMultiplier = movementMultiplier;
        this.speedMultiplier = speedMultiplier;
    }

    public AbstractMouseKbdListener getCurrentMouseKbdListener() {
        return currentMouseKbdListener;
    }

    private void setTrackingObject(Entity trackingObject,
                                   String trackingName) {
        this.trackingObject.setEntity(trackingObject);
        this.trackingName = trackingName;
        EventManager.publish(Event.CAMERA_TRACKING_OBJECT_UPDATE, this, new FocusView(trackingObject), trackingName);
    }

    public Scene getScene() {
        return scene;
    }

    @Override
    public boolean isRotating() {
        return getMode().isFocus() && (FastMath.abs(horizontal.y) > 1e-6 || FastMath.abs(vertical.y) > 1e-6);
    }

    private boolean isTracking() {
        return trackingObject != null && trackingName != null;
    }
}
