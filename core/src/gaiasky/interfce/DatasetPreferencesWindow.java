/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import gaiasky.interfce.beans.AttributeComboBoxBean;
import gaiasky.scenegraph.StarGroup;
import gaiasky.scenegraph.octreewrapper.OctreeWrapper;
import gaiasky.util.*;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.FilterRule;
import gaiasky.util.filter.FilterRule.IComparator;
import gaiasky.util.filter.attrib.*;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.*;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;

import java.time.ZoneId;

public class DatasetPreferencesWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetPreferencesWindow.class);

    private final CatalogInfo ci;
    private OwnTextField pointSize;
    private OwnCheckBox allVisible;
    private Table filterTable;
    private final DatasetPreferencesWindow dpw;
    private Filter filter;
    private boolean filterEdited;

    public DatasetPreferencesWindow(CatalogInfo ci, Skin skin, Stage stage) {
        super(I18n.txt("gui.preferences") + " - " + ci.name, skin, stage);
        this.ci = ci;
        this.dpw = this;
        this.filterEdited = false;

        setAcceptText(I18n.txt("gui.ok"));
        setCancelText(I18n.txt("gui.cancel"));
        setModal(false);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        // Name
        content.add(new OwnLabel(I18n.txt("gui.dataset.name"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad);
        content.add(new OwnLabel(ci.name, skin)).left().padRight(pad).padBottom(pad).row();
        // Type
        content.add(new OwnLabel(I18n.txt("gui.dataset.type"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad);
        content.add(new OwnLabel(ci.type.toString(), skin)).left().padRight(pad).padBottom(pad).row();
        // Added
        content.add(new OwnLabel(I18n.txt("gui.dataset.loaded"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad);
        content.add(new OwnLabel(ci.loadDateUTC.atZone(ZoneId.systemDefault()).toString(), skin)).left().padRight(pad).padBottom(pad).row();
        // Desc
        content.add(new OwnLabel(I18n.txt("gui.dataset.description"), skin, "hud-subheader")).right().padRight(pad).padBottom(pad * 2f);
        content.add(new OwnLabel(TextUtils.capString(ci.description, 55), skin)).left().padRight(pad).padBottom(pad * 2f).row();

        // Highlight
        content.add(new OwnLabel(I18n.txt("gui.dataset.highlight"), skin, "hud-header")).left().colspan(2).padBottom(pad).row();

        // Point size
        IValidator pointSizeValidator = new FloatValidator(0.5f, 5.0f);
        pointSize = new OwnTextField(Float.toString(ci.hlSizeFactor), skin, pointSizeValidator);
        content.add(new OwnLabel(I18n.txt("gui.dataset.highlight.size"), skin)).left().padRight(pad).padBottom(pad);
        content.add(pointSize).left().padRight(pad).padBottom(pad).row();

        // All visible
        allVisible = new OwnCheckBox(I18n.txt("gui.dataset.highlight.allvisible"), skin, pad);
        allVisible.setChecked(ci.hlAllVisible);
        content.add(allVisible).left().colspan(2).padBottom(pad * 2f).row();

        // Filters
        content.add(new OwnLabel(I18n.txt("gui.dataset.filter"), skin, "hud-header")).left().colspan(2).padBottom(pad5).row();
        filterTable = new Table(skin);
        content.add(filterTable).left().colspan(2).row();

        filter = ci.filter != null ? ci.filter.deepCopy() : null;
        generateFilterTable(filter);

    }

    private void generateFilterTable(Filter filter) {
        float minSelectWidth = 100f * GlobalConf.UI_SCALE_FACTOR;
        filterTable.clearChildren();
        if (filter != null && filter.hasRules()) {
            // Operation
            OwnSelectBox<String> operation = new OwnSelectBox<>(skin);
            operation.setWidth(minSelectWidth);
            operation.setItems("and", "or");
            operation.setSelected(filter.getOperationString().toLowerCase());
            operation.addListener(event -> {
               if(event instanceof ChangeEvent){
                   String newOp = operation.getSelected();
                   filter.setOperation(newOp);
                   filterEdited = true;
                   return true;
               }
               return false;
            });
            filterTable.add(new OwnLabel(I18n.txt("gui.dataset.filter.operation"), skin)).left().padRight(pad * 2f).padBottom(pad);
            filterTable.add(operation).left().expandX().padBottom(pad).row();

            // Rules
            Array<FilterRule> rules = filter.getRules();
            Table rulesTable = new Table(skin);
            filterTable.add(rulesTable).colspan(2);

            for(FilterRule rule : rules) {
                // UNIT
                OwnLabel unit = new OwnLabel(rule.getAttribute().getUnit(), skin);

                // ATTRIBUTE
                boolean stars = (ci.object instanceof StarGroup || ci.object instanceof OctreeWrapper);
                Array<AttributeComboBoxBean> attrs = new Array<>(stars ? 12 : 7);
                // Add particle attributes (dist, alpha, delta)
                attrs.add(new AttributeComboBoxBean(new AttributeDistance()));
                attrs.add(new AttributeComboBoxBean(new AttributeRA()));
                attrs.add(new AttributeComboBoxBean(new AttributeDEC()));
                attrs.add(new AttributeComboBoxBean(new AttributeEclLatitude()));
                attrs.add(new AttributeComboBoxBean(new AttributeEclLongitude()));
                attrs.add(new AttributeComboBoxBean(new AttributeGalLatitude()));
                attrs.add(new AttributeComboBoxBean(new AttributeGalLongitude()));
                if (stars) {
                    // Star attributes (appmag, absmag, mualpha, mudelta, radvel)
                    attrs.add(new AttributeComboBoxBean(new AttributeAppmag()));
                    attrs.add(new AttributeComboBoxBean(new AttributeAbsmag()));
                    attrs.add(new AttributeComboBoxBean(new AttributeMualpha()));
                    attrs.add(new AttributeComboBoxBean(new AttributeMudelta()));
                    attrs.add(new AttributeComboBoxBean(new AttributeRadvel()));
                }
                OwnSelectBox<AttributeComboBoxBean> attribute = new OwnSelectBox<>(skin);
                attribute.setItems(attrs);
                attribute.setSelected(getAttributeBean(rule.getAttribute(), attrs));
                attribute.addListener(event -> {
                    if(event instanceof ChangeEvent){
                        IAttribute newAttr = attribute.getSelected().attr;
                        rule.setAttribute(newAttr);
                        // Update unit
                        unit.setText(newAttr.getUnit());
                        filterEdited = true;
                        return true;
                    }
                    return false;
                });
                rulesTable.add(attribute).left().padRight(pad).padBottom(pad5);

                // COMPARATOR
                String[] cmps = new String[]{"<", "<=", ">", ">=", "==", "!="};
                OwnSelectBox<String> comparator = new OwnSelectBox<>(skin);
                comparator.setWidth(minSelectWidth);
                comparator.setItems(cmps);
                comparator.setSelected(rule.getComparator().toString());
                comparator.addListener(event ->{
                    if(event instanceof ChangeEvent){
                        IComparator newComp = rule.getComparatorFromString(comparator.getSelected());
                        rule.setComparator(newComp);
                        filterEdited = true;
                        return true;
                    }
                   return false;
                });
                rulesTable.add(comparator).left().padRight(pad).padBottom(pad5);

                // VALUE
                FloatValidator fval = new FloatValidator(-Float.MAX_VALUE, Float.MAX_VALUE);
                OwnTextField value = new OwnTextField(Double.toString(rule.getValue()), skin, fval);
                value.addListener(event -> {
                    if(event instanceof ChangeEvent){
                        if(value.isValid()){
                            try {
                                rule.setValue(Float.parseFloat(value.getText()));
                                filterEdited = true;
                                return true;
                            }catch(Exception e){
                                logger.error(e);
                                return false;
                            }
                        }
                        return false;
                    }
                   return false;
                });
                rulesTable.add(value).left().padRight(pad).padBottom(pad5);


                // UNIT
                rulesTable.add(unit).left().padRight(pad * 3f).padBottom(pad5);

                // RUBBISH
                OwnTextIconButton rubbish = new OwnTextIconButton("", skin, "rubbish");
                rubbish.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.filter.rule.remove"), skin));
                rubbish.addListener(event ->{
                    if(event instanceof ChangeEvent){
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
            addRule.pad(pad);
            rulesTable.add(addRule).left().padTop(pad).row();
            addRule.addListener(event -> {
                if(event instanceof ChangeEvent){
                    dpw.addRule(filter);
                    filterEdited = true;
                    return true;
                }
                return false;
            });
        } else {
            // Add
            filterTable.add(new OwnLabel("No filters yet", skin)).left().padBottom(pad).row();
            OwnTextIconButton addFilter = new OwnTextIconButton(I18n.txt("gui.dataset.filter.add"), skin, "add");
            addFilter.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.filter.add"), skin));
            addFilter.pad(pad);
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

    private void deleteRule(Filter filter, FilterRule rule){
        if(filter != null && rule != null){
            boolean removed = filter.removeRule(rule);
            if(removed){
                // Reload table
                Gdx.app.postRunnable(() -> generateFilterTable(filter));
            }
        }
    }

    private void addRule(Filter filter){
        if(filter != null){
            FilterRule rule = new FilterRule("<", new AttributeDistance(), 500);
            filter.addRule(rule);
            // Reload table
            Gdx.app.postRunnable(() -> generateFilterTable(filter));
        }
    }

    private void addFilter() {
        FilterRule rule = new FilterRule("<", new AttributeDistance(), 500);
        filter = new Filter(rule);
        // Reload table
        Gdx.app.postRunnable(() -> generateFilterTable(filter));
    }

    private AttributeComboBoxBean getAttributeBean(IAttribute attr, Array<AttributeComboBoxBean> attrs){
        for(AttributeComboBoxBean attribute : attrs){
            if(attr.toString().contains(attribute.name)){
                return attribute;
            }
        }
        return null;
    }

    @Override
    protected void accept() {
        // Point size
        if (pointSize.isValid()) {
            float newVal = Parser.parseFloat(pointSize.getText());
            if (newVal != ci.hlSizeFactor) {
                ci.setHlSizeFactor(newVal);
            }
        }
        // All visible
        boolean vis = allVisible.isChecked();
        if (vis != ci.hlAllVisible) {
            ci.setHlAllVisible(vis);
        }
        // Filter
        if(filterEdited) {
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
}
