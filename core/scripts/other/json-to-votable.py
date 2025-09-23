#! python
"""
This script reads JSON files representing planetary systems from the 'systems' directory,
extracts information about each system’s barycenter and its constituent objects, and writes
the results to a VO-compliant VOTable. For each system, it records:

- A cleaned system name (removing the "Gaia " prefix and " system" suffix)
- Equatorial coordinates: RA (alpha) and Dec (delta) in degrees
- Distance in parsecs
- Number of black holes (non-orbit objects with names ending in 'BH')
- Number of stars (non-orbit objects with names ending in 'star')

The VOTable includes proper units and UCD metadata for RA, Dec, and distance, making it
compatible with VO tools like TOPCAT and Aladin.
"""

import json
import os
from astropy.io.votable import from_table
import astropy.table

# Directory containing the JSON system files
INPUT_DIR = "systems"
# Output VOTable filename
OUTPUT_FILE = "dr4-black-holes.vot"

obj_num = 0
system_num = 0

def get_names(obj):
    """Return list of names for an object, handling both 'names' and 'name' keys."""
    if "names" in obj:
        return obj["names"]
    elif "name" in obj:
        return [obj["name"]]
    else:
        return []

def clean_system_name(raw_name: str) -> str:
    """
    Remove unwanted prefixes/suffixes from system names.
    Return the cleaned name, the file name, and the source_id.    
    """
    if raw_name is None:
        return None
    name = raw_name.strip()
    # Remove the trailing " system" if present
    if name.endswith(" system"):
        name = name[: -len(" system")]
    # Remove the leading "Gaia " if present
    if name.startswith("Gaia DR4 "):
        name = name[len("Gaia DR4 "):]

    return "BH *" + name[-6:], "gaia-BH-" + name, int(name)

def extract_system_info(filepath):
    """
    Extract system information from a single JSON file.

    Returns a dictionary containing:
    - id: Gaia source_id of the star
    - name: cleaned system name
    - ra: right ascension in degrees
    - dec: declination in degrees
    - distance: distance in parsecs
    - n_black_holes: number of black holes in the system
    - n_stars: number of stars in the system
    """
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)

    # The objects are stored in the top-level "objects" array
    objects = data.get("objects", [])
    id = -1
    system_name = None
    file_name = None
    coords = (None, None, None)  # RA, Dec, distance
    black_holes = 0
    stars = 0
    global obj_num

    # Iterate through all objects in the system
    for obj in objects:
        names = get_names(obj)
        obj_num += 1
        print(f" - {obj_num}. Object {names[0]}")  # Debug: print current object

        # Identify barycenter: archetype Invisible and contains "system" in name
        if obj.get("archetype") == "Invisible" and any("system" in n for n in names):
            system_name, file_name, id = clean_system_name(names[0])
            coords = tuple(obj["coordinates"]["positionEquatorial"])

        # Count black holes (non-Orbit objects with names ending in 'BH')
        if any(n.endswith("BH") for n in names):
            if obj.get("archetype") != "Orbit":
                black_holes += 1

        # Count stars (non-Orbit objects with names ending in 'star')
        if any(n.endswith("star") for n in names):
            if obj.get("archetype") != "Orbit":
                stars += 1

    return {
        "id": id,
        "name": system_name + "|" + file_name,
        "ra": coords[0],
        "dec": coords[1],
        "distance": coords[2],
        "n_black_holes": black_holes,
        "n_stars": stars
    }

def main():
    """Main function: process all JSON files and write a VO-compliant VOTable."""
    rows = []
    global system_num
    # Iterate through all JSON files in the input directory
    for fname in os.listdir(INPUT_DIR):
        if fname.endswith(".json"):
            filepath = os.path.join(INPUT_DIR, fname)
            system_num += 1
            print(f"{system_num}. Processing {filepath}")
            info = extract_system_info(filepath)
            # Only add entries with a valid system name
            if info["name"]:
                rows.append(info)

    # Build an Astropy Table from the collected data
    table = astropy.table.Table(
        rows=rows,
        names=["id", "name", "ra", "dec", "distance", "n_black_holes", "n_stars"],
        dtype=["i8", "str", "f8", "f8", "f8", "i4", "i4"]
    )

    # Add metadata for VO tools (units + UCDs)
    table["id"].meta["ucd"] = "meta.id;meta.main"
    table["name"].meta["ucd"] = "meta.id"
    table["ra"].unit = "deg"
    table["ra"].meta["ucd"] = "pos.eq.ra"
    table["dec"].unit = "deg"
    table["dec"].meta["ucd"] = "pos.eq.dec"
    table["distance"].unit = "pc"
    table["distance"].meta["ucd"] = "pos.distance"

    # Convert the Astropy Table to a VOTable and write it to disk
    votable = from_table(table)
    votable.to_xml(OUTPUT_FILE)
    print(f"Wrote {len(rows)} entries to {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
