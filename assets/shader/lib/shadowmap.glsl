#ifndef GLSL_LIB_SHADOWMAP
#define GLSL_LIB_SHADOWMAP

#ifdef shadowMapFlag
#define bias 0.03
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;

#ifdef numCSM
// CASCADED SHADOW MAPS.

uniform sampler2D u_csmSamplers[numCSM];
uniform vec2 u_csmPCFClip[numCSM];

float getCSMShadowness(sampler2D sampler, vec3 uv, vec2 offset) {
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 16581375.0);
    return step(uv.z, dot(texture2D(sampler, uv.xy + offset), bitShifts) + bias); // (1.0/255.0)
}

float getCSMShadow(sampler2D sampler, vec3 uv, float pcf) {
    return (
    getCSMShadowness(sampler, uv, vec2(pcf,pcf)) +
    getCSMShadowness(sampler, uv, vec2(-pcf,pcf)) +
    getCSMShadowness(sampler, uv, vec2(pcf,-pcf)) +
    getCSMShadowness(sampler, uv, vec2(-pcf,-pcf)) ) * 0.25;
}
float getShadow(vec3 shadowMapUv, vec3 csmUVs[numCSM]) {
    for(int i=0; i < numCSM; i++){
        vec2 pcfClip = u_csmPCFClip[i];
        float pcf = pcfClip.x;
        float clip = pcfClip.y;
        vec3 uv = csmUVs[i];
        if(uv.x >= clip && uv.x <= 1.0 - clip &&
        uv.y >= clip && uv.y <= 1.0 - clip &&
        uv.z >= 0.0 && uv.z <= 1.0){

            #if numCSM > 0
            if(i == 0) return getCSMShadow(u_csmSamplers[0], uv, pcf);
            #endif
            #if numCSM > 1
            if(i == 1) return getCSMShadow(u_csmSamplers[1], uv, pcf);
            #endif
            #if numCSM > 2
            if(i == 2) return getCSMShadow(u_csmSamplers[2], uv, pcf);
            #endif
            #if numCSM > 3
            if(i == 3) return getCSMShadow(u_csmSamplers[3], uv, pcf);
            #endif
            #if numCSM > 4
            if(i == 4) return getCSMShadow(u_csmSamplers[4], uv, pcf);
            #endif
            #if numCSM > 5
            if(i == 5) return getCSMShadow(u_csmSamplers[5], uv, pcf);
            #endif
            #if numCSM > 6
            if(i == 6) return getCSMShadow(u_csmSamplers[6], uv, pcf);
            #endif
            #if numCSM > 7
            if(i == 7) return getCSMShadow(u_csmSamplers[7], uv, pcf);
            #endif

        }
    }
    // default map
    return getCSMShadow(u_shadowTexture, shadowMapUv, u_shadowPCFOffset);
}

#else
// REGULAR SHADOW MAPS.

float getShadowness(vec2 uv, vec2 offset, float compare){
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    return step(compare - bias, dot(texture(u_shadowTexture, uv + offset), bitShifts)); //+(1.0/255.0));
}


float textureShadowLerp(vec2 size, vec2 uv, float compare){
    vec2 texelSize = vec2(1.0) / size;
    vec2 f = fract(uv * size + 0.5);
    vec2 centroidUV = floor(uv * size + 0.5) / size;

    float lb = getShadowness(centroidUV, texelSize * vec2(0.0, 0.0), compare);
    float lt = getShadowness(centroidUV, texelSize * vec2(0.0, 1.0), compare);
    float rb = getShadowness(centroidUV, texelSize * vec2(1.0, 0.0), compare);
    float rt = getShadowness(centroidUV, texelSize * vec2(1.0, 1.0), compare);
    float a = mix(lb, lt, f.y);
    float b = mix(rb, rt, f.y);
    float c = mix(a, b, f.x);
    return c;
}

float getShadow(vec3 shadowMapUv) {
    // Complex lookup: PCF + interpolation (see http://codeflow.org/entries/2013/feb/15/soft-shadow-mapping/)
    vec2 size = vec2(1.0 / (2.0 * u_shadowPCFOffset));
    float result = 0.0;
    for(int x=-2; x<=2; x++) {
        for(int y=-2; y<=2; y++) {
            vec2 offset = vec2(float(x), float(y)) / size;
            result += textureShadowLerp(size, shadowMapUv.xy + offset, shadowMapUv.z);
        }
    }
    return result / 25.0;

    // Simple lookup
    //return getShadowness(v_data.shadowMapUv.xy, vec2(0.0), v_data.shadowMapUv.z);
}

#endif // numCSM

#endif // shadowMapFlag
#endif // GLSL_LIB_SHADOWMAP