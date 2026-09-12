package com.realestate.contracts.infrastructure.persistence;

import com.realestate.contracts.application.ContractUsage;
import com.realestate.contracts.domain.TenancyContract;
import com.realestate.shared.domain.BusinessException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContractUsageAdapter implements ContractUsage {
    private final JdbcTemplate jdbc;

    public ContractUsageAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void ensureCompatible(TenancyContract c) {
        Long count =
                jdbc.queryForObject(
                        """
            SELECT count(*) FROM "RentTransaction" r JOIN "Transaction" t ON t.id=r.transaction_id
            WHERE r.tenancy_contract_id=? AND
              (r.tenant_id<>? OR t.property_id<>? OR r.start_date<? OR r.end_date>?)
            """,
                        Long.class,
                        c.id(),
                        c.tenantId(),
                        c.propertyId(),
                        Timestamp.from(c.startDate()),
                        Timestamp.from(c.endDate()));
        if (count != null && count > 0)
            throw BusinessException.conflict(
                    "Existing rent records would no longer match this contract");
    }

    public void ensureDeletable(String id) {
        Long count =
                jdbc.queryForObject(
                        "SELECT count(*) FROM \"RentTransaction\" WHERE tenancy_contract_id=?",
                        Long.class,
                        id);
        if (count != null && count > 0)
            throw BusinessException.conflict(
                    "Delete associated unpaid rent transactions before deleting their contract");
    }
}
