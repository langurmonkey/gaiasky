# This script tests the popup notifications system.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.displayPopupNotification("The script is now running. Hooray!", 10.0)
gs.displayPopupNotification("This is another notification, slightly longer than the previous one, albeit not too much. If this has line breaks, the line break system works.", 10.0)
gs.displayPopupNotification("This notification\ncontains manual\nbreaks")
gs.displayPopupNotification("This one is long but it also contains\nmanual breaks. Let's see how these two play together, or if they work at all...")

gs.sleep(8.0)

gs.displayPopupNotification("We are about to finish...")

gs.sleep(1.0)

gs.displayPopupNotification("Hold on tight!", 3.0)

gs.sleep(2.0)

gs.displayPopupNotification("Almost done.", 3.0)

gs.sleep(3.0)

gs.displayPopupNotification("Done!", 4.0)

gs.enableInput()

gateway.close()
