package io.github.lburgazzoli.quarkus.ck;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "kc")
public interface ApplicationConfig {

    Map<String, String> worker();

    Map<String, Connector> connectors();

    interface Connector {
        Map<String, String> params();
    }
}
