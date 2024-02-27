# This script tests the positioning of the camera with relation to two objects.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

# Camera looks at sunny side of Earth
gs.setCameraPositionAndFocus("Earth", "Sun", 0, 30)
gs.sleep(3)

# Camera looks at 50% sunny, 50% shady
gs.setCameraPositionAndFocus("Earth", "Sun", 90, 30)
gs.sleep(3)

# Camera looks at shady side of Earth
gs.setCameraPositionAndFocus("Earth", "Sun", 180, 30)
gs.sleep(3)

# Camera looks at sunny side of Mars
gs.setCameraPositionAndFocus("Mars", "Sun", 0, 30)
gs.sleep(3)

# Camera looks at shady side of Mars
gs.setCameraPositionAndFocus("Mars", "Sun", 180, 30)
gs.sleep(3)

gs.enableInput()

gateway.shutdown()
