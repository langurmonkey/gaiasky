/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.record.ParticleStar;
import gaiasky.util.filter.Filter;
import gaiasky.util.filter.FilterRule;
import gaiasky.util.filter.attrib.AttributeUCD;
import gaiasky.util.filter.attrib.AttributeX;
import gaiasky.util.ucd.UCD;
import org.junit.Assert;
import org.junit.Test;

public class FilterRuleTest {

    private static final UCD CLASS = new UCD("meta.code", "klass", null, 0);
    private static final UCD MAG = new UCD("phot.mag", "magnitude", "mag", 0);

    private static ParticleStar star(UCD ucd,
                                     Object value) {
        var extra = new ObjectMap<UCD, Object>();
        if (ucd != null && value != null) {
            extra.put(ucd, value);
        }
        return new ParticleStar(1, new String[]{"a"}, 10, 0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0, 5000f, 4f, 0f, extra);
    }

    private static AttributeUCD klass() {
        return new AttributeUCD(CLASS);
    }

    @Test
    public void categoricalEqualityIgnoresCase() {
        var rule = new FilterRule(FilterRule.Mode.CATEGORICAL, "==", klass(), "galaxy");
        Assert.assertTrue("case must not matter", rule.evaluate(star(CLASS, "GALAXY")));
        Assert.assertFalse(rule.evaluate(star(CLASS, "STAR")));
    }

    @Test
    public void categoricalInequalityAcceptsRecordsWithoutTheColumn() {
        var rule = new FilterRule(FilterRule.Mode.CATEGORICAL, "!=", klass(), "GALAXY");
        Assert.assertFalse(rule.evaluate(star(CLASS, "GALAXY")));
        Assert.assertTrue(rule.evaluate(star(CLASS, "STAR")));
        // A record that does not carry the column is not of this category.
        Assert.assertTrue("a missing category is not the one excluded", rule.evaluate(star(null, null)));
    }

    @Test
    public void blankCellsAreACategoryOfTheirOwn() {
        var blank = new FilterRule(FilterRule.Mode.CATEGORICAL, "==", klass(), "");
        // Absent, null, empty and whitespace-only cells are all blank.
        Assert.assertTrue(blank.evaluate(star(null, null)));
        Assert.assertTrue("a null value is blank", blank.evaluate(star(CLASS, "")));
        Assert.assertTrue("a padded cell is blank", blank.evaluate(star(CLASS, "   ")));
        Assert.assertFalse(blank.evaluate(star(CLASS, "GALAXY")));

        // And blank is not a real category, so it survives an exclusion of a real one.
        var notGalaxy = new FilterRule(FilterRule.Mode.CATEGORICAL, "!=", klass(), "GALAXY");
        Assert.assertTrue(notGalaxy.evaluate(star(null, null)));
        Assert.assertTrue(notGalaxy.evaluate(star(CLASS, "")));
        Assert.assertFalse(notGalaxy.evaluate(star(CLASS, " GALAXY ")));
    }

    @Test
    public void categoricalModeDropsOrderingOperators() {
        var rule = new FilterRule(">", klass(), "B");
        rule.setMode(FilterRule.Mode.CATEGORICAL);
        Assert.assertEquals("==", rule.getComparator().toString());
        Assert.assertEquals("==", rule.getComparatorFromString(">").toString());
        Assert.assertEquals("!=", rule.getComparatorFromString("!=").toString());
    }

    @Test
    public void numericModeKeepsOrderingOperators() {
        var rule = new FilterRule(">", klass(), "B");
        rule.setMode(FilterRule.Mode.CATEGORICAL);
        rule.setMode(FilterRule.Mode.NUMERIC);
        Assert.assertEquals(">", rule.getComparatorFromString(">").toString());
    }

    @Test
    public void categoricalComparesNumbersAsTheirText() {
        // A column can declare numbers and still be compared as a category; the mode decides, not the
        // record. 5 and "5" name the same category, 5.0 does not.
        var rule = new FilterRule(FilterRule.Mode.CATEGORICAL, "==", klass(), "5");
        Assert.assertTrue(rule.evaluate(star(CLASS, 5)));
        Assert.assertFalse(rule.evaluate(star(CLASS, 5.5)));
    }

    @Test
    public void numericModeStillParsesNumbersHeldAsText() {
        // The behaviour of every rule before categorical mode existed has to survive.
        var rule = new FilterRule(">", new AttributeUCD(MAG), 5.0);
        Assert.assertEquals(FilterRule.Mode.NUMERIC, rule.getMode());
        Assert.assertTrue(rule.evaluate(star(MAG, "10")));
        Assert.assertFalse(rule.evaluate(star(MAG, "3")));
    }

    @Test
    public void stringAttributeIsResolvedPerColumn() {
        Assert.assertTrue("a text column is categorical", new AttributeUCD(CLASS, true).isStringAttribute());
        Assert.assertFalse("a numeric column is not", new AttributeUCD(MAG, false).isStringAttribute());
        Assert.assertFalse("untyped columns default to numeric", new AttributeUCD(CLASS).isStringAttribute());
        // The built-in attributes are numeric, and never categorical.
        Assert.assertFalse(new AttributeX().isStringAttribute());
    }

    @Test
    public void modeSurvivesCopyAndDeepCopy() {
        var rule = new FilterRule(FilterRule.Mode.CATEGORICAL, "!=", klass(), "GALAXY");
        var copy = rule.copy();
        Assert.assertEquals(FilterRule.Mode.CATEGORICAL, copy.getMode());
        Assert.assertEquals("!=", copy.getComparator().toString());
        Assert.assertTrue(copy.evaluate(star(CLASS, "STAR")));

        var filter = new Filter(rule);
        var deepCopy = filter.deepCopy();
        Assert.assertEquals(FilterRule.Mode.CATEGORICAL, deepCopy.getRules().first().getMode());
        Assert.assertTrue(deepCopy.evaluate(star(CLASS, "STAR")));
        Assert.assertFalse(deepCopy.evaluate(star(CLASS, "GALAXY")));
    }

    @Test
    public void filterLinksCategoricalAndNumericRules() {
        var byClass = new FilterRule(FilterRule.Mode.CATEGORICAL, "==", klass(), "GALAXY");
        var byX = new FilterRule(">", new AttributeX(), 5.0);

        var and = new Filter("and", byClass.copy(), byX.copy());
        Assert.assertTrue(and.evaluate(star(CLASS, "GALAXY")));
        Assert.assertFalse(and.evaluate(star(CLASS, "STAR")));

        var or = new Filter("or", byClass.copy(), byX.copy());
        Assert.assertTrue(or.evaluate(star(CLASS, "STAR")));
    }
}
