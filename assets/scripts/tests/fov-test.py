# Test script. Tests setting the fov.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.print("Changing field of view")

gs.setFov(10.0)
gs.sleep(1)
gs.setFov(20.0)
gs.sleep(1)
gs.setFov(30.0)
gs.sleep(1)
gs.setFov(40.0)
gs.sleep(1)
gs.setFov(50.0)
gs.sleep(1)
gs.setFov(60.0)
gs.sleep(1)
gs.setFov(70.0)
gs.sleep(1)
gs.setFov(80.0)
gs.sleep(1)
gs.setFov(60.0)

gs.enableInput()

gateway.close()
