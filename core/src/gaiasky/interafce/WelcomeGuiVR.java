package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;

public class WelcomeGuiVR extends AbstractGui {

    private Table center;

    public WelcomeGuiVR(Lwjgl3Graphics graphics, Float unitsPerPixel){
        super(graphics, unitsPerPixel);
    }
    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);
        skin = GlobalResources.skin;

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
