# This script tests the back-buffer scaling factor.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point


gs.setBackBufferScale(0.5)

gs.sleep(5.0)

gs.setBackBufferScale(1.0)


gateway.close()
