package com.boppreh.frangoapp;

import com.boppreh.Crypto;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

class Profile implements Serializable {

    public interface Notify {
        void notifyUpdate();
    }

    public String id;
    public String name;
    public List<IAccount> accounts;
    public PrivateKey offlineMasterKey;
    public PublicKey onlineMasterKey;
    public transient Notify notifier;

    public Profile() {

    }

    Profile(String id, String name, List<IAccount> accounts) {
        this.id = id;
        this.name = name;
        this.accounts = accounts;
    }

    public byte[] userIdFor(String domain) {
        return Crypto.hash(onlineMasterKey.getEncoded(), domain.getBytes());
    }
}
