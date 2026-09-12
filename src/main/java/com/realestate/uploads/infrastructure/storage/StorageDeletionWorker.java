package com.realestate.uploads.infrastructure.storage;

import com.realestate.uploads.application.FileStorage;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StorageDeletionWorker {
    private static final Logger LOG = LoggerFactory.getLogger(StorageDeletionWorker.class);
    private final JdbcTemplate jdbc;
    private final FileStorage storage;
    private final MeterRegistry metrics;

    public StorageDeletionWorker(JdbcTemplate jdbc, FileStorage storage, MeterRegistry metrics) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.storage.deletion-interval-ms:30000}")
    @Transactional
    public void deletePending() {
        var keys =
                jdbc.queryForList(
                        "SELECT object_key FROM \"StorageDeletion\" ORDER BY created_at LIMIT 20 FOR UPDATE SKIP LOCKED",
                        String.class);
        for (String key : keys) {
            try {
                storage.delete(key);
                jdbc.update("DELETE FROM \"StorageDeletion\" WHERE object_key=?", key);
            } catch (RuntimeException ex) {
                metrics.counter("storage.deletion.failures").increment();
                LOG.warn("Storage deletion deferred for retry");
            }
        }
    }
}
