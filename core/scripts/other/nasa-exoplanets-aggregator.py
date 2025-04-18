#!/usr/bin/python3

import argparse, os, sys, math, json, random
import numpy as np
from dataclasses import dataclass

# The input to this script is the Composite table of NASA expolanet archive.
# This table needs to be cross-matched with the full table to get the Gaia, HIP, HD and TIC ids,
# which are expected in columns 3, 4, 5, and 6 respectively.

colors = [
    [0.2588, 0.5216, 0.9569], # blue
    [0.8588, 0.1961, 0.2118], # red
    [0.9569, 0.7608, 0.0510], # yellow
    [0.2353, 0.7294, 0.3294], # green
    [1.0000, 0.4000, 1.0000], # pink
    [0.8512, 0.5960, 0.0000]  # orange
]

def clamp(x, minimum, maximum):
    return max(minimum, min(x, maximum))

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
    
def get_float(row, idx):
    return float(row[idx])

def get_string(row, idx):
    return row[idx]
    
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
            letters = set()

            for component in system_entry[1]:
                pl_letter = component[1][-1]
                letters.add(pl_letter)
                planets[pl_letter] = component

            system['star'] = star
            system['planets'] = planets
            system['all'] = system_entry[1]

            systems[system_name] = system

            print(f"System '{system_name}' has {len(planets)/2} planets (snum: {star[9]}, pnum: {star[10]}).")

    # Construct a JSON file for each system
    for system_name in systems:

        system = systems[system_name]
        json_list = []    

        # ======= Star
        star = system['star']
        smap = {}


        # id
        gaia = star[3]
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
        hip = star[4]
        if hip:
            names.append(hip)
        hd = star[5]
        if hd:
            names.append(hd)
        tic = star[6]
        if tic:
            names.append(tic)
        smap["names"] = names

        dist = float(star[33])

        # skip if there are no distances
        if math.isnan(dist):
            continue
        
        # color
        v = float(star[34])
        k = float(star[35])
        col = v - k
        if math.isnan(col):
            # default
            col = 0.656
        smap["colorBV"] = col

        # teff
        teff = float(star[25])
        if not math.isnan(teff):
            smap["tEff"] = teff

        # rgba
        if not math.isnan(teff):
            smap["color"] = teff_to_rgba(teff)

        # mag
        v_mag = float(star[34])
        k_mag = float(star[35])
        gaia_mag = float(star[36])

        mag = gaia_mag
        if math.isnan(mag):
            mag = v_mag
        if math.isnan(mag):
            mag = k_mag
        if math.isnan(mag):
            # default
            mag = 14.0
        smap["appMag"] = mag

        # component type
        smap["componentType"] = "Systems"

        # parent
        smap["parent"] = "Universe"

        # archetype
        smap["archetype"] = "Star"

        # coordinates
        ra = float(star[31])
        dec = float(star[32])

        if math.isnan(ra) or math.isnan(dec):
            continue

        coord = {}
        coord["impl"] = "gaiasky.util.coord.StaticCoordinates"
        coord["positionEquatorial"] = [ra, dec, dist]

        smap["coordinates"] = coord

        # other star attributes
        # radius
        rad = float(star[26])
        if not math.isnan(rad):
            smap["rad_Rsun"] = rad
        # mass
        mass = float(star[27])
        if math.isnan(mass):
            mass = random.uniform(0.7, 20.0)
        smap["mass_Msun"] = mass
        # metallicity
        met = float(star[28])
        if not math.isnan(met):
            smap["metallicity"] = met
        # spectral type
        sptype = star[24]
        if sptype:
            smap["sp_type"] = sptype
        # log(G)
        logg = float(star[30])
        if not math.isnan(logg):
            smap["log_G"] = logg
        # met
        met = float(star[28])
        if not math.isnan(met):
            smap["metallicity"] = met


        json_list.append(smap)

        # ======= Planets
        planets = system['planets']
        for planet_letter in planets:

            planet = planets[planet_letter]

            # ======= Planet
            plmap = {}

            # name
            plmap["name"] = planet[1]

            # color
            eqt = get_float(planet, 22)
            if not math.isnan(eqt):
                pl_color = teff_to_rgba(eqt)
            else:
                pl_color = [0.4, 0.5, 0.98, 1.0]
            plmap["color"] = pl_color

            # component type
            plmap["componentType"] = "Planets"

            # parent
            plmap["parent"] = names[0]

            # archetype
            plmap["archetype"] = "Planet"

            # size
            rade = get_float(planet, 15)
            radj = get_float(planet, 16)
            if not math.isnan(rade):
                radius = rade * 6378.0
            elif not math.isnan(radj):
                radius = radj * 71492.0
            else:
                radius = clamp(random.gauss(6000.0, 500.0), 450.0, 15000.0)
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
                "period": random.uniform(1.0, 50.0),
                "axialtilt": random.uniform(0.0, 50.0),
                "inclination": random.uniform(0.0, 10.0),
                "meridianangle": random.uniform(0.0, 180.0)
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
            masse = get_float(planet, 17)
            massj = get_float(planet, 18)
            if math.isnan(masse) and math.isnan(massj):
                masse = random.uniform(0.5, 500.0)
            elif not math.isnan(massj) and math.isnan(masse):
                masse = massj * 317.82838
            if not math.isnan(masse):
                plmap["mass_Mearth"] = masse
            if not math.isnan(massj):
                plmap["mass_Mjupiter"] = massj
            insol = get_float(planet, 21)
            if not math.isnan(insol):
                plmap["insol"] = insol


            # ======= Planet orbit
            plomap = {}

            # name
            plomap["name"] = planet[1] + " orbit"

            # color
            orb_color = colors[random.randint(0, 5)]
            plomap["color"] = [orb_color[0], orb_color[1], orb_color[2], 0.6]

            # component types
            plomap["componentTypes"] = [ "Orbits", "Planets" ]

            # parent
            plomap["parent"] = names[0]

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
            period = get_float(planet, 13)
            sma = get_float(planet, 14) * au_km

            if math.isnan(sma) and math.isnan(period):
                # both are nan, invent period
                period = random.uniform(10.0, 2000.0)

            if math.isnan(sma) and not math.isnan(period):
                # compute sma from period and masses using Kepler's third law
                mp_kg = masse * mearth_kg
                ms_kg = mass * msun_kg 
                sma = ((math.pow(period, 2.0) * G * (mp_kg + ms_kg)) / (4 * math.pow(math.pi, 2.0))) ** (1.0 / 3.0)
            elif math.isnan(period) and not math.isnan(sma):
                # compute period from sma and masses using Kepler's third law
                mp_kg = masse * mearth_kg
                ms_kg = mass * msun_kg
                period = ((math.pow(sma, 3.0) * 4 * math.pow(math.pi, 2.0)) / (G * (mp_kg + ms_kg))) ** (1.0 / 2.0)
                # seconds to days
                period = period / 86400.0

            orbit["period"] = period
            orbit["semimajoraxis"] = sma
            # epoch is J2010.0
            epoch = 2455197.5
            orbit["epoch"] = epoch
            # eccentricity
            e = get_float(planet, 20)
            if math.isnan(e):
                e = random.uniform(0.0, 0.8)
            orbit["eccentricity"] = e
            # inclination
            i = random.gauss(0.0, 4.0)
            orbit["inclination"] = i
            # ascending node
            anode = random.uniform(0.0, 180.0)
            orbit["ascendingnode"] = anode
            # argument of pericenter
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
        filename = star[2] + ".json"
        abspath = os.path.join(args.DEST, filename)

        print(f"Writing file: {abspath}")

        with open(abspath, 'w', encoding='utf-8') as f:
            json.dump(json_data, f, indent=4)

if __name__ == "__main__":
    main()
