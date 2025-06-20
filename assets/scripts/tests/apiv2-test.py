# Test script. This class tests access to the new APIv2.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True, auto_field=True))
apiv2 = gateway.entry_point.apiv2
# Base module
base = apiv2.base
# Camera module
camera = apiv2.camera

base.sleep(10)

gateway.shutdown()
