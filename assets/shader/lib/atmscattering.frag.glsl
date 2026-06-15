#ifndef GLSL_LIB_ATMSCAT
#define GLSL_LIB_ATMSCAT

#if defined(atmosphereGround) || defined(atmosphericScattering)
#include <shader/lib/luma.glsl>

#define exposure 2.0

uniform vec3 v3PlanetPos;       /* Planet position relative to camera origin */
uniform vec3 v3CameraPos;       /* Camera position transformed into space matching uniforms */
uniform vec3 v3LightPos;        /* Direction vector towards the sun */
uniform vec3 v3InvWavelength;   /* 1 / pow(wavelength, 4) */

uniform float fCameraHeight;
uniform float fOuterRadius;
uniform float fInnerRadius;
uniform float fKrESun;
uniform float fKmESun;
uniform float fKr4PI;
uniform float fKm4PI;
uniform float fScale;
uniform float fScaleDepth;
uniform float fScaleOverScaleDepth;
uniform float fAlpha;
uniform float fG;
uniform vec3 v3O3InvWavelength;
uniform float fO3PeakHeight;
uniform float fO3Width;

uniform int nSamples;

#define EPSILON 1e-8

float rayleighPhase(float fCos2) {
    return 0.75 + 0.75 * fCos2;
}

float miePhase(float fCos, float fCos2) {
    float g2 = fG * fG;
    return 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow(1.0 + g2 - 2.0 * fG * fCos, 1.5);
}

float scale(float fCos) {
    float x = 1.0 - fCos;
    return fScaleDepth * exp(-0.00287 + x * (0.459 + x * (3.83 + x * (-6.80 + x * 5.25))));
}

float getNearIntersection(vec3 pos, vec3 ray, float distance2, float radius2) {
    float B = 2.0 * dot(pos, ray);
    float C = distance2 - radius2;
    float fDet = max(0.0, B * B - 4.0 * C);
    return 0.5 * (-B - sqrt(fDet));
}

// ------------------------------------------------------------------
// CORE NUMERICAL ATMOSPHERE INTEGRATOR
// ------------------------------------------------------------------
vec3 integrateAtmosphere(vec3 v3Start, vec3 v3Ray, float fFar, out vec3 v3OutAttenuate) {
    float fSampleLength = fFar / float(nSamples);
    float fScaledLength = fSampleLength * fScale;
    vec3 v3SampleRay = v3Ray * fSampleLength;
    vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;

    vec3 v3FrontColor = vec3(0.0);

    // Transmittance starts perfectly clear
    vec3 v3CurrentAttenuation = vec3(1.0);
    float fCameraOpticalDepth = 0.0;

    for (int i = 0; i < nSamples; i++) {
        float fHeight = max(length(v3SamplePoint), EPSILON);
        float fHeightDiff = fInnerRadius - fHeight;
        float fDepth = exp(fScaleOverScaleDepth * min(fHeightDiff, 0.0));

        // Dynamic optical depth checking from this specific ray slice towards space
        float fLightAngle = dot(v3LightPos, v3SamplePoint) / fHeight;
        float fLightOpticalDepth = fDepth * scale(fLightAngle);

        fCameraOpticalDepth += fDepth * fScaledLength;
        float fScatter = fLightOpticalDepth + fCameraOpticalDepth;

        // Stratosphere Ozone profiling
        float fO3Height = fHeight - fInnerRadius;
        float fO3Density = exp(-((fO3Height - fO3PeakHeight) * (fO3Height - fO3PeakHeight)) / (fO3Width * fO3Width));
        float fO3Extinction = fO3Density * fScaledLength;

        // Evaluate actual attenuation for this specific ray position slice
        v3CurrentAttenuation = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI) - (fCameraOpticalDepth * fO3Extinction * v3O3InvWavelength));

        // Accumulate scattered atmospheric coloring
        v3FrontColor += v3CurrentAttenuation * (fDepth * fScaledLength);
        v3SamplePoint += v3SampleRay;
    }

    v3OutAttenuate = v3CurrentAttenuation;
    return v3FrontColor;
}
#endif // atmosphereGround || atmosphericScattering


// ------------------------------------------------------------------
// GROUND SHADER
// ------------------------------------------------------------------
#ifdef atmosphereGround
void computeAtmosphericScatteringGround(vec3 v_position, out vec3 outGlow, out vec3 outTransmittance) {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;

    vec3 v3Pos = normalize(v_position) * fInnerRadius;
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length(v3Ray);
    v3Ray /= fFar;

    vec3 v3Start;
    if (fCameraHeight < fOuterRadius) {
        v3Start = v3CameraPos;
    } else {
        float fNear = getNearIntersection(v3CameraPos, v3Ray, fCameraHeight2, fOuterRadius2);
        v3Start = v3CameraPos + v3Ray * fNear;
        fFar -= fNear;
    }

    vec3 v3Attenuate;
    vec3 v3FrontColor = integrateAtmosphere(v3Start, v3Ray, fFar, v3Attenuate);

    float fCos = clamp(dot(v3LightPos, v3Ray), -0.9999, 0.9999);
    float fCos2 = fCos * fCos;

    vec3 rayleighColor = rayleighPhase(fCos2) * v3FrontColor * (v3InvWavelength * fKrESun);
    vec3 mieColor = miePhase(fCos, fCos2) * v3FrontColor * fKmESun;

    // Outputs
    outGlow = (vec3(1.0) - exp((rayleighColor + mieColor) * -exposure)) * fAlpha;
    outTransmittance = v3Attenuate;
}
#else
void computeAtmosphericScatteringGround(vec3 v_position, out vec3 outGlow, out vec3 outTransmittance){
    outGlow = vec3(0.0);
    outTransmittance = vec3(1.0);
}
#endif


// ------------------------------------------------------------------
// SKY SHADER
// ------------------------------------------------------------------
#ifdef atmosphericScattering
vec4 computeAtmosphericScattering(vec3 v_position) {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;

    // Sky sphere uses the outer radius bounding shell
    vec3 v3Pos = normalize(v_position) * fOuterRadius;
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length(v3Ray);
    v3Ray /= fFar;

    vec3 v3Start;
    if (fCameraHeight < fOuterRadius) {
        v3Start = v3CameraPos;
    } else {
        float fNear = getNearIntersection(v3CameraPos, v3Ray, fCameraHeight2, fOuterRadius2);
        v3Start = v3CameraPos + v3Ray * fNear;
        fFar -= fNear;
    }

    vec3 v3Attenuate;
    vec3 v3FrontColor = integrateAtmosphere(v3Start, v3Ray, fFar, v3Attenuate);

    // Match directional winding of sky geometry (-v3LightPos)
    float fCos = clamp(dot(v3LightPos, v3Ray), -0.9999, 0.9999);
    float fCos2 = fCos * fCos;

    vec3 rayleighColor = rayleighPhase(fCos2) * v3FrontColor * (v3InvWavelength * fKrESun);
    vec3 mieColor = miePhase(fCos, fCos2) * v3FrontColor * fKmESun;

    vec4 tonedAtmosphere = vec4(vec3(1.0) - exp((rayleighColor + mieColor) * -exposure), 1.0);

    // inner goes to half the height of the atmosphere.
    float inner = fInnerRadius + (fOuterRadius - fInnerRadius) * 0.1;
    // current height, normalized in [0,1], with 1 at the atmosphere half height and 0 in space.
    float heightNormalized = 1.0 - clamp(((fCameraHeight - inner) / (fOuterRadius - inner)), 0.0, 1.0);
    float fadeFactor = smoothstep(0.1, 1.0, 1.0 - heightNormalized);
    float lma = luma(tonedAtmosphere.rgb);
    float scl = smoothstep(0.2, 0.5, lma);

    tonedAtmosphere.a = (heightNormalized * (1.0 - fadeFactor) + lma * fadeFactor) * scl * fAlpha;
    return tonedAtmosphere;
}
#else
vec4 computeAtmosphericScattering(vec3 v_position){ return vec4(0.0); }
#endif

#endif // ATMSCAT