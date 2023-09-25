#define SPIKE_WIDTH 0.02
#define CORE_SIZE 0.4

float parabola(float x, float k){
    return pow(4.0 * x * (1.0 - x), k);
}

float cubicPulse(float c, float w, float x){
    x = abs(x - c);
    if(x > w) return 0.0;
    x /= w;
    return 1.0 - x * x * (3.0 - 2.0 * x);
}

// Star with spikes (see https://www.shadertoy.com/view/3slBD8)
float starSpikes(vec2 uv){
    float d = 1.0 - length(uv - 0.5);

    float spikeV = cubicPulse(0.5, SPIKE_WIDTH, uv.x) * parabola(uv.y, 2.0) * 0.5;
    float spikeH = cubicPulse(0.5, SPIKE_WIDTH, uv.y) * parabola(uv.x, 2.0) * 0.5;
    float core = pow(d, 20.0) * CORE_SIZE;
    float corona = pow(d, 6.0);

    float val = spikeV + spikeH + core + corona;
    return val;
}

// Legacy star
float starLegacy(vec2 uv) {
    float dist_center = 1.0 - clamp(length(uv - 0.5) * 2.0, 0.0, 1.0);
    return pow(dist_center, 3.0) * 0.3 + dist_center * 0.05;
}
