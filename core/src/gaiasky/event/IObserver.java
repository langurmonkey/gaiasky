/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.event;

/**
 * Interface to be implemented by anyone willing to watch and be notified of events.
 */
public interface IObserver {

    /**
     * Event notification call.
     *
     * @param event  The event type.
     * @param source The source object, if any.
     * @param data   The data associated with this event.
     */
    void notify(final Event event, Object source, final Object... data);

}
