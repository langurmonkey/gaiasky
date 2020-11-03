/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.KeyframesPathObject;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.comp.ViewAngleComparator;

import java.util.Comparator;
import java.util.Iterator;

/**
 * Input listener for the natural camera.
 *
 * @author tsagrista
 */
public class NaturalMouseKbdListener extends MouseKbdListener implements IObserver {

    /**
     * The button for rotating the camera either around its center or around the
     * focus.
     */
    public int leftMouseButton = Buttons.LEFT;
    /** The button for panning the camera along the up/right plane */
    public int rightMouseButton = Buttons.RIGHT;
    /** The button for moving the camera along the direction axis */
    public int middleMouseButton = Buttons.MIDDLE;
    /**
     * Whether scrolling requires the activeKey to be pressed (false) or always
     * allow scrolling (true).
     */
    public boolean alwaysScroll = true;
    /** The weight for each scrolled amount. */
    public float scrollFactor = -0.1f;
    /** The key for rolling the camera **/
    public int rollKey = Keys.SHIFT_LEFT;
    /** FOCUS_MODE comparator **/
    private Comparator<IFocus> comp;

    /** The current (first) button being pressed. */
    protected int button = -1;

    private float startX, startY;
    /** Max pixel distance to be considered a click **/
    private float MOVE_PX_DIST;
    /** Max distance from the click to the actual selected star **/
    private int MIN_PIX_DIST;
    private Vector2 gesture = new Vector2();

    /** dx(mouse pointer) since last time **/
    private double dragDx;
    /** dy(mouse pointer) since last time **/
    private double dragDy;
    /** Smoothing factor applied in the non-cinematic mode **/
    private double noAccelSmoothing;
    /** Scaling factor applied in the non-cinematic mode **/
    private double noAccelFactor;
    /** Drag vectors **/
    private Vector2 currentDrag, lastDrag;

    /** Save time of last click, in ms */
    private long lastClickTime = -1;
    /** Maximum double click time, in ms **/
    private static final long doubleClickTime = 400;

    /** We're dragging or selecting a keyframe **/
    private boolean keyframeBeingDragged = false;

    protected static class GaiaGestureListener extends GestureAdapter {
        public NaturalMouseKbdListener inputListener;
        private float previousZoom;

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            previousZoom = 0;
            return false;
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            return false;
        }

        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            return false;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
            float newZoom = distance - initialDistance;
            float amount = newZoom - previousZoom;
            previousZoom = newZoom;
            float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
            return inputListener.zoom(amount / ((w > h) ? h : w));
        }

        @Override
        public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
            return false;
        }
    }

    public final GaiaGestureListener gestureListener;

    protected NaturalMouseKbdListener(final GaiaGestureListener gestureListener, NaturalCamera camera) {
        super(gestureListener, camera);
        this.gestureListener = gestureListener;
        this.gestureListener.inputListener = this;
        this.camera = camera;
        this.comp = new ViewAngleComparator<>();
        // 1% of width
        this.MOVE_PX_DIST = (float) Math.max(5, Gdx.graphics.getWidth() * 0.01);
        this.MIN_PIX_DIST = (int) (5 * GlobalConf.UI_SCALE_FACTOR);

        this.dragDx = 0;
        this.dragDy = 0;
        this.noAccelSmoothing = 16.0;
        this.noAccelFactor = 10.0;

        this.currentDrag = new Vector2();
        this.lastDrag = new Vector2();
    }

    public NaturalMouseKbdListener(final NaturalCamera camera) {
        this(new GaiaGestureListener(), camera);
        EventManager.instance.subscribe(this, Events.TOUCH_DOWN, Events.TOUCH_UP, Events.TOUCH_DRAGGED, Events.SCROLLED, Events.KEY_DOWN, Events.KEY_UP);
    }

    private int touched;
    private boolean multiTouch;

    private KeyframesPathObject kpo;

    private KeyframesPathObject getKeyframesPathObject() {
        if (kpo != null)
            return kpo;

        Array<SceneGraphNode> l = GaiaSky.instance.sg.getRoot().getChildrenByType(KeyframesPathObject.class, new Array<>(1));
        if (!l.isEmpty()) {
            kpo = (KeyframesPathObject) l.get(0);
            return kpo;
        }
        return null;
    }

    private IFocus getKeyframeCollision(int screenX, int screenY) {
        if (getKeyframesPathObject() != null)
            return getKeyframesPathObject().select(screenX, screenY, MIN_PIX_DIST, camera);
        else
            return null;
    }

    private boolean dragKeyframe(int screenX, int screenY, double dragDx, double dragDy) {
        if (isKeyPressed(Keys.SHIFT_LEFT) && !anyPressed(Keys.CONTROL_LEFT, Keys.ALT_LEFT)) {
            // Rotate around up (rotate dir)
            return getKeyframesPathObject().rotateAroundUp(dragDx, dragDy, camera);
        } else if (isKeyPressed(Keys.CONTROL_LEFT) && !anyPressed(Keys.SHIFT_LEFT, Keys.ALT_LEFT)) {
            // Rotate around dir (rotate up)
            return getKeyframesPathObject().rotateAroundDir(dragDx, dragDy, camera);
        } else if (isKeyPressed(Keys.ALT_LEFT) && !anyPressed(Keys.SHIFT_LEFT, Keys.CONTROL_LEFT)) {
            // Rotate around dir.crs(up)
            return getKeyframesPathObject().rotateAroundCrs(dragDx, dragDy, camera);
        }
        return getKeyframesPathObject().moveSelection(screenX, screenY, camera);
    }

    private Array<IFocus> getHits(int screenX, int screenY) {
        Array<IFocus> l = GaiaSky.instance.getFocusableEntities();

        Array<IFocus> hits = new Array<>();

        Iterator<IFocus> it = l.iterator();
        // Add all hits
        while (it.hasNext()) {
            IFocus s = it.next();
            s.addHit(screenX, screenY, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), MIN_PIX_DIST, camera, hits);
        }

        return hits;
    }

    private IFocus getBestHit(int screenX, int screenY) {
        Array<IFocus> hits = getHits(screenX, screenY);
        if (hits.size != 0) {
            // Sort using distance
            hits.sort(comp);
            // Get closest
            return hits.get(hits.size - 1);
        }
        return null;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (GlobalConf.runtime.INPUT_ENABLED) {
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
                // Select keyframes
                if (!(anyPressed(Keys.ALT_LEFT, Keys.SHIFT_LEFT, Keys.CONTROL_LEFT) && getKeyframesPathObject() != null && getKeyframesPathObject().isSelected())) {
                    IFocus hit;
                    keyframeBeingDragged = ((hit = getKeyframeCollision(screenX, screenY)) != null);
                    if(keyframeBeingDragged){
                        // FOCUS_MODE, do not center
                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, hit, false);
                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE, false);
                    }
                }
            }
        }
        camera.setInputByController(false);
        return super.touchDown(screenX, screenY, pointer, button);
    }


    @Override
    public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
        EventManager.instance.post(Events.INPUT_EVENT, button);
        if (GlobalConf.runtime.INPUT_ENABLED) {
            touched &= -1 ^ (1 << pointer);
            multiTouch = !MathUtils.isPowerOfTwo(touched);
            if (button == this.button && button == leftMouseButton) {
                final long currentTime = TimeUtils.millis();
                final long lastLeftTime = lastClickTime;

                GaiaSky.postRunnable(() -> {
                    // 5% of width pixels distance
                    if (!GlobalConf.scene.CINEMATIC_CAMERA || gesture.dst(screenX, screenY) < MOVE_PX_DIST) {
                        boolean stopped = camera.stopMovement();
                        boolean focusRemoved = GaiaSky.instance.mainGui != null && GaiaSky.instance.mainGui.cancelTouchFocus();
                        boolean doubleClick = currentTime - lastLeftTime < doubleClickTime;
                        gesture.set(0, 0);

                        if (doubleClick && !stopped && !focusRemoved) {
                            // Select star, if any
                            IFocus hit = getBestHit(screenX, screenY);
                            if (hit != null) {
                                EventManager.instance.post(Events.FOCUS_CHANGE_CMD, hit);
                                EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
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
                } else if (gesture.dst(screenX, screenY) < MOVE_PX_DIST &&  getKeyframesPathObject() != null && getKeyframesPathObject().isSelected() && !anyPressed(Keys.CONTROL_LEFT, Keys.SHIFT_LEFT, Keys.ALT_LEFT)) {
                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FREE_MODE);
                    getKeyframesPathObject().unselect();
                } else {
                    // Ensure Octants observed property is computed
                    GaiaSky.postRunnable(() -> {
                        // 5% of width pixels distance
                        if (gesture.dst(screenX, screenY) < MOVE_PX_DIST) {
                            // Stop
                            camera.setYaw(0);
                            camera.setPitch(0);

                            // Right click, context menu
                            IFocus hit = getBestHit(screenX, screenY);
                            EventManager.instance.post(Events.POPUP_MENU_FOCUS, hit, screenX, screenY);
                        }
                    });
                    camera.setHorizontal(0);
                    camera.setVertical(0);
                }
            }

            // Remove keyboard focus from GUI elements
            EventManager.instance.notify(Events.REMOVE_KEYBOARD_FOCUS);

            this.button = -1;
        }
        camera.setInputByController(false);
        return super.touchUp(screenX, screenY, pointer, button);
    }

    protected boolean processDrag(int screenX, int screenY, double deltaX, double deltaY, int button) {
        boolean accel = GlobalConf.scene.CINEMATIC_CAMERA;
        if (accel) {
            dragDx = deltaX;
            dragDy = deltaY;
        } else {
            currentDrag.set((float) deltaX, (float) deltaY);
            // Check orientation of last vs current
            if (Math.abs(currentDrag.angle(lastDrag)) > 90) {
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
                if (dragDx != 0)
                    camera.addRoll(dragDx, accel);
            } else {
                camera.addRotateMovement(dragDx, dragDy, false, accel);
            }
        } else if (button == rightMouseButton) {
            if (keyframeBeingDragged || (getKeyframesPathObject() != null && getKeyframesPathObject().isSelected() && anyPressed(Keys.SHIFT_LEFT, Keys.CONTROL_LEFT, Keys.ALT_LEFT))) {
                // Drag keyframe
                dragKeyframe(screenX, screenY, dragDx, dragDy);
            } else {
                if(camera.getMode().isFree()){
                    // Strafe (pan)
                    camera.setHorizontal(-dragDx * 5f);
                    camera.setVertical(-dragDy * 5f);
                } else {
                    // Look around
                    camera.addRotateMovement(dragDx, dragDy, true, accel);
                }
            }
        } else if (button == middleMouseButton) {
            if (dragDx != 0)
                camera.addForwardForce(dragDx);
        }
        camera.setInputByController(false);
        return false;
    }

    private double lowPass(double smoothedValue, double newValue, double smoothing) {
        return smoothedValue + (newValue - smoothedValue) / smoothing;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (GlobalConf.runtime.INPUT_ENABLED) {
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

    @Override
    public boolean scrolled(int amountY) {
        if (GlobalConf.runtime.INPUT_ENABLED) {
            return zoom(amountY * scrollFactor);
        }
        return false;
    }

    public boolean zoom(float amount) {
        if (alwaysScroll)
            camera.addForwardForce(amount);
        camera.setInputByController(false);
        return false;
    }

    @Override
    public void update() {
        if (isKeyPressed(Keys.UP)) {
            camera.addForwardForce(1.0f);
        }
        if (isKeyPressed(Keys.DOWN)) {
            camera.addForwardForce(-1.0f);
        }
        if (isKeyPressed(Keys.RIGHT)) {
            if (camera.getMode().isFocus())
                camera.addHorizontal(-1.0f, true);
            else
                camera.addYaw(1.0f, true);
        }
        if (isKeyPressed(Keys.LEFT)) {
            if (camera.getMode().isFocus())
                camera.addHorizontal(1.0f, true);
            else
                camera.addYaw(-1.0f, true);
        }
    }


    @Override
    public void activate() {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case TOUCH_DOWN:
            this.touchDown((int) data[0], (int) data[1], (int) data[2], (int) data[3]);
            break;
        case TOUCH_UP:
            this.touchUp((int) data[0], (int) data[1], (int) data[2], (int) data[3]);
            break;
        case TOUCH_DRAGGED:
            this.touchDragged((int) data[0], (int) data[1], (int) data[2]);
            break;
        case SCROLLED:
            this.scrolled((int) data[0]);
            break;
        case KEY_DOWN:
            this.keyDown((int) data[0]);
            break;
        case KEY_UP:
            this.keyUp((int) data[0]);
            break;
        default:
            break;
        }
    }

}
