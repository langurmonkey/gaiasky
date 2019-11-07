# This script showcases lines and parked runnables
#
# The script creates a polyline and updates it with a new segment
# between the current positions of Earth and Venus every 0.2 seconds.
#
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

class LineUpdaterRunnable(object):
    def __init__(self, polyline):
        self.polyline = polyline
        self.t0 = 0
        self.seq = 1

    def run(self):
        earthp = gs.getObjectPosition("Earth")
        venusp = gs.getObjectPosition("Venus")
        pl = self.polyline.getPointCloud()

        pl.setX(0, earthp[0])
        pl.setY(0, earthp[1])
        pl.setZ(0, earthp[2])
        pl.setX(1, venusp[0])
        pl.setY(1, venusp[1])
        pl.setZ(1, venusp[2])

        self.polyline.markForUpdate()
        
        currt = time.time()
        # Each 0.2 seconds
        if currt - self.t0 > 0.2:
            gs.addPolyline("line-%d" % self.seq, [earthp[0], earthp[1], earthp[2], venusp[0], venusp[1], venusp[2]], [ 0.8, 0.8, .2, .4 ], 1 )
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

gs.addPolyline("line-em", [earthp[0], earthp[1], earthp[2], venusp[0], venusp[1], venusp[2]], [ 1., .2, .2, .8 ], 1 )

gs.sleep(0.5)

# create line
line_em = gs.getObject("line-em")

# park the line updater
gs.parkRunnable("line-updater", LineUpdaterRunnable(line_em))

gs.setSimulationPace(3e6)
gs.startSimulationTime()

gs.sleep(120)

gs.stopSimulationTime()

# clean up and finish
print("Cleaning up and ending")

gs.unparkRunnable("line-updater")
gs.removeModelObject("line-em")
gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()

