#!/usr/bin/env python3

# This script converts Wavefront files from a regular
# XYZ cartesian system where X is the origin axis, XY is the
# references plane and Z points north, to the Gaia Sky
# reference system, where Z is the origin axis, ZX is the
# reference plane, and Y points north.
# Effectively, it does the following for every position:
# X -> Z'
# Y -> X'
# Z -> Y'

import sys
import os

def usage():
    print("Bad arguments:")
    print("\t%s FILE" % sys.argv[0])


args = sys.argv[1:]

if len(args) != 1:
    usage()
    sys.exit(1)

ifile = args[0]

if os.path.isfile(ifile):
    ofile = os.path.splitext(ifile)[0] + ".proc.obj"
    print("Output file: %s" % ofile)
    fo = open(ofile, 'w')
    with open(ifile, 'r') as fi:
        for line in fi:
            if line.startswith('v ') or line.startswith('vn '):
                # Look for vertex or vertex normal
                tokens = line.split()
                # t X Y Z -> Z X Y -> X' Y' Z'
                #fo.write(tokens[0] + ' ' + tokens[3] + ' ' + tokens[1] + ' ' + tokens[2] + '\n')

                # Kevin's meshes
                # X fundamental axis, XZ fundamental plane, Y down
                x = float(tokens[2])
                y = float(tokens[3])
                z = float(tokens[1])
                fo.write(tokens[0] + ' ' + str(x) + ' ' + str(y) + ' ' + str(z) + '\n')

                
            else:
                # Just output line
                fo.write(line)

    fo.close()
else:
    print("File does not exist: %s" % ifile)





