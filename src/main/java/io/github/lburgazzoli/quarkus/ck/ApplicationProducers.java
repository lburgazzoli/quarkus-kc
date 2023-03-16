package io.github.lburgazzoli.quarkus.ck;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.SharedTopicAdmin;

import static org.apache.kafka.clients.admin.AdminClientConfig.CLIENT_ID_CONFIG;

public class ApplicationProducers {
    @Produces
    @Singleton
    public WorkerConfig config(ApplicationConfig config) {
        return new ConnectConfig(config.worker());
    }

    @Produces
    @Singleton
    public OffsetBackingStore offsetStore(ApplicationConfig appConfig, WorkerConfig config) {
        Map<String, Object> adminProps = new HashMap<>(config.originals());
        adminProps.put(CLIENT_ID_CONFIG, appConfig.id() + "-admin");

        SharedTopicAdmin admin = new SharedTopicAdmin(adminProps);

        KafkaOffsetBackingStore store = new KafkaOffsetBackingStore(admin,appConfig::id);
        store.configure(config);

        return store;
    }

    @Produces
    @Singleton
    public RestServer restServer(WorkerConfig config) {
        RestServer rest = new RestServer(config, null);
        rest.initializeServer();

        return rest;
    }
}
