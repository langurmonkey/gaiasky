#version 330 core
// Light scattering implementation by Toni Sagrista

uniform sampler2D u_texture0;
uniform vec2 u_viewport;

uniform vec2 u_lightPositions[10];
uniform float u_lightViewAngles[10];

uniform float u_decay;
uniform float u_density;
uniform float u_weight;
uniform int u_numSamples;
uniform int u_nLights;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

float len(vec2 vect, float ar){
	return sqrt(vect.x * vect.x * ar * ar + vect.y * vect.y);
}

void main()
{
	 float exposure = 0.4;
	 float ar = u_viewport.x / u_viewport.y;
	 fragColor = vec4(0.0);

	 vec4 col = vec4(0.0);
	 for (int clpos = 0; clpos < u_nLights; clpos++){
	 	 float viewAngle = u_lightViewAngles[clpos];
		 vec2 deltaTexCoord = (v_texCoords - u_lightPositions[clpos]);
		 float d1 = length(deltaTexCoord);
		 
		 vec2 tc = vec2(v_texCoords);

		 deltaTexCoord *= 1.0 / float(u_numSamples) * u_density;
		 float illuminationDecay = 1.0;
		 
		 vec4 color = texture(u_texture0, tc);
		 
		 tc += deltaTexCoord * fract( sin(dot(v_texCoords.xy + fract(12.33), vec2(12.9898, 78.233)))* 43758.5453 );
		 for(int i = 0; i < u_numSamples; i++){
		    tc -= deltaTexCoord;
		    vec4 sampl = texture(u_texture0, tc);
		    if(len(tc - u_lightPositions[clpos], ar) > viewAngle){
		    	sampl = vec4(0.0, 0.0, 0.0, 0.0);
		    }
		    
		    sampl *= illuminationDecay * u_weight;
		    color += sampl;
		    illuminationDecay *= u_decay;
		 }
		 fragColor += clamp(color * exposure, 0.0, 1.0);
	 }
}
