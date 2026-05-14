package com.example.util;

import java.util.ArrayList;
import java.util.List;

public class CenterEventManager {
    private static final CenterEventManager instance = new CenterEventManager();
    private final List<CenterEventObserver> observers = new ArrayList<>();

    private CenterEventManager() {}

    public static CenterEventManager getInstance() {
        return instance;
    }

    public void addObserver(CenterEventObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    public void removeObserver(CenterEventObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(CenterEvent event) {
        for (CenterEventObserver obs : observers) {
            obs.onCenterUpdated(event);
        }
    }
}