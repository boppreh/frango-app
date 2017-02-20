package com.boppreh.frangoapp;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

public class PendingAccount implements IAccount {

    String domain;

    public PendingAccount(String domain) {
        this.domain = domain;
    }

    @Override
    public String getName() {
        return domain;
    }

    @Override
    public String getSubtitle() {
        return "registering...";
    }

    @Override
    public List<Session> getSessions() {
        return new ArrayList<Session>();
    }

    @Override
    public void remove(PrivateKey offlineMasterKey, final MainActivity activity, final RemovalCallback callback) {
        callback.removeAndNotify(this);
    }

    @Override
    public void logout(Session session, MainActivity activity, Notify callback) {
        // Should never have any sessions.
    }

    @Override
    public void login(MainActivity mainActivity, byte[] sessionHash, Notify notify) {
        // Should never have any sessions.c
    }
}
