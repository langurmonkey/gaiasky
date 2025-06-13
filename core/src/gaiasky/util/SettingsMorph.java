/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import gaiasky.util.Settings.*;
import gaiasky.util.Settings.CamrecorderSettings.KeyframeSettings;
import gaiasky.util.Settings.ControlsSettings.GamepadSettings;
import gaiasky.util.Settings.GraphicsSettings.FullscreenSettings;
import gaiasky.util.Settings.PostprocessSettings.*;
import gaiasky.util.Settings.ProgramSettings.*;
import gaiasky.util.Settings.ProgramSettings.ModeCubemapSettings.PlanetariumSettings;
import gaiasky.util.Settings.ProgramSettings.NetSettings.MasterSettings;
import gaiasky.util.Settings.ProgramSettings.NetSettings.SlaveSettings;
import gaiasky.util.Settings.ProgramSettings.PointerSettings.GuidesSettings;
import gaiasky.util.Settings.SceneSettings.*;
import gaiasky.util.Settings.SceneSettings.CameraSettings.FocusSettings;
import gaiasky.util.Settings.SceneSettings.RendererSettings.ElevationSettings;
import gaiasky.util.Settings.SceneSettings.RendererSettings.LineSettings;
import gaiasky.util.Settings.SceneSettings.RendererSettings.ShadowSettings;
import gaiasky.util.Settings.SceneSettings.StarSettings.GroupSettings;
import gaiasky.util.Settings.SceneSettings.StarSettings.ThresholdSettings;
import gaiasky.util.camera.rec.KeyframesManager.PathType;
import gaiasky.util.parse.Parser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Convert old settings (properties files) to new settings (YAML-based).
 */
public class SettingsMorph {

    /**
     * This method loads the given properties file into a {@link java.util.Properties},
     * creates a {@link Settings} and fills it up with the contents of the
     * properties file. Finally, it writes the {@link Settings} instance to
     * disk in the YAML format to the given <code>yamlFile</code>.
     *
     * @param propertiesFile The location of the properties file.
     * @param yamlFile       The location to save the YAML settings file.
     */
    public static void morphSettings(final Path propertiesFile, final Path yamlFile) throws IOException {
        // Load properties file
        Properties p = new Properties();
        p.load(new FileInputStream(propertiesFile.toFile()));

        // Create settings
        final var s = new Settings();

        // Configuration version
        s.configVersion = i32("properties.version", p);

        // Performance
        var performance = new PerformanceSettings();
        performance.multithreading = bool("global.conf.multithreading", p);
        performance.numberThreads = i32("global.conf.numthreads", p);
        s.performance = performance;

        // Graphics
        var graphics = new GraphicsSettings();
        graphics.screenOutput = bool("graphics.screen.screenoutput", p);

        int w = i32("graphics.screen.width", p);
        int h = i32("graphics.screen.height", p);
        graphics.resolution = new int[] { w, h };

        FullscreenSettings fullScreen = new FullscreenSettings();
        int fsw = i32("graphics.screen.fullscreen.width", p);
        int fsh = i32("graphics.screen.fullscreen.height", p);
        fullScreen.resolution = new int[] { fsw, fsh };
        fullScreen.active = bool("graphics.screen.fullscreen", p);
        graphics.fullScreen = fullScreen;

        graphics.resizable = bool("graphics.screen.resizable", p);
        graphics.vsync = bool("graphics.screen.vsync", p);
        graphics.fpsLimit = i32("graphics.limit.fps", p);

        graphics.backBufferScale = f64("graphics.backbuffer.scale", p);
        graphics.dynamicResolution = bool("graphics.dynamic.resolution", p);
        graphics.quality = GraphicsQuality.valueOf(str("scene.graphics.quality", p).toUpperCase(Locale.ROOT));
        s.graphics = graphics;

        // Frame
        var frame = new FrameSettings();
        frame.mode = screenshotMode(str("graphics.render.mode", p));
        int fw = i32("graphics.render.width", p);
        int fh = i32("graphics.render.height", p);
        frame.resolution = new int[] { fw, fh };
        frame.targetFps = f64("graphics.render.targetfps", p);
        frame.location = str("graphics.render.folder", p);
        frame.prefix = str("graphics.render.filename", p);
        frame.time = bool("graphics.render.time", p);
        frame.format = ImageFormat.valueOf(str("graphics.render.format", p).toUpperCase(Locale.ROOT));
        frame.quality = f32("graphics.render.quality", p);
        s.frame = frame;

        // Screenshot
        var screenshot = new ScreenshotSettings();
        screenshot.mode = screenshotMode(str("screenshot.mode", p).toUpperCase(Locale.ROOT));
        int sw = i32("screenshot.width", p);
        int sh = i32("screenshot.height", p);
        screenshot.resolution = new int[] { sw, sh };
        screenshot.location = str("screenshot.folder", p);
        screenshot.format = ImageFormat.valueOf(str("screenshot.format", p).toUpperCase(Locale.ROOT));
        screenshot.quality = f32("screenshot.quality", p);
        s.screenshot = screenshot;

        // Camrecorder
        var camrec = new CamrecorderSettings();
        var keyframe = new KeyframeSettings();
        keyframe.position = PathType.valueOf(str("graphics.camera.keyframe.path.position", p));
        keyframe.orientation = PathType.valueOf(str("graphics.camera.keyframe.path.orientation", p));
        camrec.keyframe = keyframe;
        camrec.targetFps = f64("graphics.camera.recording.targetfps", p);
        camrec.auto = bool("graphics.camera.recording.frameoutputatuo", p);
        s.camrecorder = camrec;

        // Data
        var data = new DataSettings();
        data.location = str("data.location", p);
        ArrayList<String> dataFiles = new ArrayList<>();
        var jsonCatalog = str("data.json.catalog", p);
        if (jsonCatalog != null && !jsonCatalog.isBlank()) {
            String[] tokens = jsonCatalog.split(java.io.File.pathSeparator);
            dataFiles.addAll(Arrays.asList(tokens));
        }
        var jsonObjects = str("data.json.objects", p);
        if (jsonObjects != null && !jsonObjects.isBlank()) {
            String[] tokens = jsonObjects.split(",");
            dataFiles.addAll(Arrays.asList(tokens));
        }
        // Make paths relative to $data/
        data.dataFiles = new ArrayList<>(dataFiles.size());
        for (String dataFile : dataFiles) {
            Path dataLocation = Path.of(Settings.settings.data.location);
            String relative = dataLocation.toUri().relativize(new java.io.File(dataFile).toURI()).getPath();
            String f = Constants.DATA_LOCATION_TOKEN + relative;
            data.dataFiles.add(f);
        }

        data.highAccuracy = bool("data.highaccuracy.positions", p);
        data.reflectionSkyboxLocation = str("data.skybox.location", p);
        data.realGaiaAttitude = bool("data.attitude.real", p);
        s.data = data;

        // Scene
        var scene = new SceneSettings();
        scene.homeObject = str("scene.object.startup", p);
        scene.initialization = new InitializationSettings();
        scene.initialization.lazyTexture = bool("scene.lazy.texture", p);
        scene.initialization.lazyMesh = bool("scene.lazy.mesh", p);
        var camera = new CameraSettings();
        camera.speedLimitIndex = i32("scene.camera.speedlimit", p);
        camera.speed = f64("scene.camera.focus.vel", p);
        camera.turn = f64("scene.camera.turn.vel", p);
        camera.rotate = f64("scene.camera.rotate.vel", p);
        camera.fov = f32("scene.camera.fov", p);
        camera.cinematic = bool("scene.camera.cinematic", p);
        camera.targetMode = bool("scene.camera.free.targetmode", p);
        var focusLock = new FocusSettings();
        focusLock.position = bool("scene.focuslock", p);
        focusLock.orientation = bool("scene.focuslock.orientation", p);
        camera.focusLock = focusLock;
        scene.camera = camera;
        scene.fadeMs = i32("scene.object.fadems", p);
        var star = new StarSettings();
        star.brightness = f32("scene.star.brightness", p);
        star.power = f32("scene.star.brightness.pow", p);
        star.pointSize = f32("scene.star.point.size", p);
        star.textureIndex = i32("scene.star.tex.index", p);
        var group = new GroupSettings();
        group.billboard = bool("scene.star.group.billboard.flag", p);
        group.numBillboard = i32("scene.star.group.billboards", p);
        group.numLabels = i32("scene.star.group.labels", p);
        group.numVelocityVector = i32("scene.star.group.velocityvectors", p);
        star.group = group;
        star.threshold = new ThresholdSettings();
        star.threshold.quad = f64("scene.star.threshold.quad", p);
        star.threshold.point = f64("scene.star.threshold.point", p);
        star.threshold.none = f64("scene.star.threshold.none", p);
        float alpha0 = f32("scene.point.alpha.min", p);
        float alpha1 = f32("scene.point.alpha.max", p);
        star.opacity = new float[] { alpha0, alpha1 };
        scene.star = star;
        var octree = new OctreeSettings();
        float th0 = f32("scene.octant.threshold.0", p);
        float th1 = f32("scene.octant.threshold.1", p);
        octree.threshold = new float[] { th0, th1 };
        octree.maxStars = i32("scene.octree.maxstars", p);
        octree.fade = bool("scene.octree.particle.fade", p);
        scene.octree = octree;
        var renderer = new RendererSettings();
        renderer.ambient = f32("scene.ambient", p);
        renderer.shadow = new ShadowSettings();
        renderer.shadow.active = bool("scene.shadowmapping", p);
        renderer.shadow.resolution = i32("scene.shadowmapping.resolution", p);
        renderer.shadow.number = i32("scene.shadowmapping.nshadows", p);
        renderer.elevation = new ElevationSettings();
        renderer.elevation.type = ElevationType.valueOf(str("scene.elevation.type", p).toUpperCase(Locale.ROOT));
        renderer.elevation.multiplier = f64("scene.elevation.multiplier", p);
        renderer.elevation.quality = f64("scene.tessellation.quality", p);
        var line = new LineSettings();
        line.width = f32("scene.line.width", p);
        scene.renderer.line = line;
        scene.renderer = renderer;
        var label = new LabelSettings();
        label.size = f32("scene.label.size", p);
        label.number = f32("scene.label.number", p);
        scene.label = label;
        scene.properMotion = new ProperMotionSettings();
        scene.properMotion.length = f32("scene.propermotion.lenfactor", p);
        scene.properMotion.number = f32("scene.propermotion.numfactor", p);
        scene.properMotion.colorMode = i32("scene.propermotion.colormode", p);
        scene.properMotion.arrowHeads = bool("scene.propermotion.arrowheads", p);
        scene.crosshair = new CrosshairSettings();
        scene.crosshair.focus = bool("scene.crosshair.focus", p);
        scene.crosshair.closest = bool("scene.crosshair.closest", p);
        scene.crosshair.home = bool("scene.crosshair.home", p);
        scene.visibility = new HashMap<>();
        p.keySet().stream().filter(key -> ((String) key).startsWith("scene.visibility.")).forEach(key -> {
            String ct = ((String) key).substring(17);
            scene.visibility.put(ct, bool((String) key, p));
        });
        s.scene = scene;

        // Postprocess
        var postprocess = new PostprocessSettings();
        postprocess.antialiasing = new AntialiasSettings();
        postprocess.antialiasing.type = AntialiasType.getFromCode(i32("postprocess.antialiasing", p));
        postprocess.bloom = new BloomSettings();
        postprocess.bloom.intensity = f32("postprocess.bloom.intensity", p);
        postprocess.unsharpMask = new UnsharpMaskSettings();
        postprocess.unsharpMask.factor = f32("postprocess.unsharpmask.factor", p);
        postprocess.motionBlur.active = f32("postprocess.motionblur", p) != 0;
        postprocess.lensFlare.active = bool("postprocess.lensflare", p);
        postprocess.lightGlow.active = bool("postprocess.lightscattering", p);
        postprocess.reprojection.active = bool("postprocess.fisheye", p);
        var levels = new LevelsSettings();
        levels.brightness = f32("postprocess.brightness", p);
        levels.contrast = f32("postprocess.contrast", p);
        levels.hue = f32("postprocess.hue", p);
        levels.saturation = f32("postprocess.saturation", p);
        levels.gamma = f32("postprocess.gamma", p);
        postprocess.levels = levels;
        postprocess.toneMapping = new ToneMappingSettings();
        postprocess.toneMapping.setType(str("postprocess.tonemapping.type", p));
        postprocess.toneMapping.exposure = f32("postprocess.exposure", p);
        s.postprocess = postprocess;

        // Program
        var program = new ProgramSettings();
        program.safeMode = bool("program.safe.graphics.mode", p);
        program.debugInfo = bool("program.debuginfo", p);
        program.fileChooser = new FileChooserSettings();
        program.fileChooser.showHidden = bool("program.filechooser.showhidden", p);
        program.fileChooser.lastLocation = str("program.last.filesystem.location", p);
        program.pointer = new PointerSettings();
        program.pointer.coordinates = bool("program.pointer.coords.display", p);
        program.pointer.guides = new GuidesSettings();
        program.pointer.guides.active = bool("program.pointer.guides.display", p);
        program.pointer.guides.color = arr("program.pointer.guides.color", p);
        program.pointer.guides.width = f32("program.pointer.guides.width", p);
        program.recursiveGrid = new RecursiveGridSettings();
        program.recursiveGrid.setOrigin(str("program.recursivegrid.origin", p));
        program.recursiveGrid.projectionLines = bool("program.recursivegrid.origin.lines", p);
        program.minimap = new MinimapSettings();
        program.minimap.active = bool("program.display.minimap", p);
        program.minimap.size = f32("program.minimap.size", p);
        program.minimap.inWindow = false;
        program.modeStereo = new ModeStereoSettings();
        program.modeStereo.active = bool("program.stereoscopic", p);
        program.modeStereo.profile = StereoProfile.values()[i32("program.stereoscopic.profile", p)];
        program.modeCubemap = new ModeCubemapSettings();
        program.modeCubemap.active = bool("program.cubemap", p);
        program.modeCubemap.setProjection(str("program.cubemap.projection", p));
        program.modeCubemap.faceResolution = i32("program.cubemap.face.resolution", p);
        program.modeCubemap.celestialSphereIndexOfRefraction = f32("program.modeCubemap.celestialSphereIndexOfRefraction", p);
        program.modeCubemap.planetarium = new PlanetariumSettings();
        program.modeCubemap.planetarium.angle = f32("program.planetarium.angle", p);
        program.modeCubemap.planetarium.aperture = f32("program.planetarium.aperture", p);
        program.net = new NetSettings();
        program.net.restPort = i32("program.restport", p);
        program.net.master = new MasterSettings();
        program.net.master.active = bool("program.net.master", p);
        program.net.slave = new SlaveSettings();
        program.net.slave.active = bool("program.net.slave", p);
        program.net.slave.configFile = str("program.net.slave.config", p);
        program.net.slave.yaw = f32("program.net.slave.yaw", p);
        program.net.slave.pitch = f32("program.net.slave.pitch", p);
        program.net.slave.roll = f32("program.net.slave.roll", p);
        program.net.slave.warpFile = str("program.net.slave.warp", p);
        program.net.slave.blendFile = str("program.net.slave.blend", p);
        program.ui = new UiSettings();
        program.ui.theme = str("program.ui.theme", p);
        program.ui.scale = f32("program.ui.scale", p);
        program.exitConfirmation = bool("program.exit.confirmation", p);
        program.locale = str("program.locale", p);
        program.update = new UpdateSettings();
        program.update.lastVersion = "";
        program.url = new UrlSettings();
        program.url.versionCheck = escapeURL(str("program.url.versioncheck", p));
        program.url.dataMirror = escapeURL(str("program.url.data.mirror", p));
        program.url.dataDescriptor = escapeURL(str("program.url.data.descriptor", p));
        s.program = program;

        // Controls
        var controls = new ControlsSettings();
        controls.gamepad = new GamepadSettings();
        controls.gamepad.mappingsFile = str("controls.gamepad.mappings.file", p);
        controls.gamepad.invertX = bool("controls.invert.x", p);
        controls.gamepad.invertY = bool("controls.invert.y", p);
        String blacklist = str("controls.blacklist", p);
        controls.gamepad.blacklist = GlobalResources.parseWhitespaceSeparatedList(blacklist);
        if (controls.gamepad.blacklist == null)
            controls.gamepad.blacklist = new String[] {};
        s.controls = controls;

        // Spacecraft
        var spacecraft = new SpacecraftSettings();
        spacecraft.velocityDirection = bool("spacecraft.velocity.direction", p);
        spacecraft.showAxes = bool("spacecraft.show.axes", p);
        s.spacecraft = spacecraft;

        // Persist to file
        YAMLFactory yaml = new YAMLFactory();
        yaml.disable(Feature.WRITE_DOC_START_MARKER).enable(Feature.MINIMIZE_QUOTES).enable(Feature.INDENT_ARRAYS);
        ObjectMapper mapper = new ObjectMapper(yaml);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.findAndRegisterModules();

        FileOutputStream fos = new FileOutputStream(yamlFile.toFile());
        SequenceWriter sequenceWriter = mapper.writerWithDefaultPrettyPrinter().writeValues(fos);
        sequenceWriter.write(s);
        fos.close();
    }

    private static ScreenshotMode screenshotMode(String value) {
        if (value.equalsIgnoreCase("redraw")) {
            value = "ADVANCED";
        }
        return ScreenshotMode.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private static float[] arr(final String key, final Properties properties) {
        var arr = str(key, properties);
        return Parser.parseFloatArray(arr);
    }

    private static String str(final String key, final Properties properties) {
        return properties.getProperty(key);
    }

    private static boolean bool(final String key, final Properties properties) {
        return Parser.parseBoolean(properties.getProperty(key));
    }

    private static float f32(final String key, final Properties properties) {
        return Parser.parseFloat(properties.getProperty(key));
    }

    private static double f64(final String key, final Properties properties) {
        return Parser.parseDouble(properties.getProperty(key));
    }

    private static int i32(final String key, final Properties properties) {
        return Parser.parseInt(properties.getProperty(key));
    }

    private static long i64(final String key, final Properties properties) {
        return Parser.parseLong(properties.getProperty(key));
    }

    private static String escapeURL(final String url) {
        return url.replaceAll("\\\\", "");
    }
}
