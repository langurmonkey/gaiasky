/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import java.util.ArrayList;
import java.util.List;

/**
 * The complete description of one procedural galaxy, as produced by a single random draw of
 * {@link GalaxyGenerator}.
 * <p>
 * It carries both halves of the same galaxy: the billboard datasets that sample it as particles
 * ({@link #full} and {@link #half}), and the fields that voxelise it as an analytic density
 * ({@link #volumeFields}). The two are views of one draw and not independent descriptions - building
 * the volume from anything else would give a galaxy that does not match its own stars.
 * <p>
 * The legacy billboard-only path uses {@link #full} and {@link #half}; a hybrid galaxy uses
 * {@link #full} for its stars and {@link #volumeFields} for its volume, and ignores {@link #half}
 * because the gas, bulge and dust it holds are no longer drawn as sprites.
 */
public class GalaxySpec {
    /** The morphology this spec was generated from. **/
    public final GalaxyGenerator.GalaxyMorphology morphology;
    /** The seed of the draw, so that the same galaxy can be rebuilt later. **/
    public final long seed;
    /** Full-resolution billboard channels: stars, HII regions and points. **/
    public final BillboardDataset[] full;
    /** Half-resolution billboard channels: the gas, bulge and dust sprites a billboard galaxy draws. **/
    public final BillboardDataset[] half;
    /** The same gas, bulge and dust, as volume fields. **/
    public final GalaxyAttributes[] volumeFields;

    public GalaxySpec(GalaxyGenerator.GalaxyMorphology morphology,
                      long seed,
                      BillboardDataset[] full,
                      BillboardDataset[] half) {
        this.morphology = morphology;
        this.seed = seed;
        this.full = full;
        this.half = half;
        this.volumeFields = toVolumeFields(half);
    }

    /**
     * Copies the volumetric channels of a billboard half into volume fields, dropping the sampler
     * parameters and giving the occluding ones a default occlusion fraction.
     *
     * @param half The half-resolution billboard channels.
     *
     * @return The volume fields, in the same order.
     */
    private static GalaxyAttributes[] toVolumeFields(BillboardDataset[] half) {
        List<GalaxyAttributes> fields = new ArrayList<>();
        for (var dataset : half) {
            if (!dataset.type.isVolumetric()) {
                continue;
            }
            var field = new GalaxyAttributes(dataset);
            // The field model has one spiral, so the volume half of a galaxy never names the second
            // one: SPIRAL_LOG collapses onto SPIRAL (Distribution#toVolumeDistribution). The billboard
            // datasets keep it, since a dust sprite is scaled differently in each.
            field.distribution = field.distribution.toVolumeDistribution();
            // The dataset's intensity is a sprite alpha; a field's is a density weight. See
            // GalaxyAttributes.initialVolumeIntensity.
            field.intensity = GalaxyAttributes.initialVolumeIntensity(field.type);
            if (field.isOcclusive() && field.occlusionThreshold <= 0f) {
                field.occlusionThreshold = DEFAULT_DUST_OCCLUSION_THRESHOLD;
            }
            fields.add(field);
        }
        return fields.toArray(new GalaxyAttributes[0]);
    }

    /**
     * Fraction of its own peak above which a generated dust field hides what is behind it.
     * <p>
     * A field is only opaque above this level, and the generator has no way to know how thick a
     * particular morphology's lanes should be, so it picks a conservative value: dense lanes hide,
     * the faint outskirts only dim. A scene that wants different behaviour writes a positive
     * {@code occlusionThreshold} on the field, which takes precedence.
     */
    public static final float DEFAULT_DUST_OCCLUSION_THRESHOLD = 0.05f;

    /** @return The fields that go into the emissive slot, in descriptor order. **/
    public List<GalaxyAttributes> emissiveFields() {
        return fieldsOfSlot(false);
    }

    /** @return The fields that go into the occlusive slot, in descriptor order. **/
    public List<GalaxyAttributes> occlusiveFields() {
        return fieldsOfSlot(true);
    }

    private List<GalaxyAttributes> fieldsOfSlot(boolean occlusive) {
        List<GalaxyAttributes> out = new ArrayList<>();
        for (var field : volumeFields) {
            if (field.isOcclusive() == occlusive) {
                out.add(field);
            }
        }
        return out;
    }

    /** @return Whether this spec describes a galaxy with anything to voxelise. **/
    public boolean hasVolume() {
        return volumeFields.length > 0;
    }
}
