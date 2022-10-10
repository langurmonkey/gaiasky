package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;

public class RefSysTransform implements Component {
    private static final Log logger = Logger.getLogger(RefSysTransform.class);
    public String transformName;
    public Matrix4d matrix;
    public Matrix4 matrixf;
    public boolean floatVersion = false;

    public void setTransformFunction(String transformFunction) {
        setTransformName(transformFunction);
    }

    public void setTransformName(String transformFunction) {
        setMatrix(transformFunction);
    }

    public void setMatrix(String transformName) {
        this.transformName = transformName;
        if (transformName != null && !transformName.isEmpty()) {
            try {
                Method m = ClassReflection.getMethod(Coordinates.class, transformName);
                Object obj = m.invoke(null);

                Matrix4d trf = null;
                if (obj instanceof Matrix4) {
                    trf = new Matrix4d(((Matrix4) obj).val);
                } else if (obj instanceof Matrix4d) {
                    trf = new Matrix4d((Matrix4d) obj);
                }
                this.matrix = trf;

                if (floatVersion && this.matrix != null) {
                    this.matrixf = this.matrix.putIn(new Matrix4());
                }
            } catch (Exception e) {
                logger.error(e);
            }
        } else {
            if (matrix != null) {
                matrix.idt();
            }
            if (matrixf != null) {
                matrixf.idt();
            }
        }
    }

    /**
     * Constructs the transformation matrix from a double array containing
     * the values in a column-major order (first the four values of the first
     * column, then the second, etc.). The double array
     * must have at least 16 elements; the first 16 will be copied.
     **/
    public void setTransformValues(double[] transformValues) {
        this.matrix = new Matrix4d(transformValues);
    }
}
