#/usr/bin/env python

import sys, json, os

def help():
    print("Usage:")
    print("%s %s %s" % (sys.argv[0], "MASTER_FILE", "FILE_TO_PATCH"))
    print()
    print("Example:")
    print("  %s sso-dr3.json trojans.json" % sys.argv[0])
    exit(1)


n = len(sys.argv)

if n != 3:
    help()

main = sys.argv[1]
patch = sys.argv[2]

mymap = {}

# Read master file
loaded = 0
skipped = 0
with open(main, 'r') as f:
    data = json.load(f)
    for obj in data['objects']:
        if 'orbit' in obj:
            orbit = obj['orbit']
            if 'argofpericenter' in orbit:
                mymap[obj['name']] = orbit['argofpericenter']
                loaded += 1
            else:
                skipped += 1
        else:
            skipped += 1

print("Loaded %d values (%d skipped)." % (loaded, skipped))

# Read file to patch, update and write to new file
patched = 0
notpatched = 0
with open(patch, 'r') as f:
    data = json.load(f)
    for obj in data['objects']:
        name = obj['name']
        if 'orbit' in obj:
            orbit = obj['orbit']
            if 'argofpericenter' in orbit:
                if name in mymap:
                    orbit['argofpericenter'] = mymap[name]
                    patched += 1
                else:
                    print("Name not on map: %s" % name)
                    notpatched += 1
            else:
                print("Object has no 'argofpericenter': %s" % name)
                notpatched += 1

    print("Patched: %d, Not patched: %d." % (patched, notpatched))

    out = os.path.splitext(patch)[0] + ".patch.json"

    print("Saving result to: %s." % out)
    with open(out, 'w') as outFile:
        json.dump(data, outFile, indent=4)

print("Done.")
exit(0)
