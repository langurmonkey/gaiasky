# This script tests the planetarium, 360 and stereo mode commands
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.setCameraFocus("Sun")
gs.sleep(3)

gs.setCinematicCamera(True)
gs.setRotationCameraSpeed(20.0)
gs.cameraRotate(1.0, 0.0)

gs.print("Bloom")
gs.setBloom(1.0)
gs.sleep(4)
gs.setBloom(0.5)
gs.sleep(4)
gs.setBloom(0.1)
gs.sleep(4)
gs.setBloom(0.0)

gs.print("Star glow")
gs.setStarGlow(False)
gs.sleep(4)
gs.setStarGlow(True)
gs.sleep(4)

gs.print("Lens flare")
gs.setLensFlare(True)
gs.sleep(4)
gs.setLensFlare(False)
gs.sleep(4)
gs.setLensFlare(True)

gs.setCinematicCamera(False)

gateway.close()
