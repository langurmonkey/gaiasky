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
uniform sampler2D u_texture1;// velocity map
uniform vec2 u_viewport;
uniform float u_blurScale;
uniform int u_blurSamplesMax;
uniform float u_velScale;

in vec2 v_texCoords;

layout (location = 0) out vec4 fragColor;

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
    vec3 col = texture(u_texture0, v_texCoords).rgb;
    vec2 velocity = texture(u_texture1, v_texCoords).rg;
    velocity = maxLength(vpow(velocity, 1.6), 0.05);
    vec2 vel = velocity * u_velScale * u_blurScale;

    float speed = length(vel * u_viewport);
    int nSamples = clamp(int(speed), 1, u_blurSamplesMax);

    vec3 blurred = col;
    for (int i = 1; i < nSamples; ++i) {
        // The -0.5 centers the blur on the fragment
        vec2 offset = vel * (float(i) / float(nSamples - 0.5));
        blurred += texture(u_texture0, v_texCoords + offset).rgb;
    }
    blurred /= float(nSamples);
    fragColor = vec4(blurred, 1.0);

    //float l = abs(length(velocity) * 5.0);
    //fragColor.rgb = vec3(l);
    //fragColor.a = 1.0;
    //if(v_texCoords.y < 0.5)
    //    fragColor = vec4(blurred, 1.0);
    //
    //if(v_texCoords.x < 0.5){
    //    // Show velocity buffer
    //    fragColor = velocityBuffer(velocity);
    //}
}
