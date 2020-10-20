#!/usr/bin/env bash

# This script runs catalogs defined in catalogs-def.json. It needs to be called
# with some variables set: LOGS_LOC, DATA_LOC, DR_BASE, DR_LOC, COLS, CATDEF
# Dependencies: jq
# You must copy this script and the definition to your $GS folder for it to work properly

# Get script path
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  GSDIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$GSDIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
GSDIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

usage() {
    echo "Usage: $0 [-c catalog_1,catalog_2,...] [-n max_files] [-h]"
    echo
    echo "    OPTIONS:"
    echo "       -c    comma-separated list of catalog names (small,default,bright,large,verylarge,extralarge,ratherlarge,ruwe)"
    echo "       -n    maximum number of files to load, negative for unlimited"
    echo "       -h    show this help"
    1>&2; exit 1;
}

NFILES=-1

while getopts ":c:n:h" arg; do
    case $arg in
        c)
            CATALOGS=${OPTARG}
            ;;
        n)
            NFILES=${OPTARG}
            ;;
        h)
            usage
            ;;
        *)
            usage
            ;;
    esac
done
# Datasets to generate. Passed via arguments.
# Values: small, default, bright, large, verylarge, extralarge, ratherlarge, ruwe
if [ -z "$CATALOGS" ]; then
    TORUN=("small" "default")
    echo "Using default catalog list: ${TORUN[*]}"
else
    IFS=',' read -r -a TORUN <<< "$CATALOGS"
    echo "Using user catalog list: ${TORUN[*]}"
fi

function generate() {
  echo "GENERATING: $DSNAME"
  echo "Input: $DR_LOC/csv/"
  echo "Output: $DR_LOC/out/$DSNAME/"
  echo "Log: $LOGS_LOC/$DSNAME.out"
  echo "Cmd: $CMD"

  $( eval $CMD )
}

NCAT=$(jq '.catalogs | length' $CATDEF)

for CURRENT_CATALOG in "${TORUN[@]}"; do
    for ((j=0;j<NCAT;j++)); do
        # Get catalog name
        NAME=$(jq ".catalogs[$j].name" $CATDEF)
        # Remove quotes
        NAME=$(sed -e 's/^"//' -e 's/"$//' <<<"$NAME")
        if [ "$NAME" == "$CURRENT_CATALOG" ]; then
            DSNAME="00$j-$(date +'%Y%m%d')-$CATALOG_NAME-$NAME"
            echo $DSNAME
            CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/"
            NATTR=$(jq ".catalogs[$j] | length" $CATDEF)
            for ((k=0;k<NATTR;k++)); do
                KEY=$(jq ".catalogs[$j] | keys[$k]" $CATDEF)
                # Remove quotes
                KEY=$(sed -e 's/^"//' -e 's/"$//' <<<"$KEY")
                if [ "$KEY" != "name" ]; then
                    VAL=$(jq ".catalogs[$j].$KEY" $CATDEF)
                    # Remove quotes
                    VAL=$(sed -e 's/^"//' -e 's/"$//' <<<"$VAL")
                    #echo "$KEY -> $VAL"
                    if [ "$VAL" == "null" ]; then
                        CMD="$CMD --$KEY"
                    else
                        CMD="$CMD --$KEY $VAL"
                    fi
                fi
            done
            CMD="$CMD --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
            generate
        fi
    done
done
