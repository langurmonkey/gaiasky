package gaiasky.interafce;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Settings;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnProgressBar;

public class VRSelectionGui extends AbstractGui {
    protected Container<Table> container;
    protected Table contents;
    protected OwnProgressBar progress;

    private boolean selectionState = false;
    private double selectionCompletion = 0d;

    public VRSelectionGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final Boolean vr) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        float ow = Settings.settings.graphics.backBufferResolution[0];
        float oh = Settings.settings.graphics.backBufferResolution[1];
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);

        container = new Container<>();
        container.setFillParent(true);
        container.top().right();
        container.padTop(oh / 2f);

        contents = new Table();
        contents.setFillParent(false);

        // Progress
        OwnLabel hold = new OwnLabel("Selecting object, hold button...", skin, "headline");
        progress = new OwnProgressBar(0, 100, 0.1f, false, skin, "default-horizontal");
        progress.setPrefWidth(ow * 0.3f);

        contents.add(hold).top().padBottom(10).row();
        contents.add(progress);

        // Center
        container.padRight(ow * 0.7f / 2f - hoffset);

        container.setActor(contents);

        rebuildGui();

        EventManager.instance.subscribe(this, Event.VR_SELECTING_STATE);
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
    public boolean mustDraw() {
        return selectionState;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case VR_SELECTING_STATE:
            selectionState = (Boolean) data[0];
            selectionCompletion = (Double) data[1];

            if (selectionState && progress != null) {
                progress.setValue((float) (selectionCompletion * 100d));
            }

            break;
        default:
            break;
        }
    }
}
