// Including this file requires lib_luma.glsl

struct Normal {
    float t, b, r, l, tl, bl, br, tr;
};

vec2 regular(Normal n){
    float sum0 = n.tr + n.t + n.br;
    float sum1 = n.tl + n.b + n.bl;
    float sum2 = n.tl + n.l + n.tr;
    float sum3 = n.bl + n.r + n.br;
    float dx = (sum1 - sum0);
    float dy = (sum2 - sum3);

    return vec2(dx, dy);
}

vec2 sobel(Normal n){
    float dx = n.tl + n.l * 2.0 + n.bl - n.tr - n.r * 2.0 - n.br;
    float dy = n.tl + n.t * 2.0 + n.tr - n.bl - n.b * 2.0 - n.br;
    return vec2(dx, dy);
}

vec2 scharr(Normal n){
    float dx = n.tl * 3.0 + n.l * 10.0 + n.bl * 3.0 - n.tr * 3.0 - n.r * 10.0 - n.br * 3.0;
    float dy = n.tl * 3.0 + n.t * 10.0 + n.tr * 3.0 - n.bl * 3.0 - n.b * 10.0 - n.br * 3.0;
    return vec2(dx, dy);
}

Normal partialNormalMap(sampler2D tex, vec2 texCoords) {
    // Offset coordinates
    const ivec3 off = ivec3(-1, 0, 1);

    float t = luma(textureOffset(tex, texCoords.xy, off.yz).rgb);
    float b = luma(textureOffset(tex, texCoords.xy, off.yx).rgb);
    float r = luma(textureOffset(tex, texCoords.xy, off.zy).rgb);
    float l = luma(textureOffset(tex, texCoords.xy, off.xy).rgb);
    float tl = luma(textureOffset(tex, texCoords.xy, off.xz).rgb);
    float bl = luma(textureOffset(tex, texCoords.xy, off.xx).rgb);
    float br = luma(textureOffset(tex, texCoords.xy, off.zz).rgb);
    float tr = luma(textureOffset(tex, texCoords.xy, off.zx).rgb);

    return Normal(t, b, r, l, tl, bl, br, tr);
}

vec2 normalMap(sampler2D tex, vec2 texCoords) {
    return regular(partialNormalMap(tex, texCoords));
}

