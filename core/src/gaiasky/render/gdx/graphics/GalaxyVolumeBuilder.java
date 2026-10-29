/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Settings.GraphicsQuality;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorts a galaxy's datasets into the two volume slots the raymarcher knows about and generates one
 * texture per slot.
 * <p>
 * The split is by meaning, not by channel: gas and the bulge both emit and share slot 0 (so a bulge
 * standing in front of gas adds light to it instead of occluding it), while dust only absorbs and gets
 * slot 1. Stars and HII regions stay billboards - a density field would smear them into a cloud - and
 * so does any dataset whose descriptor says so.
 * <p>
 * The slot's box is the union of the fields that go into it (see
 * {@link BillboardDataset#volumeGrid(List, double, GraphicsQuality)}), because a bulge is often taller
 * than the gas disk it sits in and would otherwise be cut in half.
 */
public final class GalaxyVolumeBuilder {

    /** Emissive slot index, matching {@code volume.galaxy.fragment.glsl}. **/
    public static final int SLOT_EMISSIVE = 0;
    /** Occlusive slot index. **/
    public static final int SLOT_OCCLUSIVE = 1;
    /** Number of volume slots a generated galaxy may use. **/
    public static final int SLOTS = 2;

    /**
     * One volume to generate: the datasets that share it, the grid they fit in, and the mean luma the
     * calibration should aim for.
     *
     * @param slot            The slot index, {@link #SLOT_EMISSIVE} or {@link #SLOT_OCCLUSIVE}.
     * @param datasets        The datasets, at most {@link BillboardDataset#MAX_FIELDS}.
     * @param grid            The grid that holds all of them.
     * @param targetMeanLuma  Mean luma at unit intensity, from {@link VolumeCalibration}.
     */
    public record Slot(int slot, List<BillboardDataset> datasets, VoxelGrid grid, float targetMeanLuma) {
    }

    private GalaxyVolumeBuilder() {
    }

    /**
     * Works out what to generate, without touching GL.
     *
     * @param datasets The galaxy's datasets.
     * @param sizePc   The galaxy's lateral size, in parsecs.
     * @param quality  Graphics quality, which sets the voxel size and the voxel budget.
     *
     * @return One entry per slot that has anything in it, in slot order. Empty if no dataset asks for
     * a volume.
     */
    public static List<Slot> plan(BillboardDataset[] datasets, double sizePc, GraphicsQuality quality) {
        var result = new ArrayList<Slot>(SLOTS);
        if (datasets == null || datasets.length == 0) {
            return result;
        }
        for (int slot = 0; slot < SLOTS; slot++) {
            var members = collect(datasets, slot);
            if (members.isEmpty()) {
                continue;
            }
            var grid = BillboardDataset.volumeGrid(members, sizePc, quality);
            float target = slot == SLOT_OCCLUSIVE
                    ? (float) VolumeCalibration.OCCLUSIVE_TARGET_MEAN_LUMA
                    : (float) VolumeCalibration.EMISSIVE_TARGET_MEAN_LUMA;
            result.add(new Slot(slot, members, grid, target));
        }
        return result;
    }

    /**
     * The datasets of a slot, in descriptor order, capped at what the compute shader can take.
     *
     * @param datasets The galaxy's datasets.
     * @param slot     The slot index.
     *
     * @return The datasets that belong to the slot, never null.
     */
    public static List<BillboardDataset> collect(BillboardDataset[] datasets, int slot) {
        var members = new ArrayList<BillboardDataset>();
        if (datasets == null) {
            return members;
        }
        for (var dataset : datasets) {
            if (dataset != null && dataset.volumeSlot() == slot) {
                members.add(dataset);
                if (members.size() == BillboardDataset.MAX_FIELDS) {
                    break;
                }
            }
        }
        return members;
    }

    /**
     * Generates the textures of a galaxy. Must run on the GL thread.
     *
     * @param datasets The galaxy's datasets.
     * @param sizePc   The galaxy's lateral size, in parsecs.
     * @param quality  Graphics quality.
     *
     * @return The textures, indexed by slot; an entry is null when the slot is empty or could not be
     * generated. Never null itself.
     */
    public static VolumeTexture[] generate(BillboardDataset[] datasets, double sizePc, GraphicsQuality quality) {
        var textures = new VolumeTexture[SLOTS];
        var generator = GalaxyVolumeGenerator.instance();
        for (var slot : plan(datasets, sizePc, quality)) {
            textures[slot.slot()] = generator.generate(slot.datasets(), slot.grid(), slot.targetMeanLuma());
        }
        return textures;
    }
}
