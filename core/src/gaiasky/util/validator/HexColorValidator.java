/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.color.ColorUtils;

public class HexColorValidator implements IValidator {
    private boolean alpha = false;

    public HexColorValidator(boolean alpha) {
        this.alpha = alpha;
    }

    @Override
    public boolean validate(String value) {
        try {
            if (alpha) {
                var ignored = ColorUtils.hexToRgba(value);
            } else {
                var ignored = ColorUtils.hexToRgb(value);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
