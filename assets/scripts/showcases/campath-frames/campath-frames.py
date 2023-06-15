# This script showcases the combination of camera paths with 
# scripting by counting frames.
#
# Created by Toni Sagrista.

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time, os, sys

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
# Entry point
gs = gateway.entry_point

# Frame rate of our camera path
targetFramerate = 60.0

class FrameCounter(object):

    def __init__(self):
        self.frames = 0
        self.seconds = 0

    def run(self):
        # This contains the frame number
        self.frames += 1
        # This contains the seconds since the start of the runnable
        self.seconds = self.frames / targetFramerate

        # self.frames contains the frame number, which coincides with
        # the line number in the camera path file
        # Just check the frame number and issue the desired command
        # This is essentially a switch-case. There are better ways to do this
        # in Python with a dictionary pointing to the functions:
        # see https://jaxenter.com/implement-switch-case-statement-python-138315.html
        if self.frames == 200:
            self.setVisibility("element.labels", True)
        elif self.frames == 500:
            self.setVisibility("element.labels", False)
        elif self.frames == 800:
            self.setVisibility("element.orbits", True)
        elif self.frames == 1700:
            self.setVisibility("element.orbits", False)
        elif self.frames == 2400:
            self.setVisibility("element.clusters", True)
        elif self.frames == 3000:
            self.setVisibility("element.clusters", False)
        elif self.frames == 3200:
            self.setVisibility("element.velocityvectors", True)
        elif self.frames == 3600:
            self.setVisibility("element.velocityvectors", False)
        elif self.frames == 4000:
            self.setVisibility("element.meshes", True)
        elif self.frames == 5000:
            self.setVisibility("element.meshes", False)

    def setVisibility(self, element, state):
            gs.setVisibility(element, state)
            elemname = element[element.index(".") + 1:]
            print("%s visibility %s : frame %d, second %f" % (elemname, state, self.frames, self.seconds))


    def toString():
        return "frame-counter-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

gs.stopSimulationTime()

# Always set camera free before camera path
# Not strictly necessary but desired
gs.setCameraFree()

# Limit Gaia Sky FPS to target
gs.setLimitFps(targetFramerate)

# park the frame counter
frameCounter = FrameCounter()
gs.parkRunnable("frame-counter", frameCounter)

# start the camera path file
pathname = os.path.dirname(sys.argv[0])
scriptpath = os.path.abspath(pathname) + "/camerapath_60fps.gsc"
print("Running %s" % scriptpath)
gs.playCameraPath(scriptpath, True)

# Unpark the runnable
gs.unparkRunnable("frame-counter")

# Here you can repeat the process with a different camera path and runnable
# You can use as many as you want

# Unlimit frame rate
gs.setLimitFps(0)

# close connection
gateway.shutdown()
