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
gs.setVisibility("element.orbits", True)
gs.setVisibility("element.labels", False)

# Around 5 AU in north ecliptic pole
eclpos = gs.eclipticToInternalCartesian(0.0, 90.0, 1e17)

gs.setCameraPosition(eclpos)
gs.setCameraFocus("Sun", -1)


# Set free camera
gs.setCameraFree()
# Starting time
gs.setSimulationTime(2017, 12, 1, 10, 5, 0, 0)

# Scale objects
gs.setObjectSizeScaling("Earth", 500000.0)
gs.setOrbitCoordinatesScaling("EarthVSOP87", 500.0)
gs.setObjectSizeScaling("Gaia", 500000000000.0)
gs.setOrbitCoordinatesScaling("HeliotropicOrbitCoordinates:Gaia", 7000.0)

gs.refreshAllOrbits()

gs.sleep(3)

# Fast pace
gs.setSimulationPace(2e6)
# Start!
gs.startSimulationTime()

gs.sleep(8.0)

# Gently zoom out for 30 seconds (30 * 60 frames)
gs.setCinematicCamera(True)
gs.setCameraSpeed(1.0)

start_frame = gs.getCurrentFrameNumber()
current_frame = start_frame

while current_frame - start_frame < 1800:
    gs.cameraForward(-0.5)
    gs.sleep(0.2)
    current_frame = gs.getCurrentFrameNumber()

# Finish! stop time
gs.stopSimulationTime()

# Scales back to normal
gs.setObjectSizeScaling("Earth", 1.0)
gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0)
gs.setObjectSizeScaling("Gaia", 1.0)
gs.setOrbitCoordinatesScaling("HeliotropicOrbitCoordinates:Gaia", 1.0)

gs.refreshAllOrbits()

# Orbits and labels off
gs.setVisibility("element.orbits", False)
gs.setVisibility("element.labels", False)

gateway.shutdown()
