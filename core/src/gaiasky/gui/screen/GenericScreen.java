/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.screen;

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
import gaiasky.gui.api.IScreen;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.input.AbstractMouseKbdListener;
import gaiasky.input.ScreenGamepadListener;
import gaiasky.input.ScreenKbdListener;
import gaiasky.util.GuiUtils;
import gaiasky.util.Settings;
import gaiasky.util.Settings.ControlsSettings.GamepadSettings;
import gaiasky.util.scene2d.OwnLabel;
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
 * Abstract base class for full-screen UI views within the application.
 *
 * <p>{@code GenericScreen} extends {@link Table} to provide a structured layout that occupies the entire
 * application viewport. It handles common UI concerns such as managing modal overlays, input redirection,
 * keyboard shortcuts (ESC, ENTER, TAB), gamepad listeners, and tabbed content interfaces.</p>
 *
 * <p>This class provides a consistent framework for creating dialogs, menus, or main screens. It
 * automatically manages the focus hierarchy, input processors, and event listeners when the screen is
 * shown or hidden.</p>
 *
 * <h3>Key Features</h3>
 * <ul>
 *   <li><b>Full-Viewport Layout:</b> Takes up the whole application viewport by default.</li>
 *   <li><b>Modal Support:</b> Configurable modal behavior to capture and disable external input.</li>
 *   <li><b>Input Management:</b> Automatically backs up and restores previous mouse/keyboard/gamepad
 *     listeners when entering or leaving the screen.</li>
 *   <li><b>Keyboard Shortcuts:</b> Built-in support for ESC (close/cancel), ENTER (accept), and TAB
 *     (focus switching).</li>
 *   <li><b>Tab System:</b> Integrated support for tabbed interfaces with automatic listener setup.</li>
 *   <li><b>Animation:</b> Default fade-in/fade-out animations when showing/hiding.</li>
 * </ul>
 *
 * <h3>Layout Structure</h3>
 * <p>By default, the screen layout consists of four parts:</p>
 * <ol>
 *   <li><b>Title:</b> A top bar displaying the screen title.</li>
 *   <li><b>Content:</b> The main area for UI elements.</li>
 *   <li><b>Buttons:</b> A horizontal group containing the Accept and Cancel buttons (right-aligned).</li>
 *   <li><b>Tabs:</b> Optional tabs line to support content switching, managed by the subclass.</li>
 * </ol>
 *
 * <h3>Subclassing</h3>
 * <p>To create a new screen, extend {@code GenericScreen} and:</p>
 * <ol>
 *   <li>Override {@link #build()}: Populate the {@link #content} table with your UI actors.</li>
 *   <li>Override {@link #accept()}: Define what happens when the Accept button is clicked.</li>
 *   <li>Override {@link #cancel()}: Define what happens when the Cancel button is clicked.</li>
 *   <li>(Optional) Override {@link #setKeyboardFocus()}: Set focus to a specific widget when the
 *     screen is shown.</li>
 * </ol>
 *
 * @see Table
 * @see Actor
 * @see Stage
 */
public abstract class GenericScreen extends Table implements IScreen {
    protected static final float pad34 = 34f;
    protected static final float pad20 = 20f;
    protected static final float pad18 = 18f;
    protected static final float pad10 = 10f;

    /**
     * Stores the last active tab for each subclass.
     */
    private static final Map<Class<? extends GenericScreen>, Integer> lastActiveTab = new HashMap<>(20);

    final protected Stage stage;
    final protected Skin skin;
    public String title;
    private final Table titleTable;
    public TextButton acceptButton, cancelButton;
    protected GenericScreen me;
    protected Table content, bottom;
    protected boolean defaultMouseKbdListener = true;
    protected boolean defaultGamepadListener = true;
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

    public GenericScreen(String title, Skin skin, Stage stage) {
        super(skin);
        this.title = title;
        this.skin = skin;
        this.stage = stage;
        this.me = this;
        this.titleTable = new Table(skin);
        this.content = new Table(skin);
        this.bottom = new Table(skin);
        this.scrolls = new Array<>(false, 5);

        // Screen properties.
        setBackground("dark-grey");
        setFillParent(true);
        align(Align.center);
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

        // TITLE
        titleTable.add(new OwnLabel(title, skin, "header"));

        // Add to main table.
        add(titleTable).left().pad(pad18).row();
        add(content).center().pad(pad18).row();
        add(bottom).expandY().bottom().right().padRight(pad18).row();
        add(buttonGroup).pad(pad18).bottom().right();

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

        // Add default mouse/kdb listener
        if (defaultMouseKbdListener) {
            mouseKbdListener = new ScreenKbdListener(this);
        }
        // Add default gamepad listener.
        if (defaultGamepadListener) {
            gamepadListener = new ScreenGamepadListener(Settings.settings.controls.gamepad.mappingsFile, this);
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
    public GenericScreen show(Stage stage, Action action) {
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

        // Disable input
        EventManager.publish(Event.INPUT_ENABLED_CMD, this, false);

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
    public GenericScreen show(Stage stage) {
        show(stage, Actions.sequence(
                Actions.alpha(0f),
                Actions.fadeIn(0.6f, Interpolation.fade)));
        setKeyboardFocus();
        showDialogHook(stage);
        return this;
    }

    protected void showDialogHook(Stage stage) {
    }

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage at the specified position.
     */
    public GenericScreen show(Stage stage, float x, float y) {
        show(stage, sequence(Actions.alpha(0f), Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds(), Interpolation.fade)));
        setPosition(Math.round(x), FastMath.round(y));
        setKeyboardFocus();
        return this;
    }

    /**
     * Sets the keyboard focus, override in case you want to set the focus to a
     * specific item.
     **/
    public void setKeyboardFocus() {

    }

    public Table getTitleTable() {
        return titleTable;
    }

    /**
     * Hides the dialog with the given action and then removes it from the
     * stage.
     */
    public void hide(Action action) {
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

        // Enable input
        EventManager.publish(Event.INPUT_ENABLED_CMD, this, true);
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
                        if (abstractMouseKbdListener.isActive()) {
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
