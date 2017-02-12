package com.boppreh.frangoapp;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.Cipher;

/**
 * Created by BoppreH on 2017-02-11.
 */
public class Account {
    public byte[] userId;
    public String domain;
    public KeyPair keyPair;
    public List<Session> sessions;

    public Account(byte[] userId, String domain, KeyPair keyPair) {
        this(userId, domain, keyPair, new ArrayList<Session>());
    }

    public Account(byte[] userId, String domain, KeyPair keyPair, List<Session> sessions) {
        this.userId = userId;
        this.domain = domain;
        this.keyPair = keyPair;
        this.sessions = sessions;
    }
}
