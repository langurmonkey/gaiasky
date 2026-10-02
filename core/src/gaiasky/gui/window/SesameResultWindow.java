/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import gaiasky.util.SesameResolver;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;

import java.util.Locale;

/**
 * Displays the result of a Sesame name resolver query: the resolved name, the object type, the
 * sky coordinates, and the list of aliases of the object.
 */
public class SesameResultWindow extends GenericDialog {

    /** Result of the query, or null if there was none. **/
    private SesameResolver.Result result;
    /** Error message of the query, or null if there was none. **/
    private String error;
    /** Whether the query is still in flight. **/
    private boolean running;
    /** The query term. **/
    private final String query;

    public SesameResultWindow(Stage stage, Skin skin, String query, SesameResolver.Result result) {
        super(I18n.msg("gui.search.sesame.title", query), skin, stage);
        this.query = query;
        this.result = result;

        // Start totally transparent.
        this.getColor().a = 0f;

        setModal(false);
        setAcceptText(I18n.msg("gui.ok"));

        buildSuper();

        build();
        pack();
    }

    /**
     * Marks the window as running, i.e. the query has been launched and no result is available yet.
     */
    public void setRunning() {
        this.running = true;
        this.result = null;
        this.error = null;
    }

    /**
     * Sets the result of the query, and updates the contents of the window.
     *
     * @param result The result, which may be null if the object was not found.
     * @param error  The error message, which may be null if the query succeeded.
     */
    public void setResult(SesameResolver.Result result, String error) {
        this.running = false;
        this.result = result;
        this.error = error;
        build();
        pack();
    }

    @Override
    protected void build() {
        content.clear();

        float contentWidth = 800f;
        if (running) {
            var l = new OwnLabel(I18n.msg("gui.search.sesame.running", query), skin);
            l.setWidth(contentWidth);
            content.add(l).left().pad(pad10).row();
            return;
        }

        if (result == null) {
            var l = new OwnLabel(error != null ? I18n.msg("gui.search.sesame.error", query, error)
                                               : I18n.msg("gui.search.sesame.noresult", query), skin);
            l.setColor(ColorUtils.gRedC);
            l.setWidth(contentWidth);
            content.add(l).left().pad(pad10).row();
            return;
        }

        // Main name and type.
        var name = new OwnLabel(TextUtils.capitalise(result.oname()), skin, "object-name");
        name.setColor(ColorUtils.gYellowC);
        var otype = new OwnLabel(result.otype() == null || result.otype().isEmpty() ? "-" : result.otype(), skin);
        otype.setColor(ColorUtils.gBlueC);

        Table head = new Table(skin);
        head.add(name).left().padRight(pad20);
        head.add(otype).left();
        head.pack();

        content.add(head).left().pad(pad10).row();

        // Coordinates.
        if (result.raDeg() != null && result.decDeg() != null) {
            var ra = new OwnLabel(I18n.msg("gui.search.sesame.ra") + ": "
                                          + formatRa(result.raDeg()), skin);
            var dec = new OwnLabel(I18n.msg("gui.search.sesame.dec") + ": "
                                           + formatDec(result.decDeg()), skin);
            Table coords = new Table(skin);
            coords.add(ra).left().padRight(pad20);
            coords.add(dec).left();
            coords.pack();
            content.add(coords).left().pad(pad10).row();
        }

        // Aliases.
        if (result.aliases() != null && !result.aliases().isEmpty()) {
            content.add(new OwnLabel(I18n.msg("gui.search.sesame.aliases"), skin, "title-s")).left().pad(pad10)
                   .row();

            Table aliases = new Table(skin);
            aliases.align(Align.topLeft);
            for (var a : result.aliases()) {
                var l = new OwnLabel(a, skin);
                l.setTooltip(a);
                aliases.add(l).left().padRight(pad20).padBottom(pad10 / 2f).row();
            }
            aliases.pack();

            var scroll = new OwnScrollPane(aliases, skin, "minimalist-nobg");
            scroll.setScrollingDisabled(true, false);
            scroll.setForceScroll(false, false);
            scroll.setFadeScrollBars(false);
            scroll.setOverscroll(false, false);
            scroll.setSmoothScrolling(true);
            scroll.setWidth(contentWidth);
            scroll.setHeight(Math.clamp(aliases.getHeight(), 120f, 400f));

            content.add(scroll).left().pad(pad10).row();
        }
    }

    /** Formats a right ascension in degrees as sexagesimal hours. **/
    private String formatRa(double raDeg) {
        double hours = (raDeg / 15d + 24d) % 24d;
        int h = (int) hours;
        double m = (hours - h) * 60d;
        int min = (int) m;
        double s = (m - min) * 60d;
        return String.format(Locale.ROOT, "%02dh%02dm%05.2fs", h, min, s);
    }

    /** Formats a declination in degrees as sexagesimal degrees. **/
    private String formatDec(double decDeg) {
        char sign = decDeg < 0 ? '-' : '+';
        double a = Math.abs(decDeg);
        int d = (int) a;
        double m = (a - d) * 60d;
        int min = (int) m;
        double s = (m - min) * 60d;
        return String.format(Locale.ROOT, "%c%02d°%02d'%05.2f\"", sign, d, min, s);
    }

    @Override
    public void touch() {
        build();
    }

    @Override
    public void setKeyboardFocus() {
        stage.setKeyboardFocus(acceptButton);
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