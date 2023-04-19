/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.nio.file.Path;

public class CompressTextures {

    /**
     * Compresses all PNG and JPG files in the given location into ETC1A-compressed ZKTX files.
     *
     * @param args The directory to process.
     */
    public static void main(String[] args) {

        CLIArgs cliArgs = new CLIArgs();
        JCommander jc = JCommander.newBuilder().addObject(cliArgs).build();
        jc.setProgramName("texturecompressor");
        try {
            jc.parse(args);

            if (cliArgs.help) {
                printUsage(jc);
                return;
            }
        } catch (Exception e) {
            System.out.print("gaiasky: bad program arguments\n\n");
            printUsage(jc);
        }

        var loc = Path.of(cliArgs.location);
        //if (Files.exists(loc)) {
        //    try (Stream<Path> list = Files.list(loc)) {
        //        list.forEach((entry) -> {
        //            var file = entry.toFile();
        //            if (file.exists() && file.isFile() && file.canRead()) {
        //                var fileName = entry.toAbsolutePath().toString();
        //                if (fileName.toLowerCase().endsWith(".jpg")
        //                        || fileName.toLowerCase().endsWith(".jpeg")
        //                        || fileName.toLowerCase().endsWith(".png")) {
        //                    try {
        //                        var outName = fileName.substring(0, fileName.lastIndexOf('.')) + ".zktx";
        //                        var outFile = Path.of(outName).toFile();
        //                        if (outFile.exists()) {
        //                            // Delete!
        //                            FileUtils.delete(outFile);
        //                        }
        //                        KTXProcessor.convert(fileName, outName, false, true, false);
        //                    } catch (Exception e) {
        //                        System.out.println("Error: " + e);
        //                    }
        //                }
        //            }
        //        });
        //    } catch (IOException e) {
        //        System.out.println("Error: " + e);
        //    }
        //} else {
        //    System.out.println("Location does not exist: " + loc);
        //}
    }

    private static void printUsage(JCommander jc) {
        jc.usage();
    }

    /**
     * Program CLI arguments.
     */
    private static class CLIArgs {
        @Parameter(names = { "-h", "--help" }, description = "Show program options and usage information.", help = true, order = 0) private boolean help = false;

        @Parameter(names = { "-l", "--location" }, description = "Specify the directory to process.", order = 1, required = true) private String location = null;
    }
}
