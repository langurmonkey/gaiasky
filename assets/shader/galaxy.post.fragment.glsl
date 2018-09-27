#version 120

uniform sampler2D tex_accum;
uniform sampler2D tex_reveal;

varying vec2 v_texCoord;

void main() {
    vec4 accum = texture2D(tex_accum, TexCoord);
    float r = texture2D(tex_reveal, TexCoord).r;
    if(r == 1.0)
        discard;
    gl_FragColor = vec4(accum.rgb / max(accum.a, 0.00001), 1.0 - r);
}
