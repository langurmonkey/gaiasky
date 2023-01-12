uniform float u_svtTileSize;
uniform float u_svtDepth;
uniform float u_svtId;

// This function converts regular texture coordinates
// to texture coordinates in the SVT buffer texture
// using the indirection texture.
vec2 svtTexCoords(vec2 texCoords) {
    vec4 indirection = texture2D(u_svtIndirectionTexture, texCoords);
    vec2 pageCoord = indirection.rg; // red-green has the UV coordinates in the SVT buffer texture.
    float mipLevel = u_svtDepth - indirection.b * u_svtDepth; // blue channel has level/depth.
    float indirectionSize = pow(2.0, u_svtDepth);
    vec2 withinPageCoord = fract(texCoords * vec2(indirectionSize * 2.0, indirectionSize));
    return (pageCoord + withinPageCoord);
}