/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import gaiasky.interafce.GenericDialog;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextArea;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

public class MemInfoWindow extends GenericDialog {

    private OwnScrollPane meminfoscroll;

    public MemInfoWindow(Stage stg, Skin skin) {
        super(I18n.txt("gui.help.meminfo"), skin, stg);

        setCancelText(I18n.txt("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        float pad = 5 * GlobalConf.UI_SCALE_FACTOR;
        float tawidth = 500 * GlobalConf.UI_SCALE_FACTOR;

        String meminfostr = "";
        for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
            meminfostr += I18n.txt("gui.help.name") + ": " + mpBean.getName() + ": " + mpBean.getUsage() + "\n";
        }

        TextArea meminfo = new OwnTextArea(meminfostr, skin, "no-disabled");
        meminfo.setDisabled(true);
        meminfo.setPrefRows(10);
        meminfo.setWidth(tawidth);
        meminfo.clearListeners();

        meminfoscroll = new OwnScrollPane(meminfo, skin, "minimalist-nobg");
        meminfoscroll.setWidth(tawidth);
        meminfoscroll.setForceScroll(false, true);
        meminfoscroll.setSmoothScrolling(true);
        meminfoscroll.setFadeScrollBars(false);

        content.add(meminfoscroll).padBottom(pad).row();

    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

}
