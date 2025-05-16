# Paths of Mars and the Sun in a frame of reference tied to Earth. Demonstrates paths in geocentric model.
# Created by Svetlin Tassev


from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

class LineUpdaterRunnable(object):

    def __init__(self, line,target):
        self.seq = -1
        self.frames = 0
        self.prevpos = None
        self.positions = []
        self.factor = 1.0
        self.lastt = 0.0
        self.line = line
        self.target=target
        self.simT=0

    def run(self):
        earthp = gs.getObjectPosition("Earth")
        marsp = gs.getObjectPosition(self.target)


        # Add line every .05 seconds
        currt = time.time()
        if ((currt - self.lastt >= 0.05) and ((gs.getSimulationTime()-self.simT)!=0)): 
            lpos = [marsp[0] - earthp[0], marsp[1] - earthp[1], marsp[2] - earthp[2]]
            
            self.seq += 1
            if self.seq > 1:
                pc = self.line.getPointCloud()
                if pc.getNumPoints() == 0:
                    # Add two first
                    pc.addPoint(self.prevpos[0], self.prevpos[1], self.prevpos[2])
                    pc.addPoint(lpos[0], lpos[1], lpos[2])
                else:
                    # Add one
                    pc.addPoint(lpos[0], lpos[1], lpos[2])

            # Save raw positions
            if self.seq > 0:
                self.positions.append([lpos[0], lpos[1], lpos[2]])
                self.prevpos = lpos
                self.lastt = currt

        # Update all lines to put center on Earth
        if (self.line is not None and len(self.positions) > 1)  and ((gs.getSimulationTime()-self.simT)!=0):
            
            pc = self.line.getPointCloud()
            #gs.print("Polyline: %d, positions: %d" % (pc.getNumPoints(), len(self.positions)))
            for i in range(pc.getNumPoints()):
                pc.set(i, self.positions[i][0] + earthp[0], self.positions[i][1] + earthp[1], self.positions[i][2] + earthp[2])
            self.line.markForUpdate()
        self.simT=gs.getSimulationTime()
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
gs.setCameraFocus("Earth")
gs.setSimulationTime(2022,9, 30, 0, 0, 0, 0)

gs.setCameraPosition([-378590332.03124994, 721963500.9765624, 109496810.91308592])
gs.setCameraDirection([0.4636272729518448, -0.8763825681700292, -0.13039687873158354])
gs.setCameraUp([0.2037368191119465, -0.037779009195493506, 0.9782965066902537])

gs.setSimulationPace(6e6)


gs.setObjectSizeScaling("Mars", 2000.0)
gs.setObjectSizeScaling("Earth", 2000.0)
gs.setObjectSizeScaling("Moon", 2000.0)
#gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0/100000)
gs.setObjectSizeScaling("Sun", 20.0)
gs.setOrbitCoordinatesScaling("MoonAACoordinates", 100.0)

gs.refreshAllOrbits();
gs.refreshObjectOrbit("Moon");
gs.forceUpdateScene();

gs.sleep(1.0)

a=0.6
w=0

gs.addTrajectoryLine("line-eM", [], [ .75, .25, .25, a ] )
lineM = gs.getLineObject("line-eM", 10.0)
# park the line updater
lineUpdaterM = LineUpdaterRunnable(lineM,"Mars")
gs.parkRunnable("line-updaterM", lineUpdaterM)


#gs.addPolyline("line-eSun", [], [ 1.0, 1.0,1.0, a/6. ], 2. )
gs.addTrajectoryLine("line-eSun", [], [ 1.0, 1.0,1.0, 1. ] )

lineSun = gs.getLineObject("line-eSun", 10.0)
# park the line updater
lineUpdaterSun = LineUpdaterRunnable(lineSun,"Sun")
gs.parkRunnable("line-updaterSun", lineUpdaterSun)

gs.sleep(1.0)

gs.startSimulationTime()

gs.sleep(10)
gs.setVisibility("element.orbits", False)
gs.sleep(20)

gs.stopSimulationTime()

gs.sleep(1)

# clean up and finish
gs.unparkRunnable("line-updaterM")
gs.removeModelObject("line-eM")

gs.unparkRunnable("line-updaterSun")
gs.removeModelObject("line-eSun")
gs.cameraStop()
# Finish flushing
gs.sleepFrames(4)
#gs.sleep(1)


gs.setObjectSizeScaling("Mars", 1.0)
gs.setObjectSizeScaling("Earth", 1.0)
gs.setObjectSizeScaling("Moon", 1.0)
gs.setObjectSizeScaling("Sun", 1.0)
gs.setOrbitCoordinatesScaling("MoonAACoordinates", 1.0)

gs.refreshAllOrbits();
gs.refreshObjectOrbit("Moon");
gs.forceUpdateScene();

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()
