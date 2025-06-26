# This script tests the go-to and capture frame API calls.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import os

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def remove_files_with_prefix(directory, prefix):
    for filename in os.listdir(directory):
        if filename.startswith(prefix):
            filepath = os.path.join(directory, filename)
            if os.path.isfile(filepath):
                os.remove(filepath)
                print(f"Removed: {filepath}")

out = gs.getDefaultFramesDir()
prefix = "scripting-test"

print(f"This script will write many image files to '{out}'.")
print("All images will be deleted at the end.")
op = input("Are you sure you want to continue? [y/n]: ")

if op == "y":
    gs.cameraStop()

    gs.setRotationCameraSpeed(40)
    gs.setTurningCameraSpeed(30)
    gs.setCameraSpeed(30)

    gs.configureRenderOutput(1280, 720, 30, out, prefix)
    gs.setFrameOutput(True)

    gs.goToObject("Sun", -1, 2.5)

    gs.setHeadlineMessage("Sun")
    gs.setSubheadMessage("This is the Sun, our star")

    gs.sleepFrames(1)
    gs.clearAllMessages()

    gs.setFrameOutput(False)

    gs.sleep(2)
    print("Cleaning up...")
    remove_files_with_prefix(out, prefix)

gateway.shutdown()
