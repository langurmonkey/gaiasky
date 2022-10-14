#!/bin/bash

# Convert constellation boundaries to JSON-friendly format.
# Get the source data from https://pbarbier.com/constellations/bound_in_20.txt
#
# Usage:
# convert.sh < bound_in_20.txt > constel-boundaries.json

echo "["
constel=
while IFS= read -r line; do
    tokens=($line)
    c=${tokens[2]}
    # Convert RA and DEC to floating point numbers
    ra=$(echo "${tokens[0]#\+} * 15.0" | bc -l)
    ra=$(printf "%.8f" ${ra})
    de=$(echo "${tokens[1]#\+}" | bc -l)
    de=$(printf "%.8f" ${de})
    if [[ "${c}" == "${constel}" ]]; then
      echo "  ,[${ra}, ${de}]"
    else
      echo "],"
      echo "["
      echo "  [${ra}, ${de}]"
    fi
    constel="${c}"
done
echo "]"
