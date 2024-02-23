# This script tests the various coordinate conversion utilities in the scripting API. To be run asynchronously.
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
gs = gateway.entry_point

gs.disableInput()
gs.cameraStop()

gs.setRotationCameraSpeed(80)
gs.setTurningCameraSpeed(100)
gs.setCameraSpeed(30)

gs.setCameraFocus("Sun")

au_to_km = 149597900.0
u_to_km = 1000000.0
text_x = 0.2
text_y = 0.2

# Request UI scale factor and apply it to the text size
ui_scale = gs.getGuiScaleFactor()
text_size = 22 * ui_scale

# Disable all grids
gs.setVisibility("element.equatorial", False)
gs.setVisibility("element.ecliptic", False)
gs.setVisibility("element.galactic", False)

def to_internal(xyz):
    return [xyz[0] * u_to_km, xyz[1] * u_to_km, xyz[2] * u_to_km]

#
# EQUATORIAL COORDINATES
#
gs.displayMessageObject(0, "Equatorial coordinates test", text_x, text_y, 1.0, 0.3, 0.3, 1.0, text_size)
# Enable equatorial grid
gs.setVisibility("element.equatorial", True)
gs.sleep(1.5)

# Go to north equatorial pole, looking down on the Sun
coordxyz = gs.equatorialToInternalCartesian(0.1, 90.1, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

# Go to south equatorial pole
coordxyz = gs.equatorialToInternalCartesian(0.1, -90.1, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

# Equatorial plane
coordxyz = gs.equatorialToInternalCartesian(90.1, 0.1, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

gs.removeObject(0)
# Disable equatorial grid
gs.setVisibility("element.equatorial", False)
gs.sleep(1.5)

#
# ECLIPTIC COORDINATES
#
gs.displayMessageObject(0, "Ecliptic coordinates test", text_x, text_y, 0.3, 1.0, 0.3, 1.0, text_size)
# Enable equatorial grid
gs.setVisibility("element.ecliptic", True)
gs.sleep(1.5)

# Go to north equatorial pole, looking down on the Sun
coordxyz = gs.eclipticToInternalCartesian(0.1, 90.1, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

# Go to south equatorial pole
coordxyz = gs.eclipticToInternalCartesian(0.1, -90.1, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

# Equatorial plane
coordxyz = gs.eclipticToInternalCartesian(90.1, 0.1, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

gs.removeObject(0)
# Disable equatorial grid
gs.setVisibility("element.ecliptic", False)
gs.sleep(1.5)


#
# GALACTIC COORDINATES
#
gs.displayMessageObject(0, "Galactic coordinates test", text_x, text_y, 0.3, 0.3, 1.0, 1.0, text_size)
# Enable equatorial grid
gs.setVisibility("element.galactic", True)
gs.sleep(1.5)

# Go to north equatorial pole, looking down on the Sun
coordxyz = gs.galacticToInternalCartesian(0.0, 90.0, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

# Go to south equatorial pole
coordxyz = gs.galacticToInternalCartesian(0.0, -90.0, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

# Equatorial plane
coordxyz = gs.galacticToInternalCartesian(90.0, 0.0, 8.0 * au_to_km)
gs.setCameraPosition(to_internal(coordxyz))

# Sleep
gs.sleep(1.0)

gs.removeObject(0)
# Disable equatorial grid
gs.setVisibility("element.galactic", False)
gs.sleep(1.5)

# Restore
gs.enableInput()

gateway.shutdown()
