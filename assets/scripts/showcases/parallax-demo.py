# Script to demonstrate how parallaxes work.
# Created by Stefan Jordan.

import os, time
import numpy as np

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
        python_parameters=PythonParameters())

gs = gateway.entry_point

sc=3.0
setback=True
wt=120
pace=6.e6

# This set holds all line ids to clean at the end.
all_line_ids = set([])
# All line creations go through this so that we can reliably accumulate all ids.
def new_polyline(name, points, col, width):
    all_line_ids.add(name)
    gs.addPolyline(name, points, col, width)

star_names ={
        1: {'name':"Sirius",'time':400,'parallax':379.21},
        2: {'name':"Procyon",'time':400,'parallax':284.56},
        3: {'name':"Castor",'time':400,'parallax':41.13},
        4: {'name':"Capella",'time':400,'parallax':76.2},
        5: {'name':"Aldebaran",'time':400,'parallax':48.94}
        }
istar=1
starmax=len(star_names)
starname=star_names[istar]['name']
rt=wt
for star_number,star_info in star_names.items():
    rt+=star_info['time']

def current_time_ms():
    return int(round(time.time() * 1000))


class LineUpdaterRunnable(object):
    def __init__(self, polyline, prefix):
        self.polyline = polyline
        self.prevpos = None
        self.prefix = prefix
        self.curr_prefix = prefix
        self.seq = -1
        self.frames = 0
        self.old_starname=""
        self.sunp=gs.getObjectPosition("Sun")
        self.ttstar=-1
        self.istar=1


    def run(self):
        tstar=star_names[self.istar]['time']
        self.starname=star_names[self.istar]['name']
        self.curr_prefix = "%s-%s" % (self.starname, self.prefix)
        if self.ttstar > tstar and self.istar<starmax:
            self.istar+=1
            self.starname=star_names[self.istar]['name']
            self.seq=-1

        # Update earth-current_star line
        earthp = gs.getObjectPosition("Earth")
        starp = gs.getObjectPosition(self.starname)
        pl = self.polyline.getPointCloud()
        pl.setX(0, earthp[0])
        pl.setY(0, earthp[1])
        pl.setZ(0, earthp[2])
        lpos=[(starp[0]*sc-earthp[0]*(sc-1.0)),(starp[1]*sc-earthp[1]*(sc-1.0)),(starp[2]*sc-earthp[2]*(sc-1.0))]
        pl.setX(1, lpos[0])
        pl.setY(1, lpos[1])
        pl.setZ(1, lpos[2])
        self.polyline.markForUpdate()

        # New line from sun to star?
        if self.starname != self.old_starname:
            self.ttstar=0
            new_polyline("%s-tosun" % self.curr_prefix, [self.sunp[0], self.sunp[1], self.sunp[2], starp[0]*sc, starp[1]*sc, starp[2]*sc], [ 1., 1., 0., .8 ], 1 )

            self.prevpos=lpos

        # Are these the epicycle lines?
        if self.frames <= rt:
            self.seq += 1
            if self.seq:
                new_polyline("%s-%d" % (self.curr_prefix, self.seq), [self.prevpos[0], self.prevpos[1], self.prevpos[2], lpos[0], lpos[1], lpos[2]], [ 0.,1.0,1.0, .8 ], 1 )
            self.prevpos = lpos
        self.frames += 1
        self.old_starname=self.starname
        self.ttstar+=1

        self.prevpos = lpos


    def toString():
        return "line-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]


gs.minimizeInterfaceWindow()
gs.cameraStop()
gs.setFov(70)
framerate=60
gs.removeDataset("Oort cloud")

# Relative path here
gs.setObjectSizeScaling("Earth", 1.0)
gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0)
gs.refreshAllOrbits()
gs.setObjectSizeScaling("Mercury", 1.0)
gs.setObjectSizeScaling("Venus", 1.0)
gs.setObjectSizeScaling("Earth", 1.0)
gs.setObjectSizeScaling("Mars", 1.0)
gs.setObjectSizeScaling("Jupiter", 1.0)
gs.setObjectSizeScaling("Saturn", 1.0)
gs.setObjectSizeScaling("Uranus", 1.0)
gs.setObjectSizeScaling("Neptune", 1.0)


# Relative path here.
gs.configureRenderOutput(1920,1080, framerate, '/tmp/new_parallax', 'parallax')

# Orbits and labels off.
gs.setVisibility("element.orbits", True)
gs.setOrbitSolidAngleThreshold(1.e-6)
gs.setVisibility("element.labels", False)

gs.goToObjectInstant("Earth")
gs.sleep(5)

gs.setObjectSizeScaling("Mercury", 1000.0)
gs.setObjectSizeScaling("Venus", 1000.0)
gs.setObjectSizeScaling("Earth", 1000.0)
gs.setObjectSizeScaling("Mars", 1000.0)
gs.setObjectSizeScaling("Jupiter", 1000.0)
gs.setObjectSizeScaling("Saturn", 1000.0)
gs.setObjectSizeScaling("Uranus", 1000.0)
gs.setObjectSizeScaling("Neptune", 1000.0)
gs.setSimulationPace(pace)

gs.cameraTransition([-119.33309890919794, 275.2446393750978, 1.8369701987210304e-14],
                    [0.3674506460230185, 0.15930788350993066, -0.9162974522440038],
                    [-0.695410113548505, 0.7012644250440867, -0.1569489730518871],5.)
gs.setCameraFocus("Sun", -1)


gs.sleep(3)
# Start!

gs.startSimulationTime()
gs.sleep(3.0)
# Gently increase distance from Earth.
for rlog in np.arange(0.,5.+0.01,0.01):
	r=10**rlog
	print(r)
	eclpos=gs.eclipticToInternalCartesian(0.0,90.0,3.e14*r)
	if rlog==2.5:
            gs.setVisibility("element.orbits", False)
            gs.setOrbitCoordinatesScaling("EarthVSOP87", 100000.0)
            gs.setObjectSizeScaling("Earth", 100000000.0)
            gs.refreshAllOrbits()
	gs.setCameraPosition(eclpos)
	gs.sleep(0.01)
gs.setVisibility("element.orbits", True)
gs.refreshAllOrbits()
gs.sleep(10)
gs.refreshAllOrbits()


gs.sleep(3)

pos_vec=[-29178502.999257535, -6972443.095667365, -5.427187481258541e-09]
dir=[0.9738075737428171, 0.22737372031765776, -2.5100504992979705e-05]
up=[-0.23241476985557868, 0.9726167666419175, 1.0632884247878861e-17]
gs.cameraTransition(pos_vec, dir, up,5.)
gs.sleep(10)


start_frame = gs.getCurrentFrameNumber()
current_frame = start_frame
earthp = gs.getObjectPosition("Earth")
sunp = gs.getObjectPosition("Sun")
starp = gs.getObjectPosition(starname)


new_polyline("line-earth", [earthp[0], earthp[1], earthp[2], earthp[0], earthp[1], earthp[2]], [ 1., 0., 0., .8 ], 1 )

gs.sleep(0.5)

# Create polyline.
line_earth = gs.getLineObject("line-earth")
gs.parkRunnable("line-updater", LineUpdaterRunnable(line_earth, "line-updater"))

while current_frame - start_frame < rt:
    current_frame = gs.getCurrentFrameNumber()

# Finish! stop time.
gs.stopSimulationTime()
###############################################gs.setFrameOutput(False)
gs.sleep(5.0)
earthp = gs.getObjectPosition("Earth")
print("earthp=",earthp[0],earthp[1],earthp[2])
earthpeq=gs.internalCartesianToEquatorial(earthp[0],earthp[1],earthp[2])
earthpec=gs.equatorialToEcliptic(earthpeq)
print(earthpec[0],earthpec[1],earthpec[2])
print(dir)
print(up)



if setback:
    # Scales back to normal.
    gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0)
    gs.setObjectSizeScaling("Gaia", 1.0)
    gs.setOrbitCoordinatesScaling("GaiaCoordinates", 1.0)
    gs.setObjectSizeScaling("Mercury", 1.0)
    gs.setObjectSizeScaling("Venus", 1.0)
    gs.setObjectSizeScaling("Earth", 1.0)
    gs.setObjectSizeScaling("Mars", 1.0)
    gs.setObjectSizeScaling("Jupiter", 1.0)
    gs.setObjectSizeScaling("Saturn", 1.0)
    gs.setObjectSizeScaling("Uranus", 1.0)
    gs.setObjectSizeScaling("Neptune", 1.0)


    gs.refreshAllOrbits()

    # Orbits and labels off.
    gs.setVisibility("element.orbits", False)
    gs.setVisibility("element.labels", False)
gs.setFrameOutput(False)

gs.unparkRunnable("line-updater")

# Restore.
gs.maximizeInterfaceWindow()
# clean up and finish.
print("Cleaning up and ending")

for line_id in all_line_ids:
    gs.removeModelObject(line_id)
print("Lines removed: %d" % len(all_line_ids))

gs.removeDataset("Sonne")
print("Sonne removed")

gs.sleep(1.0)

gateway.shutdown()

