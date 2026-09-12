package com.realestate.transactions.application;

import com.realestate.contracts.application.TenancyContractRepository;
import com.realestate.contracts.domain.TenancyContract;
import com.realestate.properties.application.PropertyRepository;
import com.realestate.properties.domain.Property;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.*;
import com.realestate.transactions.domain.*;
import com.realestate.users.application.LandlordRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LedgerService {
    public enum Kind {
        BASE,
        RENT,
        EXPENSE,
        MANAGEMENT,
        PAYMENT
    }

    private final LedgerEntryRepository entries;
    private final RentDetailRepository rents;
    private final FeeDetailRepository fees;
    private final ExpenseDetailRepository expenses;
    private final PaymentDetailRepository payments;
    private final TenancyContractRepository contracts;
    private final PropertyRepository properties;
    private final LandlordRepository landlords;
    private final IdempotencyStore idempotency;
    private final Clock clock;

    public LedgerService(
            LedgerEntryRepository entries,
            RentDetailRepository rents,
            FeeDetailRepository fees,
            ExpenseDetailRepository expenses,
            PaymentDetailRepository payments,
            TenancyContractRepository contracts,
            PropertyRepository properties,
            LandlordRepository landlords,
            IdempotencyStore idempotency,
            Clock clock) {
        this.entries = entries;
        this.rents = rents;
        this.fees = fees;
        this.expenses = expenses;
        this.payments = payments;
        this.contracts = contracts;
        this.properties = properties;
        this.landlords = landlords;
        this.idempotency = idempotency;
        this.clock = clock;
    }

    public LedgerEntry get(String id) {
        return entries.require(Rules.id(id));
    }

    @Transactional
    public LedgerEntry create(Changes data, Kind kind, String actor, String requestKey) {
        String key = null, fingerprint = null;
        if (requestKey != null) {
            if (!requestKey.matches("[A-Za-z0-9._:-]{8,128}"))
                throw BusinessException.invalid(
                        "Idempotency-Key must contain 8–128 safe characters");
            key = actor + ":" + kind + ":" + requestKey;
            fingerprint = Hashes.sha256(new TreeMap<>(data.values()).toString());
            idempotency.lock(key);
            var existing = idempotency.find(key);
            if (existing.isPresent()) {
                if (!existing.get().fingerprint().equals(fingerprint))
                    throw BusinessException.conflict(
                            "Idempotency key was already used with another request");
                return entries.find(existing.get().resultId())
                        .orElseThrow(
                                () ->
                                        BusinessException.conflict(
                                                "The original result has been deleted"));
            }
        }
        var entry = save(UUID.randomUUID().toString(), data, null, kind);
        if (key != null) idempotency.save(key, fingerprint, entry.id());
        return entry;
    }

    @Transactional
    public LedgerEntry update(String id, Changes data, Kind kind) {
        var old = entries.requireLocked(Rules.id(id));
        old.editable();
        if (kind == Kind.BASE) kind = kindFor(id);
        assertKind(id, kind);
        if (kind == Kind.RENT) assertUnpaidChildren(id);
        if (kind == Kind.PAYMENT && payment(id).status().equals("PAID"))
            throw BusinessException.conflict("Paid payments cannot be edited");
        return save(id, data, old, kind);
    }

    private LedgerEntry save(String id, Changes data, LedgerEntry old, Kind kind) {
        data.only(
                "property_id",
                "type",
                "description",
                "amount",
                "is_VAT",
                "transaction_date",
                "transaction_month",
                "tenant_id",
                "tenancy_contract_id",
                "start_date",
                "end_date",
                "landlord_id");
        String type =
                data.string(
                        "type", old != null ? old.type() : kind == Kind.BASE ? null : kind.name());
        if (type == null) throw BusinessException.invalid("Transaction type is required");
        if (kind != Kind.BASE && !type.equals(kind.name()))
            throw BusinessException.invalid("Transaction type does not match the endpoint");
        if (old != null && !old.type().equals(type))
            throw BusinessException.invalid("Transaction type is immutable");
        String propertyId = data.string("property_id", old == null ? null : old.propertyId());
        Property property = properties.require(Rules.id(propertyId));
        var entry =
                new LedgerEntry(
                        id,
                        Rules.money(
                                data.decimal("amount", old == null ? null : old.amount()), false),
                        data.bool("is_VAT", old != null && old.isVAT()),
                        type,
                        data.string("description", old == null ? null : old.description()),
                        data.instant(
                                "transaction_date", old == null ? null : old.transactionDate()),
                        data.integer("transaction_month", old == null ? 0 : old.transactionMonth()),
                        propertyId,
                        null,
                        old == null ? clock.instant() : old.createdAt(),
                        clock.instant());
        TenancyContract contract = null;
        RentDetail rent = null;
        if (kind == Kind.RENT) {
            var previous =
                    old == null
                            ? null
                            : rents.first("transactionId", id)
                                    .orElseThrow(() -> BusinessException.missing("Rent"));
            String contractId =
                    data.string(
                            "tenancy_contract_id",
                            previous == null ? null : previous.tenancyContractId());
            contract = contracts.requireLocked(Rules.id(contractId));
            String tenant = data.string("tenant_id", previous == null ? null : previous.tenantId());
            var start = data.instant("start_date", previous == null ? null : previous.startDate());
            var end = data.instant("end_date", previous == null ? null : previous.endDate());
            contract.validateRent(propertyId, tenant, start, end);
            rent =
                    new RentDetail(
                            previous == null ? UUID.randomUUID().toString() : previous.id(),
                            tenant,
                            id,
                            contractId,
                            start,
                            end,
                            previous == null ? clock.instant() : previous.createdAt(),
                            clock.instant());
        }
        entries.save(entry);
        switch (kind) {
            case RENT -> {
                rents.save(rent);
                synchronizeGenerated(entry, property, contract);
            }
            case EXPENSE -> {
                if (old == null)
                    expenses.save(
                            new ExpenseDetail(
                                    UUID.randomUUID().toString(),
                                    id,
                                    clock.instant(),
                                    clock.instant()));
            }
            case MANAGEMENT -> {
                if (old == null)
                    fees.save(
                            new FeeDetail(
                                    UUID.randomUUID().toString(),
                                    id,
                                    clock.instant(),
                                    clock.instant()));
            }
            case PAYMENT -> {
                var previous = old == null ? null : payment(id);
                String landlord =
                        data.string("landlord_id", previous == null ? null : previous.landlordId());
                landlords.require(Rules.id(landlord));
                if (!property.landlordId().equals(landlord))
                    throw BusinessException.invalid("Payment landlord must own the property");
                payments.save(
                        new PaymentDetail(
                                previous == null ? UUID.randomUUID().toString() : previous.id(),
                                "PENDING",
                                landlord,
                                id,
                                previous == null ? clock.instant() : previous.createdAt(),
                                clock.instant()));
            }
            case BASE -> {}
        }
        return entry;
    }

    private void synchronizeGenerated(
            LedgerEntry rent, Property property, TenancyContract contract) {
        var existing = entries.query(Query.where("sourceRentId", rent.id()).limit(10));
        var fee = contract.managementFee(rent.amount());
        upsertGenerated(rent, property, existing, "MANAGEMENT", fee);
        upsertGenerated(rent, property, existing, "PAYMENT", rent.amount().subtract(fee));
    }

    private void upsertGenerated(
            LedgerEntry rent,
            Property property,
            List<LedgerEntry> existing,
            String type,
            BigDecimal amount) {
        var old = existing.stream().filter(e -> e.type().equals(type)).findFirst().orElse(null);
        if (amount.signum() == 0) {
            if (old != null) entries.delete(old.id());
            return;
        }
        String id = old == null ? UUID.randomUUID().toString() : old.id();
        entries.save(
                new LedgerEntry(
                        id,
                        amount,
                        rent.isVAT(),
                        type,
                        (type.equals("MANAGEMENT") ? "Management fee" : "Landlord payment")
                                + " for rent "
                                + rent.id(),
                        rent.transactionDate(),
                        rent.transactionMonth(),
                        rent.propertyId(),
                        rent.id(),
                        old == null ? clock.instant() : old.createdAt(),
                        clock.instant()));
        if (type.equals("MANAGEMENT") && old == null)
            fees.save(
                    new FeeDetail(
                            UUID.randomUUID().toString(), id, clock.instant(), clock.instant()));
        if (type.equals("PAYMENT")) {
            var previous = old == null ? null : payment(id);
            payments.save(
                    new PaymentDetail(
                            previous == null ? UUID.randomUUID().toString() : previous.id(),
                            "PENDING",
                            property.landlordId(),
                            id,
                            previous == null ? clock.instant() : previous.createdAt(),
                            clock.instant()));
        }
    }

    private void assertUnpaidChildren(String id) {
        for (var child : entries.query(Query.where("sourceRentId", id).limit(10))) {
            entries.requireLocked(child.id());
            if (child.type().equals("PAYMENT") && payment(child.id()).status().equals("PAID"))
                throw BusinessException.conflict(
                        "A generated payment is paid; rent cannot be changed");
        }
    }

    private PaymentDetail payment(String entryId) {
        return payments.first("transactionId", entryId)
                .orElseThrow(() -> BusinessException.missing("Payment"));
    }

    private Kind kindFor(String id) {
        if (rents.first("transactionId", id).isPresent()) return Kind.RENT;
        if (payments.first("transactionId", id).isPresent()) return Kind.PAYMENT;
        if (expenses.first("transactionId", id).isPresent()) return Kind.EXPENSE;
        if (fees.first("transactionId", id).isPresent()) return Kind.MANAGEMENT;
        return Kind.BASE;
    }

    private void assertKind(String id, Kind kind) {
        if (kindFor(id) != kind) throw BusinessException.missing(kind.name() + " transaction");
    }

    @Transactional
    public boolean delete(String id) {
        var entry = entries.requireLocked(Rules.id(id));
        entry.editable();
        if (kindFor(id) == Kind.RENT) assertUnpaidChildren(id);
        if (kindFor(id) == Kind.PAYMENT && payment(id).status().equals("PAID"))
            throw BusinessException.conflict("Paid payments cannot be deleted");
        entries.delete(id);
        return true;
    }

    @Transactional
    public LedgerEntry paymentStatus(String id, String status) {
        if (!Set.of("PENDING", "PAID").contains(status))
            throw BusinessException.invalid("Invalid payment status");
        var initial = entries.require(Rules.id(id));
        if (initial.sourceRentId() != null) entries.requireLocked(initial.sourceRentId());
        var entry = entries.requireLocked(id);
        var detail = payment(id);
        if (detail.status().equals("PAID") && !status.equals("PAID"))
            throw BusinessException.conflict("Paid payments cannot be reopened");
        payments.save(
                new PaymentDetail(
                        detail.id(),
                        status,
                        detail.landlordId(),
                        id,
                        detail.createdAt(),
                        clock.instant()));
        return entry;
    }
}
