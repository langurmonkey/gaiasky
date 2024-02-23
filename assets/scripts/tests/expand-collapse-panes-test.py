# This script tests the expandUIPane() and collapseUIPane() commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(string):
    gs.print(string)
    print(string)

lprint("Expanding time")
gs.expandUIPane("Time")
gs.sleep(1.0)

lprint("Expanding camera")
gs.expandUIPane("Camera")
gs.sleep(1.0)

lprint("Expanding visiblity")
gs.expandUIPane("Visibility")
gs.sleep(1.0)

lprint("Expanding visual settings")
gs.expandUIPane("VisualSettings")
gs.sleep(1.0)

lprint("Expanding datasets")
gs.expandUIPane("Datasets")
gs.sleep(1.0)

lprint("Expanding location log")
gs.expandUIPane("LocationLog")
gs.sleep(1.0)

lprint("Expanding bookmarks")
gs.expandUIPane("Bookmarks")
gs.sleep(1.0)

lprint("Collapsing bookmarks")
gs.collapseUIPane("Bookmarks")
gs.sleep(1.0)

gateway.shutdown()
