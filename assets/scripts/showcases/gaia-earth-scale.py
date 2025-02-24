# Scales up the Earth and Gaia and shows their movement around the Sun.
#
# Created by Toni Sagrista.

import time
from py4j.clientserver import ClientServer, JavaParameters

def current_time_ms():
    return int(round(time.time() * 1000))

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.cameraStop()

# Orbits and labels off
gs.backupSettings()
gs.setVisibility("element.orbits", True)
gs.setVisibility("element.labels", False)

# Around 5 AU in north ecliptic pole
eclpos = gs.eclipticToInternalCartesian(0.0, 90.0, 1.4e17)

gs.setCameraPosition(eclpos)
gs.setCameraFocus("Sun", -1)

# Starting time
gs.setSimulationTime(2017, 12, 1, 10, 5, 0, 0)

# Scale objects
gs.setObjectSizeScaling("Earth", 500000.0)
gs.setOrbitCoordinatesScaling("EarthVSOP87", 400.0)

gs.setObjectSizeScaling("Gaia", 90000000000.0)
gs.setObjectSizeScaling("Gaia orbit", 7000.0)

gs.refreshAllOrbits()

gs.sleep(1)

# Fast pace
gs.setSimulationPace(1e6)
# Start!
gs.startSimulationTime()

gs.sleep(2.0)

# Gently zoom out for 30 seconds (30 * 60 frames)
gs.setCinematicCamera(True)

gs.cameraRotate(0.0, 0.04)
gs.sleep(10.0)

# Finish! stop time
gs.cameraStop()
gs.stopSimulationTime()

# Scales back to normal
gs.setObjectSizeScaling("Earth", 1.0)
gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0)

gs.setObjectSizeScaling("Gaia", 1.0)
gs.setObjectSizeScaling("Gaia orbit", 1.0)

gs.refreshAllOrbits()
gs.restoreSettings()

gateway.shutdown()
