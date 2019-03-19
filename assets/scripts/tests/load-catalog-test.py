# This script tests the catalog load functionality.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.cameraStop()
gs.maximizeInterfaceWindow()

# Modify this path to your own file!
gs.loadDataset("hip-script", "/home/tsagrista/git/gaiasky/assets/assets-bak/data/hip.vot")

gs.sleep(8)

gs.hideDataset("hip-script")

gs.sleep(8)

gs.showDataset("hip-script")

gateway.close()
