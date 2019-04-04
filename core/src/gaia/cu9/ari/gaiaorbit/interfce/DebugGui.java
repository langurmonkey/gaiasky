/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

/**
 * This GUI shows debug information at the top-right corner of the screen
 * 
 * @author tsagrista
 *
 */
public class DebugGui extends AbstractGui {
    protected DebugInterface debugInterface;

    public DebugGui() {
        super();
    }

    @Override
    public void initialize(AssetManager assetManager) {
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        interfaces = new Array<>();
        skin = GlobalResources.skin;

        // DEBUG INFO - TOP RIGHT
        debugInterface = new DebugInterface(skin, lock);
        debugInterface.setFillParent(true);
        debugInterface.right().top();
        debugInterface.pad(5, 0, 0, 5);
        interfaces.add(debugInterface);

        rebuildGui();
    }

    @Override
    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            if (debugInterface != null)
                ui.addActor(debugInterface);
        }
    }

    @Override
    public boolean cancelTouchFocus() {
        return false;
    }

}
