# Test script. Tests visibility commands.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setVisibility("element.ecliptic", True)
gs.sleep(4)
gs.setVisibility("element.ecliptic", False)

gs.enableInput()

gateway.close()