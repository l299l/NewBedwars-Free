package com.l299l.newbedwars.panel.tls;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class TlsSetup {
    private static final String ALIAS = "panel";
    private static final long RENEW_BEFORE_MILLIS = TimeUnit.DAYS.toMillis(30);

    public record Result(SSLContext context, X509Certificate certificate, boolean selfSigned, boolean created, String fingerprint) {}

    private TlsSetup() {}

    public static Result ownKeystore(Path keystore, char[] password) throws IOException, GeneralSecurityException {
        if (!Files.isRegularFile(keystore)) throw new IOException("keystore " + keystore + " does not exist");
        KeyStore store = KeyStore.getInstance(keystore.toFile(), password);
        X509Certificate certificate = null;
        for (Enumeration<String> aliases = store.aliases(); aliases.hasMoreElements(); ) {
            String alias = aliases.nextElement();
            if (store.isKeyEntry(alias) && store.getCertificate(alias) instanceof X509Certificate x) {
                certificate = x;
                break;
            }
        }
        if (certificate == null) throw new GeneralSecurityException("the keystore has no private key with a certificate");
        return new Result(context(store, password), certificate, false, false, SelfSignedCertificate.fingerprint(certificate));
    }

    public static Result selfSigned(Path file, byte[] panelKey, Collection<String> wantedNames, Date now)
            throws IOException, GeneralSecurityException {
        char[] password = keystorePassword(panelKey);
        Set<String> names = SelfSignedCertificate.cleanNames(wantedNames);
        if (Files.isRegularFile(file)) {
            try {
                KeyStore store = KeyStore.getInstance("PKCS12");
                try (InputStream in = Files.newInputStream(file)) {
                    store.load(in, password);
                }
                Certificate existing = store.getCertificate(ALIAS);
                if (existing instanceof X509Certificate x && store.isKeyEntry(ALIAS) && stillGood(x, names, now)) {
                    return new Result(context(store, password), x, true, false, SelfSignedCertificate.fingerprint(x));
                }
            } catch (IOException | GeneralSecurityException ignored) {
            }
        }
        SelfSignedCertificate.Generated generated = SelfSignedCertificate.generate("NewBedwars Panel", names,
                new Date(now.getTime() - TimeUnit.HOURS.toMillis(1)));
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry(ALIAS, generated.keyPair().getPrivate(), password, new Certificate[]{generated.certificate()});
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            store.store(out, password);
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        X509Certificate certificate = generated.certificate();
        return new Result(context(store, password), certificate, true, true, SelfSignedCertificate.fingerprint(certificate));
    }

    static boolean stillGood(X509Certificate certificate, Set<String> names, Date now) {
        if (certificate.getNotAfter().getTime() - now.getTime() < RENEW_BEFORE_MILLIS) return false;
        if (certificate.getNotBefore().after(now)) return false;
        Set<String> present = new HashSet<>();
        try {
            Collection<List<?>> sans = certificate.getSubjectAlternativeNames();
            if (sans != null) {
                for (List<?> san : sans) present.add(normalize(String.valueOf(san.get(1))));
            }
        } catch (CertificateParsingException e) {
            return false;
        }
        for (String name : names) {
            if (!present.contains(normalize(name))) return false;
        }
        return true;
    }

    private static String normalize(String name) {
        byte[] ip = SelfSignedCertificate.ipBytes(name);
        if (ip != null) {
            try {
                return InetAddress.getByAddress(ip).getHostAddress();
            } catch (IOException e) {
                return name;
            }
        }
        return name.toLowerCase(Locale.ROOT);
    }

    static char[] keystorePassword(byte[] panelKey) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update("nbw-panel-selfsigned".getBytes(StandardCharsets.US_ASCII));
        return Base64.getEncoder().encodeToString(digest.digest(panelKey)).toCharArray();
    }

    private static SSLContext context(KeyStore store, char[] password) throws GeneralSecurityException {
        KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);
        return context;
    }

    public static List<String> defaultNames(String bind, Collection<String> configured) {
        Set<String> names = new LinkedHashSet<>(List.of("localhost", "127.0.0.1", "::1"));
        if (bind != null && !bind.isBlank()) names.add(bind.trim());
        names.addAll(configured);
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp()) continue;
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address.isLinkLocalAddress() || address.isLoopbackAddress()) continue;
                    String text = address.getHostAddress();
                    int zone = text.indexOf('%');
                    names.add(zone < 0 ? text : text.substring(0, zone));
                }
            }
        } catch (SocketException ignored) {
        }
        return new ArrayList<>(names);
    }
}
