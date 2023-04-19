/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.GenericDialog;

public class WindowKbdListener extends GuiKbdListener {

    private final GenericDialog dialog;

    public WindowKbdListener(GenericDialog dialog) {
        super(dialog.getStage());
        this.dialog = dialog;
    }

    @Override
    public Array<Group> getContentContainers() {
        var a = new Array<Group>(3);
        a.add(dialog.getCurrentContentContainer());
        a.add(dialog.getBottmGroup());
        a.add(dialog.getButtonsGroup());
        return a;
    }

    @Override
    public boolean close() {
        return dialog.closeCancel();
    }

    @Override
    public boolean accept() {
        return false;
    }

    @Override
    public boolean select() {
        var target = stage.getKeyboardFocus();
        if (target != dialog.acceptButton && dialog.acceptButton != null) {
            stage.setKeyboardFocus(dialog.acceptButton);
            return true;
        } else if (target != dialog.cancelButton && dialog.cancelButton != null) {
            stage.setKeyboardFocus(dialog.cancelButton);
            return true;
        }
        return false;
    }

    @Override
    public boolean tabLeft() {
        return dialog.tabLeft();
    }

    @Override
    public boolean tabRight() {
        return dialog.tabRight();
    }
}
