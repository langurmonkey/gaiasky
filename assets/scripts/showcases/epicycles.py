# This script showcases lines and parked runnables

# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters

class LineUpdaterRunnable(object):

    def __init__(self):
        self.seq = -1
        self.frames = 0
        self.prevpos = None
        self.lnames = []

    def run(self):
        earthp = gs.getObjectPosition("Earth")
        marsp = gs.getObjectPosition("Mars")

        lpos = [marsp[0] - earthp[0], marsp[1] - earthp[1], marsp[2] - earthp[2]]

        if self.frames % 15 == 0:
            self.seq += 1
            if self.seq > 0:
                gs.addPolyline("line-em-%d" % self.seq, [self.prevpos[0], self.prevpos[1], self.prevpos[2], lpos[0], lpos[1], lpos[2]], [ 1., .2, .2, .8 ], 1 )
                self.lnames.append("line-em-%d" % self.seq)
            self.prevpos = lpos

        self.frames += 1

    def toString():
        return "line-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True),
                      callback_server_parameters=CallbackServerParameters())
gs = gateway.entry_point

gs.cameraStop()

gs.stopSimulationTime()
gs.setVisibility("element.orbits", True)
gs.setCameraLock(True)
gs.setCameraOrientationLock(False)

gs.setFov(49)
gs.setCameraFocus("Earth")
gs.setCameraPosition([-372392379.013749, 693877297.622799, 159753345.098243])

earthp = gs.getObjectPosition("Earth")
marsp = gs.getObjectPosition("Mars")

gs.sleep(0.5)

# park the line updater
lineUpdater = LineUpdaterRunnable()
gs.parkRunnable("line-updater", lineUpdater)

gs.setSimulationTime(2015, 11, 19, 0, 0, 0, 0)
gs.setSimulationPace(4e6)
gs.startSimulationTime()

gs.sleep(30)
gs.setVisibility("element.orbits", False)
gs.sleep(30)

gs.stopSimulationTime()

# clean up and finish
print("Cleaning up and ending")

gs.unparkRunnable("line-updater")
for lname in lineUpdater.lnames:
    gs.removeModelObject(lname)
gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.close()
