package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;

/**
 * GUI that is operated with a game controller and optimized for that purpose.
 */
public class ControllerGui extends AbstractGui {

    private final Table content, camT, timeT, optT, datT, visT;
    private Actor camera, time, options, datasets, visuals;

    public ControllerGui() {
        super();
        this.skin = GlobalResources.skin;
        content = new Table(skin);
        camT = new Table(skin);
        timeT = new Table(skin);
        optT = new Table(skin);
        datT = new Table(skin);
        visT = new Table(skin);
    }

    @Override
    protected void rebuildGui() {
        content.clearChildren();
        camT.clearChildren();
        timeT.clearChildren();
        datT.clearChildren();
        optT.clearChildren();
        visT.clearChildren();

        float padBig = 30f * GlobalConf.UI_SCALE_FACTOR;
        camera = new OwnLabel("Camera", skin, "main-title-s");
        camT.add(camera);

        time = new OwnLabel("Time", skin, "main-title-s");
        timeT.add(time);

        datasets = new OwnLabel("Datasets", skin, "main-title-s");
        datT.add(datasets);

        options = new OwnLabel("Options", skin, "main-title-s");
        optT.add(options);

        visuals = new OwnLabel("Visuals", skin, "main-title-s");
        visT.add(visuals);




        content.add(camT).center().padRight(padBig);
        content.add(timeT).center().padRight(padBig);
        content.add(datT).center().padRight(padBig);
        content.add(optT).center().padRight(padBig);
        content.add(visT).center().padRight(padBig);

        content.center();
        content.setFillParent(true);
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        Viewport vp = new ScreenViewport();
        ui = new Stage(vp, GlobalResources.spriteBatch);

        EventManager.instance.subscribe(this, Events.SHOW_CONTROLLER_GUI_ACTION);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        rebuildGui();
    }

    @Override
    public void notify(final Events event, final Object... data) {
        // Empty by default
        switch (event) {
            case SHOW_CONTROLLER_GUI_ACTION:

                if (content.isVisible()) {
                    // Hide and remove
                    content.setVisible(false);
                    content.remove();
                } else {
                    // Show
                    // Add and show
                    ui.addActor(content);

                    content.setVisible(true);
                }


                break;
            default:
                break;
        }
    }
}
