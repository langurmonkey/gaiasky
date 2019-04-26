
varying vec4 v_color;
varying vec2 v_texCoords;
varying float v_opacity;
varying float v_depth;

uniform sampler2D u_texture;
uniform float u_scale;
uniform float u_opacity;

void main(void){
    // Smoothing is adapted arbitrarily to produce crisp borders at all sizes
    float smoothing = 1.0 / (16.0 * u_scale);
    float dist = texture2D(u_texture, v_texCoords).a;
    float alpha = smoothstep(0.6 - smoothing, 0.6 + smoothing, dist);
    float aa = alpha * v_opacity * u_opacity;
	
	if (aa < 0.001)
	    discard;
	    
    gl_FragColor = vec4(v_color.rgb, aa * v_color.a);

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
