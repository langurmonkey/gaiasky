package gaiasky.util;

import gaiasky.event.Event;
import gaiasky.event.EventManager;

/**
 * Wrapper class which handles a one-time update runnable.
 * Use the {@link OneTimeRunnable#post()} method to publish it. The runnable
 * will be published, run once, and unpublished.
 */
public abstract class OneTimeRunnable implements Runnable {
    private final String name;

    public OneTimeRunnable(String name) {
        this.name = name;
    }

    public void post() {
        EventManager.publish(Event.PARK_RUNNABLE, this, name, this);
    }

    @Override
    public void run() {
        // Run process.
        process();
        // Unpark runnable.
        EventManager.publish(Event.UNPARK_RUNNABLE, this, name);
    }

    protected abstract void process();
}
