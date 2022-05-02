/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.interafce.MessageBean;
import gaiasky.interafce.NotificationsInterface;
import gaiasky.util.Logger.Log;
import gaiasky.vr.openvr.VRContext;
import gaiasky.vr.openvr.VRContext.VRDevice;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Creates a report whenever Gaia Sky crashes and saves it to disk.
 * It also handles the last session's log and writes it to disk if necessary.
 */
public class CrashReporter {

    public static void reportCrash(Throwable t, Log logger) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");

        // Crash directory
        Path crashDir = SysUtils.getCrashReportsDir();
        try {
            Files.createDirectories(crashDir);
        } catch (IOException e) {
            logger.error(e);
        }
        // Date string
        Date now = new Date();
        String dateString = df.format(now);

        // Write system info
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String stackTrace = sw.toString();

        Array<String> crashInfo = new Array<>();
        crashInfo.add("#");
        crashInfo.add("# GAIA SKY CRASH REPORT");
        crashInfo.add("# " + now);
        crashInfo.add("");
        crashInfo.add("## STACK TRACE");
        crashInfo.add(stackTrace);

        appendSystemInfo(crashInfo);

        // LOG FILE
        Path logFile = writeLog(logger, crashDir, dateString);

        // Output crash info
        crashInfo.forEach(str -> print(logger, str));

        // CRASH FILE
        Path crashReportFile = writeCrash(logger, crashDir, dateString, crashInfo);

        // Closure
        String crf1 = "Crash report file saved to: " + crashReportFile.toAbsolutePath();
        String crf4 = "Full log file saved to: " + (logFile != null ? logFile.toAbsolutePath() : "permission error");
        String crf2 = "Please attach these files to the bug report";
        String crf3 = "Create a bug report here: " + Settings.REPO_ISSUES;
        int len = Math.max(crf1.length(), Math.max(crf3.length(), crf4.length()));
        char[] chars = new char[len];
        Arrays.fill(chars, '#');
        String separatorLine = new String(chars);
        print(logger, "");
        print(logger, separatorLine);
        print(logger, crf1);
        print(logger, crf4);
        print(logger, crf2);
        print(logger, crf3);
        print(logger, separatorLine);
        print(logger, "");
    }

    public static void writeLastSessionLog(Log logger) {
        Path logDir = SysUtils.getLogDir();
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            logger.error(e);
        }
        // Write session log
        writeLog(logger, logDir, "lastsession");
    }

    /**
     * Writes the given logger to a file in the given directory using the given
     * suffix string.
     * @param logger The logger.
     * @param dir The path to the output directory.
     * @param suffixString The suffix for the log file name (can be a date string).
     * @return The path to the created log file.
     */
    private static Path writeLog(Log logger, Path dir, String suffixString) {
        if(Files.exists(dir) && Files.isWritable(dir)) {
            // LOG FILE
            List<MessageBean> logMessages = NotificationsInterface.getHistorical();
            Path logFile = dir.resolve("gaiasky_log_" + suffixString + ".txt");
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(logFile.toFile()));
                for (MessageBean b : logMessages) {
                    writer.write(b.formatMessage(true));
                    writer.newLine();
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Writing log crashed... Inception level 1 achieved! :_D", e);
                } else {
                    System.err.println("Writing log crashed... Inception level 1 achieved! :_D");
                    e.printStackTrace(System.err);
                }
            } finally {
                try {
                    // Close the writer regardless of what happens...
                    if (writer != null)
                        writer.close();
                } catch (Exception e) {
                    if (logger != null)
                        logger.error("Closing writer crashed (inception level 2 achieved!)", e);
                }
            }
            return logFile;
        } else {
            System.err.println("Log directory (" + dir.toAbsolutePath() + ") does not exist or is not writable");
            return null;
        }
    }

    private static Path writeCrash(Log logger, Path crashDir, String dateString, Array<String> crashInfo) {
        Path crashReportFile = crashDir.resolve("gaiasky_crash_" + dateString + ".txt");
        final BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(crashReportFile.toFile()));
            crashInfo.forEach(str -> {
                try {
                    writer.write(str);
                    writer.newLine();
                } catch (Exception e) {
                    if (logger != null) {
                        logger.error("Writing crash report crashed (inception level 1 achieved!)", e);
                    } else {
                        System.err.println("Writing crash report crashed (inception level 1 achieved!)");
                        e.printStackTrace(System.err);
                    }
                }
            });
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
                if (logger != null)
                    logger.error("Closing writer crashed (inception level 2 achieved!)", e);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Creating crash writer crashed (inception level 1 achieved!)", e);
            } else {
                System.err.println("Creating crash writer crashed (inception level 1 achieved!)");
                e.printStackTrace(System.err);
            }
        }
        return crashReportFile;
    }

    private static void print(Log logger, String str) {
        if (logger == null || !EventManager.instance.hasSubscriptors(Event.POST_NOTIFICATION)) {
            System.err.println(str);
        } else {
            logger.error(str);
        }
    }

    private static void appendSystemInfo(Array<String> strArray) {
        /* Gaia Sky info */
        if (Settings.settings != null && Settings.settings.version != null) {
            strArray.add("");
            strArray.add("## GAIA SKY INFORMATION");
            strArray.add("Version: " + Settings.settings.version.version);
            strArray.add("Build: " + Settings.settings.version.build);
            strArray.add("Builder: " + Settings.settings.version.builder);
            strArray.add("System: " + Settings.settings.version.system);
            strArray.add("Build time: " + Settings.settings.version.buildTime);
        } else {
            strArray.add("");
            strArray.add("## Can't get Gaia Sky version information");
            strArray.add("## Settings.settings[.version] is null!");
        }

        /* Java info */
        strArray.add("");
        strArray.add("## JAVA INFORMATION");
        strArray.add("Java version: " + System.getProperty("java.version"));
        strArray.add("Java runtime name: " + System.getProperty("java.runtime.name"));
        strArray.add("Java VM name: " + System.getProperty("java.vm.name"));
        strArray.add("Java VM version: " + System.getProperty("java.vm.version"));
        strArray.add("Java VM vendor: " + System.getProperty("java.vm.vendor"));

        /* System information */
        strArray.add("");
        strArray.add("## SYSTEM INFORMATION");
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor cp = hal.getProcessor();
            strArray.add("CPU: " + cp.getProcessorIdentifier().getName());
            strArray.add(cp.getProcessorIdentifier().getIdentifier());
            strArray.add("Proc ID: " + cp.getProcessorIdentifier().getProcessorID());
            strArray.add("CPU Family: " + cp.getProcessorIdentifier().getFamily());
            strArray.add(cp.getPhysicalPackageCount() + " physical CPU package(s)");
            strArray.add(cp.getPhysicalProcessorCount() + " physical CPU core(s)");
            int nLp = cp.getLogicalProcessorCount();
            strArray.add(nLp + " logical CPU(s)");
        } catch (Error e) {
            strArray.add("Could not get CPU information!");
        }

        String mbUnits = " MB";
        strArray.add("Java used memory: " + MemInfo.getUsedMemory() + mbUnits);
        strArray.add("Java free memory: " + MemInfo.getFreeMemory() + mbUnits);
        strArray.add("Java total memory: " + MemInfo.getTotalMemory() + mbUnits);
        strArray.add("Java max memory (-Xmx): " + MemInfo.getMaxMemory() + mbUnits);
        strArray.add("Total system RAM: " + MemInfo.getTotalRam() + mbUnits);

        File[] roots = File.listRoots();
        for (File root : roots) {
            strArray.add(" # File system root: " + root.getAbsolutePath());
            strArray.add("   Total space: " + (root.getTotalSpace() * Constants.BYTE_TO_MB) + mbUnits);
            strArray.add("   Free space: " + (root.getFreeSpace() * Constants.BYTE_TO_MB) + mbUnits);
            strArray.add("   Usable space: " + (root.getUsableSpace() * Constants.BYTE_TO_MB) + mbUnits);
        }

        /* OS info */
        strArray.add("");
        strArray.add("## OS INFORMATION");
        try {
            SystemInfo si = new SystemInfo();
            OperatingSystem os = si.getOperatingSystem();
            strArray.add("OS name: " + os.toString());
            strArray.add("OS version: " + System.getProperty("os.version"));
            strArray.add("OS architecture: " + System.getProperty("os.arch"));
            strArray.add("Booted: " + Instant.ofEpochSecond(os.getSystemBootTime()));
            strArray.add("Uptime: " + FormatUtil.formatElapsedSecs(si.getOperatingSystem().getSystemUptime()));
        } catch (Exception e) {
            strArray.add("OS name: " + System.getProperty("os.name"));
            strArray.add("OS version: " + System.getProperty("os.version"));
            strArray.add("OS architecture: " + System.getProperty("os.arch"));
        }

        try {
            /* GL info */
            strArray.add("");
            strArray.add("## GL INFORMATION");
            strArray.add("Graphcis device: " + Gdx.gl.glGetString(GL20.GL_RENDERER));
            strArray.add("GL vendor: " + Gdx.gl.glGetString(GL20.GL_VENDOR));
            strArray.add("GL version: " + Gdx.gl.glGetString(GL20.GL_VERSION));
            strArray.add("GLSL version: " + Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));

            String extensions = Gdx.gl.glGetString(GL20.GL_EXTENSIONS);
            IntBuffer buf = BufferUtils.newIntBuffer(16);
            if (extensions == null || extensions.isEmpty()) {
                Gdx.gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, buf);
                int next = buf.get(0);
                String[] extensionsString = new String[next];
                for (int i = 0; i < next; i++) {
                    extensionsString[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
                }
                extensions = TextUtils.arrayToStr(extensionsString, "", "", " ");
            }
            strArray.add("GL extensions: " + extensions);

            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
            int maxSize = buf.get(0);
            strArray.add("GL max texture size: " + maxSize);
        } catch (Exception e) {
            strArray.add("## GL INFORMATION not available");
        }

        if(Settings.settings != null && Settings.settings.runtime.openVr) {
           VRContext vrContext = GaiaSky.instance.vrContext;
           if(vrContext != null){
               /* VR info **/
               strArray.add("");
               strArray.add("## VR INFORMATION");
               strArray.add("VR resolution: " + vrContext.getWidth() + "x" + vrContext.getHeight());
               Array<VRDevice> devices = vrContext.getDevices();
               for(VRDevice device : devices) {
                   strArray.add("Device: " + device.renderModelName);
                   strArray.add("    Model number: " + device.modelNumber);
                   strArray.add("    Manufacturer: " + device.manufacturerName);
                   strArray.add("    Role: " + device.getControllerRole());
                   strArray.add("    Type: " + device.getType());
                   strArray.add("    Index: " + (device.getPose() != null ? device.getPose().getIndex() : "null"));
               }
           }
        }
    }
}
