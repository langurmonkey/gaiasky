# This script takes a screenshot and ends.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.setScreenshotsMode("ADVANCED")
gs.takeScreenshot()

gateway.shutdown()
