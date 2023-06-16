package gaiasky.util.coord.chebyshev;


import javax.xml.crypto.Data;

/**
 * The class <code>{@link ChebyshevCoefficients}</code> carries the
 * Chebyshev coefficients of the ephemerides of a specific body.
 *
 * @author {@literal Wolfgang Löffler <loeffler@ari.uni-heidelberg.de>}
 */
public class ChebyshevCoefficients {

	/** The <code>{@link Header}s</code> for the vector types */
	public Header[] header = new Header[1];
	/** The <code>{@link Coefficients}</code> for the position data */
	public Coefficients[] coefficients = new Coefficients[1];

	/**
	 * The class <code>{@link Header}</code> contains the header data of a set of
	 * Chebyshev polynomials describing the structure of the
	 * <code>{@link Coefficients}</code> for position and velocity.
	 */
	public static class Header {

		/** The number of three spatial dimensions */
		public int nDimensions = 3;
		/** The number of time granules */
		public int nGranules;
		/** A flag indicating whether the time granules are equidistant */
		public boolean isEquidistant;
		/** The start time in nanoseconds TCB */
		public long nanosecondsTcbBegin;
		/** The end time in nanoseconds TCB */
		public long nanosecondsTcbEnd;

		/**
		 * Initialises this <code>{@link Header}</code> with the given
		 * number of time granules and a flag indicating
		 * whether the time granules are equidistant.
		 *
		 * @param nGranules           the number of time granules
		 * @param isEquidistant       <code>true</code> if the time granules are
		 *                            equidistant
		 * @param nanosecondsTcbBegin the start time in nanoseconds TCB
		 * @param nanosecondsTcbEnd   the end time in nanoseconds TCB
		 */
		public Header(final int nGranules, final boolean isEquidistant, final long nanosecondsTcbBegin, final long nanosecondsTcbEnd) {

			this.nGranules = nGranules;
			this.isEquidistant = isEquidistant;
			this.nanosecondsTcbBegin = nanosecondsTcbBegin;
			this.nanosecondsTcbEnd = nanosecondsTcbEnd;
		}

		/**
		 * Compares the given and this <code>{@link Header}</code>. Returns
		 * <code>true</code> if they contain the same data, <code>false</code>
		 * otherwise.
		 *
		 * @param header the <code>{@link Header}</code>
		 *
		 * @return <code>true</code> if this and the given
		 *         <code>{@link Header}</code> are equal
		 */
		public boolean equals(final Header header) {

			if (this.nGranules != header.nGranules) {
				return false;
			}
			if (this.isEquidistant != header.isEquidistant) {
				return false;
			}
			if (this.nanosecondsTcbBegin != header.nanosecondsTcbBegin) {
				return false;
			}
			if (this.nanosecondsTcbEnd != header.nanosecondsTcbEnd) {
				return false;
			}

			return true;
		}
	}

	/**
	 * The class <code>{@link Coefficients}</code> contains the begin times and the
	 * Chebyshev polynomial coefficient data for the three dimensional vectors of
	 * the time granules.
	 */
	public static class Coefficients {

		/** The begin times of the time granules */
		public long[] nanoSecondsTcb;
		/**
		 * The Chebyshev coefficient data as array
		 * <code>[granule][dimension][coefficient]</code>
		 */
		public double[][][] data;

		/**
		 * Initialises this <code>{@link Data}</code> with the given array of begin
		 * times in nanoseconds TCB as well as Chebyshev polynomial coefficient data as
		 * array <code>[granule][dimension][coefficient]</code> for the positions and
		 * velocities.
		 *
		 * @param nanoSecondsTcb the array of begin times in nanoseconds TCB
		 * @param data           the Chebyshev polynomial coefficient data as array
		 *                       <code>[granule][dimension][coefficient]</code> for the
		 *                       positions
		 */
		public Coefficients(final long[] nanoSecondsTcb, final double[][][] data) {

			this.nanoSecondsTcb = nanoSecondsTcb;
			this.data = data;
		}
	}

}
