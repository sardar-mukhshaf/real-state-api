package com.realestate.companies.application;

import com.realestate.companies.domain.Company;
import com.realestate.shared.application.*;
import com.realestate.shared.domain.Rules;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {
    private final CompanyRepository companies;
    private final Clock clock;

    public CompanyService(CompanyRepository companies, Clock clock) {
        this.companies = companies;
        this.clock = clock;
    }

    @Transactional
    public Company create(String name, String vatRegNo) {
        return companies.save(
                new Company(
                        UUID.randomUUID().toString(),
                        name,
                        vatRegNo,
                        clock.instant(),
                        clock.instant()));
    }

    public Company get(String id) {
        return companies.require(Rules.id(id));
    }

    public List<Company> list(int page, int size) {
        return companies.query(Query.all().page(page, size));
    }

    public Optional<Company> first() {
        return companies.query(Query.all().sorted("createdAt", false).limit(1)).stream()
                .findFirst();
    }

    @Transactional
    public Company update(String id, Changes data) {
        data.only("name", "vat_reg_no");
        var old = companies.requireLocked(Rules.id(id));
        return companies.save(
                new Company(
                        id,
                        data.string("name", old.name()),
                        data.string("vat_reg_no", old.vatRegNo()),
                        old.createdAt(),
                        clock.instant()));
    }

    @Transactional
    public boolean delete(String id) {
        companies.requireLocked(Rules.id(id));
        companies.delete(id);
        return true;
    }
}
