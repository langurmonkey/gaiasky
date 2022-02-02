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

# Let's look in the general direction of the dataset
gs.setCameraFree()
lookat = gs.equatorialToInternalCartesian(0.0, 0.0, 1.0)
gs.setCameraDirection(lookat)

# Let's load the data
name = "hip-script"
# Modify this path to your own file!
assets = gs.getAssetsLocation()
gs.loadStarDataset(name, assets + "/scripts/tests/hip-subset.vot", 2.0, [0.3, 0.4, 0.9, 0.6], None, None, True)

gs.sleep(3)

lprint("Highlighting dataset: %s" % name)
gs.highlightDataset(name, "RA", "rainbow", 0.0, 15.0, True)

gs.sleep(4)

lprint("Removing dataset: %s" % name)
gs.removeDataset(name)

lprint("Test finished")

gateway.shutdown()
