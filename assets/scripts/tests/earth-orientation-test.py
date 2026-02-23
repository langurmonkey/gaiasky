# This script computes the current sidereal time at Greenwhich, and
# checks that the orientation of the Earth is right (London). 
# Created by Svetlin Tassev

import time
from py4j.clientserver import ClientServer, JavaParameters
import numpy as np

from astropy.time import Time

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

lon=0.
lat=51.5

gs.setSimulationPace(1)
gs.stopSimulationTime()
t0=gs.getSimulationTime()
t = Time(t0/1000., scale='utc',location=(str(lon)+'d',str(lat)+'d'),format='unix')
sid=t.sidereal_time('apparent').rad 
# -10.*np.pi/180.
print(sid*180./np.pi) # Print sidereal time

#Vector looking at zenith from lat/lon location in gaiasky coordinates.
r=np.array([np.sin(sid)*np.cos(lat*np.pi/180.),
   np.sin(lat*np.pi/180.),
   np.cos(sid)*np.cos(lat*np.pi/180.)]) 

#Set location to Earth's center + r*radius of earth
gs.setCameraPosition(r*6.5e6/1.e3+np.array(gs.getObjectPosition('Earth'))*1.e6,True)

# v looks east.
v=np.cross(np.array([0,1.,0.]),r)
#gs.setCameraDirection(v,True)
#gs.setCameraUp(r,True)
gs.setCameraFree()
gs.setCameraDirection(r,True) # Look at zenith. Check out RA/delta of view.
gs.setCameraUp(v,True)

gs.sleep(10)

gs.setCameraDirection(-r,True) # Look at nadir. check out location on earth's skin.
gs.setCameraUp(v,True)

gs.sleep(10)

gs.setCameraFocus("Earth") # again look at nadir; this time gaiasky shows lat/lon on earth.

# close connection
gateway.close()
