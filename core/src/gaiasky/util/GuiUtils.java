/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import net.jafama.FastMath;

/**
 * Utilities for the Graphical User Interface.
 */
public class GuiUtils {

    private static final ThreadLocal<Vector2> vec2 = ThreadLocal.withInitial(Vector2::new);

    public static void addNotificationWindow(String title, String message, Skin skin, Stage stage, Runnable ok) {
        GenericDialog dialog = new GenericDialog(title, skin, stage) {

            @Override
            protected void build() {
                OwnLabel msg = new OwnLabel(message, skin);
                content.add(msg).pad(pad18).row();

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
        GenericDialog exitDialog = new GenericDialog(I18n.msg("notif.error", I18n.msg("gui.download.noconnection.title")), skin, stage) {

            @Override
            protected void build() {
                String text;
                if (Settings.settings.program.offlineMode) {
                    text = I18n.msg("gui.download.offlinemode");
                } else {
                    text = I18n.msg("gui.download.noconnection");
                }
                OwnLabel info = new OwnLabel(text, skin);
                content.add(info).left().pad(pad10).padBottom(pad34).row();

                Table status = new Table(skin);
                // Data server status.
                var dataServer = new OwnLabel(I18n.msg("gui.connection.dataserver"), skin);
                status.add(dataServer).left().padRight(pad34 * 3f).padBottom(pad18);
                status.add(getKoImage()).left().padBottom(pad18).row();

                // Internet connection status.
                var internet = new OwnLabel(I18n.msg("gui.connection.internet"), skin);
                status.add(internet).left().padRight(pad34 * 3f).padBottom(pad18);
                var cell = status.add();
                cell.left().padBottom(pad18).row();
                cell.setActor(new OwnLabel("...", skin));

                DownloadHelper.checkInternetConnection(
                        () -> {
                            GaiaSky.postRunnable(() -> cell.setActor(getOkImage()));
                        },
                        () -> {
                            GaiaSky.postRunnable(() -> cell.setActor(getKoImage()));
                        }
                );

                content.add(status).center().pad(pad10).padBottom(pad34).row();

                content.add(new OwnLabel("To fix the issue, you can try:", skin)).left().pad(pad10).row();
                content.add(new Link(I18n.msg("gui.connection.fix.manual"),
                                     skin,
                                     "link",
                                     Settings.settings.program.url.dataMirror)).pad(pad10).row();
                content.add(new Link(I18n.msg("gui.connection.fix.report"), skin, "link", Settings.REPO_ISSUES)).pad(pad10);
            }

            private OwnImage getKoImage() {
                var ko = skin.getDrawable("iconic-circle-x");
                var koImage = new OwnImage(ko);
                koImage.setColor(skin.getColor("red"));
                return koImage;
            }

            private OwnImage getOkImage() {
                var ok = skin.getDrawable("iconic-circle-check");
                var okImage = new OwnImage(ok);
                okImage.setColor(skin.getColor("green"));
                return okImage;
            }

            @Override
            protected boolean accept() {
                GaiaSky.postRunnable(Gdx.app::exit);
                return true;
            }

            @Override
            protected void cancel() {
            }

            @Override
            public void dispose() {
            }

        };
        exitDialog.setAcceptText(I18n.msg("gui.exit"));
        exitDialog.setCancelText(null);
        exitDialog.buildSuper();
        exitDialog.show(stage);
    }

    public static void addNoVRConnectionExit(Skin skin, Stage stage) {
        GenericDialog exitDialog = new GenericDialog(I18n.msg("notif.error", I18n.msg("gui.vr.noconnection.title")), skin, stage) {

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
            }

            @Override
            public void dispose() {
            }

        };
        exitDialog.setAcceptText(I18n.msg("gui.exit"));
        exitDialog.setCancelText(null);
        exitDialog.buildSuper();
        exitDialog.show(stage);
    }

    public static void addNoVRDataExit(Skin skin, Stage stage) {
        GenericDialog exitDialog = new GenericDialog(I18n.msg("notif.error", I18n.msg("gui.vr.nodata.title")), skin, stage) {

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
        exitDialog.setAcceptText(I18n.msg("gui.exit"));
        exitDialog.setCancelText(null);
        exitDialog.buildSuper();
        exitDialog.show(stage);
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
    public static void sliderMove(boolean up, float percent, Slider slider) {
        float max = slider.getMaxValue();
        float min = slider.getMinValue();
        float val = slider.getValue();
        float inc = (max - min) * percent;
        slider.setValue(MathUtils.clamp(val + (up ? inc : -inc), min, max));
    }

    /**
     * Moves the selection of the given select box up or down.
     *
     * @param up        Whether to move up (true) or down (false).
     * @param jump      If true, the selection is moved all the way up or down.
     * @param selectBox The select box.
     */
    public static void selectBoxMoveSelection(boolean up, boolean jump, SelectBox<?> selectBox) {
        var index = selectBox.getSelectedIndex();
        if (jump) {
            index = up ? 0 : selectBox.getItems().size - 1;
        } else {
            index = up ? FastMath.max(index - 1, 0) : (index + 1) % selectBox.getItems().size;
        }
        // Select.
        selectBox.setSelectedIndex(index);
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
        } else if (actor instanceof WidgetGroup group) {
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
     * Gets the first scroll pane ancestor of the given actor, if any.
     *
     * @param actor The actor.
     *
     * @return The first scroll pane ancestor. Null if it does not exist.
     */
    public static ScrollPane getScrollPaneAncestor(Actor actor) {
        if (actor == null) {
            return null;
        } else if (actor instanceof ScrollPane scroll) {
            return scroll;
        } else {
            return getScrollPaneAncestor(actor.getParent());
        }
    }

    /**
     * If the given actor has a scroll pane ancestor, this method makes sure that
     * the actor is visible by moving the scroll position if required.
     *
     * @param actor The actor.
     */
    public static void ensureScrollVisible(Actor actor) {
        if (actor != null) {

            // Look for scroll pane.
            Actor parent = actor.getParent();
            while (parent != null && !(parent instanceof ScrollPane)) {
                parent = parent.getParent();
            }

            if (parent != null) {
                var scrollPane = (ScrollPane) parent;
                var coordinates = actor.localToAscendantCoordinates(scrollPane.getActor(), vec2.get().set(actor.getX(), actor.getY()));
                scrollPane.scrollTo(coordinates.x, coordinates.y, actor.getWidth(), FastMath.min(200f, actor.getHeight() * 10f));
            }
        }
    }

    /**
     * Get all input widgets recursively.
     *
     * @param actors The list of actors.
     * @param list   The output list.
     *
     * @return The output list with all the input widgets.
     */
    public static Array<Actor> getInputWidgets(Array<? extends Actor> actors, Array<Actor> list) {
        for (var actor : actors) {
            getInputWidgets(actor, list);
        }
        return list;
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
}
