/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Settings;
import gaiasky.util.Settings.StereoProfile;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;

import java.text.DecimalFormat;

public class StereoGui extends AbstractGui {
    private final Skin skin;

    protected NotificationsInterface notificationsOne, notificationsTwo;
    protected Container<Button> buttonContainer;
    protected Button back;
    protected CustomInterface customInterface;

    protected DecimalFormat nf;

    public StereoGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
    }

    public void initialize(final AssetManager assetManager, final SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        stage = new Stage(vp, sb);
    }

    /**
     * Constructs the interface
     */
    public void doneLoading(AssetManager assetManager) {
        interfaces = new Array<>();

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, Event.STEREO_PROFILE_CMD);
    }

    private void buildGui() {
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }

        nf = new DecimalFormat("##0.###");

        // Notifications one - Bottom left
        notificationsOne = new NotificationsInterface(skin, true, true, false, false);
        notificationsOne.setFillParent(true);
        notificationsOne.left().bottom();
        notificationsOne.pad(0, 5, 5, 0);
        interfaces.add(notificationsOne);

        // Notifications two - Bottom centre
        notificationsTwo = new NotificationsInterface(skin, true, true, false, false);
        notificationsTwo.setFillParent(true);
        notificationsTwo.bottom();
        notificationsTwo.setX(Gdx.graphics.getWidth() / 2.0f);
        notificationsTwo.pad(0, 5, 5, 0);
        interfaces.add(notificationsTwo);

        // Back to normal mode - Bottom right
        float bw = 48f, bh = 48f;
        KeyBindings kb = KeyBindings.instance;

        buttonContainer = new Container<>();
        back = new OwnImageButton(skin, "sc-exit");
        back.setSize(bw, bh);
        back.setName("exit stereo mode");
        back.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.stereo.notice.back"), kb.getStringKeys("action.toggle/element.stereomode"), skin));
        back.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.STEREOSCOPIC_CMD, this, !Settings.settings.program.modeStereo.active);
            }
            return false;
        });
        buttonContainer.setActor(back);
        buttonContainer.setFillParent(true);
        buttonContainer.bottom().right().pad(0, 0, 5, 5);

        // CUSTOM MESSAGES
        customInterface = new CustomInterface(stage, skin, lock);
        interfaces.add(customInterface);

        /* ADD TO UI */
        rebuildGui();

    }

    protected void rebuildGui() {

        if (stage != null) {
            stage.clear();
            if (notificationsOne != null) {
                stage.addActor(notificationsOne);
            }
            if (notificationsTwo != null) {
                stage.addActor(notificationsTwo);
            }
            if (buttonContainer != null) {
                stage.addActor(buttonContainer);
            }

        }
    }

    /**
     * Removes the focus from this Gui and returns true if the focus was in the
     * GUI, false otherwise.
     *
     * @return true if the focus was in the GUI, false otherwise.
     */
    public boolean cancelTouchFocus() {
        if (stage.getScrollFocus() != null) {
            stage.setScrollFocus(null);
            stage.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public void update(double dt) {
        notificationsTwo.setX(notificationsTwo.getMessagesWidth() / 2);
        stage.act((float) dt);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.STEREO_PROFILE_CMD) {
            StereoProfile profile = StereoProfile.values()[(Integer) data[0]];
            notificationsTwo.setVisible(!profile.isAnaglyph());
        }
    }

}
