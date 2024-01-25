# This script tests backing up and restoring the settings state.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def pprint(text):
    gs.print(text)
    print(text)

gs.backupSettings()
pprint("We have just backed up our settings.")

gs.disableInput()
gs.cameraStop()

gs.setRotationCameraSpeed(20)
gs.setTurningCameraSpeed(20)
gs.setCameraSpeed(20)

pprint("We have set the camera speed, rotation and turn speed to 20.")
input("Press enter to continue.")

gs.setFov(80.0)

pprint("We have set the fov to 80 degrees.")
input("Press enter to continue.")



gs.restoreSettings()
pprint("We have restored the settings. Good bye.")

gateway.shutdown()
