/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.elements.OsculatingElements;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class StateVector2Elements {

    public static void main(String[] args) {
        try {
            Array<SSO> objects = new Array<>();

            // Read CSV file.
            System.out.println("Loading and converting...");
            Scanner sc = new Scanner(new File("/media/tsagrista/Daten/Gaia/data/sso/FPR/FPR-SSO-result.csv"));
            sc.useDelimiter(",|\n");
            int headerTokens = 10;
            while (sc.hasNext()) {
                if (headerTokens == 0) {
                    var object = new SSO();
                    object.numberMp = Parser.parseIntException(sc.next());
                    object.name = sc.next();
                    object.oscEpoch = Parser.parseDoubleException(sc.next());

                    double x = Parser.parseDoubleException(sc.next());
                    double y = Parser.parseDoubleException(sc.next());
                    double z = Parser.parseDoubleException(sc.next());
                    Vector3d pos = new Vector3d(x, y, z);
                    double vx = Parser.parseDoubleException(sc.next());
                    double vy = Parser.parseDoubleException(sc.next());
                    double vz = Parser.parseDoubleException(sc.next());
                    Vector3d vel = new Vector3d(vx, vy, vz);

                    object.elements = new OsculatingElements(pos, vel);

                    object.sigma_a = Parser.parseDoubleException(sc.next());

                    objects.add(object);

                } else {
                    sc.next();
                    headerTokens--;
                }
            }
            sc.close();

            // Write elements to file.
            System.out.println("Writing output...");
            FileWriter writer = new FileWriter("/media/tsagrista/Daten/Gaia/data/sso/FPR/FPR-SSO-result.elements.csv");

            writer.append(SSO.getHeader()).append("\n");

            for (SSO object : objects) {
                writer.append(object.toString()).append("\n");
            }
            writer.close();

            System.out.println("Done.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SSO {
        int numberMp;
        String name;
        double oscEpoch;
        double sigma_a;
        OsculatingElements elements;

        public static String getHeader() {
            return "number_mp,name,epoch,x,y,z,vx,vy,vz,mu,e,i,ascendingnode,meananomaly,argofpericenter,semimajoraxis,sigma_a";
        }

        @Override
        public String toString() {
            return numberMp + "," +
                    name + "," +
                    oscEpoch + "," +
                    elements.getPositionAtPericenter().toString() + "," +
                    elements.getVelocityAtPericenter() + "," +
                    elements.getMu() + "," +
                    elements.getEccentricity() + "," +
                    elements.getInclination() * MathUtilsDouble.radDeg + "," +
                    elements.getNode() * MathUtilsDouble.radDeg + "," +
                    elements.getMeanAnomaly() * MathUtilsDouble.radDeg + "," +
                    elements.getPericenter() * MathUtilsDouble.radDeg + "," +
                    elements.getSemiMajorAxis() + "," +
                    sigma_a;
        }
    }
}
