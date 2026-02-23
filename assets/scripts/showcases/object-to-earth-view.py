# This script implements a routine to get seamless view for an object
# from Earth, starting at any position. The view is always aligned to the NCP.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import numpy as np

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

m_to_u = gs.getMeterToInternalUnitConversion()

def normalize(v):
    norm = np.linalg.norm(v)
    if norm == 0:
        raise ValueError("Zero-length vector")
    return v / norm

"""
   Compute up vector for the given direction vector so that
   it is as much aligned to the NCP as possible. 
"""
def compute_up_vector(dir):
    ncp = np.array([0.0, 1.0, 0.0])  # North Celestial Pole

    # Project ncp onto the plane orthogonal to dir
    ncp_proj = ncp - np.dot(ncp, dir) * dir

    # Normalize to get the up vector
    up = normalize(ncp_proj)

    return up

def view_from_earth(target_name: str):
    earth_pos = np.array(gs.getObjectPosition("Earth"))
    target_pos = np.array(gs.getObjectPosition(target_name))

    if np.allclose(earth_pos, target_pos):
        raise ValueError("Points A and B are the same; direction vector is undefined.")

    # Compute unit vector from earth to target; that's our direction
    et = target_pos - earth_pos
    dir = normalize(et)

    # Final position of our camera, 30_000 kilometers away in +Y direction
    pos = earth_pos + np.array([0.0, 30_000_000 * m_to_u, 0.0])

    # Up, arbitrary
    up = compute_up_vector(dir)
    
    # Create transition, 15 seconds
    gs.cameraTransition(pos.tolist(), dir.tolist(), up.tolist(), 15.0)

    # Once we face the new target, we change focus
    gs.setCameraFocus(target_name)


target_name = input("Enter the name of the target object: ")
view_from_earth(target_name)

gateway.close()
