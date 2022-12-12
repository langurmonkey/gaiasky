package gaiasky.util.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An interface that can optionally use an {@link java.util.concurrent.atomic.AtomicBoolean}
 * as a signal to abort its execution.
 */
public interface AbortableRunnable extends Runnable {
    /**
     * Sets the abort object.
     *
     * @param abort The abort object.
     */
    public void setAbort(AtomicBoolean abort);
}
