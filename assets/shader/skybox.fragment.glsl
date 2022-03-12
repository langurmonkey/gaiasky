#version 330 core

// Uniforms
uniform samplerCube u_environmentCubemap;

// INPUT
in vec3 v_texCoords;

// OUTPUT
out vec4 gl_FragColor;

void main()
{
    gl_FragColor = texture(u_environmentCubemap, v_texCoords);
}
