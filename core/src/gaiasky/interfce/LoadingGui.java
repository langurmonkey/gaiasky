/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalResources;
import gaiasky.util.I18n;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.StdRandom;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;

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
        } else {
            vp.update(GaiaSky.graphics.getWidth(), GaiaSky.graphics.getHeight(), true);
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

        OwnLabel gaiasky = new OwnLabel(GlobalConf.getApplicationTitle(vr), skin, "main-title");

        lastUpdateTime = 0;
        i = -1;
        m1 = BigInteger.ZERO;
        m2 = BigInteger.ZERO;
        OwnLabel loading = new OwnLabel(I18n.bundle.get("notif.loading.wait"), skin, "hud-header");
        spin = new OwnLabel("0", skin, "mono");
        spin.setColor(skin.getColor("theme"));

        center.add(gaiasky).center().padBottom(pad10 * 2f).row();
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


    private long waitTime = 1400;
    @Override
    public void update(double dt) {
        super.update(dt);
        // Fibonacci numbers
        long currTime = System.currentTimeMillis();
        if(currTime - lastUpdateTime > waitTime){
            randomFunnyText();
            lastUpdateTime = currTime;
            waitTime = StdRandom.uniform(800, 3000);
        }
    }

    /** Return the i fibonacci number **/
    private void fibonacci(){
        i++;
        BigInteger next;
        if(i == 0l){
            next = BigInteger.ZERO;
        }else if (i == 1l){
            next = BigInteger.ONE;
        } else {
            next = m1.add(m2);
        }
        m2 = m1;
        m1 = next;

        spin.setText(next.toString());
    }

    private final String[] loadingTexts = new String[]{
            "Looking for more stars...",
            "Initializing flux capacitor...",
            "Setting up continuum transfunctioner...",
            "Pumping up atmospheres...",
            "Creating more rocky planets...",
            "Downloading RAM modules...",
            "Fitting GPU performance curves...",
            "Tracing planetary orbits...",
            "Dodging asteroids...",
            "Introducing molecular clouds...",
            "Modelling dust maps...",
            "Caching distant galaxies...",
            "Listening to the sound of space...",
            "Following lonely star...",
            "Restoring Vulcan planet...",
            "Tuning gravity strength...",
            "Adjusting fundamental physical constants...",
            "Assigning moons to planets...",
            "Registering extraterrestrials...",
            "Synchronizing spatiotemporal serendipities...",
            "Extracting anomalous materials...",
            "Computing interesting manifolds..."
    };
    private void randomFunnyText(){
        spin.setText(loadingTexts[StdRandom.uniform(loadingTexts.length)]);
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
