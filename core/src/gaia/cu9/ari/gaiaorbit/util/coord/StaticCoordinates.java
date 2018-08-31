package gaia.cu9.ari.gaiaorbit.util.coord;

import java.time.Instant;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import gaia.cu9.ari.gaiaorbit.scenegraph.Orbit;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * A position that never changes
 * 
 * @author Toni Sagrista
 *
 */
public class StaticCoordinates implements IBodyCoordinates {

    Vector3d position;
    String transformName;
    Matrix4d trf;

    @Override
    public void doneLoading(Object... params) {
        if (trf != null) {
            this.position.mul(trf);
        }
    }

    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant date, Vector3d out) {
        return out.set(position);
    }

    @Override
    public Vector3d getEclipticCartesianCoordinates(Instant date, Vector3d out) {
        return out.set(position);
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant date, Vector3d out) {
        return out.set(position);
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Matrix4d transform = (Matrix4d) m.invoke(null);

                trf = new Matrix4d(transform);

            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        } else {
            // Equatorial, nothing
        }
    }

    public void setTransformMatrix(double[] transformMatrix) {
        trf = new Matrix4d(transformMatrix);
    }

    public void setPosition(double[] position) {
        this.position = new Vector3d(position[0] * Constants.KM_TO_U, position[1] * Constants.KM_TO_U, position[2] * Constants.KM_TO_U);

    }

    @Override
    public Orbit getOrbitObject() {
        return null;
    }

}
