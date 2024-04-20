# OptFlowCam keyframes processing

This directory includes an implementation of the [OptFlowCam](https://livelyliz.github.io/OptFlowCam/) technique described in the paper:

- *Piotrowski, Motejat, Roessl, Theisel. "OptFlowCam: A 3D-Image-Flow-Based Metric in Camera Space
for Camera Paths in Scenes with Extreme Scale Variations", Computer Graphics Forum, 2024*, [10.1111/cgf.15056](https://doi.org/10.1111/cgf.15056), [link](https://diglib.eg.org:443/handle/10.1111/cgf15056).

The Optical Flow Camera is used in Gaia Sky as an additional export method for keyframes. The technique produces very good results for keyframed camera paths with large scale variations. The script
 itself processes Gaia Sky keyframe files (``.gkf``) and converts them to camera path files (``.gsc``).

The original code was provided by **L. Piotrowski**, and is licensed under the GPLv3, as is the
code in this directory.

### Changes to original code

We have modified the original source in the following manner:

- add target (point of interest) location parsing. Additionally, targets have been added to the Gaia Sky keyframes file format, so that a keyframe created when the camera is in focus mode, automatically gets the position of the focus as a target.
- add frame rate as program argument.
- global clean up code (remove ambiguous Unicode characters, remove unused functions, etc.).

### Requirements

Gaia Sky spawns a new process with the system Python interpreter, which needs to have all the required dependencies installed (i.e. NumPy, ArgParse, etc.). Gaia Sky, submits the desired arguments and waits for the process to finish. Once finished, it looks at the exit value. If it is 0, everything went fine and the camera file was generated. Otherwise, an error is issued and presented to the user.
