package gaiasky.util.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread that waits for a task to be executed. Tasks can
 * be aborted.
 */
public class ServiceThread extends Thread {
    protected Runnable task;
    protected final Object threadLock;

    protected final AtomicBoolean awake;
    protected final AtomicBoolean running;

    public ServiceThread() {
        this("service-thread");
    }

    public ServiceThread(String name) {
        this.threadLock = new Object();
        this.running = new AtomicBoolean(true);
        this.awake = new AtomicBoolean(false);
        this.setName(name);
    }

    public Object getThreadLock() {
        return this.threadLock;
    }

    /**
     * Whether the thread is running or it is stopped.
     *
     * @return The running state.
     */
    public boolean isRunning() {
        return this.running.get();
    }

    /**
     * Stops the daemon iterations when the current task has finished.
     */
    public void stopDaemon() {
        this.running.set(false);
        synchronized (this.threadLock) {
            this.threadLock.notifyAll();
        }
    }

    /**
     * Queries the thread state.
     *
     * @return True if the thread is currently running stuff, false otherwise.
     */
    public boolean isAwake() {
        return this.awake.get();
    }

    /**
     * This method offers the new task to the service thread. If the thread is sleeping,
     * the new task is set and executed right away. Otherwise, the method blocks
     * and does a busy wait until the current task finishes.
     *
     * @param task The new task to run.
     */
    public void offerTask(Runnable task) {
        waitCurrentTask();
        synchronized (this.threadLock) {
            this.task = task;
            this.threadLock.notify();
            this.awake.set(true);
        }
    }

    /**
     * This method wakes up the thread and runs the current task. If the thread is sleeping,
     * the task is executed right away. Otherwise, the method blocks
     * and does a busy wait until the current task finishes.
     *
     */
    public void wakeUp() {
        waitCurrentTask();
        synchronized (this.threadLock) {
            this.threadLock.notify();
            this.awake.set(true);
        }
    }

    /**
     * This method does an active wait until the current task is finished.
     * If no task is executed, this returns immediately.
     */
    public void waitCurrentTask() {
        // Wait if needed
        while (this.awake.get()) {
            // Busy wait
        }
    }

    @Override
    public void run() {
        while (this.running.get()) {
            synchronized (this.threadLock) {
                if (task != null) {
                    task.run();
                }

                /* ----------- WAIT FOR NOTIFY ----------- */
                try {
                    this.awake.set(false);
                    this.threadLock.wait(Long.MAX_VALUE - 8);
                } catch (InterruptedException e) {
                    // Keep on!
                    this.awake.set(true);
                }
            }
        }

    }
}
