/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.galaxy.GalaxyGenerator;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

public class PointDataWriter {
    private static final Logger.Log logger = Logger.getLogger(PointDataWriter.class);

    private static final String separator = " ";

    public void writeData(Array<IParticleRecord> particles, String filePath, boolean overwrite) throws Exception {
        if (particles != null && particles.size > 0) {
            FileHandle fh = new FileHandle(filePath);
            File f = fh.file();
            if (fh.exists() && f.isFile()) {
                if (overwrite) {
                    fh.delete();
                } else {
                    logger.info(I18n.msg("error.file.exists", filePath));
                    return;
                }
            }

            if (fh.isDirectory()) {
                throw new RuntimeException(I18n.msg("error.file.isdir", filePath));
            }
            f.createNewFile();

            int lineLen = particles.get(0).rawDoubleData().length;

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filePath))));

            // HEADER
            // POS
            bw.write("X" + separator + "Y" + separator + "Z");
            if (lineLen > 3) {
                // SIZE
                bw.write(separator + "size");
            }
            if (lineLen > 4) {
                // COL
                bw.write(separator + "r" + separator + "g" + separator + "b");
            }
            bw.newLine();

            int n = particles.size;
            for (int i = 0; i < n; i++) {
                double[] star = particles.get(i).rawDoubleData();
                StringBuilder sb = new StringBuilder();
                sb.append(star[0]);
                for (int j = 1; j < star.length; j++) {
                    sb.append(separator);
                    sb.append(star[j]);
                }

                bw.write(sb.toString());
                bw.newLine();
            }

            bw.close();

            Logger.getLogger(GalaxyGenerator.class).info(I18n.msg("notif.written", n, filePath));
        }

    }
}
