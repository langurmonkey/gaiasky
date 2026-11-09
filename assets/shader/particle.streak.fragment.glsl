#version 330 core

/*
 * Velocity streaks. The shape and the size come from the vertex shader; this turns the quad into a
 * comet - round across, a small bright core at the head and a long soft tail behind it - and
 * outputs it premultiplied for the additive blend the system is drawn with.
 *
 * Distances are in widths rather than in pixels or in the parameter of the quad, because that is
 * what makes the head round: a core of a fixed size in pixels would spread over the whole length of
 * a short streak and vanish inside a long one.
 */

in float v_s;
in float v_len;
in float v_x;
in float v_a;
in vec3 v_col;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    // Across: a soft parabola, so the streak has a rounded edge instead of a hard one.
    float across = 1.0 - v_x * v_x;

    // Along: the tail falls away slowly and carries most of the light, and the core is a compact
    // gaussian at the head. The two are added rather than blended, so the head reaches about twice
    // the brightness of the plain streak and the tail about four fifths of it.
    float t = clamp(v_s / max(v_len, 1e-3), 0.0, 1.0);
    float tail = pow(1.0 - t, 1.45);
    float core = exp(-0.55 * v_s * v_s);
    float along = 0.75 * tail + 1.15 * core;

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
