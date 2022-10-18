// Input value must be normalized for all color maps.


// Blues - Red channel
float colormap_blues_red(float x) {
    if (x < 0.8724578971287745) {
        return ((((-2.98580898761749E+03 * x + 6.75014845489710E+03) * x - 4.96941610635258E+03) * x + 1.20190439358912E+03) * x - 2.94374708396149E+02) * x + 2.48449410219242E+02;
    } else {
        return 8.0;
    }
}

// Blues - Green channel
float colormap_blues_green(float x) {
    if (x < 0.3725897611307026) {
        return -1.30453729372935E+02 * x + 2.51073069306930E+02;
    } else {
        return (-4.97095598364922E+01 * x - 1.77638812495581E+02) * x + 2.75554584848896E+02;
    }
}

// Blues - Blue channel
float colormap_blues_blue(float x) {
    if (x < 0.8782350698420436) {
        return (((-1.66242968759033E+02 * x + 2.50865766027010E+02) * x - 1.82046165445353E+02) * x - 3.29698266187334E+01) * x + 2.53927912915449E+02;
    } else {
        return -3.85153281423831E+02 * x + 4.93849833147981E+02;
    }
}

// Blues - Sequential color map
vec3 colormap_blues(float x) {
    float r = clamp(colormap_blues_red(x) / 255.0, 0.0, 1.0);
    float g = clamp(colormap_blues_green(x) / 255.0, 0.0, 1.0);
    float b = clamp(colormap_blues_blue(x) / 255.0, 0.0, 1.0);
    return vec3(r, g, b);
}

// Greens - Red channel
float colormap_greens_red(float x) {
    if (x < 0.6193682068820651) {
        return ((1.41021531432983E+02 * x - 3.78122271460656E+02) * x - 1.08403692154170E+02) * x + 2.45743977533647E+02;
    } else {
        return ((-8.63146749682724E+02 * x + 1.76195389457266E+03) * x - 1.43807716183136E+03) * x + 4.86922446232568E+02;
    }
}

// Greens - Green channel
float colormap_greens_green(float x) {
    return (-1.37013460576160E+02 * x - 4.54698187198101E+01) * x + 2.52098684286706E+02;
}

// Greens - Blue channel
float colormap_greens_blue(float x) {
    if (x < 0.5062477983469252) {
        return ((3.95067226937040E+02 * x - 4.52381961582927E+02) * x - 1.25304923569201E+02) * x + 2.43770002412197E+02;
    } else {
        return ((2.98249378459208E+02 * x - 6.14859580726999E+02) * x + 2.22299590241459E+02) * x + 1.21998454489668E+02;
    }
}

// Greens - Sequential color map
vec3 colormap_greens(float x) { float r = clamp(colormap_greens_red(x) / 255.0, 0.0, 1.0);
    float g = clamp(colormap_greens_green(x) / 255.0, 0.0, 1.0);
    float b = clamp(colormap_greens_blue(x) / 255.0, 0.0, 1.0);
    return vec3(r, g, b);
}

// Reds - Red channel
float colormap_reds_red(float x) {
    if (x < 0.7109796106815338) {
        return (((-9.58108609441667E+02 * x + 8.89060620527714E+02) * x - 2.42747192807495E+02) * x + 9.97906310565304E+00) * x + 2.54641252219400E+02;
    } else {
        return ((-9.93985373158007E+02 * x + 1.96524174972026E+03) * x - 1.54068189744713E+03) * x + 6.72947219603874E+02;
    }
}

// Reds - Green channel
float colormap_reds_green(float x) {
    if (x < 0.7679868638515472) {
        return ((((2.66433610509335E+03 * x - 5.05488641558587E+03) * x + 3.69542277742922E+03) * x - 1.36931912848446E+03) * x - 5.12669839132577E+01) * x + 2.41929417192750E+02;
    } else {
        return (-2.11738816337853E+02 * x + 2.78333107855597E+02) * x - 6.66958752910143E+01;
    }
}

// Reds - Blue channel
float colormap_reds_blue(float x) {
    return (((-6.83475279000297E+02 * x + 1.55250107598171E+03) * x - 9.25799053039285E+02) * x - 1.67380812671938E+02) * x + 2.37145226675143E+02;
}

// Reds - Sequential color map
vec3 colormap_reds(float x) {
    float r = clamp(colormap_reds_red(x) / 255.0, 0.0, 1.0);
    float g = clamp(colormap_reds_green(x) / 255.0, 0.0, 1.0);
    float b = clamp(colormap_reds_blue(x) / 255.0, 0.0, 1.0);
    return vec3(r, g, b);
}


// Rainbow discrete
vec3 colormap_rainbow18(float x) {
    float x16 = x * 16.0;
    const float s = 1.0 / 255.0;
    if (x16 < 1.0) {
        return vec3(150.0, 0.0, 150.0) * s;
    } else if (x16 < 2.0) {
        return vec3(200.0, 0.0, 200.0) * s;
    } else if (x16 < 3.0) {
        return vec3(100.0, 100.0, 150.0) * s;
    } else if (x16 < 4.0) {
        return vec3(100.0, 100.0, 200.0) * s;
    } else if (x16 < 5.0) {
        return vec3(100.0, 100.0, 255.0) * s;
    } else if (x16 < 6.0) {
        return vec3(0.0, 140.0, 0.0) * s;
    } else if (x16 < 7.0) {
        return vec3(150.0, 170.0, 0.0) *s;
    } else if (x16 < 8.0) {
        return vec3(200.0, 200.0, 0.0) * s;
    } else if (x16 < 9.0) {
        return vec3(150.0, 200.0, 0.0) * s;
    } else if (x16 < 10.0) {
        return vec3(200.0, 255.0, 120.0) * s;
    } else if (x16 < 11.0) {
        return vec3(255.0, 255.0, 0.0) * s;
    } else if (x16 < 12.0) {
        return vec3(255.0, 200.0, 0.0) * s;
    } else if (x16 < 13.0) {
        return vec3(255.0, 160.0, 0.0) * s;
    } else if (x16 < 14.0) {
        return vec3(255.0, 125.0, 0.0) * s;
    } else if (x16 < 15.0) {
        return vec3(200.0, 50.0, 100.0) * s;
    } else {
        return vec3(175.0, 50.0, 75.0) * s;
    }
}

// Cool - Red channel
float colormap_cool_red(float x) {
    return (1.0 + 1.0 / 63.0) * x - 1.0 / 63.0;
}

// Cool - Green channel
float colormap_cool_green(float x) {
    return -(1.0 + 1.0 / 63.0) * x + (1.0 + 1.0 / 63.0);
}

// Cool - Luminosity down and then up
vec3 colormap_cool(float x) {
    float r = clamp(colormap_cool_red(x), 0.0, 1.0);
    float g = clamp(colormap_cool_green(x), 0.0, 1.0);
    float b = 1.0;
    return vec3(r, g, b);
}


float colormap_seismic_f(float x) {
    return ((-2010.0 * x + 2502.5950459) * x - 481.763180924) / 255.0;
}

// Seismic - Red channel
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

// Seismic - Green channel
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

// Seismic - Blue channel
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

// Seismic - Diverging color map
vec3 colormap_seismic(float x) {
    return vec3(colormap_seismic_red(x), colormap_seismic_green(x), colormap_seismic_blue(x));
}


float colormap_f(float x) {
    return ((-9.93427e0 * x + 1.56301e1) * x + 2.44663e2 * x) / 255.0;
}

// Carnation - Blue channel
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

// Carnation - Green channel
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

// Carnation - Red channel
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

// Carnation - Diverging color map
vec3 colormap_carnation(float x) {
    return vec3(colormap_carnation_red(x), colormap_carnation_green(x), colormap_carnation_blue(x));
}

// Hot metal - Green channel
float colormap_hotmetal_green(float x) {
    if (x < 0.6) {
        return 0.0;
    } else if (x <= 0.95) {
        return ((x - 0.6) * 728.57) / 255.0;
    } else {
        return 1.0;
    }
}

// Hot metal - Red channel
float colormap_hotmetal_red(float x) {
    if (x < 0.0) {
        return 0.0;
    } else if (x <= 0.57147) {
        return 446.22 * x / 255.0;
    } else {
        return 1.0;
    }
}

// Hot metal - Sequential with increasing L*, but not linear
vec3 colormap_hotmetal(float x) {
    return vec3(colormap_hotmetal_red(x), colormap_hotmetal_green(x), 0.0);
}

// Regular rainbow - All around bad color map
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
        0 - reds
        1 - greens
        2 - blues
        3 - rainbow18
        4 - rainbow
        5 - seismic
        6 - carnation
        7 - hotmetal
        8 - cool
*/
vec3 colormap(int cmap, float value){
    cmap = cmap % 9;
    if (cmap == 0){
        return colormap_reds(value);
    } else if (cmap == 1){
        return colormap_greens(value);
    } else if (cmap == 2){
        return colormap_blues(value);
    } else if (cmap == 3){
        return colormap_rainbow18(value);
    }else if (cmap == 4){
        return colormap_rainbow(value);
    } else if (cmap == 5){
        return colormap_seismic(value);
    } else if (cmap == 6){
        return colormap_carnation(value);
    } else if (cmap == 7){
        return colormap_hotmetal(value);
    }else if (cmap == 8){
        return colormap_cool(value);
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
