package com.l299l.newbedwars.panel.tls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SelfSignedCertificateTest {
    @TempDir
    Path dir;

    private final byte[] key = new byte[32];

    @Test
    void generatedCertificateParsesAndVerifies() throws Exception {
        Date now = new Date();
        SelfSignedCertificate.Generated g = SelfSignedCertificate.generate("NewBedwars Panel",
                List.of("localhost", "panel.example.com", "127.0.0.1", "::1", "192.168.1.20"), now);
        X509Certificate c = g.certificate();
        c.checkValidity(new Date(now.getTime() + 1000));
        assertEquals(3, c.getVersion());
        assertEquals("SHA256withECDSA", c.getSigAlgName());
        assertEquals(-1, c.getBasicConstraints());
        assertTrue(c.getKeyUsage()[0]);
        assertEquals(List.of("1.3.6.1.5.5.7.3.1"), c.getExtendedKeyUsage());
        assertTrue(c.getSubjectX500Principal().getName().contains("CN=NewBedwars Panel"));
        List<String> sans = new ArrayList<>();
        for (List<?> san : c.getSubjectAlternativeNames()) sans.add(san.get(0) + "=" + san.get(1));
        assertTrue(sans.contains("2=localhost"), sans.toString());
        assertTrue(sans.contains("2=panel.example.com"), sans.toString());
        assertTrue(sans.contains("7=127.0.0.1"), sans.toString());
        assertTrue(sans.contains("7=192.168.1.20"), sans.toString());
        assertTrue(sans.stream().anyMatch(s -> s.startsWith("7=0:0:0:0:0:0:0:1")), sans.toString());
        long days = TimeUnit.MILLISECONDS.toDays(c.getNotAfter().getTime() - c.getNotBefore().getTime());
        assertEquals(SelfSignedCertificate.VALID_DAYS, days);
        assertTrue(SelfSignedCertificate.fingerprint(c).matches("([0-9A-F]{2}:){31}[0-9A-F]{2}"));
    }

    @Test
    void namesAreCleaned() {
        assertEquals(Set.of("localhost", "10.0.0.1", "a-b.example.org"),
                SelfSignedCertificate.cleanNames(List.of(" localhost ", "0.0.0.0", "", "10.0.0.1", "a-b.example.org", "bad name", "x/y")));
    }

    @Test
    void keystoreIsReusedUntilNamesChangeOrItExpires() throws Exception {
        Path file = dir.resolve("selfsigned.p12");
        Date now = new Date();
        TlsSetup.Result first = TlsSetup.selfSigned(file, key, List.of("localhost", "127.0.0.1"), now);
        assertTrue(first.created());
        assertTrue(first.selfSigned());
        assertTrue(Files.isRegularFile(file));
        assertNotNull(first.context());

        TlsSetup.Result again = TlsSetup.selfSigned(file, key, List.of("127.0.0.1", "LOCALHOST"), now);
        assertFalse(again.created());
        assertEquals(first.fingerprint(), again.fingerprint());

        TlsSetup.Result moreNames = TlsSetup.selfSigned(file, key, List.of("localhost", "panel.example.com"), now);
        assertTrue(moreNames.created());

        Date later = new Date(now.getTime() + TimeUnit.DAYS.toMillis(SelfSignedCertificate.VALID_DAYS - 10));
        assertTrue(TlsSetup.selfSigned(file, key, List.of("localhost"), later).created());

        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        assertTrue(TlsSetup.selfSigned(file, otherKey, List.of("localhost"), later).created());
    }

    @Test
    void ownKeystoreIsLoaded() throws Exception {
        Path file = dir.resolve("selfsigned.p12");
        TlsSetup.Result made = TlsSetup.selfSigned(file, key, List.of("localhost"), new Date());
        TlsSetup.Result own = TlsSetup.ownKeystore(file, TlsSetup.keystorePassword(key));
        assertFalse(own.selfSigned());
        assertEquals(made.fingerprint(), own.fingerprint());
        assertThrows(Exception.class, () -> TlsSetup.ownKeystore(file, "wrong".toCharArray()));
        assertThrows(Exception.class, () -> TlsSetup.ownKeystore(dir.resolve("missing.p12"), "x".toCharArray()));
    }
}
