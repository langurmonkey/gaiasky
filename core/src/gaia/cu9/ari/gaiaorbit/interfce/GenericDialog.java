package gaia.cu9.ari.gaiaorbit.interfce;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.CollapsibleWindow;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;

public abstract class GenericDialog extends CollapsibleWindow {

    final protected Stage stage;
    final protected Skin skin;
    protected GenericDialog me;
    protected Table content;
    protected float pad;
    private String acceptText = null, cancelText = null;
    protected TextButton acceptButton, cancelButton;

    private Runnable acceptRunnable, cancelRunnable;

    private Actor previousKeyboardFocus, previousScrollFocus;
    private FocusListener focusListener;

    protected InputListener ignoreTouchDown = new InputListener() {
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.cancel();
            return false;
        }
    };

    protected Array<OwnScrollPane> scrolls;

    public GenericDialog(String title, Skin skin, Stage stage) {
        super(title, skin);
        this.skin = skin;
        this.stage = stage;
        this.me = this;
        this.content = new Table(skin);
        this.scrolls = new Array<OwnScrollPane>(5);
    }

    protected void setAcceptText(String acceptText) {
        this.acceptText = acceptText;
    }

    protected void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    protected void recalculateButtonSize() {
        float w = Math.max(Math.max(acceptButton != null ? acceptButton.getWidth() : 0, cancelButton != null ? cancelButton.getWidth() : 0) + 10 * GlobalConf.SCALE_FACTOR, 80 * GlobalConf.SCALE_FACTOR);
        if (acceptButton != null)
            acceptButton.setWidth(w);
        if (cancelButton != null)
            cancelButton.setWidth(w);
    }

    public void buildSuper() {
        pad = 5 * GlobalConf.SCALE_FACTOR;

        build();

        /** BUTTONS **/
        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.pad(pad);
        buttonGroup.space(pad);

        if (acceptText != null) {
            acceptButton = new OwnTextButton(acceptText, skin, "default");
            acceptButton.setName("accept");
            acceptButton.addListener(new EventListener() {

                @Override
                public boolean handle(Event event) {
                    if (event instanceof ChangeEvent) {
                        accept();
                        if (acceptRunnable != null)
                            acceptRunnable.run();
                        me.hide();
                        return true;
                    }
                    return false;
                }

            });
            buttonGroup.addActor(acceptButton);
        }
        if (cancelText != null) {
            cancelButton = new OwnTextButton(cancelText, skin, "default");
            cancelButton.setName("cancel");
            cancelButton.addListener(new EventListener() {
                @Override
                public boolean handle(Event event) {
                    if (event instanceof ChangeEvent) {
                        cancel();
                        if (cancelRunnable != null)
                            cancelRunnable.run();
                        me.hide();
                        return true;
                    }

                    return false;
                }

            });
            buttonGroup.addActor(cancelButton);
        }
        recalculateButtonSize();

        add(content).pad(pad).row();
        add(buttonGroup).pad(pad).bottom().right();
        getTitleTable().align(Align.left);

        pack();

        // Add keys for ESC and ENTER
        me.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent) {
                    InputEvent ievent = (InputEvent) event;
                    if (ievent.getType() == Type.keyUp) {
                        int key = ievent.getKeyCode();
                        switch (key) {
                        case Keys.ESCAPE:
                            // Exit
                            cancel();
                            if (cancelRunnable != null)
                                cancelRunnable.run();
                            me.hide();
                            return true;
                        case Keys.ENTER:
                            // Exit
                            accept();
                            if (acceptRunnable != null)
                                acceptRunnable.run();
                            me.hide();
                            return true;
                        default:
                            // Nothing
                            break;
                        }
                    }
                }
                return false;
            }
        });

        focusListener = new FocusListener() {
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused)
                    focusChanged(event);
            }

            public void scrollFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused)
                    focusChanged(event);
            }

            private void focusChanged(FocusEvent event) {
                Stage stage = getStage();
                if (isModal() && stage != null && stage.getRoot().getChildren().size > 0 && stage.getRoot().getChildren().peek() == GenericDialog.this) { // Dialog is top most actor.
                    Actor newFocusedActor = event.getRelatedActor();
                    if (newFocusedActor != null && !newFocusedActor.isDescendantOf(GenericDialog.this) && !(newFocusedActor.equals(previousKeyboardFocus) || newFocusedActor.equals(previousScrollFocus)))
                        event.cancel();
                }
            }
        };

        /** CAPTURE SCROLL FOCUS **/
        stage.addListener(new EventListener() {

            @Override
            public boolean handle(Event event) {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;

                    if (ie.getType() == Type.mouseMoved) {
                        for (OwnScrollPane scroll : scrolls) {
                            if (ie.getTarget().isDescendantOf(scroll)) {
                                stage.setScrollFocus(scroll);
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // Set position
        setPosition(Math.round(stage.getWidth() / 2f - this.getWidth() / 2f), Math.round(stage.getHeight() / 2f - this.getHeight() / 2f));

        // Modal
        setModal(true);
    }

    /**
     * Build the content here
     */
    protected abstract void build();

    /**
     * The accept function, if any
     */
    protected abstract void accept();

    /**
     * The cancel function, if any
     */
    protected abstract void cancel();

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage with custom
     * action which can be null for instant show
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

        // Disable input
        EventManager.instance.post(Events.INPUT_ENABLED_CMD, false);

        return this;
    }

    /**
     * {@link #pack() Packs} the dialog and adds it to the stage, centered with
     * default fadeIn action
     */
    public GenericDialog show(Stage stage) {
        show(stage, sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Interpolation.fade)));
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2), Math.round((stage.getHeight() - getHeight()) / 2));
        setKeyboardFocus();
        return this;
    }

    /**
     * Sets the keyboard focus, override in case you want to set the focus to a
     * specific item
     **/
    public void setKeyboardFocus() {

    }

    /**
     * Hides the dialog with the given action and then removes it from the
     * stage.
     */
    public void hide(Action action) {
        Stage stage = getStage();
        if (stage != null) {
            removeListener(focusListener);
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

        // Enable input
        EventManager.instance.post(Events.INPUT_ENABLED_CMD, true);
    }

    /**
     * Sets the runnable which runs when accept is clicked
     * @param r The runnable
     */
    public void setAcceptRunnable(Runnable r) {
        this.acceptRunnable = r;
    }

    /**
     * Sets the runnable which runs when cancel is clicked
     * @param r The runnable
     */
    public void setCancelRunnable(Runnable r) {
        this.cancelRunnable = r;
    }

    /**
     * Hides the dialog. Called automatically when a button is clicked. The
     * default implementation fades out the dialog over 400 milliseconds and
     * then removes it from the stage.
     */
    public void hide() {
        hide(sequence(fadeOut(0.4f, Interpolation.fade), Actions.removeActor()));
        stage.setKeyboardFocus(previousKeyboardFocus);
    }

    /**
     * Sets the enabled property on the given components
     * 
     * @param enabled
     * @param components
     */
    protected void enableComponents(boolean enabled, Disableable... components) {
        for (Disableable c : components) {
            if (c != null)
                c.setDisabled(!enabled);
        }
    }

    protected static String txt(String key) {
        return I18n.bundle.get(key);
    }

    protected static String txt(String key, Object... args) {
        return I18n.bundle.format(key, args);
    }

}
