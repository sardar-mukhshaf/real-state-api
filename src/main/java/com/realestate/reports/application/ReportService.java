package com.realestate.reports.application;

import com.realestate.companies.application.CompanyService;
import com.realestate.contracts.application.ContractService;
import com.realestate.properties.application.PropertyService;
import com.realestate.reports.domain.*;
import com.realestate.shared.application.Data;
import com.realestate.shared.domain.BusinessException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reports;
    private final PropertyService properties;
    private final ContractService contracts;
    private final CompanyService companies;
    private final Clock clock;

    public ReportService(
            ReportRepository reports,
            PropertyService properties,
            ContractService contracts,
            CompanyService companies,
            Clock clock) {
        this.reports = reports;
        this.properties = properties;
        this.contracts = contracts;
        this.companies = companies;
        this.clock = clock;
    }

    public Map<String, Object> summary(
            String propertyId, String startDate, String endDate, Integer year) {
        LocalDate from, until;
        try {
            if (startDate != null && endDate != null) {
                from = LocalDate.parse(startDate);
                until = LocalDate.parse(endDate).plusDays(1);
            } else if (year != null && startDate == null && endDate == null) {
                validateYear(year);
                from = LocalDate.of(year, 1, 1);
                until = from.plusYears(1);
            } else throw BusinessException.invalid("Provide startDate and endDate, or year");
            if (!until.isAfter(from) || until.isAfter(from.plusYears(10)))
                throw BusinessException.invalid(
                        "Date range must be ordered and no longer than 10 years");
        } catch (DateTimeException ex) {
            throw BusinessException.invalid("Invalid report dates");
        }
        var property = properties.view(propertyId, false);
        var rows = reports.aggregate(propertyId, from, until, null, false);
        var result = identity(property);
        var names =
                Map.of(
                        "EXPENSE",
                        "expenses",
                        "MANAGEMENT",
                        "managementFees",
                        "RENT",
                        "rentReceived",
                        "PAYMENT",
                        "landlordPayments");
        names.forEach(
                (type, name) -> {
                    var lines = rows.stream().filter(r -> r.type().equals(type)).toList();
                    result.put(name, Totals.from(lines));
                    result.put(name + "ByMonth", section(lines, false));
                });
        return result;
    }

    public Map<String, Object> monthly(String propertyId, int month, Integer year) {
        int selectedYear = year == null ? Year.now(clock).getValue() : year;
        validateYear(selectedYear);
        if (month < 1 || month > 12) throw BusinessException.invalid("Month must be 1–12");
        var property = properties.view(propertyId, false);
        var contract =
                contracts
                        .activeOrLatest(propertyId)
                        .orElseThrow(
                                () ->
                                        BusinessException.invalid(
                                                "No tenancy contract found for this property"));
        var rows =
                reports.aggregate(
                        propertyId,
                        LocalDate.of(selectedYear, 1, 1),
                        LocalDate.of(selectedYear + 1, 1, 1),
                        month,
                        true);
        var result = identity(property);
        result.put("income", section(ofType(rows, "RENT"), true));
        result.put("expenses", section(ofType(rows, "EXPENSE"), true));
        var fees = Totals.from(ofType(rows, "MANAGEMENT"));
        result.put("managementFee", section(ofType(rows, "MANAGEMENT"), true));
        result.put("landlordPayments", Totals.from(ofType(rows, "PAYMENT")));
        result.put(
                "management",
                Data.map(
                        "statementNo",
                        "ST" + LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyMMdd")),
                        "vatRegNo",
                        companies.first().map(c -> c.vatRegNo()).orElse(""),
                        "percentage",
                        contract.mngFeePercentage(),
                        "fee",
                        fees.net(),
                        "vat",
                        fees.vat(),
                        "total",
                        fees.gross()));
        result.put("year", selectedYear);
        result.put("month", month);
        return result;
    }

    private List<ReportLine> ofType(List<ReportLine> rows, String type) {
        return rows.stream().filter(r -> r.type().equals(type)).toList();
    }

    private Map<String, Object> section(List<ReportLine> lines, boolean descriptions) {
        var rows =
                lines.stream()
                        .map(
                                line -> {
                                    var totals = Totals.from(List.of(line));
                                    var row =
                                            Data.map(
                                                    "month",
                                                    line.month()
                                                            .format(
                                                                    DateTimeFormatter.ofPattern(
                                                                            "MMM uuuu",
                                                                            Locale.ENGLISH)),
                                                    "net",
                                                    totals.net(),
                                                    "vat",
                                                    totals.vat(),
                                                    "gross",
                                                    totals.gross());
                                    if (descriptions) row.put("description", line.description());
                                    return row;
                                })
                        .toList();
        return Data.map("byMonth", rows, "totals", Totals.from(lines));
    }

    private Map<String, Object> identity(Map<String, Object> property) {
        var addresses = (List<?>) property.get("property_address");
        return Data.map(
                "propertyLandlord",
                property.get("landlord"),
                "propertyAddress",
                addresses.isEmpty() ? null : addresses.getFirst());
    }

    private void validateYear(int year) {
        if (year < 1900 || year > 9998) throw BusinessException.invalid("Year must be 1900–9998");
    }
}
