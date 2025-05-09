package gaiasky.util.coord.vsop87;

//VSOP87-Multilang http://www.celestialprogramming.com/vsop87-multilang/index.html
//Greg Miller (gmiller@gregmiller.net) 2024.  Released as Public Domain

//Binary implementation of VSOP87, requires the vsop87*.bin file for the version
//you want to use.  Order of the vairables is the same as in the offsets.txt
//file. E.g, x,y,z,xv,yv,zv for rectangular versions.

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.math.MathUtilsDouble;
import net.jafama.FastMath;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VSOP87Binary implements IObserver {
    int[] offsets_earth = {};
    int[] offsets_emb = {};
    int[] offsets_jupiter = {};
    int[] offsets_mars = {};
    int[] offsets_mercury = {};
    int[] offsets_neptune = {};
    int[] offsets_saturn = {};
    int[] offsets_uranus = {};
    int[] offsets_venus = {};
    int[] offsets_sun = {};
    int varCount = 3;
    double[] data;

    public final String version;
    private final String fileName;
    private double percentSkipped;

    /**
     * Creates a new instance of the VSOP87 binary with the given binary file. Uses all terms (no truncation).
     *
     * @param binaryFilePath The path to the binary data file to use.
     * @throws Exception If the file can't be read.
     */
    public VSOP87Binary(String binaryFilePath) throws Exception {
        this(binaryFilePath, 0.0);
    }

    /**
     * Creates a new instance of the VSOP87 binary with the given binary file, and the given truncation.
     *
     * @param binaryFilePath The path to the binary data file to use.
     * @param percentSkipped The truncation factor, as the percentage of terms to skip, in [0,1].
     * @throws Exception If the file can't be read.
     */
    public VSOP87Binary(String binaryFilePath, double percentSkipped) throws Exception {
        this.fileName = binaryFilePath;
        this.version = guessVersion(binaryFilePath);
        this.percentSkipped = MathUtilsDouble.clamp(percentSkipped, 0.0, 1.0);
        assignOffsets(this.version);
        loadData(binaryFilePath);

        EventManager.instance.subscribe(this, Event.HIGH_ACCURACY_CMD);
    }

    public String getFileName() {
        return fileName;
    }

    public void setPercentSkipped(double percentSkipped) {
        this.percentSkipped = percentSkipped;
    }

    String guessVersion(String filename) {
        if (filename.contains("vsop87a")) {
            return "vsop87a";
        }
        if (filename.contains("vsop87b")) {
            return "vsop87b";
        }
        if (filename.contains("vsop87c")) {
            return "vsop87c";
        }
        if (filename.contains("vsop87d")) {
            return "vsop87d";
        }
        if (filename.contains("vsop87e")) {
            return "vsop87e";
        }
        if (filename.contains("vsop87")) {
            return "vsop87";
        }
        return null;
    }

    void convertEndianness(byte[] b) {
        int i = 0;
        while (i < b.length) {
            byte t0 = b[i];
            byte t1 = b[1 + i];
            byte t2 = b[2 + i];
            byte t3 = b[3 + i];
            byte t4 = b[4 + i];
            byte t5 = b[5 + i];
            byte t6 = b[6 + i];
            byte t7 = b[7 + i];

            b[i] = t7;
            b[1 + i] = t6;
            b[2 + i] = t5;
            b[3 + i] = t4;
            b[4 + i] = t3;
            b[5 + i] = t2;
            b[6 + i] = t1;
            b[7 + i] = t0;

            i += 8;
        }
    }

    void loadData(String filename) throws Exception {
        Path p = Paths.get(filename);
        long size = Files.size(p);
        byte[] bytes = Files.readAllBytes(p);
        convertEndianness(bytes);

        ByteBuffer b = ByteBuffer.wrap(bytes);
        DoubleBuffer buffer = b.asDoubleBuffer();

        this.data = new double[(int) (size / 8)];
        for (int i = 0; i < size / 8; i++) {
            data[i] = buffer.get();
        }
    }

    public static double[] getMoon(double[] earth, double[] emb) {
        final double[] temp = {0.0, 0.0, 0.0};

        temp[0] = (emb[0] - earth[0]) * (1 + 1 / 0.01230073677);
        temp[1] = (emb[1] - earth[1]) * (1 + 1 / 0.01230073677);
        temp[2] = (emb[2] - earth[2]) * (1 + 1 / 0.01230073677);

        return temp;
    }

    public double[] getEarth(double t) {
        return this.getPlanet(offsets_earth, t);
    }

    public double[] getEmb(double t) {
        return this.getPlanet(offsets_emb, t);
    }

    public double[] getJupiter(double t) {
        return this.getPlanet(offsets_jupiter, t);
    }

    public double[] getMars(double t) {
        return this.getPlanet(offsets_mars, t);
    }

    public double[] getMercury(double t) {
        return this.getPlanet(offsets_mercury, t);
    }

    public double[] getNeptune(double t) {
        return this.getPlanet(offsets_neptune, t);
    }

    public double[] getSaturn(double t) {
        return this.getPlanet(offsets_saturn, t);
    }

    public double[] getSun(double t) {
        return this.getPlanet(offsets_sun, t);
    }

    public double[] getUranus(double t) {
        return this.getPlanet(offsets_uranus, t);
    }

    public double[] getVenus(double t) {
        return this.getPlanet(offsets_venus, t);
    }


    double[] getPlanet(int[] offsets, double t) {
        final double[] p = new double[varCount];
        for (int i = 0; i < varCount; i++) {
            double acc = 0;
            for (int j = 0; j < 6; j++) {
                double eacc = 0;
                int o = offsets[i * 6 * 2 + j * 2] * 3;
                int length = offsets[i * 6 * 2 + j * 2 + 1];
                // Since the terms appear bottom-to-top in the file, we start after the skipped.
                int start = (int) (length * percentSkipped);
                o += 3 * start;
                for (int k = start; k < length; k++) {
                    final double a = this.data[o];
                    final double b = this.data[o + 1];
                    final double c = this.data[o + 2];

                    eacc += a * FastMath.cos(b + c * t);

                    o += 3;
                }
                acc += eacc * FastMath.pow(t, j);
            }
            p[i] = (acc);
        }
        return p;
    }

    void assignOffsets(String version) {
        //vsop87
        int[] vsop87_earth = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] vsop87_emb = {0, 438, 438, 258, 696, 100, 0, 0, 0, 0, 0, 0, 796, 584, 1380, 251, 1631, 53, 1684, 1, 1685, 1, 1686, 1, 1687, 597, 2284, 254, 2538, 55, 2593, 1, 2594, 1, 2595, 1, 2596, 688, 3284, 313, 3597, 80, 3677, 5, 3682, 2, 3684, 1, 3685, 183, 3868, 57, 3925, 6, 3931, 1, 3932, 1, 3933, 1, 3934, 197, 4131, 58, 4189, 8, 4197, 1, 4198, 1, 4199, 1};
        int[] vsop87_jupiter = {4200, 646, 4846, 341, 5187, 189, 5376, 92, 5468, 29, 5497, 2, 5499, 733, 6232, 338, 6570, 164, 6734, 69, 6803, 13, 0, 0, 6816, 723, 7539, 341, 7880, 166, 8046, 72, 8118, 12, 0, 0, 8130, 915, 9045, 425, 9470, 205, 9675, 89, 9764, 30, 9794, 2, 9796, 139, 9935, 74, 10009, 24, 10033, 3, 0, 0, 0, 0, 10036, 141, 10177, 75, 10252, 24, 10276, 3, 0, 0, 0, 0};
        int[] vsop87_mars = {10279, 820, 11099, 504, 11603, 248, 0, 0, 0, 0, 0, 0, 11851, 994, 12845, 471, 13316, 158, 13474, 2, 13476, 1, 13477, 1, 13478, 1009, 14487, 483, 14970, 153, 15123, 2, 15125, 1, 15126, 1, 15127, 1192, 16319, 567, 16886, 173, 17059, 16, 17075, 4, 17079, 3, 17082, 236, 17318, 90, 17408, 21, 17429, 1, 17430, 1, 0, 0, 17431, 234, 17665, 95, 17760, 24, 17784, 1, 17785, 1, 17786, 1};
        int[] vsop87_mercury = {17787, 496, 18283, 225, 18508, 42, 0, 0, 0, 0, 0, 0, 18550, 658, 19208, 312, 19520, 56, 19576, 1, 19577, 1, 19578, 1, 19579, 659, 20238, 294, 20532, 57, 20589, 1, 20590, 1, 20591, 1, 20592, 810, 21402, 370, 21772, 80, 21852, 1, 0, 0, 0, 0, 21853, 272, 22125, 106, 22231, 13, 22244, 1, 22245, 1, 22246, 1, 22247, 249, 22496, 89, 22585, 11, 22596, 1, 22597, 1, 22598, 1};
        int[] vsop87_neptune = {22599, 1149, 23748, 435, 24183, 155, 24338, 45, 24383, 14, 24397, 4, 24401, 1161, 25562, 493, 26055, 190, 26245, 48, 26293, 16, 26309, 2, 26311, 1163, 27474, 494, 27968, 192, 28160, 47, 28207, 16, 28223, 2, 28225, 1097, 29322, 461, 29783, 169, 29952, 45, 29997, 11, 30008, 2, 30010, 210, 30220, 71, 30291, 17, 30308, 4, 0, 0, 0, 0, 30312, 214, 30526, 69, 30595, 17, 30612, 4, 0, 0, 0, 0};
        int[] vsop87_saturn = {30616, 1492, 32108, 727, 32835, 354, 33189, 162, 33351, 62, 33413, 13, 33426, 1503, 34929, 713, 35642, 314, 35956, 126, 36082, 43, 36125, 3, 36128, 1515, 37643, 723, 38366, 328, 38694, 129, 38823, 41, 38864, 3, 38867, 1731, 40598, 866, 41464, 389, 41853, 157, 42010, 51, 42061, 7, 42068, 260, 42328, 129, 42457, 45, 42502, 9, 42511, 1, 0, 0, 42512, 262, 42774, 127, 42901, 45, 42946, 10, 42956, 1, 0, 0};
        int[] vsop87_uranus = {42957, 2047, 45004, 900, 45904, 343, 46247, 97, 46344, 18, 46362, 2, 46364, 2047, 48411, 963, 49374, 377, 49751, 105, 49856, 21, 0, 0, 49877, 2047, 51924, 967, 52891, 370, 53261, 106, 53367, 21, 0, 0, 53388, 2047, 55435, 1023, 56458, 398, 56856, 124, 56980, 25, 57005, 2, 57007, 396, 57403, 136, 57539, 27, 57566, 2, 0, 0, 0, 0, 57568, 403, 57971, 140, 58111, 25, 58136, 3, 0, 0, 0, 0};
        int[] vsop87_venus = {58139, 308, 58447, 168, 58615, 52, 0, 0, 0, 0, 0, 0, 58667, 439, 59106, 167, 59273, 25, 59298, 1, 59299, 1, 59300, 1, 59301, 442, 59743, 158, 59901, 28, 59929, 1, 59930, 1, 59931, 1, 59932, 499, 60431, 201, 60632, 43, 60675, 1, 0, 0, 0, 0, 60676, 155, 60831, 56, 60887, 8, 60895, 1, 60896, 1, 60897, 1, 60898, 163, 61061, 52, 61113, 10, 61123, 1, 61124, 1, 61125, 1};
        int[] vsop87_sun = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //vsop87a
        int[] vsop87a_earth = {0, 843, 843, 491, 1334, 204, 1538, 18, 1556, 15, 1571, 6, 1577, 854, 2431, 496, 2927, 202, 3129, 17, 3146, 15, 3161, 6, 3167, 178, 3345, 120, 3465, 53, 3518, 12, 3530, 6, 3536, 2};
        int[] vsop87a_emb = {3538, 793, 4331, 478, 4809, 185, 4994, 18, 5012, 10, 5022, 6, 5028, 804, 5832, 482, 6314, 184, 6498, 17, 6515, 10, 6525, 6, 6531, 154, 6685, 113, 6798, 46, 6844, 10, 6854, 4, 6858, 2};
        int[] vsop87a_jupiter = {6860, 1055, 7915, 488, 8403, 255, 8658, 140, 8798, 58, 8856, 11, 8867, 1037, 9904, 499, 10403, 259, 10662, 136, 10798, 60, 10858, 11, 10869, 216, 11085, 104, 11189, 65, 11254, 27, 11281, 10, 11291, 3};
        int[] vsop87a_mars = {11294, 1584, 12878, 956, 13834, 387, 14221, 135, 14356, 41, 14397, 21, 14418, 1612, 16030, 969, 16999, 384, 17383, 136, 17519, 44, 17563, 21, 17584, 355, 17939, 232, 18171, 122, 18293, 51, 18344, 16, 18360, 7};
        int[] vsop87a_mercury = {18367, 1449, 19816, 792, 20608, 299, 20907, 54, 20961, 15, 20976, 10, 20986, 1438, 22424, 782, 23206, 299, 23505, 59, 23564, 15, 23579, 10, 23589, 598, 24187, 351, 24538, 143, 24681, 28, 24709, 10, 24719, 7};
        int[] vsop87a_neptune = {24726, 772, 25498, 330, 25828, 102, 25930, 33, 25963, 7, 0, 0, 25970, 746, 26716, 325, 27041, 97, 27138, 34, 27172, 7, 0, 0, 27179, 133, 27312, 37, 27349, 11, 27360, 2, 0, 0, 0, 0};
        int[] vsop87a_saturn = {27362, 1652, 29014, 892, 29906, 481, 30387, 215, 30602, 87, 30689, 31, 30720, 1658, 32378, 917, 33295, 465, 33760, 201, 33961, 88, 34049, 32, 34081, 420, 34501, 217, 34718, 87, 34805, 44, 34849, 19, 34868, 6};
        int[] vsop87a_uranus = {34874, 1464, 36338, 649, 36987, 249, 37236, 84, 37320, 12, 0, 0, 37332, 1447, 38779, 659, 39438, 255, 39693, 80, 39773, 12, 0, 0, 39785, 235, 40020, 98, 40118, 33, 40151, 12, 0, 0, 0, 0};
        int[] vsop87a_venus = {40163, 548, 40711, 338, 41049, 99, 41148, 5, 41153, 4, 41157, 3, 41160, 565, 41725, 325, 42050, 99, 42149, 5, 42154, 4, 42158, 3, 42161, 190, 42351, 108, 42459, 45, 42504, 10, 42514, 3, 42517, 3};
        int[] vsop87a_sun = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //vsop87b
        int[] vsop87b_earth = {0, 184, 184, 134, 318, 62, 380, 14, 394, 6, 400, 2, 402, 623, 1025, 379, 1404, 144, 1548, 23, 1571, 11, 1582, 4, 1586, 523, 2109, 290, 2399, 134, 2533, 20, 2553, 9, 2562, 2};
        int[] vsop87b_emb = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] vsop87b_jupiter = {2564, 249, 2813, 120, 2933, 82, 3015, 33, 3048, 13, 3061, 3, 3064, 860, 3924, 426, 4350, 225, 4575, 120, 4695, 48, 4743, 11, 4754, 727, 5481, 371, 5852, 186, 6038, 97, 6135, 45, 6180, 9};
        int[] vsop87b_mars = {6189, 441, 6630, 291, 6921, 161, 7082, 64, 7146, 18, 7164, 9, 7173, 1409, 8582, 891, 9473, 442, 9915, 194, 10109, 75, 10184, 24, 10208, 1107, 11315, 672, 11987, 368, 12355, 160, 12515, 57, 12572, 17};
        int[] vsop87b_mercury = {12589, 818, 13407, 492, 13899, 231, 14130, 39, 14169, 13, 14182, 10, 14192, 1583, 15775, 931, 16706, 438, 17144, 162, 17306, 23, 17329, 12, 17341, 1209, 18550, 706, 19256, 318, 19574, 111, 19685, 17, 19702, 10};
        int[] vsop87b_neptune = {19712, 172, 19884, 49, 19933, 13, 19946, 2, 0, 0, 0, 0, 19948, 539, 20487, 224, 20711, 59, 20770, 18, 0, 0, 0, 0, 20788, 596, 21384, 251, 21635, 71, 21706, 23, 21729, 7, 0, 0};
        int[] vsop87b_saturn = {21736, 500, 22236, 247, 22483, 111, 22594, 54, 22648, 24, 22672, 11, 22683, 1437, 24120, 817, 24937, 438, 25375, 192, 25567, 85, 25652, 30, 25682, 1208, 26890, 627, 27517, 338, 27855, 154, 28009, 65, 28074, 27};
        int[] vsop87b_uranus = {28101, 311, 28412, 130, 28542, 39, 28581, 15, 0, 0, 0, 0, 28596, 1441, 30037, 655, 30692, 259, 30951, 69, 31020, 8, 0, 0, 31028, 1387, 32415, 625, 33040, 249, 33289, 69, 33358, 12, 0, 0};
        int[] vsop87b_venus = {33370, 210, 33580, 121, 33701, 51, 33752, 12, 33764, 4, 33768, 4, 33772, 416, 34188, 235, 34423, 72, 34495, 7, 34502, 4, 34506, 2, 34508, 323, 34831, 174, 35005, 62, 35067, 8, 35075, 3, 35078, 2};
        int[] vsop87b_sun = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //vsop87c
        int[] vsop87c_earth = {0, 1007, 1007, 600, 1607, 248, 1855, 46, 1901, 20, 1921, 7, 1928, 1007, 2935, 600, 3535, 248, 3783, 46, 3829, 20, 3849, 7, 3856, 178, 4034, 97, 4131, 47, 4178, 11, 4189, 5, 0, 0};
        int[] vsop87c_emb = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] vsop87c_jupiter = {4194, 1272, 5466, 638, 6104, 358, 6462, 190, 6652, 73, 6725, 15, 6740, 1272, 8012, 638, 8650, 358, 9008, 190, 9198, 73, 9271, 15, 9286, 216, 9502, 124, 9626, 68, 9694, 39, 9733, 11, 9744, 5};
        int[] vsop87c_mars = {9749, 1907, 11656, 1023, 12679, 511, 13190, 192, 13382, 81, 13463, 26, 13489, 1907, 15396, 1023, 16419, 511, 16930, 192, 17122, 81, 17203, 26, 17229, 355, 17584, 252, 17836, 133, 17969, 56, 18025, 20, 18045, 7};
        int[] vsop87c_mercury = {18052, 1853, 19905, 1023, 20928, 413, 21341, 135, 21476, 42, 21518, 16, 21534, 1853, 23387, 1023, 24410, 413, 24823, 135, 24958, 42, 25000, 16, 25016, 598, 25614, 360, 25974, 167, 26141, 47, 26188, 12, 26200, 7};
        int[] vsop87c_neptune = {26207, 821, 27028, 342, 27370, 113, 27483, 37, 27520, 14, 27534, 1, 27535, 821, 28356, 342, 28698, 113, 28811, 37, 28848, 14, 28862, 1, 28863, 133, 28996, 61, 29057, 20, 29077, 8, 29085, 1, 29086, 1};
        int[] vsop87c_saturn = {29087, 2047, 31134, 1023, 32157, 511, 32668, 250, 32918, 110, 33028, 40, 33068, 2047, 35115, 1023, 36138, 511, 36649, 250, 36899, 110, 37009, 40, 37049, 420, 37469, 234, 37703, 91, 37794, 45, 37839, 22, 37861, 9};
        int[] vsop87c_uranus = {37870, 1926, 39796, 856, 40652, 341, 40993, 106, 41099, 23, 41122, 2, 41124, 1926, 43050, 856, 43906, 341, 44247, 106, 44353, 23, 44376, 2, 44378, 235, 44613, 159, 44772, 65, 44837, 18, 44855, 6, 44861, 1};
        int[] vsop87c_venus = {44862, 685, 45547, 406, 45953, 133, 46086, 25, 46111, 12, 46123, 4, 46127, 685, 46812, 406, 47218, 133, 47351, 25, 47376, 12, 47388, 4, 47392, 190, 47582, 117, 47699, 49, 47748, 12, 47760, 3, 47763, 3};
        int[] vsop87c_sun = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //vsop87d
        int[] vsop87d_earth = {0, 184, 184, 99, 283, 49, 332, 11, 343, 5, 0, 0, 348, 559, 907, 341, 1248, 142, 1390, 22, 1412, 11, 1423, 5, 1428, 526, 1954, 292, 2246, 139, 2385, 27, 2412, 10, 2422, 3};
        int[] vsop87d_emb = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] vsop87d_jupiter = {2425, 249, 2674, 141, 2815, 81, 2896, 42, 2938, 12, 2950, 5, 2955, 760, 3715, 369, 4084, 191, 4275, 109, 4384, 45, 4429, 10, 4439, 745, 5184, 381, 5565, 190, 5755, 98, 5853, 46, 5899, 9};
        int[] vsop87d_mars = {5908, 441, 6349, 287, 6636, 130, 6766, 41, 6807, 11, 6818, 5, 6823, 1217, 8040, 686, 8726, 310, 9036, 129, 9165, 36, 9201, 15, 9216, 1118, 10334, 596, 10930, 313, 11243, 111, 11354, 28, 11382, 9};
        int[] vsop87d_mercury = {11391, 818, 12209, 494, 12703, 230, 12933, 53, 12986, 15, 13001, 10, 13011, 1380, 14391, 839, 15230, 395, 15625, 153, 15778, 28, 15806, 13, 15819, 1215, 17034, 711, 17745, 326, 18071, 119, 18190, 18, 18208, 10};
        int[] vsop87d_neptune = {18218, 172, 18390, 82, 18472, 25, 18497, 9, 18506, 1, 18507, 1, 18508, 423, 18931, 183, 19114, 57, 19171, 15, 19186, 2, 19188, 1, 19189, 607, 19796, 250, 20046, 72, 20118, 22, 20140, 7, 0, 0};
        int[] vsop87d_saturn = {20147, 500, 20647, 260, 20907, 111, 21018, 58, 21076, 26, 21102, 11, 21113, 1152, 22265, 642, 22907, 321, 23228, 148, 23376, 68, 23444, 27, 23471, 1205, 24676, 639, 25315, 342, 25657, 157, 25814, 64, 25878, 28};
        int[] vsop87d_uranus = {25906, 283, 26189, 154, 26343, 60, 26403, 16, 26419, 2, 0, 0, 26421, 947, 27368, 426, 27794, 151, 27945, 46, 27991, 7, 27998, 1, 27999, 1124, 29123, 514, 29637, 192, 29829, 55, 29884, 11, 0, 0};
        int[] vsop87d_venus = {29895, 210, 30105, 133, 30238, 59, 30297, 15, 30312, 5, 30317, 4, 30321, 367, 30688, 215, 30903, 70, 30973, 9, 30982, 5, 30987, 5, 30992, 330, 31322, 180, 31502, 63, 31565, 7, 31572, 3, 31575, 2};
        int[] vsop87d_sun = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        //vsop87e
        int[] vsop87e_earth = {0, 1199, 1199, 698, 1897, 349, 2246, 112, 2358, 67, 2425, 29, 2454, 1212, 3666, 710, 4376, 345, 4721, 111, 4832, 68, 4900, 29, 4929, 275, 5204, 186, 5390, 96, 5486, 40, 5526, 22, 5548, 8};
        int[] vsop87e_emb = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] vsop87e_jupiter = {5556, 1083, 6639, 499, 7138, 258, 7396, 140, 7536, 59, 7595, 11, 7606, 1064, 8670, 510, 9180, 262, 9442, 136, 9578, 61, 9639, 11, 9650, 229, 9879, 110, 9989, 66, 10055, 30, 10085, 10, 10095, 3};
        int[] vsop87e_mars = {10098, 1572, 11670, 966, 12636, 446, 13082, 205, 13287, 74, 13361, 38, 13399, 1590, 14989, 993, 15982, 444, 16426, 203, 16629, 75, 16704, 38, 16742, 404, 17146, 273, 17419, 147, 17566, 70, 17636, 27, 17663, 10};
        int[] vsop87e_mercury = {17673, 1553, 19226, 948, 20174, 473, 20647, 187, 20834, 95, 20929, 43, 20972, 1553, 22525, 946, 23471, 478, 23949, 186, 24135, 98, 24233, 43, 24276, 625, 24901, 363, 25264, 178, 25442, 63, 25505, 34, 25539, 14};
        int[] vsop87e_neptune = {25553, 720, 26273, 291, 26564, 83, 26647, 27, 26674, 5, 0, 0, 26679, 693, 27372, 287, 27659, 79, 27738, 28, 27766, 5, 0, 0, 27771, 126, 27897, 33, 27930, 10, 27940, 2, 0, 0, 0, 0};
        int[] vsop87e_saturn = {27942, 1651, 29593, 896, 30489, 481, 30970, 214, 31184, 87, 31271, 31, 31302, 1657, 32959, 921, 33880, 464, 34344, 200, 34544, 89, 34633, 32, 34665, 424, 35089, 218, 35307, 87, 35394, 45, 35439, 19, 35458, 6};
        int[] vsop87e_uranus = {42098, 1432, 43530, 628, 44158, 234, 44392, 79, 44471, 10, 0, 0, 44481, 1411, 45892, 634, 46526, 238, 46764, 75, 46839, 10, 0, 0, 46849, 232, 47081, 97, 47178, 33, 47211, 12, 0, 0, 0, 0};
        int[] vsop87e_venus = {47223, 956, 48179, 583, 48762, 266, 49028, 106, 49134, 61, 49195, 32, 49227, 972, 50199, 574, 50773, 264, 51037, 106, 51143, 59, 51202, 31, 51233, 288, 51521, 186, 51707, 100, 51807, 42, 51849, 20, 51869, 11};
        int[] vsop87e_sun = {35464, 1293, 36757, 816, 37573, 461, 38034, 206, 38240, 84, 38324, 34, 38358, 1291, 39649, 814, 40463, 470, 40933, 206, 41139, 87, 41226, 34, 41260, 376, 41636, 243, 41879, 123, 42002, 61, 42063, 26, 42089, 9};

        if (version.equals("vsop87")) {
            varCount = 6;
            offsets_earth = vsop87_earth;
            offsets_emb = vsop87_emb;
            offsets_jupiter = vsop87_jupiter;
            offsets_mars = vsop87_mars;
            offsets_mercury = vsop87_mercury;
            offsets_neptune = vsop87_neptune;
            offsets_saturn = vsop87_saturn;
            offsets_uranus = vsop87_uranus;
            offsets_venus = vsop87_venus;
            offsets_sun = vsop87_sun;
        } else if (version.equals("vsop87a")) {
            offsets_earth = vsop87a_earth;
            offsets_emb = vsop87a_emb;
            offsets_jupiter = vsop87a_jupiter;
            offsets_mars = vsop87a_mars;
            offsets_mercury = vsop87a_mercury;
            offsets_neptune = vsop87a_neptune;
            offsets_saturn = vsop87a_saturn;
            offsets_uranus = vsop87a_uranus;
            offsets_venus = vsop87a_venus;
            offsets_sun = vsop87a_sun;
        } else if (version.equals("vsop87b")) {
            offsets_earth = vsop87b_earth;
            offsets_emb = vsop87b_emb;
            offsets_jupiter = vsop87b_jupiter;
            offsets_mars = vsop87b_mars;
            offsets_mercury = vsop87b_mercury;
            offsets_neptune = vsop87b_neptune;
            offsets_saturn = vsop87b_saturn;
            offsets_uranus = vsop87b_uranus;
            offsets_venus = vsop87b_venus;
            offsets_sun = vsop87b_sun;
        } else if (version.equals("vsop87c")) {
            offsets_earth = vsop87c_earth;
            offsets_emb = vsop87c_emb;
            offsets_jupiter = vsop87c_jupiter;
            offsets_mars = vsop87c_mars;
            offsets_mercury = vsop87c_mercury;
            offsets_neptune = vsop87c_neptune;
            offsets_saturn = vsop87c_saturn;
            offsets_uranus = vsop87c_uranus;
            offsets_venus = vsop87c_venus;
            offsets_sun = vsop87c_sun;
        } else if (version.equals("vsop87d")) {
            offsets_earth = vsop87d_earth;
            offsets_emb = vsop87d_emb;
            offsets_jupiter = vsop87d_jupiter;
            offsets_mars = vsop87d_mars;
            offsets_mercury = vsop87d_mercury;
            offsets_neptune = vsop87d_neptune;
            offsets_saturn = vsop87d_saturn;
            offsets_uranus = vsop87d_uranus;
            offsets_venus = vsop87d_venus;
            offsets_sun = vsop87d_sun;
        } else if (version.equals("vsop87e")) {
            offsets_earth = vsop87e_earth;
            offsets_emb = vsop87e_emb;
            offsets_jupiter = vsop87e_jupiter;
            offsets_mars = vsop87e_mars;
            offsets_mercury = vsop87e_mercury;
            offsets_neptune = vsop87e_neptune;
            offsets_saturn = vsop87e_saturn;
            offsets_uranus = vsop87e_uranus;
            offsets_venus = vsop87e_venus;
            offsets_sun = vsop87e_sun;
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.HIGH_ACCURACY_CMD && source != this) {
            Boolean highAccuracy = (Boolean) data[0];
            setPercentSkipped(highAccuracy ? 0 : 0.9);
        }

    }
}
