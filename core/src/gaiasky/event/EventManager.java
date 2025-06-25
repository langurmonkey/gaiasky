/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.event;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.scene.entity.EntityRadio;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * This is the main manager of events within Gaia Sky. It enables the subscription of {@link IObserver} objects to
 * specific events, and also the submission of events (optionally delayed).
 * </p><p>
 * See {@link Event} for information on the events.
 * </p>
 */
public class EventManager implements IObserver {

    /** Singleton pattern **/
    public static final EventManager instance = new EventManager();
    /** Queue of delayed messages (one per time frame). **/
    private final Map<TimeFrame, PriorityQueue<Telegram>> delayedQueue;
    /** Queue of messages waiting for a consumer. */
    private final List<Telegram> waitingList;
    /** Telegram pool **/
    private final Pool<Telegram> pool;
    /** Subscriptions Event-Observers **/
    private final Map<Integer, Set<IObserver>> subscriptions = new ConcurrentHashMap<>();
    /** The time frame to use if none is specified **/
    private TimeFrame defaultTimeFrame;

    public EventManager() {
        this.pool = new Pool<>(20) {
            protected Telegram newObject() {
                return new Telegram();
            }
        };
        // Initialize queues, one for each time frame.
        delayedQueue = new HashMap<>(TimeFrame.values().length);
        for (TimeFrame tf : TimeFrame.values()) {
            PriorityQueue<Telegram> pq = new PriorityQueue<>();
            delayedQueue.put(tf, pq);
        }
        defaultTimeFrame = TimeFrame.REAL_TIME;
        waitingList = Collections.synchronizedList(new ArrayList<>());
        subscribe(this, Event.EVENT_TIME_FRAME_CMD);
    }

    /**
     * Register a new event to the default event manager instance with the given source
     * and data.
     *
     * @param event  The event.
     * @param source The source object, if any.
     * @param data   The event data.
     */
    public static void publish(final Event event, Object source, final Object... data) {
        instance.post(event, source, data);
    }

    /**
     * Register a new delayed event in the default manager with the given type, data, delay and the default
     * time frame. The default time frame can be changed using the event
     * {@link Event#EVENT_TIME_FRAME_CMD}. The event will be passed along after
     * the specified delay time [ms] in the given time frame has passed.
     *
     * @param event   The event.
     * @param source  The source object, if any.
     * @param delayMs Milliseconds of delay in the given time frame.
     * @param data    The event data.
     */
    public static void publishDelayed(Event event, Object source, long delayMs, Object... data) {
        instance.postDelayed(event, source, delayMs, data);
    }

    /**
     * Register a new event with the given source and with the given data. The message is automatically
     * dispatched if at least one consumer is present. Otherwise, it waits until a consumer is available.
     *
     * @param event  The event.
     * @param source The source object, if any.
     * @param data   The event data.
     */
    public static void publishWaitUntilConsumer(final Event event, Object source, final Object... data) {
       instance.postWaitUntilConsumer(event, source, data);
    }

    /**
     * Subscribe the given observer to the given event types.
     *
     * @param observer The observer to subscribe.
     * @param events   The event types to subscribe to.
     */
    public void subscribe(IObserver observer, Event... events) {
        for (Event event : events) {
            subscribe(observer, event);
        }
    }

    /**
     * Register a listener for the specified message code. Messages without an
     * explicit receiver are broadcast to all its registered listeners.
     *
     * @param msg      the message code
     * @param listener the listener to add
     */
    public void subscribe(IObserver listener, Event msg) {
        Set<IObserver> listeners = subscriptions.computeIfAbsent(msg.ordinal(), k -> ConcurrentHashMap.newKeySet(3));
        // Associate an empty ordered array with the message code. Sometimes the order matters
        listeners.add(listener);
    }

    public void unsubscribe(IObserver listener, Event... events) {
        for (Event event : events) {
            unsubscribe(listener, event);
        }
    }

    /**
     * Unregister the specified listener for the specified message code.
     *
     * @param event    The message code.
     * @param listener The listener to remove.
     **/
    public void unsubscribe(IObserver listener, Event event) {
        Set<IObserver> listeners = subscriptions.get(event.ordinal());
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Unregister all the subscriptions of the given listener.
     *
     * @param listener The listener to remove.
     */
    public void removeAllSubscriptions(IObserver listener) {
        Set<Integer> km = subscriptions.keySet();
        for (int key : km) {
            subscriptions.get(key).remove(listener);
        }
    }

    /**
     * Unregister all the subscriptions of the given listeners.
     *
     * @param listeners The listeners to remove.
     */
    public void removeAllSubscriptions(IObserver... listeners) {
        Set<Integer> km = subscriptions.keySet();
        for (int key : km) {
            for (IObserver listener : listeners) {
                subscriptions.get(key).remove(listener);
            }
        }
    }

    /**
     * Remove all subscriptions of {@link gaiasky.scene.entity.EntityRadio} for the given entity.
     *
     * @param entity The entity.
     */
    public void removeRadioSubscriptions(Entity entity) {
        Set<Integer> km = subscriptions.keySet();
        for (int key : km) {
            Set<IObserver> set = subscriptions.get(key);
            Iterator<IObserver> it = set.iterator();
            while (it.hasNext()) {
                IObserver obs = it.next();
                if (obs instanceof EntityRadio radio) {
                    if (radio.getEntity() == entity) {
                        it.remove();
                    }
                }
            }
        }
    }

    public void clearAllSubscriptions() {
        subscriptions.clear();
    }

    /**
     * Register a new data-less event with the given source.
     *
     * @param event  The event.
     * @param source The source object, if any.
     */
    public void post(final Event event, Object source) {
        post(event, source, new Object[0]);
    }

    /**
     * Register a new event with the given source and with the given data.
     *
     * @param event  The event.
     * @param source The source object, if any.
     * @param data   The event data.
     */
    public void post(final Event event, Object source, final Object... data) {
        Set<IObserver> observers = subscriptions.get(event.ordinal());
        if (observers != null && !observers.isEmpty()) {
            for (IObserver observer : observers) {
                observer.notify(event, source, data);
            }
        }
    }

    /**
     * Register a new event with the given source and with the given data. The message is automatically
     * dispatched if at least one consumer is present. Otherwise, it waits until a consumer is available.
     *
     * @param event  The event.
     * @param source The source object, if any.
     * @param data   The event data.
     */
    public void postWaitUntilConsumer(final Event event, Object source, final Object... data) {
        Set<IObserver> observers = subscriptions.get(event.ordinal());
        if (observers == null || observers.isEmpty()) {
            // Add to consumers queue.
            Telegram t = pool.obtain();
            t.event = event;
            t.source = source;
            t.data = data;
            t.timestamp = defaultTimeFrame.getCurrentTimeMs();
            waitingList.add(t);
        } else {
            // Dispatch.
            for (IObserver observer : observers) {
                observer.notify(event, source, data);
            }
        }
    }


    /**
     * Register a new delayed event with the given type, data, delay and the default
     * time frame. The default time frame can be changed using the event
     * {@link Event#EVENT_TIME_FRAME_CMD}. The event will be passed along after
     * the specified delay time [ms] in the given time frame has passed.
     *
     * @param event   The event.
     * @param source  The source object, if any.
     * @param delayMs Milliseconds of delay in the given time frame.
     * @param data    The event data.
     */
    public void postDelayed(Event event, Object source, long delayMs, Object... data) {
        postDelayed(event, source, delayMs, defaultTimeFrame, data);
    }

    /**
     * Register a new event with the given type, data, delay and time frame. The event will
     * be passed along after the specified delay time [ms] in the given time
     * frame has passed.
     *
     * @param event   The event.
     * @param source  The source object, if any.
     * @param delayMs Milliseconds of delay in the given time frame.
     * @param frame   The time frame, either real time (user) or simulation time
     *                (simulation clock time).
     * @param data    The event data.
     */
    public void postDelayed(Event event, Object source, long delayMs, TimeFrame frame, Object... data) {
        if (delayMs <= 0) {
            post(event, source, data);
        } else {
            Telegram t = pool.obtain();
            t.event = event;
            t.source = source;
            t.data = data;
            t.timestamp = frame.getCurrentTimeMs() + delayMs;

            // Add to queue
            delayedQueue.get(frame).add(t);
        }
    }

    /**
     * Dispatches any telegrams with a timestamp that has expired. Any
     * dispatched telegrams are removed from the queue.
     * <p>
     * This method must be called each time through the main loop.
     */
    public void dispatchDelayedMessages() {
        // Dispatch delayed messages.
        for (TimeFrame tf : delayedQueue.keySet()) {
            dispatch(delayedQueue.get(tf), tf.getCurrentTimeMs());
        }

        // Dispatch waiting messages.
        if (!waitingList.isEmpty()) {
            var it = waitingList.iterator();
            while (it.hasNext()) {
                var t = it.next();
                var observers = subscriptions.get(t.event.ordinal());
                if (observers != null && !observers.isEmpty()) {
                    for (IObserver observer : observers) {
                        observer.notify(t.event, t.source, t.data);
                    }
                    it.remove();
                }
            }
        }
    }

    private void dispatch(PriorityQueue<Telegram> queue, long currentTime) {
        if (queue.size() == 0)
            return;

        // Now peek at the queue to see if any telegrams need dispatching.
        // Remove all telegrams from the front of the queue that have gone
        // past their time stamp.
        do {
            // Read the telegram from the front of the queue
            final Telegram telegram = queue.peek();
            if (telegram.timestamp > currentTime)
                break;

            // Send the telegram to the recipient
            discharge(telegram);

            // Remove it from the queue
            queue.poll();
        } while (queue.size() > 0);
    }

    private void discharge(Telegram telegram) {
        post(telegram.event, telegram.source, telegram.data);
        // Release the telegram to the pool
        pool.free(telegram);
    }

    public boolean hasSubscriptors(Event event) {
        Set<IObserver> scr = subscriptions.get(event.ordinal());
        return scr != null && !scr.isEmpty();
    }

    public boolean isSubscribedTo(IObserver o, Event event) {
        Set<IObserver> scr = subscriptions.get(event.ordinal());
        return scr != null && scr.contains(o);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.EVENT_TIME_FRAME_CMD) {
            defaultTimeFrame = (TimeFrame) data[0];
        }
    }

    /** Time frame options **/
    public enum TimeFrame {
        /** Real time from the user's perspective **/
        REAL_TIME,
        /** Simulation time in the simulation clock **/
        SIMULATION_TIME;

        public long getCurrentTimeMs() {
            if (this.equals(REAL_TIME)) {
                return TimeUtils.millis();
            } else if (this.equals(SIMULATION_TIME)) {
                return GaiaSky.instance.time.getTime().toEpochMilli();
            }
            return -1;
        }
    }
}
