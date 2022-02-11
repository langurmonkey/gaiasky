/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.components;

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
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.interafce.ControlsWindow;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.comp.CelestialBodyComparator;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextField;

/**
 * A component that shows a search box and some of the objects in Gaia Sky.
 */
public class ObjectsComponent extends GuiComponent implements IObserver {
    protected ISceneGraph sg;

    protected Actor objectsList;
    protected TextField searchBox;
    protected OwnScrollPane focusListScrollPane;

    protected Table infoTable;
    protected Cell<?> infoCell1, infoCell2;
    protected OwnLabel infoMessage1, infoMessage2;

    public ObjectsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED);
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
                                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE, true);
                                    EventManager.publish(Event.FOCUS_CHANGE_CMD, this, focus, true);
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
        final com.badlogic.gdx.scenes.scene2d.ui.List<String> focusList = new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin);
        focusList.setName("objects list");
        Array<IFocus> focusableObjects = sg.getFocusableObjects();
        Array<String> names = new Array<>(false, focusableObjects.size);

        for (IFocus focus : focusableObjects) {
            // Omit stars with no proper names
            if (focus.getName() != null && !GlobalResources.isNumeric(focus.getName())) {
                names.add(focus.getName());
            }
        }
        names.sort();

        SceneGraphNode sol = sg.getNode("Sun");
        if (sol != null) {
            Array<IFocus> solChildren = new Array<>();
            sol.addFocusableObjects(solChildren);
            solChildren.sort(new CelestialBodyComparator());
            for (IFocus cb : solChildren)
                names.insert(0, cb.getName());
        }

        focusList.setItems(names);
        focusList.pack();//
        focusList.addListener(event -> {
            if (event instanceof ChangeEvent) {
                ChangeEvent ce = (ChangeEvent) event;
                Actor actor = ce.getTarget();
                @SuppressWarnings("unchecked") final String text = ((com.badlogic.gdx.scenes.scene2d.ui.List<String>) actor).getSelected().toLowerCase().trim();
                if (sg.containsNode(text)) {
                    SceneGraphNode node = sg.getNode(text);
                    if (node instanceof IFocus) {
                        IFocus focus = (IFocus) node;
                        boolean timeOverflow = focus.isCoordinatesTimeOverflow();
                        boolean ctOn = GaiaSky.instance.isOn(focus.getCt());
                        if (!timeOverflow && ctOn) {
                            GaiaSky.postRunnable(() -> {
                                EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE, true);
                                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, focus, true);
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
        objectsList = focusList;

        focusListScrollPane = new OwnScrollPane(objectsList, skin, "minimalist-nobg");
        focusListScrollPane.setName("objects list scroll");

        focusListScrollPane.setFadeScrollBars(false);
        focusListScrollPane.setScrollingDisabled(true, false);

        focusListScrollPane.setHeight(160f);
        focusListScrollPane.setWidth(contentWidth);

        /*
         * ADD TO CONTENT
         */

        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(pad12);
        objectsGroup.addActor(searchBox);
        if (focusListScrollPane != null) {
            objectsGroup.addActor(focusListScrollPane);
        }
        objectsGroup.addActor(infoTable);

        component = objectsGroup;

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
        if (event == Event.FOCUS_CHANGED) {// Update focus selection in focus list
            SceneGraphNode sgn = null;
            if (data[0] instanceof String) {
                sgn = sg.getNode((String) data[0]);
            } else {
                sgn = (SceneGraphNode) data[0];
            }
            // Select only if data[1] is true
            if (sgn != null) {
                // Update focus selection in focus list
                @SuppressWarnings("unchecked") List<String> objList = (List<String>) objectsList;
                Array<String> items = objList.getItems();
                SceneGraphNode node = (SceneGraphNode) data[0];

                // Select without firing events, do not use set()
                objList.getSelection().items().clear();
                objList.getSelection().items().add(node.getName());

                int itemIdx = items.indexOf(node.getName(), false);
                if (itemIdx >= 0) {
                    objList.getSelection().setProgrammaticChangeEvents(false);
                    objList.setSelectedIndex(itemIdx);
                    objList.getSelection().setProgrammaticChangeEvents(true);
                    float itemHeight = objList.getItemHeight();
                    focusListScrollPane.setScrollY(itemIdx * itemHeight);
                }
            }
        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

}
