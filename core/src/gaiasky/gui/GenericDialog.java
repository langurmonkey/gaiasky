/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.input.AbstractMouseKbdListener;
import gaiasky.input.WindowGamepadListener;
import gaiasky.input.WindowKbdListener;
import gaiasky.util.GuiUtils;
import gaiasky.util.Settings;
import gaiasky.util.Settings.ControlsSettings.GamepadSettings;
import gaiasky.util.scene2d.CollapsibleWindow;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.Separator;

import java.util.HashSet;
import java.util.Set;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

public abstract class GenericDialog extends CollapsibleWindow {
    protected static float pad34;
    protected static float pad20;
    protected static float pad18;
    protected static float pad10;

    static {
        updatePads();
    }

    final protected Stage stage;
    final protected Skin skin;
    public TextButton acceptButton, cancelButton;
    protected GenericDialog me;
    protected Table content, bottom;
    protected boolean modal = true;
    protected boolean defaultMouseKbdListener = true;
    protected boolean defaultGamepadListener = true;
    protected float lastPosX = -1, lastPosY = -1;
    protected HorizontalGroup buttonGroup;
    protected boolean enterExit = true, escExit = true;
    protected Runnable acceptRunnable, cancelRunnable;
    // Specific mouse/keyboard listener, if any.
    protected AbstractMouseKbdListener mouseKbdListener;
    // The gamepad listener for this window, if any.
    protected AbstractGamepadListener gamepadListener;
    /** If this dialog has tabs, this list holds them. **/
    protected Array<Button> tabButtons;
    /** Currently selected tab **/
    protected int selectedTab = 0;
    /** Actual actor for each tab. **/
    protected Array<Group> tabContents;
    /** Tab contents stack. **/
    protected Stack tabStack;
    protected InputListener ignoreTouchDown = new InputListener() {
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.cancel();
            return false;
        }
    };
    protected Array<OwnScrollPane> scrolls;
    // Backup active mouse/keyboard listeners before entering this dialog.
    protected Set<AbstractMouseKbdListener> backupMouseKbdListeners = new HashSet<>();
    // Backup of the gamepad listeners present before entering this dialog.
    protected Set<ControllerListener> backupGamepadListeners = null;
    private String acceptText = null, cancelText = null;
    private String acceptStyle = "default", cancelStyle = "default";
    private Actor previousKeyboardFocus, previousScrollFocus;

    public GenericDialog(String title, Skin skin, Stage stage) {
        super(title, skin);
        this.skin = skin;
        this.stage = stage;
        this.me = this;
        this.content = new Table(skin);
        this.bottom = new Table(skin);
        this.scrolls = new Array<>(false, 5);
    }

    public static void updatePads() {
        pad34 = 34f;
        pad20 = 20f;
        pad18 = 18f;
        pad10 = 10f;
    }

    public void setAcceptText(String acceptText) {
        this.acceptText = acceptText;
        if (acceptButton != null) {
            acceptButton.setText(acceptText);
            recalculateButtonSize();
        }
    }

    public void setAcceptButtonStyle(String style) {
        this.acceptStyle = style;
        if (acceptButton != null) {
            acceptButton.setStyle(skin.get(this.acceptStyle, TextButtonStyle.class));
            recalculateButtonSize();
        }
    }

    public void setAcceptButtonColor(Color col) {
        if (acceptButton != null) {
            acceptButton.setColor(col);
        }
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
        if (cancelButton != null) {
            cancelButton.setText(cancelText);
            recalculateButtonSize();
        }
    }

    public void setCancelButtonStyle(String style) {
        this.cancelStyle = style;
        if (cancelButton != null) {
            cancelButton.setStyle(skin.get(this.cancelStyle, TextButtonStyle.class));
            recalculateButtonSize();
        }
    }

    public void setCancelButtonColors(Color textColor, Color buttonColor) {
        if (cancelButton != null) {
            cancelButton.setColor(buttonColor);
            cancelButton.getLabel().setColor(textColor);
        }
    }

    /**
     * Add tab contents to the list of tabs.
     *
     * @param tabContent The contents.
     */
    protected void addTabContent(Group tabContent) {
        if (tabStack == null) {
            tabStack = new Stack();
        }
        if (tabContents == null) {
            tabContents = new Array<>();
        }
        tabStack.add(tabContent);
        tabContents.add(tabContent);
    }

    /**
     * Prepares the tab button listeners that make tab buttons actually change the content. It also
     * sets up the button group, which restricts the number of tabs that can be checked at once to one.
     */
    protected void setUpTabListeners() {
        if (tabContents != null && tabButtons != null && tabButtons.size == tabContents.size) {
            // Let only one tab button be checked at a time.
            ButtonGroup<Button> tabsGroup = new ButtonGroup<>();
            tabsGroup.setMinCheckCount(1);
            tabsGroup.setMaxCheckCount(1);

            // Listen to changes in the tab button checked states.
            // Set visibility of the tab content to match the checked state.
            ChangeListener tabListener = new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (((Button) actor).isChecked()) {
                        for (int i = 0; i < tabContents.size; i++) {
                            tabContents.get(i).setVisible(tabButtons.get(i).isChecked());
                        }
                        if (tabButtons != null) {
                            selectedTab = tabsGroup.getCheckedIndex();
                        }
                    }
                }
            };

            // Add listeners to tabs, and tabs and groups.
            for (int i = 0; i < tabButtons.size; i++) {
                tabButtons.get(i).addListener(tabListener);
                tabsGroup.add(tabButtons.get(i));
            }
        }
    }

    public void setModal(boolean modal) {
        this.modal = modal;
        super.setModal(modal);
    }

    protected void recalculateButtonSize() {
        // Width.
        float w = 128f;
        for (Actor button : buttonGroup.getChildren()) {
            w = Math.max(button.getWidth() + pad18 * 4f, w);
        }
        for (Actor button : buttonGroup.getChildren()) {
            button.setWidth(w);
        }

        // Height.
        float h = 30f;
        for (Actor button : buttonGroup.getChildren()) {
            h = Math.max(button.getHeight(), h);
        }
        for (Actor button : buttonGroup.getChildren()) {
            button.setHeight(h + 5);
        }
    }

    public void buildSuper() {

        // BUTTONS
        buttonGroup = new HorizontalGroup();
        buttonGroup.space(pad10);

        if (acceptText != null) {
            acceptButton = new OwnTextButton(acceptText, skin, acceptStyle);
            acceptButton.setName("accept");
            acceptButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    return closeAccept();
                }
                return false;

            });
            buttonGroup.addActor(acceptButton);
        }
        if (cancelText != null) {
            cancelButton = new OwnTextButton(cancelText, skin, cancelStyle);
            cancelButton.setName("cancel");
            cancelButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    return closeCancel();
                }

                return false;
            });
            buttonGroup.addActor(cancelButton);
        }
        recalculateButtonSize();

        add(content).left().pad(pad18).row();
        add(bottom).expandY().bottom().right().padRight(pad18).row();
        add(buttonGroup).pad(pad18).bottom().right();
        getTitleTable().align(Align.left);

        // Align top left
        align(Align.top | Align.left);

        pack();

        // Add keys for ESC, ENTER and TAB
        me.addListener(event -> {
            if (event instanceof InputEvent ie) {
                if (ie.getType() == Type.keyUp) {
                    int key = ie.getKeyCode();
                    switch (key) {
                    case Keys.ESCAPE -> {
                        if (escExit) {
                            closeCancel();
                        }
                        // Do not propagate to parents
                        event.stop();
                        return true;
                    }
                    case Keys.ENTER -> {
                        if (enterExit) {
                            closeAccept();
                        }
                        // Do not propagate to parents
                        event.stop();
                        return true;
                    }
                    case Keys.TAB -> {
                        // Next focus, do nothing
                        return true;
                    }
                    default -> {
                    }
                    // Nothing
                    }
                }
            }
            return false;
        });

        // CAPTURE SCROLL FOCUS
        stage.addListener(event -> {
            if (event instanceof InputEvent ie) {

                if (ie.getType() == Type.mouseMoved) {
                    for (OwnScrollPane scroll : scrolls) {
                        if (ie.getTarget().isDescendantOf(scroll)) {
                            stage.setScrollFocus(scroll);
                        }
                    }
                }
            }
            return false;
        });

        // Build actual content.
        build();

        // Modal.
        setModal(this.modal);

        // Add default mouse/kdb listener
        if (defaultMouseKbdListener) {
            mouseKbdListener = new WindowKbdListener(this);
        }
        // Add default gamepad listener.
        if (defaultGamepadListener) {
            gamepadListener = new WindowGamepadListener(Settings.settings.controls.gamepad.mappingsFile, this);
        }
    }

    /**
     * Build the content here.
     */
    protected abstract void build();

    /**
     * Closes the window with the accept action.
     */
    public boolean closeAccept() {
        // Exit
        boolean close = accept();
        if (acceptRunnable != null) {
            acceptRunnable.run();
        }
        if (close) {
            me.hide();
        }
        return close;
    }

    /**
     * Closes the window with the cancel action.
     */
    public boolean closeCancel() {
        cancel();
        if (cancelRunnable != null) {
            cancelRunnable.run();
        }
        me.hide();
        return true;
    }

    /**
     * The accept function, if any.
     *
     * @return True if the dialog must close after the call, false otherwise.
     */
    protected abstract boolean accept();

    /**
     * The cancel function, if any.
     */
    protected abstract void cancel();

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage with custom
     * action which can be null for instant show.
     */
    public GenericDialog show(Stage stage, Action action) {
        clearActions();
        removeCaptureListener(ignoreTouchDown);

        previousKeyboardFocus = null;
        Actor actor = stage.getKeyboardFocus();
        if (actor != null && !actor.isDescendantOf(this))
            previousKeyboardFocus = actor;

        previousScrollFocus = null;
        actor = stage.getScrollFocus();
        if (actor != null && !actor.isDescendantOf(this))
            previousScrollFocus = actor;

        pack();
        stage.addActor(this);
        stage.setKeyboardFocus(this);
        stage.setScrollFocus(this);
        if (action != null)
            addAction(action);

        addOwnListeners();
        touch();

        // Focus first
        focusFirstInputWidget();
        EventManager.publish(Event.CLEAN_PRESSED_KEYS, this);

        if (this.modal) {
            // Disable input
            EventManager.publish(Event.INPUT_ENABLED_CMD, this, false);
        }

        return this;
    }

    /**
     * Override this method to update the contents of this dialog before displaying it.
     */
    public void touch() {
    }

    public Group getCurrentContentContainer() {
        if (tabButtons != null && !tabButtons.isEmpty()) {
            return tabContents.get(selectedTab);
        } else {
            return content;
        }
    }

    public Group getBottmGroup() {
        return bottom;
    }

    public Group getButtonsGroup() {
        return buttonGroup;
    }

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage, centered with
     * default fadeIn action.
     */
    public GenericDialog show(Stage stage) {
        show(stage, Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.6f, Interpolation.fade)));
        if (lastPosX >= 0 && lastPosY >= 0) {
            setPosition(Math.round(lastPosX), Math.round(lastPosY));
        } else {
            setPosition(Math.round((stage.getWidth() - getWidth()) / 2f), Math.round((stage.getHeight() - getHeight()) / 2f));
        }
        setKeyboardFocus();
        showDialogHook(stage);
        return this;
    }

    protected void showDialogHook(Stage stage) {
    }

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage at the specified position.
     */
    public GenericDialog show(Stage stage, float x, float y) {
        show(stage, sequence(Actions.alpha(0f), Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds(), Interpolation.fade)));
        setPosition(Math.round(x), Math.round(y));
        setKeyboardFocus();
        return this;
    }

    /**
     * Sets the keyboard focus, override in case you want to set the focus to a
     * specific item.
     **/
    public void setKeyboardFocus() {

    }

    /**
     * Hides the dialog with the given action and then removes it from the
     * stage.
     */
    public void hide(Action action) {
        lastPosX = this.getX();
        lastPosY = this.getY();

        Stage stage = getStage();
        if (stage != null) {
            if (previousKeyboardFocus != null && previousKeyboardFocus.getStage() == null)
                previousKeyboardFocus = null;
            Actor actor = stage.getKeyboardFocus();
            if (actor == null || actor.isDescendantOf(this))
                stage.setKeyboardFocus(previousKeyboardFocus);

            if (previousScrollFocus != null && previousScrollFocus.getStage() == null)
                previousScrollFocus = null;
            actor = stage.getScrollFocus();
            if (actor == null || actor.isDescendantOf(this))
                stage.setScrollFocus(previousScrollFocus);
        }
        if (action != null) {
            addCaptureListener(ignoreTouchDown);
            addAction(sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()));
        } else
            remove();

        removeOwnListeners();
        EventManager.publish(Event.CLEAN_PRESSED_KEYS, this);

        if (this.modal) {
            // Enable input
            EventManager.publish(Event.INPUT_ENABLED_CMD, this, true);
        }
    }

    /**
     * Hides the dialog. Called automatically when a button is clicked. The
     * default implementation fades out the dialog over 400 milliseconds and
     * then removes it from the stage.
     */
    public void hide() {
        hide(Actions.sequence(
                Actions.alpha(1f),
                Actions.fadeOut(0.6f)));
    }

    /**
     * Adds a horizontal separator to the main content table with the default colspan of 1.
     */
    public void addSeparator() {
        addSeparator(1);
    }

    /**
     * Adds a horizontal separator to the main content table with the given colspan.
     *
     * @param colspan The colspan to use.
     */
    public void addSeparator(int colspan) {
        if (content != null)
            content.add(new Separator(skin, "gray")).colspan(colspan).center().growX().padTop(pad18).padBottom(pad18).row();
    }

    /**
     * Sets the runnable which runs when accept is clicked.
     *
     * @param r The runnable.
     */
    public void setAcceptRunnable(Runnable r) {
        this.acceptRunnable = r;
    }

    public boolean hasAcceptRunnable() {
        return acceptRunnable != null;
    }

    /**
     * Sets the runnable which runs when cancel is clicked.
     *
     * @param r The runnable.
     */
    public void setCancelRunnable(Runnable r) {
        this.cancelRunnable = r;
    }

    public boolean hasCancelRunnable() {
        return cancelRunnable != null;
    }

    /**
     * Sets the enabled property on the given components.
     *
     * @param enabled    The state to set.
     * @param components The components to enable or disable.
     */
    protected void enableComponents(boolean enabled, Disableable... components) {
        for (Disableable c : components) {
            if (c != null)
                c.setDisabled(!enabled);
        }
    }

    private void addOwnListeners() {
        if (mouseKbdListener != null) {
            var inputProcessor = Gdx.input.getInputProcessor();
            if (inputProcessor instanceof InputMultiplexer inputMultiplexer) {
                for (var processor : inputMultiplexer.getProcessors()) {
                    if (processor instanceof AbstractMouseKbdListener abstractMouseKbdListener) {
                        if (abstractMouseKbdListener.isActive() && isModal()) {
                            abstractMouseKbdListener.deactivate();
                            backupMouseKbdListeners.add(abstractMouseKbdListener);
                        }
                    }
                }
                inputMultiplexer.addProcessor(0, mouseKbdListener);
            }
        }
        if (gamepadListener != null) {
            GamepadSettings gamepadSettings = Settings.settings.controls.gamepad;
            // Backup and clean
            backupGamepadListeners = gamepadSettings.getControllerListeners();
            gamepadSettings.removeAllControllerListeners();

            // Add and activate.
            gamepadSettings.addControllerListener(gamepadListener);
        }
    }

    private void removeOwnListeners() {
        if (mouseKbdListener != null) {
            var inputProcessor = Gdx.input.getInputProcessor();
            if (inputProcessor instanceof InputMultiplexer inputMultiplexer) {
                inputMultiplexer.removeProcessor(mouseKbdListener);
                for (var abstractMouseKbdListener : backupMouseKbdListeners) {
                    abstractMouseKbdListener.activate();
                }
                backupMouseKbdListeners.clear();
            }
        }
        if (gamepadListener != null) {
            GamepadSettings gamepadSettings = Settings.settings.controls.gamepad;
            // Remove current listener
            gamepadSettings.removeControllerListener(gamepadListener);

            // Restore backup.
            gamepadSettings.setControllerListeners(backupGamepadListeners);
            backupGamepadListeners = null;
        }
    }

    public boolean tabRight() {
        if (tabButtons != null) {
            selectedTab = (selectedTab + 1) % tabButtons.size;
            tabButtons.get(selectedTab).setChecked(true);
            focusFirstInputWidget();
            return true;
        }
        return false;
    }

    public boolean tabLeft() {
        if (tabButtons != null) {
            selectedTab = selectedTab - 1;
            if (selectedTab < 0) {
                selectedTab = tabButtons.size - 1;
            }
            tabButtons.get(selectedTab).setChecked(true);
            focusFirstInputWidget();
            return true;
        }
        return false;
    }

    public void focusFirstInputWidget() {
        var inputWidgets = GuiUtils.getInputWidgets(getCurrentContentContainer(), new Array<>());
        if (!inputWidgets.isEmpty()) {
            stage.setKeyboardFocus(inputWidgets.get(0));
        }
    }

    public Table getContent() {
        return content;
    }

    public Table getBottom() {
        return bottom;
    }

    public Array<Button> getTabButtons() {
        return tabButtons;
    }

    public abstract void dispose();

    @Override
    public void act(float delta) {
        super.act(delta);
        if (gamepadListener != null && gamepadListener.isActive()) {
            gamepadListener.update();
        }
    }

    public Stage getStage() {
        return stage;
    }

}
