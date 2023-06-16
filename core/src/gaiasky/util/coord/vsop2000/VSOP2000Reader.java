/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord.vsop2000;

import gaiasky.util.parse.Parser;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Reads VSOP2000 data files.
 */
public class VSOP2000Reader {

    public VSOP2000Reader() {
        super();
    }

    /**
     * Reads the given file and returns an array of {@link VSOP2000Coordinate} with 3 positions, for X, Y and Z respectively.
     *
     * @param file The file, given as a path.
     *
     * @return An array of {@link VSOP2000Coordinate} with 3 positions for X, Y and Z.
     *
     * @throws FileNotFoundException If the file is not found.
     */
    public VSOP2000Coordinate[] read(Path file) throws FileNotFoundException {
        final VSOP2000Coordinate[] result = new VSOP2000Coordinate[3];
        final Scanner s = new Scanner(file.toFile());

        // Read each coordinate, X:0, Y:1, Z:2.
        for (int ci = 0; ci < 3; ci++) {
            // Read header.
            var h = s.nextLine().strip();
            var hTokens = h.split("\\s+");
            int bodyIndex = Parser.parseIntException(hTokens[1]);
            String bodyName = hTokens[2];
            String coordName = hTokens[3];

            var coord = new VSOP2000Coordinate();
            coord.idx = bodyIndex;
            coord.body = bodyName;
            coord.coordinate = coordName;
            result[ci] = coord;

            // Read 16 sizes, for each term.
            for (int ti = 0; ti < 16; ti++) {
                var t = s.nextLine().strip();
                var termSize = Parser.parseIntException(t);
                coord.numTerms[ti] = termSize;
                if (termSize > 0) {
                    coord.terms[ti] = new double[termSize][19];
                } else {
                    coord.terms[ti] = null;
                }
            }

            // Read records.
            for (int r = 0; r < 16; r++) {
                if(coord.numTerms[r] <= 0) {
                    continue;
                }
                // Skip k=r header.
                s.nextLine();
                // Read data lines.
                int numRecords = coord.numTerms[r];
                for (int line = 0; line < numRecords; line++) {
                    double[] record = coord.terms[r][line];
                    var l = s.nextLine();
                    // Skip index.
                    // int idx = Parser.parseIntException(l.substring(0, 7).trim());
                    // Per-body coefficients b_kij.
                    int pos = 8;
                    for (int iCoeff = 0; iCoeff < 17; iCoeff++) {
                        int len = iCoeff != 0 && iCoeff % 4 == 0 ? 4 : 3;
                        String coeffStr = l.substring(pos, pos + len).trim();
                        int coeff = Parser.parseIntException(coeffStr);
                        record[iCoeff] = coeff;
                        pos += len;
                    }

                    // Skip 3 zeroes (10 chars).
                    pos += 9;

                    // s_ki and c_ki
                    var sc = l.substring(pos).trim().split("\\s+");
                    double s_ki = Parser.parseDoubleException(sc[0].replace("D", "E"));
                    double c_ki = Parser.parseDoubleException(sc[1].replace("D", "E"));
                    record[17] = s_ki;
                    record[18] = c_ki;
                }
            }
        }

        return result;
    }

    public static class VSOP2000Coordinate {
        // Body index.
        int idx;
        // Body name.
        String body;
        // Coordinate name (X, Y or Z).
        String coordinate;
        // Number of terms per coordinate.
        int[] numTerms = new int[16];
        // Matrix containing the terms.
        // [0] - term records, up to 16.
        // [1] - number of records in the term.
        // [2] - each single record. 17*bkij, ski, cki -> 17 + 1 + 1 = 19.
        double[][][] terms = new double[16][][];
    }

}
