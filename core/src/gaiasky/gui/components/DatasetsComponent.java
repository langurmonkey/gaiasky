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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.datasets.DatasetFiltersWindow;
import gaiasky.gui.datasets.DatasetInfoWindow;
import gaiasky.gui.datasets.DatasetTransformsWindow;
import gaiasky.gui.datasets.DatasetVisualSettingsWindow;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.IOrbitCoordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatasetsComponent extends GuiComponent implements IObserver {
    private static final float MAX_SCROLL_HEIGHT = 800f;
    private final Map<String, Table> groupMap;
    private final Map<String, ProgrammaticButton[]> imageMap;
    private final Map<String, ColorPickerAbstract> colorMap;
    private final Map<String, OwnSliderReset> scalingMap;
    private final Map<String, DatasetVisualSettingsWindow> visualSettingsMap;
    private final Map<String, DatasetFiltersWindow> filtersMap;
    private final Map<String, DatasetInfoWindow> infoMap;
    private final Map<String, DatasetTransformsWindow> transformsMap;
    private final CatalogManager catalogManager;
    private Table group;
    private OwnScrollPane scroll;
    private OwnLabel noDatasetsLabel;
    private float componentWidth;


    public DatasetsComponent(Skin skin,
                             Stage stage,
                             CatalogManager catalogManager) {
        super(skin, stage);
        this.catalogManager = catalogManager;
        groupMap = new ConcurrentHashMap<>();
        imageMap = new ConcurrentHashMap<>();
        colorMap = new ConcurrentHashMap<>();
        scalingMap = new ConcurrentHashMap<>();
        visualSettingsMap = new ConcurrentHashMap<>();
        filtersMap = new ConcurrentHashMap<>();
        infoMap = new ConcurrentHashMap<>();
        transformsMap = new ConcurrentHashMap<>();
        EventManager.instance.subscribe(this, Event.CATALOG_ADD, Event.CATALOG_REMOVE, Event.CATALOG_VISIBLE, Event.CATALOG_HIGHLIGHT,
                                        Event.CATALOG_POINT_SIZE_SCALING_CMD, Event.CATALOGS_RELOAD);
    }

    @Override
    public void initialize(float componentWidth) {
        this.componentWidth = componentWidth;

        group = new Table();
        group.align(Align.topLeft);
        scroll = new OwnScrollPane(group, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(true, false);
        scroll.setForceScroll(false, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);

        reloadDatasets();

        final var buttonWidth = 300f;
        final var buttonHeight = 50f;
        OwnTextIconButton loadDataset = new OwnTextIconButton(I18n.msg("gui.dsload.title"), skin, "load");
        loadDataset.setSize(buttonWidth, buttonHeight);
        loadDataset.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                EventManager.publish(Event.SHOW_LOAD_DATASET_ACTION, this);
            }
        });


        Table main = new Table(skin);
        main.add(scroll).center().top().padBottom(pad12).row();
        main.add(loadDataset).center();
        component = main;

    }

    public void reloadDatasets() {
        // Clear maps.
        imageMap.clear();
        colorMap.clear();
        visualSettingsMap.clear();
        filtersMap.clear();
        infoMap.clear();
        transformsMap.clear();
        groupMap.clear();

        // Clear main table.
        group.clear();

        Collection<DatasetCard> cis = this.catalogManager.getCatalogInfos();
        if (cis != null) {
            for (DatasetCard ci : cis) {
                addCatalogInfo(ci);
            }
        }
        group.pack();

        scroll.setWidth(group.getWidth() + pad12);
        recalculateScrollHeight();

        addNoDatasets();
    }

    private void goToMissionStart(DatasetCard ci,
                                  Actor source) {
        if (ci.entity != null && ci.isMission()) {
            var catalogEntity = ci.entity;
            var satellite = EntityUtils.getFirstChildWithComponent(catalogEntity, Mapper.parentOrientation);
            if (satellite != null) {
                // Let's see if we have a start time.
                Instant start = null;
                var coordinates = Mapper.coordinates.get(satellite);
                if (coordinates.coordinates instanceof IOrbitCoordinates oc) {
                    var orbit = oc.getOrbitObject();
                    if (orbit != null) {
                        // Our orbit is here.
                        var tr = Mapper.trajectory.get(orbit);
                        if (!tr.closedLoop) {
                            var verts = Mapper.verts.get(orbit);
                            if (verts != null && verts.pointCloudData != null && !verts.pointCloudData.samples.isEmpty()) {
                                start = verts.pointCloudData.getStart();
                            }
                        }
                    }
                }
                if (start == null) {
                    // We do not have time, only go to object.
                    GaiaSky.postRunnable(() -> {
                        // First, set camera mode and focus.
                        EventManager.publish(Event.CAMERA_MODE_CMD, source, CameraManager.CameraMode.FOCUS_MODE, true);
                        EventManager.publish(Event.FOCUS_CHANGE_CMD, source, satellite, true);
                        GaiaSky.postRunnable(() -> {
                            // Go to object.
                            EventManager.publish(Event.GO_TO_OBJECT_CMD, this);
                        });
                    });
                } else {
                    var time = start;
                    GaiaSky.postRunnable(() -> {
                        // First, time.
                        EventManager.publish(Event.TIME_CHANGE_CMD, this, time);

                        // Second, focus after a delay.
                        var task = new Timer.Task() {
                            @Override
                            public void run() {
                                // Set camera mode and focus.
                                EventManager.publish(Event.CAMERA_MODE_CMD, source, CameraManager.CameraMode.FOCUS_MODE, true);
                                EventManager.publish(Event.FOCUS_CHANGE_CMD, source, satellite, true);
                                GaiaSky.postRunnable(() -> {
                                    // Go to object.
                                    EventManager.publish(Event.GO_TO_OBJECT_CMD, this);
                                });

                            }
                        };
                        Timer.schedule(task, 0.5f);
                    });
                }
            }
        }
    }

    private void setDatasetVisibility(DatasetCard ci,
                                      ProgrammaticButton eye,
                                      boolean visible,
                                      Actor source) {
        if (ci.entity != null) {
            if (source != eye) {
                eye.setCheckedNoFire(!visible);
            }
            EventManager.publish(Event.CATALOG_VISIBLE, this, ci.name, visible);
            if (Mapper.mesh.has(ci.entity)) {
                var base = Mapper.base.get(ci.entity);
                EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, this, ci.entity, base.getName(), visible);
            }
        }
    }

    private void setDatasetHighlight(DatasetCard ci,
                                     OwnImageButton mark,
                                     boolean highlight,
                                     Actor source) {
        if (ci.entity != null) {
            if (source != mark) {
                mark.setCheckedNoFire(highlight);
            }
            EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight);
        }
    }

    private void showDatasetFilters(DatasetCard ci) {
        if (!filtersMap.containsKey(ci.name)) {
            var dfw = new DatasetFiltersWindow(ci, skin, stage);
            dfw.setVisible(true);
            dfw.show(stage);
            // Add close listener to remove from map.
            dfw.setCloseListener(() -> {
                filtersMap.remove(ci.name, dfw);
            });
            filtersMap.put(ci.name, dfw);
        }
    }

    private void showDatasetInfo(DatasetCard ci) {
        if (!infoMap.containsKey(ci.name)) {
            var diw = new DatasetInfoWindow(ci, skin, stage);
            diw.setVisible(true);
            diw.show(stage);
            // Add close listener to remove from map.
            diw.setCloseListener(() -> {
                infoMap.remove(ci.name, diw);
            });
            infoMap.put(ci.name, diw);
        }
    }

    private void showDatasetVisualSettings(DatasetCard ci) {
        if (!visualSettingsMap.containsKey(ci.name)) {
            var dpw = new DatasetVisualSettingsWindow(ci, skin, stage);
            dpw.setVisible(true);
            dpw.show(stage);
            // Add close listener to remove from map.
            dpw.setCloseListener(() -> {
                visualSettingsMap.remove(ci.name, dpw);
            });
            visualSettingsMap.put(ci.name, dpw);
        }
    }

    private void showDatasetTransforms(DatasetCard ci) {
        if (!transformsMap.containsKey(ci.name)) {
            var dtw = new DatasetTransformsWindow(ci, skin, stage);
            dtw.setVisible(true);
            dtw.show(stage);
            // Add close listener to remove from map.
            dtw.setCloseListener(() -> {
                transformsMap.remove(ci.name, dtw);
            });
            transformsMap.put(ci.name, dtw);
        }
    }

    private void addCatalogInfo(DatasetCard ci) {
        // Controls
        Table controls = new Table(skin);

        var visibilityButton = new OwnImageButton(skin, "eye");
        visibilityButton.setCheckedNoFire(!ci.isVisible(true));
        visibilityButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.toggle"), skin));
        visibilityButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                setDatasetVisibility(ci, visibilityButton, !ci.isVisible(true), visibilityButton);
                return true;
            }
            return false;
        });

        var highlightButton = new OwnImageButton(skin, "highlight-ds-s");
        highlightButton.setCheckedNoFire(ci.highlighted);
        highlightButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.highlight"), skin));
        highlightButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                setDatasetHighlight(ci, highlightButton, highlightButton.isChecked(), highlightButton);
                return true;
            }
            return false;
        });

        var goToButton = new OwnImageButton(skin, "go-to");
        goToButton.setCheckedNoFire(!ci.isVisible(true));
        goToButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.mission.goto"), skin));
        goToButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                goToMissionStart(ci, goToButton);
                return true;
            }
            return false;
        });


        var filtersButton = new OwnImageButton(skin, "filter");
        filtersButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.filter"), skin));
        filtersButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                showDatasetFilters(ci);
                return true;
            }
            return false;
        });

        var visualSettingsButton = new OwnImageButton(skin, "bolt");
        visualSettingsButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.visuals"), skin));
        visualSettingsButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                showDatasetVisualSettings(ci);
                return true;
            }
            return false;
        });

        var transformsButton = new OwnImageButton(skin, "matrix");
        transformsButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.transforms"), skin));
        transformsButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                showDatasetTransforms(ci);
                return true;
            }
            return false;
        });

        var infoButton = new OwnTextIconButton("", skin, "info");
        infoButton.setPad(0);
        infoButton.setIconColor(ColorUtils.gBlueC);
        infoButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.info"), skin));
        infoButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                showDatasetInfo(ci);
                return true;
            }
            return false;
        });

        var rubbishButton = new OwnTextIconButton("", skin, "rubbish");
        rubbishButton.setPad(0);
        rubbishButton.setIconColor(ColorUtils.gRedC);
        rubbishButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.remove"), skin));
        rubbishButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Remove dataset
                EventManager.publish(Event.CATALOG_REMOVE, this, ci.name);
                return true;
            }
            return false;
        });

        imageMap.put(ci.name, new ProgrammaticButton[]{visibilityButton, highlightButton});
        if (ci.isHighlightable()) {
            controls.add(visibilityButton).padRight(pad6);
            controls.add(highlightButton).padRight(pad30);
            controls.add(visualSettingsButton).padRight(pad6);
            if (ci.hasParticleAttributes()) {
                controls.add(filtersButton).padRight(pad6);
            }
        } else {
            controls.add(visibilityButton).padRight(pad30);
        }
        // Can only add arbitrary transformations to star and particle sets (and octrees).
        if (ci.hasParticleAttributes()) {
            controls.add(transformsButton).padRight(pad30);
        }
        // Go to object for mission datasets.
        if (ci.isMission()) {
            controls.add(goToButton).padRight(pad30);
        }

        controls.add(infoButton).padRight(pad3);
        controls.add(rubbishButton);

        // Dataset table
        Table t = new Table(skin);
        t.align(Align.topLeft);
        // Color picker
        ColorPickerAbstract cp;
        if (ci.hasParticleAttributes()) {
            ColormapPicker cmp = new ColormapPicker(ci.name, ci.hlColor, ci, stage, skin);
            cmp.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.highlight.color.select"), skin));
            cmp.setNewColorRunnable(() -> ci.setHlColor(cmp.getPickedColorArray()));
            cmp.setNewColormapRunnable(() -> ci.setHlColormap(cmp.getPickedCmapIndex(),
                                                              cmp.getPickedCmapAttribute(),
                                                              cmp.getPickedCmapMin(),
                                                              cmp.getPickedCmapMax()));
            cp = cmp;
        } else {
            ColorPicker clp = new ColorPicker(ci.name, ci.hlColor, stage, skin);
            clp.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.highlight.color.select"), skin));
            clp.setNewColorRunnable(() -> ci.setHlColor(clp.getPickedColorArray()));
            cp = clp;
        }
        colorMap.put(ci.name, cp);


        var title = new Table(skin);
        title.left();
        OwnLabel nameLabel = new OwnLabel(TextUtils.capString(ci.name, 23), skin, "hud-subheader");
        nameLabel.addListener(new OwnTextTooltip(ci.name, skin));
        title.add(nameLabel).left().padRight(pad6);
        if (ci.dd != null) {
            var type = ci.dd.datasetType;
            var icon = new OwnImage(skin.getDrawable(type.getIcon()));
            String typeString;
            if (I18n.hasMessage("gui.download.type." + type.typeStr)) {
                typeString = I18n.msg("gui.download.type." + type.typeStr);
            } else {
                typeString = TextUtils.trueCapitalise(type.typeStr);
            }
            icon.setTooltip(typeString, skin);
            icon.setSize(35f, 35f);
            title.add(icon).left();
        }

        float pad = 4.8f;
        if (ci.isHighlightable()) {
            t.add(controls).left().padBottom(pad);
            t.add(cp).right().size(28.8f).padRight(pad6).padBottom(pad).row();
        } else {
            t.add(controls).colspan(2).left().padBottom(pad).row();
        }
        int cap = 25;
        String types = ci.type.toString() + " / " + ci.getCt();
        OwnLabel typesLabel = new OwnLabel(TextUtils.capString(types, cap), skin, "grey-large");
        typesLabel.addListener(new OwnTextTooltip(types, skin));
        t.add(typesLabel).colspan(2).left().row();
        String description = ci.description != null ? ci.description : "";
        OwnLabel desc = new OwnLabel(TextUtils.capString(description, cap), skin);
        desc.addListener(new OwnTextTooltip(description, skin));
        t.add(desc).left().expandX();
        if (!description.isBlank()) {
            Link info = new Link("(+)", skin.get("link", Label.LabelStyle.class), null);
            info.addListener(new OwnTextTooltip(description, skin));
            t.add(info).left().padLeft(pad);
        }

        if (ci.nParticles > 0) {
            t.row();
            OwnLabel nObjects = new OwnLabel(I18n.msg("gui.objects") + ": " + ci.nParticles, skin, "default-blue");
            String bytes = ci.sizeBytes > 0 ? I18n.msg("gui.size") + ": " + GlobalResources.humanReadableByteCount(ci.sizeBytes, true) : "";
            nObjects.addListener(new OwnTextTooltip(nObjects.getText() + (bytes.isEmpty() ? "" : ", " + bytes), skin));
            t.add(nObjects).left();
        }

        if (ci.isHighlightable()) {
            var sizeScaling = new OwnSliderReset(I18n.msg("gui.dataset.size"),
                                                 Constants.MIN_POINT_SIZE_SCALE,
                                                 Constants.MAX_POINT_SIZE_SCALE,
                                                 Constants.SLIDER_STEP_TINY,
                                                 1f,
                                                 skin);
            sizeScaling.setWidth(350f);
            if (ci.entity != null) {
                var graph = Mapper.graph.get(ci.entity);
                var hl = Mapper.highlight.get(ci.entity);
                if (hl != null) {
                    float pointScaling;
                    if (graph.parent != null && Mapper.octree.has(graph.parent)) {
                        pointScaling = Mapper.highlight.get(graph.parent).pointscaling * hl.pointscaling;
                    } else {
                        pointScaling = hl.pointscaling;
                    }
                    sizeScaling.setMappedValue(pointScaling);
                }
            }
            sizeScaling.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    double val = sizeScaling.getMappedValue();
                    EventManager.publish(Event.CATALOG_POINT_SIZE_SCALING_CMD, sizeScaling, ci.name, val);
                    return true;
                }
                return false;
            });
            t.row();
            t.add(sizeScaling).left().colspan(2).padTop(pad9);
            scalingMap.put(ci.name, sizeScaling);
        }

        CollapsibleEntry catalogWidget = new CollapsibleEntry(title, t, skin);
        catalogWidget.align(Align.topLeft);
        catalogWidget.pad(pad9);
        catalogWidget.setWidth(componentWidth * 0.94f);
        catalogWidget.setExpandRunnable(() -> {
            EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this);
            recalculateScrollHeight();
        });
        catalogWidget.setCollapseRunnable(() -> {
            EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this);
            recalculateScrollHeight();
        });
        catalogWidget.addListener(new InputListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (event instanceof InputEvent ie) {
                    InputEvent.Type type = ie.getType();
                    if (type == InputEvent.Type.touchDown) {
                        if (ie.getButton() == Input.Buttons.RIGHT) {
                            GaiaSky.postRunnable(() -> {
                                // Context menu
                                ContextMenu datasetContext = new ContextMenu(skin, "default");
                                // Visibility
                                boolean currentVisibility = ci.isVisible(true);
                                MenuItem visibility = new MenuItem(I18n.msg(currentVisibility ? "gui.hide" : "gui.show"), skin,
                                                                   skin.getDrawable(currentVisibility ? "eye-s-off" : "eye-s-on"));
                                visibility.addListener(new ChangeListener() {
                                    @Override
                                    public void changed(ChangeEvent event,
                                                        Actor actor) {
                                        setDatasetVisibility(ci, visibilityButton, !currentVisibility, catalogWidget);
                                    }
                                });
                                datasetContext.addItem(visibility);
                                if (ci.isHighlightable()) {
                                    // Highlight
                                    boolean currentHighlight = ci.highlighted;
                                    MenuItem highlight = new MenuItem(I18n.msg(currentHighlight ? "gui.deemphasize" : "gui.highlight"), skin,
                                                                      skin.getDrawable(currentHighlight ? "highlight-s-off" : "highlight-s-on"));
                                    highlight.addListener(new ChangeListener() {
                                        @Override
                                        public void changed(ChangeEvent event,
                                                            Actor actor) {
                                            setDatasetHighlight(ci, highlightButton, !currentHighlight, catalogWidget);
                                        }
                                    });
                                    datasetContext.addItem(highlight);
                                    // Settings
                                    MenuItem settings = new MenuItem(I18n.msg("gui.settings.dataset"), skin, skin.getDrawable("prefs-icon"));
                                    settings.addListener(new ChangeListener() {
                                        @Override
                                        public void changed(ChangeEvent event,
                                                            Actor actor) {
                                            showDatasetVisualSettings(ci);
                                        }
                                    });
                                    datasetContext.addItem(settings);
                                }
                                // Delete
                                MenuItem delete = new MenuItem(I18n.msg("gui.download.delete"), skin, skin.getDrawable("iconic-trash"));
                                delete.addListener(new ChangeListener() {
                                    @Override
                                    public void changed(ChangeEvent event,
                                                        Actor actor) {
                                        EventManager.publish(Event.CATALOG_REMOVE, this, ci.name);
                                    }
                                });
                                datasetContext.addItem(delete);
                                datasetContext.showMenu(stage, Gdx.input.getX(ie.getPointer()) / GaiaSky.settings().program.ui.scale,
                                                        stage.getHeight() - Gdx.input.getY(ie.getPointer()) / GaiaSky.settings().program.ui.scale);
                            });
                            // Set to processed
                            event.setBubbles(false);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        group.add(catalogWidget).left().row();

        groupMap.put(ci.name, catalogWidget);
    }

    /**
     * Removes the notice.
     */
    private void removeNoDatasets() {
        if (noDatasetsLabel != null) {
            noDatasetsLabel.remove();
            noDatasetsLabel = null;
            group.pack();
        }
    }

    /**
     * Adds notice if there are no catalogs in the component.
     */
    private void addNoDatasets() {
        if (groupMap != null && groupMap.isEmpty() && noDatasetsLabel == null) {
            noDatasetsLabel = new OwnLabel(I18n.msg("gui.dataset.notfound"), skin);
            noDatasetsLabel.setWidth(componentWidth * 0.94f);
            noDatasetsLabel.setAlignment(Align.left, Align.left);
            group.addActor(noDatasetsLabel);
            group.pack();
        }
    }

    private void recalculateScrollHeight() {
        if (scroll != null && group != null) {
            group.pack();
            scroll.setHeight(Math.min(MAX_SCROLL_HEIGHT, group.getHeight()));
        }
    }

    @Override
    public void notify(gaiasky.event.Event event,
                       Object source,
                       Object... data) {
        switch (event) {
            case CATALOGS_RELOAD -> reloadDatasets();
            case CATALOG_ADD -> {
                removeNoDatasets();
                addCatalogInfo((DatasetCard) data[0]);
                recalculateScrollHeight();
            }
            case CATALOG_REMOVE -> {
                String datasetName = (String) data[0];
                if (groupMap.containsKey(datasetName)) {
                    groupMap.get(datasetName).remove();
                    groupMap.remove(datasetName);
                    imageMap.remove(datasetName);
                    colorMap.remove(datasetName);
                    EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this);
                }
                addNoDatasets();
                recalculateScrollHeight();
            }
            case CATALOG_VISIBLE -> {
                if (source != this) {
                    String datasetName = (String) data[0];
                    boolean visible = (Boolean) data[1];
                    ProgrammaticButton eye = imageMap.get(datasetName)[0];
                    eye.setCheckedNoFire(!visible);
                }
            }
            case PER_OBJECT_VISIBILITY_CMD -> {
                if (source != this) {
                    Entity e = null;
                    if (data[0] instanceof FocusView view) {
                        e = view.getEntity();
                    } else if (data[0] instanceof Entity entity) {
                        e = entity;
                    }
                    if (e != null) {
                        String datasetName = (String) data[1];
                        boolean checked = (Boolean) data[2];
                        if (Mapper.mesh.has(e)) {
                            ProgrammaticButton eye = imageMap.get(datasetName)[0];
                            eye.setCheckedNoFire(!checked);
                        }
                    }
                }
            }
            case CATALOG_HIGHLIGHT -> {
                if (source != this) {
                    DatasetCard ci = (DatasetCard) data[0];
                    float[] col = ci.hlColor;
                    if (colorMap.containsKey(ci.name) && col != null) {
                        if (ci.plainColor) {
                            colorMap.get(ci.name).setPickedColor(col);
                        }
                    }

                    if (imageMap.containsKey(ci.name)) {
                        boolean hl = (Boolean) data[1];
                        ProgrammaticButton hig = imageMap.get(ci.name)[1];
                        hig.setCheckedNoFire(hl);
                    }
                }
            }
            case CATALOG_POINT_SIZE_SCALING_CMD -> {
                if (source != this) {
                    String datasetName = (String) data[0];
                    double val = (Double) data[1];
                    if (scalingMap.containsKey(datasetName)) {
                        var slider = scalingMap.get(datasetName);
                        slider.setProgrammaticChangeEvents(false);
                        slider.setMappedValue(val);
                        slider.setProgrammaticChangeEvents(true);
                    }

                }
            }
            default -> {
            }
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

}
