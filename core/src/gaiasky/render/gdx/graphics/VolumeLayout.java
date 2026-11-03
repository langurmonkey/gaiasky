/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.util.Logger;

import java.util.List;
import java.util.Locale;

/**
 * The parts of the volume generation that do not depend on which backend evaluates the field: the
 * packing of the fields into the flat array the shader and the CPU port both read, the geometry of
 * the sampling grid inside the volume's box, and the validation the two backends have to agree on.
 * <p>
 * Everything here is a mirror of a piece of {@code assets/shader/compute/galgen.volume.comp.glsl} or
 * of {@code assets/shader/lib/galaxy.glsl}, and is shared so that the GPU and the CPU voxeliser
 * cannot disagree about where a voxel is or which field parameter lives where.
 */
public final class VolumeLayout {
    private static final Logger.Log logger = Logger.getLogger(VolumeLayout.class);

    /** Number of fields summed into one slot. Mirrors {@code GX_MAX_FIELDS}. **/
    public static final int MAX_FIELDS = GalaxyAttributes.MAX_FIELDS;
    /** Length in floats of one field in the packed array. Mirrors {@code GX_FIELD_STRIDE}. **/
    public static final int FIELD_STRIDE = GalaxyAttributes.FIELD_STRIDE;

    private VolumeLayout() {
    }

    /**
     * Packs the slot's fields into the flat array the field evaluator reads.
     *
     * @param fields The fields, in slot order.
     * @param count  How many of them to pack, at most {@link #MAX_FIELDS}.
     *
     * @return An array of {@code MAX_FIELDS * FIELD_STRIDE} floats.
     */
    public static float[] packFields(List<? extends GalaxyAttributes> fields,
                                     int count) {
        float[] packed = new float[MAX_FIELDS * FIELD_STRIDE];
        for (int i = 0; i < count; i++) {
            fields.get(i).packFields(packed, i * FIELD_STRIDE);
        }
        return packed;
    }

    /**
     * Number of fields a slot can hold, with a warning when it declares more.
     *
     * @param fields The fields of the slot.
     *
     * @return The count to use, at most {@link #MAX_FIELDS}.
     */
    public static int usableCount(List<? extends GalaxyAttributes> fields) {
        if (fields == null) {
            return 0;
        }
        int count = fields.size();
        if (count > MAX_FIELDS) {
            logger.warn("Slot declares " + count + " fields, which is more than the " + MAX_FIELDS
                                + " a volume slot can hold. Dropping the last " + (count - MAX_FIELDS) + ".");
            return MAX_FIELDS;
        }
        return count;
    }

    /**
     * Whether the slot stores density (occluding) rather than light. One slot holds either, never
     * both: the alpha channel has a single interpretation, so a mixed slot is a descriptor mistake
     * and the first field decides.
     *
     * @param fields The fields of the slot.
     * @param count  How many of them to look at.
     *
     * @return True if the slot occludes.
     */
    public static boolean slotIsOcclusive(List<? extends GalaxyAttributes> fields,
                                          int count) {
        boolean occlusive = fields.get(0).isOcclusive();
        for (int i = 1; i < count; i++) {
            if (fields.get(i).isOcclusive() != occlusive) {
                logger.warn("Slot mixes occluding and emitting fields (field 0 is "
                                    + (occlusive ? "occluding" : "emitting") + ", field " + i
                                    + " is not). The first field decides.");
                break;
            }
        }
        return occlusive;
    }

    /**
     * Warns about a spiral whose pitch is too small for its radial span: it comes out as a set of
     * radial spokes rather than a spiral, and it looks like a cross.
     *
     * @param fields The fields of the slot.
     * @param count  How many of them to check.
     */
    public static void warnOnPitch(List<? extends GalaxyAttributes> fields,
                                   int count) {
        // baseAngle is in tenths of a degree and measured from the radial direction, so a
        // plausible-looking "12" is 1.2 deg and almost straight - which is why this is worth saying
        // out loud rather than leaving the user with a cross.
        for (int i = 0; i < count; i++) {
            var field = fields.get(i);
            double sweep = field.spiralArmSweepDegrees();
            if (field.distribution.isAnySpiral() && sweep < 25.0) {
                logger.warn(String.format(Locale.ROOT,
                                          "Field %d (%s, %s) has baseAngle=%.1f, which sweeps only %.1f deg of arm "
                                                  + "between minRadius %.3f and baseRadius %.1f: the arms come out as radial spokes, not a spiral. "
                                                  + "baseAngle is in tenths of a degree, measured from the radial direction, so a 4-arm spiral "
                                                  + "with arms that curve needs something in the 400-700 range.",
                                          i, field.type, field.distribution, field.baseAngle, sweep,
                                          field.minRadius, field.baseRadius));
            }
        }
    }

    /**
     * Centre of voxel (0, 0, 0) in galaxy space.
     * <p>
     * The box is centred on the origin, so this is the near corner plus half a cell - the field is
     * evaluated at {@code origin + voxel * cell}, which is what keeps the field's symmetry (and the
     * spiral's winding) aligned with the texture's.
     *
     * @param lateralHalf  Half-extent along x and z, in galaxy space.
     * @param verticalHalf Half-extent along y.
     * @param grid         The grid being filled.
     *
     * @return {@code {x, y, z}} of the centre of the first voxel.
     */
    public static float[] gridOrigin(double lateralHalf,
                                     double verticalHalf,
                                     VoxelGrid grid) {
        float[] cell = gridCell(lateralHalf, verticalHalf, grid);
        return new float[]{
                (float) -lateralHalf + 0.5f * cell[0],
                (float) -verticalHalf + 0.5f * cell[1],
                (float) -lateralHalf + 0.5f * cell[2]
        };
    }

    /**
     * Galaxy-space size of one voxel on each axis. Not uniform: the grid is thinner than it is wide.
     *
     * @param lateralHalf  Half-extent along x and z.
     * @param verticalHalf Half-extent along y.
     * @param grid         The grid being filled.
     *
     * @return {@code {x, y, z}} voxel sizes.
     */
    public static float[] gridCell(double lateralHalf,
                                   double verticalHalf,
                                   VoxelGrid grid) {
        return new float[]{
                (float) (2.0 * lateralHalf / grid.nx),
                (float) (2.0 * verticalHalf / grid.ny),
                (float) (2.0 * lateralHalf / grid.nz)
        };
    }

    /**
     * The volume's box in object-size units: {@code {-x,-y,-z, +x,+y,+z}}, centred, with the lateral
     * half-extent at exactly 0.5 so that the largest axis spans the object's size. The vertical one
     * follows the fields' aspect, so a flat galaxy gets a flat box.
     *
     * @param lateralHalf  Half-extent along x and z, in galaxy space.
     * @param verticalHalf Half-extent along y, in galaxy space.
     *
     * @return The six corners of the box.
     */
    public static float[] boxFor(double lateralHalf,
                                 double verticalHalf) {
        float hy = (float) (0.5 * verticalHalf / Math.max(lateralHalf, 1e-9));
        return new float[]{-0.5f, -hy, -0.5f, 0.5f, hy, 0.5f};
    }

    /**
     * Whether a texture can be refilled for the given grid instead of a new one being allocated.
     * <p>
     * The dimensions and the storage format have to match exactly: one texel is written per voxel of
     * the grid, so a texture of another size would be written past its end.
     *
     * @param existing The texture currently bound to the slot, or null.
     * @param grid     The grid about to be generated.
     *
     * @return True if {@code existing} can receive the new content.
     */
    public static boolean canReuse(VolumeTexture existing,
                                   VoxelGrid grid) {
        return existing != null && existing.texture() != null
                && canReuse(existing.width(), existing.height(), existing.depth(), existing.type(), grid);
    }

    /**
     * Whether a texture of the given shape can receive a generation of the given grid. Split out from
     * {@link #canReuse(VolumeTexture, VoxelGrid)} so that the decision can be tested without a
     * texture, which needs a GL context.
     *
     * @param width  The texture's extent on the first axis.
     * @param height The texture's extent on the second axis.
     * @param depth  The texture's extent on the third axis.
     * @param type   The texture's storage format.
     * @param grid   The grid about to be generated.
     *
     * @return True if the content fits.
     */
    public static boolean canReuse(int width,
                                   int height,
                                   int depth,
                                   VolumeType type,
                                   VoxelGrid grid) {
        return grid != null && type == VolumeType.RGBA
                && width == grid.nx && height == grid.ny && depth == grid.nz;
    }
}
