package com.realestate.transactions.application;

import com.realestate.contracts.application.TenancyContractRepository;
import com.realestate.contracts.domain.TenancyContract;
import com.realestate.properties.application.*;
import com.realestate.properties.domain.Property;
import com.realestate.shared.application.*;
import com.realestate.transactions.domain.*;
import com.realestate.users.application.*;
import com.realestate.users.domain.User;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LedgerViews {
    private final LedgerEntryRepository entries;
    private final RentDetailRepository rents;
    private final PaymentDetailRepository payments;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final TenancyContractRepository contracts;

    public LedgerViews(
            LedgerEntryRepository entries,
            RentDetailRepository rents,
            PaymentDetailRepository payments,
            PropertyRepository properties,
            UserRepository users,
            TenancyContractRepository contracts) {
        this.entries = entries;
        this.rents = rents;
        this.payments = payments;
        this.properties = properties;
        this.users = users;
        this.contracts = contracts;
    }

    public PageResult<LedgerEntry> search(
            String propertyId,
            String propertyName,
            List<String> types,
            int page,
            int size,
            String sort,
            boolean descending) {
        if (propertyId != null)
            properties.require(com.realestate.shared.domain.Rules.id(propertyId));
        return entries.search(propertyId, propertyName, types, page, size, sort, descending);
    }

    public Map<String, Object> one(String id, LedgerService.Kind kind) {
        return views(List.of(entries.require(id)), kind).getFirst();
    }

    public List<Map<String, Object>> views(List<LedgerEntry> values, LedgerService.Kind kind) {
        if (values.isEmpty()) return List.of();
        var ids = values.stream().map(LedgerEntry::id).toList();
        var rentMap = new HashMap<String, RentDetail>();
        rents.query(Query.where("transactionId", ids).limit(10000))
                .forEach(r -> rentMap.put(r.transactionId(), r));
        var paymentMap = new HashMap<String, PaymentDetail>();
        payments.query(Query.where("transactionId", ids).limit(10000))
                .forEach(p -> paymentMap.put(p.transactionId(), p));
        var propertyMap = new HashMap<String, Property>();
        properties
                .query(
                        Query.where(
                                        "id",
                                        values.stream()
                                                .map(LedgerEntry::propertyId)
                                                .distinct()
                                                .toList())
                                .limit(10000))
                .forEach(p -> propertyMap.put(p.id(), p));
        var userIds = new HashSet<String>();
        rentMap.values().forEach(r -> userIds.add(r.tenantId()));
        paymentMap.values().forEach(p -> userIds.add(p.landlordId()));
        var userMap = new HashMap<String, User>();
        if (!userIds.isEmpty())
            users.query(Query.where("id", userIds).limit(10000))
                    .forEach(u -> userMap.put(u.id(), u));
        var contractMap = new HashMap<String, TenancyContract>();
        if (!rentMap.isEmpty())
            contracts
                    .query(
                            Query.where(
                                            "id",
                                            rentMap.values().stream()
                                                    .map(RentDetail::tenancyContractId)
                                                    .distinct()
                                                    .toList())
                                    .limit(10000))
                    .forEach(c -> contractMap.put(c.id(), c));
        return values.stream()
                .map(
                        e -> {
                            var property = propertyMap.get(e.propertyId());
                            var view =
                                    Data.map(
                                            "id",
                                            e.id(),
                                            "property_id",
                                            e.propertyId(),
                                            "type",
                                            e.type(),
                                            "description",
                                            e.description(),
                                            "amount",
                                            e.amount(),
                                            "is_VAT",
                                            e.isVAT(),
                                            "transaction_date",
                                            e.transactionDate(),
                                            "transaction_month",
                                            e.transactionMonth(),
                                            "created_at",
                                            e.createdAt(),
                                            "updated_at",
                                            e.updatedAt(),
                                            "property",
                                            PropertyService.summary(property),
                                            "source_rent_id",
                                            e.sourceRentId());
                            var rent = rentMap.get(e.id());
                            if (rent != null) {
                                var tenant = userMap.get(rent.tenantId());
                                var contract = contractMap.get(rent.tenancyContractId());
                                var tenancy =
                                        Data.map(
                                                "id",
                                                contract.id(),
                                                "property_id",
                                                contract.propertyId(),
                                                "tenant_id",
                                                contract.tenantId(),
                                                "start_date",
                                                contract.startDate(),
                                                "end_date",
                                                contract.endDate(),
                                                "rent_per_month",
                                                contract.rentPerMonth());
                                view.put(
                                        "rent_transaction",
                                        Data.map(
                                                "id",
                                                rent.id(),
                                                "first_name",
                                                tenant.firstName(),
                                                "last_name",
                                                tenant.lastName(),
                                                "email",
                                                tenant.email(),
                                                "start_date",
                                                rent.startDate(),
                                                "end_date",
                                                rent.endDate(),
                                                "tenancy_contract",
                                                tenancy));
                                if (kind == LedgerService.Kind.RENT) {
                                    view.put("tenant", UserService.contact(tenant));
                                    view.put("tenancy_contract", tenancy);
                                    view.put("tenancy_contract_id", contract.id());
                                    view.put("start_date", rent.startDate());
                                    view.put("end_date", rent.endDate());
                                }
                            }
                            var payment = paymentMap.get(e.id());
                            if (payment != null) {
                                var landlord = userMap.get(payment.landlordId());
                                view.put(
                                        "landlord_payment_transaction",
                                        Data.map(
                                                "id",
                                                payment.id(),
                                                "first_name",
                                                landlord.firstName(),
                                                "last_name",
                                                landlord.lastName(),
                                                "email",
                                                landlord.email(),
                                                "status",
                                                payment.status()));
                                if (kind == LedgerService.Kind.PAYMENT) {
                                    view.put("id", payment.id());
                                    view.put("transaction_id", e.id());
                                    view.put("landlord_id", payment.landlordId());
                                    var contact = UserService.contact(landlord);
                                    contact.put("status", payment.status());
                                    view.put("landlord", contact);
                                }
                            }
                            return view;
                        })
                .toList();
    }
}
