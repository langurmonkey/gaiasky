#ifndef GLSL_LIB_NOISE4Q
#define GLSL_LIB_NOISE4Q

// 4D value noise, after https://www.shadertoy.com/view/4lfSzS.
// Use three coordinates for a direction and the fourth for the radius: the structure stretches radially for free, with no poles and no rings.

vec4 stnHash4(vec4 n) { return fract(sin(n) * 1399763.5453123); }

// Value noise in [0,1], smoothstep interpolated on the integer lattice.
float stnNoise4(vec4 x) {
    vec4 n3 = vec4(0.0, 0.25, 0.5, 0.75);
    vec4 p2 = floor(x.wwww + n3);
    vec4 b = floor(x.xxxx + n3) + floor(x.yyyy + n3) * 157.0 + floor(x.zzzz + n3) * 113.0;
    vec4 p1 = b + fract(p2 * 0.00390625) * vec4(164352.0, -164352.0, 163840.0, -163840.0);
    p2 = b + fract((p2 + 1.0) * 0.00390625) * vec4(164352.0, -164352.0, 163840.0, -163840.0);

    vec4 f1 = fract(x.xxxx + n3);
    vec4 f2 = fract(x.yyyy + n3);
    f1 = f1 * f1 * (3.0 - 2.0 * f1);
    f2 = f2 * f2 * (3.0 - 2.0 * f2);

    vec4 n1 = vec4(0.0, 1.0, 157.0, 158.0);
    vec4 n2 = vec4(113.0, 114.0, 270.0, 271.0);

    vec4 vs1 = mix(stnHash4(p1), stnHash4(n1.yyyy + p1), f1);
    vec4 vs2 = mix(stnHash4(n1.zzzz + p1), stnHash4(n1.wwww + p1), f1);
    vec4 vs3 = mix(stnHash4(p2), stnHash4(n1.yyyy + p2), f1);
    vec4 vs4 = mix(stnHash4(n1.zzzz + p2), stnHash4(n1.wwww + p2), f1);

    vs1 = mix(vs1, vs2, f2);
    vs3 = mix(vs3, vs4, f2);

    vs2 = mix(stnHash4(n2.xxxx + p1), stnHash4(n2.yyyy + p1), f1);
    vs4 = mix(stnHash4(n2.zzzz + p1), stnHash4(n2.wwww + p1), f1);
    vs2 = mix(vs2, vs4, f2);

    vs4 = mix(stnHash4(n2.xxxx + p2), stnHash4(n2.yyyy + p2), f1);
    vec4 vs5 = mix(stnHash4(n2.zzzz + p2), stnHash4(n2.wwww + p2), f1);
    vs4 = mix(vs4, vs5, f2);

    f1 = fract(x.zzzz + n3);
    f2 = fract(x.wwww + n3);
    f1 = f1 * f1 * (3.0 - 2.0 * f1);
    f2 = f2 * f2 * (3.0 - 2.0 * f2);

    vs1 = mix(vs1, vs2, f1);
    vs3 = mix(vs3, vs4, f1);
    vs1 = mix(vs1, vs3, f2);

    float r = dot(vs1, vec4(0.25));
    return r * r * (3.0 - 2.0 * r);
}

#endif // GLSL_LIB_NOISE4Q
