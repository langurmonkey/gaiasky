/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr.input;

import com.badlogic.gdx.math.Vector2;
import gaiasky.vr.openxr.input.actions.Action;

public interface XrInputListener {

    boolean showUI(boolean value, XrControllerDevice device);
    boolean accept(boolean value, XrControllerDevice device);
    boolean cameraMode(boolean value, XrControllerDevice device);
    boolean rotate(boolean value, XrControllerDevice device);
    boolean move(Vector2 value, XrControllerDevice device);
    boolean select(float value, XrControllerDevice device);
}
