package com.l299l.newbedwars.panel.network;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Logger;

public final class PeerClient {
    public record Reply(int status, String contentType, String disposition, long length, InputStream body) {}

    public interface Payload {
        record None() implements Payload {}

        record Bytes(byte[] data, String contentType) implements Payload {}

        record Stream(InputStream in, long length, String contentType) implements Payload {}
    }

    private final NetworkConfig.Peer peer;
    private final String secret;
    private final PeerPins pins;
    private final Logger logger;
    private final HttpClient client;

    public PeerClient(NetworkConfig.Peer peer, String secret, PeerPins pins, Logger logger) {
        this.peer = peer;
        this.secret = secret;
        this.pins = pins;
        this.logger = logger;
        HttpClient.Builder builder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER);
        if (peer.https() && !peer.systemCertificates()) {
            try {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{new PinningTrust(this::pin, this::firstUse)}, new SecureRandom());
                builder.sslContext(context);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        this.client = builder.build();
    }

    public NetworkConfig.Peer peer() {
        return peer;
    }

    public String pin() {
        if (!peer.fingerprint().isEmpty() && !peer.systemCertificates()) return peer.fingerprint();
        return pins.get(peer.id());
    }

    public String pinSource() {
        if (peer.systemCertificates()) return "system";
        if (!peer.fingerprint().isEmpty()) return "config";
        return pins.get(peer.id()) == null ? "none" : "first-use";
    }

    private void firstUse(String fingerprint) {
        if (!peer.fingerprint().isEmpty()) return;
        pins.put(peer.id(), fingerprint);
        logger.info("Panel peer " + peer.id() + ": trusting its certificate on first use (SHA-256 " + fingerprint
                + "). Run /nbwpanel network forget " + peer.id() + " if it changes on purpose.");
    }

    public Reply send(String method, String rawPath, String rawQuery, NodeAuth.Identity identity, String bodyHash,
                      Payload payload, Duration timeout) throws PeerException, InterruptedException {
        String query = rawQuery == null || rawQuery.isEmpty() ? "" : "?" + rawQuery;
        HttpRequest.BodyPublisher publisher;
        String contentType = null;
        if (payload instanceof Payload.Bytes b) {
            publisher = HttpRequest.BodyPublishers.ofByteArray(b.data());
            contentType = b.contentType();
        } else if (payload instanceof Payload.Stream s) {
            HttpRequest.BodyPublisher stream = HttpRequest.BodyPublishers.ofInputStream(() -> s.in());
            publisher = s.length() >= 0 ? HttpRequest.BodyPublishers.fromPublisher(stream, s.length()) : stream;
            contentType = s.contentType();
        } else {
            publisher = HttpRequest.BodyPublishers.noBody();
        }
        NodeAuth.Call call = new NodeAuth.Call(method, rawPath, rawQuery == null ? "" : rawQuery, System.currentTimeMillis(),
                NodeAuth.nonce(), identity, bodyHash, peer.id());
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(peer.url() + rawPath + query))
                .method(method, publisher).timeout(timeout);
        for (Map.Entry<String, String> header : NodeAuth.headers(secret, call).entrySet()) {
            request.header(header.getKey(), header.getValue());
        }
        if (contentType != null && !contentType.isBlank()) request.header("Content-Type", contentType);
        try {
            HttpResponse<InputStream> response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
            return new Reply(response.statusCode(), response.headers().firstValue("Content-Type").orElse(null),
                    response.headers().firstValue("Content-Disposition").orElse(null),
                    response.headers().firstValueAsLong("Content-Length").orElse(-1), response.body());
        } catch (HttpTimeoutException e) {
            throw new PeerException(PeerException.Kind.TIMEOUT, e);
        } catch (ConnectException e) {
            throw new PeerException(PeerException.Kind.UNREACHABLE, e);
        } catch (IOException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t instanceof PinningTrust.PinMismatch) throw new PeerException(PeerException.Kind.UNTRUSTED, e);
            }
            throw new PeerException(PeerException.Kind.UNREACHABLE, e);
        }
    }
}
