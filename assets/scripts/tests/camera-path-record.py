# This script tests the camera path recording API call.
# Created by Toni Sagrista

import time, os
from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

# Prints to both Gaia Sky and Python logs
def printall(string):
    # print to gaia sky log
    gs.print(string)
    # print to python log
    print(string)

filename = "test_script_path"
printall("Recording camera path for 2 seconds with name '%s'" % filename)

gs.startRecordingCameraPath(filename)

gs.sleep(2.0)

gs.stopRecordingCameraPath()

printall("Script done")

gateway.close()
