package com.jaf.recipebook.events;

public class DbRefreshEvent {
    public boolean shouldRefresh;

    public DbRefreshEvent(boolean shouldRefresh){
        this.shouldRefresh = shouldRefresh;
    }
}
