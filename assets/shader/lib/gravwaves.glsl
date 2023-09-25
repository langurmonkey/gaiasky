uniform vec4 u_hterms; // hpluscos, hplussin, htimescos, htimessin
uniform vec3 u_gw; // Location of gravitational wave, cartesian
uniform mat3 u_gwmat3; // Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
uniform float u_ts; // Time in seconds since start
uniform float u_omgw; // Wave frequency
// Computes
vec3 computeGravitationalWaves(vec3 pos, vec3 gw, mat3 gwmat3, float t, float omgw, vec4 hterms) {
    float hpluscos = hterms.x;
    float hplussin = hterms.y;
    float htimescos = hterms.z;
    float htimessin = hterms.w;
    
    float agw = gw.x;
    float dgw = gw.y;
    
    vec3 p = gw;
    mat3 eplus = mat3(1,0,0, 0,-1,0, 0,0,0);
    mat3 etimes = mat3(0,1,0, 1,0,0, 0,0,0);
    
    mat3 P = gwmat3;
    mat3 pepluspt = P * eplus * transpose(P);
    mat3 petimespt = P * etimes * transpose(P);
    
    float plusphase = hpluscos * cos(omgw * t) + hplussin * sin(omgw * t);
    float timesphase = htimescos * cos(omgw * t) + htimessin * sin(omgw * t);
    
    pepluspt = pepluspt * plusphase;
    petimespt = petimespt * timesphase;
    
    mat3 pept = pepluspt + petimespt;
    
    // Backup distance to pos
    float poslen = length(pos);
    // Normalize pos
    pos = normalize(pos);
    
    vec3 huu = pept * pos * pos;
    vec3 hu = 0.5 * pept * pos;
    
    vec3 deltau = ((pos + p) / (2 * (1 + pos * p))) * huu - hu;
    
    // Apply shift, compute new position using backup distance
    pos = pos + deltau;
    pos = normalize(pos) * poslen;
    return pos;
}