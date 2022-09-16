# This script tests adding and removing polylines.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

gs.setCinematicCamera(True)
gs.setRotationCameraSpeed(30.)
gs.setCameraSpeed(100.)

gs.setCameraFocusInstantAndGo("Sun")
gs.goToObject("Sun", 1e-7)

gs.sleep(3)

gs.addPolyline("Line1", [ 0.0, 0.0, 0.0, 1e9, 10.0, 200.0, 0.3, 6e9, 4444444.0 ], [ .2, .4, .8, .8 ], 1 )
gs.addPolyline("Line2", [ 0.0, 0.0, 0.0, 1e6, 7e8, 2000000.0, 0.99, 444444.0, 3e9 ], [ .9, .4, .3, .8 ], 2 )
gs.print("Lines added")

gs.sleep(3)

gs.cameraRotate(1.0, 0.5)

gs.sleep(6)

gs.removeModelObject("Line1")
gs.removeModelObject("Line2")
gs.print("Lines removed")

gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()

gateway.shutdown()
