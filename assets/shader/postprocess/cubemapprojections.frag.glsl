#version 330 core

uniform sampler2D u_texture0;
uniform samplerCube u_cubemap;
uniform vec2 u_viewport;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#define PI 3.141592653589793238462643383
#define TWOPI PI * 2.0
#define PITWO PI / 2.0
#define DEG_RAD PI / 180.0
#define RAD_DEG 180.0 / PI

#ifdef equirectangular
// Equirectangular (spherical)
vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
    float lat = tc.y * PI;
    float lon = tc.x * TWOPI;
    vec3 cubemaptc;
    cubemaptc.x = -sin(lon) * sin(lat);
    cubemaptc.y = cos(lat);
    cubemaptc.z = -cos(lon) * sin(lat);
    return texture(cubemap, cubemaptc);
}
#endif//equirectangular

#ifdef cylindrical
// Cylindrical
vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
    float lon = tc.x * TWOPI + PI;
    float lat = asin((1.0 - tc.y) * 2.0 - 1.0) - PI / 2.0;

    vec3 cubemaptc;
    cubemaptc.x = -sin(lon) * sin(lat);
    cubemaptc.y = cos(lat);
    cubemaptc.z = -cos(lon) * sin(lat);
    return texture(cubemap, cubemaptc);
}
#endif//cylindrical

#ifdef hammer
// Hammer
vec4 cubeToProjection(samplerCube cubemap, vec2 tc) {
    // The 0.9 factor is a hack to make the projection occupy the full extent of the screen
    #define EXTENT 0.9
    #define EXTENTRES2 (1.0 - EXTENT) / 2.0
    float x = (1.0 - tc.x * EXTENT - EXTENTRES2) * 360.0 - 180.0;
    float y = (1.0 - tc.y * EXTENT - EXTENTRES2) * 180.0 - 90.0;
    //x *= 1.273;
    //y *= 1.273;
    x *= DEG_RAD;
    y *= DEG_RAD;

    float zsqr = 1.0 - pow(x / 4.0, 2.0) - pow(y / 2.0, 2.0);
    if ((x * x / 8.0 + y * y / 2.0 - 1.0) > 0.0) {
        // Deprojection not valid, values must be inside space
        return vec4(0.0, 0.0, 0.0, 1.0);
    }
    float z = sqrt(zsqr);

    float lon = 2.0 * atan(4.0 * z * z - 2.0, z * x) - PI;
    float lat = asin(z * y) - PITWO;

    vec3 cubemaptc;
    cubemaptc.x = -sin(lon) * sin(lat);
    cubemaptc.y = cos(lat);
    cubemaptc.z = -cos(lon) * sin(lat);
    return texture(cubemap, cubemaptc);
}
#endif//hammer

#ifdef azimuthal
uniform float u_planetariumAngle;
uniform float u_planetariumAperture;

mat3 rotAxis(vec3 axis, float a) {
    float s=sin(a);
    float c=cos(a);
    float oc=1.0-c;
    vec3 as=axis*s;
    mat3 p=mat3(axis.x*axis, axis.y*axis, axis.z*axis);
    mat3 q=mat3(c, -as.z, as.y, as.z, c, -as.x, -as.y, as.x, c);
    return p*oc+q;
}

// Domemaster (planetarium) -- Azimuthal equidistant projection.
vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
    float aperture = u_planetariumAperture;

    vec2 vp = u_viewport;
    tc = tc * 2.0 - 1.0;
    vec2 arv = vp.xy / min(vp.x, vp.y);
    tc *= arv;
    float r = length(tc);

    if (r <= 1.0) {
        float phi = atan(tc.y, tc.x);
        float theta = r * (aperture / 2.0) * DEG_RAD;

        vec3 cubemaptc;
        cubemaptc.x = sin(theta) * cos(phi);
        cubemaptc.y = -sin(theta) * sin(phi);
        cubemaptc.z = cos(theta);

        mat3 r = rotAxis(vec3(1.0, 0.0, 0.0), u_planetariumAngle * DEG_RAD);
        cubemaptc = cubemaptc * r;

        return texture(cubemap, cubemaptc);
    } else {
        return vec4(0.0, 0.0, 0.0, 1.0);
    }

}
#endif//azimuthal

#ifdef orthographic
// Orthographic (spherical) hemispherical view
//vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
//    vec2 vp = u_viewport;
//    tc = tc * 2.0 - 1.0;
//    vec2 arv = vp.xy / min(vp.x, vp.y);
//    tc *= arv;
//    
//	float r = length(tc);
//	if (r <= 1.0) {
//        vec3 cubemaptc;
//        cubemaptc.x = tc.x;
//        cubemaptc.y = -tc.y;
//        cubemaptc.z = sqrt(1.0-r*r);
//        return texture(cubemap, cubemaptc);
//    } else {
//        return vec4(0.0, 0.0, 0.0, 1.0);
//    }
//}
vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
    vec2 vp = u_viewport;
    tc = (tc-vec2(0.0,0.5))*2.0;
    vec2 arv = vp.xy / min(vp.x/2., vp.y);
    tc *= arv;
    
    if (tc.x<=arv.x){
		tc.x = tc.x - arv.x/2.;
	    float r = length(tc);
	    if (r <= 1.0) {
            vec3 cubemaptc;
            cubemaptc.x = -tc.x;
            cubemaptc.y = -tc.y;
            cubemaptc.z = -sqrt(1.0-r*r);
            return texture(cubemap, cubemaptc);
        } else {
            return vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else {
		tc.x = tc.x - arv.x*1.5;
		float r = length(tc);
	    if (r <= 1.0) {
            vec3 cubemaptc;
            cubemaptc.x = tc.x;
            cubemaptc.y = -tc.y;
            cubemaptc.z = sqrt(1.0-r*r);
            return texture(cubemap, cubemaptc);
        } else {
            return vec4(0.0, 0.0, 0.0, 1.0);
        }
	}
}
#endif//orthographic

#ifdef orthosphere
uniform float u_celestialSphereIndexOfRefraction;
// Orthographic view of the celestial sphere. 
vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
	float n = u_celestialSphereIndexOfRefraction;
    vec2 vp = u_viewport;
    tc = tc * 2.0 - 1.0;
    vec2 arv = vp.xy / min(vp.x, vp.y);
    tc *= arv;
    
    float r = length(tc);
    if (r <= 1.0) {
        vec3 cubemaptc;
        //front
        cubemaptc.x = tc.x;
        cubemaptc.y = -tc.y;
        cubemaptc.z = -sqrt(1.0-r*r);
        vec4 b = texture(cubemap, cubemaptc);
        
        //back
        //float n=1.33;
        float scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        vec4 a =  vec4(0.0, 0.0, 0.0, 1.0);
        if (r*scale<1){
			cubemaptc.x = tc.x*scale;
			cubemaptc.y = -tc.y*scale;
			cubemaptc.z = sqrt(1.0-r*r*scale*scale);
			
			a.x = texture(cubemap, cubemaptc).x;
		} 
		
		n=1.0+(n-1.0)*1.003;
        scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        if (r*scale<1){
			cubemaptc.x = tc.x*scale;
			cubemaptc.y = -tc.y*scale;
			cubemaptc.z = sqrt(1.0-r*r*scale*scale);
			
			a.y = texture(cubemap, cubemaptc).y;
		}
		
		n=1.0+(n-1.0)*1.003;
        scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        if (r*scale<1){
			cubemaptc.x = tc.x*scale;
			cubemaptc.y = -tc.y*scale;
			cubemaptc.z = sqrt(1.0-r*r*scale*scale);
			
			a.z = texture(cubemap, cubemaptc).z;
		} 
        
        //dim the back a bit without affecting brightest areas (e.g. the Sun).
        float c = 1.0 - 0.5*(b.x+b.y+b.z)/3.;
        c = c*(0.5+0.5*(a.x+a.y+a.z)/3.);
        a.x = a.x * c;
        a.y = a.y * c;
        a.z = a.z * c;
		
        return 1.0-(1.0-a)*(1.0-b); //screen overlay
    } else {
        return vec4(0.0, 0.0, 0.0, 1.0);
    }
}
#endif//orthosphere



#ifdef orthosphere_crosseye
uniform float u_celestialSphereIndexOfRefraction;
vec4 cubeToProjection(samplerCube cubemap, vec2 tc){
	float n = u_celestialSphereIndexOfRefraction;
    vec2 vp = u_viewport;
    tc = (tc-vec2(0.0,0.5))*2.0;
    vec2 arv = vp.xy / min(vp.x/2., vp.y);
    tc *= arv;
    
    if (tc.x<=arv.x){
	tc.x = tc.x - arv.x/2.;
	float r = length(tc);
	if (r <= 1.0) {
            vec3 cubemaptc;
            float sx = tc.x;
            cubemaptc.y = -tc.y;
            float sz = -sqrt(1.0-r*r);
            cubemaptc.x = (30. * sx -       sz)/sqrt(901.);//rotation for each view.
            cubemaptc.z = (      sx + 30. * sz)/sqrt(901.);//coefficients are normalized
            vec4 b = texture(cubemap, cubemaptc);

	    //    vec4 a = texture(cubemap, cubemaptc);   
        float scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        vec4 a =  vec4(0.0, 0.0, 0.0, 1.0);
        if (r*scale<1){
	    sz = sqrt(1.0-r*r*scale*scale);
            sx=tc.x*scale;
	    cubemaptc.x = (30. * sx -       sz)/sqrt(901.);
            cubemaptc.z = (      sx + 30. * sz)/sqrt(901.);
			cubemaptc.y = -tc.y*scale;
			
			a.x = texture(cubemap, cubemaptc).x;
		} 
		
		n=1.0+(n-1.0)*1.003;
        scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        if (r*scale<1){
	    sz = sqrt(1.0-r*r*scale*scale);
            sx=tc.x*scale;
	    cubemaptc.x = (30. * sx -       sz)/sqrt(901.);
            cubemaptc.z = (      sx + 30. * sz)/sqrt(901.);
			cubemaptc.y = -tc.y*scale;
			
			a.y = texture(cubemap, cubemaptc).y;
		}
		
		n=1.0+(n-1.0)*1.003;
        scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        if (r*scale<1){
	    sz = sqrt(1.0-r*r*scale*scale);
            sx=tc.x*scale;
	    cubemaptc.x = (30. * sx -       sz)/sqrt(901.);
            cubemaptc.z = (      sx + 30. * sz)/sqrt(901.);
			cubemaptc.y = -tc.y*scale;
			
			a.z = texture(cubemap, cubemaptc).z;
		} 
	        float c = 1.0 - 0.5*(b.x+b.y+b.z)/3.;
            c = c*(0.5+0.5*(a.x+a.y+a.z)/3.);
            a.x = a.x * c;
            a.y = a.y * c;
            a.z = a.z * c;
            return 1.0-(1.0-a)*(1.0-b);
        } else {
            return vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else {
	tc.x = tc.x - arv.x*1.5;
	float r = length(tc);
	if (r <= 1.0) {
            vec3 cubemaptc;
            float sx = tc.x;
            cubemaptc.y = -tc.y;
            float sz = -sqrt(1.0-r*r);
            cubemaptc.x = (30. * sx +       sz)/sqrt(901.);
            cubemaptc.z = (    - sx + 30. * sz)/sqrt(901.);
            vec4 b = texture(cubemap, cubemaptc);

	    //vec4 a = texture(cubemap, cubemaptc);   
        float scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        vec4 a =  vec4(0.0, 0.0, 0.0, 1.0);
        if (r*scale<1){
	    sz = sqrt(1.0-r*r*scale*scale);
            sx=tc.x*scale;
	    cubemaptc.x = (30. * sx +       sz)/sqrt(901.);
            cubemaptc.z = (    - sx + 30. * sz)/sqrt(901.);

			cubemaptc.y = -tc.y*scale;
			
			a.x = texture(cubemap, cubemaptc).x;
		} 
		
		n=1.0+(n-1.0)*1.003;
        scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        if (r*scale<1){
	    sz = sqrt(1.0-r*r*scale*scale);
            sx=tc.x*scale;
	    cubemaptc.x = (30. * sx +       sz)/sqrt(901.);
            cubemaptc.z = (    - sx + 30. * sz)/sqrt(901.);
			cubemaptc.y = -tc.y*scale;
			
			a.y = texture(cubemap, cubemaptc).y;
		}
		
		n=1.0+(n-1.0)*1.003;
        scale = sin(2.0*asin(r/n)-asin(r))/r; //(-1+2.0*r*r/(n*n)+2.0*sqrt(1.0-r*r)*sqrt(1.0-r*r/n/n)/n);
        if (r*scale<1){
	    sz = sqrt(1.0-r*r*scale*scale);
            sx=tc.x*scale;
	    cubemaptc.x = (30. * sx +       sz)/sqrt(901.);
            cubemaptc.z = (    - sx + 30. * sz)/sqrt(901.);
			cubemaptc.y = -tc.y*scale;
			
			a.z = texture(cubemap, cubemaptc).z;
		} 
            float c = 1.0 - 0.5*(b.x+b.y+b.z)/3.;
            c = c*(0.5+0.5*(a.x+a.y+a.z)/3.);
            a.x = a.x * c;
            a.y = a.y * c;
            a.z = a.z * c;
            return 1.0-(1.0-a)*(1.0-b);
        } else {
            return vec4(0.0, 0.0, 0.0, 1.0);
        }
	}
}
#endif//orthosphere_crosseye

void main(void){
    fragColor = cubeToProjection(u_cubemap, v_texCoords);
}
