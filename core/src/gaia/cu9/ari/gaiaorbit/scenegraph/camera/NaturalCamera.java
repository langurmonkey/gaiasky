/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.*;
import gaia.cu9.ari.gaiaorbit.render.ComponentTypes.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.*;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.scenegraph.component.RotationComponent;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.OctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.MasterManager;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.OctreeNode;

/**
 * Models the movement of the camera
 *
 * @author Toni Sagrista
 */
public class NaturalCamera extends AbstractCamera implements IObserver {

    private static final double MIN_DIST = 1 * Constants.M_TO_U;

    /**
     * VR offset
     **/
    public Vector3d vroffset;

    /**
     * Acceleration and velocity
     **/
    public Vector3d accel, vel, posbak;
    /**
     * The force acting on the entity and the friction
     **/
    private Vector3d force, friction;

    public Vector3d direction, up, focusDirection;
    /**
     * Indicates whether the camera is facing the focus or not
     **/
    public boolean facingFocus;

    /**
     * Auxiliary double vectors
     **/
    private Vector3d aux1, aux2, aux3, aux5, aux4, dx, nextFocusPosition, nextClosestPosition;
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
     * Fov value backup
     **/
    float fovBackup;

    /** Gravity in game mode **/
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
     * The direction point to seek
     */
    private Vector3d lastvel;
    /**
     * FOCUS_MODE position
     **/
    private Vector3d focusPos;
    /**
     * Free mode target
     **/
    private Vector3d freeTargetPos;
    private boolean freeTargetOn;

    private Vector3d desired;

    /**
     * VR mode stuff
     **/
    private boolean firstAux = true;
    private float firstAngl = 0;

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

    /** Home object as defined in the properties file **/
    private IFocus home;

    /**
     * Holds whether the last input was issued by currentMouseKbdListener. Useful to keep
     * things rolling even if currentMouseKbdListener sticks do not move
     **/
    boolean inputByController = false;

    boolean diverted = false;
    boolean vr = false;

    private float planetariumFocusAngle = 0f;

    public double[] hudScales;
    public Color[] hudColors;
    public int hudColor;
    public float hudw, hudh;
    private static double HUD_SCALE_MIN = 0.5f;
    private static double HUD_SCALE_MAX = 3.0f;

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

    private SpriteBatch spriteBatch;
    private Texture crosshairFocus, crosshairClosest, crosshairHome, crosshairArrow, velocityCrosshair, antivelocityCrosshair, gravWaveCrosshair;
    private Sprite[] hudSprites;

    public NaturalCamera(AssetManager assetManager, CameraManager parent, boolean vr) {
        super(parent);
        vroffset = new Vector3d();
        vel = new Vector3d();
        accel = new Vector3d();
        force = new Vector3d();
        posbak = new Vector3d();
        this.vr = vr;
        initialize(assetManager);

    }

    public void initialize(AssetManager assetManager) {
        camera = new PerspectiveCamera(GlobalConf.scene.CAMERA_FOV, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = (float) CAM_NEAR;
        camera.far = (float) CAM_FAR;

        // init cameras vector
        cameras = new PerspectiveCamera[] { camera, camLeft, camRight };

        fovFactor = camera.fieldOfView / 40f;

        up = new Vector3d(1, 0, 0);
        direction = new Vector3d(0, 1, 0);
        focusDirection = new Vector3d();
        desired = new Vector3d();
        pitch = new Vector3d(0.0f, 0.0f, -3.0291599E-6f);
        yaw = new Vector3d(0.0f, 0.0f, -7.9807205E-6f);
        roll = new Vector3d(0.0f, 0.0f, -1.4423944E-4f);
        horizontal = new Vector3d();
        vertical = new Vector3d();

        friction = new Vector3d();
        lastvel = new Vector3d();
        focusPos = new Vector3d();
        freeTargetPos = new Vector3d();
        freeTargetOn = false;

        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux3 = new Vector3d();
        aux4 = new Vector3d();
        aux5 = new Vector3d();
        auxf1 = new Vector3();
        aux2f2 = new Vector2();

        dx = new Vector3d();
        nextFocusPosition = new Vector3d();
        nextClosestPosition = new Vector3d();

        // Mouse and keyboard listeners
        naturalMouseKbdListener = new NaturalMouseKbdListener(this);
        gameMouseKbdListener = new GameMouseKbdListener(this);
        currentMouseKbdListener = null;
        // Controller listeners
        controllerListener = new NaturalControllerListener(this, GlobalConf.controls.CONTROLLER_MAPPINGS_FILE);
        if (vr)
            openVRListener = new OpenVRListener(this);

        // Init sprite batch for crosshair
        spriteBatch = new SpriteBatch(1000, GlobalResources.spriteShader);

        // Focus crosshair
        crosshairFocus = new Texture(Gdx.files.internal("img/crosshair-focus.png"));
        crosshairFocus.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Closest crosshair
        crosshairClosest = new Texture(Gdx.files.internal("img/crosshair-closest.png"));
        crosshairClosest.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Home crosshair
        crosshairHome = new Texture(Gdx.files.internal("img/crosshair-home.png"));
        crosshairHome.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Arrow crosshair
        crosshairArrow = new Texture(Gdx.files.internal("img/crosshair-arrow.png"));
        crosshairArrow.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Velocity vector crosshair
        velocityCrosshair = new Texture(Gdx.files.internal("img/ai-vel.png"));
        velocityCrosshair.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Antivelocity vector crosshair
        antivelocityCrosshair = new Texture(Gdx.files.internal("img/ai-antivel.png"));
        antivelocityCrosshair.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Grav wave crosshair
        gravWaveCrosshair = new Texture(Gdx.files.internal("img/gravwave-pointer.png"));
        gravWaveCrosshair.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        // Speed HUD
        Texture sHUD = new Texture(Gdx.files.internal("img/hud-corners.png"));
        sHUD.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        hudw = sHUD.getWidth();
        hudh = sHUD.getHeight();

        hudScales = new double[] { HUD_SCALE_MIN, HUD_SCALE_MIN + (HUD_SCALE_MAX - HUD_SCALE_MIN) / 3d, HUD_SCALE_MIN + (HUD_SCALE_MAX - HUD_SCALE_MIN) * 2d / 3d };
        hudSprites = new Sprite[hudScales.length];
        hudColors = new Color[] { Color.WHITE, Color.GREEN, Color.GOLD, Color.LIME, Color.PINK, Color.ORANGE, Color.CORAL, Color.CYAN, Color.FIREBRICK, Color.FOREST };

        for (int i = 0; i < hudScales.length; i++) {
            hudSprites[i] = new Sprite(sHUD);
            hudSprites[i].setOriginCenter();
        }

        // FOCUS_MODE is changed from GUI
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGE_CMD, Events.FOV_CHANGED_CMD, Events.ORIENTATION_LOCK_CMD, Events.CAMERA_POS_CMD, Events.CAMERA_DIR_CMD, Events.CAMERA_UP_CMD, Events.CAMERA_FWD, Events.CAMERA_ROTATE, Events.CAMERA_PAN, Events.CAMERA_ROLL, Events.CAMERA_TURN, Events.CAMERA_STOP, Events.CAMERA_CENTER, Events.GO_TO_OBJECT_CMD, Events.PLANETARIUM_FOCUS_ANGLE_CMD, Events.PLANETARIUM_CMD, Events.FREE_MODE_COORD_CMD, Events.CATALOG_VISIBLE, Events.CATALOG_REMOVE, Events.FOCUS_NOT_AVAILABLE, Events.TOGGLE_VISIBILITY_CMD);
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
        camUpdate(dt, time);
        if (MasterManager.instance != null) {
            // Send camera state
            MasterManager.instance.boardcastCameraAndTime(this.pos, this.direction, this.up, time);
        }
    }

    private void camUpdate(double dt, ITimeFrameProvider time) {
        currentMouseKbdListener.update();
        controllerListener.update();
        if (GlobalConf.runtime.OPENVR)
            openVRListener.update();

        // Next focus and closest positions
        computeNextPositions(time);

        // The whole update thread must lock the value of direction and up
        distance = pos.len();
        CameraMode m = (parent.current == this ? parent.mode : lastMode);
        double realTransUnits = m.isGame() ? getTranslateUnits(1e-5) : getTranslateUnits();
        double translateUnits = Math.max(10d * Constants.M_TO_U, realTransUnits);
        switch (m) {
        case FOCUS_MODE:
            if (focus != null && focus.withinMagLimit() && !focus.isCoordinatesTimeOverflow()) {
                focusBak = focus;
                focus.getAbsolutePosition(aux4);
                // Hack, fix this by understanding underlying problem
                if (!aux4.hasNaN()) {
                    focusPos.set(aux4);
                }
                dx.set(0, 0, 0);

                if (GlobalConf.scene.FOCUS_LOCK) {
                    // Get focus dx
                    dx.set(nextFocusPosition).sub(focusPos);

                    // Lock orientation - FOR NOW THIS ONLY WORKS WITH
                    // PLANETS/MOONS
                    if (GlobalConf.scene.FOCUS_LOCK_ORIENTATION && time.getDt() > 0 && focus.getOrientation() != null) {
                        Double anglebak = null;
                        RotationComponent rc = focus.getRotationComponent();
                        if (rc != null) {
                            // Rotation component present - planets, etc
                            anglebak = rc.angle;
                        } else if (focus.getOrientationQuaternion() != null) {
                            anglebak = focus.getOrientationQuaternion().getPitch();
                        }
                        if (anglebak != null) {
                            Double angle = previousOrientationAngle != 0 ? (anglebak - previousOrientationAngle) : 0;
                            // aux5 <- focus (future) position
                            focus.getAbsolutePosition(aux5);
                            // aux3 <- focus to camera vector
                            aux3.set(pos).sub(aux5);
                            // aux2 <- spin axis
                            aux2.set(0, 1, 0).mul(focus.getOrientation());
                            // rotate aux3 around focus spin axis
                            aux3.rotate(aux2, angle);
                            // aux3 <- camera pos after rotating
                            aux3.add(aux5);
                            // pos <- aux3
                            pos.set(aux3);
                            direction.rotate(aux2, angle);
                            up.rotate(aux2, angle);

                            previousOrientationAngle = anglebak;
                        }

                    }

                    // Add dx to camera position
                    pos.add(dx);

                }

                // Update direction to follow focus and activate custom input
                // listener
                this.focus.getAbsolutePosition(aux4).add(dx);

                if (!GlobalConf.runtime.OPENVR) {
                    if (!diverted) {
                        directionToTarget(dt, aux4, GlobalConf.scene.TURNING_SPEED / (GlobalConf.scene.CINEMATIC_CAMERA ? 1e3f : 1e2f), planetariumFocusAngle);
                    } else {
                        updateRotationFree(dt, GlobalConf.scene.TURNING_SPEED);
                    }
                    updateRoll(dt, GlobalConf.scene.TURNING_SPEED);
                }

                updatePosition(dt, translateUnits, realTransUnits);
                updateRotation(dt, aux4);

                // Update focus direction
                focusDirection.set(aux4).sub(pos).nor();
                focus = focusBak;

                double dist = aux4.dst(pos);
                if (dist < focus.getRadius()) {
                    // aux2 <- focus-cam with a length of radius
                    aux2.set(pos).sub(aux4).nor().scl(focus.getRadius());
                    // Correct camera position
                    pos.set(aux4).add(aux2);
                }

                EventManager.instance.post(Events.FOCUS_INFO_UPDATED, focus.getDistToCamera() - focus.getRadius(), focus.getViewAngle(), focus.getAlpha(), focus.getDelta(), focus.getAbsolutePosition(aux2).len() - focus.getRadius());
            } else {
                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
            }
            break;
        case GAME_MODE:
            if (gravity && (closestBody != null) && !currentMouseKbdListener.isKeyPressed(Input.Keys.SPACE)) {
                // Add gravity to force, pulling to closest body
                Vector3d camObj = closestBody.getAbsolutePosition(aux1).sub(pos);
                double dist = camObj.len();
                // Gravity adds only at twice the radius
                if (dist < closestBody.getRadius() * 2) {
                    force.add(camObj.nor().scl(0.002));
                    fullStop = false;
                } else {
                    fullStop = true;
                }
            } else {
                fullStop = true;
            }
        case FREE_MODE:
            updatePosition(dt, translateUnits, GlobalConf.scene.FREE_CAMERA_TARGET_MODE_ON ? realTransUnits : 1);

            if (!GlobalConf.runtime.OPENVR) {
                // If target is present, update direction
                if (freeTargetOn) {
                    directionToTarget(dt, freeTargetPos, GlobalConf.scene.TURNING_SPEED / (GlobalConf.scene.CINEMATIC_CAMERA ? 1e3f : 1e2f), planetariumFocusAngle);
                    if (facingFocus) {
                        freeTargetOn = false;
                    }
                }

                // Update direction with pitch, yaw, roll
                updateRotationFree(dt, GlobalConf.scene.TURNING_SPEED);
                updateRoll(dt, GlobalConf.scene.TURNING_SPEED);
            }
            updateLateral(dt, translateUnits);

            break;
        case GAIA_SCENE_MODE:
            if (entity1 == null || entity2 == null) {
                entity1 = (CelestialBody) GaiaSky.instance.sg.getNode("Gaia");
                entity2 = (CelestialBody) GaiaSky.instance.sg.getNode("Earth");
                entity3 = (CelestialBody) GaiaSky.instance.sg.getNode("Mars");
            }
            AbstractPositionEntity fccopy = entity1.getLineCopy();
            fccopy.getRoot().translation.set(0f, 0f, 0f);
            fccopy.getRoot().update(time, null, this);
            this.pos.set(fccopy.translation);

            this.pos.add(0, 0, entity1.getRadius() * 5);
            this.posinv.set(this.pos).scl(-1);
            this.direction.set(0, 0, -1);
            this.up.set(0, 1, 0);
            closestBody = entity1;

            // Return to pool
            SceneGraphNode ape = fccopy;
            do {
                ape.returnToPool();
                ape = ape.parent;
            } while (ape != null);

            break;
        default:
            break;
        }

        // Update camera recorder
        EventManager.instance.post(Events.UPDATE_CAM_RECORDER, time, pos, direction, up);

        // Update actual camera
        lastFwdTime += dt;
        lastMode = m;

        posDistanceCheck();

        if (pos.hasNaN()) {
            pos.set(posbak);
        } else {
            posbak.set(pos);
        }

        updatePerspectiveCamera();
    }

    public void updateHUD(float dt) {
        double angleVelDirection = vel.anglePrecise(direction);
        if (angleVelDirection < 60 || angleVelDirection > 120) {
            // Current speed
            double speed = vel.len() * (angleVelDirection < 60 ? 1 : -1) * Constants.U_TO_KM;
            // Distance covered since last frame
            double dist = (speed * dt);

            float hud_scl_dist = 1;
            double sp = Math.abs(speed);
            if (sp < 3e4) {
                hud_scl_dist = 1e4f;
                hudColor = 0;
            } else if (sp < 3e6) {
                hud_scl_dist = 1e6f;
                hudColor = 1;
            } else if (sp < 3e8) {
                hud_scl_dist = 1e8f;
                hudColor = 2;
            } else if (sp < 3e10) {
                hud_scl_dist = 1e10f;
                hudColor = 3;
            } else if (sp < 3e12) {
                hud_scl_dist = 1e12f;
                hudColor = 4;
            } else if (sp < 3e14) {
                hud_scl_dist = 1e14f;
                hudColor = 5;
            } else if (sp < 3e20) {
                hud_scl_dist = 1e20f;
                hudColor = 6;
            }

            // Update scales
            for (int i = 0; i < hudScales.length; i++) {
                double distpart = dist / hud_scl_dist;

                hudScales[i] = hudScales[i] + (HUD_SCALE_MAX - HUD_SCALE_MIN) * distpart;
                if (dist > 0 && hudScales[i] > HUD_SCALE_MAX) {
                    hudScales[i] = HUD_SCALE_MIN;
                }
                if (dist < 0 && hudScales[i] < HUD_SCALE_MIN) {
                    hudScales[i] = HUD_SCALE_MAX;
                }
            }
        } else {
            hudColor = 0;
        }

    }

    protected void updatePerspectiveCamera() {
        camera.position.set(0f, 0f, 0f);
        camera.direction.set(direction.valuesf());
        camera.up.set(up.valuesf());
        camera.update();
        //cameraUpdate(camera);

        posinv.set(pos).scl(-1);
    }

    /**
     * Does the camera math in higher precision Matrix4d objects and then down-casts
     * the results into the perspective camera
     */
    protected void cameraUpdate(PerspectiveCamera camera) {
        // Update in double matrices
        super.update(camera, aux1.set(0, 0, 0), direction, up);

        // Down-cast to perspective camera
        projection.putIn(camera.projection);
        view.putIn(camera.view);
        combined.putIn(camera.combined);

        // Update frustum
        camera.invProjectionView.set(camera.combined);
        Matrix4.inv(camera.invProjectionView.val);
        camera.frustum.update(camera.invProjectionView);
    }

    /**
     * Adds a forward movement by the given amount.
     *
     * @param amount Positive for forward force, negative for backward force.
     */
    public void addForwardForce(double amount) {
        double tu = getTranslateUnits();
        if (amount <= 0) {
            // Avoid getting stuck in surface
            tu = Math.max(10d * Constants.M_TO_U, tu);
        }
        if (parent.mode == CameraMode.FOCUS_MODE) {
            desired.set(focusDirection);
        } else {
            desired.set(direction);
        }

        desired.nor().scl(amount * tu * (GlobalConf.runtime.OPENVR ? 10 : 100));
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

    /**
     * Adds a pan movement to the camera.
     *
     * @param deltaX Amount of horizontal movement.
     * @param deltaY Amount of vertical movement.
     */
    public void addPanMovement(double deltaX, double deltaY) {
        double tu = getTranslateUnits();
        desired.set(direction).crs(up).nor().scl(-deltaX * tu);
        desired.add(aux1.set(up).nor().scl(-deltaY * tu));
        force.set(desired);
        // We reset the time counter
        lastFwdTime = 0;
    }

    public void forward(double amount) {
        forward(amount, 0);
    }

    public void forward(double amount, double minTu) {
        double tu = getTranslateUnits(minTu);
        desired.set(direction).nor().scl(amount * tu);
        vel.add(desired).clamp(0, 5e12);
        lastFwdTime = 0;
    }

    public void strafe(double amount) {
        strafe(amount, 0);
    }

    public void strafe(double amount, double minTu) {
        double tu = getTranslateUnits(minTu);
        desired.set(direction).crs(up).nor().scl(amount * tu);
        vel.add(desired).clamp(0, 5e12);
        lastFwdTime = 0;
    }

    public void vertical(double amount) {
        vertical(amount, 0);
    }

    public void vertical(double amount, double minTu) {
        double tu = getTranslateUnits(minTu);
        desired.set(up).nor().scl(amount * tu);
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
            addYaw(deltaX, acceleration);
            addPitch(deltaY, acceleration);
        } else if (parent.mode.equals(CameraMode.FOCUS_MODE)) {
            double th = 30;
            double vadeg = Math.toDegrees(focus.getViewAngle());

            if (focusLookKeyPressed) {
                diverted = true;
                addYaw(deltaX, acceleration);
                addPitch(deltaY, acceleration);
            } else {
                // This factor slows the rotation as the focus gets closer and closer
                double factor = vadeg > th ? Math.pow(th / vadeg, 3) : 1.0;
                addHorizontal(deltaX * factor, acceleration);
                addVertical(deltaY * factor, acceleration);
            }
        }
    }

    public void setGamepadMultiplier(double amount) {
        gamepadMultiplier = amount;
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
        horizontal.y = amount;
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
        vertical.y = amount;
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
     *
     * @return True if the camera had any movement at all and it has been stopped.
     * False if camera was already still.
     */
    public boolean stopTotalMovement() {
        boolean stopped = (vel.len2() != 0 || yaw.y != 0 || pitch.y != 0 || roll.y != 0 || vertical.y != 0 || horizontal.y != 0);
        force.setZero();
        vel.setZero();
        yaw.setZero();
        pitch.setZero();
        roll.setZero();
        horizontal.setZero();
        vertical.setZero();
        return stopped;
    }

    public boolean stopRotateMovement() {
        boolean stopped = (yaw.y != 0 || pitch.y != 0 || vertical.y != 0 || horizontal.y != 0);
        yaw.setZero();
        pitch.setZero();
        horizontal.setZero();
        vertical.setZero();
        return stopped;
    }

    public boolean stopRollMovement() {
        boolean stopped = (roll.y != 0);
        roll.setZero();
        return stopped;
    }

    public boolean stopTurnMovement() {
        boolean stopped = (yaw.y != 0 || pitch.y != 0 || roll.y != 0 || vertical.y != 0 || horizontal.y != 0);
        yaw.setZero();
        pitch.setZero();
        roll.setZero();
        horizontal.setZero();
        vertical.setZero();
        return stopped;
    }

    /**
     * Stops the camera movement.
     *
     * @return True if the camera had any movement at all and it has been stopped.
     * False if camera was already still.
     */
    public boolean stopForwardMovement() {
        boolean stopped = (vel.len2() != 0);
        force.setZero();
        vel.setZero();
        return stopped;
    }

    /**
     * Updates the position of this entity using the current force
     *
     * @param dt
     * @param multiplier
     */
    protected void updatePosition(double dt, double multiplier, double transUnits) {
        boolean cinematic = GlobalConf.scene.CINEMATIC_CAMERA;
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

        double forceLen = force.len();
        double velocity = vel.len();

        // Half a second after we have stopped zooming, real friction kicks in
        if (fullStop) {
            double counterAmount = lastFwdAmount < 0 && cinematic ? Math.min(transUnits, 200) : 2;
            if (getMode().isFocus() && lastFwdAmount > 0) {
                double factor = cinematic ? 100 : 1;
                counterAmount *= factor / ((focus.getDistToCamera() - focus.getRadius()) / focus.getRadius());
            }
            friction.set(vel).nor().scl(-velocity * counterAmount * dt);
        } else
            friction.set(force).nor().scl(-forceLen * dt);

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
            if (GlobalConf.scene.CAMERA_SPEED_LIMIT > 0 && vel.len() > GlobalConf.scene.CAMERA_SPEED_LIMIT) {
                vel.clamp(0, GlobalConf.scene.CAMERA_SPEED_LIMIT);
            }

            // Velocity changed direction
            if (lastvel.dot(vel) < 0) {
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
            aux1.set(vel).scl(dt);
            // Aux2 contains the new position
            pos.add(aux1);

            accel.setZero();

            lastvel.set(vel);
            force.setZero();
        }
        posinv.set(pos).scl(-1);
    }

    private void posDistanceCheck() {
        // Check terrain collision
        if (closestBody != null) {
            // New position
            closestBody.getPredictedPosition(aux5, GaiaSky.instance.time, this, false);

            double h = closestBody.getHeight(pos, aux5);
            double hs = closestBody.getHeightScale() * GlobalConf.scene.ELEVATION_MULTIPLIER;
            double minDist = h + hs / 10.0;
            double newDist = aux5.scl(-1).add(pos).len();
            if (newDist < minDist) {
                aux5.nor().scl(minDist - newDist);
                pos.add(aux5);
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

        defaultState(pitch, !GlobalConf.scene.CINEMATIC_CAMERA && !inputByController);
        defaultState(yaw, !GlobalConf.scene.CINEMATIC_CAMERA && !inputByController);
    }

    private void updateRoll(double dt, double rotateSpeed) {
        if (updatePosition(roll, dt)) {
            // Roll
            rotate(direction, -roll.z * rotateSpeed);
        }
        defaultState(roll, !GlobalConf.scene.CINEMATIC_CAMERA && !inputByController);
    }

    /**
     * Updates the direction vector using the pitch, yaw and roll forces.
     *
     * @param dt
     */
    private void updateRotation(double dt, final Vector3d rotationCenter) {
        // Add position to compensate for coordinates centered on camera
        // rotationCenter.add(pos);
        if (updatePosition(vertical, dt)) {
            // Pitch
            aux1.set(direction).crs(up).nor();
            rotateAround(rotationCenter, aux1, vertical.z * GlobalConf.scene.ROTATION_SPEED);
        }
        if (updatePosition(horizontal, dt)) {
            // Yaw
            rotateAround(rotationCenter, up, -horizontal.z * GlobalConf.scene.ROTATION_SPEED);
        }

        defaultState(vertical, !GlobalConf.scene.CINEMATIC_CAMERA && !inputByController);
        defaultState(horizontal, !GlobalConf.scene.CINEMATIC_CAMERA && !inputByController);

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
     *
     * @param angle
     * @param dt
     * @return
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
     * @param dt               The current time step
     * @param target           The position of the target
     * @param turnVelocity     The velocity at which to turn
     * @param planetariumAngle In degrees. In the case of planetaria, the target
     *                         must be a few degrees lower (skewed domes) so that we
     *                         need to target a point which is a few degrees above
     *                         the focus.
     */
    private void directionToTarget(double dt, final Vector3d target, double turnVelocity, double planetariumAngle) {
        desired.set(target).sub(pos);
        if (planetariumAngle != 0) {
            // Use up to target area above focus with given angle
            double uplen = Math.tan(MathUtilsd.degRad * planetariumAngle) * desired.len();
            aux3.set(desired).crs(up);
            aux3.crs(desired);
            aux2.set(aux3).nor().scl(uplen);
            aux1.set(target).add(aux2);
            desired.set(aux1).sub(pos);
        }
        desired.nor();
        double angl = desired.angle(direction);
        // boolean samedir = aux1.set(desired).add(direction).len2() >
        // Math.max(desired.len2(), direction.len2());
        if (angl > 0.5) {
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

    private void setMouseKbdListener(MouseKbdListener newListener) {
        InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
        if (currentMouseKbdListener != newListener) {

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
    }

    /**
     * Updates the camera mode
     */
    @Override
    public void updateMode(CameraMode mode, boolean centerFocus, boolean postEvent) {
        InputMultiplexer im = (InputMultiplexer) Gdx.input.getInputProcessor();
        switch (mode) {
        case FOCUS_MODE:
            diverted = !centerFocus;
            checkFocus();
        case FREE_MODE:
        case GAIA_SCENE_MODE:
        case GAME_MODE:
            Gdx.app.postRunnable(() -> {
                MouseKbdListener newListener = mode == CameraMode.GAME_MODE ? gameMouseKbdListener : naturalMouseKbdListener;
                setMouseKbdListener(newListener);

                Controllers.clearListeners();
                GlobalConf.controls.addControllerListener(controllerListener);
                if (GlobalConf.runtime.OPENVR)
                    GaiaSky.instance.vrContext.addListener(openVRListener);
            });
            break;
        default:
            Gdx.app.postRunnable(() -> {
                // Unregister input controllers
                im.removeProcessor(currentMouseKbdListener);
                GlobalConf.controls.removeControllerListener(controllerListener);
                // Remove vr listener
                if (GlobalConf.runtime.OPENVR)
                    GaiaSky.instance.vrContext.removeListener(openVRListener);
            });
            break;
        }
    }

    public void setFocus(IFocus focus) {
        if (focus != null && GaiaSky.instance.isOn(focus.getCt())) {
            this.focus = focus;
            this.focus.makeFocus();
            // Reset facing focus
            this.facingFocus = false;
            // Create event to notify focus change
            EventManager.instance.post(Events.FOCUS_CHANGED, focus);
        }
    }

    public double getTranslateUnits(double min) {
        double dist;
        if (parent.mode.useFocus() && focus != null) {
            dist = focus.getDistToCamera() - (focus.getHeight(pos, false) + MIN_DIST);
        } else if (parent.mode.useClosest() && closestBody != null) {
            dist = closestBody.getDistToCamera() - (closestBody.getHeight(pos, false) + MIN_DIST);
        } else {
            dist = distance;
        }
        ;
        return dist > 0 ? Math.max(dist, min) * GlobalConf.scene.CAMERA_SPEED : 0;
    }

    /**
     * This depends on the distance from the focus.
     *
     * @return The translate units
     */
    public double getTranslateUnits() {
        return getTranslateUnits(0.5e-8);
    }

    /**
     * Depends on the distance to the focus
     *
     * @return The rotation units
     */
    public double getRotationUnits() {
        double dist;
        if (parent.mode == CameraMode.FOCUS_MODE) {
            AbstractPositionEntity ancestor = (AbstractPositionEntity) focus;
            dist = ancestor.distToCamera - ancestor.getRadius();
        } else {
            dist = distance;
        }
        return Math.max(2000, Math.min(dist * Constants.U_TO_KM, GlobalConf.scene.ROTATION_SPEED));
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case FOCUS_CHANGE_CMD:
            // Check the type of the parameter: IFocus or String
            IFocus focus = null;

            // Center focus or not
            boolean centerFocus = !GlobalConf.runtime.OPENVR;
            if (data.length > 1)
                centerFocus = (Boolean) data[1];

            if (data[0] instanceof String) {
                SceneGraphNode sgn = GaiaSky.instance.sg.getNode((String) data[0]);
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
            float fov = MathUtilsd.clamp((float) data[0], Constants.MIN_FOV, Constants.MAX_FOV);

            for (PerspectiveCamera cam : cameras) {
                cam.fieldOfView = fov;
            }
            fovFactor = camera.fieldOfView / 40f;
            if (parent.current == this) {
                EventManager.instance.post(Events.FOV_CHANGE_NOTIFICATION, fov, fovFactor);
            }
            break;
        case PLANETARIUM_CMD:
            boolean state = (boolean) data[0];
            EventManager.instance.post(Events.FISHEYE_CMD, state);
            if (state) {
                fovBackup = GaiaSky.instance.cam.getCamera().fieldOfView;
                EventManager.instance.post(Events.FOV_CHANGED_CMD, 130f);
                EventManager.instance.post(Events.PLANETARIUM_FOCUS_ANGLE_CMD, 30f);
            } else {
                EventManager.instance.post(Events.FOV_CHANGED_CMD, fovBackup);
                EventManager.instance.post(Events.PLANETARIUM_FOCUS_ANGLE_CMD, 0f);
            }
            break;
        case CAMERA_POS_CMD:
            pos.set((double[]) data[0]);
            posinv.set(pos).scl(-1d);
            break;
        case CAMERA_DIR_CMD:
            direction.set((double[]) data[0]).nor();
            break;
        case CAMERA_UP_CMD:
            up.set((double[]) data[0]).nor();
            break;
        case CAMERA_FWD:
            addForwardForce((double) data[0]);
            break;
        case CAMERA_ROTATE:
            addRotateMovement((double) data[0], (double) data[1], false, true);
            break;
        case CAMERA_TURN:
            addRotateMovement((double) data[0], (double) data[1], true, true);
            break;
        case CAMERA_PAN:

            break;
        case CAMERA_ROLL:
            addRoll((double) data[0], GlobalConf.scene.CINEMATIC_CAMERA);
            break;
        case CAMERA_STOP:
            stopTotalMovement();
            break;
        case CAMERA_CENTER:
            diverted = false;
            break;
        case GO_TO_OBJECT_CMD:
            if (this.focus != null) {
                final IFocus f = this.focus;
                Gdx.app.postRunnable(() -> {
                    // Position camera near focus
                    stopTotalMovement();

                    f.getAbsolutePosition(aux1);
                    pos.set(aux1);

                    pos.add(0, 0, -f.getSize() * 3);
                    posinv.set(pos).scl(-1);
                    direction.set(0, 0, 1);
                    up.set(0, 1, 0);
                    rotate(up, 0.01);
                    updatePerspectiveCamera();
                });

            }
            break;
        case PLANETARIUM_FOCUS_ANGLE_CMD:
            if (GlobalConf.runtime.OPENVR || data.length == 0)
                planetariumFocusAngle = 0;
            else
                planetariumFocusAngle = (float) data[0];
            break;
        case ORIENTATION_LOCK_CMD:
            previousOrientationAngle = 0;
            break;
        case FREE_MODE_COORD_CMD:
            float ra = (Float) data[0];
            float dec = (Float) data[1];
            float dist = (float) (1e12 * Constants.PC_TO_U);
            aux1.set(MathUtilsd.degRad * ra, MathUtilsd.degRad * dec, dist);
            Coordinates.sphericalToCartesian(aux1, freeTargetPos);
            facingFocus = false;
            freeTargetOn = true;
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
                }
                if (found) {
                    // Set camera  free
                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
                }
            }
            break;
        case TOGGLE_VISIBILITY_CMD:
            if (getMode().isFocus()) {
                ComponentType ct = ComponentType.getFromKey((String) data[0]);
                if (this.focus != null && ct != null && this.focus.getCt().isEnabled(ct)) {
                    // Set camera  free
                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
                }
            }
            break;
        default:
            break;
        }

    }

    /**
     * Rotates the direction and up vector of this camera by the given angle around
     * the given axis, with the axis attached to given point. The direction and up
     * vector will not be orthogonalized.
     *
     * @param rotationCenter the point to attach the axis to
     * @param rotationAxis   the axis to rotate around
     * @param angle          the angle, in degrees
     */
    public void rotateAround(final Vector3d rotationCenter, Vector3d rotationAxis, double angle) {
        rotate(rotationAxis, angle);

        // aux3 <- pos-point vector
        aux3.set(pos).sub(rotationCenter);
        aux3.rotate(rotationAxis, angle);
        pos.set(aux3).add(rotationCenter);
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
     * Applies the given force to this entity's acceleration
     *
     * @param force
     */
    protected void applyForce(Vector3d force) {
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
            this.focus.getAbsolutePosition(aux1);
            if (pos.dst(aux1) < this.focus.getRadius()) {
                // Position camera near focus
                stopTotalMovement();

                this.focus.getAbsolutePosition(aux1);
                pos.set(aux1);

                pos.add(0, 0, -this.focus.getSize() * 6);
                posinv.set(pos).scl(-1);
                direction.set(0, 0, 1);
            }
        }
    }

    public void resetState() {
        pos.setZero();
        posinv.setZero();
        vroffset.setZero();
        direction.set(0, 0, -1);
        for (PerspectiveCamera cam : cameras) {
            cam.position.setZero();
            cam.direction.set(0, 0, -1);
            cam.update();
        }
    }

    public void setInputByController(boolean controller) {
        this.inputByController = controller;
    }

    @Override
    public void setCamera(PerspectiveCamera cam) {
        this.camera = cam;
    }

    public void setThrust(double thrust, int direction) {
        this.thrust = thrust;
        this.thrustDirection = direction;
    }

    @Override
    public void render(int rw, int rh) {
        boolean draw = !GlobalConf.program.CUBEMAP360_MODE && !GlobalConf.program.STEREOSCOPIC_MODE && !GlobalConf.postprocess.POSTPROCESS_FISHEYE;

        spriteBatch.begin();

        // Renders crosshair if focus mode
        if (draw) {
            // Mark home in ORANGE
            if(GlobalConf.scene.CROSSHAIR_HOME) {
                if (home == null && GaiaSky.instance.sg != null)
                    home = GaiaSky.instance.sg.findFocus(GlobalConf.scene.STARTUP_OBJECT);
                if (home != null) {
                    drawCrosshair(home, false, crosshairHome, crosshairArrow, rw, rh, 1f, 0.7f, 0.1f, 1f);
                }
            }

            // Mark closest object in BLUE
            if (GlobalConf.scene.CROSSHAIR_CLOSEST && closest != null) {
                drawCrosshair(closest, false, crosshairClosest, crosshairArrow, rw, rh, 0.3f, 0.5f, 1f, 1f);
            }

            // Mark focus in GREEN
            if (GlobalConf.scene.CROSSHAIR_FOCUS && getMode().isFocus()) {
                // Green, focus mode
                drawCrosshair(focus, true, crosshairFocus, crosshairArrow, rw, rh, 0.2f, 1f, 0.4f, 1f);
            }

            // Velocity crosshair only if we move
            // double speed = vel.len();
            // if (speed > 0) {
            // float chw = velocityCrosshair.getWidth();
            // float chh = velocityCrosshair.getHeight();
            // float chw2 = chw / 2;
            // float chh2 = chh / 2;
            //
            // if (vel.anglePrecise(direction) < 60) {
            //
            // // ANTIVEL
            // drawVelCrosshair(antivelocityCrosshair, rw, rh, chw, chh, chw2,
            // chh2, -1);
            //
            // // VEL
            // drawVelCrosshair(velocityCrosshair, rw, rh, chw, chh, chw2, chh2,
            // 1);
            // } else {
            // // VEL
            // drawVelCrosshair(velocityCrosshair, rw, rh, chw, chh, chw2, chh2,
            // 1);
            //
            // // ANTIVEL
            // drawVelCrosshair(antivelocityCrosshair, rw, rh, chw, chh, chw2,
            // chh2, -1);
            // }
            // }

            // Gravitational waves crosshair
            if (GlobalConf.runtime.GRAVITATIONAL_WAVES) {
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
        }

        if (false && GlobalConf.program.DISPLAY_HUD) {
            // Speed HUD
            float dx, dy;
            float centerx = rw / 2;
            float centery = rh / 2;
            if (vel.len2() != 0) {
                aux1.set(vel);
                if (vel.anglePrecise(direction) > 90) {
                    aux1.scl(-1);
                }
                projectToScreen(aux1, auxf1, rw, rh, 0, 0, 0, 0);
                dx = auxf1.x - rw / 2;
                dy = auxf1.y - rh / 2;
            } else {
                dx = 0;
                dy = 0;
            }

            for (int i = 0; i < hudScales.length; i++) {
                float scl = (float) hudScales[i];

                float dscale = (float) MathUtilsd.lint(scl, HUD_SCALE_MIN, HUD_SCALE_MAX, 1d, 0d);

                Sprite s = hudSprites[i];
                s.setColor(hudColors[hudColor]);
                s.setOriginCenter();
                s.setScale(scl);
                s.setPosition(centerx + (dx * dscale) - hudw / 2, centery + (dy * dscale) - hudh / 2);
                s.draw(spriteBatch);

            }
        }

        spriteBatch.end();
    }

    private void drawCrosshair(IFocus chFocus, boolean focusMode, Texture crosshairTex, Texture arrowTex, int rw, int rh, float r, float g, float b, float a) {
        if(chFocus != null) {
            if (!focusMode) {
                drawCrosshair(chFocus.getClosestAbsolutePos(aux1).add(posinv), chFocus.getClosestDistToCamera(), chFocus.getRadius(), crosshairTex, arrowTex, rw, rh, r, g, b, a);
            } else {
                drawCrosshair(chFocus.getAbsolutePosition(aux1).add(posinv), chFocus.getDistToCamera(), chFocus.getRadius(), crosshairTex, arrowTex, rw, rh, r, g, b, a);
            }
        }
    }

    /**
     * Draws a crosshair given a camera-relative position
     * @param pos The position in floating camera coordinates
     * @param distToCam The distance to the camera
     * @param radius Radius of object
     * @param crosshairTex Crosshair texture
     * @param arrowTex Arrow texture
     * @param rw Width
     * @param rh Height
     * @param r Red
     * @param g Green
     * @param b Blue
     * @param a Alpha
     */
    private void drawCrosshair(Vector3d pos, double distToCam, double radius, Texture crosshairTex, Texture arrowTex, int rw, int rh, float r, float g, float b, float a) {
        if (distToCam > radius * 2) {
            float chw = crosshairTex.getWidth();
            float chh = crosshairTex.getHeight();
            float chw2 = chw / 2;
            float chh2 = chh / (vr ? 1 : 2);

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
                    float ang = firstAux ? -90 + aux2f2.angle() : firstAngl;
                    if (firstAux) {
                        firstAngl = ang;
                    }
                    firstAux = !firstAux;
                    aux2f2.set(auxf1.x - (rw / 2), auxf1.y - (rh / 2));
                    aux2.set(up).rotate(direction, 90).add(up).scl(0.04);
                    aux1.set(vroffset).add(aux2).scl(1 / Constants.M_TO_U).add(direction);
                    projectToScreen(aux1, auxf1, rw, rh, chw, chh, chw2, chh2);
                    spriteBatch.draw(arrowTex, auxf1.x, auxf1.y, chw2, chh2, chw, chh, 1f, 1f, ang, 0, 0, (int) chw, (int) chw, false, false);
                } else {
                    aux2f2.set(auxf1.x - (Gdx.graphics.getWidth() / 2), auxf1.y - (Gdx.graphics.getHeight() / 2));
                    spriteBatch.draw(arrowTex, auxf1.x - chw2, auxf1.y - chh2, chw2, chh2, chw, chh, 1f, 1f, -90 + aux2f2.angle(), 0, 0, (int) chw, (int) chh, false, false);
                }
            }
        }

    }

    private void drawVelCrosshair(Texture tex, int rw, int rh, float chw, float chh, float chw2, float chh2, float scl) {
        aux1.set(vel).scl(scl);
        projectToScreen(aux1, auxf1, rw, rh, chw, chh, chw2, chh2);

        spriteBatch.draw(tex, auxf1.x - chw2, auxf1.y - chh2, chw, chh);
    }

    /**
     * Projects to screen
     *
     * @param vec
     * @param out
     * @param rw
     * @param rh
     * @param chw
     * @param chh
     * @param chw2
     * @param chh2
     * @return False if projected point falls outside the screen bounds, true
     * otherwise
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
            } else if (out.x <= w2 && out.y <= h2) {
                // Q3
                out.x = chw2;
                out.y = chh2;
            } else if (out.x > w2 && out.y < h2) {
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
        spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
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
}
