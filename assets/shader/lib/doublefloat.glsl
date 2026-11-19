#ifndef GLSL_LIB_DOUBLEFLOAT
#define GLSL_LIB_DOUBLEFLOAT
// This library emulates double-precision floating point numbers
// using two single-precision floating point numbers

// Based on the Fortran single-double package
// http://crd.lbl.gov/~dhbailey/mpdist/

// Optimization barrier.
// The error-free transformations below (Knuth's two-sum, Dekker's product) are
// only exact if the compiler neither contracts them into fused multiply-adds
// nor reassociates the float arithmetic. GLSL before 4.00 has no way to forbid
// that, so every intermediate is routed through an int round trip with a value
// the compiler cannot know at compile time.
// u_dsBarrier is deliberately never set by the application: an unset uniform is
// 0, so the round trip is the identity at runtime while staying opaque to the
// optimizer. Do not remove, and do not bind it.
// The orbital elements shader opts out with DS_NO_BARRIER, because it relies on
// the compiler contracting the error-free transformations to recover the speed
// of the pre-barrier code. That is safe there and only there: it does not use
// these transforms to keep a position stable over long time spans, it starts
// from scratch on every vertex.
#ifndef DS_NO_BARRIER
uniform int u_dsBarrier = 0;
float ds_hb(float x) {
    return intBitsToFloat(floatBitsToInt(x) + u_dsBarrier);
}
#else
float ds_hb(float x) {
    return x;
}
#endif

// Setting
vec2 ds(float a) {
    return vec2(a, 0.0);
}
vec2 ds_set(float a) {
    return ds(a);
}

// Comparison:
//          res = -1 if a < b
//              = 0 if a == b
//              = 1 if a > b
float ds_compare(vec2 dsa, vec2 dsb) {
    if (dsa.x < dsb.x) return -1.0;
    else if (dsa.x == dsb.x)
    {
        if (dsa.y < dsb.y) return -1.0;
        else if (dsa.y == dsb.y) return 0.0;
        else return 1.0;
    }
    else return 1.0;
}


// Addition: res = ds_add(a, b) => res = a + b
vec2 ds_add(vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float t1, t2, e;

    t1 = ds_hb(dsa.x + dsb.x);
    e = ds_hb(t1 - dsa.x);
    t2 = ds_hb(ds_hb(ds_hb(dsb.x - e) + ds_hb(dsa.x - ds_hb(t1 - e))) + ds_hb(dsa.y + dsb.y));

    dsc.x = ds_hb(t1 + t2);
    dsc.y = ds_hb(t2 - ds_hb(dsc.x - t1));
    return dsc;
}

// Subtraction: res = ds_sub(a, b) => res = a - b
// Expressed through ds_add so there is a single EFT in the library and both
// sides round identically to ds_add(a, -b).
vec2 ds_sub (vec2 dsa, vec2 dsb) {
    return ds_add(dsa, -dsb);
}


// Multiplication: res = ds_mul(a, b) => res = a * b
vec2 ds_mul(vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float c11, c21, c2, e, t1, t2;
    float a1, a2, b1, b2, cona, conb, split = ds_hb(8193.0);

    cona = ds_hb(dsa.x * split);
    conb = ds_hb(dsb.x * split);
    a1 = ds_hb(cona - ds_hb(cona - dsa.x));
    b1 = ds_hb(conb - ds_hb(conb - dsb.x));
    a2 = ds_hb(dsa.x - a1);
    b2 = ds_hb(dsb.x - b1);

    c11 = ds_hb(dsa.x * dsb.x);
    c21 = ds_hb(ds_hb(a2 * b2) + ds_hb(ds_hb(a2 * b1) + ds_hb(ds_hb(a1 * b2) + ds_hb(ds_hb(a1 * b1) - c11))));

    c2 = ds_hb(ds_hb(dsa.x * dsb.y) + ds_hb(dsa.y * dsb.x));

    t1 = ds_hb(c11 + c2);
    e = ds_hb(t1 - c11);
    t2 = ds_hb(ds_hb(ds_hb(dsa.y * dsb.y) + ds_hb(ds_hb(c2 - e) + ds_hb(c11 - ds_hb(t1 - e)))) + c21);

    dsc.x = ds_hb(t1 + t2);
    dsc.y = ds_hb(t2 - ds_hb(dsc.x - t1));

    return dsc;
}

// Division: res = ds_div(a, b) => res = a / b
// NOT barriered: no shader calls it. Barrier every intermediate with ds_hb
// before using it, or it has the same contraction problem as the rest.
vec2 ds_div(vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float a1, a2, b1, b2, cona, conb, c11, c2, c21, e, s1, s2;
    float t1, t2, t11, t12, t21, t22, split = 8193.0;

    // Compute approximation to the quotient
    s1 = dsa.x / dsb.x;

    //   On systems with a fused multiply add, such as IBM systems, it is faster to
    //  uncomment the next two lines and comment out the following lines until !>.
    //   On other systems, do the opposite.
    // c11 = s1 * dsb(1)
    //c21 = s1 * dsb(1) - c11
    //   This splits s1 and dsb(1) into high-order and low-order words.
    cona = s1 * split;
    conb = dsb.x * split;
    a1 = cona - (cona - s1);
    b1 = conb - (conb - dsb.x);
    a2 = s1 - a1;
    b2 = dsb.x - b1;

    // Multiply s1 * dsb(1) using Dekker's method.
    c11 = s1 * dsb.x;
    c21 = (((a1 * b1 - c11) + a1 * b2) + a2 * b1) + a2 * b2;

    // Compute s1 * dsb(2) (only high-order word is needed).
    c2 = s1 * dsb.y;

    // Compute (c11, c21) + c2 using Knuth's trick.
    t1 = c11 + c2;
    e = t1 - c11;
    t2 = ((c2 - e) + (c11 - (t1 - e))) + c21;

    //   The result is t1 + t2, after normalization.
    t12 = t1 + t2;
    t22 = t2 - (t12 - t1);

    //   Compute dsa - (t12, t22) using Knuth's trick.
    t11 = dsa.x - t12;
    e = t11 - dsa.x;
    t21 = ((-t12 - e) + (dsa.x - (t11 - e))) + dsa.y - t22;

    //  Compute high-order word of (t11, t21) and divide by dsb(1).
    s2 = (t11 + t21) / dsb.x;

    //   The result is s1 + s2, after normalization.
    dsc.x = s1 + s2;
    dsc.y = s2 - (dsc.x - s1);

    return dsc;
}

// Modulo: res = ds_mod(a, f) => res = a mod f
// Uses the identity a mod f = a - f * trunc(a / f), computed in
// double-single precision so the result is accurate even when
// a is much larger than f.
vec2 ds_mod(vec2 dsa, float f) {
    vec2 fs = ds_set(f);
    vec2 q = ds_div(dsa, fs);
    // Truncate toward zero, in double-single precision.
    float t = trunc(q.x);
    vec2 qt = ds_set(t);
    return ds_sub(dsa, ds_mul(fs, qt));
}

// Non-negative modulo: res = ds_modf(a, f) => a mod f, in [0, f), f > 0
// Unlike ds_mod, the quotient is floored, so the result never goes negative.
vec2 ds_modf(vec2 dsa, float f) {
    float q = floor((dsa.x + dsa.y) / f);
    vec2 r = ds_sub(dsa, ds_mul(ds_set(q), ds_set(f)));
    if (r.x < 0.0) r = ds_add(r, ds_set(f));
    else if (r.x >= f) r = ds_sub(r, ds_set(f));
    return r;
}

// 2*pi as a double-float
const vec2 DS_TWOPI = vec2(6.2831854820251465, -1.7484555314695172e-07);

// Reduce a double-float angle to [0, 2*pi)
vec2 ds_mod2pi(vec2 dsa) {
    float q = floor((dsa.x + dsa.y) / DS_TWOPI.x);
    vec2 r = ds_sub(dsa, ds_mul(ds_set(q), DS_TWOPI));
    if (r.x < 0.0) r = ds_add(r, DS_TWOPI);
    else if (r.x >= DS_TWOPI.x) r = ds_sub(r, DS_TWOPI);
    return r;
}

// Mean anomaly (rad) in double-float, reduced to [0, 2*pi).
//   deltat: time since epoch, in days, as a double-float
//   period: orbital period, in days
//   M0:     mean anomaly at epoch, in radians
vec2 ds_meanAnomaly(vec2 deltat, float period, float M0) {
    float n = DS_TWOPI.x / period;
    // Reduce modulo the period so n*deltat stays small.
    vec2 deltat_p = ds_modf(deltat, period);
    vec2 M_d = ds_add(ds_set(M0), ds_mul(deltat_p, ds_set(n)));
    return ds_mod2pi(M_d);
}

// result = pow(dsa, 2)
vec2 ds_pow2(vec2 dsa) {
    return ds_mul(dsa, dsa);
}

// result = pow(dsa, 3)
vec2 ds_pow3(vec2 dsa) {
    return ds_mul(ds_pow2(dsa), dsa);
}
#endif
