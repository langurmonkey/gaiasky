/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.GlobalResources;
import gaiasky.util.scene2d.OwnLabel;

public class GaiaSkyUI implements ApplicationListener, IObserver {

    private GaiaSky gaiasky;
    private Skin skin;
    private Stage ui;
    private Lwjgl3Window window;


    public GaiaSkyUI(GaiaSky gaiasky){
        super();
        this.gaiasky = gaiasky;
        this.skin = GlobalResources.skin;
        EventManager.instance.subscribe(this, Events.INITIALIZED_INFO);
    }

    public void setWindow(Lwjgl3Window window){
       this.window = window;
    }

    @Override
    public void create() {

    }

    @Override
    public void resize(int width, int height) {
        System.out.println("Resize: " + width + "x" + height);
        if(ui != null){
            ui.getViewport().update(width, height, true);
        }
    }

    @Override
    public void render() {
        if(ui != null){
            ui.act(Gdx.graphics.getDeltaTime());
            ui.getViewport().apply();
            ui.draw();
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(Events event, Object... data) {
        switch(event){
        case INITIALIZED_INFO:
            // Initialize full gui
            Stage ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
            OwnLabel l = new OwnLabel("HEY THIS IS DRAWN!", skin);
            l.setColor(1, 1, 1, 1);
            Container<OwnLabel> c = new Container<>(l);
            c.top().left();
            c.setFillParent(true);

            ui.addActor(c);

            this.ui = ui;
            break;
        default:
            break;
        }
    }
}
