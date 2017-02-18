package com.boppreh;

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by BoppreH on 2017-02-11.
 */

public class Crypto {


    public static class Exception extends java.lang.Exception {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static SecureRandom secureRandom = new SecureRandom();

    public static byte[] hash(byte[]... dataParts) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            digest.update(cat(dataParts));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Failed to hash data.", e);
        }
    }

    public static byte[] hash(String... dataParts) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            digest.update(cat(dataParts).getBytes("UTF-8"));
            return digest.digest();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new Exception("Failed to hash data.", e);
        }
    }

    public static byte[] sign(PrivateKey key, byte[] data) throws Exception {
        try {
            // Should be "SHA256WithRSA/PSS", but only Android KeyStore keys support that.
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initSign(key);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new Exception("Failed to sign session hash.", e);
        }
    }

    public static byte[] encrypt(PublicKey key, byte[] data) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new Exception("Failed to sign data.", e);
        }
    }

    public static byte[] decrypt(PrivateKey key, byte[] data) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new Exception("Failed to sign data.", e);
        }
    }

    public static byte[] cat(byte[]... parts) {
        if (parts.length == 0) {
            return new byte[0];
        }
        if (parts.length == 1) {
            return parts[0];
        }

        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }

        byte[] result = new byte[length];
        int i = 0;
        for (byte[] part : parts) {
            for (int j = 0; j < part.length; i++, j++) {
                result[i] = part[j];
            }
        }
        return result;
    }

    public static String cat(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part);
        }
        return builder.toString();
    }

    public static String toBase64(String data) {
        return Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
    }

    public static String toBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public static byte[] fromBase64(String data) {
        return Base64.decode(data, Base64.DEFAULT);
    }

    public static byte[] random(int nBytes) {
        byte[] bytes = new byte[nBytes];
        secureRandom.nextBytes(bytes);
        return bytes;
    }


    public static KeyPair createKey(byte[] seed) throws Exception {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, new SecureRandom(seed));
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Failed to create key pair.", e);
        }
    }

    public static PublicKey loadPublicKey(byte[] encoded) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new Exception("Failed to load public RSA key.", e);
        }
    }

    public static PrivateKey loadPrivateKey(byte[] encoded) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new Exception("Failed to load private RSA key.", e);
        }
    }

    public static byte[] read(InputStream stream) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        try {
            while (true) {
                int bytesRead = stream.read(b);
                if (bytesRead == -1) {
                    break;
                }
                bos.write(b, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new Exception("Failed to load public RSA key.", e);
        }
        return bos.toByteArray();
    }

    public static PublicKey loadPublicKey(InputStream stream) throws Exception {
        return loadPublicKey(read(stream));
    }

    public static PrivateKey loadPrivateKey(InputStream stream) throws Exception {
        return loadPrivateKey(read(stream));
    }

    public static void listAvailableAlgorithms() {
        for (Provider provider : Security.getProviders()) {
            Log.d("PROVIDER", provider.getName());
            for (String key : provider.stringPropertyNames())
                Log.d("ALGOS", "\t" + key + "\t" + provider.getProperty(key));
        }
    }

    public static List<byte[]> splitAt(byte[] buffer, int... indexes) throws Exception {
        List<byte[]> result = new ArrayList<>();
        int lastIndex = 0;
        for (int index : indexes) {
            if (index >= buffer.length) {
                throw new Exception("Buffer is too short to split.", null);
            }
            result.add(Arrays.copyOfRange(buffer, lastIndex, index));
            lastIndex = index;
        }
        result.add(Arrays.copyOfRange(buffer, lastIndex, buffer.length));
        return result;
    }
}
