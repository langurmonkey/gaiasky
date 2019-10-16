# Test script. Tests GUI scroll movement commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setGuiScrollPosition(20.0)
gs.sleep(1)
gs.setGuiScrollPosition(40.0)
gs.sleep(1)
gs.setGuiScrollPosition(60.0)
gs.sleep(1)
gs.setGuiScrollPosition(80.0)
gs.sleep(1)
gs.setGuiScrollPosition(100.0)
gs.sleep(1)

gs.enableInput()

gateway.shutdown()
