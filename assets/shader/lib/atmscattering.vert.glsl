#ifndef GLSL_LIB_ATMSCAT
#define GLSL_LIB_ATMSCAT

#if defined(atmosphereGround) || defined(atmosphericScattering)
uniform float fInnerRadius; /* The inner (planetary) radius*/
// OUTPUTS
out vec3 v_position;

void prepareAtmosphericScattering(float fragHeight) {
    v_position = a_position * (1.0 + fragHeight / fInnerRadius);
}

#else
void prepareAtmosphericScattering(float fragHeight) {}
#endif // atmosphereGround || atmosphericScattering


#endif // ATMSCAT
