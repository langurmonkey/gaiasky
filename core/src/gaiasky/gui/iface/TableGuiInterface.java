/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.gui.api.IGuiInterface;

public abstract class TableGuiInterface extends Table implements IGuiInterface {
    protected TableGuiInterface(Skin skin) {
        super(skin);
    }

    @Override
    public boolean isOn() {
        return hasParent() && isVisible();
    }
}
