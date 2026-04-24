#ifndef GLSL_LIB_LINES
#define GLSL_LIB_LINES

// Determine how thick you want the "solid" part of the line (0.0 to 1.0).
// 0.6 means the line is solid for 60% of the quad width and fades at the last 40%
#define LINE_THICKNESS 0.5
// Use new method based on screen-space rate of change.
// Comment out to use the legacy method based on a power function.
#define LINE_FWIDTH

#ifdef LINE_FWIDTH
// New method that produces solid lines based on screen-space rate of change.
// Returns a vec2 where x is the alpha transparency, and y is the core (0 in this case).
vec2 computeLine(vec2 uv) {
    // x is in [-1, 1], where 0 is the center of the line.
    float x = (uv.y - 0.5) * 2.0;
    float absX = abs(x);
    // Calculate the screen-space rate of change.
    // This tells us how much 'x' changes from one pixel to the next
    float delta = fwidth(absX);

    // Create a sharp but anti-aliased edge.
    // The transition happens over exactly the distance of 'delta' (one pixel)
    float alpha = 1.0 - smoothstep(LINE_THICKNESS - delta, LINE_THICKNESS, absX);
    return vec2(alpha, 0.0);
}
#else

#ifndef PI
#define PI 3.14159265359
#endif // PI

// Legacy method with a power function falloff and a core. Pre Gaia Sky 3.7.3.
// Returns a vec2 where x is the alpha transparency, and y is the core.
vec2 computeLine(vec2 uv) {
    // x is in [-1,1], where 0 is the center of the line
    float x = (uv.y - 0.5) * 2.0;

    float core = min(cos(PI * x / 2.0), 1.0 - abs(x));
    float alpha = pow(core, 1.8);
    float cplus = pow(core, 10.0);
    return vec2(alpha, cplus);
}
#endif // LINE_FWIDTH

#endif // GLSL_LIB_LINES
