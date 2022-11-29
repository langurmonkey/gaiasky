package gaiasky.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.util.GuiUtils;
import gaiasky.util.scene2d.OwnSliderPlus;
import gaiasky.util.scene2d.OwnTextField;

public abstract class GuiKbdListener extends AbstractMouseKbdListener {

    protected final Stage stage;

    protected GuiKbdListener(Stage stage) {
        super(new GuiGestureListener(), null);
        this.stage = stage;
        this.minPollInterval = 250;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isActive()) {
            long now = TimeUtils.millis();
            boolean b = super.keyDown(keycode);
            lastPollTime = now;
            return b;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isActive()) {
            long now = TimeUtils.millis();
            super.keyUp(keycode);
            lastPollTime = now;

            if (keycode == Keys.UP) {
                // Focus up.
                moveUp();
                return true;
            } else if (keycode == Keys.DOWN) {
                // Focus down.
                moveDown();
                return true;
            } else if (keycode == Keys.RIGHT) {
                moveRight();
                return true;
            } else if (keycode == Keys.LEFT) {
                moveLeft();
                return true;
            } else if (keycode == Keys.TAB && !isKeyPressed(Keys.SHIFT_LEFT)) {
                // Tab right.
                tabRight();
                return true;
            } else if (keycode == Keys.TAB && isKeyPressed(Keys.SHIFT_LEFT)) {
                // Tab left.
                tabLeft();
                return true;
            } else if (keycode == Keys.ENTER) {
                // Action down.
                actionDown();
                return true;
            } else if (keycode == Keys.ESCAPE) {
                // Close (cancel) dialog.
                close();
                return true;
            } else if (keycode == Keys.HOME) {
                moveHome();
                return true;
            } else if (keycode == Keys.END) {
                moveEnd();
                return true;
            } else if (keycode == Keys.ALT_LEFT || keycode == Keys.ALT_RIGHT) {
                select();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean pollKeys() {
        if (isKeyPressed(Keys.UP)) {
            moveUp();
            return true;
        } else if (isKeyPressed(Keys.DOWN)) {
            moveDown();
            return true;
        }
        return false;
    }

    public Array<Group> getContentContainers() {
        var a = new Array<Group>(1);
        a.add(stage.getRoot());
        return a;
    }

    public abstract void close();

    public void actionDown() {
        Actor target = stage.getKeyboardFocus();

        if (target != null) {
            if (target instanceof CheckBox) {
                // Check or uncheck.
                CheckBox cb = (CheckBox) target;
                if (!cb.isDisabled()) {
                    cb.setChecked(!cb.isChecked());
                }
            } else {
                // Fire change event.
                ChangeEvent event = Pools.obtain(ChangeEvent.class);
                event.setTarget(target);
                target.fire(event);
                Pools.free(event);
            }
        }
    }

    public abstract void accept();

    public abstract void select();

    public abstract void tabLeft();

    public abstract void tabRight();

    public void moveLeft() {
        moveHorizontal(true);
    }

    public void moveRight() {
        moveHorizontal(false);
    }

    private void moveHorizontal(boolean left) {
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof OwnTextField) {
            var textField = (OwnTextField) focus;
            textField.moveCursor(!left, false);
        } else if (focus instanceof OwnSliderPlus) {
            var slider = (OwnSliderPlus) focus;
            GuiUtils.sliderMove(!left, 0.05f, slider);
        } else if (focus instanceof SelectBox) {
            var selectBox = (SelectBox<?>) focus;
            GuiUtils.selectBoxMoveSelection(left, false, selectBox);
        }
    }

    public void moveHome() {
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof OwnTextField) {
            var textField = (OwnTextField) focus;
            textField.goHome(true);
        } else if (focus instanceof OwnSliderPlus) {
            OwnSliderPlus s = (OwnSliderPlus) focus;
            GuiUtils.sliderMove(false, 1.0f, s);
        } else if (focus instanceof SelectBox) {
            var selectBox = (SelectBox<?>) focus;
            GuiUtils.selectBoxMoveSelection(true, true, selectBox);
        }
    }

    public void moveEnd() {
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof OwnTextField) {
            var textField = (OwnTextField) focus;
            textField.goEnd(true);
        } else if (focus instanceof OwnSliderPlus) {
            OwnSliderPlus s = (OwnSliderPlus) focus;
            GuiUtils.sliderMove(true, 1.0f, s);
        } else if (focus instanceof SelectBox) {
            var selectBox = (SelectBox<?>) focus;
            GuiUtils.selectBoxMoveSelection(false, true, selectBox);
        }
    }

    /**
     * Moves the focus up.
     */
    public void moveUp() {
        moveFocusVertical(true);
    }

    /**
     * Moves the focus down.
     */
    public void moveDown() {
        moveFocusVertical(false);
    }

    public void moveFocusVertical(boolean up) {
        var focus = stage.getKeyboardFocus();
        var inputWidgets = GuiUtils.getInputWidgets(getContentContainers(), new Array<>());
        if (!inputWidgets.isEmpty()) {
            var index = inputWidgets.indexOf(focus, true);
            if (index < 0) {
                index = 0;
            }
            if (up) {
                index = index - 1;
                if (index < 0) {
                    index = inputWidgets.size - 1;
                }
            } else {
                index = (index + 1) % inputWidgets.size;
            }
            stage.setKeyboardFocus(inputWidgets.get(index));
        }
    }

    private static class GuiGestureListener implements GestureListener {

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
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
        public boolean panStop(float x, float y, int pointer, int button) {
            return false;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
            return false;
        }

        @Override
        public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
            return false;
        }

        @Override
        public void pinchStop() {

        }
    }
}
