#version 330 core

/*
 * Velocity streaks. The shape comes from the vertex shader; this only turns the quad into a soft
 * comet - round across, bright at the head and fading along the tail - and outputs it premultiplied
 * for the additive blend the system is drawn with.
 */

in float v_t;
in float v_x;
in float v_a;
in vec3 v_col;

uniform float u_ar;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    float across = 1.0 - v_x * v_x;
    float along = pow(1.0 - v_t, 1.7);
    float a = v_a * across * along;

    if (a < 0.002) {
        discard;
    }

    fragColor = vec4(v_col * a, a);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
