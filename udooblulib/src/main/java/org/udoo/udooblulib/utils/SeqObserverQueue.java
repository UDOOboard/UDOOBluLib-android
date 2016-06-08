package org.udoo.udooblulib.utils;

import android.util.Log;

import org.udoo.udooblulib.BuildConfig;
import org.udoo.udooblulib.exceptions.UdooBluException;

import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

/**
 * Created by harlem88 on 23/02/16.
 */
public class SeqObserverQueue<T> extends Observable implements Runnable {
    private BlockingQueue<Callable> tBlockingDeque;
    private final static String TAG = "SeqObserverQueue";

    public SeqObserverQueue(BlockingQueue<Callable> tBlockingQeque) {
        tBlockingDeque = tBlockingQeque;
    }

    Queue<Observer> observers = new ConcurrentLinkedQueue<>();

    boolean changed = false;

    /**
     * Adds the specified observer to the list of observers. If it is already
     * registered, it is not added a second time.
     *
     * @param observer the Observer to add.
     */
    public void addObserver(Observer observer) {
        if (observer == null) {
            throw new NullPointerException("observer == null");
        }
        observers.add(observer);
    }

    /**
     * Clears the changed flag for this {@code Observable}. After calling
     * {@code clearChanged()}, {@code hasChanged()} will return {@code false}.
     */
    protected void clearChanged() {
        changed = false;
    }

    /**
     * Returns the number of observers registered to this {@code Observable}.
     *
     * @return the number of observers.
     */
    public int countObservers() {
        return observers.size();
    }

    /**
     * Removes the specified observer from the list of observers. Passing null
     * won't do anything.
     *
     * @param observer the observer to remove.
     */
    public synchronized void deleteObserver(Observer observer) {
        observers.remove(observer);
    }

    /**
     * Removes all observers from the list of observers.
     */
    public synchronized void deleteObservers() {
        observers.clear();
    }

    /**
     * Returns the changed flag for this {@code Observable}.
     *
     * @return {@code true} when the changed flag for this {@code Observable} is
     * set, {@code false} otherwise.
     */
    public boolean hasChanged() {
        return changed;
    }

    /**
     * If {@code hasChanged()} returns {@code true}, calls the {@code update()}
     * method for every observer in the list of observers using null as the
     * argument. Afterwards, calls {@code clearChanged()}.
     * <p>
     * Equivalent to calling {@code notifyObservers(null)}.
     */
    public void notifyObservers() {
        notifyObservers(null);
    }

    /**
     * If {@code hasChanged()} returns {@code true}, calls the {@code update()}
     * method for every Observer in the list of observers using the specified
     * argument. Afterwards calls {@code clearChanged()}.
     *
     * @param data the argument passed to {@code update()}.
     */
    @SuppressWarnings("unchecked")
    public void notifyObservers(Object data) {
        int size = 0;
        synchronized (this) {
            if (hasChanged()) {
                clearChanged();
                size = observers.size();
            }
        }
        if (size > 0) {
            for (int i = 0; i< size; i++) {
                observers.poll().update(this, data);
            }
        }
    }

    public void notifyObserver(Object data) {
        observers.poll().update(this, data);
    }

    /**
     * Sets the changed flag for this {@code Observable}. After calling
     * {@code setChanged()}, {@code hasChanged()} will return {@code true}.
     */
    protected void setChanged() {
        changed = true;
    }

    public void run() {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        tBlockingDeque.take().call();
                    } catch (Exception e) {
                        setChanged();
                        notifyObserver(new UdooBluException(UdooBluException.BLU_SEQ_OBSERVER_ERROR));
                        if (BuildConfig.DEBUG)
                            Log.e(TAG, "run: "+ e.getMessage());
                    }

                }
            }
        });
    }
}
