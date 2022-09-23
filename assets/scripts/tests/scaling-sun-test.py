# Test script. Tests the scaling commands.
# Created by Toni Sagrista

import time
from py4j.clientserver import ClientServer, JavaParameters

def current_time_ms():
    return int(round(time.time() * 1000))

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.setObjectSizeScaling("Sun", 1.0)

gateway.shutdown()
