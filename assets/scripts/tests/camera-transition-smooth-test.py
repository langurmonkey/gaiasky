# This script tests the cameraTransition command using the smoothing functions.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import math
import numpy as np

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(string):
    gs.print(string)
    print(string)

gs.cameraStop()

# Go to Earth
gs.setCameraFocusInstantAndGo("Earth")
gs.sleep(2)
gs.setCameraFree()

lprint("First, we do a normal linear transition")
# Normal transition.
gs.cameraTransition([-5593.0417731364, 13008.1430225486, 1542.9688571213], 
                    "internal",
                    [0.3965101844, -0.9104556836, -0.1176865406],
                    [0.7060462888, 0.2205005155, 0.6729622283],
                    10.0,
                    True)
gs.sleep(2)


# Go to Earth
gs.setCameraFocusInstantAndGo("Earth")
gs.sleep(2)
gs.setCameraFree()

lprint("Now, we do a smooth transition.")
lprint("The transition in position lasts 10 seconds, while the transition in orientation lasts 7 seconds.")
gs.cameraTransition([-5593.0417731364, 13008.1430225486, 1542.9688571213], 
                    "internal",
                    [0.3965101844, -0.9104556836, -0.1176865406],
                    [0.7060462888, 0.2205005155, 0.6729622283],
                    10.0,
                    "logisticsigmoid",
                    60.0,
                    7.0,
                    "logisticsigmoid",
                    12.0,
                    True)

lprint("Script ended")

gateway.close()
