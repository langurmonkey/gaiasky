/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.BookmarksManager;
import gaiasky.interfce.BookmarksManager.BNode;
import gaiasky.interfce.ControlsWindow;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.scene2d.*;

public class BookmarksComponent extends GuiComponent implements IObserver {
    private static final Log logger = Logger.getLogger(BookmarksComponent.class);
    static private final Vector2 tmpCoords = new Vector2();

    protected ISceneGraph sg;

    protected Tree<TreeNode, String> bookmarksTree;
    protected TextField searchBox;
    protected OwnScrollPane bookmarksScrollPane;

    protected Table infoTable;
    protected Cell infoCell1, infoCell2;
    protected OwnLabel infoMessage1, infoMessage2;

    private boolean events = true;
    private Actor lastEntered;

    public BookmarksComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED, Events.BOOKMARKS_ADD, Events.BOOKMARKS_REMOVE);
    }

    @Override
    public void initialize() {
        float contentWidth = ControlsWindow.getContentWidth();
        searchBox = new OwnTextField("", skin);
        searchBox.setName("search box");
        searchBox.setWidth(contentWidth);
        searchBox.setMessageText(I18n.txt("gui.objects.search"));
        searchBox.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyUp && !searchBox.getText().isEmpty()) {
                    String text = searchBox.getText().toLowerCase().trim();
                    if (sg.containsNode(text)) {
                        SceneGraphNode node = sg.getNode(text);
                        if (node instanceof IFocus) {
                            IFocus focus = (IFocus) node;
                            boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                            boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                            if (!timeOverflow && ctOn) {
                                GaiaSky.postRunnable(() -> {
                                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE, true);
                                    EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                                });
                            } else if (timeOverflow) {
                                info(I18n.txt("gui.objects.search.timerange.1", text), I18n.txt("gui.objects.search.timerange.2"));
                            } else {
                                info(I18n.txt("gui.objects.search.invisible.1", text), I18n.txt("gui.objects.search.invisible.2", focus.getCt().toString()));
                            }
                        }
                    } else {
                        info(null, null);
                    }
                    if (GaiaSky.instance.getICamera() instanceof NaturalCamera)
                        ((NaturalCamera) GaiaSky.instance.getICamera()).getCurrentMouseKbdListener().removePressedKey(ie.getKeyCode());

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
        bookmarksTree = new Tree(skin);
        bookmarksTree.setName("bookmarks tree");
        reloadBookmarksTree();
        bookmarksTree.addListener(event -> {
            if (events)
                if (event instanceof ChangeEvent) {
                    ChangeEvent ce = (ChangeEvent) event;
                    Actor actor = ce.getTarget();
                    TreeNode selected = (TreeNode) ((Tree) actor).getSelectedNode();
                    if (!selected.hasChildren()) {
                        String name = selected.getValue();
                        if (sg.containsNode(name)) {
                            SceneGraphNode node = sg.getNode(name);
                            if (node instanceof IFocus) {
                                IFocus focus = (IFocus) node;
                                boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                                boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                                if (!timeOverflow && ctOn) {
                                    GaiaSky.postRunnable(() -> {
                                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE, true);
                                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                                    });
                                    info(null, null);
                                } else if (timeOverflow) {
                                    info(I18n.txt("gui.objects.search.timerange.1", name), I18n.txt("gui.objects.search.timerange.2"));
                                } else {
                                    info(I18n.txt("gui.objects.search.invisible.1", name), I18n.txt("gui.objects.search.invisible.2", focus.getCt().toString()));
                                }
                            }
                        } else {
                            info(null, null);
                        }
                    }
                    return true;
                } else if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    ie.toCoordinates(event.getListenerActor(), tmpCoords);
                    if (ie.getType() == Type.touchDown && ie.getButton() == Input.Buttons.RIGHT) {
                        TreeNode target = bookmarksTree.getNodeAt(tmpCoords.y);
                        // Context menu!
                        if (target != null){
                            //selectBookmark(target.getValue(), true);
                            logger.info(target.getValue());

                            GaiaSky.postRunnable(()-> {
                                ContextMenu cm = new ContextMenu(skin, "default");
                                MenuItem newFolder = new MenuItem("New folder...", skin);
                                MenuItem move = new MenuItem("Move " + target.getValue() + "...", skin);
                                cm.addItem(move);
                                cm.addItem(newFolder);

                                cm.showMenu(stage, Gdx.input.getX(ie.getPointer()), Gdx.graphics.getHeight() - Gdx.input.getY(ie.getPointer()));
                            });
                        }
                    }
                }
            return false;
        });

        bookmarksScrollPane = new OwnScrollPane(bookmarksTree, skin, "minimalist-nobg");
        bookmarksScrollPane.setName("bookmarks scroll");

        bookmarksScrollPane.setFadeScrollBars(false);
        bookmarksScrollPane.setScrollingDisabled(true, false);

        bookmarksScrollPane.setHeight(100 * GlobalConf.UI_SCALE_FACTOR);
        bookmarksScrollPane.setWidth(contentWidth);

        /*
         * ADD TO CONTENT
         */
        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(space8);
        objectsGroup.addActor(searchBox);
        if (bookmarksScrollPane != null) {
            objectsGroup.addActor(bookmarksScrollPane);
        }
        objectsGroup.addActor(infoTable);

        component = objectsGroup;

    }

    private class TreeNode extends Tree.Node<TreeNode, String, OwnLabel> {

        public TreeNode(String text, Skin skin) {
            super(new OwnLabel(text, skin));
            setValue(text);
        }
    }

    public void reloadBookmarksTree() {
        java.util.List<BNode> bms = BookmarksManager.getBookmarks();
        for (BNode bookmark : bms) {
            TreeNode node = new TreeNode(bookmark.name, skin);
            bookmarksTree.add(node);
            genSubtree(node, bookmark);
        }
        bookmarksTree.pack();
    }

    private void genSubtree(TreeNode parent, BNode bookmark){
        if(bookmark.children != null && !bookmark.children.isEmpty()){
            for(BNode child : bookmark.children){
                TreeNode tn = new TreeNode(child.name, skin);
                parent.add(tn);
                genSubtree(tn, child);
            }
        }
    }

    public void selectBookmark(String bookmark, boolean fire) {
        if (bookmark != null || bookmarksTree.getSelectedValue() != bookmark) {
            boolean bkup = events;
            events = fire;
            // Select without firing events, do not use set()
            TreeNode node = bookmarksTree.findNode(bookmark);
            if (node != null) {
                bookmarksTree.getSelection().set(node);
                node.expandTo();
                scrollTo(node);
            }

            events = bkup;
        }
    }

    private void scrollTo(TreeNode node) {
        float y = getYPosition(bookmarksTree.getNodes(), node, 0f);
        bookmarksScrollPane.setScrollY(y);
    }

    private float getYPosition(Array<TreeNode> nodes, TreeNode node, float accumY) {
        if (nodes == null || nodes.isEmpty())
            return accumY;

        for (TreeNode n : nodes) {
            if (n != node) {
                accumY += n.getHeight() + bookmarksTree.getYSpacing();
                if (n.isExpanded()) {
                    accumY += getYPosition(n.getChildren(), node, 0f);
                }
            } else {
                // Found it!
                return accumY;
            }
            if (n.isAscendantOf(node))
                break;
        }
        return accumY;
    }

    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
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
    public void notify(Events event, Object... data) {
        switch (event) {
            case FOCUS_CHANGED:
                // Update focus selection in focus list
                SceneGraphNode sgn;
                if (data[0] instanceof String) {
                    sgn = sg.getNode((String) data[0]);
                } else {
                    sgn = (SceneGraphNode) data[0];
                }
                // Select only if data[1] is true
                if (sgn != null) {
                    SceneGraphNode node = (SceneGraphNode) data[0];
                    selectBookmark(node.getName(), false);
                }
                break;
            case BOOKMARKS_ADD:
                reloadBookmarksTree();
                selectBookmark(((SceneGraphNode) data[0]).getName(), false);
                break;
            case BOOKMARKS_REMOVE:
                reloadBookmarksTree();
                selectBookmark(null, false);
                break;
            default:
                break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

}
