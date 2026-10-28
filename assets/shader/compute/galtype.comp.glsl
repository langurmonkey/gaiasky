#version 430 core

// Voxelises the analytic galaxy density field (assets/shader/lib/galaxy.glsl) into one volume
// slot's RGBA8 texture.
//
// One invocation per voxel, one imageStore, no atomics on the image, no blur and no particle
// intermediate: the field is continuous, so every voxel is independent of every other one.
//
// Two passes over the same grid, because the texture is normalised by a quantity that is only
// known once the whole field has been evaluated:
//
//   pass 0 (u_pass = REDUCE) evaluates the field, writes nothing, and max-reduces the two
//          quantities the volume metadata calls for - the peak of the weighted sum (brightness at
//          the actual intensities) and the peak of the unweighted sum (brightness at unit
//          intensity, i.e. the "merged reference").
//   pass 1 (u_pass = WRITE) evaluates the field again and stores it, divided by whichever of the
//          two the slot needs.
//
// Two passes beat one pass plus a rescale because RGBA8 clamps: a single pass would have to write
// raw values above 1.0 (losing the peak, which is exactly the voxel the normalisation is made
// against) or pre-scale by a guess (losing 8-bit precision everywhere). Evaluating the field twice
// is a few milliseconds on an integrated GPU, once per galaxy.

layout(local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

#include <shader/lib/galaxy.glsl>

// The slot's texture. Colour in .rgb, brightness in .a - COLOR_MODE_TEXTURE in
// volume.galaxy.fragment.glsl.
layout(rgba8, binding = 0) uniform image3D u_volume;

// Peak reduction. [0] = peak of the weighted sum, [1] = peak of the unweighted sum.
layout(std430, binding = 1) buffer Peaks {
    uint peaks[2];
};

#define PASS_REDUCE 0
#define PASS_WRITE 1

// What the denominator of the stored brightness is, and therefore which peak this slot uses.
// Emissive (gas, bulge): divided by the merged reference, so the peak sits at 1.0 at unit
// intensities and lowering an intensity dims the volume instead of renormalising it.
// Occlusive (dust): divided by its own peak, so the stored peak is exactly 1.0, which is what
// DUST_SIGMA in volume.galaxy.fragment.glsl is calibrated against.
#define REF_MERGED 0
#define REF_SELF 1

// Which pass this dispatch runs.
uniform int u_pass;
// Number of fields summed into this slot, 1 to GX_MAX_FIELDS.
uniform int u_fieldCount;
// The fields, packed; see GalaxyShape and gxUnpack() in the library.
uniform float u_fields[GX_FIELD_ARRAY];
// Galaxy-space centre of voxel (0, 0, 0).
uniform vec3 u_gridOrigin;
// Galaxy-space size of one voxel. Not uniform across axes: the grid is thinner than it is wide.
uniform vec3 u_gridCell;
// Voxel counts on each axis.
uniform ivec3 u_gridDims;
// REF_MERGED or REF_SELF.
uniform int u_referenceMode;
// Extra scalar applied to the stored brightness after normalisation, so 1.0 means "the peak sits at
// the reference". This is where the calibration from sprite brightness to per-voxel emission lives:
// a dataset's `intensity` is a billboard-sprite multiplier (many overlapping quads), not a per-unit-
// length emission, so the two are off by a constant that depends on the channel.
uniform float u_gain;

// Peak of a non-negative float as an ordered uint, so that atomicMax on a uint max-reduces floats.
// floatBitsToUint is monotonically increasing for non-negative values, which is why this is
// enough and no bit twist is needed.
uint orderedBits(float v) {
    return floatBitsToUint(max(v, 0.0));
}

// Workgroup reduction scratch. At global scope because GLSL does not allow `shared` in a nested
// scope.
shared uint sWeighted[256];
shared uint sUnweighted[256];

void main() {
    ivec3 voxel = ivec3(gl_GlobalInvocationID);
    // The workgroup is 256-wide on x only, so the y and z extents are exact and only x can
    // overshoot. Voxels outside the grid still reach the barrier below, which is why this masks
    // instead of returning.
    bool inside = all(lessThan(voxel, u_gridDims));

    GalaxyFieldSample field;
    field.chroma = vec3(0.0);
    field.weighted = 0.0;
    field.unweighted = 0.0;
    if (inside) {
        vec3 p = u_gridOrigin + vec3(voxel) * u_gridCell;
        field = gxSampleFields(p, u_fields, u_fieldCount);
    }

    if (u_pass == PASS_REDUCE) {
        // Reduce inside the workgroup first: one atomic per group instead of one per voxel turns
        // millions of contended atomics into thousands.
        uint i = gl_LocalInvocationIndex;
        sWeighted[i] = orderedBits(field.weighted);
        sUnweighted[i] = orderedBits(field.unweighted);
        barrier();
        for (uint stride = 128u; stride > 0u; stride >>= 1u) {
            if (i < stride) {
                sWeighted[i] = max(sWeighted[i], sWeighted[i + stride]);
                sUnweighted[i] = max(sUnweighted[i], sUnweighted[i + stride]);
            }
            barrier();
        }
        if (i == 0u) {
            atomicMax(peaks[0], sWeighted[0]);
            atomicMax(peaks[1], sUnweighted[0]);
        }
        return;
    }

    // Reference is uniform across the dispatch, but reading it here keeps the two passes in one
    // program: the buffer is bound and the value is already in the SSBO by the time this runs.
    float reference = uintBitsToFloat(peaks[u_referenceMode == REF_SELF ? 0 : 1]);
    float brightness = u_gain * field.weighted / max(reference, 1.0e-12);

    imageStore(u_volume, voxel, vec4(clamp(field.chroma, 0.0, 1.0), clamp(brightness, 0.0, 1.0)));
}
