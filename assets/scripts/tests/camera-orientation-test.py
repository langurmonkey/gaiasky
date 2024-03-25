# This script tests the positioning of the camera with relation to two objects.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(string):
    gs.print(string)
    print(string)

def print_quat(name, quat):
    lprint("%s: [x: %f, y: %f, z: %f, w: %f]" % (name, quat[0], quat[1], quat[2], quat[3]))


gs.cameraStop()
gs.setCameraFree()

ori = gs.getCameraOrientationQuaternion()
print_quat("Current camera orientation", ori)
gs.sleep(2)

lprint("Changing orientation using [0, 1, 0, 0]...")
gs.setCameraOrientationQuaternion([0.0, 1.0, 0.0, 0.0])
gs.sleep(2)

lprint("Restoring original orientation...")
gs.setCameraOrientationQuaternion(ori)


gateway.shutdown()
