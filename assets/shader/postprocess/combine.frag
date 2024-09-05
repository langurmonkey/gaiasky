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
uniform sampler2D u_texture1;
uniform float u_src1Intensity;
uniform float u_src2Intensity;
uniform float u_src1Saturation;
uniform float u_src2Saturation;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

// The constants 0.3, 0.59, and 0.11 are chosen because the
// human eye is more sensitive to green light, and less to blue.
const vec3 GRAYSCALE = vec3(0.3, 0.59, 0.11);

// 0 = totally desaturated
// 1 = saturation unchanged
// higher = increase saturation

vec3 adjustSaturation(vec3 color, float saturation) {
	vec3 grey = vec3(dot(color, GRAYSCALE));
	return mix(grey, color, saturation);
}

void main()
{
	// lookup inputs
	vec3 src1 = texture(u_texture0, v_texCoords).rgb * u_src1Intensity;
	vec3 src2 = texture(u_texture1, v_texCoords).rgb * u_src2Intensity;

	// adjust color saturation and intensity
	src1.rgb = adjustSaturation(src1.rgb, u_src1Saturation);
	src2.rgb = adjustSaturation(src2.rgb, u_src2Saturation);

	// combine
	fragColor = vec4(src1 + src2 , 1.0);
}