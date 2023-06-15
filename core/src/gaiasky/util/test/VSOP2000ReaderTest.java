/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.test;

import gaiasky.util.coord.vsop2000.VSOP2000Reader;

import java.nio.file.Path;

public class VSOP2000ReaderTest {

    public static void main(String[] args) {
        Path moonFile = Path.of("/home/tsagrista/Downloads/VSOP2000/vsop2000-p11.dat");
        VSOP2000Reader reader = new VSOP2000Reader();
        try {
            var data = reader.read(moonFile);
            System.out.println("Read file: " + moonFile.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
