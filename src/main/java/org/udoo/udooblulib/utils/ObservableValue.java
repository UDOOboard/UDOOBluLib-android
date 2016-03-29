package org.udoo.udooblulib.utils;


import java.util.Observable;

public class ObservableValue<T> extends Observable {
    public ObservableValue() {}

    public void setValue(T n) {
        if (n != null) {
            setChanged();
            notifyObservers(n);
        }
    }
}