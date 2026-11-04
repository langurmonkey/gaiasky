/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import gaiasky.GaiaSky;
import gaiasky.gui.beans.AttributeComboBoxBean;
import gaiasky.gui.window.GenericDialog;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.record.ParticleType;
import gaiasky.util.DatasetCard;
import gaiasky.util.Logger;
import gaiasky.util.TextUtils;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.FilterRule;
import gaiasky.util.filter.attrib.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.ucd.UCD;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;

import java.util.Locale;

/**
 * Window to define filters for a dataset.
 */
public class DatasetFiltersWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetFiltersWindow.class);

    /** Operators a numeric column accepts. **/
    private static final String[] NUMERIC_COMPARATORS = {"<", "<=", ">", ">=", "==", "!="};
    /** Operators a categorical column accepts: a category has equality and nothing else. **/
    private static final String[] CATEGORICAL_COMPARATORS = {"==", "!="};
    /** Side of the square icon that shows the type of a column. **/
    private static final float TYPE_ICON_SIZE = 30f;

    private final DatasetCard ci;
    private Table filterTable;
    private Filter filter;
    private OwnTextIconButton addFilterOrRule;
    private boolean filterEdited;

    public DatasetFiltersWindow(DatasetCard ci,
                                Skin skin,
                                Stage stage) {
        super(I18n.msg("gui.dataset.filters") + " - " + ci.name, skin, stage);
        this.ci = ci;
        this.filterEdited = false;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));
        setModal(false);

        content.add().width(700f).row();
        content.getMinHeight();

        // Build
        buildSuper();
    }

    private boolean hasRules() {
        return filter != null && filter.hasRules();
    }

    private void updateButtonTextAndTooltip(OwnTextIconButton button) {
        boolean hasRules = hasRules();
        button.removeTooltipListeners();

        button.setText(I18n.msg(hasRules ? "gui.dataset.filter.rule.add" : "gui.dataset.filter.add"));
        button.addListener(new OwnTextTooltip(I18n.msg(hasRules ? "gui.tooltip.dataset.filter.rule.add" : "gui.tooltip.dataset.filter.add"), skin));
    }

    @Override
    protected void build() {
        if (ci.hasParticleAttributes()) {
            filter = ci.filter != null ? ci.filter.deepCopy() : null;

            // Title
            content.add(new OwnLabel(I18n.msg("gui.dataset.filter"), skin, "hud-header")).left().colspan(2).padBottom(pad18).padTop(pad20).row();

            // Button
            addFilterOrRule = new OwnTextIconButton("", skin, "add");
            addFilterOrRule.setSpace(pad18);
            addFilterOrRule.pad(pad18);
            updateButtonTextAndTooltip(addFilterOrRule);
            addFilterOrRule.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (hasRules()) {
                        ((DatasetFiltersWindow) me).addRule(filter);
                        updateButtonTextAndTooltip(addFilterOrRule);
                        filterEdited = true;
                        return true;
                    } else {
                        ((DatasetFiltersWindow) me).addFilter();
                        updateButtonTextAndTooltip(addFilterOrRule);
                        filterEdited = true;
                        return true;
                    }
                }
                return false;
            });
            content.add(addFilterOrRule).left().padBottom(pad34).row();

            // Filters table
            filterTable = new Table(skin);
            filterTable.top().left();
            var scrollPane = new OwnScrollPane(filterTable, skin, "minimalist-nobg");
            scrollPane.setScrollbarsVisible(true);
            scrollPane.setFadeScrollBars(false);
            scrollPane.setScrollingDisabled(true, false);
            scrollPane.setWidth(1180f);
            scrollPane.setHeight(500f);
            content.add(scrollPane).left().colspan(2).row();

            generateFilterTable(filter);
        }

    }

    private void generateFilterTable(Filter filter) {
        float minSelectWidth = 160f;
        filterTable.clearChildren();
        if (filter != null && filter.hasRules()) {
            OwnSelectBox<String> operation = new OwnSelectBox<>(skin);
            operation.setWidth(minSelectWidth);
            operation.setItems("and", "or", "xor");
            operation.setSelected(filter.getOperationString().toLowerCase(Locale.ROOT));
            operation.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    String newOp = operation.getSelected();
                    filter.setOperation(newOp);
                    filterEdited = true;
                    return true;
                }
                return false;
            });
            filterTable.add(new OwnLabel(I18n.msg("gui.dataset.filter.operation"), skin)).left().padRight(pad18 * 2f).padBottom(pad18);
            filterTable.add(operation).left().expandX().padBottom(pad18).row();

            // Rules
            Array<FilterRule> rules = filter.getRules();
            Table rulesTable = new Table(skin);
            filterTable.add(rulesTable).colspan(2);

            ParticleType type;
            if (Mapper.octree.has(ci.entity)) {
                type = ParticleType.STAR;
            } else {
                var set = Mapper.particleSet.has(ci.entity) ? Mapper.particleSet.get(ci.entity) : Mapper.starSet.get(ci.entity);
                type = ParticleType.type(set);
            }
            var attributes = type.attributes;
            // The column types come from the dataset, not from a record, so a rule and the operator
            // list it can use are decided once per column.
            var set = getSet();

            for (FilterRule rule : rules) {
                // The mode follows the column, and it is what evaluate() branches on, so forcing it
                // here keeps the operator list and the evaluation in agreement.
                rule.setMode(modeOf(rule.getAttribute()));
                boolean categorical = rule.getMode() == FilterRule.Mode.CATEGORICAL;

                // UNIT
                var unitStr = rule.getAttribute().getUnit();
                var unitStrCapped = unitStr != null ? TextUtils.capString(unitStr, 12) : "";

                OwnLabel unit = new OwnLabel(unitStrCapped, skin);
                unit.setWidth(130f);
                if (unitStr != null && unitStr.length() > unitStrCapped.length()) {
                    unit.addListener(new OwnTextTooltip(unitStr, skin));
                }

                // ATTRIBUTE
                Array<AttributeComboBoxBean> attrs = new Array<>(false, 15);
                // Add particle attributes (dist, alpha, delta)
                for (var attrClass : attributes) {
                    try {
                        var instance = (IAttribute) attrClass.getConstructors()[0].newInstance();
                        attrs.add(new AttributeComboBoxBean(instance));
                    } catch (Exception e) {
                        logger.error("Error instantiating class: " + attrClass, e);
                    }
                }
                // Extra attributes
                if (set != null && !set.data().isEmpty()) {
                    var first = set.data().getFirst();
                    if (first.hasExtra()) {
                        ObjectMap.Keys<UCD> ucds = first.extraKeys();
                        for (UCD ucd : ucds)
                            attrs.add(new AttributeComboBoxBean(new AttributeUCD(ucd, set.isCategoricalColumn(ucd))));
                    }
                }

                // Value text field and validator
                FloatValidator numberValidator = new FloatValidator(-Float.MAX_VALUE, Float.MAX_VALUE);
                FloatValidator colorValidator = new FloatValidator(0, 1);
                // Show the text the comparison will actually use: a category is what is written.
                String valueText = rule.getValue() != null ? FilterRule.asText(rule.getValue()) : "";
                OwnTextField value = new OwnTextField(valueText,
                                                      skin,
                                                      validatorFor(rule.getAttribute(), categorical, numberValidator, colorValidator));
                value.setHintTooltip(valuesHint(set, rule.getAttribute()));

                OwnSelectBox<AttributeComboBoxBean> attribute = new OwnSelectBox<>(skin);
                attribute.setItems(attrs);
                var selectedAttr = getAttributeBean(rule.getAttribute(), attrs);
                attribute.setSelected(selectedAttr != null ? selectedAttr : attrs.first());

                // TYPE: an icon of fixed width, so the columns of the rules line up whatever the
                // attribute holds.
                Container<Image> typeBox = new Container<>(typeIcon(skin, categorical));
                var typeTooltip = new OwnTextTooltip(typeTooltipText(categorical), skin);
                typeBox.addListener(typeTooltip);

                // COMPARATOR
                OwnSelectBox<String> comparator = new OwnSelectBox<>(skin);
                comparator.setWidth(minSelectWidth);
                comparator.setItems(comparatorsFor(rule.getMode()));
                comparator.setSelected(rule.getComparator().toString());
                comparator.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        rule.setComparator(rule.getComparatorFromString(comparator.getSelected()));
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });

                // VALUE
                value.setWidth(280f);
                value.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        if (value.isValid()) {
                            // The mode is read from the rule, not from the attribute the field was
                            // built for: the field may have been retargeted since.
                            if (rule.getMode() == FilterRule.Mode.CATEGORICAL) {
                                // A category is exactly what is typed: parsing it would turn the
                                // code "007" into the number 7.
                                rule.setValue(value.getText());
                            } else {
                                try {
                                    rule.setValue(Float.parseFloat(value.getText()));
                                } catch (Exception e) {
                                    rule.setValue(value.getText());
                                }
                            }
                            filterEdited = true;
                            return true;
                        }
                        return false;
                    }
                    return false;
                });

                // ATTRIBUTE listener, once the widgets it has to touch exist
                attribute.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        IAttribute newAttr = attribute.getSelected().attr;
                        rule.setAttribute(newAttr);
                        // A different column may hold a different kind of value: the mode, the
                        // operator list, the validator and the unit all follow it.
                        rule.setMode(modeOf(newAttr));
                        boolean newCategorical = rule.getMode() == FilterRule.Mode.CATEGORICAL;
                        comparator.setItems(comparatorsFor(rule.getMode()));
                        comparator.setSelected(rule.getComparator().toString());
                        value.setValidator(validatorFor(newAttr, newCategorical, numberValidator, colorValidator));
                        // The hint follows the column, like the operator list does.
                        value.setHintTooltip(valuesHint(set, newAttr));
                        // The type icon and its tooltip follow the column too.
                        typeBox.setActor(typeIcon(skin, newCategorical));
                        typeTooltip.setText(typeTooltipText(newCategorical));
                        if (newCategorical) {
                            // A value typed for another column is not a category: left in place, it
                            // would filter a text column against a number and match nothing.
                            rule.setValue("");
                            value.setText("");
                        }
                        var newUnitStr = newAttr.getUnit();
                        unit.setText(newUnitStr != null ? TextUtils.capString(newUnitStr, 12) : "");
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });

                // ROW
                rulesTable.add(typeBox).size(TYPE_ICON_SIZE).left().padRight(pad18).padBottom(pad10);
                rulesTable.add(attribute).left().padRight(pad18).padBottom(pad10);
                rulesTable.add(comparator).left().padRight(pad18).padBottom(pad10);
                rulesTable.add(value).left().padRight(pad18).padBottom(pad10);
                rulesTable.add(unit).left().padRight(pad18 * 3f).padBottom(pad10);

                // RUBBISH
                OwnTextIconButton rubbish = new OwnTextIconButton("", skin, "rubbish");
                rubbish.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.dataset.filter.rule.remove"), skin));
                rubbish.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        deleteRule(filter, rule);
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });
                rulesTable.add(rubbish).left().padBottom(pad10).row();
            }

        } else {
            // Add
            filterTable.add(new OwnLabel(I18n.msg("gui.dataset.filter.nofilters"), skin)).left().padBottom(pad18).row();
        }
        pack();
    }

    private void deleteRule(Filter filter,
                            FilterRule rule) {
        if (filter != null && rule != null) {
            boolean removed = filter.removeRule(rule);
            if (removed) {
                // Reload table
                GaiaSky.postRunnable(() -> {
                    generateFilterTable(filter);
                    updateButtonTextAndTooltip(addFilterOrRule);
                });
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

    /**
     * Builds the icon that shows the type of a column.
     *
     * @param skin        The skin.
     * @param categorical Whether the column is categorical.
     *
     * @return The icon.
     */
    private static Image typeIcon(Skin skin,
                                  boolean categorical) {
        Image icon = new Image(skin.getDrawable(categorical ? "icon-categorical" : "icon-numeric"));
        icon.setScaling(Scaling.fit);
        return icon;
    }

    /**
     * The text of the tooltip that explains the type icon.
     *
     * @param categorical Whether the column is categorical.
     *
     * @return The tooltip text.
     */
    private static String typeTooltipText(boolean categorical) {
        return I18n.msg(categorical ? "gui.dataset.filter.type.categorical" : "gui.dataset.filter.type.numeric");
    }

    /**
     * @return The particle set of the dataset, or null if the entity does not hold one.
     */
    private ParticleSet getSet() {
        if (Mapper.particleSet.has(ci.entity)) {
            return Mapper.particleSet.get(ci.entity);
        }
        if (Mapper.starSet.has(ci.entity)) {
            return Mapper.starSet.get(ci.entity);
        }
        return null;
    }

    /**
     * The mode a rule over this attribute has to use. Only extra catalogue columns can be
     * categorical; the built-in attributes are all numeric.
     *
     * @param attr The attribute.
     *
     * @return The mode.
     */
    private static FilterRule.Mode modeOf(IAttribute attr) {
        return attr != null && attr.isStringAttribute() ? FilterRule.Mode.CATEGORICAL : FilterRule.Mode.NUMERIC;
    }

    /**
     * Lists the values a categorical column can take, for the hint tooltip of the value field.
     * <p>
     * The list is capped: a column whose values are all different would otherwise fill the tooltip
     * with the whole dataset. A blank cell is shown as such, because it is a value a rule can select.
     *
     * @param set  The particle set of the dataset, or null.
     * @param attr The attribute.
     *
     * @return The tooltip text, or null if there is nothing to say about this attribute.
     */
    private static String valuesHint(ParticleSet set,
                                     IAttribute attr) {
        if (set == null || !(attr instanceof AttributeUCD ucdAttr) || !attr.isStringAttribute()) {
            return null;
        }
        var sample = set.getCategoricalValues(ucdAttr.ucd);
        if (sample.isEmpty()) {
            return null;
        }
        var sb = new StringBuilder();
        sb.append(sample.truncated ? I18n.msg("gui.dataset.filter.values.more", ParticleSet.MAX_CATEGORICAL_VALUES)
                                   : I18n.msg("gui.dataset.filter.values.count", sample.values.size));
        sb.append("\n");
        for (int i = 0; i < sample.values.size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String v = sample.values.get(i);
            sb.append(v.isEmpty() ? I18n.msg("gui.dataset.filter.values.empty") : v);
        }
        return sb.toString();
    }

    private static String[] comparatorsFor(FilterRule.Mode mode) {
        return mode == FilterRule.Mode.CATEGORICAL ? CATEGORICAL_COMPARATORS : NUMERIC_COMPARATORS;
    }

    private static IValidator validatorFor(IAttribute attr,
                                           boolean categorical,
                                           FloatValidator numberValidator,
                                           FloatValidator colorValidator) {
        if (categorical) {
            // A category is free text.
            return null;
        }
        if (attr instanceof AttributeColorRed || attr instanceof AttributeColorBlue || attr instanceof AttributeColorGreen) {
            return colorValidator;
        }
        return attr.isNumberAttribute() ? numberValidator : null;
    }

    private AttributeComboBoxBean getAttributeBean(IAttribute attr,
                                                   Array<AttributeComboBoxBean> attrs) {
        for (AttributeComboBoxBean bean : attrs) {
            // Compare the attributes themselves: matching on the label picks the wrong entry whenever
            // one column name is a substring of another.
            if (bean.attr.equals(attr) || bean.name.equals(attr.toString())) {
                return bean;
            }
        }
        return null;
    }

    @Override
    protected boolean accept() {
        // Filter
        if (filterEdited) {
            if (ci.filter != null) {
                ci.filter = filter.hasRules() ? filter : null;
                ci.highlight(ci.highlighted);
            } else {
                ci.filter = filter.hasRules() ? filter : null;
                ci.highlight(ci.highlighted);
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
