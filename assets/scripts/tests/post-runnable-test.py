# This script tests posting and parking runnables that run on the main loop thread
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters

"""
Prints to both gaia sky and python
"""
def lprint(string):
    gs.print(string)
    print(string)

class PrintRunnable(object):
    def run(self):
        lprint("Hello from Python!")

    class Java:
        implements = ["java.lang.Runnable"]

class FrameCounterRunnable(object):
    def __init__(self):
        self.n = 0
        
    def run(self):
        self.n = self.n + 1
        if self.n % 30 == 0:
            lprint("Number of frames: %d" % self.n)

    class Java:
        implements = ["java.lang.Runnable"]


gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

# We post a simple runnable which prints "Hello from Python!" through the event interface once
gs.postRunnable(PrintRunnable())

# We park a runnable which counts the frames and prints the current number 
# of frames every 30 of them
gs.parkRunnable("frame_counter", FrameCounterRunnable())

gs.sleep(5.0)

# We unpark the frame counter
gs.unparkRunnable("frame_counter")

lprint("Exiting script")

gateway.close()

