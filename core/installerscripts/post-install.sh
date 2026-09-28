#!/usr/bin/env bash

# Link run script
ln -s $I4J_INSTALL_LOCATION/gaiasky /usr/bin/gaiasky
# Link desktop file
ln -s $I4J_INSTALL_LOCATION/gaiasky.desktop /usr/share/applications/gaiasky.desktop

# Update desktop/MIME database so that the gaiasky:// URL scheme handler
# is registered.
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database -q /usr/share/applications
fi

# Add man page
cp $I4J_INSTALL_LOCATION/gaiasky.6.gz /usr/share/man/man6/
