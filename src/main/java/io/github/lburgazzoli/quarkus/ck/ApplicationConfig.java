package io.github.lburgazzoli.quarkus.ck;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "kc")
public interface ApplicationConfig {

    String id();

    Map<String, String> worker();

    Map<String, Connector> connectors();

    interface Connector {
        String type();
        Map<String, String> config();
    }
}
