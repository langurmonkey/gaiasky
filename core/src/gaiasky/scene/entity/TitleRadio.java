/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.event.Event;
import gaiasky.scene.Mapper;

import java.util.Objects;

public class TitleRadio extends EntityRadio {

    public TitleRadio(Entity entity) {
        super(entity);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        var body = Mapper.body.get(entity);

        if (Objects.requireNonNull(event) == Event.UI_THEME_RELOAD_INFO) {
            Skin skin = (Skin) data[0];
            // Get new theme color and put it in the label colour
            LabelStyle headerStyle = skin.get("header", LabelStyle.class);
            body.labelColor[0] = headerStyle.fontColor.r;
            body.labelColor[1] = headerStyle.fontColor.g;
            body.labelColor[2] = headerStyle.fontColor.b;
        }

    }
}
