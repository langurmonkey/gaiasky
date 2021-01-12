package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.scenegraph.*;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.scene2d.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndividualVisibilityWindow extends GenericDialog {

    protected float space8, space4, space2;
    protected ISceneGraph sg;

    public IndividualVisibilityWindow(ISceneGraph sg, Stage stage, Skin skin){
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
        /*
         * MESHES
         */
        Group meshesGroup = visibilitySwitcher(MeshObject.class, I18n.txt("gui.meshes"), "meshes");

        /*
         * CONSTELLATIONS
         */
        Group constelGroup = visibilitySwitcher(Constellation.class, I18n.txt("element.constellations"), "constellation");

        content.add(meshesGroup).left().padBottom(pad10 * 2f).row();
        content.add(constelGroup).left();
    }

    private Group visibilitySwitcher(Class<? extends FadeNode> clazz, String title, String id) {
        float componentWidth = ControlsWindow.getContentWidth();
        VerticalGroup objectsGroup = new VerticalGroup();
        objectsGroup.space(space4);
        objectsGroup.left();
        objectsGroup.columnLeft();
        Array<SceneGraphNode> objects = new Array<>();
        List<OwnCheckBox> cbs = new ArrayList<>();
        sg.getRoot().getChildrenByType(clazz, objects);
        Array<String> names = new Array<>(false, objects.size);
        Map<String, IVisibilitySwitch> cMap = new HashMap<>();

        for (SceneGraphNode object : objects) {
            // Omit stars with no proper names
            if (object.getName() != null && !GlobalResources.isNumeric(object.getName())) {
                names.add(object.getName());
                cMap.put(object.getName(), (IVisibilitySwitch) object);
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

        scrollPane.setHeight(Math.min(160f, objectsGroup.getHeight()));
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
