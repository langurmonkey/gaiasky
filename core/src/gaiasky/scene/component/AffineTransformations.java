package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.scenegraph.component.RotateTransform;
import gaiasky.scenegraph.component.ScaleTransform;
import gaiasky.scenegraph.component.TranslateTransform;
import gaiasky.util.math.Matrix4d;

public class AffineTransformations implements Component {

    /** Affine transformations, applied each cycle **/
    public Array<ITransform> transformations;

    public void setTransformations(Object[] transformations) {
        initArray();
        for (int i = 0; i < transformations.length; i++)
            this.transformations.add((ITransform) transformations[i]);
    }

    private void initArray() {
        if (this.transformations == null) {
            this.transformations = new Array<>(3);
        }
    }

    public void setTranslate(double[] translation) {
        initArray();
        TranslateTransform tt = new TranslateTransform();
        tt.setVector(translation);
        this.transformations.add(tt);
    }

    public void setRotate(double[] axisDegrees) {
        initArray();
        RotateTransform rt = new RotateTransform();
        rt.setAxis(new double[]{axisDegrees[0], axisDegrees[1], axisDegrees[2]});
        rt.setAngle(axisDegrees[3]);
        this.transformations.add(rt);
    }

    public void setScale(double[] sc) {
        initArray();
        ScaleTransform st = new ScaleTransform();
        st.setScale(sc);
        this.transformations.add(st);
    }

    public Matrix4 apply(Matrix4 mat){
        if(transformations != null) {
            for (ITransform tr : transformations) {
                tr.apply(mat);
            }
        }
        return mat;
    }

    public Matrix4d apply(Matrix4d mat){
        if(transformations != null) {
            for (ITransform tr : transformations) {
                tr.apply(mat);
            }
        }
        return mat;
    }
}
