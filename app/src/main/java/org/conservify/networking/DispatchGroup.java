package org.conservify.networking;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DispatchGroup {
    private static final String TAG = "JS";
    private AtomicInteger counter = new AtomicInteger();
    private AtomicReference<Runnable> runnable = new AtomicReference<Runnable>(null);

    public synchronized void enter() {
        counter.incrementAndGet();
    }

    public synchronized void leave() {
        long value = counter.decrementAndGet();
        if (value == 0) {
            notifyGroup();
        } else if (value < 0) {
            throw new RuntimeException(String.format("DispatchGroup: excess leave calls: {}", value));
        }
    }

    public void notify(Runnable r) {
        Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ")");
        runnable.set(r);
        notifyGroup();
    }

    private synchronized void notifyGroup() {
        long value = counter.get();
        if (value == 0) {
            Runnable running = runnable.getAndSet(null);
            if (running != null) {
                running.run();
            }
            else {
                Log.i(TAG, "DispatchGroup::late notifyGroup()");
            }
        } else if (value < 0) {
            Log.i(TAG, "DispatchGroup::negative notifyGroup()");
        }
    }

    public void reset() {
        counter.set(0);
        runnable.set(null);
    }
}
