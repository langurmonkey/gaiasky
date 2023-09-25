#ifndef GLSL_LIB_DOUBLEFLOAT
#define GLSL_LIB_DOUBLEFLOAT
// This library emulates double-precision floating point numbers
// using two single-precision floating point numbers

// Based on the Fortran single-double package
// http://crd.lbl.gov/~dhbailey/mpdist/

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

    t1 = dsa.x + dsb.x;
    e = t1 - dsa.x;
    t2 = ((dsb.x - e) + (dsa.x - (t1 - e))) + dsa.y + dsb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}

// Subtraction: res = ds_sub(a, b) => res = a - b
vec2 ds_sub (vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float e, t1, t2;

    t1 = dsa.x - dsb.x;
    e = t1 - dsa.x;
    t2 = ((-dsb.x - e) + (dsa.x - (t1 - e))) + dsa.y - dsb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}


// Multiplication: res = ds_mul(a, b) => res = a * b
vec2 ds_mul(vec2 dsa, vec2 dsb) {
    vec2 dsc;
    float c11, c21, c2, e, t1, t2;
    float a1, a2, b1, b2, cona, conb, split = 8193.0;

    cona = dsa.x * split;
    conb = dsb.x * split;
    a1 = cona - (cona - dsa.x);
    b1 = conb - (conb - dsb.x);
    a2 = dsa.x - a1;
    b2 = dsb.x - b1;

    c11 = dsa.x * dsb.x;
    c21 = a2 * b2 + (a2 * b1 + (a1 * b2 + (a1 * b1 - c11)));

    c2 = dsa.x * dsb.y + dsa.y * dsb.x;

    t1 = c11 + c2;
    e = t1 - c11;
    t2 = dsa.y * dsb.y + ((c2 - e) + (c11 - (t1 - e))) + c21;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);

    return dsc;
}

// Division: res = ds_div(a, b) => res = a / b
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

// result = pow(dsa, 2)
vec2 ds_pow2(vec2 dsa) {
    return ds_mul(dsa, dsa);
}

// result = pow(dsa, 3)
vec2 ds_pow3(vec2 dsa) {
    return ds_mul(ds_pow2(dsa), dsa);
}
#endif