// Visualize both functions:
// http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIoKDEveCktKDEvMSkpLygoMS8xMDApLSgxLzEpKSIsImNvbG9yIjoiIzNFOEExNyJ9LHsidHlwZSI6MCwiZXEiOiJsb2coMS4wKngrMS4wKS9sb2coMS4wKjEwMCsxLjApOyIsImNvbG9yIjoiI0YyMUYxRiJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIjEiLCIxMDAiLCIwIiwiMSJdLCJzaXplIjpbNjQ4LDM5OF19XQ--
float defaultDepth(float z, float near, float far) {
    return ((1.0 / z) - (1.0 / near)) / ((1.0 / far) - (1.0 / near));
}
float logarithmicDepth(float z, float zfar, float k) {
    return log(k * z + 1.0) / log(zfar * k + 1.0);
}

float getDepthValue(float zfar, float k){
    float w = 1.0 / gl_FragCoord.w;
    return logarithmicDepth(w, zfar, k);
}

float getDepthValue(float z, float zfar, float k){
    return logarithmicDepth(z, zfar, k);
}

float recoverWValue(float depth, float zfar, float k){
    float w = (exp(depth * log(zfar * k + 1.0)) - 1.0) / k;
    return 1.0 / w;
}
