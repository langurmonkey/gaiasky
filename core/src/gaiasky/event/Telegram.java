/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.event;

public class Telegram implements Comparable<Telegram> {

    /** Time in ms at which this telegram must be served **/
    long timestamp;
    Event event;
    Object source;
    Object[] data;

    public Telegram() {
    }

    public Telegram(Event event, Object source, long time, final Object... data) {
        this.event = event;
        this.source = source;
        this.timestamp = time;
        this.data = data;
    }

    @Override
    public int compareTo(Telegram o) {
        if (this.equals(o))
            return 0;
        return (this.timestamp - o.timestamp < 0) ? -1 : 1;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

}
