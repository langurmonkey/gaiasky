/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

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
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.api.IScreen;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.input.AbstractMouseKbdListener;
import gaiasky.input.ScreenGamepadListener;
import gaiasky.input.ScreenKbdListener;
import gaiasky.util.GuiUtils;
import gaiasky.util.Settings.ControlsSettings.GamepadSettings;
import gaiasky.util.scene2d.CollapsibleWindow;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.Separator;
import net.jafama.FastMath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * An abstract, extensible {@code GenericDialog} intended to be subclassed for creating
 * custom interactive dialog windows. It serves as a wrapper around a main content table and
 * a bottom row for action buttons (Accept/Cancel).
 *
 * <p>This class provides a layout manager that automatically arranges the main content area
 * and the footer button group. Subclasses must implement the {@code build()} method to
 * populate the content table before showing the dialog. It supports multiple tabs,
 * modal/non-modal behavior, and captures input (mouse, keyboard, gamepad) to prevent
 * interference with the rest of the application while the dialog is open.</p>
 *
 * <h2>Layout Structure</h2>
 * <ul>
 *   <li><strong>Content Table:</strong> Defined by the {@code build()} method implementation in the subclass.
 *   </li>
 *   <li><strong>Footer Table:</strong> Contains the action buttons (Accept/Cancel) if configured.
 *   </li>
 *   <li><strong>Tab System:</strong> Subclasses can add multiple tabs using {@link #addTabContent(Group)}.
 *   Tabs are managed via a stack and a button group that restricts selection to one tab at a time.
 *   </li>
 * </ul>
 *
 * <h2>Subclassing Guidelines</h2>
 * <p>To create a functional dialog, the subclass must:</p>
 * <ol>
 *   <li>Implement the {@code build()} method to add UI actors to the content table.</li>
 *   <li>Implement the {@code accept()} method, which returns {@code true} if the dialog should close
 *       upon clicking the accept button.</li>
 *   <li>Implement the {@code cancel()} method, which is called upon clicking cancel or pressing Escape.</li>
 *   <li>(Optional) Override {@code dispose()} for cleanup resources.</li>
 * </ol>
 *
 * <h2>Common Operations</h2>
 * <p>Subclasses typically override the following methods or use the provided helpers:</p>
 * <ul>
 *   <li>{@link #show(Stage)} or {@link #show(Stage, Action)} to display the dialog.</li>
 *   <li>{@link #setAcceptText(String)} and {@link #setCancelText(String)} to customize button labels.</li>
 *   <li>{@link #setModal(boolean)} to toggle modal behavior (blocking input).</li>
 *   <li>{@link #addSeparator()} to add horizontal dividers within the content area.</li>
 *   <li>{@link #tabRight()} and {@link #tabLeft()} for manual tab navigation.</li>
 * </ul>
 *
 * <p>The class handles internal logic for:</p>
 * <ul>
 *   <li>Input multiplexing (pausing other listeners when modal, restoring them on hide).</li>
 *   <li>Focus management (setting initial focus, tracking previous focus).</li>
 *   <li>Tab state persistence (remembering the last active tab per subclass type).</li>
 *   <li>Window positioning (centering or custom coordinates).</li>
 * </ul>
 *
 * @see CollapsibleWindow
 */
public abstract class GenericDialog extends CollapsibleWindow implements IScreen {
    /**
     * Stores the last active tab for each subclass.
     */
    private static final Map<Class<? extends GenericDialog>, Integer> lastActiveTab = new HashMap<>(20);

    final protected Stage stage;
    final protected Skin skin;
    protected TextButton acceptButton, cancelButton;
    protected GenericDialog me;
    protected Table content, bottom;
    protected boolean modal = true;
    protected boolean defaultMouseKbdListener = true;
    protected boolean defaultGamepadListener = true;
    protected float lastPosX = -1, lastPosY = -1;
    protected HorizontalGroup buttonGroup;
    protected boolean enterExit = true, escExit = true;
    protected boolean keysListener = true;
    private boolean initializing = true;
    protected Runnable acceptListener, cancelListener, closeListener;
    // Specific mouse/keyboard listener, if any.
    protected AbstractMouseKbdListener mouseKbdListener;
    // The gamepad listener for this window, if any.
    protected AbstractGamepadListener gamepadListener;
    /** If this dialog has tabs, this list holds them. **/
    protected Array<TextButton> tabButtons;
    /** Currently selected tab **/
    protected int selectedTab;
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
    protected Set<ControllerListener> backupGamepadListeners;
    private String acceptText, cancelText;
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
                            if (!initializing) {
                                lastActiveTab.put(me.getClass(), selectedTab);
                            }
                        }
                    }
                }
            };

            // Add listeners to tabs, and tabs and groups.
            initializing = true;
            for (int i = 0; i < tabButtons.size; i++) {
                tabButtons.get(i).addListener(tabListener);
                tabsGroup.add(tabButtons.get(i));
            }
            initializing = false;

            // Select tab.
            var lastSelected = lastActiveTab.getOrDefault(me.getClass(), 0);
            tabsGroup.setChecked(tabButtons.get(lastSelected).getText().toString());
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
            w = FastMath.max(button.getWidth() + pad18 * 4f, w);
        }
        for (Actor button : buttonGroup.getChildren()) {
            button.setWidth(w);
        }

        // Height.
        float h = 30f;
        for (Actor button : buttonGroup.getChildren()) {
            h = FastMath.max(button.getHeight(), h);
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
        if (keysListener) {
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
        }

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
            mouseKbdListener = new ScreenKbdListener(this);
        }
        // Add default gamepad listener.
        if (defaultGamepadListener) {
            gamepadListener = new ScreenGamepadListener(GaiaSky.settings().controls.gamepad.mappingsFile, this);
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
        if (acceptListener != null) {
            acceptListener.run();
        }
        if (closeListener != null) {
            closeListener.run();
        }
        if (close) {
            me.hide();
        }
        return close;
    }

    /**
     * Closes the window with the cancel action.
     *
     * @return Whether the cancel operation succeeded.
     */
    public boolean closeCancel() {
        cancel();
        if (cancelListener != null) {
            cancelListener.run();
        }
        if (closeListener != null) {
            closeListener.run();
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
     *
     * @param stage  The stage.
     * @param action Action to run.
     *
     * @return The generic dialog instance.
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

    public Group getBottomGroup() {
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
        return show(stage, -1, -1);
    }

    public GenericDialog show(Stage stage, float x, float y) {
        show(stage, Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.6f, Interpolation.fade)));
        if (x >= 0 && y >= 0) {
            setPosition(Math.round(x), FastMath.round(y));
        } else if (lastPosX >= 0 && lastPosY >= 0) {
            setPosition(Math.round(lastPosX), FastMath.round(lastPosY));
        } else {
            setPosition(Math.round((stage.getWidth() - getWidth()) / 2f), FastMath.round((stage.getHeight() - getHeight()) / 2f));
        }
        setKeyboardFocus();
        showDialogHook(stage);
        return this;
    }

    protected void showDialogHook(Stage stage) {
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
                Actions.fadeOut(0.6f),
                Actions.removeActor(this)));
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
     * @param colSpan The column span to use.
     */
    public void addSeparator(int colSpan) {
        if (content != null)
            content.add(new Separator(skin, "gray")).colspan(colSpan).center().growX().padTop(pad18).padBottom(pad18).row();
    }

    /**
     * Sets a listener that runs when accept is clicked.
     *
     * @param r The listener.
     */
    public void setAcceptListener(Runnable r) {
        this.acceptListener = r;
    }

    public boolean hasAcceptRunnable() {
        return acceptListener != null;
    }

    /**
     * Sets a listener that runs when cancel is clicked.
     *
     * @param r The listener.
     */
    public void setCancelListener(Runnable r) {
        this.cancelListener = r;
    }

    public boolean hasCancelRunnable() {
        return cancelListener != null;
    }

    /**
     * Sets a listener that runs when the dialog is closed. It runs when either the accept or the cancel button is
     * clicked.
     *
     * @param r The listener.
     */
    public void setCloseListener(Runnable r) {
        this.closeListener = r;
    }

    public boolean hasCloseRunnable() {
        return closeListener != null;
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
            GamepadSettings gamepadSettings = GaiaSky.settings().controls.gamepad;
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
            GamepadSettings gamepadSettings = GaiaSky.settings().controls.gamepad;
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

    public Array<TextButton> getTabButtons() {
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

    public Button acceptButton() {
        return acceptButton;
    }

    public Button cancelButton() {
        return cancelButton;
    }
}
