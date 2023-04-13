package io.github.lburgazzoli.quarkus.ck;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.jmx.JmxCollector;
import io.quarkus.arc.Unremovable;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.policy.AllConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.connector.policy.ConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.runtime.standalone.StandaloneHerder;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.apache.kafka.connect.storage.KafkaOffsetBackingStore;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.ConnectUtils;
import org.apache.kafka.connect.util.SharedTopicAdmin;
import org.eclipse.microprofile.context.ManagedExecutor;

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

    /**
     * Exposes metrics as part of standard Quarkus' metrics endpoint http://localhost:8080/q/metrics.
     *
     * @param config the application config
     * @return a {@link CollectorRegistry} instance
     * @throws MalformedObjectNameException
     * @throws IOException
     */
    @Unremovable
    @Produces
    @Singleton
    public CollectorRegistry collectorRegistry(ApplicationConfig config)
            throws MalformedObjectNameException, IOException {

        CollectorRegistry registry = new CollectorRegistry(true);
        registry.register(new JmxCollector(config.prometheus().config(), JmxCollector.Mode.AGENT));

        return registry;
    }

    /**
     * Allow to configure either a file based or kafka based store for offsets
     *
     * @param appConfig the application config
     * @param workerConfig the worker config
     * @return a {@link OffsetBackingStore} instance
     */
    @Produces
    @Singleton
    public OffsetBackingStore offsetStore(ApplicationConfig appConfig, WorkerConfig workerConfig) {
        OffsetBackingStore store;

        String path = workerConfig.getString(ConnectConfig.OFFSET_STORAGE_FILE_FILENAME_CONFIG);
        if (path != null && !path.isEmpty()) {
            store = new FileOffsetBackingStore();
        } else {
            Map<String, Object> adminProps = new HashMap<>(workerConfig.originals());
            adminProps.put(CLIENT_ID_CONFIG, appConfig.id() + "-admin");

            SharedTopicAdmin admin = new SharedTopicAdmin(adminProps);

            store = new KafkaOffsetBackingStore(admin);
        }

        store.configure(workerConfig);

        return store;

    }

    @Produces
    @Singleton
    public RestServer restServer(WorkerConfig config) {
        // if only the RestServer were an interface ...
        RestServer rest = new RestServer(config) {
            @Override
            public void initializeResources(Herder herder) {
                // do nothing
            }
        };

        // do not start the rest server as connectors are exposed by quarkus
        // rest.initializeServer();

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

        // if only Plugins were an interface ...
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

    @Produces
    @Singleton
    public Herder herder(
            Worker worker,
            WorkerConfig config,
            ManagedExecutor executor) {

        return new StandaloneHerder(worker, ConnectUtils.lookupKafkaClusterId(config), new AllConnectorClientConfigOverridePolicy()) {
            @Override
            public synchronized void start() {
                executor.submit(super::start);
            }
        };
    }
}
