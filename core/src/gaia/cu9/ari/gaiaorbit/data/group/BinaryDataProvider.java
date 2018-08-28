package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.SysUtilsFactory;

/**
 * Reads arrays of star beans from binary files, usually to go in an octree.
 * 
 * @author tsagrista
 *
 */
public class BinaryDataProvider extends AbstractStarGroupDataProvider {

    @Override
    public Array<? extends ParticleBean> loadData(String file) {
        return loadData(file, 1d);
    }

    @Override
    public Array<? extends ParticleBean> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));
        loadDataMapped(file, factor);
        logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));

        return list;
    }

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        list = readData(is);
        return list;
    }

    public void writeData(Array<StarBean> data, OutputStream out) {
        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(out);
        try {
            // Size of stars
            data_out.writeInt(data.size);
            for (StarBean sb : data) {
                writeStarBean(sb, data_out);
            }

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try {
                data_out.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

    }

    protected void writeStarBean(StarBean sb, DataOutputStream out) throws IOException {
        // Double
        for (int i = 0; i < StarBean.I_APPMAG; i++) {
            out.writeDouble(sb.data[i]);
        }
        // Float
        for (int i = StarBean.I_APPMAG; i < StarBean.I_HIP; i++) {
            out.writeFloat((float) sb.data[i]);
        }
        // Int
        for (int i = StarBean.I_HIP; i < StarBean.SIZE; i++) {
            out.writeInt((int) sb.data[i]);
        }

        out.writeLong(sb.id);
        out.writeInt(sb.name.length());
        out.writeChars(sb.name);
    }

    public Array<StarBean> readData(InputStream in) {
        Array<StarBean> data = null;
        DataInputStream data_in = new DataInputStream(in);

        try {
            // Read size of stars
            int size = data_in.readInt();
            data = new Array<StarBean>(size);
            for (int i = 0; i < size; i++) {
                data.add(readStarBean(data_in));
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

        return data;
    }

    protected StarBean readStarBean(DataInputStream in) throws IOException {
        double data[] = new double[StarBean.SIZE];
        // Double
        for (int i = 0; i < StarBean.I_APPMAG; i++) {
            data[i] = in.readDouble();
        }
        // Float
        for (int i = StarBean.I_APPMAG; i < StarBean.I_HIP; i++) {
            data[i] = in.readFloat();
        }
        // Int
        for (int i = StarBean.I_HIP; i < StarBean.SIZE; i++) {
            data[i] = in.readInt();
        }

        Long id = in.readLong();
        int nameLength = in.readInt();
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < nameLength; i++)
            name.append(in.readChar());

        return new StarBean(data, id, name.toString());
    }

    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        try {
            FileChannel fc = new RandomAccessFile(SysUtilsFactory.getSysUtils().getTruePath(file), "r").getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            // Read size of stars
            int size = mem.getInt();
            list = new Array<StarBean>(size);
            for (int i = 0; i < size; i++) {
                list.add(readStarBean(mem));
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public StarBean readStarBean(MappedByteBuffer mem) {
        double data[] = new double[StarBean.SIZE];
        // Double
        for (int i = 0; i < StarBean.I_APPMAG; i++) {
            data[i] = mem.getDouble();
        }
        // Float
        for (int i = StarBean.I_APPMAG; i < StarBean.I_HIP; i++) {
            data[i] = mem.getFloat();
        }
        // Int
        for (int i = StarBean.I_HIP; i < StarBean.SIZE; i++) {
            data[i] = mem.getInt();
        }

        Long id = mem.getLong();
        int nameLength = mem.getInt();
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < nameLength; i++)
            name.append(mem.getChar());

        return new StarBean(data, id, name.toString());
    }

}
