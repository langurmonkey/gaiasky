/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.main.KeyBindings;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IVisibilitySwitch;
import gaiasky.scene.component.Base;
import gaiasky.scene.view.FocusView;
import gaiasky.util.GlobalResources;
import gaiasky.util.Pair;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.util.List;
import java.util.*;

public class IndividualVisibilityWindow extends GenericDialog implements IObserver {

    protected final Scene scene;
    protected float space8, space4, space2;
    protected Cell<?> elementsCell;
    // Component type currently selected
    protected String currentComponentType = null;
    protected ComponentType currentCt = null;
    protected Map<String, CheckBox> cbMap;

    public IndividualVisibilityWindow(Scene scene, Stage stage, Skin skin) {
        super(I18n.msg("gui.visibility.individual"), skin, stage);

        this.scene = scene;

        space8 = 12.8f;
        space4 = 6.4f;
        space2 = 3.2f;

        cbMap = new HashMap<>();

        setAcceptText(I18n.msg("gui.close"));
        setModal(false);
        setWidth(500f);

        // Build
        buildSuper();
        // Pack
        pack();

        EventManager.instance.subscribe(this, Event.PER_OBJECT_VISIBILITY_CMD, Event.CATALOG_VISIBLE);
    }

    @Override
    protected void build() {
        content.clear();

        final String cct = currentComponentType;
        // Components
        float buttonPadHor = 14f;
        float buttonPadVert = 8f;
        int visTableCols = 7;
        Table buttonTable = new Table(skin);
        // Always one button checked
        ButtonGroup<Button> buttonGroup = new ButtonGroup<>();
        buttonGroup.setMinCheckCount(1);
        buttonGroup.setMaxCheckCount(1);

        content.add(buttonTable).top().center().padBottom(pad18).row();
        elementsCell = content.add().top().left();

        ComponentType[] visibilityEntities = ComponentType.values();
        int j = 0;
        for (final ComponentType ct : visibilityEntities) {
            final String name = ct.getName();
            if (name != null && componentFilter(ct)) {
                Button button;
                if (ct.style != null) {
                    Image icon = new Image(skin.getDrawable(ct.style));
                    button = new OwnTextIconButton("", icon, skin, "toggle");
                } else {
                    continue;
                }
                // Name is the key
                button.setName(ct.key);
                // Tooltip (with or without hotkey)
                String[] hk = KeyBindings.instance.getStringKeys("action.toggle/" + ct.key, true);
                if (hk != null) {
                    button.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(ct.getName()), hk, skin));
                } else {
                    button.addListener(new OwnTextTooltip(TextUtils.capitalise(ct.getName()), skin));
                }

                button.addListener(event -> {
                    if (event instanceof ChangeEvent && button.isChecked()) {
                        // Change content only when button is checked!
                        Group elementsList = visibilitySwitcher(ct, TextUtils.capitalise(ct.getName()), ct.getName());
                        elementsCell.clearActor();
                        elementsCell.setActor(elementsList);
                        content.pack();
                        currentComponentType = name;
                        currentCt = ct;
                        return true;
                    }
                    return false;
                });

                if (name.equals(cct)) {
                    button.setChecked(true);
                }
                Cell<?> c = buttonTable.add(button).padBottom(buttonPadVert);
                if ((j + 1) % visTableCols == 0) {
                    buttonTable.row();
                } else {
                    c.padRight(buttonPadHor);
                }
                buttonGroup.add(button);
                j++;
            }
        }
        if (cct != null)
            buttonGroup.setChecked(cct);
        content.pack();
    }

    /**
     * Filters component types for the individual visibility window. Returns true for those components that should be listed
     * in the window.
     * @param ct The component type.
     * @return Whether it should be listed or not.
     */
    private boolean componentFilter(ComponentType ct) {
        return ct != ComponentType.Labels
                && ct != ComponentType.Atmospheres
                && ct != ComponentType.Clouds
                && ct != ComponentType.Effects
                && ct != ComponentType.VelocityVectors
                && ct != ComponentType.Keyframes
                && ct != ComponentType.RecursiveGrid
                && ct != ComponentType.Equatorial
                && ct != ComponentType.Ecliptic
                && ct != ComponentType.Galactic
                && ct != ComponentType.Boundaries
                && ct != ComponentType.Systems;
    }

    private boolean filter(final String[] names, final String filter) {
        if (filter == null || filter.isEmpty())
            return true;

        for (final String name : names) {
            if (name.toLowerCase(Locale.ROOT).contains(filter))
                return true;
        }
        return false;
    }

    private void addObjects(final VerticalGroup objectsGroup, final List<OwnCheckBox> checkBoxes, final ComponentType ct, final String filter) {
        if (ct == ComponentType.Locations) {
            addObjectsLocations(objectsGroup, checkBoxes, filter);
        } else {
            addObjectsRegular(objectsGroup, checkBoxes, ct, filter);
        }
    }

    private void addObjectsLocations(final VerticalGroup objectsGroup, final List<OwnCheckBox> checkBoxes, final String filter) {
        objectsGroup.clear();
        checkBoxes.clear();
        Array<Entity> objects = new Array<>();
        scene.findEntitiesByComponentType(ComponentType.Locations, objects);
        Array<String> typeNames = new Array<>(false, objects.size);
        Map<String, Pair<Map<String, IVisibilitySwitch>, Array<String>>> typeMap = new HashMap<>();
        cbMap.clear();
        // Organize by types.
        for (Entity object : objects) {
            var base = Mapper.base.get(object);

            if (filter(base.names, filter) &&
                    !isHookObject(base)) {
                var loc = Mapper.loc.get(object);
                var hasLoc = loc != null;
                var name = base.getName();

                var defaultType = I18n.msg("gui.location.type.default");
                var type = hasLoc ?
                        (loc.locationType != null ? loc.locationType : defaultType) :
                        Mapper.graph.get(object).parentName;
                if (!typeMap.containsKey(type)) {
                    Array<String> objNames = new Array<>();
                    Map<String, IVisibilitySwitch> objMap = new HashMap<>();
                    typeMap.put(type, new Pair<>(objMap, objNames));
                    typeNames.add(type);
                }
                Pair<Map<String, IVisibilitySwitch>, Array<String>> pair = typeMap.get(type);
                pair.getSecond().add(name);
                pair.getFirst().put(name, new FocusView(object));
            }
        }
        // Sort all names.
        typeNames.sort();
        typeMap.forEach((key, value) -> value.getSecond().sort());


        if (typeNames.isEmpty()) {
            objectsGroup.addActor(new OwnLabel(I18n.msg("gui.elements.type.none"), skin));
        } else {
            for (String typeName : typeNames) {
                Pair<Map<String, IVisibilitySwitch>, Array<String>> pair = typeMap.get(typeName);
                var names = pair.getSecond();
                var map = pair.getFirst();
                Array<OwnCheckBox> groupCheckBoxes = new Array<>();

                // Table for type checkboxes.
                Table cbs = new Table(skin);
                cbs.top().left();
                for (String name : names) {
                    HorizontalGroup objectHGroup = new HorizontalGroup();
                    objectHGroup.space(space4);
                    objectHGroup.left();
                    OwnCheckBox cb = new OwnCheckBox(name, skin, space4);
                    cb.left();
                    IVisibilitySwitch obj = map.get(name);
                    cb.setChecked(obj.isVisible(true));
                    groupCheckBoxes.add(cb);
                    cbMap.put(name, cb);

                    cb.addListener((event) -> {
                        if (event instanceof ChangeListener.ChangeEvent && map.containsKey(name)) {
                            GaiaSky.postRunnable(() -> {
                                EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, cb, obj, obj.getName(), cb.isChecked());
                                // Meshes are single objects but also catalogs!
                                // Connect to catalog visibility
                                if (Mapper.mesh.has(((FocusView) obj).getEntity())) {
                                    EventManager.publish(Event.CATALOG_VISIBLE, cb, obj.getName(), cb.isChecked());
                                }
                            });
                            return true;
                        }
                        return false;
                    });

                    objectHGroup.addActor(cb);
                    // Tooltips
                    if (obj.getDescription() != null) {
                        ImageButton meshDescTooltip = new OwnImageButton(skin, "tooltip");
                        meshDescTooltip.addListener(new OwnTextTooltip((obj.getDescription() == null || obj.getDescription().isEmpty() ? "No description" : obj.getDescription()), skin));
                        objectHGroup.addActor(meshDescTooltip);
                    }

                    cbs.add(objectHGroup).top().left().padBottom(space2).row();
                    checkBoxes.add(cb);
                }

                // Create collapsible pane for type.
                Table buttons;
                // Select all.
                Button selectAll = new OwnImageButton(skin, "select-all");
                selectAll.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        for (var checkBox : groupCheckBoxes) {
                            if (!checkBox.isDisabled()) {
                                checkBox.setChecked(true);
                            }
                        }
                        return true;
                    }
                    return false;
                });
                selectAll.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.select.all"), skin));

                // Select none.
                Button selectNone = new OwnImageButton(skin, "select-none");
                selectNone.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        for (var checkBox : groupCheckBoxes) {
                            if (!checkBox.isDisabled()) {
                                checkBox.setChecked(false);
                            }
                        }
                        return true;
                    }
                    return false;
                });
                selectNone.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.select.none"), skin));

                final float buttonSize = 17f;
                buttons = new Table(skin);
                buttons.padRight(pad20);
                buttons.add(selectAll).size(buttonSize, buttonSize).right().bottom().padRight(pad10);
                buttons.add(selectNone).size(buttonSize, buttonSize).right().bottom();

                CollapsiblePane cp = new CollapsiblePane(
                        stage,
                        typeName,
                        cbs,
                        510f,
                        skin,
                        "header",
                        "expand-collapse",
                        null,
                        filter != null && !filter.isEmpty(),
                        null,
                        buttons);
                objectsGroup.addActor(cp);
            }
        }

        objectsGroup.pack();
    }

    private void addObjectsRegular(final VerticalGroup objectsGroup, final List<OwnCheckBox> checkBoxes, final ComponentType ct, final String filter) {
        objectsGroup.clear();
        checkBoxes.clear();
        Array<Entity> objects = new Array<>();
        scene.findEntitiesByComponentType(ct, objects);
        Array<String> names = new Array<>(false, objects.size);
        Map<String, IVisibilitySwitch> objMap = new HashMap<>();
        cbMap.clear();

        for (Entity object : objects) {
            // Omit stars with no proper names and particle groups
            var base = Mapper.base.get(object);
            var name = base.getName();
            if (name != null
                    && !GlobalResources.isNumeric(name)
                    && !exception(ct, object)
                    && filter(base.names, filter)
                    && !isHookObject(base)) {
                names.add(name);
                objMap.put(name, new FocusView(object));
            }
        }
        names.sort();

        if (names.isEmpty()) {
            objectsGroup.addActor(new OwnLabel(I18n.msg("gui.elements.type.none"), skin));
        } else {
            for (String name : names) {
                HorizontalGroup objectHgroup = new HorizontalGroup();
                objectHgroup.space(space4);
                objectHgroup.left();
                OwnCheckBox cb = new OwnCheckBox(name, skin, space4);
                IVisibilitySwitch obj = objMap.get(name);
                cb.setChecked(obj.isVisible(true));
                cbMap.put(name, cb);

                cb.addListener((event) -> {
                    if (event instanceof ChangeListener.ChangeEvent && objMap.containsKey(name)) {
                        GaiaSky.postRunnable(() -> {
                            EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, cb, obj, obj.getName(), cb.isChecked());
                            // Meshes are single objects but also catalogs!
                            // Connect to catalog visibility
                            if (Mapper.mesh.has(((FocusView) obj).getEntity())) {
                                EventManager.publish(Event.CATALOG_VISIBLE, cb, obj.getName(), cb.isChecked());
                            }
                        });
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
                checkBoxes.add(cb);
            }
        }

        if (ct.equals(ComponentType.Stars)) {
            objectsGroup.addActor(new OwnLabel("", skin));
            objectsGroup.addActor(new OwnLabel(TextUtils.breakCharacters(I18n.msg("notif.visibility.stars"), 40), skin));
        }

        objectsGroup.pack();
    }

    private Group visibilitySwitcher(final ComponentType ct, final String title, final String id) {
        float componentWidth = 495f;

        // Objects
        final VerticalGroup objectsGroup = new VerticalGroup();
        objectsGroup.space(space4);
        objectsGroup.left();
        objectsGroup.columnLeft();
        final List<OwnCheckBox> checkBoxes = new ArrayList<>();
        addObjects(objectsGroup, checkBoxes, ct, null);

        OwnScrollPane scrollPane = new OwnScrollPane(objectsGroup, skin, "minimalist-nobg");
        scrollPane.setName(id + " scroll");

        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        scrollPane.setHeight(360f);
        scrollPane.setWidth(componentWidth);

        // Filter
        OwnTextField filter = new OwnTextField("", skin);
        filter.setWidth(componentWidth);
        filter.setMessageText(I18n.msg("gui.dataset.filter"));
        filter.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                addObjects(objectsGroup, checkBoxes, ct, filter.getText().trim().toLowerCase(Locale.ROOT));
                return true;
            }
            return false;
        });

        // Buttons
        HorizontalGroup buttons = new HorizontalGroup();
        buttons.space(pad10);
        OwnTextIconButton selAll = new OwnTextIconButton("", skin, "select-all");
        selAll.addListener(new OwnTextTooltip(I18n.msg("gui.select.all"), skin));
        selAll.pad(space2);
        selAll.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                GaiaSky.postRunnable(() -> checkBoxes.forEach((i) -> i.setChecked(true)));
                return true;
            }
            return false;
        });
        OwnTextIconButton selNone = new OwnTextIconButton("", skin, "select-none");
        selNone.addListener(new OwnTextTooltip(I18n.msg("gui.select.none"), skin));
        selNone.pad(space2);
        selNone.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                GaiaSky.postRunnable(() -> checkBoxes.forEach((i) -> i.setChecked(false)));
                return true;
            }
            return false;
        });
        buttons.addActor(selAll);
        buttons.addActor(selNone);

        Table header = new Table(skin);
        header.add(new OwnLabel(TextUtils.trueCapitalise(title), skin, "header")).left().pad(5f).width(componentWidth - 100f);
        header.add(buttons).right().pad(5f).width(100f);

        VerticalGroup group = new VerticalGroup();
        group.left();
        group.columnLeft();
        group.space(space8);

        group.addActor(header);
        group.addActor(filter);
        group.addActor(scrollPane);

        return group;
    }

    /**
     * Checks whether the given archetype is a hook object, i.e., a virtual object that is there only to
     * aggregate a group of objects as children in the scene graph.
     * @param b The base component.
     * @return Whether it is a hook object.
     */
    private boolean isHookObject(Base b) {
        return b.archetype.getName().equals("FadeNode") ||
                b.archetype.getName().equals("OrbitalElementsGroup") ||
                b.archetype.getName().equals("GenericCatalog") ||
                b.archetype.getName().equals("Invisible") ||
                b.getName().endsWith("-hook");
    }

    /**
     * Implements the exception code. Returns true if the given object should not be listed
     * under the given component type.
     *
     * @param ct     The component type
     * @param object The object
     *
     * @return Whether this object is an exception (should not be listed) or not.
     */
    private boolean exception(ComponentType ct, Entity object) {
        return (ct == ComponentType.Planets && Mapper.trajectory.has(object))
                || Mapper.particleSet.has(object)
                || Mapper.starSet.has(object);
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.PER_OBJECT_VISIBILITY_CMD) {
            // Old
            if (data[0] instanceof IVisibilitySwitch obj) {
                String name = (String) data[1];
                boolean checked = (Boolean) data[2];
                // Update checkbox if necessary
                if (currentCt != null && obj.hasCt(currentCt)) {
                    CheckBox cb = cbMap.get(name);
                    if (cb != null && source != cb) {
                        cb.setProgrammaticChangeEvents(false);
                        cb.setChecked(checked);
                        cb.setProgrammaticChangeEvents(true);
                    }
                }
            }

            // New
            if (data[0] instanceof Entity entity) {
                var base = Mapper.base.get(entity);
                String name = (String) data[1];
                boolean checked = (Boolean) data[2];
                // Update checkbox if necessary
                if (currentCt != null && base.hasCt(currentCt)) {
                    CheckBox cb = cbMap.get(name);
                    if (cb != null && source != cb) {
                        cb.setProgrammaticChangeEvents(false);
                        cb.setChecked(checked);
                        cb.setProgrammaticChangeEvents(true);
                    }
                }
            }

        } else if (event == Event.CATALOG_VISIBLE) {
            String name = (String) data[0];
            boolean checked = (Boolean) data[1];

            // New model
            var entity = scene.getEntity(name);
            if (entity != null) {
                var base = Mapper.base.get(entity);
                var mesh = Mapper.mesh.get(entity);
                if (mesh != null && currentCt != null && base.hasCt(currentCt)) {
                    CheckBox cb = cbMap.get(name);
                    if (cb != null && source != cb) {
                        cb.setProgrammaticChangeEvents(false);
                        cb.setChecked(checked);
                        cb.setProgrammaticChangeEvents(true);
                    }

                }
            }
        }
    }
}
