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
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

import java.math.BigInteger;

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
    private BigInteger m1, m2;
    private long i;
    private long lastUpdateTime;

    public LoadingGui() {
        this(0, false);
    }

    public LoadingGui(Boolean vr) {
        this(0, vr);
    }

    public LoadingGui(Integer hoffset, Boolean vr) {
        super();
        this.vr = vr;
        this.hoffset = hoffset;
    }

    @Override
    public void initialize(AssetManager assetManager) {
        interfaces = new Array<>();
        float pad30 = 30f * GlobalConf.UI_SCALE_FACTOR;
        float pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
        float pad05 = 5f * GlobalConf.UI_SCALE_FACTOR;
        // User interface
        Viewport vp = new ScreenViewport();
        ui = new Stage(vp, GlobalResources.spriteBatch);
        if(vr) {
            vp.update(GlobalConf.screen.BACKBUFFER_WIDTH, GlobalConf.screen.BACKBUFFER_HEIGHT, true);
        }
        skin = GlobalResources.skin;

        center = new Table();
        center.setFillParent(true);
        center.center();
        if (hoffset > 0)
            center.padLeft(hoffset);
        else if (hoffset < 0)
            center.padRight(-hoffset);

        bottom = new Table();
        bottom.setFillParent(true);
        bottom.right().bottom();
        bottom.pad(pad10);

        FileHandle gslogo = Gdx.files.internal(vr ? "img/gaiasky-vr-logo-s.png" : (GlobalConf.UI_SCALE_FACTOR > 1f ? "img/gaiasky-logo.png" : "img/gaiasky-logo-s.png"));
        Texture logotex = new Texture(gslogo);
        logotex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        Image logoimg = new Image(logotex);
        logoimg.setOrigin(Align.center);

        lastUpdateTime = 0;
        i = -1;
        m1 = BigInteger.ZERO;
        m2 = BigInteger.ZERO;
        OwnLabel loading = new OwnLabel(I18n.bundle.get("notif.loading.wait"), skin, "hud-header");
        spin = new OwnLabel("0", skin, "mono");
        spin.setColor(skin.getColor("theme"));

        center.add(logoimg).center().padBottom(pad10).row();
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
        if(currTime - lastUpdateTime > 200){
            i++;
            BigInteger next;
            if(i == 0l){
                next = BigInteger.ZERO;
            }else if (i == 1l){
                next = BigInteger.ONE;
            } else {
                next = m1.add(m2);
            }
            spin.setText(next.toString());
            m2 = m1;
            m1 = next;



            lastUpdateTime = currTime;
        }
    }

    private void reset(){
        i = 0l;
        m1 = BigInteger.ZERO;
        m2 = BigInteger.ZERO;
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
