package com.realestate;

import static org.assertj.core.api.Assertions.*;

import com.realestate.contracts.domain.TenancyContract;
import com.realestate.reports.domain.*;
import com.realestate.shared.domain.BusinessException;
import com.realestate.uploads.domain.Upload;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AccountingTest {
    private final String id = UUID.randomUUID().toString(),
            tenant = UUID.randomUUID().toString(),
            property = UUID.randomUUID().toString();

    private TenancyContract contract(String fee) {
        return new TenancyContract(
                id,
                tenant,
                property,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2027-01-01T00:00:00Z"),
                new BigDecimal(fee),
                new BigDecimal("1000"),
                Instant.EPOCH,
                Instant.EPOCH);
    }

    @Test
    void feesUseDecimalRoundingAndZeroIsValid() {
        assertThat(contract("12.5").managementFee(new BigDecimal("999.99")))
                .isEqualByComparingTo("125.00");
        assertThat(contract("0").managementFee(new BigDecimal("1000")))
                .isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> contract("100.0001")).isInstanceOf(BusinessException.class);
    }

    @Test
    void rentMustMatchContractOwnershipAndDates() {
        var c = contract("10");
        assertThatCode(() -> c.validateRent(property, tenant, c.startDate(), c.endDate()))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                c.validateRent(
                                        UUID.randomUUID().toString(),
                                        tenant,
                                        c.startDate(),
                                        c.endDate()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(
                        () ->
                                c.validateRent(
                                        property,
                                        tenant,
                                        c.startDate().minusSeconds(1),
                                        c.endDate()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void totalsRetainTheDocumentedLegacyVatFormula() {
        var result =
                Totals.from(
                        List.of(
                                new ReportLine(
                                        "RENT",
                                        YearMonth.of(2026, 1),
                                        "Rent",
                                        new BigDecimal("100.00"),
                                        new BigDecimal("20.00"))));
        assertThat(result.net()).isEqualByComparingTo("80.00");
        assertThat(result.net().add(result.vat())).isEqualByComparingTo(result.gross());
    }

    @Test
    void uploadRequiresMatchingSignatureMimeAndExtension() {
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10, 0};
        assertThatCode(() -> new Upload("test.png", "image/png", png)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new Upload("test.jpg", "image/jpeg", png))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new Upload("../test.png", "image/png", png))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new Upload("test.svg", "image/svg+xml", "<svg/>".getBytes()))
                .isInstanceOf(BusinessException.class);
    }
}
