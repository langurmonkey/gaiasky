/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ColormapPicker;
import gaiasky.interafce.ControlsWindow;
import gaiasky.interafce.DatasetPreferencesWindow;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.util.*;
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
    private final Map<String, ColormapPicker> colorMap;
    private final CatalogManager catalogManager;

    public DatasetsComponent(final Skin skin, final Stage stage, final CatalogManager catalogManager) {
        super(skin, stage);
        this.catalogManager = catalogManager;
        groupMap = new HashMap<>();
        imageMap = new HashMap<>();
        colorMap = new HashMap<>();
        EventManager.instance.subscribe(this, Events.CATALOG_ADD, Events.CATALOG_REMOVE, Events.CATALOG_VISIBLE, Events.CATALOG_HIGHLIGHT);
    }

    @Override
    public void initialize() {

        group = new VerticalGroup();
        group.space(pad * 3f);
        group.align(Align.left);

        Collection<CatalogInfo> cis = this.catalogManager.getCatalogInfos();
        if (cis != null) {
            for (CatalogInfo ci : cis) {
                addCatalogInfo(ci);
            }
        }

        component = group;
    }

    private void addCatalogInfo(CatalogInfo ci) {
        // Controls
        HorizontalGroup controls = new HorizontalGroup();
        controls.space(pad);
        OwnImageButton eye = new OwnImageButton(skin, "eye-toggle");
        eye.setCheckedNoFire(!ci.isVisible(true));
        eye.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.toggle"), skin));
        eye.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Toggle visibility
                if (ci.object != null) {
                    boolean visible = !ci.isVisible(true);
                    EventManager.instance.post(Events.CATALOG_VISIBLE, ci.name, visible, true);
                }
                return true;
            }
            return false;
        });

        OwnImageButton mark = new OwnImageButton(skin, "highlight-ds-s");
        mark.setCheckedNoFire(ci.highlighted);
        mark.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.highlight"), skin));
        mark.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CATALOG_HIGHLIGHT, ci, mark.isChecked(), true);
                return true;
            }
            return false;
        });

        OwnImageButton prefs = new OwnImageButton(skin, "prefs");
        prefs.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.preferences"), skin));
        prefs.addListener(event -> {
            if (event instanceof ChangeEvent) {
                DatasetPreferencesWindow dpw = new DatasetPreferencesWindow(ci, skin, stage);
                dpw.show(stage);
                return true;
            }
            return false;
        });

        ImageButton rubbish = new OwnImageButton(skin, "rubbish-bin");
        rubbish.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.remove"), skin));
        rubbish.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Remove dataset
                EventManager.instance.post(Events.CATALOG_REMOVE, ci.name);
                return true;
            }
            return false;
        });

        imageMap.put(ci.name, new OwnImageButton[] { eye, mark });
        controls.addActor(eye);
        if (ci.isRegular())
            controls.addActor(prefs);
        controls.addActor(rubbish);

        // Dataset table
        Table t = new Table(skin);
        // Color picker
        ColormapPicker cp = new ColormapPicker(ci.name, ci.hlColor, ci, stage, skin);
        cp.addListener(new TextTooltip(I18n.txt("gui.tooltip.dataset.highlight.color.select"), skin));
        cp.setNewColorRunnable(() -> ci.setHlColor(cp.getPickedColor()));
        cp.setNewColormapRunnable(() -> ci.setHlColormap(cp.getPickedCmapIndex(), cp.getPickedCmapAttribute(), cp.getPickedCmapMin(), cp.getPickedCmapMax()));
        colorMap.put(ci.name, cp);

        OwnLabel nameLabel = new OwnLabel(TextUtils.capString(ci.name, 26), skin, "hud-subheader");
        nameLabel.addListener(new OwnTextTooltip(ci.name, skin));
        if (ci.isRegular()) {
            t.add(nameLabel).left().padBottom(pad);
            t.add(cp).size(28.8f).right().padBottom(pad).row();
        } else {
            t.add(nameLabel).left().padBottom(pad);
            t.add().size(28.8f).right().padBottom(pad).row();
        }
        if (ci.isRegular()) {
            t.add(controls).left().padBottom(pad);
            t.add(mark).right().padBottom(pad).row();
        } else {
            t.add(controls).colspan(2).left().padBottom(pad).row();
        }
        int cap = 25;
        String types = ci.type.toString() + " / " + ci.object.ct.toString();
        OwnLabel typesLabel = new OwnLabel(TextUtils.capString(types, cap), skin);
        typesLabel.addListener(new OwnTextTooltip(types, skin));
        t.add(typesLabel).colspan(2).left().row();
        OwnLabel desc = new OwnLabel(TextUtils.capString(ci.description, cap), skin);
        desc.addListener(new OwnTextTooltip(ci.description, skin));
        t.add(desc).left().expandX();
        Link info = new Link("(i)", skin.get("link", Label.LabelStyle.class), null);
        info.addListener(new OwnTextTooltip(ci.description, skin));
        t.add(info).left().padLeft(pad);

        if (ci.nParticles > 0) {
            t.row();
            OwnLabel nObjects = new OwnLabel(I18n.txt("gui.objects") + ": " + ci.nParticles, skin);
            String bytes = ci.sizeBytes > 0 ? I18n.txt("gui.size") + ": " + GlobalResources.humanReadableByteCount(ci.sizeBytes, true) : "";
            nObjects.addListener(new OwnTextTooltip(nObjects.getText() + ", " + bytes, skin));
            t.add(nObjects).left();
        }

        if (ci.object instanceof ParticleGroup) {
            OwnSliderPlus sizeScaling = new OwnSliderPlus(I18n.txt("gui.dataset.size"), Constants.MIN_POINT_SIZE_SCALE, Constants.MAX_POINT_SIZE_SCALE, Constants.SLIDER_STEP_TINY, skin);
            sizeScaling.setName("star brightness");
            sizeScaling.setWidth(320f);
            sizeScaling.setMappedValue(ci.object.getPointscaling());
            sizeScaling.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    float val = sizeScaling.getMappedValue();
                    ci.object.setPointscaling(val);
                }
                return false;
            });
            t.row();
            t.add(sizeScaling).left().colspan(2).padTop(pad9);
        }

        VerticalGroup ciGroup = new VerticalGroup();
        ciGroup.space(pad * 2f);
        ciGroup.align(Align.left);

        // Info
        ScrollPane scroll = new OwnScrollPane(t, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(false, true);
        scroll.setForceScroll(false, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);
        scroll.setWidth(ControlsWindow.getContentWidth() * 0.96f);
        scroll.setHeight(170f);

        ciGroup.addActor(scroll);

        group.addActor(ciGroup);

        groupMap.put(ci.name, ciGroup);
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case CATALOG_ADD:
            addCatalogInfo((CatalogInfo) data[0]);
            break;
        case CATALOG_REMOVE:
            String ciName = (String) data[0];
            if (groupMap.containsKey(ciName)) {
                groupMap.get(ciName).remove();
                groupMap.remove(ciName);
                imageMap.remove(ciName);
                colorMap.remove(ciName);
                EventManager.instance.post(Events.RECALCULATE_OPTIONS_SIZE);
            }
            break;
        case CATALOG_VISIBLE:
            boolean ui = false;
            if (data.length > 2)
                ui = (Boolean) data[2];
            if (!ui) {
                ciName = (String) data[0];
                boolean visible = (Boolean) data[1];
                OwnImageButton eye = imageMap.get(ciName)[0];
                eye.setCheckedNoFire(!visible);
            }
            break;
        case CATALOG_HIGHLIGHT:
            ui = false;
            if (data.length > 2)
                ui = (Boolean) data[2];
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
        default:
            break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

}
