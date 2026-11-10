package com.l299l.newbedwars.panel.tls;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

final class Der {
    private Der() {}

    static byte[] tlv(int tag, byte[]... parts) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (byte[] p : parts) body.writeBytes(p);
        byte[] content = body.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        writeLength(out, content.length);
        out.writeBytes(content);
        return out.toByteArray();
    }

    private static void writeLength(ByteArrayOutputStream out, int length) {
        if (length < 0x80) {
            out.write(length);
            return;
        }
        int bytes = 0;
        for (int n = length; n > 0; n >>>= 8) bytes++;
        out.write(0x80 | bytes);
        for (int i = bytes - 1; i >= 0; i--) out.write((length >>> (8 * i)) & 0xFF);
    }

    static byte[] sequence(byte[]... parts) {
        return tlv(0x30, parts);
    }

    static byte[] set(byte[]... parts) {
        return tlv(0x31, parts);
    }

    static byte[] integer(BigInteger value) {
        return tlv(0x02, value.toByteArray());
    }

    static byte[] bool(boolean value) {
        return tlv(0x01, new byte[]{(byte) (value ? 0xFF : 0x00)});
    }

    static byte[] octetString(byte[] value) {
        return tlv(0x04, value);
    }

    static byte[] bitString(byte[] value) {
        return bitString(value, 0);
    }

    static byte[] bitString(byte[] value, int unusedBits) {
        byte[] content = new byte[value.length + 1];
        content[0] = (byte) unusedBits;
        System.arraycopy(value, 0, content, 1, value.length);
        return tlv(0x03, content);
    }

    static byte[] utf8(String value) {
        return tlv(0x0C, value.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] explicit(int number, byte[] content) {
        return tlv(0xA0 | number, content);
    }

    static byte[] implicitPrimitive(int number, byte[] value) {
        return tlv(0x80 | number, value);
    }

    static byte[] time(Date date) {
        SimpleDateFormat utc = new SimpleDateFormat("yyMMddHHmmss'Z'");
        utc.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat general = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        general.setTimeZone(TimeZone.getTimeZone("UTC"));
        String year = new SimpleDateFormat("yyyy").format(date);
        if (Integer.parseInt(year) < 2050) return tlv(0x17, utc.format(date).getBytes(StandardCharsets.US_ASCII));
        return tlv(0x18, general.format(date).getBytes(StandardCharsets.US_ASCII));
    }

    static byte[] oid(String dotted) {
        String[] parts = dotted.split("\\.");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(Integer.parseInt(parts[0]) * 40 + Integer.parseInt(parts[1]));
        for (int i = 2; i < parts.length; i++) {
            long value = Long.parseLong(parts[i]);
            int groups = 1;
            for (long v = value >>> 7; v > 0; v >>>= 7) groups++;
            for (int g = groups - 1; g >= 0; g--) {
                int b = (int) ((value >>> (7 * g)) & 0x7F);
                out.write(g == 0 ? b : b | 0x80);
            }
        }
        return tlv(0x06, out.toByteArray());
    }
}
