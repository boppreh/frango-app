package com.boppreh.frangoapp;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.boppreh.Crypto;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_RETAINED_FRAGMENT = "RetainedFragment";

    public static final int SESSION_HASH_SIZE = 32;
    public static final int SCAN_ACCOUNT_LOGIN = 0x0000fe39;
    public static final int SCAN_PROFILE_IMPORT = 0x0000fe38;

    MainFragment fragment;

    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setTitle("Frango");
        }

        FragmentManager fm = getFragmentManager();
        fragment = (MainFragment) fm.findFragmentByTag(TAG_RETAINED_FRAGMENT);
        if (fragment == null) {
            fragment = new MainFragment();
            fm.beginTransaction().add(fragment, TAG_RETAINED_FRAGMENT).commit();
        }

        ProfilesAdapter adapter = new ProfilesAdapter(this, getSupportFragmentManager());
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(pager, true);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == SCAN_ACCOUNT_LOGIN) {
            final byte[] data = intent.getByteArrayExtra("SCAN_RESULT_BYTE_SEGMENTS_0");
            if (data == null || data.length == 0) {
                return;
            }

            // Hackish, but I found no way of passing data between a scan intent and the scan results.
            final Profile profile = fragment.profiles.get(pager.getCurrentItem());

            final String domain;
            final byte[] sessionHash;
            try {
                List<byte[]> parts = Crypto.splitAt(data, SESSION_HASH_SIZE);
                byte[] domainBytes = parts.get(1);
                domain = new String(domainBytes, "UTF-8");
                sessionHash = parts.get(0);
            } catch (UnsupportedEncodingException | Crypto.AlgorithmException e) {
                error("Failed to parse data from QR code", e.getMessage());
                e.printStackTrace();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Confirm action")
                    .setMessage("Do you really want to authenticate this session at " + domain + "?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                try {
                                    Account account = getAccount(profile, domain);
                                    login(account, sessionHash);
                                } catch (NoSuchAccountException e) {
                                    registerAndLogin(profile, domain, sessionHash);
                                }
                            } catch (Crypto.Exception | JSONException e) {
                                error("Fail", e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }

    public void error(final String title, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });
    }

    public void post(String url, final JSONObject body, Response.Listener<String> responseListener) {
        post(url, body, responseListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    error("Error", (String) new JSONObject(new String(error.networkResponse.data)).get("error"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    error("Error", "Server returned status code " + error.networkResponse.statusCode + " but an invalid body.");
                }
                error.printStackTrace();
            }
        });
    }

    public void post(String url, final JSONObject body, Response.Listener<String> responseListener, Response.ErrorListener errorListener) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                responseListener, errorListener) {
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

    private void registerAndLogin(Profile profile, String domain, final byte[] sessionHash) throws Crypto.Exception, JSONException {
        Log.d("STEP", "registering");

        byte[] seed = Crypto.random(32);
        byte[] revocationCode = Crypto.random(32);
        byte[] revocationCodeHash = Crypto.hash(revocationCode);
        KeyPair keyPair = Crypto.createSigningKey(domain, seed);
        byte[] recoveryCode = Crypto.encrypt(profile.onlineMasterKey, Crypto.cat(revocationCode, seed));
        final Account account = new Account(profile.userIdFor(domain), domain, keyPair, recoveryCode, revocationCodeHash);
        profile.accounts.add(0, account);

        String url = "https://" + account.domain + "/frango/register";

        final JSONObject body = new JSONObject();
        body.put("user_id", Crypto.toBase64(account.userId));
        body.put("public_key", Crypto.toBase64(account.keyPair.getPublic().getEncoded()));
        body.put("recovery_code", Crypto.toBase64(account.recoveryCode));
        body.put("revocation_code_hash", Crypto.toBase64(account.revocationCodeHash));

        post(url, body, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    pager.getAdapter().notifyDataSetChanged();
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
                pager.getAdapter().notifyDataSetChanged();
                try {
                    account.save(MainActivity.this);
                } catch (IOException | JSONException e) {
                    error("Failed to persist changes", e.getMessage());
                }
            }
        });
    }

    private Account getAccount(Profile profile, String domain) throws NoSuchAccountException {
        for (Account account : profile.accounts) {
            if (account.domain.equals(domain)) {
                return account;
            }
        }
        throw new NoSuchAccountException(domain);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_create_profile) {
            final Profile profile = new Profile("", new ArrayList<Account>());

            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Profile creation")
                    .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText nameField = (EditText) ((AlertDialog) dialog).findViewById(R.id.name);

                            if (nameField.getText().length() == 0) {
                                profile.name = nameField.getHint().toString();
                            } else {
                                profile.name = nameField.getText().toString();
                            }
                            fragment.profiles.add(profile);
                            pager.getAdapter().notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setView(R.layout.profile_creation)
                    .create();

            dialog.show();

            final ImageView imageView = (ImageView) dialog.findViewById(R.id.offline_code);
            final int side = Math.min(imageView.getDrawable().getIntrinsicWidth(), imageView.getDrawable().getIntrinsicHeight());

            ((TextView) dialog.findViewById(R.id.description)).setText("Generating key...");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            (new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    byte[] seed = Crypto.random(32);
                    KeyPair keyPair = null;
                    try {
                        keyPair = Crypto.createSigningKey("master key", seed);
                    } catch (Crypto.Exception e) {
                        dialog.cancel();
                        error("Failed to create new master key pair", e.getMessage());
                        return null;
                    }
                    profile.onlineMasterKey = keyPair.getPublic();
                    profile.offlineMasterKey = keyPair.getPrivate();

                    BitMatrix result;
                    try {
                        result = new MultiFormatWriter().encode(Crypto.toBase64(seed),
                                BarcodeFormat.QR_CODE, side, side, null);
                    } catch (WriterException e) {
                        dialog.cancel();
                        error("Failed to create QR code with backup codes", e.getMessage());
                        return null;
                    }
                    int w = result.getWidth();
                    int h = result.getHeight();
                    int[] pixels = new int[w * h];
                    for (int y = 0; y < h; y++) {
                        int offset = y * w;
                        for (int x = 0; x < w; x++) {
                            pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                        }
                    }
                    final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bitmap);
                            ((TextView) dialog.findViewById(R.id.description)).setText("The QR code below is your Offline Master Key. If you ever lose your phone, you will need it to recover your accounts. Copy it somewhere safe.");
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                    });
                    return null;
                }
            }).execute();

            return true;
        } else if (id == R.id.action_import) {
            final IntentIntegrator integrator = new IntentIntegrator(this, MainActivity.SCAN_PROFILE_IMPORT);
            integrator.initiateScan();
        }

        return super.onOptionsItemSelected(item);
    }
}

class NoSuchAccountException extends Exception {
    NoSuchAccountException(String domain) {
        super("No account for domain " + domain);
    }
}
