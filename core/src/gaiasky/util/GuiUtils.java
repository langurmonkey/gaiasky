/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.interafce.GenericDialog;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextTooltip;

public class GuiUtils {

    public static void addNoConnectionWindow(Skin skin, Stage stage) {
        addNoConnectionWindow(skin, stage, null);
    }

    public static void addNoConnectionWindow(Skin skin, Stage stage, Runnable ok) {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", I18n.txt("gui.download.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info = new OwnLabel(I18n.txt("gui.download.noconnection.continue"), skin);
                Link manualDownload = new Link(I18n.txt("gui.download.manual"), skin, "link", Settings.settings.program.url.dataMirror);
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

            @Override
            public void dispose() {
            }

        };

        exitw.setAcceptText(I18n.txt("gui.ok"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static void addNoConnectionExit(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", I18n.txt("gui.download.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info = new OwnLabel(I18n.txt("gui.download.noconnection"), skin);
                OwnLabel gsExit = new OwnLabel(I18n.txt("notif.gaiasky.exit"), skin);
                Link manualDownload = new Link(I18n.txt("gui.download.manual"), skin, "link", "https://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
                content.add(info).left().pad(10).row();
                content.add(gsExit).left().pad(10).row();
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

            @Override
            public void dispose() {
            }

        };
        exitw.setAcceptText(I18n.txt("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static void addNoVRConnectionExit(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", I18n.txt("gui.vr.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info1 = new OwnLabel(I18n.txt("gui.vr.noconnection.1"), skin);
                OwnLabel info2 = new OwnLabel(I18n.txt("gui.vr.noconnection.2"), skin);
                OwnLabel gsExit = new OwnLabel(I18n.txt("notif.gaiasky.exit"), skin);
                content.add(info1).left().padTop(10).padBottom(5).row();
                content.add(info2).left().padBottom(10).row();
                content.add(gsExit).left().padTop(10).row();
            }

            @Override
            protected void accept() {
                Gdx.app.exit();
            }

            @Override
            protected void cancel() {
                Gdx.app.exit();
            }

            @Override
            public void dispose() {
            }

        };
        exitw.setAcceptText(I18n.txt("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static void addNoVRDataExit(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.txt("notif.error", I18n.txt("gui.vr.nodata.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info1 = new OwnLabel(I18n.txt("gui.vr.nodata.1"), skin);
                OwnLabel info2 = new OwnLabel(I18n.txt("gui.vr.nodata.2"), skin);
                OwnLabel gsExit = new OwnLabel(I18n.txt("notif.gaiasky.exit"), skin);
                content.add(info1).left().padTop(10).padBottom(5).row();
                content.add(info2).left().padBottom(10).row();
                content.add(gsExit).left().padTop(10).row();
            }

            @Override
            protected void accept() {
                Gdx.app.exit();
            }

            @Override
            protected void cancel() {
                Gdx.app.exit();
            }

            @Override
            public void dispose() {
            }

        };
        exitw.setAcceptText(I18n.txt("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static HorizontalGroup getTooltipHorizontalGroup(Actor actor, String tooltipText, Skin skin){
        return getTooltipHorizontalGroup(actor, tooltipText, 12.8f, skin);
    }

    public static HorizontalGroup getTooltipHorizontalGroup(Actor actor, String tooltipText, float space, Skin skin){
        HorizontalGroup hg = new HorizontalGroup();
        hg.space(space);
        hg.addActor(actor);
        OwnImageButton tooltip = new OwnImageButton(skin, "tooltip");
        tooltip.addListener(new OwnTextTooltip(tooltipText, skin));
        hg.addActor(tooltip);
        return hg;
    }

    public static HorizontalGroup tooltipHg(Actor actor, String key, Skin skin){
       return getTooltipHorizontalGroup(actor, I18n.txt(key), 12.8f, skin);
    }

}
