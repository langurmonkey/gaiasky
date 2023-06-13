# This script showcases lines and parked runnables

# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

class LineUpdaterRunnable(object):

    def __init__(self, line):
        self.seq = -1
        self.frames = 0
        self.prevpos = None
        self.positions = []
        self.factor = 4.0
        self.lastt = 0.0
        self.line = line

    def run(self):
        earthp = gs.getObjectPosition("Earth")
        marsp = gs.getObjectPosition("Mars")

        lpos = [marsp[0] - earthp[0], marsp[1] - earthp[1], marsp[2] - earthp[2]]
        # Scale up
        lpos = [lpos[0] * self.factor, lpos[1] * self.factor, lpos[2] * self.factor]

        # Add line every .15 seconds
        currt = time.time()
        if currt - self.lastt >= 0.15:
            self.seq += 1
            if self.seq > 0:
                pc = self.line.getPointCloud()
                if pc.getNumPoints() == 0:
                    # Add two first
                    pc.addPoint(self.prevpos[0], self.prevpos[1], self.prevpos[2])
                    pc.addPoint(lpos[0], lpos[1], lpos[2])
                else:
                    # Add one
                    pc.addPoint(lpos[0], lpos[1], lpos[2])

            # Save raw positions
            self.positions.append([lpos[0], lpos[1], lpos[2]])
            self.prevpos = lpos
            self.lastt = currt

        # Update all lines to put center on Earth
        if self.line is not None and len(self.positions) > 1:
            pc = self.line.getPointCloud()
            #gs.print("Polyline: %d, positions: %d" % (pc.getNumPoints(), len(self.positions)))
            for i in range(pc.getNumPoints()):
                pc.setX(i, self.positions[i][0] + earthp[0])
                pc.setY(i, self.positions[i][1] + earthp[1])
                pc.setZ(i, self.positions[i][2] + earthp[2])
            self.line.markForUpdate()

        self.frames += 1

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
gs.setVisibility("element.others", True)
gs.setCameraLock(True)
gs.setCameraOrientationLock(False)

gs.setFov(60)
gs.setCameraFocus("Sun")
gs.setCameraPosition([650591440.987582, -1443344531.151316, -219531339.581399])

earthp = gs.getObjectPosition("Earth")
marsp = gs.getObjectPosition("Mars")

gs.addTrajectoryLine("line-em", [], [ 1., .2, .2, .8 ], 0.0 )
line = gs.getLineObject("line-em", 10.0)

gs.sleep(0.5)

# park the line updater
lineUpdater = LineUpdaterRunnable(line)
gs.parkRunnable("line-updater", lineUpdater)

gs.setSimulationTime(2015, 11, 19, 0, 0, 0, 0)

gs.sleep(5)

gs.setSimulationPace(4e6)
gs.startSimulationTime()

gs.sleep(10)
gs.setVisibility("element.orbits", False)
gs.sleep(20)

gs.stopSimulationTime()

# clean up and finish
gs.unparkRunnable("line-updater")
gs.removeModelObject("line-em")
gs.cameraStop()
# Finish flushing
gs.setVisibility("element.orbits", True)
gs.sleepFrames(4)

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()
