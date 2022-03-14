// OUTPUTS
layout (location = 2) out vec4 normalBuffer;
layout (location = 3) out vec4 reflectionMask;

void ssrBuffers(){
    normalBuffer = vec4(0.0, 0.0, 0.0, 1.0);
    reflectionMask = vec4(0.0, 0.0, 0.0, 1.0);
}
