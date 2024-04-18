// Camera motion blur effect by Toni Sagrista.
// This implementation follows: https://developer.nvidia.com/gpugems/gpugems3/part-iv-image-effects/chapter-27-motion-blur-post-processing-effect
#version 330 core

uniform sampler2D u_texture0;// scene
uniform sampler2D u_texture1;// depth map

// Delta camera position between last and this frame.
uniform vec3 u_dCam;
// Z-far and K values for depth buffer.
uniform vec2 u_zfark;
// Current projection-view matrix, inverted.
uniform mat4 u_projViewInverse;
// Previous projection-view matrix.
uniform mat4 u_prevProjView;

uniform float u_blurScale;
uniform int u_blurSamplesMax;

// Viewport.
uniform vec2 u_viewport;

in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {

    // depth buffer
    float depth = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zfark.x, u_zfark.y);
    // H is the viewport position at this pixel in the range -1 to 1 (NDC).
    vec4 H = vec4(v_texCoords.x * 2.0 - 1.0, (1.0 - v_texCoords.y) * 2.0 - 1.0, depth, 1.0);
    // Transform by the view-projection inverse. Clip coordinates.
    vec4 D = u_projViewInverse * H;
    // Transform by the view-projection inverse.
    vec4 worldPos = D / D.w;

    // Current viewport position
    vec4 currentPos = H;
    // Use the world position, and transform by the previous view-
    // projection matrix.
    vec4 previousPos = u_prevProjView * worldPos;
    // Convert to nonhomogeneous points [-1,1] by dividing by w.
    previousPos /= previousPos.w;
    // Use this frame's position and last frame's to compute the pixel
    // velocity.
    vec2 velocity = (currentPos.xy - previousPos.xy) / 2.0;

    vec2 vel = velocity * u_blurScale;

    float speed = length(vel * u_viewport);
    int nSamples = clamp(int(speed), 1, u_blurSamplesMax);

    // Get color at this fragment.
    vec3 color = texture(u_texture0, v_texCoords).rgb;
    for (int i = 1; i < nSamples; ++i) {
        vec2 offset = vel * (float(i) / float(nSamples));
        color += texture(u_texture0, v_texCoords + offset).rgb;
    }
    // Average all samples to get final color.
    color /= float(nSamples);
    fragColor = vec4(color, 1.0);

    //float l = abs(length(velocity) * 5.0);
    //fragColor.rgb = vec3(l);
    //fragColor.a = 1.0;
    //if(v_texCoords.y < 0.5)
    //    fragColor = vec4(color, 1.0);
    //
    //if(v_texCoords.x < 0.5){
    //    // Show velocity buffer
    //    fragColor = velocityBuffer(velocity);
    //}
}
