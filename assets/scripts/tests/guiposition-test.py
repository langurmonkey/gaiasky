# Test script. Tests GUI position commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

print("Warning, this script uses deprecated methods (setGuiPosition)!")
gs.print("Warning, this script uses deprecated methods (setGuiPosition)!")

gs.disableInput()
gs.cameraStop()

gs.setGuiPosition(0, 0)
gs.sleep(1)
gs.setGuiPosition(0, 0)
gs.sleep(1)
gs.setGuiPosition(0.5, 0.5)
gs.sleep(1)
gs.setGuiPosition(1, 1)
gs.sleep(1)
gs.setGuiPosition(0, 1)

gs.enableInput()

gateway.shutdown()
