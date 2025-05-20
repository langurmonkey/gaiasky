/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.util.GuiUtils;

public abstract class GuiKbdListener extends AbstractMouseKbdListener {

    protected final Stage stage;

    protected GuiKbdListener(Stage stage) {
        super(new GuiGestureListener(), null);
        this.stage = stage;
        this.minPollInterval = 250;
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (isActive()) {
            long now = TimeUtils.millis();
            super.keyDown(keyCode);
            lastPollTime = now;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keyCode) {
        if (isActive()) {
            // Convert to logical.
            keyCode = InputUtils.physicalToLogicalKeyCode(keyCode);

            long now = TimeUtils.millis();
            super.keyUp(keyCode);
            lastPollTime = now;

            return switch (keyCode) {
                case Keys.UP -> moveUp();
                case Keys.DOWN -> moveDown();
                case Keys.LEFT -> moveLeft();
                case Keys.TAB -> !isKeyPressed(Keys.SHIFT_LEFT) ? tabRight() : tabLeft();
                case Keys.ENTER -> actionDown();
                case Keys.ESCAPE -> close();
                case Keys.HOME -> moveHome();
                case Keys.END -> moveEnd();
                case Keys.ALT_LEFT, Keys.ALT_RIGHT -> select();
                default -> false;
            };
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

    public boolean actionDown() {
        Actor target = stage.getKeyboardFocus();

        if (target != null) {
            if (target instanceof CheckBox cb) {
                // Check or uncheck.
                if (!cb.isDisabled()) {
                    cb.setChecked(!cb.isChecked());
                    return true;
                }
            } else if (target instanceof Button) {
                // Fire change event on buttons.
                ChangeEvent event = Pools.obtain(ChangeEvent::new);
                event.setTarget(target);
                target.fire(event);
                Pools.free(event);
                return true;
            }
        }
        return false;
    }

    public abstract boolean close();

    public abstract boolean accept();

    public abstract boolean select();

    public abstract boolean tabLeft();

    public abstract boolean tabRight();

    public boolean moveLeft() {
        return moveHorizontal(true);
    }

    public boolean moveRight() {
        return moveHorizontal(false);
    }

    private boolean moveHorizontal(boolean left) {
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof Slider) {
            GuiUtils.sliderMove(!left, 0.05f, (Slider) focus);
            return true;
        } else if (focus instanceof SelectBox<?> selectBox) {
            GuiUtils.selectBoxMoveSelection(left, false, selectBox);
            return true;
        }
        return false;
    }

    public boolean moveHome() {
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof Slider) {
            GuiUtils.sliderMove(false, 1.0f, (Slider) focus);
            return true;
        } else if (focus instanceof SelectBox<?> selectBox) {
            GuiUtils.selectBoxMoveSelection(true, true, selectBox);
            return true;
        }
        return false;
    }

    public boolean moveEnd() {
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof Slider) {
            GuiUtils.sliderMove(true, 1.0f, (Slider) focus);
            return true;
        } else if (focus instanceof SelectBox<?> selectBox) {
            GuiUtils.selectBoxMoveSelection(false, true, selectBox);
            return true;
        }
        return false;
    }

    /**
     * Moves the focus up.
     */
    public boolean moveUp() {
        return moveFocusVertical(true);
    }

    /**
     * Moves the focus down.
     */
    public boolean moveDown() {
        return moveFocusVertical(false);
    }

    public boolean moveFocusVertical(boolean up) {
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
            GuiUtils.ensureScrollVisible(inputWidgets.get(index));
            return true;
        }
        return false;
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
