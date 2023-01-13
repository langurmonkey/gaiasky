uniform float u_svtTileSize;
uniform float u_svtDepth;
uniform float u_svtId;

const float mipBias = -1.0;

// This function converts regular texture coordinates
// to texture coordinates in the SVT buffer texture
// using the indirection texture.
vec2 svtTexCoords(vec2 texCoords) {
    float mode = 1.0;

    // Size of the buffer texture, in tiles.
    float cacheSizeInTiles = textureSize(u_svtCacheTexture, 0).x / u_svtTileSize;

    if (mode == 1.0) {
        // DECENT BOOK-ACCURATE IMPLEMENTATION
        float bias = log2(u_svtTileSize) - 0.5 + mipBias;
        vec4 indirectionEntry = texture2D(u_svtIndirectionTexture, texCoords, bias) * 255.0;
        vec2 pageCoord = indirectionEntry.rg; // red-green has the XY coordinates of the tile in the cache texture.
        float reverseMipmapLevel = indirectionEntry.b;// blue channel has the reverse mipmap-level.
        float mipExp = exp2(reverseMipmapLevel);
        // Need to account for the aspect ratio of our virtual texture (2:1).
        vec2 withinPageCoord = fract(texCoords * mipExp * vec2(2.0, 1.0));
        return ((pageCoord + withinPageCoord) / cacheSizeInTiles);
    } else {
        // MY OWN IMPLEMENTATION - BAD
        vec4 indirectionEntry = texture2D(u_svtIndirectionTexture, texCoords);
        vec2 pageCoord = indirectionEntry.rg; // red-green has the UV coordinates of the tile in the cache texture.
        float level = indirectionEntry.b * u_svtDepth;// blue channel has reverse-mipmap-level over depth.
        vec2 indirectionSize = vec2(pow(2.0, level + 1.0), pow(2.0, level));
        // The multiplication by indirectionSize ensures only the relevant part of texCoords is used.
        // The division by bufferSizeInTiles ensures that we stay within the tile.
        vec2 withinPageCoord = fract(texCoords * indirectionSize) / cacheSizeInTiles;
        return (pageCoord + withinPageCoord);
    }
}