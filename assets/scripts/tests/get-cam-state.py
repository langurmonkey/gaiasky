# Test script. Tests getCameraPosition(units) with different units,
# getCameraDirection() and getCameraUp(). 
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


print()
print("Camera orientation:")
dir = gs.getCameraDirection()
print( " -  Direction:     [%.10f, %.10f, %.10f]" %(dir[0], dir[1], dir[2]))
up = gs.getCameraUp()
print( " -  Up:     [%.10f, %.10f, %.10f]" %(up[0], up[1], up[2]))

gateway.shutdown()
