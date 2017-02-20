package com.boppreh.frangoapp;

import java.security.PrivateKey;
import java.util.List;

public interface IAccount {

    public interface RemovalCallback {
        void removeAndNotify(IAccount account);
    }
    public interface Notify {
        void notifyUpdate(IAccount account);
    }

    String getName();
    String getSubtitle();
    List<Session> getSessions();

    void remove(PrivateKey offlineMasterKey, final MainActivity activity, final RemovalCallback callback);
    void logout(final Session session, final MainActivity activity, final Notify callback);
    void login(MainActivity mainActivity, byte[] sessionHash, Notify notify);
}
