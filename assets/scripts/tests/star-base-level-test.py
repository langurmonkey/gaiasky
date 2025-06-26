# This script tests the star size commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.maximizeInterfaceWindow()

gs.setMinStarOpacity(0.95)
gs.sleep(2)
gs.setMinStarOpacity(0.8)
gs.sleep(2)
gs.setMinStarOpacity(0.6)
gs.sleep(2)
gs.setMinStarOpacity(0.4)
gs.sleep(2)
gs.setMinStarOpacity(0.2)
gs.sleep(2)
gs.setMinStarOpacity(0.0)

gateway.shutdown()
