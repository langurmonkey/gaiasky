# This script tests the synchronous camera file playing.
# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface
import time

gs = EventScriptingInterface.instance()

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

fname = "/home/tsagrista/.gaiasky/script/camera-path-test.gsc"
print("(1/2) Starting synchronous camera file execution: " + fname)

t0 = time.time()
gs.runCameraPath(fname, True)
t1 = time.time()

print("Sync exec: script regained control after %.4f seconds" % (t1 - t0))

print("(2/2) Starting asynchronous camera file execution: " + fname)

t0 = time.time()
gs.runCameraPath(fname)
t1 = time.time()

print("Async exec: script regained control after %.4f seconds" % (t1 - t0))

gs.maximizeInterfaceWindow()
gs.enableInput()

print("Script finishes")
