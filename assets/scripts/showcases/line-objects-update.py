# This script showcases lines and parked runnables.
#
# The script creates a line object between the positions of the Earth and the Moon. Then,
# it parks a runnable which updates the line every frame with the new positions of the
# objects, so that the line is always up to date, even if the objects change position. 
# Every few seconds, we add a new line (in green) with the current state, so that we can
# track the past states of the line.
# Finally, time is started to showcase the line movement.
#
# Created by Toni Sagrista.

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

class LineUpdaterRunnable(object):
    def __init__(self, polyline):
        self.polyline = polyline
        self.lastTime = -1
        self.lines = 0

    def run(self):
        # Here goes the code that runs every frame.
        # It gets the positions of Earth and Moon, and
        # updates the two ends of the line with them.
        earthp = gs.getObjectPosition("Earth")
        moonp = gs.getObjectPosition("Moon")
        pl = self.polyline.getPointCloud()

        pl.setX(0, earthp[0])
        pl.setY(0, earthp[1])
        pl.setZ(0, earthp[2])
        pl.setX(1, moonp[0])
        pl.setY(1, moonp[1])
        pl.setZ(1, moonp[2])

        # Persist line every 0.2 seconds
        now = time.time()
        if now - self.lastTime > 0.2:
            gs.addPolyline("line-em-" + str(self.lines), [earthp[0], earthp[1], earthp[2], moonp[0], moonp[1], moonp[2]], [ 0.3, 0.9, 0.1, 0.8 ], 1 )
            self.lines = self.lines + 1
            self.lastTime = now
             
        
        # We need to mark the line for update, otherwise
        # our changes won't be streamed to graphics memory.
        self.polyline.markForUpdate()

    def toString():
        return "line-update-runnable"

    class Java:
        # This is important, it makes the Java class to implement
        # the Runnable interface, which is required by the parkRunnable() method!
        implements = ["java.lang.Runnable"]

# First, create the gateway and entry point.
gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

# Here we just do some preparation.
gs.cameraStop()

gs.stopSimulationTime()
gs.setVisibility("element.orbits", True)
gs.setCameraLock(True)
gs.setCameraOrientationLock(False)
gs.setFov(55.0)

# Move to the Earth.
gs.goToObject("Earth", 91.38e-2)

print("We will now add a line between the Earth and Moon")

# Get current positions of Earth and Moon.
earthp = gs.getObjectPosition("Earth")
moonp = gs.getObjectPosition("Moon")

# Add a polyline identified by "line-em" with the current positions.
gs.addPolyline("line-em", [earthp[0], earthp[1], earthp[2], moonp[0], moonp[1], moonp[2]], [ 0.9, 0.3, 0.1, 0.8 ], 1 )

gs.sleep(0.5)

# Get the model line object using the same name ("line-em").
line_em = gs.getLineObject("line-em")

# Park the line updater.
# The LineUpdaterRunnable must implement java.lang.Runnable.
runnable = LineUpdaterRunnable(line_em)
gs.parkRunnable("line-updater", runnable)

# Set time pace and start time.
gs.setSimulationPace(1e5)
gs.startSimulationTime()

# Wait for a bit.
gs.sleep(20)

# Stop time.
gs.stopSimulationTime()

# Clean up and finish.
print("Cleaning up and ending")

gs.unparkRunnable("line-updater")

gs.removeModelObject("line-em")
for i in range(0, runnable.lines, 1):
    gs.removeModelObject("line-em-" + str(i))


gs.cameraStop()

# Gracefully close connection.
gateway.shutdown()
