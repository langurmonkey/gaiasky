float lint2(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return mix(y0, y1, (x - x0) / (x1 - x0));
}

float lint(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) x = x0;
    if(x >= x1) x = x1;
    return y0 + (y1 - y0) * smoothstep(x0, x1, x);
}
