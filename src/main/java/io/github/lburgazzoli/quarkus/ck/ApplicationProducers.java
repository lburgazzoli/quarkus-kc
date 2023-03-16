package io.github.lburgazzoli.quarkus.ck;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.kafka.connect.runtime.WorkerConfig;

public class ApplicationProducers {
    @Produces
    @Singleton
    public WorkerConfig config(ApplicationConfig config) {
        return new ConnectConfig(config.connect());
    }

}
