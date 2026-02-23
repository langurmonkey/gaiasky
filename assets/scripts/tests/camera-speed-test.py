# This script tests the setCameraSpeed() call.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.setCameraSpeed(10)
gs.sleep(1.0)
gs.setCameraSpeed(20)
gs.sleep(1.0)
gs.setCameraSpeed(30)
gs.sleep(1.0)
gs.setCameraSpeed(40)
gs.sleep(1.0)
gs.setCameraSpeed(50)
gs.sleep(1.0)
gs.setCameraSpeed(60)
gs.sleep(1.0)
gs.setCameraSpeed(70)
gs.sleep(1.0)
gs.setCameraSpeed(80)
gs.sleep(1.0)
gs.setCameraSpeed(90)
gs.sleep(1.0)
gs.setCameraSpeed(100)
gs.sleep(1.0)

gateway.close()
