# This script tests adding lines between objects.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

"""
Concats two java arrays into one
"""
def concat(a, b):
    return [a[0], a[1], a[2], b[0], b[1], b[2]]

gs.cameraStop()

gs.stopSimulationTime()

gs.setCameraFocusInstantAndGo("Earth")

gs.print("We will now add lines between the positions of Earth-Moon, Earth-Sun, Earth-Mercury and Arcturus-Achernar")
gs.print("You will have 30 seconds to observe and explore the system before we remove the lines and end the script")

gs.sleep(2)

earthp = gs.getObjectPosition("Earth")
moonp = gs.getObjectPosition("Moon")
solp = gs.getObjectPosition("Sun")
mercuryp = gs.getObjectPosition("Mercury")
arcturusp = gs.getObjectPosition("Arcturus")
achernarp = gs.getObjectPosition("Achernar")

gs.addPolyline("Line0", concat(earthp, moonp), [ 1., .2, .2, .8 ], 1 )
gs.addPolyline("Line1", concat(earthp, solp), [ .2, 1., .2, .8 ], 2 )
gs.addPolyline("Line2", concat(earthp, mercuryp), [ 2., .2, 1., .8 ], 3 )
gs.addPolyline("Line3", concat(arcturusp, achernarp), [ 1., 1., .2, .8 ], 2 )

gs.print("Lines added, you have 30 seconds")

gs.sleep(30)

gs.print("Removing lines and ending")
gs.removeModelObject("Line0")
gs.removeModelObject("Line1")
gs.removeModelObject("Line2")
gs.removeModelObject("Line3")

gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()

gateway.close()
