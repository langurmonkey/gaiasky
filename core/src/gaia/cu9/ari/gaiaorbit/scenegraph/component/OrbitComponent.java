/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

public class OrbitComponent {

    /** Source file **/
    public String source;
    /** Orbital period in days **/
    public double period;
    /** Base epoch **/
    public double epoch;
    /** Semi major axis of the ellipse, a in Km.**/
    public double semimajoraxis;
    /** Eccentricity of the ellipse. **/
    public double e;
    /** Inclination, angle between the reference plane (ecliptic) and the orbital plane. **/
    public double i;
    /** Longitude of the ascending node in degrees. **/
    public double ascendingnode;
    /** Argument of perihelion in degrees. **/
    public double argofpericenter;
    /** Mean anomaly at epoch, in degrees. **/
    public double meananomaly;
    /** G*M of central body (gravitational constant). Defaults to the Sun's **/
    public double mu = 1.32712440041e20;

    public OrbitComponent() {

    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPeriod(Double period) {
        this.period = period;
    }

    public void setEpoch(Double epoch) {
        this.epoch = epoch;
    }

    public void setSemimajoraxis(Double semimajoraxis) {
        this.semimajoraxis = semimajoraxis;
    }

    public void setEccentricity(Double e) {
        this.e = e;
    }

    public void setInclination(Double i) {
        this.i = i;
    }

    public void setAscendingnode(Double ascendingnode) {
        this.ascendingnode = ascendingnode;
    }

    public void setArgofpericenter(Double argofpericenter) {
        this.argofpericenter = argofpericenter;
    }

    public void setMeananomaly(Double meanAnomaly) {
        this.meananomaly = meanAnomaly;
    }

    public void setMu(Double mu) {
        this.mu = mu;
    }

}
