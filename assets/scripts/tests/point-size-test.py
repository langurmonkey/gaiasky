# This script tests the star size commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.maximizeInterfaceWindow()

gs.setStarSize(20.0)
gs.sleep(2)
gs.setStarSize(15.0)
gs.sleep(2)
gs.setStarSize(10.0)
gs.sleep(2)
gs.setStarSize(5.0)
gs.sleep(2)
gs.setStarSize(1.0)
gs.sleep(2)
gs.setStarSize(3.0)

gateway.close()
