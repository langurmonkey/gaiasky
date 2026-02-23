# Test script. Gets and prints the current camera state. 
# Created by Toni Sagrista

import math
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True, auto_field=True))
apiv2 = gateway.entry_point.apiv2
cam = apiv2.camera

print("Camera position:")
pos = cam.get_position("internal")
print( " -  Internal:     [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = cam.get_position("km")
print( " -  Km:           [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = cam.get_position("au")
print( " -  AU:           [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = cam.get_position("ly")
print( " -  Light years:  [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))
pos = cam.get_position("pc")
print( " -  Parsecs:      [%.10f, %.10f, %.10f]" %(pos[0], pos[1], pos[2]))


print()
print("Camera orientation:")
dir = cam.get_direction()
print( " -  Direction:     [%.10f, %.10f, %.10f]" %(dir[0], dir[1], dir[2]))
up = cam.get_up()
print( " -  Up:     [%.10f, %.10f, %.10f]" %(up[0], up[1], up[2]))

gateway.close()
