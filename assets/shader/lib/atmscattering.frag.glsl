#ifndef GLSL_LIB_ATMSCAT_F
#define GLSL_LIB_ATMSCAT_F

float rayleighPhase(float fCos2) {
    // Calculate the angle between view direction and light direction
    // This gives us the phase function for reddening at sunset
    // Rayleigh phase function: 3/16π * (1 + cos²θ)
    return 0.75 * (1.0 + fCos2);  // The 3/(16π) is included in fKrESun
}

float miePhase(float fCos, float fCos2) {
    // Mie phase function (Henyey-Greenstein)
    float g2 = fG * fG;
    return 1.5 * ((1.0 - g2) / (2.0 + g2)) * (1.0 + fCos2) / pow (1.0 + g2 - 2.0 * fG * fCos, 1.5);
}

#endif // ATMSCAT_F