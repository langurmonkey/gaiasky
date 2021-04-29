# This script tests the catalog load functionality.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
from os.path import expanduser

"""
Prints to both gaia sky and python
"""
def lprint(string):
    gs.print(string)
    print(string)

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.cameraStop()
gs.maximizeInterfaceWindow()

name = "hip-script"
# Modify this path to your own file!
assets = gs.getAssetsLocation()
gs.loadDataset(name, assets + "/scripts/tests/hip-subset.vot")

lprint("Dataset ready: %s" % name)
gs.sleep(4)

lprint("Hiding dataset: %s" % name)
gs.hideDataset(name)

gs.sleep(4)

lprint("Showing dataset: %s" % name)
gs.showDataset(name)

gs.sleep(4)

lprint("Removing dataset: %s" % name)
gs.removeDataset(name)

lprint("Test finished")

gateway.shutdown()
