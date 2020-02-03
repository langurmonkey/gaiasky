/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.comp.CelestialBodyComparator;
import gaiasky.util.scene2d.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectsComponent extends GuiComponent implements IObserver {
    private static final Log logger = Logger.getLogger(ObjectsComponent.class);
    boolean list = true;

    protected ISceneGraph sg;

    protected Actor objectsList;
    protected TextField searchBox;
    protected OwnScrollPane focusListScrollPane;

    protected Table infoTable;
    protected Cell infoCell1, infoCell2;
    protected OwnLabel infoMessage1, infoMessage2;

    /**
     * Tree to model equivalences
     */
    private TwoWayHashmap<SceneGraphNode, Node> treeToModel;

    public ObjectsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED);
    }

    @Override
    public void initialize() {
        float componentWidth = 160 * GlobalConf.UI_SCALE_FACTOR;
        searchBox = new OwnTextField("", skin);
        searchBox.setName("search box");
        searchBox.setWidth(componentWidth);
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

        treeToModel = new TwoWayHashmap<>();

        logger.info(I18n.txt("notif.sgtree.init"));

        final com.badlogic.gdx.scenes.scene2d.ui.List<String> focusList = new com.badlogic.gdx.scenes.scene2d.ui.List<>(skin);
        focusList.setName("objects list");
        Array<IFocus> focusableObjects = sg.getFocusableObjects();
        Array<String> names = new Array<>(focusableObjects.size);

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
        objectsList = focusList;
        logger.info(I18n.txt("notif.sgtree.initialised"));

        focusListScrollPane = new OwnScrollPane(objectsList, skin, "minimalist-nobg");
        focusListScrollPane.setName("objects list scroll");

        focusListScrollPane.setFadeScrollBars(false);
        focusListScrollPane.setScrollingDisabled(true, false);

        focusListScrollPane.setHeight(100 * GlobalConf.UI_SCALE_FACTOR);
        focusListScrollPane.setWidth(componentWidth);

        /*
         * MESHES
         */
        Group meshesGroup = visibilitySwitcher(MeshObject.class, I18n.txt("gui.meshes"), "meshes");

        /*
         * CONSTELLATIONS
         */
        Group constelGroup = visibilitySwitcher(Constellation.class, I18n.txt("element.constellations"), "constellation");

        /*
         * ADD TO CONTENT
         */

        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(space8);
        objectsGroup.addActor(searchBox);
        if (focusListScrollPane != null) {
            objectsGroup.addActor(focusListScrollPane);
        }
        objectsGroup.addActor(infoTable);

        if (meshesGroup != null) {
            objectsGroup.addActor(meshesGroup);
        }

        if (constelGroup != null) {
            objectsGroup.addActor(constelGroup);
        }

        component = objectsGroup;

    }

    private Group visibilitySwitcher(Class<? extends FadeNode> clazz, String title, String id) {
        float componentWidth = 160 * GlobalConf.UI_SCALE_FACTOR;
        VerticalGroup objectsVgroup = new VerticalGroup();
        objectsVgroup.space(space4);
        objectsVgroup.left();
        objectsVgroup.columnLeft();
        Array<SceneGraphNode> objects = new Array<>();
        List<OwnCheckBox> cbs = new ArrayList<>();
        sg.getRoot().getChildrenByType(clazz, objects);
        Array<String> names = new Array<>(objects.size);
        Map<String, IVisibilitySwitch> cmap = new HashMap<>();

        for (SceneGraphNode object : objects) {
            // Omit stars with no proper names
            if (object.getName() != null && !GlobalResources.isNumeric(object.getName())) {
                names.add(object.getName());
                cmap.put(object.getName(), (IVisibilitySwitch) object);
            }
        }
        names.sort();

        for (String name : names) {
            HorizontalGroup objectHgroup = new HorizontalGroup();
            objectHgroup.space(space4);
            objectHgroup.left();
            OwnCheckBox cb = new OwnCheckBox(name, skin, space4);
            IVisibilitySwitch obj = cmap.get(name);
            cb.setChecked(obj.isVisible());

            cb.addListener((event) -> {
                if (event instanceof ChangeEvent && cmap.containsKey(name)) {
                    GaiaSky.postRunnable(() -> obj.setVisible(cb.isChecked()));
                    return true;
                }
                return false;
            });

            objectHgroup.addActor(cb);
            // Tooltips
            if (obj.getDescription() != null) {
                ImageButton meshDescTooltip = new OwnImageButton(skin, "tooltip");
                meshDescTooltip.addListener(new OwnTextTooltip((obj.getDescription() == null || obj.getDescription().isEmpty() ? "No description" : obj.getDescription()), skin));
                objectHgroup.addActor(meshDescTooltip);
            }

            objectsVgroup.addActor(objectHgroup);
            cbs.add(cb);
        }

        objectsVgroup.pack();
        OwnScrollPane scrollPane = new OwnScrollPane(objectsVgroup, skin, "minimalist-nobg");
        scrollPane.setName(id + " scroll");

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        scrollPane.setHeight(Math.min(100 * GlobalConf.UI_SCALE_FACTOR, objectsVgroup.getHeight()));
        scrollPane.setWidth(componentWidth);

        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(space4);
        OwnTextButton selAll = new OwnTextButton(I18n.txt("gui.select.all"), skin);
        selAll.pad(space2, space8, space2, space8);
        selAll.setHeight(18 * GlobalConf.UI_SCALE_FACTOR);
        selAll.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                GaiaSky.postRunnable(() -> cbs.stream().forEach((i) -> i.setChecked(true)));
                return true;
            }
            return false;
        });
        OwnTextButton selNone = new OwnTextButton(I18n.txt("gui.select.none"), skin);
        selNone.pad(space2, space8, space2, space8);
        selNone.setHeight(18 * GlobalConf.UI_SCALE_FACTOR);
        selNone.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                GaiaSky.postRunnable(() -> cbs.stream().forEach((i) -> i.setChecked(false)));
                return true;
            }
            return false;
        });
        buttons.addActor(selAll);
        buttons.addActor(selNone);

        VerticalGroup group = new VerticalGroup();
        group.left();
        group.columnLeft();
        group.space(space4);

        group.addActor(new OwnLabel(TextUtils.trueCapitalise(title), skin, "header"));
        group.addActor(scrollPane);
        group.addActor(buttons);

        return objects.size == 0 ? null : group;
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
    public void notify(Events event, Object... data) {
        switch (event) {
        case FOCUS_CHANGED:
            // Update focus selection in focus list
            SceneGraphNode sgn = null;
            if (data[0] instanceof String) {
                sgn = sg.getNode((String) data[0]);
            } else {
                sgn = (SceneGraphNode) data[0];
            }
            // Select only if data[1] is true
            if (sgn != null) {
                // Update focus selection in focus list
                @SuppressWarnings("unchecked") com.badlogic.gdx.scenes.scene2d.ui.List<String> objList = (com.badlogic.gdx.scenes.scene2d.ui.List<String>) objectsList;
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
