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

import argparse, os, sys, json, glob
from tempfile import mkstemp
from shutil import move
from os import fdopen, remove
import subprocess

def check_args(args=None):
    parser = argparse.ArgumentParser(prog='gaiasky-release', description='This script prepares Gaia Sky for a new release. It will edit the build/script files, generate a tag, update the changelog file and creates the application release packages.')
    parser.add_argument('-g', '--gs_folder', type=str, default=os.environ.get('GS'), help='Location of the Gaia Sky folder. If not present, it uses the $GS environment variable.')
    parser.add_argument('-u', '--undo', action='store_true', help='Undoes the changes to the build and script files. If this flag is not present, the default is to *do* a release.')
    parser.add_argument('-t', '--tag', type=str, help='Tag name for the new release. If this is not present, the tag is not created, the change log is not generated and the release is not packed.')
    parser.add_argument('-a', '--tag_annotation', type=str, help='Tag annotation for the new release.')
    parser.add_argument('-d', '--def_file', type=str, help='JSON file with the action definitions.')
    parser.add_argument('-p', '--keystore_pwd', type=str, help='Keystore password for the windows binary.', required=True)

    return parser.parse_args(args)

def comment_line(file_path, pattern, comment_char, uncomment=False):
    #Create temp file
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


    # Find tag/version.
def gen_downloads_table(gsfolder):
    packages_dir = max(glob.glob(os.path.join(gsfolder, 'releases', 'packages-*')), key=os.path.getmtime)
    packages_name = os.path.basename(packages_dir)
    version_rev = packages_name[9:]
    version = packages_name[9:pg.rfind('.')]
    version_underscore = version.replace('.', '_')

    # Prepare SHA256 checksums.
    shamap = {}
    shaf = open(os.path.join(packages_dir, 'sha256sums'))
    for line in shaf:
        tokens = line.split(' ')
        key = tokens[1][tokens[1].rfind('.') + 1:].strip()
        shamap[key] = tokens[0]

    fin = open(os.path.join(gsfolder, 'core', 'scripts', 'release', 'downloads-table.template.html'), 'rt')
    fout = open(os.path.join(packages_dir, 'downloads-table.html'), 'wt')

    for line in fin:
        # Substitute version and revision.
        line = line.replace('${VERSION_FLAT}', version_underscore).replace('${VERSION_REVISION}', version_rev)
        line = line.replace('${SHA256_DEB}', shamap['deb'])
        line = line.replace('${SHA256_RPM}', shamap['rpm'])
        line = line.replace('${SHA256_SH}', shamap['sh'])
        line = line.replace('${SHA256_APPIMAGE}', shamap['appimage'])
        line = line.replace('${SHA256_WIN}', shamap['exe'])
        line = line.replace('${SHA256_DMG}', shamap['dmg'])
        line = line.replace('${SHA256_TAR}', shamap['gz'])
        fout.write(line)

    print("Generated downloads table file: %s" % fout.name)


if __name__ == '__main__':
    arguments = check_args(sys.argv[1:])

    print("Gaia Sky folder: %s" % arguments.gs_folder)


    # PARSE FILE DATA
    if arguments.def_file is None:
        arguments.def_file = "%s/%s" % (get_script_path(), "gaiasky-release-none.json")

        print("Loading definitions file: %s" % arguments.def_file)
        with open(arguments.def_file, 'r') as f:
            defs = json.load(f)

        # Make sure the version is set wherever it needs to be set
        print("Before running this script:")
        print()
        print("- Bump up the version number in core/exe/de.uni_heidelberg.zah.GaiaSky.metainfo.xml")
        print("- Make sure that $GS/releasenotes.txt exists and is ready, otherwise we will auto-generate it!")
        print()
        input("Press any key to continue, or C-c to quit...")

        releaserules = defs["releaserules"]

        # PROCESS FILES
        process_files(releaserules, arguments.gs_folder, do=not arguments.undo)

        # If undo, end 
        if arguments.undo:
            print("Undid possible changes, finishing here")
            print("Note that -t is not supported with -u")
            exit()

        # CREATE RELEASE - Only if not undo, tag is not empty and we have commands to run
        if arguments.tag is not None and defs["releasecommands"] is not None:
            if arguments.tag_annotation is None:
                arguments.tag_annotation = "Version %s" % arguments.tag

            print()
            print()
            print("======================== CREATE RELEASE ======================")
            print("  base:          %s" % arguments.gs_folder)
            print("  tag:           %s" % arguments.tag)
            print("  annotation:    %s" % arguments.tag_annotation)
            print("==============================================================")
            print()
            print()

            # Check revision >= 2
            if '-' in arguments.tag:
                revision = arguments.tag[arguments.tag.rfind('-')+1:]
                if int(revision) < 2:
                    print("ERROR: revision number can't be less than 2 -> %d" % int(revision))
                    exit()

            commands = defs["releasecommands"]

            for command in commands:
                cmdstr = []
                for cmd in command.split():
                    for key in arguments.__dict__:
                        if "&%s&" % key in cmd:
                            cmd = cmd.replace("&%s&" % key, "%s" % arguments.__dict__[key])
                    cmdstr.append(cmd)

                print("==> RUNNING: %s" % cmdstr)
                p = subprocess.Popen(cmdstr, cwd=arguments.gs_folder)
                p.wait()

            # REVERT FILES
            process_files(releaserules, arguments.gs_folder, do=False)

            # HTML DOWNLOADS TABLE
            gen_downloads_table(arguments.gs_folder)

            # PRINT TODOS
            print()
            print()
            print("================ TODOs ================")
            print()
            print(" > Your release %s is in %s/releases" % (arguments.tag, arguments.gs_folder))
            print(" > Generate the changelog for the new tag and prepend it to CHANGELOG.md: generate-changelog %s" % arguments.tag)
            print(" > Upload the files in andromeda.ari.uni-heidelberg.de:/gaiasky/files/releases/ (do not forget updates.xml)")
            print(" > Update symlink to latest: rm latest && ln -s new_release latest")
            print(" > Generate the html listings for the new files: dir2html")
            print(" > Update TYPO3 ARI website to point to new files: http://zah.uni-heidelberg.de/typo3")
            print("     > The HTML downloads table is in the packages folder!")
            print(" > Upload javadoc for new version (publish-javadoc %s && publish-javadoc latest)" % arguments.tag)
            print(" > Update docs if necessary (in particular, scripting API links): %s/docs" % arguments.gs_folder)
            print(" > Add new release to codeberg: https://codeberg.org/gaiasky/gaiasky/releases")
            print(" > Create new docs tag (%s) and generate the docs: make versions publish" % arguments.tag)
            print(" > Build AUR package (do 'makepkg --printsrcinfo > .SRCINFO') and commit AUR git repository")
            print(" > Update flatpak repo and do pull request. See here: https://codeberg.org/gaiasky/gaiasky/issues/337#issuecomment-521949")
            print(" > Post to social media")
            print()
            print(">DONE<")

        else:
            print("You must give a tag number!")
