/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.gui.window.GenericDialog;
import gaiasky.gui.beans.AttributeComboBoxBean;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.CatalogInfo;
import gaiasky.util.Logger;
import gaiasky.util.TextUtils;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.FilterRule;
import gaiasky.util.filter.FilterRule.IComparator;
import gaiasky.util.filter.attrib.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import gaiasky.util.ucd.UCD;
import gaiasky.util.validator.FloatValidator;

/**
 * Window to define filters for a dataset.
 */
public class DatasetFiltersWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetFiltersWindow.class);

    private final CatalogInfo ci;
    private Table filterTable;
    private Filter filter;
    private OwnTextIconButton addFilterOrRule;
    private boolean filterEdited;

    public DatasetFiltersWindow(CatalogInfo ci,
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
            filterTable.add(new OwnLabel(I18n.msg("gui.dataset.filter.operation"), skin)).left().padRight(pad18 * 2f).padBottom(pad18);
            filterTable.add(operation).left().expandX().padBottom(pad18).row();

            // Rules
            Array<FilterRule> rules = filter.getRules();
            Table rulesTable = new Table(skin);
            filterTable.add(rulesTable).colspan(2);

            for (FilterRule rule : rules) {
                // UNIT
                var unitStr = rule.getAttribute().getUnit();
                var unitStrCapped = unitStr != null ? TextUtils.capString(unitStr, 12) : "";

                OwnLabel unit = new OwnLabel(unitStrCapped, skin);
                unit.setWidth(130f);
                if (unitStr != null && unitStr.length() > unitStrCapped.length()) {
                    unit.addListener(new OwnTextTooltip(unitStr, skin));
                }

                // ATTRIBUTE
                boolean stars = Mapper.starSet.has(ci.entity) || Mapper.octree.has(ci.entity);
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
                if (Mapper.particleSet.has(ci.entity) || Mapper.starSet.has(ci.entity)) {
                    var set = Mapper.particleSet.has(ci.entity) ? Mapper.particleSet.get(ci.entity) : Mapper.starSet.get(ci.entity);
                    if (!set.data().isEmpty()) {
                        IParticleRecord first = set.data().get(0);
                        if (first.hasExtra()) {
                            ObjectMap.Keys<UCD> ucds = first.extraKeys();
                            for (UCD ucd : ucds)
                                attrs.add(new AttributeComboBoxBean(new AttributeUCD(ucd)));
                        }
                    }
                }

                // Value text field and validator
                FloatValidator numberValidator = new FloatValidator(-Float.MAX_VALUE, Float.MAX_VALUE);
                FloatValidator colorValidator = new FloatValidator(0, 1);
                boolean useColorVal = rule.getAttribute() instanceof AttributeColorRed || rule.getAttribute() instanceof AttributeColorBlue || rule.getAttribute() instanceof AttributeColorGreen;
                boolean isNumberAttribute = rule.getAttribute().isNumberAttribute();
                OwnTextField value = new OwnTextField(rule.getValue().toString(), skin, useColorVal ? colorValidator : (isNumberAttribute ? numberValidator : null));

                OwnSelectBox<AttributeComboBoxBean> attribute = new OwnSelectBox<>(skin);
                attribute.setItems(attrs);
                attribute.setSelected(getAttributeBean(rule.getAttribute(), attrs));
                attribute.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        IAttribute newAttr = attribute.getSelected().attr;
                        rule.setAttribute(newAttr);
                        // Update validator for colors
                        boolean isNum = rule.getAttribute().isNumberAttribute();
                        if (newAttr instanceof AttributeColorRed || newAttr instanceof AttributeColorBlue || newAttr instanceof AttributeColorGreen) {
                            value.setValidator(colorValidator);
                        } else {
                            if(isNum) {
                                value.setValidator(numberValidator);
                            } else {
                                value.setValidator(null);
                            }
                        }
                        // Update unit
                        unit.setText(newAttr.getUnit());
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });
                rulesTable.add(attribute).left().padRight(pad18).padBottom(pad10);

                // COMPARATOR
                String[] comparators = new String[]{"<", "<=", ">", ">=", "==", "!="};
                OwnSelectBox<String> comparator = new OwnSelectBox<>(skin);
                comparator.setWidth(minSelectWidth);
                comparator.setItems(comparators);
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
                rulesTable.add(comparator).left().padRight(pad18).padBottom(pad10);

                // VALUE
                value.setWidth(280f);
                value.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        if (value.isValid()) {
                            try {
                                rule.setValue(Float.parseFloat(value.getText()));
                                filterEdited = true;
                                return true;
                            } catch (Exception e) {
                                rule.setValue(value.getText());
                                filterEdited = true;
                                return true;
                            }
                        }
                        return false;
                    }
                    return false;
                });
                rulesTable.add(value).left().padRight(pad18).padBottom(pad10);

                // UNIT
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

    private AttributeComboBoxBean getAttributeBean(IAttribute attr,
                                                   Array<AttributeComboBoxBean> attrs) {
        for (AttributeComboBoxBean attribute : attrs) {
            if (attr.toString().contains(attribute.name)) {
                return attribute;
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
