#include shader/lib_noise.glsl
vec4 sampleHeight(sampler2D tex, vec2 tc){
    if (u_heightSize.x < 0.0){
        // Use perlin noise
        float size = u_heightNoiseSize;
        vec2 coord = tc * size;
        float frequency = 4.0;
        float n = 0.0;

        n += 1.0  * abs(psnoise(coord * frequency, vec2(size * frequency)));
        n += 0.5  * abs(psnoise(coord * frequency * 2.0, vec2(size * frequency * 2.0)));
        n += 0.25 * abs(psnoise(coord * frequency * 4.0, vec2(size * frequency * 4.0)));

        return vec4(n);
    } else {
        // Use texture
        return texture(tex, tc);
    }
}
