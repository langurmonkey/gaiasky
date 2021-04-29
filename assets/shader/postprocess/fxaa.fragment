/**
 * @license
 * Copyright (c) 2011 NVIDIA Corporation. All rights reserved.
 *
 * TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
 * *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
 * OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, NONINFRINGEMENT,IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA
 * OR ITS SUPPLIERS BE  LIABLE  FOR  ANY  DIRECT, SPECIAL,  INCIDENTAL,  INDIRECT,  OR
 * CONSEQUENTIAL DAMAGES WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS
 * OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY
 * OTHER PECUNIARY LOSS) ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE,
 * EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

// Whitepaper describing the technique:
// http://developer.download.nvidia.com/assets/gamedev/files/sdk/11/FXAA_WhitePaper.pdf
#version 330 core

uniform sampler2D u_texture0;

// The inverse of the viewport dimensions along X and Y
uniform vec2 u_viewportInverse;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

// Return the luma, the estimation of luminance from rgb inputs.
// This approximates luma using one FMA instruction,
// skipping normalization and tossing out blue.
// luma() will range 0.0 to 2.963210702.
float luma(vec3 rgb) {
    return rgb.y * (0.587 / 0.299) + rgb.x;
}
vec3 lerp3(vec3 a, vec3 b, float amountOfA) {
    return (vec3(-amountOfA) * b) + ((a * vec3(amountOfA)) + b);
}

/*
FXAA_PRESET - Choose compile-in knob preset 1-5.
------------------------------------------------------------------------------
FXAA_EDGE_THRESHOLD - The minimum amount of local contrast required
                      to apply algorithm.
                      1.0/3.0  - too little
                      1.0/4.0  - good start
                      1.0/8.0  - applies to more edges
                      1.0/16.0 - overkill
------------------------------------------------------------------------------
FXAA_EDGE_THRESHOLD_MIN - Trims the algorithm from processing darks.
                          Perf optimization.
                          1.0/32.0 - visible limit (smaller isn't visible)
                          1.0/16.0 - good compromise
                          1.0/12.0 - upper limit (seeing artifacts)
------------------------------------------------------------------------------
FXAA_SEARCH_STEPS - Maximum number of search steps for end of span.
------------------------------------------------------------------------------
FXAA_SEARCH_THRESHOLD - Controls when to stop searching.
                        1.0/4.0 - seems to be the best quality wise
------------------------------------------------------------------------------
FXAA_SUBPIX_CAP - Insures fine detail is not completely removed.
                  This is important for the transition of sub-pixel detail,
                  like fences and wires.
                  3.0/4.0 - default (medium amount of filtering)
                  7.0/8.0 - high amount of filtering
                  1.0 - no capping of sub-pixel aliasing removal
------------------------------------------------------------------------------
FXAA_SUBPIX_TRIM - Controls sub-pixel aliasing removal.
                   1.0/2.0 - low removal
                   1.0/3.0 - medium removal
                   1.0/4.0 - default removal
                   1.0/8.0 - high removal
                   0.0 - complete removal
*/

#ifndef FXAA_PRESET
    #define FXAA_PRESET 5
#endif
#if (FXAA_PRESET == 1)
    #define FXAA_EDGE_THRESHOLD      (1.0/4.0)
    #define FXAA_EDGE_THRESHOLD_MIN  (1.0/12.0)
    #define FXAA_SEARCH_STEPS        10
    #define FXAA_SEARCH_THRESHOLD    (1.0/4.0)
    #define FXAA_SUBPIX_CAP          (3.0/4.0)
    #define FXAA_SUBPIX_TRIM         (1.0/3.0)
 #endif
#if (FXAA_PRESET == 2)
    #define FXAA_EDGE_THRESHOLD      (1.0/4.0)
    #define FXAA_EDGE_THRESHOLD_MIN  (1.0/12.0)
    #define FXAA_SEARCH_STEPS        12
    #define FXAA_SEARCH_THRESHOLD    (1.0/4.0)
    #define FXAA_SUBPIX_CAP          (3.0/4.0)
    #define FXAA_SUBPIX_TRIM         (1.0/3.0)
#endif
#if (FXAA_PRESET == 3)
    #define FXAA_EDGE_THRESHOLD      (1.0/8.0)
    #define FXAA_EDGE_THRESHOLD_MIN  (1.0/16.0)
    #define FXAA_SEARCH_STEPS        16
    #define FXAA_SEARCH_THRESHOLD    (1.0/4.0)
    #define FXAA_SUBPIX_CAP          (3.0/4.0)
    #define FXAA_SUBPIX_TRIM         (1.0/4.0)
#endif
#if (FXAA_PRESET == 4)
    #define FXAA_EDGE_THRESHOLD      (1.0/8.0)
    #define FXAA_EDGE_THRESHOLD_MIN  (1.0/24.0)
    #define FXAA_SEARCH_STEPS        24
    #define FXAA_SEARCH_THRESHOLD    (1.0/4.0)
    #define FXAA_SUBPIX_CAP          (3.0/4.0)
    #define FXAA_SUBPIX_TRIM         (1.0/4.0)
#endif
#if (FXAA_PRESET == 5)
    #define FXAA_EDGE_THRESHOLD      (1.0/8.0)
    #define FXAA_EDGE_THRESHOLD_MIN  (1.0/24.0)
    #define FXAA_SEARCH_STEPS        32
    #define FXAA_SEARCH_THRESHOLD    (1.0/4.0)
    #define FXAA_SUBPIX_CAP          (4.0/5.0)
    #define FXAA_SUBPIX_TRIM         (1.0/4.0)
#endif

#define FXAA_SUBPIX_TRIM_SCALE (1.0/(1.0 - FXAA_SUBPIX_TRIM))


vec4 fxaa(sampler2D tex, vec2 texCoords, vec2 viewportInv) {
    // LOCAL CONTRAST CHECK
    vec3 rgbN = textureOffset(tex, texCoords.xy, ivec2(0, -1)).xyz;
    vec3 rgbW = textureOffset(tex, texCoords.xy, ivec2(-1, 0)).xyz;
    vec3 rgbM = texture(tex, texCoords.xy).xyz;
    vec3 rgbE = textureOffset(tex, texCoords.xy, ivec2(1, 0)).xyz;
    vec3 rgbS = textureOffset(tex, texCoords.xy, ivec2(0, 1)).xyz;

    float lumaN = luma(rgbN);
    float lumaW = luma(rgbW);
    float lumaM = luma(rgbM);
    float lumaE = luma(rgbE);
    float lumaS = luma(rgbS);
    float rangeMin = min(lumaM, min(min(lumaN, lumaW), min(lumaS, lumaE)));
    float rangeMax = max(lumaM, max(max(lumaN, lumaW), max(lumaS, lumaE)));

    float range = rangeMax - rangeMin;
    if (range < max(FXAA_EDGE_THRESHOLD_MIN, rangeMax * FXAA_EDGE_THRESHOLD)){
        return vec4(rgbM, 1.0);
    }

    float lumaL = (lumaN + lumaW + lumaE + lumaS) * 0.25;
    float rangeL = abs(lumaL -lumaM);
    float blendL = max(0.0, (rangeL / range) - FXAA_SUBPIX_TRIM) * FXAA_SUBPIX_TRIM_SCALE;
    blendL = min(FXAA_SUBPIX_CAP, blendL);

    // SUB-PIXEL ALIASING TEST
    vec3 rgbL = rgbN + rgbW + rgbM + rgbE + rgbS;
    vec3 rgbNW = textureOffset(tex, texCoords.xy, ivec2(-1, -1)).xyz;
    vec3 rgbNE = textureOffset(tex, texCoords.xy, ivec2(1, -1)).xyz;
    vec3 rgbSW = textureOffset(tex, texCoords.xy, ivec2(-1, 1)).xyz;
    vec3 rgbSE = textureOffset(tex, texCoords.xy, ivec2(1, 1)).xyz;
    rgbL += (rgbNW + rgbNE + rgbSW + rgbSE);
    rgbL *= vec3(1.0 / 9.0);

    float lumaNW = luma(rgbNW);
    float lumaNE = luma(rgbNE);
    float lumaSW = luma(rgbSW);
    float lumaSE = luma(rgbSE);

    // VERTICAL/HORIZONTAL EDGE TEST
    float edgeVert =
        abs((0.25 * lumaNW) + (-0.5 * lumaN) + (0.25 * lumaNE)) +
        abs((0.50 * lumaW) + (-1.0 * lumaM) + (0.50 * lumaE)) +
        abs((0.25 * lumaSW) + (-0.5 * lumaS) + (0.25 * lumaSE));
    float edgeHorz =
        abs((0.25 * lumaNW) + (-0.5 * lumaW) + (0.25 * lumaSW)) +
        abs((0.50 * lumaN) + (-1.0 * lumaM) + (0.50 * lumaS)) +
        abs((0.25 * lumaNE) + (-0.5 * lumaE) + (0.25 * lumaSE));

    bool horzSpan = edgeHorz >= edgeVert;
    float lengthSign = horzSpan ? -viewportInv.y : -viewportInv.x;

    if(!horzSpan) {
        lumaN = lumaW;
        lumaS = lumaE;
    }
    float gradientN = abs(lumaN - lumaM);
    float gradientS = abs(lumaS - lumaM);
    lumaN = (lumaN + lumaM) * 0.5;
    lumaS = (lumaS + lumaM) * 0.5;

    if (gradientN < gradientS) {
        lumaN = lumaS;
        lumaN = lumaS;
        gradientN = gradientS;
        lengthSign *= -1.0;
    }

    vec2 posN;
    posN.x = texCoords.x + (horzSpan ? 0.0 : lengthSign * 0.5);
    posN.y = texCoords.y + (horzSpan ? lengthSign * 0.5 : 0.0);

    gradientN *= FXAA_SEARCH_THRESHOLD;

    // END-OF-EDGE SEARCH
    vec2 posP = posN;
    vec2 offNP = horzSpan ? vec2(viewportInv.x, 0.0) : vec2(0.0, viewportInv.y);
    float lumaEndN = lumaN;
    float lumaEndP = lumaN;
    bool doneN = false;
    bool doneP = false;
    posN += offNP * vec2(-1.0, -1.0);
    posP += offNP * vec2( 1.0,  1.0);

    for(int i = 0; i < FXAA_SEARCH_STEPS; i++) {
        if(!doneN) {
            lumaEndN = luma(texture(tex, posN.xy).xyz);
        }
        if(!doneP) {
            lumaEndP = luma(texture(tex, posP.xy).xyz);
        }

        doneN = doneN || (abs(lumaEndN - lumaN) >= gradientN);
        doneP = doneP || (abs(lumaEndP - lumaN) >= gradientN);

        if(doneN && doneP) {
            break;
        }
        if(!doneN) {
            posN -= offNP;
        }
        if(!doneP) {
            posP += offNP;
        }
    }
    float dstN = horzSpan ? texCoords.x - posN.x : texCoords.y - posN.y;
    float dstP = horzSpan ? posP.x - texCoords.x : posP.y - texCoords.y;
    bool directionN = dstN < dstP;
    lumaEndN = directionN ? lumaEndN : lumaEndP;

    if(((lumaM - lumaN) < 0.0) == ((lumaEndN - lumaN) < 0.0))
    {
        lengthSign = 0.0;
    }

    float spanLength = (dstP + dstN);
    dstN = directionN ? dstN : dstP;
    float subPixelOffset = (0.5 + (dstN * (-1.0/spanLength))) * lengthSign;
    vec3 rgbF = texture(tex, vec2(
        texCoords.x + (horzSpan ? 0.0 : subPixelOffset),
        texCoords.y + (horzSpan ? subPixelOffset : 0.0))).xyz;

    return vec4(lerp3(rgbL, rgbF, blendL), 1.0);
}

void main() {
    fragColor = fxaa(u_texture0, v_texCoords, u_viewportInverse);
}
