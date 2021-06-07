/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.util.GlobalResources;

/**
 * This GUI shows debug information at the top-right corner of the screen
 */
public class DebugGui extends AbstractGui {
    protected DebugInterface debugInterface;
    private Container di;

    public DebugGui(Lwjgl3Graphics graphics, Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        this.ui = new Stage(vp, sb);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        float pad = 16f;
        skin = GlobalResources.skin;

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.right().top();
        di = new Container<>(debugInterface);
        di.setFillParent(true);
        di.right().top();
        di.pad(pad, 0, 0, pad);

        rebuildGui();
    }

    @Override
    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            if (debugInterface != null && di != null)
                ui.addActor(di);
        }
    }


    @Override
    public void update(double dt) {
        ui.act((float) dt);
    }

    @Override
    public boolean cancelTouchFocus() {
        return false;
    }

}
