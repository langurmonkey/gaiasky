/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaia.cu9.ari.gaiaorbit.assets.ShaderTemplatingLoader;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

public final class ShaderLoader {
    private static Log logger = Logger.getLogger(ShaderLoader.class);

    public static String BasePath = "";
    public static boolean Pedantic = true;

    public static ShaderProgram fromFile(String vertexFileName, String fragmentFileName) {
        return ShaderLoader.fromFile(vertexFileName, fragmentFileName, "");
    }

    public static ShaderProgram fromFile(String vertexFileName, String fragmentFileName, String defines) {
        String log = "\"" + vertexFileName + "/" + fragmentFileName + "\"";
        if (defines.length() > 0) {
            log += " w/ (" + defines.replace("\n", ", ") + ")";
        }
        log += "...";
        logger.debug("Compiling " + log);

        String vpSrc = Gdx.files.internal(BasePath + vertexFileName + ".vertex").readString();
        String fpSrc = Gdx.files.internal(BasePath + fragmentFileName + ".fragment").readString();

        // Resolve includes
        vpSrc = ShaderTemplatingLoader.resolveIncludes(vpSrc);
        fpSrc = ShaderTemplatingLoader.resolveIncludes(fpSrc);

        ShaderProgram program = ShaderLoader.fromString(vpSrc, fpSrc, vertexFileName, fragmentFileName, defines);
        return program;
    }

    public static ShaderProgram fromString(String vertex, String fragment, String vertexName, String fragmentName) {
        return ShaderLoader.fromString(vertex, fragment, vertexName, fragmentName, "");
    }

    public static ShaderProgram fromString(String vertex, String fragment, String vertexName, String fragmentName, String defines) {
        ShaderProgram.pedantic = ShaderLoader.Pedantic;
        ShaderProgram shader = new ShaderProgram(insertDefines(vertex, defines), insertDefines(fragment, defines));

        if (!shader.isCompiled()) {
            logger.error("Compile error: " + vertexName + "/" + fragmentName);
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
                if(line.trim().startsWith("#version ")){
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
