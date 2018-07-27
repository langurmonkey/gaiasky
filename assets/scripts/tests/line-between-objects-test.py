# This script tests adding lines between objects.
# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface


gs = EventScriptingInterface.instance()

gs.cameraStop()

gs.stopSimulationTime()

gs.setCameraFocusInstantAndGo("Earth")

print("We will now add lines between Earth-Moon, Earth-Sol, Earth-Mercury and Arcturus-Achernar")
print("You will have 30 seconds to observe and explore the system before we remove the lines and end the script")

gs.sleep(2)

earthp = gs.getObjectPosition("Earth")
moonp = gs.getObjectPosition("Moon")
solp = gs.getObjectPosition("Sol")
mercuryp = gs.getObjectPosition("Mercury")
arcturusp = gs.getObjectPosition("Arcturus")
achernarp = gs.getObjectPosition("Achernar")

gs.addPolyline("Line0", earthp + moonp, [ 1., .2, .2, .8 ], 1 )
gs.addPolyline("Line1", earthp + solp, [ .2, 1., .2, .8 ], 2 )
gs.addPolyline("Line2", earthp + mercuryp, [ 2., .2, 1., .8 ], 3 )
gs.addPolyline("Line3", arcturusp + achernarp, [ 1., 1., .2, .8 ], 2 )

print("Lines added, you have 30 seconds")

gs.sleep(30)

print("Removing lines and ending")
gs.removeModelObject("Line0")
gs.removeModelObject("Line1")
gs.removeModelObject("Line2")
gs.removeModelObject("Line3")

gs.cameraStop()

gs.maximizeInterfaceWindow()
gs.enableInput()
