# This script 
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.landAtObjectLocation("Earth", 2.0, 45.0)

gateway.close()
