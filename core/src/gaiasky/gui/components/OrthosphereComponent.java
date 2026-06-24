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
import gaiasky.event.IObserver;
import gaiasky.gui.main.KeyBindings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;
import gaiasky.util.scene2d.OwnTextIconButton;

/**
 * Component that appears in panorama mode and is used to control some of its parameters.
 */
public class OrthosphereComponent extends CubemapComponent {


    public OrthosphereComponent(Skin skin,
                                Stage stage) {
        super(skin, stage, "orthosphere");
    }

    @Override
    public void initializeComponent(float componentWidth) {

    }

    @Override
    public void addToTable(Table t) {

    }
}
