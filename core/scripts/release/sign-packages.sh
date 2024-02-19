#!/bin/bash

# This script gets a directory and creates detached signatures for
# all packages in it.

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <directory>"
    exit 1
fi

dir=$1

for file in $dir/*.gz $dir/*.appimage $dir/*.deb $dir/*.rpm $dir/*.sh $dir/*.dmg $dir/*.exe $dir/*.zip; do
    if [ -e "$file" ]; then
      echo "Creating signature file: $(basename $file)"
      # Create detached signature file, force [y] to overwrite
      gpg --yes --detach-sign "$file"
    fi
done
