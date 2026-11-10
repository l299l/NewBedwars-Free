package com.l299l.newbedwars.common.update;

import java.io.IOException;
import java.util.List;

public interface UpdateSource {
    String name();

    List<ReleaseInfo> releases(Product product) throws IOException;
}
