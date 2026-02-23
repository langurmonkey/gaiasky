# Test script. Tests brightness and contrast commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.print("Testing brightness")

gs.setBrightnessLevel(-1.0)
gs.sleep(1)
gs.setBrightnessLevel(-0.5)
gs.sleep(1)
gs.setBrightnessLevel(-0.0)
gs.sleep(1)
gs.setBrightnessLevel(0.5)
gs.sleep(1)
gs.setBrightnessLevel(1.0)
gs.sleep(1)
gs.setBrightnessLevel(0.0)
gs.sleep(1)

gs.print("Testing contrast")

gs.setContrastLevel(0.0)
gs.sleep(1)
gs.setContrastLevel(0.5)
gs.sleep(1)
gs.setContrastLevel(1.0)
gs.sleep(1)
gs.setContrastLevel(1.5)
gs.sleep(1)
gs.setContrastLevel(2.0)
gs.sleep(1)
gs.setContrastLevel(1.0)
gs.sleep(1)

gs.print("Testing hue")

gs.setHueLevel(0.0)
gs.sleep(1)
gs.setHueLevel(0.5)
gs.sleep(1)
gs.setHueLevel(1.0)
gs.sleep(1)
gs.setHueLevel(1.5)
gs.sleep(1)
gs.setHueLevel(2.0)
gs.sleep(1)
gs.setHueLevel(1.0)
gs.sleep(1)

gs.print("Testing saturation")

gs.setSaturationLevel(0.0)
gs.sleep(1)
gs.setSaturationLevel(0.5)
gs.sleep(1)
gs.setSaturationLevel(1.0)
gs.sleep(1)
gs.setSaturationLevel(1.5)
gs.sleep(1)
gs.setSaturationLevel(2.0)
gs.sleep(1)
gs.setSaturationLevel(1.0)
gs.sleep(1)

gs.print("Check out of bounds parameter still works")
gs.setContrastLevel(4.0)
gs.sleep(1)
gs.setContrastLevel(1.0)

gs.enableInput()

gateway.close()
