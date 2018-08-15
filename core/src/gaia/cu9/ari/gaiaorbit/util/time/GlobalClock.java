package gaia.cu9.ari.gaiaorbit.util.time;

import java.time.Instant;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.util.Constants;

/**
 * Keeps pace of the simulation time vs real time and holds the global clock. It
 * uses a time warp factor which is a multiplier to real time.
 * 
 * @author Toni Sagrista
 *
 */
public class GlobalClock implements IObserver, ITimeFrameProvider {

	private static final double MS_TO_HOUR = 1 / 3600000d;

	/** The current time of the clock **/
	public Instant time;
	long lastTime;
	/** Target time to stop the clock, if any **/
	private Instant targetTime;
	/** The hour difference from the last frame **/
	public double hdiff;

	/** Represents the time wrap multiplier. Scales the real time **/
	public double timeWarp = 1;

	// Seconds since last event POST
	private float lastUpdate = 1;
	/**
	 * The fixed frame rate when not in real time. Set negative to use real time
	 **/
	public float fps = -1;

	/**
	 * Creates a new GlobalClock
	 * 
	 * @param timeWrap The time wrap multiplier
	 * @param instant  The instant with which to initialise the clock
	 */
	public GlobalClock(double timeWrap, Instant instant) {
		super();
		// Now
		this.timeWarp = timeWrap;
		hdiff = 0d;
		time = instant;
		targetTime = null;
		lastTime = time.toEpochMilli();
		EventManager.instance.subscribe(this, Events.PACE_CHANGE_CMD, Events.TIME_WARP_DECREASE_CMD,
				Events.TIME_WARP_INCREASE_CMD, Events.TIME_CHANGE_CMD, Events.TARGET_TIME_CMD);
	}

	double msacum = 0d;

	/**
	 * Update function
	 * 
	 * @param dt Delta time in seconds
	 */
	public void update(double dt) {
		if (dt != 0) {
			// In case we are in constant rate mode
			if (fps > 0) {
				dt = 1 / fps;
			}

			int sign = (int) Math.signum(timeWarp);
			double h = Math.abs(dt * timeWarp * Constants.S_TO_H);
			hdiff = h * sign;

			double ms = sign * h * Constants.H_TO_MS;

			long currentTime = time.toEpochMilli();
			lastTime = currentTime;

			long newTime = currentTime + (long) ms;
			// Check target time
			if (targetTime != null) {
				long target = targetTime.toEpochMilli();
				if ((timeWarp > 0 && currentTime <= target && newTime > target)
						|| (timeWarp < 0 && currentTime >= target && newTime < target)) {
					newTime = target;
					// Unset target time
					targetTime = null;
					// STOP!
					setTimeWarp(0);
				}
			}

			if (newTime > Constants.MAX_TIME_MS) {
				newTime = Constants.MAX_TIME_MS;
				if (currentTime < Constants.MAX_TIME_MS) {
					EventManager.instance.post(Events.POST_NOTIFICATION,
							"Maximum time reached (" + (Constants.MAX_TIME_MS * Constants.MS_TO_Y) + " years)!");
					// Turn off time
					EventManager.instance.post(Events.TOGGLE_TIME_CMD, false, false);
				}
				newTime = Constants.MAX_TIME_MS;
				time = Instant.ofEpochMilli(newTime);
				EventManager.instance.post(Events.TIME_CHANGE_INFO, time);
				lastUpdate = 0;
			} else if (newTime < Constants.MIN_TIME_MS) {
				newTime = Constants.MIN_TIME_MS;
				if (currentTime > Constants.MIN_TIME_MS) {
					EventManager.instance.post(Events.POST_NOTIFICATION,
							"Minimum time reached (" + (Constants.MIN_TIME_MS * Constants.MS_TO_Y) + " years)!");
					// Turn off time
					EventManager.instance.post(Events.TOGGLE_TIME_CMD, false, false);
				}
				newTime = Constants.MIN_TIME_MS;
				time = Instant.ofEpochMilli(newTime);
				EventManager.instance.post(Events.TIME_CHANGE_INFO, time);
				lastUpdate = 0;
			} else {
				time = Instant.ofEpochMilli(newTime);
			}

			// Post event each 1/2 second
			lastUpdate += dt;
			if (lastUpdate > .5) {
				EventManager.instance.post(Events.TIME_CHANGE_INFO, time);
				lastUpdate = 0;
			}
		} else if (time.toEpochMilli() - lastTime != 0) {
			hdiff = (time.toEpochMilli() - lastTime) * MS_TO_HOUR;
			lastTime = time.toEpochMilli();
		} else {
			hdiff = 0d;
		}
	}

	@Override
	public Instant getTime() {
		return time;
	}

	@Override
	public void notify(Events event, Object... data) {
		switch (event) {
		case TARGET_TIME_CMD:
			if (data.length > 0) {
				targetTime = (Instant) data[0];
			} else {
				targetTime = null;
			}
			break;
		case PACE_CHANGE_CMD:
			// Update pace
			setTimeWarp((Double) data[0]);
			break;
		case TIME_WARP_INCREASE_CMD:
			double tw;
			if (timeWarp == 0) {
				tw = 0.125;
			} else if (timeWarp == -0.125) {
				tw = 0;
			} else if (timeWarp < 0) {
				tw = timeWarp / 2.0;
			} else {
				tw = timeWarp * 2.0;
			}
			setTimeWarp(tw);
			break;
		case TIME_WARP_DECREASE_CMD:
			if (timeWarp == 0.125) {
				tw = 0;
			} else if (timeWarp == 0) {
				tw = -0.125;
			} else if (timeWarp < 0) {
				tw = timeWarp * 2.0;
			} else {
				tw = timeWarp / 2.0;
			}
			setTimeWarp(tw);
			break;
		case TIME_CHANGE_CMD:
			// Update time
			Instant newinstant = ((Instant) data[0]);
			long newt = newinstant.toEpochMilli();
			boolean updt = false;
			if (newt > Constants.MAX_TIME_MS) {
				newt = Constants.MAX_TIME_MS;
				EventManager.instance.post(Events.POST_NOTIFICATION,
						"Time overflow, set to maximum (" + (Constants.MIN_TIME_MS * Constants.MS_TO_Y) + " years)");
				updt = true;
			}
			if (newt < Constants.MIN_TIME_MS) {
				newt = Constants.MIN_TIME_MS;
				EventManager.instance.post(Events.POST_NOTIFICATION,
						"Time overflow, set to minimum (" + (Constants.MIN_TIME_MS * Constants.MS_TO_Y) + " years)");
				updt = true;
			}
			if (updt) {
				this.time = Instant.ofEpochMilli(newt);
			} else {
				this.time = newinstant;
			}
			break;
		default:
			break;
		}

	}

	public void setTimeWarp(double tw) {
		this.timeWarp = tw;
		checkTimeWarpValue();
		EventManager.instance.post(Events.PACE_CHANGED_INFO, this.timeWarp);
	}

	private void checkTimeWarpValue() {
		if (timeWarp > Constants.MAX_WARP) {
			timeWarp = Constants.MAX_WARP;
		}
		if (timeWarp < Constants.MIN_WARP) {
			timeWarp = Constants.MIN_WARP;
		}
	}

	/**
	 * Provides the time difference in hours
	 */
	@Override
	public double getDt() {
		return hdiff;
	}

	@Override
	public double getWarpFactor() {
		return timeWarp;
	}

	public boolean isFixedRateMode() {
		return fps > 0;
	}

	@Override
	public float getFixedRate() {
		return fps;
	}

	@Override
	public boolean isTimeOn() {
		return timeWarp != 0d;
	}

}
