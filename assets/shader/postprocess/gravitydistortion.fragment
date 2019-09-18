#version 330 core

uniform vec2 u_viewport;
uniform sampler2D u_texture0;
uniform vec2 u_massPosition;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

float massRadius = 20.0;
float distortionRadius = 40.0;
float lim = massRadius + distortionRadius;

float lint(float x, float x0, float x1, float y0, float y1) {
    return mix(y0, y1, (x - x0) / (x1 - x0));
}

vec4 distort(sampler2D texture, vec2 texCoords, vec2 viewport) {
    vec2 pxCoords = texCoords * viewport;
    vec2 pxMass = u_massPosition;

    // Distance in pixels
    vec2 massToCoord = pxCoords - pxMass;
    float dist = length(massToCoord);


    if(dist < massRadius){
        return vec4(0.0);
    }else if(dist <= lim){
        float currlen = dist - massRadius;
        float newlen = lint(currlen, 0.0, distortionRadius, 0.0, lim);
        vec2 newTexCoord = normalize(massToCoord) * newlen + pxMass;

        return texture(texture, newTexCoord / viewport);
    }else{
        return texture(texture, texCoords.xy);
    }

}

void main() {
	fragColor = distort(u_texture0, v_texCoords, u_viewport);
}