# This script tests the getObjectRadius(obj) API call.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

radEarth = gs.getObjectRadius("Earth")
print("The radius of the Earth is %f Km" % radEarth)
gs.print("The radius of the Earth is %f Km" % radEarth)

radMoon = gs.getObjectRadius("Moon")
print("The radius of the Moon is %f Km" % radMoon)
gs.print("The radius of the Moon is %f Km" % radMoon)

gs.enableInput()

gateway.close()
