package com.l299l.newbedwars.common.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpFetcher implements Fetcher {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration API_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(2);

    private final HttpClient client;
    private final String userAgent;

    public HttpFetcher(String userAgent) {
        this.userAgent = userAgent;
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String get(String url) throws IOException {
        HttpResponse<String> response = send(request(url, API_TIMEOUT).header("Accept", "application/json").build(),
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    @Override
    public InputStream open(String url) throws IOException {
        HttpResponse<InputStream> response = send(request(url, DOWNLOAD_TIMEOUT).build(),
                HttpResponse.BodyHandlers.ofInputStream());
        return response.body();
    }

    private HttpRequest.Builder request(String url, Duration timeout) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("Only https URLs are allowed: " + url);
        return HttpRequest.newBuilder(uri).timeout(timeout).header("User-Agent", userAgent).GET();
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            HttpResponse<T> response = client.send(request, handler);
            if (response.statusCode() != 200) {
                if (response.body() instanceof InputStream in) in.close();
                throw new IOException("HTTP " + response.statusCode() + " from " + request.uri().getHost());
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
