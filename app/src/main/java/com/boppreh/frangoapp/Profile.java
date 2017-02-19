package com.boppreh.frangoapp;

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
}
