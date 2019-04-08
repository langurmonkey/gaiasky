# This script tests the star brightness commands.
# Created by Toni Sagrista


from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.setStarBrightness(100.0)
gs.sleep(2)
gs.setStarBrightness(70.0)
gs.sleep(2)
gs.setStarBrightness(50.0)
gs.sleep(2)
gs.setStarBrightness(30.0)
gs.sleep(2)
gs.setStarBrightness(12.0)
gs.sleep(2)

gateway.close()