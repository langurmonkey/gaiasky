/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.event;

public class Telegram implements Comparable<Telegram> {

    /** Time in ms at which this telegram must be served **/
    long timestamp;
    Events event;
    Object[] data;

    public Telegram() {
    }

    public Telegram(Events event, long time, final Object... data) {
        this.event = event;
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
