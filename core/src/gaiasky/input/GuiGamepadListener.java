/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.gui.api.IGamepadMappings;
import gaiasky.util.GuiUtils;

/**
 * A gamepad listener for GUI elements like windows and panes.
 */
public abstract class GuiGamepadListener extends AbstractGamepadListener {
    protected final Stage stage;

    protected GuiGamepadListener(String mappingsFile, Stage stage) {
        super(mappingsFile);
        this.stage = stage;
    }

    protected GuiGamepadListener(IGamepadMappings mappings, Stage stage) {
        super(mappings);
        this.stage = stage;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        long now = TimeUtils.millis();
        if (buttonCode == mappings.getButtonA()) {
            actionDown();
            lastButtonPollTime = now;
        } else if (buttonCode == mappings.getButtonB()) {
            back();
            lastButtonPollTime = now;

        } else if (buttonCode == mappings.getButtonLB()) {
            tabLeft();
            lastButtonPollTime = now;
        } else if (buttonCode == mappings.getButtonRB()) {
            tabRight();
            lastButtonPollTime = now;

        } else if (buttonCode == mappings.getButtonStart()) {
            start();
            lastButtonPollTime = now;
        } else if (buttonCode == mappings.getButtonSelect()) {
            select();
            lastButtonPollTime = now;

        } else if (buttonCode == mappings.getButtonDpadUp()) {
            moveUp();
            lastButtonPollTime = now;
        } else if (buttonCode == mappings.getButtonDpadDown()) {
            moveDown();
            lastButtonPollTime = now;
        } else if (buttonCode == mappings.getButtonDpadRight()) {
            moveRight();
            lastButtonPollTime = now;
        } else if (buttonCode == mappings.getButtonDpadLeft()) {
            moveLeft();
            lastButtonPollTime = now;
        }
        lastControllerUsed = controller;

        return true;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        long now = TimeUtils.millis();
        value = (float) applyZeroPoint(value);
        if (now - lastAxisEvtTime > axisEventDelay) {
            // Event-based
            if (axisCode == mappings.getAxisLstickV()) {
                // LEFT STICK vertical - move focus vertically.
                if (value > 0) {
                    moveDown();
                    lastAxisEvtTime = now;
                } else if (value < 0) {
                    moveUp();
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisLstickH()) {
                // LEFT STICK horizontal - move focus horizontally.
                if (value > 0) {
                    moveRight();
                    lastAxisEvtTime = now;
                } else if (value < 0) {
                    moveLeft();
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisRstickV()) {
                // RIGHT STICK vertical.
                if (value != 0) {
                    rightStickVertical(value);
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisRstickH()) {
                // RIGHT STICK horizontal.
                if (value != 0) {
                    rightStickHorizontal(value);
                    lastAxisEvtTime = now;
                }
            }
        }
        lastControllerUsed = controller;
        return true;
    }

    @Override
    public boolean pollAxes() {
        if (lastControllerUsed != null) {
            float valueLV = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisLstickV()));
            if (valueLV > 0) {
                moveDown();
                return true;
            } else if (valueLV < 0) {
                moveUp();
                return true;
            }
            float valueLH = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisLstickH()));
            if (valueLH > 0) {
                moveRight();
                return true;
            } else if (valueLH < 0) {
                moveLeft();
                return true;
            }
            float valueRV = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisRstickV()));
            if (valueRV != 0) {
                rightStickVertical(valueRV);
                return true;
            }
            float valueRH = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisRstickH()));
            if (valueRH != 0) {
                rightStickHorizontal(valueRH);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean pollButtons() {
        if (isKeyPressed(mappings.getButtonDpadUp())) {
            moveUp();
            return true;
        } else if (isKeyPressed(mappings.getButtonDpadDown())) {
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

    /**
     * By default, the main action acts on the keyboard focus actor by
     * firing a change event or checking it, if it is a checkbox.
     */
    public void actionDown() {
        Actor target = stage.getKeyboardFocus();

        if (target != null) {
            if (target instanceof Button b) {
                // Check or uncheck.
                if (!b.isDisabled()) {
                    b.setChecked(!b.isChecked());
                }
            } else {
                // Fire change event.
                ChangeEvent event = Pools.obtain(ChangeEvent::new);
                event.setTarget(target);
                target.fire(event);
                Pools.free(event);
            }
        }
    }

    public abstract void back();

    public abstract void start();

    public abstract void select();

    public abstract void tabLeft();

    public abstract void tabRight();

    public abstract void moveLeft();

    public abstract void moveRight();

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
            GuiUtils.ensureScrollVisible(inputWidgets.get(index));
        }
    }

    /**
     * By default, the vertical right stick does:
     * <ul>
     *     <li>Change the selection in select boxes.</li>
     *     <li>Move the first scroll pane found in the content up and down.</li>
     * </ul>
     *
     * @param value The axis value.
     */
    public void rightStickVertical(float value) {
        if (value != 0) {
            rightStickVertical(stage.getKeyboardFocus(), value);
        }
    }

    protected void rightStickVertical(Actor focus, float value) {
        if (focus instanceof SelectBox) {
            // Up/down in select box.
            var selectBox = (SelectBox<?>) stage.getKeyboardFocus();
            GuiUtils.selectBoxMoveSelection(value < 0, false, selectBox);
        } else {
            // Move scroll.
            var scroll = GuiUtils.getScrollPaneIn(getContentContainers().get(0));
            if (scroll != null) {
                scroll.setScrollY(scroll.getScrollY() + 300 * value);
            }
        }
    }

    /**
     * By default, the horizontal right stick does:
     * <ul>
     *     <li>Move sliders right and left.</li>
     * </ul>
     *
     * @param value The axis value.
     */
    public void rightStickHorizontal(float value) {
        rightStickHorizontal(stage.getKeyboardFocus(), value);
    }

    protected void rightStickHorizontal(Actor focus, float value) {
        if (focus instanceof Slider) {
            GuiUtils.sliderMove(value > 0, 0.05f, (Slider) focus);
        } else if (focus instanceof SelectBox) {
            var selectBox = (SelectBox<?>) stage.getKeyboardFocus();
            GuiUtils.selectBoxMoveSelection(value < 0, false, selectBox);
        }
    }

}
