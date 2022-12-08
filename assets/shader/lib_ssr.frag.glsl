#ifndef GLSL_LIB_SSR
#define GLSL_LIB_SSR
// OUTPUTS
layout (location = 2) out vec4 normalBuffer;
layout (location = 3) out vec4 reflectionMask;

void ssrBuffers(){
    normalBuffer = vec4(0.0, 0.0, 0.0, 1.0);
    reflectionMask = vec4(0.0, 0.0, 0.0, 1.0);
}
#endif