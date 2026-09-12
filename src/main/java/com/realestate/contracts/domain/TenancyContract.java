package com.realestate.contracts.domain;

import com.realestate.shared.domain.*;
import java.math.*;
import java.time.Instant;

public record TenancyContract(
        String id,
        String tenantId,
        String propertyId,
        Instant startDate,
        Instant endDate,
        BigDecimal mngFeePercentage,
        BigDecimal rentPerMonth,
        Instant createdAt,
        Instant updatedAt) {
    public TenancyContract {
        Rules.id(id);
        Rules.id(tenantId);
        Rules.id(propertyId);
        Rules.dates(startDate, endDate);
        rentPerMonth = Rules.money(rentPerMonth, false);
        if (mngFeePercentage == null
                || mngFeePercentage.signum() < 0
                || mngFeePercentage.compareTo(new BigDecimal("100")) > 0)
            throw BusinessException.invalid("Management fee must be between 0 and 100 percent");
        try {
            mngFeePercentage = mngFeePercentage.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw BusinessException.invalid("Management percentage supports 4 decimal places");
        }
    }

    public BigDecimal managementFee(BigDecimal rent) {
        return rent.multiply(mngFeePercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public void validateRent(String property, String tenant, Instant start, Instant end) {
        Rules.dates(start, end);
        if (!propertyId.equals(property) || !tenantId.equals(tenant))
            throw BusinessException.invalid("Rent property and tenant must match the contract");
        if (start.isBefore(startDate) || end.isAfter(endDate))
            throw BusinessException.invalid("Rent dates must be inside the tenancy contract");
    }
}
