##
## Script that showcases an arbitrary transformation to the Milky Way object,
## which is of type 'billboard set'. The transformation consists on a rotation
## around the Z axis.
##

from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters
from time import time
import numpy as np

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True),
                      callback_server_parameters=CallbackServerParameters())
gs = gateway.entry_point

duration = 15.0 # in seconds

class AngleUpdater(object):
    def __init__(self, gateway, gs):
        self.gateway = gateway
        self.gs = gs

        # References to JVM objects
        self.mat = self.gateway.jvm.gaiasky.util.math.Matrix4d()
        self.coord = self.gateway.jvm.gaiasky.util.coord.Coordinates

        self.step = 3.0 # degrees/second
        self.angle = 0.0 # current angle in degrees
        self.elapsed = 0.0 # current elapsed time in seconds
        self.start = time()
        self.prev = self.start
        # Conversion factor from parsec to internal units
        self.pc_to_iu = self.gs.parsecsToInternalUnits(1.0)

    def run(self):
        # This runs every frame!
        # Apply stepwise rigid rotation around Z to the Milky Way.
        #
        # The Milky Way is defined in the Galactocentric system, so we need to:
        #
        # 1. Convert to equatorial
        # 2. Translate it so that the center of the Milky Way is in (0 0 0). The translation is (0, 0, -8 Kpc)
        # 3. Apply the rotation
        # 4. Undo the translation, i.e. (0, 0, 8 Kpc)
        # 5. Back to galactic

        # Chain transformations - Mind the order, last to first!
        self.mat.idt()
        self.mat.mul(self.coord.galToEq())
        self.mat.translate(0.0, 0.0, 8000.0 * self.pc_to_iu)
        self.mat.rotate(0.0, 0.0, 1.0, self.angle)
        self.mat.translate(0.0, 0.0, -8000.0 * self.pc_to_iu)
        self.mat.mul(self.coord.eqToGal())

        self.gs.setDatasetTransformationMatrix("Milky Way", self.mat.getValues())

        
        self.now = time()
        self.dt = self.now - self.prev
        self.angle = self.angle + self.step * self.dt

        new_elapsed = self.now - self.start
        if int(new_elapsed) != int(self.elapsed):
            print("%f / %f" % (int(new_elapsed), duration))
        self.elapsed = new_elapsed
        self.prev = self.now

    def toString():
        return "angle-updater"

    class Java:
        # This is important, it makes the Java class to implement
        # the Runnable interface, which is required by the parkRunnable() method!
        implements = ["java.lang.Runnable"]



# We create an angle updater and submit it so that it runs every cycle.
updater = AngleUpdater(gateway, gs)
gs.parkRunnable("angle-updater", updater)

# We let it run for a while.
gs.sleep(duration)

# Remove angle updater.
gs.unparkRunnable("angle-updater")

# Restore transform matrix to identity
mat = gateway.jvm.gaiasky.util.math.Matrix4d()
mat.idt()
gs.setDatasetTransformationMatrix("Milky Way", mat.getValues())

# Exit
gs.sleep(1.0)
gateway.close()
gateway.shutdown()
