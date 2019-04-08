# This script tests the planetarium, 360 and stereo mode commands
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.sleep(3)

gs.print("360 mode")
gs.set360Mode(True)
gs.sleep(3)
gs.set360Mode(False)
gs.sleep(3)

gs.print("Planetarium mode")
gs.setPlanetariumMode(True)
gs.sleep(3)
gs.setPlanetariumMode(False)
gs.sleep(3)

gs.print("Stereoscopic mode and profiles")
gs.setStereoscopicMode(True)
gs.sleep(3)

for i in range(5):
    gs.setStereoscopicProfile(i)
    gs.sleep(3)

gs.setStereoscopicMode(False)

gateway.close()
