/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;

public class CollapsiblePane extends Table {

    /** Collapse speed in pixels per second **/
    protected float collapseSpeed;
    private CollapsibleWindow dialogWindow;
    ImageButton expandIcon, detachIcon;
    float lastX = -1, lastY = -1;
    Actor content;
    String labelText;
    Skin skin;
    Stage stage;
    float space;
    Cell<?> contentCell;
    float targetHeight;
    boolean expanding = false;
    boolean collapsing = false;
    Runnable expandCollapseRunnable = null;

    /**
     * Creates a collapsible pane.
     *
     * @param stage             The main stage.
     * @param labelText         The text of the label.
     * @param content           The content actor.
     * @param width             The preferred width of this pane.
     * @param skin              The skin to use.
     * @param labelStyle        The style of the label.
     * @param expandButtonStyle The style of the expand icon.
     * @param detachButtonStyle The style of the detach icon.
     * @param expanded          Whether the pane is expanded or collapsed to begin with.
     * @param shortcut          The shortcut to expand/collapse. Shown in a tooltip.
     * @param topIcons          List of top icons that will be added between the label and the
     *                          expand/detach icons.
     */
    public CollapsiblePane(final Stage stage, final String labelText, final Actor content, float width, final Skin skin, String labelStyle, String expandButtonStyle, String detachButtonStyle, boolean expanded, String shortcut, Actor... topIcons) {
        this(stage, labelText, content, width, skin, labelStyle, expandButtonStyle, detachButtonStyle, expanded, null, shortcut, topIcons);
    }

    /**
     * Creates a collapsible pane.
     *
     * @param stage                  The main stage.
     * @param labelText              The text of the label.
     * @param content                The content actor.
     * @param width                  The preferred width of this pane.
     * @param skin                   The skin to use.
     * @param labelStyle             The style of the label.
     * @param expandButtonStyle      The style of the expand icon.
     * @param detachButtonStyle      The style of the detach icon.
     * @param expanded               Whether the pane is expanded or collapsed to begin with.
     * @param expandCollapseRunnable Runs when the pane is expanded or collapsed.
     * @param shortcut               The shortcut to expand/collapse. Shown in a tooltip.
     * @param topIcons               List of top icons that will be added between the label and the
     *                               expand/detach icons.
     */
    public CollapsiblePane(final Stage stage, final String labelText, final Actor content, float width, final Skin skin, String labelStyle, String expandButtonStyle, String detachButtonStyle, boolean expanded, Runnable expandCollapseRunnable, String shortcut, Actor... topIcons) {
        super(skin);
        this.stage = stage;
        this.labelText = labelText;
        this.content = content;
        this.skin = skin;
        this.space = 6.4f;
        this.collapseSpeed = 100;
        this.expandCollapseRunnable = expandCollapseRunnable;

        OwnLabel mainLabel = new OwnLabel(labelText, skin, labelStyle);
        float lw = mainLabel.getWidth();
        LabelStyle ls = skin.get(labelStyle, LabelStyle.class);

        float mWidth = width * 0.8f;
        if (lw > mWidth) {
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
            for (int chars = labelText.length() - 1; chars > 0; chars--) {
                layout.setText(ls.font, TextUtils.capString(labelText, chars));
                if (layout.width <= mWidth) {
                    mainLabel.setText(TextUtils.capString(labelText, chars));
                    break;
                }
            }
        }
        mainLabel.addListener(new ClickListener() {
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (event.getButton() == Buttons.LEFT)
                    toggleExpandCollapse(mainLabel);

                // Bubble up
                super.touchUp(event, x, y, pointer, button);
            }
        });
        mainLabel.addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                if (type == Type.enter) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });

        // Expand icon
        expandIcon = new OwnImageButton(skin, expandButtonStyle);
        expandIcon.setName("expand-collapse");
        expandIcon.setChecked(expanded);
        expandIcon.addListener(event -> {
            if (event instanceof ChangeEvent) {
                toggleExpandCollapse(expandIcon);
                return true;
            }
            return false;
        });
        if (shortcut == null)
            expandIcon.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.expandcollapse.group"), skin));
        else
            expandIcon.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.tooltip.expandcollapse.group"), shortcut, skin));

        if (detachButtonStyle != null) {
            // Detach icon
            detachIcon = new OwnImageButton(skin, detachButtonStyle);
            detachIcon.setName("detach-panel");
            detachIcon.setChecked(false);
            detachIcon.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    detach();
                    return true;
                }
                return false;
            });
            detachIcon.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.detach.group"), skin));
        }

        // Question icon
        OwnLabel questionLabel = new OwnLabel("(?)", skin, "question");
        if (shortcut != null && !shortcut.isEmpty())
            questionLabel.addListener(new OwnTextHotkeyTooltip(labelText, shortcut, skin));

        Table headerTable = new Table(skin);

        HorizontalGroup titleGroup = new HorizontalGroup();
        titleGroup.space(6.4f);
        titleGroup.addActor(expandIcon);
        titleGroup.addActor(mainLabel);
        if (shortcut != null && !shortcut.isEmpty())
            titleGroup.addActor(questionLabel);

        HorizontalGroup headerGroupLeft = new HorizontalGroup();
        headerGroupLeft.space(space).align(Align.left);

        if (topIcons != null) {
            for (Actor topIcon : topIcons) {
                if (topIcon != null)
                    headerGroupLeft.addActor(topIcon);
            }
        }

        HorizontalGroup headerGroupRight = new HorizontalGroup();
        headerGroupRight.space(space).align(Align.right);
        if (detachIcon != null) {
            headerGroupRight.addActor(detachIcon);
        }

        headerTable.add(titleGroup).left().padRight(6.4f);

        if (detachIcon != null) {
            headerTable.add(headerGroupLeft).right().pad(6.4f);
            headerTable.add().expandX();
            headerTable.add(headerGroupRight).right();
        } else {
            headerTable.add(headerGroupLeft).right().pad(6.4f).expandX();
        }

        add(headerTable).padBottom(this.space).width(width).row();
        contentCell = add().prefHeight(0).width(width);

        if (expanded)
            contentCell.setActor(content);

        layout();
        targetHeight = getHeight();

    }

    /**
     * Creates a collapsible pane.
     *
     * @param stage    The main stage.
     * @param title    The title.
     * @param content  The content actor.
     * @param width    The preferred width of this pane.
     * @param skin     The skin to use.
     * @param shortcut The keyboard shortcut to use.
     * @param topIcons List of top icons that will be added between the label and the
     *                 expand/detach icons.
     */
    public CollapsiblePane(Stage stage, String title, final Actor content, float width, Skin skin, boolean expanded, String shortcut, Actor... topIcons) {
        this(stage, title, content, width, skin, "header", "expand-collapse", "detach", expanded, shortcut, topIcons);
    }

    public boolean expandPane() {
        if (!expandIcon.isChecked()) {
            expandIcon.setChecked(true);
            expanding = true;
            collapsing = false;
            return true;
        }
        return false;
    }

    public boolean collapsePane() {
        if (expandIcon.isChecked()) {
            expandIcon.setChecked(false);
            expanding = false;
            collapsing = true;
            return true;
        }
        return false;
    }

    public void togglePane() {
        if (!expandPane())
            collapsePane();
    }

    private void toggleExpandCollapse(Actor source) {
        boolean expand;
        if (source != expandIcon) {
            expand = !expandIcon.isChecked();
            expandIcon.setProgrammaticChangeEvents(false);
            expandIcon.setChecked(expand);
            expandIcon.setProgrammaticChangeEvents(true);
        } else {
            expand = expandIcon.isChecked();
        }

        if (expand && dialogWindow == null) {
            // Expand
            contentCell.setActor(content);
            expanding = true;
            collapsing = false;
        } else {
            // Collapse
            contentCell.clearActor();
            expanding = false;
            collapsing = true;
        }
        if (expandCollapseRunnable != null) {
            expandCollapseRunnable.run();
        }
        pack();
        EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this);
    }

    public void detach() {
        dialogWindow = createWindow(labelText, content, skin, stage, lastX, lastY);

        // Display
        if (!stage.getActors().contains(dialogWindow, true))
            stage.addActor(dialogWindow);

        expandIcon.setChecked(false);
        expandIcon.setDisabled(true);
        detachIcon.setDisabled(true);
    }

    private CollapsibleWindow createWindow(String labelText, final Actor content, Skin skin, Stage stage, float x, float y) {
        final CollapsibleWindow window = new CollapsibleWindow(labelText, skin);
        window.align(Align.center);

        OwnScrollPane contentScroll = new OwnScrollPane(content, skin, "minimalist-nobg");
        contentScroll.setName("collapsible pane scroll");
        contentScroll.setSize(content.getWidth(), content.getHeight() * 1.1f);

        window.add(contentScroll).pad(8f).row();

        // Close button
        OwnTextButton close = new OwnTextButton(I18n.msg("gui.close"), skin, "default");
        close.setName("close");
        close.addListener(event -> {
            if (event instanceof ChangeEvent) {
                lastX = window.getX();
                lastY = window.getY();
                window.remove();
                dialogWindow = null;
                expandIcon.setDisabled(false);
                detachIcon.setDisabled(false);
                return true;
            }

            return false;
        });
        Container<Button> closeContainer = new Container<>(close);
        close.setWidth(185f);
        closeContainer.align(Align.right);

        window.add(closeContainer).pad(8f).bottom().right();
        window.getTitleTable().align(Align.left);
        window.align(Align.left);
        window.pack();

        x = x < 0 ? stage.getWidth() / 2f - window.getWidth() / 2f : x;
        y = y < 0 ? stage.getHeight() / 2f - window.getHeight() / 2f : y;
        window.setPosition(Math.round(x), Math.round(y));
        window.pack();

        return window;
    }

    public Button getExpandCollapseActor() {
        return expandIcon;
    }

}
