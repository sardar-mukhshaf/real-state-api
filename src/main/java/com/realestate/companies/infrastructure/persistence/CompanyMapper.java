package com.realestate.companies.infrastructure.persistence;

import com.realestate.companies.domain.Company;

public final class CompanyMapper {
    private CompanyMapper() {}

    public static Company toDomain(CompanyEntity entity) {
        return new Company(
                entity.id, entity.name, entity.vatRegNo, entity.createdAt, entity.updatedAt);
    }

    public static CompanyEntity toEntity(Company value) {
        var entity = new CompanyEntity();
        entity.id = value.id();
        entity.name = value.name();
        entity.vatRegNo = value.vatRegNo();
        entity.createdAt = value.createdAt();
        entity.updatedAt = value.updatedAt();
        return entity;
    }
}
