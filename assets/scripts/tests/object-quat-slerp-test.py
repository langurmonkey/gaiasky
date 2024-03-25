# This script tests the setObjectQuaternionSlerpOrientation() API call, where
# we set the orientation provider for an object (Gaia in this case). After
# running this script, Gaia will no longer follow the NSL, and will instead
# source its orientation from a file that has data from the beginning of 2020
# to the end of 2027.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters
import os

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

def lprint(string):
    gs.print(string)
    print(string)

# The data file is in the same directory as the script.
dir = os.path.abspath(os.path.dirname(__file__))
data_file = os.path.join(dir, "quaternion-orientations.csv")

# Set slerp orientation server.
#gs.setObjectQuaternionSlerpOrientation("Gaia", data_file)
# Uncomment next line to use nlerp instead.
gs.setObjectQuaternionNlerpOrientation("Gaia", data_file)

# At this point, Gaia sources its orientation from the CSV file.
lprint("Gaia is now using quaternion-orientaitons.csv")

gateway.shutdown()
