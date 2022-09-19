# This script tests the planetarium, 360 and stereo mode commands
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.sleep(3)

gs.print("360 mode")
gs.setPanoramaMode(True)
gs.sleep(3)
gs.setPanoramaMode(False)
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
    gs.sleep(1)

gs.setStereoscopicMode(False)

gateway.shutdown()
