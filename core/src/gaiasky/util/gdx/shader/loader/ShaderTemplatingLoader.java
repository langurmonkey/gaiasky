/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.Constants;
import gaiasky.util.Settings;

import java.util.Scanner;

public class ShaderTemplatingLoader {

    private static final String GLSL_EXTENSION = "(glsl|glslv|glslf|vs|fs|gs|vert|frag|vsh|fsh)";
    private static final String FILE_REFERENCE_REGEXP = "[^<>]+\\." + GLSL_EXTENSION;
    private static final String FILE_REFERENCE_BRACKET_REGEXP = "<" + FILE_REFERENCE_REGEXP + ">";
    private static final String INCLUDE_STATEMENT_REGEXP = "^\\s*#include\\s+(" + FILE_REFERENCE_REGEXP + "|" + FILE_REFERENCE_BRACKET_REGEXP + ")\\s*$";

    public static String load(String file) {
        if (file.matches(FILE_REFERENCE_BRACKET_REGEXP)) {
            // Strip brackets.
            file = file.replace("<", "").replace(">", "");
        }
        FileHandle fh;
        if(file.startsWith(Constants.DATA_LOCATION_TOKEN)) {
            // Dataset.
            fh = Settings.settings.data.dataFileHandle(file);
        } else {
            // Internal file (assets directory).
            fh = Gdx.files.internal(file);
        }
        return load(fh);
    }

    public static String load(FileHandle fh) {
        if (fh == null) {
            return null;
        }
        String in = fh.readString();
        return resolveIncludes(in);
    }

    public static String resolveIncludes(String in) {
        final StringBuilder stringBuilder = new StringBuilder();

        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.matches(INCLUDE_STATEMENT_REGEXP)) {
                // Load file and include.
                String inc = line.substring(line.indexOf("#include") + 9);
                String incSource = ShaderTemplatingLoader.load(inc.strip());
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
