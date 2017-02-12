package com.boppreh.frangoapp;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

/**
 * Created by BoppreH on 2017-02-11.
 */
public class Account {
    public byte[] userId;
    public String domain;
    public KeyPair keyPair;
    public List<String> sessions;

    public Account(byte[] userId, String domain, KeyPair keyPair) {
        this(userId, domain, keyPair, new ArrayList<String>());
    }

    public Account(byte[] userId, String domain, KeyPair keyPair, List<String> sessions) {
        this.userId = userId;
        this.domain = domain;
        this.keyPair = keyPair;
        this.sessions = sessions;
    }
}
