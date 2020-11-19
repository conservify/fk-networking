package org.conservify.networking;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DispatchGroup {
    private AtomicInteger counter = new AtomicInteger();
    private AtomicReference<Runnable> runnable = new AtomicReference<Runnable>(null);

    public synchronized void enter() {
        counter.incrementAndGet();
    }

    public synchronized void leave() {
        if (counter.decrementAndGet() == 0) {
            notifyGroup();
        }
    }

    public void notify(Runnable r) {
        runnable.set(r);
        notifyGroup();
    }

    private synchronized void notifyGroup() {
        if (counter.get() == 0) {
            Runnable running = runnable.getAndSet(null);
            if (running != null) {
                running.run();
            }
        }
    }

    public void reset() {
        counter.set(0);
        runnable.set(null);
    }
}
