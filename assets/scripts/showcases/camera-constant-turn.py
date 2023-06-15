# This Gaia Sky script showcases a constant camera turn using a parked runnable.
#
# Created by Toni Sagrista.

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

class CameraUpdateRunnable(object):
    def __init__(self, gs, rotation_rate):
        self.gs = gs
        self.rotation_rate = rotation_rate
        self.prev_time = time.perf_counter()
        self.direction = [0.0, 0.0, 1.0]
        self.up = [0.0, 1.0, 0.0]
        self.prev_time = time.time()

        # Set the direction and up
        self.gs.setCameraDirection(self.direction)
        self.gs.setCameraUp(self.up)

    def run(self):
        self.time = time.time()

        # This is the number of seconds since the last frame
        dt = self.time - self.prev_time

        # Actual degrees to rotate this frame
        rot_deg = dt * self.rotation_rate

        # Rotate the direction angle around up by rot_deg degrees
        self.direction = self.gs.rotate3([self.direction[0], self.direction[1], self.direction[2]], [0.0, 1.0, 0.0], rot_deg)
        # Set it
        self.gs.setCameraDirection(self.direction, True)
        # We do not need to set the up vector, since it never changes


        # Store prev_time for use in next frame
        self.prev_time = self.time

    def toString():
        return "camera-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

gs.cameraStop()
gs.setCameraFree()

gs.stopSimulationTime()
gs.setVisibility("element.orbits", True)
gs.setCameraLock(True)
gs.setCameraOrientationLock(False)

gs.setFov(49)

# Rotation rate in deg/s
rotation_rate = 15.0

# park the camera updater
gs.parkRunnable("cam-updater", CameraUpdateRunnable(gs, rotation_rate))

gs.sleep(20)

# clean up and finish
print("Cleaning up and ending")

gs.unparkRunnable("cam-updater")
gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()
