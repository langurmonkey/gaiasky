/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.utils.Align;
import net.jafama.FastMath;

public class OwnWindow extends Table {
    static private final Vector2 tmpPosition = new Vector2();
    static private final Vector2 tmpSize = new Vector2();
    static private final int MOVE = 1 << 5;
    protected int edge;
    protected boolean dragging;
    boolean isMovable = true, isModal, isResizable;
    int resizeBorder = 8;
    boolean keepWithinStage = true;
    Label titleLabel;
    Table titleTable;
    boolean drawTitleTable;
    private WindowStyle style;

    public OwnWindow(String title, Skin skin) {
        this(title, skin.get(WindowStyle.class));
        setSkin(skin);
    }

    public OwnWindow(String title, Skin skin, String styleName) {
        this(title, skin.get(styleName, WindowStyle.class));
        setSkin(skin);
    }

    public OwnWindow(String title, WindowStyle style) {
        if (title == null)
            throw new IllegalArgumentException("title cannot be null.");
        setTouchable(Touchable.enabled);
        setClip(true);

        titleLabel = new Label(title, new LabelStyle(style.titleFont, style.titleFontColor));
        titleLabel.setEllipsis(true);

        titleTable = new Table() {
            public void draw(Batch batch, float parentAlpha) {
                if (drawTitleTable)
                    super.draw(batch, parentAlpha);
            }
        };
        titleTable.add(titleLabel).expandX().fillX().minWidth(0);
        addActor(titleTable);

        setStyle(style);
        setWidth(150);
        setHeight(150);

        addCaptureListener(new InputListener() {
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                toFront();
                return false;
            }
        });
        addListener(new InputListener() {
            float startX, startY, lastX, lastY;

            private void updateEdge(float x, float y) {
                float border = resizeBorder / 2f;
                float width = getWidth(), height = getHeight();
                float padTop = getPadTop(), padRight = getPadRight();
                float right = width - padRight;
                edge = 0;
                if (isResizable && x >= getPadLeft() - border && x <= right + border && y >= getPadBottom() - border) {
                    if (x < getPadLeft() + border)
                        edge |= Align.left;
                    if (x > right - border)
                        edge |= Align.right;
                    if (y < getPadBottom() + border)
                        edge |= Align.bottom;
                    if (edge != 0)
                        border += 25;
                    if (x < getPadLeft() + border)
                        edge |= Align.left;
                    if (x > right - border)
                        edge |= Align.right;
                    if (y < getPadBottom() + border)
                        edge |= Align.bottom;
                }
                if (isMovable && edge == 0 && y <= height && y >= height - padTop && x >= getPadLeft() && x <= right)
                    edge = MOVE;
            }

            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == 0) {
                    updateEdge(x, y);
                    dragging = edge != 0;
                    startX = x;
                    startY = y;
                    lastX = x - getWidth();
                    lastY = y - getHeight();
                }
                return edge != 0 || isModal;
            }

            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                dragging = false;
            }

            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (!dragging)
                    return;
                float width = getWidth(), height = getHeight();
                float windowX = getX(), windowY = getY();

                float minWidth = getMinWidth();
                float minHeight = getMinHeight();
                Stage stage = getStage();
                boolean clampPosition = keepWithinStage && stage != null && getParent() == stage.getRoot();

                if ((edge & MOVE) != 0) {
                    float amountX = x - startX, amountY = y - startY;
                    windowX += amountX;
                    windowY += amountY;
                }
                if ((edge & Align.left) != 0) {
                    float amountX = x - startX;
                    if (width - amountX < minWidth)
                        amountX = -(minWidth - width);
                    if (clampPosition && windowX + amountX < 0)
                        amountX = -windowX;
                    width -= amountX;
                    windowX += amountX;
                }
                if ((edge & Align.bottom) != 0) {
                    float amountY = y - startY;
                    if (height - amountY < minHeight)
                        amountY = -(minHeight - height);
                    if (clampPosition && windowY + amountY < 0)
                        amountY = -windowY;
                    height -= amountY;
                    windowY += amountY;
                }
                if ((edge & Align.right) != 0) {
                    float amountX = x - lastX - width;
                    if (width + amountX < minWidth)
                        amountX = minWidth - width;
                    if (clampPosition && windowX + width + amountX > stage.getWidth())
                        amountX = stage.getWidth() - windowX - width;
                    width += amountX;
                }
                if ((edge & Align.top) != 0) {
                    float amountY = y - lastY - height;
                    if (height + amountY < minHeight)
                        amountY = minHeight - height;
                    if (clampPosition && windowY + height + amountY > stage.getHeight())
                        amountY = stage.getHeight() - windowY - height;
                    height += amountY;
                }
                setBounds(windowX, windowY, width, height);
            }

            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateEdge(x, y);
                return isModal;
            }

            public boolean keyDown(InputEvent event, int keycode) {
                return isModal;
            }

            public boolean keyUp(InputEvent event, int keycode) {
                return isModal;
            }

            public boolean keyTyped(InputEvent event, char character) {
                return isModal;
            }
        });
    }

    /**
     * Returns the window's style. Modifying the returned style may not have an effect until
     * {@link #setStyle(WindowStyle)} is
     * called.
     */
    public WindowStyle getStyle() {
        return style;
    }

    public void setStyle(WindowStyle style) {
        if (style == null)
            throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        setBackground(style.background);
        titleLabel.setStyle(new LabelStyle(style.titleFont, style.titleFontColor));
        invalidateHierarchy();
    }

    public void keepWithinStage() {
        if (!keepWithinStage)
            return;
        Stage stage = getStage();
        if (stage == null)
            return;
        Camera camera = stage.getCamera();
        if (camera instanceof OrthographicCamera orthographicCamera) {
            float parentWidth = stage.getWidth();
            float parentHeight = stage.getHeight();
            if (getX(Align.right) - camera.position.x > parentWidth / 2 / orthographicCamera.zoom)
                setPosition(camera.position.x + parentWidth / 2 / orthographicCamera.zoom, getY(Align.right), Align.right);
            if (getX(Align.left) - camera.position.x < -parentWidth / 2 / orthographicCamera.zoom)
                setPosition(camera.position.x - parentWidth / 2 / orthographicCamera.zoom, getY(Align.left), Align.left);
            if (getY(Align.top) - camera.position.y > parentHeight / 2 / orthographicCamera.zoom)
                setPosition(getX(Align.top), camera.position.y + parentHeight / 2 / orthographicCamera.zoom, Align.top);
            if (getY(Align.bottom) - camera.position.y < -parentHeight / 2 / orthographicCamera.zoom)
                setPosition(getX(Align.bottom), camera.position.y - parentHeight / 2 / orthographicCamera.zoom, Align.bottom);
        } else if (getParent() == stage.getRoot()) {
            float parentWidth = stage.getWidth();
            float parentHeight = stage.getHeight();
            if (getX() < 0)
                setX(0);
            if (getRight() > parentWidth)
                setX(parentWidth - getWidth());
            if (getY() < 0)
                setY(0);
            if (getTop() > parentHeight)
                setY(parentHeight - getHeight());
        }
    }

    public void draw(Batch batch, float parentAlpha) {
        Stage stage = getStage();
        if (stage != null && stage.getKeyboardFocus() == null)
            stage.setKeyboardFocus(this);

        keepWithinStage();

        if (style.stageBackground != null) {
            stageToLocalCoordinates(tmpPosition.set(0, 0));
            if (stage != null) {
                stageToLocalCoordinates(tmpSize.set(stage.getWidth(), stage.getHeight()));
            }
            drawStageBackground(batch, parentAlpha, getX() + tmpPosition.x, getY() + tmpPosition.y, getX() + tmpSize.x,
                    getY() + tmpSize.y);
        }

        super.draw(batch, parentAlpha);
    }

    protected void drawStageBackground(Batch batch, float parentAlpha, float x, float y, float width, float height) {
        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
        style.stageBackground.draw(batch, x, y, width, height);
    }

    protected void drawBackground(Batch batch, float parentAlpha, float x, float y) {
        super.drawBackground(batch, parentAlpha, x, y);

        // Manually draw the title table before clipping is done.
        titleTable.getColor().a = getColor().a;
        float padTop = getPadTop(), padLeft = getPadLeft();
        titleTable.setSize(getWidth() - padLeft - getPadRight(), padTop);
        titleTable.setPosition(padLeft, getHeight() - padTop);
        drawTitleTable = true;
        titleTable.draw(batch, parentAlpha);
        drawTitleTable = false; // Avoid drawing the title table again in drawChildren.
    }

    public Actor hit(float x, float y, boolean touchable) {
        if (!isVisible())
            return null;
        Actor hit = super.hit(x, y, touchable);
        if (hit == null && isModal && (!touchable || getTouchable() == Touchable.enabled))
            return this;
        float height = getHeight();
        if (hit == null || hit == this)
            return hit;
        if (y <= height && y >= height - getPadTop() && x >= 0 && x <= getWidth()) {
            // Hit the title bar, don't use the hit child if it is in the Window's table.
            Actor current = hit;
            while (current.getParent() != this)
                current = current.getParent();
            if (getCell(current) != null)
                return this;
        }
        return hit;
    }

    public void setMovable(boolean isMovable) {
        this.isMovable = isMovable;
    }

    public boolean isModal() {
        return isModal;
    }

    public void setModal(boolean isModal) {
        this.isModal = isModal;
    }

    public boolean isResizable() {
        return isResizable;
    }

    public void setResizable(boolean isResizable) {
        this.isResizable = isResizable;
    }

    public float getPrefWidth() {
        return FastMath.max(super.getPrefWidth(), titleTable.getPrefWidth() + getPadLeft() + getPadRight());
    }

    public Table getTitleTable() {
        return titleTable;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    protected HorizontalGroup hg(Actor... actors) {
        HorizontalGroup hg = new HorizontalGroup();
        for (Actor a : actors)
            hg.addActor(a);
        return hg;
    }


}
