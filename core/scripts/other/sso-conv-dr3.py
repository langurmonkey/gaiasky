#!/usr/bin/env python

import json, io, re, math

# Max number of asteroids to process
N_MAX = 180000
# Ref epoch in jd: Jan 1 2010
REF_EPOCH = 2455197.5
# Unit conversion
AU_TO_KM = 149598000
Y_TO_D = 365.25 
# Standard gravitational parameter of the Sun
GM_SUN = 1.32712440019e20

class SSO(object):
    def __init__(self, name, color, epoch, meananomaly, semimajoraxis, eccentricity, argofpericenter, ascendingnode, period, inclination):
        self.name = name
        self.color = color
        self.parent = "dr3-asteroids-hook"
        self.impl = "gaiasky.scenegraph.Orbit"
        self.provider = "gaiasky.data.orbit.OrbitalParametersProvider"
        self.ct = [ "Asteroids", "Orbits" ]
        self.transformFunction = "eclipticToEquatorial"
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
    name = values[2]
    color = [0.4, 0.4, 1.0, 0.5]
    # Epoch in julain days since Jan 1 2010
    jdjan12010 = values[3]
    epoch = float(jdjan12010) + REF_EPOCH
    # Mean anomaly [deg]
    meananomaly = float(values[9])
    # Semimajor axis [Km]
    a_au = float(values[4])
    semimajoraxis = a_au * AU_TO_KM
    # Eccentricity
    eccentricity = float(values[5])
    # Argument of pericenter [deg]
    argofpericenter = float(values[8])
    # Ascending node [deg]
    ascendingnode = float(values[7])
    # Period in days
    period = pow(a_au, 1.5) * Y_TO_D
    # Inclination [deg]
    inclination = float(values[6])
    
    bean = SSO(name, color, epoch, meananomaly, semimajoraxis, eccentricity, argofpericenter, ascendingnode, period, inclination)
    
    jsonstr = json.dumps(bean.__dict__)
    
    return jsonstr
    

with open('/media/tsagrista/Daten/Gaia/data/sso/dr3-sso.csv', 'r') as fr:
    lines = fr.readlines()
    with open('/tmp/sso-dr3.json', 'w') as fw:
        fw.write("{\"objects\" : [\n")
        fw.write("{ \"name\" : \"dr3-asteroids-hook\", \"position\" : [0.0, 0.0, 0.0], \"ct\" : [\"Asteroids\"], \"fadeout\" : [1e-5, 2e-4], \"parent\" : \"Universe\", \"impl\" : \"gaiasky.scenegraph.OrbitalElementsGroup\" },\n")

        N = min(N_MAX, len(lines))
        for idx, line in enumerate(lines[1:N]):
            if line.strip():
                jsonstring = to_json(line, idx)
                fw.write(jsonstring)
                if idx < N - 2:
                    fw.write(",")
                fw.write("\n")

        fw.write("]}")
