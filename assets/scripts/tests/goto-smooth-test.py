# This script tests the go-to API calls.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def pprint(text):
   gs.print(text)
   print(text)


gs.cameraStop()

gs.goToObjectInstant("Earth")
pprint("Regular go-to Mars")
gs.sleep(2.0)

gs.setCinematicCamera(True)
gs.goToObject("Mars")

gs.sleep(4.0)

gs.goToObjectInstant("Earth")
pprint("Smooth go-to Mars")
gs.sleep(2.0)

gs.goToObjectSmooth("Mars", 13.0, 5.0)
gs.setCameraFocus("Mars")
gs.sleep(3.0)

gs.setCinematicCamera(False)

gateway.shutdown()
