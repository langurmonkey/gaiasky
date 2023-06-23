/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.BookmarksManager.BookmarkNode;
import gaiasky.gui.ControlsWindow;
import gaiasky.gui.NewBookmarkFolderDialog;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BookmarksComponent extends GuiComponent implements IObserver {
    static private final Vector2 tmpCoords = new Vector2();
    private final Drawable folderIcon;
    private final Drawable bookmarkIcon;
    private final Set<ContextMenu> contextMenus;
    protected Scene scene;
    protected FocusView view;
    protected Tree<TreeNode, String> bookmarksTree;
    protected TextField searchBox;
    protected OwnScrollPane bookmarksScrollPane;
    protected Table infoTable;
    protected Cell<?> infoCell1, infoCell2;
    protected OwnLabel infoMessage1, infoMessage2;
    private boolean events = true;

    public BookmarksComponent(Skin skin, Stage stage) {
        super(skin, stage);
        folderIcon = skin.getDrawable("iconic-folder-small");
        bookmarkIcon = skin.getDrawable("iconic-bookmark-small");
        contextMenus = new HashSet<>();
        view = new FocusView();
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED, Event.BOOKMARKS_ADD, Event.BOOKMARKS_REMOVE, Event.BOOKMARKS_REMOVE_ALL);
    }

    @Override
    public void initialize() {
        float contentWidth = ControlsWindow.getContentWidth();
        searchBox = new OwnTextField("", skin);
        searchBox.setName("search box");
        searchBox.setWidth(contentWidth);
        searchBox.setMessageText(I18n.msg("gui.objects.search"));
        searchBox.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyUp && !searchBox.getText().isEmpty()) {
                    String text = searchBox.getText().toLowerCase().trim();
                    if (scene.index().containsEntity(text)) {
                        Entity node = scene.getEntity(text);
                        if (Mapper.focus.has(node)) {
                            view.setEntity(node);
                            IFocus focus = view;
                            boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                            boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                            if (!timeOverflow && ctOn) {
                                GaiaSky.postRunnable(() -> {
                                    EventManager.publish(Event.CAMERA_MODE_CMD, searchBox, CameraMode.FOCUS_MODE, true);
                                    EventManager.publish(Event.FOCUS_CHANGE_CMD, searchBox, focus, true);
                                });
                            } else if (timeOverflow) {
                                info(I18n.msg("gui.objects.search.timerange.1", text), I18n.msg("gui.objects.search.timerange.2"));
                            } else {
                                info(I18n.msg("gui.objects.search.invisible.1", text), I18n.msg("gui.objects.search.invisible.2", focus.getCt().toString()));
                            }
                        }
                    } else {
                        info(null, null);
                    }

                    if (ie.getKeyCode() == Keys.ESCAPE) {
                        // Lose focus
                        stage.setKeyboardFocus(null);
                    }
                } else if (ie.getType() == Type.keyDown) {
                    if (ie.getKeyCode() == Keys.CONTROL_LEFT || ie.getKeyCode() == Keys.CONTROL_RIGHT) {
                        // Lose focus
                        stage.setKeyboardFocus(null);
                    }
                }
                return true;
            }
            return false;
        });

        // Info message
        infoTable = new Table(skin);
        infoCell1 = infoTable.add();
        infoTable.row();
        infoCell2 = infoTable.add();

        infoMessage1 = new OwnLabel("", skin, "default-blue");
        infoMessage2 = new OwnLabel("", skin, "default-blue");

        /*
         * OBJECTS
         */
        bookmarksTree = new Tree<>(skin);
        bookmarksTree.setName("bookmarks tree");
        reloadBookmarksTree();
        bookmarksTree.addListener(event -> {
            if (events)
                if (event instanceof ChangeEvent) {
                    ChangeEvent ce = (ChangeEvent) event;
                    Actor actor = ce.getTarget();
                    TreeNode selected = (TreeNode) ((Tree) actor).getSelectedNode();
                    if (selected != null && !selected.hasChildren()) {
                        if (selected.node.position == null) {
                            // Object bookmark.
                            String name = selected.node.name;
                            if (scene.index().containsEntity(name)) {
                                Entity node = scene.getEntity(name);
                                if (Mapper.focus.has(node)) {
                                    view.setEntity(node);
                                    view.getFocus(name);
                                    IFocus focus = view;
                                    boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                                    boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                                    if (!timeOverflow && ctOn) {
                                        GaiaSky.postRunnable(() -> {
                                            EventManager.publish(Event.CAMERA_MODE_CMD, bookmarksTree, CameraMode.FOCUS_MODE, true);
                                            EventManager.publish(Event.FOCUS_CHANGE_CMD, bookmarksTree, focus, true);
                                        });
                                        info(null, null);
                                    } else if (timeOverflow) {
                                        info(I18n.msg("gui.objects.search.timerange.1", name), I18n.msg("gui.objects.search.timerange.2"));
                                    } else {
                                        info(I18n.msg("gui.objects.search.invisible.1", name), I18n.msg("gui.objects.search.invisible.2", focus.getCt().toString()));
                                    }
                                }
                            } else {
                                info(null, null);
                            }
                        } else {
                            // Position bookmark.
                            GaiaSky.postRunnable(() -> {
                                var p = selected.node.position;
                                var d = selected.node.direction;
                                var u = selected.node.up;
                                EventManager.publish(Event.CAMERA_MODE_CMD, bookmarksTree, CameraMode.FREE_MODE, true);
                                EventManager.publish(Event.CAMERA_POS_CMD, bookmarksTree, (Object) new double[]{p.x, p.y, p.z});
                                EventManager.publish(Event.CAMERA_DIR_CMD, bookmarksTree, (Object) new double[]{d.x, d.y, d.z});
                                EventManager.publish(Event.CAMERA_UP_CMD, bookmarksTree, (Object) new double[]{u.x, u.y, u.z});
                                EventManager.publish(Event.TIME_CHANGE_CMD, bookmarksTree, selected.node.time);
                            });

                        }
                    }
                    return true;
                } else if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    ie.toCoordinates(event.getListenerActor(), tmpCoords);
                    if (ie.getType() == Type.touchDown && ie.getButton() == Input.Buttons.RIGHT) {
                        TreeNode target = bookmarksTree.getNodeAt(tmpCoords.y);
                        // Context menu!
                        if (target != null) {
                            //selectBookmark(target.getValue(), true);
                            GaiaSky.postRunnable(() -> {
                                ContextMenu cm = new ContextMenu(skin, "default");
                                // New folder...
                                BookmarkNode parent = target.node.getFirstFolderAncestor();
                                String parentName = "/" + (parent == null ? "" : parent.path.toString());
                                MenuItem newDirectory = new MenuItem(I18n.msg("gui.bookmark.context.newfolder", parentName), skin);
                                newDirectory.addListener(evt -> {
                                    if (evt instanceof ChangeEvent) {
                                        NewBookmarkFolderDialog newBookmarkFolderDialog = new NewBookmarkFolderDialog(parent != null ? parent.path.toString() : "/", skin, stage);
                                        newBookmarkFolderDialog.setAcceptRunnable(() -> {
                                            String folderName = newBookmarkFolderDialog.input.getText();
                                            EventManager.publish(Event.BOOKMARKS_ADD, newDirectory, parent != null ? parent.path.resolve(folderName).toString() : folderName, true);
                                            reloadBookmarksTree();
                                        });
                                        newBookmarkFolderDialog.show(stage);
                                        return true;
                                    }
                                    return false;
                                });
                                cm.addItem(newDirectory);
                                // Delete
                                MenuItem delete = new MenuItem(I18n.msg("gui.bookmark.context.delete", target.getValue()), skin);
                                delete.addListener(evt -> {
                                    if (evt instanceof ChangeEvent) {
                                        EventManager.publish(Event.BOOKMARKS_REMOVE, delete, target.node.path.toString());
                                        reloadBookmarksTree();
                                        return true;
                                    }
                                    return false;
                                });
                                cm.addItem(delete);

                                cm.addSeparator();

                                // Move up and down
                                MenuItem moveUp = new MenuItem(I18n.msg("gui.bookmark.context.move.up"), skin);
                                moveUp.addListener(evt -> {
                                    if (evt instanceof ChangeEvent) {
                                        EventManager.publish(Event.BOOKMARKS_MOVE_UP, moveUp, target.node);
                                        reloadBookmarksTree();
                                        return true;
                                    }
                                    return false;
                                });
                                cm.addItem(moveUp);
                                MenuItem moveDown = new MenuItem(I18n.msg("gui.bookmark.context.move.down"), skin);
                                moveDown.addListener(evt -> {
                                    if (evt instanceof ChangeEvent) {
                                        EventManager.publish(Event.BOOKMARKS_MOVE_DOWN, moveDown, target.node);
                                        reloadBookmarksTree();
                                        return true;
                                    }
                                    return false;
                                });
                                cm.addItem(moveDown);

                                // Move to...
                                if (target.node.parent != null) {
                                    MenuItem move = new MenuItem(I18n.msg("gui.bookmark.context.move", target.getValue(), "/"), skin);
                                    move.addListener(evt -> {
                                        if (evt instanceof ChangeEvent) {
                                            EventManager.publish(Event.BOOKMARKS_MOVE, move, target.node, null);
                                            reloadBookmarksTree();
                                            return true;
                                        }
                                        return false;
                                    });
                                    cm.addItem(move);
                                }
                                List<BookmarkNode> folders = GaiaSky.instance.getBookmarksManager().getFolders();
                                for (BookmarkNode folder : folders) {
                                    if (!target.node.isDescendantOf(folder)) {
                                        MenuItem mv = new MenuItem(I18n.msg("gui.bookmark.context.move", target.getValue(), "/" + folder.path.toString()), skin);
                                        mv.addListener(evt -> {
                                            if (evt instanceof ChangeEvent) {
                                                EventManager.publish(Event.BOOKMARKS_MOVE, mv, target.node, folder);
                                                reloadBookmarksTree();
                                                return true;
                                            }
                                            return false;
                                        });
                                        cm.addItem(mv);
                                    }
                                }

                                newMenu(cm);
                                cm.showMenu(stage, Gdx.input.getX(ie.getPointer()) / Settings.settings.program.ui.scale, stage.getHeight() - Gdx.input.getY(ie.getPointer()) / Settings.settings.program.ui.scale);
                            });
                        } else {
                            // New folder
                            GaiaSky.postRunnable(() -> {
                                ContextMenu cm = new ContextMenu(skin, "default");
                                // New folder...
                                String parentName = "/";
                                MenuItem newDirectory = new MenuItem(I18n.msg("gui.bookmark.context.newfolder", parentName), skin);
                                newDirectory.addListener(evt -> {
                                    if (evt instanceof ChangeEvent) {
                                        NewBookmarkFolderDialog nbfd = new NewBookmarkFolderDialog("/", skin, stage);
                                        nbfd.setAcceptRunnable(() -> {
                                            String folderName = nbfd.input.getText();
                                            EventManager.publish(Event.BOOKMARKS_ADD, newDirectory, folderName, true);
                                            reloadBookmarksTree();
                                        });
                                        nbfd.show(stage);
                                        return true;
                                    }
                                    return false;
                                });
                                cm.addItem(newDirectory);
                                newMenu(cm);
                                cm.showMenu(stage, Gdx.input.getX(ie.getPointer()), Gdx.graphics.getHeight() - Gdx.input.getY(ie.getPointer()));
                            });
                        }
                    }
                    event.setBubbles(false);
                    return true;
                }
            return false;
        });

        bookmarksScrollPane = new OwnScrollPane(bookmarksTree, skin, "minimalist-nobg");
        bookmarksScrollPane.setName("bookmarks scroll");

        bookmarksScrollPane.setFadeScrollBars(false);
        bookmarksScrollPane.setScrollingDisabled(true, false);

        bookmarksScrollPane.setHeight(260f);
        bookmarksScrollPane.setWidth(contentWidth);

        /*
         * ADD TO CONTENT
         */
        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(pad12);
        objectsGroup.addActor(searchBox);
        if (bookmarksScrollPane != null) {
            objectsGroup.addActor(bookmarksScrollPane);
        }
        objectsGroup.addActor(infoTable);

        component = objectsGroup;

    }

    private void newMenu(ContextMenu cm) {
        for (ContextMenu menu : contextMenus) {
            menu.remove();
        }
        contextMenus.add(cm);
    }

    public void reloadBookmarksTree() {
        java.util.List<BookmarkNode> bookmarks = GaiaSky.instance.getBookmarksManager().getBookmarks();
        bookmarksTree.clearChildren();
        for (BookmarkNode bookmark : bookmarks) {
            TreeNode node = new TreeNode(bookmark, skin);
            if (bookmark.folder)
                node.setIcon(folderIcon);
            else
                node.setIcon(bookmarkIcon);
            bookmarksTree.add(node);
            genSubtree(node, bookmark);
        }
        bookmarksTree.pack();
    }

    private void genSubtree(TreeNode parent, BookmarkNode bookmark) {
        if (bookmark.children != null && !bookmark.children.isEmpty()) {
            for (BookmarkNode child : bookmark.children) {
                TreeNode tn = new TreeNode(child, skin);
                if (child.folder)
                    tn.setIcon(folderIcon);
                else
                    tn.setIcon(bookmarkIcon);
                parent.add(tn);
                genSubtree(tn, child);
            }
        }
    }

    public void selectBookmark(String bookmark, boolean fire) {
        if (bookmark == null) {
            bookmarksTree.getSelectedValue();
        }
        boolean backup = events;
        events = fire;
        // Select without firing events, do not use set()
        TreeNode node = bookmarksTree.findNode(bookmark);
        if (node != null) {
            bookmarksTree.getSelection().set(node);
            node.expandTo();
            scrollTo(node);
        }

        events = backup;
    }

    private void scrollTo(TreeNode node) {
        float y = getYPosition(bookmarksTree.getRootNodes(), node);
        bookmarksScrollPane.setScrollY(y);
    }

    private float getYPosition(Array<TreeNode> nodes, TreeNode node) {
        if (nodes == null || nodes.isEmpty())
            return 0;

        float yPos = 0;
        for (TreeNode n : nodes) {
            if (n != node) {
                yPos += n.getHeight() + bookmarksTree.getYSpacing();
                if (n.isExpanded()) {
                    yPos += getYPosition(n.getChildren(), node);
                }
            } else {
                // Found it!
                return yPos;
            }
            if (n.isAscendantOf(node))
                break;
        }
        return yPos;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    private void info(String info1, String info2) {
        if (info1 == null) {
            infoMessage1.setText("");
            infoMessage2.setText("");
            info(false);
        } else {
            infoMessage1.setText(info1);
            infoMessage2.setText(info2);
            info(true);
        }
        infoTable.pack();
    }

    private void info(boolean visible) {
        if (visible) {
            infoCell1.setActor(infoMessage1);
            infoCell2.setActor(infoMessage2);
        } else {
            infoCell1.setActor(null);
            infoCell2.setActor(null);
        }
        infoTable.pack();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case FOCUS_CHANGED -> {
                // Update focus selection in focus list
                FocusView focus = null;
                if (data[0] instanceof String) {
                    view.setEntity(scene.getEntity((String) data[0]));
                    focus = view;
                } else if (data[0] instanceof FocusView) {
                    focus = (FocusView) data[0];
                }
                // Select only if data[1] is true
                if (focus != null) {
                    selectBookmark(focus.getName(), false);
                }
            }
            case BOOKMARKS_ADD -> {
                var d0 = data[0];
                if (d0 instanceof String) {
                    String name = (String) d0;
                    reloadBookmarksTree();
                    selectBookmark(name, false);
                } else {
                    String name = (String) data[4];
                    reloadBookmarksTree();
                    selectBookmark(name, false);
                }
            }
            case BOOKMARKS_REMOVE, BOOKMARKS_REMOVE_ALL -> reloadBookmarksTree();
            default -> {
            }
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    private static class TreeNode extends Tree.Node<TreeNode, String, OwnLabel> {
        public BookmarkNode node;

        public TreeNode(BookmarkNode node, Skin skin) {
            super(new OwnLabel(node.name, skin));
            this.node = node;
            setValue(node.name);
        }
    }

}
