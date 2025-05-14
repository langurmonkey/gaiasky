/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.main.KeyBindings;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.view.KeyframesView;
import gaiasky.util.Settings;
import gaiasky.util.comp.ViewAngleComparator;
import net.jafama.FastMath;

import java.util.Comparator;
import java.util.Objects;

/**
 * Implements the mouse and keyboard listener in default mode.
 */
public class MainMouseKbdListener extends AbstractMouseKbdListener implements IObserver {

    /**
     * Maximum double click time, in ms.
     **/
    private static final long doubleClickTime = 400;
    /**
     * The gesture listener.
     */
    protected final GaiaGestureListener gestureListener;
    /**
     * FOCUS_MODE comparator.
     **/
    private final Comparator<Entity> comp;
    /**
     * Max pixel distance to be considered a click.
     **/
    private final float MOVE_PX_DIST;
    /**
     * Max distance from the click to the actual selected star.
     **/
    private final int MIN_PIX_DIST;
    private final Vector2 gesture = new Vector2();
    /**
     * Smoothing factor applied in the non-cinematic mode.
     **/
    private final double noAccelSmoothing;
    /**
     * Scaling factor applied in the non-cinematic mode.
     **/
    private final double noAccelFactor;
    /**
     * Drag vectors.
     **/
    private final Vector2 currentDrag;
    private final Vector2 lastDrag;
    private final NaturalCamera camera;
    private final FocusView view;
    /**
     * The button for rotating the camera either around its center or around the
     * focus.
     */
    public int leftMouseButton = Buttons.LEFT;
    /**
     * The button for panning the camera along the up/right plane.
     */
    public int rightMouseButton = Buttons.RIGHT;
    /**
     * The button for moving the camera along the direction axis.
     */
    public int middleMouseButton = Buttons.MIDDLE;
    /**
     * Whether scrolling requires the activeKey to be pressed (false) or always
     * allow scrolling (true).
     */
    public boolean alwaysScroll = true;
    /**
     * The weight for each scrolled amount.
     */
    public float scrollFactor = -0.1f;
    /**
     * Current movement multiplier state.
     **/
    private boolean movementMultiplierState = false;
    /**
     * The key for rolling the camera.
     **/
    public int rollKey = Keys.SHIFT_LEFT;
    /**
     * The current (first) button being pressed.
     */
    protected int button = -1;
    private float startX, startY;
    /**
     * dx(mouse pointer) since last time.
     **/
    private double dragDx;
    /**
     * dy(mouse pointer) since last time.
     **/
    private double dragDy;
    /**
     * Save time of last click, in ms.
     */
    private long lastClickTime = -1;
    /**
     * We're dragging or selecting a keyframe.
     **/
    private boolean keyframeBeingDragged = false;
    private int touched;
    private boolean multiTouch;
    /**
     * Reference to the scene.
     **/
    private Scene scene;
    /**
     * Keyframes path entity.
     **/
    private Entity kpo;
    private KeyframesView kfView;

    /** Key register **/
    private final KeyRegister register;

    protected MainMouseKbdListener(final GaiaGestureListener gestureListener,
                                   NaturalCamera camera) {
        super(gestureListener, camera);
        this.camera = camera;
        this.gestureListener = gestureListener;
        this.gestureListener.inputListener = this;
        this.comp = new ViewAngleComparator<>();
        this.register = new KeyRegister();
        // 1% of width
        this.MOVE_PX_DIST = (float) FastMath.max(5, Gdx.graphics.getWidth() * 0.01);
        this.MIN_PIX_DIST = (int) (8f);

        this.dragDx = 0;
        this.dragDy = 0;
        this.noAccelSmoothing = 16.0;
        this.noAccelFactor = 10.0;

        this.view = new FocusView();

        this.currentDrag = new Vector2();
        this.lastDrag = new Vector2();
    }

    public MainMouseKbdListener(final NaturalCamera camera) {
        this(new GaiaGestureListener(), camera);
        EventManager.instance.subscribe(this, Event.TOUCH_DOWN, Event.TOUCH_UP, Event.TOUCH_DRAGGED, Event.SCROLLED, Event.KEY_DOWN, Event.KEY_UP, Event.SCENE_LOADED);
    }

    private KeyframesView getKeyframesPathObject() {
        if (scene != null) {
            if (kfView == null) {
                kfView = new KeyframesView(scene);
            }
            if (kpo != null) {
                kfView.setEntity(kpo);
                return kfView;
            }

            ImmutableArray<Entity> l = scene.findEntitiesByFamily(scene.getFamilies().keyframes);
            if (l.size() > 0) {
                kpo = l.get(0);
                kfView.setEntity(kpo);
                return kfView;
            }
        }
        return null;
    }

    private FocusView getKeyframeCollision(int screenX,
                                           int screenY) {
        if (getKeyframesPathObject() != null)
            return getKeyframesPathObject().select(screenX, screenY, MIN_PIX_DIST, camera);
        else
            return null;
    }

    private void dragKeyframe(int screenX,
                              int screenY,
                              double dragDx,
                              double dragDy) {
        if (isKeyPressed(Keys.SHIFT_LEFT) && !anyPressed(Keys.CONTROL_LEFT, Keys.ALT_LEFT)) {
            // Rotate around up (rotate dir)
            Objects.requireNonNull(getKeyframesPathObject()).rotateAroundUp(dragDx, dragDy, camera);
            return;
        } else if (isKeyPressed(Keys.CONTROL_LEFT) && !anyPressed(Keys.SHIFT_LEFT, Keys.ALT_LEFT)) {
            // Rotate around dir (rotate up)
            Objects.requireNonNull(getKeyframesPathObject()).rotateAroundDir(dragDx, dragDy, camera);
            return;
        } else if (isKeyPressed(Keys.ALT_LEFT) && !anyPressed(Keys.SHIFT_LEFT, Keys.CONTROL_LEFT)) {
            // Rotate around dir.crs(up)
            Objects.requireNonNull(getKeyframesPathObject()).rotateAroundCrs(dragDx, dragDy, camera);
            return;
        }
        Objects.requireNonNull(getKeyframesPathObject()).moveSelection(screenX, screenY, camera);
    }

    private Array<Entity> getHits(int screenX,
                                  int screenY) {
        Array<Entity> l = camera.getScene().findFocusableEntities();
        Array<Entity> hits = new Array<>();

        // Add all hits
        for (Entity entity : l) {
            view.setEntity(entity);
            view.addEntityHitCoordinate(screenX, screenY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), MIN_PIX_DIST, camera, hits);
        }

        return hits;
    }

    private Entity getBestHit(int screenX,
                              int screenY) {
        Array<Entity> hits = getHits(screenX, screenY);
        if (hits.size != 0) {
            // Sort using distance
            hits.sort(comp);
            // Get closest
            return hits.get(hits.size - 1);
        }
        return null;
    }

    @Override
    public boolean touchDown(final int screenX,
                             final int screenY,
                             final int pointer,
                             final int button) {
        if (isActive()) {
            if (Settings.settings.runtime.inputEnabled) {
                touched |= (1 << pointer);
                multiTouch = !MathUtils.isPowerOfTwo(touched);
                if (multiTouch)
                    this.button = -1;
                else if (this.button < 0) {
                    startX = screenX;
                    startY = screenY;
                    gesture.set(startX, startY);
                    this.button = button;
                }
                if (button == Buttons.RIGHT) {
                    // Select keyframes.
                    if (!(anyPressed(Keys.ALT_LEFT, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT) && getKeyframesPathObject() != null && getKeyframesPathObject().isSelected())) {
                        FocusView hit;
                        keyframeBeingDragged = ((hit = getKeyframeCollision(screenX, screenY)) != null);
                        if (keyframeBeingDragged) {
                            // FOCUS_MODE, do not center.
                            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, hit.getEntity(), false);
                            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE, false);
                        }
                    }
                }
            }
            camera.setGamepadInput(false);
            return super.touchDown(screenX, screenY, pointer, button);
        }
        return false;
    }

    @Override
    public boolean touchUp(final int screenX,
                           final int screenY,
                           final int pointer,
                           final int button) {
        if (isActive()) {
            EventManager.publish(Event.INPUT_EVENT, this, button);
            if (Settings.settings.runtime.inputEnabled) {
                touched &= ~(1 << pointer);
                multiTouch = !MathUtils.isPowerOfTwo(touched);
                if (button == this.button && button == leftMouseButton) {
                    final long currentTime = TimeUtils.millis();
                    final long lastLeftTime = lastClickTime;

                    GaiaSky.postRunnable(() -> {
                        // 5% of width pixels distance.
                        if (!Settings.settings.scene.camera.cinematic || gesture.dst(screenX, screenY) < MOVE_PX_DIST) {
                            boolean stopped = camera.stopMovement();
                            boolean focusRemoved = GaiaSky.instance.mainGui != null && GaiaSky.instance.mainGui.cancelTouchFocus();
                            boolean doubleClick = currentTime - lastLeftTime < doubleClickTime;
                            gesture.set(0, 0);

                            if (doubleClick && !stopped && !focusRemoved) {
                                // Select star, if any
                                Entity hit = getBestHit(screenX, screenY);
                                if (hit != null) {
                                    EventManager.publish(Event.FOCUS_CHANGE_CMD, this, hit);
                                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE);
                                }
                            }
                        }
                    });
                    dragDx = 0;
                    dragDy = 0;
                    lastClickTime = currentTime;
                } else if (button == this.button && button == rightMouseButton) {
                    if (keyframeBeingDragged) {
                        keyframeBeingDragged = false;
                    } else if (gesture.dst(screenX, screenY) < MOVE_PX_DIST && getKeyframesPathObject() != null && getKeyframesPathObject().isSelected() && !anyPressed(
                            Keys.CONTROL_LEFT, Keys.SHIFT_LEFT, Keys.ALT_LEFT)) {
                        EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FREE_MODE);
                        Objects.requireNonNull(getKeyframesPathObject()).unselect();
                    } else {
                        // Ensure Octants observed property is computed
                        GaiaSky.postRunnable(() -> {
                            // 5% of width pixels distance
                            if (gesture.dst(screenX, screenY) < MOVE_PX_DIST && !Settings.settings.program.modeStereo.active) {
                                // Stop
                                camera.setYaw(0);
                                camera.setPitch(0);

                                // Right click, context menu
                                Entity hit = getBestHit(screenX, screenY);
                                EventManager.publish(Event.CONTEXT_MENU_CMD, this, hit, screenX, screenY);
                            }
                        });
                        camera.setHorizontal(0);
                        camera.setVertical(0);
                    }
                }

                // Remove keyboard focus from GUI elements
                EventManager.instance.notify(Event.REMOVE_KEYBOARD_FOCUS_CMD, this);

                this.button = -1;
            }
            camera.setGamepadInput(false);
            return super.touchUp(screenX, screenY, pointer, button);
        }
        return false;
    }

    protected boolean processDrag(int screenX,
                                  int screenY,
                                  double deltaX,
                                  double deltaY,
                                  int button) {
        if (isActive()) {
            boolean accel = Settings.settings.scene.camera.cinematic;
            if (accel) {
                dragDx = deltaX;
                dragDy = deltaY;
            } else {
                currentDrag.set((float) deltaX, (float) deltaY);
                // Check orientation of last vs current
                if (Math.abs(currentDrag.angleDeg(lastDrag)) > 90) {
                    // Reset
                    dragDx = 0;
                    dragDy = 0;
                }

                dragDx = lowPass(dragDx, deltaX * noAccelFactor, noAccelSmoothing);
                dragDy = lowPass(dragDy, deltaY * noAccelFactor, noAccelSmoothing);
                // Update last drag
                lastDrag.set(currentDrag);
            }

            if (button == leftMouseButton) {
                if (isKeyPressed(rollKey)) {
                    if (dragDx != 0) {
                        camera.addRoll(dragDx, accel);
                    }
                } else {
                    camera.addRotateMovement(dragDx, dragDy, false, accel);
                }
            } else if (button == rightMouseButton) {
                if (keyframeBeingDragged || (getKeyframesPathObject() != null && getKeyframesPathObject().isSelected() && anyPressed(Keys.SHIFT_LEFT, Keys.CONTROL_LEFT,
                                                                                                                                     Keys.ALT_LEFT))) {
                    // Drag keyframe
                    dragKeyframe(screenX, screenY, dragDx, dragDy);
                } else {
                    if (camera.getMode().isFree()) {
                        // Strafe (pan)
                        camera.setHorizontal(-dragDx * 5f);
                        camera.setVertical(-dragDy * 5f);
                    } else {
                        // Rotate around focus.
                        camera.addRotateMovement(dragDx, dragDy, true, accel);
                    }
                }
            } else if (button == middleMouseButton) {
                if (dragDx != 0) {
                    camera.addForwardForce(dragDx);
                }
            }
            camera.setGamepadInput(false);
            return false;
        }
        return false;
    }

    private double lowPass(double smoothedValue,
                           double newValue,
                           double smoothing) {
        return smoothedValue + (newValue - smoothedValue) / smoothing;
    }

    @Override
    public boolean touchDragged(int screenX,
                                int screenY,
                                int pointer) {
        if (isActive()) {
            if (Settings.settings.runtime.inputEnabled) {
                boolean result = super.touchDragged(screenX, screenY, pointer);
                if (result || this.button < 0)
                    return result;
                final double deltaX = (screenX - startX) / Gdx.graphics.getWidth();
                final double deltaY = (startY - screenY) / Gdx.graphics.getHeight();
                startX = screenX;
                startY = screenY;
                return processDrag(screenX, screenY, deltaX, deltaY, button);
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean scrolled(float amountX,
                            float amountY) {
        if (isActive()) {
            super.scrolled(amountX, amountY);
            if (Settings.settings.runtime.inputEnabled) {
                return zoom(amountY * scrollFactor);
            }
        }
        return false;
    }

    public boolean zoom(float amount) {
        if (alwaysScroll)
            camera.addForwardForce(amount);
        camera.setGamepadInput(false);
        return false;
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (isActive()) {
            // Convert to logical.
            keyCode = InputUtils.physicalToLogicalKeyCode(keyCode);

            if (Settings.settings.runtime.inputEnabled) {
                register.registerKeyDownTime(keyCode, TimeUtils.millis());
            }
            return super.keyDown(keyCode);
        }
        return false;
    }

    @Override
    public boolean pollKeys() {
        long now = System.currentTimeMillis();
        boolean result = false;

        // Process cursors.
        var keyboardFocus = GaiaSky.instance.mainGui.getGuiStage().getKeyboardFocus();
        if (!(keyboardFocus instanceof TextField)) {
            float horizontalScale = Settings.settings.scene.camera.cinematic ? 0.01f : 1f;
            if (isKeyPressed(Keys.UP)) {
                camera.addForwardForce(1.0f);
                result = true;
            }
            if (isKeyPressed(Keys.DOWN)) {
                camera.addForwardForce(-1.0f);
                result = true;
            }
            if (isKeyPressed(Keys.RIGHT)) {
                if (camera.getMode().isFocus()) {
                    camera.addHorizontal(-1.0f * horizontalScale, true);
                } else {
                    camera.addYaw(horizontalScale, true);
                }
                result = true;
            }
            if (isKeyPressed(Keys.LEFT)) {
                if (camera.getMode().isFocus()) {
                    camera.addHorizontal(horizontalScale, true);
                } else {
                    camera.addYaw(-1.0f * horizontalScale, true);
                }
                result = true;
            }
        }

        // Camera speed-up button.
        var bindings = KeyBindings.instance;
        var speedUpKeys = bindings.getKeys("action.camera.speedup");
        if (allPressed(speedUpKeys)) {
            if (!movementMultiplierState) {
                // Activate.
                movementMultiplierState = true;
                camera.setCameraMultipliers(6, 5);
            }
        } else {
            if (movementMultiplierState) {
                // Deactivate.
                movementMultiplierState = false;
                camera.setCameraMultipliers(1, 1);

            }
        }

        // Keys to speed up and slow down time.
        long speedMs = bindings.findAction("action.doubletime").maxKeyDownTimeMs;
        long slowMs = bindings.findAction("action.dividetime").maxKeyDownTimeMs;
        var speedTimeKeys = bindings.getKeys("action.doubletime");
        var slowTimeKeys = bindings.getKeys("action.dividetime");
        if (allPressed(speedTimeKeys)) {
            if (now - register.lastKeyDownTime(speedTimeKeys) > speedMs) {
                var t = GaiaSky.instance.time.getWarpFactor();
                // Speed up.
                if (t == 0) {
                    t = 0.1;
                } else if (t > -0.1 && t < 0) {
                    t = 0;
                }
                double inc = Settings.settings.scene.camera.cinematic ? 0.01 : 0.05;
                EventManager.instance.post(Event.TIME_WARP_CMD, this, t < 0 ? t + FastMath.abs(t * inc) : t + t * inc);
            }
        } else if (allPressed(slowTimeKeys)) {
            if (now - register.lastKeyDownTime(slowTimeKeys) > slowMs) {
                var t = GaiaSky.instance.time.getWarpFactor();
                // Slow down.
                if (t == 0) {
                    t = -0.1;
                } else if (t < 0.1 && t > 0) {
                    t = 0;
                }
                double inc = Settings.settings.scene.camera.cinematic ? 0.01 : 0.05;
                EventManager.instance.post(Event.TIME_WARP_CMD, this, t < 0 ? t - FastMath.abs(t * inc) : t - t * inc);
            }
        }

        return result;
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        switch (event) {
        case TOUCH_DOWN -> this.touchDown((int) data[0], (int) data[1], (int) data[2], (int) data[3]);
        case TOUCH_UP -> this.touchUp((int) data[0], (int) data[1], (int) data[2], (int) data[3]);
        case TOUCH_DRAGGED -> this.touchDragged((int) data[0], (int) data[1], (int) data[2]);
        case SCROLLED -> this.scrolled(0f, (float) data[0]);
        case KEY_DOWN -> this.keyDown((int) data[0]);
        case KEY_UP -> this.keyUp((int) data[0]);
        case SCENE_LOADED -> this.scene = (Scene) data[0];
        default -> {
        }
        }
    }

    protected static class GaiaGestureListener extends GestureAdapter {
        public MainMouseKbdListener inputListener;
        private float previousZoom;

        @Override
        public boolean touchDown(float x,
                                 float y,
                                 int pointer,
                                 int button) {
            previousZoom = 0;
            return false;
        }

        @Override
        public boolean zoom(float initialDistance,
                            float distance) {
            if (inputListener.isActive()) {
                float newZoom = distance - initialDistance;
                float amount = newZoom - previousZoom;
                previousZoom = newZoom;
                float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
                return inputListener.zoom(amount / (Math.min(w, h)));
            }
            return false;
        }
    }

}
