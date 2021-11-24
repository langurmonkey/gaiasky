/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Settings.StereoProfile;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;

/**
 * Full OpenGL GUI with all the controls and whistles.
 */
public class StereoGui extends AbstractGui {
    private Skin skin;

    protected NotificationsInterface notificationsOne, notificationsTwo;
    protected CustomInterface customInterface;

    protected INumberFormat nf;

    public StereoGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
    }

    public void initialize(final AssetManager assetManager, final SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);
    }

    /**
     * Constructs the interface
     */
    public void doneLoading(AssetManager assetManager) {
        Logger.getLogger(this.getClass()).info(I18n.txt("notif.gui.init"));

        interfaces = new Array<>();

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, Events.STEREO_PROFILE_CMD);
    }

    private void buildGui() {
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }

        nf = NumberFormatFactory.getFormatter("##0.###");

        // NOTIFICATIONS ONE - BOTTOM LEFT
        notificationsOne = new NotificationsInterface(skin, lock, true, true, false, false);
        notificationsOne.setFillParent(true);
        notificationsOne.left().bottom();
        notificationsOne.pad(0, 5, 5, 0);
        interfaces.add(notificationsOne);

        // NOTIFICATIONS TWO - BOTTOM CENTRE
        notificationsTwo = new NotificationsInterface(skin, lock, true, true, false, false);
        notificationsTwo.setFillParent(true);
        notificationsTwo.bottom();
        notificationsTwo.setX(Gdx.graphics.getWidth() / 2);
        notificationsTwo.pad(0, 5, 5, 0);
        interfaces.add(notificationsTwo);

        // CUSTOM MESSAGES
        customInterface = new CustomInterface(ui, skin, lock);
        interfaces.add(customInterface);

        /** ADD TO UI **/
        rebuildGui();

    }

    protected void rebuildGui() {

        if (ui != null) {
            ui.clear();
            if (notificationsOne != null)
                ui.addActor(notificationsOne);
            if (notificationsTwo != null)
                ui.addActor(notificationsTwo);

        }
    }

    /**
     * Removes the focus from this Gui and returns true if the focus was in the
     * GUI, false otherwise.
     *
     * @return true if the focus was in the GUI, false otherwise.
     */
    public boolean cancelTouchFocus() {
        if (ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public void update(double dt) {
        notificationsTwo.setX(notificationsTwo.getMessagesWidth() / 2);
        ui.act((float) dt);
    }

    @Override
    public void notify(final Events event, final Object... data) {
        if (event == Events.STEREO_PROFILE_CMD) {
            StereoProfile profile = StereoProfile.values()[(Integer) data[0]];
            notificationsTwo.setVisible(profile != StereoProfile.ANAGLYPH);
        }
    }

}
