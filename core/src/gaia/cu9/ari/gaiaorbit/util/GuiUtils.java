/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaia.cu9.ari.gaiaorbit.interfce.GenericDialog;
import gaia.cu9.ari.gaiaorbit.util.scene2d.Link;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

public class GuiUtils {

    public static void addNoConnectionWindow(Skin skin, Stage stage) {
        addNoConnectionWindow(skin, stage, null);
    }

    public static void addNoConnectionWindow(Skin skin, Stage stage, Runnable ok) {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", I18n.txt("gui.download.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info = new OwnLabel(I18n.txt("gui.download.noconnection"), skin);
                Link manualDownload = new Link(I18n.txt("gui.download.manual"), skin, "link", "http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
                content.add(info).pad(10).row();
                content.add(manualDownload).pad(10);
            }

            @Override
            protected void accept() {
                if(ok != null){
                    ok.run();
                }
            }

            @Override
            protected void cancel() {
            }

        };

        exitw.setAcceptText(I18n.txt("gui.ok"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static void addExitWindow(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", I18n.txt("gui.download.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info = new OwnLabel(I18n.txt("gui.download.noconnection"), skin);
                OwnLabel gsExit = new OwnLabel(I18n.txt("notif.gaiasky.exit"), skin);
                Link manualDownload = new Link(I18n.txt("gui.download.manual"), skin, "link", "http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
                content.add(info).pad(10).row();
                content.add(gsExit).pad(10).row();
                content.add(manualDownload).pad(10);
            }

            @Override
            protected void accept() {
                Gdx.app.exit();
            }

            @Override
            protected void cancel() {
                Gdx.app.exit();
            }

        };
        exitw.setAcceptText(I18n.txt("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

}
