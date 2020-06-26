package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnProgressBar;

public class VRSelectionGui extends AbstractGui {
    protected Container<Table> container;
    protected Table contents;
    protected OwnProgressBar progress;

    private boolean selectionState = false;
    private double selectionCompletion = 0d;

    public VRSelectionGui() {
        super();
    }


    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        float h = GlobalConf.screen.BACKBUFFER_HEIGHT;
        float w = GlobalConf.screen.BACKBUFFER_WIDTH;
        Viewport vp = new ScreenViewport();
        ui = new Stage(vp, GlobalResources.spriteBatch);
        vp.update((int) w, (int) h, true);
        skin = GlobalResources.skin;

        container = new Container<>();
        container.setFillParent(true);
        container.top().right();
        container.padTop((h / 3f));

        contents = new Table();
        contents.setFillParent(false);

        // Progress
        OwnLabel hold = new OwnLabel("Selecting object, hold button...", skin, "headline");
        progress = new OwnProgressBar(0, 100, 0.1f, false, skin, "default-horizontal");
        progress.setPrefWidth(w * 0.3f);

        contents.add(hold).top().padBottom(10).row();
        contents.add(progress);

        // Center
        container.padRight((w * 0.7f) / 2f - hoffset);

        container.setActor(contents);

        rebuildGui();

        EventManager.instance.subscribe(this, Events.VR_SELECTING_STATE);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    @Override
    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            if (container != null)
                ui.addActor(container);
        }
    }

    @Override
    public boolean mustDraw(){
        return selectionState;
    }


    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
            case VR_SELECTING_STATE:
                selectionState = (Boolean) data[0];
                selectionCompletion = (Double) data[1];

                if(selectionState && progress != null){
                    progress.setValue((float)(selectionCompletion * 100d));
                }

                break;
            default:
                break;
        }
    }
}
