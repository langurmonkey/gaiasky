package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
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
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.NaturalInputListener;
import gaia.cu9.ari.gaiaorbit.scenegraph.*;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.comp.CelestialBodyComparator;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectsComponent extends GuiComponent implements IObserver {
    private static final Log logger = Logger.getLogger(ObjectsComponent.class);
    boolean tree = false;
    boolean list = true;

    protected ISceneGraph sg;

    protected Actor objectsList;
    protected TextField searchBox;
    protected OwnScrollPane focusListScrollPane;

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
        float sp1 = 1 * GlobalConf.SCALE_FACTOR;
        float sp4 = 4 * GlobalConf.SCALE_FACTOR;
        float componentWidth = 160 * GlobalConf.SCALE_FACTOR;
        searchBox = new OwnTextField("", skin);
        searchBox.setName("search box");
        searchBox.setWidth(componentWidth);
        searchBox.setMessageText(txt("gui.objects.search"));
        searchBox.addListener(event -> {
            if (event instanceof InputEvent) {
                InputEvent ie = (InputEvent) event;
                if (ie.getType() == Type.keyUp && !searchBox.getText().isEmpty()) {
                    String text = searchBox.getText();
                    if (sg.containsNode(text.toLowerCase())) {
                        final SceneGraphNode node = sg.getNode(text.toLowerCase());
                        if (node instanceof IFocus) {
                            IFocus focus = (IFocus) node;
                            if (!focus.isCoordinatesTimeOverflow()) {
                                Gdx.app.postRunnable(() -> {
                                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus, true);
                                    EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                                });
                            }

                        }
                    }
                    NaturalInputListener.pressedKeys.remove(ie.getKeyCode());

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

        /*
         * OBJECTS
         */

        treeToModel = new TwoWayHashmap<SceneGraphNode, Node>();

        logger.info(txt("notif.sgtree.init"));

        if (tree) {
            final Tree objectsTree = new Tree(skin, "bright");
            objectsTree.setName("objects list");
            objectsTree.setPadding(sp1);
            objectsTree.setIconSpacing(sp1, sp1);
            objectsTree.setYSpacing(0);
            Array<Node> nodes = createTree(sg.getRoot().children);
            for (Node node : nodes) {
                objectsTree.add(node);
            }
            objectsTree.expandAll();
            objectsTree.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (objectsTree.getSelection().hasItems()) {
                        if (objectsTree.getSelection().hasItems()) {
                            Node n = objectsTree.getSelection().first();
                            final SceneGraphNode node = treeToModel.getBackward(n);
                            if (node instanceof IFocus) {
                                IFocus focus = (IFocus) node;
                                if (!focus.isCoordinatesTimeOverflow()) {
                                    Gdx.app.postRunnable(() -> {
                                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus, true);
                                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                                    });
                                }

                            }

                        }

                    }
                    return true;
                }
                return false;
            });
            objectsList = objectsTree;
        } else if (list) {
            final com.badlogic.gdx.scenes.scene2d.ui.List<String> focusList = new com.badlogic.gdx.scenes.scene2d.ui.List<String>(skin, "light");
            focusList.setName("objects list");
            Array<IFocus> focusableObjects = sg.getFocusableObjects();
            Array<String> names = new Array<String>(focusableObjects.size);

            for (IFocus focus : focusableObjects) {
                // Omit stars with no proper names
                if (focus.getName() != null && !GlobalResources.isNumeric(focus.getName())) {
                    names.add(focus.getName());
                }
            }
            names.sort();

            SceneGraphNode sol = sg.getNode("Sol");
            if (sol != null) {
                Array<IFocus> solChildren = new Array<IFocus>();
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
                    @SuppressWarnings("unchecked") final String name = ((com.badlogic.gdx.scenes.scene2d.ui.List<String>) actor).getSelected();
                    if (sg.containsNode(name)) {
                        SceneGraphNode node = sg.getNode(name);
                        if (node instanceof IFocus) {
                            IFocus focus = (IFocus) node;
                            if (!focus.isCoordinatesTimeOverflow()) {
                                Gdx.app.postRunnable(() -> {
                                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus, true);
                                    EventManager.instance.post(Events.FOCUS_CHANGE_CMD, focus, true);
                                });
                            }
                        }
                    }
                    return true;
                }
                return false;
            });
            objectsList = focusList;
        }
        logger.info(txt("notif.sgtree.initialised"));

        if (tree || list) {
            focusListScrollPane = new OwnScrollPane(objectsList, skin, "minimalist");
            focusListScrollPane.setName("objects list scroll");

            focusListScrollPane.setFadeScrollBars(false);
            focusListScrollPane.setScrollingDisabled(true, false);

            focusListScrollPane.setHeight(tree ? 200 * GlobalConf.SCALE_FACTOR : 100 * GlobalConf.SCALE_FACTOR);
            focusListScrollPane.setWidth(componentWidth);
        }

        /*
         * MESHES
         */
        Group meshesGroup = visibilitySwitcher(MeshObject.class, txt("gui.meshes"), "meshes");

        /*
         * CONSTELLATIONS
         */
        Group constelGroup = visibilitySwitcher(Constellation.class, txt("element.constellations"), "constellation");

        /*
         * ADD TO CONTENT
         */

        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(sp4);
        objectsGroup.addActor(searchBox);
        if (focusListScrollPane != null) {
            objectsGroup.addActor(focusListScrollPane);
        }

        if (meshesGroup != null) {
            objectsGroup.addActor(meshesGroup);
        }

        if (constelGroup != null) {
            objectsGroup.addActor(constelGroup);
        }

        component = objectsGroup;

    }

    private Group visibilitySwitcher(Class<? extends FadeNode> clazz, String title, String id) {
        float componentWidth = 160 * GlobalConf.SCALE_FACTOR;
        float sp4 = 4 * GlobalConf.SCALE_FACTOR;
        VerticalGroup objectsVgroup = new VerticalGroup();
        objectsVgroup.space(sp4);
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
            objectHgroup.space(sp4);
            objectHgroup.left();
            OwnCheckBox cb = new OwnCheckBox(name, skin, sp4);
            IVisibilitySwitch obj = cmap.get(name);
            cb.setChecked(obj.isVisible());

            cb.addListener((event) -> {
                if (event instanceof ChangeEvent && cmap.containsKey(name)) {
                    Gdx.app.postRunnable(() -> {
                        obj.setVisible(cb.isChecked());
                    });
                    return true;
                }
                return false;
            });

            objectHgroup.addActor(cb);
            // Tooltips
            if(obj.getDescription() != null) {
                ImageButton meshDescTooltip = new OwnImageButton(skin, "tooltip");
                meshDescTooltip.addListener(new TextTooltip((obj.getDescription() == null || obj.getDescription().isEmpty() ? "No description" : obj.getDescription()), skin));
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

        scrollPane.setHeight(Math.min(100 * GlobalConf.SCALE_FACTOR, objectsVgroup.getHeight()));
        scrollPane.setWidth(componentWidth);

        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(sp4);
        OwnTextButton selAll = new OwnTextButton(txt("gui.select.all"), skin);
        selAll.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                Gdx.app.postRunnable(() -> {
                    cbs.stream().forEach((i) -> i.setChecked(true));
                });
                return true;
            }
            return false;
        });
        OwnTextButton selNone = new OwnTextButton(txt("gui.select.none"), skin);
        selNone.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                Gdx.app.postRunnable(() -> {
                    cbs.stream().forEach((i) -> i.setChecked(false));
                });
                return true;
            }
            return false;
        });
        buttons.addActor(selAll);
        buttons.addActor(selNone);

        VerticalGroup group = new VerticalGroup();
        group.left();
        group.columnLeft();
        group.space(sp4);

        group.addActor(new OwnLabel(TextUtils.trueCapitalise(title), skin, "header"));
        group.addActor(scrollPane);
        group.addActor(buttons);

        return objects.size == 0 ? null : group;
    }

    private Array<Node> createTree(Array<SceneGraphNode> nodes) {
        Array<Node> treeNodes = new Array<Node>(nodes.size);
        for (SceneGraphNode node : nodes) {
            Label l = new Label(node.name, skin, "ui-10");
            l.setColor(Color.BLACK);
            Node treeNode = new Node(l);

            if (node.children != null && node.children.size != 0) {
                treeNode.addAll(createTree(node.children));
            }

            treeNodes.add(treeNode);
            treeToModel.add(node, treeNode);
        }

        return treeNodes;
    }

    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
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
                if (tree) {
                    Tree objList = ((Tree) objectsList);
                    Node node = treeToModel.getForward(sgn);
                    objList.getSelection().set(node);
                    node.expandTo();

                    focusListScrollPane.setScrollY(focusListScrollPane.getMaxY() - node.getActor().getY());
                } else if (list) {
                    // Update focus selection in focus list
                    @SuppressWarnings("unchecked") com.badlogic.gdx.scenes.scene2d.ui.List<String> objList = (com.badlogic.gdx.scenes.scene2d.ui.List<String>) objectsList;
                    Array<String> items = objList.getItems();
                    SceneGraphNode node = (SceneGraphNode) data[0];

                    // Select without firing events, do not use set()
                    objList.getSelection().items().clear();
                    objList.getSelection().items().add(node.name);

                    int itemIdx = items.indexOf(node.name, false);
                    if (itemIdx >= 0) {
                        objList.getSelection().setProgrammaticChangeEvents(false);
                        objList.setSelectedIndex(itemIdx);
                        objList.getSelection().setProgrammaticChangeEvents(true);
                        float itemHeight = objList.getItemHeight();
                        focusListScrollPane.setScrollY(itemIdx * itemHeight);
                    }
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
