/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;

public class GaiaComponent extends GuiComponent {

    protected CheckBox transitColor, onlyObservedStars, computeGaiaScan;

    public GaiaComponent(Skin skin, Stage stage) {
        super(skin, stage);
    }

    @Override
    public void initialize() {
        computeGaiaScan = new CheckBox(" " + I18n.txt("gui.gaiascan.enable"), skin);
        computeGaiaScan.setName("compute gaia scan");
        computeGaiaScan.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.COMPUTE_GAIA_SCAN_CMD, I18n.txt("gui.gaiascan.compute"), computeGaiaScan.isChecked());
                return true;
            }
            return false;
        });
        computeGaiaScan.setChecked(GlobalConf.scene.COMPUTE_GAIA_SCAN);

        transitColor = new CheckBox(" " + I18n.txt("gui.gaiascan.colour"), skin);
        transitColor.setName("transit color");
        transitColor.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.TRANSIT_COLOUR_CMD, I18n.txt("gui.gaiascan.transit"), transitColor.isChecked());
                return true;
            }
            return false;
        });
        transitColor.setChecked(GlobalConf.scene.STAR_COLOR_TRANSIT);

        //        onlyObservedStars = new CheckBox(I18n.txt("gui.gaiascan.onlyobserved"), skin);
        //        onlyObservedStars.setName("only observed stars");
        //        onlyObservedStars.addListener(new EventListener() {
        //            @Override
        //            public boolean handle(Event event) {
        //                if (event instanceof ChangeEvent) {
        //                    EventManager.instance.post(Events.ONLY_OBSERVED_STARS_CMD, I18n.txt("gui.gaiascan.only"), onlyObservedStars.isChecked());
        //                    return true;
        //                }
        //                return false;
        //            }
        //        });
        //        onlyObservedStars.setChecked(GlobalConf.scene.ONLY_OBSERVED_STARS);

        VerticalGroup gaiaGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        gaiaGroup.addActor(computeGaiaScan);
        gaiaGroup.addActor(transitColor);
        //gaiaGroup.addActor(onlyObservedStars);

        component = gaiaGroup;

    }

    @Override
    public void dispose() {
    }

}
