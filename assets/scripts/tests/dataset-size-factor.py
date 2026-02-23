# This script showcases lines and parked runnables

# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

gs.cameraStop()

assets = gs.getAssetsLocation()
gs.loadDataset("TestDS", assets + "/scripts/tests/hip-subset.vot", True)

gs.setDatasetHighlightSizeFactor("TestDS", 2.0)
gs.highlightDataset("TestDS", 0, True)
gs.sleep(2.0)
gs.highlightDataset("TestDS",  False)
gs.sleep(2.0)
gs.setDatasetHighlightSizeFactor("TestDS", 5.0)
gs.highlightDataset("TestDS", 1,  True)
gs.sleep(2.0)
gs.highlightDataset("TestDS", False)
gs.sleep(2.0)
gs.setDatasetHighlightSizeFactor("TestDS", 0.5)
gs.highlightDataset("TestDS", 2, True)
gs.sleep(5.0)
gs.highlightDataset("TestDS", False)
gs.removeDataset("TestDS")

# close connection
gateway.close()
