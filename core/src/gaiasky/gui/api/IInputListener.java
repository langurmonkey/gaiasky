/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.api;

/**
 * Input listener interface.
 */
public interface IInputListener {

    /** Update the listener. **/
    void update();

    /** Activate the listener. **/
    void activate();

    /** Deactivate the listener. **/
    void deactivate();
}
