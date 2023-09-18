/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.SnapshotArray;

public class ContextMenu extends Table {
    private static final Vector2 tmpVector = new Vector2();

    private final Skin skin;

    private final ContextMenuStyle style;
    private ContextMenuListener listener;

    private InputListener stageListener;
    private InputListener sharedMenuItemInputListener;

    private ChangeListener sharedMenuItemChangeListener;

    private InputListener defaultInputListener;

    /** The parent sub-menu, that this popup menu belongs to or null if this sub menu is root */
    private ContextMenu parentSubMenu;
    /** The current sub-menu, set by MenuItem */
    private ContextMenu activeSubMenu;
    private MenuItem activeItem;

    public ContextMenu(Skin skin, String styleName) {
        super();
        this.skin = skin;
        this.style = skin.get(styleName, ContextMenuStyle.class);
        setTouchable(Touchable.enabled);
        pad(0);
        setBackground(style.background);
        createListeners();
    }

    /**
     * Removes every instance of {@link ContextMenu} form {@link Stage} actors.
     * <p>
     * Generally called from {@link ApplicationListener#resize(int, int)} to remove menus on resize event.
     */
    public static void removeAll(Stage stage) {
        for (Actor actor : stage.getActors()) {
            if (actor instanceof ContextMenu) {
                ContextMenu menu = (ContextMenu) actor;
                menu.removeHierarchy();
            }
        }
    }

    private void createListeners() {
        stageListener = new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!getRootMenu().subMenuStructureContains(x, y)) {
                    remove();
                }
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                SnapshotArray<Actor> children = getChildren();

                if (children.size == 0 || activeSubMenu != null)
                    return false;

                if (keycode == Input.Keys.DOWN) {
                    selectNextItem();
                }

                if (activeItem == null)
                    return false;

                if (keycode == Input.Keys.UP) {
                    selectPreviousItem();
                }

                if (keycode == Input.Keys.LEFT && activeItem.containerMenu.parentSubMenu != null) {
                    activeItem.containerMenu.parentSubMenu.setActiveSubMenu(null);
                }

                if (keycode == Input.Keys.RIGHT && activeItem.getSubMenu() != null) {
                    activeItem.showSubMenu();
                    activeSubMenu.selectNextItem();
                }

                if (keycode == Input.Keys.ENTER) {
                    activeItem.fireChangeEvent();
                }

                return true;
            }
        };

        sharedMenuItemInputListener = new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1 && event.getListenerActor() instanceof MenuItem) {
                    MenuItem item = (MenuItem) event.getListenerActor();
                    if (item.isDisabled() == false) {
                        setActiveItem(item, false);
                    }
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 && event.getListenerActor() instanceof MenuItem) {
                    if (activeSubMenu != null)
                        return;

                    MenuItem item = (MenuItem) event.getListenerActor();
                    if (item == activeItem) {
                        setActiveItem(null, false);
                    }
                }
            }
        };

        sharedMenuItemChangeListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (event.isStopped() == false)
                    removeHierarchy();
            }
        };
    }

    private ContextMenu getRootMenu() {
        if (parentSubMenu != null)
            return parentSubMenu.getRootMenu();
        return this;
    }

    private boolean subMenuStructureContains(float x, float y) {
        if (contains(x, y))
            return true;
        if (activeSubMenu != null)
            return activeSubMenu.subMenuStructureContains(x, y);
        return false;
    }

    private void removeHierarchy() {
        if (activeItem != null && activeItem.containerMenu.parentSubMenu != null) {
            activeItem.containerMenu.parentSubMenu.removeHierarchy();
        }
        remove();
    }

    private void selectNextItem() {
        SnapshotArray<Actor> children = getChildren();
        if (children.size == 0)
            return;
        int startIndex = activeItem == null ? 0 : children.indexOf(activeItem, true) + 1;
        for (int i = startIndex; ; i++) {
            if (i >= children.size)
                i = 0;
            Actor actor = children.get(i);
            if (actor instanceof MenuItem && ((MenuItem) actor).isDisabled() == false) {
                setActiveItem((MenuItem) actor, true);
                break;
            }
        }
    }

    private void selectPreviousItem() {
        SnapshotArray<Actor> children = getChildren();
        if (children.size == 0)
            return;
        int startIndex = children.indexOf(activeItem, true) - 1;
        for (int i = startIndex; ; i--) {
            if (i == -1)
                i = children.size - 1;
            Actor actor = children.get(i);
            if (actor instanceof MenuItem && ((MenuItem) actor).isDisabled() == false) {
                setActiveItem((MenuItem) actor, true);
                break;
            }
        }
    }

    @Override
    public <T extends Actor> Cell<T> add(T actor) {
        if (actor instanceof MenuItem) {
            throw new IllegalArgumentException("MenuItems can be only added to ContextMenu by using addItem(MenuItem) method");
        }

        return super.add(actor);
    }

    public void addItem(MenuItem item) {
        super.add(item).fillX().expandX().row();
        pack();
        item.addListener(sharedMenuItemChangeListener);
        item.addListener(sharedMenuItemInputListener);
    }

    public void addSeparator() {
        add(new Separator(skin, "gray")).padTop(2f).padBottom(2f).fillX().expandX().row();
    }

    /**
     * Returns input listener that can be added to scene2d actor. When right mouse button is pressed on that actor,
     * menu will be displayed
     */
    public InputListener getDefaultInputListener() {
        return getDefaultInputListener(Buttons.RIGHT);
    }

    /**
     * Returns input listener that can be added to scene2d actor. When mouse button is pressed on that actor,
     * menu will be displayed
     *
     * @param mouseButton from {@link Buttons}
     */
    public InputListener getDefaultInputListener(final int mouseButton) {
        if (defaultInputListener == null) {
            defaultInputListener = new InputListener() {
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    return true;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                    if (event.getButton() == mouseButton)
                        showMenu(event.getStage(), event.getStageX(), event.getStageY());
                }
            };
        }

        return defaultInputListener;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (style.border != null)
            style.border.draw(batch, getX(), getY(), getWidth(), getHeight());
    }

    /**
     * Shows menu as given stage coordinates
     *
     * @param stage stage instance that this menu is being added to
     * @param x     stage x position
     * @param y     stage y position
     */
    public void showMenu(Stage stage, float x, float y) {
        setPosition(x, y - getHeight());
        if (stage.getHeight() - getY() > stage.getHeight())
            setY(getY() + getHeight());
        ActorUtils.keepWithinStage(stage, this);
        stage.addActor(this);
    }

    /**
     * Shows menu below (or above if not enough space) given actor.
     *
     * @param stage stage instance that this menu is being added to
     * @param actor used to get calculate menu position in stage, menu will be displayed above or below it
     */
    public void showMenu(Stage stage, Actor actor) {
        Vector2 pos = actor.localToStageCoordinates(tmpVector.setZero());
        float menuY;
        if (pos.y - getHeight() <= 0) {
            menuY = pos.y + actor.getHeight() + getHeight() - 5;
        } else {
            menuY = pos.y + 5;
        }
        showMenu(stage, pos.x, menuY);
    }

    public boolean contains(float x, float y) {
        return getX() < x && getX() + getWidth() > x && getY() < y && getY() + getHeight() > y;
    }

    /** Called by framework, when ContextMenu is added to MenuItem as submenu */
    void setActiveSubMenu(ContextMenu newSubMenu) {
        if (activeSubMenu == newSubMenu)
            return;
        if (activeSubMenu != null)
            activeSubMenu.remove();
        activeSubMenu = newSubMenu;
        if (newSubMenu != null) {
            newSubMenu.setParentMenu(this);
        }
    }

    @Override
    protected void setStage(Stage stage) {
        super.setStage(stage);
        if (stage != null)
            stage.addListener(stageListener);
    }

    @Override
    public boolean remove() {
        if (getStage() != null)
            getStage().removeListener(stageListener);
        if (activeSubMenu != null)
            activeSubMenu.remove();
        setActiveItem(null, false);
        parentSubMenu = null;
        activeSubMenu = null;
        return super.remove();
    }

    void setActiveItem(MenuItem newItem, boolean keyboardChange) {
        activeItem = newItem;
        if (listener != null)
            listener.activeItemChanged(newItem, keyboardChange);
    }

    public MenuItem getActiveItem() {
        return activeItem;
    }

    void setParentMenu(ContextMenu parentSubMenu) {
        this.parentSubMenu = parentSubMenu;
    }

    public ContextMenuListener getListener() {
        return listener;
    }

    public void setListener(ContextMenuListener listener) {
        this.listener = listener;
    }

    /**
     * Listener used to get events from {@link ContextMenu}.
     *
     * @since 1.0.2
     */
    public interface ContextMenuListener {
        /**
         * Called when active menu item (the highlighted one) has changed. This can't be used to listen when
         * {@link MenuItem} was pressed, add {@link ChangeListener} to {@link MenuItem} directly to achieve this.
         *
         * @param newActiveItem     new item that is now active. May be null.
         * @param changedByKeyboard whether the change occurred by keyboard (arrows keys) or by mouse.
         */
        void activeItemChanged(MenuItem newActiveItem, boolean changedByKeyboard);
    }

    static public class ContextMenuStyle {
        public Drawable background;
        public Drawable border;

        public ContextMenuStyle() {
        }

        public ContextMenuStyle(Drawable background, Drawable border) {
            this.background = background;
            this.border = border;
        }

        public ContextMenuStyle(ContextMenuStyle style) {
            this.background = style.background;
            this.border = style.border;
        }
    }
}
