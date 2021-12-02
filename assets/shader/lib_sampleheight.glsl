#include shader/lib_noise.glsl
vec4 sampleHeight(sampler2D tex, vec2 tc){
    if (u_heightSize.x < 0.0){
        // Use perlin noise
        vec2 size = vec2(u_heightNoiseSize * 2.0, u_heightNoiseSize);
        vec2 coord = tc * size;
        float frequency = 6.0;
        float n = 0.0;

        n += 1.0  * abs(psnoise(coord * frequency, size * frequency));
        n += 0.25  * abs(psnoise(coord * frequency * 4.0, size * frequency * 4.0));
        n += 0.125 * abs(psnoise(coord * frequency * 8.0, size * frequency * 8.0));

        return vec4(n);
    } else {
        // Use texture
        return vec4(vec3(1.0) - texture(tex, tc).rgb, 1.0);
    }
}
