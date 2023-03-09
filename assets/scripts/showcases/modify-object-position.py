# In this script, we add a shape object loaded via an external JSON catalog,
# and then proceed to modify its position in a runnable to follow the Earth.

# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import os



class PositionUpdateRunnable(object):

    def __init__(self, object):
        self.object = object

    def run(self):
        e = gs.getObjectPredictedPosition("Earth")

        gs.setObjectPosition(self.object, [e[0] + 8.0e-3, e[1], e[2]])

    def toString():
        return "position-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

gs.cameraStop()

gs.setCameraFocus("Earth")

gs.loadDataset("Shape object dataset", os.path.abspath("./catalog-shapetest.json"))

gs.sleep(3.0)

object = gs.getObject("Test Object")
posUpdater = PositionUpdateRunnable(object)
gs.parkCameraRunnable("pos-updater", posUpdater)

gs.setSimulationPace(1000.0)
gs.startSimulationTime()

gs.sleep(20.0)

gs.stopSimulationTime()

position = gs.getObjectPosition("Test Object")
print("%f, %f, %f" % (position[0], position[1], position[2]))

# clean up and finish
gs.unparkRunnable("pos-updater")
gs.removeModelObject("Test Object")
gs.cameraStop()


gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.shutdown()
