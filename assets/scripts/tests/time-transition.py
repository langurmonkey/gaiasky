# This script tests the timeTransition command using the smoothing functions.
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

# Go to Solar System view, on Jan 1, 2020.
gs.setSimulationTime(2020,
                      1,
                      1,
                      10,
                      0,
                      0,
                      0)
gs.setCameraFree()
gs.setCameraPosition([-1099.6305943305, 2548.5724614622, 17.3116216262], "internal", True)
gs.setCameraDirection([0.4008511509, -0.9161201844, -0.0064932645], True)
gs.setCameraUp([0.1870734144, 0.0749121531, 0.9794854297], True)
gs.sleep(2)

lprint("We start a smooth transition in time from 2020 to 2025.")
gs.timeTransition(2025,
                  1,
                  1,
                  10,
                  0,
                  0,
                  0,
                  10.0,
                  "logisticsigmoid",
                  12.0,
                  True)

lprint("Script ended")

gateway.close()
