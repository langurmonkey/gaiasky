/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WarpMeshReader {

    static public boolean isValidWarpMeshAscii(Path file) {
        if (file != null && Files.exists(file) && Files.isRegularFile(file) && Files.isReadable(file)) {

            try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                var sep = "\\s+";
                // Type.
                var type = Parser.parseIntException(reader.readLine().trim());
                // Dimensions.
                String line = reader.readLine();
                var tokens = line.split(sep);
                var nx = Parser.parseIntException(tokens[0].trim());
                var ny = Parser.parseIntException(tokens[1].trim());

                // Read first node.
                line = reader.readLine();
                tokens = line.split(sep);
                if(tokens.length >= 5) {
                    for (int i = 0; i < 5; i++) {
                        Parser.parseFloatException(tokens[i].trim());
                    }
                    return true;
                }

            } catch (Exception e) {
                return false;
            }

        }

        return false;
    }

    static public WarpMesh readWarpMeshAscii(FileHandle file) throws RuntimeException {
        if (file != null && file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file.file()))) {
                var sep = "\\s+";
                var mesh = new WarpMesh();

                // Mesh type.
                mesh.type = Parser.parseIntException(reader.readLine().trim());

                // Dimensions.
                String line = reader.readLine();
                var tokens = line.split(sep);
                mesh.nx = Parser.parseIntException(tokens[0].trim());
                mesh.ny = Parser.parseIntException(tokens[1].trim());

                // Actual mesh data.
                mesh.nodes = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    tokens = line.split(sep);
                    // Contains x, y, u, v, intensity
                    float[] values = new float[5];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = Parser.parseFloatException(tokens[i].trim());
                    }
                    mesh.nodes.add(values);
                }

                return mesh;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static class WarpMesh {
        public int type;
        public int nx, ny;
        public List<float[]> nodes;
    }
}
