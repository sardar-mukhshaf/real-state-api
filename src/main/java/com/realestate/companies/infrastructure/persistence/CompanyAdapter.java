package com.realestate.companies.infrastructure.persistence;

import com.realestate.companies.application.CompanyRepository;
import com.realestate.companies.domain.Company;
import com.realestate.shared.infrastructure.persistence.JpaStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyAdapter extends JpaStore<Company, CompanyEntity> implements CompanyRepository {
    public CompanyAdapter(CompanyJpaRepository repository, EntityManager em) {
        super(
                repository,
                em,
                CompanyEntity.class,
                CompanyMapper::toDomain,
                CompanyMapper::toEntity);
    }
}
