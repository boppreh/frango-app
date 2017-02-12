package com.boppreh.frangoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.boppreh.Crypto;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ListActivity {

    public static final int SESSION_HASH_SIZE = 32;

    PublicKey onlineMasterKey;
    Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profile = new Profile(this);

        Account account1 = new Account(this, "user_id".getBytes(), "4mm.org", null);
        account1.sessions.add(new Session(new Date(100000), "hash"));
        account1.sessions.add(new Session(new Date(200000), "hash"));
        account1.sessions.add(new Session(new Date(300000), "hash"));
        profile.accounts.put("4mm.org", account1);

        Account account2 = new Account(this, "user_id".getBytes(), "boppreh.com", null);
        account2.sessions.add(new Session(new Date(400000), "hash"));
        profile.accounts.put("boppreh.com", account2);

        Account account3 = new Account(this, "user_id".getBytes(), "example.com", null);
        profile.accounts.put("example.com", account3);

        setListAdapter(profile);
        setContentView(R.layout.activity_main);

        final IntentIntegrator integrator = new IntentIntegrator(this);
        Button clickButton = (Button) findViewById(R.id.scan);
        clickButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                integrator.initiateScan();
            }
        });
    }

    private void createMasterKey() {
        try {
            KeyPair masterKey = Crypto.createKey(Crypto.random(32));
            onlineMasterKey = masterKey.getPublic();
        } catch (Crypto.Exception e) {
            error("Failed to create master key", e.getMessage());
            e.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            final byte[] data = scanResult.getDataBytes();
            (new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        initiateLogin(data);
                    } catch (UnsupportedEncodingException e) {
                        error("Invalid QR code", "The scanned QR code contained an invalid domain (non-UTF-8).");
                        e.printStackTrace();
                    } catch (Exception e) {
                        error("Failed to process request", e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                }
            }).execute();
        }
    }

    private void error(final String title, final String message) {
        final Activity activity = this;
        (new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                new AlertDialog.Builder(activity)
                        .setTitle(title)
                        .setMessage(message)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                return null;
            }
        }).execute();
    }

    private void post(String url, final JSONObject body) {
        post(url, body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Ok.
            }
        });
    }

    private void post(String url, final JSONObject body, Response.Listener<String> responseListener) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error("Error", error.getMessage());
                Log.d("POST ERROR", new String(error.networkResponse.data));
                error.printStackTrace();
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return body.toString().getBytes();
            }
        };

        queue.add(stringRequest);
    }

    private void registerAndLogin(String domain, final byte[] sessionHash) throws Crypto.Exception, JSONException {
        byte[] seed = Crypto.random(32);
        byte[] userId = Crypto.hash(onlineMasterKey.getEncoded(), domain.getBytes());
        final Account account = new Account(this, userId, domain, Crypto.createKey(Crypto.hash(seed)));
        byte[] recoveryCode = Crypto.encrypt(onlineMasterKey, seed);
        //accounts.put(domain, account);

        String url = "https://" + account.domain + "/frango/register";

        final JSONObject body = new JSONObject();
        body.put("user_id", Crypto.toBase64(account.userId));
        body.put("public_key", Crypto.toBase64(account.keyPair.getPublic().getEncoded()));
        body.put("recovery_code", Crypto.toBase64(recoveryCode));

        post(url, body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    login(account, sessionHash);
                } catch (JSONException e) {
                    error("Server replied with invalid data", e.getMessage());
                    e.printStackTrace();
                } catch (Crypto.Exception e) {
                    error("Error registering new account", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void login(Account account, byte[] sessionHash) throws JSONException, Crypto.Exception {
        String url = "https://" + account.domain + "/frango/login";

        final JSONObject body = new JSONObject();
        body.put("user_id", Crypto.toBase64(account.userId));
        body.put("session_hash", Crypto.toBase64(sessionHash));
        body.put("signature", Crypto.toBase64(Crypto.sign(account.keyPair.getPrivate(), sessionHash)));

        post(url, body);
    }

    private void initiateLogin(byte[] qrCodeData) throws UnsupportedEncodingException, Crypto.Exception, JSONException {
        List<byte[]> parts = Crypto.splitAt(qrCodeData, SESSION_HASH_SIZE);
        byte[] sessionHash = parts.get(0);
        byte[] domainBytes = parts.get(1);

        final String domain = new String(domainBytes, "UTF-8");
        Log.d("QRCODE", domain);

        if (profile.accounts.containsKey(domain)) {
            login(profile.accounts.get(domain), sessionHash);
        } else {
            registerAndLogin(domain, sessionHash);

            final MainActivity activity = this;
            this.runOnUiThread(new Runnable() {
                public void run() {
                    //ListView accounts = (ListView) findViewById(R.id.accounts);
                    //TextView session = new TextView(activity);
                    //session.setText("Creating account at " + domain);
                    //accounts.addView(session);
                }
            });
        }

        Log.d("QRCODE", "Finished login.");
    }
}
