# Test script. Tests the 'setForceDisplayLabel()' API call.
# This script needs Hipparcos to be loaded beforehand.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.print("Forcing label of Phobos")
gs.setForceDisplayLabel("phobos", True)
gs.print("Forcing label of Delta Her (HIP 84379)")
gs.setForceDisplayLabel("del her", True)
gs.setForceDisplayLabel("unukalhai", True)


gs.enableInput()

gateway.close()
