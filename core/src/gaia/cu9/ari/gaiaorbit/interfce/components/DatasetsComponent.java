package gaia.cu9.ari.gaiaorbit.interfce.components;

import java.util.Iterator;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.CatalogInfo;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;

public class DatasetsComponent extends GuiComponent implements IObserver {
    private static final Log logger = Logger.getLogger(DatasetsComponent.class);
    private VerticalGroup group;
    private float pad = 3 * GlobalConf.SCALE_FACTOR;

    public DatasetsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.ADD_CATALOG_INFO);
    }

    @Override
    public void initialize() {

        group = new VerticalGroup();
        group.space(pad);
        group.align(Align.left);

        Array<CatalogInfo> cis = CatalogInfo.catalogs;

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


        Table t = new Table();
        t.add(new OwnLabel(ci.name, skin, "hud-subheader")).left().row();
        t.add(new OwnLabel(txt("gui.dataset.type") + ": " + ci.type.toString(), skin)).left().row();
        t.add(new OwnLabel(ci.description, skin)).left().padBottom(pad).row();

        HorizontalGroup ciGroup = new HorizontalGroup();
        ciGroup.space(pad);

        // Info
        ScrollPane scroll = new OwnScrollPane(t, skin, "minimalist-nobg");
        scroll.setScrollingDisabled(false, true);
        scroll.setForceScroll(false, false);
        scroll.setFadeScrollBars(false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);
        scroll.setWidth(155 * GlobalConf.SCALE_FACTOR);
        scroll.setHeight(GlobalConf.SCALE_FACTOR > 1 ? 90 : 50);

        // Controls
        VerticalGroup controls = new VerticalGroup();
        controls.space(pad);
        ImageButton eye = new OwnImageButton(skin, "eye-toggle");
        eye.addListener(new TextTooltip(txt("gui.dataset.tooltip.toggle"), skin));
        eye.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Toggle visibility
                if (ci.object != null) {
                    boolean newvis = !ci.object.isVisible();
                    ci.object.setVisible(newvis);
                    logger.info(txt("notif.visibility." + (newvis ? "on" : "off"), ci.name));
                }
                return true;
            }
            return false;
        });
        ImageButton rubbish = new OwnImageButton(skin, "rubbish-bin");
        rubbish.addListener(new TextTooltip(txt("gui.dataset.tooltip.remove"), skin));
        rubbish.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                // Remove dataset
                ci.removeCatalog();
                ciGroup.remove();
                return true;
            }
            return false;
        });

        controls.addActor(eye);
        controls.addActor(rubbish);

        ciGroup.addActor(controls);
        ciGroup.addActor(scroll);

        group.addActor(ciGroup);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case ADD_CATALOG_INFO:
            addCatalogInfo((CatalogInfo) data[0]);
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
