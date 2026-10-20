/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.util.Settings;
import gaiasky.util.Settings.PostprocessSettings.AntialiasType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Checks that the antialiasing type codes (used in the configuration file and in
 * the preferences window combo box) map to the right enum values, including the
 * post-processing ones which are identified by negative codes.
 */
public class AntialiasTypeTest {

    @Test
    public void codesRoundTrip() {
        for (AntialiasType type : AntialiasType.values()) {
            assertEquals(type, AntialiasType.getFromCode(type.getAACode()));
        }
    }

    @Test
    public void smaaIsAPostProcessAntialias() {
        assertEquals(AntialiasType.SMAA, AntialiasType.getFromCode(-3));
        assertTrue(AntialiasType.SMAA.isPostProcessAntialias());
        assertEquals(-3, AntialiasType.SMAA.getAACode());
    }

    @Test
    public void ssaaIsNotAPostProcessAntialias() {
        assertFalse(AntialiasType.SSAA.isPostProcessAntialias());
        assertFalse(AntialiasType.NONE.isPostProcessAntialias());
    }

    /**
     * The preferences dialog turns the combo box code back into an antialias type with this method.
     * If a code is missing here the selection silently falls back to NONE and the dialog shows
     * "No antialiasing" the next time it is opened.
     */
    @Test
    public void comboBoxCodesMapBackToTheirType() {
        var postprocess = new Settings.PostprocessSettings();
        for (AntialiasType type : AntialiasType.values()) {
            assertEquals(type, postprocess.getAntialias(type.getAACode()));
        }
    }
}
