/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.scene2d.OwnProgressBar;

public class LoadProgressInterface extends TableGuiInterface implements IObserver {

    private OwnProgressBar progress;

    public LoadProgressInterface(float width, Skin skin) {
        super(skin);
        build(width, skin);
        EventManager.instance.subscribe(this, Events.UPDATE_LOAD_PROGRESS);
    }

    private void build(float width, Skin skin){
        progress = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "small-horizontal");
        progress.setValue(0);
        progress.setPrefHeight(16f);
        progress.setPrefWidth(width);
        add(progress);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void update() {
    }

    @Override
    public void notify(Events event, Object... data) {
        switch(event){
        case UPDATE_LOAD_PROGRESS:
            float val = (Float) data[0];
            if(progress != null)
                progress.setValue(val * 100f);
            break;
        default:
            break;
        }

    }
}
