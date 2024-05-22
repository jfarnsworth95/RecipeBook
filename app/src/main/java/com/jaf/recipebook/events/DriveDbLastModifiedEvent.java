package com.jaf.recipebook.events;

public class DriveDbLastModifiedEvent {
    public String status;
    public boolean hasBackups;

    public DriveDbLastModifiedEvent(String status, boolean hasBackups){
        this.status = status;
        this.hasBackups = hasBackups;
    }
}
