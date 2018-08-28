package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.stars.HYGBinaryLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class HYGDataProvider extends AbstractStarGroupDataProvider {
    private static final boolean dumpToDisk = false;

    public HYGDataProvider() {
        super();
        countsPerMag = new long[22];
    }

    public Array<StarBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public Array<StarBean> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));

        FileHandle f = Gdx.files.internal(file);

        initLists(f);
        loadData(f.read(), factor);

        if (list != null)
            logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));

        return list;
    }

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        try {
            initLists();

            InputStream data = is;
            DataInputStream data_in = new DataInputStream(data);
            // Read size of stars
            int size = data_in.readInt();
            for (int idx = 0; idx < size; idx++) {
                try {
                    Set<String> treated = new HashSet<String>();
                    // name_length, name, appmag, absmag, colorbv, ra, dec,
                    // dist, mualpha, mudelta, radvel, id, hip
                    int nameLength = data_in.readInt();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < nameLength; i++) {
                        sb.append(data_in.readChar());
                    }
                    String name = sb.toString();
                    float appmag = data_in.readFloat();
                    float absmag = data_in.readFloat();
                    float colorbv = data_in.readFloat();
                    float ra = data_in.readFloat();
                    float dec = data_in.readFloat();
                    float dist = data_in.readFloat();
                    float mualpha = data_in.readFloat();
                    float mudelta = data_in.readFloat();
                    float radvel = data_in.readFloat();
                    long id = data_in.readInt();
                    id = -1l;// HIP stars with no gaia id go by 0
                    int hip = data_in.readInt();
                    if (appmag < GlobalConf.data.LIMIT_MAG_LOAD && !name.equals("Sol") && !treated.contains(name.toLowerCase())) {
                        treated.add(name.toLowerCase());

                        double flux = Math.pow(10, -absmag / 2.5f);
                        double starsize = Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / 1.5;

                        double rarad = Math.toRadians(ra);
                        double decrad = Math.toRadians(dec);

                        Vector3d pos = Coordinates.sphericalToCartesian(rarad, decrad, dist, new Vector3d());
                        Vector3d pm = AstroUtils.properMotionsToCartesian(mualpha, mudelta, radvel, rarad, decrad, dist * Constants.U_TO_PC);

                        float[] rgb = ColourUtils.BVtoRGB(colorbv);
                        double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);
                        colors.put(id, rgb);
                        sphericalPositions.put(id, new double[] { ra, dec, dist });

                        double[] point = new double[StarBean.SIZE];
                        point[StarBean.I_HIP] = hip;
                        point[StarBean.I_TYC1] = -1;
                        point[StarBean.I_TYC2] = -1;
                        point[StarBean.I_TYC3] = -1;
                        point[StarBean.I_X] = pos.x;
                        point[StarBean.I_Y] = pos.y;
                        point[StarBean.I_Z] = pos.z;
                        point[StarBean.I_PMX] = pm.x;
                        point[StarBean.I_PMY] = pm.y;
                        point[StarBean.I_PMZ] = pm.z;
                        point[StarBean.I_MUALPHA] = mualpha;
                        point[StarBean.I_MUDELTA] = mudelta;
                        point[StarBean.I_RADVEL] = radvel;
                        point[StarBean.I_COL] = col;
                        point[StarBean.I_SIZE] = starsize;
                        point[StarBean.I_APPMAG] = appmag;
                        point[StarBean.I_ABSMAG] = absmag;

                        list.add(new StarBean(point, id, name));

                        int appclmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                        countsPerMag[(int) appclmp] += 1;
                    }
                } catch (EOFException eof) {
                    logger.error(eof);
                    list = null;
                }
            }

            data_in.close();

            if (dumpToDisk) {
                dumpToDisk(list, "/tmp/hyg.bin", "bin");
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return list;
    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

}
