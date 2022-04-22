/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * GaiaTools
 * Copyright (C) 2006 Gaia Data Processing and Analysis Consortium
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package gaiasky.util.gaia;


/**
 * This class implements a combination of the Ecliptic Pole Scanning Law (EPSL)
 * and the Nominal Scanning Law (NSL), by switching from EPSL to NSL at the
 * reference epoch (tRef). Since the scanning law parameters (in particular
 * nuRef and omegaRef) apply to tRef, this means there is no discontinuity in
 * the motion of the z axis when switching from EPSL to NSL.
 * 
 * @author Lennart Lindegren
 */
public class EpslAndNsl extends AnalyticalAttitudeDataServer {
	
	private final Epsl epsl = new Epsl();
	private final Nsl37 nsl = new Nsl37();

	/**
	 * Default constructor:
	 */
	public EpslAndNsl() {
		setDefault();
		copyRefValues();
	}

	/**
	 * Constructor for arbitrary reference time (= switch from EPSL to NSL)
	 * and Epsl mode (PRECEDING or FOLLOWING):
	 * @param tRef time of the switch
	 * @param mode which mode to switch to
	 */
	public EpslAndNsl(long tRef, Epsl.Mode mode) {
		setDefault();
		if (mode == Epsl.Mode.FOLLOWING) {
			this.setNuRef(Math.PI);
		}
		copyRefValues();
		setRefTime(tRef);
	}
	
	/**
	 * Copy reference values from this to Epsl and Nsl37:
	 */
	private void copyRefValues() {
		// set reference parameters at tRef for EPSL:
		epsl.setXiRef(getXiRef());
		epsl.setNuRef(getNuRef());
		epsl.setOmegaRef(getOmegaRef());
		epsl.setTargetScanPeriod(getTargetScanPeriod());
		epsl.setInitialized(true); // no need to recompute anything

		// set NSL parameters = new Nsl37();
		nsl.setXiRef(getXiRef());
		nsl.setNuRef(getNuRef());
		nsl.setOmegaRef(getOmegaRef());
		nsl.setTargetPrecessionRate(getTargetPrecessionRate());
		nsl.setTargetScanPeriod(getTargetScanPeriod());
		nsl.setInitialized(false);
	}

	/**
	 * @see gaiasky.util.gaia.AnalyticalAttitudeDataServer#getAttitude(long)
	 * 
	 * @param tNow - the time elapsed since the epoch of J2010 in ns (TCB)
	 * @return attitude for the given time
	 */
	@Override
	public IAttitude getAttitudeNative(long tNow)  {
		if (!initialized) {
			copyRefValues();
			super.setInitialized(true);
		}

		// The reference time is the time to switch.
		if (tNow < getRefTime()) {
			return epsl.getAttitude(tNow);
		} else {
			return nsl.getAttitude(tNow);
		}
	}
	
	@Override
	public void setRefTime(long t) {
		super.setRefTime(t);
		
		nsl.setRefTime(t);
		epsl.setRefTime(t);
		
		setInitialized(false);
	}
}
