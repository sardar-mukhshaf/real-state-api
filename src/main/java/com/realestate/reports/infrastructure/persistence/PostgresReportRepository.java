package com.realestate.reports.infrastructure.persistence;

import com.realestate.reports.application.ReportRepository;
import com.realestate.reports.domain.ReportLine;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresReportRepository implements ReportRepository {
    private final JdbcTemplate jdbc;

    public PostgresReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ReportLine> aggregate(
            String propertyId,
            LocalDate from,
            LocalDate until,
            Integer accountingMonth,
            boolean descriptions) {
        String desc = descriptions ? "description" : "''";
        String sql =
                "SELECT type,extract(year from transaction_date)::int AS year,extract(month from transaction_date)::int AS month,"
                        + desc
                        + " AS description,"
                        + "sum(amount) AS gross,sum(CASE WHEN \"is_VAT\" THEN amount*0.20 ELSE 0 END) AS vat "
                        + "FROM \"Transaction\" WHERE property_id=? AND transaction_date>=? AND transaction_date<?"
                        + (accountingMonth == null ? "" : " AND transaction_month=?")
                        + " GROUP BY type,year,month"
                        + (descriptions ? ",description" : "")
                        + " ORDER BY year,month,type"
                        + (descriptions ? ",description" : "");
        var params =
                new ArrayList<Object>(
                        List.of(
                                propertyId,
                                java.sql.Timestamp.valueOf(from.atStartOfDay()),
                                java.sql.Timestamp.valueOf(until.atStartOfDay())));
        if (accountingMonth != null) params.add(accountingMonth);
        return jdbc.query(
                sql,
                (rs, row) ->
                        new ReportLine(
                                rs.getString("type"),
                                YearMonth.of(rs.getInt("year"), rs.getInt("month")),
                                rs.getString("description"),
                                rs.getBigDecimal("gross"),
                                rs.getBigDecimal("vat")),
                params.toArray());
    }
}
