# Test script. Tests the scaling commands.
# Created by Toni Sagrista

import time
from py4j.clientserver import ClientServer, JavaParameters


gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point


# Scale JWST
gs.setObjectSizeScaling("JWST", 00500000.0)


gateway.shutdown()
