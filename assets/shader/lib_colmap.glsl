// Value is assumed to be normalized in [0,1] in all cases


// SPACE
float colormap_space_red(float x) {
    if (x < 37067.0 / 158860.0) {
        return 0.0;
    } else if (x < 85181.0 / 230350.0) {
        float xx = x - 37067.0 / 158860.0;
        return (780.25 * xx + 319.71) * xx / 255.0;
    } else if (x < (sqrt(3196965649.0) + 83129.0) / 310480.0) {
        return ((1035.33580904442 * x - 82.5380748768798) * x - 52.8985266363332) / 255.0;
    } else if (x < 231408.0 / 362695.0) {
        return (339.41 * x - 33.194) / 255.0;
    } else if (x < 152073.0 / 222340.0) {
        return (1064.8 * x - 496.01) / 255.0;
    } else if (x < 294791.0 / 397780.0) {
        return (397.78 * x - 39.791) / 255.0;
    } else if (x < 491189.0 / 550980.0) {
        return 1.0;
    } else if (x < 1.0) {
        return (5509.8 * x + 597.91) * x / 255.0;
    } else {
        return 1.0;
    }
}

float colormap_space_green(float x) {
    float xx;
    if (x < 0.0) {
        return 0.0;
    } else if (x < (-sqrt(166317494.0) + 39104.0) / 183830.0) {
        return (-1838.3 * x + 464.36) * x / 255.0;
    } else if (x < 37067.0 / 158860.0) {
        return (-317.72 * x + 74.134) / 255.0;
    } else if (x < (3.0 * sqrt(220297369.0) + 58535.0) / 155240.0) {
        return 0.0;
    } else if (x < 294791.0 / 397780.0) {
        xx = x - (3.0 * sqrt(220297369.0) + 58535.0) / 155240.0;
        return (-1945.0 * xx + 1430.2) * xx / 255.0;
    } else if (x < 491189.0 / 550980.0) {
        return ((-1770.0 * x + 3.92813840044638e3) * x - 1.84017494792245e3) / 255.0;
    } else {
        return 1.0;
    }
}

float colormap_space_blue(float x) {
    if (x < 0.0) {
        return 0.0;
    } else if (x < 51987.0 / 349730.0) {
        return (458.79 * x) / 255.0;
    } else if (x < 85181.0 / 230350.0) {
        return (109.06 * x + 51.987) / 255.0;
    } else if (x < (sqrt(3196965649.0) + 83129.0) / 310480.0) {
        return (339.41 * x - 33.194) / 255.0;
    } else if (x < (3.0 * sqrt(220297369.0) + 58535.0) / 155240.0) {
        return ((-1552.4 * x + 1170.7) * x - 92.996) / 255.0;
    } else if (x < 27568.0 / 38629.0) {
        return 0.0;
    } else if (x < 81692.0 / 96241.0) {
        return (386.29 * x - 275.68) / 255.0;
    } else if (x <= 1.0) {
        return (1348.7 * x - 1092.6) / 255.0;
    } else {
        return 1.0;
    }
}

vec3 colormap_space(float x) {
    return vec3(colormap_space_red(x), colormap_space_green(x), colormap_space_blue(x));
}

// SEISMIC
float colormap_seismic_f(float x) {
    return ((-2010.0 * x + 2502.5950459) * x - 481.763180924) / 255.0;
}

float colormap_seismic_red(float x) {
    if (x < 0.0) {
        return 3.0 / 255.0;
    } else if (x < 0.238) {
        return ((-1810.0 * x + 414.49) * x + 3.87702) / 255.0;
    } else if (x < 51611.0 / 108060.0) {
        return (344441250.0 / 323659.0 * x - 23422005.0 / 92474.0) / 255.0;
    } else if (x < 25851.0 / 34402.0) {
        return 1.0;
    } else if (x <= 1.0) {
        return (-688.04 * x + 772.02) / 255.0;
    } else {
        return 83.0 / 255.0;
    }
}

float colormap_seismic_green(float x) {
    if (x < 0.0) {
        return 0.0;
    } else if (x < 0.238) {
        return 0.0;
    } else if (x < 51611.0 / 108060.0) {
        return colormap_seismic_f(x);
    } else if (x < 0.739376978894039) {
        float xx = x - 51611.0 / 108060.0;
        return ((-914.74 * xx - 734.72) * xx + 255.) / 255.0;
    } else {
        return 0.0;
    }
}

float colormap_seismic_blue(float x) {
    if (x < 0.0) {
        return 19.0 / 255.0;
    } else if (x < 0.238) {
        float xx = x - 0.238;
        return (((1624.6 * xx + 1191.4) * xx + 1180.2) * xx + 255.0) / 255.0;
    } else if (x < 51611.0 / 108060.0) {
        return 1.0;
    } else if (x < 174.5 / 256.0) {
        return (-951.67322673866 * x + 709.532730938451) / 255.0;
    } else if (x < 0.745745353439206) {
        return (-705.250074130877 * x + 559.620050530617) / 255.0;
    } else if (x <= 1.0) {
        return ((-399.29 * x + 655.71) * x - 233.25) / 255.0;
    } else {
        return 23.0 / 255.0;
    }
}

vec3 colormap_seismic(float x) {
    return vec3(colormap_seismic_red(x), colormap_seismic_green(x), colormap_seismic_blue(x));
}


// CARNATION
float colormap_f(float x) {
    return ((-9.93427e0 * x + 1.56301e1) * x + 2.44663e2 * x) / 255.0;
}

float colormap_carnation_blue(float x) {
    if (x < 0.0) {
        return 11.0 / 255.0;
    } else if (x < 0.16531216481302) {
        return (((-1635.0 * x) + 1789.0) * x + 3.938) / 255.0;
    } else if (x < 0.50663669203696) {
        return 1.0;
    } else if (x < 0.67502056695956) {
        return ((((1.28932e3 * x) - 7.74147e2) * x - 9.47634e2) * x + 7.65071e2) / 255.0;
    } else if (x < 1.0) {
        return colormap_f(x);
    } else {
        return 251.0 / 255.0;
    }
}

float colormap_carnation_green(float x) {
    if (x < 0.0) {
        return 0.0;
    } else if (x < 0.33807590140751) {
        return colormap_f(x);
    } else if (x < 0.50663669203696) {
        return (((-5.83014e2 * x - 8.38523e2) * x + 2.03823e3) * x - 4.86592e2) / 255.0;
    } else if (x < 0.84702285244773) {
        return 1.0;
    } else if (x < 1.0) {
        return (((-5.03306e2 * x + 2.95545e3) * x - 4.19210e3) * x + 1.99128e3) / 255.0;
    } else {
        return 251.0 / 255.0;
    }
}

float colormap_carnation_red(float x) {
    if (x < 0.16531216481302) {
        return 1.0;
    } else if (x < 0.33807590140751) {
        return (((-5.15164e3 * x + 5.30564e3) * x - 2.65098e3) * x + 5.70771e2) / 255.0;
    } else if (x < 0.67502056695956) {
        return colormap_f(x);
    } else if (x < 0.84702285244773) {
        return (((3.34136e3 * x - 9.01976e3) * x + 8.39740e3) * x - 2.41682e3) / 255.0;
    } else {
        return 1.0;
    }
}

vec3 colormap_carnation(float x) {
    return vec3(colormap_carnation_red(x), colormap_carnation_green(x), colormap_carnation_blue(x));
}

// HOT METAL
float colormap_hotmetal_green(float x) {
    if (x < 0.6) {
        return 0.0;
    } else if (x <= 0.95) {
        return ((x - 0.6) * 728.57) / 255.0;
    } else {
        return 1.0;
    }
}

float colormap_hotmetal_red(float x) {
    if (x < 0.0) {
        return 0.0;
    } else if (x <= 0.57147) {
        return 446.22 * x / 255.0;
    } else {
        return 1.0;
    }
}

vec3 colormap_hotmetal(float x) {
    return vec3(colormap_hotmetal_red(x), colormap_hotmetal_green(x), 0.0);
}

// REGULAR RAINBOW

vec3 colormap_rainbow(float x) {
    float r = 0.0, g = 0.0, b = 0.0;

    if (x < 0.0) {
        r = 127.0 / 255.0;
    } else if (x <= 1.0 / 9.0) {
        r = 1147.5 * (1.0 / 9.0 - x) / 255.0;
    } else if (x <= 5.0 / 9.0) {
        r = 0.0;
    } else if (x <= 7.0 / 9.0) {
        r = 1147.5 * (x - 5.0 / 9.0) / 255.0;
    } else {
        r = 1.0;
    }

    if (x <= 1.0 / 9.0) {
        g = 0.0;
    } else if (x <= 3.0 / 9.0) {
        g = 1147.5 * (x - 1.0 / 9.0) / 255.0;
    } else if (x <= 7.0 / 9.0) {
        g = 1.0;
    } else if (x <= 1.0) {
        g = 1.0 - 1147.5 * (x - 7.0 / 9.0) / 255.0;
    } else {
        g = 0.0;
    }

    if (x <= 3.0 / 9.0) {
        b = 1.0;
    } else if (x <= 5.0 / 9.0) {
        b = 1.0 - 1147.5 * (x - 3.0 / 9.0) / 255.0;
    } else {
        b = 0.0;
    }

    if (x == 0.0)
    return vec3(0.0);

    return vec3(r, g, b);
}


// MAIN
/*
    Computes the color map for the normalized value using the given color map.
    Color maps:
        0 - space
        1 - seismic
        2 - carnation
        3 - hotmetal
        4 - rainbow
*/
vec3 colormap(int cmap, float value){
    cmap = cmap % 5;
    if (cmap == 0){
        return colormap_space(value);
    } else if (cmap == 1){
        return colormap_seismic(value);
    } else if (cmap == 2){
        return colormap_carnation(value);
    } else if (cmap == 3){
        return colormap_hotmetal(value);
    }else if (cmap == 4){
        return colormap_rainbow(value);
    }
    return vec3(0.0);
}

vec3 colormap(int cmap, float value, vec2 minmax){
    float x;
    if (minmax.y < minmax.x){
        x = (value - minmax.y) / (minmax.x - minmax.y);
        // Invert
        x = 1.0 - x;
    } else {
        x = (value - minmax.x) / (minmax.y - minmax.x);
    }
    return colormap(cmap, clamp(x, 0.0, 1.0));
}
