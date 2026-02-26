# Original source from Lisa Piotrowski et. al., licensed under GPLv3.0.
# https://github.com/LivelyLiz/OptFlowCam
#
# Paper: Piotrowski, Motejat, Roessl, Theisel
#        OptFlowCam: A 3D-Image-Flow-Based Metric in Camera Space for Camera Paths
#        in Scenes with Extreme Scale Variations. Eurographics 2024
# 
# Modifications by Toni Sagrista:
# - add target location parsing (targets --points of interest-- added to keyframes file format).
# - add frame rate as argument.
# - clean up code (non ascii characters, unused functions, etc.).

import argparse
import csv
import os
import sys
import dateutil.parser

import json

import optflowcam_interpolation as ofc_ip

import numpy as np

def km_to_iu(kilometers):
    return kilometers/(1e6)

def check_path(value):
    '''
    Checks if a given filepath exists and expands it to its absolute path
    '''
    value = os.path.abspath(os.path.expanduser(value))
    if not os.path.isfile(value):
        raise argparse.ArgumentTypeError("File "+value+" does not exist or is not a file.")
    return value

def validate_filepath(filepath, throw=True):
    '''
    Validate that the given path is a path to an existing file
    '''
    if not os.path.isfile(filepath):
        if throw:
            raise ValueError("File "+filepath+" does not exist or is not a file.")
    return os.path.isfile(filepath)

def make_filepath_unique(filepath):
    '''
    Make a given path to a file unique.
    Also creates missing directories in the path.
    The returned path is guaranteed to exist and being unique
    '''
    dir = os.path.dirname(filepath)
    
    # Only attempt makedirs if 'dir' is not an empty string
    if dir and not os.path.isdir(dir):
        os.makedirs(dir)

    base_filename, ext = os.path.splitext(os.path.basename(filepath))
    file_num = 1
    
    # Ensure we use the correct directory for the join
    # if dir is empty, os.path.join handles it, but being explicit is safer
    while os.path.isfile(filepath):
        filepath = os.path.join(dir, base_filename+"_"+str(file_num)+ext)
        file_num = file_num + 1

    return filepath

def write_csv(filepath, data):
    with open(filepath, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile, delimiter=',',
                            quotechar='|', quoting=csv.QUOTE_MINIMAL)
        writer.writerows(data)
        csvfile.close()

def read_csv(filepath):
    '''
    Read csv-like file and return as lines as list of list
    '''
    with open(filepath, newline='') as csvfile:
        reader = csv.reader(csvfile, delimiter=',', quotechar='|')
        rows = [row for row in reader]
        csvfile.close()

    return rows

def parse_commandline():
    parser = argparse.ArgumentParser(
        prog='optflowcam_convert',
        description ='Convert a Gaia Sky keyframes file (.gkf) to a Gaia Sky camera path file (.gsc) using the Optical Flow Camera method.')

    parser.add_argument('-i', 
                        '--in_file', 
                        help='The filepath to the file containing the camera path to be optimized.', 
                        type=check_path, 
                        required=True)
    parser.add_argument('-o', 
                        '--out_file', 
                        default=None, 
                        help='The filepath to which the result will be written. Default directory is same as in_file and default filename is <in_file>_result. You can use [in] as first characters to specify a path relative to the <in_file>.')
    parser.add_argument('-f', 
                        '--fps', 
                        default=60.0, 
                        type=float,
                        help='The target frame rate of the produced script.')
    
    args = parser.parse_args()
    validate_filepath(args.in_file)
    
    infilename, ext = os.path.splitext(args.in_file)
    if args.out_file == None:
        args.out_file=infilename+"_result"
    
    if args.out_file.startswith("[in]"):
        args.out_file = os.path.join(os.path.dirname(infilename), args.out_file[args.out_file.find(os.sep)+1:])
    
    if not args.out_file.endswith(".gsc"):
        args.out_file += ".gsc"
    args.out_file = make_filepath_unique(args.out_file)

    return args

def convert_gaia_to_optflow(gaia_keyframes):
    keyframes = []
    knots = []

    scale = 1.0

    t = 0
    for i, row in enumerate(gaia_keyframes):
        duration = float(row[0])
        t = t + duration
        try:
            # Integer time (epoch millis)
            simtime = int(row[1])
        except:
            # Instant string
            d = dateutil.parser.parse(row[1])
            simtime = d.timestamp() * 1_000


        pos = [float(p) / scale for p in row[2:5]]
        view = [float(p) / scale for p in row[5:8]]
        up = [float(p) / scale for p in row[8:11]]

        if len(row) == 13:
            # We have no target. Set standard of 500000 Km.
            fs = km_to_iu(500000) / scale
        elif len(row) == 16:
            # We have a target. Compute distance.
            target = [float(p)/scale for p in row[11:14]]
            pnp = np.array(pos)
            tnp = np.array(target)
            fs = np.linalg.norm(np.subtract(tnp, pnp)) / scale
        else:
            sys.exit("Bad format in keyframes file, row %d" % i)

        keyframe = {
            "duration": duration,
            "position": pos,
            "view": view,
            "up": up,
            "frustum_scale": fs,
            "simtime": simtime,
            "focal": 1
        }
        keyframes.append(keyframe)
        knots.append(t)

    return keyframes, knots

def convert_optflow_to_gaia(path: list, keyframes: list, fps: float):
    n = len(path)

    # Simulation time needs a strict linear interpolation between keyframes.
    # Keyframe_frames contains the final keyframe index for each frame.
    keyframe_frames = []
    for i in range(1, len(keyframes)):
        kf0 = i - 1
        kf1 = i
        curr_num_frames = int(fps * keyframes[kf1]["duration"])
        for j in range(curr_num_frames):
            keyframe_frames.append(kf1)
    keyframe_frames.append(kf1)


    current_kf = 0
    partial_count = 0
    gaia_path = [None]*n
    for i, p in enumerate(path):
        # Linear interpolation of simulation time.
        kf1i = keyframe_frames[i]
        kf1 = keyframes[kf1i]
        kf0 = keyframes[kf1i - 1]

        if current_kf != kf1i:
            # Restart counter.
            partial_count = 0
            current_kf = kf1i
        else:
            partial_count = partial_count + 1

        duration = kf1["duration"]
        curr_num_frames = int(fps * duration)

        # Work out interpolation variable.
        t = partial_count / (curr_num_frames - 1) if curr_num_frames > 1 else 0.0
        # Clamp t.
        t = max(0.0, min(1.0, t))

        simtime0 = kf0["simtime"]
        simtime1 = kf1["simtime"]
        simtime = int((1-t)*simtime0 + t*simtime1)

        # Add frame parameters.
        pos = p["position"]
        view = p["view"]
        up = p["up"]

        row = [simtime, pos[0], pos[1], pos[2], view[0], view[1], view[2], up[0], up[1], up[2]]
        gaia_path[i] = row

    return gaia_path

def main():
    args = parse_commandline()
    fps = args.fps

    gaia_keyframes = read_csv(args.in_file)
    keyframes, knots = convert_gaia_to_optflow(gaia_keyframes) 

    n = int(knots[-1] * fps - knots[0] * fps) 

    path = ofc_ip.interpolate_keyframes(keyframes, knots, 1, 
                                        "CatmullRom", "3DImageFlow", n,
                                        rho=np.sqrt(2))
    gaia_path = convert_optflow_to_gaia(path, keyframes, fps)

    with open(args.in_file + "_optflow.json", 'w', encoding='utf-8') as f:

        data = {
            "paths": [
                {   "name" : "gaia",
                    "cameras" : path }
            ]
        }

        json.dump(data, f, ensure_ascii=False, indent=4)
        f.close()

    write_csv(args.out_file, gaia_path)
    print("Output file written to %s" % args.out_file)

if __name__ == "__main__":
    main()
