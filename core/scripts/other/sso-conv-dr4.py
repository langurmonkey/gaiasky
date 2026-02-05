#!/usr/bin/env python

import json
import argparse
import numpy as np
from astropy.io import votable

# Unit conversion
AU_TO_KM = 149597870.7
# Max number of asteroids to process
N_MAX = 400000

class SSO(object):
    """
    Represents a Gaia Sky SSO object with the required 
    orbital parameter structure.
    """
    def __init__(self, name, epoch, ma, a_au, e, w, node, period, i, nobs, type):
        self.name = str(name)
        self.color = [0.4, 0.4, 1.0, 0.5]
        self.pointColor = [1.0, 1.0, 1.0, 0.2]
        self.parent = "dr4-asteroids-hook"
        self.archetype = "Orbit"
        self.provider = "gaiasky.data.orbit.OrbitalParametersProvider"
        self.ct = ["Asteroids", "Orbits"]
        self.n_observations = int(nobs)
        self.type = type
        
        # Gaia Sky expects semi-major axis in Kilometers
        self.orbit = {
            "epoch": float(epoch),
            "meananomaly": float(ma),
            "semimajoraxis": float(a_au * AU_TO_KM),
            "eccentricity": float(e),
            "argofpericenter": float(w),
            "ascendingnode": float(node),
            "period": float(period),
            "inclination": float(i)
        }
        self.onlybody = True
        self.newmethod = True

def process_to_json(input_votable, output_json):
    """
    Reads the VOTable and writes the Gaia Sky compatible JSON file.
    """
    print(f"[*] Reading VOTable: {input_votable}")
    try:
        vot = votable.parse(input_votable)
        data = vot.get_first_table().to_table()
    except Exception as e:
        print(f"[!] Error reading VOTable: {e}")
        return

    # Determine total objects to process
    N = min(N_MAX, len(data))
    print(f"[*] Processing {N} objects into Gaia Sky format...")

    objects_list = []
    
    # 1. Add the Hook/Group object required by Gaia Sky
    hook = {
        "name": "dr4-asteroids-hook",
        "position": [0.0, 0.0, 0.0],
        "ct": ["Asteroids"],
        "fadeout": [1e-5, 2e-4],
        "parent": "Universe",
        "archetype": "OrbitalElementsGroup"
    }
    objects_list.append(hook)

    # 2. Iterate through table rows
    # We use column names defined in the previous script
    for i in range(N):
        row = data[i]
        
        # Mapping table columns to SSO class
        # Note: epoch is taken directly from the 'epoch' column in the VOTable
        sso_bean = SSO(
            name=row['denomination'] if 'denomination' in row.colnames else f"Asteroid_{i}",
            epoch=row['epoch'],
            ma=row['meananomaly'],
            a_au=row['semimajoraxis'],
            e=row['eccentricity'],
            w=row['argofpericenter'],
            node=row['ascendingnode'],
            period=row['period'],
            i=row['inclination'],
            nobs=row['n_observations'],
            type='A' if int(row['type']) == 3 else 'C'
        )
        
        # Convert class instance to dictionary for JSON serialization
        objects_list.append(sso_bean.__dict__)

    # 3. Write the final JSON file
    print(f"[*] Writing to: {output_json}")
    with open(output_json, 'w') as f:
        json.dump({"objects": objects_list}, f, indent=2)

    print("[+] Gaia Sky JSON generation complete!")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert SSO VOTable to Gaia Sky JSON.")
    parser.add_argument("input", help="Input VOTable with orbital elements")
    parser.add_argument("output", help="Output JSON file path")
    
    args = parser.parse_args()
    process_to_json(args.input, args.output)
