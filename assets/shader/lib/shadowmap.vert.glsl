#ifndef GLSL_LIB_SHADOWMAP_VERT
#define GLSL_LIB_SHADOWMAP_VERT

//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
uniform mat4 u_shadowMapProjViewTrans;

void getShadowMapUv(in vec4 pos, out vec3 shadowMapUv){
    vec4 posShadow = u_shadowMapProjViewTrans * pos;
    shadowMapUv = (posShadow.xyz / posShadow.w) * 0.5 + 0.5;
}

#ifdef shadowMapGlobalFlag
uniform mat4 u_shadowMapProjViewTransGlobal;

void getShadowMapUvGlobal(in vec4 pos, out vec3 shadowMapUvGlobal){
    vec4 posShadowGlobal = u_shadowMapProjViewTransGlobal * pos;
    shadowMapUvGlobal = (posShadowGlobal.xyz / posShadowGlobal.w) * 0.5 + 0.5;
}


#endif // shadowMapGlobalFlag
#ifdef numCSM
uniform mat4 u_csmTransforms[numCSM];
uniform sampler2D u_csmSamplers[numCSM];
uniform float u_csmClip[numCSM];
uniform float u_csmPCF;

void getCsmLightSpacePos(in vec4 pos, out vec3 csmLightSpacePos[numCSM]) {
    for(int i = 0 ; i < numCSM ; i++){
        vec4 csmPos = u_csmTransforms[i] * pos;
        csmLightSpacePos[i] = (csmPos.xyz / csmPos.w) * 0.5 + 0.5;
    }
}
#endif // numCSM
#endif // shadowMapFlag

#endif // GLSL_LIB_SHADOWMAP_VERT