# This script tests the cameraTransition command using steps.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import math
import numpy as np

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(string):
    gs.print(string)
    print(string)

""" Logistic sigmoid """
def expit(x, span):
    if x == 0.0:
        return 0.0
    if x == 1.0:
        return 1.0
    x = x * span - (span / 2.0)
    return math.exp(x) / (1.0 + math.exp(x))

def gen_expit(n, span):
    v = [0.0]
    for x in np.arange(0.0, 1.0, 1.0 / n):
        v.append(expit(x, span))

    v.append(1.0)
    return v

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
lprint("We use the logistic sigmoid method for the positions (starts and ends slow).")
lprint("We use the logit method for the orientations (starts and ends fast).")
steps = gen_expit(200, 25.0)
gs.cameraTransition([-5593.0417731364, 13008.1430225486, 1542.9688571213], 
                    "internal",
                    [0.3965101844, -0.9104556836, -0.1176865406],
                    [0.7060462888, 0.2205005155, 0.6729622283],
                    10.0,
                    "logisticsigmoid",
                    60.0,
                    "logit",
                    0.1,
                    True)

lprint("Script ended")

gateway.shutdown()
