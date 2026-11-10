package com.l299l.newbedwars.common.update;

import java.io.IOException;
import java.io.InputStream;

public interface Fetcher {
    String get(String url) throws IOException;

    InputStream open(String url) throws IOException;
}
