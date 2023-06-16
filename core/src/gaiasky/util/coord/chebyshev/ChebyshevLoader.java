package gaiasky.util.coord.chebyshev;

import gaiasky.util.coord.chebyshev.ChebyshevCoefficients.Header;

import java.io.*;
import java.nio.file.Path;

/**
 * The class <code>{@link ChebyshevLoader}</code> loads Chebyshev
 * polynomial coefficients files into an instance of
 * {@link ChebyshevCoefficients}.
 *
 * @author {@literal Wolfgang Löffler <loeffler@ari.uni-heidelberg.de>}
 */
public class ChebyshevLoader {

    private String fileName;


    protected ChebyshevLoader() {
    }

    /**
     * Returns the <code>{@link ChebyshevCoefficients}</code> holding
     * the Chebyshev polynomial coefficients for the position as loaded from the
     * corresponding disc file. The Chebyshev polynomial coefficients for the
     * velocity are not loaded.
     *
     * @return the <code>{@link ChebyshevCoefficients}</code>
     */
    public ChebyshevCoefficients loadData(Path filePath) throws FileNotFoundException {

        final String positionTypeName = "position";
        final int positionTypeIndex = 0;

        final InputStream inputStream = new FileInputStream(filePath.toFile());

        final ChebyshevCoefficients chebyshevEphemerisData = new ChebyshevCoefficients();

        try {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader);) {

                chebyshevEphemerisData.header[positionTypeIndex] = this.loadHeader(bufferedReader);
                chebyshevEphemerisData.coefficients[positionTypeIndex] = this.loadData(bufferedReader, chebyshevEphemerisData.header[positionTypeIndex]);

            } finally {
                inputStream.close();
            }
        } catch (final IOException e) {
            throw new RuntimeException("Cannot read the resource file " + filePath, e);
        }

        return chebyshevEphemerisData;
    }


    /**
     * Reads the header of a Chebyshev ephemeris file for the given position ephemeris
     * from the given
     * <code>{@link BufferedReader}</code> and returns the parsed
     * <code>{@link Header}</code>.
     *
     * @param bufferedReader the <code>{@link BufferedReader}</code> with the
     *                       Chebyshev position ephemeris file
     * @return the <code>{@link Header}</code>
     * @throws IOException if reading from the
     *                     <code>{@link BufferedReader}</code> or the parsing into
     *                     the <code>{@link Header}</code> fails
     */
    private Header loadHeader(final BufferedReader bufferedReader) throws IOException {

        final String bodyLine = bufferedReader.readLine();
        if (null == bodyLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String vectorLine = bufferedReader.readLine();
        if (null == vectorLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String dimensionsLine = bufferedReader.readLine();
        if (null == dimensionsLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String granulesLine = bufferedReader.readLine();
        if (null == granulesLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String equisizedLine = bufferedReader.readLine();
        if (null == equisizedLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String tcbBeginLine = bufferedReader.readLine();
        if (null == tcbBeginLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String tcbEndLine = bufferedReader.readLine();
        if (null == tcbEndLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String emptyLine = bufferedReader.readLine();
        if (null == emptyLine) {
            throw new IOException("Unexpeced end of file.");
        }
        final String headerLine = bufferedReader.readLine();
        if (null == headerLine) {
            throw new IOException("Unexpeced end of file.");
        }

        if (!this.parseHeaderLine(vectorLine).equals("position")) {
            throw new IOException("Unexpected vector type in file.");
        }

        // If the contents of the loaded data files are corrupt,
        // the following .parseXyz() calls may fail miserably and throw exceptions

        final int nGranules = Integer.parseInt(this.parseHeaderLine(granulesLine));
        final boolean isEquisized = Boolean.parseBoolean(this.parseHeaderLine(equisizedLine));
        final long nanoscondsTcbBegin = Long.parseLong(this.parseHeaderLine(tcbBeginLine));
        final long nanoscondsTcbEnd = Long.parseLong(this.parseHeaderLine(tcbEndLine));

        return new Header(nGranules, isEquisized, nanoscondsTcbBegin, nanoscondsTcbEnd);
    }

    /**
     * Reads the data of a Chebyshev coefficients file from the given
     * <code>{@link BufferedReader}</code> and returns the parsed
     * <code>{@link ChebyshevCoefficients.Coefficients}</code>.
     *
     * @param bufferedReader the <code>{@link BufferedReader}</code> with the
     *                       Chebyshev coefficients file
     * @return the
     * <code>{@link ChebyshevCoefficients.Coefficients}</code>
     * @throws IOException if reading from the
     *                     <code>{@link BufferedReader}</code> or the parsing into
     *                     the
     *                     <code>{@link ChebyshevCoefficients.Coefficients}</code> fails
     */
    private ChebyshevCoefficients.Coefficients loadData(final BufferedReader bufferedReader,
                                                        final Header header) throws IOException {

        // The nanosecondsTcbArray array holds the begin times of the
        // time granules. One extra array element is added at the end
        // to hold the end time of the last granule.

        final long[] nanosecondsTcbArray = new long[header.nGranules + 1];
        nanosecondsTcbArray[header.nGranules] = header.nanosecondsTcbEnd;

        final double[][][] coefficientsArray = new double[header.nGranules][header.nDimensions][];

        for (int iGranule = 0; iGranule < header.nGranules; iGranule++) {
            for (int iDimensions = 0; iDimensions < header.nDimensions; iDimensions++) {

                final String dataLine = bufferedReader.readLine();
                if (null == dataLine) {
                    throw new IOException("Unexpeced end of file.");
                }
                final String[] dataLineTokens = this.splitDataLine(dataLine.strip());
                final int[] dataLineHeader = this.parseDataLineHeader(dataLineTokens);
                if (iGranule != dataLineHeader[0]) {
                    throw new IOException("Unexpeced file format.");
                }
                if (iDimensions != dataLineHeader[1]) {
                    throw new IOException("Unexpeced file format.");
                }
                final int nCoefficients = dataLineHeader[2];
                if (4 + nCoefficients != dataLineTokens.length) {
                    throw new IOException("Unexpeced file format.");
                }
                final long nanosecondsTcb = this.parseDataLineTime(dataLineTokens);
                if (nanosecondsTcb < header.nanosecondsTcbBegin || header.nanosecondsTcbEnd < nanosecondsTcb) {
                    throw new IOException("Granule time out of range.");
                }
                nanosecondsTcbArray[iGranule] = nanosecondsTcb;
                coefficientsArray[iGranule][iDimensions] = this.parseDataLineCoefficients(dataLineTokens);
            }

            final int nCoefficientsX = coefficientsArray[iGranule][0].length;
            final int nCoefficientsY = coefficientsArray[iGranule][1].length;
            final int nCoefficientsZ = coefficientsArray[iGranule][2].length;

            if (nCoefficientsX != nCoefficientsZ || nCoefficientsY != nCoefficientsZ) {
                throw new IOException("Different polynomial orders for vector components.");
            }
        }

        return new ChebyshevCoefficients.Coefficients(nanosecondsTcbArray, coefficientsArray);
    }

    /**
     * Splits the given line at the colon and returns the second part stripped of
     * leading and trailing spaces.
     *
     * @param line the line <code>{@link String}</code>.
     * @return the second part of the line <code>{@link String}</code>
     * @throws IOException if splitting the given line fails
     */
    private String parseHeaderLine(final String line) throws IOException {

        final String[] splitLine = line.split(":");
        if (2 != splitLine.length) {
            throw new IOException("Unexpected file format.");
        }

        return splitLine[1].strip();
    }

    /**
     * Splits the given data line <code>{@link String}</code> into an array of token
     * <code>{@link String}s</code>.
     *
     * @param line the data line <code>{@link String}</code>
     * @return the array of token <code>{@link String}s</code>
     * @throws IOException if the given data line <code>{@link String}</code> does
     *                     not contain the minimum number of expected tokens
     */
    private String[] splitDataLine(final String line) throws IOException {

        final String[] splitLine = line.split("\\s+");
        if (4 > splitLine.length) {
            throw new IOException("Unexpected file format.");
        }

        return splitLine;
    }

    /**
     * Parses the first three tokens of the given data line tokens
     * <code>{@link String}</code> array into an <code>int</code> array
     * <code>[iGranule, nDimensions, nCoefficients]</code>.
     *
     * @param dataLineTokens the <code>{@link String}</code> array of data line
     *                       tokens
     * @return the code>int</code> array
     * <code>[iGranule, nDimensions, nCoefficients]</code>
     */

    private int[] parseDataLineHeader(final String[] dataLineTokens) {

        final int iGranule = Integer.parseInt(dataLineTokens[0].strip());
        final int nDimensions = Integer.parseInt(dataLineTokens[1].strip());
        final int nCoefficients = Integer.parseInt(dataLineTokens[2].strip());

        return new int[]{
                iGranule, nDimensions, nCoefficients,
        };
    }

    /**
     * Parses the fourth token of the given data line tokens
     * <code>{@link String}</code> array into a <code>long</code> containing the
     * time in nanoseconds TCB.
     *
     * @param dataLineTokens the <code>{@link String}</code> array of data line
     *                       tokens
     * @return the time in nanoseconds TCB
     */
    private long parseDataLineTime(final String[] dataLineTokens) {

        return Long.parseLong(dataLineTokens[3].strip());
    }

    /**
     * Parses the fifth and following tokens of the given data line tokens
     * <code>{@link String}</code> array into a <code>double</code> array of
     * Chebyshev coefficients.
     *
     * @param dataLineTokens the the <code>{@link String}</code> array of data line
     *                       tokens
     * @return the <code>double</code> array of Chebyshev coefficients
     */
    private double[] parseDataLineCoefficients(final String[] dataLineTokens) {

        final double[] coefficients = new double[dataLineTokens.length - 4];
        for (int iCoefficient = 0; iCoefficient < dataLineTokens.length - 4; iCoefficient++) {
            coefficients[iCoefficient] = Double.parseDouble(dataLineTokens[iCoefficient + 4].strip());
        }

        return coefficients;
    }

}
