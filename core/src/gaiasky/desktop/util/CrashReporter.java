/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.interfce.MessageBean;
import gaiasky.interfce.NotificationsInterface;
import gaiasky.util.GlobalConf;
import gaiasky.util.Logger.Log;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.*;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CrashReporter {

    public static void reportCrash(Throwable t, Log logger) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");

        // Crash directory
        File crashDir = SysUtils.getCrashReportsDir();
        crashDir.mkdirs();
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
        File logFile = writeLog(logger, crashDir, dateString);

        // Output crash info
        for (String str : crashInfo)
            print(logger, str);

        // CRASH FILE
        File crashReportFile = writeCrash(logger, crashDir, dateString, crashInfo);

        // Closure
        String crf1 = "Crash report file saved to: " + crashReportFile.getPath();
        String crf4 = "Full log file saved to: " + logFile.getPath();
        String crf2 = "Please attach these files to the bug report";
        String crf3 = "Create a bug report here: https://gitlab.com/langurmonkey/gaiasky/issues";
        int len = Math.max(crf1.length(), Math.max(crf2.length(), Math.max(crf3.length(), crf4.length())));
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

    private static File writeLog(Log logger, File crashDir, String dateString) {
        // LOG FILE
        List<MessageBean> logMessages = NotificationsInterface.getHistorical();
        File logFile = new File(crashDir, "gaiasky_log_" + dateString + ".txt");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(logFile));
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
                writer.close();
            } catch (Exception e) {
            }
        }
        return logFile;
    }

    private static File writeCrash(Log logger, File crashDir, String dateString, Array<String> crashInfo) {
        File crashReportFile = new File(crashDir, "gaiasky_crash_" + dateString + ".txt");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(crashReportFile));
            for (String str : crashInfo) {
                writer.write(str);
                writer.newLine();
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Writing crash report crashed... Inception level 1 achieved! :_D", e);
            } else {
                System.err.println("Writing crash report crashed... Inception level 1 achieved! :_D");
                e.printStackTrace(System.err);
            }
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {

            }
        }
        return crashReportFile;
    }

    private static void print(Log logger, String str) {
        if (logger == null || !EventManager.instance.hasSubscriptors(Events.POST_NOTIFICATION)) {
            System.err.println(str);
        } else {
            logger.error(str);
        }
    }

    private static void appendSystemInfo(Array<String> strArray) {
        /* Gaia Sky info */
        if (GlobalConf.version != null) {
            strArray.add("");
            strArray.add("## GAIA SKY INFORMATION");
            strArray.add("Version: " + GlobalConf.version.version);
            strArray.add("Build: " + GlobalConf.version.build);
            strArray.add("Builder: " + GlobalConf.version.builder);
            strArray.add("System: " + GlobalConf.version.system);
            strArray.add("Build time: " + GlobalConf.version.buildtime);
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
            strArray.add("CPU: " + cp.getName());
            strArray.add("CPU arch: " + (cp.isCpu64bit() ? "64-bit" : "32-bit"));
        } catch (Error e) {
            strArray.add("Could not get CPU information!");
        }

        strArray.add("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        strArray.add("Free memory (bytes): " + Runtime.getRuntime().freeMemory());
        long maxMemory = Runtime.getRuntime().maxMemory();
        strArray.add("Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
        strArray.add("Total memory available to JVM (bytes): " + Runtime.getRuntime().totalMemory());
        File[] roots = File.listRoots();
        for (File root : roots) {
            strArray.add("File system root: " + root.getAbsolutePath());
            strArray.add("Total space (bytes): " + root.getTotalSpace());
            strArray.add("Free space (bytes): " + root.getFreeSpace());
            strArray.add("Usable space (bytes): " + root.getUsableSpace());
        }

        /* OS info */
        strArray.add("");
        strArray.add("## OS INFORMATION");
        strArray.add("OS name: " + System.getProperty("os.name"));
        strArray.add("OS version: " + System.getProperty("os.version"));
        strArray.add("OS architecture: " + System.getProperty("os.arch"));

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
                String[] extensionsstr = new String[next];
                for (int i = 0; i < next; i++) {
                    extensionsstr[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
                }
                extensions = arrayToStr(extensionsstr, " ");
            }
            strArray.add("GL extensions: " + extensions);

            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
            int maxSize = buf.get(0);
            strArray.add("GL max texture size: " + maxSize);
        } catch (Exception e) {
            strArray.add("## GL INFORMATION not available");
        }

    }

    private static String arrayToStr(String[] arr, String sep) {
        String buff = new String();
        for (int i = 0; i < arr.length; i++) {
            buff += arr[i] + sep;
        }
        return buff;
    }

}
