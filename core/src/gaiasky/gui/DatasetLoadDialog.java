/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.data.group.DatasetOptions;
import gaiasky.data.group.DatasetOptions.DatasetLoadType;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Constants;
import gaiasky.util.GlobalResources;
import gaiasky.util.GuiUtils;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.TextFieldComparatorValidator;

public class DatasetLoadDialog extends GenericDialog {

    public OwnCheckBox particles, stars, clusters, variables, fadeIn, fadeOut;
    public OwnTextField dsName, magnitudeScale, fadeInMin, fadeInMax, fadeOutMin, fadeOutMax, profileDecay;
    public OwnSliderPlus particleSize, colorNoise;
    public ColorPicker particleColor, labelColor;
    public OwnSelectBox<ComponentType> componentType;

    private final String fileName;

    private final float sliderWidth;
    private final float fieldWidth;
    private final float titleWidth;
    private final float cpSize;
    private final float taWidth;

    public DatasetLoadDialog(final String title, final String fileName, final Skin skin, final Stage ui) {
        super(title, skin, ui);

        this.fileName = fileName;
        sliderWidth = 664f;
        fieldWidth = 288f;
        titleWidth = 288f;
        cpSize = 32f;
        taWidth = 800f;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        OwnLabel info = new OwnLabel(I18n.msg("gui.dsload.info"), skin, "hud-subheader");
        content.add(info).left().padBottom(pad15).row();

        // Table containing the actual widget
        Table container = new Table(skin);
        Container<Table> cont = new Container<>(container);

        // Radio buttons
        stars = new OwnCheckBox(I18n.msg("gui.dsload.stars"), skin, "radio", pad5);
        stars.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (stars.isChecked()) {
                    container.clear();
                    addStarsWidget(container);
                    pack();
                }
                return true;
            }
            return false;
        });
        stars.setChecked(true);
        content.add(stars).left().padBottom(pad10).row();

        particles = new OwnCheckBox(I18n.msg("gui.dsload.particles"), skin, "radio", pad5);
        particles.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (particles.isChecked()) {
                    container.clear();
                    addParticlesWidget(container);
                    pack();
                }
                return true;
            }
            return false;
        });
        particles.setChecked(false);
        content.add(particles).left().padBottom(pad10).row();

        clusters = new OwnCheckBox(I18n.msg("gui.dsload.clusters"), skin, "radio", pad5);
        clusters.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (clusters.isChecked()) {
                    container.clear();
                    addStarClustersWidget(container);
                    pack();
                }
                return true;
            }
            return false;
        });
        clusters.setChecked(false);
        content.add(clusters).left().padBottom(pad10).row();

        variables = new OwnCheckBox(I18n.msg("gui.dsload.variable"), skin, "radio", pad5);
        variables.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (variables.isChecked()) {
                    container.clear();
                    addVariableStarsWidget(container);
                    pack();
                }
                return true;
            }
            return false;
        });
        variables.setChecked(false);
        content.add(variables).left().padBottom(pad10 * 2f).row();

        new ButtonGroup<>(particles, stars, clusters, variables);

        content.add(cont).left();
    }

    private void addStarsWidget(Table container) {
        OwnLabel starProps = new OwnLabel(I18n.msg("gui.dsload.stars.properties"), skin, "hud-subheader");
        container.add(starProps).colspan(2).left().padTop(pad15).padBottom(pad10).row();

        // Name
        addFileName(container);

        // Label color
        addLabelColor(container);

        // Magnitude multiplier
        FloatValidator sclValidator = new FloatValidator(-100f, 100f);
        magnitudeScale = new OwnTextField("0.0", skin, sclValidator);
        magnitudeScale.setWidth(fieldWidth);
        container.add(new OwnLabel(I18n.msg("gui.dsload.magnitude.scale"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        container.add(GuiUtils.tooltipHg(magnitudeScale, "gui.dsload.magnitude.scale.tooltip", skin)).left().padBottom(pad10).row();

        // Fade
        addFadeAttributes(container);
        // Default fade out for stars
        fadeOut.setChecked(true);
        fadeOutMin.setText("10000");
        fadeOutMax.setText("80000");
    }

    private void addParticlesWidget(Table container) {

        OwnLabel particleProps = new OwnLabel(I18n.msg("gui.dsload.particles.properties"), skin, "hud-subheader");
        container.add(particleProps).colspan(2).left().padTop(pad15).padBottom(pad10).row();

        // Name
        addFileName(container);

        // Particle color
        addParticleColor(container);

        // Color noise
        colorNoise = new OwnSliderPlus(I18n.msg("gui.dsload.color.noise"), Constants.MIN_COLOR_NOISE, Constants.MAX_COLOR_NOISE, Constants.SLIDER_STEP_TINY, false, skin);
        colorNoise.setName("profile decay");
        colorNoise.setWidth(sliderWidth);
        colorNoise.setValue(0.2f);
        colorNoise.addListener(event -> {
            if (event instanceof ChangeEvent) {
                updateFrameBuffer();
                return true;
            }
            return false;
        });
        container.add(GuiUtils.tooltipHg(colorNoise, "gui.dsload.color.noise.tooltip", skin)).colspan(2).left().padBottom(pad15).row();

        // Label color
        addLabelColor(container);

        // Particle size
        particleSize = new OwnSliderPlus(I18n.msg("gui.dsload.size"), Constants.MIN_PARTICLE_SIZE, Constants.MAX_PARTICLE_SIZE, Constants.SLIDER_STEP_SMALL, false, skin);
        particleSize.setName("profile decay");
        particleSize.setWidth(sliderWidth);
        particleSize.setValue(10f);
        particleSize.addListener(event -> {
            if (event instanceof ChangeEvent) {
                updateFrameBuffer();
                return true;
            }
            return false;
        });
        container.add(particleSize).colspan(2).left().padBottom(pad10).row();

        // Profile falloff
        FloatValidator falloffVal = new FloatValidator(0.3f, 200f);
        profileDecay = new OwnTextField("5.0", skin, falloffVal);
        profileDecay.setWidth(fieldWidth);
        container.add(new OwnLabel(I18n.msg("gui.dsload.profiledecay"), skin, titleWidth)).left().padRight(pad10).padBottom(pad15);
        container.add(GuiUtils.tooltipHg(profileDecay, "gui.dsload.profiledecay.tooltip", skin)).left().padBottom(pad15).row();

        // Component type
        ComponentType[] componentTypes = new ComponentType[] { ComponentType.Others, ComponentType.Stars, ComponentType.Galaxies, ComponentType.Clusters, ComponentType.Asteroids, ComponentType.Locations };
        componentType = new OwnSelectBox<>(skin);
        componentType.setWidth(fieldWidth);
        componentType.setItems(componentTypes);
        componentType.setSelected(ComponentType.Galaxies);
        container.add(new OwnLabel(I18n.msg("gui.dsload.ct"), skin, titleWidth)).left().padRight(pad10).padBottom(pad5);
        container.add(componentType).left().padBottom(pad5).row();

        // Fade
        addFadeAttributes(container);
    }

    private void addStarClustersWidget(Table container) {
        OwnLabel clustersProps = new OwnLabel(I18n.msg("gui.dsload.clusters.properties"), skin, "hud-subheader");
        container.add(clustersProps).colspan(2).left().padTop(pad15).padBottom(pad10).row();

        // Info
        String scInfoStr = I18n.msg("gui.dsload.clusters.info") + '\n';
        int scLines = GlobalResources.countOccurrences(scInfoStr, '\n');
        TextArea scInfo = new OwnTextArea(scInfoStr, skin, "info");
        scInfo.setDisabled(true);
        scInfo.setPrefRows(scLines + 1);
        scInfo.setWidth(taWidth);
        scInfo.clearListeners();

        container.add(scInfo).colspan(2).left().padTop(pad5).padBottom(pad10).row();

        // Name
        addFileName(container);

        // Particle color
        addParticleColor(container);

        // Label color
        addLabelColor(container);

        // Component type
        ComponentType[] componentTypes = new ComponentType[] { ComponentType.Others, ComponentType.Stars, ComponentType.Galaxies, ComponentType.Clusters, ComponentType.Asteroids, ComponentType.Locations };
        componentType = new OwnSelectBox<>(skin);
        componentType.setWidth(fieldWidth);
        componentType.setItems(componentTypes);
        componentType.setSelected(ComponentType.Clusters);
        container.add(new OwnLabel(I18n.msg("gui.dsload.ct"), skin, titleWidth)).left().padRight(pad10).padBottom(pad5);
        container.add(componentType).left().padBottom(pad5).row();

        // Fade
        addFadeAttributes(container);

    }

    private void addVariableStarsWidget(Table container) {
        OwnLabel starProps = new OwnLabel(I18n.msg("gui.dsload.variable.properties"), skin, "hud-subheader");
        container.add(starProps).colspan(2).left().padTop(pad15).padBottom(pad10).row();

        // Info
        String scInfoStr = I18n.msg("gui.dsload.variable.info") + '\n';
        int scLines = GlobalResources.countOccurrences(scInfoStr, '\n');
        TextArea scInfo = new OwnTextArea(scInfoStr, skin, "info");
        scInfo.setDisabled(true);
        scInfo.setPrefRows(scLines + 1);
        scInfo.setWidth(taWidth);
        scInfo.clearListeners();

        container.add(scInfo).colspan(2).left().padTop(pad5).padBottom(pad10).row();

        // Name
        addFileName(container);

        // Label color
        addLabelColor(container);

        // Magnitude multiplier
        FloatValidator sclValidator = new FloatValidator(-100f, 100f);
        magnitudeScale = new OwnTextField("0.0", skin, sclValidator);
        magnitudeScale.setWidth(fieldWidth);
        container.add(new OwnLabel(I18n.msg("gui.dsload.magnitude.scale"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        container.add(GuiUtils.tooltipHg(magnitudeScale, "gui.dsload.magnitude.scale.tooltip", skin)).left().padBottom(pad10).row();

        // Fade
        addFadeAttributes(container);
        // Default fade out for stars
        fadeOut.setChecked(true);
        fadeOutMin.setText("10000");
        fadeOutMax.setText("80000");
    }

    private void addFileName(Table container) {
        dsName = new OwnTextField(fileName, skin);
        dsName.setWidth(fieldWidth);
        container.add(new OwnLabel(I18n.msg("gui.dsload.name"), skin, titleWidth)).left().padRight(pad10).padBottom(pad10);
        container.add(dsName).left().padBottom(pad10).row();
    }

    private void addParticleColor(Table container) {
        particleColor = new ColorPicker(new float[] { 0.3f, 0.3f, 1f, 1f }, stage, skin);
        particleColor.setNewColorRunnable(this::updateFrameBuffer);
        container.add(new OwnLabel(I18n.msg("gui.dsload.color"), skin, titleWidth)).left().padRight(pad10).padBottom(pad5);
        container.add(particleColor).size(cpSize).left().padBottom(pad5).row();
    }

    private void addLabelColor(Table container) {
        labelColor = new ColorPicker(new float[] { 0.3f, 0.3f, 1f, 1f }, stage, skin);
        container.add(new OwnLabel(I18n.msg("gui.dsload.color.label"), skin, titleWidth)).left().padRight(pad10).padBottom(pad5);
        Table lc = new Table(skin);
        lc.add(labelColor).size(cpSize);
        container.add(GuiUtils.tooltipHg(lc, "gui.dsload.color.label.tooltip", skin)).left().padBottom(pad5).row();
    }

    private void addFadeAttributes(Table container) {

        OwnLabel fadeLabel = new OwnLabel(I18n.msg("gui.dsload.fade"), skin, "hud-subheader");
        container.add(fadeLabel).colspan(2).left().padTop(pad15).padBottom(pad10).row();

        // Info
        String ssInfoStr = I18n.msg("gui.dsload.fade.info") + '\n';
        int ssLines = GlobalResources.countOccurrences(ssInfoStr, '\n');
        TextArea fadeInfo = new OwnTextArea(ssInfoStr, skin, "info");
        fadeInfo.setDisabled(true);
        fadeInfo.setPrefRows(ssLines + 1);
        fadeInfo.setWidth(taWidth);
        fadeInfo.clearListeners();

        container.add(fadeInfo).colspan(2).left().padTop(pad5).padBottom(pad10).row();

        // Fade in
        fadeIn = new OwnCheckBox(I18n.msg("gui.dsload.fade.in"), skin, pad5);
        container.add(fadeIn).left().padRight(pad10).padBottom(pad5);

        HorizontalGroup fadeInGroup = new HorizontalGroup();
        fadeInGroup.space(pad5);
        fadeInMin = new OwnTextField("0", skin);
        fadeInMax = new OwnTextField("10", skin);
        fadeInGroup.addActor(new OwnLabel("[", skin));
        fadeInGroup.addActor(fadeInMin);
        fadeInGroup.addActor(new OwnLabel(", ", skin));
        fadeInGroup.addActor(fadeInMax);
        fadeInGroup.addActor(new OwnLabel("] pc", skin));
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
        fadeIn.setChecked(true);
        fadeIn.setProgrammaticChangeEvents(true);
        fadeIn.setChecked(false);

        container.add(fadeInGroup).left().padBottom(pad5).row();

        // Fade out
        fadeOut = new OwnCheckBox(I18n.msg("gui.dsload.fade.out"), skin, pad5);
        container.add(fadeOut).left().padRight(pad10).padBottom(pad5);

        HorizontalGroup fadeOutGroup = new HorizontalGroup();
        fadeOutGroup.space(pad5);
        fadeOutMin = new OwnTextField("3000", skin);
        fadeOutMax = new OwnTextField("6000", skin);
        fadeOutGroup.addActor(new OwnLabel("[", skin));
        fadeOutGroup.addActor(fadeOutMin);
        fadeOutGroup.addActor(new OwnLabel(", ", skin));
        fadeOutGroup.addActor(fadeOutMax);
        fadeOutGroup.addActor(new OwnLabel("] pc", skin));
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
        fadeOut.setChecked(true);
        fadeOut.setProgrammaticChangeEvents(true);
        fadeOut.setChecked(false);

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

        container.add(fadeOutGroup).left().padBottom(pad5).row();
    }

    public DatasetOptions generateDatasetOptions() {
        DatasetOptions datasetOptions = new DatasetOptions();

        if (stars.isChecked()) {
            datasetOptions.type = DatasetLoadType.STARS;
            datasetOptions.magnitudeScale = magnitudeScale.getDoubleValue(0);
        } else if (particles.isChecked()) {
            datasetOptions.type = DatasetLoadType.PARTICLES;
            datasetOptions.ct = componentType.getSelected();
            datasetOptions.profileDecay = profileDecay.getDoubleValue(5d);
            datasetOptions.particleColor = particleColor.getPickedColorDouble();
            datasetOptions.particleColorNoise = colorNoise.getValue();
            datasetOptions.particleSize = particleSize.getValue()  * (Settings.settings.scene.renderer.pointCloud.isTriangles() ? .2e-3 : 1.0);
            datasetOptions.particleSizeLimits = new double[] { 2.5d, Math.min(100d, 5d * datasetOptions.particleSize) };
        } else if (clusters.isChecked()) {
            datasetOptions.type = DatasetLoadType.CLUSTERS;
            datasetOptions.ct = componentType.getSelected();
            datasetOptions.particleColor = particleColor.getPickedColorDouble();
        } else if (variables.isChecked()) {
            datasetOptions.type = DatasetLoadType.VARIABLES;
            datasetOptions.magnitudeScale = magnitudeScale.getDoubleValue(0);
        }
        // Common properties
        datasetOptions.catalogName = dsName.getText();
        datasetOptions.labelColor = labelColor.getPickedColorDouble();
        addFadeInfo(datasetOptions);

        return datasetOptions;
    }

    private void addFadeInfo(DatasetOptions dops) {
        if (fadeIn.isChecked()) {
            dops.fadeIn = new double[] { fadeInMin.getDoubleValue(0d), fadeInMax.getDoubleValue(0d) };
        }
        if (fadeOut.isChecked()) {
            dops.fadeOut = new double[] { fadeOutMin.getDoubleValue(2000d), fadeOutMax.getDoubleValue(8000d) };
        }
    }

    private void updateFrameBuffer() {
    }

    @Override
    protected void accept() {

    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
