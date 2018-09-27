# This script tests posting and parking runnables that run on the main loop thread
# Created by Toni Sagrista

from gaia.cu9.ari.gaiaorbit.script import EventScriptingInterface
from java.lang import Runnable

class PrintRunnable(Runnable):
    def run(self):
        print("I RUN!")
        
class FrameCounterRunnable(Runnable):
    def __init__(self):
        self.n = 0
        
    def run(self):
        self.n = self.n + 1
        if self.n % 30 == 0:
            print "Number of frames: %d" % self.n
        

gs = EventScriptingInterface.instance()

# We post a simple runnable which prints "I RUN!" once
gs.postRunnable(PrintRunnable())

# We park a runnable which counts the frames and prints the current number 
# of frames every 30 of them
gs.parkRunnable("frame_counter", FrameCounterRunnable())

gs.sleep(30.0)

# We unpark the frame counter
gs.unparkRunnable("frame_counter")

print "Exiting script"

