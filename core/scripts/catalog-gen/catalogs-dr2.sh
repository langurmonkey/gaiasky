#!/usr/bin/env bash

# This script runs the generation of all LOD DR2 catalogs
# You must copy this script to your $GS folder for it to work properly

# Get script path
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  GSDIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$GSDIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
GSDIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

# Locations
LOGS_LOC="$GSDIR/dr2_logs"
DATA_LOC="$HOME/gaiadata"
DR_BASE="$DATA_LOC/DR2"
DR_LOC="$DR_LOC/dr2"

# Column names (see ColId)
COLS="sourceid,ra,dec,pllx,ra_err,dec_err,pllx_err,pmra,pmdec,radvel,pmra_err,pmdec_err,radvel_err,gmag,bpmag,rpmag,ref_epoch,teff,radius,ag,ebp_min_rp"

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

function contains() {
    local n=$#
    local value=${!n}
    for ((i=1;i < $#;i++)) {
        if [ "${!i}" == "${value}" ]; then
            echo "y"
            return 0
        fi
    }
    echo "n"
    return 1
}

# SMALL
if [ $(contains "${TORUN[@]}" "small") == "y" ]; then
  DSNAME="001_$(date +'%Y%m%d')_dr2-small"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.1 --pllxerrfaint 0.005 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# DEFAULT
if [ $(contains "${TORUN[@]}" "default") == "y" ]; then
  DSNAME="002_$(date +'%Y%m%d')_dr2-default"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.2 --pllxerrfaint 0.005 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# BRIGHT
if [ $(contains "${TORUN[@]}" "bright") == "y" ]; then
  DSNAME="003_$(date +'%Y%m%d')_dr2-bright"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.9 --pllxerrfaint 0.0 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# LARGE
if [ $(contains "${TORUN[@]}" "large") == "y" ]; then
  DSNAME="004_$(date +'%Y%m%d')_dr2-large"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.5 --pllxerrfaint 0.125 --hip --pllxzeropoint -0.029 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# VERYLARGE
if [ $(contains "${TORUN[@]}" "verylarge") == "y" ]; then
  DSNAME="005_$(date +'%Y%m%d')_dr2-verylarge"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.9 --pllxerrfaint 0.9 --hip --pllxzeropoint -0.029 --postprocess --childcount 10000 --parentcount 50000 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# EXTRALARGE
if [ $(contains "${TORUN[@]}" "extralarge") == "y" ]; then
  DSNAME="006_$(date +'%Y%m%d')_dr2-extralarge"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --hip --pllxzeropoint -0.029 --geodistfile $DR_BASE/geo_distances/ --postprocess --childcount 10000 --parentcount 50000 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# RATHERLARGE
if [ $(contains "${TORUN[@]}" "ratherlarge") == "y" ]; then
  DSNAME="007_$(date +'%Y%m%d')_dr2-ratherlarge"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --pllxerrbright 0.5 --pllxerrfaint 0.5 --hip --pllxzeropoint -0.029 --postprocess --childcount 1000 --parentcount 50000 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi

# RUWE
if [ $(contains "${TORUN[@]}" "ruwe") == "y" ]; then
  DSNAME="008_$(date +'%Y%m%d')_dr2-ruwe"
  CMD="nohup $GSDIR/octreegen --loader CsvCatalogDataProvider --input $DR_LOC/csv/ --output $DR_LOC/out/$DSNAME/ --maxpart 100000 --ruwe 1.4 --ruwe-file $DR_BASE/ruwe/ruwes.txt.gz --hip --pllxzeropoint -0.029 --postprocess --childcount 1000 --parentcount 50000 --magcorrections --xmatchfile $GSDIR/data/gdr2hip/gdr2-hip-xmatch-all.csv --columns $COLS --nfiles $NFILES > $LOGS_LOC/$DSNAME.out"
  generate
fi
