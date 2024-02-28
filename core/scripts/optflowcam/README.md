# OptFlowCam

This directory includes an implementation of the OptFlowCam technique described in the paper:

- *Piotrowski, Motejat, Roessl, Theisel. "OptFlowCam: A 3D-Image-Flow-Based Metric in Camera Space
for Camera Paths in Scenes with Extreme Scale Variations", Eurographics 2024*

The technique produces very good results for keyframed camera paths with large scale variations. The script
is used to process Gaia Sky keyframe files (``.gkf``) and convert them to camera path files (``.gsc``).

The original code was provided by **L. Piotrowski**, and is licensed under the GPLv3, as is the
code in this directory.

### Changes to original code

We have adapted the original sources
by cleaning it up and implementing the parsing of the keyframe targets in the keyframe file format. In
Gaia Sky, keyframes created when the camera is in focus mode include the location of the focus object
as the target. This location is retrieved by this script in order to do the OptFlowCam processing.

### Requirements

Gaia Sky spawns a new process with the system Python interpreter, which needs to have all the required
dependencies installed (i.e. numpy, argparse, etc.). Gaia Sky, submits the desired arguments and waits
for the process to finish. Once finished, it looks at the exit value. If it is 0, everything went fine
and the camera file was generated. Otherwise, an error is issued and presented to the user.