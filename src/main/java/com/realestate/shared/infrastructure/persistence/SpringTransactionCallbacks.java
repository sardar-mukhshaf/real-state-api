package com.realestate.shared.infrastructure.persistence;

import com.realestate.shared.application.TransactionCallbacks;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.*;

@Component
public class SpringTransactionCallbacks implements TransactionCallbacks {
    private static final Logger LOG = LoggerFactory.getLogger(SpringTransactionCallbacks.class);

    public void onRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("A transaction is required");
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED)
                            try {
                                action.run();
                            } catch (RuntimeException ex) {
                                LOG.error(
                                        "Rollback storage cleanup failed; reconciliation required");
                            }
                    }
                });
    }
}
