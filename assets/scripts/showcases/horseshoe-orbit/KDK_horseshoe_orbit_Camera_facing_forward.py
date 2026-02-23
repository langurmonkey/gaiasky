# This script demonstrates relative orbital motions of an object on a 
# horseshoe orbit near Jupiter with respect to the Jupiter-Sun system. 
# The orbit is integrated using the Kick-Drift-Kick method. It is traced
# in several different frames of reference: one tied with Jupiter's orbit;
# and in the another, in which the object orbit is traced onto the plane
# of the orbit of the object itself.  In this version of the script, 
# the camera faces along the velocity vector of the object.
#
# Created by Svetlin Tassev.

from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import time
import numpy as np
import os

class KDKRunnable(object):

    def __init__(self,object, xC,vC):
        self.object=object
        self.ti=gs.getSimulationTime()
        self.vC=vC
        self.xC=xC
    def run(self):
        ti=gs.getSimulationTime()
        dt=(ti-self.ti)
        if (dt!=0):
            dt=dt/1000. #convert to seconds
            #print(dt)
            eps=1.e-32
            
            xA0 = np.array(gs.getObjectPosition("Sun"))*1.e9
            xA = np.array(gs.getObjectPredictedPosition("Sun"))*1.e9
            
            xB0 = np.array(gs.getObjectPosition("Jupiter"))*1.e9
            xB = np.array(gs.getObjectPredictedPosition("Jupiter"))*1.e9
            
            xC=self.xC
            vC=self.vC
            
            ###
            
            G=6.6743e-11
            Msun=1.989e30
            Mjup=1.89813e27
            
            #Kick
            xAC = xC-xA0
            a = -xAC*G*Msun/np.sqrt(xAC.dot(xAC))**3
            
            xBC=xC-xB0
            a += -xBC*G*Mjup/np.sqrt(xBC.dot(xBC))**3
            
            vC=vC+a*dt/2.
            
            #Drift
            xC=xC+vC*dt
            
            #Kick
            xAC=xC-xA
            a=-xAC*G*Msun/np.sqrt(xAC.dot(xAC))**3
            
            xBC=xC-xB
            a += -xBC*G*Mjup/np.sqrt(xBC.dot(xBC))**3
            
            vC=vC+a*dt/2.
            ###
            gs.setObjectPosition(self.object, float(xC)/1.e9)
            self.xC=xC
            self.vC=vC
            self.ti=ti
            
            ##set camera or not
            up=np.cross(vC,a)
            uu=up.dot(up)
            if (uu>eps):
                up/=np.sqrt(uu)
                gs.setCameraUp(up,True)
            gs.setCameraDirection(vC,True)
            gs.setCameraPosition(xC/1.e3,True)
            
            
    def toString():
        return "KDK-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]



class CameraUpdaterRunnable():

    def __init__(self):
        self.rBAold = []
        self.rCA_tied = [] # camera coordinates in frame "tied" with A and B
        self.u_tied = [] # camera Up
        self.dir_tied = [] # camera dir
        self.rCA0 = [] # original cam coordinates relative to A
        self.lastt = 0.0
        self.cam_coo=False # do we have the camera settings in the "tied" coordinates?
        self.simT=gs.getSimulationTime()
    def run(self):
        eps=1.e-32
        Ap = gs.getObjectPredictedPosition("Sun")
        Bp = gs.getObjectPredictedPosition("Jupiter")
        Ap0 = gs.getObjectPosition("Sun")
        Bp0 = gs.getObjectPosition("Jupiter")
        
        Cp = gs.getCameraPosition()
        Cp=np.array([Cp[0]/1.e6,Cp[1]/1.e6,Cp[2]/1.e6]) # convert to object position units
        
        U = gs.getCameraUp()

        rBA = np.array([Bp[0] - Ap[0], Bp[1] - Ap[1], Bp[2] - Ap[2]])
        rBA0 = np.array([Bp0[0] - Ap0[0], Bp0[1] - Ap0[1], Bp0[2] - Ap0[2]])
        rCA0 = np.array([Cp[0] - Ap0[0], Cp[1] - Ap0[1], Cp[2] - Ap0[2]])
        rCA = np.array([Cp[0] - Ap[0], Cp[1] - Ap[1], Cp[2] - Ap[2]])
        r2 = []
        
        # See if camera position needs refreshing every dt sec

        currt = time.time()
                
        dt=0.1
        if currt - self.lastt >= dt:
            if ((gs.getSimulationTime()-self.simT)==0):# if time is paused, reset camera
                self.cam_coo=False
                self.lastt=currt
                
        do=False # should we be moving the camera? Not yet.
        
        #dt1=0.000
        if (((gs.getSimulationTime()-self.simT)!=0)): # and (currt - self.lastt >= dt1)):
            r1 = rBA.copy() #(rBA+rBA0)/2. #rBA.copy()
            r2 = rBA-rBA0 #self.rBAold
            
            r10 = rBA0.copy() #(rBA+rBA0)/2. #rBA.copy()
            r20 = rBA-rBA0 #self.rBAold
            
            do = ((r2.dot(r2))/(r1.dot(r1))>eps)   # check that objects moved relative to each other. If not then camera won't be moved.
            
            # Unless objects moved towards each other, we can set up our orthonormal coordinate system.
            if (do):  
                do = False # Wait to make sure objects didn't move towards each other.
                #Set the normalized vectors
                r2/=np.sqrt(r2.dot(r2))
                r1/=np.sqrt(r1.dot(r1))
                r2=np.cross(r1,r2)   # make r2 orthogonal to r1
                #r2=r2-r1.dot(r2)*r1 # make orthogonal
                if (r2.dot(r2)>eps): # if objects move towards each other, this would fail.
                    r2/=np.sqrt(r2.dot(r2)) #normalize
                    r3 = np.cross(r1,r2) # get third unit vector
                    r3/=np.sqrt(r3.dot(r3))
                    do=True # okay, coordinate system is set up, so move on with camera move below
                #Set the normalized vectors0
                r20/=np.sqrt(r20.dot(r20))
                r10/=np.sqrt(r10.dot(r10))
                r20=np.cross(r10,r20)   # make r2 orthogonal to r1
                #r2=r2-r1.dot(r20)*r1 # make orthogonal
                if (r20.dot(r20)>eps): # if objects move towards each other, this would fail.
                    r20/=np.sqrt(r20.dot(r20)) #normalize
                    r30 = np.cross(r10,r20) # get third unit vector
                    r30/=np.sqrt(r30.dot(r30))
                    do=True # okay, coordinate system is set up, so move on with camera move below
            # Save previous time
            # Save previous time
            self.lastt = currt
 
        self.simT=gs.getSimulationTime()
        
        # Setting up the components of camera dir, up and position (rCA) in the "tied" reference frame
        # We will use these to rotate the camera direction and FoV in the next frames ... until the 
        # time is paused (self.cam_coo=False). When that happens, we will recalculate this.
        if (do and not(self.cam_coo)): 
            #Let's calculate camera coordinates
            rCAm=rCA0 #(rCA0+rCA)/2. #rCA0
            self.rCA_tied=np.array([rCAm.dot(r10), rCAm.dot(r20), rCAm.dot(r30)])
            #self.rCA_tied=np.array([self.rCA0.dot(r1), self.rCA0.dot(r2), self.rCA0.dot(r3)])
            # Cam dir
            rdir = gs.getCameraDirection()
            rdir = np.array([rdir[0],rdir[1],rdir[2]])
            rdir/=np.sqrt(rdir.dot(rdir))#normalize camera direction vector
            self.dir_tied=np.array([rdir.dot(r10), rdir.dot(r20), rdir.dot(r30)])
            
            # Cam Up
            ra=(r10+r20+r30) # this is random: a vector that's preferably not aligned with any ri0. Hopefully, this would ensure no division of zero happens below.
            ra/=np.sqrt(ra.dot(ra))
            u1=np.cross(ra,rdir)
            u1 /= np.sqrt(u1.dot(u1)) # This may need some rework to check that we are not dividing by ~0.
            u2 = np.cross(u1,rdir)
            u2 /= np.sqrt(u2.dot(u2))
            self.u_tied=np.array([u1.dot(U),u2.dot(U)])
            
            self.cam_coo=True # cam set up done.
        
        #While time is running, move camera and rotate Up and direction vectors using the
        # components calculated in the preceding if statement:
        if (do and self.cam_coo):
            campos = self.rCA_tied[0]*r1 + self.rCA_tied[1]*r2 + self.rCA_tied[2]*r3
            Am=np.array([Ap[0],Ap[1],Ap[2]])
            campos+= Am
            campos*=1.e6 #convert to camera position units
            
            gs.setCameraPosition(campos, True)
            
            #set cam dir
            gs.setCameraDirection(self.dir_tied[0]*r1+self.dir_tied[1]*r2+self.dir_tied[2]*r3, True)
            
            #set cam up
            rdir = gs.getCameraDirection()
            rdir = np.array([rdir[0],rdir[1],rdir[2]])
            rdir/=np.sqrt(rdir.dot(rdir))#normalize camera direction vector
            ra=(r1+r2+r3) # Same random vector as above.
            ra/=np.sqrt(ra.dot(ra))
            u1=np.cross(ra,rdir)
            u1 /= np.sqrt(u1.dot(u1))
            u2 = np.cross(u1,rdir)
            u2 /= np.sqrt(u2.dot(u2))
            gs.setCameraUp(self.u_tied[0]*u1+self.u_tied[1]*u2, True)
            
    def toString():
        return "camera-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]


class RelativeLineUpdaterRunnableSyn():

    def __init__(self, line,target):
        self.positions = []
        self.lastt = 0.0
        self.line = line
        self.target=target
        self.dAB=np.array([0,0,0])
        self.ok=False
        self.ti=gs.getSimulationTime()
    def run(self):
        
        ###########
        currt = time.time()
        ti=gs.getSimulationTime()
        
        ###########
        Ap = np.array(gs.getObjectPosition(self.target))
        Bp = np.array(gs.getObjectPosition("Sun"))
        Cp = np.array(gs.getObjectPosition("Test Object"))
        
        # Add line every .05 seconds
        if ((currt - self.lastt >= 0.1) and ((ti-self.ti)!=0)): 
            if (self.dAB.dot(self.dAB))==0:
                self.dAB=Bp-Ap
                self.ok=True
            else:
                dAB=Bp-Ap
                vAB=dAB-self.dAB
                
                dAB/=np.sqrt(dAB.dot(dAB))
                vAB=np.cross(vAB,dAB)
                vAB/=np.sqrt(vAB.dot(vAB))
                r3=np.cross(vAB,dAB)
                dr=Cp-Ap
                self.positions.append(np.array([dr.dot(dAB),dr.dot(vAB),dr.dot(r3)]))
                pc = self.line.getPointCloud()
                pc.addPoint(0.,0.,0.) # irrelevant what point you add
                self.lastt = currt
                

        # Update all lines to put center on Earth
        if (self.ok and (self.line is not None and len(self.positions) > 1)  and (ti-self.ti)!=0):
            pc = self.line.getPointCloud()
            
            dAB=Bp-Ap
            vAB=dAB-self.dAB
            self.dAB=dAB
            
            dAB/=np.sqrt(dAB.dot(dAB))
            vAB=np.cross(vAB,dAB)
            vAB/=np.sqrt(vAB.dot(vAB))
            r3=np.cross(vAB,dAB)
            
            #gs.print("Polyline: %d, positions: %d" % (pc.getNumPoints(), len(self.positions)))
            for i in range(pc.getNumPoints()):
                s=self.positions[i]
                x=s[0]*dAB +s[1]*vAB+s[2]*r3
                pc.set(i, x[0] + Ap[0], x[1] + Ap[1], x[2] + Ap[2])
            self.line.markForUpdate()
        self.ti=ti

    def toString():
        return "line-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]


class RelativeLineUpdaterRunnableSynProjectOnOrbit():

    def __init__(self, line,target):
        self.positions = []
        self.lastt = 0.0
        self.line = line
        self.target=target
        self.dCB=np.array([0,0,0])
        self.z=np.array([0,0,0])
        self.ok=0
        self.z_ok=0
        self.ti=gs.getSimulationTime()
    def run(self):
        eps=1.e-12**4
        ###########
        currt = time.time()
        ti=gs.getSimulationTime()
        
        ###########
        Ap = np.array(gs.getObjectPosition(self.target))
        Bp = np.array(gs.getObjectPosition("Sun"))
        Cp = np.array(gs.getObjectPosition("Test Object"))
        # Add line every .05 seconds
        if ((currt - self.lastt >= 0.1) and ((ti-self.ti)!=0)): 
            if self.ok==0:
                self.dCB=Cp-Bp
                self.ok=1
            else:
                dCB=Cp-Bp
                vCB=dCB-self.dCB
                vCB1=vCB.copy()
                vCB=np.cross(np.cross(vCB,dCB),dCB)
                n1=vCB.dot(vCB)
                n2=dCB.dot(dCB)
                if n1*n2>eps:
                    vCB/=np.sqrt(n1)
                    dCB/=np.sqrt(n2)
                    AProj=(Ap-Bp).dot(dCB)*dCB+(Ap-Bp).dot(vCB)*vCB+Bp
                    
                    dAB=Bp-AProj
                    self.ok+=1
                    if (self.ok>3):
                        vAB=vCB1#
                        if self.z_ok==1:
                            if np.cross(dAB,self.z).dot(vAB)<0:
                                vAB*=-1.
                        
                        dAB/=np.sqrt(dAB.dot(dAB))
                        vAB=np.cross(vAB,dAB)
                        vAB/=np.sqrt(vAB.dot(vAB))
                        r3=np.cross(vAB,dAB)
                        if self.z_ok==0:
                            self.z=vAB
                            self.z_ok=1
                        dr=Cp-AProj
                        self.positions.append(np.array([dr.dot(dAB),dr.dot(vAB),dr.dot(r3)]))
                        pc = self.line.getPointCloud()
                        pc.addPoint(0.,0.,0.) # irrelevant what point you add
                    else:
                        self.dCB=Cp-Bp
                
                self.lastt = currt
                

        # Update all lines to put center on Earth
        if ((self.ok>3) and (self.line is not None and len(self.positions) > 1)  and (ti-self.ti)!=0):
            pc = self.line.getPointCloud()
            
            dCB=Cp-Bp
            vCB=dCB-self.dCB
            vCB1=vCB.copy()
            vCB=np.cross(np.cross(vCB,dCB),dCB)
            
            n1=vCB.dot(vCB)
            n2=dCB.dot(dCB)
            if n1*n2>eps:
                self.dCB=dCB
                vCB/=np.sqrt(n1)
                dCB/=np.sqrt(n2)
            
                AProj=(Ap-Bp).dot(dCB)*dCB+(Ap-Bp).dot(vCB)*vCB+Bp
                
                dAB=Bp-AProj
                vAB=vCB1#dAB-self.dAB
                if self.z_ok==1:
                    if np.cross(dAB,self.z).dot(vAB)<0:
                        vAB*=-1.
                
                
                
                dAB/=np.sqrt(dAB.dot(dAB))
                vAB=np.cross(vAB,dAB)
                vAB/=np.sqrt(vAB.dot(vAB))
                r3=np.cross(vAB,dAB)
                
                #gs.print("Polyline: %d, positions: %d" % (pc.getNumPoints(), len(self.positions)))
                for i in range(pc.getNumPoints()):
                    s=self.positions[i]
                    x=s[0]*dAB +s[1]*vAB+s[2]*r3
                    pc.set(i, x[0] + AProj[0], x[1] + AProj[1], x[2] + AProj[2])
                self.line.markForUpdate()
        self.ti=ti

    def toString():
        return "line-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]



class LineUpdaterRunnable():

    def __init__(self,line):
        self.lastt = time.time()
        self.ti=gs.getSimulationTime()
        self.line=line
    def run(self):
        currt = time.time()
        ti=gs.getSimulationTime()
        dt=(ti-self.ti)
        currt = time.time()
        ti=gs.getSimulationTime()
            
        if ((dt!=0) and (currt - self.lastt >= 0.1/5.)): 
                xC = np.array(gs.getObjectPosition("Test Object"))
                pc = self.line.getPointCloud()
                pc.addPoint(float(xC[0]),float(xC[1]),float(xC[2]))
                self.line.markForUpdate()
                self.lastt=currt
            
    def toString():
        return "camera-update-runnable"

    class Java:
        implements = ["java.lang.Runnable"]

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True),
                      python_parameters=PythonParameters())
gs = gateway.entry_point

gs.cameraStop()

gs.stopSimulationTime()
gs.setVisibility("element.orbits", True)
gs.setVisibility("element.others", True)

gs.setCameraOrientationLock(False)
gs.setCameraLock(True)

gs.setFov(90)
gs.setCameraFocus("Sun")
gs.sleep(2.0)
#gs.setCameraFree()
#In [3]: [i for i in gs.getCameraPosition()]
gs.setCameraPosition([-1101643066.4062498, -5883867.740631103, -770956298.8281249])
gs.sleep(1)
#In [4]: [i for i in gs.getCameraUp()]
gs.setCameraUp([-0.129836636345712, 0.9748286046965904, 0.1812502119384385])

#In [5]: [i for i in gs.getCameraDirection()]
#gs.setCameraDirection([0.5191094471614878, -0.06868781814939244, 0.8519432877284553])


gs.setSimulationTime(2023,1, 20, 0, 0, 0, 0)
gs.sleep(2.0)
warp=2**26 #23 25
gs.setSimulationPace(warp)
xS = np.array(gs.getObjectPosition("Sun"),dtype=np.float64)
xE = np.array(gs.getObjectPosition("Jupiter"),dtype=np.float64)
gs.sleep(2.0)
gs.setSimulationTime(2023,1, 20, 0, 1, 0, 0) # set 1min later
gs.sleep(2.0)
xS1 = np.array(gs.getObjectPosition("Sun"),dtype=np.float64)
xE1 = np.array(gs.getObjectPosition("Jupiter"),dtype=np.float64)

eps=0.3 # AU<0.35AU for hill sphere

xC = (xE1-xS1)*(5.-eps)/5.+xS1
xC += (xE-xS)*(5.-eps)/5.+xS
xC /= 2.0
vC = (5.+eps/2.)/5.*((xE1-xS1)-(xE-xS))*1.e9/60. # convert to meters, then divide by deltat=1 s

aC=np.cross(xC,vC)
aC/=np.sqrt(aC.dot(aC))

xC += aC*np.sqrt(xC.dot(xC))*eps/5.

#xN = xC/np.sqrt(xC.dot(xC))
#vC -= xN*np.sqrt(vC.dot(vC))*(eps/2.)/5.


gs.sleep(2.0)
gs.setSimulationTime(2025,1, 11, 0, 0, 0, 0)
gs.sleep(1.0)


#vN=vC/np.sqrt(vC.dot(vC))
#xC+=vN*10.

#print(xS1)
#print(xE1)
#print(xC)
#print(vC)

#di=vC/np.sqrt(vC.dot(vC))
#gs.setCameraPosition(xC*1.e6)
#gs.setCameraDirection(di)
#up=np.cross(xC-xS1,vC)
#up/=np.sqrt(up.dot(up))
#gs.setCameraUp(up)

#gs.setObjectSizeScaling("Mercury", 1000.0)
#gs.setObjectSizeScaling("Venus", 1000.0)
#gs.setObjectSizeScaling("Mars", 1000.0)
#gs.setObjectSizeScaling("Earth", 1000.0)
gs.setObjectSizeScaling("Jupiter", 250.0)
#gs.setObjectSizeScaling("Saturn",250.0)
#gs.setObjectSizeScaling("Uranus", 250.0)
#gs.setObjectSizeScaling("Neptune", 250.0)
#gs.setObjectSizeScaling("Pluto", 1000.0)
#gs.setObjectSizeScaling("Moon", 1000.0)
##gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0/100000)
#gs.setObjectSizeScaling("Sun", 20.0)
#gs.setOrbitCoordinatesScaling("MoonAACoordinates", 50.0)

gs.refreshAllOrbits();
gs.refreshObjectOrbit("Moon");
gs.forceUpdateScene();

gs.sleep(5.0)






gs.loadDataset("Shape object dataset", os.path.abspath("./particles-shapetest.json"))

gs.sleep(3.0)

object = gs.getObject("Test Object")
gs.sleep(0.5)
# park the camera updater
xC*=1.e9
KDKUpdater = KDKRunnable(object,xC,vC)
gs.parkCameraRunnable("kdk-updater", KDKUpdater)

gs.removeModelObject("line-e")
gs.sleep(1.0)

gs.addTrajectoryLine("line-e", [], [ .6, .6, .9, 0.8 ] )
line = gs.getLineObject("line-e", 10.0)
#lineUpdater = LineUpdaterRunnable(xC*1.e9,vC,line)
#lineUpdater = RelativeLineUpdaterRunnableSyn(line,"Jupiter")
lineUpdater = RelativeLineUpdaterRunnableSynProjectOnOrbit(line,"Jupiter")
gs.parkRunnable("line-updater", lineUpdater)

gs.removeModelObject("line-e1")
gs.sleep(2.0)
gs.addTrajectoryLine("line-e1", [], [ .9, .9, .6, 0.3 ] )
line1 = gs.getLineObject("line-e1", 10.0)
lineUpdater1 = LineUpdaterRunnable(line1)
##lineUpdater = RelativeLineUpdaterRunnableSyn(xC*1.e9,vC,line,"Jupiter")
gs.parkRunnable("line-updater1", lineUpdater1)

gs.showDataset("Gaia DR3 large")
gs.setVisibility("element.asteroids", False)
gs.setVisibility("element.milkyway", True)
gs.setVisibility("element.moons", False)
gs.setVisibility("element.nebulae", True)

gs.sleep(2)
gs.startSimulationTime()

gs.sleep(40)
gs.setVisibility("element.orbits", False)
gs.sleep(40)

gs.setVisibility("element.orbits", True)
gs.sleep(3)
#camUpdater = CameraUpdaterRunnable()
#gs.parkCameraRunnable("cam-updater", camUpdater)
gs.sleep(80)

gs.setVisibility("element.orbits", False)
gs.sleep(80)
gs.hideDataset("Gaia DR3 large")
gs.setVisibility("element.asteroids", False)
gs.setVisibility("element.milkyway", False)
gs.setVisibility("element.moons", False)
gs.setVisibility("element.nebulae", False)

gs.sleep(600)


gs.stopSimulationTime()
gs.showDataset("Gaia DR3 large")

gs.setObjectSizeScaling("Mercury", 1.0)
gs.setObjectSizeScaling("Venus", 1.0)
gs.setObjectSizeScaling("Mars", 1.0)
gs.setObjectSizeScaling("Earth", 1.0)
gs.setObjectSizeScaling("Jupiter", 1.0)
gs.setObjectSizeScaling("Saturn",1.0)
gs.setObjectSizeScaling("Uranus", 1.0)
gs.setObjectSizeScaling("Neptune", 1.0)
gs.setObjectSizeScaling("Pluto", 1.0)
gs.setObjectSizeScaling("Moon", 1.0)
#gs.setOrbitCoordinatesScaling("EarthVSOP87", 1.0/100000)
gs.setObjectSizeScaling("Sun", 1.0)
gs.setOrbitCoordinatesScaling("MoonAACoordinates", 1.0)

gs.refreshAllOrbits();
gs.refreshObjectOrbit("Moon");
gs.forceUpdateScene();

# clean up and finish
gs.cameraStop()
gs.sleep(1.0)
#gs.unparkRunnable("cam-updater")
gs.sleep(1.0)
gs.unparkRunnable("kdk-updater")
gs.sleep(1.0)
gs.unparkRunnable("line-updater")
gs.sleep(0.5)
gs.unparkRunnable("line-updater1")
# Finish flushing
gs.sleepFrames(4)

gs.maximizeInterfaceWindow()
gs.enableInput()

# close connection
gateway.close()
