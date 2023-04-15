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

/**
 * Reads warp mesh files as defined by Paul Bourke <a href="http://paulbourke.net/dataformats/meshwarp/">here</a>.
 *
 * <ol>
 * <li>First line contains the mesh type, (2) rectangular or (1) polar.</li>
 * <li>Second line contains two integers with the dimensions, nx and ny.</li>
 * <li>The following lines contain the nodes, nx times ny. Each line contains 5 values:
 * <ol>
 *     <li>Position x and y of the node in normalized coordinates. These coordinates can extend beyond the image bounds [0,1].</li>
 *     <li>Texture coordinates UV in [0,1]. These refer to the original input image. Values outside of [0,1] indicate that the node is not to be used.</li>
 *     <li>Multiplicative intensity (alpha?) for brightness compensation.</li>
 * </ol>
 *
 * </li>
 *
 * </ol>
 */
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