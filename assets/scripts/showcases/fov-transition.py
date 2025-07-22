# This script creates a transition in the field of view angle with the
# user-given parameters.
# The script assumes a locked frame rate of 60FPS.
# 
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point


def fov_transition(fov0, fov1, duration):
    import time
    class FovTransition(object):
        def __init__(self, fov0, fov1, duration):
            self.duration = duration
            self.frame = 0
            self.framerate = 60.0

            self.fov0 = fov0
            self.fov1 = fov1
            self.dfov = fov1 - fov0
            self.step = self.dfov / (self.duration * self.framerate)

            self.frame_time = 1.0 / self.framerate
            self.t = 0
            self.fov = fov0

            self.finished = False
    
        def run(self):
    
            if (self.dfov > 0 and self.fov < self.fov1) or (self.dfov < 0 and self.fov > self.fov1):
                gs.setFov(self.fov)
                self.fov += self.step
                self.t += self.frame_time
            else:
                # We remove ourselves
                gs.removeRunnable("fov_transition")
                self.finished = True
        
        class Java:
            implements = ["java.lang.Runnable"]

    runnable = FovTransition(fov0, fov1, duration)
    gs.parkRunnable("fov_transition", runnable)
    # Wait until finished
    while (not runnable.finished):
        time.sleep(0.05)
    

fov0 = float(input("Enter the initial FoV in degrees:  "))
fov1 = float(input("Enter the final FoV in degrees:    "))
duration = float(input("Enter the duration in seconds: "))

print(f"Creating {duration}s transition between {fov0} and {fov1}")

fov_transition(fov0, fov1, duration)

gateway.shutdown()
