# This script tests the reprojection mode.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point


gs.setFov(150.0)
gs.setReprojectionMode("stereographic_180")

gs.sleep(6.0)

gs.setReprojectionMode("disabled")
gs.setFov(55.0)


gateway.shutdown()
