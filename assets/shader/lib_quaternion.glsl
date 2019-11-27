

vec4 quatFromAxes(float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz){
    // the trace is the sum of the diagonal elements; see
    // http://mathworld.wolfram.com/MatrixTrace.html
    float t = xx + yy + zz;
    float x, y, z, w;
    // we protect the division by s by ensuring that s>=1
    if (t >= 0) { // |w| >= .5
        float s = sqrt(t + 1.0); // |s|>=1 ...
        w = 0.5 * s;
        s = 0.5 / s; // so this division isn't bad
        x = (zy - yz) * s;
        y = (xz - zx) * s;
        z = (yx - xy) * s;
    } else if ((xx > yy) && (xx > zz)) {
        float s = sqrt(1.0 + xx - yy - zz); // |s|>=1
        x = s * 0.5; // |x| >= .5
        s = 0.5 / s;
        y = (yx + xy) * s;
        z = (xz + zx) * s;
        w = (zy - yz) * s;
    } else if (yy > zz) {
        float s = sqrt(1.0 + yy - xx - zz); // |s|>=1
        y = s * 0.5; // |y| >= .5
        s = 0.5 / s;
        x = (yx + xy) * s;
        z = (zy + yz) * s;
        w = (xz - zx) * s;
    } else {
        float s = sqrt(1.0 + zz - xx - yy); // |s|>=1
        z = s * 0.5; // |z| >= .5
        s = 0.5 / s;
        x = (xz + zx) * s;
        y = (zy + yz) * s;
        w = (yx - xy) * s;
    }

    return vec4(x, y, z, w);
}

vec4 rotationQuaternion(vec3 direction, vec3 up) {
    vec3 tmp = normalize(cross(up, direction));
    vec3 tmp2 = normalize(cross(direction, tmp));
    return quatFromAxes(tmp.x, tmp2.x, direction.x, tmp.y, tmp2.y, direction.y, tmp.z, tmp2.z, direction.z);
}
