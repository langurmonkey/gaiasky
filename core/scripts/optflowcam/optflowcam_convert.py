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

import json

import camera_spline as ofc_cs

import numpy as np

def m_to_iu(meters):
    return meters/(1e9)

def km_to_iu(kilometers):
    return kilometers/(1e6)

def iu_to_m(ius):
    return ius*1e9

def iu_to_km(ius):
    return ius*1e6

def pc_to_km(parsec):
    return parsec*3.0857*1e13

def pc_to_iu(parsec):
    return parsec*3.0857*1e7

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
    if not os.path.isdir(dir):
        os.makedirs(dir)

    base_filename, ext = os.path.splitext(os.path.basename(filepath))
    file_num = 1
    while os.path.isfile(filepath):
        filepath = os.path.join(dir, base_filename+"_"+str(file_num)+ext)
        file_num = file_num + 1

    return filepath

def write_csv(filepath, data):
    with open(filepath, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile, delimiter=' ',
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
        t = t + float(row[0])
        spacetime = int(row[1])
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
            "position": pos,
            "view": view,
            "up": up,
            "frustum_scale": fs,
            "time": spacetime,
            "focal": 1
        }
        keyframes.append(keyframe)
        knots.append(t)

    return keyframes, knots

def convert_optflow_to_gaia(path, keyframes):
    spacetime_start = keyframes[0]["time"]
    spacetime_end = keyframes[-1]["time"]

    n = len(path)

    gaia_path = [None]*n
    for i, p in enumerate(path):
        spacetime = int(spacetime_start + i/n*(spacetime_end - spacetime_start))

        pos = p["position"]
        view = p["view"]
        up = p["up"]

        row = [spacetime, pos[0], pos[1], pos[2], view[0], view[1], view[2], up[0], up[1], up[2]]
        gaia_path[i] = row

    return gaia_path

def main():
    args = parse_commandline()
    fps = args.fps

    gaia_keyframes = read_csv(args.in_file)
    keyframes, knots = convert_gaia_to_optflow(gaia_keyframes) 

    n = int(knots[-1]*fps - knots[0]*fps) + 1 

    path = ofc_cs.frustum_transform_spline(keyframes, knots, 1, n, method="CatmullRom", metric="3DImageFlow")
    gaia_path = convert_optflow_to_gaia(path, keyframes)

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

if __name__ == "__main__":
    main()
