# This script tests the cameraTransition() command synchronously and asynchronously.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(string):
    gs.print(string)
    print(string)

gs.disableInput()
gs.cameraStop()

# Go to Earth
gs.setCameraFocusInstantAndGo("Earth")
gs.sleep(2)
gs.setCameraFree()

# SYNC
lprint("Starting SYNC transition that will last 15 seconds")

# This takes us outside the MilkyWay
gs.cameraTransition([-3.755500744789524E11, -2.922083993465335E11, 1.177110695636744E12], [-0.07161522514290668, 0.26160383453064656, -0.9625147756198811], [0.22780313354328777, 0.9437774863679357, 0.2395616592296831], 15.0, True)

lprint("Transition finalized")
gs.sleep(2)

# Back to Earth
gs.setCameraFocusInstantAndGo("Earth")
gs.sleep(2)
gs.setCameraFree()

# ASYNC
lprint("Starting ASYNC transition")

# This takes us outside the MilkyWay, again
gs.cameraTransition([-3.755500744789524E11, -2.922083993465335E11, 1.177110695636744E12], [-0.07161522514290668, 0.26160383453064656, -0.9625147756198811], [0.22780313354328777, 0.9437774863679357, 0.2395616592296831], 5.0, False)

lprint("Transition finalized")

gs.enableInput()

lprint("Script ended")

gateway.close()
