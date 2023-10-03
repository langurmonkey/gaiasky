<a name="3.5.5-1"></a>
## [3.5.5-1](https://codeberg.org/gaiasky/gaiasky/tree/3.5.5) (2023-10-03)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.5...3.5.5-1)

### Features
- update umbra and penumbra highlight colors.
- improve default pane background, touch up mini-map layout.
- add collapsible groups and per-group 'select all' and 'select none' controls to dataset manager. 
- add transparency support (encoded in diffuse texture/color) to shadow maps.
- add support for scattering diffuse material properties in default and tessellation shaders.

### Bug Fixes
- regression where all actions were printed to stdout. 
- unexpected and weird behaviour when spamming repeatedly left buttons in new UI. 

<a name="3.5.5"></a>
## [3.5.5](https://codeberg.org/gaiasky/gaiasky/tree/3.5.4-1) (2023-09-29)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.4-1...3.5.5)

### Features
- add new user interface which replaces the old controls window. The old controls window is still available (activate it in preferences window). 
- add new 'play camera path' action, bound to `alt`+`c` by default.
- update default control pane key bindings (time, camera, visibility, etc.) to not use the Alt key. 
- add better star close-up shader, and a new 'scene settings' section in preferences window with an option to render stars as spheres. 
- prepare PBR shader to accept iridescence, transmission and thickness values.
- revamp shader include directive to accept different extensions and file references in angle brackets. All shader libraries moved to `shader/lib`. 
- retire Gaia FOV camera modes. 
- adjust default atmosphere exposure value. 
- disable fading scrollbars everywhere. 
- tune normal strength in tessellation shaders to map to elevation multiplier. 

### Bug Fixes
- enable full shader file names in raymarching shaders. 
- typo, 'user interface resetted' -> 'user interface reset'. 
- restore height sampling functionality to prevent clipping through tessellated terrain.
- remove cinematic camera slow-down when close to the surface of a planet. 
- scale tessellation quality using the body size to prevent severe slow-downs in smaller bodies. 

<a name="3.5.4-1"></a>
## [3.5.4-1](https://codeberg.org/gaiasky/gaiasky/tree/3.5.4) (2023-09-21)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.4...3.5.4-1)

### Bug Fixes
- move action stuttering on some systems due to no input changes since last sync. 
- application title in loading screen squashes the logo. 

### Build System
- set bundled JRE version to 17 instead of 20; seemingly, there are non-negligible performance issues with the JRE 20 on some configurations. 

<a name="3.5.4"></a>
## [3.5.4](https://codeberg.org/gaiasky/gaiasky/tree/3.5.3) (2023-09-20)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.3...3.5.4)

### Features
- add square recursive grid style, additionally to the existing circular concentric rings. 
- add diagonal lines at 30 and 60 degrees to the circular recursive grid. 
- redesign time pane layout and widgets (warp slider, play/pause, information boxes, etc.) in controls window. 
- upgrade internal version numbering to include a sequence number within the revision (major.minor.revision-sequence). It should be backwards compatible, but starting with this version, internal version numbers have at least 7 digits instead of 5. 
- bump source version number. 
- refactor separator UI elements. 
- redesign loading screen to follow welcome screen style. 

<a name="3.5.3"></a>
## [3.5.3](https://codeberg.org/gaiasky/gaiasky/tree/3.5.2) (2023-09-14)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.2...3.5.3)

### Bug Fixes
- atmosphere object distance falloff. 
- exit JVM process with a proper status code depending on application state. 
- mismatch in bundled JRE versions in install4j script. Fixes [#737](https://codeberg.org/gaiasky/gaiasky/issues/737). 
- glitches and artifacts in atmosphere ground tessellation shader. 
- base opacity applied to model bodies when rendered as billboards. 
- update minimum required JRE in install4j script to 15. Fix version tag. 
- use camrecorder target frame rate instead of frame output system one in keyframes. 
- check keyboard focus state when polling cursors in main mouse/kdb listener. Fixes [#733](https://codeberg.org/gaiasky/gaiasky/issues/733). 
- raymarching event description. 
- small improvements in UCD parser for IDs and magnitudes. 
- do not preemptively check display resolution in macOS, as it usually runs a headless JVM. 
- clean editing keyframe on deletion. Fixes [#734](https://codeberg.org/gaiasky/gaiasky/issues/734). 
- do not render keyframe lines/points when camcorder is playing. Fix index issue in path linear interpolator. Fixes [#735](https://codeberg.org/gaiasky/gaiasky/issues/735). 

### Features
- new welcome screen title design and splash. 
- add check for keyframe timings (t_kf * fps % 1 == 0) and respective notice. 
- add new visibility type 'Keyframes', which controls the keyframe points and lines. Add new keyboard mapping SHIFT+K to toggle it. 
- add tooltips to keyframe playback buttons. 
- move `onlyBody` trajectory attribute to `bodyRepresentation`, which is an enum that enables the representation of only body, only orbit or both. The `onlyBody` attribute is still kept as a proxy. Rename `pointColor` to `bodyColor` in trajectories. `pointColor` is kept as an alias. 
- skip back/fwd, step back/fwd, play/pause and timeline slider all work. Move keyframes logic and model to keyframe manager. 
- add direct playback capability to keyframes. Still WIP. 
- keyframe system to use new camera path object in export operation. 
- move from buffered file-backed recording and playback to model-backed solutions for the camrecorder. This means that, during recording, the data is captured and stored in memory and persisted to disk when the recording finishes. During playback, the data is loaded to memory at once at the beginning and played from there. 
- use quaternion spherical linear interpolation for the camera orientation when exporting keyframes. The old setting 'camrecorder > keyframe > orientation' is not used anymore. 
- use quaternion interpolation for the camera transition methods in the scripting API implementation. 

### Merge Requests
- Merge branch 'keyframes'

<a name="3.5.2"></a>
## [3.5.2](https://codeberg.org/gaiasky/gaiasky/tree/3.5.1) (2023-07-26)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.1...3.5.2)

### Bug Fixes
- exporting keyframes to camera path is missing the very last frame. Fixes [#729](https://codeberg.org/gaiasky/gaiasky/issues/729). 
- restore line rendering in keyframes, lost in a regression during the line refactoring campaign. 
- only deactivate main mouse/kbd listener if the current dialog is modal. 
- full-screen log item in translation files. 
- visual layout and information structure in about window, system tab. 
- apply patch provided by luzpaz fixing many typos in comments and strings. Fixes [#726](https://codeberg.org/gaiasky/gaiasky/issues/726). 

### Features
- improve keyframes window layout, with more space for keyframes and better sizing of keyframes table. 
- default to full screen for small displays, and refactor display resolution fetching process. 
- check AMD APU code name to detect Steam Deck. 
- steam deck programmatic detection to default to full screen at startup.

<a name="3.5.1"></a>
## [3.5.1](https://codeberg.org/gaiasky/gaiasky/tree/3.5.0) (2023-07-19)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.0...3.5.1)

### Bug Fixes
- paths in Windows do not accept certain characters and are unsuitable to represent our bookmarks. Add specific `BookmarkPath` implementation that fixes this. 
- VR flag from `--openvr` to `--openxr`. 

<a name="3.5.0"></a>
## [3.5.0](https://codeberg.org/gaiasky/gaiasky/tree/3.5.0-rc10) (2023-07-17)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.5.0-rc10...3.5.0)

### Features
- add control for maximum number of velocity vectors per star set in preferences dialog, performance section.
- add CLI flag to force HiDPI mode either to 'Logical' or 'Pixels'.
- enable holding comma or period to slow down or speed up time. Map time speed up and slow down to d-pad up and down in game controllers.
- add `trailMinOpacity` to trajectories. This raises the opacity of a trail to the given minimum value.
- speeding up and slowing down time can be done by pressing and holding '.' and ',' respectively. No more frantically clicking a key. Also, the time warp change is smooth now.
- add camera position/orientation named bookmarks, additionally to the already existing object bookmarks. Add bookmarks for many eclipses in the default bookmarks.txt.
- use coordinates provider from objects to sample periodic orbits instead of hardcoded algorithms.
- dim atmosphere w.r.t. camera, star and eclipsing body positions during eclipses.
- initial support for ephemeris based on Chebyshev polynomials.
- add support for VSOP2000.
- add eclipses by projecting the shadow umbra and penumbra from moons to planets and vice-versa. The option to outline the umbra and penumbra is given as a checkbox in the preferences. Eclipses can be deactivated, also from the preferences window, graphics tab.
- celestial sphere showcase script
- add a few showcase scripts
- scripts showing an object on a horseshoe orbit around Jupiter
- update wording on point and line styles in preferences dialog to make it more concise and clear.
- use GPU lines as the default line renderer for trajectories. This results in a big performance improvement, especially when many lines are on display. GPU lines now also can use the geometry shader to render as polyline quad-strips.
- use a geometry shader to generate the triangles in the polyline quadstrip renderer instead of the CPU. Performance is much improved.
- add geometry shader stage (optional) to shader program provider and to extended shader program.
- add alpha value to color map in the dataset highlight color picker.
- adapt star cluster loader to use the new extended particle set features by default.
- enable arbitrary models rendered with instancing in extended particle sets.
- add extended particle sets. These support, in addition to positions, proper motions and sizes. They also can be rendered with icosahedron sphere models instead of quads. Add lazy initialization of render systems.
- enable labels for regular particle sets.
- improve unsharp mask shader to produce much cleaner and useful results.
- remove non-instanced triangles mode (and renderers); they are a waste of memory and almost never faster than the instanced version. Use screen-aligned billboards in regular mode, and scene-aligned ones in any of the cubemap modes (360, planetarium, etc.).
- add support for texture arrays in particle sets. Particle sets can now define a group of texture files taht will be applied to the particles at random.
- activate v-sync by default during welcome and loading GUI (not in VR). Use busy wait to lock to the perfect target FPS, when active.
- add maximum allowed distance as a hard limit, set at 50 Gpc, roughly twice the size of the observable universe.
- top info interface date and time elements are clickable, and display the date/time edit dialog.
- migrate VR version to OpenXR API.
- add arbitrary warping mesh support to distor the final image according to a warping mesh file in PFM (portable float map) format.
- replace lens flare checkbox with lens flare strength slider.
- enable choosing the lens flare type (pseudo lens flare, real lens flare) in the settings. Make new lens flare the default.
- proper lens flare post-processing effect.
- add maximum number of virtual texture tile load operations per frame to settings file, and increase its default value from 3 to 8. Use a deque instead of a queue for the tiles waiting to be loaded, and add the newly observed tiles to the head instead of the tail.
- increase chromatic aberration amount in lens flare effect.
- add variations of API calls concerning positions using the distance units as an extra parameter.
- move archetypes definition to JSON file to facilitate the automatic generation of documentation.
- move attribute map definition to JSON file, which contains the definitions of all the attributes per component, and also a description for each of them. The aim is to generate part of the data format documentation from this JSON file.
- enable implementing body coordinates directly from Python scripts. Add new API calls and a full script example with data files.
- expose upscale filter setting to UI via a select box in the preferences window.
- add chromatic aberration shader, together with a slider in the preferences window to disable it or control the amount.
- enable proper motion for single particles, fix issues with tracking.
- add support for ambient occlusion sampler (standalone and with metallic and roughness channels) in PBR normal and tessellation shaders.
- add (partial) support for glTF, binary glTF and embedded glTF.
- add (hidden) attribute "renderParticles" to star and particle sets to disable the rendering of particles and stars for that set.
- add aliases to label position for pc and Km, remove unnecessary operations from shape updater.
- add keyboard mapping and action to multiply the camera movement speed (mapped to 'Z' by default).
- add actions and key bindings to toggle the camera mode and the cinematic behaviour.
- support translation in Km with the 'translateKm' attribute.
- directly support archetype names in JSON data format, additionally to the legacy class names.
- enable affine transformation support for shape objects.
- support 'standard' PBR attributes in OBJ loader.
- add warp mesh file selector to preferences, to select the warping mesh for the new spherical mirror projection. Fix layout for file choosers in frame and screenshot locations.
- add support for the spherical mirror projection in planetarium mode.
- add upscale filter setting to preferences, add XBRZ upscale shader, filter and effect.
- non user-prompted events (download fail, checkum error, etc.) create persistent notifications which need to be closed manually. Persistent notifications are accompanied with a close button to indicate they need to be closed manually.
- add parallax demo script.
- separate height scale from elevation multiplier in shaders. Decrease step of some sliders.
- enable arbitrary parameter map injection in data loaders.
- add `fadeDistanceUp` and `fadeDistanceDown` to trajectory objects to control the fading distances when a body is present.
- add animations to all UI elements, add animation time to settings.
- add in/out animations to gamepad GUI and maximize/minimize to debug and focus interfaces. Add animation time as a new setting in settings file. Promote date dialog to generic dialog.
- add size attribute to ray-marching effects, enable absolute predicted positions for ray-marching effects, instead of only a static position.
- add per-vertex colors (instead of per-segment) to polyline quadstrip renderer for smooth shading.
- add shortcut to settings in context menu.
- more on input.
- first OpenXR test, not working on Linux over SteamVR due to unsupported swapchain formats.
- add ambient level and color to individual models.
- add 'fixed angular size' support for star datasets. It renders all stars with a fixed angular size. In the case of variable stars, if a fixed angular size is set, the variability is expressed via the opacity.

### Bug Fixes
- correctly initialize camera focus and mode at gamepad/VR GUI creation.
- restore functionality in archive/DB information window when selecting stars.
- restore functionality of location log, lost in a regression during the ECS refactoring.
- star set labels respect label fading factor.
- move Gaia Sky logo over the title in help window.
- 'cancel download' catalan and spanish translation texts.
- roughness and metallic colors and textures not being set correctly in wavefront loader.
- look-up table paths in procedural generation window.
- regression in go-to command with star and particle groups.
- check for empty configuration file at startup and overwrite it if necessary.
- orientation locking does not work in backwards time. Fixes [#718](https://codeberg.org/gaiasky/gaiasky/issues/718).
- bump default safe mode OpenGL version to 3.3 to support instancing. GPUs from 2007 support it, so it should be safe. Also, do not attempt to compile double-precision geometry shaders in safe mode, since they are not used and may crash anyways.
- line trail mapping in non-timestamped trajectories. Fixes [#715](https://codeberg.org/gaiasky/gaiasky/issues/715).
- double-rebuilding of dataset manager on close. Also, closing the dataset manager does not persist the preferences.
- shader version for normal shader from 410 to 330 to prevent crashes in old GPUs.
- regression in texture binding introduced in 3.5.0-RC3 (commit 7db456cc).
- adjust rotate/turn strength when using the arrow keys.
- random bugs in label render system.
- do not show collapse/expand buttons in collapsible windows if collapsing is disabled.
- wrong key used in galactic latitude attribute in color map picker.
- input multiplexer and welcome GUI initialization sequence may cause a startup crash in certain conditions.
- initialization sequence for distance scale factor, and particle groups breaking in VR when using triangles.
- do not skip processing of LOD-based object when it is the current camera focus.
- adjust visibility and opacity determination for entities with active fade in map; mostly used for NEARGALCAT objects.
- spread GPU streaming of multi-component billboard datasets over several frames.
- billboard set texture array uniform setting.
- pixel-perfect interaction in VR menus. Surface normal was being transformed with a matrix that contained a translation instead of only using rotations.
- properly scale particle set particles in VR.
- proper motions in VR mode.
- interactive load of JSON datasets that contain objects with 3D models blocks main thread.
- guard in `getLineObject()` calls with a timeout does not use the time out. Fixes [#711](https://codeberg.org/gaiasky/gaiasky/issues/711).
- scale orbit element particles for VR, readjust size limits.
- slider texture filtering issue in green, blue, orange and red themes.
- star surface shader crash when motion blur is on.
- non-canonical OpenGL parameters in some configuration calls.
- headless mode crashes on start.
- star billboard and quad positions in stereo mode.
- absolute position method in particle set does not guard against null parameters. Fixes [#710](https://codeberg.org/gaiasky/gaiasky/issues/710).
- dataset manager layout, especially on low-resolution displays.
- move hardcoded billboard galaxy threshold to model initializer.
- single star rendering from afar, spherical position determination, graph update sequence for proper motion and other objects, bypass area and loc update when components are off, camera position lock for stars in star sets.
- several issues with single star rendering and magnitude initialization.
- add mechanism to automatically disable certain post-processing effects on certain render modes (e.g. light glow on panorama/planetarium mode).
- internal dataset loading operations out of order: move scene graph insertion before set-up.
- enable model-less shape objects for label-only use cases.
- removing objects with children only effectively removes the first children, leaving orphan objects in the scene graph which do not get updated but get added to the render lists.
- removal of an object from the graph does not remove its children.
- normals, bi-normals and tangents in icosphere creator.
- crash with static light models, file filter in dataset loader.
- cloud virtual textures not working due to missing shader attribute.
- crash loading wrongly constructed cluster file.
- trail attribute of orbits not always working. GPU non-trail orbits not working.

### Performance Improvements
- improve performance of velocity vectors in LOD datasets by setting restrictions on the octant's solid angle before sending the star sets to the velocity vector renderer.
- remove guard clauses in shader interpolation function that are already covered by `smoothstep()`.
- distribute SVT render pass over 5 frames to split contribution over time and achieve more or less constant frame pacing.

### Code Refactoring
- set 'useColor' in models to false by default, so that the object color is not passed to the 3D model unless explicitly stated.
- remove dpendency on gdx-gltf, implement own modification which directly loads meshes using 32-bit integer indices instead of 16-bit shorts.
- rename some classes to make them more concise. Fix and improve javadoc comments.
- consolidate shader and resource disposing in post processors.
- unify tessellation and regular shader infrastructure.
- trigger star/particle set update task in updater systems instead of via the camera motion event. Shorten minimum times between metadata updates.
- move particle and star set updater methods to consumers initialized at creation.
- rename all math utilities converted from single to double precision from [name]d to [name]Double.
- use solid angle component instead of hardcoded variables for star cluster thresholds.
- move 'forceLabel' attribute to Label component.

### Build System
- AUR package dependency from `jre-openjdk` to `java-runtime`.
- update bundled JRE to 20.0.1, update installer welcome image.
- upgrade to LibGDX 1.12.0.
- update build file tasks to latest gradle syntax recommendations.
- update oshi-core from 5.8.7 to 6.4.1.
- remove commons-math3 dependency by implementing own interpolator.
- update STIL from 4.0.+ to 4.1.+, and Jackson from 2.13.2 to 2.15.+.
- remove dependency on commons-imaging library for monochrome to RGB conversions.
- update JCommander from 1.81 to 1.82; upgrade slf4j-nop from 1.7.+ to 2.0.+.
- remove JPEG-XL support via external library (vavi-image), better wait for official support in Java Image I/O.
- upgrade LWJGL version from 3.3.1 to 3.3.2.
- improve format of release notes file in template.
- upgrade to Gradle 8.1.1.

### Documentation
- started writing test protocol document.
- add package descriptions to all packages except for the `gaiasky.util` children.
- add package descriptions, refactor API interfaces to own packages.
- improve documentation of some API calls.
- update AppStream metadata file with proper id and screenshots.

### Style
- update style with new hard wrap length and new wrapping rules for function signature parameters.

<a name="3.4.2"></a>
## [3.4.2](https://codeberg.org/gaiasky/gaiasky/tree/3.4.1) (2023-03-15)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.4.1...3.4.2)

### Bug Fixes
- labels in shape objects not showing up, focusable shape objects not working.
- unchecked cast in focus check method.
- welcome screen Gaia Sky icon causing problems with macOS package.
- initialize screenshots and frame output post processor even if not initially active.

### Documentation
- add note concerning support for Apple M1/2 ARM.

### Features
- add support for reference system transformations to STIL data provider.
- add image format and quality to preferences dialog for screenshots and frame output system.
- add dark background to content frame in welcome screen.
- do not force-show dataset manager when no base data is found.


<a name="3.4.1"></a>
## [3.4.1](https://codeberg.org/gaiasky/gaiasky/tree/3.4.0) (2023-03-09)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.4.0...3.4.1)

### Bug Fixes
- typo in German translation file. Fixes [#706](https://codeberg.org/gaiasky/gaiasky/issues/706). 
- remove unused configuration setting `scriptsLocation`. 
- tooltip and layout issues in datasets component. 
- invalid focus state in natural camera. 
- add object name checks to most API calls. 
- omit regular gamepad window bindings in gamepad configuration window. 
- first loading frame produced in VR with incorrect sizing. 
- connect visibility buttons in gamepad GUI to global visibility event, and add tooltips with name. 
- remove IVRHeadsetView interface from OpenVR initialization so that Gaia Sky works with OpenComposite, which translates OpenVR to OpenXR. 
- properly close file stream when done with them. 
- prevent hang on close due to daemon thread notify() calls. 
- incorrect filtering in slider backgrounds. 
- incorrect filtering in UI table baground image. 
- script with wrong loader name. Fixes [#703](https://codeberg.org/gaiasky/gaiasky/issues/703). 

### Code Refactoring
- update VR controllers in their own system. 
- move VR UI classes to own package. 

### Documentation
- update API call fade in/out descriptions. 

### Features
- add generic VR controller. 
- add VR controller interaction in VR welcome screen. 
- welcome and loading VR screens are now 3D surfaces in-scene. 
- sRGB setting enables SRGB format only in VR frame buffers. 
- add specific mappings for Valve Index VR controllers. 
- VR controllers use same mappings format as gamepads. 
- use gamepad UI in VR. 
- initial implementation of a proper in-scene VR user interface with mouse interaction. 

<a name="3.4.0"></a>
## [3.4.0](https://codeberg.org/gaiasky/gaiasky/tree/3.3.2) (2023-02-13)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.3.2...3.4.0)

### Features
- sparse virtual texture support.
- add initial support for the JPEG-XL (.jxl) image format.
- add filter box to dataset lists in the dataset manager.
- add 'clear' button to text fields to clear the contents at once.
- implement proper update mechanism for objects via JSON descriptors.

### Bug Fixes
- escape config file backup path in Windows.
- VR controller paths in VR context. Fixes [#702](https://codeberg.org/gaiasky/gaiasky/issues/702).
- build task including certs and other unneeded stuff.
- implement bilinear interpolation on SVT, make interpolation generic regardless of data structure used.
- skip only GB instead of GBA in RGB buffer readout in automatic tone mapping effect.
- error computing mean position in particle set when there are no particles.

### Build System
- upgrade build script to install4j 10.0.4.

### Code Refactoring
- move source version to settings.
- move light glow code to own render pass class.
- move shadow map code to own render pass class.

### Documentation
- flag Gaia Sky VR as alpha software.

<a name="3.3.2"></a>
## [3.3.2](https://codeberg.org/gaiasky/gaiasky/tree/3.3.1) (2022-12-18)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.3.1...3.3.2)

### Bug Fixes
- stop intercepting data location in paths, implementation did not work for Windows when the original path contained '*', and it was useless anyway, as we always use fully-defined paths. 
- avoid expanding dataset file paths in dataset manager to prevent horizontal overflow. 
### Features
- improve drag rotation behaviour when very close to objects. 


<a name="3.3.1"></a>
## [3.3.1](https://codeberg.org/gaiasky/gaiasky/tree/3.3.0) (2022-12-13)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.3.0...3.3.1)

### Bug Fixes
- backup/restore perspective camera state before/after rendering off-screen frames and screenshots to avoid rendering artifacts whenever any of the cubemap-based modes (planetarium, panorama) is on. 
- context menu crashes when no object is hit. Fixes [#694](https://codeberg.org/gaiasky/gaiasky/issues/694). 
- add 'gamepads detected' notification text to I18N files. 
- screen resizing sets internal resolution state and is persisted on restart. Adjust automatic UI scaling algorithm. 
- apply 'angle from zenith' in planetarium mode as camera rotation instead of as an effect parameter to enable 5-side optimization when aperture <= 180. 
- reimplement automatic tone mapping algorithm and manager. 
- refocusing on a star set does not always work. 
- camera speed API call not mapping values correctly. 
- add 'compiling shaders' message during loading process. 
- bookmarks to stars not selecting the right objects. 
- affine transformations applied correctly to mesh objects. 

### Build System
- changelog creation script now does not produce a full change log for the whole history of the project anymore. It now gets the tag or tag   range as input and the maintainer is supposed to update the   `CHANGELOG.md` file manually. The changes are provided since a few   releases ago in the release notes file. 

### Code Refactoring
- move ambient light watcher from main Gaia Sky class to own inner class, remove unused event. 
- guard GLSL libraries with `#ifndef`/`#define` preprocessor statements to prevent double definitions. 
- change screenshots system from poll to reactive. 
- rename controller GUI to gamepad GUI. 
- move content of 'Gaia' tab to 'Data' tab in preferences window. 

### Documentation
- improve contributing guidelines for translations. 

### Features
- re-balance the weights for every axis-mapped action in main gamepad listener, resulting in a much smoother navigation with game controllers and joysticks. 
- add sRGB color space support in preparation for migration to OpenXR. Activate it with the key `graphics::useSRGB` in the configuration file. 
- add support for images in tip generator, include gamepad input images. 
- complete Spanish translation to 100%. 
- add star visual settings to gamepad GUI. 
- add preliminary keyboard support for navigating UI menus and windows. 
- add star glow factor control and API call to fine tune the amount of light irradiated by stars close to the camera. 
- add screen mode button at the top-right of the welcome GUI. 
- increase global font size and UI spacing. 
- generic controller support in all UI windows. 
- add gamepad support in dataset manager window. 
- remove music component from controls window. 
- activate lazy shader loading for all but the basic shader versions (SSR, motion blur, relativistic mode, gravitational waves). 
- migration to new dataset structure, add data location cleaner utility at startup. 
- add some extra room between dataset types to improve readability. 
- support gzipped data descriptor files. 
- introduce index of refraction for the celestial sphere when orthosphere view is on. Included as a slider in the experimental section of the GUI. 


<a name="3.3.0"></a>
## [3.3.0](https://codeberg.org/gaiasky/gaiasky/tree/3.3.0-rc02) (2022-11-11)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.3.0-rc02...3.3.0)

### Bug Fixes
- disable tessellation on macOS by default.
- most recent version determination in version checker.

### Code Refactoring
- move render lists to java collections.

### Documentation
- update in-app mastodon reference from cat to social, as it's in English.
- remove twitter link, add mastodon hashtag.

### Features
- enable minimizing focus info interface, fix debug interface layout.

### Style
- mend variable names in about window to follow camel case.
- fix linter stylistic warnings in GUI and interfaces.


<a name="3.3.0-rc02"></a>
## [3.3.0-rc02](https://codeberg.org/gaiasky/gaiasky/tree/3.3.0-rc01) (2022-11-03)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.3.0-rc01...3.3.0-rc02)

### Bug Fixes
- remove unnecessary spacing in layout of focus info interface.
- trajectory size determination algorithm not accurate, breaks when adding points close to the origin.
- add model size attribute to compute solid angle for model objects more accurately.

### Features
- key bindings file versioning. If the key bindings file starts with the line `#v[version]`, that version is compared to the default one and overwritten when necessary. That makes updating key bindings much easier.
- add new camera mode, orthosphere view, which includes the regular and the cross-eye orthosphere projections.
- add cross-eye view of the orthographic projection of the celestial sphere


<a name="3.3.0-rc01"></a>
## [3.3.0-rc01](https://codeberg.org/gaiasky/gaiasky/tree/3.2.1) (2022-10-27)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.1...3.3.0-rc01)

### Bug Fixes
- correctly query graphics device for resolution and apply scaling.
- toggling SSR and motion blur does not update depth state in some shaders.
- double definitions in shader libraries.
- render constellation boundaries as closed polygons in order to avoid artifacts.
- index errors in keybaord in controller GUI.
- back-buffer scale is now applied correctly (and only once), works with external view.
- remove angle from zenith from cubemap renderer in planetarium, use shader-based solution.
- prevent rendering titles if panorama mode is on.
- initialize position from coordinates object during initialization phase.
- land at location never returns. Fixes [#674](https://codeberg.org/gaiasky/gaiasky/issues/674).
- set foucs with `FocusView` object type. Fixes [#671](https://codeberg.org/gaiasky/gaiasky/issues/671).
- provide a correct index mapping for arbitrary attributes with string values.
- particle size of interactively loaded point cloud datasets.
- vertically flip UV coordinates of two-faced billboard to correct texture orientation.
- regression in billboard group rendering. Fixes [#663](https://codeberg.org/gaiasky/gaiasky/issues/663).
- update coordinates in invisible only when present. Fixes [#662](https://codeberg.org/gaiasky/gaiasky/issues/662).
- prevent runtime error due to non-invertible matrix in spacecraft entity.
- diffuse color contribution calculated incorrectly when nLights > 1 in normal shaders.
- names in star groups can now be localized, fix focus name in panel.
- translate strings of filters, shapes, datasets and minimap. Fixes [#403](https://codeberg.org/gaiasky/gaiasky/issues/403).
- moon orbits are recomputed more often.
- filters crash with instanced star renderers.
- modal windows made not collapsible by default.
- hotkey tooltip backgrounds.
- jump in Pluto's orbit due to deviation between full periods.
- highlight 'all visible' setting in quad-based star renderers.
- frame sequence number synchronized, value updated when opening preferences.
- use view angle instead of view angle apparent in go to object API call.
- increase number of vertices of minimap shape renderer, fixes crash in heliosphere minimap.
- typo in Jupiter English translation file, add meshes to data descriptor file.
- break link in dataset manager if too long.
- make sure direction and up vectors are orthogonal in camera transition call.
- increase size star point buffer when needed.
- null-check satellite attitude before getting quaternion. Fixes [#402](https://codeberg.org/gaiasky/gaiasky/issues/402).
- empty tips may crash Gaia Sky at startup.
- 'add scene graph object' event missing source object. Fixes [#400](https://codeberg.org/gaiasky/gaiasky/issues/400).
- remove phase of pi radians in default-model orbital elements.
- regression with libgdx 1.11.0 that caused vertical tooltips.
- null-check settings in crash reporter.
- workaround for libgdx backslash bug in asset manager. Fixes [#398](https://codeberg.org/gaiasky/gaiasky/issues/398).
- hide system cursor correctly with GLFW until libgdx 1.10.1 is released.
- use minimum width for debug interface to prevent dynamic resizing depending on content.

### Build System
- update changelog template repository to codeberg.
- upgrade gradle wrapper version to 7.5.1.
- update gitlab references to codeberg, when possible. Use codeberg API for version checking.
- remove gitlab CI file.
- move namespace from `gitlab.com/langurmonkey` to `gitlab.com/gaiasky`.
- docs project no longer a submodule.
- add aarch64 JRE to macOS bundle for M1 machines. Move to macOS single bundle archive from deprecated old single bundle.
- force safe graphics mode on M1 macOS.
- upgrade to libgdx 1.11.0 and LWJGL 3.3.1 --- this adds M1 Mac support.
- use default GC (G1) in favor of Shenandoah (only LTS).

### Code Refactoring
- flatten object hierarchy by removing some classes, merging their functionality upwards.
- abstract attitude loading system, remove Gaia class, use heliotropic satellite.
- add `I18nFormatter` to reformat i18n files.
- flatten object hierarchy by removing some classes, merging their functionality upwards.
- remove old date formatting infrastructure (desktop, html, mobile) in favor of a direct approach.
- remove useless number formatting infrastructure.

### Documentation
- typos and so.
- improve comments on color maps GLSL code.
- add new panorama orthographic projection to API javadocs.
- remove wrong license (leftover from old copy-paste) in fisheye fragment shader code.
- add contributor
- update repository pointers to codeberg.

### Features
- recompute UI scale at startup when starting with default config file. Fix particle set size when rendered as points, fix star scaling issue.
- add back-buffer scale API call.
- add repository to -v information.
- Include "orthosphere" panorama mode -- orthospherical projection with both hemispheres overlaid to give a view of the celestial sphere from the outside.
- enable gamepad operation in welcome GUI.
- Include orthographic projection in panorama mode. Includes both hemispheres on the screen, side by side. Can be cycled through with `ctrl`+`shift`+`k`.
- add support for KTX and ZKTX textures.
- add back-buffer scale controls to UI.
- add re-projection GUI drop-down in preferences window. Add `setReprojectionMode()` scripting API call.
- rename fisheye post-processing effect and shader to reprojection. Update cubemap projection from fisheye to azimuthal equidistant.
- Add shaders for Lambert equal-area, orthographic and stereographic projections.
- expose fisheye post-processing mode to configuration file and settings.
- change from thread to scheduled task to remove the mode change info pop-up.
- add mode change pop-up setting to enable or disable showing a pop-up with information when changing modes (panorama, planetarium, stereo, etc.).
- add GUI button to exit stereo mode.
- add new object type, 'cosmic locations', to mark the positions of interesting areas or regions.
- enable scene lights for shape objects when static light is off.
- new model attribute 'blendMode', which defaults to 'alpha' but can also be set to 'additive' to control the object blending.
- add several new functions to enable setting the camera state from scripts.
- add time zone to settings. Time zone can be either UTC, or the system default. Update date dialog year limits, fix time component layout.
- raymarching effects work with ECS model.
- light glow effect now works with ECS model.
- expose post-processor properties as settings in configuration file.
- add zero-point to gamepad configuration.
- introduce gamepad support for spacecraft mode, remove 'gaia scene' camera mode (can be mimicked with focus mode), refactor input controllers, fix default SDL gamepad mappings file.
- save configuration when closing dataset manager window.
- add popup notice when opening the keyframes window if component 'others' is not visible.
- add full screen bit depth and refresh rate to fully qualify selected full screen modes.
- improve layout and information of crash window.
- add notice when there are no datasets.

<a name="3.2.1"></a>
## [3.2.1](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0) (2022-06-21).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0...3.2.1).

### Bug Fixes
- translate strings of filters, shapes, datasets and minimap. Fixes [#403](https://codeberg.org/gaiasky/gaiasky/issues/403).
- filters crash with instanced star renderers.
- moon orbits are recomputed more often.
- modal windows made not collapsible by default.
- hotkey tooltip backgrounds.
- jump in Pluto's orbit due to deviation between full periods.
- highlight 'all visible' setting in quad-based star renderers.
- frame sequence number synchronized, value updated when opening preferences.
- typo in Jupiter English translation file, add meshes to data descriptor file.
- increase number of vertices of minimap shape renderer, fixes crash in heliosphere minimap.
- break link in dataset manager if too long.

### Build System
- move namespace from 'codeberg.org/langurmonkey' to 'codeberg.org/gaiasky'.
- docs project no longer a submodule.

### Features
- save configuration when closing dataset manager window.
- use view angle instead of view angle apparent for `goToObject()` API call.


<a name="3.2.0"></a>
## [3.2.0](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc16) (2022-06-07)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc16...3.2.0)

### Bug Fixes
- make sure direction and up vectors are orthogonal in camera transition call.
- increase size star point buffer when needed.
- null-check satellite attitude before getting quaternion. Fixes [#402](https://codeberg.org/gaiasky/gaiasky/issues/402).
- empty tips may crash Gaia Sky at startup.
- 'add scene graph object' event missing source object. Fixes [#400](https://codeberg.org/gaiasky/gaiasky/issues/400).
- remove phase of pi radians in default-model orbital elements.

### Features
- add number of samples to orbit objects.
- add popup notice when opening the keyframes window if component 'others' is not visible.
- add full screen bit depth and refresh rate to fully qualify selected full screen modes.


<a name="3.2.0-rc16"></a>
## [3.2.0-rc16](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc15) (2022-05-16)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc15...3.2.0-rc16)

### Bug Fixes
- regression with libgdx 1.11.0 that caused vertical tooltips.

### Build System
- force safe graphics mode on M1 macOS.
- add aarch64 JRE to macOS bundle for M1 machines. Move to macOS single bundle archive from deprecated old single bundle.
- downgrade jamepad to 2.0.14.2 as the newer 2.0.20.0 does not work with ARM macs.


<a name="3.2.0-rc15"></a>
## [3.2.0-rc15](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc14) (2022-05-12)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc14...3.2.0-rc15)

### Bug Fixes
- null-check settings in crash reporter.

### Build System
- upgrade to libgdx 1.11.0 and LWJGL 3.3.1 --- this adds M1 Mac support.

### Features
- improve layout and information of crash window.


<a name="3.2.0-rc14"></a>
## [3.2.0-rc14](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc13) (2022-04-29)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc13...3.2.0-rc14)

### Bug Fixes
- workaround for libgdx backslash bug in asset manager. Fixes [#398](https://codeberg.org/gaiasky/gaiasky/issues/398).
- hide system cursor correctly with GLFW until libgdx 1.10.1 is released.
- use minimum width for debug interface to prevent dynamic resizing depending on content.

### Build System
- use default GC (G1) in favor of Shenandoah (only LTS).

### Code Refactoring
- flatten object hierarchy by removing some classes, merging their functionality upwards.
- abstract attitude loading system, remove gaia class, use heliotropic satellite.
- add `I18nFormatter` to reformat i18n files.
- remove useless number formatting infrastructure.
- remove old date formatting infrastructure (desktop, html, mobile) in favor of a direct approach.

### Features
- add notice when there are no datasets.


<a name="3.2.0-rc13"></a>
## [3.2.0-rc13](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc12) (2022-04-19)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc12...3.2.0-rc13)

### Bug Fixes
- correctly update label text when setting `SliderPlus` values.
- correctly shut down background worker and manager threads so that JVM can finish gently.
- add default values for orbit line and point colors.
- configure crash window size with same code as regular window.

### Build System
- remove run tasks, use '--args' gradle argument instead.
- sign Windows packages with self-sigend certificate.
- add Linux archive for itch.io.
- add Windows archive to `install4j` template for uploading to itch.io.

### Code Refactoring
- move update process to runnable, protect render lists from outer access.
- improve service thread implementation.
- move tips and funny texts to main bundle, add some dangling hardcoded strings to bundle, enable translation of keyboard keys.
- move all text from -v flag to i18n keys.
- remove some warnings, clean up code.
- rename some packages and move some code around.

### Documentation
- update contributing document to reflect new objects file.

### Features
- add cyrillic characters to `main-font`, `font2d` and `font3d` fonts.
- new API call: `setDatasetPointSizeMultiplier(String, Double)`.
- enable translation of object names, and add first translation files for most common objects like planets, constellations, etc.
- add scaffolding to translate welcome tips and funny sentences. Add Catalan translation for those.
- complete catalan translation file, add neat options to translation status utility.
- add buttons to launch preferences dialog and to quit at the bottom right of the welcome screen.
- add translation status code and task, update catalan translation file.
- add offline mode, activated in configuration file.

### Performance Improvements
- separate UI reload from localized name updates.


<a name="3.2.0-rc12"></a>
## [3.2.0-rc12](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc11) (2022-03-22)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc11...3.2.0-rc12)

### Bug Fixes
- set argument of pericenter to zero when the epoch is not the reference epoch in the SSO converter for DR3.
- compute mu automatically if period is set in orbital elements.

### Code Refactoring
- use bit mask instead of 64-bit integer as attributes mask so that we can register more than 64 attributes. Add proper 3-component specular color to materials. Add diffuse cubemaps for models and clouds. Fix a number of shader issues.
- rename `u_environmentCubemap` to `u_diffuseCubemap` in shaders.
- rename setting `data::skyboxLocation` to `data::reflectionSkyboxLocation`.

### Features
- add meshes as datasets, connect dataset visibility to per-object visibility controls for meshes.
- add specular, normal, emissive, metallic, roughness and height cubemap support to default and tessellation shaders.
- add cubemap diffuse texturing capability to models.


<a name="3.2.0-rc11"></a>
## [3.2.0-rc11](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc10) (2022-03-14)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc10...3.2.0-rc11)

### Code Refactoring
- remove unused id from components, fix skybox orientation.

### Features
- implement the use of cubemaps in skyboxes. Fix cubemap reflection directions.
- asteroids get full dataset controls (except for colormaps) like highlighting, coloring and sizing.
- add catalog info goodies to asteroids catalogs.


<a name="3.2.0-rc10"></a>
## [3.2.0-rc10](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc09) (2022-03-11)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc09...3.2.0-rc10)

### Bug Fixes
- some data paths using forward slashes '/' instead of '\' on Windows.
- big refactor that fixes the runtime activation and deactivation of both motion blur and SSR. Lots of little fixes and improvements to the render system.
- add VR offset to reflection view direction.
- VR controller info positioning, settings crash.

### Code Refactoring
- move double array to util package.

### Features
- add asteroids/sso catalog types.

### Performance Improvements
- improve performance of orbital elements particles by treating them as whole groups in the CPU using new model object and renderer.


<a name="3.2.0-rc09"></a>
## [3.2.0-rc09](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc08) (2022-03-08)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc08...3.2.0-rc09)

### Code Refactoring
- old Milky Way renderer converted to general-purpose billboard group infrastructure to enable representation of any quad-based point data.


<a name="3.2.0-rc08"></a>
## [3.2.0-rc08](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc07) (2022-03-07)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc07...3.2.0-rc08)

### Bug Fixes
- add null-checks for some OpenVR properties (required by Oculus 2). Add VR information in crash reporter. Fixes [#393](https://codeberg.org/gaiasky/gaiasky/issues/393) (again).


<a name="3.2.0-rc07"></a>
## [3.2.0-rc07](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc06) (2022-03-04)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc06...3.2.0-rc07)

### Bug Fixes
- wrong scale factor in orbital elementss-based orbits in VR.
- update `VRControllerRole` values from `ETrackedControllerRole` from SteamVR spec. Fixes [#393](https://codeberg.org/gaiasky/gaiasky/issues/393).
- broken `setObjectVisibility()` API call. Fixes [#391](https://codeberg.org/gaiasky/gaiasky/issues/391).
- escape path before sending SAMP metadata. Fixes [#392](https://codeberg.org/gaiasky/gaiasky/issues/392).
- regression adding bookmarks. Fixes [#390](https://codeberg.org/gaiasky/gaiasky/issues/390).
- directional lights from stars still applied when stars are made invisible.
- restrict the rendering of pointer guides and cross-hairs in stereo and cubemap modes.

### Code Refactoring
- remove unused and obsolete jython fix.
- improve shader combination and lookup (from ssr branch).

### Features
- expose SSR to preferences dialog, experimental section.
- screen space reflections Merge branch 'ssr'.


<a name="3.2.0-rc06"></a>
## [3.2.0-rc06](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc05) (2022-02-23)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc05...3.2.0-rc06)

### Bug Fixes
- improve check box layout in preferences dialog.
- focus info interface width jitters when moving in free mode on occasions.
- Gaia FOV modes with triangle-based stars.

### Features
- add new red-blue anaglyph profile mode, additionally to the pre-existing red-cyan.
- add proxy configuration directly in Gaia Sky's config file.
- add dynamic resolution checkbox to preferences dialog.
- finish dynamic resolution implementation with an arbitrary number of levels.


<a name="3.2.0-rc05"></a>
## [3.2.0-rc05](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc04) (2022-02-15)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc04...3.2.0-rc05)

### Bug Fixes
- highlight dataset API call.
- particle dataset loading default size limits when using tris.
- issues with dataset loading via scripting.
- improve error handling in dataset manager.
- Julian date algorithm.
- prevent repeated entries in search suggestions.

### Code Refactoring
- add source object to events by default.

### Features
- expand/collapse panes by clicking on title.
- add collapsible entry and use it for datasets in datasets component.
- add context menu to dataset items in dataset component.
- add GUI control to edit object fade time [ms].
- improve layout and UX of datasets component.
- add roughness texture and value to normal shader, enable mipmaps in skybox.
- add popup notifications for certain important actions and events. These popup notifications can be closed by clicking on them, and they stay on screen for 8 seconds by default.
- additional API call to load star datasets.
- save session log file to.
- add API call to set label colors.
- enable label colors for all objects. Always defaults to white.
- add method to inject transformation matrix directly into orbit, add change of basis matrix creation utility.
- allow spherical coordinates in `StaticCoordinates`, additional fixes.
- add background thread count and pool size to debug information.


<a name="3.2.0-rc04"></a>
## [3.2.0-rc04](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc03) (2022-01-26)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc03...3.2.0-rc04)

### Bug Fixes
- dataset manager path handling on Windows.


<a name="3.2.0-rc03"></a>
## [3.2.0-rc03](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc02) (2022-01-25)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc02...3.2.0-rc03)

### Bug Fixes
- initial VR gui distance.
- lighting bug when multiple stars cast a light on an object.

### Features
- new 'force label visibility' flag for model objects. This flag causes the label of the object to always be rendered, regardless of the solid angle and other constraints. The flag is controlled by new button at the top of the focus information pane (bottom-right) and via two new scripting API calls.
- simplify loading mechanism by joining catalog files with object files. No distinction is necessary anymore, for all of them work in the same way and are loaded by the same entities.
- add file list and scroll pane to dataset information in dataset manager.
- add pixel lighting shading to meshes.


<a name="3.2.0-rc02"></a>
## [3.2.0-rc02](https://codeberg.org/gaiasky/gaiasky/tree/3.2.0-rc01) (2022-01-18)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.2.0-rc01...3.2.0-rc02)

### Bug Fixes
- set encoding of i18n files to UTF-8, update formatting.
- layout of version line table.
- regression in apparent magnitude resource bundle key.
- effective temperature array initialization bug in STIL loader.
- add notice whenever a `default-data` update is available.

### Build System
- update gradlew version.
- update install4j script to latest version, use bundled JRE for .deb, upgrade to Java 17.

### Features
- updated the Bulgarian translation.
- improve layout of welcome and loading GUIs.
- redesign dataset manager. The old download manager/catalog selection duo is phased out in favor of the new dataset manager. This is more usable and less confusing, while allowing for parallel downloads.
- update splash.
- add camera distance from Sun in the camera section of the focus information pane.
- update welcome GUI background image.

### Merge Requests
- Merge branch 'new-dataset-manager'.


<a name="3.2.0-rc01"></a>
## [3.2.0-rc01](https://codeberg.org/gaiasky/gaiasky/tree/3.1.6) (2021-12-17)
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.6...3.2.0-rc01)

### Bug Fixes
- crosshair in cubemap, planetarium, stereo and VR modes.
- remove usage of deprecated Java APIs.
- do not add objects that already exist (have same names and same type) to scene graph.
- cloud rendering artifacts.
- reflections in tessellation shaders.
- reflected cubemap orientation (was upside down).
- restore correct values on cancel in preferences dialog.
- show warn message when trying to select object from invisible dataset in search dialog.
- getting particle position no longer results in null pointer.
- update directory permissions error message to make it easier to understand.
- default style of headline and subhead messages, as well as their positioning.
- JSON output of REST API server.
- reload data files when data path changes.
- data manager misbehavior when data location path is a symlink.
- rename old configuration files after conversion to new format.
- time offset (6711 yr) in Moon's position lookup.
- fix star clusters fade between model and billboard.
- color picker listener stops working after first click.

### Build System
- remove old run targets.
- remove deprecated features from build files.
- update gradle wrapper version to 7.3.
- upgrade Jackson library version.
- remove GSON dependency version.
- Java minimum version set to 15 in build script check.
- automatically generate release notes during build.
- update appimage JDK version to `16.0.2+7`.

### Features
- new non-constant-density fog shader which approximates physical fog much better than before.
- add an arbitrary number of load progress bars.
- enable loading internal JSON descriptor files from UI.
- interactive procedural generation of planetary surfaces, clouds and atmospheres.
- add interactive surface generation from the GUI.
- interactive procedural generation of cloud and atmosphere components from the GUI.
- add 'randomize all' function to totally randomize planet surfaces.
- add shift to biome LUT, improve procedural generation.
- generate normal map from elevation data if needed.
- planet generation with elevation, diffuse and specular textures.
- materials overhaul.
- get Gaia Sky ready for star systems with proper orbits.
- add `--headless` flag to run in headless mode (hidden window).
- add API calls to configure and take screenshots.
- get Gaia Sky ready for star systems with proper orbits.
- add individual size scale factor to star/particle group datasets.
- improve mode switching dialogs with a few goodies and QOL updates.
- implement mosaic cubemaps, quad-based star group renderer.
- enable orbit trails in `GPU` VBO mode and remove the "orbit style" setting, for now the "GPU lines" line style setting uses VBOs.
- add 'New directory' button to file chooser, fix event propagation with generic dialogs.
- show release notes at startup after a version update.
- convert provider parameters to dataset options for STIL provider.
- add variability to close-up stars and star models.
- add variable stars as a new dataset type.
- add provider parameters to data providers.
- improve CA,DE,ES translations.
- improve bookmarks, add missing i18n keys Fixes [#380](https://codeberg.org/gaiasky/gaiasky/issues/380).
- shapes (spheres, cones, cylinders, etc.) of arbitrary sizes can now be added around any object, with the possibility of tracking the object's size. This is an extension of [#378](https://codeberg.org/gaiasky/gaiasky/issues/378) which includes many more options plus an API entry point.
- add shapes around objects Fixes [#378](https://codeberg.org/gaiasky/gaiasky/issues/378).
- add setting to select preferred units (ly/pc).
- add the possibility to track objects.

### Performance Improvements
- initially size index hash maps to avoid resize operations.

### Style
- consolidate normal shader vertex data into struct.
- organize imports in whole codebase.

### Merge Requests
- Merge branch 'points-triangles'.

<a name="3.1.6"></a>
## [3.1.6](https://codeberg.org/gaiasky/gaiasky/tree/3.1.5) (2021-09-22).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.5...3.1.6).

### Bug Fixes
- VR GUI object initialization -- consolidate init() signature.

<a name="3.1.5"></a>
## [3.1.5](https://codeberg.org/gaiasky/gaiasky/tree/3.1.4) (2021-09-22).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.4...3.1.5).

### Bug Fixes
- pointer guides use wrong shader program and render incorrectly.
- concurrent camera state modification issue resulting in camera jumps and skips when capturing still frames -- regression introduced with arbitrary precision module in `3.1.0`.
- order of repositories in build file.
- frame output target FPS not persisted correctly.
- add notice when location log is empty.
- individual visibility in asteroids and other orbital elements-based objects.
- preferences dialog catalog selection tab.
- manipulate visibility of stars with proper names.
- bug in `goToObject()` camera direction.
- star offset in star groups [#375](https://codeberg.org/gaiasky/gaiasky/issues/375).
- some tweaks to VR mode, fix crashes.

### Build System
- remove gradle plugin portal from repositories.

### Code Refactoring
- API change: `unparkRunnable()` is now deprecated in favor of `removeRunnable()`.
- remove all statics from global resources.
- encapsulate global resources.
- remove generics from `IAttribute`, remove static model from star groups.
- multiple internal initialization changes.

### Documentation
- update URLs in  file.

### Features
- change value of screenshot mode from 'redraw' to 'advanced' both in the API call `setFrameOutputMode()` and in the configuration file.
- improve welcome screen button icons.
- add a filter text field to per-object visibility window.
- add collapsible groups to catalog selection window.
- add mouse-over behavior for most UI elements.
- add hover over feature to buttons in skins.
- new YAML based configuration system to replace java properties file.


<a name="3.1.4"></a>
## [3.1.4](https://codeberg.org/gaiasky/gaiasky/tree/3.1.3) (2021-07-02).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.3...3.1.4).

### Bug Fixes
- tone mapping persistence issue [#374](https://codeberg.org/gaiasky/gaiasky/issues/374).
- unify internal delta time across all modules.
- regression in `getObjectPosition()` since `3.1.0` [#372](https://codeberg.org/gaiasky/gaiasky/issues/372).
- camera direction precision issue in focus mode.

### Build System
- update AUR JRE dependency.
- use externally built JDK for appimage [#361](https://codeberg.org/gaiasky/gaiasky/issues/361).
- remove JSAMP, add as dependency.
- remove gson dependency.
- update dependency versions.
- fix CI JDK dependency.

### Documentation
- update JDK requirement in `README.md` from 11 to 15.

### Features
- non-blocking task-based search suggestions.
- allow spaceships of multiple sizes.
- add multiple spaceships to spacecraft mode.


<a name="3.1.3"></a>
## [3.1.3](https://codeberg.org/gaiasky/gaiasky/tree/3.1.2) (2021-06-22).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.2...3.1.3).

### Bug Fixes
- constellation update thread broken [#371](https://codeberg.org/gaiasky/gaiasky/issues/371).
- remove atmosphere softening hack for close by objects.
- focus with no star ancestor [#370](https://codeberg.org/gaiasky/gaiasky/issues/370).

### Code Refactoring
- render types reorganized and improved.

### Features
- adjust spacecraft camera values for better positioning.

### Performance Improvements
- performance improvements in arbitrary precision vector distance method.

<a name="3.1.2"></a>
## [3.1.2](https://codeberg.org/gaiasky/gaiasky/tree/3.1.1) (2021-06-16).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.1...3.1.2).

### Bug Fixes
- broken visibility of datasets (star/particle groups) [#369](https://codeberg.org/gaiasky/gaiasky/issues/369).
- enable more than one light glow effect at a time.
- set logging level of STIL and JSAMP to WARN [#367](https://codeberg.org/gaiasky/gaiasky/issues/367).

### Build System
- fix `git-chglog` configuration so that merge requests are correctly captured.

### Features
- add apparent magnitude from camera [#368](https://codeberg.org/gaiasky/gaiasky/issues/368).

<a name="3.1.1"></a>
## [3.1.1](https://codeberg.org/gaiasky/gaiasky/tree/3.1.0) (2021-06-11).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.1.0...3.1.1).

### Bug Fixes
- crash when window is minimized (Windows) [#366](https://codeberg.org/gaiasky/gaiasky/issues/366).

### Build System
- change developer_name to be consistent with FlatHub metadata (max 60 chars).

<a name="3.1.0"></a>
## [3.1.0](https://codeberg.org/gaiasky/gaiasky/tree/3.0.3) (2021-06-10).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.0.3...3.1.0).

### Features
- add first implementation of new component: location log
- add „“ characters to font files, remove unused fonts
- improve logging in shader loader
- increase font size of dataset name in catalog selection and data manager windows
- multiple directional lights in per-pixel-lighting shader
- do not render `-Z` direction in cube map mode if mode is planetarium and aperture > 180
- add dynamic resolution flag to configuration file - for testing purposes only!
- spacecraft GUI is now a table with a background
- expose back buffer scale factor to configuration
- add new logo to `README.md` file
- separate max nummber of billboards, labels and velocity vectors in configuration
- new MWSC description
- improve error dialog, saner default properties
- bump source and configuration version numbers
- replace `BigDecimal` with faster `Apfloat`
- add arbitrary precision floating point vector
- add RGB color channels to filter attributes, add XOR operation
- condense date/time in control panel into a single line
- add transition control to dataset settings window
- add number of objects and size to datasets pane
- add support for versioning in metadata binary files
- max octree depth set to 19
- add per-object visibility API calls
- add per-object visibility to focus info panel
- add per-object visibility controls
- move visibility property from fade nodes to scene graph nodes

### Merge requests
- Bulgarian translation, contributed by [Georgi Georgiev](https://codeberg.org/RacerBG).

### Bug Fixes
- update list of JRE modules for Appimage
- untranslatable strings, fixes [#356](https://codeberg.org/gaiasky/gaiasky/issues/356).
- music module omitted if initialization fails, fixes [#360](https://codeberg.org/gaiasky/gaiasky/issues/360), [#362](https://codeberg.org/gaiasky/gaiasky/issues/362).
- AppImage not using bundled JRE. Fixes [#361](https://codeberg.org/gaiasky/gaiasky/issues/361).
- README docs URL.
- attitude navigator ball UI scaling.
- free camera stops when very close to stars.
- particle passing parent translation to children instead of its own.
- mini-map crash due to shader version not found on some macOS systems.
- free mode coordinate command gets doubles instead of floats.
- float/double errors and little bugs.
- reformulate `plx/plx_e > crti`.
- pad catalog number in launch script.
- fix metadata binary version 1 with long children ids.
- wee typos and fixes.
- keyframes arrow caps, leftover focus when exiting keyframe mode.
- dataset highlight size factor  limits consolidated across UI and scripting.
- 'make all particles visible' fix in highlighted datasets.
- loading particle datasets crashed sometimes.
- STIL loader fails if stars have no extra attributes.
- octant id determination in creator.
- typo 'camrecorder' -> 'camcorder'.

### Build System
- upgrade to Install4j 9.0.3.
- use Jlink instead of manual method to build packaged JRE (appimage).
- remove VAMDC repository, add JSOUP target version.
- add metadata to Appimage.
- switch to local JSMAP library, as VAMDC repository looks down.
- upgrade libgdx to 1.10.0, bump gs version in build script.
- upgrade build system to gradle 7.0.
- JSAMP maven is down, adding jar to lib.

### Documentation
- clean up javadoc comments.
- add missing acknowledgments and contributors.

### Style
- migrate missing strings to I18n system, move all `I18n.bundle()` to new `I18n.txt()`.
- rename some variables and format some files.
- clean up and refactor render code, organize imports in whole project.
- some shader formatting.

<a name="3.0.3"></a>
## [3.0.3](https://codeberg.org/gaiasky/gaiasky/tree/3.0.2) (2021-02-25).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.0.2...3.0.3).

### Features
- improvements to catalog generation (hashmap to treemap, rename params, accept multiple string ids per column, etc.).
- add search suggestions to search dialog - fixes [#351](https://codeberg.org/gaiasky/gaiasky/issues/351) [#351](https://codeberg.org/gaiasky/gaiasky/issues/351).
- remember 'show hidden' preference in file chooser.

### Bug Fixes
- controller image fetch crash.
- `getDistanceTo()` with star group object, `goToObject()` with no angle.
- `setSimulationTime()` crash.
- move `wikiname` to celestial body, remove unused parameters, prepare star to be loaded directly.
- use proper values for depth test.
- post-process bugs (sorting, etc.).
- check the wrong catalog type 'catalog-lod'.
- use local descriptors when server descriptor fails to recognize a catalog.
- button sizes adapt to content (fixes [#353](https://codeberg.org/gaiasky/gaiasky/issues/353)).
- bug introduced in 40b99a2 - star cores not applied alpha - fixes [#352](https://codeberg.org/gaiasky/gaiasky/issues/352).
- move temp folder into data folder - partially fixes [#350](https://codeberg.org/gaiasky/gaiasky/issues/350).
- local catalog numbers work when no internet connection available.
- update jamepad and gdx-controllers versions due to macOS crash.

### Build System
- exclude appimage files from install media.
- remove branding from installer strings.
- move to gdx-controllers 2.1.0, macos tests pending.
- genearte md5 and sha256 of appimage package.
- add appimage build.
- update docs repository pointer.
- update bundled jre version to 15.0.2.
- complete move to Shenandonah GC.
- use Shenandonah GC instead of G1, minor fixes.
- upgrade to libgdx 1.9.14.

### Performance Improvements
- remove runtime limiting magnitude.

### Style
- cosmetic changes to octree generator.
- renamed some variables, add some extra code comments.
- tweak some parameters in star renderer.


<a name="3.0.2"></a>
## [3.0.2](https://codeberg.org/gaiasky/gaiasky/tree/3.0.1) (2021-01-21).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.0.1...3.0.2).

### Features
- add warning when selecting more than one star catalog.
- add white core to star shaders.
- add T_eff to STIL-loaded catalogs.
- add color conversion by Harre and Heller.
- add output format version argument to octree generator.
- support for  in catalog selector.
- add versioning to binary catalog format. Create new, more compact version.
- improve information of version line in welcome and loading screens.
- add GL info to welcome screen.
- new connection to wikipedia REST api to show content in a window.
- add unsharp mask post-processing filter.
- new checkbox textures, adjust window visuals.
- add projection lines to star groups.
- dataset selection dialog uses same structure as dataset manager.
- time warp slider instead of buttons.
- new fractional UI scaling from x0.7 to x2.0.
- add regexp to some column names for STIL loader, add invalid names array.
- case-insensitive columns in STIL loader, enable FITS loading.

### Bug Fixes
- stuttering updating counts top-down in large octrees, now the counts are updated locally, bottom-up, when octants are loaded/unloaded.
- RAM units in crash report, add indentation.
- default proper motion factor and length values.
- 'App not responding' message on win10 - fix by upgrading to gdx-controllers 2.0.0, plus some other goodies.
- remove useless network checker thread, fix thumbnail URL crash on win10.
- minimizing screen crashes Gaia Sky on Win10. Fixes [#333](https://codeberg.org/gaiasky/gaiasky/issues/333), [#345](https://codeberg.org/gaiasky/gaiasky/issues/345).
- VR init failure actually prompts right error message.
- properties files' encodings set to UTF-8. Fixes [#344](https://codeberg.org/gaiasky/gaiasky/issues/344).
- VR mode now accepts any window resize, backbuffer size used for everything internally.
- BREAKING CHANGE API landOnObjectLocation() -> landAtObjectLocation().
- octreegen additional split accepts now coma and spaces.
- use different sprite batch for VR UI with backbuffer size.
- pan scaled with fov factor.
- red-night theme disabled styles.
- proper 'disabled' textures for buttons.
- labels occlude objects behind, buffer writes disabled.
- download speed moving cancel button in dataset manager.
- safemode flag used correctly, fix raymarching not being setup in safe mode.

### Build System
- auto-update offered through install4j, backup solution in-app still available when not launched using install4j.
- remove sdl2gdx in favor of gdx-controllers:2.0.0.
- exclude old `gdx-controllers` library.
- add --parallelism parameter to.
- fix script so that geodistances file is additional data instead of special argument.
- fix helper script args.
- update release instructions with flatpak, fix build script.

### Code Refactoring
- interface particle record to allow for multiple implementations.
- binary providers are versioned, fix binary version 0/1 loading.
- increase number of maps for octree gen.
- modify default bloom settings (default intensity, passes, amount).

### Documentation
- fix javadocs for binary format (1/n).

### Performance Improvements
- arrays of size not dependent on maxPart for octreegen.
- remove boundingBox from octant, reduce memory token duplication.
- replace extra attributes hashmap with objectdoublemap for RAM compactness.
- do not write star name strings if they are the same as ID, velocity vectors represented with single-precision floats.
- reduce main memory usage of stars by adjusting data types.
- switch to unordered gdx Arrays when possible to minimize copy operations.
- replace `java.util.ArrayList`s with libgdx's `Array`s to minimize allocations.
- index lists are of base types, use dst2 for distance sorting.
- improve memory usage of extra star attributes and fix render system unnecessary `setUniform` calls.
- reduce memory usage in particle groups -> no metadata array.

### Style
- fix missing coma in night-red theme JSON file.
- update thread names, fix monitor objects, increase sg update time interval.

<a name="3.0.1"></a>
## [3.0.1](https://codeberg.org/gaiasky/gaiasky/tree/3.0.0) (2020-12-10).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/3.0.0...3.0.1).

### Features
- saner error reporting with new dialog.
- add error dialog that works with OpenGL 2.0 and informs the user of insufficient OpenGL or Java versions.
- add safe graphics mode CLI argument `--safemode`
- dynamic resolution scaling - first implementation, deactivated.
- add safe graphics mode, which does not use float buffers at all. It is activated by default if the context creation for 4.1 fails. It uses OpenGL 3.1.
- download manager is capable of resuming downloads.
- special flag to enable OpenGL debug output.
- enable GPU debug info with `--debug` flag.

### Bug Fixes
- show information dialog in case of OpenGL or java version problems.
- disposing bookmarks manager without it being initialized.
- update default screen size.
- remove idle FPS and backbuffer configuration
- file chooser allows selection when entering directories if in 'DIRECTORIES' mode.
- update default max number of stars.
- increase max heap space from 4 to 8 GB in all configurations.
- 24-bit depth buffer, 8-bit stencil.
- JSON pointer from DR2 to eDR3.

### Build System
- update bundled JRE version to 11.0.9+11.

### Code Refactoring
- all startup messages to I18N bundle, fix swing themes.

### Documentation
- update pointers to documentation.

<a name="3.0.0"></a>
## [3.0.0](https://codeberg.org/gaiasky/gaiasky/tree/2.3.1) (2020-12-02).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.3.1...3.0.0).

### Features
- add number of objects to download manager.
- velocity scaling allows approaching stars slowly.
- API call to set the maximum allowed time.
- add arrow caps to polylines.
- add progress bar to dataset loading, touch up some styles.
- download helper accepts local files, reorganize catalogs.
- new API call to get parameters from stars given its name or id.
- add brightness power and reload defaults to visual settings.
- improve loading tips subsystem with custom styles and arbitrary elements.
- 3D fonts can be limited in solid angle size.
- UI adjustments and tweaks.
- new welcome screen reorganizes dataset management.
- add complimentary color to inner recursive grid.
- add projection lines on reference system plane, with distances.
- first final version of recursive grid.
- new recursive grid object.
- catalog selection displayed when more than one Gaia catalog is selected.
- add wavefront converter, update gradle version.
- fix color picker.
- camera speed-from-distance function rescaling.
- first version of gamepad keyboard.
- update eDR3 catalog descriptors.
- controller UI to modify some properties using a gamepad.
- add `--debug` flag for more info.
- restructure loading GUI layout.
- improve `--version` information.
- add ASCII Gaia image to text ouptut
- update data descriptor with new MW model.

### Bug Fixes
- adjust default area line width.
- star clusters visual appearance.
- min star size scaled by resolution.
- apply scale factor to milky way.
- camera group bottom buttons aligned to center.
- emulate 64-bit float with two 32-bit floats in shader to be able to extend time beyond +-5 Myr.
- controller mappings not found on first startup. Fixes [#341](https://codeberg.org/gaiasky/gaiasky/issues/341).
- use Java2D instead of Toolkit to determine initial screen size.
- data description update.
- controller mappings looking for assets location if not found.
- manpage gen.
- smooth game camera view.
- spacecraft mode fixes.
- GUI registry check.
- add timeout to sync behavior in dataset loading.
- new default startup window size to accommodate welcome screen.
- update default data desc pointers to version 3.0.0.
- default fps limit value, aux vectors in recursive grid.
- overwrite coordinate system matrix by recursive grid.
- start some units over `XZ` plane to avoid conflicting with recursive grid.
- gaiasky script defaults back to system java installation if nothing else is found.
- octreegen empty hip x-match crash.
- points in VertsObject with wrong uniform name - incorrect location.
- do not round dialog position values.
- blue, orange and red themes crashed.
- controls scroll box resizing.
- download data window sizings, update data desc.
- regular color picker does not show dialog.
- music player actually finds audio files.
- size of keyboard shortcuts table in controls pane.
- disable background models' depth test.
- focused widgets in scroll panes capture all keyboard events.
- actually send errors to `stderr` instead of `stdout`.
- fix VR properties data pointer.
- motion blur bug producing wrong results for models.
- `touchUp` event on Link and LinkButton objects not working.
- improve logging messages in case of index name conflicts.
- update URL pointers after ARI CMS update.
- graphics quality in log messages.

### Build System
- modify installer unpacking message.
- ignore release candidates in changelog, update some defaults.
- generate `sha256` in catalog-pack script.
- macOS does not query screen size due to exception.
- check OS when trying to use Linux commands.
- remove music files from release, don't use OS-dependent system for controller mappings.
- upgrade to libgdx `1.9.12`.
- update STIL library jar.
- update version and data pointer.

### Code Refactoring
- run code inspections, cleanup. Improve particle effects.
- `begin()` and `end()` substituted with `bind()`.
- remove unused or derived uniform definitions.
- use `java.utils` collections whenever possible, libgdx buggy since `1.9.11`
- complete font update to more modern, spacey choices.
- all regular UI fonts from Tahoma to Roboto regular.
- use `system.out` with UTF-8 encoding, improve gen scripts.
- remove ape, Gaia scan properties.
- move RenderGroup to render package for consistency.

<a name="2.3.1"></a>
## [2.3.1](https://codeberg.org/gaiasky/gaiasky/tree/2.3.0) (2020-07-08).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.3.0...2.3.1).

### Bug Fixes
- shader lint function.
- additional check for http->https redirects.

### Code Refactoring
- update some URLs from http to https.

### Features
- hot reload of galaxy models.
   .
<a name="2.3.0"></a>
## [2.3.0](https://codeberg.org/gaiasky/gaiasky/tree/2.2.6) (2020-07-07).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.6...2.3.0).

### Bug Fixes
- update name and source version number.
- error in lib_math shadier code.
- remove default fade-out values in star groups, added to loading dialog.
- interpolation limits in math shader library.
- initial update not performed on fade node children if ct is off.
- uncomment unhandled event debug info.
- windows crash due to stars '\*' not being accepted in paths.
- add notice concerning the selection of more than one Gaia catalog.
- changing focus to different object in same particle group works.
- default value for magnitude scale is 0, fix float validator range.
- disable depth test for billboards.
- inconsistencies with STAR_MIN_OPACITY_CMD.
- ensure non-empty field in search dialog.

### Build System
- fix build with text folder.
- remove all absolute paths to project folder.

### Code Refactoring
- observer fields final, package name typo.
- clean up gaia hacks, ray marching plubming.
- post-processing subsystem made more generic.
- move render system to java collections and streams.

### Documentation
- improve readme listings.
- update acknowledgements.
- add iconic license.
- add package-info package documentation, update changelog.

### Features
- update server to HTTPS.
- add call to set 'all visible' dataset property.
- add 'invert X look axis' as well as Y.
- axis power value and sensitivity in config window.
- sliders now contain value label.
- sensitivity sliders for game controllers.
- add tips to loading screen.
- post-processor to accept external shader code in the data folder.
- ray marching shaders.
- raymarching post-processing shaders.
- complete move to SDL-back controllers.
- full refactoring of controller mappings system.
- interactive gamepad configuration.
- add support for emissive textures, fix obj loading issue.
- API call to modify solid angle threshold of orbits.
- add properties for some star settings.
- adjust size of star billboards.
- add API call to scale orbits. Use with caution!.
- distances in AU and parsec start at 0.1 mark.
- add star brightness power setter to API.


<a name="2.2.6"></a>
## [2.2.6](https://codeberg.org/gaiasky/gaiasky/tree/2.2.5) (2020-05-15).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.5...2.2.6).

### Bug Fixes
- camera turn depends on fov.
- stars with negative parallaxes use default [#329](https://codeberg.org/gaiasky/gaiasky/issues/329).
- load VO table crash on Windows [#329](https://codeberg.org/gaiasky/gaiasky/issues/329).
- program crash when minimizing in windows [#333](https://codeberg.org/gaiasky/gaiasky/issues/333).
- do not assume default location for hip, pass as agrument.
- file count value when max number of files is specified.
- safecheck to prevent window sizes of 0x0 on resize events with AMD graphics on windows.
- transition from point to billboard in star shading.
- remove rounding in generic dialog positioning for smooth rendering.
- adjust brightness scalings, remove unused variables and parameters.
- wrong synchronize location in streaming octree loader [#332](https://codeberg.org/gaiasky/gaiasky/issues/332).
- camera mode change in SAMP select row call.
- goToObject() skip fix.
- SAMP local icon to work with dev version and releases.
- get object positions by name in particle groups.
- fix UI layout of date dialog.
- star cluster loader to use mas/yr instead of deg/yr as proper motion units.
- several fixes (UI, STIL), see desc.
- load multiple catalogs with same name.
- slider step and control buttons size.
- double stars caused by incorrect shading.

### Build System
- fix publish-javadoc script.
- update build script to latest gradle version.
- more robust way to get size and nobjects from generated catalogs.
- improve catalog generation scripts for faster deployment.
- add/update scripts to build catalogs.
- fix build files.
- add catalogpack script.
- update build and installer scripts to install4j8.
- update data descriptor with new base and hi-res texture packs.
- add bookmarks and VR.md to build, update modes to gradle 6.x.
- update to gradle 6.2.2, prepare build files for gradle 7.
- pkgbuild epoch set to 2 by default.

### Code Refactoring
- use java collections instead of libgdx's, implement parallel loading in octree gen.
- update DR2 loader to generic CSV loader. Add compatibility mode to binary data format for tycho ids (tgas/DR2).
- ColourUtils -> ColorUtils.
- use local application icon for SAMP.
- move default location of mappings file to config folder.
- fix spacing in focus info interface names.
- update data descriptor for new star clusters load mechanism.
- star clusters to use the catalog infrastructure.
- move all file operations to nio (Path).

### Documentation
- update Gaia Sky VR info in repo.
- improve run from source for Windows in readme file.
- update VR docs and readme file to include new VR build.
- fix setCameraSpeedLimit() API docs.
- fix typos in comments for star/particle groups.

### Features
- better random text generator.
- fov-based visibility, autoremove popups.
- adjust size and intensity of stars in milky way model.
- add ref epoch to catalog descriptors and loaders.
- magnitude and color corrections (reddening, extinction) are now applied by default if ag and ebp_min_rp are available. Flag is now needed to explicitly deactivate them.
- redefine eDR3 catalogs.
- add procedural star shader, muted for now.
- new star shading method.
- replace Fibonacci numbers for made-up phrases.
- update distance font to include more characters.
- add crash window with tips and instructions on how to fix/report the problem.
- add shortucts for 'show log' and 'open catalog'.
- make all limit/target frame rates floating-point numbers.
- comments in camera path files: prepend '#' to comment.
- limit framerate to target framerate in camrecorder.
- API call to record camera path with given filename.
- use votable units for star clusters if available.
- load star clusters with STIL so that it also works via SAMP.
- set fov step to 0.1 to have smoother fov changes.
- grid annotations contain degree symbol and sign (latitude only).
- select first object in newly loaded catalogs.
- add icons to bookmarks tree.
- additional `cameraTransition()` that accepts camera position in Km.
- add folders to bookmarks.
- add bookmarks module.
- move individual visibility to own dialog.
- several UI fixes and QOL improvements.
- add label colors to star cluster datasets, update docs ref.
- multiple name support for star cluster loader.
- add description to star clusters dataset loader.
- star clusters can now be loaded with the rest of the catalog info infrastructure.
- velocity vectors sliders to use new slider plus.
- cap length of long ids in focus info interface, add tooltips, fix skins.
- show criteria for catalog chooser.
- add sensitivity and power function to controller properties.
- adjust focus info style to make it more compact.
- add exit confirmation setting and checkbox in preferences window and exit dialog.
- add pointer guides.
- adjust star brightness parameters.
- improve VOTable loader with default units and more safe checks.
- clean up HiDPI themes, slightly reduce icon sizes and spacings.
- add URL bar to file chooser.
- add limits to particle sizes.
- improve file chooser dialog.
- particles get right name in focus info interface.
- particle datasets may have per-particle names.

### Reverts
- fix: remove rounding in generic dialog positioning for smooth rendering


<a name="2.2.5"></a>
## [2.2.5](https://codeberg.org/gaiasky/gaiasky/tree/2.2.4) (2020-03-04).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.4...2.2.5).

### Bug Fixes
- some API calls crash when using `double[]`.
- prevent orbit overlapping by rescaling period.
- macOS system detection.
- land at location crash due to `trim()` applied to invisible name.
- add flush frames to `postRunnable()` actions in scripting implementation.
- proper extension checking for ATI vram info.
- proper fix for VMemInfo crash on arcolinux+ATI graphics.
- VRAM profiling crash for AMDGPUs [#326](https://codeberg.org/gaiasky/gaiasky/issues/326).
- adapt star brightness in cubemap modes [#318](https://codeberg.org/gaiasky/gaiasky/issues/318).
- reload default configuration file crash.
- build script typo.
- ambient light slider.

### Build System
- fix versions of sdl2gdx and jsamp, refactor VMemInfo.
- update compress, jcommander and jsamp versions, replace gdx-controllers with sdl2glx for better compatibility.
- update stil library jar.
- get jsamp from repository.
- substitute underscore by hyphen in pkgver.

### Code Refactoring
- add color array to all API calls that need a color, for consistency. Fixi some calls' documentation.
- cubemap-related properties organised and cleaned-up.
- improve error handling of OpenGL 4.x incapable video cards.

### Documentation
- clean up punctuation in API docs.
- improve API description of some calls.

### Features
- dataset options when loaded through SAMP.
- improve UI elements.
- proper implementation of FXAA.
- load datasets as particles or stars.
- add script to test color map highlighting.
- implement planetarium deviation angle in shader [#328](https://codeberg.org/gaiasky/gaiasky/issues/328).
- update controller list live in preferences window, fix connection/disconnection events.
- add extra attributes, colormaps for highlighting.
- load all attributes from VOTables.
- STIL provider works with multiple names.
- support for multiple star names in octree gen.
- add support for multiple names per object.

### BREAKING CHANGE

API call setStarSize() now gets the star point size in
pixels instead of a normalized value between 0 and 100.


<a name="2.2.4"></a>
## [2.2.4](https://codeberg.org/gaiasky/gaiasky/tree/2.2.3) (2020-01-22).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.3...2.2.4).

### Bug Fixes
- adjust brightness of sun glow, add empty release instructions file.
- macos shader and sprite batch crashes.
- libgdx scene2d ui Window snaps to integer pixel positions resulting in text rendering artifacts.
- assets location when running from source and no properties found.
- dataset highlighting API calls improved, add test scripts for a few use cases.
- screenshot/frame advanced mode messes viewport. Fixes [#319](https://codeberg.org/gaiasky/gaiasky/issues/319).
- properties file version not found. Fixes [#317](https://codeberg.org/gaiasky/gaiasky/issues/317).
- fullscreen mode before initialization, cursor in slaves.
- stop the rest server the right way.
- correct perspective of labels in cubemap modes.
- master-slave connection messed up by scripting engine.
- fisheye setting persisted.
- do not replace backslaches with forward slashes.
- use fixed keyword for scene graph loader, improve internal catalog path handling.
- typo - geenden -> beenden.
- do not apply fog to normal shaders [#312](https://codeberg.org/gaiasky/gaiasky/issues/312).
- default constructor for NBG.
- milky way adapts to fov changes.
- toggle buttons for dome, cubemap and stereo.
- notifications interface background in stereo mode.
- additive gpu VBOs.

### Build System
- fix install4j crash, sort out tar.gz md5, update changelog.
- update changelog, changelog template and scripts.
- allow more than one instance with the .exe file.
- update to lwjgl 3.2.3, deprecated annotations.
- update gradle version to 6.0.1.
- to openJDK 11.
- update CI java image to 11.
- code analyzer, gradle update, build file runners.
- update checks to java 11.
- disable motion blur by default.

### Code Refactoring
- reorganize things for multiple windows.

### Documentation
- update reference.
- update docs ref and minor changes.
- add open iconic to acknowledgements.

### Features
- replace logo images by TTF text.
- add cyrillic characters for Russian translation.
- add line width factor to conf and UI controls.
- edit timedate button is text icon button.
- new compact sliders.
- finish blend map implementation for multiple-projector blend support.
- add slave configuration and status window (S+L+V) to master instances.
- configure slave instances live.
- proper image warping for MPCDI support.
- geometry warp and blend shader, improve reverse mapping.
- configure slave instance using Gaia Sky configuration file.
- add MPCDI parsing and orientation.
- dataset highlight size factor API call.
- active planetarium mode uses cubemap method.
- add fisheye projection to cubemap mode.
- remember last tab in preferences window.
- update old preferences window icons.
- replaced external UI window with external scene view.
- half-functioning separate UI controls window.
- add experimental separate UI window (not working yet).
- minimap size controls and tooltips.
- add `ctrl`+`plus`/`minus` to increase/decrease the FOV.
- add VR icon.
- maintain a 1:1 aspect ratio for the fisheye/planetarium effect.
- adjust mw parameters.
- add support for per-object primitive in GPU arrays, improved earth-venus-dance script.
- new API call to convert equatorial cartesian to internal cartesian with unit conversion factor.

### Performance Improvements
- improve performance of api call method/parameter matching.

<a name="2.2.3"></a>
## [2.2.3](https://codeberg.org/gaiasky/gaiasky/tree/2.2.2) (2019-11-05).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.2...2.2.3).

### Bug Fixes
- last-minute fix.
- improve user notification if wrong java version is used.
- windows data loading crash -> [#308](https://codeberg.org/gaiasky/gaiasky/issues/308).

### Build System
- add some extra translations for Catalan, German and Spanish.

### Features
- update gaia sky icon with more modern version.
- add more handy information in download manager.
- add cancel download button to manager.
- add support for release notes in download manager.

<a name="2.2.2"></a>
## [2.2.2](https://codeberg.org/gaiasky/gaiasky/tree/2.2.1) (2019-10-31).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.1...2.2.2).

### Bug Fixes

- crash loading scene graph on windows [#306](https://codeberg.org/gaiasky/gaiasky/issues/306).
- add default controller rendermodel in case no suitable model is found.
- controller identifier in SteamVR - controllers work again when using SteamVR.
- block motion blur if VR mode is on.
- STIL catalogs not scaling well with global scale factor.
- catalogs loading twice.
- catalog info creation from JSON.
- wrong frame size when UI elements are on in VR.
- graphics quality images not found looking to lower qualities - not it also looks for the image in higher qualities.
- star group label scale and size.
- live update of number of glow lights.
- scripting crash when running several successive scripts.
- `eq`/`ec`/`galtoInternalCartesian()` calls unit fix.
- scripts using 'Sol' instead of 'Sun'.
- adjust star brightness map to magnitudes.
- enable input after script is finished, log connection details.
- layout of datasets pane.
- billboard positioning.
- orientation lock for quaternion-based objects.
- dataset color cycling.
- closest body being null in first frame [#303](https://codeberg.org/gaiasky/gaiasky/issues/303).
- crash resizing window when loading scene graph.
- add screen size check before persist.
- java version string without minor or revision [#302](https://codeberg.org/gaiasky/gaiasky/issues/302).

### Build System

- improve crash reporting by also outputting the log.
- update source version number to 020202.
- update to gdx 1.9.10, gradle 5.6.2.

### Code Refactoring

- motion blur shaders to work like the rest.
- complete package renaming.
- package rename, first commit.
- relocate some functions to more suitable spots.

### Documentation

- info on VR controls and whatnot.
- clarify OpenComposite vs SteamVR for running with Oculus headsets.

### Features

- minimaps finished with local group (1 and 2) and High-z.
- container background to notifications interface.
- better milky way in high and ultra quality.
- use texture_array for milky way components.
- scaling milky way particles.
- add dataset visibility toggle to context menu.
- add minimap scales for inner/outer solar system, heliosphere, Oort cloud.
- add axes objects and show map button.
- improve context menu, add highlight and quit actions.
- add twitter info and fix help layout.
- add paths to help dialog (config, data, screenshots, frames, music, mappings).
- add ecliptic and galactic longitudes and latitudes to filter attributes.
- add collapse/expand button to debug interface.
- user-defined per-dataset filters.
- add epicycles script plus some handy API calls.
- add brightness power to config file.
- add particle groups as catalog infos, start filters.
- add CPU detection to system information.
- add `setCenterFocus()` API call to disable focus centering.
- add API calls to get unit conversion factor.
- color picker to highlight datasets.
- colormap stars according to arbitrary attributes (first draft).
- add new default colors.
- update post-processing effects in real time when changing graphics quality.
- new velocity-based camera blur.

<a name="2.2.1"></a>
## [2.2.1](https://codeberg.org/gaiasky/gaiasky/tree/2.2.0) (2019-09-10).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.2.0...2.2.1).

### Bug Fixes

- spacecraft mode broken.
- moon coordinates typo causing high-frequency sinusoidal drift.
- greedy texture initialization works again.
- init VR models after VR context creation.
- VR version can't download the data before connecting to the HMD.
- parameter name in build script.
- default sprite batch causes core profile error.
- particle group length() with very distant positions.
- windows program group for VR.
- most problems with the VR version fixed by scaling the background models correctly.
- wee missing bits in z-buffer shaders.
- wee fixes imported from the VR branch.
- roll back to GL 3.2 if 4.x not supported.
- depth computation done per fragment.
- scripts Sol -> Sun.
- controller mappings format error in loading.
- cmd windows launch script actually works.
- report scene graph loading errors ([#293](https://codeberg.org/gaiasky/gaiasky/issues/293)).
- deb dependency, issue [#291](https://codeberg.org/gaiasky/gaiasky/issues/291).

### Build System

- remove unused deps, update version number.
- add VR launcher.
- info on the new VR stuff.

### Code Refactoring

- cleanup glsl log z-buffer library.
- improve shader performance and readability (from vr).

### Documentation

- fix VR flag in readme.
- some more on the VR version.
- update VR info.
- update docs reference.
- requirements table in readme.
- update readme reqs and supported hw.

### Features

- update logos and x2 UI scaling factor.
- add lazy texture and mesh initialisation to config file.
- add checkboxes for all crosshairs/markers.
- change crosshair appearance so that they stack well.
- add focus, closest and home objects to top bar.
- better particle group renderer with scaling particles and color-distance mapping.
- add closest object to top info bar.
- add top UI element with current time.
- unify VR with desktop version.
- migrate completely to adaptive-scale logarithmic z-buffer.


<a name="2.2.0"></a>
## [2.2.0](https://codeberg.org/gaiasky/gaiasky/tree/2.1.7-vr) (2019-08-01).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.7-vr...2.2.0).

### Bug Fixes

- no-GUI mode inhibits GUI-related mappings.
- memory info window layout.
- VRAM leak disposing textures when changing quality.
- truly release VRAM when unloading textures.
- scripting parameter check.
- `cameraTransition()` issue.
- more glsl leftovers.
- shader errors on macOS [#288](https://codeberg.org/gaiasky/gaiasky/issues/288) [#288](https://codeberg.org/gaiasky/gaiasky/issues/288).
- show notice with high/ultra quality.
- tangent and binormal vectors on UV spheres.
- UI inconsistencies.
- line occlusion fixed with no depth writes.
- adjust sun size and selection.
- aspect ratio of most points.
- synchronous catalog loading via script is really synchronous.
- fade node visibility tied to internal frame rate, not absolute time.
- fix dataset visibility fade time link, add cubemap projection setter in scripting API.
- frame buffer and effects cleanup on resize.
- adjust motion blur, remove blur radius.
- about window layout.
- add pad to version check buttons.
- add some value checks to scripting implementation.
- file chooser file/dir browsing state.
- bugs determining location of files.
- macOS gradle launch script.
- dataset version check in download manager.
- macOS retina display scaling, remove analytics.
- macOS script fix.

### Build System

- Improved readme file instructions.
- Requirements from JRE8 to JRE11.
- move postprocessing lib to gaia sky.
- gitlab issue templates.

### Code Refactoring

- texture component is now material component.
- remove unused webgl code.
- sphere creator to own class.
- render system cleanup.
- sprite batch shaders to version 330, moved postprocess shaders to own folder.
- sanity checks and code cleanup in scripting API implementation.
- reorganised scripts.
- cleanup scripts folder.

### Features

- add padding to tooltips by default.
- add reflections in shaders plus skybox.
- add startup object to config.
- add VRAM monitoring.
- add reset sequence number button.
- improve debug pane layout.
- initialise elevation data structures asynchronously.
- CPU generation of height data.
- add tessellation quality control.
- noise-based height.
- new scripting calls: cameraYaw/Pitch.
- handle server down event correctly.
- data downloader checks for updates.
- decouple keyboard bindings from code, i18n camera modes.
- warnings in object search.
- new checksum algorithm: MD5 -> SHA256.
- orbit refresh daemon plus shading.
- comprehensive info panel on mode switch, star textures.
- fix point scaling.
- add starburst to lens flare.
- add load queue progress to debug.
- separate HiDPI theme to checkbox in preferences.
- add point size and color attributes to asteroids.
- add dithering glsl library to simulate transparency with opaque objects.
- some work on controller mappings.
- add optional gravity to game mode.
- new camera mode: Game mode.
- walk on the surface of any height-mapped body.
- add game mode - WASD+mouse.
- add physically based fog to atmospheres.
- add Uncharted and Filmic tone mapping types.
- add color noise parameter to particle groups.
- parallax mapping.
- improve light glow performance and visual quality.
- add ACES tone mapping type.
- improve atmosphere blending with stars.
- add plumbing to allow automatic and exposure HDR tone mapping types.
- automatic tone mapping based on Reinhard's method.
- move all post-processing shaders to version 330.
- new milky way model.
- migrate search window to generic dialog.
- native support for gzipped obj models (.obj.gz).
- implement integer indices.
- use gitlab API instead of github's.
- implement sane crash reporting to file.
- migrate to Java 11.
- add 'y' and 'n' key bindings to dialogs.
- add shortcuts to expand/collapse panes.
- velocity vectors are regular component types.
- add optional arrowheads to velocity vectors.

<a name="2.1.7"></a>
## [2.1.7](https://codeberg.org/gaiasky/gaiasky/tree/2.1.6) (2019-01-11).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.6...2.1.7).

### Bug Fixes

- restored download code.
- wait for tasks to finish before shutting down.
- about window layout in non-hidpi mode.
- 'data files not found' problem.

### Build System

- remove run command echo, rearrange version logging.
- fix installer-img not found.

### Code Refactoring

- topmost render method rewritten to avoid conditionals.

### Documentation

- fix build system title case.
- update changelog.

### Features

- data download dialog details.
- improve music component with scrollable volume, track name and time position.
- add RUWE to octree generator.

<a name="2.1.6"></a>
## [2.1.6](https://codeberg.org/gaiasky/gaiasky/tree/2.1.5) (2018-12-18).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.5...2.1.6).

### Bug Fixes

- update static light in models with no texture (meshes).
- leftover code.
- remove buggy separators between some controls windows widgets.
- add 4 extra directions to FXAA, effect now much nicer on stars.
- width of tabs in prefs window lo-dpi mode.
- integer snapping in downl. mgr + part. effect.
- add cubemap edge fix to particle group.

### Build System

- update server datasets descriptor.
- minor issues.
- environment variable to skip java version check.
- minor fixes.
- script to convert usual RA[HH:MM:SS] and DEC[deg:arcmin:arcsec] to degrees.
- minify JSON descriptor files before pushing.
- update data descriptor with new nbg catalog.

### Code Refactoring

- variable name change: font3d -> fontDistanceField.
- removed data and assets-bak folders from repository.
- moved text utils methods and classes around.

### Documentation

- remove confusing line.
- environment variable to skip java version check.
- update readme with some extra info on download manager.
- extra documentation line in FXAA code.

### Features

- catalog chooser widget rewritten to make it easier to understand.
- improve disabled check box representation.
- add log to STIL provider and more.
- add support for links (references) in download manager.
- performance improvements in octree, reimplement octant frustum culling.
- slash key bound to search dialog.
- add notice in catalog chooser.
- star size affects particle groups.
- update criteria to show catalog chooser.

### Style

- nbg loader to manage distances better.

<a name="2.1.5"></a>
## [2.1.5](https://codeberg.org/gaiasky/gaiasky/tree/2.1.4) (2018-12-03).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.4...2.1.5).

### Bug Fixes

- null pointer when unloading stars [#322](https://codeberg.org/gaiasky/gaiasky/issues/322).

### Build System

- remove rpm deps as they depend on distro.
- update build scripts to install4j 7.0.8.
- update to libgdx 1.9.9.
- update data with new dr2-verylarge catalog.

### Code Refactoring

- regular textures to tex/base.
- cleaned up logger situation.

### Documentation

- update changelog.
- update rpm install command [#317](https://codeberg.org/gaiasky/gaiasky/issues/317).

### Features

- LMC, SMC, datasets can require min gs version.
- add support for nebulae.
- non-JSONLoader autoload files.
- billboard galaxies.
- passive update notifier [#321](https://codeberg.org/gaiasky/gaiasky/issues/321).
- add download speed and progress in downloaded/total to download manager.
- add progress MB data to downloader.

### Style

- fix info message.

<a name="2.1.4"></a>
## [2.1.4](https://codeberg.org/gaiasky/gaiasky/tree/2.1.3) (2018-11-23).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.3...2.1.4).

### Bug Fixes

- locale index overflow.
- German translation and locale initialisation [#320](https://codeberg.org/gaiasky/gaiasky/issues/320).
- do not preselect default dataset, only base data.
- sizing of download manager window.
- data download URL log message.
- null pointer when updating scroll focus, slash at end.
- multiple scroll focus objects [#319](https://codeberg.org/gaiasky/gaiasky/issues/319).
- octree generator.
- emission shader code.

### Build System

- add xorg-xrandr as dependency in AUR package.

### Features

- improve usability of download manager.
- ensure correct java version before building.
- dataset versioning [#318](https://codeberg.org/gaiasky/gaiasky/issues/318) [#316](https://codeberg.org/gaiasky/gaiasky/issues/316).
- STIL provider adds HIP indices.
- name support and more for STIL loader.
- script to query HIP names in simbad.
- add optional output folder to CSV process.
- script to process DR2 CSV files.

### Style

- wee reformatting.

<a name="2.1.3"></a>
## [2.1.3](https://codeberg.org/gaiasky/gaiasky/tree/2.1.2-vr) (2018-10-31).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.2-vr...2.1.3).

### Bug Fixes

- octree rendering muted.
- minimap window.
- accents and umlauts in user folder path (win) [#314](https://codeberg.org/gaiasky/gaiasky/issues/314).
- start button status update [#313](https://codeberg.org/gaiasky/gaiasky/issues/313).

### Code Refactoring

- startup log.
- shader include directive changed.

### Documentation

- remove old references to `gaiasandbox`.

### Features

- new shader init & various improvements.
- add proper motions to stil data provider.
- initial support for proper motions over SAMP.
- individual constellation selectors [#275](https://codeberg.org/gaiasky/gaiasky/issues/275).

### Style

- GaiaSky.java to use LF instead of CRLF.
- remove leftover variables in full gui.

<a name="2.1.2-vr"></a>
## [2.1.2-vr](https://codeberg.org/gaiasky/gaiasky/tree/2.1.2) (2018-09-28).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.2...2.1.2-vr).

### Bug Fixes

- remove version from window title.
- assets location in install4j template.
- heap sizes in build script.
- macOS -XstartOnFirstThread flag.
- macos builds tweaks.
- global key bindings affect invisible GUIs [#311](https://codeberg.org/gaiasky/gaiasky/issues/311).
- fix `p` double-mapping [#310](https://codeberg.org/gaiasky/gaiasky/issues/310).

### Build System

- installer detects and removes previous versions.
- new gradle 5 compile dep format.
- update to lwjgl 3.2.3.
- missing flag in rund, fix caps in ruler.
- add javadoc generator and publisher.

### Code Refactoring

- bin to scripts, now settled.
- scripts moved to bin, bin in git.

### Documentation

- small tweak to changelog template.
- improve git-chglog configuration.
- update changelog.

### Features

- update to lwjgl3 backend.
- cosmic ruler [#296](https://codeberg.org/gaiasky/gaiasky/issues/296).
- API calls to disable and enable the GUI [#312](https://codeberg.org/gaiasky/gaiasky/issues/312).

### Style

- fix issues with merge to bring it back to a working state.
- add ruler component type.
- general code cleanup.
- minor style issues.

<a name="2.1.2"></a>
## [2.1.2](https://codeberg.org/gaiasky/gaiasky/tree/2.1.1) (2018-09-18).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.1...2.1.2).

### Bug Fixes

- fix for windows paths [#309](https://codeberg.org/gaiasky/gaiasky/issues/309).
- fix run script and play camera windows.
- update changelog.

### Features

- add quit confirmation dialog.
- add new key bindings for simple actions.

<a name="2.1.1"></a>
## [2.1.1](https://codeberg.org/gaiasky/gaiasky/tree/2.1.0) (2018-09-14).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.1.0...2.1.1).

### Bug Fixes

- crash if no internet connection present [#308](https://codeberg.org/gaiasky/gaiasky/issues/308).
- fix description of very large catalog.
### Documentation

- update changelog.
- mended submodule init and update.

<a name="2.1.0"></a>
## [2.1.0](https://codeberg.org/gaiasky/gaiasky/tree/2.0.3) (2018-09-11).
[Full changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.0.3...2.1.0).

### Bug Fixes

- fix previous fix of search dialog [#307](https://codeberg.org/gaiasky/gaiasky/issues/307).
- search dialog crash if starts with number [#307](https://codeberg.org/gaiasky/gaiasky/issues/307).
- fix error loading lens dirt hi res texture.

### Build System

- new changelog generator in release script.
- add changelog generator script.

### Documentation

- updated changelog.
- add gaiasky-docs submodule.
- add commit message style guidelines.
- fix download helper docs.

### Features

- add download manager and infrastructure [#291](https://codeberg.org/gaiasky/gaiasky/issues/291) [#303](https://codeberg.org/gaiasky/gaiasky/issues/303).

### Style

- fix style of contributing once and for all.
- fix style in CONTRIBUTING.md.

## [2.0.3](https://codeberg.org/gaiasky/gaiasky/tree/2.0.3) (2018-08-28).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.0.2-vr...2.0.3).

**Implemented enhancements:**

- clean up logging code [#299](https://codeberg.org/gaiasky/gaiasky/issues/299).
- improve debug info [#298](https://codeberg.org/gaiasky/gaiasky/issues/298).
- handle vertex data more efficiently [#297](https://codeberg.org/gaiasky/gaiasky/issues/297).
- API: Provide a way to hook into main loop thread [#294](https://codeberg.org/gaiasky/gaiasky/issues/294).
- add support for different line widths [#293](https://codeberg.org/gaiasky/gaiasky/issues/293).
- API call: lines between arbitrary positions [#292](https://codeberg.org/gaiasky/gaiasky/issues/292).
- add Top/Bottom to the mode profiles for 3DTV [#268](https://codeberg.org/gaiasky/gaiasky/issues/268).

**Merged pull requests:**

- REST server static files use assets.location [#300](https://codeberg.org/gaiasky/gaiasky/pull/300) ([vga101](https://github.com/vga101)).
- re-introduce REST API [#281](https://codeberg.org/gaiasky/gaiasky/pull/281) ([vga101](https://github.com/vga101)).

## [2.0.2-vr](https://codeberg.org/gaiasky/gaiasky/tree/2.0.2-vr) (2018-07-25).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.0.2...2.0.2-vr).

## [2.0.2](https://codeberg.org/gaiasky/gaiasky/tree/2.0.2) (2018-07-06).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.0.1...2.0.2).

**Implemented enhancements:**

- add controls to manage datasets [#290](https://codeberg.org/gaiasky/gaiasky/issues/290).
- separate base texture from clouds texture [#289](https://codeberg.org/gaiasky/gaiasky/issues/289).
- add gamma correction [#288](https://codeberg.org/gaiasky/gaiasky/issues/288).
- add label size control [#287](https://codeberg.org/gaiasky/gaiasky/issues/287).
- rearrange graphical settings into preferences dialog [#286](https://codeberg.org/gaiasky/gaiasky/issues/286).

**Fixed bugs:**

- fix objects pane minimize button disappearing [#285](https://codeberg.org/gaiasky/gaiasky/issues/285).

**Merged pull requests:**

- fix broken link to DR2 default catalog [#280](https://codeberg.org/gaiasky/gaiasky/pull/280) ([vga101](https://github.com/vga101)).

## [2.0.1](https://codeberg.org/gaiasky/gaiasky/tree/2.0.1) (2018-06-14).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.0.0-vr...2.0.1).

**Implemented enhancements:**

- lazy mesh initialization [#284](https://codeberg.org/gaiasky/gaiasky/issues/284).
- improve DR2 catalogs [#283](https://codeberg.org/gaiasky/gaiasky/issues/283).
- add support for new galaxy meshes [#282](https://codeberg.org/gaiasky/gaiasky/issues/282).
- fix Gaia Sky logo resolution [#279](https://codeberg.org/gaiasky/gaiasky/issues/279).
- add utility to see logs [#278](https://codeberg.org/gaiasky/gaiasky/issues/278).
- improve grid rendering [#277](https://codeberg.org/gaiasky/gaiasky/issues/277).
- add maximum FPS option [#273](https://codeberg.org/gaiasky/gaiasky/issues/273).
- create contributing.md files with guidelines as to how to contribute [#272](https://codeberg.org/gaiasky/gaiasky/issues/272).
- only Xbox 360 controls, no XBone [#199](https://codeberg.org/gaiasky/gaiasky/issues/199).

**Fixed bugs:**

- fix Windows 32-bit build [#274](https://codeberg.org/gaiasky/gaiasky/issues/274).

**Closed issues:**

- maximum time reached [#271](https://codeberg.org/gaiasky/gaiasky/issues/271).

## [2.0.0-vr](https://codeberg.org/gaiasky/gaiasky/tree/2.0.0-vr) (2018-05-09).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/2.0.0...2.0.0-vr).

**Implemented enhancements:**

- add night theme [#270](https://codeberg.org/gaiasky/gaiasky/issues/270).

**Fixed bugs:**

- fix SAMP issues when loading [#266](https://codeberg.org/gaiasky/gaiasky/issues/266).
- fix constellation name flickering when planets are turned off [#264](https://codeberg.org/gaiasky/gaiasky/issues/264).

**Closed issues:**

- is it possible to extend the size of the user interface [#269](https://codeberg.org/gaiasky/gaiasky/issues/269).

## [2.0.0](https://codeberg.org/gaiasky/gaiasky/tree/2.0.0) (2018-04-24).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/1.5.0...2.0.0).

**Implemented enhancements:**

- add hue and saturation to levels [#263](https://codeberg.org/gaiasky/gaiasky/issues/263).
- add support for asteroid positions additionally to orbits [#262](https://codeberg.org/gaiasky/gaiasky/issues/262).
- add Hammer-Aitoff and cylindrical projections to cubemap mode [#260](https://codeberg.org/gaiasky/gaiasky/issues/260).
- expose dataset chooser to prefs window [#259](https://codeberg.org/gaiasky/gaiasky/issues/259).
- add projection minimaps [#255](https://codeberg.org/gaiasky/gaiasky/issues/255).
- add image format and quality for screenshots and frames to config [#253](https://codeberg.org/gaiasky/gaiasky/issues/253).
- add reset time hotkey [#252](https://codeberg.org/gaiasky/gaiasky/issues/252).
- apply graphics quality without restart [#251](https://codeberg.org/gaiasky/gaiasky/issues/251).
- add gravitational wave model [#249](https://codeberg.org/gaiasky/gaiasky/issues/249).
- add CMB [#248](https://codeberg.org/gaiasky/gaiasky/issues/248).
- add SAMP support [#246](https://codeberg.org/gaiasky/gaiasky/issues/246).
- use memory mapped files for speed-critical read operations [#245](https://codeberg.org/gaiasky/gaiasky/issues/245).
- remove android/html/desktop infrastructure [#244](https://codeberg.org/gaiasky/gaiasky/issues/244).
- add relativistic aberration [#242](https://codeberg.org/gaiasky/gaiasky/issues/242).
- add flag to enable dataset chooser dialog at startup [#240](https://codeberg.org/gaiasky/gaiasky/issues/240).
- improve occlusion test in light glow algorithm [#239](https://codeberg.org/gaiasky/gaiasky/issues/239).
- add pure GPU line renderer for orbits [#232](https://codeberg.org/gaiasky/gaiasky/issues/232).
- add star opacity setter to API [#231](https://codeberg.org/gaiasky/gaiasky/issues/231).
- add visual effects controls to API [#230](https://codeberg.org/gaiasky/gaiasky/issues/230).
- add stereo and 360 modes to API [#229](https://codeberg.org/gaiasky/gaiasky/issues/229).
- add star size setter to API [#228](https://codeberg.org/gaiasky/gaiasky/issues/228).
- add 'stop time' to scripting API [#226](https://codeberg.org/gaiasky/gaiasky/issues/226).
- add `setPlanetariumMode()` API call [#225](https://codeberg.org/gaiasky/gaiasky/issues/225).
- add API call to control brightness and contrast [#221](https://codeberg.org/gaiasky/gaiasky/issues/221).
- add a reload default settings button [#220](https://codeberg.org/gaiasky/gaiasky/issues/220).
- add `getSimulationTime()` to scripting [#219](https://codeberg.org/gaiasky/gaiasky/issues/219).
- add frame output state indicator [#218](https://codeberg.org/gaiasky/gaiasky/issues/218).
- set crosshair visibility API call [#215](https://codeberg.org/gaiasky/gaiasky/issues/215).
- add `setSimulationTime()` with comprehensive params to scripting [#214](https://codeberg.org/gaiasky/gaiasky/issues/214).
- add 'Back to Earth' key mapping [#209](https://codeberg.org/gaiasky/gaiasky/issues/209).
- add pointer coordinates toggle in preferences [#208](https://codeberg.org/gaiasky/gaiasky/issues/208).
- constellations with proper motions [#203](https://codeberg.org/gaiasky/gaiasky/issues/203).
- add controller debug mode to help create mappings [#202](https://codeberg.org/gaiasky/gaiasky/issues/202).
- add support for emissive colors and textures [#201](https://codeberg.org/gaiasky/gaiasky/issues/201).
- upgrade to libgdx 1.9.7 [#200](https://codeberg.org/gaiasky/gaiasky/issues/200).
- adapt normal lighting shader to accept no directional lights [#197](https://codeberg.org/gaiasky/gaiasky/issues/197).
- update Jython to 2.7.0 [#194](https://codeberg.org/gaiasky/gaiasky/issues/194).
- feature request - scripting functions [#192](https://codeberg.org/gaiasky/gaiasky/issues/192).
- add distance to Sol in focus info interface [#191](https://codeberg.org/gaiasky/gaiasky/issues/191).
- look for ways to prevent time overflow [#190](https://codeberg.org/gaiasky/gaiasky/issues/190).
- add star clusters [#188](https://codeberg.org/gaiasky/gaiasky/issues/188).
- enable proper motions [#185](https://codeberg.org/gaiasky/gaiasky/issues/185).
- allow arbitrary meshes in JSON data files [#184](https://codeberg.org/gaiasky/gaiasky/issues/184).
- add 'pause background loading' action [#181](https://codeberg.org/gaiasky/gaiasky/issues/181).
- fix action buttons (stop script, stop camera path) [#180](https://codeberg.org/gaiasky/gaiasky/issues/180).
- add titles to data with i18n [#179](https://codeberg.org/gaiasky/gaiasky/issues/179).
- crosshair when in free camera + target mode [#178](https://codeberg.org/gaiasky/gaiasky/issues/178).
- crosshair to point to focus direction when off-screen [#177](https://codeberg.org/gaiasky/gaiasky/issues/177).
- problem loading many asteroid orbits [#98](https://codeberg.org/gaiasky/gaiasky/issues/98).
- shadow mapping [#60](https://codeberg.org/gaiasky/gaiasky/issues/60).

**Fixed bugs:**

- fix position discrepancy of stars in stereo mode (points vs billboards) [#258](https://codeberg.org/gaiasky/gaiasky/issues/258).
- screenshot and frame mode switch from simple to advanced produces null pointer [#257](https://codeberg.org/gaiasky/gaiasky/issues/257).
- refactor time [#256](https://codeberg.org/gaiasky/gaiasky/issues/256).
- streaming catalog loader never attempts previously discarded pages [#241](https://codeberg.org/gaiasky/gaiasky/issues/241).
- fix returning from panorama mode through stereo mode [#238](https://codeberg.org/gaiasky/gaiasky/issues/238).
- add object scaling to scripting API [#227](https://codeberg.org/gaiasky/gaiasky/issues/227).
- fix atmosphere flickering due to z fighting [#224](https://codeberg.org/gaiasky/gaiasky/issues/224).
- fix Gaia FoV detection and projection [#223](https://codeberg.org/gaiasky/gaiasky/issues/223).
- fixed errors not logging correctly during init [#222](https://codeberg.org/gaiasky/gaiasky/issues/222).
- remove wrong \[h/sec\] units in time warp label [#217](https://codeberg.org/gaiasky/gaiasky/issues/217).
- star label positioning does not react to FoV setting [#216](https://codeberg.org/gaiasky/gaiasky/issues/216).
- fix focus issue using shift in objects component input [#213](https://codeberg.org/gaiasky/gaiasky/issues/213).
- fix NUMPAD4/5/6 to access FOV camera modes [#212](https://codeberg.org/gaiasky/gaiasky/issues/212).
- fix star min opacity initialization [#207](https://codeberg.org/gaiasky/gaiasky/issues/207).
- crash when selecting NBG galaxy with the time on [#206](https://codeberg.org/gaiasky/gaiasky/issues/206).
- goToObject(name, angle) not zooming out if current angle is larger than target [#195](https://codeberg.org/gaiasky/gaiasky/issues/195).
- NullPointerException in DesktopNetworkChecker [#193](https://codeberg.org/gaiasky/gaiasky/issues/193).
- look for ways to prevent time overflow [#190](https://codeberg.org/gaiasky/gaiasky/issues/190).
- fix visibility of date/time and time warp factor [#189](https://codeberg.org/gaiasky/gaiasky/issues/189).
- fix `facingFocus` state issue [#187](https://codeberg.org/gaiasky/gaiasky/issues/187).
- fix MAS\_TO\_DEG conversion in AstroUtils [#186](https://codeberg.org/gaiasky/gaiasky/issues/186).
- fix 'run script' window handling of scripts with same name [#182](https://codeberg.org/gaiasky/gaiasky/issues/182).
- motion blur causes problems with 360 mode [#87](https://codeberg.org/gaiasky/gaiasky/issues/87).

**Closed issues:**

- is this update also coming? [#261](https://codeberg.org/gaiasky/gaiasky/issues/261).
- enable particle effects [#254](https://codeberg.org/gaiasky/gaiasky/issues/254).
- add-ons [#250](https://codeberg.org/gaiasky/gaiasky/issues/250).
- crash with TGAS GPU dataset [#236](https://codeberg.org/gaiasky/gaiasky/issues/236).
- preferences window shows wrong version number [#234](https://codeberg.org/gaiasky/gaiasky/issues/234).
- cannot build desktop:dist [#233](https://codeberg.org/gaiasky/gaiasky/issues/233).
- feature request: galactic cartesian coordinates [#211](https://codeberg.org/gaiasky/gaiasky/issues/211).
- adding meshes [#205](https://codeberg.org/gaiasky/gaiasky/issues/205).
- on Windows, install fails with "Could not determine java version from '9.0.1' [#204](https://codeberg.org/gaiasky/gaiasky/issues/204).
- docs don't mention where record data appears [#198](https://codeberg.org/gaiasky/gaiasky/issues/198).
- so....I...uhh...broke it in the most beautiful way I could think...ever. [#196](https://codeberg.org/gaiasky/gaiasky/issues/196).
- javadocs no longer available [#183](https://codeberg.org/gaiasky/gaiasky/issues/183).
- not truly compatible with Oculus Rift [#44](https://codeberg.org/gaiasky/gaiasky/issues/44).

**Merged pull requests:**

- add REST API for remote control [#237](https://codeberg.org/gaiasky/gaiasky/pull/237) ([vga101](https://github.com/vga101)).
- DE translation and minor formatting update [#235](https://codeberg.org/gaiasky/gaiasky/pull/235) ([vga101](https://github.com/vga101)).

## [1.5.0](https://codeberg.org/gaiasky/gaiasky/tree/1.5.0) (2017-08-02).
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/1.0.4...1.5.0).

**Implemented enhancements:**

- integrate particle groups with levels of detail [#170](https://codeberg.org/gaiasky/gaiasky/issues/170).
- set up renderer using asset manager [#167](https://codeberg.org/gaiasky/gaiasky/issues/167).
- set up post processor through the asset manager [#166](https://codeberg.org/gaiasky/gaiasky/issues/166).
- scale point primitives by ratio to default [#163](https://codeberg.org/gaiasky/gaiasky/issues/163).
- implement GPU-based implementation for star catalogs [#162](https://codeberg.org/gaiasky/gaiasky/issues/162).
- additive blending [#160](https://codeberg.org/gaiasky/gaiasky/issues/160).
- enable star particle groups [#159](https://codeberg.org/gaiasky/gaiasky/issues/159).
- expose high accuracy positions setting in the GUI [#157](https://codeberg.org/gaiasky/gaiasky/issues/157).
- allow high accuracy in VSOP87 model [#156](https://codeberg.org/gaiasky/gaiasky/issues/156).
- front end to manage game controller mappings [#155](https://codeberg.org/gaiasky/gaiasky/issues/155).
- add nearby galaxies, NBG [#154](https://codeberg.org/gaiasky/gaiasky/issues/154).
- add Oort cloud [#152](https://codeberg.org/gaiasky/gaiasky/issues/152).
- add Pluto [#151](https://codeberg.org/gaiasky/gaiasky/issues/151).
- abstract controller mappings, use files to define them [#150](https://codeberg.org/gaiasky/gaiasky/issues/150).
- add target mode in free camera [#148](https://codeberg.org/gaiasky/gaiasky/issues/148).
- add 'land on object' function [#147](https://codeberg.org/gaiasky/gaiasky/issues/147).
- on-demand catalog loading from disk [#146](https://codeberg.org/gaiasky/gaiasky/issues/146).
- French translation [#145](https://codeberg.org/gaiasky/gaiasky/issues/145).
- allow for controller look y-axis to be inverted [#143](https://codeberg.org/gaiasky/gaiasky/issues/143).
- support lazy texture initialisation for faster startup [#140](https://codeberg.org/gaiasky/gaiasky/issues/140).
- add Saturn moons [#139](https://codeberg.org/gaiasky/gaiasky/issues/139).
- revamp debug info [#138](https://codeberg.org/gaiasky/gaiasky/issues/138).
- add non cinematic camera mode [#135](https://codeberg.org/gaiasky/gaiasky/issues/135).
- discard current star shader based on noise and use texture instead [#134](https://codeberg.org/gaiasky/gaiasky/issues/134).
- apply screen mode without restart [#128](https://codeberg.org/gaiasky/gaiasky/issues/128).
- make network checker (Simbad, wiki) asynchronous [#127](https://codeberg.org/gaiasky/gaiasky/issues/127).
- deprecate current swing-based preferences [#125](https://codeberg.org/gaiasky/gaiasky/issues/125).
- apply skin change without restarting [#124](https://codeberg.org/gaiasky/gaiasky/issues/124).
- colour code proper motion vectors with direction/magnitude [#123](https://codeberg.org/gaiasky/gaiasky/issues/123).
- fix layout of controls window [#121](https://codeberg.org/gaiasky/gaiasky/issues/121).
- add context menu with some options [#120](https://codeberg.org/gaiasky/gaiasky/issues/120).
- rearrange UI, fix HiDPI themes [#119](https://codeberg.org/gaiasky/gaiasky/issues/119).
- add button to stop current camera play session [#117](https://codeberg.org/gaiasky/gaiasky/issues/117).
- UI animations [#116](https://codeberg.org/gaiasky/gaiasky/issues/116).
- add Slovene language [#109](https://codeberg.org/gaiasky/gaiasky/issues/109).
- add new Parallel View stereoscopic profile [#105](https://codeberg.org/gaiasky/gaiasky/issues/105).
- upgrade to LWJGL 3 [#103](https://codeberg.org/gaiasky/gaiasky/issues/103).

**Fixed bugs:**

- fix eye separation in spacecraft+stereoscopic modes [#168](https://codeberg.org/gaiasky/gaiasky/issues/168).
- random crash at startup [#165](https://codeberg.org/gaiasky/gaiasky/issues/165).
- fix post-processing frame buffer resize issue [#164](https://codeberg.org/gaiasky/gaiasky/issues/164).
- scale point primitives by ratio to default [#163](https://codeberg.org/gaiasky/gaiasky/issues/163).
- milky Way texture off when rotated [#158](https://codeberg.org/gaiasky/gaiasky/issues/158).
- fix controller input in non-cinematic mode [#142](https://codeberg.org/gaiasky/gaiasky/issues/142).
- fix smooth transitions in multithread mode [#141](https://codeberg.org/gaiasky/gaiasky/issues/141).
- fix Quad line renderer artifacts [#137](https://codeberg.org/gaiasky/gaiasky/issues/137).
- make network checker (Simbad, wiki) asynchronous [#127](https://codeberg.org/gaiasky/gaiasky/issues/127).
- fix cast error when multithreading is on [#126](https://codeberg.org/gaiasky/gaiasky/issues/126).
- label flickering when star is perfectly aligned with camera direction [#122](https://codeberg.org/gaiasky/gaiasky/issues/122).
- fix main controls window alignments [#118](https://codeberg.org/gaiasky/gaiasky/issues/118).
- fix Gaia scan mode [#114](https://codeberg.org/gaiasky/gaiasky/issues/114).
- add timeout to version check [#112](https://codeberg.org/gaiasky/gaiasky/issues/112).
- fix configuration file lookup crash when running from source [#111](https://codeberg.org/gaiasky/gaiasky/issues/111).
- fix focus issue with objects text field [#106](https://codeberg.org/gaiasky/gaiasky/issues/106).
- fix stereoscopic mode for large distances/eye separations [#89](https://codeberg.org/gaiasky/gaiasky/issues/89).
- Gaia Sky crashes on Windows 10 32bit - JRE 8u102 [#77](https://codeberg.org/gaiasky/gaiasky/issues/77).
- fix octant detection in very low FoV angles [#70](https://codeberg.org/gaiasky/gaiasky/issues/70).

**Closed issues:**

- incorrect size of "Sol" via scripting interface [#174](https://codeberg.org/gaiasky/gaiasky/issues/174).
- parsing of version string breaks when custom git tags are used [#173](https://codeberg.org/gaiasky/gaiasky/issues/173).
- test script `getobject-test.py` crashes [#172](https://codeberg.org/gaiasky/gaiasky/issues/172).
- constellation "Antlia" misspelled as "Antila" [#153](https://codeberg.org/gaiasky/gaiasky/issues/153).
- closest object and camera speed in scripting interface [#149](https://codeberg.org/gaiasky/gaiasky/issues/149).
- cinematic camera setting not saved [#144](https://codeberg.org/gaiasky/gaiasky/issues/144).
- running Gaia Sky in Oculus Rift [#136](https://codeberg.org/gaiasky/gaiasky/issues/136).
- scripting interface: asynchronous mode? [#133](https://codeberg.org/gaiasky/gaiasky/issues/133).
- scripting interface: issues with setCameraPostion method [#132](https://codeberg.org/gaiasky/gaiasky/issues/132).
- scripting interface: calling `setStarBrightness()` seems to change the ambient light [#131](https://codeberg.org/gaiasky/gaiasky/issues/131).
- scripting interface: calling `setVisibility()` toggles independent of parameter [#130](https://codeberg.org/gaiasky/gaiasky/issues/130).
- scripting interface: calling `setCameraLock()` causes Exception in thread "LWJGL Application" [#129](https://codeberg.org/gaiasky/gaiasky/issues/129).
- translation [#107](https://codeberg.org/gaiasky/gaiasky/issues/107).

**Merged pull requests:**

- fix ARI URL [#176](https://codeberg.org/gaiasky/gaiasky/pull/176) ([vga101](https://github.com/vga101)).
- fix method comment for displayTextObject [#175](https://codeberg.org/gaiasky/gaiasky/pull/175) ([vga101](https://github.com/vga101)).
- fix customobjects-test.py [#171](https://codeberg.org/gaiasky/gaiasky/pull/171) ([vga101](https://github.com/vga101)).
- suggested improvements for German translation [#169](https://codeberg.org/gaiasky/gaiasky/pull/169) ([vga101](https://github.com/vga101)).
- fix URL to home page [#161](https://codeberg.org/gaiasky/gaiasky/pull/161) ([vga101](https://github.com/vga101)).
- fix README rendering [#115](https://codeberg.org/gaiasky/gaiasky/pull/115) ([rogersachan](https://github.com/rogersachan)).
- fix links to scripting documentation [#113](https://codeberg.org/gaiasky/gaiasky/pull/113) ([vga101](https://github.com/vga101)).
- fix TGAS extraction path in running instructions [#110](https://codeberg.org/gaiasky/gaiasky/pull/110) ([vga101](https://github.com/vga101)).
- Slovene translation [#108](https://codeberg.org/gaiasky/gaiasky/pull/108) ([kcotar](https://github.com/kcotar)).

## [1.0.4](https://codeberg.org/gaiasky/gaiasky/tree/1.0.4) (2016-12-07)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/1.0.3...1.0.4).

**Implemented enhancements:**

- improve loading times [#102](https://codeberg.org/gaiasky/gaiasky/issues/102).
- config window HiDPI mode [#101](https://codeberg.org/gaiasky/gaiasky/issues/101).
- dependent visibility for orbits [#100](https://codeberg.org/gaiasky/gaiasky/issues/100).
- map and calibrate Milky Way panorama [#94](https://codeberg.org/gaiasky/gaiasky/issues/94).
- add option to capture frames while camera path is playing [#71](https://codeberg.org/gaiasky/gaiasky/issues/71).

**Fixed bugs:**

- fix crosshair issues when resizing [#104](https://codeberg.org/gaiasky/gaiasky/issues/104).
- dependent visibility for orbits [#100](https://codeberg.org/gaiasky/gaiasky/issues/100).
- stars disappear for a while when camera approaches [#97](https://codeberg.org/gaiasky/gaiasky/issues/97).
- version `1.0.3` fills memory with frame output [#96](https://codeberg.org/gaiasky/gaiasky/issues/96).
- light glow sampling spiral should adapt to fov angle [#95](https://codeberg.org/gaiasky/gaiasky/issues/95).
- debug and spacecraft GUIs do not resize correctly [#93](https://codeberg.org/gaiasky/gaiasky/issues/93).
- resizing during loading screen causes buffer size problems [#40](https://codeberg.org/gaiasky/gaiasky/issues/40).

**Merged pull requests:**

- fixed broken download links in README.md [#99](https://codeberg.org/gaiasky/gaiasky/pull/99) ([adamkewley](https://github.com/adamkewley)).

## [1.0.3](https://codeberg.org/gaiasky/gaiasky/tree/1.0.3) (2016-11-15)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/1.0.2...1.0.3).

**Fixed bugs:**

- fix FoV modes [#92](https://codeberg.org/gaiasky/gaiasky/issues/92).
- run tutorial runs pointer [#91](https://codeberg.org/gaiasky/gaiasky/issues/91).

## [1.0.2](https://codeberg.org/gaiasky/gaiasky/tree/1.0.2) (2016-11-14)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/1.0.1...1.0.2).

## [1.0.1](https://codeberg.org/gaiasky/gaiasky/tree/1.0.1) (2016-11-11)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/1.0.0...1.0.1).

**Implemented enhancements:**

- add brightness and contrast controls [#88](https://codeberg.org/gaiasky/gaiasky/issues/88).
- improve search functionality [#85](https://codeberg.org/gaiasky/gaiasky/issues/85).
- spacecraft camera mode - Game on! [#84](https://codeberg.org/gaiasky/gaiasky/issues/84).
- update planets and moons textures [#82](https://codeberg.org/gaiasky/gaiasky/issues/82).
- add an optional crosshair in focus mode [#81](https://codeberg.org/gaiasky/gaiasky/issues/81).
- implement 360 deg mode for 360 VR videos [#80](https://codeberg.org/gaiasky/gaiasky/issues/80).

**Fixed bugs:**

- configuration dialog should appear at the center of focused screen [#90](https://codeberg.org/gaiasky/gaiasky/issues/90).
- fix resizing and full screen toggle [#86](https://codeberg.org/gaiasky/gaiasky/issues/86).
- crash - Vector pool null pointer when multi-threading is on [#83](https://codeberg.org/gaiasky/gaiasky/issues/83).
- fix connection to archive for DR1 sources [#78](https://codeberg.org/gaiasky/gaiasky/issues/78).
- error 1114 [#76](https://codeberg.org/gaiasky/gaiasky/issues/76).
- new Version 1.0.0 doesn't work on OSX 10.10.5 [#75](https://codeberg.org/gaiasky/gaiasky/issues/75).

**Closed issues:**

- Gaia Sky crashes on Windows 10, Java 1.8.0\_101 [#79](https://codeberg.org/gaiasky/gaiasky/issues/79).

## [1.0.0](https://codeberg.org/gaiasky/gaiasky/tree/1.0.0) (2016-09-13)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.800b...1.0.0).

**Implemented enhancements:**

- add orientation lock [#74](https://codeberg.org/gaiasky/gaiasky/issues/74).
- fix frame rate when recording camera [#73](https://codeberg.org/gaiasky/gaiasky/issues/73).
- add planetarium mode [#72](https://codeberg.org/gaiasky/gaiasky/issues/72).
- add sliders for star point size and minimum opacity [#68](https://codeberg.org/gaiasky/gaiasky/issues/68).
- add LOD sliders [#67](https://codeberg.org/gaiasky/gaiasky/issues/67).
- implement anaglyphic 3D [#65](https://codeberg.org/gaiasky/gaiasky/issues/65).
- add distortion to VR\_HEADSET stereoscopic mode [#64](https://codeberg.org/gaiasky/gaiasky/issues/64).
- add data source selection to Preferences [#63](https://codeberg.org/gaiasky/gaiasky/issues/63).
- add support for proper motion vectors [#62](https://codeberg.org/gaiasky/gaiasky/issues/62).
- add interface to data loaders in config dialog [#15](https://codeberg.org/gaiasky/gaiasky/issues/15).

**Fixed bugs:**

- add ambient light to persisted properties [#69](https://codeberg.org/gaiasky/gaiasky/issues/69).
- GUI should be hidden when stereoscopic is on at startup [#66](https://codeberg.org/gaiasky/gaiasky/issues/66).
- fix mouse input in stereoscopic mode [#61](https://codeberg.org/gaiasky/gaiasky/issues/61).
- app won't start [#13](https://codeberg.org/gaiasky/gaiasky/issues/13).

## [0.800b](https://codeberg.org/gaiasky/gaiasky/tree/0.800b) (2016-04-28)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.707b...0.800b).

**Implemented enhancements:**

- add playback music system [#59](https://codeberg.org/gaiasky/gaiasky/issues/59).
- improve render time, use points for all stars [#52](https://codeberg.org/gaiasky/gaiasky/issues/52).
- add smooth transitions between levels of detail [#51](https://codeberg.org/gaiasky/gaiasky/issues/51).
- use view angle as priority for click-selections [#50](https://codeberg.org/gaiasky/gaiasky/issues/50).
- get the Gaia Sanbox ready for proper motions [#48](https://codeberg.org/gaiasky/gaiasky/issues/48).

**Fixed bugs:**

- fix scritping interface timing with frame output system [#55](https://codeberg.org/gaiasky/gaiasky/issues/55).
- fix Gaia scan code [#49](https://codeberg.org/gaiasky/gaiasky/issues/49).

**Closed issues:**

- set time pace to a factor of real time [#58](https://codeberg.org/gaiasky/gaiasky/issues/58).
- add graphics mode selector [#57](https://codeberg.org/gaiasky/gaiasky/issues/57).
- fix the looks for HiDPI screens [#56](https://codeberg.org/gaiasky/gaiasky/issues/56).
- app fails to start OS X [#54](https://codeberg.org/gaiasky/gaiasky/issues/54).

## [0.707b](https://codeberg.org/gaiasky/gaiasky/tree/0.707b) (2015-09-14)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.706b...0.707b).

**Implemented enhancements:**

- simplify loading mechanism of data files [#46](https://codeberg.org/gaiasky/gaiasky/issues/46).
- add sample image when choosing theme [#38](https://codeberg.org/gaiasky/gaiasky/issues/38).
- drop old manual lo-res/hi-res texture loading and implement mipmapping [#35](https://codeberg.org/gaiasky/gaiasky/issues/35).
- update project to libgdx 1.6.0 [#34](https://codeberg.org/gaiasky/gaiasky/issues/34).
- add simple screenshot mode [#32](https://codeberg.org/gaiasky/gaiasky/issues/32).
- move default location of screenshots to `$HOME/.gaiasandbox/screenshots` [#31](https://codeberg.org/gaiasky/gaiasky/issues/31).
- add new Ceres texture from Dawn spacecraft [#30](https://codeberg.org/gaiasky/gaiasky/issues/30).
- new command to travel to focus object instantly [#29](https://codeberg.org/gaiasky/gaiasky/issues/29).
- support for location info [#28](https://codeberg.org/gaiasky/gaiasky/issues/28).
- migrate build system to gradle [#2](https://codeberg.org/gaiasky/gaiasky/issues/2).

**Fixed bugs:**

- Linux launcher not working if spaces in path [#47](https://codeberg.org/gaiasky/gaiasky/issues/47).
- fix labels in Gaia Fov mode [#45](https://codeberg.org/gaiasky/gaiasky/issues/45).
- last update date is sensible to running locale [#43](https://codeberg.org/gaiasky/gaiasky/issues/43).
- RA and DEC are wrong in binary version of HYG catalog [#42](https://codeberg.org/gaiasky/gaiasky/issues/42).
- keyboard focus stays in input texts [#41](https://codeberg.org/gaiasky/gaiasky/issues/41).
- fix new line rendering for perspective lines [#37](https://codeberg.org/gaiasky/gaiasky/issues/37).
- motion blur not working with FXAA or NFAA [#36](https://codeberg.org/gaiasky/gaiasky/issues/36).
- fix night/day blending in shader  [#33](https://codeberg.org/gaiasky/gaiasky/issues/33).
- screenshot action (F5) not working well with motion blur [#27](https://codeberg.org/gaiasky/gaiasky/issues/27).

## [0.706b](https://codeberg.org/gaiasky/gaiasky/tree/0.706b) (2015-05-05)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.705b...0.706b).

**Implemented enhancements:**

- general code style clean-up  [#25](https://codeberg.org/gaiasky/gaiasky/issues/25).
- big performance improvement in star rendering [#23](https://codeberg.org/gaiasky/gaiasky/issues/23).
- new pixel renderer [#22](https://codeberg.org/gaiasky/gaiasky/issues/22).
- add controller support [#21](https://codeberg.org/gaiasky/gaiasky/issues/21).
- motion blur effect [#20](https://codeberg.org/gaiasky/gaiasky/issues/20).
- interface overhaul [#19](https://codeberg.org/gaiasky/gaiasky/issues/19).
- better looking lines [#18](https://codeberg.org/gaiasky/gaiasky/issues/18).

**Fixed bugs:**

- handle outdated properties files in $HOME/.gaiasandbox folder [#26](https://codeberg.org/gaiasky/gaiasky/issues/26).
- scripting implementation should reset the colour [#24](https://codeberg.org/gaiasky/gaiasky/issues/24).

**Closed issues:**

- deprecated [#17](https://codeberg.org/gaiasky/gaiasky/issues/17).

## [0.705b](https://codeberg.org/gaiasky/gaiasky/tree/0.705b) (2015-04-16)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.704b...0.705b).

**Fixed bugs:**

- gaia sandbox current releases do not work on windows [#16](https://codeberg.org/gaiasky/gaiasky/issues/16).
- post-processing causes display output to disappear in frame output mode [#14](https://codeberg.org/gaiasky/gaiasky/issues/14).
- make new PixelBloomRenderSystem work for frame output and screenshots [#7](https://codeberg.org/gaiasky/gaiasky/issues/7).
- make new PixelBloomRenderSystem work in stereoscopic mode [#6](https://codeberg.org/gaiasky/gaiasky/issues/6).

## [0.704b](https://codeberg.org/gaiasky/gaiasky/tree/0.704b) (2015-03-27)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.703b...0.704b).

**Implemented enhancements:**

- remove synchronized render lists [#12](https://codeberg.org/gaiasky/gaiasky/issues/12).
- support top speeds in GUI [#11](https://codeberg.org/gaiasky/gaiasky/issues/11).
- show camera info in free mode [#10](https://codeberg.org/gaiasky/gaiasky/issues/10).
- time selector [#9](https://codeberg.org/gaiasky/gaiasky/issues/9).
- add interface tab to configuration [#8](https://codeberg.org/gaiasky/gaiasky/issues/8).
- internationalize the application [#5](https://codeberg.org/gaiasky/gaiasky/issues/5).
- move node data format to JSON [#1](https://codeberg.org/gaiasky/gaiasky/issues/1).

**Fixed bugs:**

- investigate VM crash [#4](https://codeberg.org/gaiasky/gaiasky/issues/4).
- decide fate of desktop/doc/gaiasandbox\_manual.tex [#3](https://codeberg.org/gaiasky/gaiasky/issues/3).

## [0.703b](https://codeberg.org/gaiasky/gaiasky/tree/0.703b) (2014-12-17)
[Full Changelog](https://codeberg.org/gaiasky/gaiasky/compare/0.700b...0.703b).

- multiple big additions.

## [0.700b](https://codeberg.org/gaiasky/gaiasky/tree/0.700b) (2014-12-11)

- first beta version.
