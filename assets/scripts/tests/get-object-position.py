# Test script. Tests getObjectPosition()
# Created by Toni Sagrista

import math
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

object_name = input("Object name: ")

obj = gs.getObject(object_name)
if obj is not None:
    a = gs.getObjectPosition(object_name)
    print("'%s' position: [%.8f, %.8f, %.8f]" % (object_name, a[0], a[1], a[2]))
else:
    print("Object '%s' does not exist" % object_name)

gateway.shutdown()
