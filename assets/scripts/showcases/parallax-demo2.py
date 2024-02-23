# Scales up the Earth's orbit to demonstrate parallax.
# Created by Svetlin Tassev

import time
from py4j.clientserver import ClientServer, JavaParameters

def current_time_ms():
    return int(round(time.time() * 1000))

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

# Orbits and labels off
gs.setVisibility("element.orbits", True)
gs.setVisibility("element.labels", False)

# Around 5 AU in north ecliptic pole
#eclpos = gs.eclipticToInternalCartesian(0.0, 90.0, 1e17/2)

#gs.setCameraPosition(eclpos)
gs.setCameraFocus("Earth", -1)


# Set free camera
#gs.setCameraFree()
# Starting time
gs.setSimulationTime(2024, 4, 8, 18, 15, 0, 0)

# Scale objects
#gs.setObjectSizeScaling("Mercury", 1000.0)
#gs.setObjectSizeScaling("Venus", 1000.0)
#gs.setObjectSizeScaling("Mars", 1000.0)
#gs.setObjectSizeScaling("Earth", 1000.0)
#gs.setObjectSizeScaling("Jupiter", 500.0)
#gs.setObjectSizeScaling("Saturn", 500.0)
#gs.setObjectSizeScaling("Uranus", 500.0)
#gs.setObjectSizeScaling("Neptune", 500.0)
#gs.setObjectSizeScaling("Pluto", 500.0)
#gs.setObjectSizeScaling("Moon", 500.0)
gs.setOrbitCoordinatesScaling("EarthVSOP87", 100000.0)
#gs.setObjectSizeScaling("sun", 50.0)
#gs.setOrbitCoordinatesScaling("MoonAACoordinates", 50.0)

gs.refreshAllOrbits()
gs.setVisibility("element.moons", False)
gs.sleep(3)

# Fast pace
#gs.setSimulationPace(2e5)
# Start!
#gs.startSimulationTime()

#gs.sleep(8.0)

# Gently zoom out for 30 seconds (30 * 60 frames)
#gs.setCinematicCamera(True)
#gs.setCameraSpeed(1.0)
#
#start_frame = gs.getCurrentFrameNumber()
#current_frame = start_frame

#while current_frame - start_frame < 1800:
#    gs.cameraForward(-0.5)
#    gs.sleep(0.2)
#    current_frame = gs.getCurrentFrameNumber()

# Finish! stop time
#gs.stopSimulationTime()

# Scales back to normal
#gs.setObjectSizeScaling("Mercury", 1.0)
#gs.setObjectSizeScaling("Venus", 1.0)
#gs.setObjectSizeScaling("Mars", 1.0)
#gs.setObjectSizeScaling("Earth", 1.0)
#gs.setObjectSizeScaling("Jupiter", 1.0)
#gs.setObjectSizeScaling("Saturn", 1.0)
#gs.setObjectSizeScaling("Uranus", 1.0)
#gs.setObjectSizeScaling("Neptune", 1.0)
#gs.setObjectSizeScaling("Pluto", 1.0)
#gs.setObjectSizeScaling("Moon", 1.0)
##gs.setOrbitCoordinatesScaling("EarthVSOP87", 500.0)
#gs.setObjectSizeScaling("Sun", 1.0)
#gs.setOrbitCoordinatesScaling("MoonCoordinates", 1.0)

gs.refreshAllOrbits();
gs.forceUpdateScene();
gs.setFov(100)
# Orbits and labels off
gs.setVisibility("element.orbits", False)
gs.setVisibility("element.labels", False)


gs.setSimulationPace(6e6)
gs.sleep(2)
gs.startSimulationTime()
gs.sleep(30)
gs.stopSimulationTime()


gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0)
# Restore

gs.refreshAllOrbits();
gs.forceUpdateScene();

gateway.shutdown()
