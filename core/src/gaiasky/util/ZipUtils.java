/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
    private static final Logger.Log logger = Logger.getLogger(ZipUtils.class);

    public static void unzip(String zipFilePath, String destinationDir) throws IOException {
        File dir = new File(destinationDir);

        // Create output directory if it doesn't exist.
        if (!dir.exists() && dir.mkdirs())
            logger.debug("Output directory created: " + dir);

        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(destinationDir + File.separator + fileName);
            logger.info("Unzipping to " + newFile.getAbsolutePath());

            // Create directories for sub directories in zip.
            if (new File(newFile.getParent()).mkdirs()) {
                logger.debug("Directory created: " + newFile.getParent());
            }
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            //close this ZipEntry
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        //close last ZipEntry
        zis.closeEntry();
        zis.close();
        fis.close();

    }
}
