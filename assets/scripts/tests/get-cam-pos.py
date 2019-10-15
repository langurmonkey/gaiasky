# Test script. Tests getObject() and getObjectRadius()
# Created by Toni Sagrista

import math
from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

cpos = gs.getCameraPosition()
print("Camera position: [%f, %f, %f]" %(cpos[0], cpos[1], cpos[2]))


gateway.close()
