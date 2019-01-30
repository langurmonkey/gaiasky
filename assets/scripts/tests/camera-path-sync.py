# This script tests the synchronous camera file playing.
# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface
import time

gs = EventScriptingInterface.instance()

gs.disableInput()
gs.cameraStop()
gs.minimizeInterfaceWindow()

fname = "/home/tsagrista/.gaiasky/script/camera-path-test.gsc"
gs.print("(1/2) Starting synchronous camera file execution: %s" % fname)

t0 = time.time()
gs.runCameraPath(fname, True)
t1 = time.time()

gs.print("Sync exec: script regained control after %.4f seconds" % (t1 - t0))

gs.print("(2/2) Starting asynchronous camera file execution: %s" % fname)

t0 = time.time()
gs.runCameraPath(fname)
t1 = time.time()

gs.print("Async exec: script regained control after %.4f seconds" % (t1 - t0))

gs.maximizeInterfaceWindow()
gs.enableInput()

gs.print("Script finishes")
