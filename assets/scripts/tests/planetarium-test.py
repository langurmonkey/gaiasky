# This script tests the planetarium mode.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setRotationCameraSpeed(20)
gs.setTurningCameraSpeed(20)
gs.setCameraSpeed(20)

gs.setPlanetariumMode(True)

gs.goToObject("Earth", 20.0, 4.5)
gs.sleep(4.0)

gs.setPlanetariumMode(False)

gs.enableInput()

gateway.shutdown()
