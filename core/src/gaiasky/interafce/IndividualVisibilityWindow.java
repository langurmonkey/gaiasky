package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.*;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.*;

import java.util.List;
import java.util.*;

/**
 * This window controls the visibility of individual objects
 */
public class IndividualVisibilityWindow extends GenericDialog {

    protected float space8, space4, space2;
    protected ISceneGraph sg;
    protected Cell elementsCell;

    public IndividualVisibilityWindow(ISceneGraph sg, Stage stage, Skin skin) {
        super(I18n.txt("gui.visibility.individual"), skin, stage);

        this.sg = sg;
        space8 = 12.8f;
        space4 = 6.4f;
        space2 = 3.2f;

        setAcceptText(I18n.txt("gui.close"));
        setModal(false);

        // Build
        buildSuper();
        // Pack
        pack();

    }

    @Override
    protected void build() {
        content.clear();

        // Components
        float buttonPadHor = 6f;
        int visTableCols = 7;
        Table buttonTable = new Table(skin);
        Map<String, Button> buttonMap = new HashMap<>();
        // Always one button checked
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);

        content.add(buttonTable).left().padBottom(pad10).row();
        elementsCell = content.add().left();

        ComponentType[] visibilityEntities = ComponentType.values();
        if (visibilityEntities != null) {
            for (int i = 0; i < visibilityEntities.length; i++) {
                final ComponentType ct = visibilityEntities[i];
                final String name = ct.getName();
                if (name != null) {
                    Button button;
                    if (ct.style != null) {
                        Image icon = new Image(skin.getDrawable(ct.style));
                        button = new OwnTextIconButton("", icon, skin, "toggle");
                    } else {
                        button = new OwnTextButton(name, skin, "toggle");
                    }
                    // Name is the key
                    button.setName(ct.key);
                    // Tooltip (with or without hotkey)
                    String hk = KeyBindings.instance.getStringKeys("action.toggle/" + ct.key);
                    if (hk != null) {
                        button.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(ct.getName()), hk, skin));
                    } else {
                        button.addListener(new OwnTextTooltip(TextUtils.capitalise(ct.getName()), skin));
                    }

                    buttonMap.put(name, button);
                    if (!ct.key.equals(name))
                        buttonMap.put(ct.key, button);

                    button.addListener(event -> {
                        if (event instanceof ChangeListener.ChangeEvent) {
                            Group elementsList = visibilitySwitcher(ct, TextUtils.capitalise(ct.getName()), ct.getName());
                            elementsCell.clearActor();

                            if (elementsList == null) {
                                elementsCell.setActor(new OwnLabel(I18n.txt("gui.elements.type.none"), skin));
                            }else {
                                elementsCell.setActor(elementsList);
                            }
                            content.pack();
                            return true;
                        }
                        return false;
                    });
                    Cell c = buttonTable.add(button);
                    if ((i + 1) % visTableCols == 0) {
                        buttonTable.row();
                    } else {
                        c.padRight(buttonPadHor);
                    }
                    buttonGroup.add(button);
                }
            }
        }

        content.pack();
    }

    private Group visibilitySwitcher(ComponentType ct, String title, String id) {
        float componentWidth = 400f;
        VerticalGroup objectsGroup = new VerticalGroup();
        objectsGroup.space(space4);
        objectsGroup.left();
        objectsGroup.columnLeft();
        Array<SceneGraphNode> objects = new Array<>();
        List<OwnCheckBox> cbs = new ArrayList<>();
        sg.getRoot().getChildrenByComponentType(ct, objects);
        Array<String> names = new Array<>(false, objects.size);
        Map<String, IVisibilitySwitch> cMap = new HashMap<>();

        for (SceneGraphNode object : objects) {
            // Omit stars with no proper names
            if (object.getName() != null && !GlobalResources.isNumeric(object.getName())) {
                names.add(object.getName());
                cMap.put(object.getName(), object);
            }
        }
        names.sort();

        for (String name : names) {
            HorizontalGroup objectHgroup = new HorizontalGroup();
            objectHgroup.space(space4);
            objectHgroup.left();
            OwnCheckBox cb = new OwnCheckBox(name, skin, space4);
            IVisibilitySwitch obj = cMap.get(name);
            cb.setChecked(obj.isVisible());

            cb.addListener((event) -> {
                if (event instanceof ChangeListener.ChangeEvent && cMap.containsKey(name)) {
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

            objectsGroup.addActor(objectHgroup);
            cbs.add(cb);
        }

        objectsGroup.pack();
        OwnScrollPane scrollPane = new OwnScrollPane(objectsGroup, skin, "minimalist-nobg");
        scrollPane.setName(id + " scroll");

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        scrollPane.setHeight(Math.min(360f, objectsGroup.getHeight()));
        scrollPane.setWidth(componentWidth);

        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(pad5);
        OwnTextIconButton selAll = new OwnTextIconButton("", skin, "audio");
        selAll.addListener(new OwnTextTooltip(I18n.txt("gui.select.all"), skin));
        selAll.pad(space2);
        selAll.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                GaiaSky.postRunnable(() -> cbs.stream().forEach((i) -> i.setChecked(true)));
                return true;
            }
            return false;
        });
        OwnTextIconButton selNone = new OwnTextIconButton("", skin, "ban");
        selNone.addListener(new OwnTextTooltip(I18n.txt("gui.select.none"), skin));
        selNone.pad(space2);
        selNone.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
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

    @Override
    protected void accept() {

    }

    @Override
    protected void cancel() {

    }
}
