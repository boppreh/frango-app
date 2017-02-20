package com.boppreh.frangoapp;

import com.boppreh.Crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

class Profile {
    public CharSequence name;
    public List<Account> accounts;
    public PrivateKey offlineMasterKey;
    public PublicKey onlineMasterKey;

    Profile(CharSequence name, List<Account> accounts) {
        this.name = name;
        this.accounts = accounts;
    }

    public byte[] userIdFor(String domain) {
        return Crypto.hash(onlineMasterKey.getEncoded(), domain.getBytes());
    }
}
