/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import gaiasky.util.i18n.I18n;

public class CollapsibleWindow extends OwnWindow {
    protected Actor me;
    protected Skin skin;
    /**
     * Collapse speed in pixels per second.
     **/
    protected float collapseSpeed;
    String expandIcon = "window-expand";
    String collapseIcon = "window-collapse";
    private boolean collapsed, collapsing = false, expanding = false;
    private float collapseHeight;
    private float expandHeight;
    private Vector2 vec2;
    private float maxWidth = -1f, maxHeight = -1f;
    /** Whether the user can collapse the window by clicking on the menu bar. **/
    private boolean collapsible = true;

    public CollapsibleWindow(String title, Skin skin) {
        this(title, skin, 2000);
    }

    public CollapsibleWindow(String title, Skin skin, String styleName) {
        this(title, skin, styleName, 2000);
    }

    public CollapsibleWindow(String title, Skin skin, float collapseSpeed) {
        super(title, skin);
        initWindow(skin, collapseSpeed);
    }

    public CollapsibleWindow(String title, Skin skin, String styleName, float collapseSpeed) {
        super(title, skin, styleName);
        initWindow(skin, collapseSpeed);
    }

    private void initWindow(final Skin skin, final float collapseSpeed) {
        this.me = this;
        this.skin = skin;
        this.collapseSpeed = collapseSpeed;
        this.collapseHeight = 32f;

        vec2 = new Vector2();
        addListener(new ClickListener() {
            private float startx, starty;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                startx = x + getX();
                starty = y + getY();
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                float endx = x + getX();
                float endy = y + getY();
                vec2.set(endx - startx, endy - starty);
                // pixels of margin
                if (vec2.len() < 3f) {
                    if (getHeight() - y <= getPadTop() && y < getHeight() && x > 0f && x < getWidth())
                        if (button == Input.Buttons.LEFT) {
                            // Left mouse button on title toggles collapse
                            toggleCollapsed();
                        }
                }
                super.touchUp(event, x, y, pointer, button);
            }
        });

        // Pad title cell
        getTitleTable().getCells().get(0).padLeft(8f);
        // Mouse pointer on title
        getTitleTable().addListener(event -> {
            if (event instanceof InputEvent) {
                Type type = ((InputEvent) event).getType();
                // Click
                if (collapsible && type == Type.mouseMoved) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Hand);
                } else if (type == Type.exit) {
                    Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                }
                return true;
            }
            return false;
        });

        addListener(event -> {
            if (isResizable()) {
                if (event instanceof InputEvent) {
                    Type type = ((InputEvent) event).getType();
                    if (type == Type.mouseMoved) {
                        if ((edge & Align.bottom) != 0 && maxHeight == -1f) {
                            Gdx.graphics.setSystemCursor(SystemCursor.VerticalResize);
                        } else if ((edge & Align.right) != 0 && maxWidth == -1f) {
                            Gdx.graphics.setSystemCursor(SystemCursor.HorizontalResize);
                        }
                    } else if (type == Type.exit) {
                        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
                    }

                }
            }
            return false;
        });
        if (collapsible) {
            getTitleTable().addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.expandcollapse"), skin));
        }
    }

    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        float width = getWidth(), height = getHeight();
        float padTop = getPadTop();

        super.drawBackground(batch, parentAlpha, x, y);

        Drawable icon = collapsed ? skin.getDrawable(expandIcon) : skin.getDrawable(collapseIcon);
        float iw = icon.getMinWidth();
        float ih = icon.getMinHeight();

        x += width - iw - getPadRight();
        y += height - getPadTop() / 2f;
        y -= (padTop - ih) / 2f;

        icon.draw(batch, x, y, iw, ih);

    }

    public void act(float delta) {
        super.act(delta);

        if (collapsing) {
            float pixels = collapseSpeed * delta;
            // COLLAPSING
            if (getHeight() > collapseHeight) {
                float currHeight = getHeight();
                float newHeight = Math.max(collapseHeight, currHeight - pixels);
                setHeight(newHeight);
                setY(getY() + (currHeight - newHeight));
            } else {
                if (getStage() != null)
                    getStage().setScrollFocus(null);
                collapsing = false;
                collapsed = true;
            }
        } else if (expanding) {
            float pixels = collapseSpeed * delta;
            // EXPANDING
            if (getHeight() < expandHeight) {
                float currHeight = getHeight();
                float newHeight = Math.min(expandHeight, currHeight + pixels);
                setHeight(newHeight);
                setY(getY() + (currHeight - newHeight));
            } else {
                expanding = false;
                collapsed = false;
            }
        }
    }

    public void expand() {
        if (!collapsed || expanding || collapsing)
            return;
        else {
            expanding = true;
        }

    }

    public void expandInstant() {
        if (!collapsed)
            return;
        setHeight(expandHeight);
        setY(getY() - expandHeight + collapseHeight);
        collapsed = false;
    }

    public void collapse() {
        if (collapsible) {
            if (collapsed || expanding || collapsing)
                return;
            else {
                expandHeight = getHeight();
                collapsing = true;
            }
        }
    }

    public void collapseInstant() {
        if (collapsible) {
            if (collapsed)
                return;
            expandHeight = getHeight();
            setHeight(collapseHeight);
            setY(getY() + expandHeight - collapseHeight);
            collapsed = true;
            if (getStage() != null)
                getStage().setScrollFocus(null);
        }
    }

    public void toggleCollapsed() {
        if (collapsible) {
            if (collapsed)
                expand();
            else
                collapse();
        }
    }

    public boolean isCollapsible() {
        return collapsible;
    }

    public void setCollapsible(boolean collapsible) {
        this.collapsible = collapsible;
        if (collapsible) {
            getTitleTable().addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.expandcollapse"), skin));
        } else {
            getTitleTable().clearListeners();
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    @Override
    public void pack() {
        collapsed = false;
        super.pack();
    }

    public void setResizable(boolean w, boolean h) {
        setResizable(w || h);
        if (w) {
            maxWidth = -1f;
        } else {
            pack();
            maxWidth = this.getWidth();
        }
        if (h) {
            maxHeight = -1f;
        } else {
            pack();
            maxHeight = this.getHeight();
        }
    }

    @Override
    public float getPrefWidth() {
        if (maxWidth < 0f)
            return super.getPrefWidth();
        else
            return maxWidth;
    }

    @Override
    public float getMaxWidth() {
        if (maxWidth < 0f)
            return super.getMaxWidth();
        else
            return maxWidth;
    }

    @Override
    public float getWidth() {
        if (maxWidth < 0f)
            return super.getWidth();
        else
            return maxWidth;
    }

    @Override
    public float getPrefHeight() {
        if (maxHeight < 0f)
            return super.getPrefHeight();
        else
            return maxHeight;
    }

    @Override
    public float getMaxHeight() {
        if (maxHeight < 0f)
            return super.getMaxHeight();
        else
            return maxHeight;
    }

    @Override
    public void setModal(boolean isModal) {
        super.setModal(isModal);
        // Only non-modal windows can be collapsed.
        setCollapsible(!isModal);
    }

    @Override
    public float getHeight() {
        if (maxHeight < 0f)
            return super.getHeight();
        else
            return maxHeight;
    }
}