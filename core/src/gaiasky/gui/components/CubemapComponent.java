/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.main.KeyBindings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;
import gaiasky.util.scene2d.OwnTextIconButton;

/**
 * Abstract component for all cubemap-related modes: planetarium, panorama, orthosphere.
 */
public abstract class CubemapComponent extends GuiComponent {

    protected String key;
    protected Button backButton;

    /**
     * Factor to apply to the default component width.
     */
    protected float widthMultiplier = 1f;

    public CubemapComponent(Skin skin,
                            Stage stage,
                            String key) {
        super(skin, stage);
        this.key = key;
    }


    @Override
    public void initialize(float componentWidth) {
        KeyBindings kb = KeyBindings.instance;
        var modeCubemap = GaiaSky.settings().program.modeCubemap;

        initializeComponent(componentWidth * widthMultiplier);

        // Go back button
        backButton = new OwnTextIconButton(I18n.msg("gui." + key + ".notice.back"), skin, "back");
        backButton.setName("exit mode " + key);
        backButton.setWidth(componentWidth * widthMultiplier);
        backButton.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui." + key + ".notice.back"),
                                                        kb.getStringKeys("action.toggle/element." + key, true),
                                                        skin));
        backButton.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                EventManager.publish(Event.CUBEMAP_CMD,
                                     this,
                                     !modeCubemap.active,
                                     modeCubemap.projection);
            }
            return false;
        });

        /*
         * ADD TO CONTENT
         */
        Table t = new Table(skin);
        addToTable(t);
        t.add(backButton).left();

        component = t;

    }

    public abstract void initializeComponent(float componentWidth);

    public abstract void addToTable(Table t);

    @Override
    public void dispose() {
    }
}
