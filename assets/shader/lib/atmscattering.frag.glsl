#ifndef GLSL_LIB_ATMSCAT
#define GLSL_LIB_ATMSCAT

#if defined(atmosphereGround) || defined(atmosphericScattering)
#define exposureGround 0.5
#define exposureSky 0.25
uniform vec3 v3PlanetPos; /* The position of the planet */
uniform vec3 v3CameraPos; /* The camera's current position*/
uniform vec3 v3LightPos; /* The direction vector to the light source*/
uniform vec3 v3InvWavelength; /* 1 / pow(wavelength, 4) for the red, green, and blue channels*/

uniform float fCameraHeight;
uniform float fOuterRadius; /* The outer (atmosphere) radius*/
uniform float fInnerRadius; /* The inner (planetary) radius*/
uniform float fKrESun; /* Kr * ESun*/
uniform float fKmESun; /* Km * ESun*/
uniform float fKr4PI; /* Kr * 4 * PI*/
uniform float fKm4PI; /* Km * 4 * PI*/
uniform float fScale; /* 1 / (fOuterRadius - fInnerRadius)*/
uniform float fScaleDepth; /* The scale depth (i.e. the altitude at which the atmosphere's average density is found)*/
uniform float fScaleOverScaleDepth; /* fScale / fScaleDepth*/
uniform float fAlpha; /* Atmosphere effect opacity */
uniform float fG; /* Mie asymmetry factor */

uniform int nSamples;

// INPUTS
float rayleighPhase(float fCos2) {
    // Calculate the angle between view direction and light direction
    // This gives us the phase function for reddening at sunset
    // Rayleigh phase function: 3/16π * (1 + cos²θ)
    return 0.75 + 0.75 * fCos2;
}

float miePhase(float fCos, float fCos2) {
    // Mie phase function (Henyey-Greenstein)
    float g2 = fG * fG;
    return 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow (1.0 + g2 - 2.0 * fG * fCos, 1.5);
}
float scale(float fCos) {
    float x = 1.0 - fCos;
    return fScaleDepth * exp (-0.00287 + x * (0.459 + x * (3.83 + x * (-6.80 + x * 5.25))));
}
// Returns the far intersection point of a line and a sphere
float getNearIntersection(vec3 pos, vec3 ray, float distance2, float radius2) {
    float B = 2.0 * dot (pos, ray);
    float C = distance2 - radius2;
    float fDet = max (0.0, B * B - 4.0 * C);
    return 0.5 * (-B - sqrt(fDet));
}
// Returns the far intersection point of a line and a sphere
float getFarIntersection(vec3 pos, vec3 ray, float distance2, float radius2) {
    float B = 2.0 * dot(pos, ray);
    float C = distance2 - radius2;
    float fDet = max(0.0, B*B - 4.0 * C);
    return 0.5 * (-B + sqrt(fDet));
}

#endif// atmosphereGround || atmosphericScattering

//
// GROUND SHADER
//
#ifdef atmosphereGround
vec3 computeAtmosphericScatteringGround(vec3 v_position) {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;

    vec3 v3Pos = v_position * fInnerRadius;
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length(v3Ray);
    v3Ray /= fFar;

    // Calculate starting position
    float fNear = getNearIntersection(v3CameraPos, v3Ray, fCameraHeight2, fOuterRadius2);
    vec3 v3Start = v3CameraPos + v3Ray * fNear;
    fFar -= fNear;
    float fStartDepth = exp((fInnerRadius - fOuterRadius) / fScaleDepth);
    float fCameraAngle = dot(-v3Ray, v_position) / length(v_position);
    float fLightAngle = dot(v3LightPos, v_position) / length(v_position);
    float fCameraScale = scale(fCameraAngle);
    float fLightScale = scale(fLightAngle);
    float fCameraOffset = fStartDepth * fCameraScale;
    float fTemp = (fLightScale + fCameraScale);

    // Initialize scattering loop vairables
    float fSampleLength = fFar / float(nSamples);
    float fScaledLength = fSampleLength * fScale;
    vec3 v3SampleRay = v3Ray * fSampleLength;
    vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;

    // Loop through the rays
    vec3 v3FrontColor = vec3(0.0);
    vec3 v3Attenuate;
    for (int i = 0; i < nSamples; i++) {
        float fHeight = length(v3SamplePoint);
        float fDepth = exp(fScaleOverScaleDepth * (fInnerRadius - fHeight));
        float fScatter = fDepth * fTemp - fCameraOffset;

        v3Attenuate = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI));
        v3FrontColor += v3Attenuate * (fDepth * fScaledLength);
        v3SamplePoint += v3SampleRay;
    }

    float inner = fInnerRadius + (fOuterRadius - fInnerRadius) * 0.5;
    float heightNormalized = clamp(((fCameraHeight - inner) / (fOuterRadius - inner)), 0.0, 1.0);
    float fadeFactor = smoothstep(0.5, 1.0, heightNormalized);

    vec3 direction = v3CameraPos - v3Pos;
    float fCos = dot(-v3LightPos, normalize(direction));
    float fCos2 = fCos * fCos;

    // Rayleigh phase
    float fRayleighPhase = rayleighPhase(fCos2);
    vec3 rayleighColor = fRayleighPhase * v3FrontColor * (v3InvWavelength * fKrESun);

    // Mie phase
    float fMiePhase = miePhase(fCos, fCos2);
    vec3 mieColor = fMiePhase * v3FrontColor * fKmESun;

    // Tone mapping
    vec3 tonedAtmosphere = vec3(1.0) - exp((rayleighColor + mieColor) * -exposureGround);

    return tonedAtmosphere * fAlpha * fadeFactor;
}
#else
vec3 computeAtmosphericScatteringGround(vec3 v_position){
    return vec3(0.0);
}
#endif// atmosphereGround

//
// SKY SHADER
//
#ifdef atmosphericScattering
vec4 computeAtmosphericScattering(vec3 v_position) {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;
    /* Get the ray from the camera to the vertex, and its length (which is the far point of the ray passing through the atmosphere)*/
    vec3 v3Pos = v_position * fOuterRadius;
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length (v3Ray);
    v3Ray /= fFar;

    // Calculate the closest intersection of the ray with the outer atmosphere (which is the near point of the ray passing through the atmosphere)
    float fNear = getNearIntersection (v3CameraPos, v3Ray, fCameraHeight2, fOuterRadius2);

    // Calculate the ray's starting position, then calculate its scattering offset
    vec3 v3Start;
    float fStartAngle;
    float fStartDepth;

    if (fCameraHeight < fOuterRadius) {
        // Inside atmosphere
        v3Start = v3CameraPos;
        float fHeight = length (v3Start);
        fStartAngle = dot (v3Ray, v3Start) / fHeight;
        fStartDepth = exp(fScaleOverScaleDepth * (fInnerRadius - fCameraHeight));
    } else {
        // Outside atmosphere
        v3Start = v3CameraPos + v3Ray * fNear;
        fFar -= fNear;
        fStartAngle = dot (v3Ray, v3Start) / fOuterRadius;
        fStartDepth = exp(-1.0 / fScaleDepth);
    }

    float fStartOffset = fStartDepth * scale(fStartAngle);

    /* Initialize the scattering loop variables*/
    float fSampleLength = fFar / float(nSamples);
    float fScaledLength = fSampleLength * fScale;
    vec3 v3SampleRay = v3Ray * fSampleLength;
    vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;

    // Now loop through the sample rays
    vec3 v3FrontColor = vec3 (0.0);
    for (int i = 0; i < nSamples; i++) {
        float fHeight = length(v3SamplePoint);
        float fDepth = exp(fScaleOverScaleDepth * (fInnerRadius - fHeight));
        float fLightAngle = dot(v3LightPos, v3SamplePoint) / fHeight;
        float fCameraAngle = dot(v3Ray, v3SamplePoint) / fHeight;
        float fScatter = (fStartOffset + fDepth * (scale(fLightAngle) - scale(fCameraAngle)));
        vec3 v3Attenuate = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI));

        v3FrontColor += v3Attenuate * (fDepth * fScaledLength);
        v3SamplePoint += v3SampleRay;
    }
    // Height normalized to control the opacity
    // Normalized in [1,0], for [ground,space]
    float inner = fInnerRadius + (fOuterRadius - fInnerRadius) * 0.5;
    float heightNormalized = 1.0 - clamp(((fCameraHeight - inner) / (fOuterRadius - inner)), 0.0, 1.0);
    float fadeFactor = smoothstep(0.5, 1.0, 1.0 - heightNormalized);

    // Rayleigh and Mie phases
    // Direction from the vertex to the camera
    vec3 direction = v3CameraPos - v3Pos;
    float fCos = dot(-v3LightPos, normalize(direction));
    float fCos2 = fCos * fCos;

    // Rayleigh phase
    float fRayleighPhase = rayleighPhase(fCos2);
    vec3 rayleighColor = fRayleighPhase * v3FrontColor * (v3InvWavelength * fKrESun);

    // Mie phase
    float fMiePhase = miePhase(fCos, fCos2);
    vec3 mieColor = fMiePhase * v3FrontColor * fKmESun;

    // Tone mapping
    vec4 tonedAtmosphere;
    tonedAtmosphere.rgb = vec3(1.0) - exp((rayleighColor + mieColor) * -exposureSky);

    float lma = luma(tonedAtmosphere.rbg);
    float scl = smoothstep(0.05, 0.2, lma);
    tonedAtmosphere.a = (heightNormalized * (1.0 - fadeFactor) + lma * fadeFactor) * scl * fAlpha;
    return tonedAtmosphere;
}
#else
vec4 computeAtmosphericScattering(vec3 v_position){
    return vec4(0.0);
}
#endif// atmosphericScattering

#endif// ATMSCAT
