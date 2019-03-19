package gaia.cu9.ari.gaiaorbit.desktop.util;

import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.util.Scanner;

public class EqGalTest {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter right ascension [deg]: ");
        double ra = readFloat(sc);
        System.out.print("Enter declination [deg]: ");
        double dec = readFloat(sc);
        double dist = 10;
        Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

        Vector3d posgal = new Vector3d(pos);
        posgal.mul(Coordinates.eqToGal());
        Vector3d posgalsph = Coordinates.cartesianToSpherical(posgal, new Vector3d());
        double l = posgalsph.x * MathUtilsd.radiansToDegrees;
        double b = posgalsph.y * MathUtilsd.radiansToDegrees;

        System.out.println("Galactic coordinates - l: " + l + ", b: " + b);
    }

    private static float readFloat(Scanner sc) {
        try {
            float val = sc.nextFloat();
            return val;
        } catch (Exception e) {
            System.err.println("Input is not a valid float");
            System.exit(1);
        }
        return 0;
    }
}
