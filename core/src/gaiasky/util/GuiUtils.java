/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.gui.GenericDialog;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

public class GuiUtils {

    public static void addNoConnectionWindow(Skin skin, Stage stage) {
        addNoConnectionWindow(skin, stage, null);
    }

    public static void addNoConnectionWindow(Skin skin, Stage stage, Runnable ok) {
        String title = I18n.msg("notif.error", I18n.msg("gui.download.noconnection.title"));
        if (Settings.settings.program.offlineMode) {
            title = I18n.msg("gui.system.offlinemode");
        }
        GenericDialog dialog = new GenericDialog(title, skin, stage) {

            @Override
            protected void build() {
                String text;
                if (Settings.settings.program.offlineMode) {
                    OwnLabel info = new OwnLabel(I18n.msg("gui.download.offlinemode.continue"), skin);
                    content.add(info).pad(pad18).row();
                    Link docs = new Link(I18n.msg("gui.wiki.moreinfo"), skin, Settings.DOCUMENTATION + "/Config-file.html");
                    content.add(docs).pad(pad18).padTop(pad20).row();
                } else {
                    OwnLabel info = new OwnLabel(I18n.msg("gui.download.noconnection.continue"), skin);
                    content.add(info).pad(pad18).row();
                    Link manualDownload = new Link(I18n.msg("gui.download.manual"), skin, "link", Settings.settings.program.url.dataMirror);
                    content.add(manualDownload).pad(pad18);
                }

            }

            @Override
            protected boolean accept() {
                if (ok != null) {
                    ok.run();
                }
                return true;
            }

            @Override
            protected void cancel() {
            }

            @Override
            public void dispose() {
            }

        };

        dialog.setAcceptText(I18n.msg("gui.ok"));
        dialog.setCancelText(null);
        dialog.buildSuper();
        dialog.show(stage);
    }

    public static void addNoConnectionExit(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.msg("notif.error", I18n.msg("gui.download.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                String text;
                if (Settings.settings.program.offlineMode) {
                    text = I18n.msg("gui.download.offlinemode");
                } else {
                    text = I18n.msg("gui.download.noconnection");
                }
                OwnLabel info = new OwnLabel(text, skin);
                OwnLabel gsExit = new OwnLabel(I18n.msg("notif.gaiasky.exit"), skin);
                Link manualDownload = new Link(I18n.msg("gui.download.manual"), skin, "link", "https://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
                content.add(info).left().pad(10).row();
                content.add(gsExit).left().pad(10).row();
                content.add(manualDownload).pad(10);
            }

            @Override
            protected boolean accept() {
                GaiaSky.postRunnable(Gdx.app::exit);
                return true;
            }

            @Override
            protected void cancel() {
                GaiaSky.postRunnable(Gdx.app::exit);
            }

            @Override
            public void dispose() {
            }

        };
        exitw.setAcceptText(I18n.msg("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static void addNoVRConnectionExit(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.msg("notif.error", I18n.msg("gui.vr.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info1 = new OwnLabel(I18n.msg("gui.vr.noconnection.1"), skin);
                OwnLabel info2 = new OwnLabel(I18n.msg("gui.vr.noconnection.2"), skin);
                OwnLabel gsExit = new OwnLabel(I18n.msg("notif.gaiasky.exit"), skin);
                content.add(info1).left().padTop(10).padBottom(5).row();
                content.add(info2).left().padBottom(10).row();
                content.add(gsExit).left().padTop(10).row();
            }

            @Override
            protected boolean accept() {
                GaiaSky.postRunnable(Gdx.app::exit);
                return true;
            }

            @Override
            protected void cancel() {
                GaiaSky.postRunnable(Gdx.app::exit);
            }

            @Override
            public void dispose() {
            }

        };
        exitw.setAcceptText(I18n.msg("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static void addNoVRDataExit(Skin skin, Stage stage) {
        GenericDialog exitw = new GenericDialog(I18n.msg("notif.error", I18n.msg("gui.vr.nodata.title")), skin, stage) {

            @Override
            protected void build() {
                OwnLabel info1 = new OwnLabel(I18n.msg("gui.vr.nodata.1"), skin);
                OwnLabel info2 = new OwnLabel(I18n.msg("gui.vr.nodata.2"), skin);
                OwnLabel gsExit = new OwnLabel(I18n.msg("notif.gaiasky.exit"), skin);
                content.add(info1).left().padTop(10).padBottom(5).row();
                content.add(info2).left().padBottom(10).row();
                content.add(gsExit).left().padTop(10).row();
            }

            @Override
            protected boolean accept() {
                GaiaSky.postRunnable(Gdx.app::exit);
                return true;
            }

            @Override
            protected void cancel() {
                GaiaSky.postRunnable(Gdx.app::exit);
            }

            @Override
            public void dispose() {
            }

        };
        exitw.setAcceptText(I18n.msg("gui.exit"));
        exitw.setCancelText(null);
        exitw.buildSuper();
        exitw.show(stage);
    }

    public static HorizontalGroup getTooltipHorizontalGroup(Actor actor, String tooltipText, Skin skin) {
        return getTooltipHorizontalGroup(actor, tooltipText, 12.8f, skin);
    }

    public static HorizontalGroup getTooltipHorizontalGroup(Actor actor, String tooltipText, float space, Skin skin) {
        HorizontalGroup hg = new HorizontalGroup();
        hg.space(space);
        hg.addActor(actor);
        OwnImageButton tooltip = new OwnImageButton(skin, "tooltip");
        tooltip.addListener(new OwnTextTooltip(tooltipText, skin));
        hg.addActor(tooltip);
        return hg;
    }

    public static HorizontalGroup tooltipHg(Actor actor, String key, Skin skin) {
        return getTooltipHorizontalGroup(actor, I18n.msg(key), 12.8f, skin);
    }

    /**
     * Moves the slider up or down by the given percentage.
     *
     * @param up      Whether to move it up.
     * @param percent The percentage in [0,1].
     * @param slider  The slider to move.
     */
    public static void sliderMove(boolean up, float percent, OwnSliderPlus slider) {
        float max = slider.getMaxValue();
        float min = slider.getMinValue();
        float val = slider.getValue();
        float inc = (max - min) * percent;
        slider.setValue(MathUtils.clamp(val + (up ? inc : -inc), min, max));
    }

    /**
     * Gets the first scroll pane contained in the given actor by
     * traversing it recursively, if it exists.
     *
     * @param actor The container actor.
     *
     * @return The first scroll pane found, or null if none is found.
     */
    public static ScrollPane getScrollPaneIn(Actor actor) {
        if (actor instanceof ScrollPane) {
            return (ScrollPane) actor;
        } else if (actor instanceof WidgetGroup) {
            var group = (WidgetGroup) actor;
            var children = group.getChildren();
            for (var child : children) {
                ScrollPane scroll;
                if ((scroll = getScrollPaneIn(child)) != null) {
                    return scroll;
                }
            }
        }
        return null;
    }

    /**
     * Get all the input widgets in the given container actor by traversing it
     * recursively.
     *
     * @param actor The actor.
     * @param list  The list with all the input widgets in the actor.
     *
     * @return The input list.
     */
    public static Array<Actor> getInputWidgets(Actor actor, Array<Actor> list) {
        if (actor != null) {
            if (isInputWidget(actor) && isNotDisabled(actor) && !isTooltipWidget(actor)) {
                list.add(actor);
            } else if (actor instanceof WidgetGroup) {
                getInputWidgetsInGroup((WidgetGroup) actor, list);
            }
        }
        return list;
    }

    private static void getInputWidgetsInGroup(WidgetGroup actor, Array<Actor> list) {
        var children = actor.getChildren();
        for (var child : children) {
            getInputWidgets(child, list);
        }
    }

    /**
     * Check if the given actor is an input widget.
     *
     * @param actor The actor.
     *
     * @return True if the actor is an input widget.
     */
    public static boolean isInputWidget(Actor actor) {
        return actor instanceof SelectBox ||
                actor instanceof TextField ||
                actor instanceof Button ||
                actor instanceof Slider;
    }

    /**
     * Check if the given actor is a tooltip widget.
     *
     * @param actor The actor.
     *
     * @return True if the actor is a tooltip widget.
     */
    public static boolean isTooltipWidget(Actor actor) {
        return actor instanceof OwnImageButton && ((OwnImageButton) actor).getStyle().imageUp.toString().contains("tooltip");
    }

    /**
     * Check if the actor is not disabled.
     *
     * @param actor The actor.
     *
     * @return True if the actor is not disabled.
     */
    public static boolean isNotDisabled(Actor actor) {
        return !(actor instanceof Disableable) || !((Disableable) actor).isDisabled();
    }

    /**
     * Check if the actor currently has a change listener attached.
     *
     * @param actor The actor.
     *
     * @return True if the actor has a change listener.
     */
    public static boolean hasChangeListener(Actor actor) {
        var listeners = actor.getListeners();
        for (var listener : listeners) {
            if (listener instanceof ChangeListener) {
                return true;
            }
        }
        return false;
    }
}
