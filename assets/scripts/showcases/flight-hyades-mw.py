# Script that showcases a flight to the Hyades, proper motions and
# a zoom out of the galaxy.
#
# Created by Stefan Jordan

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point


## PREPPING

gs.disableInput()
gs.cameraStop()

gs.setSmoothLodTransitions(True)
gs.setFov(60)
gs.setRotationCameraSpeed(20)
gs.setTurningCameraSpeed(20)
gs.setCameraSpeed(20)

gs.setSimulationPace(0)
gs.setStarBrightness(40.0)
gs.setStarSize(6.0)
gs.setBrightnessLevel(0)
gs.setContrastLevel(1)
gs.setAmbientLight(0)
gs.setBloom(0)
gs.setMotionBlur(False)

gs.setSaturationLevel(1.0)
gs.setVisibility("element.clusters", False)
gs.setVisibility("element.velocityvectors", False)

gs.setRotationCameraSpeed(40)
gs.setTurningCameraSpeed(30)
gs.setCameraSpeed(30)
gs.setVisibility('element.labels',False)
gs.setVisibility('element.constellations',False)


gs.setSimulationTime(2018,4,25,10,0,0,0)

gs.setTurningCameraSpeed(10)
gs.setRotationCameraSpeed(10)

gs.setRotationCameraSpeed(20)
gs.setTurningCameraSpeed(20)
gs.setCameraSpeed(30)
gs.setVisibility('element.labels',False)

gs.setSaturationLevel(2)

gs.setVisibility('element.planets',True)
gs.setVisibility('element.moons',True)
gs.setCinematicCamera(True)

gs.setCameraFocusInstantAndGo("Sun")
gs.sleep(2)
gs.setCameraFocusInstant("Bellatrix")

gs.sleep(2)
gs.sleep(2)


gs.setCameraLock(True)
gs.setVisibility('element.labels',False)
gs.setVisibility('element.planets',False)
gs.setVisibility('element.constellations',True)
gs.sleep(7)
gs.setVisibility('element.planets',False)


gs.setCameraFocus("78 tau")
gs.sleep(3)
gs.setVisibility('element.constellations',False)
gs.setCameraSpeed(0.5)

gs.sleep(5)
gs.setVisibility("element.clusters", False)

gs.goToObject("78 tau", 0.000007778, 25)
gs.setSaturationLevel(1.7)
gs.setCameraLock(True)
gs.sleep(7)
gs.startSimulationTime()

def frange(x, y, jump):
    while x < y:
        yield x
        x += jump

for speed in frange(0.0, 0.01, 0.00025):
  gs.cameraRotate(speed,0)
  gs.sleep(0.1)

gs.sleep(20)
gs.cameraStop()
gs.setCameraLock(False)
gs.setCameraFree()
gs.sleep(5)
gs.setProperMotionsNumberFactor(2000)
gs.sleep(3)

for pace in frange(.1e11, .5e12, .1e11):
    gs.setSimulationPace(pace)
    gs.sleep(0.1)

gs.sleep(15)
gs.cameraRotate(0.1, 0.0)
gs.sleep(10)

gs.setCameraSpeed(2.0)

fwdval = 0.1
saturation = 2.0
for t in range(0, 500000):
    gs.cameraForward(-fwdval)
    gs.setSaturationLevel(saturation)
    gs.sleep(0.1)

    saturation -= 0.01
    if saturation <= 1.0:
        saturation = 1.0

    fwdval += 0.1
    if fwdval >= 1.0:
        fwdval = 1.0
    if gs.getDistanceTo("Sun") > 6622825900000000000:
        break


gs.sleep(5)
gs.cameraStop()
gs.stopSimulationTime()
gs.sleep(2)


##
## CLEAN UP AND FINISH
##

gs.setFrameOutput(False)

gs.enableInput()
gs.setSimulationPace(1)
gs.setStarBrightness(27.0)
gs.setStarSize(8.0)
gs.setRotationCameraSpeed(40)
gs.setTurningCameraSpeed(24)
gs.setCameraSpeed(40)
gs.setBrightnessLevel(0)
gs.setContrastLevel(1)
gs.setAmbientLight(0)
gs.setBloom(0)
gs.setMotionBlur(False)
gs.setFov(60)

gs.setSaturationLevel(1.0)

gs.setVisibility('element.planets',True)
gs.setVisibility('element.moons',True)

# close gateway
gateway.shutdown()
