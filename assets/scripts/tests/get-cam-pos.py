# Test script. Tests getObject() and getObjectRadius()
# Created by Toni Sagrista

import math
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

cpos = gs.getCameraPosition()
print("Camera position: [%f, %f, %f]" %(cpos[0], cpos[1], cpos[2]))

gateway.shutdown()
