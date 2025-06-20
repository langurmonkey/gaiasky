# This script implements a routine to get seamless view for an object
# from Earth, starting at any position.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import numpy as np

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

m_to_u = gs.getMeterToInternalUnitConversion()

def view_from_earth(target_name: str):
    earth_pos = np.array(gs.getObjectPosition("Earth"))
    target_pos = np.array(gs.getObjectPosition(target_name))

    if np.allclose(earth_pos, target_pos):
        raise ValueError("Points A and B are the same; direction vector is undefined.")

    # Compute unit vector from earth to target; that's our direction
    et = target_pos - earth_pos
    earth_target = et / np.linalg.norm(et)

    # Final position of our camera, 10_000 kilometers away in +Y direction
    pos = earth_pos + np.array([0.0, 10_000_000 * m_to_u, 0.0])

    # Up, arbitrary
    up = np.cross(earth_target, np.array([1.0, 0.0, 0.0]))
    
    # Create transition, 10 seconds
    gs.cameraTransition(pos.tolist(), earth_target.tolist(), up.tolist(), 10.0)

    # Once we face the new target, we change focus
    gs.setCameraFocus(target_name)


view_from_earth("Bellatrix")
