/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

import com.badlogic.gdx.Input.Keys;
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
import gaiasky.interfce.ControlsWindow;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextField;

public class BookmarksComponent extends GuiComponent implements IObserver {
    private static final Log logger = Logger.getLogger(BookmarksComponent.class);

    protected ISceneGraph sg;

    protected List<String> bookmarksList;
    protected TextField searchBox;
    protected OwnScrollPane bookmarksScrollPane;

    protected Table infoTable;
    protected Cell infoCell1, infoCell2;
    protected OwnLabel infoMessage1, infoMessage2;

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

        logger.info(I18n.txt("notif.sgtree.init"));

        bookmarksList = new List<>(skin);
        bookmarksList.setName("objects list");

        reloadBookmarks();

        java.util.List<String> bms = BookmarksManager.getBookmarks();
        Array<String> bookmarks = new Array<>(bms.size());
        for (String bookmark : bms)
            bookmarks.add(bookmark);


        bookmarksList.setItems(bookmarks);
        bookmarksList.pack();//
        bookmarksList.addListener(event -> {
            if (event instanceof ChangeEvent) {
                ChangeEvent ce = (ChangeEvent) event;
                Actor actor = ce.getTarget();
                @SuppressWarnings("unchecked") final String text = ((List<String>) actor).getSelected().toLowerCase().trim();
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
                return true;
            }
            return false;
        });

        bookmarksScrollPane = new OwnScrollPane(bookmarksList, skin, "minimalist-nobg");
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

    public void reloadBookmarks() {
        java.util.List<String> bms = BookmarksManager.getBookmarks();
        Array<String> bookmarks = new Array<>(bms.size());
        for (String bookmark : bms)
            bookmarks.add(bookmark);


        bookmarksList.getSelection().setProgrammaticChangeEvents(false);
        bookmarksList.setItems(bookmarks);
        bookmarksList.getSelection().setProgrammaticChangeEvents(true);
        bookmarksList.pack();//
        bookmarksList.addListener(event -> {
            if (event instanceof ChangeEvent) {
                ChangeEvent ce = (ChangeEvent) event;
                Actor actor = ce.getTarget();
                @SuppressWarnings("unchecked") final String text = ((List<String>) actor).getSelected().toLowerCase().trim();
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
                            info(null, null);
                        } else if (timeOverflow) {
                            info(I18n.txt("gui.objects.search.timerange.1", text), I18n.txt("gui.objects.search.timerange.2"));
                        } else {
                            info(I18n.txt("gui.objects.search.invisible.1", text), I18n.txt("gui.objects.search.invisible.2", focus.getCt().toString()));
                        }
                    }
                } else {
                    info(null, null);
                }
                return true;
            }
            return false;
        });
    }

    public void selectBookmark(String bookmark) {
        bookmarksList.getSelection().items().clear();
        if (bookmark != null) {
            // Select without firing events, do not use set()
            bookmarksList.getSelection().items().add(bookmark);

            Array<String> items = bookmarksList.getItems();
            int itemIdx = items.indexOf(bookmark, false);
            if (itemIdx >= 0) {
                bookmarksList.getSelection().setProgrammaticChangeEvents(false);
                bookmarksList.setSelectedIndex(itemIdx);
                bookmarksList.getSelection().setProgrammaticChangeEvents(true);
                float itemHeight = bookmarksList.getItemHeight();
                bookmarksScrollPane.setScrollY(itemIdx * itemHeight);
            }
        }
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
                    selectBookmark(node.getName());
                }
                break;
            case BOOKMARKS_ADD:
                reloadBookmarks();
                selectBookmark(((SceneGraphNode) data[0]).getName());
                break;
            case BOOKMARKS_REMOVE:
                reloadBookmarks();
                selectBookmark(null);
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
