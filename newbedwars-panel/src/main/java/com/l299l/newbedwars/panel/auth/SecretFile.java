package com.l299l.newbedwars.panel.auth;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class SecretFile {
    public static final int KEY_BYTES = 32;
    private static final byte[] MAGIC = "NBWP".getBytes(StandardCharsets.US_ASCII);
    private static final byte VERSION = 1;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public SecretFile(byte[] key) {
        if (key == null || key.length != KEY_BYTES) throw new IllegalArgumentException("the key must be " + KEY_BYTES + " bytes");
        this.key = new SecretKeySpec(key.clone(), "AES");
    }

    public byte[] encrypt(byte[] plain) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        byte[] header = header();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(header);
        byte[] sealed = cipher.doFinal(plain);
        byte[] out = new byte[header.length + iv.length + sealed.length];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(iv, 0, out, header.length, iv.length);
        System.arraycopy(sealed, 0, out, header.length + iv.length, sealed.length);
        return out;
    }

    public byte[] decrypt(byte[] data) throws GeneralSecurityException {
        byte[] header = header();
        if (data.length < header.length + IV_BYTES + TAG_BITS / 8
                || !Arrays.equals(Arrays.copyOf(data, header.length), header)) {
            throw new GeneralSecurityException("not a panel data file");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, header.length, IV_BYTES));
        cipher.updateAAD(header);
        return cipher.doFinal(data, header.length + IV_BYTES, data.length - header.length - IV_BYTES);
    }

    private static byte[] header() {
        byte[] header = Arrays.copyOf(MAGIC, MAGIC.length + 1);
        header[MAGIC.length] = VERSION;
        return header;
    }

    public static byte[] loadOrCreateKey(Path file, String fromEnvironment) throws IOException {
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            byte[] key;
            try {
                key = Base64.getDecoder().decode(fromEnvironment.trim());
            } catch (IllegalArgumentException e) {
                throw new IOException("NBW_PANEL_KEY is not valid Base64");
            }
            if (key.length != KEY_BYTES) throw new IOException("NBW_PANEL_KEY must be " + KEY_BYTES + " bytes (Base64)");
            return key;
        }
        if (Files.exists(file)) {
            byte[] key = Files.readAllBytes(file);
            if (key.length != KEY_BYTES) throw new IOException(file + " is damaged (expected " + KEY_BYTES + " bytes)");
            return key;
        }
        byte[] key = new byte[KEY_BYTES];
        RANDOM.nextBytes(key);
        writeAtomically(file, key);
        return key;
    }

    public static void writeAtomically(Path file, byte[] data) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
        try {
            restrict(tmp);
            Files.write(tmp, data);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static void restrict(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
        }
    }
}
