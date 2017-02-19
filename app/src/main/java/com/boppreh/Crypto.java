package com.boppreh;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Crypto {


    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final long VALIDITY_IN_MILLISECONDS = 100L * 365 * 24 * 80 * 80 * 1000;
    private static final String SELF_SIGNED_COMMON_NAME = "Master Frango";
    private static final String ENCRYPTION_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String SIGNATURE_ALGORITHM = "SHA256WithRSA/PSS";
    private static final int RSA_KEY_SIZE = 2048;
    private static KeyStore androidKeyStore;

    public static class Exception extends java.lang.Exception {
        Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class AlgorithmException extends Exception {
        AlgorithmException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MissingKeyException extends Exception {
        MissingKeyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class KeyStoreUnavailableException extends Exception {
        KeyStoreUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static SecureRandom secureRandom = new SecureRandom();

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] hash(byte[]... dataParts) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            digest.update(cat(dataParts));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new AlgorithmException("Failed to hash data.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] hash(String... dataParts) throws AlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            digest.update(cat(dataParts).getBytes("UTF-8"));
            return digest.digest();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new AlgorithmException("Failed to hash data.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] sign(PrivateKey key, byte[] data) throws AlgorithmException, KeyStoreUnavailableException {
        try {
            Signature instance = Signature.getInstance(SIGNATURE_ALGORITHM);
            instance.initSign(key);
            instance.update(data);
            return instance.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new AlgorithmException("Failed to sign data.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static boolean verify(PublicKey key, byte[] data, byte[] signature) throws AlgorithmException, KeyStoreUnavailableException {
        try {
            Signature instance = Signature.getInstance(SIGNATURE_ALGORITHM);
            instance.initVerify(key);
            instance.update(data);
            return instance.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new AlgorithmException("Failed to verify signature.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] encrypt(PublicKey key, byte[] data) throws AlgorithmException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new AlgorithmException("Failed to sign data.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] decrypt(PrivateKey key, byte[] data) throws AlgorithmException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            throw new AlgorithmException("Failed to sign data.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
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

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static String cat(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part);
        }
        return builder.toString();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static String toBase64(String data) {
        return Base64.encodeToString(data.getBytes(), Base64.DEFAULT);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static String toBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] fromBase64(String data) {
        return Base64.decode(data, Base64.DEFAULT);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] fromBase64(byte[] data) {
        return Base64.decode(new String(data), Base64.DEFAULT);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] random(int nBytes) {
        byte[] bytes = new byte[nBytes];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static PublicKey decodePublicKey(byte[] encoded) throws AlgorithmException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new AlgorithmException("Failed to load public RSA key.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static PrivateKey decodePrivateKey(byte[] encoded) throws AlgorithmException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new AlgorithmException("Failed to load private RSA key.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
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
            throw new Exception("Failed to read input stream", e);
        }
        return bos.toByteArray();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void listAvailableAlgorithms() {
        for (Provider provider : Security.getProviders()) {
            Log.d("PROVIDER", provider.getName());
            for (String key : provider.stringPropertyNames())
                Log.d("ALGOS", "\t" + key + "\t" + provider.getProperty(key));
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static List<byte[]> splitAt(byte[] buffer, int... indexes) throws AlgorithmException {
        List<byte[]> result = new ArrayList<>();
        int lastIndex = 0;
        for (int index : indexes) {
            if (index >= buffer.length) {
                throw new AlgorithmException("Buffer is too short to split.", null);
            }
            result.add(Arrays.copyOfRange(buffer, lastIndex, index));
            lastIndex = index;
        }
        result.add(Arrays.copyOfRange(buffer, lastIndex, buffer.length));
        return result;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void storeKeyPair(KeyPair keyPair, String alias, KeyProtection keyProtection) throws KeyStoreUnavailableException, AlgorithmException {
        Certificate[] chain = {generateSelfSignedCertificate(keyPair, SELF_SIGNED_COMMON_NAME, VALIDITY_IN_MILLISECONDS)};
        try {
            getAndroidKeyStore().setEntry(alias, new KeyStore.PrivateKeyEntry(keyPair.getPrivate(), chain), keyProtection);
        } catch (KeyStoreException e) {
            throw new KeyStoreUnavailableException("Failed to store key pair in key store", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static KeyPair createEncryptionKey(String alias, byte[] seed) throws AlgorithmException, KeyStoreUnavailableException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA);
            generator.initialize(RSA_KEY_SIZE, new SecureRandom(seed));
            KeyPair keyPair = generator.generateKeyPair();
            storeKeyPair(keyPair, alias, new KeyProtection.Builder(KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .build());
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new AlgorithmException("Failed to create key pair.", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static KeyPair createSigningKey(String alias, byte[] seed) throws AlgorithmException, KeyStoreUnavailableException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA);
            generator.initialize(RSA_KEY_SIZE, new SecureRandom(seed));
            KeyPair keyPair = generator.generateKeyPair();
            storeKeyPair(keyPair, alias, new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                    .build());
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new AlgorithmException("Failed to create key pair.", e);
        }
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String commonName, long validityInMilliseconds) throws AlgorithmException {
        // This is absolutely ridiculous. This code requires an extra (large!) dependency,
        // all so it can pass a completely useless parameter to a function that doesn't
        // take no for an answer.
        // All this is simply generating a self-signed certificate to store the public part in
        // the key store.
        X500Name x500Name = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, commonName).build();
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                x500Name,
                BigInteger.valueOf(new Random().nextLong()),
                new Date(),
                new Date(System.currentTimeMillis() + validityInMilliseconds),
                x500Name,
                subjectPublicKeyInfo);
        Security.addProvider(new BouncyCastleProvider());
        try {
            X509CertificateHolder certHolder = certBuilder.build(new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("SC").build(keyPair.getPrivate()));
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        } catch (CertificateException | OperatorCreationException e) {
            throw new AlgorithmException("Failed to generate self-signed certificate from key pair", e);
        }
    }

    private static KeyStore getAndroidKeyStore() throws KeyStoreUnavailableException {
        if (androidKeyStore == null) {
            try {
                androidKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
                androidKeyStore.load(null);
            } catch (IOException | GeneralSecurityException e) {
                throw new KeyStoreUnavailableException("Failed to load Android key store", e);
            }
        }
        return androidKeyStore;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static KeyPair loadKeyPair(String alias) throws MissingKeyException, KeyStoreUnavailableException {
        try {
            PrivateKey privateKey = (PrivateKey) getAndroidKeyStore().getKey(alias, null);
            Certificate certificate = getAndroidKeyStore().getCertificate(alias);
            if (certificate == null) {
                throw new MissingKeyException("Failed to load public key", null);
            }
            PublicKey publicKey = certificate.getPublicKey();
            return new KeyPair(publicKey, privateKey);
        } catch (GeneralSecurityException e) {
            throw new MissingKeyException("Failed to load key pair", e);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static byte[] sign(String alias, byte[] data) throws KeyStoreUnavailableException, MissingKeyException, AlgorithmException {
        return sign(loadKeyPair(alias).getPrivate(), data);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static boolean verify(String alias, byte[] data, byte[] signature) throws KeyStoreUnavailableException, MissingKeyException, AlgorithmException {
        return verify(loadKeyPair(alias).getPublic(), data, signature);
    }
}
