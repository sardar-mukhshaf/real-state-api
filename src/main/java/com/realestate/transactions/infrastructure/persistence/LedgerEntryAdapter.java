package com.realestate.transactions.infrastructure.persistence;

import com.realestate.shared.application.PageResult;
import com.realestate.shared.domain.BusinessException;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import com.realestate.transactions.application.LedgerEntryRepository;
import com.realestate.transactions.domain.LedgerEntry;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class LedgerEntryAdapter extends JpaStore<LedgerEntry, LedgerEntryEntity>
        implements LedgerEntryRepository {
    public LedgerEntryAdapter(LedgerEntryJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                LedgerEntryEntity.class,
                LedgerEntryMapper::toDomain,
                LedgerEntryMapper::toEntity);
    }

    public PageResult<LedgerEntry> search(
            String propertyId,
            String propertyName,
            List<String> types,
            int page,
            int size,
            String sort,
            boolean descending) {
        com.realestate.shared.application.Query.all().page(page, size);
        var allowed =
                Map.of(
                        "id",
                        "t.id",
                        "amount",
                        "t.amount",
                        "transaction_date",
                        "t.transaction_date",
                        "created_at",
                        "t.created_at");
        if (!allowed.containsKey(sort))
            throw BusinessException.invalid("Unsupported transaction sort");
        if (types != null
                && (types.size() > 4
                        || !Set.of("RENT", "EXPENSE", "MANAGEMENT", "PAYMENT").containsAll(types)))
            throw BusinessException.invalid("Invalid transaction types");
        var params = new HashMap<String, Object>();
        String where = " WHERE true";
        if (propertyId != null) {
            where += " AND t.property_id=:property";
            params.put("property", propertyId);
        }
        if (propertyName != null && !propertyName.isBlank()) {
            if (propertyName.length() > 255)
                throw BusinessException.invalid("Property filter is too long");
            where += " AND lower(p.name) LIKE :name ESCAPE '!'";
            params.put(
                    "name",
                    "%"
                            + propertyName
                                    .toLowerCase(Locale.ROOT)
                                    .replace("!", "!!")
                                    .replace("%", "!%")
                                    .replace("_", "!_")
                            + "%");
        }
        if (types != null && !types.isEmpty()) {
            where += " AND t.type IN (:types)";
            params.put("types", types);
        }
        String from = " FROM \"Transaction\" t JOIN \"Property\" p ON p.id=t.property_id";
        var rows =
                em.createNativeQuery(
                        "SELECT t.*"
                                + from
                                + where
                                + " ORDER BY "
                                + allowed.get(sort)
                                + (descending ? " DESC" : " ASC")
                                + ",t.id",
                        LedgerEntryEntity.class);
        var count = em.createNativeQuery("SELECT count(*)" + from + where);
        params.forEach(
                (key, value) -> {
                    rows.setParameter(key, value);
                    count.setParameter(key, value);
                });
        @SuppressWarnings("unchecked")
        List<LedgerEntryEntity> values =
                rows.setFirstResult(page * size).setMaxResults(size).getResultList();
        return new PageResult<>(
                values.stream().map(LedgerEntryMapper::toDomain).toList(),
                page,
                size,
                ((Number) count.getSingleResult()).longValue());
    }
}
