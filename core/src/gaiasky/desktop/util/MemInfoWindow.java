/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import gaiasky.interafce.GenericDialog;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextArea;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

public class MemInfoWindow extends GenericDialog {

    public MemInfoWindow(Stage stg, Skin skin) {
        super(I18n.txt("gui.help.meminfo"), skin, stg);

        setCancelText(I18n.txt("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        float pad = 8f;
        float taWidth = 800f;

        StringBuilder memInfoStr = new StringBuilder();
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            memInfoStr.append(I18n.txt("gui.help.name")).append(": ").append(mpBean.getName()).append(": ").append(mpBean.getUsage()).append("\n");
        }

        TextArea meminfo = new OwnTextArea(memInfoStr.toString(), skin, "no-disabled");
        meminfo.setDisabled(true);
        meminfo.setPrefRows(10);
        meminfo.setWidth(taWidth);
        meminfo.clearListeners();

        OwnScrollPane memInfoScroll = new OwnScrollPane(meminfo, skin, "minimalist-nobg");
        memInfoScroll.setWidth(taWidth);
        memInfoScroll.setForceScroll(false, true);
        memInfoScroll.setSmoothScrolling(true);
        memInfoScroll.setFadeScrollBars(false);

        content.add(memInfoScroll).padBottom(pad).row();

    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

}
