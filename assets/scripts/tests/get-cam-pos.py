# Test script. Tests getCameraPosition(units) with different units. 
# Created by Toni Sagrista

import math
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

print("Camera position:")
pos = gs.getCameraPosition("internal")
print( " -  Internal:     [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = gs.getCameraPosition("km")
print( " -  Km:           [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = gs.getCameraPosition("au")
print( " -  AU:           [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = gs.getCameraPosition("ly")
print( " -  Light years:  [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = gs.getCameraPosition("pc")
print( " -  Parsecs:      [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))

gateway.shutdown()
