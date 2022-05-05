package com.game_proj;

import java.util.ArrayList;
import java.util.List;

public class SimpleSubject implements Subject {

    private List<Observer> observers;
    private String message;

    public SimpleSubject()
    {
        observers = new ArrayList<>();
    }

    @Override
    public void registerObserver(Observer obs)
    {
        observers.add(obs);
    }

    @Override
    public void removeObserver(Observer obs)
    {
        int i = observers.indexOf(obs);
        if (i >= 0)
        {
            observers.remove(i);
        }
    }

    @Override
    public void notifyObservers()
    {
        for (Observer obs: observers)
        {
            obs.update(this.message);
        }
    }

    public void setVal(String message)
    {
        this.message = message;
        notifyObservers();
    }
    
}
