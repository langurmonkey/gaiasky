uniform sampler2D u_texture;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    gl_FragColor = v_color * texture(u_texture, v_texCoords);
}
