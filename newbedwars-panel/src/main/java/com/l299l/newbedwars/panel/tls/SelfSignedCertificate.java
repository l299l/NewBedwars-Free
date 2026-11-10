package com.l299l.newbedwars.panel.tls;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class SelfSignedCertificate {
    public static final long VALID_DAYS = 397;
    private static final String ECDSA_SHA256 = "1.2.840.10045.4.3.2";
    private static final String COMMON_NAME = "2.5.4.3";
    private static final String SUBJECT_ALT_NAME = "2.5.29.17";
    private static final String BASIC_CONSTRAINTS = "2.5.29.19";
    private static final String KEY_USAGE = "2.5.29.15";
    private static final String EXTENDED_KEY_USAGE = "2.5.29.37";
    private static final String SERVER_AUTH = "1.3.6.1.5.5.7.3.1";
    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern DNS = Pattern.compile("[A-Za-z0-9*]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*");

    public record Generated(KeyPair keyPair, X509Certificate certificate) {}

    private SelfSignedCertificate() {}

    public static Generated generate(String commonName, Collection<String> names, Date notBefore) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        Date notAfter = new Date(notBefore.getTime() + TimeUnit.DAYS.toMillis(VALID_DAYS));
        byte[] algorithm = Der.sequence(Der.oid(ECDSA_SHA256));
        byte[] name = Der.sequence(Der.set(Der.sequence(Der.oid(COMMON_NAME), Der.utf8(commonName))));
        byte[] serial = new byte[16];
        new SecureRandom().nextBytes(serial);
        serial[0] &= 0x7F;
        byte[] tbs = Der.sequence(
                Der.explicit(0, Der.integer(BigInteger.TWO)),
                Der.integer(new BigInteger(1, serial)),
                algorithm,
                name,
                Der.sequence(Der.time(notBefore), Der.time(notAfter)),
                name,
                keyPair.getPublic().getEncoded(),
                Der.explicit(3, Der.sequence(
                        extension(BASIC_CONSTRAINTS, true, Der.sequence()),
                        extension(KEY_USAGE, true, Der.bitString(new byte[]{(byte) 0x80}, 7)),
                        extension(EXTENDED_KEY_USAGE, false, Der.sequence(Der.oid(SERVER_AUTH))),
                        extension(SUBJECT_ALT_NAME, false, subjectAltNames(names)))));
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(tbs);
        byte[] certificate = Der.sequence(tbs, algorithm, Der.bitString(signer.sign()));
        X509Certificate parsed = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certificate));
        parsed.verify(keyPair.getPublic());
        return new Generated(keyPair, parsed);
    }

    private static byte[] extension(String oid, boolean critical, byte[] value) {
        return critical
                ? Der.sequence(Der.oid(oid), Der.bool(true), Der.octetString(value))
                : Der.sequence(Der.oid(oid), Der.octetString(value));
    }

    private static byte[] subjectAltNames(Collection<String> names) {
        List<byte[]> entries = new ArrayList<>();
        for (String name : names) {
            byte[] ip = ipBytes(name);
            if (ip != null) entries.add(Der.implicitPrimitive(7, ip));
            else entries.add(Der.implicitPrimitive(2, name.getBytes(StandardCharsets.US_ASCII)));
        }
        return Der.sequence(entries.toArray(new byte[0][]));
    }

    static byte[] ipBytes(String name) {
        if (!IPV4.matcher(name).matches() && !name.contains(":")) return null;
        try {
            return InetAddress.getByName(name).getAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static Set<String> cleanNames(Collection<String> names) {
        Set<String> result = new LinkedHashSet<>();
        for (String raw : names) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty() || name.equals("0.0.0.0") || name.equals("::")) continue;
            if (ipBytes(name) != null || (DNS.matcher(name).matches() && name.length() <= 253)) result.add(name);
        }
        return result;
    }

    public static String fingerprint(X509Certificate certificate) throws GeneralSecurityException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            if (i > 0) out.append(':');
            out.append(String.format("%02X", digest[i]));
        }
        return out.toString();
    }
}
