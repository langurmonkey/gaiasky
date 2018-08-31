package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.NaturalInputListener;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.MeshObject;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.TwoWayHashmap;
import gaia.cu9.ari.gaiaorbit.util.comp.CelestialBodyComparator;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnCheckBox;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextField;

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

        treeToModel = new TwoWayHashmap<SceneGraphNode, Node>();

        logger.info(txt("notif.sgtree.init"));

        if (tree) {
            final Tree objectsTree = new Tree(skin, "bright");
            objectsTree.setName("objects list");
            objectsTree.setPadding(1 * GlobalConf.SCALE_FACTOR);
            objectsTree.setIconSpacing(1 * GlobalConf.SCALE_FACTOR, 1 * GlobalConf.SCALE_FACTOR);
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
                    final String name = ((com.badlogic.gdx.scenes.scene2d.ui.List<String>) actor).getSelected();
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

        // Get meshes
        Array<SceneGraphNode> meshes = new Array<SceneGraphNode>();
        sg.getRoot().getChildrenByType(MeshObject.class, meshes);

        // Add if any
        VerticalGroup meshesGroup = null;
        if (meshes.size > 0) {
            meshesGroup = new VerticalGroup();
            meshesGroup.left();
            meshesGroup.columnLeft();
            meshesGroup.space(4 * GlobalConf.SCALE_FACTOR);

            Label meshesLabel = new Label(txt("gui.meshes"), skin, "header");
            meshesGroup.addActor(meshesLabel);

            VerticalGroup meshesVertical = new VerticalGroup();
            meshesVertical.left();
            meshesVertical.columnLeft();
            meshesVertical.space(4 * GlobalConf.SCALE_FACTOR);
            OwnScrollPane meshesScroll = new OwnScrollPane(meshesVertical, skin, "minimalist-nobg");
            meshesScroll.setScrollingDisabled(false, true);
            meshesScroll.setForceScroll(true, false);
            meshesScroll.setFadeScrollBars(true);
            meshesScroll.setOverscroll(false, false);
            meshesScroll.setSmoothScrolling(true);
            meshesScroll.setWidth(componentWidth + 12 * GlobalConf.SCALE_FACTOR);

            for (SceneGraphNode node : meshes) {
                MeshObject mesh = (MeshObject) node;
                HorizontalGroup meshGroup = new HorizontalGroup();
                meshGroup.space(4 * GlobalConf.SCALE_FACTOR);
                meshGroup.left();
                OwnCheckBox meshCb = new OwnCheckBox(mesh.name, skin, 5 * GlobalConf.SCALE_FACTOR);
                meshCb.setChecked(true);
                meshCb.addListener((event) -> {
                    if (event instanceof ChangeEvent) {
                        Gdx.app.postRunnable(() -> {
                            mesh.setVisible(meshCb.isChecked());
                        });
                    }
                    return false;
                });
                // Tooltips

                ImageButton meshDescTooltip = new OwnImageButton(skin, "tooltip");
                meshDescTooltip.addListener(new TextTooltip((mesh.getDescription() == null || mesh.getDescription().isEmpty() ? "No description" : mesh.getDescription()), skin));

                meshGroup.addActor(meshCb);
                meshGroup.addActor(meshDescTooltip);
                meshesVertical.addActor(meshGroup);
            }
            meshesGroup.addActor(meshesScroll);
        }

        VerticalGroup objectsGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left).space(3 * GlobalConf.SCALE_FACTOR);
        objectsGroup.addActor(searchBox);
        if (focusListScrollPane != null) {
            objectsGroup.addActor(focusListScrollPane);
        }

        if (meshesGroup != null) {
            objectsGroup.addActor(meshesGroup);
        }

        component = objectsGroup;

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
                    com.badlogic.gdx.scenes.scene2d.ui.List<String> objList = (com.badlogic.gdx.scenes.scene2d.ui.List<String>) objectsList;
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
