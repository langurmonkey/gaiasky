
// v_texCoords are UV coordinates in [0..1]
varying vec2 v_texCoords;
varying float v_alpha;
varying float v_textureNumber;
varying float v_depth;

uniform sampler2D u_nebulaTexture0;
uniform sampler2D u_nebulaTexture1;
uniform sampler2D u_nebulaTexture2;
uniform sampler2D u_nebulaTexture3;


vec4 getSample(){
	if(v_textureNumber < 1.0){
		return texture2D(u_nebulaTexture0, v_texCoords);
	}else if(v_textureNumber < 2.0){
		return texture2D(u_nebulaTexture1, v_texCoords);
	}else if(v_textureNumber < 3.0){
		return texture2D(u_nebulaTexture2, v_texCoords);
	}else{
		return texture2D(u_nebulaTexture3, v_texCoords);
	}
}

void main() {
    vec4 texColor = getSample();
    gl_FragColor = vec4(texColor.rgb, 1.0) * texColor.a * v_alpha;

	// Logarithmic depth buffer
	gl_FragDepth = v_depth;
}
