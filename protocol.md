# Gaia Sky test protocol

This document lists a sequence of actions to be performed in order to test the readiness of Gaia Sky for release. These are typically pain-points that have either failed in the past or are too obscure to be caught during the regular usage of the program during the testing phase.

The actions are separated into rough categories.


## Configuration File

- Start Gaia Sky with non-existing configuration file.
- Start Gaia Sky with blank (empty) configuration file.
- Start Gaia Sky with non-existing data directory (`~/.local/share/gaiasky`).
- If there is a version bump, check that the configuration file is updated.

## Basic Navigation

- Check instant go-to command works (`Ctrl`+`g`).
    - Planets.
    - Single stars.
    - Stars in star sets.
    - Particles in particle sets.
- Same as previous, but with the smooth go-to command (focus pane).

## Shapes

- Add shape around Earth (right click context menu).
    - Wireframe and triangles.
    - Start time, check shape tracks object.
    - Remove all shapes.

## Procedural Generation

- Load Exonia dataset.
- Navigate to Exonia C, D, E and F, and check that the procedural generation works.
- Fire up the procedural generation window (right click on planet > procedural generation).
- Play around with the settings.
    - Test generation of surface, clouds and atmosphere.
    - Test 'randomize' and 'randomize all' actions.

## Bookmarks

- Click on object bookmark.
- Click on (invisible) object bookmark.
- Create object bookmark (focus info pane).
- Move object bookmark to directory.
- Click on location bookmark.
- Create location bookmark (context menu).

## SAMP

- Load star dataset from SAMP.
- Load particle dataset from SAMP.
- Load star cluster dataset from SAMP.
- Highlight (select) in Topcat, check action is reciprocated in Gaia Sky.
    - Same as above, but the other way around.

## Velocity Vectors

- Velocity vectors.
    - With regular dataset: line width, number, length, arrow caps.
    - With LOD dataset: performance.

## Location Log

- Check that closest object is correctly added to the location log as the camera moves.

## Info Windows

- Select star and check that the archive info window works. Check that the links (JSON data, archive) at the top work.
- Select a star or planet and check that the Wikipedia info window works.
