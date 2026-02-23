# This script tests the go-to API calls.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setRotationCameraSpeed(20)
gs.setTurningCameraSpeed(20)
gs.setCameraSpeed(20)

gs.goToObject("Sun", 20.0, 4.5)

gs.setHeadlineMessage("Sun")
gs.setSubheadMessage("This is the Sun, our star")

gs.sleep(4)
gs.clearAllMessages()

gs.goToObject("Sun", 5.5)

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

gs.setCameraFocus("Sun")
gs.sleep(4)

gs.enableInput()

gateway.close()
