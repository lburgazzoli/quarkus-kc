package io.github.lburgazzoli.quarkus.ck;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.jmx.JmxCollector;
import io.quarkus.arc.Unremovable;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.policy.AllConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.connector.policy.ConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.SharedTopicAdmin;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.admin.AdminClientConfig.CLIENT_ID_CONFIG;

public class ApplicationProducers {
    @Produces
    @Singleton
    public WorkerConfig config(ApplicationConfig config) {
        return new ConnectConfig(config.worker());
    }

    @Unremovable
    @Produces
    @Singleton
    public CollectorRegistry collectorRegistry(ApplicationConfig config)
            throws MalformedObjectNameException, IOException {

        CollectorRegistry registry = new CollectorRegistry(true);
        registry.register(new JmxCollector(config.prometheus().config(), JmxCollector.Mode.AGENT));

        return registry;
    }

    @Produces
    @Singleton
    public OffsetBackingStore offsetStore(ApplicationConfig appConfig, WorkerConfig config) {
        OffsetBackingStore store;

        String path = config.getString(ConnectConfig.OFFSET_STORAGE_FILE_FILENAME_CONFIG);
        if (path != null && !path.isEmpty()) {
            store = new FileOffsetBackingStore();
        } else {
            Map<String, Object> adminProps = new HashMap<>(config.originals());
            adminProps.put(CLIENT_ID_CONFIG, appConfig.id() + "-admin");

            SharedTopicAdmin admin = new SharedTopicAdmin(adminProps);

            store = new KafkaOffsetBackingStore(admin);
        }

        store.configure(config);

        return store;

    }

    @Produces
    @Singleton
    public RestServer restServer(WorkerConfig config) {
        RestServer rest = new RestServer(config);
        rest.initializeServer();

        return rest;
    }

    @Produces
    @Singleton
    public ConnectorClientConfigOverridePolicy overridePolicy() {
        return new AllConnectorClientConfigOverridePolicy();
    }

    @Produces
    @Singleton
    public Worker worker(
        ApplicationConfig appConfig,
        WorkerConfig config,
        OffsetBackingStore offsetStore,
        ConnectorClientConfigOverridePolicy overridePolicy) {

        Plugins plugins = new Plugins(appConfig.worker());
        plugins.compareAndSwapWithDelegatingLoader();

        return new Worker(
            appConfig.id(),
            Time.SYSTEM,
            plugins,
            config,
            offsetStore,
            overridePolicy);
    }
}
