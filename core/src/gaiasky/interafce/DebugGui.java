/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;

/**
 * This GUI shows debug information at the top-right corner of the screen
 * 
 * @author tsagrista
 *
 */
public class DebugGui extends AbstractGui {
    protected DebugInterface debugInterface;
    private Container di;

    public DebugGui() {
        super();
        this.ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
    }

    @Override
    public void initialize(AssetManager assetManager) {
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        float pad = 10 * GlobalConf.UI_SCALE_FACTOR;
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
