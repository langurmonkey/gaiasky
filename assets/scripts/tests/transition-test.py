# This script tests the go-to commands. To be run asynchronously.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

gs.sleep(2)
gs.setCameraFree()

# SYNC
gs.print("Starting SYNC transition that will last 15 seconds")

gs.cameraTransition([-3.755500744789524E11, -2.922083993465335E11, 1.177110695636744E12], [-0.07161522514290668, 0.26160383453064656, -0.9625147756198811], [0.22780313354328777, 0.9437774863679357, 0.2395616592296831], 15.0, True)

gs.print("Transition finalized")
gs.sleep(2)

# Back to Earth

gs.setCameraFocusInstantAndGo("Earth")
gs.sleep(2)
gs.setCameraFree()

# ASYNC
gs.print("Starting ASYNC transition")

gs.cameraTransition([-3.755500744789524E11, -2.922083993465335E11, 1.177110695636744E12], [-0.07161522514290668, 0.26160383453064656, -0.9625147756198811], [0.22780313354328777, 0.9437774863679357, 0.2395616592296831], 5.0, False)

gs.print("Transition finalized")

gs.enableInput()
gs.maximizeInterfaceWindow()

gs.print("Script ended")

gateway.shutdown()
