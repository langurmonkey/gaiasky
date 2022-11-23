package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.gui.GenericDialog;
import gaiasky.gui.IGamepadMappings;

/**
 * Provides the default gamepad listener for UI windows.
 */
public class WindowGamepadListener extends AbstractGamepadListener {

    private final Stage stage;
    private final GenericDialog dialog;

    public WindowGamepadListener(String mappingsFile, Stage stage, GenericDialog dialog) {
        super(mappingsFile);
        this.stage = stage;
        this.dialog = dialog;
    }

    public WindowGamepadListener(IGamepadMappings mappings, Stage stage, GenericDialog dialog) {
        super(mappings);
        this.stage = stage;
        this.dialog = dialog;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        super.buttonDown(controller, buttonCode);
        if (buttonCode == mappings.getButtonA()) {
            fireChange();
        } else if (buttonCode == mappings.getButtonB()) {
            cycleDialogButtons();

        } else if (buttonCode == mappings.getButtonLB()) {
            dialog.tabLeft();
        } else if (buttonCode == mappings.getButtonRB()) {
            dialog.tabRight();

        } else if (buttonCode == mappings.getButtonStart()) {
            dialog.closeAccept();
        } else if (buttonCode == mappings.getButtonSelect()) {
            cycleDialogButtons();

        } else if (buttonCode == mappings.getButtonDpadUp()) {
            moveUp();
        } else if (buttonCode == mappings.getButtonDpadDown()) {
            moveDown();
        }
        lastControllerUsed = controller;
        return true;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        return super.buttonUp(controller, buttonCode);
    }

    @Override
    public void pollAxis() {
        if (lastControllerUsed != null) {
            float valueLV = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisLstickV()));
            if (valueLV > 0) {
                moveDown();
            } else if (valueLV < 0) {
                moveUp();
            }
            float valueRV = (float) applyZeroPoint(lastControllerUsed.getAxis(mappings.getAxisRstickV()));
            scroll(valueRV);
        }
    }

    @Override
    public void pollButtons() {
        if (isKeyPressed(mappings.getButtonDpadUp())) {
            moveUp();
        } else if (isKeyPressed(mappings.getButtonDpadDown())) {
            moveDown();
        }
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        long now = TimeUtils.millis();
        value = (float) applyZeroPoint(value);
        if (now - lastAxisEvtTime > AXIS_EVT_DELAY) {
            // Event-based
            if (axisCode == mappings.getAxisLstickV()) {
                // LEFT STICK vertical - move focus
                if (value > 0) {
                    moveDown();
                    lastAxisEvtTime = now;
                } else {
                    moveUp();
                    lastAxisEvtTime = now;
                }
            } else if (axisCode == mappings.getAxisRstickV()) {
                // RIGHT STICK vertical - move scroll
                scroll(value);
                lastAxisEvtTime = now;
            }
        }
        lastControllerUsed = controller;
        return true;
    }

    public void moveUp() {
        move(true);
    }

    public void moveDown() {
        move(false);
    }

    public void move(boolean up) {
        var focus = stage.getKeyboardFocus();
        var inputWidgets = getInputWidgets(dialog.getActualContentContainer(), new Array<>());
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

    public void scroll(float value) {
        if (value != 0) {
            var scroll = getScrollPaneIn(dialog.getActualContentContainer());
            if (scroll != null) {
                scroll.setScrollY(scroll.getScrollY() + 150 * value);
            }
        }
    }

    public void cycleDialogButtons() {
        Actor target = stage.getKeyboardFocus();
        if (target != dialog.acceptButton && dialog.acceptButton != null) {
            stage.setKeyboardFocus(dialog.acceptButton);
        } else if (target != dialog.cancelButton && dialog.cancelButton != null) {
            stage.setKeyboardFocus(dialog.cancelButton);
        }
    }

    public void fireChange() {
        Actor target = stage.getKeyboardFocus();

        if (target != null) {
            if (target instanceof CheckBox) {
                // Check or uncheck
                CheckBox cb = (CheckBox) target;
                if (!cb.isDisabled()) {
                    cb.setChecked(!cb.isChecked());
                }
            } else {
                // Fire change event
                ChangeEvent event = Pools.obtain(ChangeEvent.class);
                event.setTarget(target);
                target.fire(event);
                Pools.free(event);
            }
        }
    }

    private ScrollPane getScrollPaneIn(Actor actor) {
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

    private Array<Actor> getInputWidgets(Actor actor, Array<Actor> list) {
        if (actor != null) {
            if (isInputWidget(actor)) {
                list.add(actor);
            } else if (actor instanceof WidgetGroup) {
                getInputWidgetsInGroup((WidgetGroup) actor, list);
            }
        }
        return list;
    }

    private Array<Actor> getInputWidgetsInGroup(WidgetGroup actor, Array<Actor> list) {
        var children = actor.getChildren();
        for (var child : children) {
            getInputWidgets(child, list);
        }
        return list;
    }

    private boolean isInputWidget(Actor actor) {
        return actor instanceof SelectBox ||
                actor instanceof TextField ||
                actor instanceof Button ||
                actor instanceof Slider;
    }

}
