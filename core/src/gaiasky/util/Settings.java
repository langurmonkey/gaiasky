package gaiasky.util;

import gaiasky.desktop.util.camera.CameraKeyframeManager;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.GlobalConf.ImageFormat;
import gaiasky.util.GlobalConf.ProgramConf.OriginType;
import gaiasky.util.GlobalConf.SceneConf.ElevationType;
import gaiasky.util.GlobalConf.SceneConf.GraphicsQuality;
import gaiasky.util.GlobalConf.ScreenshotMode;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class contains the settings for Gaia Sky, organized into
 * several sub-classes by topic.
 */
public class Settings {
    public static Settings settings;

    public int version;

    public DataSettings data;
    public PerformanceSettings performance;
    public GraphicsSettings graphics;
    public SceneSettings scene;
    public ProgramSettings program;
    public ControlsSettings controls;
    public FrameSettings frame;
    public ScreenshotSettings screenshot;
    public CamrecorderSettings camrecorder;
    public PostprocessSettings postprocess;
    public SpacecraftSettings spacecraft;
    public RuntimeSettings runtime;

    public static class DataSettings {
        public String location;
        public List<String> catalogFiles;
        public List<String> objectFiles;
        public String skyboxLocation;
        public boolean highAccuracy;
        public boolean realGaiaAttitude;
    }

    public static class PerformanceSettings {
        public boolean multithreading;
        public int numberThreads;
    }

    public static class GraphicsSettings {
        public GraphicsQuality quality;
        public int[] resolution;
        public boolean resizable;
        public FullscreenSettings fullScreen;
        public boolean vsync;
        public double fpsLimit;
        public double backBufferScale;
        public boolean dynamicResolution;
        public boolean screenOutput;

        public void setQuality(final String qualityString) {
            this.quality = GraphicsQuality.valueOf(qualityString.toUpperCase());
        }

        public static class FullscreenSettings {
            public int[] resolution;
            public boolean active;
        }
    }

    public static class SceneSettings {
        public String homeObject;
        public long fadeMs;
        public CameraSettings camera;
        public StarSettings star;
        public LabelSettings label;
        public double lineWidth;
        public ProperMotionSettings properMotion;
        public OctreeSettings octree;
        public RendererSettings renderer;
        public CrosshairSettings crosshair;
        public InitializationSettings initialization;
        public boolean[] visibility;

        public void setVisibility(final Map<String, Object> map) {
            ComponentType[] cts = ComponentType.values();
            visibility = new boolean[cts.length];
            for (ComponentType ct : cts) {
                String key = ct.name();
                if (map.containsKey(key)) {
                    visibility[ct.ordinal()] = (boolean) map.get(key);
                }
            }
        }

        public static class CameraSettings {
            public int speedLimit;
            public double speed;
            public double turn;
            public double rotate;
            public double fov;
            public boolean cinematic;
            public boolean targetMode;
            public FocusSettings focusLock;

            public static class FocusSettings {
                public boolean position;
                public boolean orientation;
            }
        }

        public static class StarSettings {
            public double brightness;
            public double power;
            public double pointSize;
            public int textureIndex;
            public double[] opacity;
            public GroupSettings group;
            public ThresholdSettings threshold;
            public ProperMotionSettings properMotion;
            public OctreeSettings octree;

            public static class GroupSettings {
                public boolean billboard;
                public int numBillboard;
                public int numLabel;
                public int numVelocityVector;
            }

            public static class ThresholdSettings {
                public double quad;
                public double point;
                public double none;
            }
        }

        public static class LabelSettings {
            public double size;
            public double number;
        }

        public static class ProperMotionSettings {
            public double length;
            public double number;
            public int colorMode;
            public boolean arrowHeads;
        }

        public static class OctreeSettings {
            public int maxStars;
            public double[] threshold;
            public boolean fade;
        }

        public static class RendererSettings {
            public int line;
            public int orbit;
            public double ambient;
            public ShadowSettings shadow;
            public ElevationSettings elevation;

            public static class ShadowSettings {
                public boolean active;
                public int resolution;
                public int number;
            }

            public static class ElevationSettings {
                public ElevationType type;
                public double multiplier;
                public double quality;

                public void setType(final String typeString) {
                    this.type = ElevationType.valueOf(typeString.toUpperCase());
                }
            }
        }

        public static class CrosshairSettings {
            public boolean focus;
            public boolean closest;
            public boolean home;
        }

        public static class InitializationSettings {
            public boolean lazyTexture;
            public boolean lazyMesh;
        }

    }

    public static class ProgramSettings {
        public boolean safeMode;
        public boolean debugInfo;
        public boolean hud;
        public MinimapSettings minimap;
        public FileChooserSettings fileChooser;
        public PointerSettings pointer;
        public RecursiveGridSettings recursiveGrid;
        public ModeStereoSettings modeStereo;
        public ModeCubemapSettings modeCubemap;
        public NetSettings net;
        public String scriptsLocation;
        public UiSettings ui;
        public boolean exitConfirmation;
        public String locale;
        public UpdateSettings update;
        public UrlSettings url;

        public static class MinimapSettings {
            public boolean active;
            public int size;
        }

        public static class FileChooserSettings {
            public boolean showHidden;
        }

        public static class PointerSettings {
            public boolean coordinates;
            public GuidesSettings guides;

            public static class GuidesSettings {
                public boolean active;
                public float[] color;
                public double width;

            }
        }

        public static class RecursiveGridSettings {
            public OriginType origin;
            public boolean projectionLines;

            public void setOrigin(final String originString) {
                origin = OriginType.valueOf(originString.toUpperCase());
            }
        }

        public static class ModeStereoSettings {
            public boolean active;
            public int profile;
        }

        public static class ModeCubemapSettings {
            public boolean active;
            public CubemapProjection projection;
            public int faceResolution;
            public PlanetariumSettings planetarium;

            public void setProjection(final String projectionString) {
                projection = CubemapProjection.valueOf(projectionString.toUpperCase());
            }

            public static class PlanetariumSettings {
                public double aperture;
                public double angle;
            }
        }

        public static class NetSettings {
            public int restPort;
            public MasterSettings master;
            public SlaveSettings slave;

            public static class MasterSettings {
                public boolean active;
                public List<String> slaves;
            }

            public static class SlaveSettings {
                public boolean active;
                public String configFile;
                public String warpFile;
                public String blendFile;
                public double yaw;
                public double pitch;
                public double roll;

            }
        }

        public static class UiSettings {
            public String theme;
            public double scale;
        }

        public static class UpdateSettings {
            public Instant lastCheck;
            public String lastVersion;
        }

        public static class UrlSettings {
            public String versionCheck;
            public String dataMirror;
            public String dataDescriptor;
        }
    }

    public static class ControlsSettings {
        public GamepadSettings gamepad;

        public static class GamepadSettings {
            public String mappingsFile;
            public boolean invertX;
            public boolean invertY;
            public String[] blacklist;
        }
    }

    public static class ScreenshotSettings {
        public String location;
        public ImageFormat format;
        public double quality;
        public ScreenshotMode mode;
        public int[] resolution;

        public void setFormat(final String formatString) {
            format = ImageFormat.valueOf(formatString.toUpperCase());
        }

        public void setMode(final String modeString) {
            mode = ScreenshotMode.valueOf(modeString);
        }
    }

    public static class FrameSettings extends ScreenshotSettings {
        public String prefix;
        public boolean time;
        public double targetFps;
    }

    public static class CamrecorderSettings {
        public double targetFps;
        public KeyframeSettings keyframe;
        public boolean auto;

        public static class KeyframeSettings {
            public CameraKeyframeManager.PathType position;
            public CameraKeyframeManager.PathType orientation;

            public void setPosition(final String positionString) {
                position = getPathType(positionString);
            }

            public void setOrientation(final String orientationString) {
                orientation = getPathType(orientationString);
            }

            private CameraKeyframeManager.PathType getPathType(String str) {
                return CameraKeyframeManager.PathType.valueOf(str.toUpperCase());
            }
        }

    }

    public static class PostprocessSettings {
        public GlobalConf.PostprocessConf.Antialias antialias;
        public BloomSettings bloom;
        public UnsharpMaskSettings unsharpMask;
        public boolean motionBlur;
        public boolean lensFlare;
        public boolean lightGlow;
        public boolean fisheye;
        public LevelsSettings levels;
        public ToneMappingSettings toneMapping;

        public void setAntialias(final String antialiasString) {
            antialias = GlobalConf.PostprocessConf.Antialias.valueOf(antialiasString.toUpperCase());
        }

        public static class BloomSettings {
            public double intensity;
        }

        public static class UnsharpMaskSettings {
            public double factor;
        }

        public static class LevelsSettings {
            public double brightness;
            public double contrast;
            public double hue;
            public double saturation;
            public double gamma;
        }

        public static class ToneMappingSettings {
            public GlobalConf.PostprocessConf.ToneMapping type;
            public double exposure;

            public void setType(final String typeString) {
                type = GlobalConf.PostprocessConf.ToneMapping.valueOf(typeString.toUpperCase());
            }
        }
    }

    public static class SpacecraftSettings {
        public boolean velocityDirection;
        public boolean showAxes;
    }

    public static class RuntimeSettings implements IObserver {
        public boolean openVr = false;
        public boolean OVR = false;
        public boolean displayGui;
        public boolean updatePause;
        public boolean timeOn;
        public boolean realTime;
        public boolean inputEnabled;
        public boolean recordCamera;
        public boolean recordKeyframeCamera;

        public boolean drawOctree;
        public boolean relativisticAberration = false;
        public boolean gravitationalWaves = false;
        public boolean displayVrGui = false;

        // Max clock time, 5 Myr by default
        public long maxTimeMs = 5000000L * (long) Nature.Y_TO_MS;
        // Min clock time, -5 Myr by default
        public long minTimeMs = -maxTimeMs;

        public RuntimeSettings() {
            EventManager.instance.subscribe(this, Events.INPUT_ENABLED_CMD, Events.DISPLAY_GUI_CMD, Events.TOGGLE_UPDATEPAUSE, Events.TIME_STATE_CMD, Events.RECORD_CAMERA_CMD, Events.GRAV_WAVE_START, Events.GRAV_WAVE_STOP, Events.DISPLAY_VR_GUI_CMD);
        }

        /**
         * Toggles the time
         */
        public void toggleTimeOn(Boolean timeOn) {
            this.timeOn = Objects.requireNonNullElseGet(timeOn, () -> !this.timeOn);
        }

        /**
         * Toggles the record camera
         */
        private double backupLimitFps = 0;

        public void toggleRecord(Boolean rec, Settings settings) {
            recordCamera = Objects.requireNonNullElseGet(rec, () -> !recordCamera);

            if (recordCamera) {
                // Activation, set limit FPS
                backupLimitFps =  settings.graphics.fpsLimit;
                settings.graphics.fpsLimit = settings.frame.targetFps;
            } else {
                // Deactivation, remove limit
                settings.graphics.fpsLimit = backupLimitFps;
            }
        }
        @Override
        public void notify(Events event, Object... data) {

            switch (event) {
            case INPUT_ENABLED_CMD:
                inputEnabled = (boolean) data[0];
                break;

            case DISPLAY_GUI_CMD:
                displayGui = (boolean) data[0];
                break;
            case DISPLAY_VR_GUI_CMD:
                if (data.length > 1) {
                    displayVrGui = (Boolean) data[1];
                } else {
                    displayVrGui = !displayVrGui;
                }
                break;
            case TOGGLE_UPDATEPAUSE:
                updatePause = !updatePause;
                EventManager.instance.post(Events.UPDATEPAUSE_CHANGED, updatePause);
                break;
            case TIME_STATE_CMD:
                toggleTimeOn((Boolean) data[0]);
                break;
            case RECORD_CAMERA_CMD:
                toggleRecord((Boolean) data[0], settings);
                break;
            case GRAV_WAVE_START:
                gravitationalWaves = true;
                break;
            case GRAV_WAVE_STOP:
                gravitationalWaves = false;
                break;
            default:
                break;

            }
        }
    }
}
