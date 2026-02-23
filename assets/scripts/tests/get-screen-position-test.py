# Test script. Tests getObjectScreenCoordinates(name). 
# Created by Toni Sagrista

import math
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

name = input("Object name: ")

pos = gs.getObjectScreenCoordinates(name);

if pos is not None:
    print("Screen coordinates [px]: [%f, %f]" % (pos[0], pos[1]))
else:
    print("Object does not exist, or is off-screen.")

gateway.close()
