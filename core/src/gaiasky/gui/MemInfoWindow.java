/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.MemInfo;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextArea;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

public class MemInfoWindow extends GenericDialog {

    public MemInfoWindow(Stage stage, Skin skin) {
        super(I18n.msg("gui.help.meminfo"), skin, stage);

        setCancelText(I18n.msg("gui.close"));

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        float pad = 8f;
        float taWidth = 1200f;

        StringBuilder memInfoStr = new StringBuilder();
        memInfoStr.append(TextUtils.capitalise(I18n.msg("gui.debug.ram.used"))).append(": ")
                .append(MemInfo.getUsedMemory()).append(" ").append(I18n.msg("gui.debug.ram.unit")).append("\n");
        memInfoStr.append(TextUtils.capitalise(I18n.msg("gui.debug.ram.free"))).append(": ")
                .append(MemInfo.getFreeMemory()).append(" ").append(I18n.msg("gui.debug.ram.unit")).append("\n");
        memInfoStr.append(TextUtils.capitalise(I18n.msg("gui.debug.ram.alloc"))).append(": ")
                .append(MemInfo.getTotalMemory()).append(" ").append(I18n.msg("gui.debug.ram.unit")).append("\n");
        memInfoStr.append(TextUtils.capitalise(I18n.msg("gui.debug.ram.total"))).append(": ")
                .append(MemInfo.getMaxMemory()).append(" ").append(I18n.msg("gui.debug.ram.unit")).append("\n\n");
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            memInfoStr.append(mpBean.getName()).append(": ").append(mpBean.getUsage()).append("\n\n");
        }

        final OwnScrollPane memInfoScroll = getOwnScrollPane(memInfoStr, taWidth);

        content.add(memInfoScroll).padBottom(pad).row();

    }

    private OwnScrollPane getOwnScrollPane(StringBuilder memInfoStr, float taWidth) {
        OwnTextArea memInfo = new OwnTextArea(memInfoStr.toString(), skin, "disabled-nobg");
        memInfo.setDisabled(true);
        memInfo.setPrefRows(13);
        memInfo.setWidth(taWidth - 15f);
        float fontHeight = memInfo.getStyle().font.getLineHeight();
        memInfo.offsets();
        memInfo.setHeight((memInfo.getLines() + 3) * fontHeight);
        memInfo.clearListeners();

        OwnScrollPane memInfoScroll = new OwnScrollPane(memInfo, skin, "minimalist-nobg");
        memInfoScroll.setWidth(taWidth);
        memInfoScroll.setForceScroll(false, false);
        memInfoScroll.setSmoothScrolling(true);
        memInfoScroll.setFadeScrollBars(false);
        return memInfoScroll;
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

}
