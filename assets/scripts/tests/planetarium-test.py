# This script tests the go-to commands. To be run asynchronously.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
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

gateway.close()
