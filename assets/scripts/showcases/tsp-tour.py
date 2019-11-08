# This script showcases the creation and animation of a random path between stars
#
# This script animates a TSP path visiting all stars of a given .tsp
# file containing the id and the equatorial cartesian position
# (see http://www.math.uwaterloo.ca/tsp/stardata/). 
# The script does not load the stars and needs the *.tsp file to work
#
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time

# Units in .tsp to Km
to_km = 3.085678e12

# Global runnable state
finished = False

# Time [seconds] until the next point batch is added
# Decrease to make the tour appear faster (limited by frame rate)
dt = 0.02

# Number of points to add at once
# Increase to make the tour appear faster
batch_size = 400

# The file containing [ID, X, Y, Z]
positions_file = "/media/tsagrista/NeuDaten/Gaia/gaiasky/tsp/gaia250000.tsp.gz"
# The file containing a list of integer indices, one for each position in the positions file
indices_file = "/media/tsagrista/NeuDaten/Gaia/gaiasky/tsp/star250000.9794191.tour.txt.gz"

"""
Prints to Gaia Sky and python outputs
"""
def lprint(string):
    gs.print(string)
    print(string)

class LineUpdaterRunnable(object):
    def __init__(self, polyline, positions):
        self.polyline = polyline
        self.positions = positions
        self.t0 = 0
        self.seq = 0
        self.n = len(positions)
        self.d_class = gateway.jvm.double

        # Auxiliary double array
        self.darr = gateway.new_array(self.d_class, 3)

    def run(self):
        currt = time.time()
        if currt - self.t0 > dt:
            pl = self.polyline.getPointCloud()

            for i in range(batch_size):
                if self.seq < self.n:
                    # Still have points, add next
                    self.darr[0] = positions[self.seq][0]
                    self.darr[1] = positions[self.seq][1]
                    self.darr[2] = positions[self.seq][2]

                    internal_pos = gs.equatorialCartesianToInternalCartesian(self.darr, to_km)
                    pl.addPoints(internal_pos)
                else: 
                    global finished
                    lprint("We are done")
                    finished = True
                    break
                
                self.seq += 1
                    
            self.polyline.markForUpdate()
        
            self.t0 = currt




    def toString():
        return "line-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

"""
Reads a file into memory and returns it as a string array
"""
def read_file_lines(file_path):
    lines = []
    if file_path.endswith(".gz"):
        import gzip
        # Gzipped
        with gzip.open(file_path, 'rb') as f:
            for line in f:
                lines.append(line.decode("utf-8"))
    else:
        # Uncompressed
        with open(file_path) as f:
            lines = f.readlines()
    return lines

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

# Basic init
gs.cameraStop()
gs.setVisibility("element.orbits", False)
gs.setVisibility("element.others", True)

# Load data
lprint("Loading positions from %s" % positions_file)
lines = read_file_lines(positions_file)
positions = []
for line in lines:
    if line:
        tokens = line.split()
        # We assume a number if first letter of first token
        # does not start by an alphabetic character
        if not tokens[0][0].isalpha():
            positions.append([float(tokens[1]), float(tokens[2]), float(tokens[3])])

npoints = len(positions)
lprint("%d positions read from %s" % (npoints, positions_file))

lprint("Loading indices from %s" % indices_file)
lines = read_file_lines(indices_file)
indices = []
for line in lines:
    if line:
        indices.append(int(line))

nindices = len(indices)
lprint("%d indices read from %s" % (nindices, indices_file))

# Check sizes match
if npoints != nindices:
    lprint("Number of points does not match the number of indices: %d != %d" % (npoints, nindices))
    gateway.shutdown()
    exit()

# Create final positions list
sorted_positions = [None] * npoints
for i in range(npoints):
    # Indices are in [1,n], we need [0,n-1]
    sorted_positions[indices[i] - 1] = positions[i]

# Create line object
gs.addPolyline("tsp-tour", [], [1., .2, .2, .4], 1)
gs.sleep(1.0)
tsp_path = gs.getObject("tsp-tour")

# park the line updater
gs.parkRunnable("line-updater", LineUpdaterRunnable(tsp_path, sorted_positions))

lprint("Runnable is parked")

# Sleep till runnable is done
while not finished:
    gs.sleep(2.0)

# Cleanup and exit
gs.enableInput()
gs.unparkRunnable("line-updater")
gateway.shutdown()

