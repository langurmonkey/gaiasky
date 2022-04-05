package gaiasky.util.concurrent;

/**
 * A thread that waits for a task to be executed. Tasks can
 * be aborted.
 */
public class ServiceThread extends Thread {
    protected Runnable task;
    protected final Object threadLock;

    protected boolean awake;
    protected boolean running;

    public ServiceThread() {
        this("service-thread");
    }

    public ServiceThread(String name) {
        this.threadLock = new Object();
        this.running = true;
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
        return this.running;
    }

    /**
     * Stops the daemon iterations when the current task has finished.
     */
    public void stopDaemon() {
        this.running = false;
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
        return this.awake;
    }

    /**
     * This methods offers the new task to the service thread. If the thread is sleeping,
     * the new task is set and executed right away. Otherwise, the method blocks
     * and does a busy wait until the current task finishes.
     *
     * @param task The new task to run.
     */
    public void offerTask(Runnable task) {
        // Wait if needed
        while (this.awake) {
            // Busy wait
        }
        synchronized (this.threadLock) {
            this.task = task;
            this.threadLock.notify();
        }
    }

    @Override
    public void run() {

        while (this.running) {
            synchronized (this.threadLock) {
                if (task != null) {
                    task.run();
                }

                /* ----------- WAIT FOR NOTIFY ----------- */
                try {
                    this.awake = false;
                    this.threadLock.wait(Long.MAX_VALUE - 8);
                } catch (InterruptedException e) {
                    // Keep on!
                    this.awake = true;
                }
            }
        }

    }
}
