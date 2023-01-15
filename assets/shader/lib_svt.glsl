#ifdef svtFlag

uniform vec2 u_svtResolution;
uniform float u_svtTileSize;
uniform float u_svtDepth;
uniform float u_svtId;

const float mipBias = 0.0;

// Does not take into account GL_TEXTURE_MIN_LOD/GL_TEXTURE_MAX_LOD/GL_TEXTURE_LOD_BIAS.
/*
Computes the mipmap level with the given textel coordinate and bias, similar to
what textureQueryLod() does.
*/
float mipmapLevel(in vec2 texelCoord, in float bias) {
    vec2  dxVtc        = dFdx(texelCoord);
    vec2  dyVtc        = dFdy(texelCoord);
    float deltaMaxSqr  = max(dot(dxVtc, dxVtc), dot(dyVtc, dyVtc));
    return             0.5 * log2(deltaMaxSqr) + bias;
}

/*
This function queries the indirection buffer with the given texture coordinates
and bias. If the tile is invalid, it sequentially loops through the upper levels
until a valid tile is found. The root level is always guaranteed to be found.
*/
vec4 queryIndirectionBuffer(vec2 texCoords, float bias) {
    float lod = mipmapLevel(texCoords * u_svtResolution, bias);
    lod = floor(lod + bias);
    vec4 indirectionEntry = textureLod(u_svtIndirectionTexture, texCoords, lod);
    while (indirectionEntry.a != 1.0 && lod <= u_svtDepth) {
        // Go one level up in the mipmap sequence.
        lod = lod + 1.0;
        // Query again.
        indirectionEntry = textureLod(u_svtIndirectionTexture, texCoords, lod);
    }
    return indirectionEntry;
}

/*
This function converts regular texture coordinates
to texture coordinates in the SVT buffer texture
using the indirection texture.
*/
vec2 svtTexCoords(vec2 texCoords) {
    float mode = 1.0;

    // Size of the buffer texture, in tiles.
    float cacheSizeInTiles = textureSize(u_svtCacheTexture, 0).x / u_svtTileSize;

    if (mode == 1.0) {
        // DECENT BOOK-ACCURATE IMPLEMENTATION
        float bias = log2(u_svtTileSize) - 0.5 + mipBias;
        vec4 indirectionEntry = queryIndirectionBuffer(texCoords, 0.0) * 255.0;
        //vec4 indirectionEntry = textureLod(u_svtIndirectionTexture, texCoords, bias) * 255.0;
        vec2 pageCoord = indirectionEntry.rg; // red-green has the XY coordinates of the tile in the cache texture.
        float reverseMipmapLevel = indirectionEntry.b;// blue channel has the reverse mipmap-level.
        float mipExp = exp2(reverseMipmapLevel);
        // Need to account for the aspect ratio of our virtual texture (2:1).
        vec2 withinPageCoord = fract(texCoords * mipExp * vec2(2.0, 1.0));
        // The next line prevents bilinear filtering artifacts due to unrelated tiles being side-by-side in the cache.
        // For each tile, we sample an area (tile_resolution - 2)^2, leaving a 1px border which should be filled with
        // data from the adjacent tiles. However, in high resolution tiles (512, 1024, etc.), this 1 pixel is not
        // noticable at all if the border is not set up in the tiles.
        withinPageCoord = ((withinPageCoord * (u_svtTileSize - 2.0)) / u_svtTileSize) + (1.0 / u_svtTileSize);
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
#end // svtFlag
