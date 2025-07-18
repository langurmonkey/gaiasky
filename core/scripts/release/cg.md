<a name="unreleased"></a>
## [Unreleased](https://codeberg.org/gaiasky/gaiasky/src/branch/master)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.6.10...HEAD)


<a name="3.6.10"></a>
## [3.6.10](https://codeberg.org/gaiasky/gaiasky/releases/tag/3.6.10) (2025-07-18)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.6.9-2...3.6.10)

### Bug Fixes
- Layout issue in right pane of dataset manager window prevented it from using scroll, causing the window to be too large in some cases. 
- Typo in default shader class (`u_emissionCubemap` -> `u_emissiveCubemap`) prevented emissive/night cubemaps from working. 
- `emissiveCubemap` property was misnamed `emissiveColormap`. Add alias `nightCubemap`. Add `specularValue` and `specularValues` as aliases to `specular` with floating point number parameters. 
- Wording in new data pack notification window. 
- Properly filtre directories when building the SVT quadtree structure to avoid incorrect 'Worng directory name format' warnings. 
- Deactivating atmospheres causes night texture to apply uniformly to all planet as if it were a regular emissive texture. 
- Procedural generation window does not fit in the window with the new UI theme. Fix layout by introducing scroll panes and resizing elements. 
- Accept both `level1` and `level01` directory formatting for sparse virtual texture tiles. 
- Layout issue and tooltip text in datasets component. 
- Move `genVersionFile` task to the top of the dependency list (before `compileJava`) so that we always have the correct file available. 

### Features
- Set default SVT cache size to 10. Add information tooltip to cache size in preferences window. 
- Directory structure of virtual texture tiles now accepts more formats. 
- Country perimeter lines disappear when the camera gets close to the surface of the Planet. 
- Add markers for towns and landmarks. Night lights go out with ambient light in PBR shader. 
- Add new attributes to dataset definition format: links (now accepts multiple sources as links), creator (the creator or curator of the dataset), credits (specific attribute for credits instead of adding them to the description; multiple strings accepted). 
- Add file-level cache to TLE subsystem. TLE files are also cached per-group to avoid unnecessary sequential calls to fetch the same file. By default, the TTL for TLE files is 1 hour. 
- Localize Wikipedia API queries for object information. 

