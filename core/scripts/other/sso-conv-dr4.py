#!/usr/bin/env python

import json
import argparse
import numpy as np
from astropy.io import votable

# Unit conversion
AU_TO_KM = 149597870.7
# Max number of asteroids to process
N_MAX = 400000

# Valid body representation values
BODY_REPRESENTATIONS = ["ONLY_ORBIT", "ONLY_BODY", "BODY_AND_ORBIT"]

# Flexible column name candidates (first match wins)
COL_NAME        = ["denomination", "name", "designation", "object_name"]
COL_EPOCH       = ["epoch", "epoch_jd", "epochjd", "epoch_state_vector"]
COL_MEANANOMALY = ["meananomaly", "mean_anomaly", "ma", "man"]
COL_SMA         = ["semimajoraxis", "semi_major_axis", "sma", "semimajax", "a"]
COL_ECC         = ["eccentricity", "ecc", "e"]
COL_ARGPERI     = ["argofpericenter", "arg_of_pericenter", "argperi", "aop", "aperi", "w"]
COL_ASCNODE     = ["ascendingnode", "ascending_node", "ascnode", "an", "node"]
COL_PERIOD      = ["period", "orbital_period", "orb_period"]
COL_INC         = ["inclination", "inc", "i"]
COL_NOBS        = ["n_observations", "nobs", "num_obs", "n_obs"]
COL_TYPE        = ["type", "object_type", "typeShort", "type_short"]


def find_col(colnames, candidates):
    """
    Returns the first candidate column name found in colnames (case-insensitive),
    or None if none match.
    """
    lower = {c.lower(): c for c in colnames}
    for candidate in candidates:
        if candidate.lower() in lower:
            return lower[candidate.lower()]
    return None


def get_col(row, colnames, candidates, default=None):
    """
    Retrieves the value from a row for the first matching candidate column.
    Returns default if no column is found.
    """
    col = find_col(colnames, candidates)
    if col is not None:
        return row[col]
    return default


class SSO(object):
    """
    Represents a Gaia Sky SSO object with the required 
    orbital parameter structure.
    """
    def __init__(self, name, epoch, ma, a_au, e, w, node, period, i, nobs, type,
                 hook_name, body_representation):
        self.name = str(name)
        self.color = [0.4, 0.4, 1.0, 0.5]
        if body_representation != "ONLY_ORBIT":
            self.pointColor = [1.0, 1.0, 1.0, 0.2]
        self.parent = hook_name
        self.archetype = "Orbit"
        self.provider = "gaiasky.data.orbit.OrbitalParametersProvider"
        self.ct = ["Asteroids", "Orbits"]
        self.bodyRepresentation = body_representation

        if nobs is not None:
            self.n_observations = int(nobs)

        if type is not None:
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
        self.newMethod = True


def resolve_type(row, colnames):
    """
    Resolves the object type string from the type column if present.
    Returns 'A' for type code 3 (Asteroid), 'C' otherwise, or None if absent.
    """
    val = get_col(row, colnames, COL_TYPE, default=None)
    if val is None:
        return None
    try:
        return 'A' if int(val) == 3 else 'C'
    except (ValueError, TypeError):
        # Value is already a string like 'A' or 'C'
        return str(val).strip()


def process_to_json(input_votable, output_json, hook_name, body_representation, name_suffix):
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

    colnames = data.colnames

    # Warn about missing optional columns
    if find_col(colnames, COL_NOBS) is None:
        print("[~] Warning: no n_observations column found, field will be omitted.")
    if find_col(colnames, COL_TYPE) is None:
        print("[~] Warning: no type column found, field will be omitted.")

    # Abort if any required column is missing
    required = {
        "epoch":           COL_EPOCH,
        "meananomaly":     COL_MEANANOMALY,
        "semimajoraxis":   COL_SMA,
        "eccentricity":    COL_ECC,
        "argofpericenter": COL_ARGPERI,
        "ascendingnode":   COL_ASCNODE,
        "period":          COL_PERIOD,
        "inclination":     COL_INC,
    }
    missing = [label for label, cands in required.items() if find_col(colnames, cands) is None]
    if missing:
        print(f"[!] Missing required columns: {', '.join(missing)}. Aborting.")
        return

    # Determine total objects to process
    N = min(N_MAX, len(data))
    print(f"[*] Processing {N} objects into Gaia Sky format...")
    print(f"[*] Hook object name: {hook_name}")
    print(f"[*] Body representation: {body_representation}")
    print(f"[*] Name suffix: {name_suffix}")

    objects_list = []

    # 1. Add the Hook/Group object required by Gaia Sky
    hook = {
        "name": f"{hook_name}",
        "position": [0.0, 0.0, 0.0],
        "ct": ["Asteroids"],
        "fadeout": [1e-5, 2e-4],
        "parent": "Universe",
        "archetype": "OrbitalElementsGroup"
    }
    objects_list.append(hook)

    # 2. Iterate through table rows
    for i in range(N):
        row = data[i]

        nobs = get_col(row, colnames, COL_NOBS, default=None)
        obj_type = resolve_type(row, colnames)

        sso_name = get_col(row, colnames, COL_NAME, default=f"Asteroid_{i}")
        sso_bean = SSO(
            name=f"{sso_name}{name_suffix}",
            epoch=get_col(row, colnames, COL_EPOCH),
            ma=get_col(row, colnames, COL_MEANANOMALY),
            a_au=get_col(row, colnames, COL_SMA),
            e=get_col(row, colnames, COL_ECC),
            w=get_col(row, colnames, COL_ARGPERI),
            node=get_col(row, colnames, COL_ASCNODE),
            period=get_col(row, colnames, COL_PERIOD),
            i=get_col(row, colnames, COL_INC),
            nobs=nobs,
            type=obj_type,
            hook_name=hook_name,
            body_representation=body_representation,
        )

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
    parser.add_argument(
        "--hook-name",
        default="dr4-asteroids-hook",
        help="Name of the hook/group object (default: dr4-asteroids-hook)"
    )
    parser.add_argument(
        "--body-representation",
        default="ONLY_ORBIT",
        choices=BODY_REPRESENTATIONS,
        help="Body representation mode (default: ONLY_ORBIT)"
    )
    parser.add_argument(
        "--name-suffix",
        default="",
        help="Suffix to add to object names (default: none)"
    )

    args = parser.parse_args()
    process_to_json(args.input, args.output, args.hook_name, args.body_representation, args.name_suffix)
