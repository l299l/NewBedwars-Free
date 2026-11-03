package com.l299l.newbedwars.config.data.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public final class SafeYaml {
    private SafeYaml() {}

    public static Yaml create() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options);
    }
}
