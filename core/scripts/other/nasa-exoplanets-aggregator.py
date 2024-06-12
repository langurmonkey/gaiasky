#!/usr/bin/python3

import argparse, os, sys, math, json, random
import numpy as np
from dataclasses import dataclass

def get_biome_lut():
    i = random.randint(0, 4)
    file = "biome-lut.png"
    if i == 0:
        file = "biome-lut.png"
    elif i == 1:
        file = "biome-smooth-lut.png"
    elif i == 2:
        file = "brown-green-lut.png"
    elif i == 3:
        file = "psycho-smooth-lut.png"
    elif i == 4:
        file = "rock-smooth-lut.png"
    return "$data/default-data/tex/base/" + file

def get_noise_type():
    i = random.randint(0, 3)
    if i == 0:
        return "gradval"
    if i == 1:
        return "perlin"
    if i == 2: 
        return "simplex"
    if i == 3:
        return "value"

def get_fractal_type():
    i = random.randint(0, 3)
    if i == 0:
        return "fbm"
    if i == 1:
        return "ridgemulti"
    if i == 2: 
        return "billow"
    if i == 3:
        return "multi"

def check_file_exists(file_path):
    if not os.path.isfile(file_path):
        print(f"Error: The file '{file_path}' does not exist.")
        sys.exit(1)

def check_directory_exists(dir_path):
    if not os.path.isdir(dir_path):
        print(f"Error: The directory '{dir_path}' does not exist.")
        sys.exit(1)

def correct_gamma(clinear: float):
    result = 0.0
    if clinear <= 0.0031308:
        result = 12.92 * clinear
    else:
        # use 0.05 for pale colors, 0.5 for vivid colors
        a = 0.5
        result = float(((1 + a) * math.pow(clinear, 1 / 2.4) - a))
    return result

def clamp(n, min, max): 
    if n < min: 
        return min
    elif n > max: 
        return max
    else: 
        return n

def bv_to_rgba(bv): 
    t = 4600 * ((1 / ((0.92 * bv) + 1.7)) + (1 / ((0.92 * bv) + 0.62)))
    # t to xyY
    x = 0
    y = 0

    if t >= 1667 and t <= 4000:
        x = ((-0.2661239 * math.pow(10, 9)) / math.pow(t, 3)) + ((-0.2343580 * math.pow(10, 6)) / math.pow(t, 2)) + ((0.8776956 * math.pow(10, 3)) / t) + 0.179910
    elif t > 4000 and t <= 25000:
        x = ((-3.0258469 * math.pow(10, 9)) / math.pow(t, 3)) + ((2.1070379 * math.pow(10, 6)) / math.pow(t, 2)) + ((0.2226347 * math.pow(10, 3)) / t) + 0.240390
    

    if t >= 1667 and t <= 2222:
        y = -1.1063814 * math.pow(x, 3) - 1.34811020 * math.pow(x, 2) + 2.18555832 * x - 0.20219683
    elif t > 2222 and t <= 4000:
        y = -0.9549476 * math.pow(x, 3) - 1.37418593 * math.pow(x, 2) + 2.09137015 * x - 0.16748867
    elif t > 4000 and t <= 25000:
        y = 3.0817580 * math.pow(x, 3) - 5.87338670 * math.pow(x, 2) + 3.75112997 * x - 0.37001483

    # xyY to XYZ, Y = 1
    Y = 0 if (y == 0) else 1
    X = 0 if (y == 0) else (x * Y) / y
    Z = 0 if (y == 0) else ((1 - x - y) * Y) / y

    cc = [0.0, 0.0, 0.0, 1.0]
    cc[0] = correct_gamma(3.2406 * X - 1.5372 * Y - 0.4986 * Z)
    cc[1] = correct_gamma(-0.9689 * X + 1.8758 * Y + 0.0415 * Z)
    cc[2] = correct_gamma(0.0557 * X - 0.2040 * Y + 1.0570 * Z)

    mx = max(1.0, max(cc[2], max(cc[0], cc[1])))

    cc[0] = max(cc[0] / mx, 0.0)
    cc[1] = max(cc[1] / mx, 0.0)
    cc[2] = max(cc[2] / mx, 0.0)

    return cc

def teff_to_rgba(teff):
    temp = teff / 100.0

    # Red
    if temp <= 66:
        r = 255
    else: 
        x = temp - 55
        r = 351.97690566805693 + 0.114206453784165 * x - 40.25366309332127 * math.log(x)
    r = clamp(r, 0, 255)

    # Green
    if temp <= 66:
        x = max(0.01, temp - 2)
        g = -155.25485562709179 - 0.44596950469579133 * x + 104.49216199393888 * math.log(x)
    else:
        x = temp - 50
        g = 325.4494125711974 + 0.07943456536662342 * x - 28.0852963507957 * math.log(x)
    g = clamp(g, 0, 255)

    # Blue
    if temp >= 66:
        b = 255
    else:
        if temp <= 19:
            b = 0
        else:
            x = max(0.01, temp - 10)
            b = -254.76935184120902 + 0.8274096064007395 * x + 115.67994401066147 * math.log(x)
    b = clamp(b, 0, 255)

    return [r / 255.0, g / 255.0, b / 255.0, 1.0]
    
def get_float(planet, candidates, idx):
    value = float(planet[idx])
    if math.isnan(value):
        # try candidates
        for candidate in candidates:
            value = float(candidate[idx])
            if not math.isnan(value):
                return value
    return value

def get_string(planet, candidates, idx):
    value = planet[idx]
    if not value:
        # try candidates
        for candidate in candidates:
            value = candidate[idx]
            if value:
                return value
    return value
    
# constants
mearth_kg = 5.976E24
msun_kg = 1.9884E30
G = 6.6743015E-11
au_km = 149597870.7

def main():
    parser = argparse.ArgumentParser(description='Generate Gaia Sky JSON descriptor files for NASA exoplanets. The script generates one JSON per system.')
    parser.add_argument('FILE', type=str, help='The input VOTable file path.')
    parser.add_argument('DEST', type=str, help='The destination directory, where the JSON files will be saved.')

    args = parser.parse_args()

    check_file_exists(args.FILE)
    check_directory_exists(args.DEST)

    print(f"Loading file: {os.path.basename(args.FILE)}")

    from astropy.io.votable import parse_single_table

    table = parse_single_table(args.FILE)
    data = table.array

    # The dict systems has a list with all entries for a given hostname.
    system_map = {}
    n_rows = table.nrows
    for row in np.arange(n_rows):
        hostname = data['hostname'][row]
        if system_map.get(hostname) is None:
            system_map[hostname] = [data[row]]
        else:
            system_map[hostname].append(data[row])


    # Array with all systems
    systems = {}
    for system_entry in system_map.items():
        if len(system_entry) > 0:
            system = {}
            system_name = system_entry[0]
            # Star
            star = system_entry[1][0]
            planets = {}
            letters = []
            # first, get planets with default_flag = 1 (index 8)
            for component in system_entry[1]:
                pl_letter = component[3]
                letters.append(pl_letter)
                if planets.get("#" + pl_letter) is None:
                    planets["#" + pl_letter] = []

                # append all to *letter
                planets["#" + pl_letter].append(component)

                # save default to letter
                if int(component[8]) == 1:
                    planets[pl_letter] = component

            # if we have no planet with default_flag=1, just get first
            for pl_letter in letters:
                if planets.get(pl_letter) is None:
                    for component in system_entry[1]:
                        if pl_letter == component[3]:
                            planets[pl_letter] = component


            system['star'] = star
            system['planets'] = planets
            system['all'] = system_entry[1]

            systems[system_name] = system

            print(f"System '{system_name}' has {len(planets)} planets (snum: {star[9]}, pnum: {star[10]}).")

    # Construct a JSON file for each system
    for system_name in systems:

        system = systems[system_name]
        json_list = []    

        # ======= Star
        star = system['star']
        smap = {}


        # id
        gaia = star[7]
        if gaia:
            tokens = gaia.split()
            if len(tokens) >= 3:
                sourceid = int(tokens[2])
                smap["id"] = sourceid

        # names
        names = []
        names.append(star[2])
        if gaia:
            names.append(gaia)
        hip = star[5]
        if hip:
            names.append(hip)
        hd = star[4]
        if hd:
            names.append(hd)
        tic = star[6]
        if tic:
            names.append(tic)
        smap["names"] = names

        dist = float(star[92])
        plx = float(star[93])

        # skip if there are no distances
        if math.isnan(dist) and math.isnan(plx):
            continue

        # compute distance from parallax if needed
        if math.isnan(dist):
            dist = plx / 1000.0
        
        # color
        b = float(star[94])
        v = float(star[95])
        j = float(star[96])
        h = float(star[97])
        k = float(star[98])
        col = b - v
        if math.isnan(col):
            col = v - j
        if math.isnan(col):
            col = v - h
        if math.isnan(col):
            col = j - h
        if math.isnan(col):
            col = h - k
        if math.isnan(col):
            # default
            col = 0.656
        smap["colorBV"] = col

        # teff
        teff = float(star[68])
        if not math.isnan(teff):
            smap["tEff"] = teff

        # rgba
        if math.isnan(teff):
            smap["color"] = bv_to_rgba(col)
        else:
            smap["color"] = teff_to_rgba(teff)

        # mag
        gaia_mag = float(star[108])
        g_mag = float(star[100])
        t_mag = float(star[110])
        kep_mag = float(star[111])

        mag = gaia_mag
        if math.isnan(mag):
            mag = g_mag
        if math.isnan(mag):
            mag = t_mag
        if math.isnan(mag):
            mag = kep_mag
        if math.isnan(mag):
            mag = b
        if math.isnan(mag):
            mag = v
        if math.isnan(mag):
            # default
            mag = 14.0
        smap["appMag"] = mag

        # component type
        smap["componentType"] = "Stars"

        # parent
        smap["parent"] = "Universe"

        # archetype
        smap["archetype"] = "Star"

        # coordinates
        ra = float(star[82])
        dec = float(star[84])

        if math.isnan(ra) or math.isnan(dec):
            continue

        coord = {}
        coord["impl"] = "gaiasky.util.coord.StaticCoordinates"
        coord["positionEquatorial"] = [ra, dec, dist]

        smap["coordinates"] = coord

        # other star attributes
        # radius
        rad = float(star[69])
        if not math.isnan(rad):
            smap["rad_Rsun"] = rad
        # mass
        mass = float(star[70])
        if math.isnan(mass):
            mass = random.uniform(0.7, 20.0)
        smap["mass_Msun"] = mass
        # metallicity
        met = float(star[71])
        if not math.isnan(met):
            smap["metallicity"] = met
        # luminosity
        lum = float(star[73])
        if not math.isnan(lum):
            smap["luminosity"] = lum
        # log(G)
        logg = float(star[74])
        if not math.isnan(logg):
            smap["log_G"] = logg
        # age
        age = float(star[75])
        if not math.isnan(age):
            smap["age"] = age
        # density
        dens = float(star[76])
        if not math.isnan(dens):
            smap["density"] = dens


        json_list.append(smap)

        # ======= Planets
        planets = system['planets']
        for planet_letter in planets:

            if not planet_letter.startswith("#"):
                planet = planets[planet_letter]
                candidates = planets["#" + planet_letter]

                # ======= Planet
                plmap = {}

                # name
                plmap["name"] = planet[1]

                # color
                eqt = get_float(planet, candidates, 50)
                if not math.isnan(eqt):
                    pl_color = teff_to_rgba(eqt)
                else:
                    pl_color = [0.4, 0.5, 0.98, 1.0]
                plmap["color"] = pl_color

                # component type
                plmap["componentType"] = "Planets"

                # parent
                plmap["parent"] = star[2]

                # archetype
                plmap["archetype"] = "Planet"

                # size
                rade = get_float(planet, candidates, 36)
                radj = get_float(planet, candidates, 37)
                if not math.isnan(rade):
                    radius = rade * 6378.0
                elif not math.isnan(radj):
                    radius = radj * 71492.0
                else:
                    radius = 20000.0
                plmap["size"] = radius

                # absmag
                plmap["absMag"] = random.uniform(15.0, 29.0)

                # coordinates
                coord = {
                    "impl" : "gaiasky.util.coord.OrbitLintCoordinates",
                    "orbitname" : planet[1] + " orbit"
                }
                plmap["coordinates"] = coord

                # rigid rotation
                rot = {
                    "period": 5.0,
                    "axialtilt": 5.0,
                    "inclination": 8.0,
                    "meridianangle": 150.0
                }
                plmap["rotation"] = rot

                # randomize
                randomize = ["model", "cloud", "atmosphere"]
                plmap["randomize"] = randomize

                # seeds
                seeds = [random.randint(-999999, 999999), random.randint(-999999, 999999), random.randint(-999999, 999999)]
                plmap["seed"] = seeds


                # other planet attributes
                # radius (Earth)
                if not math.isnan(rade):
                    plmap["radius_Rearth"] = rade
                # radius (Jupiter)
                if not math.isnan(radj):
                    plmap["radius_Rjupiter"] = radj
                # mass (in Earth and Jupiter units)
                masse = get_float(planet, candidates, 38)
                massj = get_float(planet, candidates, 39)
                if math.isnan(masse) and math.isnan(massj):
                    masse = random.uniform(0.5, 500.0)
                elif not math.isnan(massj) and math.isnan(masse):
                    masse = massj * 317.82838
                if not math.isnan(masse):
                    plmap["mass_Mearth"] = masse
                if not math.isnan(massj):
                    plmap["mass_Mjupiter"] = massj
                dens = get_float(planet, candidates, 47)
                if not math.isnan(dens):
                    plmap["density"] = dens


                # ======= Planet orbit
                plomap = {}

                # name
                plomap["name"] = planet[1] + " orbit"

                # color
                plomap["color"] = pl_color

                # component types
                plomap["componentTypes"] = [ "Orbits", "Planets" ]

                # parent
                plomap["parent"] = star[2]

                # archetype
                plomap["archetype"] = "Orbit"

                # provider
                plomap["provider"] = "gaiasky.data.orbit.OrbitalParametersProvider"

                # model
                plomap["model"] = "extrasolar_system"

                # method
                plomap["newMethod"] = True

                # orbital elements
                orbit = {}
                # period and semi-major axis
                period = get_float(planet, candidates, 34)
                sma = get_float(planet, candidates, 35) * au_km

                if math.isnan(sma) and math.isnan(period):
                    # both are nan, invent period
                    period = random.uniform(10.0, 2000.0)

                if math.isnan(sma) and not math.isnan(period):
                    # compute sma from period and masses using Kepler's third law
                    mp_kg = masse * mearth_kg
                    ms_kg = mass * msun_kg 
                    sma = ((math.pow(period, 2.0) * 4 * math.pow(math.pi, 2.0)) / (G * (mp_kg + ms_kg))) ** (1.0 / 3.0)
                    # m to km
                    sma = sma / 1000.0
                elif math.isnan(period) and not math.isnan(sma):
                    # compute period from sma and masses using Kepler's third law
                    mp_kg = masse * mearth_kg
                    ms_kg = mass * msun_kg
                    period = ((math.pow(sma, 3.0) * G * (mp_kg + ms_kg)) / (4 * math.pow(math.pi, 2.0))) ** (1.0 / 2.0)
                    # seconds to days
                    period = period / 86400.0

                orbit["period"] = period
                orbit["semimajoraxis"] = sma
                # epoch is J2010.0
                epoch = 2455197.5
                orbit["epoch"] = epoch
                # eccentricity
                e = get_float(planet, candidates, 48)
                if math.isnan(e):
                    e = random.uniform(0.0, 0.4)
                orbit["eccentricity"] = e
                # inclination
                i = get_float(planet, candidates, 51)
                if math.isnan(i):
                    i = random.uniform(0.0, 40.0)
                orbit["inclination"] = i
                # ascending node
                anode = random.uniform(0.0, 180.0)
                orbit["ascendingnode"] = anode
                # argument of pericenter
                argper = get_float(planet, candidates, 62)
                if math.isnan(argper):
                    argper = random.uniform(0.0, 90.0)
                orbit["argofpericenter"] = argper
                # mean anomaly
                orbit["meananomaly"] = 0.0

                plomap["orbit"] = orbit

                # add to JSON objects list
                json_list.append(plomap)
                json_list.append(plmap)


        # final data object
        json_data = {
            "objects": json_list
        }

        # Save to file
        filename = star[2].replace(" ", "_") + "-system.json"
        abspath = os.path.join(args.DEST, filename)

        print(f"Writing file: {abspath}")

        with open(abspath, 'w', encoding='utf-8') as f:
            json.dump(json_data, f, indent=4)

if __name__ == "__main__":
    main()
