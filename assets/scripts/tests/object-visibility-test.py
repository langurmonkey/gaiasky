# Test script. Tests per-object visibility commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

print("Rigel visibility: %s" % gs.getObjectVisibility("Rigel"))
print("Arcturus visibility: %s" % gs.getObjectVisibility("Arcturus"))
print("Earth visibility: %s" % gs.getObjectVisibility("Earth"))
print()
print("Making Rigel, Arcturus and Earth invisible...")
print()
gs.setObjectVisibility("Rigel", False)
gs.setObjectVisibility("Arcturus", False)
gs.setObjectVisibility("Earth", False)
gs.sleep(5)
print("Rigel visibility: %s" % gs.getObjectVisibility("Rigel"))
print("Arcturus visibility: %s" % gs.getObjectVisibility("Arcturus"))
print("Earth visibility: %s" % gs.getObjectVisibility("Earth"))
print()
gs.sleep(5)
print("Making Rigel, Arcturus and Earth visible again...")
gs.setObjectVisibility("Rigel", True)
gs.setObjectVisibility("Arcturus", True)
gs.setObjectVisibility("Earth", True)
print()
gs.sleep(5)
print("Rigel visibility: %s" % gs.getObjectVisibility("Rigel"))
print("Arcturus visibility: %s" % gs.getObjectVisibility("Arcturus"))
print("Earth visibility: %s" % gs.getObjectVisibility("Earth"))

gs.enableInput()

gateway.shutdown()
