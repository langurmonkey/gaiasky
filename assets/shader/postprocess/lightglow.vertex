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

#define N 30

// Pre pass
uniform sampler2D u_texture2;

uniform vec2 u_viewport;
uniform int u_nLights;
uniform int u_nSamples;
uniform float u_spiralScale;
uniform vec2 u_lightPositions[N];

in vec4 a_position;
in vec2 a_texCoord0;

out vec2 v_texCoords;

out float v_lums[N];

float fx(float t, float a){
    return a * t * cos(t);
}

float fy(float t, float a){
    return a * t * sin(t);
}

void main(){
    float ar = u_viewport.x / u_viewport.y;
    for (int li = 0; li < u_nLights; li++){
        // Size of sampling spiral
        float a = u_spiralScale;

        // Archimedes' spiral (fx = a*t*cos(t), fy = a*t*sin(t)) sampling from 0 to 3*Pi (extends to a radius of roughly 10)
        float t = 0;
        float dt = 3.0 * 3.14159 / u_nSamples;

        float lum = 0.0;
        for (int idx = 0; idx < u_nSamples; idx++){
            vec2 curr_coord = clamp(u_lightPositions[li] + vec2(fx(t, a) / ar, fy(t, a)), 0.0, 1.0);
            lum += (clamp(texture(u_texture2, curr_coord), 0.0, 1.0)).r;
            t += dt;
        }
        lum += texture(u_texture2, u_lightPositions[li] + vec2(fx(t, a) / ar, fy(t, a) * ar)).r;
        lum /= u_nSamples;

        v_lums[li] = lum;
    }
    v_texCoords = a_texCoord0;
    gl_Position = a_position;
}