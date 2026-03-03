/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.api;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;

/**
 * Common interface for windows and screens to handle events in a unified way.
 */
public interface IScreen {

    Stage getStage();
    Group getCurrentContentContainer();
    Group getBottomGroup();
    Group getButtonsGroup();
    Button acceptButton();
    Button cancelButton();
    boolean closeAccept();
    boolean closeCancel();
    boolean tabLeft();
    boolean tabRight();
}
