# Test script. Tests getObjectPosition()
# Created by Toni Sagrista

import math
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

object_name = input("Object name: ")

obj = gs.getObject(object_name)
if obj is not None:
    # Change the units from "internal" to "km", "au", "ly", "pc", etc.
    a = gs.getObjectPosition(object_name, "internal")
    print("'%s' position (absolute): [%.8f, %.8f, %.8f]" % (object_name, a[0], a[1], a[2]))

    c = gs.getCameraPosition("internal")
    print("'%s' position (absolute): [%.8f, %.8f, %.8f]" % ("camera", c[0], c[1], c[2]))
    b = [a[0] - c[0], a[1] - c[1], a[2] - c[2]]
    print("'%s' position (camera): [%.8f, %.8f, %.8f]" % (object_name, b[0], b[1], b[2]))

else:
    print("Object '%s' does not exist" % object_name)



gateway.close()
