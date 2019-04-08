/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.coord.vsop87;

public interface iVSOP87 {

    public double L0(double t);

    public double L1(double t);

    public double L2(double t);

    public double L3(double t);

    public double L4(double t);

    public double L5(double t);

    public double B0(double t);

    public double B1(double t);

    public double B2(double t);

    public double B3(double t);

    public double B4(double t);

    public double B5(double t);

    public double R0(double t);

    public double R1(double t);

    public double R2(double t);

    public double R3(double t);

    public double R4(double t);

    public double R5(double t);

    public void setHighAccuracy(boolean highAccuracy);
}
