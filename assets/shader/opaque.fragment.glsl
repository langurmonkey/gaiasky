// Renders all black for the occlusion testing
void main() {

    gl_FragColor = vec4(0.0, 0.5, 0.0, 1.0);

    // Debug! - vectors
    //float theta = acos(L.z); // in [0..Pi]
    //float phi = atan(L.y/L.x); // in [0..2Pi]
    //vec4 debugcol = vec4(0.0, L.y, 0.0, 1.0);
    //gl_FragColor = debugcol;

    // Debug! - visualise depth buffer
    //gl_FragColor = vec4(vec3(gl_FragCoord.z), 1.0f);
}
