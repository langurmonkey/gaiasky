# Test script. Tests the 'setLabelColor()' and 'setForceDisplayLabel()' API calls.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setForceDisplayLabel("Earth", True)
gs.setLabelColor("Earth", [0.3, 0.5, 1.0, 0.8])
gs.setLabelColor("Aldebaran", [0.8, 0.2, 0.3, 0.8])

gs.enableInput()

gateway.shutdown()
