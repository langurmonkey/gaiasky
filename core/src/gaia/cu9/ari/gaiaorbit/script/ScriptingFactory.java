package gaia.cu9.ari.gaiaorbit.script;

/**
 * Creates an initializes the scripting subsystems
 */
public abstract class ScriptingFactory {

    private static ScriptingFactory factory;

    public static ScriptingFactory getInstance() {
        return factory;
    }

    public static void initialize(ScriptingFactory factory) {
        ScriptingFactory.factory = factory;
    }

    public abstract int getNumRunningScripts();

}
