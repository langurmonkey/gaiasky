/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

public class RingComponent {
    public int divisions;
    public float innerRadius, outerRadius;

    public RingComponent() {

    }

    public void setInnerradius(Double innerRadius) {
        this.innerRadius = innerRadius.floatValue();
    }

    public void setOuterradius(Double outerRadius) {
        this.outerRadius = outerRadius.floatValue();
    }

    public void setDivisions(Long divisions) {
        this.divisions = divisions.intValue();
    }

}
