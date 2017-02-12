package com.boppreh.frangoapp;

import java.util.Date;

/**
 * Created by BoppreH on 2017-02-12.
 */
public class Session {
    public Date timestamp;
    public byte[] sessionHash;
    public State state;

    public enum State {
        CREATING, CREATED, REMOVING
    }

    public Session(Date timestamp, byte[] sessionHash, State state) {
        this.timestamp = timestamp;
        this.sessionHash = sessionHash;
        this.state = state;
    }
}

