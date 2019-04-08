# This script tests the catalog load functionality.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

"""
Prints to both gaia sky and python
"""
def lprint(string):
    gs.print(string)
    print(string)

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.cameraStop()
gs.maximizeInterfaceWindow()

name = "hip-script"
# Modify this path to your own file!
gs.loadDataset(name, "/home/tsagrista/git/gaiasky/assets/assets-bak/data/hip.vot")

lprint("Dataset ready: %s" % name)

lprint("Hiding dataset: %s" % name)
gs.hideDataset(name)

gs.sleep(8)

lprint("Showing dataset: %s" % name)
gs.showDataset(name)

gs.sleep(8)

lprint("Removing dataset: %s" % name)
gs.removeDataset(name)

lprint("Test finished")

gateway.close()
