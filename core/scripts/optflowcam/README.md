# OptFlowCam keyframes processing

This directory includes an implementation of the [OptFlowCam](https://livelyliz.github.io/OptFlowCam/) technique described in the paper:

- *Piotrowski, Motejat, Roessl, Theisel. "OptFlowCam: A 3D-Image-Flow-Based Metric in Camera Space
for Camera Paths in Scenes with Extreme Scale Variations", Computer Graphics Forum, 2024*, [10.1111/cgf.15056](https://doi.org/10.1111/cgf.15056), [link](https://diglib.eg.org:443/handle/10.1111/cgf15056).

The Optical Flow Camera is used in Gaia Sky as an additional export method for keyframes. The technique produces very good results for keyframed camera paths with large scale variations. The script
 itself processes Gaia Sky keyframe files (``.gkf``) and converts them to camera path files (``.gsc``).

The original code was provided by **L. Piotrowski**, and is licensed under the GPLv3, as is the code in this directory.

### Changes to original code

We have modified the original source in the following manner:

- add target (point of interest) location parsing. Additionally, targets have been added to the Gaia Sky keyframes file format, so that a keyframe created when the camera is in focus mode, automatically gets the position of the focus as a target.
- add frame rate as program argument.
- global code clean-up (remove ambiguous Unicode characters, remove unused functions, etc.).

### Requirements

Gaia Sky spawns a new process with the system Python 3 interpreter and manages dependencies with `pipenv`.

### Linux
On Linux, you know how to install this. For instance, on Arch Linux, you do `pacman -S python python-pipenv`.

### macOS
On macOS, you can use `brew` to install `python3` and `pipenv`:

```console
brew install python3 pipenv
```

### Windows
On Windows, download Python 3 from the official [source](https://python.org/downloads), and install it. During installation, check the box that says "Add Python to PATH". Then, open a command prompt (`Win + R`, type `cmd`), and type in `pip install pipenv` to install `pipenv`. After that, you may need to add `pipenv` to your PATH. More info [here](https://pipenv.pypa.io/en/latest/installation.html).

## Running

If the environment is successfully installed, then Gaia Sky can access it. When exporting using the OptFlowCam option, Gaia Sky creates a process to install the dependencies, and then another to run the script. In it, Gaia Sky submits the desired arguments (input/output files, FPS) and waits for the process to finish. Once finished, it looks at the exit value. If it is 0, everything went fine and the camera file was generated. Otherwise, an error is issued and presented to the user.

## Running off-line

The OptFlowCam script is a standalone script that can be run directly. To do so, install the environment:

```bash
pipenv install numpy python-dateutil
```

Then run the script with your keyframes file. This will produce a camera path file.

```bash
pipenv run python3 optflowcam_convert.py -i $INPUT -o $OUTPUT -f $FPS
```

where

- `$INPUT`: Your `.gkf` keyframes file. 
- `$OUTPUT`: The output `.gsc` camera path file. Optional.
- `$FPS`: Frame rate. Optional.
