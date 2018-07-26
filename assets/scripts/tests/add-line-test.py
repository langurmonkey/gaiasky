# This script tests adding and removing polylines.
# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface


gs = EventScriptingInterface.instance()

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

gs.setCinematicCamera(True)
gs.setRotationCameraSpeed(50.)
gs.setCameraSpeed(15.)

gs.setCameraFocusInstantAndGo("Sol")
gs.goToObject("Sol", 1e-8)

gs.sleep(3)

gs.addPolyline("Line1", [ 0.0, 0.0, 0.0, 1e9, 10.0, 200.0, 0.3, 1e10, 4444444.0 ], [ .2, .4, .8, .8 ] )
print("Line1 added")

gs.sleep(3)

gs.cameraRotate(1.0, 0.5)

gs.sleep(6)

gs.removeModelObject("Line1")
print("Line1 removed")

gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()
