/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.interfce.ColorPicker;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The datasets pane in the controls window
 */
public class DatasetsComponent extends GuiComponent implements IObserver {
    private VerticalGroup group;
    private float pad = 3 * GlobalConf.UI_SCALE_FACTOR;

    private Map<String, WidgetGroup> groupMap;
    private Map<String, OwnImageButton[]> imageMap;

    public DatasetsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        groupMap = new HashMap<>();
        imageMap = new HashMap<>();
        EventManager.instance.subscribe(this, Events.CATALOG_ADD, Events.CATALOG_REMOVE, Events.CATALOG_VISIBLE, Events.CATALOG_HIGHLIGHT);
    }

    @Override
    public void initialize() {

        group = new VerticalGroup();
        group.space(pad * 3f);
        group.align(Align.left);

        Collection<CatalogInfo> cis = CatalogManager.instance().getCatalogInfos();
        if (cis != null) {
            Iterator<CatalogInfo> it = cis.iterator();
            while (it.hasNext()) {
                CatalogInfo ci = it.next();
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
        eye.setCheckedNoFire(!ci.isVisible());
        eye.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.toggle"), skin));
        eye.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Toggle visibility
                if (ci.object != null) {
                    boolean newvis = !ci.isVisible();
                    EventManager.instance.post(Events.CATALOG_VISIBLE, ci.name, newvis, true);
                }
                return true;
            }
            return false;
        });

        OwnImageButton mark = new OwnImageButton(skin, "highlight-ds-s");
        mark.setCheckedNoFire(ci.highlighted);
        mark.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.highlight"), skin));
        mark.addListener((event) -> {
            if(event instanceof ChangeEvent){
                EventManager.instance.post(Events.CATALOG_HIGHLIGHT, ci.name, mark.isChecked(), ci.color, true);
                return true;
            }
            return false;
        });

        OwnImageButton prefs = new OwnImageButton(skin, "prefs");
        prefs.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.preferences"), skin));
        prefs.addListener((event) -> {
            if(event instanceof ChangeEvent){
                return true;
            }
            return false;
        });

        ImageButton rubbish = new OwnImageButton(skin, "rubbish-bin");
        rubbish.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.remove"), skin));
        rubbish.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Remove dataset
                EventManager.instance.post(Events.CATALOG_REMOVE, ci.name);
                return true;
            }
            return false;
        });


        imageMap.put(ci.name, new OwnImageButton[]{eye, mark});
        controls.addActor(eye);
        controls.addActor(prefs);
        controls.addActor(rubbish);

        // Dataset table
        Table t = new Table();
        // Color picker
        ColorPicker cp = new ColorPicker(ci.name, ci.color, stage, skin);
        cp.addListener(new TextTooltip(I18n.txt("gui.tooltip.dataset.highlight.color.select"), skin));
        cp.setNewColorRunnable(()->{
            ci.setColor(cp.getPickedColor());
        });
        t.add(new OwnLabel(ci.name, skin, "hud-subheader")).left().padBottom(pad);
        t.add(cp).size(18f * GlobalConf.UI_SCALE_FACTOR).right().padBottom(pad).row();
        t.add(controls).left().padBottom(pad);
        t.add(mark).right().padBottom(pad).row();
        t.add(new OwnLabel(I18n.txt("gui.dataset.type") + ": " + ci.type.toString(), skin)).colspan(2).left().row();
        t.add(new OwnLabel(TextUtils.capString(ci.description, GlobalConf.UI_SCALE_FACTOR < 1.5 ? 22 : 28), skin)).left();
        Link plus = new Link("(i)",  skin.get("link", Label.LabelStyle.class), null);
        plus.addListener(new OwnTextTooltip(ci.description, skin));
        t.add(plus).left().padLeft(pad);

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
        scroll.setWidth(175 * GlobalConf.UI_SCALE_FACTOR);
        scroll.setHeight(GlobalConf.UI_SCALE_FACTOR > 1.5 ? 125 : 75);


        //ciGroup.addActor(controls);
        ciGroup.addActor(scroll);

        group.addActor(ciGroup);

        groupMap.put(ci.name, ciGroup);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CATALOG_ADD:
            addCatalogInfo((CatalogInfo) data[0]);
            break;
        case CATALOG_REMOVE:
            String ciName = (String) data[0];
            if(groupMap.containsKey(ciName)){
                groupMap.get(ciName).remove();
                groupMap.remove(ciName);
                imageMap.remove(ciName);
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
             if(data.length > 3)
                 ui = (Boolean) data[3];
             if(!ui){
                 ciName = (String) data[0];
                 boolean hl = (Boolean) data[1];
                 OwnImageButton hig = imageMap.get(ciName)[1];
                 hig.setCheckedNoFire(hl);
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
