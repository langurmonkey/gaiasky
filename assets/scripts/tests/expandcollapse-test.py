# Test script. Tests GUI expand and collapse.
# Created by Toni Sagrista

from py4j.java_gateway import JavaGateway, GatewayParameters

gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()
gs.maximizeInterfaceWindow()

gs.sleep(1)
gs.expandGuiComponent("CameraComponent")
gs.sleep(1)
gs.expandGuiComponent("VisibilityComponent")
gs.sleep(1)
gs.collapseGuiComponent("CameraComponent")
gs.sleep(1)
gs.collapseGuiComponent("VisibilityComponent")
gs.sleep(1)

gs.enableInput()

gateway.close()