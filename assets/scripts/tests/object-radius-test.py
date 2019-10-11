# This script tests the positioning of the camera with relation to two objects.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

radEarth = gs.getObjectRadius("Earth")
gs.print("The radius of the Earth is %f Km" % radEarth)

radMoon = gs.getObjectRadius("Moon")
gs.print("The radius of the Moon is %f Km" % radMoon)

gs.enableInput()

gateway.close()
