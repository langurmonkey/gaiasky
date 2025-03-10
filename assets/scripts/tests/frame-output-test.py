# Test script. Tests the frame output system in both its simple and advanced modes.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
from pathlib import Path
import os, shutil

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def pprint(text):
    gs.print(text)
    print(text)

home = str(Path.home())
outdir = home + '/temp/frame-output-test/'

# Remove if it exists.
if os.path.isdir(outdir):
    shutil.rmtree(outdir)

# Create.
Path(outdir).mkdir(parents=True, exist_ok=True)

# Simple mode.
pprint("Testing simple mode")

gs.configureFrameOutput(500, 500, 15, outdir, 'test-simple')

gs.setFrameOutputMode("simple")

gs.setFrameOutput(True)

gs.sleepFrames(10)

gs.setFrameOutput(False)

pprint("OK")

# Advanced mode.
pprint("Testing advanced mode")

gs.configureFrameOutput(500, 500, 15, outdir, 'test-adv')

gs.setFrameOutputMode("advanced")

gs.setFrameOutput(True)

gs.sleepFrames(10)

gs.setFrameOutput(False)

pprint("OK")


# Count files.
nfiles = len([name for name in os.listdir(outdir) if os.path.isfile(os.path.join(outdir, name))])
pprint(f"Produced frames: {nfiles}")

# Clean up.
pprint("Cleaning up")
shutil.rmtree(outdir)

pprint("Done")

gateway.shutdown()
