package com.boppreh.frangoapp;

import com.boppreh.Crypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by BoppreH on 2017-02-12.
 */
public class Session {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ssZ");

    public Date timestamp;
    public byte[] sessionHash;
    public State state;

    public enum State {
        CREATING, CREATED, REMOVING
    }

    public Session(Date timestamp, byte[] sessionHash) {
        this(timestamp, sessionHash, State.CREATED);
    }

    public Session(Date timestamp, byte[] sessionHash, State state) {
        this.timestamp = timestamp;
        this.sessionHash = sessionHash;
        this.state = state;
    }

    public String getIso8601() {
        return DATE_FORMAT.format(timestamp);
    }

    public JSONObject marshall() throws JSONException {
        if (state != State.CREATED) {
            throw new JSONException("Cannot marshall pending objects.");
        }

        JSONObject obj = new JSONObject();
        obj.put("timestamp", getIso8601());
        obj.put("session_hash", Crypto.toBase64(sessionHash));
        return obj;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Session)) {
            return false;
        }
        return Arrays.equals(((Session) obj).sessionHash, this.sessionHash);
    }
}

