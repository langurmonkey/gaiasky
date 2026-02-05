#!/usr/bin/env python

import json, io, re, math

# Max number of asteroids to process
N_MAX = 180000
# Ref epoch in jd: Jan 1 2010
REF_EPOCH = 2455197.5
# Unit conversion
AU_TO_KM = 149597870.7
Y_TO_D = 365.25 
# Standard gravitational parameter of the Sun
GM_SUN = 1.32712440019e20

class SSO(object):
    def __init__(self, name, color, epoch, meananomaly, semimajoraxis, eccentricity, argofpericenter, ascendingnode, period, inclination):
        self.name = name
        self.color = color
        self.pointColor = [1.0, 1.0, 1.0, 0.2]
        self.parent = "fpr-asteroids-hook"
        self.archetype = "Orbit"
        self.provider = "gaiasky.data.orbit.OrbitalParametersProvider"
        self.transformFunction = "eclipticToEquatorial"
        self.ct = [ "Asteroids", "Orbits" ]
        self.orbit = {}
        self.orbit["epoch"] = epoch
        self.orbit["meananomaly"] = meananomaly
        self.orbit["semimajoraxis"] = semimajoraxis
        self.orbit["eccentricity"] = eccentricity
        self.orbit["argofpericenter"] = argofpericenter
        self.orbit["ascendingnode"] = ascendingnode
        self.orbit["period"] = period
        self.orbit["inclination"] = inclination
        self.onlybody = True
        self.newmethod = True
        

def to_json(line, idx):
    values = line.split(',')
    # Designation
    name = values[1]
    color = [0.4, 0.4, 1.0, 0.5]
    # Epoch in julain days since Jan 1 2010
    jdjan12010 = values[2]
    epoch = float(jdjan12010) + REF_EPOCH
    # Mean anomaly [deg]
    meananomaly = float(values[8])
    # Semimajor axis [Km]
    a_au = float(values[3])
    semimajoraxis = a_au * AU_TO_KM
    # Eccentricity
    eccentricity = float(values[4])
    # Argument of pericenter [deg]
    argofpericenter = float(values[7])
    # Ascending node [deg]
    ascendingnode = float(values[6])
    # Period in days
    period = pow(a_au, 1.5) * Y_TO_D
    # Inclination [deg]
    inclination = float(values[5])

    bean = SSO(name, color, epoch, meananomaly, semimajoraxis, eccentricity, argofpericenter, ascendingnode, period, inclination)
    
    jsonstr = json.dumps(bean.__dict__)
    
    return jsonstr

#file = '/media/tsagrista/Daten/Gaia/data/sso/FPR/fpr-00-20230702/FPR-SSO-result.elements.csv'
file = '/media/tsagrista/Daten/Gaia/data/sso/FPR/fpr-01-20230911/all.source.fpr.csv'

with open(file, 'r') as fr:
    lines = fr.readlines()
    with open('/tmp/sso-fpr.json', 'w') as fw:
        fw.write("{\"objects\" : [\n")
        fw.write("{ \"name\" : \"fpr-asteroids-hook\", \"position\" : [0.0, 0.0, 0.0], \"ct\" : [\"Asteroids\"], \"fadeout\" : [1e-5, 2e-4], \"parent\" : \"Universe\", \"archetype\" : \"OrbitalElementsGroup\" },\n")

        N = min(N_MAX, len(lines))
        # Skip first line (header)
        for idx, line in enumerate(lines[1:N]):
            if line.strip():
                jsonstring = to_json(line, idx)
                fw.write(jsonstring)
                if idx < N - 2:
                    fw.write(",")
                fw.write("\n")

        fw.write("]}")
