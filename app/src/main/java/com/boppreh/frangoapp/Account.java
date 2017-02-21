package com.boppreh.frangoapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
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
import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Account implements IAccount, Serializable {
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

    public static Account load(Activity activity, String domain) throws IOException, Crypto.Exception, ClassNotFoundException, ParseException, JSONException {
        FileInputStream stream = activity.openFileInput("account_" + Crypto.toBase64(domain) + ".json");
        return unmarshall(new String(Crypto.read(stream)));
    }

    @Override
    public String getName() {
        return domain;
    }

    @Override
    public String getSubtitle() {
        return sessions.size() + " sessions";
    }

    @Override
    public List<Session> getSessions() {
        return sessions;
    }

    public void remove(PrivateKey offlineMasterKey, final MainActivity activity, final RemovalCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("user_id", Crypto.toBase64(userId));
            byte[] recovery = Crypto.decrypt(offlineMasterKey, recoveryCode);
            List<byte[]> recoveryParts = Crypto.splitAt(recovery, 32);
            byte[] seed = recoveryParts.get(0);
            byte[] revocationCode = recoveryParts.get(1);
            body.put("revocation_code", Crypto.toBase64(revocationCode));
        } catch (JSONException e) {
            activity.error("Failed encode account removal", e.getMessage());
            e.printStackTrace();
            return;
        } catch (Crypto.Exception e) {
            activity.error("Failed to decrypt recovery code", e.getMessage());
            e.printStackTrace();
            return;
        }

        activity.post("https://" + domain + "/frango/revoke", body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                callback.removeAndNotify(Account.this);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse.statusCode == 404) {
                    activity.error("Invalid account", "The account doesn't exist anymore. It'll be removed from the list.");
                    callback.removeAndNotify(Account.this);
                } else {
                    activity.error("Error on account removal", error.getMessage());
                }
            }
        });
    }

    @Override
    public void logout(final Session session, final MainActivity activity, final Notify callback) {
        JSONObject body = new JSONObject();
        String url = "https://" + domain + "/frango/logout";
        try {
            body.put("user_id", Crypto.toBase64(userId));
            body.put("session_hash", Crypto.toBase64(session.sessionHash));
            body.put("signature", Crypto.toBase64(Crypto.sign(keyPair.getPrivate(), session.sessionHash)));
        } catch (Crypto.Exception | JSONException e) {
            activity.error("Error on logout", e.getMessage());
            e.printStackTrace();
            return;
        }

        activity.post(url, body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                sessions.remove(session);
                callback.notifyUpdate(Account.this);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse.statusCode == 404) {
                    activity.error("Invalid session", "The session doesn't exist anymore. It'll be removed from the list.");
                    sessions.remove(session);
                    callback.notifyUpdate(Account.this);
                } else {
                    activity.error("Error on log out", error.getMessage());
                }
            }
        });
    }

    public void login(final MainActivity activity, byte[] sessionHash, final Notify callback) {
        String url = "https://" + domain + "/frango/login";

        final JSONObject body;
        try {
            body = new JSONObject();
            body.put("user_id", Crypto.toBase64(userId));
            body.put("session_hash", Crypto.toBase64(sessionHash));
            body.put("signature", Crypto.toBase64(Crypto.sign(keyPair.getPrivate(), sessionHash)));
        } catch (Crypto.AlgorithmException | JSONException |Crypto.KeyStoreUnavailableException e) {
            activity.error("Failed to login", e.getMessage());
            return;
        }

        final Session session = new Session(new Date(), sessionHash);
        sessions.add(0, session);

        activity.post(url, body, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                callback.notifyUpdate(Account.this);
            }
        });
    }
}
