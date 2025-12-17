#ifndef GLSL_LIB_ATMSCAT
#define GLSL_LIB_ATMSCAT

#if defined(atmosphereGround) || defined(atmosphericScattering)
// OUTPUTS
out vec3 v_position;

void prepareAtmosphericScattering() {
    v_position = a_position;
}

#else
void prepareAtmosphericScattering() {}
#endif // atmosphereGround || atmosphericScattering


#endif // ATMSCAT
