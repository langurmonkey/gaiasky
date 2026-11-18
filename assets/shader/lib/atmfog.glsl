#ifndef GLSL_LIB_ATMFOG
#define GLSL_LIB_ATMFOG

#ifdef atmosphereGround
    // FOG
    #ifndef GLSL_LIB_ATMSCAT
    uniform float fInnerRadius; /* The inner (planetary) radius*/
    uniform float fOuterRadius;
    uniform float fCameraHeight;
    #endif // LIB_ATMSCAT
    uniform float u_fogDensity;
    uniform vec3 u_fogCol;
    // Internal units per km. Comes from Constants.KM_TO_U on the material, it is not a constant of
    // the shader: the distance scale factor changes it (1e-6 normally, 1e-2 with a scale of 1e4).
    uniform float u_kmToU;

    in vec3 o_fragPosition;
    in float o_fragHeight;

    // Fog e-folding distance, in km, and the height over which it fades out.
    const float FOG_NORM_KM = 400.0;
    const float FOG_HEIGHT_KM = 32.0;

    vec3 applyFog(in vec3 fragColor, in vec3 rayDir,  in vec3 sunDir, in float NL) {
        if (u_fogDensity > 0.0) {
            // o_fragPosition and o_fragHeight are in internal units, so they need the real factor.
            // This used to be a hardcoded 1e6, which is only right when the distance scale is 1.
            float uToKm = u_kmToU > 0.0 ? 1.0 / u_kmToU : 1.0e6;
            float distance = length(o_fragPosition) * uToKm / FOG_NORM_KM;
            float fragHeight = 1.0 - clamp(o_fragHeight * uToKm / FOG_HEIGHT_KM, 0.0, 1.0);
            float camHeight = 1.0 - clamp((fCameraHeight - fInnerRadius) / (fOuterRadius - fInnerRadius), 0.0, 1.0);
            float fogDensity = u_fogDensity * 0.15;

            float sunAmount = max(dot(rayDir, sunDir), 0.0);
            vec3 fogColor = mix(u_fogCol, // original color
                                    vec3(1.0, 0.9, 0.7), // yellowish
                                    pow(sunAmount, 8.0));

            float fogAmount = (1.0 - exp(-distance)) * fragHeight * fogDensity * NL * 4.0 * camHeight;
            return mix(fragColor, fogColor, fogAmount);
        } else {
            return fragColor;
        }
    }
#else
    vec3 applyFog(in vec3 fragColor, in float NL){
        return fragColor;
    }
#endif // atmosphereGround

#endif // GLSL_LIB_ATMFOG
