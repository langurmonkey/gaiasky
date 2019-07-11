#ifdef atmosphereGround
// FOG
uniform float fInnerRadius; /* The inner (planetary) radius*/
uniform float fOuterRadius;
uniform float fCameraHeight;
uniform float u_fogDensity;
uniform vec3 u_fogCol;

in vec3 o_fragPosition;
in float o_fragHeight;

vec3 applyFogToColor(in vec3  fragColor,      // original color of the pixel
in float distance, // distance to fragment in Km
in float camHeight, // camera height in Km
in float fragHeight, // fragment Height in Km
in float NL) {
    float fogAmount = (1.0 - exp(-distance * camHeight * u_fogDensity * NL)) / camHeight;
    return mix(fragColor, u_fogCol, fogAmount);
}

vec3 applyFog(in vec3 fragColor, in float NL){
    if(u_fogDensity > 0.0){
        #define U_TO_KM 1.0e6
        // Distance normalized with planet radius (to 637 Km in Earth)
        float distToFrag = saturate(length(o_fragPosition) * U_TO_KM / (fInnerRadius * 90000.0));
        // Fragment height, what?
        float fragHeight = o_fragHeight * U_TO_KM ;
        // Camera height normalized to atmosphere height
        float camHeight = max((fCameraHeight - fInnerRadius) / (fOuterRadius - fInnerRadius), 0.5) + 1.0;
        return applyFogToColor(fragColor.rgb, distToFrag, camHeight, fragHeight, NL * 1.5);
    }
}
#else
vec3 applyFog(in vec3 fragColor, in float NL){
    return fragColor;
}
#endif
