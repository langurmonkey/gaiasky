package gaia.cu9.ari.gaiaorbit.script;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import org.python.core.PyCode;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory class to create, execute and cancel Jython scripts.
 * @author Toni Sagrista
 *
 */
public class JythonFactory extends ScriptingFactory implements IObserver {
    private static final Log logger = Logger.getLogger(JythonFactory.class);
    
    /** Singleton pattern **/
    private static JythonFactory instance = null;

    //    private ExecutorService executor;
    private final PythonInterpreter interpreter;

    private static final int maxScripts = 1;

    /**
     * Synchronized map with curent scripts
     */
    private Map<String, ScriptRunnable> currentScripts;

    /**
     * Gets the JythonFactory instance.
     * @return The factory instance.
     */
    public synchronized static JythonFactory getInstance() {
        if (instance == null) {
            instance = new JythonFactory();
        }
        return instance;
    }

    /**
     * Only initialize the executor service and the interpreter.
     */
    private JythonFactory() {
        //	executor = Executors.newCachedThreadPool(new JythonThreadFactory());
        interpreter = PythonInterpreter.threadLocalStateInterpreter(null);
        currentScripts = Collections.synchronizedMap(new LinkedHashMap<String, ScriptRunnable>() {

            @Override
            public ScriptRunnable put(String key, ScriptRunnable value) {
                ScriptRunnable sr = super.put(key, value);
                EventManager.instance.post(Events.NUM_RUNNING_SCRIPTS, this.size());
                return sr;
            }

            @Override
            public ScriptRunnable remove(Object key) {
                ScriptRunnable sr = super.remove(key);
                EventManager.instance.post(Events.NUM_RUNNING_SCRIPTS, this.size());
                return sr;
            }

        });
        EventManager.instance.subscribe(this, Events.RUN_SCRIPT_PYCODE, Events.RUN_SCRIPT_PATH, Events.CANCEL_SCRIPT_CMD);
    }

    private PyCode compileJythonScript(String script) {
        return interpreter.compile(script);
    }

    public PyCode compileJythonScript(File script) throws Exception {
        return interpreter.compile(new FileReader(script));
    }

    /**
     * Runs the given PyCode, optionally in a thread.
     * @param code The already compiled PyCode object.
     * @param path The path to the script file.
     * @param async Boolean indicating whether to run the script in a separate thread or not. If 
     * true, the execution is asynchronous and the call returns immediately.
     */
    private void runJythonScript(final PyCode code, String path, boolean async) {
        if (currentScripts.size() < maxScripts) {
            Thread run = new ScriptRunnable(code, path);
            // Maximum priority to script
            run.setPriority(Thread.MAX_PRIORITY);

            if (async) {
                run.start();
            } else {
                run.run();
            }
        } else {
            logger.info(I18n.bundle.format("notif.script.max", maxScripts));
        }
    }

    /**
     * Runs the given script, optionally in a thread.
     * @param script The python script to run.
     * @param path The path to the script file.
     * @param async Boolean indicating whether to run the script in a separate thread or not. If 
     * true, the execution is asynchronous and the call returns immediately.
     */
    private void runJythonScript(final String script, String path, boolean async) {
        try {
            runJythonScript(compileJythonScript(script), path, async);
        } catch (Exception e) {
            EventManager.instance.post(Events.JAVA_EXCEPTION, e);
        }

    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case RUN_SCRIPT_PYCODE:
            PyCode code = (PyCode) data[0];
            String path = (String) data[1];
            boolean async = true;
            if (data.length > 2)
                async = (Boolean) data[2];
            runJythonScript(code, path, async);
            break;
        case RUN_SCRIPT_PATH:
            path = (String) data[0];
            FileHandle file = Gdx.files.internal(path);
            String string = file.readString();
            async = true;
            if (data.length > 1)
                async = (Boolean) data[1];
            runJythonScript(string, path, async);
            break;
        case CANCEL_SCRIPT_CMD:
            String pathToCancel = currentScripts.keySet().iterator().next();
            cancelScript(pathToCancel);
            break;
        }

    }

    /**
     * The default thread factory
     */
    static class JythonThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        JythonThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "scripting-thread-" + poolNumber.getAndIncrement();
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    public int getNumRunningScripts() {
        return currentScripts.size();
    }

    /**
     * Cancels the running script identified by the given path.
     * @param path The path.
     */
    private void cancelScript(String path) {
        if (currentScripts.containsKey(path)) {
            ScriptRunnable sr = currentScripts.get(path);
            try {
                // TODO Figure a better way than this to stop the script
                sr.stop();
            } catch (Exception e) {
                EventManager.instance.post(Events.JAVA_EXCEPTION, e);
            }
            sr.interpreter.cleanup();
            sr.cleanup();
        }
    }

    /**
     * This class runs scripts.
     * @author Toni Sagrista
     *
     */
    private class ScriptRunnable extends Thread {
        final PyCode code;
        final String path;
        PythonInterpreter interpreter;

        ScriptRunnable(PyCode code, String path) {
            this.code = code;
            this.path = path;
        }

        @Override
        public void run() {
            if (currentScripts.size() < maxScripts) {
                if (currentScripts.containsKey(path)) {
                    logger.info(I18n.bundle.format("notif.script.already", path));
                    return;
                }
                currentScripts.put(path, this);
                try {
                    interpreter = PythonInterpreter.threadLocalStateInterpreter(null);
                    interpreter.exec(code);
                    cleanup();
                } catch (Exception e) {
                    if (e.getCause() instanceof ThreadDeath) {
                        logger.info("Script stopped");
                    } else {
                        logger.error(e);
                    }
                }
            }
        }

        void cleanup() {
            // Re-enable input and remove objects, just in case
            EventManager.instance.post(Events.REMOVE_ALL_OBJECTS);
            EventManager.instance.post(Events.CLEAR_MESSAGES);
            EventManager.instance.post(Events.INPUT_ENABLED_CMD, true);
            currentScripts.remove(path);
        }

    }
}
