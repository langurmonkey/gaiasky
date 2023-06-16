package gaiasky.util.coord.chebyshev;

import gaiasky.util.coord.AstroUtils;

/**
 * The class <code>{@link SdtTcb}</code> holds a moment in time in Barycentric
 * Coordinate Time (TCB) as number of elapsed nanoseconds since the Gaia
 * reference epoch J2010.0 = JD2455197.5 = 2010-01-01T00:00:00.0 TCB.
 * <p>
 * TCB is the relativistic coordinate time of the Barycentric Reference System
 * (BCRS) and is equivalent to the proper time experienced by a clock at rest in
 * a coordinate frame co-moving with the Solar System's centre of gravity at
 * infinite distance in a hypothetical universe that contains only the Solar
 * System.
 *
 * @author {@literal Wolfgang Löffler <loeffler@ari.uni-heidelberg.de>}
 */
public class SdtTcb extends SdtTime implements Comparable<SdtTcb> {

	/**
	 * The TCB reference epoch J2010.0 = JD2455197.5 = 2010-01-01T00:00:00.0 TCB.
	 */
	public static final double referenceEpoch = AstroUtils.JD_J2010;

	/**
	 * Initialises this <code>{@link SdtTcb}</code> with the given number of elapsed
	 * nanoseconds TCB since the Gaia reference epoch J2010.0 =
	 * 2010-01-01T00:00:00.0 TCB.
	 *
	 * @param nanoSecondsTcb the number of elapsed nanoseconds TCB
	 */
	public SdtTcb(final long nanoSecondsTcb) {

		super(nanoSecondsTcb, SdtTcb.referenceEpoch, Scale.TCB);
	}

	/**
	 * Initialises this <code>{@link SdtTcb}</code> with the given
	 * <code>{@link SdtTcb}</code>.
	 *
	 * @param tcb the <code>{@link SdtTcb}</code>
	 */
	public SdtTcb(final SdtTcb tcb) {

		super(tcb.nanoSeconds, SdtTcb.referenceEpoch, Scale.TCB);
	}

	/**
	 * Initialises this <code>{@link SdtTcb}</code> with the given <code>
	 * {@link SdtTime.JulianDate}</code>.
	 *
	 * @param julianDate the <code>{@link SdtTime.JulianDate}</code>
	 */
	public SdtTcb(final SdtTime.JulianDate julianDate) {

		super(SdtTime.asNanoSeconds(julianDate, SdtTcb.referenceEpoch), SdtTcb.referenceEpoch, Scale.TCB);
	}

	/**
	 * Initialises this <code>{@link SdtTcb}</code> with the given
	 * <code>{@link SdtTcb}</code> plus the given time delta in the given
	 * <code>{@link SdtTime.Unit}</code>.
	 *
	 * @param tcb           the <code>{@link SdtTcb}</code>
	 * @param deltaTime     the delta time to be added
	 * @param deltaTimeUnit the <code>{@link SdtTime.Unit}</code> of the delta time
	 *                      to be added
	 */
	public SdtTcb(final SdtTcb tcb, final long deltaTime, final SdtTime.Unit deltaTimeUnit) {

		super(tcb.nanoSeconds + deltaTime * deltaTimeUnit.asNanoSeconds, SdtTcb.referenceEpoch, Scale.TCB);
	}

	/**
	 * Returns <code>true</code> if this <code>{@link SdtTcb}</code> is within the
	 * given reference <code>{@link SdtTcb}</code> plus/minus the given delta.
	 *
	 * @param tcb      the reference <code>{@link SdtTcb}</code>
	 * @param deltaT   the delta <code>{@link SdtTcb}</code>
	 * @param timeUnit the <code>{@link SdtTime.Unit}</code> in which the deltaT is
	 *                 given
	 *
	 * @return <code>true</code>, if the first given <code>{@link SdtTcb}</code> is
	 *         within the reference <code>{@link SdtTcb}</code> plus/minus the given
	 *         delta
	 */
	public boolean isWithin(final SdtTcb tcb, final long deltaT, final SdtTime.Unit timeUnit) {

		return SdtTime.isWithin(this, tcb, deltaT, timeUnit);
	}

	/**
	 * Returns <code>true</code> if this <code>{@link SdtTcb}</code> is within the
	 * given time interval (begin exclusive, end inclusive).
	 *
	 * @param tcbBegin the exclusive begin <code>{@link SdtTcb}</code> of the time
	 *                 interval
	 * @param tcbEnd   the inclusive end of the <code>{@link SdtTcb}</code> of the
	 *                 time interval
	 *
	 * @return <code>true</code> if this <code>{@link SdtTcb}</code> is within the
	 *         given time interval
	 */
	public boolean isWithin(final SdtTcb tcbBegin, final SdtTcb tcbEnd) {

		return SdtTime.isWithin(this, tcbBegin, tcbEnd);
	}

	/**
	 * Returns <code>true</code> if this <code>{@link SdtTcb}</code> has passed the
	 * given reference <code>{@link SdtTcb}</code>.
	 *
	 * @param tcb the reference <code>{@link SdtTcb}</code> to be passed
	 *
	 * @return <code>true</code> if this <code>{@link SdtTcb}</code> has passed the
	 *         given reference <code>{@link SdtTcb}</code>
	 */
	public boolean hasPassed(final SdtTcb tcb) {

		return SdtTime.hasPassed(this, tcb);
	}

	/**
	 * Returns this <code>{@link SdtTcb}</code> as
	 * <code>{@link SdtTime.JulianDate}</code>.
	 *
	 * @return the <code>{@link SdtTime.JulianDate}</code>
	 */
	public SdtTime.JulianDate asJulianDate() {

		return SdtTime.asJulianDate(this.nanoSeconds, SdtTcb.referenceEpoch);
	}

	/**
	 * Returns a human readable <code>{@link String}</code> representation of this
	 * <code>{@link SdtTcb}</code> in ISO format.
	 *
	 * @return a human readable <code>{@link String}</code> representation of this
	 *         <code>{@link SdtTcb}</code> inf ISO format
	 */
	@Override
	public String toString() {

		return SdtTime.asIsoDate(this);
	}

	/**
	 * Returns 0 if the this moment in time is the same as the given one. Returns a
	 * value less than 0, if this moment in time is earlier than the given one.
	 * Returns a value greater than 0, if the this moment in time is later than the
	 * second one.
	 *
	 * @param tcb the moment in time <code>{@link SdtTcb}</code> to which to compare
	 *
	 * @return 0 if the this moment in time is the same as the given one. Returns a
	 *         value less than 0, if this moment in time is earlier than the given
	 *         one. Returns a value greater than 0, if the this moment in time is
	 *         later than the second one.
	 */
	@Override
	public int compareTo(final SdtTcb tcb) {

		return SdtTime.compare(this, tcb);
	}

	/**
	 * The enum <code>{@link Scale}</code> specifies TCB as
	 * <code>{@link SdtTime.Scale}</code>.
	 */
	public enum Scale implements SdtTime.Scale {

		/** TCB time scale */
		TCB;
	}

	/**
	 * The inner class <code>{@link Range}</code> implements a TCB interval. The
	 * begin time is inclusive, the end time exclusive.
	 */
	public static class Range extends SdtTime.Range<SdtTcb> {

		/**
		 * Initialises this <code>~{@link Range}</code> with the given TCB interval.
		 *
		 * @param tcbBegin the begin <code>{@link SdtTcb}</code>
		 * @param tcbEnd   the end <code>{@link SdtTcb}</code>
		 */
		public Range(final SdtTcb tcbBegin, final SdtTcb tcbEnd) {

			super(tcbBegin, tcbEnd);
		}
	}

}
