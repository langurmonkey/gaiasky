# Test script. Tests visibility commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setVisibility("element.ecliptic", True)
gs.sleep(4)
gs.setVisibility("element.ecliptic", False)

gs.enableInput()

gateway.close()