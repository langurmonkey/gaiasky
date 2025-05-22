/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

/**
 * Holds attributes used to update an orbit/trajectory by fetching new data in TLE (Two-Line Element set) format from a URL.
 */
public class TLESource implements Component {

    /** URL to fetch the TLE data. **/
    public String urlTLE;
    /** Name of the satellite/spacecraft in the TLE data file. **/
    public String nameTLE;
    /** Update interval, in days. Set negative to update every time, disregarding the last update time. **/
    public double tleUpdateInterval = 10.0;

}
