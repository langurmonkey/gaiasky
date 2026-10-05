// Camera motion blur effect by Toni Sagrista.
// This implementation follows: https://developer.nvidia.com/gpugems/gpugems3/part-iv-image-effects/chapter-27-motion-blur-post-processing-effect
#version 330 core

uniform sampler2D u_texture0;// scene
uniform sampler2D u_texture1;// depth map

// Delta camera position between last and this frame.
uniform vec3 u_dCam;
// Z-far and K values for depth buffer.
uniform vec2 u_zFarK;
// Projection matrix coefficients A and B, with zClip = A * zView + B,
// A = -(f+n)/(f-n), B = -2fn/(f-n).
uniform vec2 u_projAB;
// Previous projection-view matrix.
uniform mat4 u_prevProjView;
// Inverse of projection-view matrix.
uniform mat4 u_projViewInverse;

uniform float u_blurScale;
uniform int u_blurSamplesMax;

// Viewport.
uniform vec2 u_viewport;

in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

void main() {

    // Recover view-space distance from logarithmic depth buffer.
    float viewZ = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zFarK.x, u_zFarK.y);
    // NDC coordinates of this pixel.
    vec2 ndc = vec2(v_texCoords.x * 2.0 - 1.0, v_texCoords.y * 2.0 - 1.0);
    // Clip-space position of the point at view distance viewZ:
    // w = viewZ, xy = ndc * w, z = A * (-viewZ) + B.
    float w = viewZ;
    float zClip = -u_projAB.x * viewZ + u_projAB.y;
    vec4 currentPosWorld = u_projViewInverse * vec4(ndc * w, zClip, w);
    // Compute previous world position. The world position is camera-relative
    // (floating origin), in global axes. The previous-frame position of the
    // same point is obtained by subtracting the camera position delta
    // (dPos = prevPos - pos).
    vec3 previousPosWorld = currentPosWorld.xyz - u_dCam;

    // Use the world position, and transform by the previous view-
    // projection matrix.
    vec4 previousPosClip = u_prevProjView * vec4(previousPosWorld, 1.0);
    // Convert to nonhomogeneous points [-1,1] by dividing by w.
    previousPosClip /= previousPosClip.w;
    // Use this frame's position and last frame's to compute the pixel
    // velocity.
    vec2 velocity = (previousPosClip.xy - ndc) / 2.0;
    // Scale with blur scale parameter.
    vec2 vel = velocity * u_blurScale;
    // Compute viewport speed and number of smaples.
    float speed = length(vel * u_viewport);
    int nSamples = clamp(int(speed), 1, u_blurSamplesMax);

    // Get color at this fragment.
    vec3 color = texture(u_texture0, v_texCoords).rgb;
    for (int i = 1; i < nSamples; ++i) {
        vec2 offset = vel * (float(i) / float(nSamples) - 0.5);
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
