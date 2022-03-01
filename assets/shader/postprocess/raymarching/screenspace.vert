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

in vec4 a_position;
in vec2 a_texCoord0;

uniform mat4 u_frustumCorners;
uniform mat4 u_camInvViewTransform;

out vec2 v_texCoords;
out vec3 v_ray;

void main() {
	float index = a_position.z;

	v_texCoords = a_texCoord0;
	gl_Position = a_position;
    gl_Position.z = 0.1;

	vec4 ray = u_frustumCorners[int(index)];
	ray /= abs(ray.z);
	ray = u_camInvViewTransform * ray;
	v_ray = ray.xyz;
}