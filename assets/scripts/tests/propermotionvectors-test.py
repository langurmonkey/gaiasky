# Test script. Tests getObject() and getObjectRadius()
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setVisibility("element.velocityvectors", True)

gs.sleep(1)

gs.setProperMotionsNumberFactor(1)
gs.sleep(0.5)
gs.setProperMotionsNumberFactor(20)
gs.sleep(0.5)
gs.setProperMotionsNumberFactor(60)
gs.sleep(0.5)
gs.setProperMotionsNumberFactor(80)
gs.sleep(0.5)
gs.setProperMotionsNumberFactor(100)
gs.sleep(0.5)


gs.setProperMotionsLengthFactor(500)
gs.sleep(0.5)
gs.setProperMotionsLengthFactor(2000)
gs.sleep(0.5)
gs.setProperMotionsLengthFactor(8000)
gs.sleep(0.5)
gs.setProperMotionsLengthFactor(15000)
gs.sleep(0.5)
gs.setProperMotionsLengthFactor(30000)
gs.sleep(0.5)

gs.setVisibility("element.velocityvectors", False)

gs.enableInput()

gateway.close()
