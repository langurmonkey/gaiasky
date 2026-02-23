# This script tests the synchronous camera file playing.
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

gs.disableInput()
gs.cameraStop()

fname = os.path.abspath("./camera-path-test.gsc")
printall("(1/2) Starting synchronous camera file execution: %s" % fname)

t0 = time.time()
gs.runCameraPath(fname, True)
t1 = time.time()

printall("Sync exec: script regained control after %.4f seconds" % (t1 - t0))

printall("(2/2) Starting asynchronous camera file execution: %s" % fname)

t0 = time.time()
gs.runCameraPath(fname)
t1 = time.time()


printall("Async exec: script regained control after %.4f seconds" % (t1 - t0))

gs.enableInput()

printall("Script finishes")

gateway.close()
