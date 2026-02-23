# Script demonstrating the daily (and of course for longer time intervals, monthly) path of the Sun on the celestial sphere, similar to an armillary sphere.
# Created by Svetlin Tassev

import time
from py4j.clientserver import ClientServer, JavaParameters

def current_time_ms():
    return int(round(time.time() * 1000))

au_to_km = 149597900.0
u_to_km = 1000000.0

def to_internal(xyz):
    return [xyz[0] * u_to_km, xyz[1] * u_to_km, xyz[2] * u_to_km]



gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.stopSimulationTime()

gs.setObjectSizeScaling("Sun", 30.0)
gs.setObjectSizeScaling("Earth", 3000.0)

gs.setStarBrightness(0.0)
gs.setStarMinOpacity(0.0)
gs.setStarSize(10)
gs.setAmbientLight(0.8)

gs.setCameraFocus("Earth")
gs.setCameraOrientationLock(True)


gs.setVisibility("element.equatorial", True)
gs.setVisibility("element.asteroids", False)
gs.setVisibility("element.milkyway", False)
gs.setVisibility("element.moons", False)
gs.setVisibility("element.nebulae", False)
gs.setVisibility("element.others", False)
gs.setVisibility("element.orbits", False)

gs.setObjectVisibility("Mercury", False)
gs.setObjectVisibility("Venus", False)
gs.setObjectVisibility("Mars", False)
gs.setObjectVisibility("Jupiter", False)
gs.setObjectVisibility("Saturn", False)
gs.setObjectVisibility("Uranus", False)
gs.setObjectVisibility("Neptune", False)
gs.setObjectVisibility("Pluto", False)

#gs.setObjectVisibility("Earth", True)
#gs.setCameraPosition(pos)
coordxyz=gs.getObjectPosition("Earth")
coordxyz[2]+=50.
#coordxyz = gs.equatorialToInternalCartesian(0.01 * au_to_km, 0.01 * au_to_km, 1.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))


#direction=[0.6143652958797233,0.022023359676058417,0.7887143049593178]
#gs.setCameraDirection(direction)
gs.setCameraUp([0.,1.,0.])
gs.setOrthosphereViewMode(True)


gs.refreshAllOrbits();
gs.refreshObjectOrbit("Moon");
gs.forceUpdateScene();


gs.setSimulationPace(16384)
gs.sleep(2)
gs.startSimulationTime()

gs.sleep(30)



gs.setObjectSizeScaling("Sun", 1.0)
gs.setObjectSizeScaling("Earth", 1.0)

gs.setVisibility("element.equatorial", False)
gs.setVisibility("element.asteroids", False)
gs.setVisibility("element.milkyway", True)
gs.setVisibility("element.moons", True)
gs.setVisibility("element.nebulae", False)
gs.setVisibility("element.others", False)

gs.setObjectVisibility("Mercury", True)
gs.setObjectVisibility("Venus", True)
gs.setObjectVisibility("Mars", True)
gs.setObjectVisibility("Jupiter", True)
gs.setObjectVisibility("Saturn",  True)
gs.setObjectVisibility("Uranus",  True)
gs.setObjectVisibility("Neptune", True)
gs.setObjectVisibility("Pluto",   True)
gs.setStarBrightness(25.0)
gs.setStarMinOpacity(0.0)
gs.setStarSize(12)
gs.setAmbientLight(0.)

gs.setFov(60)

gs.stopSimulationTime()
gs.setOrthosphereViewMode(False)
gs.forceUpdateScene();

gateway.close()
