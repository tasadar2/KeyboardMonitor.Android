package com.transpiria.keyboardmonitor;

import java.util.ArrayList;
import java.util.List;

public class Event<T> {
    public List<T> Observers = new ArrayList<>();

    public void AddObserver(T observer) {
        Observers.add(observer);
    }

    public void RemoveObserver(T observer) {
        Observers.remove(observer);
    }
}

