##
## Script that showcases an arbitrary transformation (rotation) to a dataset.
## This script gets the dataset name as input.
##

from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters
from time import time
import numpy as np

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True),
                      callback_server_parameters=CallbackServerParameters())
gs = gateway.entry_point


# Back up current settings to settings stack
gs.clearSettingsStack()
gs.backupSettings()

coord = gateway.jvm.gaiasky.util.coord.Coordinates

# Get all datasets
datasets = gs.listDatasets()

print("Here are all the available datasets:")
for dataset in datasets:
    print("- %s" % dataset)

ds_name = input("Give the name of the desired dataset: ")

op = input("What operation do you want to perform? (t: transform, c: clear): ")

if op == "t":
    # Rotate 45 degrees
    mat = gateway.jvm.gaiasky.util.math.Matrix4D()
    mat.rotate(0.0, 1.0, 0.0, 10.0)
    gs.setDatasetTransformationMatrix(ds_name, mat.getValues())
elif op == "c":
    gs.clearDatasetTransformationMatrix(ds_name)
else:
    print("Unknown operation: %s" % op)


# Restore settings from stack
gs.restoreSettings()
# Exit
gateway.close()
gateway.shutdown()
