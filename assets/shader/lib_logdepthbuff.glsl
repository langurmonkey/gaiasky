#define z_far 1e24
#define K 1.0
float getDepthValue(float fragmentDist) {
    return log(K * fragmentDist + 1.0) / log(K * z_far + 1.0);
}