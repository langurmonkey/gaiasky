/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.GenericDialog;
import gaiasky.gui.IGamepadMappings;

public class WindowGamepadListener extends GuiGamepadListener {

    private final GenericDialog dialog;

    public WindowGamepadListener(String mappingsFile, GenericDialog dialog) {
        super(mappingsFile, dialog.getStage());
        this.dialog = dialog;
    }

    public WindowGamepadListener(IGamepadMappings mappings, GenericDialog dialog) {
        super(mappings, dialog.getStage());
        this.dialog = dialog;
    }

    @Override
    public void moveLeft() {
    }

    @Override
    public void moveRight() {
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
    public void select() {
        Actor target = stage.getKeyboardFocus();
        if (target != dialog.acceptButton && dialog.acceptButton != null) {
            stage.setKeyboardFocus(dialog.acceptButton);
        } else if (target != dialog.cancelButton && dialog.cancelButton != null) {
            stage.setKeyboardFocus(dialog.cancelButton);
        }
    }

    @Override
    public void tabLeft() {
        dialog.tabLeft();
    }

    @Override
    public void tabRight() {
        dialog.tabRight();
    }

    @Override
    public void back() {
        select();
    }

    @Override
    public void start() {
        dialog.closeAccept();
    }

}
