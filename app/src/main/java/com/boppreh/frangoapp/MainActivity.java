package com.boppreh.frangoapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.Context;
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
import android.widget.ExpandableListView;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_RETAINED_FRAGMENT = "RetainedFragment";

    public static final int SESSION_HASH_SIZE = 32;
    public static final int SCAN_ACCOUNT_LOGIN = 0x0000fe39;
    public static final int SCAN_PROFILE_IMPORT = 0x0000fe38;
    public static final String PROFILES_IDS_FILENAME = "profiles_ids";

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
            try {
                ArrayList<String> profileIds = (ArrayList<String>) load(PROFILES_IDS_FILENAME);
                for (String profileId : profileIds) {
                    Profile profile = (Profile) load("profile_" + profileId);
                    fragment.profiles.add(profile);
                }
            } catch (IOException | ClassNotFoundException e) {
                //error("Failed to load profiles", e.getMessage());
                e.printStackTrace();
            }
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
                                    login(profile, getAccount(profile, domain), sessionHash);
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

        } else if (requestCode == SCAN_PROFILE_IMPORT) {

            final byte[] data = intent.getByteArrayExtra("SCAN_RESULT_BYTE_SEGMENTS_0");
            if (data == null || data.length == 0) {
                return;
            }

            final byte[] keyEncoded = Crypto.fromBase64(data);
            final Profile profile = new Profile(Crypto.toBase64(Crypto.random(8)), "Imported profile", new ArrayList<IAccount>());
            (new AsyncTask<Void, Integer, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    String keyId = "master " + profile.id;
                    KeyPair keyPair;
                    try {
                        keyPair = (KeyPair) load(keyEncoded);
                    } catch (ClassNotFoundException | IOException e) {
                        error("Failed to import profile", e.getMessage());
                        e.printStackTrace();
                        return null;
                    }
                    profile.onlineMasterKey = keyPair.getPublic();
                    profile.offlineMasterKey = keyPair.getPrivate();

                    fragment.profiles.add(profile);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pager.getAdapter().notifyDataSetChanged();
                        }
                    });

                    try {
                        serialize("profile_" + profile.id, profile);
                        ArrayList<String> profileIds = new ArrayList<>();
                        for (Profile profile : fragment.profiles) {
                            profileIds.add(profile.id);
                        }
                        serialize(PROFILES_IDS_FILENAME, profileIds);
                    } catch (IOException e) {
                        error("Failed to save profiles", e.getMessage());
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

    private void registerAndLogin(final Profile profile, String domain, final byte[] sessionHash) throws Crypto.Exception, JSONException {
        Log.d("STEP", "registering");

        byte[] revocationCode = Crypto.random(32);
        byte[] revocationCodeHash = Crypto.hash(revocationCode);
        KeyPair keyPair = Crypto.createSigningKey(domain);
        byte[] recoveryCode = Crypto.encrypt(profile.onlineMasterKey, Crypto.cat(revocationCode, keyPair.getPrivate().getEncoded()));
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
                    profile.notifier.notifyUpdate();
                    login(profile, account, sessionHash);
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

    private void login(final Profile profile, final IAccount account, final byte[] sessionHash) throws JSONException, Crypto.Exception {
        account.login(this, sessionHash, new IAccount.Notify() {
            @Override
            public void notifyUpdate(IAccount account) {
                profile.notifier.notifyUpdate();
            }
        });
    }

    private IAccount getAccount(Profile profile, String domain) throws NoSuchAccountException {
        for (IAccount account : profile.accounts) {
            if (account.getName().equals(domain)) {
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
            final Profile profile = new Profile(Crypto.toBase64(Crypto.random(8)), "", new ArrayList<IAccount>());

            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Profile creation")
                    .setPositiveButton("Finish", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText nameField = (EditText) ((Dialog) dialog).findViewById(R.id.name);

                            if (nameField.getText().length() == 0) {
                                profile.name = nameField.getHint().toString();
                            } else {
                                profile.name = nameField.getText().toString();
                            }
                            fragment.profiles.add(profile);
                            pager.getAdapter().notifyDataSetChanged();
                            try {
                                serialize("profile_" + profile.id, profile);
                                ArrayList<String> profileIds = new ArrayList<>();
                                for (Profile profile : fragment.profiles) {
                                    profileIds.add(profile.id);
                                }
                                serialize(PROFILES_IDS_FILENAME, profileIds);
                            } catch (IOException e) {
                                error("Failed to save profiles", e.getMessage());
                                e.printStackTrace();
                            }
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
                    KeyPair keyPair;
                    try {
                        String keyId = "master " + profile.id;
                        keyPair = Crypto.createSigningKey(keyId);
                        profile.onlineMasterKey = keyPair.getPublic();
                        profile.offlineMasterKey = keyPair.getPrivate();
                    } catch (Crypto.Exception e) {
                        dialog.cancel();
                        error("Failed to create new master key pair", e.getMessage());
                        return null;
                    }

                    BitMatrix result;
                    try {
                        result = new MultiFormatWriter().encode(Crypto.toBase64(serialize(keyPair)),
                                BarcodeFormat.QR_CODE, side, side, null);
                    } catch (WriterException | IOException e) {
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

        } else if (id == R.id.action_delete_all) {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm action")
                    .setMessage("Do you really want to remove all profiles, accounts and sessions from this device?")
                    .setPositiveButton("Yes, wipe everything", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String dir = getFilesDir().getAbsolutePath();
                            File f0 = new File(dir, PROFILES_IDS_FILENAME);
                            boolean d0 = f0.delete();
                            Log.w("Delete Check", "File deleted: " + dir + "/myFile " + d0);
                            fragment.profiles.clear();
                            pager.getAdapter().notifyDataSetChanged();
                            finishAffinity();
                            System.exit(0);
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

        return super.onOptionsItemSelected(item);
    }

    public byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(obj);
        byte[] result = bos.toByteArray();
        bos.close();
        os.close();
        return result;
    }

    public Object load(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(bis);
        Object obj = is.readObject();
        is.close();
        bis.close();
        return obj;
    }

    public void serialize(String filename, Serializable obj) throws IOException {
        FileOutputStream fos  = this.openFileOutput(filename, Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(obj);
        os.close();
        fos.close();
    }

    public Object load(String filename) throws IOException, ClassNotFoundException {
        FileInputStream fis = this.openFileInput(filename);
        ObjectInputStream is = new ObjectInputStream(fis);
        Object obj = is.readObject();
        is.close();
        fis.close();
        return obj;
    }
}

class NoSuchAccountException extends Exception {
    NoSuchAccountException(String domain) {
        super("No account for domain " + domain);
    }
}
