# Test script. Tests GUI position commands.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.maximizeInterfaceWindow()

gs.setGuiPosition(0, 0)
gs.sleep(1)
gs.minimizeInterfaceWindow()
gs.setGuiPosition(0, 0)
gs.sleep(1)
gs.maximizeInterfaceWindow()
gs.setGuiPosition(0.5, 0.5)
gs.sleep(1)
gs.setGuiPosition(1, 1)
gs.sleep(1)
gs.setGuiPosition(0, 1)

gs.enableInput()

gateway.close()