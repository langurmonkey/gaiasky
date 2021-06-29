# Test script. Tests per-object visibility commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setObjectVisibility("Rigel", False)
gs.setObjectVisibility("Arcturus", False)
gs.setObjectVisibility("Earth", False)
gs.sleep(2)
print("Rigel visibility: %s" % gs.getObjectVisibility("Rigel"))
print("Arcturus visibility: %s" % gs.getObjectVisibility("Arcturus"))
print("Earth visibility: %s" % gs.getObjectVisibility("Earth"))
gs.sleep(2)
gs.setObjectVisibility("Rigel", True)
gs.setObjectVisibility("Arcturus", True)
gs.setObjectVisibility("Earth", True)

gs.enableInput()

gateway.shutdown()
