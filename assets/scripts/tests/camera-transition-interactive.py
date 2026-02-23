# This script tests the cameraTransition command, accepting position, direction
# and up as user input.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import math
import numpy as np

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(text: str):
    gs.print(text)
    print(text)

gs.cameraStop()

gs.setCameraFree()

lprint("Camera transition parameters. Enter the arrays separated by comas, without brackets.")
pos_str = input("Target position [px, py, pz], internal units: ")
pos = np.asarray(np.fromstring(pos_str, dtype=float, sep=','))

dir_str = input("Target direction [vx, vy, vz]: ")
dir = np.asarray(np.fromstring(dir_str, dtype=float, sep=','))

up_str = input("Target up [ux, uy, yz]: ")
up = np.asarray(np.fromstring(up_str, dtype=float, sep=','))

pos_time_str = input("Position transition time [seconds]: ")
pos_time = float(pos_time_str)


ori_time_str = input("Orientation transition time [seconds]: ")
ori_time = float(ori_time_str)

gs.cameraTransition(pos, 
                    "internal",
                    dir,
                    up,
                    pos_time,
                    "logisticsigmoid",
                    60.0,
                    ori_time,
                    "logisticsigmoid",
                    12.0,
                    True)

lprint("Script ended")

gateway.close()
