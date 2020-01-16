package io.netty.example.https.security;

import io.netty.example.https.util.ApplicationProperties;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

public final class KeyStoreData {

    public static final String KEYSTORE_FILEPATH = "keystore.filepath";
    public static final String KEYSTORE_PASSWORD = "keystore.password";
    public static final String ENTRY_ALIAS = "entry.alias";
    public static final String ENTRY_KEY_PASSWORD = "entry.key.password";

    public static final String PKCS_12 = "PKCS12";

    /**
     * Returns a keystore data object from the keystore file specified in the security configuration file.
     *
     * @param securityConfigFilePath The path of the security configuration file
     * @return keystore data object with data from the keystore file
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws UnrecoverableEntryException
     */
    public static KeyStoreData from(String securityConfigFilePath)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException,
                   UnrecoverableEntryException {

        Properties securityConfigProp = ApplicationProperties.from(securityConfigFilePath);
        final String keystoreFilePath = securityConfigProp.getProperty(KEYSTORE_FILEPATH);
        final char[] keystorePassword = securityConfigProp.getProperty(KEYSTORE_PASSWORD).toCharArray();
        final String alias = securityConfigProp.getProperty(ENTRY_ALIAS);
        final char[] entryKeyPassword = securityConfigProp.getProperty(ENTRY_KEY_PASSWORD).toCharArray();

        KeyStore keyStore = KeyStore.getInstance(PKCS_12);
        try (final FileChannel keystoreFileChannel = FileChannel
                .open(Paths.get(keystoreFilePath), StandardOpenOption.READ)) {
            keyStore.load(Channels.newInputStream(keystoreFileChannel), keystorePassword);
        }

        PrivateKeyEntry privateKeyEntry =
                (PrivateKeyEntry) keyStore.getEntry(alias, new PasswordProtection(entryKeyPassword));
        return new KeyStoreData(privateKeyEntry.getPrivateKey(),
                                (X509Certificate[]) privateKeyEntry.getCertificateChain());
    }

    private final PrivateKey key;
    private final X509Certificate[] certificateChain;

    public KeyStoreData(PrivateKey key, X509Certificate[] certificateChain) {
        this.key = key;
        this.certificateChain = certificateChain;
    }

    public PrivateKey getKey() {
        return key;
    }

    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }
}