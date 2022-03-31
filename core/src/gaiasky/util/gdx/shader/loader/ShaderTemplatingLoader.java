/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.Scanner;

/**
 * Loads shaders with extra functionality to add code from other shaders.
 * Recognizes the directive #include shader.glsl in <code>.glsl</code> files.
 */
public class ShaderTemplatingLoader {

    public static String load(String file) {
        FileHandle fh = Gdx.files.internal(file);
        return load(fh);
    }

    public static String load(FileHandle fh) {
        String in = fh.readString();
        return resolveIncludes(in);
    }

    public static String resolveIncludes(String in){
        final StringBuilder stringBuilder = new StringBuilder();

        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.matches("\\s*#include\\s+\\S+\\.glsl\\s*")) {
                // Load file and include
                String inc = line.substring(line.indexOf("#include") + 9);
                String incSource = ShaderTemplatingLoader.load(inc);
                stringBuilder.append(incSource);
                stringBuilder.append('\n');
            } else if (!line.isEmpty() && !line.startsWith("//")) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        }
        scanner.close();
        return stringBuilder.toString();
    }

}
