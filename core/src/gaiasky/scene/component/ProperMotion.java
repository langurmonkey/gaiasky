package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.coord.AstroUtils;

/**
 * Proper motion component.
 */
public class ProperMotion implements Component, ICopy {

    /** Proper motion epoch in julian days. Defaults to J2015.5. **/
    public double epochJd = AstroUtils.JD_J2015_5;

    /**
     * Proper motion in cartesian equatorial internal coordinates [U/yr].
     **/
    public Vector3 pm;

    /**
     * MuAlpha* [mas/yr], MuDelta [mas/yr], RadVel [km/s].
     **/
    public Vector3 pmSph;

    /** This flag is up if pm is not null, and it is not zero. **/
    public boolean hasPm;

    public void setEpochJd(Double epochJd) {
        this.epochJd = epochJd;
    }

    /**
     * Sets the epoch as a Gregorian calendar year and a fraction (i.e. 2015.5).
     *
     * @param epochYear The Gregorian calendar year and fraction, as a double.
     */
    public void setEpochYear(Double epochYear) {
        this.epochJd = AstroUtils.getJulianDate(epochYear);
    }

    public void initPm() {
        if (pmSph == null) {
            pmSph = new Vector3();
        }
        if (pm == null) {
            pm = new Vector3();
        }
    }

    public void setMuAlpha(Double muAlpha){
        setMuAlphaMasYr(muAlpha);
    }

    public void setMuAlphaMasYr(Double muAlpha) {
        initPm();
        pmSph.x = muAlpha.floatValue();
    }

    public void setMuDelta(Double muDelta){
        setMuDeltaMasYr(muDelta);
    }

    public void setMuDeltaMasYr(Double muDelta) {
        initPm();
        pmSph.y = muDelta.floatValue();
    }

    public void setVr(Double rv) {
       setRadialVelocityKms(rv);
    }

    public void setVrKms(Double rv) {
        setRadialVelocityKms(rv);
    }
    public void setRadialVelocity(Double rv) {
        setRadialVelocityKms(rv);
    }

    public void setRadialVelocityKms(Double rv) {
        initPm();
        pmSph.z = rv.floatValue();
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.pm = new Vector3(pm);
        copy.pmSph = new Vector3(pmSph);
        copy.hasPm = hasPm;
        return copy;
    }
}
