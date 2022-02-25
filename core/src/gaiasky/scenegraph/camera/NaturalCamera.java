/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.interafce.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.util.*;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;
import gaiasky.util.gdx.g2d.Sprite;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;
import org.lwjgl.opengl.GL30;

/**
 * Models the movement of the camera
 */
public class NaturalCamera extends AbstractCamera implements IObserver {

    private static final double MIN_DIST = 1 * Constants.M_TO_U;

    /**
     * VR offset
     **/
    public Vector3d vrOffset;

    /**
     * Acceleration and velocity
     **/
    public Vector3d accel, vel, posBak;
    /**
     * The force acting on the entity and the friction
     **/
    private final Vector3b force;
    private Vector3b friction;

    public Vector3d direction, up;
    private Vector3b focusDirection;

    /**
     * Indicates whether the camera is facing the focus or not
     **/
    public boolean facingFocus;

    /**
     * Auxiliary double vectors
     **/
    private Vector3d aux1, aux2, aux5;
    private Vector3b dx, aux1b, aux2b, aux3b, aux4b, aux5b, nextFocusPosition, nextClosestPosition;
    private Vector2 aux2f2;
    /**
     * Auxiliary float vector
     **/
    private Vector3 auxf1;
    /**
     * Acceleration, velocity and position for pitch, yaw and roll
     **/
    private Vector3d pitch, yaw, roll;
    /**
     * Acceleration, velocity and position for the horizontal and vertical
     * rotation around the focus
     **/
    private Vector3d horizontal, vertical;
    /**
     * Time since last forward control issued, in seconds
     **/
    private double lastFwdTime = 0d;
    /**
     * The last forward amount, positive forward, negative backward
     **/
    private double lastFwdAmount = 0;
    /**
     * Previous angle in orientation lock
     **/
    double previousOrientationAngle = 0;
    /**
     * Previous orientation matrix, for focus lock
     */
    Matrix4d previousOrientation;
    /**
     * Fov value backup
     **/
    float fovBackup;

    /**
     * Gravity in game mode
     **/
    boolean gravity = true;

    /**
     * Thrust which keeps the camera going. Mainly for game pads
     **/
    private double thrust = 0;
    private int thrustDirection = 0;

    /**
     * Whether the camera stops after a few seconds or keeps going
     **/
    private boolean fullStop = true;

    /**
     * Entities for the GAIA_SCENE_MODE mode
     **/
    protected CelestialBody entity1 = null, entity2 = null, entity3 = null;

    private CameraMode lastMode;

    /**
     * The focus entity
     */
    public IFocus focus, focusBak;

    /**
     * The tracking object, if any
     */
    private IFocus trackingObject;
    /**
     * The name of the tracking object
     */
    private String trackingName;

    /**
     * The direction point to seek
     */
    private Vector3d lastVel;
    /**
     * FOCUS_MODE position
     **/
    private Vector3b focusPos;
    /**
     * Free mode target
     **/
    private Vector3b freeTargetPos;
    private boolean freeTargetOn;

    private Vector3b desired;

    /**
     * VR mode stuff
     **/
    private boolean firstAux = true;
    private float firstAngle = 0;

    /**
     * Velocity module, in case it comes from a game pad.
     * Sets velocity in the direction of the direction vector
     **/
    private double velocityGamepad = 0;
    private double gamepadMultiplier = 1;
    /**
     * VR velocity vectors
     **/
    private Vector3 velocityVR0, velocityVR1;
    /**
     * Magnitude of velocityVR vector. Sets the velocity in the direction
     * of the VR controller
     **/
    private double velocityVRX = 0;
    private double velocityVRY = 0;

    /**
     * Home object as defined in the properties file
     **/
    private IFocus home;

    /**
     * Holds whether the last input was issued by currentMouseKbdListener. Useful to keep
     * things rolling even if currentMouseKbdListener sticks do not move
     **/
    boolean inputByController = false;

    /** We use this lock to update any attributes of this camera **/
    private final Object updateLock = new Object();
    boolean diverted = false;
    boolean vr = false;

    /**
     * Flag that marks whether the projection has already been modified.
     * Only in master-slave configurations.
     */
    boolean projectionFlag = false;

    public double[] hudScales;
    public Color[] hudColors;
    public float hudWidth, hudHeight;
    private static final double HUD_SCALE_MIN = 0.5f;
    private static final double HUD_SCALE_MAX = 3.0f;

    /**
     * The current listener
     */
    private MouseKbdListener currentMouseKbdListener;
    /**
     * Implements the regular mouse+kbd camera input
     **/
    private NaturalMouseKbdListener naturalMouseKbdListener;
    /**
     * Implements WASD movement + mouse look camera input
     */
    private GameMouseKbdListener gameMouseKbdListener;

    /**
     * Implements gamepad camera input
     **/
    private NaturalControllerListener controllerListener;

    /**
     * VR listener
     **/
    private OpenVRListener openVRListener;

    private double DIST_A;
    private double DIST_B;
    private double DIST_C;

    private SpriteBatch spriteBatch;
    private ShapeRenderer shapeRenderer;

    private Sprite spriteFocus, spriteClosest, spriteHome;
    private Texture crosshairArrow, gravWaveCrosshair;

    public NaturalCamera(AssetManager assetManager, CameraManager parent, boolean vr, ShaderProgram spriteShader, ShaderProgram shapeShader) {
        super(parent);
        vrOffset = new Vector3d();
        vel = new Vector3d();
        accel = new Vector3d();
        force = new Vector3b();
        posBak = new Vector3d();
        previousOrientation = new Matrix4d();
        this.vr = vr;
        initialize(spriteShader, shapeShader);

    }

    public void initialize(ShaderProgram spriteShader, ShaderProgram shapeShader) {
        if (vr) {
            camera = new PerspectiveCamera(Settings.settings.scene.camera.fov, Settings.settings.graphics.backBufferResolution[0], Settings.settings.graphics.backBufferResolution[1]);
        } else {
            camera = new PerspectiveCamera(Settings.settings.scene.camera.fov, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;
        fovBackup = Settings.settings.scene.camera.fov;

        // init cameras vector
        cameras = new PerspectiveCamera[] { camera, camLeft, camRight };

        fovFactor = camera.fieldOfView / 40f;

        up = new Vector3d(1, 0, 0);
        direction = new Vector3d(0, 1, 0);
        focusDirection = new Vector3b();
        desired = new Vector3b();
        pitch = new Vector3d(0.0f, 0.0f, -3.0291599E-6f);
        yaw = new Vector3d(0.0f, 0.0f, -7.9807205E-6f);
        roll = new Vector3d(0.0f, 0.0f, -1.4423944E-4f);
        horizontal = new Vector3d();
        vertical = new Vector3d();

        friction = new Vector3b();
        lastVel = new Vector3d();
        focusPos = new Vector3b();
        freeTargetPos = new Vector3b();
        freeTargetOn = false;

        DIST_A = 0.1 * Constants.PC_TO_U;
        DIST_B = 5.0 * Constants.KPC_TO_U;
        DIST_C = 5000.0 * Constants.MPC_TO_U;

        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux5 = new Vector3d();
        auxf1 = new Vector3();
        aux2f2 = new Vector2();

        aux1b = new Vector3b();
        aux2b = new Vector3b();
        aux3b = new Vector3b();
        aux4b = new Vector3b();
        aux5b = new Vector3b();

        dx = new Vector3b();
        nextFocusPosition = new Vector3b();
        nextClosestPosition = new Vector3b();

        // Mouse and keyboard listeners
        naturalMouseKbdListener = new NaturalMouseKbdListener(this);
        gameMouseKbdListener = new GameMouseKbdListener(this);
        currentMouseKbdListener = null;
        // Controller listeners
        controllerListener = new NaturalControllerListener(this, Settings.settings.controls.gamepad.mappingsFile);
        ControllerConnectionListener controllerConnectionListener = new ControllerConnectionListener();
        Controllers.addListener(controllerConnectionListener);
        if (vr)
            openVRListener = new OpenVRListener(this);

        // Shape renderer (pointer guide lines)
        shapeRenderer = new ShapeRenderer(10, shapeShader);
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight);

        // Init sprite batch for crosshair
        spriteBatch = new SpriteBatch(50, spriteShader);

        // Focus crosshair
        Texture crosshairFocus = new Texture(Gdx.files.internal("img/crosshair-focus.png"));
        crosshairFocus.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        spriteFocus = new Sprite(crosshairFocus);

        // Closest crosshair
        Texture crosshairClosest = new Texture(Gdx.files.internal("img/crosshair-closest.png"));
        crosshairClosest.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        spriteClosest = new Sprite(crosshairClosest);

        // Home crosshair
        Texture crosshairHome = new Texture(Gdx.files.internal("img/crosshair-home.png"));
        crosshairHome.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        spriteHome = new Sprite(crosshairHome);

        // Arrow crosshair
        crosshairArrow = new Texture(Gdx.files.internal("img/crosshair-arrow.png"));
        crosshairArrow.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Velocity vector crosshair
        Texture velocityCrosshair = new Texture(Gdx.files.internal("img/ai-vel.png"));
        velocityCrosshair.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Anti-velocity vector crosshair
        Texture antiVelocityCrosshair = new Texture(Gdx.files.internal("img/ai-antivel.png"));
        antiVelocityCrosshair.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Grav wave crosshair
        gravWaveCrosshair = new Texture(Gdx.files.internal("img/gravwave-pointer.png"));
        gravWaveCrosshair.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Speed HUD
        Texture sHUD = new Texture(Gdx.files.internal("img/hud-corners.png"));
        sHUD.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        hudWidth = sHUD.getWidth();
        hudHeight = sHUD.getHeight();

        hudScales = new double[] { HUD_SCALE_MIN, HUD_SCALE_MIN + (HUD_SCALE_MAX - HUD_SCALE_MIN) / 3d, HUD_SCALE_MIN + (HUD_SCALE_MAX - HUD_SCALE_MIN) * 2d / 3d };
        Sprite[] hudSprites = new Sprite[hudScales.length];
        hudColors = new Color[] { Color.WHITE, Color.GREEN, Color.GOLD, Color.LIME, Color.PINK, Color.ORANGE, Color.CORAL, Color.CYAN, Color.FIREBRICK, Color.FOREST };

        for (int i = 0; i < hudScales.length; i++) {
            hudSprites[i] = new Sprite(sHUD);
            hudSprites[i].setOriginCenter();
        }

        // FOCUS_MODE is changed from GUI
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGE_CMD, Event.FOV_CHANGED_CMD, Event.ORIENTATION_LOCK_CMD, Event.CAMERA_POS_CMD, Event.CAMERA_DIR_CMD, Event.CAMERA_UP_CMD, Event.CAMERA_PROJECTION_CMD, Event.CAMERA_FWD, Event.CAMERA_ROTATE, Event.CAMERA_PAN, Event.CAMERA_ROLL, Event.CAMERA_TURN, Event.CAMERA_STOP, Event.CAMERA_CENTER, Event.GO_TO_OBJECT_CMD, Event.CUBEMAP_CMD, Event.FREE_MODE_COORD_CMD, Event.CATALOG_VISIBLE, Event.CATALOG_REMOVE, Event.FOCUS_NOT_AVAILABLE, Event.TOGGLE_VISIBILITY_CMD, Event.CAMERA_CENTER_FOCUS_CMD, Event.CONTROLLER_CONNECTED_INFO, Event.CONTROLLER_DISCONNECTED_INFO, Event.NEW_DISTANCE_SCALE_FACTOR, Event.CAMERA_TRACKING_OBJECT_CMD);
    }

    private void computeNextPositions(ITimeFrameProvider time) {
        if (getMode().isFocus() && focus != null) {
            focus.getPredictedPosition(nextFocusPosition, time, this, false);
        }
        if (!getMode().isFocus() && closestBody != null) {
            if (closestBody != focus)
                closestBody.getPredictedPosition(nextClosestPosition, time, this, false);
            else
                nextClosestPosition.set(nextFocusPosition);

        }
    }

    public void update(double dt, ITimeFrameProvider time) {
        // SLAVE - orient
        if (SlaveManager.projectionActive()) {
            camOrientProjection(SlaveManager.instance.yaw, SlaveManager.instance.pitch, SlaveManager.instance.roll);
        }

        // Update camera
        camUpdate(dt, time);

        // MASTER - broadcast
        if (MasterManager.instance != null) {
            // Send camera state
            MasterManager.instance.boardcastCameraAndTime(this.pos, this.direction, this.up, time);
        }
    }

    private void camUpdate(double dt, ITimeFrameProvider time) {
        currentMouseKbdListener.update();
        controllerListener.update();
        if (Settings.settings.runtime.openVr)
            openVRListener.update();

        // Next focus and closest positions
        computeNextPositions(time);

        // The whole update thread must lock the value of direction and up
        distance = pos.lend();
        CameraMode m = (parent.current == this ? parent.mode : lastMode);
        double realTransUnits = m.isGame() ? speedScaling(1e-5) : speedScaling();
        double translateUnits = Math.max(10d * Constants.M_TO_U, realTransUnits);
        switch (m) {
        case FOCUS_MODE:
            if (focus != null && !focus.isCoordinatesTimeOverflow()) {
                final double appMagCamera, appMagEarth;
                synchronized (updateLock) {
                    focusBak = focus;
                    this.focus.getAbsolutePosition(aux4b);
                    // Hack, fix this by understanding underlying problem
                    if (!aux4b.hasNaN()) {
                        focusPos.set(aux4b);
                    }
                    dx.set(0, 0, 0);

                    if (Settings.settings.scene.camera.focusLock.position) {
                        // Get focus dx
                        dx.set(nextFocusPosition).sub(focusPos);

                        // Lock orientation - FOR NOW THIS ONLY WORKS WITH
                        // PLANETS and MOONS
                        if (Settings.settings.scene.camera.focusLock.orientation && time.getHdiff() > 0 && focus.getOrientation() != null) {
                            RotationComponent rc = focus.getRotationComponent();
                            if (rc != null) {
                                // Rotation component present - planets, etc
                                double angleBak = rc.angle;
                                double angle = previousOrientationAngle != 0 ? (angleBak - previousOrientationAngle) : 0;
                                // aux5 <- focus (future) position
                                focus.getAbsolutePosition(aux5b);
                                // aux3 <- focus to camera vector
                                aux3b.set(pos).sub(aux5b);
                                // aux2 <- spin axis
                                aux2.set(0, 1, 0).mul(focus.getOrientation());
                                // rotate aux3 around focus spin axis
                                aux3b.rotate(aux2, angle);
                                // aux3 <- camera pos after rotating
                                aux3b.add(aux5b);
                                // pos <- aux3
                                pos.set(aux3b);
                                direction.rotate(aux2, angle);
                                up.rotate(aux2, angle);
                                previousOrientationAngle = angleBak;
                            } else if (focus.getOrientationQuaternion() != null) {
                                Matrix4d ori = focus.getOrientation();
                                // aux5 <- focus (future) position
                                focus.getAbsolutePosition(aux5b);
                                // aux3 <- focus->camera vector
                                aux3b.set(pos).sub(aux5b);
                                // aux3 <- orientation difference from last frame = aux * O * O'^-1
                                aux3b.mul(ori).mul(previousOrientation);
                                // aux3 <- camera pos after rotating
                                aux3b.add(aux5b);
                                // pos <- aux3
                                pos.set(aux3b);
                                direction.mul(ori).mul(previousOrientation);
                                up.mul(ori).mul(previousOrientation);
                                // Set ori to this frame's inv(ori)
                                previousOrientation.set(ori).inv();
                            }

                        }

                        // Add dx to camera position
                        pos.add(dx);

                    }

                    // aux4b <- foucs.abspos + dx
                    this.focus.getAbsolutePosition(aux4b).add(dx);

                    if (!Settings.settings.runtime.openVr) {
                        if (!diverted) {
                            directionToTarget(dt, aux4b, Settings.settings.scene.camera.turn / (Settings.settings.scene.camera.cinematic ? 1e3f : 1e2f));
                        } else {
                            updateRotationFree(dt, Settings.settings.scene.camera.turn);
                        }
                        updateRoll(dt, Settings.settings.scene.camera.turn);
                    }

                    updatePosition(dt, translateUnits, realTransUnits);
                    updateRotation(dt, aux4b);

                    // Update focus direction
                    focusDirection.set(aux4b).sub(pos).nor();
                    focus = focusBak;

                    double dist = aux4b.dstd(pos);
                    if (dist < focus.getRadius()) {
                        // aux2 <- focus-cam with a length of radius
                        aux2b.set(pos).sub(aux4b).nor().scl(focus.getRadius());
                        // Correct camera position
                        pos.set(aux4b).add(aux2b);
                    }

                    // Track
                    if (!Settings.settings.runtime.openVr && this.trackingObject != null && this.trackingName != null) {
                        // Track the tracking object
                        this.trackingObject.getAbsolutePosition(trackingName, aux5b);
                        directionToTrackingObject(aux5b);
                    }

                    // Apparent magnitude from camera
                    appMagCamera = computeFocusApparentMagnitudeCamera();
                    // Apparent magnitude from Earth (planets, etc)
                    appMagEarth = computeFocusApparentMagnitudeEarth();
                }

                EventManager.publish(Event.FOCUS_INFO_UPDATED, this, focus.getDistToCamera() - focus.getRadius(), focus.getViewAngle(), focus.getAlpha(), focus.getDelta(), focus.getAbsolutePosition(aux2b).lend() - focus.getRadius(), appMagCamera, appMagEarth);
            } else {
                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
            }
            break;
        case GAME_MODE:
            synchronized (updateLock) {
                if (gravity && (closestBody != null) && closestBody instanceof Planet && !currentMouseKbdListener.isKeyPressed(Input.Keys.SPACE)) {
                    // Add gravity to force, pulling to the closest body
                    final Vector3b camObj = closestBody.getAbsolutePosition(aux1b).sub(pos);
                    final double dist = camObj.lend();
                    // Gravity acts only at twice the radius, in planets
                    if (dist < closestBody.getRadius() * 2d) {
                        force.add(camObj.nor().scl(0.002d));
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
                updatePosition(dt, translateUnits, Settings.settings.scene.camera.targetMode ? realTransUnits : 1);
                if (!Settings.settings.runtime.openVr) {
                    // If target is present, update direction
                    if (freeTargetOn) {
                        directionToTarget(dt, freeTargetPos, Settings.settings.scene.camera.turn / (Settings.settings.scene.camera.cinematic ? 1e3d : 1e2d));
                        if (facingFocus) {
                            freeTargetOn = false;
                        }
                    }

                    // Update direction with pitch, yaw, roll
                    updateRotationFree(dt, Settings.settings.scene.camera.turn);
                    updateRoll(dt, Settings.settings.scene.camera.turn);
                }
                updateLateral(dt, translateUnits);
            }
            break;
        case GAIA_SCENE_MODE:
            if (entity1 == null || entity2 == null) {
                entity1 = (CelestialBody) GaiaSky.instance.sceneGraph.getNode("Gaia");
                entity2 = (CelestialBody) GaiaSky.instance.sceneGraph.getNode("Earth");
                entity3 = (CelestialBody) GaiaSky.instance.sceneGraph.getNode("Mars");
            }
            SceneGraphNode focusCopy = entity1.getLineCopy();
            focusCopy.getRoot().translation.set(0, 0, 0);
            focusCopy.getRoot().update(time, null, this);
            focusCopy.translation.put(this.pos);

            this.pos.add(0, 0, entity1.getRadius() * 5.0);
            this.posinv.set(this.pos).scl(-1);
            this.direction.set(0, 0, -1);
            this.up.set(0, 1, 0);
            closestBody = entity1;

            // Return to pool
            SceneGraphNode ape = focusCopy;
            do {
                ape.returnToPool();
                ape = ape.parent;
            } while (ape != null);

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
    public void camOrientProjection(float yaw, float pitch, float roll) {
        if (projectionFlag) {
            // yaw - rotate to the right
            direction.rotate(up, -yaw);

            // pitch - rotate up
            aux1.set(direction).crs(up);
            direction.rotate(aux1, pitch);
            up.rotate(aux1, pitch);

            // roll - clockwise
            up.rotate(direction, roll);
            projectionFlag = false;
        }
    }

    /**
     * Updates the perspective camera float values from the computed double vectors
     */
    protected void updatePerspectiveCamera() {
        camera.position.set(0f, 0f, 0f);
        camera.direction.set(direction.valuesf());
        camera.up.set(up.valuesf());
        camera.update();

        posinv.set(pos).scl(-1);
    }

    /**
     * Adds a forward movement by the given amount.
     *
     * @param amount Positive for forward force, negative for backward force.
     */
    public void addForwardForce(double amount) {
        double tu = speedScaling();
        if (amount <= 0) {
            // Avoid getting stuck in surface
            tu = Math.max(10d * Constants.M_TO_U, tu);
        }
        if (parent.mode == CameraMode.FOCUS_MODE) {
            desired.set(focusDirection);
        } else {
            desired.set(direction);
        }

        desired.nor().scl(amount * tu * (Settings.settings.runtime.openVr ? 10 : 100));
        force.add(desired);
        // We reset the time counter
        lastFwdTime = 0;
        lastFwdAmount = amount;
    }

    /**
     * Sets the gamepad velocity as it comes from the joystick sensor.
     *
     * @param amount The amount in [-1, 1].
     */
    public void setVelocity(double amount) {
        velocityGamepad = amount;
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
    public void setVelocityVR(Vector3 p0, Vector3 p1, double amountX, double amountY) {
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

    public void forward(double amount, double minTu) {
        double speedScaling = speedScaling(minTu);
        desired.set(direction).nor().scl(amount * speedScaling);
        vel.add(desired).clamp(0, 5e12);
        lastFwdTime = 0;
    }

    public void strafe(double amount, double minTu) {
        double speedScaling = speedScaling(minTu);
        desired.set(direction).crs(up).nor().scl(amount * speedScaling);
        vel.add(desired).clamp(0, 5e12);
        lastFwdTime = 0;
    }

    public void vertical(double amount) {
        vertical(amount, 0);
    }

    public void vertical(double amount, double minTu) {
        double speedScaling = speedScaling(minTu);
        desired.set(up).nor().scl(amount * speedScaling);
        vel.add(desired).clamp(0, 5e12);
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
    public void addRotateMovement(double deltaX, double deltaY, boolean focusLookKeyPressed, boolean acceleration) {
        // Just update yaw with X and pitch with Y
        if (parent.mode.equals(CameraMode.FREE_MODE)) {
            deltaX *= fovFactor;
            deltaY *= fovFactor;
            addYaw(deltaX, acceleration);
            addPitch(deltaY, acceleration);
        } else if (parent.mode.equals(CameraMode.FOCUS_MODE)) {
            double th = 30;
            double solidAngleDeg = Math.toDegrees(focus.getViewAngle());

            if (focusLookKeyPressed) {
                diverted = true;
                addYaw(deltaX * fovFactor, acceleration);
                addPitch(deltaY * fovFactor, acceleration);
            } else {
                // This factor slows the rotation as the focus gets closer and closer
                double factor = solidAngleDeg > th ? Math.pow(th / solidAngleDeg, 2d) : 1.0;
                addHorizontal(deltaX * factor, acceleration);
                addVertical(deltaY * factor, acceleration);
            }
        }
    }

    public void addAmount(Vector3d vec, double amount, boolean x) {
        if (x)
            vec.x += amount;
        else
            vec.y = amount;
    }

    /**
     * Adds the given amount to the camera yaw acceleration
     **/
    public void addYaw(double amount, boolean acceleration) {
        addAmount(yaw, amount, acceleration);
    }

    public void setYaw(double amount) {
        yaw.x = 0;
        yaw.y = amount;
    }

    /**
     * Adds the given amount to the camera pitch acceleration
     **/
    public void addPitch(double amount, boolean acceleration) {
        addAmount(pitch, amount, acceleration);
    }

    public void setPitch(double amount) {
        pitch.x = 0;
        pitch.y = amount;
    }

    /**
     * Adds the given amount to the camera roll acceleration
     **/
    public void addRoll(double amount, boolean acceleration) {
        addAmount(roll, amount, acceleration);
    }

    public void setRoll(double amount) {
        roll.x = 0;
        roll.y = amount;
    }

    /**
     * Adds the given amount to camera horizontal rotation around the focus
     * acceleration, or pan in free mode
     **/
    public void addHorizontal(double amount, boolean acceleration) {
        addAmount(horizontal, amount, acceleration);
    }

    public void setHorizontal(double amount) {
        horizontal.x = 0;
        horizontal.y = amount * fovFactor;
    }

    /**
     * Adds the given amount to camera vertical rotation around the focus
     * acceleration, or pan in free mode
     **/
    public void addVertical(double amount, boolean acceleration) {
        addAmount(vertical, amount, acceleration);
    }

    public void setVertical(double amount) {
        vertical.x = 0;
        vertical.y = amount * fovFactor;
    }

    /**
     * Stops the camera movement.
     *
     * @return True if the camera had any movement at all and it has been stopped.
     * False if camera was already still.
     */
    public boolean stopMovement() {
        boolean stopped = (vel.len2() != 0 || yaw.y != 0 || pitch.y != 0 || roll.y != 0 || vertical.y != 0 || horizontal.y != 0);
        force.setZero();
        vel.setZero();
        yaw.y = 0;
        pitch.y = 0;
        roll.y = 0;
        horizontal.y = 0;
        vertical.y = 0;
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
    }

    public void stopRotateMovement() {
        yaw.setZero();
        pitch.setZero();
        horizontal.setZero();
        vertical.setZero();
    }

    /**
     * Stops the camera movement.
     */
    public void stopForwardMovement() {
        boolean stopped = (vel.len2() != 0);
        force.setZero();
        vel.setZero();
    }

    /**
     * Updates the position of this entity using the current force
     */
    protected void updatePosition(double dt, double multiplier, double transUnits) {
        boolean cinematic = Settings.settings.scene.camera.cinematic;
        // Calculate velocity if coming from gamepad
        if (velocityGamepad != 0) {
            vel.set(direction).nor().scl(velocityGamepad * gamepadMultiplier * multiplier);
        } else if (velocityVRX != 0 || velocityVRY != 0) {
            aux1.set(velocityVR1).sub(velocityVR0).nor();

            // p0-p1 direction (Y)
            vel.set(aux1).scl(velocityVRY * gamepadMultiplier * multiplier);

            // cross(p0,p1) direction (X)
            aux1.crs(up).nor().scl(velocityVRX * gamepadMultiplier * multiplier);
            vel.add(aux1);
        }

        double forceLen = force.lend();
        double velocity = vel.len();

        // Half a second after we have stopped zooming, real friction kicks in
        if (fullStop) {
            double counterAmount = lastFwdAmount < 0 && cinematic ? Math.min(transUnits, 200) : 2;
            if (getMode().isFocus() && lastFwdAmount > 0) {
                double factor = cinematic ? 100 : 1;
                counterAmount *= factor / ((focus.getDistToCamera() - focus.getRadius()) / focus.getRadius());
            }
            friction.set(vel).nor().scl(-velocity * counterAmount * dt);
        } else {
            friction.set(force).nor().scl(-forceLen * dt);
        }

        force.add(friction);

        if (lastFwdTime > (cinematic ? 250 : currentMouseKbdListener.getResponseTime()) && velocityGamepad == 0 && velocityVRX == 0 && velocityVRY == 0 && fullStop || lastFwdAmount > 0 && transUnits == 0) {
            stopForwardMovement();
        }

        if (thrust != 0)
            force.add(thrust).scl(thrustDirection);
        applyForce(force);

        if (!(force.isZero() && velocity == 0 && accel.isZero())) {
            vel.add(accel.scl(dt));

            // Clamp to top speed
            if (Settings.settings.scene.camera.speedLimit > 0 && vel.len() > Settings.settings.scene.camera.speedLimit) {
                vel.clamp(0, Settings.settings.scene.camera.speedLimit);
            }

            // Velocity changed direction
            if (lastVel.dot(vel) < 0) {
                vel.setZero();
            }

            velocity = vel.len();

            if (parent.mode.equals(CameraMode.FOCUS_MODE)) {
                // Use direction vector as velocity so that if we turn the
                // velocity also turns
                double sign = Math.signum(vel.dot(focusDirection));
                vel.set(focusDirection).nor().scl(sign * velocity);
            }

            vel.clamp(0, multiplier);
            // Aux1 is the step to take
            aux1b.set(vel).scl(dt);
            // Aux2 contains the new position
            pos.add(aux1b);

            accel.setZero();

            lastVel.set(vel);
            force.setZero();
        }
        posinv.set(pos).scl(-1);
    }

    private void posDistanceCheck() {
        // Check terrain collision
        if (closestBody != null) {
            // New position
            closestBody.getPredictedPosition(aux5b, GaiaSky.instance.time, this, false);

            double h = closestBody.getHeight(pos, aux5b);
            double hs = closestBody.getHeightScale() * Settings.settings.scene.renderer.elevation.multiplier;
            double minDist = h + hs / 10.0;
            double newDist = aux5b.scl(-1).add(pos).lend();
            if (newDist < minDist) {
                aux5b.nor().scl(minDist - newDist);
                pos.add(aux5b);
                posinv.set(pos).scl(-1);
            }
        }
    }

    /**
     * Updates the rotation for the free camera.
     */
    private void updateRotationFree(double dt, double rotateSpeed) {
        // Add position to compensate for coordinates centered on camera
        if (updatePosition(pitch, dt)) {
            // Pitch
            aux1.set(direction).crs(up).nor();
            rotate(aux1, pitch.z * rotateSpeed);
        }
        if (updatePosition(yaw, dt)) {
            // Yaw
            rotate(up, -yaw.z * rotateSpeed);
        }

        defaultState(pitch, !Settings.settings.scene.camera.cinematic && !inputByController);
        defaultState(yaw, !Settings.settings.scene.camera.cinematic && !inputByController);
    }

    private void updateRoll(double dt, double rotateSpeed) {
        if (updatePosition(roll, dt)) {
            // Roll
            rotate(direction, -roll.z * rotateSpeed);
        }
        defaultState(roll, !Settings.settings.scene.camera.cinematic && !inputByController);
    }

    /**
     * Updates the direction vector using the pitch, yaw and roll forces.
     */
    private void updateRotation(double dt, final Vector3b rotationCenter) {
        // Add position to compensate for coordinates centered on camera
        // rotationCenter.add(pos);
        if (updatePosition(vertical, dt)) {
            // Pitch
            aux1.set(direction).crs(up).nor();
            rotateAround(rotationCenter, aux1, vertical.z * Settings.settings.scene.camera.rotate);
        }
        if (updatePosition(horizontal, dt)) {
            // Yaw
            rotateAround(rotationCenter, up, -horizontal.z * Settings.settings.scene.camera.rotate);
        }

        defaultState(vertical, !Settings.settings.scene.camera.cinematic && !inputByController);
        defaultState(horizontal, !Settings.settings.scene.camera.cinematic && !inputByController);

    }

    private void defaultState(Vector3d vec, boolean resetVelocity) {
        // Always reset acceleration
        vec.x = 0;

        // Reset velocity if needed
        if (resetVelocity)
            vec.y = 0;
    }

    private void updateLateral(double dt, double translateUnits) {
        // Pan with hor
        aux1.set(direction).crs(up).nor();
        aux1.scl(horizontal.y * gamepadMultiplier * dt * translateUnits);
        aux2.set(up).nor().scl(vertical.y * gamepadMultiplier * dt * translateUnits);
        aux1.add(aux2);
        translate(aux1);

    }

    /**
     * Updates the given accel/vel/pos of the angle using dt.
     */
    private boolean updatePosition(Vector3d angle, double dt) {
        if (angle.x != 0 || angle.y != 0) {
            // Calculate velocity from acceleration
            angle.y += angle.x * dt;
            // Cap velocity
            angle.y = Math.signum(angle.y) * Math.abs(angle.y);
            // Update position
            angle.z = (angle.y * dt) % 360f;
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
    private void directionToTarget(double dt, final Vector3b target, double turnVelocity) {
        desired.set(target).sub(pos).nor();
        double desiredDirectionAngle = desired.angle(direction);
        if (desiredDirectionAngle > Math.min(0.3, 0.3 * fovFactor)) {
            // Add desired to direction with given turn velocity (v*dt)
            desired.scl(turnVelocity * dt);
            direction.add(desired).nor();

            // Update up so that it is always perpendicular
            aux1.set(direction).crs(up);
            up.set(aux1).crs(direction).nor();
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
    private void directionToTrackingObject(final Vector3b target) {
        desired.set(target).sub(pos).nor();
        direction.set(desired);
        aux1.set(direction).crs(up);
        up.set(aux1).crs(direction).nor();
        facingFocus = false;
    }

    private void setMouseKbdListener(MouseKbdListener newListener) {
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
     * Updates the camera mode
     */
    @Override
    public void updateMode(ICamera previousCam, CameraMode previousMode, CameraMode newMode, boolean centerFocus, boolean postEvent) {
        InputProcessor ip = Gdx.input.getInputProcessor();
        if (ip instanceof InputMultiplexer) {
            InputMultiplexer im = (InputMultiplexer) ip;
            switch (newMode) {
            case FOCUS_MODE:
                diverted = !centerFocus;
                checkFocus();
            case FREE_MODE:
            case GAIA_SCENE_MODE:
            case GAME_MODE:
                MouseKbdListener newListener = newMode == CameraMode.GAME_MODE ? gameMouseKbdListener : naturalMouseKbdListener;
                setMouseKbdListener(newListener);
                addControllerListener();
                if (Settings.settings.runtime.openVr)
                    GaiaSky.instance.vrContext.addListener(openVRListener);
                break;
            default:
                // Unregister input controllers
                im.removeProcessor(currentMouseKbdListener);
                removeControllerListener();
                // Remove vr listener
                if (Settings.settings.runtime.openVr)
                    GaiaSky.instance.vrContext.removeListener(openVRListener);
                break;
            }
        }
    }

    public NaturalControllerListener getControllerListener() {
        return controllerListener;
    }

    public void addControllerListener() {
        Settings.settings.controls.gamepad.addControllerListener(controllerListener);
        controllerListener.activate();
    }

    public void removeControllerListener() {
        Settings.settings.controls.gamepad.removeControllerListener(controllerListener);
        controllerListener.deactivate();
    }

    public void setFocus(IFocus focus) {
        if (focus != null && GaiaSky.instance.isOn(focus.getCt())) {
            this.focus = focus;
            this.focus.makeFocus();
            // Reset facing focus
            this.facingFocus = false;
            // Create event to notify focus change
            EventManager.publish(Event.FOCUS_CHANGED, this, focus);
        }
    }

    /**
     * The speed scaling function.
     *
     * @param min The minimum speed.
     *
     * @return The speed scaling.
     */
    public double speedScaling(double min) {
        double dist;
        double starEdge = 0.5 * Constants.PC_TO_U;
        if (parent.mode.useFocus() && focus != null) {
            // FOCUS mode -> use focus object
            dist = focus.getDistToCamera() - (focus.getHeight(pos, false) + MIN_DIST);
        } else if (parent.mode.useClosest() && proximity.effective[0] != null) {
            // FREE/GAME mode -> use closest object
            if (closestBody != null && closestBody.getDistToCamera() < proximity.effective[0].getDistToCamera()) {
                dist = closestBody.getDistToCamera() - (closestBody.getHeight(pos, false) + MIN_DIST);
            } else if (proximity.effective[0] != null && !proximity.effective[0].isStar() && (proximity.effective[0].getClosestDistToCamera() + MIN_DIST) < starEdge) {
                dist = distance * Math.pow((proximity.effective[0].getClosestDistToCamera() + MIN_DIST) / starEdge, 1.6);
            } else {
                dist = distance;
            }
        } else {
            dist = distance;
        }

        double func;
        if (dist < DIST_A) {
            // d < 0.1 pc
            func = MathUtilsd.lint(dist, 0, DIST_A, 0, 1e6) * Constants.DISTANCE_SCALE_FACTOR;
        } else if (dist < DIST_B) {
            // 0.1 pc < d < 5 Kpc
            func = MathUtilsd.lint(dist, DIST_A, DIST_B, 1e6, 1e10) * Constants.DISTANCE_SCALE_FACTOR;
        } else {
            // d > 5 Kpc
            func = MathUtilsd.lint(dist, DIST_B, DIST_C, 1e10, 2e16) * Constants.DISTANCE_SCALE_FACTOR;
        }

        return dist > 0 ? Math.max(func, min) * Settings.settings.scene.camera.speed : 0;
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
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case FOCUS_CHANGE_CMD:
            setTrackingObject(null, null);
            // Check the type of the parameter: IFocus or String
            IFocus focus = null;

            // Center focus or not
            boolean centerFocus = !Settings.settings.runtime.openVr;
            if (data.length > 1)
                centerFocus = (Boolean) data[1];

            if (data[0] instanceof String) {
                SceneGraphNode sgn = GaiaSky.instance.sceneGraph.getNode((String) data[0]);
                if (sgn instanceof IFocus) {
                    focus = (IFocus) sgn;
                    diverted = !centerFocus;
                }
            } else if (data[0] instanceof IFocus) {
                focus = (IFocus) data[0];
                diverted = !centerFocus;
            }
            setFocus(focus);

            checkFocus();

            break;
        case FOV_CHANGED_CMD:
            boolean checkMax = source instanceof Actor;
            float fov = MathUtilsd.clamp((float) data[0], Constants.MIN_FOV, checkMax ? Constants.MAX_FOV : 179f);

            for (PerspectiveCamera cam : cameras) {
                cam.fieldOfView = fov;
            }
            fovFactor = camera.fieldOfView / 40f;
            if (parent.current == this) {
                EventManager.publish(Event.FOV_CHANGE_NOTIFICATION, this, fov, fovFactor);
            }
            break;
        case CUBEMAP_CMD:
            boolean state = (boolean) data[0];
            CubemapProjection p = (CubemapProjection) data[1];
            if (p.isPlanetarium() && state && !Settings.settings.runtime.openVr) {
                fovBackup = GaiaSky.instance.cameraManager.getCamera().fieldOfView;
            }
            break;
        case CAMERA_POS_CMD:
            synchronized (updateLock) {
                pos.set((double[]) data[0]);
                posinv.set(pos).scl(-1d);
            }
            break;
        case CAMERA_DIR_CMD:
            synchronized (updateLock) {
                direction.set((double[]) data[0]).nor();
            }
            break;
        case CAMERA_UP_CMD:
            synchronized (updateLock) {
                up.set((double[]) data[0]).nor();
            }
            break;
        case CAMERA_PROJECTION_CMD:
            synchronized (updateLock) {
                // Position
                pos.set((double[]) data[0]);
                posinv.set(pos).scl(-1d);
                // Direction
                direction.set((double[]) data[1]).nor();
                // Up
                up.set((double[]) data[2]).nor();
                // Change projection flag
                projectionFlag = true;
            }
            break;
        case CAMERA_FWD:
            synchronized (updateLock) {
                addForwardForce((double) data[0]);
            }
            break;
        case CAMERA_ROTATE:
            synchronized (updateLock) {
                addRotateMovement((double) data[0], (double) data[1], false, true);
            }
            break;
        case CAMERA_TURN:
            synchronized (updateLock) {
                addRotateMovement((double) data[0], (double) data[1], true, true);
            }
            break;
        case CAMERA_PAN:

            break;
        case CAMERA_ROLL:
            synchronized (updateLock) {
                addRoll((double) data[0], Settings.settings.scene.camera.cinematic);
            }
            break;
        case CAMERA_STOP:
            synchronized (updateLock) {
                stopTotalMovement();
            }
            break;
        case CAMERA_CENTER:
            synchronized (updateLock) {
                diverted = false;
            }
            break;
        case GO_TO_OBJECT_CMD:
            if (this.focus != null) {
                final IFocus f = this.focus;
                GaiaSky.postRunnable(() -> {
                    setTrackingObject(null, null);
                    // Position camera near focus
                    stopTotalMovement();

                    f.getAbsolutePosition(aux1b);
                    pos.set(aux1b);

                    double dx = 0d;
                    double dy = f.getSize() / 4d;
                    double dz = -f.getSize() * 4d;
                    if (Settings.settings.runtime.openVr) {
                        dz = -dz;
                    }

                    pos.add(dx, dy, dz);
                    posinv.set(pos).scl(-1d);
                    direction.set(aux1b).sub(pos).nor();
                    up.set(direction.x, direction.z, -direction.y).nor();
                    rotate(up, 0.01);
                    updatePerspectiveCamera();
                });
            }
            break;
        case ORIENTATION_LOCK_CMD:
            synchronized (updateLock) {
                previousOrientationAngle = 0;
            }
            break;
        case FREE_MODE_COORD_CMD:
            synchronized (updateLock) {
                double ra = (Double) data[0];
                double dec = (Double) data[1];
                double dist = 1e12d * Constants.PC_TO_U;
                aux1.set(MathUtilsd.degRad * ra, MathUtilsd.degRad * dec, dist);
                Coordinates.sphericalToCartesian(aux1, aux2);
                freeTargetPos.set(aux2);
                facingFocus = false;
                freeTargetOn = true;
            }
            break;
        case FOCUS_NOT_AVAILABLE:
            if (getMode().isFocus()) {
                boolean found = false;
                if (data[0] instanceof IFocus) {
                    focus = (IFocus) data[0];
                    found = isFocus(focus);
                } else if (data[0] instanceof OctreeWrapper) {
                    OctreeWrapper octree = (OctreeWrapper) data[0];
                    OctreeNode octant = this.focus.getOctant();
                    if (octant != null && octant.getRoot() == octree.root) {
                        found = true;
                    }
                } else if (data[0] instanceof GenericCatalog) {
                    GenericCatalog gc = (GenericCatalog) data[0];
                    if (gc.children != null && gc.children.contains((SceneGraphNode) this.focus, true)) {
                        found = true;
                    }
                }
                if (found) {
                    // Set camera  free
                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                }
            }
            break;
        case TOGGLE_VISIBILITY_CMD:
            if (getMode().isFocus()) {
                ComponentType ct = ComponentType.getFromKey((String) data[0]);
                if (this.focus != null && ct != null && this.focus.getCt().isEnabled(ct)) {
                    // Set camera  free
                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                }
            }
            break;
        case CAMERA_CENTER_FOCUS_CMD:
            synchronized (updateLock) {
                setCenterFocus((Boolean) data[0]);
            }
            break;
        case CONTROLLER_CONNECTED_INFO:
            Settings.settings.controls.gamepad.addControllerListener(controllerListener, (String) data[0]);
            break;
        case CONTROLLER_DISCONNECTED_INFO:
            // Nothing
            break;
        case NEW_DISTANCE_SCALE_FACTOR:
            synchronized (updateLock) {
                DIST_A = 0.1 * Constants.PC_TO_U;
                DIST_B = 5.0 * Constants.KPC_TO_U;
                DIST_C = 5000.0 * Constants.MPC_TO_U;
            }
            break;
        case CAMERA_TRACKING_OBJECT_CMD:
            final IFocus newTrackingObject = (IFocus) data[0];
            final String newTrackingName = (String) data[1];
            synchronized (updateLock) {
                this.setTrackingObject(newTrackingObject, newTrackingName != null ? newTrackingName.toLowerCase() : null);
            }
            break;
        default:
            break;
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
    public void rotateAround(final Vector3b rotationCenter, Vector3d rotationAxis, double angle) {
        rotate(rotationAxis, angle);

        // aux3 <- pos-point vector
        aux3b.set(pos).sub(rotationCenter);
        aux3b.rotate(rotationAxis, angle);
        pos.set(aux3b).add(rotationCenter);
        posDistanceCheck();
    }

    public void rotate(Vector3d axis, double angle) {
        direction.rotate(axis, angle);
        up.rotate(axis, angle);
    }

    /**
     * Moves the camera by the given amount on each axis.
     *
     * @param x the displacement on the x-axis
     * @param y the displacement on the y-axis
     * @param z the displacement on the z-axis
     */
    public void translate(double x, double y, double z) {
        pos.add(x, y, z);
        posDistanceCheck();
    }

    /**
     * Moves the camera by the given vector.
     *
     * @param vec the displacement vector
     */
    public void translate(Vector3d vec) {
        pos.add(vec);
        posDistanceCheck();
    }

    /**
     * Applies the given force to this entity's acceleration.
     *
     * @param force The force.
     */
    protected void applyForce(Vector3b force) {
        if (force != null) {
            accel.add(force);
        }
    }

    @Override
    public PerspectiveCamera[] getFrontCameras() {
        return new PerspectiveCamera[] { camera };
    }

    @Override
    public PerspectiveCamera getCamera() {
        return camera;
    }

    @Override
    public Vector3d getDirection() {
        return direction;
    }

    @Override
    public void setDirection(Vector3d dir) {
        this.direction.set(dir);
    }

    @Override
    public Vector3d getUp() {
        return up;
    }

    public void setUp(Vector3d up) {
        this.up.set(up);
    }

    @Override
    public Vector3d[] getDirections() {
        return new Vector3d[] { direction };
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
    public boolean isFocus(IFocus focus) {
        return this.focus != null && this.focus == focus;
    }

    @Override
    public IFocus getFocus() {
        return getMode().equals(CameraMode.FOCUS_MODE) ? this.focus : null;
    }

    /**
     * Checks the position of the camera does not collide with the focus object.
     */
    public void checkFocus() {
        if (focus != null && !(focus instanceof Star) && !(focus instanceof ParticleGroup)) {
            // Move camera if too close to focus
            this.focus.getAbsolutePosition(aux1b);
            if (pos.dstd(aux1b, aux2b) < this.focus.getRadius()) {
                // Position camera near focus
                stopTotalMovement();

                this.focus.getAbsolutePosition(aux1b);
                pos.set(aux1b);

                pos.add(0d, 0d, -this.focus.getSize() * 6d);
                posinv.set(pos).scl(-1d);
                direction.set(0d, 0d, 1d);
            }
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
        if (focus instanceof CelestialBody) {
            // m - H = 5 * log10(r * D) + g
            // m: apparent magnitude
            // H: absolute magnitude
            // r: dist to star [au]
            // D: dist to Earth [au]
            // g: term for phase effects (~0)
            double distCamAu = pos.put(aux4b).sub(focus.getAbsolutePosition(aux5b)).lend() * Constants.U_TO_AU;
            SceneGraphNode starAncestor = focus.getFirstStarAncestor();
            double distStarAu = (starAncestor != null ? starAncestor.getAbsolutePosition(aux4b).sub(focus.getAbsolutePosition(aux5b)).lend() : focus.getAbsolutePosition(aux5b).lend()) * Constants.U_TO_AU;
            return 5d * Math.log10(distStarAu * distCamAu) + focus.getAbsmag();

        } else {
            // m - M = 5 * log10(d) - 5
            // m: apparent magnitude
            // M: absolute magnitude
            // d: distance [pc]
            return 5d * Math.log10(focus.getDistToCamera() * Constants.U_TO_PC) - 5d + focus.getAbsmag();
        }
    }

    /**
     * Computes the apparent magnitude of the current focus object as seen from Earth.
     *
     * @return The magnitude.
     */
    private double computeFocusApparentMagnitudeEarth() {
        // Apparent magnitude from Earth (planets, etc)
        ISceneGraph sg = GaiaSky.instance.sceneGraph;
        SceneGraphNode earth = sg.getNode("Earth");
        if (focus instanceof CelestialBody && earth != null) {
            // Distance between earth and the body
            // Apparent magnitude in Solar System bodies
            // m - H = 5 * log10(r * D) + g
            // m: apparent magnitude
            // H: absolute magnitude
            // r: dist to star [AU]
            // D: dist to Earth [AU]
            // g: term for phase effects (~0)
            double distEarthAu = earth.getAbsolutePosition(aux4b).sub(focus.getAbsolutePosition(aux5b)).lend() * Constants.U_TO_AU;
            SceneGraphNode starAncestor = focus.getFirstStarAncestor();
            double distStarAu = (starAncestor != null ? starAncestor.getAbsolutePosition(aux4b).sub(focus.getAbsolutePosition(aux5b)).lend() : focus.getAbsolutePosition(aux5b).lend()) * Constants.U_TO_AU;
            return 5d * Math.log10(distStarAu * distEarthAu) + focus.getAbsmag();
        } else {
            return Double.NaN;
        }
    }

    public void setInputByController(boolean controller) {
        this.inputByController = controller;
    }

    @Override
    public void setCamera(PerspectiveCamera perspectiveCamera) {
        this.camera = perspectiveCamera;
    }

    public void setThrust(double thrust, int direction) {
        this.thrust = thrust;
        this.thrustDirection = direction;
    }

    @Override
    public void render(int rw, int rh) {

        boolean modeStereo = Settings.settings.program.modeStereo.active;
        boolean modeStereoVR = modeStereo && Settings.settings.program.modeStereo.isStereoVR();
        boolean modeCubemap = Settings.settings.program.modeCubemap.active;
        boolean modeFisheye = Settings.settings.postprocess.fisheye;
        boolean modeVR = Settings.settings.runtime.openVr;

        if(modeStereoVR) {
            // No pointer guides or cross-hairs
            return;
        }

        // Pointer guides
        if (Settings.settings.program.pointer.guides.active && !modeStereo && !modeFisheye && !modeCubemap && !modeVR) {
            int mouseX = Gdx.input.getX();
            int mouseY = rh - Gdx.input.getY();
            shapeRenderer.begin(ShapeType.Line);
            Gdx.gl.glEnable(GL30.GL_BLEND);
            Gdx.gl.glLineWidth(Settings.settings.program.pointer.guides.width);
            float[] pc = Settings.settings.program.pointer.guides.color;
            shapeRenderer.setColor(pc[0], pc[1], pc[2], pc[3]);
            shapeRenderer.line(0, mouseY, rw, mouseY);
            shapeRenderer.line(mouseX, 0, mouseX, rh);
            shapeRenderer.end();
        }


        spriteBatch.begin();

        boolean decal = modeCubemap || modeStereo || modeFisheye || modeVR;
        float chScale = 1f;
        if(modeCubemap) {
            chScale = 4f;
        } else if(modeFisheye) {
            chScale = 2f;
        }
        // Mark home in ORANGE
        if (Settings.settings.scene.crosshair.home) {
            if (home == null && GaiaSky.instance.sceneGraph != null)
                home = GaiaSky.instance.sceneGraph.findFocus(Settings.settings.scene.homeObject);
            if (home != null) {
                drawCrosshair(spriteBatch, home, decal, false, spriteHome, crosshairArrow, chScale, rw, rh, 1f, 0.7f, 0.1f, 1f);
            }
        }

        // Mark closest object in BLUE
        if (Settings.settings.scene.crosshair.closest && closest != null) {
            drawCrosshair(spriteBatch, closest, decal, false, spriteClosest, crosshairArrow, chScale, rw, rh, 0.3f, 0.5f, 1f, 1f);
        }

        // Mark the focus in GREEN
        if (Settings.settings.scene.crosshair.focus && getMode().isFocus()) {
            // Green, focus mode
            drawCrosshair(spriteBatch, focus, decal, true, spriteFocus, crosshairArrow, chScale, rw, rh, 0.2f, 1f, 0.4f, 1f);
        }

        // Gravitational waves crosshair
        if (Settings.settings.runtime.gravitationalWaves) {
            RelativisticEffectsManager gw = RelativisticEffectsManager.getInstance();

            float chw = gravWaveCrosshair.getWidth();
            float chh = gravWaveCrosshair.getHeight();
            float chw2 = chw / 2;
            float chh2 = chh / 2;

            aux1.set(gw.gw).nor().scl(1e12).add(posinv);

            GlobalResources.applyRelativisticAberration(aux1, this);
            // GravitationalWavesManager.instance().gravitationalWavePos(aux1);

            boolean inside = projectToScreen(aux1, auxf1, rw, rh, chw, chh, chw2, chh2);

            if (inside) {
                // Cyan
                spriteBatch.setColor(0, 1, 1, 1);
                spriteBatch.draw(gravWaveCrosshair, auxf1.x - chw2, auxf1.y - chh2, chw, chh);
            }
        }

        spriteBatch.end();
    }

    private void drawCrosshairDecal(SpriteBatch batch, IFocus chFocus, Sprite sprite, float r, float g, float b, float a) {
        sprite.setColor(r, g, b, a);
        Vector3b p = chFocus.getClosestAbsolutePos(aux1b).add(posinv);
        Vector3d pos = aux5;
        p.put(pos);
        DecalUtils.drawSprite(sprite, batch, (float) pos.x, (float) pos.y, (float) pos.z, 0.0008d, 1f, this, true, 0.04f, 0.04f);
    }

    private void drawCrosshair(SpriteBatch batch, IFocus chFocus, boolean decal, boolean focusMode, Sprite crosshairSprite, Texture arrowTex, float crosshairScale, int rw, int rh, float r, float g, float b, float a) {
        if (chFocus != null) {
            if (decal) {
                crosshairSprite.setScale(crosshairScale);
                drawCrosshairDecal(batch, chFocus, crosshairSprite, r, g, b, a);
            } else {
                if (!focusMode) {
                    drawCrosshair(chFocus.getClosestAbsolutePos(aux1b).add(posinv), chFocus.getClosestDistToCamera(), chFocus.getRadius(), crosshairSprite.getTexture(), arrowTex, rw, rh, r, g, b, a);
                } else {
                    drawCrosshair(chFocus.getAbsolutePosition(aux1b).add(posinv), chFocus.getDistToCamera(), chFocus.getRadius(), crosshairSprite.getTexture(), arrowTex, rw, rh, r, g, b, a);
                }
            }
        }
    }

    /**
     * Draws a crosshair given a camera-relative position
     *
     * @param p            The position in floating camera coordinates
     * @param distToCam    The distance to the camera
     * @param radius       Radius of object
     * @param crosshairTex Crosshair texture
     * @param arrowTex     Arrow texture
     * @param rw           Width
     * @param rh           Height
     * @param r            Red
     * @param g            Green
     * @param b            Blue
     * @param a            Alpha
     */
    private void drawCrosshair(Vector3b p, double distToCam, double radius, Texture crosshairTex, Texture arrowTex, int rw, int rh, float r, float g, float b, float a) {
        if (distToCam > radius * 2) {
            float chw = crosshairTex.getWidth();
            float chh = crosshairTex.getHeight();
            float chw2 = chw / 2;
            float chh2 = chh / (vr ? 1 : 2);

            Vector3d pos = aux5;
            p.put(pos);
            GlobalResources.applyRelativisticAberration(pos, this);
            RelativisticEffectsManager.getInstance().gravitationalWavePos(pos);

            if (vr) {
                pos.nor().scl(distToCam - radius);
            }
            boolean inside = projectToScreen(pos, auxf1, rw, rh, chw, chh, chw2, chh2);

            spriteBatch.setColor(r, g, b, a);

            if (inside) {
                spriteBatch.draw(crosshairTex, auxf1.x - chw2, auxf1.y - chh2, chw, chh);
            } else {
                if (vr) {
                    float ang = firstAux ? -90f + aux2f2.angleDeg() : firstAngle;
                    if (firstAux) {
                        firstAngle = ang;
                    }
                    firstAux = !firstAux;
                    aux2f2.set(auxf1.x - (rw / 2f), auxf1.y - (rh / 2f));
                    aux2.set(up).rotate(direction, 90).add(up).scl(0.04);
                    aux1.set(vrOffset).add(aux2).scl(1 / Constants.M_TO_U).add(direction);
                    projectToScreen(aux1, auxf1, rw, rh, chw, chh, chw2, chh2);
                    spriteBatch.draw(arrowTex, auxf1.x, auxf1.y, chw2, chh2, chw, chh, 1f, 1f, ang, 0, 0, (int) chw, (int) chw, false, false);
                } else {
                    aux2f2.set(auxf1.x - (rw / 2f), auxf1.y - (rh / 2f));
                    spriteBatch.draw(arrowTex, auxf1.x - chw2, auxf1.y - chh2, chw2, chh2, chw, chh, 1f, 1f, -90f + aux2f2.angleDeg(), 0, 0, (int) chw, (int) chh, false, false);
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
    private boolean projectToScreen(Vector3d vec, Vector3 out, int rw, int rh, float chw, float chh, float chw2, float chh2) {
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
    public void resize(int width, int height) {
        if (!vr) {
            camera.viewportHeight = height;
            camera.viewportWidth = width;
            camera.update(true);
        }
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight);
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, camera.viewportWidth, camera.viewportHeight);
    }

    @Override
    public Vector3d getVelocity() {
        return vel;
    }

    public void setDiverted(boolean diverted) {
        this.diverted = diverted;
    }

    public MouseKbdListener getCurrentMouseKbdListener() {
        return currentMouseKbdListener;
    }

    private void setTrackingObject(final IFocus trackingObject, final String trackingName) {
        this.trackingObject = trackingObject;
        this.trackingName = trackingName;
        EventManager.publish(Event.CAMERA_TRACKING_OBJECT_UPDATE, this, trackingObject, trackingName);
    }
}
