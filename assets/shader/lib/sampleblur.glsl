#ifndef GLSL_LIB_SAMPLEBLUR
#define GLSL_LIB_SAMPLEBLUR
// Single-pass 5x5 Gaussian blur with correct offsets and weights
vec4 textureBlur5x5(sampler2D tex, vec2 uv, float blurStrength) {
    vec2 texelSize = 1.0 / textureSize(tex, 0);

    // Gaussian 5x5 kernel weights (sigma = 1.0, normalized)
    // Row-major order: [-2,-2] to [2,2]
    float kernel[25] = float[25](
    0.003765, 0.015019, 0.023792, 0.015019, 0.003765,
    0.015019, 0.059912, 0.094907, 0.059912, 0.015019,
    0.023792, 0.094907, 0.150342, 0.094907, 0.023792,
    0.015019, 0.059912, 0.094907, 0.059912, 0.015019,
    0.003765, 0.015019, 0.023792, 0.015019, 0.003765
    );

    vec4 result = vec4(0.0);
    int index = 0;

    // Sample in a 5x5 grid around the current pixel
    for (int y = -2; y <= 2; y++) {
        for (int x = -2; x <= 2; x++) {
            // Calculate texture coordinate offset
            vec2 offset = vec2(float(x), float(y)) * texelSize * blurStrength;
            vec4 sampleColor = texture(tex, uv + offset);

            // Apply Gaussian weight
            result += sampleColor * kernel[index];
            index++;
        }
    }

    return result;
}

// Alternative: Simplified 5x5 with optimized sampling pattern
vec4 textureBlur5x5Optimized(sampler2D tex, vec2 uv, float blurStrength) {
    vec2 texelSize = 1.0 / textureSize(tex, 0);

    // We can optimize by sampling in a cross pattern and reusing samples
    // This uses 9 texture samples instead of 25, with minimal quality loss

    // Sample center and 4 cardinal directions
    vec4 center = texture(tex, uv);
    vec4 right = texture(tex, uv + vec2(blurStrength, 0.0) * texelSize);
    vec4 left = texture(tex, uv - vec2(blurStrength, 0.0) * texelSize);
    vec4 up = texture(tex, uv + vec2(0.0, blurStrength) * texelSize);
    vec4 down = texture(tex, uv - vec2(0.0, blurStrength) * texelSize);

    // Sample 4 diagonal directions
    vec2 diagOffset = vec2(blurStrength, blurStrength) * texelSize * 0.707;
    vec4 topRight = texture(tex, uv + diagOffset);
    vec4 topLeft = texture(tex, uv + vec2(-diagOffset.x, diagOffset.y));
    vec4 bottomRight = texture(tex, uv + vec2(diagOffset.x, -diagOffset.y));
    vec4 bottomLeft = texture(tex, uv - diagOffset);

    // Apply 5x5 Gaussian weights approximated with 9 samples
    // Center gets the full center weight plus contributions from nearby samples
    vec4 result = center * 0.204164;  // Approximates sum of center 3x3

    // Cardinal directions get combined weights
    result += (right + left + up + down) * 0.180914;

    // Diagonal directions
    result += (topRight + topLeft + bottomRight + bottomLeft) * 0.123832;

    return result;
}

// Precise 5x5 Gaussian blur with explicit offsets (most accurate)
vec4 textureGaussianBlur5x5(sampler2D tex, vec2 uv, float sigma) {
    vec2 texelSize = 1.0 / textureSize(tex, 0);

    // Pre-calculated offsets for a 5x5 grid
    vec2 offsets[25] = vec2[25](
    vec2(-2.0, -2.0), vec2(-1.0, -2.0), vec2(0.0, -2.0), vec2(1.0, -2.0), vec2(2.0, -2.0),
    vec2(-2.0, -1.0), vec2(-1.0, -1.0), vec2(0.0, -1.0), vec2(1.0, -1.0), vec2(2.0, -1.0),
    vec2(-2.0, 0.0),  vec2(-1.0, 0.0),  vec2(0.0, 0.0),  vec2(1.0, 0.0),  vec2(2.0, 0.0),
    vec2(-2.0, 1.0),  vec2(-1.0, 1.0),  vec2(0.0, 1.0),  vec2(1.0, 1.0),  vec2(2.0, 1.0),
    vec2(-2.0, 2.0),  vec2(-1.0, 2.0),  vec2(0.0, 2.0),  vec2(1.0, 2.0),  vec2(2.0, 2.0)
    );

    // Calculate Gaussian weights dynamically based on sigma
    float twoSigma2 = 2.0 * sigma * sigma;
    float weights[25];
    float weightSum = 0.0;

    for (int i = 0; i < 25; i++) {
        float x = offsets[i].x;
        float y = offsets[i].y;
        float weight = exp(-(x*x + y*y) / twoSigma2);
        weights[i] = weight;
        weightSum += weight;
    }

    // Normalize weights
    for (int i = 0; i < 25; i++) {
        weights[i] /= weightSum;
    }

    // Apply convolution
    vec4 result = vec4(0.0);
    for (int i = 0; i < 25; i++) {
        vec2 offset = offsets[i] * texelSize;
        result += texture(tex, uv + offset) * weights[i];
    }

    return result;
}

// Simple drop-in replacement with adjustable blur amount
vec4 textureBlur5x5Simple(sampler2D tex, vec2 uv) {
    return textureBlur5x5(tex, uv, 1.0);
}
#endif