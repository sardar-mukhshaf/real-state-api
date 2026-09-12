package com.realestate.contracts.application;

import com.realestate.contracts.domain.TenancyContract;

public interface ContractUsage {
    void ensureCompatible(TenancyContract contract);

    void ensureDeletable(String id);
}
