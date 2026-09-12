package com.realestate.transactions.application;

import com.realestate.shared.application.*;
import com.realestate.transactions.domain.LedgerEntry;
import java.util.List;

public interface LedgerEntryRepository extends Store<LedgerEntry> {
    PageResult<LedgerEntry> search(
            String propertyId,
            String propertyName,
            List<String> types,
            int page,
            int size,
            String sort,
            boolean descending);
}
