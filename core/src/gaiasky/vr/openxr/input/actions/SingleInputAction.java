/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr.input.actions;

import gaiasky.vr.openxr.input.XrControllerDevice;
import org.lwjgl.openxr.XrActionStateGetInfo;

import static org.lwjgl.openxr.XR10.XR_TYPE_ACTION_STATE_GET_INFO;

public abstract class SingleInputAction<T> extends Action implements InputAction {

    protected static final XrActionStateGetInfo getInfo = XrActionStateGetInfo.calloc().type(XR_TYPE_ACTION_STATE_GET_INFO);

    public T currentState;
    public boolean changedSinceLastSync;
    public long lastChangeTime;
    public boolean isActive;

    protected SingleInputAction(String name, String localizedName, int type, XrControllerDevice device) {
        super(name, localizedName, type, device);
    }

}
