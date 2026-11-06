/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyAttributes.ChannelType;
import gaiasky.scene.record.GalaxyAttributes.Distribution;
import gaiasky.scene.record.GalaxyGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Substructure inside a dust lane: the dust must not share the gas's clump realisation, and the
 * lanes must be textured along and across themselves rather than being drawn as smooth arcs.
 * <p>
 * The two changes are checked separately because they act on different things. The clump seed moves
 * where the dust is clumped, and it must leave the gas bit-identical. The filament field multiplies
 * the density of the spiral dust and nothing else, so the gas of the same galaxy keeps its own
 * structure, and a disk - which has no lanes to break up - keeps none at all.
 */
public class DustSubstructureTest {

    private static final long SEED = 87224L;

    private static GalaxyAttributes field(ChannelType type,
                                          Distribution distribution) {
        var f = new GalaxyAttributes();
        f.type = type;
        f.distribution = distribution;
        f.baseRadius = 1f;
        f.minRadius = 0.1f;
        f.numArms = 2;
        f.armSigma = 0.38f;
        f.baseAngle = 651.35f;
        f.heightScale = 0.02f;
        f.intensity = 1f;
        return f;
    }

    private static GalaxyShape shape(GalaxyAttributes f) {
        return GalaxyShape.from(VolumeLayout.packFields(List.of(f), 1), 0);
    }

    /** Angular position of the arm centreline at a radius, without warp. **/
    private static float theta(GalaxyShape s,
                               float r) {
        return GalaxyDensityField.pitchB(s.baseAngle) * (float) Math.log(r / (0.1f * s.baseRadius));
    }

    /** Mean and standard deviation of a series. **/
    private static float[] stats(float[] v) {
        float mean = 0f;
        for (float x : v) {
            mean += x;
        }
        mean /= v.length;
        float var = 0f;
        for (float x : v) {
            var += (x - mean) * (x - mean);
        }
        return new float[]{mean, (float) Math.sqrt(var / v.length)};
    }

    /** Densities sampled along the lane at a radius, over one radian of arc. **/
    private static float[] alongLane(GalaxyShape s,
                                     float r) {
        var sample = new GalaxyDensityField.Sample();
        int n = 600;
        float[] v = new float[n];
        float t0 = theta(s, r);
        for (int i = 0; i < n; i++) {
            float phi = t0 - 0.5f + i / (float) (n - 1);
            GalaxyDensityField.sample(s, r * (float) Math.cos(phi), 0f, r * (float) Math.sin(phi), 0.005f, sample);
            v[i] = sample.density;
        }
        return v;
    }

    /** Densities sampled across the lane at a radius, over the lane and its surroundings. **/
    private static float[] acrossLane(GalaxyShape s,
                                      float r) {
        var sample = new GalaxyDensityField.Sample();
        int n = 600;
        float[] v = new float[n];
        float t0 = theta(s, r);
        for (int i = 0; i < n; i++) {
            float d = -0.15f + 0.3f * i / (n - 1);
            float px = r * (float) Math.cos(t0) - d * (float) Math.sin(t0);
            float pz = r * (float) Math.sin(t0) + d * (float) Math.cos(t0);
            GalaxyDensityField.sample(s, px, 0f, pz, 0.005f, sample);
            v[i] = sample.density;
        }
        return v;
    }

    @Test
    public void theGasFieldIsUnchanged() {
        // The clump seed is a parameter with a default of zero, and the gas passes zero: the gas
        // field is bit-identical to what it was before the dust got its own realisation. These are
        // the densities the previous field produced at six points of a generated Sb galaxy.
        var spec = new GalaxyGenerator().generateGalaxySpec(GalaxyGenerator.GalaxyMorphology.Sb, SEED);
        GalaxyAttributes gas = null;
        for (var f : spec.volumeFields) {
            if (f.type == ChannelType.GAS) {
                gas = f;
            }
        }
        Assert.assertNotNull(gas);
        var s = shape(gas);
        var sample = new GalaxyDensityField.Sample();
        float[][] points = {{0.12f, 0.01f, 0.20f}, {0.35f, 0f, -0.18f}, {-0.42f, 0.02f, 0.31f},
                {0.05f, -0.01f, 0.55f}, {-0.21f, 0f, -0.44f}, {0.6f, 0.03f, 0.05f}};
        float[] expected = {0.013018824f, 0.08236768f, 0f, 7.723014e-4f, 0.03350771f, 0f};
        for (int i = 0; i < points.length; i++) {
            GalaxyDensityField.sample(s, points[i][0], points[i][1], points[i][2], 0.007f, sample);
            Assert.assertEquals("gas at point " + i, expected[i], sample.density, 0f);
        }
    }

    @Test
    public void theDustDrawsAnIndependentClumpRealisation() {
        // A dust field sharing the gas realisation sits where the gas is brightest, and the marcher
        // emits the gas and absorbs it at the same sample, so the two cancel. Correlated here means
        // shared; the two draws have to be independent but identical in distribution.
        int n = 0;
        float[] a = new float[8000], b = new float[8000];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                for (int k = 0; k < 20; k++) {
                    float x = -0.8f + 1.6f * i / 19f, y = -0.1f + 0.2f * j / 19f, z = -0.8f + 1.6f * k / 19f;
                    a[n] = GalaxyDensityField.clumps(x, y, z, 1f, 0f);
                    b[n] = GalaxyDensityField.clumps(x, y, z, 1f, GalaxyDensityField.DUST_CLUMP_SEED);
                    n++;
                }
            }
        }
        float[] sa = stats(a), sb = stats(b);
        float cov = 0f;
        for (int i = 0; i < n; i++) {
            cov += (a[i] - sa[0]) * (b[i] - sb[0]);
        }
        float corr = cov / (n * sa[1] * sb[1]);
        Assert.assertEquals("the two draws have the same statistics", sa[0], sb[0], 0.05f);
        Assert.assertEquals("and the same spread", sa[1], sb[1], 0.05f);
        Assert.assertTrue("the dust must not clump with the gas, correlation " + corr, Math.abs(corr) < 0.1f);
    }

    @Test
    public void theFilamentsAreLongerAlongTheLaneThanAcrossIt() {
        // Count how often the multiplier crosses its own mean walking along the arm centreline and
        // walking across it, and compare crossings per unit of path length. A filament is a strand
        // along the flow, so the same length of path has to cross more of them sideways than
        // forwards. The walk along the arm has to follow the spiral - varying the azimuth at a
        // fixed radius just sweeps across the lanes over and over.
        var s = shape(field(ChannelType.DUST, Distribution.SPIRAL));
        float cell = 0.005f;
        int n = 2000;
        float alongLen = 0f, acrossLen = 0f;
        int alongCross = 0, acrossCross = 0;
        for (float arm = 0f; arm < s.numArms; arm += 1f) {
            float[] along = new float[n];
            float r0 = 0.15f, r1 = 0.95f;
            float prevX = 0f, prevZ = 0f;
            for (int i = 0; i < n; i++) {
                float r = r0 + (r1 - r0) * i / (n - 1);
                float phi = theta(s, r) + arm * 2f * (float) Math.PI / s.numArms;
                float px = r * (float) Math.cos(phi), pz = r * (float) Math.sin(phi);
                along[i] = GalaxyDensityField.dustFilaments(s, px, 0f, pz, r, 0f, cell);
                if (i > 0) {
                    float dx = px - prevX, dz = pz - prevZ;
                    alongLen += (float) Math.sqrt(dx * dx + dz * dz);
                }
                prevX = px;
                prevZ = pz;
            }
            alongCross += crossings(along);
        }
        for (float r = 0.25f; r <= 0.85f; r += 0.1f) {
            float[] across = new float[n];
            float t0 = theta(s, r);
            for (int i = 0; i < n; i++) {
                float d = -0.06f + 0.12f * i / (n - 1);
                float px = r * (float) Math.cos(t0) - d * (float) Math.sin(t0);
                float pz = r * (float) Math.sin(t0) + d * (float) Math.cos(t0);
                across[i] = GalaxyDensityField.dustFilaments(s, px, 0f, pz, (float) Math.sqrt(px * px + pz * pz), 0f, cell);
            }
            acrossCross += crossings(across);
            acrossLen += 0.12f;
        }
        float alongDensity = alongCross / alongLen;
        float acrossDensity = acrossCross / acrossLen;
        Assert.assertTrue("strands have to be longer along the lane than across it: " + alongDensity
                                  + " crossings per unit length along, " + acrossDensity + " across",
                          acrossDensity > 1.5f * alongDensity);
    }

    private static int crossings(float[] v) {
        float[] st = stats(v);
        int c = 0;
        for (int i = 1; i < v.length; i++) {
            if ((v[i - 1] - st[0]) * (v[i] - st[0]) < 0f) {
                c++;
            }
        }
        return c;
    }

    @Test
    public void theVeilKeepsTheLaneContinuous() {
        // Thresholded rather than scaled, so the gaps are empty enough to read as gaps; but not to
        // zero, so the lane stays a lane instead of a row of dashes.
        var s = shape(field(ChannelType.DUST, Distribution.SPIRAL));
        int n = 120;
        float peak = 0f, low = 2f;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float px = -1f + 2f * i / (n - 1), pz = -1f + 2f * j / (n - 1);
                float r = (float) Math.sqrt(px * px + pz * pz);
                float v = GalaxyDensityField.dustFilaments(s, px, 0f, pz, r, 0f, 0.005f);
                peak = Math.max(peak, v);
                low = Math.min(low, v);
            }
        }
        Assert.assertEquals(1f, peak, 1e-6f);
        Assert.assertEquals(GalaxyDensityField.DUST_FILAMENT_VEIL, low, 1e-6f);
        Assert.assertTrue("the veil has to be a gap, not a dimming", GalaxyDensityField.DUST_FILAMENT_VEIL < 0.2f);
    }

    @Test
    public void theSubstructureOnlyReachesTheSpiralDust() {
        // Along a lane: the gas is smooth, the spiral dust is broken up. A disk has no arms, so it
        // gets neither the arm modulation nor the strands, in either channel.
        float r = 0.45f;
        float[] gasAlong = alongLane(shape(field(ChannelType.GAS, Distribution.SPIRAL)), r);
        float[] dustAlong = alongLane(shape(field(ChannelType.DUST, Distribution.SPIRAL)), r);
        float gasCv = stats(gasAlong)[1] / stats(gasAlong)[0];
        float dustCv = stats(dustAlong)[1] / stats(dustAlong)[0];
        Assert.assertTrue("the spiral dust has to vary along its lanes", dustCv > 0.5f);
        Assert.assertTrue("and far more than the gas does, " + dustCv + " against " + gasCv, dustCv > 2f * gasCv);

        // A disk is azimuthally uniform: the ring of samples is constant up to the clumps.
        float[] disk = alongLane(shape(field(ChannelType.DUST, Distribution.DISK)), r);
        Assert.assertEquals("a disk has no azimuthal structure", 0f, stats(disk)[1], 1e-6f);
    }

    @Test
    public void theDustKeepsALaneAcrossTheArm() {
        // The strands must not eat the lane: the densest dust is still on the centreline, twice the
        // width of the Gaussian apart.
        var s = shape(field(ChannelType.DUST, Distribution.SPIRAL));
        float r = 0.5f;
        float[] v = acrossLane(s, r);
        int mid = v.length / 2;
        float ridge = Math.max(v[mid - 2], Math.max(v[mid - 1], Math.max(v[mid], v[mid + 1])));
        Assert.assertTrue("there has to be a lane at r = " + r, ridge > 0f);
        float quarter = 0.075f;  // Two arm sigmas out, where the Gaussian is down by e^-2.
        int offset = (int) (quarter / 0.3f * (v.length - 1));
        int left = mid - offset, right = mid + offset;
        Assert.assertTrue("the lane has to be a ridge: flank " + v[left] + ", ridge " + ridge,
                          v[left] < 0.5f * ridge);
        Assert.assertTrue("on both sides: flank " + v[right], v[right] < 0.5f * ridge);
    }

    /** Mean and trough of the filament multiplier over a slice of the mid-plane. **/
    private static float[] multiplier(GalaxyShape s) {
        int n = 160;
        double mean = 0;
        float low = 2f, high = -1f;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float px = -1f + 2f * i / (n - 1), pz = -1f + 2f * j / (n - 1);
                float r = (float) Math.sqrt(px * px + pz * pz);
                float v = GalaxyDensityField.dustFilaments(s, px, 0f, pz, r, 0f, 0.005f);
                mean += v;
                low = Math.min(low, v);
                high = Math.max(high, v);
            }
        }
        return new float[]{(float) (mean / (n * (double) n)), low, high};
    }

    /** A spiral dust field with the substructure knobs set, packed as the shader would read it. **/
    private static GalaxyShape configured(float contrast,
                                          float scale,
                                          float threshold) {
        var f = field(ChannelType.DUST, Distribution.SPIRAL);
        f.filamentContrast = contrast;
        f.filamentScale = scale;
        f.filamentThreshold = threshold;
        return shape(f);
    }

    @Test
    public void theKnobsSurviveThePacking() {
        // The three sliders travel to the shader in the packed uniform, so they have to come back
        // out of it: a member of GalaxyShape that no slot fills is a slider that does nothing.
        var s = configured(0.5f, 12f, 0.3f);
        Assert.assertEquals(0.5f, s.filamentContrast, 0f);
        Assert.assertEquals(12f, s.filamentScale, 0f);
        Assert.assertEquals(0.3f, s.filamentThreshold, 0f);
        Assert.assertEquals(GalaxyAttributes.FIELD_STRIDE, 37);
    }

    @Test
    public void contrastIsHowDeepTheGapsGo() {
        // 0 is off, and it has to be off exactly: the multiplier is 1 everywhere, so a field that
        // does not want strands is the field it was before they existed.
        float[] off = multiplier(configured(0f, 20f, 0.5f));
        Assert.assertEquals("a smooth lane", 1f, off[0], 0f);
        Assert.assertEquals(1f, off[1], 0f);

        // Deeper gaps with more contrast, down to the same veil the default produces.
        float[] shallow = multiplier(configured(0.3f, 20f, 0.5f));
        float[] deep = multiplier(configured(0.9f, 20f, 0.5f));
        Assert.assertTrue("0.3 leaves more between the strands than 0.9: " + shallow[1] + " against " + deep[1],
                          shallow[1] > deep[1]);
        Assert.assertEquals(0.1f, deep[1], 0.02f);
        // The strands themselves are untouched: only what is between them changes.
        Assert.assertEquals(1f, shallow[2], 1e-6f);
        Assert.assertEquals(1f, deep[2], 1e-6f);
    }

    @Test
    public void theThresholdKeepsLessOfTheLane() {
        // The field's mean value sits around 0.5, so raising the level above it has to empty more
        // of the lane. This is what the sculptor reaches for when the strands are too fat.
        float[] low = multiplier(configured(0.88f, 20f, 0.25f));
        float[] mid = multiplier(configured(0.88f, 20f, 0.55f));
        float[] high = multiplier(configured(0.88f, 20f, 0.8f));
        Assert.assertTrue("0.25 keeps more than 0.55", low[0] > mid[0]);
        Assert.assertTrue("0.55 keeps more than 0.8", mid[0] > high[0]);
    }

    @Test
    public void theStrandFrequencyIsItsOwn() {
        // It must not follow laneNoiseScale, which is the warp's: turning the warp up would
        // otherwise bead the strands as well.
        var f = field(ChannelType.DUST, Distribution.SPIRAL);
        f.filamentContrast = 0.88f;
        f.filamentScale = 12f;
        f.laneNoiseScale = 6f;
        var s = shape(f);
        // Compare against a field with the same strand frequency and no lane scale: identical.
        var g = field(ChannelType.DUST, Distribution.SPIRAL);
        g.filamentContrast = 0.88f;
        g.filamentScale = 12f;
        g.laneNoiseScale = 1f;
        Assert.assertEquals(multiplier(shape(g))[0], multiplier(s)[0], 0f);

        // A higher strand frequency gives more crossings over the same path.
        var fine = field(ChannelType.DUST, Distribution.SPIRAL);
        fine.filamentContrast = 0.88f;
        fine.filamentScale = 28f;
        Assert.assertTrue("a higher frequency means more strands",
                          multiplier(shape(fine))[0] != multiplier(s)[0]);
    }
}
