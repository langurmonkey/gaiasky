/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.Particle;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.Star;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Vector3b;

import java.io.*;

/**
 * Loads and writes particle data to/from our own binary format. The format is
 * defined as follows. The operations are based on abstract position entities, so
 * this is not suitable for loading star groups or particle groups.
 *
 * <ul>
 * <li>32 bits (int) with the number of stars, starNum repeat the following
 * starNum times (for each star)</li>
 * <li>32 bits (int) - The the length of the name, or nameLength</li>
 * <li>16 bits * nameLength (chars) - The name of the star</li>
 * <li>32 bits (float) - appmag</li>
 * <li>32 bits (float) - absmag</li>
 * <li>32 bits (float) - colorbv</li>
 * <li>32 bits (float) - r</li>
 * <li>32 bits (float) - g</li>
 * <li>32 bits (float) - b</li>
 * <li>32 bits (float) - a</li>
 * <li>32 bits (float) - ra [deg]</li>
 * <li>32 bits (float) - dec [deg]</li>
 * <li>32 bits (float) - distance [u]</li>
 * <li>64 bits (double) - x [u]</li>
 * <li>64 bits (double) - y [u]</li>
 * <li>64 bits (double) - z [u]</li>
 * <li>32 bits (float) - mualpha [mas/yr]</li>
 * <li>32 bits (float) - mudelta [mas/yr]</li>
 * <li>32 bits (float) - radvel [km/s]</li>
 * <li>32 bits (float) - pmx [u/yr]</li>
 * <li>32 bits (float) - pmy [u/yr]</li>
 * <li>32 bits (float) - pmz [u/yr]</li>
 * <li>64 bits (long) - id</li>
 * <li>32 bits (int) - HIP</li>
 * <li>32 bits (int) - TYClength</li>
 * <li>16 bits (char) * TYClength - TYC</li>
 * <li>8 bits (byte) - catalogSource</li>
 * <li>64 bits (long) - pageId</li>
 * <li>32 bits (int) - particleType</li>
 * </ul>
 */
public class ParticleDataBinaryIO {
    private static final Log logger = Logger.getLogger(ParticleDataBinaryIO.class);

    public void writeParticles(Array<SceneGraphNode> particles, OutputStream out) {

        try {
            // Wrap the FileOutputStream with a DataOutputStream
            DataOutputStream data_out = new DataOutputStream(out);

            // Size of stars
            data_out.writeInt(particles.size);
            for (SceneGraphNode ape : particles) {
                if (ape instanceof Particle) {
                    Particle s = (Particle) ape;
                    // name_length, name, appmag, absmag, colorbv, r, g, b, a,
                    // ra[deg], dec[deg], dist[u], (double) x[u], (double) y[u],
                    // (double) z[u], mualpha[mas/yr], mudelta[mas/yr],
                    // radvel[km/s], pmx[u/yr], pmy[u/yr], pmz[u/yr], id, hip,
                    // tychoLength, tycho, sourceCatalog, pageid, type
                    String namesConcat = s.namesConcat();
                    data_out.writeInt(namesConcat.length());
                    data_out.writeChars(namesConcat);
                    data_out.writeFloat(s.appmag);
                    data_out.writeFloat(s.absmag);
                    data_out.writeFloat(s.colorbv);
                    data_out.writeFloat(s.cc[0]);
                    data_out.writeFloat(s.cc[1]);
                    data_out.writeFloat(s.cc[2]);
                    data_out.writeFloat(s.cc[3]);
                    data_out.writeFloat((float) s.posSph.x);
                    data_out.writeFloat((float) s.posSph.y);
                    data_out.writeFloat(s.pos.lenf());
                    data_out.writeDouble(s.pos.x.doubleValue());
                    data_out.writeDouble(s.pos.y.doubleValue());
                    data_out.writeDouble(s.pos.z.doubleValue());
                    data_out.writeFloat(s.pmSph != null ? s.pmSph.x : 0f);
                    data_out.writeFloat(s.pmSph != null ? s.pmSph.y : 0f);
                    data_out.writeFloat(s.pmSph != null ? s.pmSph.z : 0f);
                    data_out.writeFloat(s.pm.x);
                    data_out.writeFloat(s.pm.y);
                    data_out.writeFloat(s.pm.z);
                    data_out.writeLong(s.id);
                    data_out.writeInt(s instanceof Star ? ((Star) s).hip : -1);
                    // Tycho ID - obsolete
                    data_out.writeInt(0);
                    data_out.writeByte(s.catalogSource);
                    data_out.writeInt(s.octantId.intValue());
                    // TODO Legacy type, remove
                    data_out.writeInt(0);
                }
            }
            data_out.close();
            out.close();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public Array<SceneGraphNode> readParticles(InputStream in) throws FileNotFoundException {
        Array<SceneGraphNode> stars = null;
        DataInputStream data_in = new DataInputStream(in);

        try {
            // Read size of stars
            int size = data_in.readInt();
            stars = new Array<>(false, size);

            for (int idx = 0; idx < size; idx++) {
                try {
                    // name_length, name, appmag, absmag, colorbv, r, g, b, a,
                    // ra[deg], dec[deg], dist[u], (double) x[u], (double) y[u],
                    // (double) z[u], mualpha[mas/yr], mudelta[mas/yr],
                    // radvel[km/s], pmx[u/yr], pmy[u/yr], pmz[u/yr], id, hip,
                    // sourceCatalog, pageid, type
                    int nameLength = data_in.readInt();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < nameLength; i++) {
                        sb.append(data_in.readChar());
                    }
                    String namesConcat = sb.toString();

                    String[] names = namesConcat.split(Constants.nameSeparatorRegex);
                    float appmag = data_in.readFloat();
                    float absmag = data_in.readFloat();
                    float colorbv = data_in.readFloat();
                    float r = data_in.readFloat();
                    float g = data_in.readFloat();
                    float b = data_in.readFloat();
                    float a = data_in.readFloat();
                    float ra = data_in.readFloat();
                    float dec = data_in.readFloat();
                    float dist = data_in.readFloat();
                    double x = data_in.readDouble();
                    double y = data_in.readDouble();
                    double z = data_in.readDouble();
                    float mualpha = data_in.readFloat();
                    float mudelta = data_in.readFloat();
                    float radvel = data_in.readFloat();
                    float pmx = data_in.readFloat();
                    float pmy = data_in.readFloat();
                    float pmz = data_in.readFloat();
                    long id = data_in.readLong();
                    int hip = data_in.readInt();
                    int tychoLength = data_in.readInt();
                    sb = new StringBuilder();
                    for (int i = 0; i < tychoLength; i++) {
                        sb.append(data_in.readChar());
                    }
                    String tycho = sb.toString();

                    byte source = data_in.readByte();
                    long pageId = data_in.readInt();
                    int type = data_in.readInt();
                    Vector3b pos = new Vector3b(x, y, z);
                    Vector3 pmSph = new Vector3(mualpha, mudelta, radvel);
                    Vector3 pm = new Vector3(pmx, pmy, pmz);
                    float[] cc;

                    if (Float.isNaN(colorbv)) {
                        colorbv = 0.62f;
                        cc = new float[] { 1.0f, 0.95f, 0.91f, 1.0f };
                    } else {
                        cc = new float[] { r, g, b, a };
                    }

                    Star s = new Star(pos, pm, pmSph, appmag, absmag, colorbv, names, ra, dec, id, hip, source);
                    s.cc = cc;
                    s.octantId = pageId;
                    s.initialize();
                    stars.add(s);
                } catch (EOFException eof) {
                    logger.error(eof);
                }
            }

        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                data_in.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

        return stars;
    }
}
