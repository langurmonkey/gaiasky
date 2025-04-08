/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.Separator;

public class ContainerPane extends Table {


    public ContainerPane(Skin skin, String title, final Actor actor) {
        super(skin);
        final float pad10 = 10f;

        background("bg-pane-border-dark");

        OwnLabel titleLabel = new OwnLabel(title, skin, "header");
        add(titleLabel).top().center().row();
        add(new Separator(skin, "small")).top().left().growX().padBottom(pad10 * 2f).row();
        add(actor);
        pack();

    }


}
