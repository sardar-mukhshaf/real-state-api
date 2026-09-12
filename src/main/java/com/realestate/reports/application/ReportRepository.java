package com.realestate.reports.application;

import com.realestate.reports.domain.ReportLine;
import java.time.LocalDate;
import java.util.List;

public interface ReportRepository {
    List<ReportLine> aggregate(
            String propertyId,
            LocalDate from,
            LocalDate untilExclusive,
            Integer accountingMonth,
            boolean descriptions);
}
