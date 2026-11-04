/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.component.ParticleSet;
import gaiasky.scene.record.ParticleStar;
import gaiasky.util.filter.attrib.AttributeDistance;
import gaiasky.util.filter.attrib.AttributeUCD;
import gaiasky.util.ucd.UCD;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.starlink.table.ColumnInfo;

import java.util.ArrayList;
import java.util.List;

public class ParticleSetColumnTypeTest {

    private static final UCD CLASS = new UCD("meta.code", "klass", null, 0);
    private static final UCD MAG = new UCD("phot.mag", "mag", "mag", 1);

    private static ParticleStar star(Object classValue,
                                     Object magValue) {
        var extra = new ObjectMap<UCD, Object>();
        if (classValue != null) {
            extra.put(CLASS, classValue);
        }
        if (magValue != null) {
            extra.put(MAG, magValue);
        }
        return new ParticleStar(1, new String[]{"a"}, 10, 0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0, 5000f, 4f, 0f, extra);
    }

    private static ParticleSet set(List<ColumnInfo> columnInfo,
                                   ParticleStar... records) {
        var set = new ParticleSet();
        set.setData(new ArrayList<>(List.of(records)), false);
        set.setColumnInfoList(columnInfo);
        return set;
    }

    @Test
    public void metadataDecidesTheType() {
        // The table declares the column, and that is the answer even when the records disagree.
        var columns = new ArrayList<ColumnInfo>();
        columns.add(new ColumnInfo("klass", String.class, "spectral class"));
        columns.add(new ColumnInfo("mag", Float.class, "magnitude"));

        var set = set(columns, star("GALAXY", 5f));
        Assert.assertTrue(set.isCategoricalColumn(CLASS));
        Assert.assertFalse(set.isCategoricalColumn(MAG));
    }

    @Test
    public void recordsDecideWhenThereIsNoMetadata() {
        var set = set(null, star("GALAXY", 5f));
        Assert.assertTrue(set.isCategoricalColumn(CLASS));
        Assert.assertFalse(set.isCategoricalColumn(MAG));
    }

    @Test
    public void blankCellsDoNotHideTheType() {
        // A column whose cells are all empty is still a text column: an empty value is text.
        var set = set(null, star("", 5f));
        Assert.assertTrue(set.isCategoricalColumn(CLASS));

        // A null cell does not decide anything, so the scan goes on.
        var setWithNulls = set(null, star(null, 5f), star("GALAXY", 6f));
        Assert.assertTrue("the first non-null value decides", setWithNulls.isCategoricalColumn(CLASS));
        Assert.assertFalse(setWithNulls.isCategoricalColumn(MAG));
    }

    @Test
    public void theColumnIndexDecides() {
        // Column 0 is text. A UCD whose name matches nothing must still be classified, by its index.
        var columns = new ArrayList<ColumnInfo>();
        columns.add(new ColumnInfo("orbit_type", String.class, "orbit class"));
        columns.add(new ColumnInfo("a", Float.class, "semi-major axis"));

        var set = set(columns, star("main belt", 2.5f));
        Assert.assertTrue(set.isCategoricalColumn(new UCD("meta.code", "renamed", null, 0)));
        Assert.assertFalse(set.isCategoricalColumn(new UCD(null, "a", "au", 1)));
    }

    @Test
    public void sampledValuesAreSortedAndBounded() {
        var set = set(null, star("trojan", 1f), star("", 1f), star("main belt", 1f), star("  main belt  ", 1f));

        var sample = set.getCategoricalValues(CLASS);
        Assert.assertFalse(sample.truncated);
        // Blank cells are a value of their own, and padding is trimmed.
        Assert.assertEquals(3, sample.values.size);
        Assert.assertEquals("", sample.values.get(0));
        Assert.assertEquals("main belt", sample.values.get(1));
        Assert.assertEquals("trojan", sample.values.get(2));
    }

    @Test
    public void aHighCardinalityColumnGivesUpEarly() {
        // Every record carries a different value: the scan must not walk the whole dataset.
        var records = new ArrayList<IParticleRecord>();
        for (int i = 0; i < 10_000; i++) {
            records.add(star("value-" + i, 1f));
        }
        var set = new ParticleSet();
        set.setData(records, false);

        var sample = set.sampleCategoricalValues(CLASS, 4);
        Assert.assertTrue(sample.truncated);
        Assert.assertEquals(4, sample.values.size);
        // Sorted, and consistent with the early exit.
        Assert.assertEquals("value-0", sample.values.get(0));
    }

    @Test
    public void sampledValuesAreCachedUntilTheDataChanges() {
        var set = set(null, star("trojan", 1f));
        Assert.assertSame(set.getCategoricalValues(CLASS), set.getCategoricalValues(CLASS));

        // A reload invalidates the sample.
        var before = set.getCategoricalValues(CLASS);
        set.setData(new ArrayList<>(List.of(star("hilda", 1f))), false);
        var after = set.getCategoricalValues(CLASS);
        Assert.assertNotSame(before, after);
        Assert.assertEquals("hilda", after.values.get(0));
    }

    @Test
    public void theRangeSkipsMissingCells() {
        var set = set(null, star("trojan", 2f), star("hilda", null), star("main belt", 7.5f));

        var range = set.getNumericRange(new AttributeUCD(MAG, false));
        Assert.assertFalse(range.isEmpty());
        Assert.assertEquals(2d, range.min, 1e-9);
        Assert.assertEquals(7.5d, range.max, 1e-9);
        Assert.assertEquals(2, range.count);
        // A missing cell is not a zero, or the minimum would be a value no record has.
        Assert.assertEquals(1, range.missing);
        Assert.assertFalse(range.isSingleValue());
    }

    @Test
    public void aSingleValueRangeSaysSo() {
        var set = set(null, star("trojan", 3f), star("hilda", 3f));
        var range = set.getNumericRange(new AttributeUCD(MAG, false));
        Assert.assertTrue(range.isSingleValue());
        Assert.assertEquals(3d, range.min, 1e-9);
    }

    @Test
    public void aTextColumnHasNoRange() {
        var set = set(null, star("trojan", 1f), star("hilda", 1f));
        // The class column holds text: the range must be empty rather than a range of hash codes.
        Assert.assertTrue(set.getNumericRange(new AttributeUCD(CLASS, true)).isEmpty());
        Assert.assertTrue(set.sampleNumericRange(new AttributeUCD(new UCD(null, "nope", null, 5), false)).isEmpty());
    }

    @Test
    public void builtInAttributesHaveARangeToo() {
        // All the test records hold the same coordinates, so the distance is a single value.
        var set = set(null, star("trojan", 1f), star("hilda", 1f));
        var range = set.getNumericRange(new AttributeDistance());
        Assert.assertEquals(2, range.count);
        Assert.assertTrue(range.isSingleValue());
        Assert.assertEquals(range.min, range.max, 0d);
    }

    @Test
    public void theRangeIsCachedUntilTheDataChanges() {
        var attr = new AttributeUCD(MAG, false);
        var set = set(null, star("trojan", 1f), star("hilda", 4f));
        Assert.assertSame(set.getNumericRange(attr), set.getNumericRange(attr));

        var before = set.getNumericRange(attr);
        set.setData(new ArrayList<>(List.of(star("hilda", 9f))), false);
        var after = set.getNumericRange(attr);
        Assert.assertNotSame(before, after);
        Assert.assertEquals(9d, after.max, 1e-9);
    }

    @Test
    public void unknownColumnsAreNumeric() {
        var set = set(null, star("GALAXY", 5f));
        Assert.assertFalse(set.isCategoricalColumn(new UCD(null, "missing", null, 9)));
        Assert.assertFalse(set.isCategoricalColumn(null));
        Assert.assertFalse("an empty set has no types", new ParticleSet().isCategoricalColumn(CLASS));
    }
}
