# This script tests the colormap dataset highlighting functionality.
# It loads a 15 degree square region and maps it in alpha and delta successively
# using different color map schemes.
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

cmaps = ["reds", "greens", "blues", "rainbow18", "rainbow", "seismic", "carnation", "hotmetal", "cool"]
cmapi = 0

gs.sleep(3.0)

gs.setCameraFocus("2548")
for i in range(8):
    gs.highlightDataset(name, "RA", cmaps[cmapi], 0.0, 15.0, True)
    cmapi = (cmapi + 1) % len(cmaps)
    gs.sleep(1)
    gs.highlightDataset(name, "DEC", cmaps[cmapi], 0.0, 15.0, True)
    cmapi = (cmapi + 1) % len(cmaps)
    gs.sleep(1)


gs.removeDataset("hip-script")
lprint("Test finished")

gateway.close()
