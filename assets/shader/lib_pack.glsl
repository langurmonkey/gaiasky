const float c_precision = 128.0;
const float c_precisionp1 = c_precision + 1.0;

// Packs a 2-vector into a float
float pack2(vec2 color) {
    color = clamp(color, 0.0, 1.0);
    return floor(color.r * c_precision + 0.5)
    + floor(color.g * c_precision + 0.5) * c_precisionp1;
}
// Unpacks a float into a 2-vector
vec2 unpack2(float value) {
    vec2 color;
    color.r = mod(value, c_precisionp1) / c_precision;
    color.g = mod(floor(value / c_precisionp1), c_precisionp1) / c_precision;
    return color;
}
// Packs a 3-vector into a float
float pack3(vec3 color) {
    color = clamp(color, 0.0, 1.0);
    return floor(color.r * c_precision + 0.5)
            + floor(color.g * c_precision + 0.5) * c_precisionp1
            + floor(color.b * c_precision + 0.5) * c_precisionp1 * c_precisionp1;
}
// Unpacks a float into a 3-vector
vec3 unpack3(float value) {
    vec3 color;
    color.r = mod(value, c_precisionp1) / c_precision;
    color.g = mod(floor(value / c_precisionp1), c_precisionp1) / c_precision;
    color.b = floor(value / (c_precisionp1 * c_precisionp1)) / c_precision;
    return color;
}
