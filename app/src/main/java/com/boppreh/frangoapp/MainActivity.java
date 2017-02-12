package com.boppreh.frangoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.app.ListActivity;
import android.content.Context;
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
import android.widget.ExpandableListView;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ExpandableListActivity {

    public static final int SESSION_HASH_SIZE = 32;

    PublicKey onlineMasterKey;
    Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profile = new Profile(this);

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

        (new Runnable() {
            @Override
            public void run() {
                String onlineMasterKeyFilename = "online_master_key.der";
                try {
                    onlineMasterKey = Crypto.loadPublicKey(openFileInput(onlineMasterKeyFilename));
                    Log.d("FILE", "loaded master key from file");
                } catch (FileNotFoundException e) {
                    createMasterKey();
                    Log.d("FILE", "created new master key");
                    try {
                        FileOutputStream fos = openFileOutput(onlineMasterKeyFilename, Context.MODE_PRIVATE);
                        try {
                            fos.write(onlineMasterKey.getEncoded());
                            Log.d("FILE", "saved master key to file");
                        } finally {
                            fos.close();
                        }
                    } catch (IOException e1) {
                        error("Failed to save online master key", e1.getMessage());
                        e1.printStackTrace();
                    }
                } catch (Crypto.Exception e) {
                    error("Failed to load online master key from storage.", e.getMessage());
                }
            }
        }).run();
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

    public void error(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });
    }

    private void post(String url, final JSONObject body) {
        post(url, body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // Ok.
            }
        });
    }

    public void post(String url, final JSONObject body, Response.Listener<String> responseListener) {
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
        byte[] userId = Crypto.hash(onlineMasterKey.getEncoded(), domain.getBytes());
        final Account account = new Account(userId, domain, null, true);
        profile.accounts.add(0, account);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                profile.notifyDataSetChanged();
            }
        });

        byte[] seed = Crypto.random(32);
        account.keyPair = Crypto.createKey(Crypto.hash(seed));
        byte[] recoveryCode = Crypto.encrypt(onlineMasterKey, seed);

        String url = "https://" + account.domain + "/frango/register";

        final JSONObject body = new JSONObject();
        body.put("user_id", Crypto.toBase64(account.userId));
        body.put("public_key", Crypto.toBase64(account.keyPair.getPublic().getEncoded()));
        body.put("recovery_code", Crypto.toBase64(recoveryCode));

        post(url, body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    account.isLoading = false;
                    profile.notifyDataSetChanged();
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

    private void login(final Account account, final byte[] sessionHash) throws JSONException, Crypto.Exception {
        String url = "https://" + account.domain + "/frango/login";

        final JSONObject body = new JSONObject();
        body.put("user_id", Crypto.toBase64(account.userId));
        body.put("session_hash", Crypto.toBase64(sessionHash));
        body.put("signature", Crypto.toBase64(Crypto.sign(account.keyPair.getPrivate(), sessionHash)));

        final Session session = new Session(new Date(), sessionHash, Session.State.CREATING);
        account.sessions.add(0, session);

        post(url, body, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                session.state = Session.State.CREATED;
                profile.notifyDataSetChanged();
            }
        });
    }

    private Account getAccount(String domain) throws NoSuchAccountException {
        for (Account account : profile.accounts) {
            if (account.domain.equals(domain)) {
                return account;
            }
        }
        throw new NoSuchAccountException(domain);
    }

    private void initiateLogin(byte[] qrCodeData) throws UnsupportedEncodingException, Crypto.Exception, JSONException {
        List<byte[]> parts = Crypto.splitAt(qrCodeData, SESSION_HASH_SIZE);
        byte[] sessionHash = parts.get(0);
        byte[] domainBytes = parts.get(1);

        final String domain = new String(domainBytes, "UTF-8");

        try {
            Account account = getAccount(domain);
            login(account, sessionHash);
        } catch (NoSuchAccountException e) {
            registerAndLogin(domain, sessionHash);
        }
    }
}

class NoSuchAccountException extends Exception {
    public NoSuchAccountException(String domain) {
        super("No account for domain " + domain);
    }
}