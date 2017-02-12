package com.boppreh.frangoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final int SESSION_HASH_SIZE = 32;
    private PublicKey onlineMasterKey;
    private Map<String, Account> accounts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KeyPair masterKey = null;
        try {
            masterKey = Crypto.createKey(Crypto.random(32));
        } catch (Crypto.Exception e) {
            error("Failed to create master key", e.getMessage());
            e.printStackTrace();
        }
        onlineMasterKey = masterKey.getPublic();

        final IntentIntegrator integrator = new IntentIntegrator(this);
        Button clickButton = (Button) findViewById(R.id.scan);
        clickButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                integrator.initiateScan();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            try {
                initiateLogin(scanResult.getDataBytes());
            } catch (UnsupportedEncodingException e) {
                error("Invalid QR code", "The scanned QR code contained an invalid domain (non-UTF-8).");
                e.printStackTrace();
            } catch (Exception e) {
                error("Failed to process request", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void error(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void post(String url, final JSONObject body) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Ok.
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error("Failed to register account", error.getMessage());
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

    private void registerAccount(String domain) throws Crypto.Exception, JSONException {
        byte[] seed = Crypto.random(32);
        byte[] userId = Crypto.hash(onlineMasterKey.getEncoded(), domain.getBytes());
        Account account = new Account(userId, domain, Crypto.createKey(Crypto.hash(seed)));
        byte[] recoveryCode = Crypto.encrypt(onlineMasterKey, seed);
        accounts.put(domain, account);

        String url = "https://" + account.domain + "/frango/register";

        final JSONObject body = new JSONObject();
        body.put("user_id", Crypto.toBase64(account.userId));
        body.put("public_key", Crypto.toBase64(account.keyPair.getPublic().getEncoded()));
        body.put("recovery_code", Crypto.toBase64(recoveryCode));

        post(url, body);
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

        String domain = new String(domainBytes, "UTF-8");

        if (!accounts.containsKey(domain)) {
            registerAccount(domain);
        }

        login(accounts.get(domain), sessionHash);
    }
}
