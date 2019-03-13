# This script tests the planetarium, 360 and stereo mode commands
# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface

from gaia.cu9.ari.gaiaorbit.event import EventManager
from gaia.cu9.ari.gaiaorbit.event import Events


gs = EventScriptingInterface.instance()

gs.sleep(3)

gs.print("360 mode")
gs.set360Mode(True)
gs.sleep(3)
gs.set360Mode(False)
gs.sleep(3)

gs.print("Planetarium mode")
gs.setPlanetariumMode(True)
gs.sleep(3)
gs.setPlanetariumMode(False)
gs.sleep(3)

gs.print("Stereoscopic mode and profiles")
gs.setStereoscopicMode(True)
gs.sleep(3)

for i in range(5):
    gs.setStereoscopicProfile(i)
    gs.sleep(3)

gs.setStereoscopicMode(False)
