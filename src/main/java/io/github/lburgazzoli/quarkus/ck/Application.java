package io.github.lburgazzoli.quarkus.ck;


import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.apache.kafka.connect.runtime.Connect;
import org.apache.kafka.connect.runtime.ConnectorConfig;
import org.apache.kafka.connect.runtime.Herder;
import org.apache.kafka.connect.runtime.rest.RestServer;
import org.apache.kafka.connect.runtime.rest.entities.ConnectorInfo;
import org.apache.kafka.connect.util.FutureCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.TreeMap;

@QuarkusMain
public class Application implements QuarkusApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @Inject
    ApplicationConfig appConfig;
    @Inject
    RestServer restServer;
    @Inject
    Herder herder;

    @Override
    public int run(String... args) throws Exception {

        try {
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
        }

        return 0;
    }

    public static void main(String ... args) {
        Quarkus.run(Application.class, args);
    }
}
