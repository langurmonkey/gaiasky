# This script creates a transition in the field of view angle with the
# user-given parameters. The transition is implemented using the
# camera.transition_fov() calls.
# 
# 
# Created by Toni Sagrista

from py4j.clientserver import ClientServer, JavaParameters

gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True, auto_field=True))
apiv2 = gateway.entry_point.apiv2
camera = apiv2.camera

target_fov = float(input("Enter the target FoV in degrees:  "))
duration = float(input("Enter the duration in seconds: "))

print(f"Creating {duration}s transition to {target_fov}")

camera.transition_fov(target_fov, duration)

gateway.shutdown()
