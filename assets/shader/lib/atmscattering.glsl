#if defined(atmosphereGround) || defined(atmosphericScattering)
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

uniform int nSamples;

// 0 in the boundary of space, 1 on the ground
out float v_heightNormalized;
// Fade factor between hieght-driven opacity and luminosity-driven opacity
out float v_fadeFactor;

float scale(float fCos) {
    float x = 1.0 - fCos;
    return fScaleDepth * exp (-0.00287 + x * (0.459 + x * (3.83 + x * (-6.80 + x * 5.25))));
}

float getNearIntersection(vec3 pos, vec3 ray, float distance2, float radius2) {
    float B = 2.0 * dot (pos, ray);
    float C = distance2 - radius2;
    float fDet = max (0.0, B * B - 4.0 * C);
    return 0.5 * (-B - sqrt (fDet));
}

float expScale(float cosine) {
    float x = 1.0 - cosine;
    return fScaleDepth * exp (-0.00287 + x * (0.459 + x * (3.83 + x * (-6.80 + x * 5.25))));
}
#endif // atmosphereGround || atmosphericScattering

#ifdef atmosphereGround

out vec4 v_atmosphereColor;

// Computes the ground atmosphere color and puts it in v_atmosphereColor
void computeAtmosphericScatteringGround() {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;
    // Get the ray from the camera to the vertex and its length (which is the far point of the ray passing through the atmosphere)
    vec3 v3Pos = a_position * fOuterRadius;
    vec3 v3Ray = v3Pos - v3CameraPos;
    float fFar = length(v3Ray);
    v3Ray /= fFar;

    // Calculate the closest intersection of the ray with the outer atmosphere (which is the near point of the ray passing through the atmosphere)
    float fNear = getNearIntersection(v3CameraPos, v3Ray, fCameraHeight2, fOuterRadius2);

    // Calculate the ray's starting position, then calculate its scattering offset
    vec3 v3Start = v3CameraPos + v3Ray * fNear;
    fFar -= fNear;
    float fDepth = exp((fInnerRadius - fOuterRadius) / fScaleDepth);
    float poslen = length(a_position);
    float fCameraAngle = dot(-v3Ray, a_position) / poslen;
    float fLightAngle = dot(v3LightPos, a_position) / poslen;
    float fCameraScale = scale(fCameraAngle);
    float fLightScale = scale(fLightAngle);
    float fCameraOffset = fDepth * fCameraScale;
    float fTemp = (fLightScale + fCameraScale);

    /* Initialize the scattering loop variables*/
    float fSampleLength = fFar / float(nSamples);
    float fScaledLength = fSampleLength * fScale;
    vec3 v3SampleRay = v3Ray * fSampleLength;
    vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;

    // Now loop through the sample rays
    vec3 v3FrontColor = vec3(0.0, 0.0, 0.0);
    vec3 v3Attenuate;
    for (int i = 0; i < nSamples; i++) {
        float fHeight = length (v3SamplePoint);
        float fDepth = exp (fScaleOverScaleDepth * (fInnerRadius - fHeight));
        float fScatter = fDepth * fTemp - fCameraOffset;

        v3Attenuate = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI));

        v3FrontColor += v3Attenuate * (fDepth * fScaledLength);
        v3SamplePoint += v3SampleRay;
    }

    float inner = fInnerRadius + (fOuterRadius - fInnerRadius) * 0.5;
    float heightNormalized = clamp(((fCameraHeight - inner) / (fOuterRadius - inner)), 0.0, 1.0);
    v_fadeFactor = smoothstep(0.5, 1.0, heightNormalized);

    v_atmosphereColor = vec4(v3FrontColor * (v3InvWavelength * fKrESun + fKmESun), fAlpha);
}
#else
// Computes the ground atmosphere color and puts it in v_atmosphereColor
void computeAtmosphericScatteringGround(){}
#endif // atmosphereGround

#ifdef atmosphericScattering
out vec4 v_frontColor;
out vec3 v_frontSecondaryColor;
out vec3 v_direction;

void computeAtmosphericScattering() {
    float fCameraHeight2 = fCameraHeight * fCameraHeight;
    float fOuterRadius2 = fOuterRadius * fOuterRadius;
    /* Get the ray from the camera to the vertex, and its length (which is the far point of the ray passing through the atmosphere)*/
    vec3 v3Pos = a_position * fOuterRadius;
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
        v3Start = v3CameraPos;
        float fHeight = length (v3Start);
        fStartAngle = dot (v3Ray, v3Start) / fHeight;
        fStartDepth = exp (fScaleOverScaleDepth * (fInnerRadius - fCameraHeight));
    } else {
        v3Start = v3CameraPos + v3Ray * fNear;
        fFar -= fNear;
        fStartAngle = dot (v3Ray, v3Start) / fOuterRadius;
        fStartDepth = exp (-1.0 / fScaleDepth);
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
        float fHeight = length (v3SamplePoint);
        float fDepth = exp (fScaleOverScaleDepth * (fInnerRadius - fHeight));
        float fLightAngle = dot (v3LightPos, v3SamplePoint) / fHeight;
        float fCameraAngle = dot (v3Ray, v3SamplePoint) / fHeight;
        float fScatter = (fStartOffset + fDepth * (scale(fLightAngle) - scale(fCameraAngle)));
        vec3 v3Attenuate = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI));

        v3FrontColor += v3Attenuate * (fDepth * fScaledLength);
        v3SamplePoint += v3SampleRay;
    }

    // Finally, scale the Mie and Rayleigh colors and set up the varying variables for the pixel shader
    v_frontColor.rgb = v3FrontColor * (v3InvWavelength * fKrESun);
    v_frontColor.a = fAlpha;
    v_frontSecondaryColor = v3FrontColor * fKmESun;

    // Height normalized to control the opacity
    // Normalized in [1,0], for [ground,space]
    float inner = fInnerRadius + (fOuterRadius - fInnerRadius) * 0.5;
    v_heightNormalized = 1.0 - clamp(((fCameraHeight - inner) / (fOuterRadius - inner)), 0.0, 1.0);
    v_fadeFactor = smoothstep(0.5, 1.0, 1.0 - v_heightNormalized);

    // Direction from the vertex to the camera
    v_direction = v3CameraPos - v3Pos;
}
#else
void computeAtmosphericScattering(){ }
#endif// atmosphericScattering
