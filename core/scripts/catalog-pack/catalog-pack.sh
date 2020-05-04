#!/bin/bash

me=`basename "$0"`

function usage() {
echo "Usage: $me LOCATION NAME TITLE DESCRIPTION EPOCH VERSION"
echo
echo "	LOCATION    Location in the file system. Must contain log, metadata.dat, particles."
echo "	NAME        The dataset file system name (dr2-small)"
echo "	TITLE       The title (DR2 small)"
echo "	DESCRIPTION The description of the dataset"
echo "	EPOCH       The reference epoch"
echo "	VERSION     The version number"
echo
echo "Example:"
echo "$me ./000_20190213_dr2-verysmall dr2-verysmall 'DR2 - very small' 'Gaia DR2 very small: 5%\\/0.5% bright\\/faint parallax relative error.' 2015.5 3"
}


SCRIPT_FILE=$(readlink -f "$0")
SCRIPT_DIR=$(dirname $SCRIPT_FILE)

if [ "$#" -ne 6 ]; then
	usage
	exit 1
fi

LOCATION=$1
NAME=$2
TITLE=$3
DESCRIPTION=$4
EPOCH=$5
VERSION=$6

if [ ! -d "$LOCATION" ] || [ ! -d "$LOCATION"/particles ]; then
	echo "ERROR: location does not exist or it does not contain a dataset: $LOCATION"
	exit 1
fi
case "$NAME" in
	*\ *)
		echo "ERROR: dataset name can not contain spaces: $NAME"
		exit 1
		;;
esac

CATALOG_FOLDER=$LOCATION/catalog
CATALOG_FILE=$LOCATION/catalog-$NAME.json
CATALOG=$CATALOG_FOLDER/$NAME

echo "CATALOG_FOLDER: $CATALOG_FOLDER"
echo "CATALOG_FILE: $CATALOG_FILE"

# PARSE DATA AND CHECK VALUES

# Get size in bytes of dataset
SIZE_BYTES=$(set -- $(du -b --max-depth=1 $LOCATION) && echo $3)
# Get particles
NOBJECTS=$(set -- $(grep Particles: $LOCATION/log) && echo $6)
re='^[0-9]+$'
if ! [[ $NOBJECTS =~ $re ]] ; then
  NOBJECTS=$(set -- $(grep Particles: $LOCATION/log) && echo $7)
fi


# Check values
if [ -z "$VERSION" ]; then
    echo "ERROR: Version is empty"
    exit 1
fi
if [ -z "$SIZE_BYTES" ]; then
    echo "ERROR: Size (bytes) is empty"
    exit 1
fi
if [ -z "$NOBJECTS" ]; then
    echo "ERROR: Nobjects is empty"
    exit 1
fi

echo "SIZE:         $SIZE_BYTES bytes"
echo "NOBJECTS:     $NOBJECTS"
echo "EPOCH:        $EPOCH"
echo "VERSION:      $VERSION"


# CREATE AND MOVE CATALOG
mkdir -p $CATALOG
mv $LOCATION/log $LOCATION/metadata.bin $LOCATION/particles $CATALOG

# PREPARE JSON DESCRIPTOR FILE
cp $SCRIPT_DIR/catalog-template.json $CATALOG_FILE
sed -i 's/<TITLE>/'"$TITLE"'/g' $CATALOG_FILE
sed -i 's/<NAME>/'"$NAME"'/g' $CATALOG_FILE
sed -i 's/<VERSION>/'"$VERSION"'/g' $CATALOG_FILE
sed -i 's/<EPOCH>/'"$EPOCH"'/g' $CATALOG_FILE
sed -i 's/<DESCRIPTION>/'"$DESCRIPTION"'/g' $CATALOG_FILE
sed -i 's/<SIZE_BYTES>/'"$SIZE_BYTES"'/g' $CATALOG_FILE
sed -i 's/<NOBJECTS>/'"$NOBJECTS"'/g' $CATALOG_FILE

# TAR
TAR_FILE=catalog-$NAME.tar.gz
cd $LOCATION
tar -czvf $TAR_FILE catalog catalog-$NAME.json

set -- $(md5sum "$TAR_FILE") && echo $1 > md5
cd -

echo "Done: $LOCATION/$TAR_FILE"
