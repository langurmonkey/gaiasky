#ifdef ssrFlag
// INPUT
in vec4 v_fragPosView;

// OUTPUTS
layout (location = 2) out vec4 normalBuffer;
layout (location = 3) out vec4 reflectionMask;
layout (location = 4) out vec4 positionBuffer;

void ssrBuffers(){
    normalBuffer = vec4(0.0, 0.0, 0.0, 1.0);
    reflectionMask = vec4(0.0, 0.0, 0.0, 1.0);
    positionBuffer = v_fragPosView;
}
#endif // ssrFlag
