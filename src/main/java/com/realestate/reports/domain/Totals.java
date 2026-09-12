package com.realestate.reports.domain;

import java.math.*;
import java.util.Collection;

public record Totals(BigDecimal net, BigDecimal vat, BigDecimal gross) {
    public static Totals from(Collection<ReportLine> lines) {
        var gross =
                lines.stream()
                        .map(ReportLine::gross)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
        var vat =
                lines.stream()
                        .map(ReportLine::vat)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
        return new Totals(gross.subtract(vat), vat, gross);
    }
}
