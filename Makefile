# This makefile is mainly intended for the Debian package, but it can
# install Gaia Sky on any Linux distro. It uses the gradle wrapper to
# build the application.
VERSION = $(shell git describe --abbrev=0 --tags HEAD)
REVISION = $(shell git rev-parse --short HEAD)

RELEASES ?= ./releases
RELEASE_LOC ?= $(RELEASES)/gaiasky-$(VERSION).$(REVISION)

PREFIX ?= /usr
APPPREFIX ?= /opt
BINPREFIX ?= $(PREFIX)/bin
SHAREPREFIX ?= $(PREFIX)/share
MANPREFIX ?= $(SHAREPREFIX)/man
DESKTOPPREFIX ?= $(SHAREPREFIX)/applications
GRADLE ?= gradlew
INSTALL ?= install
BIN ?= gaiasky
CP ?= cp
RM ?= rm

# Builds the app and puts it in the $(RELEASE_LOC) directory.
all: $(RELEASE_LOC)

$(RELEASE_LOC):
	$(GRADLE) core:dist

# Cleans build files.
clean:
	$(GRADLE) clean

# Cleans the releases directory.
distclean:
	rm -rf $(RELEASE_LOC)

# Installs Gaia Sky to $DESTDIR/$APPPREFIX and creates a symlink to $BINPREFIX.
install: all
	$(INSTALL) -m 0755 -d $(DESTDIR)$(APPPREFIX)/$(BIN)/
	$(CP) -r $(RELEASE_LOC)/* $(DESTDIR)$(APPPREFIX)/$(BIN)/
	$(INSTALL) -m 0755 -d $(DESTDIR)$(BINPREFIX)/
	ln -s $(DESTDIR)$(APPPREFIX)/$(BIN)/$(BIN) $(DESTDIR)$(BINPREFIX)/$(BIN)
	$(INSTALL) -m 0755 -d $(DESTDIR)$(MANPREFIX)/man6
	$(INSTALL) -m 0644 $(RELEASE_LOC)/$(BIN).6.gz $(DESTDIR)$(MANPREFIX)/man6

# Uninstalls Gaia Sky.
uninstall: 
	$(RM) -rf $(DESTDIR)$(APPPREFIX)/$(BIN)
	$(RM) $(DESTDIR)$(BINPREFIX)/$(BIN)
	$(RM) $(DESTDIR)$(MANPREFIX)/man6/$(BIN).6.gz

