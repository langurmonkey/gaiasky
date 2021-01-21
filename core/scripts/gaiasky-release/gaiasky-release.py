#!/usr/bin/env python3
"""
This script prepares Gaia Sky for a new release
:Author:
    Toni Sagrista Selles
:Organization:
    Astronomisches Rechen-Institut - Zentrum fur Astronomie Heidelberg - UNIVERSITAT HEIDELBERG
:Version:
    0.1
"""

import argparse, os, sys, json
from tempfile import mkstemp
from shutil import move
from os import fdopen, remove
import subprocess

def check_args(args=None):
    parser = argparse.ArgumentParser(prog='gaiasky-release', description='This script prepares Gaia Sky for a new release. It will edit the build/script files, generate a tag, update the changelog file and creates the application release packages.')
    parser.add_argument('-g', metavar="GAIASKY_LOC", type=str, default=os.environ.get('GS'), help='Location of the Gaia Sky folder. If not present, it uses the $GS environment variable.')
    parser.add_argument('-u', action='store_true', help='Undoes the changes to the build and script files. If this flag is not present, the default is to *do* a release.')
    parser.add_argument('-t', metavar="TAG_NAME", type=str, help='Tag name for the new release. If this is not present, the tag is not created, the changelog is not generated and the release is not packed.')
    parser.add_argument('-a', metavar="ANNOTATION", type=str, help='Tag annotation for the new release.')
    parser.add_argument('-p', metavar="FILE", type=str, help='JSON file with the action definitions.')

    return parser.parse_args(args)

def comment_line(file_path, pattern, comment_char, uncomment=False):
    # Create temp file
    fh, abs_path = mkstemp()
    with fdopen(fh,'w') as new_file:
        with open(file_path) as old_file:
            for line in old_file:
                if pattern in line:
                    line_lstrip = line.lstrip(' ')
                    nspaces = len(line) - len(line_lstrip)
                    spaces = line[:nspaces]
                    if uncomment:
                        if line_lstrip.startswith(comment_char):
                            new_file.write(spaces + line_lstrip[len(comment_char):])
                            print(" > OK")
                        else:
                            print(" > NO NEED")
                            new_file.write(line)
                    else:
                        if not line_lstrip.startswith(comment_char):
                            new_file.write(spaces + comment_char + line_lstrip)
                            print(" > OK")
                        else:
                            print(" > NO NEED")
                            new_file.write(line)
                else:
                    new_file.write(line)

    #Remove original file
    remove(file_path)
    #Move new file
    move(abs_path, file_path)

def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))

def process_files(defs, gsfolder, do=True):
    for file in defs:
        print()
        print("==== PROCESSING: %s ====" % (file))

        elem = defs[file]
        commentchar = elem["commentchar"]

        uncomment_list = elem["touncomment"] if "touncomment" in elem else []
        comment_list = elem["tocomment"] if "tocomment" in elem else []

        # If undoing, we revert the changes
        if not do:
            bak = uncomment_list
            uncomment_list = comment_list
            comment_list = bak

        # Uncomment lines
        for line in uncomment_list:
            print("Uncommenting: %s" % line, end="", flush=True)
            comment_line("%s/%s" % (gsfolder, file), line, commentchar, uncomment=True)

        # Comment lines
        for line in comment_list:
            print("Commenting: %s" % line, end="", flush=True)
            comment_line("%s/%s" % (gsfolder, file), line, commentchar, uncomment=False)


if __name__ == '__main__':
    arguments = check_args(sys.argv[1:])

    gs_folder = arguments.g
    def_file = arguments.p
    undo = arguments.u
    tag = arguments.t
    tag_annotation = arguments.a

    print("Gaia Sky folder: %s" % gs_folder)
    print()

    # PARSE FILE DATA
    if def_file is None:
        def_file = "%s/%s" % (get_script_path(), "gaiasky-release-none.json")

    print("Loading definitions file: %s" % def_file)
    with open(def_file, 'r') as f:
        defs = json.load(f)

    releaserules = defs["releaserules"]

    # PROCESS FILES
    process_files(releaserules, gs_folder, do=not undo)

    # If undo, end 
    if undo:
        print("Undid possible changes, finishing here")
        print("Note that -t is not supported with -u")
        exit()

    print("Before starting, make sure you updated the GS version in GaiaSkyDesktop and in the configuration files if necessary")
    input("When done, come back and hit enter to continue.")

    # CREATE RELEASE - Only if not undo, tag is not empty and we have commands to run
    if tag is not None and defs["releasecommands"] is not None:
        if tag_annotation is None:
            tag_annotation = "Version %s" % tag

        print()
        print()
        print("======================== CREATE RELEASE ======================")
        print("  base:          %s" % gs_folder)
        print("  tag:           %s" % tag)
        print("  annotation:    %s" % tag_annotation)
        print("==============================================================")
        print()
        print()

        commands = defs["releasecommands"]

        for command in commands:
            cmdstr = []
            for cmd in command.split():
                for key in arguments.__dict__:
                    if "&%s&" % key in cmd:
                        cmd = cmd.replace("&%s&" % key, "%s" % arguments.__dict__[key])
                cmdstr.append(cmd)

            print("==> RUNNING: %s" % cmdstr)
            p = subprocess.Popen(cmdstr, cwd=gs_folder)
            p.wait()

        # REVERT FILES
        process_files(releaserules, gs_folder, do=False)

        # PRINT TODOS
        print()
        print()
        print("================ TODOs ================")
        print()
        print(" > Your release %s is in %s/releases" % (tag, gs_folder))
        print(" > Upload the files in mintaka.ari.uni-heidelberg.de:/dataB/gaiasky/files/releases/")
        print(" > Generate the html listings for the new files: dir2html")
        print(" > Update TYPO3 ARI website to point to new files: http://zah.uni-heidelberg.de/typo3")
        print(" > Update docs if necessary (in particular, scripting API links): %s/docs" % gs_folder)
        print(" > Add new release to gitlab: https://gitlab.com/langurmonkey/gaiasky/-/releases")
        print(" > Create new docs tag (%s) and generate the docs: %s/docs/bin/publish-docs" % (tag, gs_folder))
        print(" > Build AUR package (do 'makepkg --printsrcinfo > .SRCINFO') and commit AUR git repository")
        print(" > Update flatpak repo and do pull request. See here: https://gitlab.com/langurmonkey/gaiasky/-/issues/337#note_460878130")
        print(" > Upload javadoc for new version (publish-javadoc %s && publish-javadoc latest)" % tag)
        print()
        print(">DONE<")

    else:
        print("You must give a tag number!")
