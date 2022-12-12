/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * Copyright (C) 2006-2011 Gaia Data Processing and Analysis Consortium
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
/*
 * GTMSIM
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
package gaiasky.util.gaia.time;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A collection of Good Time Intervals (GTIs).
 *
 * @author jhoar
 * @version $Id: GtiList.java 290526 2013-04-30 11:26:35Z pbalm $
 */
public class GtiList extends ArrayList<Gti> {

    private static final long serialVersionUID = 1L;

    private State state;

    /**
     * Create an empty GTI List
     */
    public GtiList() {
        super();
        this.state = State.UNKNOWN;
    }

    /**
     * Convenience method to add a new GTI
     *
     * @param start T
     * @param end   T
     *
     * @throws RuntimeException
     */
    public void add(final long start, final long end) throws RuntimeException {
        this.add(new Gti(start, end));
    }

    /**
     * Check if a time is within any GTI in the list
     *
     * @param time time to find
     *
     * @return first GTI object in which the time the time is found, null
     * otherwise
     */
    public Gti inside(final long time) {
        for (final Gti t : this) {
            if (t.isInside(time)) {
                return t;
            }
        }

        return null;
    }

    /**
     * sort the list such that t_i_start <= t_i+1_start forall i=0,
     * this->size()-1
     */
    public void sortIt() {
        if (this.state != State.SORTED) {
            Collections.sort(this);
            this.state = State.SORTED;
        }
    }

    /**
     * combine list with second list (logical OR)
     *
     * <pre>
     *    +----------------+ Gti 1 (
     * +-----------+ Gti 2
     * ||
     * \/
     * +-------------------+ combined
     * </pre>
     *
     * @param list List to combine with
     *
     * @throws RuntimeException
     */
    public void or(final GtiList list) throws RuntimeException {
        this.addAll(list);
        this.state = State.UNKNOWN;
        this.reduce();
    }

    /**
     * combine list with second list (logical AND)
     * Return this List combined with second list
     *
     * <pre>
     *     +----------------+ Gti 1
     * +-----------+ Gti 2
     * ||
     * \/
     *     +-------+ combined
     * </pre>
     *
     * @param list List to combine with
     *
     * @throws RuntimeException
     */
    public void and(final GtiList list) throws RuntimeException {
        final GtiList l1 = this;
        final GtiList l2 = list;

        // Reduce the sets first
        l1.reduce();
        l2.reduce();

        final int n1 = l1.size();
        final int n2 = l2.size();

        final GtiList resList = new GtiList();

        int actn2 = 0;

        for (int n = 0; n < n1; n++) {
            while ((actn2 < n2) &&
                    l2.get(actn2).getEnd() < l1.get(n).getStart()) {
                actn2++;
            }

            while ((actn2 < n2) &&
                    (l2.get(actn2).getStart() < l1.get(n).getEnd() ||
                            l2.get(actn2).getStart() == l1.get(n).getEnd())) {
                //
                // overlap found
                //	
                Gti newGti;

                final long l1Start = l1.get(n).getStart();
                final long l1End = l1.get(n).getEnd();
                final long l2Start = l2.get(actn2).getStart();
                final long l2End = l2.get(actn2).getEnd();

                if (l1Start > l2Start) {
                    newGti = new Gti(l1Start, l1Start);
                } else {
                    newGti = new Gti(l2Start, l2Start);
                }

                resList.add(newGti);

                if (l1End < l2End || l1End == l2End) {
                    newGti.setEnd(l1End);

                    break;
                } else {
                    newGti.setEnd(l2End);
                    actn2++;
                }
            }

            // Finished?
            if (actn2 >= n2) {
                break;
            }
        }

        this.clear();
        this.addAll(resList);
    }

    /**
     * Combine overlapping intervals (logical OR)
     *
     * <pre>
     *    +----------------+ Gti 1
     * +-----------+ Gti 2
     * ||
     * \/
     * +-------------------+ combined
     * </pre>
     *
     * @throws RuntimeException
     */
    public void reduce() throws RuntimeException {
        if (this.state == State.REDUCED) {
            // already reduced - nothing to do except returning list
            return;
        }

        // sort first
        this.sortIt();

        final int nGtis = this.size();

        if (nGtis == 0) {
            return;
        }

        int nNewGtis = 0;
        final GtiList newGtiList = new GtiList();
        newGtiList.state = State.REDUCED;
        newGtiList.add(this.get(0)); // copy first GTI

        for (int n = 1; n < nGtis; n++) {
            final long newStart = this.get(n).getStart();
            final long newEnd = this.get(n).getEnd();
            final long end = newGtiList.get(nNewGtis).getEnd();

            if (newStart > end) {
                //
                // a new interval
                //
                newGtiList.add(this.get(n));
                nNewGtis++;
            } else if (newEnd > end) {
                //
                // intervals overlap, and upper boundary of interval <n>
                // extends over interval <nNewGtis>
                //
                newGtiList.get(nNewGtis).setEnd(newEnd);
            }
        }

        this.clear();
        this.addAll(newGtiList);

        this.state = State.REDUCED;
    }

    /**
     * Compute the sum of the TimeIntervals
     *
     * @return sum of all GTI durations [s]
     */
    public double getOntime() {
        double time = 0.0;

        for (final Gti i : this) {
            time += i.getDuration().asSecs();
        }

        return time;
    }

    /**
     * Is the list sorted?
     *
     * @return true if the list sorted, false otherwise
     */
    public boolean isSorted() {
        return (this.state == State.SORTED) || (this.state == State.REDUCED);
    }

    /**
     * Is the list reduced?
     *
     * @return true if the list reduced, false otherwise
     */
    public boolean isReduced() {
        return this.state == State.REDUCED;
    }

    /**
     * @return String
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        for (final Gti i : this) {
            sb.append(i);
            sb.append('\n');
        }

        return sb.toString();
    }

    /**
     *
     */
    private enum State {
        /**
         * Field UNKNOWN.
         */
        UNKNOWN,
        /**
         * Field SORTED.
         */
        SORTED,
        /**
         * Field REDUCED.
         */
        REDUCED
    }
}
