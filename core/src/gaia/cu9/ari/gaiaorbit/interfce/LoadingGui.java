/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

/**
 * Displays the loading screen.
 * 
 * @author Toni Sagrista
 *
 */
public class LoadingGui extends AbstractGui {
    protected Table center, bottom;
    protected Container<Button> screenMode;

    public NotificationsInterface notificationsInterface;
    private OwnLabel spin;
    private long m1, m2, i;
    private long lastUpdateTime;

    public LoadingGui() {
        super();
    }

    @Override
    public void initialize(AssetManager assetManager) {
        interfaces = new Array<>();
        float pad30 = 30f * GlobalConf.SCALE_FACTOR;
        float pad10 = 10f * GlobalConf.SCALE_FACTOR;
        float pad05 = 5f * GlobalConf.SCALE_FACTOR;
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
        skin = GlobalResources.skin;

        center = new Table();
        center.setFillParent(true);
        center.center();
        
        bottom = new Table();
        bottom.setFillParent(true);
        bottom.right().bottom();
        bottom.pad(pad10);

        FileHandle gslogo = Gdx.files.internal("img/gaiasky-logo.png");
        Texture logotex = new Texture(gslogo);
        logotex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image logoimg = new Image(logotex);
        float scl = GlobalConf.SCALE_FACTOR / 2.5f;
        logoimg.setScale(scl);
        logoimg.setOrigin(Align.center);

        lastUpdateTime = 0;
        i = -1;
        m1 = 0;
        m2 = 0;
        OwnLabel loading = new OwnLabel(I18n.bundle.get("notif.loading.wait"), skin, "hud-header");
        spin = new OwnLabel("0", skin, "mono");
        spin.setColor(skin.getColor("theme"));

        center.add(logoimg).center();
        center.row();
        center.add(loading).padBottom(pad05).row();
        center.add(spin).padBottom(pad30).row();

        bottom.add(new OwnLabel(GlobalConf.version.version + " - build " + GlobalConf.version.build, skin, "hud-med"));
        
        // SCREEN MODE BUTTON - TOP RIGHT
        screenMode = new Container<>();
        screenMode.setFillParent(true);
        screenMode.top().right();
        screenMode.pad(pad10);
        OwnTextIconButton screenModeButton = new OwnTextIconButton("", skin, "screen-mode");
        screenModeButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                GlobalConf.screen.FULLSCREEN = !GlobalConf.screen.FULLSCREEN;
                EventManager.instance.post(Events.SCREEN_MODE_CMD);
                return true;
            }
            return false;
        });
        screenMode.setActor(screenModeButton);

        // MESSAGE INTERFACE - BOTTOM
        notificationsInterface = new NotificationsInterface(skin, lock, false, false, false, false);
        center.add(notificationsInterface);
        
        interfaces.add(notificationsInterface);

        rebuildGui();

    }

    @Override
    public void update(double dt) {
        super.update(dt);
        // Fibonacci numbers
        long currTime = System.currentTimeMillis();
        if(currTime - lastUpdateTime > 100){
            i++;
            long next;
            if(i == 0){
                next = 0;
            }else if (i == 1){
                next = 1;
            } else {
                next = m1 + m2;
            }
            spin.setText(Long.toString(next));
            m2 = m1;
            m1 = next;

            if(next > 1e25){
                reset();
            }


            lastUpdateTime = currTime;
        }
    }

    private void reset(){
        i = 0;
        m1 = 0;
        m2 = 0;
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    public void rebuildGui() {
        if (ui != null) {
            ui.clear();
            ui.addActor(screenMode);
            ui.addActor(center);
            ui.addActor(bottom);
        }
    }

}
