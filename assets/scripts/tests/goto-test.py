# This script tests the go-to commands. To be run asynchronously.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

gs.setRotationCameraSpeed(20)
gs.setTurningCameraSpeed(20)
gs.setCameraSpeed(20)

gs.goToObject("Sol", 20.0, 4.5)

gs.setHeadlineMessage("Sun")
gs.setSubheadMessage("This is the Sun, our star")

gs.sleep(4)
gs.clearAllMessages()

gs.goToObject("Sol", 5.5)

gs.setHeadlineMessage("Sun")
gs.setSubheadMessage("We are now zooming out a bit")

gs.sleep(4)
gs.clearAllMessages()

gs.goToObject("Earth", 20.0, 6.5)

gs.setHeadlineMessage("Earth")
gs.setSubheadMessage("This is the Earth, our home")

gs.sleep(4)
gs.clearAllMessages()

gs.goToObject("Earth", 2.5, 1.5)

gs.setHeadlineMessage("Earth")
gs.setSubheadMessage("Zooming out here...")

gs.sleep(4)
gs.clearAllMessages()

gs.setCameraFocus("Sol")
gs.sleep(4)

gs.enableInput()
gs.maximizeInterfaceWindow()

gateway.close()