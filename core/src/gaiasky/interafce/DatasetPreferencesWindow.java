/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.interafce.beans.AttributeComboBoxBean;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.*;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.FilterRule;
import gaiasky.util.filter.FilterRule.IComparator;
import gaiasky.util.filter.attrib.*;
import gaiasky.util.math.Vector2d;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.ucd.UCD;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.TextFieldComparatorValidator;

import java.time.ZoneId;

public class DatasetPreferencesWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetPreferencesWindow.class);

    private final CatalogInfo ci;
    private OwnTextField highlightSizeFactor, fadeInMin, fadeInMax, fadeOutMin, fadeOutMax;
    private OwnCheckBox allVisible, fadeIn, fadeOut;
    private Table filterTable;
    private final DatasetPreferencesWindow dpw;
    private Filter filter;
    private boolean filterEdited;
    private float taWidth;

    public DatasetPreferencesWindow(CatalogInfo ci, Skin skin, Stage stage) {
        super(I18n.txt("gui.preferences") + " - " + ci.name, skin, stage);
        this.ci = ci;
        this.dpw = this;
        this.filterEdited = false;
        this.taWidth = 800f;

        setAcceptText(I18n.txt("gui.ok"));
        setCancelText(I18n.txt("gui.cancel"));
        setModal(false);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        // Name
        content.add(new OwnLabel(I18n.txt("gui.dataset.name"), skin, "hud-subheader")).right().padRight(pad10).padBottom(pad10);
        content.add(new OwnLabel(ci.name, skin)).left().padRight(pad10).padBottom(pad10).row();
        // Type
        content.add(new OwnLabel(I18n.txt("gui.dataset.type"), skin, "hud-subheader")).right().padRight(pad10).padBottom(pad10);
        content.add(new OwnLabel(ci.type.toString(), skin)).left().padRight(pad10).padBottom(pad10).row();
        // Added
        content.add(new OwnLabel(I18n.txt("gui.dataset.loaded"), skin, "hud-subheader")).right().padRight(pad10).padBottom(pad10);
        content.add(new OwnLabel(ci.loadDateUTC.atZone(ZoneId.systemDefault()).toString(), skin)).left().padRight(pad10).padBottom(pad10).row();
        // Desc
        content.add(new OwnLabel(I18n.txt("gui.dataset.description"), skin, "hud-subheader")).right().padRight(pad10).padBottom(pad10 * 2f);
        content.add(new OwnLabel(TextUtils.capString(ci.description, 55), skin)).left().padRight(pad10).padBottom(pad10 * 2f).row();

        // Highlight
        content.add(new OwnLabel(I18n.txt("gui.dataset.highlight"), skin, "hud-header")).left().colspan(2).padBottom(pad10).row();

        // Highlight size factor
        IValidator pointSizeValidator = new FloatValidator(Constants.MIN_DATASET_SIZE_FACTOR, Constants.MAX_DATASET_SIZE_FACTOR);
        highlightSizeFactor = new OwnTextField(Float.toString(ci.hlSizeFactor), skin, pointSizeValidator);
        content.add(new OwnLabel(I18n.txt("gui.dataset.highlight.size"), skin)).left().padRight(pad10).padBottom(pad10);
        content.add(highlightSizeFactor).left().padRight(pad10).padBottom(pad10).row();

        // All visible
        allVisible = new OwnCheckBox(I18n.txt("gui.dataset.highlight.allvisible"), skin, pad10);
        allVisible.setChecked(ci.hlAllVisible);
        content.add(allVisible).left().colspan(2).padBottom(pad10 * 2f).row();

        // Fade
        addFadeAttributes(content);

        // Filters
        content.add(new OwnLabel(I18n.txt("gui.dataset.filter"), skin, "hud-header")).left().colspan(2).padBottom(pad5).row();
        filterTable = new Table(skin);
        content.add(filterTable).left().colspan(2).row();

        filter = ci.filter != null ? ci.filter.deepCopy() : null;
        generateFilterTable(filter);

    }

    private void addFadeAttributes(Table container) {
        float tfw = 220f;

        OwnLabel fadeLabel = new OwnLabel(I18n.txt("gui.dsload.fade"), skin, "hud-header");
        container.add(fadeLabel).colspan(2).left().padTop(pad15).padBottom(pad10).row();

        // Info
        String ssInfoStr = I18n.txt("gui.dsload.fade.info") + '\n';
        int ssLines = GlobalResources.countOccurrences(ssInfoStr, '\n');
        TextArea fadeInfo = new OwnTextArea(ssInfoStr, skin, "info");
        fadeInfo.setDisabled(true);
        fadeInfo.setPrefRows(ssLines + 1);
        fadeInfo.setWidth(taWidth);
        fadeInfo.clearListeners();

        container.add(fadeInfo).colspan(2).left().padTop(pad5).padBottom(pad10).row();

        // Fade in
        fadeIn = new OwnCheckBox(I18n.txt("gui.dsload.fade.in"), skin, pad5);
        Vector2d fi = ci.object != null ? ci.object.getFadeIn() : null;
        container.add(fadeIn).left().padRight(pad10).padBottom(pad5);

        HorizontalGroup fadeInGroup = new HorizontalGroup();
        fadeInGroup.space(pad5);
        fadeInMin = new OwnTextField(fi != null ? String.format("%.14f", fi.x * Constants.U_TO_PC) : "0", skin);
        fadeInMin.setWidth(tfw);
        fadeInMax = new OwnTextField(fi != null ? String.format("%.14f", fi.y * Constants.U_TO_PC) : "1", skin);
        fadeInMax.setWidth(tfw);
        fadeInGroup.addActor(new OwnLabel("[", skin));
        fadeInGroup.addActor(fadeInMin);
        fadeInGroup.addActor(new OwnLabel(", ", skin));
        fadeInGroup.addActor(fadeInMax);
        fadeInGroup.addActor(new OwnLabel("] " + I18n.txt("gui.unit.pc"), skin));
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

        container.add(fadeInGroup).left().padBottom(pad5).row();

        // Fade out
        fadeOut = new OwnCheckBox(I18n.txt("gui.dsload.fade.out"), skin, pad5);
        Vector2d fo = ci.object != null ? ci.object.getFadeOut() : null;
        container.add(fadeOut).left().padRight(pad10).padBottom(pad5);

        HorizontalGroup fadeOutGroup = new HorizontalGroup();
        fadeOutGroup.space(pad5);
        fadeOutMin = new OwnTextField(fo != null ? String.format("%.1f", fo.x * Constants.U_TO_PC) : "5000", skin);
        fadeOutMin.setWidth(tfw);
        fadeOutMax = new OwnTextField(fo != null ? String.format("%.1f", fo.y * Constants.U_TO_PC) : "10000", skin);
        fadeOutMax.setWidth(tfw);
        fadeOutGroup.addActor(new OwnLabel("[", skin));
        fadeOutGroup.addActor(fadeOutMin);
        fadeOutGroup.addActor(new OwnLabel(", ", skin));
        fadeOutGroup.addActor(fadeOutMax);
        fadeOutGroup.addActor(new OwnLabel("] " + I18n.txt("gui.unit.pc"), skin));
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

        container.add(fadeOutGroup).left().padBottom(pad5).row();
    }

    private void generateFilterTable(Filter filter) {
        float minSelectWidth = 160f;
        filterTable.clearChildren();
        if (filter != null && filter.hasRules()) {
            // Operation
            OwnSelectBox<String> operation = new OwnSelectBox<>(skin);
            operation.setWidth(minSelectWidth);
            operation.setItems("and", "or", "xor");
            operation.setSelected(filter.getOperationString().toLowerCase());
            operation.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    String newOp = operation.getSelected();
                    filter.setOperation(newOp);
                    filterEdited = true;
                    return true;
                }
                return false;
            });
            filterTable.add(new OwnLabel(I18n.txt("gui.dataset.filter.operation"), skin)).left().padRight(pad10 * 2f).padBottom(pad10);
            filterTable.add(operation).left().expandX().padBottom(pad10).row();

            // Rules
            Array<FilterRule> rules = filter.getRules();
            Table rulesTable = new Table(skin);
            filterTable.add(rulesTable).colspan(2);

            for (FilterRule rule : rules) {
                // UNIT
                OwnLabel unit = new OwnLabel(rule.getAttribute().getUnit(), skin);

                // ATTRIBUTE
                boolean stars = ci.object instanceof StarGroup || ci.object instanceof OctreeWrapper;
                Array<AttributeComboBoxBean> attrs = new Array<>(false, stars ? 12 : 7);
                // Add particle attributes (dist, alpha, delta)
                attrs.add(new AttributeComboBoxBean(new AttributeDistance()));
                attrs.add(new AttributeComboBoxBean(new AttributeRA()));
                attrs.add(new AttributeComboBoxBean(new AttributeDEC()));
                attrs.add(new AttributeComboBoxBean(new AttributeEclLatitude()));
                attrs.add(new AttributeComboBoxBean(new AttributeEclLongitude()));
                attrs.add(new AttributeComboBoxBean(new AttributeGalLatitude()));
                attrs.add(new AttributeComboBoxBean(new AttributeGalLongitude()));
                if (stars) {
                    // Star-only attributes (appmag, absmag, mualpha, mudelta, radvel)
                    attrs.add(new AttributeComboBoxBean(new AttributeAppmag()));
                    attrs.add(new AttributeComboBoxBean(new AttributeAbsmag()));
                    attrs.add(new AttributeComboBoxBean(new AttributeMualpha()));
                    attrs.add(new AttributeComboBoxBean(new AttributeMudelta()));
                    attrs.add(new AttributeComboBoxBean(new AttributeRadvel()));
                }
                // Colors
                attrs.add(new AttributeComboBoxBean(new AttributeColorRed()));
                attrs.add(new AttributeComboBoxBean(new AttributeColorGreen()));
                attrs.add(new AttributeComboBoxBean(new AttributeColorBlue()));
                // Extra attributes
                if (ci.object instanceof ParticleGroup) {
                    ParticleGroup pg = (ParticleGroup) ci.object;
                    if (pg.size() > 0) {
                        IParticleRecord first = pg.get(0);
                        if (first.hasExtra()) {
                            ObjectDoubleMap.Keys<UCD> ucds = first.extraKeys();
                            for (UCD ucd : ucds)
                                attrs.add(new AttributeComboBoxBean(new AttributeUCD(ucd)));
                        }
                    }
                }
                OwnSelectBox<AttributeComboBoxBean> attribute = new OwnSelectBox<>(skin);
                attribute.setItems(attrs);
                attribute.setSelected(getAttributeBean(rule.getAttribute(), attrs));
                attribute.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        IAttribute newAttr = attribute.getSelected().attr;
                        rule.setAttribute(newAttr);
                        // Update unit
                        unit.setText(newAttr.getUnit());
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });
                rulesTable.add(attribute).left().padRight(pad10).padBottom(pad5);

                // COMPARATOR
                String[] cmps = new String[] { "<", "<=", ">", ">=", "==", "!=" };
                OwnSelectBox<String> comparator = new OwnSelectBox<>(skin);
                comparator.setWidth(minSelectWidth);
                comparator.setItems(cmps);
                comparator.setSelected(rule.getComparator().toString());
                comparator.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        IComparator newComp = rule.getComparatorFromString(comparator.getSelected());
                        rule.setComparator(newComp);
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });
                rulesTable.add(comparator).left().padRight(pad10).padBottom(pad5);

                // VALUE
                FloatValidator fval = new FloatValidator(-Float.MAX_VALUE, Float.MAX_VALUE);
                OwnTextField value = new OwnTextField(Double.toString(rule.getValue()), skin, fval);
                value.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        if (value.isValid()) {
                            try {
                                rule.setValue(Float.parseFloat(value.getText()));
                                filterEdited = true;
                                return true;
                            } catch (Exception e) {
                                logger.error(e);
                                return false;
                            }
                        }
                        return false;
                    }
                    return false;
                });
                rulesTable.add(value).left().padRight(pad10).padBottom(pad5);

                // UNIT
                rulesTable.add(unit).left().padRight(pad10 * 3f).padBottom(pad5);

                // RUBBISH
                OwnTextIconButton rubbish = new OwnTextIconButton("", skin, "rubbish");
                rubbish.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.filter.rule.remove"), skin));
                rubbish.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        deleteRule(filter, rule);
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });
                rulesTable.add(rubbish).left().padBottom(pad5).row();
            }

            // New rule button
            OwnTextIconButton addRule = new OwnTextIconButton(I18n.txt("gui.dataset.filter.rule.add"), skin, "add");
            addRule.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.filter.rule.add"), skin));
            addRule.pad(pad10);
            rulesTable.add(addRule).left().padTop(pad10).row();
            addRule.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    dpw.addRule(filter);
                    filterEdited = true;
                    return true;
                }
                return false;
            });
        } else {
            // Add
            filterTable.add(new OwnLabel(I18n.txt("gui.dataset.filter.nofilters"), skin)).left().padBottom(pad10).row();
            OwnTextIconButton addFilter = new OwnTextIconButton(I18n.txt("gui.dataset.filter.add"), skin, "add");
            addFilter.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.filter.add"), skin));
            addFilter.pad(pad10);
            filterTable.add(addFilter).left().padBottom(pad5).row();
            addFilter.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    dpw.addFilter();
                    filterEdited = true;
                    return true;
                }
                return false;
            });
        }
        pack();
    }

    private void deleteRule(Filter filter, FilterRule rule) {
        if (filter != null && rule != null) {
            boolean removed = filter.removeRule(rule);
            if (removed) {
                // Reload table
                GaiaSky.postRunnable(() -> generateFilterTable(filter));
            }
        }
    }

    private void addRule(Filter filter) {
        if (filter != null) {
            FilterRule rule = new FilterRule("<", new AttributeDistance(), 500);
            filter.addRule(rule);
            // Reload table
            GaiaSky.postRunnable(() -> generateFilterTable(filter));
        }
    }

    private void addFilter() {
        FilterRule rule = new FilterRule("<", new AttributeDistance(), 500);
        filter = new Filter(rule);
        // Reload table
        GaiaSky.postRunnable(() -> generateFilterTable(filter));
    }

    private AttributeComboBoxBean getAttributeBean(IAttribute attr, Array<AttributeComboBoxBean> attrs) {
        for (AttributeComboBoxBean attribute : attrs) {
            if (attr.toString().contains(attribute.name)) {
                return attribute;
            }
        }
        return null;
    }

    @Override
    protected void accept() {
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
        if (ci.object != null) {
            if (fadeIn.isChecked()) {
                ci.object.setFadein(new double[] { fadeInMin.getDoubleValue(0), fadeInMax.getDoubleValue(1e1) });
            } else {
                ci.object.setFadein(null);
            }
            if (fadeOut.isChecked()) {
                ci.object.setFadeout(new double[] { fadeOutMin.getDoubleValue(1e5), fadeOutMax.getDoubleValue(1e6) });
            } else {
                ci.object.setFadeout(null);
            }
        }
        // Filter
        if (filterEdited) {
            if (ci.filter != null) {
                synchronized (ci.filter) {
                    ci.filter = filter.hasRules() ? filter : null;
                    ci.highlight(ci.highlighted);
                }
            } else {
                ci.filter = filter.hasRules() ? filter : null;
                ci.highlight(ci.highlighted);
            }
        }

    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }
}
