package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.gui.IGamepadMappings;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnSliderPlus;

public abstract class GuiGamepadListener extends AbstractGamepadListener {
    protected final Stage stage;

    public GuiGamepadListener(String mappingsFile, Stage stage) {
        super(mappingsFile);
        this.stage = stage;
    }

    public GuiGamepadListener(IGamepadMappings mappings, Stage stage) {
        super(mappings);
        this.stage = stage;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        long now = TimeUtils.millis();
        super.buttonDown(controller, buttonCode);
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
    public boolean buttonUp(Controller controller, int buttonCode) {
        return super.buttonUp(controller, buttonCode);
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        long now = TimeUtils.millis();
        value = (float) applyZeroPoint(value);
        if (now - lastAxisEvtTime > AXIS_EVT_DELAY) {
            // Event-based
            if (axisCode == mappings.getAxisLstickV()) {
                // LEFT STICK vertical - move focus vertically
                if (value > 0) {
                    moveDown();
                    lastAxisEvtTime = now;
                } else if (value < 0) {
                    moveUp();
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisLstickH()) {
                // LEFT STICK horizontal - move focus horizontally
                if (value > 0) {
                    moveRight();
                    lastAxisEvtTime = now;
                } else if (value < 0) {
                    moveLeft();
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisRstickV()) {
                // RIGHT STICK vertical
                if (value != 0) {
                    rightStickVertical(value);
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisRstickH()) {
                // RIGHT STICK horizontal 
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
    public boolean pollAxis() {
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

    public Group getContentContainer() {
        return stage.getRoot();
    }

    /**
     * By default, the main action acts on the keyboard focus actor by
     * firing a change event or checking it, if it is a checkbox.
     */
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
        var inputWidgets = getInputWidgets(getContentContainer(), new Array<>());
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
            if (stage.getKeyboardFocus() instanceof SelectBox) {
                // Up/down in select box.
                SelectBox<?> sb = (SelectBox<?>) stage.getKeyboardFocus();
                int index = sb.getSelectedIndex();
                if (value < 0) {
                    index = Math.max(index - 1, 0);
                } else if (value > 0) {
                    index = (index + 1) % sb.getItems().size;
                }
                // Select.
                sb.setSelectedIndex(index);
            } else {
                // Move scroll.
                var scroll = getScrollPaneIn(getContentContainer());
                if (scroll != null) {
                    scroll.setScrollY(scroll.getScrollY() + 150 * value);
                }
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
        Actor focus = stage.getKeyboardFocus();
        if (focus instanceof OwnSliderPlus) {
            OwnSliderPlus s = (OwnSliderPlus) focus;
            sliderMove(value > 0, 0.05f, s);
        }
    }

    /**
     * Moves the slider up or down by the given percentage.
     *
     * @param up      Whether to move it up.
     * @param percent The percentage in [0,1].
     * @param slider  The slider to move.
     */
    public void sliderMove(boolean up, float percent, OwnSliderPlus slider) {
        float max = slider.getMaxValue();
        float min = slider.getMinValue();
        float val = slider.getValue();
        float inc = (max - min) * percent;
        slider.setValue(MathUtils.clamp(val + (up ? inc : -inc), min, max));
    }

    protected ScrollPane getScrollPaneIn(Actor actor) {
        if (actor instanceof ScrollPane) {
            return (ScrollPane) actor;
        } else if (actor instanceof WidgetGroup) {
            var group = (WidgetGroup) actor;
            var children = group.getChildren();
            for (var child : children) {
                ScrollPane scroll;
                if ((scroll = getScrollPaneIn(child)) != null) {
                    return scroll;
                }
            }
        }
        return null;
    }

    protected Array<Actor> getInputWidgets(Actor actor, Array<Actor> list) {
        if (actor != null) {
            if (isInputWidget(actor) && isNotDisabled(actor) && !isTooltipWidget(actor)) {
                list.add(actor);
            } else if (actor instanceof WidgetGroup) {
                getInputWidgetsInGroup((WidgetGroup) actor, list);
            }
        }
        return list;
    }

    private void getInputWidgetsInGroup(WidgetGroup actor, Array<Actor> list) {
        var children = actor.getChildren();
        for (var child : children) {
            getInputWidgets(child, list);
        }
    }

    protected boolean isInputWidget(Actor actor) {
        return actor instanceof SelectBox ||
                actor instanceof TextField ||
                actor instanceof Button ||
                actor instanceof Slider;
    }

    protected boolean isTooltipWidget(Actor actor) {
        return actor instanceof OwnImageButton && ((OwnImageButton) actor).getStyle().imageUp.toString().contains("tooltip");
    }

    protected boolean isNotDisabled(Actor actor) {
        return !(actor instanceof Disableable) || !((Disableable) actor).isDisabled();
    }

    protected boolean hasChangeListener(Actor actor) {
        var listeners = actor.getListeners();
        for (var listener : listeners) {
            if (listener instanceof ChangeListener) {
                return true;
            }
        }
        return false;
    }

}
