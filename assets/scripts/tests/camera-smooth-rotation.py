# This script tests the cameraRotate function.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import numpy as np

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setCinematicCamera(True)
gs.setRotationCameraSpeed(3.0)

gs.setCameraFocus("Earth")
gs.sleep(1)

for dxy in np.arange(0.0, 0.3, 0.01):
    gs.cameraRotate(dxy, dxy)
    gs.sleep(0.4)

gs.enableInput()

gateway.close()
