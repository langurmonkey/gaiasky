uniform float u_svtTileSize;
uniform float u_svtDepth;
uniform float u_svtId;

// This function converts regular texture coordinates
// to texture coordinates in the SVT buffer texture
// using the indirection texture.
vec2 svtTexCoords(vec2 texCoords) {
    vec4 indirection = texture2D(u_svtIndirectionTexture, texCoords);
    vec2 pageCoord = indirection.rg; // red-green has the UV coordinates in the SVT buffer texture.
    float level = indirection.b * u_svtDepth; // blue channel has level/depth.
    vec2 indirectionSize = vec2(pow(2.0, level + 1.0), pow(2.0, level));
    // Size of the buffer texture, in tiles.
    float bufferSizeInTiles = textureSize(u_svtBufferTexture, 0).x / u_svtTileSize;
    // The multiplication by indirectionSize ensures only the relevant part of texCoords is used.
    // The division by bufferSizeInTiles ensures that we stay within the tile.
    vec2 withinPageCoord = fract(texCoords * indirectionSize) / bufferSizeInTiles;
    return (pageCoord + withinPageCoord);
}