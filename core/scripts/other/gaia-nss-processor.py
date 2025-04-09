import pyvo, json, random, math
import numpy as np
import astropy.units as u

# Gaia source IDs to process
source_ids = [
    3937211745905473024,
    5706079252076583424,
    5706107598860484480,
    5707008335104427648,
    5706858256063509632,
    5706775174216390656,
    5706900892200998144,
    5706783622415220736
    # Add more here
]

orbit_colors = [
    [1.0, 0.4, 0.4, 1.0],  # Soft red
    [1.0, 0.8, 0.4, 1.0],  # Warm yellow-orange
    [0.6, 0.8, 1.0, 1.0],  # Light blue
    [0.7, 1.0, 0.7, 1.0],  # Soft green
    [0.9, 0.6, 1.0, 1.0],  # Lavender
    [1.0, 1.0, 0.6, 1.0],  # Pale yellow
    [0.8, 0.5, 0.2, 1.0],  # Earthy orange
    [0.4, 1.0, 1.0, 1.0],  # Cyan
]

# Connect to the ARI Gaia TAP service
tap_service = pyvo.dal.TAPService("https://gaia.ari.uni-heidelberg.de/tap")

# Query gaia_source and nss_two_body_orbit
query = f"""
SELECT o.*, 
       g.ra AS gra,
       g.dec AS gdec,
       g.ref_epoch,
       g.phot_g_mean_mag AS g_mag, 
       g.pmra, 
       g.pmdec, 
       g.radial_velocity, 
       g.bp_rp,
       g.parallax as pllx
FROM gaiadr3.nss_two_body_orbit AS o
JOIN gaiadr3.gaia_source AS g
ON o.source_id = g.source_id
WHERE o.source_id IN ({','.join(str(s) for s in source_ids)})
"""
results = tap_service.search(query)

# Convert pyvo result to astropy Table
table = results.to_table()
# Write to VOTable file
table.write("nss-query-results.vot", format="votable", overwrite=True)
print("Results written to nss-query-results.vot")

def epoch_to_julian_day(epoch):
    """
    Converts an epoch in years to Julian Days.

    Parameters:
        epoch (float): The epoch in years (e.g., 2016.0)

    Returns:
        float: The Julian Day corresponding to the given epoch.
    """
    JD_J2000 = 2451545.0  # Julian Day for J2000.0
    days_per_year = 365.25  # Average number of days in a year
    return JD_J2000 + days_per_year * (epoch - 2000)

def thiele_innes_to_campbell(A, B, F, G):
    # Compute semi-major axis, in mas
    a = np.sqrt(A**2 + B**2 + F**2 + G**2)

    # Compute inclination, in radians
    cos_i = (A*G - B*F) / (a**2)
    cos_i = np.clip(cos_i, -1.0, 1.0)  # numerical safety
    i = np.arccos(cos_i)

    # Compute Omega and omega (from atan2 formulas)
    # This gives two angles whose sum is constant
    Omega_plus_omega = np.arctan2(B - F, A + G)
    Omega_minus_omega = np.arctan2(B + F, A - G)

    # Now solve for Ω and ω
    Omega = (Omega_plus_omega + Omega_minus_omega) / 2
    omega = (Omega_plus_omega - Omega_minus_omega) / 2

    # Normalize angles to [0, 2π)
    Omega = Omega % (2 * np.pi)
    omega = omega % (2 * np.pi)

    # Convert to degrees for readability
    return {
        'a_mas': a,
        'i_deg': np.degrees(i),
        'Omega_deg': np.degrees(Omega),
        'omega_deg': np.degrees(omega)
    }

def bp_rp_to_bv(row):
    """
    Convert Gaia BP–RP color to Johnson B–V using an empirical polynomial.
    Falls back to 0.73 (solar-like) if bp_rp is not available.

    Parameters:
        row: A dictionary-like object with a 'bp_rp' key

    Returns:
        float: Estimated B–V color index
    """
    bp_rp = row.get('bp_rp', None)

    if bp_rp is None:
        return 0.73  # default to solar-type color
    try:
        bp_rp = float(bp_rp)
    except (ValueError, TypeError):
        return 0.73

    # Clamp to Gaia's realistic range
    bp_rp = max(-0.5, min(4.0, bp_rp))

    # Empirical polynomial fit from Jordi et al. (2010) and transformations used in Gaia DR2/DR3
    # Source: approximate 3rd order polynomial fit for dwarfs
    # B-V = 0.0187 + 1.0157*(BP-RP) - 0.2069*(BP-RP)^2 + 0.0156*(BP-RP)^3

    bv = 0.0187 + 1.0157 * bp_rp - 0.2069 * bp_rp**2 + 0.0156 * bp_rp**3

    return round(bv, 4)

def sanitize_for_json(obj):
    if isinstance(obj, dict):
        return {k: sanitize_for_json(v) for k, v in obj.items()}
    elif isinstance(obj, list):
        return [sanitize_for_json(v) for v in obj]
    elif hasattr(obj, 'item'):  # catches numpy scalars
        return obj.item()
    else:
        return obj

hook_object = {
      "name": "dr3-nss-hook",
      "position": [
        0.0,
        0.0,
        0.0
      ],
      "componentTypes": [
        "Stars"
      ],
      "parent": "Universe",
      "archetype": "GenericCatalog",
      "cataloginfo": {
        "name": "Gaia DR3 NSS",
        "description": "Contains a few select orbital solutions for NSS in Gaia DR3.",
        "type": "INTERNAL",
        "nobjects": len(results),
        "sizebytes": "5000"
      }
}
objects = []
objects.extend([hook_object])
n_objects = 0

for row in results:
    sid = row['source_id']
    name_base = f"NSS-{sid}"
    center_name = f"{name_base} Center"
    star_name = f"{name_base} Star"
    orbit_name = f"{name_base} Orbit"

    # Parallax and position
    parallax = row['parallax']
    if not parallax or parallax <= 0 or math.isnan(parallax):
        print(f"     {sid}: no parallax in NSS table, trying gaia_source")
        parallax = row['pllx']
        if not parallax or parallax <= 0 or math.isnan(parallax):
            print(f"SKIP {sid}: no parallax")
            continue

    distance_pc = 1000.0 / parallax
    distance_km = distance_pc * 30856775812800.0

    ra = row['ra']
    if not ra or math.isnan(ra):
        print(f"     {sid}: no RA in NSS table, trying gaia_source")
        ra = row['gra']
        if not ra or math.isnan(ra):
            print(f"SKIP {sid}: no RA")
            continue
    dec = row['dec']
    if not dec or math.isnan(dec):
        print(f"     {sid}: no DEC in NSS table, trying gaia_source")
        dec = row['gdec']
        if not dec or math.isnan(dec):
            print(f"SKIP {sid}: no DEC")
            continue
    
    position_equatorial = [ra, dec, distance_pc]

    # Epoch
    ref_epoch = epoch_to_julian_day(row['ref_epoch'])
    solution_epoch = ref_epoch + row['t_periastron']

    # Period
    period = row['period'] # days
    period_s = period * 86400.0

    # === Orbital parameters ===
    # Convert Thiele-Innes to Campbell's elements
    A = row['a_thiele_innes']
    B = row['b_thiele_innes']
    F = row['f_thiele_innes']
    G = row['g_thiele_innes']
    if None in (A, B, F, G) or math.isnan(A) or math.isnan(B) or math.isnan(F) or math.isnan(G):
        print(f"SKIP {sid}: missing Thieles-Inner elements")
        continue  # not enough info

    campbell = thiele_innes_to_campbell(A, B, F, G)

    # Step 1: semi-major axis (a1)
    a_mas = campbell['a_mas']
    a_au = (a_mas * distance_pc) / 1000.0
    a_km = a_au * 149597870.7

    # Step 2: inclination (i)
    i = campbell['i_deg']

    # Step 3: longitude of ascending node (omega)
    omega = campbell['Omega_deg']

    # Step 4: argument of periastron (w)
    w = campbell['omega_deg']

    # Estimate eccentricity if available
    e = row['eccentricity'] if row['eccentricity'] is not None else 0.0

    

    # --- Center ---
    center_obj = {
        "names": [center_name],
        "size": 50.0,
        "colorbv": 0.81,
        "componentTypes": ["Others"],
        "parent": "dr3-nss-hook",
        "archetype": "Invisible",
        "coordinates": {
            "impl": "gaiasky.util.coord.StaticCoordinates",
            "positionEquatorial": position_equatorial
        }
    }

    # --- Orbit ---
    orbit_color = random.choice(orbit_colors)
    orbit_color[3] = random.uniform(0.4, 1.0)
    orbit_obj = {
        "name": orbit_name,
        "color": orbit_color,
        "componentTypes": ["Orbits", "Stars"],
        "parent": center_name,
        "archetype": "Orbit",
        "provider": "gaiasky.data.orbit.OrbitalParametersProvider",
        "model": "extrasolar_system",
        "newmethod": True,
        "orbit": {
            "period": period,
            "epoch": solution_epoch,
            "semimajoraxis": a_km,
            "eccentricity": e,
            "inclination": i,
            "ascendingnode": omega,
            "argofpericenter": w,
            "meananomaly": 0.0
        }
    }

    # --- Star ---
    appmag = row.get('g_mag') or 10.0
    absmag = appmag - 5 * (np.log10(distance_pc) - 1)
    star_color = bp_rp_to_bv(row)

    star_obj = {
        "names": [star_name],
        "colorbv": star_color,
        "componentTypes": ["Stars"],
        "absmag": absmag,
        "appmag": appmag,
        "parent": center_name,
        "archetype": "Star",
        "coordinates": {
            "impl": "gaiasky.util.coord.OrbitLintCoordinates",
            "orbitname": orbit_name
        }
    }

    objects.extend([center_obj, orbit_obj, star_obj])
    main_objects = {
        "objects": objects
    }
    n_objects = n_objects + 1

# Write to output
with open("gaia-nss-orbits.json", "w") as f:
    json.dump(sanitize_for_json(main_objects), f, indent=2)

print(f"Input objects: {len(source_ids)}, written objects: {n_objects}")
print("Done. JSON file created.")
