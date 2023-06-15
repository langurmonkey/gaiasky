# This script showcases lines and parked runnables by adding lines
# between Earth and Venus at constant timings.
#
# The script creates a polyline and updates it with a new segment
# between the current positions of Earth and Venus every 0.2 seconds.
#
# Created by Toni Sagrista.

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

class LineUpdaterRunnable(object):
    def __init__(self, plcurrent, plhistory):
        self.plcurrent = plcurrent
        self.plhistory = plhistory
        self.t0 = 0
        self.seq = 1

    def run(self):
        earthp = gs.getObjectPosition("Earth")
        venusp = gs.getObjectPosition("Venus")
        plc = self.plcurrent.getPointCloud()

        plc.setX(0, earthp[0])
        plc.setY(0, earthp[1])
        plc.setZ(0, earthp[2])
        plc.setX(1, venusp[0])
        plc.setY(1, venusp[1])
        plc.setZ(1, venusp[2])

        self.plcurrent.markForUpdate()
        
        currt = time.time()
        # Each 0.2 seconds
        if currt - self.t0 > 0.2:
            plh = self.plhistory.getPointCloud()
            plh.addPoint(earthp[0], earthp[1], earthp[2])
            plh.addPoint(venusp[0], venusp[1], venusp[2])
            self.plhistory.markForUpdate()
            self.seq = self.seq + 1
            self.t0 = currt



    def toString():
        return "line-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

gs.cameraStop()

gs.stopSimulationTime()
gs.setVisibility("element.orbits", True)
gs.setCameraLock(True)
gs.setCameraOrientationLock(False)

gs.setFov(49)

gs.setCameraFocus("Sun")
gs.setCameraPosition([-162198358.841994, 369442731.694199, -2670370.004923])

print("We will now add a line between the Earth and Venus")

earthp = gs.getObjectPosition("Earth")
venusp = gs.getObjectPosition("Venus")

# Create polyline objects
gs.addPolyline("line-current", [earthp[0], earthp[1], earthp[2], venusp[0], venusp[1], venusp[2]], [ 1., .2, .2, .8 ], 1)
gs.addPolyline("line-hist", [], [ .7, .7, .1, .7 ], 1, 1)

gs.sleep(0.5)

# Get polyline objects
line_curr = gs.getLineObject("line-current")
line_hist = gs.getLineObject("line-hist")

# park the line updater
gs.parkRunnable("line-updater", LineUpdaterRunnable(line_curr, line_hist))

gs.setSimulationPace(3e6)
gs.startSimulationTime()

gs.sleep(120)

gs.stopSimulationTime()

# clean up and finish
print("Cleaning up and ending")

gs.unparkRunnable("line-updater")
gs.removeModelObject("line-current")
gs.removeModelObject("line-hist")
gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()

