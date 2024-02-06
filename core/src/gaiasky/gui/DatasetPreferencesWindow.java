/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.scene.Mapper;
import gaiasky.util.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector2d;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextArea;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.TextFieldComparatorValidator;

/**
 * Dataset preferences dialog.
 */
public class DatasetPreferencesWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetPreferencesWindow.class);

    private final CatalogInfo ci;
    private OwnTextField highlightSizeFactor, fadeInMin, fadeInMax, fadeOutMin, fadeOutMax;
    private OwnCheckBox allVisible, fadeIn, fadeOut;
    private final float taWidth;

    public DatasetPreferencesWindow(CatalogInfo ci,
                                    Skin skin,
                                    Stage stage) {
        super(I18n.msg("gui.preferences") + " - " + ci.name, skin, stage);
        this.ci = ci;
        this.taWidth = 800f;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));
        setModal(false);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        // Name
        content.add(new OwnLabel(I18n.msg("gui.dataset.name"), skin, "hud-subheader")).right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.name, skin)).left().padRight(pad18).padBottom(pad18).row();
        // Type
        content.add(new OwnLabel(I18n.msg("gui.dataset.type"), skin, "hud-subheader")).right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.type.toString(), skin)).left().padRight(pad18).padBottom(pad18).row();
        // Added
        content.add(new OwnLabel(I18n.msg("gui.dataset.loaded"), skin, "hud-subheader")).right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.loadDateUTC.atZone(Settings.settings.program.timeZone.getTimeZone()).toString(), skin)).left().padRight(pad18).padBottom(pad18).row();
        // Desc
        content.add(new OwnLabel(I18n.msg("gui.dataset.description"), skin, "hud-subheader")).right().padRight(pad18).padBottom(pad18 * 2f);
        content.add(new OwnLabel(TextUtils.capString(ci.description != null ? ci.description : ci.name, 55), skin)).left().padRight(pad18).padBottom(pad18 * 2f).row();

        // Highlight
        content.add(new OwnLabel(I18n.msg("gui.dataset.highlight"), skin, "hud-header")).left().colspan(2).padBottom(pad18).row();

        // Highlight size factor
        IValidator pointSizeValidator = new FloatValidator(Constants.MIN_DATASET_SIZE_FACTOR, Constants.MAX_DATASET_SIZE_FACTOR);
        highlightSizeFactor = new OwnTextField(Float.toString(ci.hlSizeFactor), skin, pointSizeValidator);
        content.add(new OwnLabel(I18n.msg("gui.dataset.highlight.size"), skin)).left().padRight(pad18).padBottom(pad20);
        content.add(highlightSizeFactor).left().padRight(pad18).padBottom(pad18).row();

        // All visible
        allVisible = new OwnCheckBox(I18n.msg("gui.dataset.highlight.allvisible"), skin, pad18);
        allVisible.setChecked(ci.hlAllVisible);
        content.add(allVisible).left().colspan(2).padBottom(pad18 * 2f).row();

        // Fade
        addFadeAttributes(content);

    }

    private void addFadeAttributes(Table container) {
        float tfw = 220f;

        OwnLabel fadeLabel = new OwnLabel(I18n.msg("gui.dsload.fade"), skin, "hud-header");
        container.add(fadeLabel).colspan(2).left().padTop(pad20).padBottom(pad18).row();

        // Info
        String ssInfoStr = I18n.msg("gui.dsload.fade.info") + '\n';
        int ssLines = GlobalResources.countOccurrences(ssInfoStr, '\n');
        TextArea fadeInfo = new OwnTextArea(ssInfoStr, skin, "info");
        fadeInfo.setDisabled(true);
        fadeInfo.setPrefRows(ssLines + 1);
        fadeInfo.setWidth(taWidth);
        fadeInfo.clearListeners();

        container.add(fadeInfo).colspan(2).left().padTop(pad10).padBottom(pad18).row();

        // Fade in
        fadeIn = new OwnCheckBox(I18n.msg("gui.dsload.fade.in"), skin, pad10);
        Vector2d fi = ci.entity != null ? Mapper.fade.get(ci.entity).fadeIn : null;
        container.add(fadeIn).left().padRight(pad18).padBottom(pad10);

        HorizontalGroup fadeInGroup = new HorizontalGroup();
        fadeInGroup.space(pad10);
        fadeInMin = new OwnTextField(fi != null ? String.format("%.14f", fi.x * Constants.U_TO_PC) : "0", skin);
        fadeInMin.setWidth(tfw);
        fadeInMax = new OwnTextField(fi != null ? String.format("%.14f", fi.y * Constants.U_TO_PC) : "1", skin);
        fadeInMax.setWidth(tfw);
        fadeInGroup.addActor(new OwnLabel("[", skin));
        fadeInGroup.addActor(fadeInMin);
        fadeInGroup.addActor(new OwnLabel(", ", skin));
        fadeInGroup.addActor(fadeInMax);
        fadeInGroup.addActor(new OwnLabel("] " + I18n.msg("gui.unit.pc"), skin));
        fadeIn.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                boolean disable = !fadeIn.isChecked();

                for (Actor child : fadeInGroup.getChildren()) {
                    if (child instanceof OwnLabel) {
                        ((OwnLabel) child).setDisabled(disable);
                    } else if (child instanceof OwnTextField) {
                        ((OwnTextField) child).setDisabled(disable);
                    }
                }
                return true;
            }
            return false;
        });
        fadeIn.setChecked(fi == null);
        fadeIn.setProgrammaticChangeEvents(true);
        fadeIn.setChecked(fi != null);

        container.add(fadeInGroup).left().padBottom(pad10).row();

        // Fade out
        fadeOut = new OwnCheckBox(I18n.msg("gui.dsload.fade.out"), skin, pad10);
        Vector2d fo = ci.entity != null ? Mapper.fade.get(ci.entity).fadeOut : null;
        container.add(fadeOut).left().padRight(pad18).padBottom(pad10);

        HorizontalGroup fadeOutGroup = new HorizontalGroup();
        fadeOutGroup.space(pad10);
        fadeOutMin = new OwnTextField(fo != null ? String.format("%.10f", fo.x * Constants.U_TO_PC) : "5000", skin);
        fadeOutMin.setWidth(tfw);
        fadeOutMax = new OwnTextField(fo != null ? String.format("%.10f", fo.y * Constants.U_TO_PC) : "10000", skin);
        fadeOutMax.setWidth(tfw);
        fadeOutGroup.addActor(new OwnLabel("[", skin));
        fadeOutGroup.addActor(fadeOutMin);
        fadeOutGroup.addActor(new OwnLabel(", ", skin));
        fadeOutGroup.addActor(fadeOutMax);
        fadeOutGroup.addActor(new OwnLabel("] " + I18n.msg("gui.unit.pc"), skin));
        fadeOut.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                boolean disable = !fadeOut.isChecked();

                for (Actor child : fadeOutGroup.getChildren()) {
                    if (child instanceof OwnLabel) {
                        ((OwnLabel) child).setDisabled(disable);
                    } else if (child instanceof OwnTextField) {
                        ((OwnTextField) child).setDisabled(disable);
                    }
                }
                return true;
            }
            return false;
        });
        fadeOut.setChecked(fo == null);
        fadeOut.setProgrammaticChangeEvents(true);
        fadeOut.setChecked(fo != null);

        // Validators
        FloatValidator fadeVal = new FloatValidator(0f, 1e10f);
        IValidator fadeInMinVal = new TextFieldComparatorValidator(fadeVal, new OwnTextField[] { fadeInMax, fadeOutMin, fadeOutMax }, null);
        IValidator fadeInMaxVal = new TextFieldComparatorValidator(fadeVal, new OwnTextField[] { fadeOutMin, fadeOutMax }, new OwnTextField[] { fadeInMin });
        IValidator fadeOutMinVal = new TextFieldComparatorValidator(fadeVal, new OwnTextField[] { fadeOutMax }, new OwnTextField[] { fadeInMin, fadeInMax });
        IValidator fadeOutMaxVal = new TextFieldComparatorValidator(fadeVal, null, new OwnTextField[] { fadeInMin, fadeInMax, fadeOutMin });

        // Set them
        fadeInMin.setValidator(fadeInMinVal);
        fadeInMax.setValidator(fadeInMaxVal);
        fadeOutMin.setValidator(fadeOutMinVal);
        fadeOutMax.setValidator(fadeOutMaxVal);

        container.add(fadeOutGroup).left().padBottom(pad10).row();
    }

    @Override
    protected boolean accept() {
        // Point size
        if (highlightSizeFactor.isValid()) {
            float newVal = Parser.parseFloat(highlightSizeFactor.getText());
            if (newVal != ci.hlSizeFactor) {
                ci.setHlSizeFactor(newVal);
            }
        }
        // All visible
        boolean vis = allVisible.isChecked();
        if (vis != ci.hlAllVisible) {
            ci.setHlAllVisible(vis);
        }
        // Fade in/out
        if (ci.entity != null) {
            var fade = Mapper.fade.get(ci.entity);
            if (fadeIn.isChecked()) {
                fade.setFadein(new double[] { fadeInMin.getDoubleValue(0), fadeInMax.getDoubleValue(1e1) });
                if (fade.fadeInMap == null) {
                    fade.setFadeInMap(new double[]{0, 1});
                }
            } else {
                fade.setFadein(null);
            }
            if (fadeOut.isChecked()) {
                fade.setFadeout(new double[] { fadeOutMin.getDoubleValue(1e5), fadeOutMax.getDoubleValue(1e6) });
                if (fade.fadeOutMap == null) {
                    fade.setFadeOutMap(new double[]{1, 0});
                }
            } else {
                fade.setFadeout(null);
            }
        }
        return true;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }
}
