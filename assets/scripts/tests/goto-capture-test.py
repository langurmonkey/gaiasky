# This script tests the go-to and capture frame commands. To be run asynchronously.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

gs.setRotationCameraSpeed(40)
gs.setTurningCameraSpeed(30)
gs.setCameraSpeed(30)

gs.configureRenderOutput(1280, 720, 60, gs.getDefaultFramesDir(), 'scripting-test')
gs.setFrameOutput(True)

gs.goToObject("Sol", -1, 2.5)

gs.setHeadlineMessage("Sun")
gs.setSubheadMessage("This is the Sun, our star")

gs.sleepFrames(40)
gs.clearAllMessages()

gs.goToObject("Earth", -1, 2.5)

gs.setHeadlineMessage("Earth")
gs.setSubheadMessage("This is the Earth, our home")

gs.sleepFrames(40)
gs.clearAllMessages()

gs.setCameraFocus("Sol")
gs.sleep(4)

gs.setFrameOutput(False)

gs.enableInput()
gs.maximizeInterfaceWindow()

gateway.close()