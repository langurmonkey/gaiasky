# This script adds lines all over the sky following a galactic grid.
#
# Created by Toni Sagrista.

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time
from numpy import arange

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

gs.cameraStop()

gs.stopSimulationTime()
gs.setVisibility("element.orbits", False)
gs.setVisibility("element.galactic", True)
gs.setVisibility("element.others", True)

gs.setFov(55)

def float_range(start, stop, step):
    while start < stop:
        yield float(start)
        start += decimal.Decimal(step)
    
pc_to_km = 3.0856776e13

names = []
# Loop over l and b
for l in arange(0.0, 360.0, 20.0):
    for b in arange(-90.0, 90.0, 10.0):
        start = gs.galacticToInternalCartesian(l, b, 10 * pc_to_km)
        end = gs.galacticToInternalCartesian(l - 10.0, b, 10 * pc_to_km)

        name = "arrow-%d-%d" % (l, b)
        gs.addPolyline(name, [start[0], start[1], start[2], end[0], end[1], end[2]], [0.8, l / 360.0, (b + 90.0) / 180.0, 1.0], 2, True)
        names.append(name)


# Remove the next four lines if you want to leave the lines in
gs.sleep(30)
# clean up and finish
print("Cleaning up and ending")

for name in names:
    gs.removeModelObject(name)

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()

