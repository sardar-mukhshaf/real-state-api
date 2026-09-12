package com.realestate.contracts.application;

import com.realestate.contracts.domain.TenancyContract;
import com.realestate.properties.application.*;
import com.realestate.properties.domain.Property;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.*;
import com.realestate.users.application.*;
import com.realestate.users.domain.User;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ContractService {
    private final TenancyContractRepository contracts;
    private final PropertyRepository properties;
    private final ContractUsage usage;
    private final UserRepository users;
    private final TenantRepository tenants;
    private final Clock clock;

    public ContractService(
            TenancyContractRepository contracts,
            PropertyRepository properties,
            UserRepository users,
            TenantRepository tenants,
            Clock clock,
            ContractUsage usage) {
        this.contracts = contracts;
        this.properties = properties;
        this.users = users;
        this.tenants = tenants;
        this.clock = clock;
        this.usage = usage;
    }

    public TenancyContract get(String id) {
        return contracts.require(Rules.id(id));
    }

    public List<TenancyContract> list(int page, int size) {
        return contracts.query(Query.all().page(page, size));
    }

    @Transactional
    public TenancyContract create(Changes data) {
        return save(UUID.randomUUID().toString(), data, null);
    }

    @Transactional
    public TenancyContract update(String id, Changes data) {
        return save(id, data, contracts.requireLocked(Rules.id(id)));
    }

    private TenancyContract save(String id, Changes data, TenancyContract old) {
        data.only(
                "property_id",
                "tenant_id",
                "start_date",
                "end_date",
                "mng_fee_percentage",
                "rent_per_month");
        String property = data.string("property_id", old == null ? null : old.propertyId()),
                tenant = data.string("tenant_id", old == null ? null : old.tenantId());
        properties.require(Rules.id(property));
        tenants.require(Rules.id(tenant));
        var value =
                new TenancyContract(
                        id,
                        tenant,
                        property,
                        data.instant("start_date", old == null ? null : old.startDate()),
                        data.instant("end_date", old == null ? null : old.endDate()),
                        data.decimal(
                                "mng_fee_percentage", old == null ? null : old.mngFeePercentage()),
                        data.decimal("rent_per_month", old == null ? null : old.rentPerMonth()),
                        old == null ? clock.instant() : old.createdAt(),
                        clock.instant());
        if (old != null) usage.ensureCompatible(value);
        return contracts.save(value);
    }

    @Transactional
    public boolean delete(String id) {
        contracts.requireLocked(Rules.id(id));
        usage.ensureDeletable(id);
        contracts.delete(id);
        return true;
    }

    public Optional<TenancyContract> activeOrLatest(String propertyId) {
        var values = contracts.query(Query.where("propertyId", propertyId).limit(10000));
        var active =
                values.stream()
                        .filter(
                                c ->
                                        !c.startDate().isAfter(clock.instant())
                                                && !c.endDate().isBefore(clock.instant()))
                        .max(Comparator.comparing(TenancyContract::startDate));
        return active.isPresent()
                ? active
                : values.stream().max(Comparator.comparing(TenancyContract::endDate));
    }

    public List<Map<String, Object>> views(List<TenancyContract> values) {
        if (values.isEmpty()) return List.of();
        var propertyMap = new HashMap<String, Property>();
        var tenantMap = new HashMap<String, User>();
        properties
                .query(
                        Query.where(
                                        "id",
                                        values.stream()
                                                .map(TenancyContract::propertyId)
                                                .distinct()
                                                .toList())
                                .limit(10000))
                .forEach(p -> propertyMap.put(p.id(), p));
        users.query(
                        Query.where(
                                        "id",
                                        values.stream()
                                                .map(TenancyContract::tenantId)
                                                .distinct()
                                                .toList())
                                .limit(10000))
                .forEach(u -> tenantMap.put(u.id(), u));
        return values.stream()
                .map(
                        c -> {
                            var p = propertyMap.get(c.propertyId());
                            return Data.map(
                                    "id",
                                    c.id(),
                                    "property_id",
                                    c.propertyId(),
                                    "tenant_id",
                                    c.tenantId(),
                                    "start_date",
                                    c.startDate(),
                                    "end_date",
                                    c.endDate(),
                                    "mng_fee_percentage",
                                    c.mngFeePercentage(),
                                    "rent_per_month",
                                    c.rentPerMonth(),
                                    "created_at",
                                    c.createdAt(),
                                    "updated_at",
                                    c.updatedAt(),
                                    "property",
                                    Data.map(
                                            "id",
                                            p.id(),
                                            "landlord_id",
                                            p.landlordId(),
                                            "name",
                                            p.name(),
                                            "size",
                                            p.size(),
                                            "price",
                                            p.price()),
                                    "tenant",
                                    UserService.contact(tenantMap.get(c.tenantId())));
                        })
                .toList();
    }

    public Map<String, Object> view(String id) {
        return views(List.of(get(id))).getFirst();
    }
}
