
float luma(vec3 v) {
    return dot(v, vec3(0.212, 0.716, 0.072));
}

vec2 normalMap(sampler2D tex, vec2 texCoords){
    // Offset coordinates
    const ivec3 off = ivec3(-1, 0, 1);

    float topHeight = luma(textureOffset(tex, texCoords.xy, off.yz).rgb);
    float bottomHeight = luma(textureOffset(tex, texCoords.xy, off.yx).rgb);
    float rightHeight = luma(textureOffset(tex, texCoords.xy, off.zy).rgb);
    float leftHeight = luma(textureOffset(tex, texCoords.xy, off.xy).rgb);
    float leftTopHeight = luma(textureOffset(tex, texCoords.xy, off.xz).rgb);
    float leftBottomHeight = luma(textureOffset(tex, texCoords.xy, off.xx).rgb);
    float rightBottomHeight = luma(textureOffset(tex, texCoords.xy, off.zz).rgb);
    float rightTopHeight = luma(textureOffset(tex, texCoords.xy, off.zx).rgb);

    // Normal map creation
    float sum0 = rightTopHeight + topHeight + rightBottomHeight;
    float sum1 = leftTopHeight + bottomHeight + leftBottomHeight;
    float sum2 = leftTopHeight + leftHeight + rightTopHeight;
    float sum3 = leftBottomHeight + rightHeight + rightBottomHeight;
    float vect1 = (sum1 - sum0);
    float vect2 = (sum2 - sum3);

    // Put them together and scale.
    return vec2(vect1, vect2);
}