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

#ifndef RADIUS
#error Please define a RADIUS
#endif

#define KERNEL_SIZE (RADIUS * 2 + 1)

uniform sampler2D u_texture0;
uniform vec2 SampleOffsets[KERNEL_SIZE];
uniform float SampleWeights[KERNEL_SIZE];

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

void main()
{
	vec3 c = vec3(0.0);

	// Combine a number of weighted image filter taps.
	for (int i = 0; i < KERNEL_SIZE; i++)
	{
		c += texture(u_texture0, v_texCoords + SampleOffsets[i]).rgb * SampleWeights[i];
	}

	fragColor.rgb = c;
	fragColor.a = 1.0;
}