package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;

import java.util.Locale;

public class Mesh implements Component {
    // Shading mode
    public MeshShading shading = MeshShading.ADDITIVE;
    public Matrix4 coordinateSystem;

    public void setAdditiveBlending(Boolean additive) {
        if (additive)
            shading = MeshShading.ADDITIVE;
        else
            shading = MeshShading.DUST;
    }

    public void setAdditiveblending(Boolean additive) {
        setAdditiveBlending(additive);
    }

    public void setShading(String shadingStr) {
        shadingStr = shadingStr.toUpperCase(Locale.ROOT);
        try {
            shading = MeshShading.valueOf(shadingStr);
        } catch (IllegalArgumentException e) {
            shading = MeshShading.ADDITIVE;
        }
    }

    public enum MeshShading {
        REGULAR,
        DUST,
        ADDITIVE
    }
}
