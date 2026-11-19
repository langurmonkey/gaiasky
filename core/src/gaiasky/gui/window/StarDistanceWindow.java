/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.scene2d.Separator;

import java.util.Locale;

/**
 * Live read-out of the closest-star distance scaling used by the camera speed, plus editable low
 * and high radius multipliers. Meant for tuning: it shows what the camera sees this frame, so it
 * does not compute anything of its own.
 * <p>
 * Only meaningful in free mode, which is the only mode where the closest star drives the
 * distance. The values are refreshed a few times per second while the window is open.
 */
public class StarDistanceWindow extends GenericDialog {
    private static final float UPDATE_INTERVAL = 0.1f;
    private static final float FIELD_WIDTH = 350f;

    private OwnLabel modeValue, stateValue, starValue, distanceValue, radiusValue, lowValue, highValue, blendValue, contributionValue, regularValue, rawValue, smoothedValue, speedValue;
    private OwnTextField lowField, highField;

    private float elapsed = 0f;

    public StarDistanceWindow(Stage stage, Skin skin) {
        super(I18n.msg("gui.debug.stardistance.title"), skin, stage);

        // Never steal the camera controls: this window is meant to stay open while flying.
        defaultMouseKbdListener = false;
        defaultGamepadListener = false;
        enterExit = false;

        setModal(false);
        setResizable(false);
        setCancelText(I18n.msg("gui.close"));

        buildSuper();
        refresh();
    }

    @Override
    protected void build() {
        var grid = new Table(skin);
        grid.defaults().left().padBottom(pad10);

        modeValue = row(grid, "gui.debug.stardistance.mode");
        stateValue = row(grid, "gui.debug.stardistance.state");

        grid.add(new Separator(skin)).colspan(2).fillX().padTop(pad10).padBottom(pad10).row();

        starValue = row(grid, "gui.debug.stardistance.star");
        radiusValue = row(grid, "gui.debug.stardistance.radius");
        distanceValue = row(grid, "gui.debug.stardistance.distance");
        lowValue = row(grid, "gui.debug.stardistance.low");
        highValue = row(grid, "gui.debug.stardistance.high");
        blendValue = row(grid, "gui.debug.stardistance.blend");
        contributionValue = row(grid, "gui.debug.stardistance.contribution");

        grid.add(new Separator(skin)).colspan(2).fillX().padTop(pad10).padBottom(pad10).row();

        regularValue = row(grid, "gui.debug.stardistance.regular");
        rawValue = row(grid, "gui.debug.stardistance.raw");
        smoothedValue = row(grid, "gui.debug.stardistance.smoothed");
        speedValue = row(grid, "gui.debug.stardistance.speed");

        content.add(grid).left().expandX().row();

        // Editable limits.
        var edit = new Table(skin);
        edit.defaults().left().padBottom(pad10);

        lowField = new OwnTextField("", skin);
        lowField.setWidth(FIELD_WIDTH);
        highField = new OwnTextField("", skin);
        highField.setWidth(FIELD_WIDTH);

        var apply = new OwnTextButton(I18n.msg("gui.debug.stardistance.apply"), skin);
        apply.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                applyLimits();
                return true;
            }
            return false;
        });

        edit.add(new OwnLabel(I18n.msg("gui.debug.stardistance.low"), skin, "default-blue")).padRight(pad18);
        edit.add(lowField).row();
        edit.add(new OwnLabel(I18n.msg("gui.debug.stardistance.high"), skin, "default-blue")).padRight(pad18);
        edit.add(highField).row();
        edit.add(apply);

        content.add(edit).left().padTop(pad18).row();

        var hint = new OwnLabel(I18n.msg("gui.debug.stardistance.hint"), skin);
        hint.setColor(ColorUtils.oDarkGrayC);
        hint.setWrap(true);
        content.add(hint).width(640f).left().padTop(pad10).row();
    }

    private OwnLabel row(Table t, String key) {
        t.add(new OwnLabel(I18n.msg(key), skin, "default-blue")).padRight(pad18);
        var value = new OwnLabel("-", skin);
        t.add(value).left().expandX().row();
        return value;
    }

    private void applyLimits() {
        var camera = freeCamera();
        if (camera == null) {
            return;
        }
        var low = parse(lowField.getText());
        var high = parse(highField.getText());
        if (low != null) {
            camera.setStarDistanceScaleLow(low);
        }
        if (high != null) {
            camera.setStarDistanceScaleHigh(high);
        }
        refresh();
    }

    private static Double parse(String text) {
        try {
            var v = Double.parseDouble(text.trim());
            return Double.isFinite(v) && v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private NaturalCamera freeCamera() {
        var camera = GaiaSky.instance.getCameraManager().current;
        return camera instanceof NaturalCamera naturalCamera ? naturalCamera : null;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!isVisible() || !hasParent()) {
            return;
        }
        elapsed += delta;
        if (elapsed < UPDATE_INTERVAL) {
            return;
        }
        elapsed = 0f;
        refresh();
    }

    private void refresh() {
        var camera = freeCamera();
        if (camera == null) {
            modeValue.setText("-");
            return;
        }
        var d = camera.starDistanceDebug();

        modeValue.setText(d.mode() != null ? d.mode().toString() : "-");
        stateValue.setText(I18n.msg(d.enabled() ? "gui.debug.stardistance.on" : "gui.debug.stardistance.off"));

        if (d.starName() != null) {
            starValue.setText(d.starName());
            radiusValue.setText(format(d.radius(), Constants.U_TO_KM, "km"));
            distanceValue.setText(format(d.starDistance(), Constants.U_TO_PC, "pc"));
            lowValue.setText(format(d.limitLow(), Constants.U_TO_PC, "pc"));
            highValue.setText(format(d.limitHigh(), Constants.U_TO_PC, "pc"));
            blendValue.setText(String.format(Locale.ROOT, "%.3f", d.blend()));
            contributionValue.setText(format(d.starContribution(), Constants.U_TO_PC, "pc"));
        } else {
            starValue.setText(I18n.msg("gui.debug.stardistance.nostar"));
            radiusValue.setText("-");
            distanceValue.setText("-");
            lowValue.setText("-");
            highValue.setText("-");
            blendValue.setText("-");
            contributionValue.setText("-");
        }

        regularValue.setText(format(d.regularDistance(), Constants.U_TO_PC, "pc"));
        rawValue.setText(format(d.rawDistance(), Constants.U_TO_PC, "pc"));
        smoothedValue.setText(format(d.smoothedDistance(), Constants.U_TO_PC, "pc"));
        speedValue.setText(Double.isFinite(d.speedScale()) ? String.format(Locale.ROOT, "%.6g u/s", d.speedScale()) : "-");

        // Keep the fields in sync with the camera unless the user is typing in them.
        if (stage.getKeyboardFocus() != lowField) {
            lowField.setText(fmt(camera.getStarDistanceScaleLow()));
        }
        if (stage.getKeyboardFocus() != highField) {
            highField.setText(fmt(camera.getStarDistanceScaleHigh()));
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.6g", v);
    }

    /**
     * Formats an internal-unit value in the given unit, followed by the value in internal units.
     */
    private static String format(double value, double unit, String unitName) {
        if (!Double.isFinite(value)) {
            return "-";
        }
        return String.format(Locale.ROOT, "%.6g %s (%.6g u)", value * unit, unitName, value);
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
