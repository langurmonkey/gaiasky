package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.record.*;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

/**
 * Provides an arbitrary number of affine transformations (rotate, scale, translate) to be applied
 * to matrices.
 */
public class AffineTransformations implements Component {

    /** Affine transformations, applied each cycle **/
    public Array<ITransform> transformations;

    public void setTransformations(Object[] transformations) {
        initialize();
        for (Object transformation : transformations) {
            this.transformations.add((ITransform) transformation);
        }
    }

    public void initialize() {
        if (this.transformations == null) {
            this.transformations = new Array<>(3);
        }
    }

    public void setTranslate(double[] translation) {
        initialize();
        TranslateTransform tt = new TranslateTransform();
        tt.setVector(translation);
        this.transformations.add(tt);
    }

    public void setTranslatePc(double[] translation) {
        double[] iu = new double[3];
        iu[0] = translation[0] * Constants.PC_TO_U;
        iu[1] = translation[1] * Constants.PC_TO_U;
        iu[2] = translation[2] * Constants.PC_TO_U;
        setTranslate(iu);
    }

    public void setTranslateKm(double[] translation) {
        double[] iu = new double[3];
        iu[0] = translation[0] * Constants.KM_TO_U;
        iu[1] = translation[1] * Constants.KM_TO_U;
        iu[2] = translation[2] * Constants.KM_TO_U;
        setTranslate(iu);
    }

    public void setQuaternion(double[] axis, double angle) {
        initialize();
        QuaternionTransform qt = new QuaternionTransform();
        qt.setQuaternion(new Vector3d(axis), angle);
        this.transformations.add(qt);
    }

    public void setRotate(double[] axisDegrees) {
        initialize();
        RotateTransform rt = new RotateTransform();
        rt.setAxis(new double[] { axisDegrees[0], axisDegrees[1], axisDegrees[2] });
        rt.setAngle(axisDegrees[3]);
        this.transformations.add(rt);
    }

    public void setScale(double[] sc) {
        initialize();
        ScaleTransform st = new ScaleTransform();
        st.setScale(sc);
        this.transformations.add(st);
    }

    public void setScale(Double sc) {
        initialize();
        ScaleTransform st = new ScaleTransform();
        st.setScale(new double[] { sc, sc, sc });
        this.transformations.add(st);
    }

    public Matrix4 apply(Matrix4 mat) {
        if (transformations != null) {
            for (ITransform tr : transformations) {
                tr.apply(mat);
            }
        }
        return mat;
    }

    public Matrix4d apply(Matrix4d mat) {
        if (transformations != null) {
            for (ITransform tr : transformations) {
                tr.apply(mat);
            }
        }
        return mat;
    }

    public ScaleTransform getScaleTransform() {
        if (this.transformations != null) {
            for (ITransform t : transformations) {
                if (t instanceof ScaleTransform) {
                    return (ScaleTransform) t;
                }
            }
        }
        return null;
    }

    public RotateTransform getRotateTransform() {
        if (this.transformations != null) {
            for (ITransform t : transformations) {
                if (t instanceof RotateTransform) {
                    return (RotateTransform) t;
                }
            }
        }
        return null;
    }

    public TranslateTransform getTranslateTransform() {
        if (this.transformations != null) {
            for (ITransform t : transformations) {
                if (t instanceof TranslateTransform) {
                    return (TranslateTransform) t;
                }
            }
        }
        return null;
    }
}
