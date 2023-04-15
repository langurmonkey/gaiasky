/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import org.apfloat.Apfloat;

import java.time.Instant;

/**
 * Provides a static position that never changes. If the entity using this
 * provider has a {@link gaiasky.scene.component.ProperMotion} component, this
 * is the position at epoch {@link gaiasky.scene.component.ProperMotion#epochJd}.
 */
public class StaticCoordinates implements IBodyCoordinates {

    private Vector3b position;
    private String transformName;
    private Matrix4d trf;

    @Override
    public void doneLoading(Object... params) {
        if (trf != null) {
            this.position.mul(trf);
        }
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        return out.set(position);
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        return out.set(position);
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        return out.set(position);
    }

    public void setTransformFunction(String transformName) {
        setTransformName(transformName);
    }

    public void setTransformName(String transformName) {
        this.transformName = transformName;
        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Object obj = m.invoke(null);

                if (obj instanceof Matrix4) {
                    trf = new Matrix4d(((Matrix4) obj).val);
                } else if (obj instanceof Matrix4d) {
                    trf = new Matrix4d((Matrix4d) obj);
                }

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

    public Vector3b getPosition() {
        return position;
    }

    public void setPosition(Vector3b pos) {
        this.position = new Vector3b(pos);
    }

    public void setPosition(double[] position) {
        setPositionKm(position);
    }

    public void setPositionKm(double[] position) {
        this.position = new Vector3b(position[0] * Constants.KM_TO_U, position[1] * Constants.KM_TO_U, position[2] * Constants.KM_TO_U);
    }

    public void setPositionkm(double[] position) {
        setPositionKm(position);
    }

    public void setPositionEquatorial(double[] position) {
        this.position = new Vector3b();
        this.position.set(Coordinates.sphericalToCartesian(position[0] * Nature.TO_RAD, position[1] * Nature.TO_RAD, position[2] * Constants.PC_TO_U, new Vector3d()));
    }

    public void setPositionGalactic(double[] position) {
        this.position = new Vector3b();
        this.position.set(Coordinates.sphericalToCartesian(position[0] * Nature.TO_RAD, position[1] * Nature.TO_RAD, position[2] * Constants.PC_TO_U, new Vector3d()));
        this.position.mul(Coordinates.galacticToEquatorial());
    }

    public void setPositionEcliptic(double[] position) {
        this.position = new Vector3b();
        this.position.set(Coordinates.sphericalToCartesian(position[0] * Nature.TO_RAD, position[1] * Nature.TO_RAD, position[2] * Constants.PC_TO_U, new Vector3d()));
        this.position.mul(Coordinates.eclipticToEquatorial());
    }

    public void setPositionPc(double[] position) {
        this.position = new Vector3b(position[0] * Constants.PC_TO_U, position[1] * Constants.PC_TO_U, position[2] * Constants.PC_TO_U);
    }

    public void setPositionpc(double[] position) {
        setPositionPc(position);
    }

    /**
     * Sets equatorial coordinates as a vector of [ra, de, distance]
     *
     * @param equatorial Vector with [ra, dec, distance] with angles in degrees and distance in parsecs
     */
    public void setEquatorial(double[] equatorial) {
        double ra = MathUtilsDouble.degRad * equatorial[0];
        double dec = MathUtilsDouble.degRad * equatorial[1];
        double dist = Constants.PC_TO_U * equatorial[2];
        this.position = new Vector3b();
        Coordinates.sphericalToCartesian(ra, dec, new Apfloat(dist), this.position);
    }

    @Override
    public String toString() {
        return "{" + "pos=" + position + ", trf='" + transformName + '\'' + '}';
    }
}
