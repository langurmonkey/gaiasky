# Test script. Tests getObject() and getObjectRadius()
# Created by Toni Sagrista

import math
from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface


gs = EventScriptingInterface.instance()

gs.disableInput()
gs.cameraStop()

gs.setVisibility("element.propermotions", True)

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

gs.setVisibility("element.propermotions", False)

gs.enableInput()
