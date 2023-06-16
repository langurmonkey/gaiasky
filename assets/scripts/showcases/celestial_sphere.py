# Land on Earth and show the celestial sphere with the horizon.
# Created by Svetlin Tassev
import time
from py4j.clientserver import ClientServer, JavaParameters
import numpy as np
from astropy.time import Time

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

lon=0.0#-71.
lat=42.#42.2

gs.setSimulationPace(1)
gs.stopSimulationTime()
t0=gs.getSimulationTime()
t = Time(t0/1000., scale='utc',location=(str(lon)+'d',str(lat)+'d'),format='unix')
sid=t.sidereal_time('apparent').rad 
# -10.*np.pi/180.
print(sid*180./np.pi) # Print sidereal time

#Vector looking at zenith from lat/lon location in gaiasky coordiantes.
r=np.array([np.sin(sid)*np.cos(lat*np.pi/180.),
   np.sin(lat*np.pi/180.),
   np.cos(sid)*np.cos(lat*np.pi/180.)]) 

#Set location to Earth's center + r*radius of earth
gs.setCameraFocus("Earth")
gs.setCameraCenterFocus(False)
gs.setCameraLock(True)
gs.setCameraOrientationLock(True)
gs.setCameraPosition(r*6.49e6/1.e3+np.array(gs.getObjectPosition('Earth'))*1.e6,True)

# v looks east.
v=np.cross(np.array([0.,1.,0.]),r)
#gs.setCameraDirection(v,True)
#gs.setCameraUp(r,True)
#gs.setCameraFree()
# dir=v*10.-r
dir=v
dir/=np.sqrt(dir.dot(dir))
dir=dir-r*0.2
dir/=np.sqrt(dir.dot(dir))
gs.setCameraDirection(dir,True) # Look East
r=np.cross(np.cross(dir,r),dir)
gs.setCameraUp(r,True)
#gs.setPanoramaMode(True)
gs.setOrthosphereViewMode(True)
gs.sleep(10.)
#gs.setCubemapMode(True,"ortho2")
#dir=r
#gs.setCameraDirection(dir,True) # Look East
#r1=np.cross(np.cross(dir,v),dir)
#gs.setCameraUp(np.cross(-r1,dir),True)

#gs.setPlanetariumMode(True)

####
#gs.setCameraFocus("Earth")
#gs.landAtObjectLocation("Earth",lon,lat)
#gs.setCameraCenterFocus(False)
gs.setFov(60)

gs.stopSimulationTime()
#gs.setOrthosphereViewMode(False)
gs.forceUpdateScene();


gs.sleep(2)
gateway.shutdown()
