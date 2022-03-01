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

uniform float offset_x;
uniform float offset_y;
uniform float zoom;

in vec4 a_position;
in vec2 a_texCoord0;

layout (location = 0) out vec4 fragColor;

void main()
{
	v_texCoords = (a_texCoord0 - vec2(offset_x, offset_y)) * zoom;
	gl_Position = a_position;
}