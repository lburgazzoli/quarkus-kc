package io.github.lburgazzoli.quarkus.ck;

import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.runtime.WorkerConfig;

import static org.apache.kafka.connect.runtime.TopicCreationConfig.PARTITIONS_VALIDATOR;
import static org.apache.kafka.connect.runtime.TopicCreationConfig.REPLICATION_FACTOR_VALIDATOR;

public class ConnectConfig extends WorkerConfig {
    private static final ConfigDef CONFIG;

    public static final String OFFSET_STORAGE_PREFIX = "offset.storage.";
    public static final String TOPIC_SUFFIX = "topic";
    public static final String PARTITIONS_SUFFIX = "partitions";
    public static final String REPLICATION_FACTOR_SUFFIX = "replication.factor";

    /**
     * <code>offset.storage.topic</code>
     */
    public static final String OFFSET_STORAGE_TOPIC_CONFIG = OFFSET_STORAGE_PREFIX + TOPIC_SUFFIX;
    private static final String OFFSET_STORAGE_TOPIC_CONFIG_DOC = "The name of the Kafka topic where source connector offsets are stored";

    /**
     * <code>offset.storage.partitions</code>
     */
    public static final String OFFSET_STORAGE_PARTITIONS_CONFIG = OFFSET_STORAGE_PREFIX + PARTITIONS_SUFFIX;
    private static final String OFFSET_STORAGE_PARTITIONS_CONFIG_DOC = "The number of partitions used when creating the offset storage topic";

    /**
     * <code>offset.storage.replication.factor</code>
     */
    public static final String OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG = OFFSET_STORAGE_PREFIX + REPLICATION_FACTOR_SUFFIX;
    private static final String OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG_DOC = "Replication factor used when creating the offset storage topic";


    static {
        CONFIG = baseConfigDef()
            .define(OFFSET_STORAGE_TOPIC_CONFIG,
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                OFFSET_STORAGE_TOPIC_CONFIG_DOC)
            .define(OFFSET_STORAGE_PARTITIONS_CONFIG,
                ConfigDef.Type.INT,
                25,
                PARTITIONS_VALIDATOR,
                ConfigDef.Importance.LOW,
                OFFSET_STORAGE_PARTITIONS_CONFIG_DOC)
            .define(OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG,
                ConfigDef.Type.SHORT,
                (short) 3,
                REPLICATION_FACTOR_VALIDATOR,
                ConfigDef.Importance.LOW,
                OFFSET_STORAGE_REPLICATION_FACTOR_CONFIG_DOC);
    }

    public ConnectConfig(Map<String, String> props) {
        super(CONFIG, props);
    }
}
