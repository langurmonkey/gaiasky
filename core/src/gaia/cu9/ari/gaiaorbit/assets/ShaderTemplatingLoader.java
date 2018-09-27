package gaia.cu9.ari.gaiaorbit.assets;

import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * Loads shaders with extra functionality to add code from other shaders.
 * Recognizes the directive <INCLUDE shader.glsl> in glsl files.
 * @author tsagrista
 *
 */
public class ShaderTemplatingLoader {

    public static String load(String file) {
        FileHandle fh = Gdx.files.internal(file);
        return load(fh);
    }

    public static String load(FileHandle fh) {
        String in = fh.readString();

        StringBuffer sb = new StringBuffer();

        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.matches("\\s*<INCLUDE \\S+\\.glsl>\\s*")) {
                // Load file and include
                String inc = line.substring(line.indexOf("<INCLUDE") + 9, line.length() - 1);
                String incSource = ShaderTemplatingLoader.load(inc);
                sb.append(incSource);
                sb.append('\n');
            } else if (!line.isEmpty() && !line.startsWith("//")) {
                sb.append(line);
                sb.append('\n');
            }
        }
        scanner.close();
        return sb.toString();
    }

}
