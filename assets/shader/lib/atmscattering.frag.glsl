#ifndef GLSL_LIB_ATMSCAT
#define GLSL_LIB_ATMSCAT

#if defined(atmosphereGround) || defined(atmosphericScattering)
#include <shader/lib/luma.glsl>

#define exposure 2.0           /* Tone-mapping exposure for the atmosphere */

uniform vec3 v3PlanetPos; /* Planet position relative to camera origin */
uniform vec3 v3CameraPos; /* Camera position in normalized coordinates (inner radius = 1) */
uniform vec3 v3LightPos; /* Unit vector towards the sun */
uniform vec3 v3InvWavelength; /* 1 / pow(wavelength, 4) for Rayleigh scattering */

uniform float fCameraHeight; /* Camera distance from planet center in normalized units */
uniform float fOuterRadius; /* Outer atmosphere boundary (inner radius + atmosphere height) */
uniform float fInnerRadius; /* Planet surface radius (always 1.0 in normalized coords) */
uniform float fKrESun; /* Rayleigh scattering coefficient × solar intensity */
uniform float fKmESun; /* Mie scattering coefficient × solar intensity */
uniform float fKr4PI; /* Rayleigh extinction coefficient × 4π */
uniform float fKm4PI; /* Mie extinction coefficient × 4π */
uniform float fScale; /* 1.0 / atmosphere height (in normalized units) */
uniform float fScaleDepth; /* Atmospheric scale depth (normalized, controls density falloff) */
uniform float fScaleOverScaleDepth; /* fScale / fScaleDepth — combined density gradient factor */
uniform float fAlpha; /* Overall opacity multiplier */
uniform float fG; /* Mie phase asymmetry factor */
uniform vec3 v3O3InvWavelength; /* Ozone absorption coefficients per RGB channel */
uniform float fO3PeakHeight; /* Ozone layer peak altitude (normalized, height above surface) */
uniform float fO3Width; /* Ozone layer Gaussian width (normalized) */

uniform int nSamples;

#define EPSILON 1e-8

float rayleighPhase(float fCos2) {
    return 0.75 + 0.75 * fCos2;
}

float miePhase(float fCos, float fCos2) {
    float g2 = fG * fG;
    return 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow(max(1.0 + g2 - 2.0 * fG * fCos, EPSILON), 1.5);
}

// Bruneton's polynomial approximation of the Chapman function.
// Computes the relative atmospheric density along a ray at angle fCos
// from the vertical, using a precomputed fit to the exact integral.
float scale(float fCos) {
    float x = 1.0 - fCos;
    return fScaleDepth * exp(-0.00287 + x * (0.459 + x * (3.83 + x * (-6.80 + x * 5.25))));
}

// Finds the nearest intersection of a ray with a sphere centered at the origin.
// Used to clip the ray to the outer atmosphere boundary.
float getNearIntersection(vec3 pos, vec3 ray, float distance2, float radius2) {
    float B = 2.0 * dot(pos, ray);
    float C = distance2 - radius2;
    float fDet = max(0.0, B * B - 4.0 * C);
    return 0.5 * (-B - sqrt(fDet));
}

// UNIFIED ATMOSPHERE INTEGRATOR
// Shared by both ground and sky shaders. Marches along a ray from
// v3Start to v3Start + v3Ray * fFar, accumulating inscattered light
// and computing the final transmittance at the ray endpoint.
vec3 integrateAtmosphere(vec3 v3Start, vec3 v3Ray, float fFar, out vec3 v3OutAttenuate) {
    float fSampleLength = fFar / float(nSamples);
    float fScaledLength = fSampleLength * fScale;
    vec3 v3SampleRay = v3Ray * fSampleLength;
    vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;

    vec3 v3FrontColor = vec3(0.0);

    // Cumulative transmittance from camera to current sample (starts fully clear)
    vec3 v3CurrentAttenuation = vec3(1.0);
    float fCameraOpticalDepth = 0.0;
    float fO3CameraDepth = 0.0;

    for (int i = 0; i < nSamples; i++) {
        float fHeight = max(length(v3SamplePoint), EPSILON);
        float fHeightDiff = fInnerRadius - fHeight;
        // Density at this sample height (clamped to avoid positive exponents below surface)
        float fDepth = exp(fScaleOverScaleDepth * min(fHeightDiff, 0.0));

        // Optical depth from this sample towards the light source (sun)
        float fLightAngle = dot(v3LightPos, v3SamplePoint) / fHeight;
        float fLightOpticalDepth = fDepth * scale(fLightAngle);

        // Accumulate camera-to-sample optical depth
        fCameraOpticalDepth += fDepth * fScaledLength;
        float fScatter = fLightOpticalDepth + fCameraOpticalDepth;

        // Ozone (O3) absorption — Gaussian density profile centered in the stratosphere
        float fO3Height = fHeight - fInnerRadius;
        float fO3Density = exp(-((fO3Height - fO3PeakHeight) * (fO3Height - fO3PeakHeight)) / (fO3Width * fO3Width));
        float fO3Extinction = fO3Density * fScaledLength;
        float fO3LightDepth = fO3Extinction * scale(fLightAngle);
        fO3CameraDepth += fO3Extinction;
        float fO3Scatter = fO3LightDepth + fO3CameraDepth;

        // Combined attenuation: Rayleigh + Mie scattering + Ozone absorption
        v3CurrentAttenuation = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI)
                                - (fO3Scatter * v3O3InvWavelength));

        // Accumulate inscattered light along the ray
        v3FrontColor += v3CurrentAttenuation * (fDepth * fScaledLength);
        v3SamplePoint += v3SampleRay;
    }

    v3OutAttenuate = v3CurrentAttenuation;
    return v3FrontColor;
}
#endif // atmosphereGround || atmosphericScattering

// GROUND SHADER
// Computes the atmospheric glow and transmittance for a point on the
// planet's surface. The ray goes from the camera to the surface point.
#ifdef atmosphereGround
void computeAtmosphericScatteringGround(vec3 v_position, out vec3 outGlow, out vec3 outTransmittance) {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;

    // Surface point in normalized coordinates
    vec3 v3Pos = normalize(v_position) * fInnerRadius;
    // Ray from camera to surface point
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length(v3Ray);
    v3Ray /= fFar;

    // Determine ray start: if inside the atmosphere, start at the camera;
    // otherwise, start at the outer atmosphere boundary.
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

    // Phase functions for the viewing angle
    float fCos = clamp(dot(v3LightPos, v3Ray), -0.9999, 0.9999);
    float fCos2 = fCos * fCos;

    vec3 rayleighColor = rayleighPhase(fCos2) * v3FrontColor * (v3InvWavelength * fKrESun);
    vec3 mieColor = miePhase(fCos, fCos2) * v3FrontColor * fKmESun;

    // Tone-mapped glow and transmittance at the surface point
    outGlow = (vec3(1.0) - exp((rayleighColor + mieColor) * -exposure)) * fAlpha;
    outTransmittance = v3Attenuate;
}
#else
void computeAtmosphericScatteringGround(vec3 v_position, out vec3 outGlow, out vec3 outTransmittance) {
    outGlow = vec3(0.0);
    outTransmittance = vec3(1.0);
}
#endif

// SKY SHADER
// Computes the sky dome color for a given view direction. The ray
// goes from the camera to the outer atmosphere boundary. Uses the
// same unified integrator as the ground shader.
#ifdef atmosphericScattering
vec4 computeAtmosphericScattering(vec3 v_position) {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;

    // Direction from planet center to the sky dome vertex
    vec3 v3VisualRay = normalize(v_position);

    // Sky dome vertex on the outer atmosphere shell
    vec3 v3Pos = v3VisualRay * fOuterRadius;
    // Ray from camera to sky dome vertex
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length(v3Ray);
    v3Ray /= fFar;

    // Determine ray start: if inside the atmosphere, start at the camera;
    // otherwise, start at the outer atmosphere boundary.
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

    // Phase functions for the viewing angle
    float fCos = clamp(dot(v3LightPos, v3VisualRay), -0.9999, 0.9999);
    float fCos2 = fCos * fCos;

    vec3 rayleighColor = rayleighPhase(fCos2) * v3FrontColor * (v3InvWavelength * fKrESun);
    vec3 mieColor = miePhase(fCos, fCos2) * v3FrontColor * fKmESun;

    vec4 tonedAtmosphere = vec4(vec3(1.0) - exp((rayleighColor + mieColor) * -exposure), 1.0);

    // Alpha fade: inner boundary at 10% of atmosphere height
    float inner = fInnerRadius + (fOuterRadius - fInnerRadius) * 0.1;
    // Normalized camera height: 1 at inner boundary, 0 in space
    float heightNormalized = 1.0 - clamp(((fCameraHeight - inner) / (fOuterRadius - inner)), 0.0, 1.0);
    float fadeFactor = smoothstep(0.1, 1.0, 1.0 - heightNormalized);
    float lma = luma(tonedAtmosphere.rgb);
    float scl = smoothstep(0.2, 0.5, lma);

    tonedAtmosphere.a = (heightNormalized * (1.0 - fadeFactor) + lma * fadeFactor) * scl * fAlpha;

    return tonedAtmosphere;
}
#else
vec4 computeAtmosphericScattering(vec3 v_position) {
    return vec4(0.0);
}
#endif

#endif // ATMSCAT
