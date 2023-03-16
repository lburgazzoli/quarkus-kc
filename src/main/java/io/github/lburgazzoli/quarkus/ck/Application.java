package io.github.lburgazzoli.quarkus.ck;


import javax.inject.Inject;

import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.policy.ConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.isolation.Plugins;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo;
import org.apache.kafka.connect.runtime.standalone.StandaloneHerder;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @Inject
    ApplicationConfig appConfig;
    @Inject
    WorkerConfig config;
    @Inject
    OffsetBackingStore offsetStore;
    @Inject
    RestServer restServer;

    @Override
    public int run(String... args) throws Exception {
        try {
            LOGGER.info("Kafka Connect standalone worker initializing ...");
            LOGGER.info("Scanning for plugin classes. This might take a moment ...");

            Plugins plugins = new Plugins(appConfig.worker());
            plugins.compareAndSwapWithDelegatingLoader();

            String kafkaClusterId = config.kafkaClusterId();
            LOGGER.debug("Kafka cluster ID: {}", kafkaClusterId);\

            ConnectorClientConfigOverridePolicy connectorClientConfigOverridePolicy = plugins.newPlugin(
                config.getString(WorkerConfig.CONNECTOR_CLIENT_POLICY_CLASS_CONFIG),
                config,
                ConnectorClientConfigOverridePolicy.class);

            Worker worker = new Worker(
                appConfig.id(),
                Time.SYSTEM,
                plugins,
                config,
                offsetStore,
                connectorClientConfigOverridePolicy);

            final Herder herder = new StandaloneHerder(worker, kafkaClusterId, connectorClientConfigOverridePolicy);
            final Connect connect = new Connect(herder, restServer);

            try {
                connect.start();

                if (appConfig.connectors() != null){

                    for (var entry: appConfig.connectors().entrySet()) {
                        FutureCallback<Herder.Created<ConnectorInfo>> cb = new FutureCallback<>((error, info) -> {
                            if (error != null) {
                                LOGGER.error("Failed to create job for {}", entry.getKey());
                            } else {
                                LOGGER.info("Created connector {}", info.result().name());
                            }
                        });

                        herder.putConnectorConfig(
                            entry.getKey(),
                            entry.getValue().params(),
                            false,
                            cb);
                        cb.get();

                    }
                }
            } catch (Throwable t) {
                LOGGER.error("Stopping after connector error", t);
                connect.stop();
                return 3;
            }

            connect.awaitStop();

        } catch (Throwable t) {
            LOGGER.error("Stopping due to error", t);
            return 2;
        }

        return 0;
    }

    public static void main(String ... args) {
        Quarkus.run(Application.class, args);
    }
}
