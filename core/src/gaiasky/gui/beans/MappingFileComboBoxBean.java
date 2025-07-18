/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.beans;

import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.io.IOException;
import java.nio.file.Path;

public class MappingFileComboBoxBean extends FileComboBoxBean {
    public MappingFileComboBoxBean(Path file) {
        super(file);
        Path assets = Settings.assetsPath(".");
        try {
            String suffix = file.toRealPath().startsWith(assets.toRealPath()) ? " [" + I18n.msgOr("gui.internal", "internal") + "]" : " [" + I18n.msgOr("gui.user", "user") + "]";
            this.name += suffix;
        } catch (IOException e) {
            Logger.getLogger(MappingFileComboBoxBean.class).error(e);
        }
    }
}
