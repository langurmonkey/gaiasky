package gaiasky.util.coord.chebyshev;

import gaiasky.util.coord.AstroUtils;

/**
 * The data-model class <code>{@link SdtTime}</code> holds a moment in time as
 * number of elapsed nanoseconds in an unspecified time scale since the Gaia
 * time reference epoch J2010.0 = JD2455197.5 = 2010-01-01T00:00:00.0. Note that
 * this reference epoch is numerically the same for all time scales (except
 * OBMT). This means, however, that the reference epochs of the individual time
 * scales do not specify the same moment in time. These reference epochs bear no
 * relation to the Gaia scanning law reference epochs or the Gaia catalogue
 * epochs.
 * <p>
 * This data-model class cannot be instantiated because its method arguments are
 * not type-safe. Only its child classes extending the class for one specific
 * time scale can be instantiated. And is their job to make sure that different
 * times cannot be mixed up.
 *
 * @author {@literal Wolfgang Löffler <loeffler@ari.uni-heidelberg.de>}
 */
public abstract class SdtTime {

	/**
	 * The immutable number of elapsed nanoseconds since the reference epoch of the
	 * specific time scale.
	 */
	public final long nanoSeconds;

	/**
	 * The reference epoch of the specific time scale as Julian Date. Maybe be
	 * <code>{@link Double#NaN}</code> if the reference epoch is not a priori
	 * defined.
	 */
	public final double referenceEpoch;

	/** The specific time scale of this <code>{@link SdtTime}</code> */
	private final SdtTime.Scale timeScale;

	/**
	 * Initialises this <code>{@link SdtTime}</code> with the given number of
	 * elapsed nanoseconds since the given reference epoch as Julian Date and the
	 * given time scale.
	 *
	 * @param nanoSeconds    the number of elapsed nanoseconds
	 * @param referenceEpoch the reference epoch as Julian Date
	 * @param timeScale      the <code>{@link SdtTime.Scale}</code>
	 */
	protected SdtTime(final long nanoSeconds, final double referenceEpoch, final SdtTime.Scale timeScale) {

		this.nanoSeconds = nanoSeconds;
		this.referenceEpoch = referenceEpoch;
		this.timeScale = timeScale;
	}

	/**
	 * Returns <code>true</code> if the first given <code>{@link SdtTime}</code> is
	 * within the second one plus/minus the given delta.
	 *
	 * @param time1    the first <code>{@link SdtTime}</code>
	 * @param time2    the second <code>{@link SdtTime}</code>
	 * @param deltaT   the delta <code>{@link SdtTime}</code>
	 * @param timeUnit the <code>{@link SdtTime.Unit}</code> in which the deltaT is
	 *                 given
	 *
	 * @return <code>true</code>, if the first given <code>{@link SdtTime}</code> is
	 *         within the second one plus/minus the given delta
	 */
	protected static boolean isWithin(final SdtTime time1, final SdtTime time2, final long deltaT, final Unit timeUnit) {

		return Math.abs(time1.nanoSeconds - time2.nanoSeconds) <= deltaT * timeUnit.asNanoSeconds;
	}

	/**
	 * Returns <code>true</code<> if the given <code>{@link SdtTime}</code> is
	 * within the given time interval (begin inclusive, end exclusive).
	 *
	 * @param time      the <code>{@link SdtTime}</code> to check
	 * @param timeBegin the inclusive begin <code>{@link SdtTime}</code> of the time
	 *                  interval
	 * @param timeEnd   the exclusive end <code>{@link SdtTime}</code> of the time
	 *                  interval
	 *
	 * @return <code>true</code<> if the given <code>{@link SdtTime}</code> is
	 *         within the given time interval
	 */
	protected static boolean isWithin(final SdtTime time, final SdtTime timeBegin, final SdtTime timeEnd) {

		return timeBegin.nanoSeconds <= time.nanoSeconds && time.nanoSeconds < timeEnd.nanoSeconds;
	}

	/**
	 * Returns <code>true</code> if the first given <code>{@link SdtTime}</code> has
	 * passed the second one.
	 *
	 * @param time1 the passing <code>{@link SdtTime}</code>
	 * @param time2 the <code>{@link SdtTime}</code> to be passed
	 *
	 * @return <code>true</code> if the first given <code>{@link SdtTime}</code> has
	 *         passed the second one
	 */
	protected static boolean hasPassed(final SdtTime time1, final SdtTime time2) {

		return time1.nanoSeconds > time2.nanoSeconds;
	}

	/**
	 * Returns the ISO date representation of the given
	 * <code>{@link SdtTime}</code>.
	 *
	 * @param time the <code>{@link SdtTime}</code>
	 *
	 * @return the ISO date <code>{@link String}</code>
	 */
	protected static String asIsoDate(final SdtTime time) {

		final JulianDate jd = SdtTime.asJulianDate(time);

		// correct for the fact that Julian Dates
		// start at noon of the preceding day rather than at midnight

		double jdFraction = jd.fraction + 0.5;
		int jdDay = jd.day + (int) jdFraction;
		jdFraction -= (int) jdFraction;

		// get integer hour, minute and second
		// round fractional second to one decimal digit

		final double dhour = jdFraction * 24.0;
		final double dmin = dhour % 1 * 60.0;
		final double dsec = dmin % 1 * 60.0;

		int hour = (int) dhour;
		int min = (int) dmin;
		int sec = (int) dsec;
		int frac = (int) Math.round(dsec % 1 * 1000.0);

		// frac contains now the fraction of a second in milliseconds
		// the rounding may have resulted in a value of frac>=1000
		// if this is the case, increment the minutes

		if (frac >= 1000) {
			frac -= 1000;
			sec += 1;
			if (sec >= 60) {
				sec -= 60;
				min += 1;
				if (min >= 60) {
					min -= 60;
					hour += 1;
					if (hour >= 24) {
						hour -= 24;
						jdDay += 1;
					}
				}
			}
		}

		// get year, month, day in the Gregorian calendar

		int a = jdDay;
		if (a >= 2299161) {
			final int x = (int) ((jdDay - 1867216.25) / 36524.25);
			a = jdDay + 1 + x - x / 4;
		}
		final int b = a + 1524;
		final int c = (int) ((b - 122.1) / 365.25);
		final int d = (int) (365.25 * c);
		final int e = (int) ((b - d) / 30.6001);

		final int day = (int) (b - d - (long) (30.6001 * e));
		final int month = e <= 13 ? e - 1 : e - 13;
		final int year = month - 1 >= 2 ? c - 4716 : c - 4715;

		return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03d [%s]",
				year, month, day, hour, min, sec, frac, time.timeScale);
	}

	/**
	 * Returns the given <code>{@link SdtTime}</code> as
	 * <code>{@link SdtTime.JulianDate}</code>. If the reference epoch of the given
	 * <code>{@link SdtTime}</code> is <code>{@link Double#NaN}</code>, the returned
	 * Julian Date will also contain <code>{@link Double#NaN}</code> values.
	 *
	 * @return the <code>{@link SdtTime.JulianDate}</code>
	 */
	protected static SdtTime.JulianDate asJulianDate(final SdtTime sdtTime) {

		return SdtTime.asJulianDate(sdtTime.nanoSeconds, sdtTime.referenceEpoch);
	}

	/**
	 * Returns the given number of elapsed nanoseconds since the given reference
	 * Epoch as <code>{@link SdtTime.JulianDate}</code>.
	 *
	 * @param nanoSeconds    the number elapsed nanoseconds
	 * @param referenceEpoch the reference epoch as Julian Date
	 *
	 * @return the <code>{@link SdtTime.JulianDate}</code>
	 */
	protected static SdtTime.JulianDate asJulianDate(final long nanoSeconds, final double referenceEpoch) {

		final double refEpochJdFraction = referenceEpoch % 1.0;
		final int refEpochJdInteger = (int) (referenceEpoch - refEpochJdFraction);

		final int sdtTimeIntegerDays = (int) (nanoSeconds / SdtTime.Unit.DAY.asNanoSeconds);

		int day = sdtTimeIntegerDays + refEpochJdInteger;
		double fraction = (double) (nanoSeconds - sdtTimeIntegerDays * SdtTime.Unit.DAY.asNanoSeconds) / SdtTime.Unit.DAY.asNanoSeconds + refEpochJdFraction;

		// due to the addition of j2010JdFraction = 0.5,
		// fraction may actually turn out to be greater than one

		day += (int) fraction;
		fraction -= (int) fraction;

		return new SdtTime.JulianDate(day, fraction);
	}

	/**
	 * Converts the given <code>{@link SdtTime.JulianDate}</code> to nanoseconds
	 * since the reference epoch J2010.0 = 2010-01-01T00:00:00.0 TCB.
	 *
	 * @param julianDate the <code>{@link JulianDate}</code>
	 *
	 * @return the number of nanoseconds since the reference epoch J2010.0
	 */
	protected static long asNanoSeconds(final SdtTime.JulianDate julianDate, final double referenceEpoch) {

		final double days = julianDate.day - AstroUtils.JD_J2010;
		final double daysJdNsFull = days * SdtTime.Unit.DAY.asNanoSeconds;
		final double daysJdNsFrac = daysJdNsFull - Math.floor(daysJdNsFull);

		final double fracJdNsFull = julianDate.fraction * SdtTime.Unit.DAY.asNanoSeconds;
		final double fracJdNsFrac = fracJdNsFull - Math.floor(fracJdNsFull);

		final long daysJdNs = (long) Math.floor(daysJdNsFull);
		final long fracJdNs = (long) Math.floor(fracJdNsFull);

		return daysJdNs + fracJdNs + Math.round(daysJdNsFrac + fracJdNsFrac);
	}

	/**
	 * Returns a human readable <code>{@link String}</code> representation of this
	 * <code>{@link SdtTime}</code>.
	 *
	 * @return a human readable <code>{@link String}</code> representation of this
	 *         <code>{@link SdtTime}</code>
	 */
	@Override
	public String toString() {

		return SdtTime.asIsoDate(this);
	}

	/**
	 * Returns 0 if the two moments in time are the same. Returns a value less than
	 * 0, if the first given time is earlier than the second one. Returns a value
	 * greater than 0, if the first given time is later than the second one. In
	 * order for this method to make physical sense, both moments of time must be in
	 * the same time scale.
	 *
	 * @param sdtTime1 the first moment in <code>{@link SdtTime}</code>
	 * @param sdtTime2 the second moment in <code>{@link SdtTime}</code>
	 *
	 * @return 0 if the two moments in time are the same. Returns a value less than
	 *         0, if the first given time is earlier than the second one. Returns a
	 *         value greater than 0, if the first given time is later than the
	 *         second one.
	 */
	protected static int compare(final SdtTime sdtTime1, final SdtTime sdtTime2) {

		return Long.compare(sdtTime1.nanoSeconds, sdtTime2.nanoSeconds);
	}

	/**
	 * The enum <code>{@link SdtTime.Unit}</code> lists the defined time units.
	 */
	public static enum Unit {

		/** nanosecond */
		NANOSECOND(1L),
		/** microsecond = 10^3 nanoseconds */
		MICROSECOND(1000L),
		/** millisecond = 10^6 nanoseconds */
		MILLISECOND(1000L * 1000L),
		/** second = 10^9 nanoseconds */
		SECOND(1000L * 1000L * 1000L),
		/** minute = 6×10^10 nanoseconds */
		MINUTE(60L * 1000L * 1000L * 1000L),
		/** hour = 3.6×10^12 nanoseconds */
		HOUR(3600L * 1000L * 1000L * 1000L),
		/** day = 8.64×10^13 nanoseconds */
		DAY(86400L * 1000L * 1000L * 1000L),
		/** year = 3.15576×10^15 nanoseconds, i.e. 365.25 * 86400.0 */
		YEAR(31557600L * 1000L * 1000L * 1000L),
		/** revolution = 6 hours = 2.16×10^13 nanoseconds */
		REVOLUTION(6L * 3600L * 1000L * 1000L * 1000L);

		/**
		 * The scaling factor of the <code>{@link SdtTime.Unit}</code> to seconds. Is
		 * zero for units shorter than a second.
		 */
		public long asSeconds;

		/**
		 * The scaling factor of the <code>{@link SdtTime.Unit}</code> to nanoseconds
		 */
		public long asNanoSeconds;

		/**
		 * Initialises this <code>{@link Unit}</code> with the given number of
		 * nanoseconds.
		 *
		 * @param nanoSeconds the number of nanoseconds in this
		 *                    <code>{@link SdtTime.Unit}</code>
		 */
		private Unit(final long nanoSeconds) {
			this.asNanoSeconds = nanoSeconds;
			this.asSeconds = this.asNanoSeconds / (1000L * 1000L * 1000L);
		}
	}

	/**
	 * The marker interface <code>{@link Scale}</code> abstracts the specific time
	 * scales.
	 */
	public interface Scale {
	}

	/**
	 * The class <code>{@link JulianDate}</code> encodes a moment in time as Julian
	 * Date as the summer of the integer and fractional parts in order to achieve
	 * the needed nano-second resolution.
	 */
	public static class JulianDate {

		/** The integer day part of the Julian Date */
		public final int day;
		/** The fractional day part of the Julian Date */
		public final double fraction;

		/**
		 * Initialises this <code>{@link JulianDate}</code> with the given integer and
		 * fractional day parts.
		 *
		 * @param day      the integer part of the Julian date
		 * @param fraction the fractional part of the Julian date
		 */
		public JulianDate(final int day, final double fraction) {

			this.day = day;
			this.fraction = fraction;
		}

		/**
		 * Initialises this <code>{@link JulianDate}</code> with the given low
		 * resolution <code>double</code> Julian Date.
		 *
		 * @param dayFraction the low resolution Julian Date
		 */
		public JulianDate(final double dayFraction) {

			this.day = (int) dayFraction;
			this.fraction = dayFraction - this.day;
		}

		/**
		 * Initialises this <code>{@link JulianDate}</code> from the given one.
		 *
		 * @param julianDate the <code>{@link JulianDate}</code>
		 */
		public JulianDate(final SdtTime.JulianDate julianDate) {

			this.day = julianDate.day;
			this.fraction = julianDate.fraction;
		}
	}

	/**
	 * The inner class <code>{@link Range}</code> implements a time interval. The
	 * begin time is inclusive, the end time exclusive.
	 */
	protected static class Range<T extends SdtTime> {

		/** The begin <code>{@link SdtTime}</code> of the time interval */
		public final T begin;
		/** The end <code>{@link SdtTime}</code> of the time interval */
		public final T end;

		/**
		 * Initialises this <code>{@link Range}</code> with the given begin and end
		 * <code>{@link SdtTime}s</code>.
		 *
		 * @param timeBegin
		 * @param timeEnd
		 */
		public Range(final T timeBegin, final T timeEnd) {

			this.begin = timeBegin;
			this.end = timeEnd;
		}

		/**
		 * Returns <code>true</code>, if the given <code>{@link SdtTime}</code> is
		 * within this <code>{@link Range}</code>.
		 *
		 * @param time the <code>{@link SdtTime}</code> to check
		 *
		 * @return <code>true</code>, if the given <code>{@link SdtTime}</code> is
		 *         within this <code>{@link Range}</code>
		 */
		public boolean isWithin(final T time) {

			return SdtTime.isWithin(time, this.begin, this.end);
		}

		/**
		 * Returns the length of this <code>{@link Range}</code> as nanoseconds.
		 *
		 * @return the length of this <code>{@link Range}</code> as nanoseconds.
		 */
		public long asNanoSeconds() {

			return this.end.nanoSeconds - this.begin.nanoSeconds;
		}

		/**
		 * Returns a human readable <code>{@link String}</code> representation of this
		 * <code>{@link SdtTime.Range}</code>.
		 *
		 * @return a human readable <code>{@link String}</code> representation of this
		 *         <code>{@link SdtTime.Range}</code>
		 */
		@Override
		public String toString() {

			final String string = "[" + this.begin.toString() + ", " + this.end + ")";

			return string;
		}
	}

}
