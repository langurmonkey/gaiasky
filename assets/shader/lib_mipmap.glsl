#ifndef GLSL_LIB_MIPMAP
#define GLSL_LIB_MIPMAP

// Does not take into account GL_TEXTURE_MIN_LOD/GL_TEXTURE_MAX_LOD/GL_TEXTURE_LOD_BIAS.
float mipmapLevel(in vec2 texelCoord, in float bias) {
    vec2  dxVtc        = dFdx(texelCoord);
    vec2  dyVtc        = dFdy(texelCoord);
    float deltaMaxSqr  = max(dot(dxVtc, dxVtc), dot(dyVtc, dyVtc));
    return             0.5 * log2(deltaMaxSqr) + bias;
}

#endif
