#define z_near 1e7
#define z_far 1e24
// This constant controls the resolution close to the camera
#define K 1e8

#define K_far z_far * K
#define one_over_near 1.0 / z_near
#define one_over_far 1.0 / z_far

float getDepthValue(float z) {
    return log(K * z + 1.0) / log(K_far + 1.0);
}

// Visualize both functions:
// http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIoKDEveCktKDEvMSkpLygoMS8xMDApLSgxLzEpKSIsImNvbG9yIjoiIzNFOEExNyJ9LHsidHlwZSI6MCwiZXEiOiJsb2coMS4wKngrMS4wKS9sb2coMS4wKjEwMCsxLjApOyIsImNvbG9yIjoiI0YyMUYxRiJ9LHsidHlwZSI6MTAwMCwid2luZG93IjpbIjEiLCIxMDAiLCIwIiwiMSJdLCJzaXplIjpbNjQ4LDM5OF19XQ--


float defaultDepth(float z) {
    return ((1.0 / z) - one_over_near) / (one_over_far - one_over_near);
}
float logarithmicDepth(float z) {
    return log(K * z + 1.0) / log(K_far + 1.0);
}
