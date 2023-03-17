package io.github.lburgazzoli.quarkus.ck;


import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.apache.kafka.connect.connector.policy.ConnectorClientConfigOverridePolicy;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.ConnectorConfig;
import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.Worker;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo;
import org.apache.kafka.connect.runtime.standalone.StandaloneHerder;
import org.apache.kafka.connect.util.ConnectUtils;
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
    RestServer restServer;
    @Inject
    ConnectorClientConfigOverridePolicy overridePolicy;
    @Inject
    Worker worker;

    @Override
    public int run(String... args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            final Herder herder = new StandaloneHerder(worker, ConnectUtils.lookupKafkaClusterId(config), overridePolicy) {
                @Override
                public synchronized void start() {
                    executor.submit(super::start);
                }
            };

            final Connect connect = new Connect(herder, restServer);

            try {
                connect.start();

                if (appConfig.connectors() != null){
                    for (var entry: appConfig.connectors().entrySet()) {
                        FutureCallback<Herder.Created<ConnectorInfo>> cb = new FutureCallback<>((error, info) -> {
                            if (error != null) {
                                LOGGER.error("Failed to create job for {}", entry.getKey(), error);
                            } else {
                                LOGGER.info("Created connector {}", info.result().name());
                            }
                        });

                        Map<String, String> connectorConfig = new TreeMap<>(entry.getValue().config());
                        connectorConfig.put(ConnectorConfig.NAME_CONFIG, entry.getKey());
                        connectorConfig.put(ConnectorConfig.CONNECTOR_CLASS_CONFIG, entry.getValue().type());

                        LOGGER.info("Creating connector {}", entry.getKey());

                        herder.putConnectorConfig(
                            entry.getKey(),
                            connectorConfig,
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
        } finally {
            executor.shutdownNow();
        }

        return 0;
    }

    public static void main(String ... args) {
        Quarkus.run(Application.class, args);
    }
}
