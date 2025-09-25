package gaiasky;

import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.TLEParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests fetching and parsing orbital data in TLE.
 */
public class TLEParserTest {

    List<String> lines;

    @Before
    public void setUp() throws Exception {
        SettingsManager.initialize(false);
        var data = "ISS (ZARYA)             \n" +
                "1 25544U 98067A   25142.17626388  .00007957  00000+0  14902-3 0  9991\n" +
                "2 25544  51.6387  75.9349 0002437 130.6256  11.0458 15.49654472511124\n" +
                "CSS (TIANHE)            \n" +
                "1 48274U 21035A   25141.48388263  .00024921  00000+0  27716-3 0  9994\n" +
                "2 48274  41.4638 182.7970 0005843 310.2375  49.7953 15.62274453231987\n" +
                "ISS (NAUKA)             \n" +
                "1 49044U 21066A   25141.31256131  .00008973  00000+0  16709-3 0  9993\n" +
                "2 49044  51.6356  80.2186 0002209 132.2472 227.8704 15.49640199216476\n" +
                "FREGAT DEB              \n" +
                "1 49271U 11037PF  25139.04138472  .00014961  00000+0  36540-1 0  9993\n" +
                "2 49271  51.6315  92.6347 0869949 128.5677 239.6424 12.28834273176327\n" +
                "CSS (WENTIAN)           \n" +
                "1 53239U 22085A   25141.48388263  .00024921  00000+0  27716-3 0  9997\n" +
                "2 53239  41.4638 182.7970 0005843 310.2375  49.7953 15.62274453 11070\n" +
                "CSS (MENGTIAN)          \n" +
                "1 54216U 22143A   25141.48388263  .00024921  00000+0  27716-3 0  9998\n" +
                "2 54216  41.4638 182.7970 0005843 310.2375  49.7953 15.62274453117983\n" +
                "TIANZHOU-8              \n" +
                "1 61983U 24211A   25141.48388263  .00024921  00000+0  27716-3 0  9995\n" +
                "2 61983  41.4638 182.7970 0005843 310.2375  49.7953 15.62274453  3829\n" +
                "PROGRESS-MS 29          \n" +
                "1 62030U 24215A   25141.31256131  .00008973  00000+0  16709-3 0  9992\n" +
                "2 62030  51.6356  80.2186 0002209 132.2472 227.8704 15.49640199    84\n" +
                "PROGRESS-MS 30          \n" +
                "1 63129U 25041B   25141.31256131  .00008973  00000+0  16709-3 0  9990\n" +
                "2 63129  51.6356  80.2186 0002209 132.2472 227.8704 15.49640199    73\n" +
                "CREW DRAGON 10          \n" +
                "1 63204U 25049A   25141.31256131  .00008973  00000+0  16709-3 0  9992\n" +
                "2 63204  51.6356  80.2186 0002209 132.2472 227.8704 15.49640199  9841\n" +
                "SOYUZ-MS 27             \n" +
                "1 63520U 25072A   25141.31256131  .00008973  00000+0  16709-3 0  9999\n" +
                "2 63520  51.6356  80.2186 0002209 132.2472 227.8704 15.49640199  2485\n" +
                "DRAGON CRS-32           \n" +
                "1 63628U 25080A   25141.31256131  .00008973  00000+0  16709-3 0  9997\n" +
                "2 63628  51.6356  80.2186 0002209 132.2472 227.8704 15.49640199    77\n" +
                "SHENZHOU-20 (SZ-20)     \n" +
                "1 63632U 25082A   25141.48388263  .00024921  00000+0  27716-3 0  9995\n" +
                "2 63632  41.4638 182.7970 0005843 310.2375  49.7953 15.62274453229125";
        try {
            lines = new ArrayList<>();
            String line;
            BufferedReader reader = new BufferedReader(new StringReader(data));
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading data lines");
            e.printStackTrace(System.err);
        }
    }

    @Test
    public void testFetchTLEData() {
        String url = "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=tle";

        try {
            var parser = new TLEParser();
            List<String> lines = parser.fetchTLEData(url);
            Assert.assertFalse("TLE data should not be empty", lines.isEmpty());
        } catch (IOException e) {
            System.out.println("Network not available. Skipping test: " + e.getMessage());
        }
    }

    @Test
    public void testParseTLE() {
        String targetName = "ISS (ZARYA)";

        try {
            var parser = new TLEParser();
            var elements = parser.extractOrbitalElements(lines, targetName);
            Assert.assertNotNull(elements);
            System.out.println(elements);
            var instant = AstroUtils.julianDateToInstant(elements.epochJD);
            Assert.assertEquals("2025-05-23T04:13:49.199230671Z", instant.toString());
            Assert.assertEquals(0.0645305142577, elements.period, 1e-10);
            Assert.assertEquals(6795.873069940159, elements.semiMajorAxis, 1e-8);
            Assert.assertEquals(51.6387, elements.inclination, 1e-5);
            Assert.assertEquals(75.9349, elements.ascendingNode, 1e-5);
            Assert.assertEquals(0.0002437, elements.eccentricity, 1e-7);
            Assert.assertEquals(130.6256, elements.argOfPericenter, 1e-5);
            Assert.assertEquals(11.0458, elements.meanAnomaly, 1e-5);
        } catch (Exception e) {
            Assert.fail("Error parsing TLE data: " + e.getMessage());
        }
    }
}
