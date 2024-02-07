/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scene.record.*;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;

import java.util.Vector;

/**
 * Represents an arbitrarily large sequence of affine transformations as a list of 4x4 matrices.
 */
public class AffineTransformations implements Component {

    /** Affine transformations, applied each cycle **/
    public Vector<ITransform> transformations;

    public synchronized void setTransformations(Object[] transformations) {
        initialize();
        for (Object transformation : transformations) {
            this.transformations.add((ITransform) transformation);
        }
    }

    public synchronized void setTransformations(Vector<ITransform> transformations) {
        this.transformations = new Vector<>(transformations);
    }

    public synchronized void initialize() {
        if (this.transformations == null) {
            this.transformations = new Vector<>(3, 2);
        }
    }

    public synchronized void clear() {
        if (this.transformations != null) {
            this.transformations.clear();
        }
    }

    public boolean isEmpty() {
        return transformations == null || transformations.isEmpty();
    }

    /**
     * Replace the transformation at the current index with the given one.
     * @param newTransform The new transformation.
     * @param index The index.
     */
    public void replace(ITransform newTransform, int index) {
        if(transformations != null && !transformations.isEmpty() && index >= 0 && index < transformations.size()) {
            transformations.set(index, newTransform);
        }
    }

    /**
     * Sets a generic 4x4 transformation matrix in the chain.
     *
     * @param matrix The matrix values in column-major order.
     */
    public synchronized void setMatrix(double[] matrix) {
        initialize();
        MatrixTransform mt = new MatrixTransform(matrix);
        this.transformations.add(mt);
    }

    public synchronized void setTransformMatrix(double[] matrix) {
        setMatrix(matrix);
    }

    public synchronized void setTranslate(double[] translation) {
        initialize();
        TranslateTransform tt = new TranslateTransform();
        tt.setVector(translation);
        this.transformations.add(tt);
    }

    public synchronized void setTranslatePc(double[] translation) {
        double[] iu = new double[3];
        iu[0] = translation[0] * Constants.PC_TO_U;
        iu[1] = translation[1] * Constants.PC_TO_U;
        iu[2] = translation[2] * Constants.PC_TO_U;
        setTranslate(iu);
    }

    public synchronized void setTranslateKm(double[] translation) {
        double[] iu = new double[3];
        iu[0] = translation[0] * Constants.KM_TO_U;
        iu[1] = translation[1] * Constants.KM_TO_U;
        iu[2] = translation[2] * Constants.KM_TO_U;
        setTranslate(iu);
    }

    public synchronized void setQuaternion(double[] axis, double angle) {
        initialize();
        QuaternionTransform qt = new QuaternionTransform();
        qt.setQuaternion(new Vector3d(axis), angle);
        this.transformations.add(qt);
    }

    public synchronized void setQuaternion(QuaternionDouble q) {
        initialize();
        QuaternionTransform qt = new QuaternionTransform();
        qt.setQuaternion(q);
        this.transformations.add(qt);
    }

    public synchronized void setRotate(double[] axisDegrees) {
        initialize();
        RotateTransform rt = new RotateTransform();
        rt.setAxis(new double[]{axisDegrees[0], axisDegrees[1], axisDegrees[2]});
        rt.setAngle(axisDegrees[3]);
        this.transformations.add(rt);
    }

    public synchronized void setScale(double[] sc) {
        initialize();
        ScaleTransform st = new ScaleTransform();
        st.setScale(sc);
        this.transformations.add(st);
    }

    public synchronized void setScale(Double sc) {
        initialize();
        ScaleTransform st = new ScaleTransform();
        st.setScale(new double[]{sc, sc, sc});
        this.transformations.add(st);
    }

    public synchronized Matrix4 apply(Matrix4 mat) {
        if (transformations != null) {
            for (ITransform tr : transformations) {
                tr.apply(mat);
            }
        }
        return mat;
    }

    public synchronized Matrix4d apply(Matrix4d mat) {
        if (transformations != null) {
            for (ITransform tr : transformations) {
                tr.apply(mat);
            }
        }
        return mat;
    }

    public synchronized ScaleTransform getScaleTransform() {
        if (this.transformations != null) {
            for (ITransform t : transformations) {
                if (t instanceof ScaleTransform) {
                    return (ScaleTransform) t;
                }
            }
        }
        return null;
    }

    public synchronized RotateTransform getRotateTransform() {
        if (this.transformations != null) {
            for (ITransform t : transformations) {
                if (t instanceof RotateTransform) {
                    return (RotateTransform) t;
                }
            }
        }
        return null;
    }

    public synchronized TranslateTransform getTranslateTransform() {
        if (this.transformations != null) {
            for (ITransform t : transformations) {
                if (t instanceof TranslateTransform) {
                    return (TranslateTransform) t;
                }
            }
        }
        return null;
    }

    /**
     * Produces a deep copy of the current affine transformations object.
     *
     * @return A deep copy of this object.
     */
    public synchronized AffineTransformations deepCopy() {
        var copy = new AffineTransformations();
        if (this.transformations != null) {
            copy.transformations = new Vector<>(this.transformations.size(), 2);
            for (var trf : this.transformations) {
                copy.transformations.add(trf.copy());
            }
        }
        return copy;
    }
}
