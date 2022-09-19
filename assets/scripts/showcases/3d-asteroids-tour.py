# Tour through the asteroids
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

# Camera params
gs.setCameraSpeed(1.0)
gs.setRotationCameraSpeed(1.0)
gs.setTurningCameraSpeed(1.0)
gs.setCinematicCamera(True)

gs.setFov(65.0)

# Visibility
gs.setVisibility("element.planets", True)
gs.setVisibility("element.atmospheres", True)
gs.setVisibility("element.stars", True)
gs.setVisibility("element.moons", True)
gs.setVisibility("element.satellites", True)
gs.setVisibility("element.galaxies", True)
gs.setVisibility("element.milkyway", True)

gs.setVisibility("element.asteroids", False)
gs.setVisibility("element.orbits", False)
gs.setVisibility("element.labels", False)
gs.setVisibility("element.constellations", False)
gs.setVisibility("element.boundaries", False)
gs.setVisibility("element.equatorial", False)
gs.setVisibility("element.ecliptic", False)
gs.setVisibility("element.galactic", False)
gs.setVisibility("element.clusters", False)
gs.setVisibility("element.meshes", False)
gs.setVisibility("element.titles", False)

gs.setCrosshairVisibility(False)

# Parallel view
gs.setStereoscopicProfile(3)
gs.setStereoscopicMode(True)

stdwait = 5.0
stdwaitlong = 10.0

# Time
gs.stopSimulationTime()
gs.setSimulationTime(2018, 4, 25, 10, 0, 0, 0)

# Camera state
gs.setCameraPosition([-1294.3864339045447 * 1e6,156.30069319755347 * 1e6,-1150.2743059128413 * 1e6])
gs.setCameraDirection([0.739144930622408,-0.09348275378626529,0.6670275453680645])
gs.setCameraUp([-0.1374839626900124,0.9485312542098752,0.2852834025843425])
gs.setCameraFocus("Sun")

gs.sleep(stdwait)

# Uncomment next line to save still frames
#gs.setFrameOutput(True)

gs.sleep(stdwait)

# Enable orbits
gs.setVisibility("element.orbits", True)
gs.sleep(stdwait)

gs.setVisibility("element.asteroids", True)
gs.sleep(stdwait)

gs.goToObject("Sun", 0.05, 0.0)

gs.cameraRotate(0.5, 0.0)

gs.sleep(stdwaitlong)
gs.cameraStop()

gs.setVisibility("element.orbits", False)

gs.sleep(stdwait)

gs.startSimulationTime()

initime = 4000.0
endtime = 4000000.0

def frange(x, y, jump):
    while x < y:
        yield x
        x += jump

# 10 seconds, in steps of 0.1 is 100 steps
step = (endtime - initime) / 200.0
gs.setSimulationPace(initime)
for t in frange(initime, endtime, step):
    gs.setSimulationPace(t)
    gs.sleep(0.05)

gs.cameraStop()

gs.sleep(stdwait)

gs.cameraRotate(0.0, 0.1)

gs.sleep(stdwaitlong)

gs.cameraRotate(0.0, -0.9)

gs.goToObject("Sun", 0.01, 0.0)

gs.stopSimulationTime()
gs.cameraStop()
gs.setFrameOutput(False)
gs.setStereoscopicMode(False)


gateway.shutdown()
