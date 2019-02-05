#!/usr/bin/env bash

# Link desktop file
ln -s $I4J_INSTALL_LOCATION/gaiasky.desktop /usr/share/applications/gaiasky.desktop
# Link run script
ln -s $I4J_INSTALL_LOCATION/gaiasky /usr/bin/gaiasky

# Add man page
cp $I4J_INSTALL_LOCATION/gaiasky.6.gz /usr/share/man/man6/
