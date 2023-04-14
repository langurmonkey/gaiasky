# In this script, we demonstrate how to implement body coordinates from a 
# Python script and how to submit the implementation to Gaia Sky.

# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
from py4j.java_collections import ListConverter
import os

class MyCoordinatesProvider(object):

    def __init__(self, gateway):
        self.gateway = gateway
        self.gs = gateway.entry_point
        self.converter = ListConverter()
        self.km_to_u = self.gs.kilometresToInternalUnits(1.0)
        self.pc_to_u = self.gs.parsecsToInternalUnits(1.0)

    def getEquatorialCartesianCoordinates(self, julianDate, outVector):
        # Here we need internal coordinates.
        x_km = 150000000 * self.km_to_u
        z_km = 200000000 * self.km_to_u
        v = [x_km, (julianDate - 2460048.0) * 100.0, z_km]

        # We need to set the result in the out vector.
        outVector.set(v[0], v[1], v[2])
        return outVector

    def toString():
        return "my-coordinates-provider"

    class Java:
        implements = ["gaiasky.util.coord.IPythonCoordinatesProvider"]


gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

# Load test star system.
gs.loadDataset("Test star system", os.path.abspath("./particles-body-coordinates.json"))

# Set coordinates provider.
provider = MyCoordinatesProvider(gateway)
gs.setObjectCoordinatesProvider("Test Coord Star", provider)

gs.startSimulationTime()
gs.setCameraFocus("Test Coord Star")

print("Coordinates provider set.")
input("Press a key to finish...")

gs.stopSimulationTime()
# Clean up before shutting down, otherwise Gaia Sky will crash
# due to the closed connection.
gs.removeObjectCoordinatesProvider("Test Coord Star")
gs.removeDataset("Coordinates test system")

gs.sleep(2.0)

gateway.shutdown()
