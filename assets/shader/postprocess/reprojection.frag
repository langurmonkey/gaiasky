// Simple and accurate fisheye projection shaders by Toni Sagrista
// License: MPL2

// Projection shaders for: Lambert equal-area projection, 
// orthographic projection, stereographic projection by
// Svetlin Tassev (2022). License: MPL2

#version 330 core

uniform sampler2D u_texture0;
uniform vec2 u_viewport;
uniform float u_fov;

// u_mode values:
//
// Fisheye:
// 0 - default
// 1 - accurate (with fov, no full coverage)
//
// Stereographic:
//
// 10 - fit screen in projection (default)
// 11 - fit long axis in screen
// 12 - fit short axis in screen 
// 13 - fit 180deg fov
//
// Lambert:
// 
// 20 - fit screen in projection (default)
// 21 - fit long axis in screen
// 22 - fit short axis in screen 
// 23 - fit 180deg fov
//
// Orthographic:
//
// 30 - fit screen in projection (default)
// 31 - fit long axis in screen
// 32 - fit short axis in screen 
// 33 - fit 180deg fov
uniform int u_mode;

in vec2 v_texCoords;
layout (location = 0) out vec4 fragColor;

#define PI 3.1415926535

void main()
{
    if (u_mode < 0) {
        // Disabled, pass-through.
        fragColor = texture(u_texture0, v_texCoords);
    }

    vec2 vp = u_viewport;
    vec2 texCoords = v_texCoords;

    // Fisheye:
    if (u_mode == 0) {
        // tc in v_texCoords in [-1, 1]
        vec2 tc = (texCoords * 2.0 - 1.0);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        // Coordinates of current fragment
        vec2 xy = tc * arv;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < 1.0) {
            float z = sqrt(1.0 - d * d);
            float arx = min(1.0, vp.y / vp.x);
            float ary = min(1.0, vp.x / vp.y);

            float r = atan(d, z) / PI;
            float phi = atan(xy.y, xy.x);

            vec2 uv;
            uv.x = (r * cos(phi) + 0.5) * arx + (1.0 - arx) / 2.0;
            uv.y = (r * sin(phi) + 0.5) * ary + (1.0 - ary) / 2.0;
            fragColor = texture(u_texture0, uv);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode ==1) {
        // tc in v_texCoords in [-1, 1]
        vec2 tc = (texCoords * 2.0 - 1.0);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        // Coordinates of current fragment
        vec2 xy = tc * arv;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < 1.0) {
            float z = sqrt(1.0 - d * d);
            float a = 1.0 / (z * tan(u_fov * 0.5));
            fragColor = texture(u_texture0, (tc * a) + 0.5);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }

    // Stereographic:
    else if (u_mode==10) {
        // tc in v_texCoords in [-0.5, 0.5]
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / sqrt(vp.x*vp.x+vp.y*vp.y);
        float c=sqrt(vp.x*vp.x+vp.y*vp.y)/min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.)*c;//fov in gnomonic projection coordinates -- along long axis
        float v_fov = 2.*atan(fov0/2.);// find fov in degrees along long axis
        float fov1 = 4.*tan(v_fov/4.);//fov in new projection (Stereographic)
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1/2.) {
            float a=4./(4.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(2*atan(rscreen/2))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }
    else if (u_mode==11) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / max(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.)*max(vp.x, vp.y)/min(vp.x, vp.y);//fov in gnomonic projection coordinates -- along long axis
        float v_fov = 2.*atan(fov0/2.);// find fov in degrees along long axis
        float fov1 = 4.*tan(v_fov/4.);//fov in new projection (Stereographic)
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1/2.) {
            float a=4./(4.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(2*atan(rscreen/2))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode==12) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.);//fov in gnomonic projection coordinates -- along short axis
        float fov1 = 4.*tan(u_fov/4.);//fov in new projection (Stereographic)


        float fov0v = 2.*tan(u_fov/2.)*max(vp.x, vp.y)/min(vp.x, vp.y);//fov in gnomonic projection coordinates -- along long axis
        float v_fov = 2.*atan(fov0v/2.);// find fov in degrees along long axis
        float fov1v = 4.*tan(v_fov/4.);

        // Coordinates of current fragment
        vec2 xy = tc * arv * fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1v/2.) {
            float a=4./(4.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(2*atan(rscreen/2))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 13) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.);//fov in gnomonic projection coordinates -- along short axis
        float v_fov = u_fov;
        float fov1 = 4.*tan(3.1415926/4.);//fov in new projection (Stereographic); //fov in new projection (orthographic). covers fov of 180deg.
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1/2.) {
            float a=4./(4.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(2*atan(rscreen/2))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }

    //Lambert equa-area:
    else if (u_mode == 20) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / sqrt(vp.x*vp.x+vp.y*vp.y);
        float c=sqrt(vp.x*vp.x+vp.y*vp.y)/min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.)*c;//fov in gnomonic projection coordinates -- along long axis
        float fov1 = sqrt(2./(1.+fov0*fov0/4.+sqrt(1.+fov0*fov0/4.)))*fov0;//fov in new projection
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);

        if (d < fov1/2.) {
            d=d*d;
            float a=sqrt(4.0-d)/(2.0-d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rscreen=sqrt(2/(1+x^2+sqrt(1+x^2)))*x ; x=rtexture
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 21) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / max(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.)*max(vp.x, vp.y)/min(vp.x, vp.y);//fov in gnomonic projection coordinates -- along long axis
        float fov1 = sqrt(2./(1.+fov0*fov0/4.+sqrt(1.+fov0*fov0/4.)))*fov0;//fov in new projection
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);

        if (d < fov1/2.) {
            d=d*d;
            float a=sqrt(4.0-d)/(2.0-d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rscreen=sqrt(2/(1+x^2+sqrt(1+x^2)))*x ; x=rtexture
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 22) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.);//fov in gnomonic projection coordinates -- along long axis
        float fov1 = sqrt(2./(1.+fov0*fov0/4.+sqrt(1.+fov0*fov0/4.)))*fov0;//fov in new projection

        float fov0v = 2.*tan(u_fov/2.)*max(vp.x, vp.y)/min(vp.x, vp.y);//fov in gnomonic projection coordinates -- along long axis
        float fov1v =  sqrt(2./(1.+fov0v*fov0v/4.+sqrt(1.+fov0v*fov0v/4.)))*fov0v;


        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);


        if (d < fov1v/2.) {
            d=d*d;
            float a=sqrt(4.0-d)/(2.0-d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rscreen=sqrt(2/(1+x^2+sqrt(1+x^2)))*x ; x=rtexture
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 23) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.);//fov in gnomonic projection coordinates -- along short axis
        float fov1 = 2.*sqrt(2.);//fov in new projection) covers fov of 180deg.
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);


        if (d < fov1/2.) {
            d=d*d;
            float a=sqrt(4.0-d)/(2.0-d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rscreen=sqrt(2/(1+x^2+sqrt(1+x^2)))*x ; x=rtexture
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }

    //Orthographic:
    else if (u_mode == 30) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / sqrt(vp.x*vp.x+vp.y*vp.y);

        float c=sqrt(vp.x*vp.x+vp.y*vp.y)/min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.)*c;//fov in gnomonic projection coordinates -- along long axis
        float v_fov = 2.*atan(fov0/2.);// find fov in degrees along long axis
        float fov1 = 2.*sin(v_fov/2.);//fov in new projection (orthographic)
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1/2.) {
            float a=1./sqrt(1.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(asin(rscreen))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 31) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / max(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.)*max(vp.x, vp.y)/min(vp.x, vp.y);//fov in gnomonic projection coordinates -- along long axis
        float v_fov = 2.*atan(fov0/2.);// find fov in degrees along long axis
        float fov1 = 2.*sin(v_fov/2.);//fov in new projection (orthographic)
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1/2.) {
            float a=1./sqrt(1.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(asin(rscreen))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 32) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.);//fov in gnomonic projection coordinates -- along short axis
        float fov1 = 2.*sin(u_fov/2.);//fov in new projection (orthographic)

        float fov0v = 2.*tan(u_fov/2.)*max(vp.x, vp.y)/min(vp.x, vp.y);//fov in gnomonic projection coordinates -- along long axis
        float v_fov = 2.*atan(fov0v/2.);// find fov in degrees along long axis
        float fov1v = 2.*sin(v_fov/2.);


        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1v/2.) {
            float a=1./sqrt(1.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(asin(rscreen))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    } else if (u_mode == 33) {
        vec2 tc = (texCoords  - 0.5);
        vec2 arv = vp.xy / min(vp.x, vp.y);
        float fov0 = 2.*tan(u_fov/2.);//fov in gnomonic projection coordinates -- along short axis
        float v_fov = u_fov;
        float fov1 = 2.;//fov in new projection (orthographic). covers fov of 180deg.
        // Coordinates of current fragment
        vec2 xy = tc * arv*fov1;
        // Distance from centre to current fragment
        float d = length(xy);
        if (d < fov1/2.) {
            float a=1./sqrt(1.-d*d);
            vec2 tc1=(xy*a)/arv/fov0+0.5;
            //rtexture=tan(asin(rscreen))
            fragColor = texture(u_texture0, tc1);
        } else {
            fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        }
    }


}
