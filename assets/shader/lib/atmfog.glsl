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

    in vec3 o_fragPosition;
    in float o_fragHeight;

    vec3 applyFog(in vec3 fragColor, in vec3 rayDir,  in vec3 sunDir, in float NL) {
        if (u_fogDensity > 0.0) {
            #define U_TO_KM 1.0e6
            // Distance normalization factor depends on the size of the planet (inner atmosphere radius)
            float normFactor = clamp(fInnerRadius * U_TO_KM / 40.0, 20.0, 400.0);
            // Normalize distance to 100 Km
            float distance = length(o_fragPosition) * U_TO_KM / normFactor;
            float fragHeight = 1.0 - clamp(o_fragHeight * U_TO_KM / (normFactor * 0.08), 0.0, 1.0);
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
