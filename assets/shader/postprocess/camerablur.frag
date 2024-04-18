/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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

vec4 velocityBuffer(vec2 velocity){
    float valx = 0.0;
    float valy = 0.0;
    if (velocity.x < 0.0){
        valx = abs(velocity.x);
    }
    if (velocity.y < 0.0){
        valy = abs(velocity.y);
    }
    return vec4(abs(velocity.x), abs(velocity.y), valx + valy, 0.5) * 2.0;
}

vec2 maxLength(vec2 v, float maxLength){
    float len = length(v);
    if (len > maxLength){
        v = normalize(v) * maxLength;
    }
    return v;
}

vec2 vpow(vec2 v, float p){
    return vec2(sign(v.x) * pow(abs(v.x), p), sign(v.y) * pow(abs(v.y), p));
}

void main() {

    // depth buffer
    float depth = 1.0 / recoverWValue(texture(u_texture1, v_texCoords).r, u_zfark.x, u_zfark.y);
    // H is the viewport position at this pixel in the range -1 to 1.
    vec4 H = vec4(v_texCoords.x * 2.0 - 1.0, (1.0 - v_texCoords.y) * 2.0 - 1.0, depth, 1.0);
    // Transform by the view-projection inverse.
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
