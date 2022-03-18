/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop87;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class VSOP87 {
    private static final Log logger = Logger.getLogger(VSOP87.class);
    public static VSOP87 instance;
    static {
        instance = new VSOP87();
    }

    private final Map<String, iVSOP87> elements;
    private final Map<String, Boolean> tried;

    public VSOP87() {
        elements = new HashMap<>();
        tried = new HashMap<>();
    }

    public iVSOP87 getVOSP87(String cb) {
        if (!tried.containsKey(cb) || !tried.get(cb)) {
            // Initialize
            String pkg = "gaiasky.util.coord.vsop87.";
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
