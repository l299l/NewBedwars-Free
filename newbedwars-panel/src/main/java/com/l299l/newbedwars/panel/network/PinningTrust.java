package com.l299l.newbedwars.panel.network;

import com.l299l.newbedwars.panel.tls.SelfSignedCertificate;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class PinningTrust extends X509ExtendedTrustManager {
    static final class PinMismatch extends CertificateException {
        PinMismatch(String message) {
            super(message);
        }
    }

    private final Supplier<String> pinned;
    private final Predicate<String> firstUse;

    PinningTrust(Supplier<String> pinned, Predicate<String> firstUse) {
        this.pinned = pinned;
        this.firstUse = firstUse;
    }

    static String fingerprint(X509Certificate certificate) throws GeneralSecurityException {
        return NetworkConfig.normalizeFingerprint(SelfSignedCertificate.fingerprint(certificate));
    }

    private void check(X509Certificate[] chain) throws CertificateException {
        if (chain == null || chain.length == 0) throw new PinMismatch("no certificate");
        String actual;
        try {
            actual = fingerprint(chain[0]);
        } catch (GeneralSecurityException e) {
            throw new PinMismatch("unreadable certificate");
        }
        String expected = pinned.get();
        if (expected == null || expected.isEmpty()) {
            if (!firstUse.test(actual)) throw new PinMismatch("pin file was damaged, certificate not trusted");
            return;
        }
        if (!MessageDigest.isEqual(expected.getBytes(), actual.getBytes())) {
            throw new PinMismatch("certificate fingerprint changed");
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        check(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        check(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        check(chain);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("client certificates are not accepted");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        throw new CertificateException("client certificates are not accepted");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        throw new CertificateException("client certificates are not accepted");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
