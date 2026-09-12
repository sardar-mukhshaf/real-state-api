package com.realestate.reports.domain;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ReportLine(
        String type, YearMonth month, String description, BigDecimal gross, BigDecimal vat) {}
