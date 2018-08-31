package gaia.cu9.ari.gaiaorbit.util.coord.vsop87;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import gaia.cu9.ari.gaiaorbit.interfce.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

public class VSOP87 {
    private static final Log logger = Logger.getLogger(VSOP87.class);
    public static VSOP87 instance;
    static {
        instance = new VSOP87();
    }

    private Map<String, iVSOP87> elements;
    private Map<String, Boolean> tried;

    public VSOP87() {
        elements = new HashMap<String, iVSOP87>();
        tried = new HashMap<String, Boolean>();
    }

    public iVSOP87 getVOSP87(String cb) {
        if (!tried.containsKey(cb) || !tried.get(cb)) {
            // Initialize
            String pkg = "gaia.cu9.ari.gaiaorbit.util.coord.vsop87.";
            String name = TextUtils.trueCapitalise(cb) + "VSOP87";
            Class<?> clazz = null;
            try {
                clazz = ClassReflection.forName(pkg + name);
            } catch (ReflectionException e) {
                clazz = DummyVSOP87.class;
            }
            try {
                elements.put(cb, (iVSOP87) ClassReflection.newInstance(clazz));
            } catch (ReflectionException e) {
                logger.error(e);
            }
            tried.put(cb, true);
        }
        return elements.get(cb);
    }
}
