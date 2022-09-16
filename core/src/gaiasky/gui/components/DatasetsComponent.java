/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.*;
import gaiasky.scene.Mapper;
import gaiasky.scenegraph.IVisibilitySwitch;
import gaiasky.scenegraph.MeshObject;
import gaiasky.util.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The datasets pane in the controls window
 */
public class DatasetsComponent extends GuiComponent implements IObserver {
    private VerticalGroup group;
    private final float pad = 4.8f;

    private final Map<String, WidgetGroup> groupMap;
    private final Map<String, OwnImageButton[]> imageMap;
    private final Map<String, ColorPickerAbstract> colorMap;
    private final Map<String, DatasetPreferencesWindow> prefsMap;
    private final Map<String, OwnSliderPlus> scalingMap;
    private final CatalogManager catalogManager;

    private OwnLabel noDatasetsLabel = null;

    public DatasetsComponent(final Skin skin, final Stage stage, final CatalogManager catalogManager) {
        super(skin, stage);
        this.catalogManager = catalogManager;
        groupMap = new HashMap<>();
        imageMap = new HashMap<>();
        colorMap = new HashMap<>();
        prefsMap = new HashMap<>();
        scalingMap = new HashMap<>();
        EventManager.instance.subscribe(this, Event.CATALOG_ADD, Event.CATALOG_REMOVE, Event.CATALOG_VISIBLE, Event.CATALOG_HIGHLIGHT, Event.CATALOG_POINT_SIZE_SCALING_CMD);
    }

    @Override
    public void initialize() {

        group = new VerticalGroup();
        group.columnAlign(Align.left);

        Collection<CatalogInfo> cis = this.catalogManager.getCatalogInfos();
        if (cis != null) {
            for (CatalogInfo ci : cis) {
                addCatalogInfo(ci);
            }
        }

        component = group;

        addNoDatasets();
    }

    private void setDatasetVisibility(CatalogInfo ci, OwnImageButton eye, boolean visible, Actor source) {
        if (ci.object != null) {
            if (source != eye) {
                eye.setCheckedNoFire(!visible);
            }
            EventManager.publish(Event.CATALOG_VISIBLE, this, ci.name, visible);
            if (ci.object instanceof MeshObject) {
                EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, this, ci.object, ci.object.getName(), visible);
            }
        }

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

    private void setDatasetHighlight(CatalogInfo ci, OwnImageButton mark, boolean highlight, Actor source) {
        if (ci.object != null) {
            if (source != mark) {
                mark.setCheckedNoFire(highlight);
            }
            EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight);
        }
        if (ci.entity != null) {
            if (source != mark) {
                mark.setCheckedNoFire(highlight);
            }
            EventManager.publish(Event.CATALOG_HIGHLIGHT, this, ci, highlight);
        }
    }

    private void showDatasetPreferences(CatalogInfo ci) {
        DatasetPreferencesWindow dpw;
        if (prefsMap.containsKey(ci.name)) {
            dpw = prefsMap.get(ci.name);
        } else {
            dpw = new DatasetPreferencesWindow(ci, skin, stage);
            prefsMap.put(ci.name, dpw);
        }
        if (!dpw.isVisible() || !dpw.hasParent()) {
            dpw.setVisible(true);
            dpw.show(stage);
        }
    }

    private void addCatalogInfo(CatalogInfo ci) {
        // Controls
        HorizontalGroup controls = new HorizontalGroup();
        controls.space(pad6);
        OwnImageButton eye = new OwnImageButton(skin, "eye-toggle");
        eye.setCheckedNoFire(!ci.isVisible(true));
        eye.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.toggle"), skin));
        eye.addListener(event -> {
            if (event instanceof ChangeEvent) {
                setDatasetVisibility(ci, eye, !ci.isVisible(true), eye);
                return true;
            }
            return false;
        });

        OwnImageButton mark = new OwnImageButton(skin, "highlight-ds-s");
        mark.setCheckedNoFire(ci.highlighted);
        mark.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.highlight"), skin));
        mark.addListener(event -> {
            if (event instanceof ChangeEvent) {
                setDatasetHighlight(ci, mark, mark.isChecked(), mark);
                return true;
            }
            return false;
        });

        OwnImageButton prefs = new OwnImageButton(skin, "prefs");
        prefs.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.preferences"), skin));
        prefs.addListener(event -> {
            if (event instanceof ChangeEvent) {
                showDatasetPreferences(ci);
                return true;
            }
            return false;
        });

        ImageButton rubbish = new OwnImageButton(skin, "rubbish-bin");
        rubbish.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.remove"), skin));
        rubbish.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Remove dataset
                EventManager.publish(Event.CATALOG_REMOVE, this, ci.name);
                return true;
            }
            return false;
        });

        imageMap.put(ci.name, new OwnImageButton[] { eye, mark });
        controls.addActor(eye);
        if (ci.isHighlightable()) {
            controls.addActor(mark);
            controls.addActor(prefs);
        }
        controls.addActor(rubbish);

        // Dataset table
        Table t = new Table(skin);
        t.align(Align.topLeft);
        // Color picker
        ColorPickerAbstract cp;
        if (ci.hasParticleAttributes()) {
            ColormapPicker cmp = new ColormapPicker(ci.name, ci.hlColor, ci, stage, skin);
            cmp.addListener(new TextTooltip(I18n.msg("gui.tooltip.dataset.highlight.color.select"), skin));
            cmp.setNewColorRunnable(() -> ci.setHlColor(cmp.getPickedColor()));
            cmp.setNewColormapRunnable(() -> ci.setHlColormap(cmp.getPickedCmapIndex(), cmp.getPickedCmapAttribute(), cmp.getPickedCmapMin(), cmp.getPickedCmapMax()));
            cp = cmp;
        } else {
            ColorPicker clp = new ColorPicker(ci.name, ci.hlColor, stage, skin);
            clp.addListener(new TextTooltip(I18n.msg("gui.tooltip.dataset.highlight.color.select"), skin));
            clp.setNewColorRunnable(() -> ci.setHlColor(clp.getPickedColor()));
            cp = clp;
        }
        colorMap.put(ci.name, cp);

        OwnLabel nameLabel = new OwnLabel(TextUtils.capString(ci.name, 26), skin, "hud-subheader");
        nameLabel.addListener(new OwnTextTooltip(ci.name, skin));

        if (ci.isHighlightable()) {
            t.add(controls).left().padBottom(pad);
            t.add(cp).right().size(28.8f).padRight(pad6).padBottom(pad).row();
        } else {
            t.add(controls).colspan(2).left().padBottom(pad).row();
        }
        int cap = 25;
        String types = ci.type.toString() + " / " + ci.getCt();
        OwnLabel typesLabel = new OwnLabel(TextUtils.capString(types, cap), skin);
        typesLabel.addListener(new OwnTextTooltip(types, skin));
        t.add(typesLabel).colspan(2).left().row();
        String description = ci.description != null ? ci.description : "";
        OwnLabel desc = new OwnLabel(TextUtils.capString(description, cap), skin);
        desc.addListener(new OwnTextTooltip(description, skin));
        t.add(desc).left().expandX();
        Link info = new Link("(i)", skin.get("link", Label.LabelStyle.class), null);
        info.addListener(new OwnTextTooltip(description, skin));
        t.add(info).left().padLeft(pad);

        if (ci.nParticles > 0) {
            t.row();
            OwnLabel nObjects = new OwnLabel(I18n.msg("gui.objects") + ": " + ci.nParticles, skin);
            String bytes = ci.sizeBytes > 0 ? I18n.msg("gui.size") + ": " + GlobalResources.humanReadableByteCount(ci.sizeBytes, true) : "";
            nObjects.addListener(new OwnTextTooltip(nObjects.getText() + ", " + bytes, skin));
            t.add(nObjects).left();
        }

        if (ci.isHighlightable()) {
            OwnSliderPlus sizeScaling = new OwnSliderPlus(I18n.msg("gui.dataset.size"), Constants.MIN_POINT_SIZE_SCALE, Constants.MAX_POINT_SIZE_SCALE, Constants.SLIDER_STEP_TINY, skin);
            sizeScaling.setWidth(320f);
            if (ci.object != null) {
                sizeScaling.setMappedValue(ci.object.getPointscaling());
            } else if (ci.entity != null) {
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
        Container<Table> c = new Container<>(t);
        c.setFillParent(true);
        c.align(Align.topLeft);
        c.minHeight(80f);
        c.width(ControlsWindow.getContentWidth() * 0.94f);

        // Info
        ScrollPane scroll = new OwnScrollPane(c, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(false, true);
        scroll.setForceScroll(false, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);
        scroll.setWidth(ControlsWindow.getContentWidth() * 0.94f);

        CollapsibleEntry catalogWidget = new CollapsibleEntry(nameLabel, scroll, skin);
        catalogWidget.align(Align.topLeft);
        catalogWidget.pad(pad9);
        catalogWidget.setWidth(ControlsWindow.getContentWidth() * 0.94f);
        catalogWidget.setExpandRunnable(() -> EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this));
        catalogWidget.setCollapseRunnable(() -> EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this));
        catalogWidget.addListener(new InputListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    InputEvent.Type type = ie.getType();
                    if (type == InputEvent.Type.touchDown) {
                        if (ie.getButton() == Input.Buttons.RIGHT) {
                            GaiaSky.postRunnable(() -> {
                                // Context menu
                                ContextMenu datasetContext = new ContextMenu(skin, "default");
                                // Visibility
                                boolean currentVisibility = ci.isVisible(true);
                                MenuItem visibility = new MenuItem(I18n.msg(currentVisibility ? "gui.hide" : "gui.show"), skin, skin.getDrawable(currentVisibility ? "eye-s-off" : "eye-s-on"));
                                visibility.addListener(new ChangeListener() {
                                    @Override
                                    public void changed(ChangeEvent event, Actor actor) {
                                        setDatasetVisibility(ci, eye, !currentVisibility, catalogWidget);
                                    }
                                });
                                datasetContext.addItem(visibility);
                                if (ci.isHighlightable()) {
                                    // Highlight
                                    boolean currentHighlight = ci.highlighted;
                                    MenuItem highlight = new MenuItem(I18n.msg(currentHighlight ? "gui.deemphasize" : "gui.highlight"), skin, skin.getDrawable(currentHighlight ? "highlight-s-off" : "highlight-s-on"));
                                    highlight.addListener(new ChangeListener() {
                                        @Override
                                        public void changed(ChangeEvent event, Actor actor) {
                                            setDatasetHighlight(ci, mark, !currentHighlight, catalogWidget);
                                        }
                                    });
                                    datasetContext.addItem(highlight);
                                    // Settings
                                    MenuItem settings = new MenuItem(I18n.msg("gui.settings.dataset"), skin, skin.getDrawable("prefs-icon"));
                                    settings.addListener(new ChangeListener() {
                                        @Override
                                        public void changed(ChangeEvent event, Actor actor) {
                                            showDatasetPreferences(ci);
                                        }
                                    });
                                    datasetContext.addItem(settings);
                                }
                                // Delete
                                MenuItem delete = new MenuItem(I18n.msg("gui.download.delete"), skin, skin.getDrawable("iconic-trash"));
                                delete.addListener(new ChangeListener() {
                                    @Override
                                    public void changed(ChangeEvent event, Actor actor) {
                                        EventManager.publish(Event.CATALOG_REMOVE, this, ci.name);
                                    }
                                });
                                datasetContext.addItem(delete);
                                datasetContext.showMenu(stage, Gdx.input.getX(ie.getPointer()) / Settings.settings.program.ui.scale, stage.getHeight() - Gdx.input.getY(ie.getPointer()) / Settings.settings.program.ui.scale);
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

        group.addActor(catalogWidget);

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
            noDatasetsLabel.setWidth(ControlsWindow.getContentWidth() * 0.94f);
            noDatasetsLabel.setAlignment(Align.left, Align.left);
            group.addActor(noDatasetsLabel);
            group.pack();
        }
    }

    @Override
    public void notify(final gaiasky.event.Event event, Object source, final Object... data) {
        switch (event) {
        case CATALOG_ADD:
            removeNoDatasets();
            addCatalogInfo((CatalogInfo) data[0]);
            break;
        case CATALOG_REMOVE:
            String ciName = (String) data[0];
            if (groupMap.containsKey(ciName)) {
                groupMap.get(ciName).remove();
                groupMap.remove(ciName);
                imageMap.remove(ciName);
                colorMap.remove(ciName);
                EventManager.publish(Event.RECALCULATE_CONTROLS_WINDOW_SIZE, this);
            }
            addNoDatasets();
            break;
        case CATALOG_VISIBLE:
            boolean ui = source == this;
            if (!ui) {
                ciName = (String) data[0];
                boolean visible = (Boolean) data[1];
                OwnImageButton eye = imageMap.get(ciName)[0];
                eye.setCheckedNoFire(!visible);
            }
            break;
        case PER_OBJECT_VISIBILITY_CMD:
            ui = source == this;
            if (!ui) {
                IVisibilitySwitch obj = (IVisibilitySwitch) data[0];
                String name = (String) data[1];
                boolean checked = (Boolean) data[2];
                if (obj instanceof MeshObject) {
                    OwnImageButton eye = imageMap.get(name)[0];
                    eye.setCheckedNoFire(!checked);
                }
            }
            break;
        case CATALOG_HIGHLIGHT:
            ui = source == this;
            if (!ui) {
                CatalogInfo ci = (CatalogInfo) data[0];
                float[] col = ci.hlColor;
                if (colorMap.containsKey(ci.name) && col != null) {
                    colorMap.get(ci.name).setPickedColor(col);
                }

                if (imageMap.containsKey(ci.name)) {
                    boolean hl = (Boolean) data[1];
                    OwnImageButton hig = imageMap.get(ci.name)[1];
                    hig.setCheckedNoFire(hl);
                }
            }
            break;
        case CATALOG_POINT_SIZE_SCALING_CMD:
            ui = source == this;
            if (!ui) {
                ciName = (String) data[0];
                double val = (Double) data[1];
                if (scalingMap.containsKey(ciName)) {
                    OwnSliderPlus slider = scalingMap.get(ciName);
                    slider.setProgrammaticChangeEvents(false);
                    slider.setMappedValue(val);
                    slider.setProgrammaticChangeEvents(true);
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
