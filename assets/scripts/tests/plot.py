# This script tests the star size commands.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.maximizeInterfaceWindow()

gs.goToObjectInstant("HD81040")
pace=4.e6
gs.startSimulationTime()
gs.setSimulationPace(pace)

for i in range(0,150):
   HDpos=gs.getObjectPosition("HD81040")
   HDpos_eq=gs.internalCartesianToEquatorial(HDpos[0],HDpos[1],HDpos[2])
   HDPLpos=gs.getObjectPosition("Planetty")
   HDPLpos_eq=gs.internalCartesianToEquatorial(HDPLpos[0],HDPLpos[1],HDPLpos[2])
   print(HDpos_eq[0],HDpos_eq[1],HDpos_eq[2],HDPLpos_eq[0],HDPLpos_eq[1],HDPLpos_eq[2])
   gs.sleepFrames(10)

gateway.shutdown()
