/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 * Original code by bmanuel
 */

package gaiasky.util.gdx.contrib.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ShaderLoader {
    private static final Log logger = Logger.getLogger(ShaderLoader.class);

    public static String BasePath = "";
    public static boolean Pedantic = true;

    public static ShaderProgram fromFile(String vertexFileName, String fragmentFileName) {
        return ShaderLoader.fromFile(vertexFileName, fragmentFileName, "");
    }

    public static ShaderProgram fromFile(String vertexFileName, String fragmentFileName, String defines) {
        String log = "\"" + vertexFileName + " / " + fragmentFileName + "\"";
        if (defines.length() > 0) {
            log += " w/ (" + defines.replace("\n", ", ") + ")";
        }
        log += "...";
        logger.debug("Compiling " + log);

        String vpSrc = loadShaderCode(vertexFileName, "vert", "vertex", "vert.glsl");
        String fpSrc = loadShaderCode(fragmentFileName, "frag", "fragment", "frag.glsl");

        // Resolve includes
        vpSrc = ShaderTemplatingLoader.resolveIncludes(vpSrc);
        fpSrc = ShaderTemplatingLoader.resolveIncludes(fpSrc);

        return ShaderLoader.fromString(vpSrc, fpSrc, vertexFileName, fragmentFileName, defines);
    }

    private static String loadShaderCode(String fileName, String... extensions) {
        for(String extension : extensions) {
            String file = fileName + "." + extension;
            try {
                return Gdx.files.internal(BasePath + file).readString();
            } catch (Exception e0) {
                // Try to load from data
                Path path = Settings.settings.data.dataPath(file);
                if (Files.exists(path) && Files.isReadable(path)) {
                    try {
                        return Files.readString(path);
                    } catch (Exception e1) {
                        continue;
                    }
                }
            }
        }
        return null;

    }

    public static ShaderProgram fromString(String vertex, String fragment, String vertexName, String fragmentName) {
        return ShaderLoader.fromString(vertex, fragment, vertexName, fragmentName, "");
    }

    public static ShaderProgram fromString(String vertex, String fragment, String vertexName, String fragmentName, String defines) {
        ShaderProgram.pedantic = ShaderLoader.Pedantic;
        ShaderProgram shader = new ShaderProgram(insertDefines(vertex, defines), insertDefines(fragment, defines));

        if (!shader.isCompiled()) {
            logger.error("Compile error: " + vertexName + " / " + fragmentName);
            logger.error(shader.getLog());
            System.exit(-1);
        }

        return shader;
    }

    private static String insertDefines(String shader, String defines) {
        // Insert defines after #version directive, if exists
        if (shader.contains("#version ")) {
            String[] lines = shader.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append("\n");
                if (line.trim().startsWith("#version ")) {
                    sb.append(defines).append("\n");
                }
            }
            return sb.toString();
        } else {
            return defines + "\n" + shader;
        }
    }

    private ShaderLoader() {
    }
}
