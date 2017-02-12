package com.boppreh.frangoapp;

import java.util.Date;

/**
 * Created by BoppreH on 2017-02-12.
 */
public class Session {
    public Date timestamp;
    public String sessionHash;

    public Session(Date timestamp, String sessionHash) {
        this.timestamp = timestamp;
        this.sessionHash = sessionHash;
    }
}

