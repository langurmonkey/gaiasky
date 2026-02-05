#!/usr/bin/env python

import sys
import argparse
import numpy as np
from astropy.io import votable
from astropy.table import Table

try:
    from tqdm import tqdm
except ImportError:
    def tqdm(iterable, **kwargs):
        """Fallback if tqdm is not installed."""
        print("Tip: Install 'tqdm' (pip install tqdm) for a visual progress bar.")
        return iterable

class Vector3D:
    """
    A minimal 3D vector class for performing basic orbital mechanics math.
    """
    def __init__(self, x, y, z):
        """Initialize vector components."""
        self.x, self.y, self.z = x, y, z

    def mag(self):
        """Return the Euclidean magnitude of the vector."""
        return np.sqrt(self.x**2 + self.y**2 + self.z**2)

    def dot(self, v):
        """Calculate the dot product with another Vector3D."""
        return self.x*v.x + self.y*v.y + self.z*v.z

    def cross(self, v):
        """Calculate the cross product with another Vector3D."""
        return Vector3D(self.y*v.z - self.z*v.y, 
                        self.z*v.x - self.x*v.z, 
                        self.x*v.y - self.y*v.x)

    def scalarmult(self, s):
        """Return a new vector scaled by factor s."""
        return Vector3D(self.x*s, self.y*s, self.z*s)

    def subtract(self, v):
        """Subtract another Vector3D from this one."""
        return Vector3D(self.x-v.x, self.y-v.y, self.z-v.z)

    def unitVector(self):
        """Return a unit vector in the same direction. Returns (0,0,0) if magnitude is 0."""
        m = self.mag()
        return Vector3D(self.x/m, self.y/m, self.z/m) if m > 0 else Vector3D(0,0,0)

    def rotateX(self, phi):
        """Perform an in-place rotation around the X-axis by phi radians."""
        ynew = self.y*np.cos(phi) - self.z*np.sin(phi)
        znew = self.y*np.sin(phi) + self.z*np.cos(phi)
        self.y, self.z = ynew, znew

    def rotateZ(self, phi):
        """Perform an in-place rotation around the Z-axis by phi radians."""
        xnew = self.x*np.cos(phi) - self.y*np.sin(phi)
        ynew = self.x*np.sin(phi) + self.y*np.cos(phi)
        self.x, self.y = xnew, ynew

class orbitalElements(object):
    """
    Handles the conversion between Cartesian state vectors (pos, vel) 
    and Keplerian orbital elements.
    """
    def __init__(self, position, velocity, G, totalMass):
        """
        Initialize with state vectors and physical constants.
        
        Args:
            position (Vector3D): Position vector in AU.
            velocity (Vector3D): Velocity vector in AU/day.
            G (float): Gravitational constant (AU^3 / day^2).
            totalMass (float): Mass of the system (Sun + Object).
        """
        self.tiny = 1.0e-10
        self.position = position
        self.velocity = velocity
        self.G = G
        self.totalMass = totalMass
        
    def calcOrbitFromVector(self):
        """
        Derives Keplerian elements (a, e, i, Omega, omega, M, T) from state vectors.
        Includes handling for near-circular and near-equatorial edge cases.
        """
        # 1. Specific Angular Momentum (h = r x v)
        angmomvec = self.position.cross(self.velocity)
        self.angmom = angmomvec.mag()
        
        gravparam = self.G * self.totalMass 
        magpos = self.position.mag() 
        magvel = self.velocity.mag() 
        vdotr = self.velocity.dot(self.position) 
        
        # 2. Eccentricity Vector (e = [(v^2 - mu/r)r - (r.v)v] / mu)
        if (magpos == 0.0):
            eccV = Vector3D(0.0,0.0,0.0) 
        else:
            eccV = self.position.scalarmult(magvel**2).subtract(self.velocity.scalarmult(vdotr))
            eccV = eccV.scalarmult(1.0/gravparam).subtract(self.position.unitVector())

        self.e = eccV.mag()

        # Semi-latus rectum
        self.semilat = self.angmom**2 / gravparam
        
        # 3. Semi-major Axis (a)
        try:
            self.a = self.semilat/(1.0 - self.e**2)
        except ZeroDivisionError:
            self.a = np.inf
            
        # 4. Inclination (i)
        self.i = np.arccos(np.clip(angmomvec.z/self.angmom, -1.0, 1.0)) if self.angmom > 0 else 0.0
            
        # 5. Longitude of Ascending Node (Omega)
        zhat = Vector3D(0.0, 0.0, 1.0)
        nplane = zhat.cross(angmomvec) # Node vector
        if nplane.mag() < self.tiny:
            self.longascend = 0.0 # Equatorial orbit
        else:
            self.longascend = np.arccos(np.clip(nplane.x/nplane.mag(), -1.0, 1.0))
            if nplane.y < 0:
                self.longascend = 2.0 * np.pi - self.longascend
        
        # 6. True Anomaly (nu)
        if self.e > self.tiny:
            edotR = eccV.dot(self.position) / (magpos * self.e) 
            self.trueanom = np.arccos(np.clip(edotR, -1.0, 1.0))
            if vdotr < 0: # Checks if moving away or towards periapsis
                self.trueanom = 2.0 * np.pi - self.trueanom
        else:
            self.trueanom = 0.0

        # 7. Argument of Periapsis (omega)
        if self.e > self.tiny and nplane.mag() > self.tiny:
            ndote = nplane.unitVector().dot(eccV.unitVector())
            self.argper = np.arccos(np.clip(ndote, -1.0, 1.0))
            if eccV.z < 0:
                self.argper = 2.0 * np.pi - self.argper
        else:
            self.argper = 0.0

        # 8. Mean Anomaly (M)
        if self.e < 1.0: # Bound orbits (Elliptic)
            E = 2.0 * np.arctan(np.sqrt((1.0 - self.e) / (1.0 + self.e)) * np.tan(self.trueanom / 2.0))
            self.meananom = (E - self.e * np.sin(E)) % (2.0 * np.pi)
        else: 
            self.meananom = 0.0 

        # 9. Orbital Period (T) in Days - Power Law Method
        if self.a > 0 and self.e < 1.0:
            self.period = pow(self.a, 1.5) * 365.2525
        else:
            self.period = np.nan

def process_sso_data(input_file, output_file):
    """
    Main processing loop: loads VOTable, calculates orbits, cleans table, and saves.
    """
    print(f"[*] Loading VOTable: {input_file}...")
    try:
        vot = votable.parse(input_file)
        data = vot.get_first_table().to_table()
    except Exception as e:
        print(f"[!] Error: {e}")
        return

    # Constants: G * M_sun in units of AU and Days
    G_msol_AU_day = (4.0 * np.pi**2) / (365.25**2)
    
    # Storage for computed values
    results = {k: [] for k in ['a', 'e', 'i', 'om', 'w', 'ma', 'period']}

    print(f"[*] Processing {len(data)} rows...")
    for row in tqdm(data, desc="Calculating Elements"):
        sv = row['h_state_vector']
        orbit = orbitalElements(Vector3D(*sv[:3]), Vector3D(*sv[3:]), G_msol_AU_day, 1.0)
        orbit.calcOrbitFromVector()
        
        results['a'].append(orbit.a)
        results['e'].append(orbit.e)
        results['i'].append(np.degrees(orbit.i))
        results['om'].append(np.degrees(orbit.longascend))
        results['w'].append(np.degrees(orbit.argper))
        results['ma'].append(np.degrees(orbit.meananom))
        results['period'].append(orbit.period)

    # Insert calculated columns into the Astropy Table
    data['epoch'] = row['epoch_state_vector_jd']
    data['semimajoraxis'] = results['a']
    data['eccentricity'] = results['e']
    data['inclination'] = results['i']
    data['ascendingnode'] = results['om']
    data['argofpericenter'] = results['w']
    data['meananomaly'] = results['ma']
    data['period'] = results['period']

    # Delete memory-heavy columns
    cols_to_remove = ['h_state_vector', 'orbital_elements_var_covar_matrix', 'epoch_state_vector_jd']
    for col in cols_to_remove:
        if col in data.colnames:
            data.remove_column(col)
            print(f"[-] Removed column: {col}")

    print(f"[*] Saving results to: {output_file}...")
    votable.from_table(data).to_xml(output_file)
    print("[+] Done!")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert SSO state vectors to Keplerian elements.")
    parser.add_argument("input", help="Path to input VOTable file")
    parser.add_argument("output", help="Path to output VOTable file")
    args = parser.parse_args()
    process_sso_data(args.input, args.output)
