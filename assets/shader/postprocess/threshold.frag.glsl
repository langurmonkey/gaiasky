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

uniform sampler2D u_texture0;
uniform float u_threshold;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

float luma(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main()
{
    vec3 tex = clamp(texture(u_texture0, v_texCoords).rgb, 0.0, 10.0);
    float brightness = luma(tex);
    // Soft knee: a hard step makes everything brighter than the threshold contribute in full, so a
    // saturated sky blooms as hard as a star. The shoulder fades the contribution in instead.
    float high = max(u_threshold * 2.0, u_threshold + 1.0e-4);
    float w = smoothstep(u_threshold, high, brightness);
    fragColor = vec4(tex.rgb * w, 1.0);
}