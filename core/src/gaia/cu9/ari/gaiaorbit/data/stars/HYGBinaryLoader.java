package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Loads the HYG catalog in binary (own) format. The format is defined as
 * follows
 *
 * <ul>
 * <li>32 bits (int) with the number of stars, starNum repeat the following
 * starNum times (for each star)</li>
 * <li>32 bits (int) - The the length of the name, or nameLength</li>
 * <li>16 bits * nameLength (chars) - The name of the star</li>
 * <li>32 bits (float) - appmag</li>
 * <li>32 bits (float) - absmag</li>
 * <li>32 bits (float) - colorbv</li>
 * <li>32 bits (float) - ra</li>
 * <li>32 bits (float) - dec</li>
 * <li>32 bits (float) - distance</li>
 * <li>32 bits (float) - proper motion x (internal units)</li>
 * <li>32 bits (float) - proper motion y (internal units)</li>
 * <li>32 bits (float) - proper motion z (internal units)</li>
 * <li>64 bits (long) - id</li>
 * </ul>
 *
 * @author Toni Sagrista
 *
 */
public class HYGBinaryLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(HYGBinaryLoader.class);

    @Override
    public Array<AbstractPositionEntity> loadData() throws FileNotFoundException {
        Array<AbstractPositionEntity> stars = new Array<AbstractPositionEntity>();
        for (String f : files) {
            FileHandle file = Gdx.files.internal(f);
            InputStream data = file.read();
            DataInputStream data_in = new DataInputStream(data);

            logger.info(I18n.bundle.format("notif.catalog.loading", file.name()));

            try {
                // Read size of stars
                int size = data_in.readInt();

                for (int idx = 0; idx < size; idx++) {
                    try {
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
                        id = -1l;
                        int hip = data_in.readInt();
                        if (appmag < GlobalConf.data.LIMIT_MAG_LOAD) {
                            Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());
                            Vector3 pmSph = new Vector3(mualpha, mudelta, radvel);
                            Vector3d pm = Coordinates.sphericalToCartesian(Math.toRadians(ra + mualpha * Nature.MILLARCSEC_TO_DEG), Math.toRadians(dec + mudelta * Nature.MILLARCSEC_TO_DEG), dist + radvel * Constants.KM_TO_U * Nature.S_TO_Y, new Vector3d());
                            pm.sub(pos);
                            Vector3 pmfloat = pm.toVector3();

                            Star s = new Star(pos, pmfloat, pmSph, appmag, absmag, colorbv, name, ra, dec, id, hip, null, (byte) 2);
                            if (runFiltersAnd(s))
                                stars.add(s);
                        }
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
        }

        logger.info(I18n.bundle.format("notif.catalog.init", stars.size));
        return stars;
    }

}
