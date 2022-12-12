/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.event.Event;
import gaiasky.event.EventManager;

import java.util.HashMap;
import java.util.Map;

public class Logger {

    private static final Map<String, Log> logObjects;
    public static LoggerLevel level = LoggerLevel.INFO;

    static {
        logObjects = new HashMap<>();
    }

    private static void error(Throwable t, String tag) {
        if (inLevel(LoggerLevel.ERROR))
            if (EventManager.instance.hasSubscriptors(Event.JAVA_EXCEPTION)) {
                EventManager.publish(Event.JAVA_EXCEPTION, null, t, tag);
            } else {
                System.err.println(tag);
                t.printStackTrace(System.err);
            }
    }

    private static void error(Throwable t) {
        if (inLevel(LoggerLevel.ERROR))
            if (EventManager.instance.hasSubscriptors(Event.JAVA_EXCEPTION)) {
                EventManager.publish(Event.JAVA_EXCEPTION, null, t);
            } else {
                t.printStackTrace(System.err);
            }
    }

    private static void error(Object... messages) {
        if (inLevel(LoggerLevel.ERROR))
            log(LoggerLevel.ERROR, messages);
    }

    private static void warn(Object... messages) {
        if (inLevel(LoggerLevel.WARN))
            log(LoggerLevel.WARN, messages);
    }

    private static void info(Object... messages) {
        if (inLevel(LoggerLevel.INFO)) {
            log(LoggerLevel.INFO, messages);
        }
    }

    private static void debug(Object... messages) {
        if (inLevel(LoggerLevel.DEBUG))
            log(LoggerLevel.DEBUG, messages);
    }

    private static void log(LoggerLevel level, Object... messages) {
        int idx = -1;
        for (int i = 0; i < messages.length; i++) {
            Object msg = messages[i];
            if (msg instanceof String && ((String) msg).contains("{}")) {
                idx = i;
                break;
            }
        }
        Object[] msgs;
        if (idx >= 0) {
            String msg = parse((String) messages[idx], removeFirstN(messages, idx + 1));
            msgs = getFirstNPlus(messages, idx, msg);
        } else {
            msgs = messages;
        }

        EventManager.publish(Event.POST_NOTIFICATION, null, level, msgs);
    }

    /**
     * Removes first n elements of given array
     *
     * @param arr The array
     * @param n   Number of elements to remove from beginning
     *
     * @return The resulting array
     */
    private static Object[] removeFirstN(Object[] arr, int n) {
        Object[] res = new Object[arr.length - n];
        if (arr.length - n >= 0)
            System.arraycopy(arr, 0 + n, res, 0, arr.length - n);
        return res;
    }

    private static Object[] getFirstNPlus(Object[] arr, int n, Object additional) {
        Object[] res = new Object[n + 1];
        if (n >= 0)
            System.arraycopy(arr, 0, res, 0, n);
        res[n] = additional;
        return res;
    }

    private static String parse(String msg, Object... args) {
        for (Object arg1 : args) {
            String arg = arg1 != null ? arg1.toString() : "null";
            msg = msg.replaceFirst("\\{}", arg);
        }
        return msg;

    }

    private static boolean inLevel(LoggerLevel l) {
        return l.getVal() <= level.getVal();
    }

    /**
     * Returns default logger
     *
     * @return The default logger
     */
    public static Log getLogger() {
        return getLogger("");
    }

    /**
     * Gets the logger for the particular class
     *
     * @param clazz The class
     *
     * @return The logger
     */
    public static Log getLogger(Class<?> clazz) {
        return getLogger(clazz.getSimpleName());
    }

    /**
     * Gets a logger for an arbitary string tag
     *
     * @param tag The tag
     *
     * @return The logger
     */
    public static Log getLogger(String tag) {
        if (logObjects.containsKey(tag)) {
            return logObjects.get(tag);
        } else {
            Log log = new Log(tag);
            logObjects.put(tag, log);
            return log;
        }
    }

    public enum LoggerLevel {
        ERROR(0),
        WARN(1),
        INFO(2),
        DEBUG(3);

        public int val;

        LoggerLevel(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }

    }

    public static class Log {
        private final String tag;

        private Log(Class<?> clazz) {
            super();
            this.tag = clazz.getSimpleName();
        }

        private Log(String tag) {
            super();
            this.tag = tag;
        }

        public void error(Throwable t) {
            Logger.error(t, tag);
        }

        public void error(Throwable t, String message) {
            Logger.error(t, prependTag(new String[] { message }));
        }

        public void error(Object... messages) {
            Logger.error(prependTag(messages));
        }

        public void warn(Object... messages) {
            Logger.warn(prependTag(messages));
        }

        public void debug(Object... messages) {
            Logger.debug(prependTag(messages));
        }

        public void info(Object... messages) {
            Logger.info(prependTag(messages));
        }

        private Object[] prependTag(Object[] msgs) {
            Object[] result = new Object[msgs.length + 1];
            System.arraycopy(msgs, 0, result, 1, msgs.length);
            result[0] = tag;
            return result;
        }
    }

}
