# This script tests the displaying of custom messages and images.
# Created by Toni Sagrista

import os
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

# Minimize interface, disable input, stop camera
gs.disableInput()
gs.cameraStop()

# Add messages
gs.displayMessageObject(0, "This is the zero message", 0.2, 0.0, 1.0, 0.0, 0.0, 1.0, 8)
gs.sleep(1.5)
gs.displayMessageObject(1, "This is the first message", 0.2, 0.2, 0.3, 0.4, 0.6, 1.0, 10)
gs.sleep(1.5)
gs.displayMessageObject(2, "This is the second message", 0.3, 0.1, 0.0, 1.0, 0.0, 1.0, 11)
gs.sleep(1.5)

# Image
profileimg = os.path.abspath("./profile.png")
gs.preloadTexture(profileimg)
gs.displayImageObject(10, profileimg, 0.1, 0.7)

# More messages
gs.displayMessageObject(3, "Monkey!", 0.7, 0.62, 0.9, 0.0, 1.0, 0.5, 18)
gs.sleep(1.5)
gs.displayMessageObject(4, "This is the fourth message", 0.4, 0.6, 1.0, 1.0, 0.0, 0.5, 17)
gs.sleep(1.5)
gs.displayMessageObject(5, "This is the fifth message", 0.5, 0.8, 1.0, 0.0, 1.0, 0.5, 22)
gs.sleep(1.5)
gs.removeObject(0)
gs.sleep(1.5)
gs.removeObject(1)
gs.sleep(1.5)
gs.removeObject(2)
gs.sleep(1.5)
gs.removeObject(3)
gs.sleep(1.5)
gs.removeObject(4)
gs.sleep(1.5)
gs.removeObject(5)
gs.sleep(1.5)
gs.removeObject(10)
gs.sleep(1.5)

# Maximize interface and enable input
gs.enableInput()

gateway.shutdown()
