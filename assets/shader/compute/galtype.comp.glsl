#version 430 core

// Voxelises the analytic galaxy density field (assets/shader/lib/galaxy.glsl) into one volume
// slot's RGBA8 texture.
//
// One invocation per voxel, one imageStore, no atomics on the image, no blur and no particle
// intermediate: the field is continuous, so every voxel is independent of every other one.
//
// Two passes over the same grid, because the texture is normalised by quantities that are only
// known once the whole field has been evaluated:
//
//   pass 0 (u_pass = REDUCE) evaluates the field, writes nothing, and reduces four statistics: the
//          peak brightness of the sum at the actual intensities and at unit intensity (the latter
//          is the "merged reference" of the volume metadata), and the luma sum of each.
//   pass 1 (u_pass = WRITE) evaluates the field again and stores it, divided by the unit peak and
//          scaled by the gain derived from the unit luma sum - see u_targetMeanRayIntegral.
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

// Results of the reduce pass. Peaks as ordered float bits, sums as fixed point.
layout(std430, binding = 1) buffer Stats {
    // [0] peak brightness of the sum at the actual intensities, [1] at unit intensity.
    uint peaks[2];
    // [0] sum of luma(light), [1] sum of luma(unitLight), scaled by GX_SUM_SCALE.
    uint sums[2];
};

#define PASS_REDUCE 0
#define PASS_WRITE 1

// Fixed point scale of the luma sums. A uint holds close to 2^32, the budget caps a grid at 16M
// voxels and a voxel's luma is a few units at most, so 32 leaves plenty of headroom while keeping
// the rounding error of a single voxel at 1/64 - and the error of the mean, which is what the gain
// is derived from, millions of times smaller than that.
#define GX_SUM_SCALE 32.0

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
// Voxel index this dispatch starts at. Zero for a whole-grid dispatch; a z-slab dispatcher moves it
// so that a grid too large to fill in one frame can be built across several.
uniform ivec3 u_gridOffset;
// Mean ray integral this slot is calibrated to: the luma a face-on ray accumulates through the
// texture, in UVW units - i.e. the mean luma of the stored voxels. 0 or less disables the
// calibration and leaves u_gain acting alone. See VolumeCalibration on the Java side for the
// numbers and where they come from.
uniform float u_targetMeanRayIntegral;
// Manual scale on top of the calibration.
uniform float u_gain;

// Workgroup reduction scratch. At global scope because GLSL does not allow `shared` in a nested
// scope.
shared uint sPeakLight[256];
shared uint sPeakUnit[256];
shared uint sSumLight[256];
shared uint sSumUnit[256];

// Peak of a non-negative float as an ordered uint, so that atomicMax on a uint max-reduces floats.
// floatBitsToUint is monotonically increasing for non-negative values, which is why this is
// enough and no bit twist is needed.
uint orderedBits(float v) {
    return floatBitsToUint(max(v, 0.0));
}

void main() {
    ivec3 voxel = ivec3(gl_GlobalInvocationID) + u_gridOffset;
    // The workgroup is 256-wide on x only, so the y and z extents are exact and only x can
    // overshoot. Voxels outside the grid still reach the barrier below, which is why this masks
    // instead of returning.
    bool inside = all(lessThan(voxel, u_gridDims));

    GalaxyFields field;
    field.light = vec3(0.0);
    field.unitLight = vec3(0.0);
    if (inside) {
        vec3 p = u_gridOrigin + vec3(voxel) * u_gridCell;
        field = gxSampleFields(p, u_fields, u_fieldCount);
    }

    float brightLight = gxBrightness(field.light);
    float brightUnit = gxBrightness(field.unitLight);

    if (u_pass == PASS_REDUCE) {
        // Reduce inside the workgroup first: one atomic per group instead of one per voxel turns
        // millions of contended atomics into thousands.
        uint i = gl_LocalInvocationIndex;
        sPeakLight[i] = orderedBits(brightLight);
        sPeakUnit[i] = orderedBits(brightUnit);
        // Rounded sums, so that the rounding does not bias the mean in one direction.
        sSumLight[i] = uint(gxLuma(field.light) * GX_SUM_SCALE + 0.5);
        sSumUnit[i] = uint(gxLuma(field.unitLight) * GX_SUM_SCALE + 0.5);
        barrier();
        for (uint stride = 128u; stride > 0u; stride >>= 1u) {
            if (i < stride) {
                sPeakLight[i] = max(sPeakLight[i], sPeakLight[i + stride]);
                sPeakUnit[i] = max(sPeakUnit[i], sPeakUnit[i + stride]);
                sSumLight[i] += sSumLight[i + stride];
                sSumUnit[i] += sSumUnit[i + stride];
            }
            barrier();
        }
        if (i == 0u) {
            atomicMax(peaks[0], sPeakLight[0]);
            atomicMax(peaks[1], sPeakUnit[0]);
            atomicAdd(sums[0], sSumLight[0]);
            atomicAdd(sums[1], sSumUnit[0]);
        }
        return;
    }

    // The normalisation is the peak at *unit* intensity, which is the "merged reference" of the
    // volume metadata: a property of the shape, so lowering an intensity dims the volume instead of
    // renormalising it. The mean is taken at unit intensity for the same reason.
    float reference = uintBitsToFloat(peaks[1]);
    // Fixed point back to a mean. In float: the sum reaches a few 1e9 at the largest budget, and
    // float's 24-bit mantissa still resolves the mean to a few 1e-7, far below the 1/255 the texture
    // can store.
    float meanLuma = float(sums[1])
            / (GX_SUM_SCALE * float(u_gridDims.x) * float(u_gridDims.y) * float(u_gridDims.z));

    float brightness = brightLight;
    if (u_targetMeanRayIntegral > 0.0 && meanLuma > 0.0 && reference > 0.0) {
        // The gain that makes this slot's mean ray integral the target one. Derived from the
        // measured mean and not from an analytic estimate, so it holds at any grid resolution and
        // for any shape. Note it does not see the intensities - both the mean and the reference are
        // measured at unit intensity - which is what keeps an intensity a linear knob instead of a
        // renormalisation.
        brightness *= u_targetMeanRayIntegral * reference / meanLuma;
    }
    brightness *= u_gain / max(reference, 1.0e-12);

    vec3 chroma = field.light / max(brightLight, 1.0e-12);
    imageStore(u_volume, voxel, vec4(clamp(chroma, 0.0, 1.0), clamp(brightness, 0.0, 1.0)));
}
