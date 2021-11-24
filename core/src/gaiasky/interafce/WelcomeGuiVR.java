package gaiasky.interafce;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.util.scene2d.OwnLabel;

public class WelcomeGuiVR extends AbstractGui {

    private Table center;

    public WelcomeGuiVR(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final Boolean vr){
        super(graphics, unitsPerPixel);
        this.skin = skin;
    }
    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);

        center = new Table();
        center.setFillParent(true);
        center.center();
        if (hoffset > 0)
            center.padLeft(hoffset);
        else if (hoffset < 0)
            center.padRight(-hoffset);
        center.add(new OwnLabel("Please, check the window on your screen", skin, "main-title-s"));

        rebuildGui();
    }

    @Override
    public void doneLoading(AssetManager assetManager) {

    }

    @Override
    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            ui.addActor(center);
        }
    }
}
