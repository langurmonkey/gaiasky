/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.Pair;

/**
 * Holds information about the mode change pop-ups that appear when changing to some camera modes.
 * Typically, those are used to inform the user of the new keyboard mappings that are in effect in the new mode, and
 * how to go back to the default camera mode.
 */
public class ModePopupInfo {

    public String title;
    public String header;
    public Array<Pair<String[], String>> mappings;
    public String warn;

    public void initMappings() {
        if (mappings == null) {
            mappings = new Array();
        }
    }

    public void addMapping(String action, String... keys) {
        initMappings();
        mappings.add(new Pair<>(keys, action));
    }
}
