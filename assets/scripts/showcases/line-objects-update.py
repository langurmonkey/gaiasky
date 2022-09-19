# This script showcases lines and parked runnables
#
# The script creates a line object between the positions of the Earth and the Moon. Then,
# it parks a runnable which updates the line points with the new positions of the
# objects, so that the line is always up to date, even when the objects move. Finally,
# time is started to showcase the line movement.

# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters

class LineUpdaterRunnable(object):
    def __init__(self, polyline):
        self.polyline = polyline

    def run(self):
        earthp = gs.getObjectPosition("Earth")
        moonp = gs.getObjectPosition("Moon")
        pl = self.polyline.getPointCloud()

        pl.setX(0, earthp[0])
        pl.setY(0, earthp[1])
        pl.setZ(0, earthp[2])
        pl.setX(1, moonp[0])
        pl.setY(1, moonp[1])
        pl.setZ(1, moonp[2])
        
        self.polyline.markForUpdate()

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

gs.setFov(55.0)

gs.goToObject("Earth", 91.38e-2)

print("We will now add a line between the Earth and Moon")

earthp = gs.getObjectPosition("Earth")
moonp = gs.getObjectPosition("Moon")

gs.addPolyline("line-em", [earthp[0], earthp[1], earthp[2], moonp[0], moonp[1], moonp[2]], [ 1., .2, .2, .8 ], 1 )

gs.sleep(0.5)

# create line
line_em = gs.getLineObject("line-em")

# park the line updater
gs.parkRunnable("line-updater", LineUpdaterRunnable(line_em))

gs.setSimulationPace(1e5)
gs.startSimulationTime()

gs.sleep(20)

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

