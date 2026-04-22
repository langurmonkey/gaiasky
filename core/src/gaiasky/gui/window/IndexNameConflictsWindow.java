/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnImage;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;


/**
 * Displays the name conflicts in the index for the datasets loaded in this session.
 */
public class IndexNameConflictsWindow extends GenericDialog {
    private final Scene scene;

    public IndexNameConflictsWindow(Stage stage, Skin skin, Scene scene) {
        super(I18n.msg("gui.conflicts.title"), skin, stage);
        this.scene = scene;

        // Start totally transparent.
        this.getColor().a = 0f;

        setModal(false);
        setAcceptText(I18n.msg("gui.ok"));

        buildSuper();
    }

    @Override
    protected void build() {
        if (scene != null && scene.index() != null) {
            content.clear();

            var textString = I18n.msg("gui.conflicts.info");
            var text = new OwnLabel(TextUtils.breakCharacters(textString, 90), skin);

            // Do not ask again.
            var doNotAsk = new OwnCheckBox(I18n.msg("gui.conflicts.confirmation.not"), skin, "default", pad10);
            doNotAsk.setChecked(false);
            doNotAsk.addListener(event -> {
                if (event instanceof ChangeListener.ChangeEvent) {
                    GaiaSky.settings().program.showNameConflicts = !doNotAsk.isChecked();
                }
                return false;
            });

            Table t = new Table(skin);
            t.align(Align.topLeft);

            var index = scene.index();
            var conflicts = index.getConflicts();
            for (var c : conflicts) {
                var nameStr = TextUtils.capitalise(c.name());
                var nameCapped = TextUtils.capString(nameStr, 15);
                var name = new OwnLabel("\"" + nameCapped + "\"", skin, "header-raw");
                name.setTooltip(nameStr);
                name.setWidth(Math.max(250f, name.getWidth()));
                name.setColor(ColorUtils.gRedC);
                t.add(name).left().padRight(pad20);
                addEntity(t, c.e1(), c.e1Archetype(), c.e1Parent());
                t.padRight(pad34);
                addEntity(t, c.e2(), c.e2Archetype(), c.e2Parent());
                t.row();
            }
            t.pack();

            var scroll = new OwnScrollPane(t, skin, "minimalist-nobg");
            scroll.setScrollingDisabled(true, false);
            scroll.setForceScroll(false, false);
            scroll.setFadeScrollBars(false);
            scroll.setOverscroll(false, false);
            scroll.setSmoothScrolling(true);

            scroll.setWidth(1100f);
            scroll.setHeight(Math.min(650f, t.getHeight()));

            Table header = new Table(skin);
            header.setWidth(t.getWidth());
            var hName = new OwnLabel(I18n.msg("gui.conflicts.table.name"), skin, "title-s");
            var hObj1 = new OwnLabel(I18n.msg("gui.conflicts.table.1"), skin, "title-s");
            var hObj2 = new OwnLabel(I18n.msg("gui.conflicts.table.2"), skin, "title-s");
            header.add(hName).width(t.getColumnWidth(0));
            header.add(hObj1).width(t.getColumnWidth(1));
            header.add(hObj2).width(t.getColumnWidth(2));

            content.add(text).left().padBottom(pad20).row();
            content.add(header).left().padBottom(pad10).row();
            content.add(scroll).left().padBottom(pad34).row();
            content.add(doNotAsk).right();
        }

    }

    public void setKeyboardFocus() {
        stage.setKeyboardFocus(cancelButton);
    }

    private void addEntity(Table t, Entity e, Archetype a, String parent) {
        Table et = new Table(skin);
        et.pad(5f, 10f, 5f, 10f);
        et.setBackground("default-rect");

        // Add archetype.
        var arch = new OwnLabel(a.getName(), skin);
        arch.setColor(ColorUtils.gBlueC);
        arch.setTooltip(I18n.msg("gui.conflicts.tooltip.archetype"));

        et.add(arch).left().padRight(5f);

        // Add parent or catalog.
        var graph = Mapper.graph.get(e);
        var ci = graph.getCatalogInfo(e);
        OwnLabel c;
        if (ci != null) {
            // Use catalog info.
            c = new OwnLabel(TextUtils.capString(ci.name, 20), skin);
            c.setColor(ColorUtils.gYellowC);
            c.setTooltip(I18n.msg("gui.conflicts.tooltip.dataset") + ": " + ci.name);
            var i = new OwnImage(skin.getDrawable("iconic-cubemap"));
            et.add(i).padRight(3f);
        } else {
            // Use parent.
            c = new OwnLabel(TextUtils.capString(parent, 20), skin);
            c.setColor(ColorUtils.gGreenC);
            c.setTooltip(I18n.msg("gui.conflicts.tooltip.parent") + ": " + parent);
            var i = new OwnImage(skin.getDrawable("iconic-fork"));
            i.setOrigin(Align.center);
            i.setRotation(180);
            et.add(i).padRight(3f);
        }
        et.add(c).left();

        t.add(et).left().padRight(pad10).padBottom(pad10 / 2f);


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
