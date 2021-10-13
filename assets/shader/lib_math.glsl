float lint(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return y0 + (y1 - y0) * smoothstep(x0, x1, x);
}
float lint2(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return mix(y0, y1, (x - x0) / (x1 - x0));
}
float lint3(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return y0 + (y1 - y0) * (x - x0) / (x1 - x0);
}
float mod(float x, float y) {
    return x - y * floor(x/y);
}

vec4 set_from_axes(float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz){
    float t = xx + yy + zz;

    float x, y, z, w;
    // we protect the division by s by ensuring that s>=1
    if (t >= 0) { // |w| >= .5
        float s = sqrt(t + 1.0); // |s|>=1 ...
        w = 0.5f * s;
        s = 0.5f / s; // so this division isn't bad
        x = (zy - yz) * s;
        y = (xz - zx) * s;
        z = (yx - xy) * s;
    } else if ((xx > yy) && (xx > zz)) {
        float s = sqrt(1.0 + xx - yy - zz); // |s|>=1
        x = s * 0.5f; // |x| >= .5
        s = 0.5f / s;
        y = (yx + xy) * s;
        z = (xz + zx) * s;
        w = (zy - yz) * s;
    } else if (yy > zz) {
        float s = sqrt(1.0 + yy - xx - zz); // |s|>=1
        y = s * 0.5f; // |y| >= .5
        s = 0.5f / s;
        x = (yx + xy) * s;
        z = (zy + yz) * s;
        w = (xz - zx) * s;
    } else {
        float s = sqrt(1.0 + zz - xx - yy); // |s|>=1
        z = s * 0.5f; // |z| >= .5
        s = 0.5f / s;
        x = (xz + zx) * s;
        y = (zy + yz) * s;
        w = (yx - xy) * s;
    }
    return vec4(x, y, z, w);
}

vec4 billboard_quaternion(vec3 dir, vec3 up) {
    dir = normalize(dir);
    up = normalize(up - (dir * dot(up, dir)));

    vec3 right = cross(up, dir);

    vec4 q = vec4(0.0);
    q.w = sqrt(1.0 + right.x + up.y + dir.z) * 0.5;
    float w4 = 1.0 / (4.0 * q.w);
    q.x = (up.z - dir.y) * w4;
    q.y = (dir.x - right.z) * w4;
    q.z = (right.y - up.x) * w4;
    return q;
}
