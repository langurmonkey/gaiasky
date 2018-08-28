package gaia.cu9.ari.gaiaorbit.util.gaia;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;

import gaia.cu9.ari.gaiaorbit.util.BinarySearchTree;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.format.IDateFormat;
import gaia.cu9.ari.gaiaorbit.util.gaia.time.Duration;
import gaia.cu9.ari.gaiaorbit.util.gaia.time.Hours;
import gaia.cu9.ari.gaiaorbit.util.gaia.time.Secs;
import gaia.cu9.ari.gaiaorbit.util.units.Quantity;

/**
 * Parses the XML files with the attitudes and their activaton times into a binary search tree.
 * @author Toni Sagrista
 */
public class AttitudeXmlParser {
    private static final Log logger = Logger.getLogger(AttitudeXmlParser.class);
    
    private static Instant endOfMission;
    private static IDateFormat format;

    static {
        format = DateFormatFactory.getFormatter("yyyy-MM-dd HH:mm:ss");
        endOfMission = getDate("2019-06-20 06:13:26");
    }

    public static BinarySearchTree parseFolder(String folder, boolean oneDayDuration, String... files) {
        final FileHandle[] list;
        if (files == null || files.length == 0) {
            list = new FileHandle[15];
            list[0] = Gdx.files.internal(folder + "OPS_RSLS_0021791_rsls_launch_minus_5_weeks_epsl_comm_following.xml");
            list[1] = Gdx.files.internal(folder + "OPS_RSLS_0021791_rsls_launch_minus_5_weeks_epsl_comm_following_TUNED2014-07-03.xml");
            list[2] = Gdx.files.internal(folder + "OPS_RSLS_0022916_rsls_nsl_gareq1_afterFirstSpinPhaseOptimization.2.xml");
            list[3] = Gdx.files.internal(folder + "OPS_RSLS_0023024_rsls_tsl_ecliptic_pole_scanning.xml");
            list[4] = Gdx.files.internal(folder + "OPS_RSLS_0023165_rsls_nls_comm_gps_may2014.xml");
            list[5] = Gdx.files.internal(folder + "OPS_RSLS_0023768_sa42deg_corrn.xml");
            list[6] = Gdx.files.internal(folder + "OPS_RSLS_0024158.xml");
            list[7] = Gdx.files.internal(folder + "OPS_RSLS_0026136_NSL_TUNEDfor2014-05-01.xml");
            list[8] = Gdx.files.internal(folder + "OPS_RSLS_0026139_EPSL-P_TUNEDfor2014-03-12.xml");
            list[9] = Gdx.files.internal(folder + "OPS_RSLS_0026141_EPSL-P_TUNEDfor2014-03-12.xml");
            list[10] = Gdx.files.internal(folder + "OPS_RSLS_0026624_EPSL-P_TUNEDfor2014-05-09.xml");
            list[11] = Gdx.files.internal(folder + "OPS_RSLS_0026767_EPSL-P_TUNEDfor2014-06-02.xml");
            list[12] = Gdx.files.internal(folder + "OPS_RSLS_0026877_EPSL-F_TUNEDfor2014-06-06.xml");
            list[13] = Gdx.files.internal(folder + "OPS_RSLS_0028463_epsl_following_FIXED_20140909.xml");
            list[14] = Gdx.files.internal(folder + "OPS_RSLS_0028750_rsls_epsl_comm_leading_nsl_cont_corrected.xml");
        } else {
            list = new FileHandle[files.length];
            int i = 0;
            for (String file : files) {
                list[i] = Gdx.files.internal(folder + file);
                i++;
            }
        }

        BinarySearchTree bst = new BinarySearchTree();

        final long overlapMs = 10 * 60 * 1000;
        final long threeHoursSec = 10800;
        // GENERATE LIST OF DURATIONS
        SortedMap<Instant, FileHandle> datesMap = new TreeMap<Instant, FileHandle>();
        for (FileHandle fh : list) {
            try {
                if (oneDayDuration) {
                    /** 
                     * Hack to get the stripped FOV mode to load fast.
                     * We set the activation time to ten minutes before today starts. 
                     */

                    LocalDateTime ldt = LocalDateTime.now();
                    ldt = ldt.withMinute(0).withSecond(0);
                    Instant date = ldt.toInstant(ZoneOffset.UTC);
                    // Date is the start of today. Lets subtract 10 minutes
                    date.minusMillis(overlapMs);
                    datesMap.put(date, fh);
                } else {
                    Instant date = parseActivationTime(fh);
                    datesMap.put(date, fh);
                }
            } catch (IOException e) {
                logger.error(e, I18n.bundle.format("error.file.parse", fh.name()));
            }
        }
        Map<FileHandle, Duration> durationMap = new HashMap<FileHandle, Duration>();
        Set<Instant> dates = datesMap.keySet();
        FileHandle lastFH = null;
        Instant lastDate = null;
        for (Instant date : dates) {
            if (lastDate != null && lastFH != null) {
                if (oneDayDuration) {
                    Duration d = new Secs(threeHoursSec + overlapMs * 2 / 1000);
                    durationMap.put(lastFH, d);
                } else {
                    long elapsed = date.toEpochMilli() - lastDate.toEpochMilli();

                    Duration d = new Hours(elapsed * Nature.MS_TO_H);
                    durationMap.put(lastFH, d);
                }
            }
            lastDate = date;
            lastFH = datesMap.get(date);
        }
        // Last element
        if (oneDayDuration) {
            Duration d = new Secs(threeHoursSec + overlapMs * 2 / 1000);
            durationMap.put(lastFH, d);
        } else {
            long elapsed = endOfMission.toEpochMilli() - lastDate.toEpochMilli();
            Duration d = new Hours(elapsed * Nature.MS_TO_H);
            durationMap.put(lastFH, d);
        }

        // PARSE ATTITUDES
        for (FileHandle fh : list) {
            logger.info(I18n.bundle.format("notif.attitude.loadingfile", fh.name()));
            try {
                AttitudeIntervalBean att = parseFile(fh, durationMap.get(fh), findActivationDate(fh, datesMap));
                bst.insert(att);
            } catch (IOException e) {
                logger.error(e, I18n.bundle.format("error.file.parse", fh.name()));
            } catch (Exception e) {
                logger.error(e, I18n.bundle.format("notif.error", e.getMessage()));
            }
        }

        logger.info(I18n.bundle.format("notif.attitude.initialized", list.length));
        return bst;
    }

    private static Instant findActivationDate(FileHandle fh, SortedMap<Instant, FileHandle> datesMap) {
        Set<Instant> keys = datesMap.keySet();
        for (Instant d : keys) {
            if (datesMap.get(d).equals(fh)) {
                return d;
            }
        }
        return null;
    }

    private static Instant parseActivationTime(FileHandle fh) throws IOException {

        XmlReader reader = new XmlReader();
        XmlReader.Element element = reader.parse(fh);
        XmlReader.Element model = element.getChildByName("model");

        /** MODEL ELEMENT **/
        String activTime = model.get("starttime");
        return getDate(activTime);
    }

    private static AttitudeIntervalBean parseFile(FileHandle fh, Duration duration, Instant activationTime) throws IOException {
        BaseAttitudeDataServer<?> result = null;

        XmlReader reader = new XmlReader();
        XmlReader.Element element = reader.parse(fh);
        XmlReader.Element model = element.getChildByName("model");

        /** MODEL ELEMENT **/
        String name = model.get("name");
        String className = model.get("classname");
        String activTime = model.get("starttime");
        double startTimeNsSince2010 = (AstroUtils.getJulianDate(activationTime) - AstroUtils.JD_J2010) * Nature.D_TO_NS;

        /** SCAN LAW ELEMENT **/
        XmlReader.Element scanlaw = model.getChildByName("scanlaw");
        String epochRef = scanlaw.getAttribute("epochref");
        Instant refEpochDate = getDate(epochRef);
        double refEpoch = AstroUtils.getJulianDate(refEpochDate) * Nature.D_TO_NS;
        double refEpochJ2010 = refEpoch - AstroUtils.JD_J2010 * Nature.D_TO_NS;

        // Spin phase
        XmlReader.Element spinphase = scanlaw.getChildByName("spinphase");
        Quantity.Angle spinPhase = new Quantity.Angle(getDouble(spinphase, "value"), spinphase.get("unit"));

        // Precession pahse
        XmlReader.Element precessphase = scanlaw.getChildByName("precessphase");
        Quantity.Angle precessionPhase = new Quantity.Angle(getDouble(precessphase, "value"), precessphase.get("unit"));

        // Precession rate - always in rev/yr
        XmlReader.Element precessrate = scanlaw.getChildByName("precessrate");
        Double precessionRate = getDouble(precessrate, "value");

        // Scan rate
        XmlReader.Element scanrate = scanlaw.getChildByName("scanrate");
        Quantity.Angle scanRate = new Quantity.Angle(getDouble(scanrate, "value"), scanrate.get("unit").split("_")[0]);

        // Solar aspect angle
        XmlReader.Element saa = scanlaw.getChildByName("solaraspectangle");
        Quantity.Angle solarAspectAngle = new Quantity.Angle(getDouble(saa, "value"), saa.get("unit"));

        if (className.contains("MslAttitudeDataServer")) {
            // We need to pass the startTime, duration and MSL to the constructor

            ModifiedScanningLaw msl = new ModifiedScanningLaw((long) startTimeNsSince2010);
            msl.setRefEpoch((long) refEpochJ2010);
            msl.setRefOmega(spinPhase.get(Quantity.Angle.AngleUnit.RAD));
            msl.setRefNu(precessionPhase.get(Quantity.Angle.AngleUnit.RAD));
            msl.setPrecRate(precessionRate);
            msl.setScanRate(scanRate.get(Quantity.Angle.AngleUnit.ARCSEC));
            msl.setRefXi(solarAspectAngle.get(Quantity.Angle.AngleUnit.RAD));
            msl.initialize();

            MslAttitudeDataServer mslDatServ = new MslAttitudeDataServer((long) startTimeNsSince2010, duration, msl);
            mslDatServ.initialize();
            result = mslDatServ;

            //            Nsl37 nsl = new Nsl37();
            //            nsl.setRefTime((long) refEpochJ2010);
            //            nsl.setNuRef(precessionPhase.get(Quantity.Angle.AngleUnit.RAD));
            //            nsl.setOmegaRef(spinPhase.get(Quantity.Angle.AngleUnit.RAD));
            //            nsl.setXiRef(solarAspectAngle.get(Quantity.Angle.AngleUnit.RAD));
            //            nsl.setTargetScanRate(scanRate.get(Quantity.Angle.AngleUnit.ARCSEC));
            //            nsl.setTargetPrecessionRate(precessionRate);
            //
            //            result = nsl;

        } else if (className.contains("Epsl")) {

            Epsl.Mode mode = name.equals("EPSL_F") ? Epsl.Mode.FOLLOWING : Epsl.Mode.PRECEDING;
            Epsl epsl = new Epsl(mode);

            epsl.setRefTime((long) refEpochJ2010);
            epsl.setNuRef(precessionPhase.get(Quantity.Angle.AngleUnit.RAD));
            epsl.setOmegaRef(spinPhase.get(Quantity.Angle.AngleUnit.RAD));
            epsl.setXiRef(solarAspectAngle.get(Quantity.Angle.AngleUnit.RAD));
            epsl.setTargetScanRate(scanRate.get(Quantity.Angle.AngleUnit.ARCSEC));
            epsl.setTargetPrecessionRate(precessionRate);

            result = epsl;
        }

        return new AttitudeIntervalBean(name, new Date(activationTime.toEpochMilli()), result, fh.name());
    }

    private static Instant getDate(String date) {
        Instant d = format.parse(date);
        return d;
    }

    private static Double getDouble(XmlReader.Element e, String property) {
        return Double.parseDouble(e.get(property));
    }
}
