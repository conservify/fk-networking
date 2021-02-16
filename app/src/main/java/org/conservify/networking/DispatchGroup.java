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
            Log.i(TAG, "DispatchGroup.leave - notifyGroup!");
            notifyGroup();
        } else if (value < 0) {
            Log.e(TAG, "DispatchGroup.leave - negative!");
            throw new RuntimeException(String.format("DispatchGroup: excess leave calls: {}", value));
        }
    }

    public void notify(Runnable r) {
        try
        {
            Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ") start");
            if (runnable == null) {
                Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ") start: " + "NULL");
            }
            else {
                Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ") start: " + runnable.get());
            }
            Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ") start: " + r);
            runnable.set(r);
            Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ") notifying");
            notifyGroup();
            Log.i(TAG, "DispatchGroup::notify(" + counter.get() + ") done");
        }
        catch (Exception e) {
            Log.e(TAG, "DispatchGroup.notify failed:", e);
        }
    }

    private synchronized void notifyGroup() {
        try {
            long value = counter.get();
            Log.i(TAG, "DispatchGroup::notifyGroupInside(" + value + ")");
            if (value == 0) {
                Runnable running = runnable.getAndSet(null);
                if (running != null) {
                    Log.i(TAG, "DispatchGroup::invoking");
                    try {
                        running.run();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "DispatchGroup.notifyGroup(run) failed:", e);
                    }
                    finally {
                        Log.i(TAG, "DispatchGroup::invoked");
                    }
                }
                else {
                    Log.i(TAG, "DispatchGroup::late notifyGroup()");
                }
            } else if (value < 0) {
                Log.i(TAG, "DispatchGroup::negative notifyGroup()");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "DispatchGroup.notifyGroup failed:", e);
        }
    }

    public void reset() {
        counter.set(0);
        runnable.set(null);
    }
}
