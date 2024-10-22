/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.window.GenericDialog;
import gaiasky.scene.Mapper;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector2d;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.TextFieldComparatorValidator;

import java.text.DecimalFormat;

/**
 * Visual settings of a particular dataset.
 */
public class DatasetVisualSettingsWindow extends GenericDialog implements IObserver {
    private final CatalogInfo ci;
    private OwnTextField highlightSizeFactor, fadeInMin, fadeInMax, fadeOutMin, fadeOutMax;
    private OwnCheckBox allVisible, fadeIn, fadeOut;
    private OwnSliderPlus pointSize, minSolidAngle, maxSolidAngle;
    private final float taWidth, sliderWidth;

    private double backupMinSolidAngle, backupMaxSolidAngle;

    public DatasetVisualSettingsWindow(CatalogInfo ci,
                                       Skin skin,
                                       Stage stage) {
        super(I18n.msg("gui.dataset.visuals") + " - " + ci.name, skin, stage);
        this.ci = ci;
        this.taWidth = 800f;
        this.sliderWidth = 800f;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));
        setModal(false);

        // Build
        buildSuper();

        EventManager.instance.subscribe(this, Event.CATALOG_POINT_SIZE_SCALING_CMD);
    }

    @Override
    protected void build() {
        // PARTICLE ASPECT
        content.add(new OwnLabel(I18n.msg("gui.dataset.particleaspect"), skin, "hud-header")).left().colspan(2).padBottom(pad18).row();

        // Particle size
        pointSize = new OwnSliderPlus(I18n.msg("gui.dataset.size"), Constants.MIN_POINT_SIZE_SCALE, Constants.MAX_POINT_SIZE_SCALE,
                Constants.SLIDER_STEP_TINY, skin);
        pointSize.setWidth(sliderWidth);
        if (ci.entity != null) {
            var graph = Mapper.graph.get(ci.entity);
            var hl = Mapper.highlight.get(ci.entity);
            if (hl != null) {
                float pointScaling;
                if (graph.parent != null && Mapper.octree.has(graph.parent)) {
                    pointScaling = Mapper.highlight.get(graph.parent).pointscaling * hl.pointscaling;
                } else {
                    pointScaling = hl.pointscaling;
                }
                pointSize.setMappedValue(pointScaling);
            }
        }
        pointSize.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                double val = pointSize.getMappedValue();
                EventManager.publish(Event.CATALOG_POINT_SIZE_SCALING_CMD, pointSize, ci.name, val);
                return true;
            }
            return false;
        });
        content.add(pointSize).colspan(2).left().top().padBottom(pad18).row();

        if (ci.isParticleSet() && !ci.isStarSet()) {
            // Min/max solid angle only for particles.
            addMinMaxSolidAngle(content);
        }
        content.padBottom(pad20).row();

        // HIGHLIGHT
        content.add(new OwnLabel(I18n.msg("gui.dataset.highlight"), skin, "hud-header")).left().colspan(2).padTop(pad34).row();
        content.add(new OwnLabel(I18n.msg("gui.dataset.highlight.info"), skin, "default-scblue")).left().colspan(2).padTop(pad20).padBottom(pad18 * 2f).row();

        // Highlight size factor
        IValidator pointSizeValidator = new FloatValidator(Constants.MIN_DATASET_SIZE_FACTOR, Constants.MAX_DATASET_SIZE_FACTOR);
        highlightSizeFactor = new OwnTextField(Float.toString(ci.hlSizeFactor), skin, pointSizeValidator);
        content.add(new OwnLabel(I18n.msg("gui.dataset.highlight.size"), skin)).left().padRight(pad18).padBottom(pad20);
        content.add(highlightSizeFactor).left().padRight(pad18).padBottom(pad18).row();

        // All visible
        allVisible = new OwnCheckBox(I18n.msg("gui.dataset.highlight.allvisible"), skin, pad18);
        allVisible.setChecked(ci.hlAllVisible);
        content.add(allVisible).left().colspan(2).padBottom(pad18 * 2f).row();

        // FADE
        addFadeAttributes(content);

    }

    private void addMinMaxSolidAngle(Table container) {
        var set = Mapper.particleSet.get(ci.entity);
        backupMinSolidAngle = set.particleSizeLimits[0];
        backupMaxSolidAngle = set.particleSizeLimits[1];

        // Min solid angle
        minSolidAngle = new OwnSliderPlus(I18n.msg("gui.dsload.solidangle.min"), Constants.MIN_MIN_SOLID_ANGLE, Constants.MAX_MIN_SOLID_ANGLE, Constants.SLIDER_STEP_WEENY, false, skin);
        minSolidAngle.setName("min solid angle");
        minSolidAngle.setWidth(sliderWidth);
        minSolidAngle.setValue((float) set.particleSizeLimits[0]);
        minSolidAngle.setNumberFormatter(new DecimalFormat("####0.####"));
        minSolidAngle.addListener(event -> {
            if (event instanceof ChangeEvent) {
                set.particleSizeLimits[0] = minSolidAngle.getValue();
                return true;
            }
            return false;
        });
        container.add(minSolidAngle).top().left().colspan(2).left().padBottom(pad18).row();

        // Max solid angle
        maxSolidAngle = new OwnSliderPlus(I18n.msg("gui.dsload.solidangle.max"), Constants.MIN_MAX_SOLID_ANGLE, Constants.MAX_MAX_SOLID_ANGLE, Constants.SLIDER_STEP_WEENY, false, skin);
        maxSolidAngle.setName("max solid angle");
        maxSolidAngle.setWidth(sliderWidth);
        maxSolidAngle.setValue((float) set.particleSizeLimits[1]);
        maxSolidAngle.setNumberFormatter(new DecimalFormat("####0.####"));
        maxSolidAngle.addListener(event -> {
            if (event instanceof ChangeEvent) {
                set.particleSizeLimits[1] = maxSolidAngle.getValue();
                return true;
            }
            return false;
        });
        container.add(maxSolidAngle).top().left().colspan(2).left().padBottom(pad18).row();
    }

    private void addFadeAttributes(Table container) {
        float tfw = 220f;

        OwnLabel fadeLabel = new OwnLabel(I18n.msg("gui.dsload.fade"), skin, "hud-header");
        container.add(fadeLabel).colspan(2).left().padTop(pad20).padBottom(pad20).row();

        // Info
        String ssInfoStr = I18n.msg("gui.dsload.fade.info") + '\n';
        int ssLines = GlobalResources.countOccurrences(ssInfoStr, '\n');
        TextArea fadeInfo = new OwnTextArea(ssInfoStr, skin, "info");
        fadeInfo.setDisabled(true);
        fadeInfo.setPrefRows(ssLines + 1);
        fadeInfo.setWidth(taWidth);
        fadeInfo.clearListeners();

        container.add(fadeInfo).colspan(2).left().padBottom(pad18).row();

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
        IValidator fadeInMinVal = new TextFieldComparatorValidator(fadeVal, new OwnTextField[]{fadeInMax, fadeOutMin, fadeOutMax}, null);
        IValidator fadeInMaxVal = new TextFieldComparatorValidator(fadeVal, new OwnTextField[]{fadeOutMin, fadeOutMax}, new OwnTextField[]{fadeInMin});
        IValidator fadeOutMinVal = new TextFieldComparatorValidator(fadeVal, new OwnTextField[]{fadeOutMax}, new OwnTextField[]{fadeInMin, fadeInMax});
        IValidator fadeOutMaxVal = new TextFieldComparatorValidator(fadeVal, null, new OwnTextField[]{fadeInMin, fadeInMax, fadeOutMin});

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
                fade.setFadein(new double[]{fadeInMin.getDoubleValue(0), fadeInMax.getDoubleValue(1e1)});
                if (fade.fadeInMap == null) {
                    fade.setFadeInMap(new double[]{0, 1});
                }
            } else {
                fade.setFadein(null);
            }
            if (fadeOut.isChecked()) {
                fade.setFadeout(new double[]{fadeOutMin.getDoubleValue(1e5), fadeOutMax.getDoubleValue(1e6)});
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
        if (ci.isParticleSet() && !ci.isStarSet()) {
            // Roll back min/max solid angle only for particles.
            var set = Mapper.particleSet.get(ci.entity);
            set.particleSizeLimits[0] = backupMinSolidAngle;
            set.particleSizeLimits[1] = backupMaxSolidAngle;
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (isVisible() && pointSize != null && source != pointSize && event == Event.CATALOG_POINT_SIZE_SCALING_CMD) {
            String datasetName = (String) data[0];
            double val = (Double) data[1];
            OwnSliderPlus slider = pointSize;
            slider.setProgrammaticChangeEvents(false);
            slider.setMappedValue(val);
            slider.setProgrammaticChangeEvents(true);
        }

    }
}
