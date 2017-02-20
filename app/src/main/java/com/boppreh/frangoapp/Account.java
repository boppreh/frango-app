package com.boppreh.frangoapp;

import android.app.Activity;
import android.content.Context;

import com.boppreh.Crypto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account {
    public byte[] userId;
    public String domain;
    public KeyPair keyPair;
    public List<Session> sessions;
    public byte[] recoveryCode;
    public byte[] revocationCodeHash;

    public Account(byte[] userId, String domain, KeyPair keyPair, byte[] recoveryCode, byte[] revocationCodeHash) {
        this(userId, domain, keyPair, recoveryCode, revocationCodeHash, new ArrayList<Session>());
    }

    public Account(byte[] userId, String domain, KeyPair keyPair, byte[] recoveryCode, byte[] revocationCodeHash, List<Session> sessions) {
        this.userId = userId;
        this.domain = domain;
        this.keyPair = keyPair;
        this.recoveryCode = recoveryCode;
        this.revocationCodeHash = revocationCodeHash;
        this.sessions = sessions;
    }

    public JSONObject marshall() throws JSONException {
        if (keyPair == null) {
            throw new JSONException("Cannot marshall pending objects.");
        }

        JSONObject obj = new JSONObject();
        obj.put("user_id", Crypto.toBase64(userId));
        obj.put("domain", domain);
        JSONArray sessionsJson = new JSONArray();
        for (Session session : sessions) {
            sessionsJson.put(session.marshall());
        }
        obj.put("sessions", sessionsJson);
        obj.put("recovery_code", Crypto.toBase64(recoveryCode));
        obj.put("revocation_code_hash", Crypto.toBase64(revocationCodeHash));

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = null;
        try {
            objectOutput = new ObjectOutputStream(byteOutput);
            objectOutput.writeObject(keyPair);
        } catch (IOException e) {
            e.printStackTrace();
            throw new JSONException("Failed to serialize key pair.");
        }
        obj.put("key_pair", Crypto.toBase64(byteOutput.toByteArray()));
        return obj;
    }

    public static Account unmarshall(String json) throws JSONException, ParseException, IOException, ClassNotFoundException {
        JSONObject obj = new JSONObject(json);
        byte[] userId = Crypto.fromBase64(obj.getString("user_id"));
        String domain = obj.getString("domain");
        byte[] revocationCodeHash = Crypto.fromBase64(obj.getString("revocation_code_hash"));
        byte[] recoveryCode = Crypto.fromBase64(obj.getString("recovery_code"));

        JSONArray sessionsJson = obj.getJSONArray("sessions");
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < sessionsJson.length(); i++) {
            JSONObject sessionJson = (JSONObject) sessionsJson.get(i);

            Date timestamp = Session.DATE_FORMAT.parse(sessionJson.getString("timestamp"));
            byte[] sessionHash = Crypto.fromBase64(sessionJson.getString("session_hash"));
            sessions.add(new Session(timestamp, sessionHash));
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(Crypto.fromBase64(obj.getString("key_pair")));
        ObjectInputStream objectStream = new ObjectInputStream(inputStream);
        KeyPair keyPair = (KeyPair) objectStream.readObject();

        return new Account(userId, domain, keyPair, recoveryCode, revocationCodeHash, sessions);
    }

    public String getFilename() {
        return "account_" + Crypto.toBase64(domain) + ".json";
    }

    public void save(Activity activity) throws JSONException, IOException {
        FileOutputStream outputStream = activity.openFileOutput(getFilename(), Context.MODE_PRIVATE);
        outputStream.write(marshall().toString().getBytes());
    }

    public static Account load(Activity activity, String domain) throws IOException, Crypto.Exception, ClassNotFoundException, ParseException, JSONException {
        FileInputStream stream = activity.openFileInput("account_" + Crypto.toBase64(domain) + ".json");
        return unmarshall(new String(Crypto.read(stream)));
    }
}
