package com.realestate.shared.application;

public interface TransactionCallbacks {
    void onRollback(Runnable action);
}
