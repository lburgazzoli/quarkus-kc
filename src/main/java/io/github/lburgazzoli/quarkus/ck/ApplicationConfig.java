package io.github.lburgazzoli.quarkus.ck;

import io.smallrye.config.ConfigMapping;

import java.io.File;
import java.util.Map;

@ConfigMapping(prefix = "kc")
public interface ApplicationConfig {

    String id();

    Map<String, String> worker();

    Map<String, Connector> connectors();

    interface Connector {
        String type();
        Map<String, String> config();
    }

    Prometheus prometheus();

    interface Prometheus {
        File config();
    }
}
