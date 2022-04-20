/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import gaiasky.util.BinarySearchTree;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gaia.time.Duration;
import gaiasky.util.gaia.time.Hours;
import gaiasky.util.i18n.I18n;
import gaiasky.util.units.Quantity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses the XML files with the attitudes and their activation times into a binary search tree.
 */
public class AttitudeXmlParser {
    private static final Log logger = Logger.getLogger(AttitudeXmlParser.class);

    private static final Instant endOfMission;
    private static final DateTimeFormatter format;
    private static final DateTimeFormatter formatWithMs;

    static {
        format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withLocale(I18n.locale).withZone(ZoneOffset.UTC);
        formatWithMs = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS").withLocale(I18n.locale).withZone(ZoneOffset.UTC);
        endOfMission = getDate("2026-09-14 17:44:20");
    }

    public static BinarySearchTree parseFolder(String folder) {
        final Array<FileHandle> list;
        try (Stream<Path> paths = Files.walk(Paths.get(Settings.settings.data.dataFile(folder)))) {
            List<Path> ps = paths.filter(Files::isRegularFile).collect(Collectors.toList());
            list = new Array<>(false, ps.size());
            for (Path p : ps) {
                if (p.toFile().getName().endsWith(".xml"))
                    list.add(new FileHandle(p.toFile()));
            }

            BinarySearchTree bst = new BinarySearchTree();

            // GENERATE LIST OF DURATIONS
            SortedMap<Instant, FileHandle> datesMap = new TreeMap<>();
            for (FileHandle fh : list) {
                try {
                    Instant date = parseActivationTime(fh);
                    datesMap.put(date, fh);
                } catch (Exception e) {
                    logger.error(e, I18n.msg("error.file.parse", fh.name()));
                }
            }
            Map<FileHandle, Duration> durationMap = new HashMap<>();
            Set<Instant> dates = datesMap.keySet();
            FileHandle lastFH = null;
            Instant lastDate = null;
            for (Instant date : dates) {
                if (lastDate != null && lastFH != null) {
                    long elapsed = date.toEpochMilli() - lastDate.toEpochMilli();

                    Duration d = new Hours(elapsed * Nature.MS_TO_H);
                    durationMap.put(lastFH, d);
                }
                lastDate = date;
                lastFH = datesMap.get(date);
            }
            // Last element
            long elapsed = endOfMission.toEpochMilli() - lastDate.toEpochMilli();
            Duration d = new Hours(elapsed * Nature.MS_TO_H);
            durationMap.put(lastFH, d);

            // PARSE ATTITUDES
            for (FileHandle fh : list) {
                logger.info(I18n.msg("notif.attitude.loadingfile", fh.name()));
                try {
                    AttitudeIntervalBean att = parseFile(fh, durationMap.get(fh), findActivationDate(fh, datesMap));
                    bst.insert(att);
                } catch (Exception e) {
                    logger.error(e, I18n.msg("notif.error", e.getMessage()));
                }
            }

            logger.info(I18n.msg("notif.attitude.initialized", list.size));
            return bst;
        } catch (Exception e) {
            logger.error("Error loading attitude files");
        }
        return null;
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

    private static Instant parseActivationTime(FileHandle fh) {

        XmlReader reader = new XmlReader();
        XmlReader.Element element = reader.parse(fh);
        XmlReader.Element model = element.getChildByName("model");

        /** MODEL ELEMENT **/
        String activTime = model.get("starttime");
        return getDate(activTime);
    }

    private static AttitudeIntervalBean parseFile(FileHandle fh, Duration duration, Instant activationTime) {
        BaseAttitudeDataServer<?> result = null;

        XmlReader reader = new XmlReader();
        XmlReader.Element element = reader.parse(fh);
        XmlReader.Element model = element.getChildByName("model");

        /** MODEL ELEMENT **/
        String name = model.get("name");
        String className = model.get("classname").replace("gaia.cu9.ari.gaiaorbit", "gaiasky");
        long startTimeNsSince2010 = (long) ((AstroUtils.getJulianDate(activationTime) - AstroUtils.JD_J2010) * Nature.D_TO_NS);

        /** SCAN LAW ELEMENT **/
        XmlReader.Element scanlaw = model.getChildByName("scanlaw");
        String epochRef = scanlaw.getAttribute("epochref");
        Instant refEpochDate = getDate(epochRef);
        long refEpochJ2010 = (long) ((AstroUtils.getJulianDate(refEpochDate) - AstroUtils.JD_J2010) * Nature.D_TO_NS);

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
            ModifiedScanningLaw msl = new ModifiedScanningLaw(startTimeNsSince2010);
            msl.setRefEpoch(refEpochJ2010);
            msl.setRefNu(precessionPhase.get(Quantity.Angle.AngleUnit.RAD));
            msl.setRefOmega(spinPhase.get(Quantity.Angle.AngleUnit.RAD));
            msl.setPrecRate(precessionRate);
            msl.setScanRate(scanRate.get(Quantity.Angle.AngleUnit.ARCSEC));
            msl.setRefXi(solarAspectAngle.get(Quantity.Angle.AngleUnit.RAD));
            msl.setTypicalHighDensityArea();
            msl.initialize();

            MslAttitudeDataServer mslDatServ = new MslAttitudeDataServer(startTimeNsSince2010, duration, msl);
            mslDatServ.initialize();
            result = mslDatServ;

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
        try {
            LocalDateTime ldt = LocalDateTime.parse(date, format);
            return ldt.toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            LocalDateTime ldt = LocalDateTime.parse(date, formatWithMs);
            return ldt.toInstant(ZoneOffset.UTC);
        }
    }

    private static Double getDouble(XmlReader.Element e, String property) {
        return Double.parseDouble(e.get(property));
    }
}
