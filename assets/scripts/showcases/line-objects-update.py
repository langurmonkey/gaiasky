# This script showcases lines and parked runnables
#
# The script creates a line object between the positions of the Earth and the Moon. Then,
# it parks a runnable which updates the line points with the new positions of the
# objects, so that the line is always up to date, even when the objects move. Finally,
# time is started to showcase the line movement.

# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface
from gaia.cu9.ari.gaiaorbit.scenegraph import Polyline
from java.lang import Runnable



class LineUpdaterRunnable(Runnable):
    def __init__(self, polyline):
        self.polyline = polyline
        
    def run(self):
        earthp = gs.getObjectPosition("Earth")
        moonp = gs.getObjectPosition("Moon")
        pl = self.polyline.getPolyline()
            
        pl.x.set(0, earthp[0])
        pl.y.set(0, earthp[1])
        pl.z.set(0, earthp[2])
        pl.x.set(1, moonp[0])
        pl.y.set(1, moonp[1])
        pl.z.set(1, moonp[2])
        
        self.polyline.markForUpdate()

gs = EventScriptingInterface.instance()

gs.cameraStop()

gs.stopSimulationTime()

gs.setFov(49)

gs.goToObject("Earth", 91.38e-2)

print("We will now add a line between the Earth and Moon")

gs.sleep(2)

earthp = gs.getObjectPosition("Earth")
moonp = gs.getObjectPosition("Moon")

gs.addPolyline("line-em", earthp + moonp, [ 1., .2, .2, .8 ], 1 )

gs.sleep(1.0)

line_em = gs.getObject("line-em")

gs.parkRunnable("line-updater", LineUpdaterRunnable(line_em))

gs.setSimulationPace(65536.0)
gs.startSimulationTime()

gs.sleep(30)

gs.stopSimulationTime()

print("Cleaning up and ending")

gs.unparkRunnable("line-updater")
gs.removeModelObject("line-em")
gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()
