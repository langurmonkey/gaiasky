#include shader/lib_logdepthbuff.glsl

attribute vec3 a_position;
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform vec3 v3PlanetPos; /* The position of the planet */
uniform vec3 v3CameraPos; /* The camera's current position*/
uniform vec3 v3LightPos; /* The direction vector to the light source*/
uniform vec3 v3InvWavelength; /* 1 / pow(wavelength, 4) for the red, green, and blue channels*/

uniform float fCameraHeight;
uniform float fCameraHeight2; /* fCameraHeight^2*/
uniform float fOuterRadius; /* The outer (atmosphere) radius*/
uniform float fOuterRadius2; /* fOuterRadius^2*/
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
uniform float fSamples;

varying vec3 v3Direction;
varying vec4 frontColor;
varying vec3 frontSecondaryColor;
varying float v_depth;

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
    uniform float u_vc; // v/c
    uniform vec3 u_velDir; // Camera velocity direction

    #include shader/lib_geometry.glsl
    #include shader/lib_relativity.glsl
#endif // relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
    uniform vec4 u_hterms; // hpluscos, hplussin, htimescos, htimessin
    uniform vec3 u_gw; // Location of gravitational wave, cartesian
    uniform mat3 u_gwmat3; // Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
    uniform float u_ts; // Time in seconds since start
    uniform float u_omgw; // Wave frequency
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

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

void main(void) {

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
    }
    else {
		v3Start = v3CameraPos + v3Ray * fNear;
		fFar -= fNear;
		fStartAngle = dot (v3Ray, v3Start) / fOuterRadius;
		fStartDepth = exp (-1.0 / fScaleDepth);
    }

    float fStartOffset = fStartDepth * scale(fStartAngle);

    /* Initialize the scattering loop variables*/
    float fSampleLength = fFar / fSamples;
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
    frontColor.rgb = v3FrontColor * (v3InvWavelength * fKrESun);
    frontColor.a = fAlpha;
    frontSecondaryColor.rgb = v3FrontColor * fKmESun;

    vec4 g_position = vec4(a_position, 1.0);
    vec4 pos = u_worldTrans * g_position;
    
    #ifdef relativisticEffects
        pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    gl_Position = u_projViewTrans * pos;

    // Logarithmic depth buffer
    v_depth = getDepthValue(length(pos.xyz));

    // Direction from the vertex to the camera 
    v3Direction = v3CameraPos - v3Pos;

}
